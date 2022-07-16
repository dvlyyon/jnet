package org.dvlyyon.net.ssh;

public class SSHClientFactory {
	public static String DEFAULT_SSH_CLIENT_CLASS = "org.dvlyyon.net.ssh.SSHClientJsch";
	
	public static SSHClientInf get(String implClass) throws Exception {
		if (implClass == null)
			implClass = DEFAULT_SSH_CLIENT_CLASS;
		return (SSHClientInf)Class.forName(implClass).newInstance();
	}
}
