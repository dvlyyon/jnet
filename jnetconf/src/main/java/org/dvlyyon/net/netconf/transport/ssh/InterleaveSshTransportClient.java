package org.dvlyyon.net.netconf.transport.ssh;

import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dvlyyon.net.netconf.NotificationListenerIf;
import org.dvlyyon.net.netconf.transport.HelloResponseProcessorIf;
import org.dvlyyon.net.netconf.transport.TransportClientIf;
import org.jdom2.Element;
import org.jdom2.Namespace;

public class InterleaveSshTransportClient implements TransportClientIf, HelloResponseProcessorIf {

	/** Logger for tracing */
	protected final static Log s_logger = LogFactory.getLog(InterleaveSshTransportClient.class);

	/** Connection information as properties */
	private Properties mProperties;

	/** Client hello-response processor */
	private HelloResponseProcessorIf  mHelloListener;

	private NotificationListenerIf   mNotificationListener;

	/** The interleave-enabled SSH connection */
	private InterleaveSshConnection mConnection;

	/** Flag that indicates the current SSH session is busy */
	private boolean mBusy;

	/** Flag that indicates the current SSH session is running within a "transaction" */
	private boolean mTransactionStarted;

	/** The base NETCONF namespace */
	private final static String BASE_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";

	/** The namespace for notifications */
	private final static String NOTIF_NAMESPACE = "urn:ietf:params:xml:ns:netconf:notification:1.0";

	@Override
	public void setup(Properties properties, HelloResponseProcessorIf hrp) {
		// TODO Auto-generated method stub
		throw new RuntimeException("It is not expected to here!");
	}   

	@Override
	/**
	 * Only call this once.
	 * The properties that define all the connection parameters:<ol>
	 * <li>host - Name or IP address of device</li>
	 * <li>port - Port over which to do SSH</li>
	 * <li>username - SSH login user name</li>
	 * <li>password - SSH login password (or certificate file descryption password)</li>
	 * <li>certificate - Fully qualified path to (OpenSSH-style PEM) certificate file</li>
	 * <li>socketTimeout - socket timeout in seconds</li>
	 * <li>responseTimeout - Time to wait for RPC response in seconds</li>
	 * <li>netconfTraceFile - Reference to a LoggerIf used for tracing.</li>
	 * <li>subsystem - SSH subsystem - defaults to "netconf" if not specified.</li>
	 * </ol>
	 */
	public void setup(Properties properties, HelloResponseProcessorIf hrp, NotificationListenerIf notifListener) {
		mHelloListener = hrp;
		mNotificationListener = notifListener;
		mProperties = properties;

		if (mConnection == null)
			mConnection = new InterleaveSshConnection(properties, this, notifListener, false);
	}

	private boolean mConnected = false;

	private boolean mNotificationsSupported;

	private boolean mInterleaveSupported;

	public boolean isConnected()
	{
		return mConnected;
	}

	public boolean isAsyn() {
		String mode = this.mProperties.getProperty("communicationMode","sync");
		if (mode.equals("async")) return true;
		return false;
	}
	
	/**
	 * Create the Synchronous SSH connection (or just connect it if alread created), and establish sessions
	 */
	public void connect()
	{
		if (mConnection == null)
			mConnection = new InterleaveSshConnection(mProperties, mHelloListener, mNotificationListener, true);
		else
			mConnection.connect();

		//m_nexus.establishSession();   // this is done in the sshConnection.connect
		mConnected = true;
	}

	private boolean acquireSession() throws RuntimeException
	{
		boolean acquired = false;
		// TODO; Make this a configurable parameter
		final int CLIENT_WAIT_QUANTUM = 100;
		int quantumCount = 100;                 // 10 seconds total wait
		while (mBusy && quantumCount > 0)
		{
			try
			{
				Thread.sleep(CLIENT_WAIT_QUANTUM);
			}
			catch (InterruptedException iex)
			{
				throw new RuntimeException("Waiting for session available - interrupted");            
			}
			quantumCount--;
		}
		synchronized (this)
		{
			if (!mBusy)
			{
				mBusy = true;
				acquired = true;
			}
			// TODO: How is this not always acquired??  all threads will come through the sync block eventually
		}
		if (acquired)
		{
			s_logger.debug("Session acquired");
		}
		else
		{
			this.s_logger.info("mBusy:"+mBusy);
			throw new RuntimeException("Timed out waiting for session to be available");
		}
		return acquired;
	}

