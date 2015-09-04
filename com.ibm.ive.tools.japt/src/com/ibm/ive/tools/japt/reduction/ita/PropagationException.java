package com.ibm.ive.tools.japt.reduction.ita;


public class PropagationException extends Exception {
	final Member from;
	final Member to;
	
	public PropagationException(String message) {
		this(null, null, message);
	}
	
	public PropagationException() {
		this(null, (Member) null);
	}
	
	public PropagationException(Member from, Member to) {
		this.from = from;
		this.to = to;
	}

	public PropagationException(Member from, Member to, String detailMessage) {
		super(detailMessage);
		this.from = from;
		this.to = to;
	}
	
	public PropagationException(Member to, String detailMessage) {
		this(null, to, detailMessage);
	}
}
