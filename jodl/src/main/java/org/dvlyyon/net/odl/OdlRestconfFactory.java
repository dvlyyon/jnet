package org.dvlyyon.net.odl;

public class OdlRestconfFactory {
	public static OdlRestconfClientInf get(String type) {
		return new OdlRestconfClientImpl();
	}
}
