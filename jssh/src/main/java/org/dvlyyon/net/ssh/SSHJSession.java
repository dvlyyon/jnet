package org.dvlyyon.net.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.schmizz.sshj.connection.channel.Channel;
import net.schmizz.sshj.connection.channel.direct.PTYMode;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Subsystem;

public class SSHJSession implements SSHSession {
	private final static Log logger = LogFactory.getLog(SSHJSession.class);
	private Channel session;

	public SSHJSession(Session session) {
		this.session = session;
	}

	@Override
	public void close() {
		try {
			session.close();
		} catch (IOException e) {
			logger.error("Exception raised when close sshj session", e);
		}
	}

	@Override
	public InputStream getInputStream() {
		return session.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}

	@Override
	public SSHSession startSubSystem(String subsystem) throws Exception {
		session = ((Session)session).startSubsystem(subsystem);
		return this;
	}

}
