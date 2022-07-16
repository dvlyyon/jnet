package org.dvlyyon.net.gnmi;


public interface GnmiRPCListenerInf {

	void registerRPC(String threadName, GnmiServerStreamObserver observer);

}
