package org.dvlyyon.net.netconf.transport.ssh;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dvlyyon.common.net.RFC3399Timestamp;
import org.dvlyyon.common.util.CLRunnable;
import org.dvlyyon.common.util.CLThread;
import org.dvlyyon.common.util.XMLUtils;
import org.dvlyyon.net.netconf.NotificationListenerIf;
import org.dvlyyon.net.netconf.transport.HelloResponseProcessorIf;
import org.jdom2.Element;

public class InterleaveSshConnection extends SshConnection {
	/** Logger for tracing */
	private final static Log s_logger = LogFactory.getLog(InterleaveSshConnection.class);

	/** The base NETCONF namespace */
	private final static String BASE_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";

	/** The namespace for notifications */
	private final static String NOTIF_NAMESPACE = "urn:ietf:params:xml:ns:netconf:notification:1.0";

	/** Host to which we are connecting */
	private String mHost;

	/** Listener that is to be invoked on receipt of notifications */
	private NotificationListenerIf mNotificationListener;
	private HelloResponseProcessorIf mHelloListener;

	/** True if we are ready for notifications */
	private boolean mReadyToReceiveNotifications;

	/** Time of the latest notification received */
	private Timestamp mLastReceivedNotificationTime;

	/** Thread used to handle notification messages from the device */
	private CLThread mListenerThread;


	/** Handle to callback that is to be invoked with response data */
	private BlockingQueue<ResponseCallbackIf> mCallbackQueue;
	
	private BlockingQueue<Element> mResponseQueue;
	
	private final int DEFAULT_QUEUE_SIZE = 2000;
	private final int MAX_CONCURRENT_RPC_NUM = 1000;
	private int mQueueSize = 2000;

	private CLThread mResponseWorkerThread;


	InterleaveSshConnection(
			Properties connectionProperties, 
			HelloResponseProcessorIf helloListener, 
			NotificationListenerIf notificationListener) throws RuntimeException {
		this(connectionProperties, helloListener,  notificationListener, true);
	}

	InterleaveSshConnection(
			Properties connectionProperties, 
			HelloResponseProcessorIf helloListener, 
			NotificationListenerIf notificationListener,
			boolean connectNow)  throws RuntimeException {
		super(connectionProperties, null, false);
		mHost = connectionProperties.getProperty("host", "localhost");
		String queueCapacity = connectionProperties.getProperty("queueCapacity", String.valueOf(DEFAULT_QUEUE_SIZE));
		try {
			mQueueSize = Integer.parseInt(queueCapacity);
		} catch (NumberFormatException e) {
			mQueueSize = DEFAULT_QUEUE_SIZE;
		}
		mResponseQueue = new ArrayBlockingQueue<Element>(mQueueSize);
		mCallbackQueue = new ArrayBlockingQueue<ResponseCallbackIf>(MAX_CONCURRENT_RPC_NUM);
		this.mNotificationListener = notificationListener;
		this.mHelloListener = helloListener;
		if (connectNow) {
			connect();
		}
	}
	
	@Override
	public void connect() {
		super.connect();
		NotificationListener listener = new NotificationListener(this);
		String threadName = "NETCONF Notification (" + mHost ;
		mListenerThread = new CLThread(threadName, listener);
		mListenerThread.startup();	 
		ResponseWorker worker = new ResponseWorker(this);
		threadName = "Response worker";
		mResponseWorkerThread = new CLThread(threadName, worker);
		mResponseWorkerThread.startup();
		
	}

	/**
	 * Sends an XML request, logging the transmission data.
	 *
	 * @param data                XML data to send.
	 * @param callback            Callback invoked when the response is received.
	 * @throws RuntimeException   if an error occurred.
	 */
	public void send(Element data, ResponseCallbackIf callback) throws RuntimeException
	{
		send(data, callback, true);
	}


