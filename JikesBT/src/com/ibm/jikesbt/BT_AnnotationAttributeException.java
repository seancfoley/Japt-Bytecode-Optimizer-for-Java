/*
 * Created on Sep 27, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.io.IOException;

/**
* @author IBM
**/
public class BT_AnnotationAttributeException extends BT_AttributeException {
	
	public BT_AnnotationAttributeException(String name) {
		super(name);
	}
	
	public BT_AnnotationAttributeException(String name, BT_BytecodeException e) {
		super(name, e);
	}
	
	public BT_AnnotationAttributeException(String name, BT_ConstantPoolException e) {
		super(name, e);
	}
	
	public BT_AnnotationAttributeException(String name, IOException e) {
		super(name, e);
	}
	
	public BT_AnnotationAttributeException(String name, BT_DescriptorException e) {
		super(name, e);
	}
	
	public BT_AnnotationAttributeException(String name, String explanation) {
		super(name, explanation);
	}
}
