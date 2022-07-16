package org.dvlyyon.net.gnmi;

import io.grpc.stub.StreamObserver;

public interface BiDirectionStreamInf <T1,T2>{
	StreamObserver<T1> openStream(GnmiStreamObserver<T2> outStream) throws Exception;
}
