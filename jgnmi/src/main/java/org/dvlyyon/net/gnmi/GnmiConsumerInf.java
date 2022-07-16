package org.dvlyyon.net.gnmi;

import java.util.List;

public interface GnmiConsumerInf<T> {

	public GnmiEvent<T> poll();

	public List<GnmiEvent<T>> pollAll();
	
	public int size();
	
	public String getID();
	
	public boolean isCompleted();
	
	public boolean isError();
	
	public String getErrorInfo();

    public void close();
}
