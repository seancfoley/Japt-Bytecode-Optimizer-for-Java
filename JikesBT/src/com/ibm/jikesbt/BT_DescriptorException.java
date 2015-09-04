/*
 * Created on Sep 28, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

/**
 * 
 * @author sfoley
 *
 * Thrown to represent an invalid class name, method name, field name, or method signature.
 */
public class BT_DescriptorException extends BT_ClassFormatRuntimeException {
	
	public BT_DescriptorException(String explanation) {
		super(explanation);
	}

}
