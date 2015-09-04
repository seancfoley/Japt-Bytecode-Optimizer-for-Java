package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */



/**
 Represents an opc_invokespecial instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_InvokeSpecialIns extends BT_MethodRefIns {

	BT_InvokeSpecialIns(BT_Method target, BT_Class targetClass) {
		super(opc_invokespecial, -1, target, targetClass);
	}
	
	/**
	 @param  target  Must be a constructor, private, or a method in an ancestor.
	**/
	BT_InvokeSpecialIns(BT_Method target) {
		super(opc_invokespecial, -1, target);
	}

	/**
	 @param  inM  The method containing the instruction (not the method referenced).
	**/
	BT_InvokeSpecialIns(int opcode, int index, int poolIndex, BT_Method inM, LoadLocation loadedFrom)
		throws BT_DescriptorException, BT_ConstantPoolException {
		super(opcode, index, poolIndex, inM, loadedFrom);
	}

	public Object clone() {
		return new BT_InvokeSpecialIns(target, targetClass);
	}

	/**
	 @return  true if this is an invokespecial of the specified method.
	**/
	public boolean isInvokeSpecial(String className, String methodName) {
		return target.cls.name.equals(className)
			&& target.name.equals(methodName);
	}
}
