package org.dvlyyon.net.gnmi;

import java.util.List;

import gnmi.Gnmi.SubscribeRequest;
import gnmi.Gnmi.SubscribeResponse;

public interface SubscriptionInf {
	public void 					subscribe(SubscribeRequest request);
	public void 					unsubscribe();
	public boolean 					isComplete();
	public boolean					isError();
	public String					getErrorInfo();
	public List<Event<SubscribeResponse>> 	popAll();
	public Event<SubscribeResponse>		pop();
	public int						size();
	public long						getSubscriptionTime();
	public String					getSubscriptionTimeInRFC3399();
	public void 					pause();
	public void						resume();
}
