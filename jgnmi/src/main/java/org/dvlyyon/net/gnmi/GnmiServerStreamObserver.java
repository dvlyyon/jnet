package org.dvlyyon.net.gnmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.stub.StreamObserver;

public class GnmiServerStreamObserver <T1,T2> 
implements StreamObserver<T1>, GnmiConsumerInf<T1> {
	private static final Logger logger = 
			Logger.getLogger(GnmiStreamObserver.class.getName());
	StreamObserver<T2> outStream = null;
	String name;
	Queue <GnmiEvent<T1>> queue = null;
	boolean isError = false;
	boolean isCompleted = false;
	String  errorInfo = null;

	public GnmiServerStreamObserver(StreamObserver<T2> outStream, String name) {
		this.outStream = outStream;
		this.name = name;
		this.queue = new ConcurrentLinkedQueue<GnmiEvent<T1>>();
	}
		
	@Override
	public GnmiEvent<T1> poll() {
		synchronized(queue) {
			return queue.poll();
		}
	}
	
	@Override
	public List<GnmiEvent<T1>> pollAll() {
		Object [] result = null;
		synchronized(queue) {
			result = queue.toArray();
			queue.clear();
		}
		if (result == null) return null;
		List <GnmiEvent<T1>> list = new ArrayList<GnmiEvent<T1>>();
		for (Object o:result) list.add((GnmiEvent<T1>)o);
		return list;
	}
	
	@Override
	public void onNext(T1 value) {
		GnmiEvent<T1> event = new GnmiEvent<T1>();
		event.putTime(System.currentTimeMillis());
		event.putEvent(value);
		synchronized(queue) {
			queue.offer(event);
		}
	}

	@Override
	public void onError(Throwable t) {
		logger.log(Level.SEVERE,"OnError:",t);
		StringBuilder sb = new StringBuilder();
		sb.append(t.toString());
		getCause(sb,t.getCause());
		errorInfo = sb.toString();
		if (outStream != null) {
			outStream.onError(t);
			outStream = null;
		} else {
			logger.info("OUTSTREAM is NULL");
		}
		isError = true;
	}

	@Override
	public void onCompleted() {
		if (outStream != null) {
		    outStream.onCompleted();
		    outStream = null;
		}
		isCompleted = true;
		logger.info("OUTSTREAM is NULL");
	}

	@Override
	public void close() {
		if (outStream != null)
			outStream.onCompleted();
		else {
			logger.info("OUTSTREAM is NULL");
		}
	}
	
	@Override
	public boolean isCompleted() {
		return isCompleted;
	}
	

	@Override
	public boolean isError() {
		return isError;
	}

	private void getCause(StringBuilder sb, Throwable t) {
		if (t != null) {
			sb.append("\nCaused by:").append(t);
			getCause(sb,t.getCause());
		}
	}

	@Override
	public String getErrorInfo() {
		return errorInfo;
	}

	@Override
	public String getID() {
		return name + String.valueOf(this.hashCode());
	}

	@Override
	public int size() {
		return queue.size();
	}
	

}
