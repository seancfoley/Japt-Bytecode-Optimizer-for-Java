package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 The exception thrown by {@link BT_Factory#fatal}.

 * @author IBM
**/
public class BT_FatalRuntimeException extends BT_RuntimeException {

	BT_FatalRuntimeException(String explanation) {
		super(explanation);
	}
}
