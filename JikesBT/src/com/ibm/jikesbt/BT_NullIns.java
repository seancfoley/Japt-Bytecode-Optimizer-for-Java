package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_aconst_null instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_NullIns extends BT_Ins {
	BT_NullIns(int opcode, int index) {
		super(opcode, index);
	}
	public Object clone() {
		return new BT_NullIns(opcode, -1);
	}

	String appendValueTo(String other) {
		return other + null;
	}

}
