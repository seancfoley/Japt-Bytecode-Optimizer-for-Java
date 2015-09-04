package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_dup instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_DupIns extends BT_Ins {
	BT_DupIns(int opcode, int index) {
		super(opc_dup, index);
	}
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();

		//
		// remove duplication of top stack word, followed by pop
		//
		if (ins.size() > n + 1 && ins.elementAt(n + 1).opcode == opc_pop) {
			return code.removeInstructionsAt(2, n);
		}
		
		// replace dup, dup, dup with dup, dup2
		if(n > 1 && ins.size() > n + 1 
			&& ins.elementAt(n - 1).opcode == opc_dup
			&& ins.elementAt(n + 1).opcode == opc_dup) {
			return code.replaceInstructionsAtWith(2, n, BT_Ins.make(opc_dup2));
		}
		
		//
		// replace "dup, store, pop" by "store"
		//
		if (ins.size() > n + 2
			&& ins.elementAt(n + 1) instanceof BT_StoreLocalIns
			&& ins.elementAt(n + 2).opcode == opc_pop) {
			return code.removeInstructionsAt(1, n + 2)
				&& code.removeInstructionsAt(1, n);
		}

		//
		// replace "dup, load, store, pop" by "load, store"
		//
		if (ins.size() > n + 3
			&& ins.elementAt(n + 1) instanceof BT_LoadLocalIns
			&& ins.elementAt(n + 2) instanceof BT_StoreLocalIns
			&& ins.elementAt(n + 3).opcode == opc_pop) {
			return code.removeInstructionsAt(1, n + 3)
				&& code.removeInstructionsAt(1, n);
		}
		return false;
	}
	public Object clone() {
		return new BT_DupIns(opcode, -1);
	}
}
