package org.dvlyyon.net.ssh.sftp;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dvlyyon.common.net.ContextInfoException;
import org.dvlyyon.common.net.LoginException;
import org.dvlyyon.common.util.CommonUtils;
import org.dvlyyon.net.ssh.SSHClientFactory;
import org.dvlyyon.net.ssh.SSHClientInf;

public class SFTPCLIClientImpl implements SFTPCLIClientInf {
	String ipAddress;
	String userName;
	String password;
	String endPattern;
	String baseDir = null;
	int port;
	int timeout = -1;
	
	TransceiverInf transceiver = null;
	SSHClientInf  connection  = null;
	SFTPConnectInf sftpClient  = null;

	private static final Log log = LogFactory.getLog(SFTPCLIClientImpl.class);
	
	@Override
	public void setContext(Properties properties) throws ContextInfoException {
		ipAddress = properties.getProperty(SERVER_IP);
		if (ipAddress == null) throw new ContextInfoException("Please set IP address of netconf validation server");
		userName = properties.getProperty(USER_NAME);
		if (userName == null) throw new ContextInfoException("Plesae set user name to sign up netconf validation server");
		password = properties.getProperty(PASSWORD);
		if (password == null) throw new ContextInfoException("Please set password for user "+userName+" to sign up netconf validation server");
		String portS = properties.getProperty(SERVER_PORT);
		if (portS == null) portS = "22";
		if (CommonUtils.parseUnsignedInt(portS) <= 0) {
			throw new ContextInfoException("Please set port to sign up netconf validation server");
		}
		port = CommonUtils.parseUnsignedInt(portS);
		String timeoutS = properties.getProperty(TIMEOUT);
		if (timeoutS != null) timeout = CommonUtils.parseUnsignedInt(timeoutS);
		
		endPattern = properties.getProperty(END_PATTERN);
		if (endPattern == null) {
			endPattern = "^.*\\$$";
		}
		baseDir = properties.getProperty("baseDir");
	}

	@Override
	public void login() throws LoginException {
		try {
			connection = SSHClientFactory.get(null);
			connection.setConfig(ipAddress,port,userName,password, "shell", 30000);
			connection.connect();
			transceiver = TransceiverFactory.get(null);
			transceiver.setEndPattern(endPattern);
			transceiver.setIOStream(connection.getInputStream(), connection.getOutputStream());
			transceiver.setTimeout(timeout);
			String loginInfo = transceiver.signedIn();
//			System.out.println(loginInfo);
			log.info(String.format("Logging to %s@%s:%s with password:%s successfully!",userName,ipAddress,port,password));
			try {
				sftpClient = SFTPClientFactory.get(null);
				sftpClient.setConfig(ipAddress, port, userName, password);
				sftpClient.connect();
				if (baseDir != null) {
					sftpClient.cd(baseDir);
					transceiver.sendCommand("cd "+baseDir+"\n");
				}
			} catch (Exception e) {
				log.error(e);
				throw new SFTPException(e.getMessage());
			}
		} catch (ClassNotFoundException ce) {
			log.error(ce);
			throw new LoginException(ce.getMessage());
		} catch (TransceiverException tsEx) {
			System.out.println(tsEx.getResponse());
			if (connection != null) close();
			log.error(tsEx);
			throw new LoginException(tsEx.getMessage());
		} catch (SFTPException sftEx) {
			if (connection != null) close();
		}catch (Exception ex) {
			if (connection != null) close();
			log.fatal(ex.getMessage(),ex);
			throw new LoginException(ex.getMessage());
		}
	}

	@Override
	public void close() {
		if (sftpClient  != null)  sftpClient.stop();
		log.info("sftp session is closed");
		if (transceiver != null)  {
			try {
				transceiver.sendCmds("exit\n",false);
				transceiver.close();
				transceiver = null;
			} catch (Exception e) {
				log.error(e);
				transceiver = null;
			}
		}
		log.info("transceiver is closed");
		if (connection  != null)  connection.stop();
		log.info("Connection is closed");
	}

	@Override
	public void put(String fileName, String content) throws Exception {
		sftpClient.put(fileName, content);
	}

	@Override
	public String execute(String command) throws Exception{
		return transceiver.sendCommand(command);
	}

	@Override
	public void put(InputStream stream, String destFileName) throws Exception {
		sftpClient.put(stream, destFileName);
	}

}
