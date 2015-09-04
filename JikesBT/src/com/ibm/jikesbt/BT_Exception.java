package com.ibm.jikesbt;

import java.io.IOException;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 The abstract class intended to be used as the base class for all
 exceptions intentionally thrown by JikesBT code except those based on
 {@link BT_RuntimeException}.
 

 * @author IBM
**/
public abstract class BT_Exception extends Exception {

	public final Throwable cause;
	
	BT_Exception() {
		cause = null;
	}
	
	BT_Exception(BT_Exception e) {
		cause = e;
	}
	
	BT_Exception(IOException e) {
		cause = e;
	}
	
	BT_Exception(RuntimeException e) {
		cause = e;
	}
	
	BT_Exception(String explanation) {
		super(explanation);
		cause = null;
	}

	private String addedMessages_ = "";

	/**
	 Appends text to the existing message.
	**/
	public void addMessage(String message) {
		addedMessages_ = addedMessages_ + " " + message;
	}

	public String getMessage() {
		return super.getMessage() + addedMessages_;
	}
	
	public Throwable getInitialCause() {
		Throwable t = this;
		Throwable initialException;
		do {
			initialException = t;
			if(t instanceof BT_Exception) {
				t =  ((BT_Exception) t).cause;
			} else {
				break;
			}	
		} while(t != null);
		return initialException;
	}
}
