package org.dvlyyon.net.ssh;

import org.dvlyyon.common.net.Connection;

public interface SSHConnection extends Connection{

	boolean authenticateWithPublicKey(String username, String certFileName, String passPhrase) throws Exception;


	boolean authenticateWithPassword(String username, String password) throws Exception;


	SSHSession openSession() throws Exception;

}
