package org.dvlyyon.net.ssh;

public class SSHConnectionFactory {
	
	public static SSHConnection get(String thirdPartyLibName) {
		switch (thirdPartyLibName) {
		case "sshj":
			return new SSHJConnection();
		case "ganymed":
			return new GanymedConnection();
		default:
			return new SSHJConnection();
		}
	}

}
