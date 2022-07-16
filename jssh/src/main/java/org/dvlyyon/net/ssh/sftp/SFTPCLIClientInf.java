package org.dvlyyon.net.ssh.sftp;

import java.io.InputStream;
import java.util.Properties;

import org.dvlyyon.common.net.ContextInfoException;
import org.dvlyyon.common.net.LoginException;

public interface SFTPCLIClientInf {
	public static final String SERVER_IP   	= "ipAddress";
	public static final String SERVER_PORT 	= "port";
	public static final String USER_NAME   	= "userName";
	public static final String PASSWORD	   	= "password";
	public static final String END_PATTERN 	= "endPattern";
	public static final String BASE_DIR	   	= "baseDir";	
	public static final String REFRESH_DSDL = "refresh";
	public static final String RELEAS_NUMBER= "releaseNum";
	public static final String BUILD_NUMBER = "buildNum";
	public static final String TIMEOUT		= "__timeout";

	public void setContext(Properties properties) throws ContextInfoException;
	public void login() throws LoginException;
	public void close();
	public void put(String fileName, String content) throws Exception;
	public void put(InputStream stream, String destFileName) throws Exception;
	public String execute(String command) throws Exception;

}
