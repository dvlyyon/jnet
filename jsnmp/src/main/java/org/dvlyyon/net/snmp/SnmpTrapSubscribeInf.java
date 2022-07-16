package org.dvlyyon.net.snmp;

import java.io.IOException;

public interface SnmpTrapSubscribeInf {
	public void subscribe(SnmpTrapListenerInf listener) throws IOException;
}
