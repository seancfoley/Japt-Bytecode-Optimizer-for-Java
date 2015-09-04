package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_invokevirtual instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_InvokeVirtualIns extends BT_MethodRefIns {

	BT_InvokeVirtualIns(BT_Method target, BT_Class targetClass) {
		super(opc_invokevirtual, -1, target, targetClass);
		if (CHECK_USER
			&& (target.isConstructor()
				|| target.isPrivate()
				|| target.isStatic()
				|| targetClass.isInterface()))
			expect(
				Messages.getString("JikesBT.InvokeVirtual_target_should_not_be_a_constructor,_private,_static,_nor_in_an_interface__{0}_1", target));
	}
	
	BT_InvokeVirtualIns(BT_Method target) {
		this(target, target.getDeclaringClass());
	}

	BT_InvokeVirtualIns(int opcode, int index, int poolIndex, BT_Method inM, LoadLocation loadedFrom)
			throws BT_DescriptorException, BT_ConstantPoolException {
		super(opcode, index, poolIndex, inM, loadedFrom);
	}

	public boolean isInvokeVirtual(String className, String methodName) {
		return target.cls.name.equals(className)
			&& target.name.equals(methodName);
	}
	
	public boolean isInvokeVirtual(String className, String methodName, String sig) {
		return isInvokeVirtual(className, methodName) && target.getSignature().toString().equals(sig);
	}

	
	public Object clone() {
		return new BT_InvokeVirtualIns(target, targetClass);
	}
}
