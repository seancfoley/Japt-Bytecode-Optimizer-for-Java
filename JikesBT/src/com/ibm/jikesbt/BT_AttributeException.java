/*
 * Created on Sep 27, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.io.IOException;

/**
Thrown to terminate operating on (reading) one class file attribute due to an
error.
Only used by JikesBT and external subclasses.
May be caught within JikesBT where the error can be recovered from.
If an error in an attribute means that the class is unusable, then a BT_ClassFileAttribute should be used
instead.  This is generally not the case.  Only the BT_CodeAttribute can render a class unusable when invalid,
and even then the class may be usable in the sense that it may be loadable by a virtual machine.
* @author IBM
**/
public class BT_AttributeException extends BT_Exception {
	String attributeName;
	
	public BT_AttributeException(String name) {
		attributeName = name;
	}
	
	public BT_AttributeException(String name, BT_BytecodeException e) {
		super(e);
		attributeName = name;
	}
	
	public BT_AttributeException(String name, BT_ConstantPoolException e) {
		super(e);
		attributeName = name;
	}
	
	public BT_AttributeException(String name, IOException e) {
		super(e);
		attributeName = name;
	}
	
	public BT_AttributeException(String name, BT_DescriptorException e) {
		super(e);
		attributeName = name;
	}
	
	public BT_AttributeException(String name, String explanation) {
		super(explanation);
		attributeName = name;
	}
	
	String getAttributeName() {
		return attributeName == null ? "<unknown>" : attributeName;
	}
}
