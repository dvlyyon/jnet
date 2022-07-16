package org.dvlyyon.net.gnmi;

public interface GnmiStreamListenerInf<T> {

	void onNext(T value, String valueOf);

	void onError(Throwable t, String valueOf);

	void onCompleted(String valueOf);

}
