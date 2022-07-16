package org.dvlyyon.net.gnmi;

public interface Event <T>{
	T getEvent();
	long getTimestamp();
}
