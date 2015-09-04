package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */



/**
 Represents an instruction that manipulates doubles:
 opc_dadd,
 opc_dsub,
 opc_dmul,
 opc_ddiv,
 opc_drem, or
 opc_dneg.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_DoubleOperationIns extends BT_Ins {
	public BT_DoubleOperationIns(int opcode, int index) {
		super(opcode, index);
	}
	public Object clone() {
		return new BT_DoubleOperationIns(opcode, -1);
	}
}
