package org.dvlyyon.net.ssh;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeMap;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

public class GanymedConnection implements SSHConnection {
	
	TreeMap <String,Object>config = null;
	Connection connection = null;
	
	@Override
	public void connect() throws Exception {
		String addr = (String)config.get(SSHConnection.HOST);
		int port = (int)config.get(SSHConnection.PORT);
		int connectTimeout = (int)config.get(SSHConnection.TCPTIMEOUT);
		int kexTimeout = (int)config.get(SSHConnection.TIMEOUT);
		
		connection = new Connection(addr,port);
		connection.connect(null, connectTimeout, kexTimeout);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

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
	public boolean authenticateWithPublicKey(String username, String certFileName, String passPhrase) 
			throws Exception{
        File certFile = new File(certFileName);
        String decryptPasswd = passPhrase;
        if (decryptPasswd != null && decryptPasswd.equals(""))
        {
           decryptPasswd = null;
        }		
		return connection.authenticateWithPublicKey(username, certFile, decryptPasswd);
	}

	@Override
	public boolean authenticateWithPassword(String username, String password) throws Exception {
		return connection.authenticateWithPassword(username, password);
	}

	@Override
	public SSHSession openSession() throws Exception {
		Session session = connection.openSession();
		return new GanymedSession(session);
	}

	@Override
	public void setConf(TreeMap<String, Object> config) {
		this.config = config;	
	}

	@Override
	public TreeMap<String, Object> getConf() {
		return this.config;
	}

	@Override
	public int getPort() {
		return (int)config.get(SSHConnection.PORT);
	}

}
