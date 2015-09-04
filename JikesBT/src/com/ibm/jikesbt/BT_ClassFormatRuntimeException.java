package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 
 * @author IBM
**/
public abstract class BT_ClassFormatRuntimeException extends BT_RuntimeException {
	BT_ClassFormatRuntimeException() {}
	
	BT_ClassFormatRuntimeException(String explanation) {
		super(explanation);
	}
}