	/**
	 * Called to perform a NETCONF RPC-type operation synchronously. This call blocks until a response is received.
	 *
	 * @param data                XML representing the NETCONF request.
	 * @param callback            Handle to callback which is to be invoked with the response.
	 * @param traceWireData       true to log the XML over-the-wire, false if no logging.
	 * @throws RuntimeException   if an error occurs while sending the request. An exception is also thrown if a
	 *                            synchronous transmission is currently going on.
	 */
	public synchronized void send(Element data, ResponseCallbackIf callback, boolean traceWireData) throws RuntimeException
	{
		// We have an interlock here to ensure request/response framework is maintained
		// and we allow concurrent RPCs
//		if (mCallInProgress)
//		{
//			throw new RuntimeException("A synchronous call is already in progress");
//		}
//		mCallInProgress = true;
		if (callback != null) { //synchronous mode
			boolean available = mCallbackQueue.offer(callback);
			if (!available) {
				throw new RuntimeException("There are too much concurrent RPCs ( " + MAX_CONCURRENT_RPC_NUM + ")");
			}
		};
		try
		{
			syncSend(data, false, traceWireData);
		}
		finally
		{
			// If there was any exception sending or receiving data (before the callback has been invoked), mark our flag
//			mCallInProgress = false;
		}
	}


	@Override
	protected void handleHelloResponse(Element response) {
		this.mHelloListener.processHelloResponse(response);
	}

	@Override
	protected void handleResponse(Element response) {
		boolean inserted = mResponseQueue.offer(response);
		if (!inserted) {
			s_logger.error("ERROR: Cannot inseart response into queue due to fullness:\n"+response);
		}
	}

	protected Element retrieveResponse()  throws InterruptedException {
		return (mResponseQueue.take());
	}

	protected void processResponse(Element response) {
		try
		{
			// We are interested in three types of responses
			// RPC-OK (in response to create-subscription); verify using the message ID and OK response
			String responseType = response.getName();
			if (responseType.equals("rpc-reply"))
			{
				// This must be a response to the create-subscription message; verify using the message ID
				// 
//				Namespace ns = Namespace.getNamespace(BASE_NAMESPACE);
//				String messageId = response.getAttributeValue("message-id");
//				if (mCreateSubscriptionMessageId.equals(messageId))
//				{
//					Element ok = response.getChild("ok", ns);
//					if (ok != null)
//					{
//						s_logger.debug("Got an OK response to create-subscription.");
//						mReadyToReceiveNotifications = true;
//					}
//					else
//					{
//						s_logger.warn("Expected an OK response to create-subscription; did not get one");
//						s_logger.warn("Actual response: " + XMLUtils.toXmlString(response));
//						mReadyToReceiveNotifications = false;
//					}
//				}
//				else
//				{
//					//s_logger.info(XMLUtils.toXmlString(response));
//					s_logger.info("Reply received to keep-alive ping on async. channel");
//				}
				ResponseCallbackIf caller = mCallbackQueue.poll();
				if (caller == null) {
					s_logger.error("We receive a RPC response and no request to it or it is async RPC:\n" + response);
					Timestamp currentTime = new Timestamp(System.currentTimeMillis());
					this.mNotificationListener.notify(currentTime, response);
				} else {
					caller.processResponse(response);
				}
			}
			else if (responseType.equals("replayComplete"))
			{
				// We don't really care about this one
				s_logger.info("Received ReplayComplete indicator from device");
				Timestamp currentTime = new Timestamp(System.currentTimeMillis());
				this.mNotificationListener.notify(currentTime, response);				
			}
			else if (responseType.equals("notificationComplete")) {
				s_logger.info("Received NotificationComplete information");
				Timestamp currentTime = new Timestamp(System.currentTimeMillis());
				this.mNotificationListener.notify(currentTime, response);
			}
			else if (responseType.equals("notification"))
			{
//				Element root = null;
				if (s_logger.isDebugEnabled())
				{
					s_logger.debug("Top-level notification XML: " + XMLUtils.toXmlString(response));
				}
				// Notifications (call the listener)
				s_logger.debug("Got a notification from device: " + responseType);
				Timestamp eventTime = null;
//				ArrayList<Element> dataNodes = new ArrayList<Element>();
				List<Element> kids = (List<Element>) response.getChildren();
				for (Element kid : kids)
				{
					if (kid.getName().equals("eventTime") && kid.getNamespaceURI().equals(NOTIF_NAMESPACE))
					{
						try
						{
							eventTime = new RFC3399Timestamp(kid.getText()).getSqlTimestamp();
							break;
						}
						catch (final Exception ex)
						{
							s_logger.warn("Error parsing event date: " + kid.getText() + " using current time");;
							eventTime = new Timestamp(System.currentTimeMillis());
						}
					}
//					else
//					{
//						dataNodes.add(kid);
//					}
				}
//				if (dataNodes.size() > 0)
//				{
//					root = new Element("data");
//					for (Element dataNode : dataNodes)
//					{
//						dataNode.detach();
//						root.addContent(dataNode);
//					}
//					if (s_logger.isDebugEnabled())
//						s_logger.debug("Notification data: " + XMLUtils.toXmlString(root));
//				}
				mLastReceivedNotificationTime = eventTime;
//				mNotificationListener.notify(eventTime, root);
				mNotificationListener.notify(eventTime, response);
			}
			else
			{
				s_logger.warn("Unexpected XML message received from device: " + responseType);
				Timestamp currentTime = new Timestamp(System.currentTimeMillis());
				this.mNotificationListener.notify(currentTime, response);
			}
		}
		catch (final Exception fex)
		{
			// Any exception in event processing should just ignore the event, instead of throwing it to the
			// caller (since the thread it is being called in will bag out otherwise)
			s_logger.error("Error processing notification: " + XMLUtils.toXmlString(response, true));
			if (s_logger.isDebugEnabled())
			{
				s_logger.error(fex, fex);
			}
		}
	}
	
