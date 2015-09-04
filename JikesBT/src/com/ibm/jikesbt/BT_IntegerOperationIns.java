package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an
 opc_iadd,
 opc_isub,
 opc_imul,
 opc_idiv,
 opc_irem,
 opc_ineg,
 opc_ishl,
 opc_ishr,
 opc_iushr,
 opc_iand,
 opc_ixor,
 or
 opc_ior
 instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_IntegerOperationIns extends BT_Ins {
	public BT_IntegerOperationIns(int opcode, int index) {
		super(opcode, index);
	}
	public Object clone() {
		return new BT_IntegerOperationIns(opcode, -1);
	}
}