	/**
	 * Obtains an SSH connection, with which to perform configuration. The connection can be obtained in one of two ways:<ul>
	 * <li>From a connection pool, if there is no transaction currently running in this thread</li>
	 * <li>From the transaction context, if there is a transaction in progress</li>
	 * </ul>
	 *
	 * @param xid                 Transaction ID (if required), or NULL if not in transaction.
	 * @return                    Synchronous SSH connection to be used to configure the device.
	 * @throws RuntimeException   if an error occurred obtaining a connection.
	 */
	private synchronized InterleaveSshConnection obtainConnection(String xid) throws RuntimeException
	{
		if (!isConnected())
			connect();

		if (xid != null)
		{
			// Compare current XID (which is just the connection) to the XID passed in
			if (!xid.equals(mConnection.toString()))
			{
				throw new RuntimeException("Transaction ID mismatch");
			}
		}
		return mConnection;
	}

	/**
	 * Destroys the connection.
	 */
	private /*synchronized*/ void destroyConnection()
	{
		if (mConnection!=null)
		{
			mConnection.disconnect();
			mConnection = null;
			mConnected = false;
		}
	}

	@Override
	public void startNotifications(String stream, Timestamp startTime, NotificationListenerIf listener)
			throws RuntimeException {
		throw new RuntimeException("It is not expect to reach here, try other API");
	}

	@Override
	public void startNotifications(String stream, Element filter, String startTime, String stopTime,
			NotificationListenerIf listener) throws RuntimeException {
		throw new RuntimeException("It is not expect to reach here, try other API");
	}

	private static Element createSubscriptionMessageXml(final String messageId, final String stream, final Element filter, 
			final String startTime, final String stopTime)
	{
		Namespace netconfNs = Namespace.getNamespace(BASE_NAMESPACE);
		Element rpc = new Element("rpc", netconfNs);
		rpc.setAttribute("message-id", messageId);
		Namespace notificationNs = Namespace.getNamespace(NOTIF_NAMESPACE);
		Element cs = new Element("create-subscription", notificationNs);
		rpc.addContent(cs);
		// Add the stream name (if specified)
		if (stream != null)
		{
			Element streamElem = new Element("stream", notificationNs);
			streamElem.setText(stream);
			cs.addContent(streamElem);
		}

		if (filter != null)
		{
			cs.addContent(filter);
		}

		// Add the start time (if specified)
		if (startTime != null)
		{
			Element fromWhen = new Element("startTime", notificationNs);
			fromWhen.setText(startTime);
			cs.addContent(fromWhen);
		}

		if (stopTime != null)
		{
			Element stopAt = new Element("stopTime", notificationNs);
			stopAt.setText(stopTime);
			cs.addContent(stopAt);    	  
		}
		return rpc;
	}

	@Override
	public Element startNotifications(final String stream, final Element filter, 
			final String startTime, final String stopTime, final String messageId) throws RuntimeException
	{
		if (!mNotificationsSupported)
		{
			throw new RuntimeException("Netconf notifications are not supported by this device");
		}
		if (!mInterleaveSupported)
		{
			throw new RuntimeException("Interleave are not supported by this device");
		}
		s_logger.info("Subscribing for notifications: - stream: " + stream + "; startTime: " + startTime);
		Element msg = createSubscriptionMessageXml(messageId, stream, filter, startTime, stopTime);
		Element response = this.send(msg, null);
		return response;
	}

	@Override
	public void stopNotifications(String stream) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopAllNotifications() {
		// TODO Auto-generated method stub

	}

	@Override
	public String startTransaction() throws RuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	private Element sendInAsynchronousMode(Element data, String xid) throws RuntimeException {
		try {
			obtainConnection(xid);
			s_logger.debug("to send request in asynchronous mode...");
		} catch (Exception ex) {
			mBusy = false;
			destroyConnection();
			throw ex;
		}
		try {
			mConnection.send(data, null);
		} catch (Exception ex) {
			mBusy = false;
			destroyConnection();
			throw ex;
		}
		String messageId = data.getAttributeValue("message-id");
		Element response = new Element("messageId");
		response.setText(messageId);
		return response;
	}
	
