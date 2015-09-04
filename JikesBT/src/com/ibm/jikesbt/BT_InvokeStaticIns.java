package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_invokestatic instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_InvokeStaticIns extends BT_MethodRefIns {

	BT_InvokeStaticIns(BT_Method target, BT_Class targetClass) {
		super(opc_invokestatic, -1, target, targetClass);
		if (CHECK_USER && !target.isStatic())
			expect(Messages.getString("JikesBT.InvokeStatic_target_should_be_a_static_method__{0}_1") + target);
	}
	
	BT_InvokeStaticIns(BT_Method target) {
		this(target, target.getDeclaringClass());
	}

	BT_InvokeStaticIns(int opcode, int index, int poolIndex, BT_Method inM, LoadLocation loadedFrom)
		throws BT_DescriptorException, BT_ConstantPoolException {
		super(opcode, index, poolIndex, inM, loadedFrom);
	}

	public Object clone() {
		return new BT_InvokeStaticIns(target, targetClass);
	}

}
