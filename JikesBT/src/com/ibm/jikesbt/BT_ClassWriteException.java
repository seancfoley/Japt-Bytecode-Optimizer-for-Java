/*
 * Created on Oct 31, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

/**
 * 
 * @author sfoley
 *
 * Thrown due to problems writing a class to a a class file.
 */
public class BT_ClassWriteException extends BT_Exception {

	public BT_ClassWriteException() {
		super();
	}

	public BT_ClassWriteException(BT_Exception e) {
		super(e);
	}

	public BT_ClassWriteException(RuntimeException e) {
		super(e);
	}

	public BT_ClassWriteException(String explanation) {
		super(explanation);
	}

}
