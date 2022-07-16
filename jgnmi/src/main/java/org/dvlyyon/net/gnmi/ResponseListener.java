package org.dvlyyon.net.gnmi;

public interface ResponseListener<T> {
	public void onNext(T value);
}
