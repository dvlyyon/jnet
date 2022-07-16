package org.dvlyyon.net.gnmi;

import gnmi.Gnmi.CapabilityRequest;
import gnmi.Gnmi.CapabilityResponse;
import gnmi.Gnmi.SubscribeRequest;
import gnmi.Gnmi.SubscribeResponse;

import static org.dvlyyon.net.gnmi.GnmiHelper.newCredential;
import static org.dvlyyon.net.gnmi.GnmiHelper.newHeaderResponseInterceptor;

import gnmi.gNMIGrpc;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.stub.StreamObserver;

public class GnmiProtoClient extends GnmiCommonClient implements GnmiClientInf {
	
	private gnmi.gNMIGrpc.gNMIStub stub;
	private CallCredentials credential;
	
	public GnmiProtoClient(GnmiClientContextInf context) throws Exception {
		this.context = context;
		channel = GnmiHelper.getChannel(context);			
		ClientInterceptor interceptor = newHeaderResponseInterceptor(context);
		Channel newChannel = ClientInterceptors.intercept(channel, interceptor);
		stub = gNMIGrpc.newStub(newChannel);
		if (context.needCredential()) {
			CallCredentials credential = newCredential(context);
			if (credential != null) {
				stub = stub.withCallCredentials(credential);
			} 	
		}
	}

	@Override
	public CapabilityResponse capacity() {
		CapabilityRequest request = CapabilityRequest.newBuilder().build();
		CapabilityResponse response = null;

		GnmiResponse <CapabilityResponse> myObserver =
				new GnmiResponse<CapabilityResponse>();
		stub.capabilities(request, myObserver);
		response = myObserver.getValue();
		if (response == null) {
			if (myObserver.isError())
				throw new RuntimeException(myObserver.getError());
		}
		return response;
	}

	@Override
	public StreamObserver<SubscribeRequest> subscribe(StreamObserver<SubscribeResponse> response,
			GnmiCredentialContextInf ccontext) {
		if (ccontext == null && context.needCredential()) {
			CallCredentials credential = newCredential(context);
			if (credential != null) {
				stub = stub.withCallCredentials(credential);
			}
		} else if (ccontext != null) {
			CallCredentials credential = newCredential(ccontext);
			if (credential != null) {
				stub = stub.withCallCredentials(credential);
			}
		}
		StreamObserver<SubscribeRequest> requestStream = stub.subscribe(response);
		return requestStream;
	}

	@Override
	public SubscriptionInf subscribe() {
		return new DefaultSubscriptionMgr(this);
	}

	@Override
	public SubscriptionInf subscribe(GnmiCredentialContextInf ccontext) {
		// TODO Auto-generated method stub
		return new DefaultSubscriptionMgr(this, ccontext);
	}
}
