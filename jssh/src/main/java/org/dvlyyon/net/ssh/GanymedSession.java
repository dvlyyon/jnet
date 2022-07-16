package org.dvlyyon.net.ssh;

import java.io.InputStream;
import java.io.OutputStream;

import ch.ethz.ssh2.Session;

public class GanymedSession implements SSHSession {
	Session session = null;
	
	public GanymedSession(Session session) {
		this.session = session;
	}
	
	@Override
	public SSHSession startSubSystem(String subsystem) throws Exception {
		session.startSubSystem(subsystem);
		return this;
	}

	@Override
	public InputStream getInputStream() {
		return session.getStdout();
	}

	@Override
	public OutputStream getOutputStream() {
		return session.getStdin();
	}

	@Override
	public void close() {
		session.close();
	}

}
