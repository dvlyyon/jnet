package org.dvlyyon.net.gnmi;

import java.util.List;
import java.util.Set;

public interface GnmiNBIMgrInf {
	public static final String CLEAR_TEXT 		= "clear_text";
	public static final String LISTEN_ON_PORT 	= "server_port";
	public static final String END_POINT 		= "end_point";
	public static final String SERVER_CERTIFICAT = "server_crt";
	public static final String SERVER_KEY		= "server_key";
	public static final String REQUEST_CLIENT_AUTH 	= "require_client_certificate";
	public static final String CLIENT_CA			= "client_crt";
	
	public void run() throws Exception;
	public Set<String> getSessions();
	public Set<String> getRPCs(String sessionId);
	public Object pop();
	public Object pop(String sessionId);
	public Object pop(String sessionId, String streamId);
	public List<GnmiEvent> popAll();
	public List<GnmiEvent> popAll(String sessionId);
	public List<GnmiEvent> popAll(String sessionId, String streamId);
	public int size();
	public int size(String sessionId);
	public int size(String sessionId, String streamId);
	public boolean isClosed();
	public boolean isError();
	public boolean isClosed(String sessionId);
	public void shutdown(String sessionId);
	public String getErrorInfo();
	public void shutdown();
}
