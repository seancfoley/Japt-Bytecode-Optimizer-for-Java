package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_instanceof instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_InstanceofIns extends BT_ClassRefIns {
	BT_InstanceofIns(BT_Class target) {
		super(opc_instanceof, target);
	}
	BT_InstanceofIns(int opcode, int index, String className, BT_Repository repository) {
		super(opcode, index, className, repository);
	}
	public Object clone() {
		return new BT_InstanceofIns(target);
	}
}