	@Override
	public void disconnect() {
		s_logger.info("disconnect is called in interleave connection");
	      if (mListenerThread != null) {
	         mListenerThread.shutdown();
	      }
	      if (mResponseWorkerThread != null) {
	    	  mResponseWorkerThread.shutdown();
	      }
	      super.disconnect();
	}

	/**
	 * The NotificationListener class is a runnable that listens to notification messages from a device.
	 *
	 * @author  Subramaniam Aiylam
	 * @since   1.6
	 */
	private static class NotificationListener implements CLRunnable
	{

		/** The parent connection */
		InterleaveSshConnection mParent;

		/**
		 * Creates a NotificationListener.
		 *
		 * @param connection    parent connection.
		 */
		NotificationListener(InterleaveSshConnection connection)
		{
			mParent = connection;
		}

		@Override
		public void run() throws InterruptedException
		{
			boolean noErrors = true;
			while (noErrors)
			{
				try
				{
					// Call out to the base stream reader
					mParent.streamDataInFromWire(true);
				}
				catch (final Exception ex)
				{
					// Give up on an exception? Or continue?
					s_logger.warn("Error encountered while reading stream: " + ex.getMessage());
					s_logger.error(ex, ex);
					// Indicate to the upper layer (via NotificationListener)  that the connection needs to be reset
					mParent.mNotificationListener.connectionTerminated();
					noErrors = false;
				}
			}
			s_logger.info("Exiting notification loop..");
		}
	}

	private static class ResponseWorker implements CLRunnable
	{

		/** The parent connection */
		InterleaveSshConnection mParent;

		/**
		 * Creates a worker class to retrieve response elements.
		 *
		 * @param connection    parent connection.
		 */
		ResponseWorker(InterleaveSshConnection connection)
		{
			mParent = connection;
		}

		@Override
		public void run() throws InterruptedException
		{
			boolean noErrors = true;
			while (noErrors)
			{
				try
				{
					Element response = mParent.retrieveResponse();
					mParent.processResponse(response);
				}
				catch (final Exception ex)
				{
					// Give up on an exception? Or continue?
					s_logger.warn("Error encountered while reading stream: " + ex.getMessage());
					if (s_logger.isDebugEnabled())
					{
						s_logger.error(ex, ex);
					}
				}
			}
			s_logger.info("Exiting notification loop..");
		}
	}

	
}
