package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_nop instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_NopIns extends BT_Ins {
	BT_NopIns(int index) {
		super(opc_nop, index);
	}
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		//
		// remove the "nop" instruction
		//
		return code.removeInstructionAt(n);
	}
	public Object clone() {
		return new BT_NopIns(-1);
	}
}
