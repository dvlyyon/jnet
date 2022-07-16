package org.dvlyyon.common.net;

import java.io.InputStream;
import java.io.OutputStream;

public interface Session {
	public void close();
	InputStream getInputStream();	
	OutputStream getOutputStream();
}
