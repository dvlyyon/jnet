package org.dvlyyon.net.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeMap;

import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SSHJConnection implements SSHConnection {
	private final static Log logger = LogFactory.getLog(SSHConnection.class);
	
	SSHClient client;
	TreeMap<String, Object> config;

	@Override
	public void setConf(TreeMap<String, Object> config) {
		this.config = config;
	}

	@Override
	public TreeMap<String, Object> getConf() {
		return config;
	}

	@Override
	public void connect() throws Exception {
		DefaultConfig defaultConfig = new DefaultConfig();	
		defaultConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
		client = new SSHClient(defaultConfig);
		int connectTimeout = (int)config.get(SSHConnection.TCPTIMEOUT);
		int timeout = (int)config.get(SSHConnection.TIMEOUT);
		client.setConnectTimeout(connectTimeout);
		client.setTimeout(timeout);
		client.addHostKeyVerifier(new PromiscuousVerifier());
		String address = (String)config.get(SSHConnection.HOST);
		int port = (int)config.get(SSHConnection.PORT);
		client.connect(address,port);
		client.getConnection().getKeepAlive().setKeepAliveInterval(5);
	}

	@Override
	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			logger.error("Exception raised when closing sshj connection", e);
		}
	}

	@Override
	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHostname() {
		return (String)config.get(SSHConnection.HOST);
	}

	@Override
	public int getPort() {
		return (int)config.get(SSHConnection.PORT);
	}

	@Override
	public boolean authenticateWithPublicKey(String username, String certFileName, String passPhrase) 
			throws Exception {
		KeyProvider kp = null;
		if (passPhrase == null) {
			kp = client.loadKeys(certFileName);
		} else {
			kp = client.loadKeys(certFileName, passPhrase);
		}
		client.authPublickey(username, kp);
		return true;
	}

	@Override
	public boolean authenticateWithPassword(String username, String password) throws Exception {
		client.authPassword(username, password);
		return true;
	}

	@Override
	public SSHSession openSession() throws Exception {
		Session session = client.startSession();
		return new SSHJSession(session);
	}

}
