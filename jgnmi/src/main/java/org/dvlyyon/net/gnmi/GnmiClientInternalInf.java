package org.dvlyyon.net.gnmi;

import gnmi.Gnmi.SubscribeRequest;
import gnmi.Gnmi.SubscribeResponse;
import io.grpc.stub.StreamObserver;

public interface GnmiClientInternalInf {
    public StreamObserver<SubscribeRequest> 
	subscribe(StreamObserver<SubscribeResponse> response, GnmiCredentialContextInf context);
}
