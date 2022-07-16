package org.dvlyyon.net.restconf;

public class RestconfFactory {
	public static RestconfClientInf get(String className) {
		return new RestconfClientImpl();
	}
}
