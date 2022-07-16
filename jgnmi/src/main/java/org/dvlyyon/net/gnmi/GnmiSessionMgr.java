package org.dvlyyon.net.gnmi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;


public class GnmiSessionMgr
implements GnmiSessionSBIMgrInf,GnmiSessionInternalNBIMgrInf {
	private static final Logger logger = Logger.getLogger(GnmiSessionMgr.class.getName());
	private String prepare = null;
	private String name = null;
	private boolean isClosed = false;
	private Map <String, GnmiServerStreamObserver> rpcMap = null;
	
	public GnmiSessionMgr(String name) {
		this.name = name;
		this.rpcMap = new HashMap<String,GnmiServerStreamObserver>();
	}

	public void close() {
		isClosed = true;		
	}

	public void prepareAcceptRPC(String threadName) {
		prepare = threadName;
	}

	public void registerRPC(GnmiServerStreamObserver observer) {
		synchronized(rpcMap) {
			rpcMap.put(observer.getID(),(GnmiServerStreamObserver)observer);
		}
	}

	@Override
	public Object pop() {
		Object [] streams = null;
		synchronized(rpcMap) {
			Collection c = rpcMap.values();
			streams = c.toArray();
		}
		if (streams != null) {
			for (Object stream:streams) {
				GnmiServerStreamObserver q = (GnmiServerStreamObserver)stream;
				Object obj = q.poll();
				if (obj != null) return obj;
			}
		}
		return null;
	}

	@Override
	public List<GnmiEvent> popAll() {
		Object [] streams = null;
		synchronized(rpcMap) {
			Collection c = rpcMap.values();
			streams = c.toArray();
		}
		List<GnmiEvent> result = new ArrayList<GnmiEvent>();
		if (streams != null) {
			for (Object stream:streams) {
				GnmiServerStreamObserver q = (GnmiServerStreamObserver)stream;
				List<GnmiEvent> objs = q.pollAll();
				if (objs != null) result.addAll(objs);
			}
		}
		return result;
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public void shutdown() {
		synchronized(rpcMap) {
			for (Entry<String, GnmiServerStreamObserver> entry:rpcMap.entrySet()) {
				entry.getValue().close();
			}
			rpcMap.clear();
		}
	}

	@Override
	public Object pop(String streamName) {
		synchronized(rpcMap) {
			GnmiServerStreamObserver stream = rpcMap.get(streamName);
			if (stream != null)
				return stream.poll();
			else {
				throw new RuntimeException("No such rpc:" + streamName);
			}
		}
	}
	
	@Override
	public List<GnmiEvent> popAll(String streamName) {
		synchronized(rpcMap) {
			GnmiServerStreamObserver stream = rpcMap.get(streamName);
			if (stream != null)
				return stream.pollAll();
			else {
				throw new RuntimeException("No such rpc:" + streamName);
			}
		}
	}
	
	@Override
	public Set<String> getRPCs() {
		return rpcMap.keySet();
	}

	@Override
	public int size() {
		Object [] streams = null;
		synchronized(rpcMap) {
			Collection c = rpcMap.values();
			streams = c.toArray();
		}
		int size = 0;
		if (streams != null) {
			for (Object stream:streams) {
				GnmiServerStreamObserver q = (GnmiServerStreamObserver)stream;
				size += q.size();
			}
		}
		return size;
	}

	@Override
	public int size(String streamName) {
		GnmiServerStreamObserver stream = rpcMap.get(streamName);
		return stream.size();
	}
}
