package org.dvlyyon.common.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeMap;

public interface Connection {
	final String HOST = "host";
	final String PORT = "port";
	final String TCPTIMEOUT = "tcp_timeout";
	final String TIMEOUT = "timeout";
	
	void setConf(TreeMap<String,Object> config);
	TreeMap<String, Object> getConf();
	void connect() throws Exception;
	void close();
	InputStream getInputStream();
	OutputStream getOutputStream();
	String getHostname();
	int getPort();

}
