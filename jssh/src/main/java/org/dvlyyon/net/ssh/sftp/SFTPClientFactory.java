package org.dvlyyon.net.ssh.sftp;

public class SFTPClientFactory {
	public static SFTPConnectInf get(String className) {
		return new SFTPConnectJsch();
	}
}
