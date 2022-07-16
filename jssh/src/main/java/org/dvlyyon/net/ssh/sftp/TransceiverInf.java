package org.dvlyyon.net.ssh.sftp;

import java.io.InputStream;
import java.io.OutputStream;

public interface TransceiverInf {
	public void setIOStream(InputStream in, OutputStream out);
	public void setEndPattern(String endPattern);
	public void setErrorPattern(String errPattern);
	public void setLoginOKPattern(String endPattern);
	public void setTimeout(int timeout);
	public String  signedIn() throws TransceiverException;
	public String  sendCommand(String cmd) throws TransceiverException;
	public String  sendCmds(String cmd,boolean sync) throws TransceiverException;
	public void close();
}
