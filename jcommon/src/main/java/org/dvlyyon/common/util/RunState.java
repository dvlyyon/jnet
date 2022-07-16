package org.dvlyyon.common.util;


public class RunState {
	public enum State {
		NORMAL,
		ERROR,
		EXCEPTION,
		INIT
	}
	
	private State result;
	private String errorInfo;
	private String info;
	private String comments;
	private Throwable exp;
	private Object extraInfo;

	public String getExtraInfo() {
		return (String)extraInfo;
	}

	public void setExtraInfo(String extraInfo) {
		this.extraInfo = extraInfo;
	}

	public RunState() {
		clear();
	}
	
	public void clear() {
		result=State.INIT;
		errorInfo=info=comments=null;
		extraInfo=null;
		exp=null;		
	}
	
	public void setExtraObject(Object extraObj) {
		this.extraInfo = extraObj;
	}
	
	public Object getExtraObject() {
		return extraInfo;
	}
	
	public State getResult() {
		return result;
	}
	public void setResult(State result) {
		this.result = result;
	}
	public String getErrorInfo() {
		return errorInfo;
	}
	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	public Throwable getExp() {
		return exp;
	}
	public void setExp(Throwable exp) {
		this.exp = exp;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

}
