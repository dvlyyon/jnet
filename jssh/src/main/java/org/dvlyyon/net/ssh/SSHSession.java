package org.dvlyyon.net.ssh;

import org.dvlyyon.common.net.Session;

public interface SSHSession extends Session {

	SSHSession startSubSystem(String subsystem) throws Exception;
}
