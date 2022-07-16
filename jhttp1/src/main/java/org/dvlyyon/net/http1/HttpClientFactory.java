package org.dvlyyon.net.http1;

public class HttpClientFactory {
	public static HttpClientInf get(String type) {
		return new ApacheHttpClient();
	}
}
