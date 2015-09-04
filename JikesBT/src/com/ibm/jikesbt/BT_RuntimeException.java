package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 The abstract class intended to be used as the base class for all
 exceptions derived from java.lang.RuntimeException that are
 intentionally thrown by JikesBT.
 Other exceptions thrown are based on {@link BT_Exception}.
 
 * @author IBM
**/
public abstract class BT_RuntimeException extends RuntimeException {

	BT_RuntimeException() {
		super();
	}
	
	BT_RuntimeException(String explanation) {
		super(explanation);
	}

	private String addedMessages_ = "";

	/**
	 Appends text to the existing message.
	**/
	public void addMessage(String message) {
		addedMessages_ = addedMessages_ + BT_Base.endl() + " -- " + message;
	}

	public String getMessage() {
		return super.getMessage() + addedMessages_;
	}
}
