package org.dvlyyon.net.ssh.sftp;

public class TransceiverFactory {
	public static TransceiverInf get(String className) {
		return new SimpleTransceiver();
	}
}
