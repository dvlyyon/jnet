package org.dvlyyon.net.gnmi;

public interface BiDirectionStreamClientInf<T1,T2> {
	BiDirectionStreamMgrInf<T1,T2> getMgr() throws Exception;
	BiDirectionStreamMgrInf<T1,T2> getMgr(int capacity) throws Exception;
}
