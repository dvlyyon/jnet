package org.dvlyyon.net.snmp;

public class SnmpClientFactory {
	public static SnmpClientInf get(String className) {
		return new Snmp4jClientImpl();
	}

}
