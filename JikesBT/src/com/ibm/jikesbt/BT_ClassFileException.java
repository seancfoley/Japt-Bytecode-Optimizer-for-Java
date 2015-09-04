package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Thrown to terminate operating on (reading) one class due to an
 error within that class, or due to a conflict between that class
 and another.
 Any error in a class that would cause a VerifyError or a ClassFormatError
 at runtime should result in a BT_ClassFileException.
 Only used by JikesBT and external subclasses.
 May be caught within JikesBT where the error can be recovered from
 by making a stub class.
 * @author IBM
**/
public class BT_ClassFileException extends BT_Exception {
	private String equivalentRuntimeError;
	
	public BT_ClassFileException(String explanation) {
		super(explanation);
	}
	
	public BT_ClassFileException(BT_ConstantPoolException e) {
		super(e);
	}
	
	public BT_ClassFileException(BT_ClassFormatRuntimeException e) {
		super(e);
	}
	
	void setEquivalentRuntimeError(String error) {
		this.equivalentRuntimeError = error;
	}
	
	public String getEquivalentRuntimeError() {
		return equivalentRuntimeError;
	}
}