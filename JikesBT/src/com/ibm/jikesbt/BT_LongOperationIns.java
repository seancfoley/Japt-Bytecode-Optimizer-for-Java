package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an
 opc_ladd,
 opc_lsub,
 opc_lmul,
 opc_ldiv,
 opc_lrem,
 opc_lneg,
 opc_lshl,
 opc_lshr,
 opc_lushr,
 opc_land,
 opc_lor,
 or
 opc_lxor
 instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_LongOperationIns extends BT_Ins {
	BT_LongOperationIns(int opcode, int index) {
		super(opcode, index);
	}
	public Object clone() {
		return new BT_LongOperationIns(opcode, -1);
	}
}
