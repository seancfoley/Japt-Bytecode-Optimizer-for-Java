package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_dup2 instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_Dup2Ins extends BT_Ins {
	BT_Dup2Ins(int opcode, int index) {
		super(opcode, index);
	}
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		//
		// remove duplication of top two stack words, followed by pop2
		//
		if (ins.size() > n + 1 && ins.elementAt(n + 1).opcode == opc_pop2) {
			return code.removeInstructionsAt(2, n);
		}

		//
		// replace "dup2, store, pop2" by "store"
		//
		if (ins.size() > n + 2
			&& ins.elementAt(n + 2).opcode == opc_pop2
			&& ins.elementAt(n + 1) instanceof BT_StoreLocalIns) {
			return code.removeInstructionsAt(1, n + 2) && code.removeInstructionsAt(1, n);
		}

		//
		// replace "dup2, load, store, pop2" by "load, store"
		//
		if (ins.size() > n + 3
			&& ins.elementAt(n + 1) instanceof BT_LoadLocalIns
			&& ins.elementAt(n + 2) instanceof BT_StoreLocalIns
			&& ins.elementAt(n + 3).opcode == opc_pop2) {
			return code.removeInstructionsAt(1, n + 3) && code.removeInstructionsAt(1, n);
		}
		return false;
	}
	public Object clone() {
		return new BT_Dup2Ins(opcode, -1);
	}
}
