package org.dvlyyon.net.gnmi;

public interface GnmiTransportListenerInf {

	void addSession(String remoteClient);

	void deleteSession(String remoteClient);

	void prepareAcceptRPC(String threadName, String sessionID);

}
