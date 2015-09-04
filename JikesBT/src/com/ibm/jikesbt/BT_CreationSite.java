package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents the relation between a class and a JVM "new" instruction that
 creates it suhc as ldc, ldc_w, new, anewarray and multianewarray.

 For a creation site the referenced class is always the class that is being created.
 The bytecodes might refer to another class such as the element class of an array type.
 See {@link BT_Class#addCreationSite} and {@link BT_MethodCallSite}.
 * @author IBM
**/
public class BT_CreationSite extends BT_ClassReferenceSite {

	BT_CreationSite(BT_CodeAttribute creator, BT_NewIns in1) {
		super(creator, in1);
	}

	public String toString() {
		String fromString = (from.getMethod() == null) ? from.toString() : from.getMethod().useName();
		return Messages.getString("JikesBT.{0}_created_at_{1}_1", new Object[] {getInstructionTarget(), fromString});
	}
	
	public BT_Class getCreation() {
		return getTarget();
	}
	
	public boolean isCreationSite() {
		return true;
	}
}
