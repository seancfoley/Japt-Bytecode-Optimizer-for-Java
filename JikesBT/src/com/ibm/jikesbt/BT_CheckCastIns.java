package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_checkcast instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_CheckCastIns extends BT_ClassRefIns {

	BT_CheckCastIns(BT_Class target) {
		super(opc_checkcast, target);
	}

	/**
	 @param index  The byte offset of the instruction.
	   -1 mean unknown.
	**/
	BT_CheckCastIns(int opcode, int index, String className, BT_Repository repo) {
		super(opcode, index, className, repo);
	}

	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		// Prevent this instruction from being optimized away
		return false;
	}

	public Object clone() {
		return new BT_CheckCastIns(target);
	}
}
