package org.dvlyyon.net.gnmi;

public interface GnmiSessionSBIMgrInf {

	void close();

	void prepareAcceptRPC(String threadName);

	void registerRPC(GnmiServerStreamObserver observer);

}