	@Override
	public Element send(Element data, String xid) throws RuntimeException {
		if (isAsyn()) {
			return sendInAsynchronousMode(data, xid);
		}
		boolean destroy = false;
		// Make sure the session is available and grab it
		acquireSession();
		// Create a session (if necessary) and send the stuff
		try {
			obtainConnection(xid);
			s_logger.debug("About to send request..");
		} catch (Exception ex) {
			mBusy = false;
			destroyConnection();
			throw ex;
		}
		try
		{
			String timeoutStr = mProperties.getProperty("responseTimeout", "-1");
			Sender s = new Sender(mConnection, data, Integer.parseInt(timeoutStr));
			Element response = s.getResponse();
			return response;
		}
		catch (final Exception ex)
		{
			// TODO: Optimization - Figure out if this was an IO (i.e. a socket exception), only then set destroy to true
			destroy = true;
			s_logger.error("Exception during NETCONF rpc send", ex);
			if (s_logger.isDebugEnabled())
			{
				s_logger.error(ex, ex);
			}
			throw new RuntimeException("An error occured sending in the SSH transport layer: " + ex.toString());
		}
		finally
		{
			if (destroy)
			{
				destroyConnection();
			}
			mBusy = false;
		}
	}

	@Override
	public void commitTransaction(String xid) throws RuntimeException {
		// TODO Auto-generated method stub

	}

	@Override
	public void rollbackTransaction(String xid) throws RuntimeException {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() {
		try
		{
			this.stopAllNotifications();
			this.destroyConnection();
			//	         m_transactionStarted = false;
			mBusy = false;
		}
		catch (final Exception ex)
		{
			s_logger.error("Exception shutting down client: " + ex.getMessage());
			//if (s_logger.isDebugEnabled())
			{
				s_logger.error(ex, ex);
			}
		}
	}

	/**
	 * The Sender class is a helper that helps to convert the full-duplex behavior of SSH to one that works more like a request-response
	 * (or half-duplex) protocol, which is what NETCONF RPC behaves like.
	 *
	 * @author  Subramaniam Aiylam
	 * @since   1.6
	 */
	static class Sender implements ResponseCallbackIf
	{
		/** Time to wait for a NETCONF response from the device before giving up  - defaults to 5 seconds */
		private static final int NETCONF_RESPONSE_TIMEOUT = 2 * 60 * 1000;

		/** The NETCONF RPC response from the device */
		private Element m_response = null;


		/**
		 * Creates a Sender with the specified parameters.
		 *
		 * @param nexus            SSH connection being used for device communications.
		 * @param request          NETCONF RPC request.
		 * @param timeoutInSeconds seconds for which to wait for response before declaring failure.
		 * @throws Exception       if an error occurred.
		 */
		Sender(final InterleaveSshConnection connection, final Element request, final int timeoutInSeconds) throws Exception
		{
			connection.send(request, this);
			try
			{
				int timeout = NETCONF_RESPONSE_TIMEOUT;
				if (timeoutInSeconds > 0)
				{
					timeout = timeoutInSeconds * 1000;
				}
				boolean wait = true;
				while (wait) {
					synchronized (this)
					{
						if (m_response == null)
						{
							this.wait(timeout);
						}
					}
					if (m_response != null) break;
					long lastUpdate = connection.getLastUpdate();
					long currentTime = System.currentTimeMillis();
					if ((currentTime-lastUpdate) >= timeout) {
						wait = false;
					}
				}
				if (m_response == null)
				{
					s_logger.warn("No response from device - timed out.");
					throw new Exception("Timed out waiting for response.");
				}
			}
			catch (final InterruptedException ie)
			{
				s_logger.warn("SSH Sender interrupted");
			}
		}

		/** */
		Element getResponse()
		{
			return m_response;
		}

		@Override
		public void processResponse(Element response)
		{
			m_response = response;
			// Release the waiting thread
			synchronized (this)
			{
				this.notify();
			}
		}
	}

	@Override
	public void processHelloResponse(Element helloMessage) throws RuntimeException {
		mHelloListener.processHelloResponse(helloMessage);
		Namespace xmlns = Namespace.getNamespace(BASE_NAMESPACE);
		Element capsRoot = helloMessage.getChild("capabilities", xmlns);
		if (capsRoot == null)
		{
			s_logger.error("Invalid hello response received; no capabilites sent by device.");
		}
		else
		{
			List<Element> caps = capsRoot.getChildren("capability", xmlns);
			for (Element capElem : caps)
			{
				String capName = capElem.getText().trim();
				if (capName.equals("urn:ietf:params:netconf:capability:notification:1.0"))
				{
					mNotificationsSupported = true;
				}
				if (capName.equals("urn:ietf:params:netconf:capability:interleave:1.0"))
				{
					mInterleaveSupported = true;
				}
				if (mNotificationsSupported && mInterleaveSupported) break;
			}
		}
	}


}
