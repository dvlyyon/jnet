package org.dvlyyon.net.gnmi;

public class GnmiEvent<T> implements Event<T> {
	public long timestamp;
	public T event;
	
	public void putEvent(T evt) {
		event = evt;
	}
	
	public void putTime(long time) {
		this.timestamp = time;
	}
	
	@Override
	public T getEvent() {
		// TODO Auto-generated method stub
		return event;
	}

	@Override
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return timestamp;
	}
	
	@Override
	public String toString() {
		return "timestamp:"+timestamp+"\n"+event.toString();
	}

}
