package com.ibm.jikesbt;


/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an
 opc_aload,  opc_aload_0,  opc_aload_1, opc_aload_2,  opc_aload_3,
 opc_iload,  opc_iload_0,  opc_iload_1, opc_iload_2,  opc_iload_3,
 opc_lload,  opc_lload_0,  opc_lload_1, opc_lload_2,  opc_lload_3,
 opc_fload,  opc_fload_0,  opc_fload_1, opc_fload_2,  opc_fload_3,
 opc_dload,  opc_dload_0,  opc_dload_1, opc_dload_2,  or opc_dload_3
 instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_LoadLocalIns extends BT_LocalIns {
	BT_LoadLocalIns(int opcode, int index, int localNr) {
		super(opcode, index, localNr);
	}
	
	BT_LoadLocalIns(
		int opcode,
		int index,
		int localNr,
		OpcodePair pair,
		boolean wide) {
		super(opcode, index, localNr);
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		OpcodePair base = getBase();
		
		BT_Ins next = (ins.size() > n + 1) ? ins.elementAt(n + 1) : null;
		BT_Ins next2 = (ins.size() > n + 2) ? ins.elementAt(n + 2) : null;
		
		//
		// when loading the same local twice in succession,
		// use a dup for the second load instead.
		//
		if (next != null
			&& next.opcode == opcode
			&& ((BT_LoadLocalIns) next).target.localNr == target.localNr
			&& !(target.localNr == 0 && next2 != null && next2.opcode == opc_getfield && base.base1 == opc_aload_0)
					 /* this last condition is here because in J9 it is better to have
						two aload_0 than aload_0/dup if a getfield follows
						because of the j9 specific aload0getfield instruction */) {
			return code.replaceInstructionsAtWith(
				1,
				n + 1,
				BT_Ins.make(isWide() ? opc_dup2 : opc_dup));
		}

		//
		// remove the load of a double or long followed by a pop2
		// skip if strict, load may not verify and we don't know here
		//
		if (!strict && next != null && next.opcode == opc_pop2 && isWide()) {
			return code.removeInstructionsAt(2, n);
		}

		//
		// remove the load of one word local followed by a pop
		// skip if strict, load may not verify and we don't know here
		//
		if (!strict && next != null && next.opcode == opc_pop && !isWide()) {
			return code.removeInstructionsAt(2, n);
		}

		//
		// replace iload n, iconst, iadd, plus some kind of store n, with iinc
		//
		int incr;
		if (ins.size() > n + 3
			&& base.base2 == opc_iload
			&& next instanceof BT_ConstantIntegerIns) {
			if (next2.opcode == opc_iadd || next2.opcode == opc_isub) {
				incr = next.getIntValue();
				if (next2.opcode == opc_isub)
					incr = -incr;
				if (!BT_Misc.overflowsSignedShort(incr)) {
					if (iincHelper(code, n + 3)) {
						code.removeInstructionsAt(3, n);
						code.insertInstructionAt(
							BT_Ins.make(opc_iinc, target.localNr, (short) incr),
							n);
						return true;
					}
				}
			}
		}
		if (n > 0
			&& ins.size() > n + 2
			&& base.base2 == opc_iload
			&& next.opcode == opc_iadd) {
			BT_Ins prev = ins.elementAt(n - 1);
			if (prev instanceof BT_ConstantIntegerIns) {
				incr = prev.getIntValue();
				if (!BT_Misc.overflowsSignedShort(incr)) {
					if (iincHelper(code, n + 2)) {
						code.removeInstructionsAt(3, n - 1);
						code.insertInstructionAt(
							BT_Ins.make(opc_iinc, target.localNr, (short) incr),
							n - 1);
						return true;
					}
				}
			}
		}

		//
		// replace "aload_0, bipush 6, istore_3, pop" with "bipush 6, istore_3"
		//
		if (!strict
			&& ins.size() > n + 3
			&& BT_Misc.opcodeStackHeight[opcode][0] == 0
			&& BT_Misc.opcodeStackHeight[opcode][1] == 1
			&& next.getStackDiff() == 1
			&& ins.elementAt(n + 2) instanceof BT_StoreLocalIns
			&& ins.elementAt(n + 3).opcode == opc_pop) {
			code.removeInstructionAt(n + 3);
			return code.removeInstructionAt(n);
		}

		//
		// replace "lload_0, bipush 6, istore_3, pop2" with "bipush 6,istore_3"
		//
		if (!strict
			&& ins.size() > n + 3
			&& BT_Misc.opcodeStackHeight[opcode][0] == 0
			&& BT_Misc.opcodeStackHeight[opcode][1] == 2
			&& next.getStackDiff() == 1
			&& ins.elementAt(n + 2) instanceof BT_StoreLocalIns
			&& ins.elementAt(n + 3).opcode == opc_pop2) {
			code.removeInstructionAt(n + 3);
			return code.removeInstructionAt(n);
		}
		
		//TODO flow analysis to find loads that are popped later, which goes in-hand
		//with the replacement of stores with pops if the stores are never loaded later
		//the benefits are minimal with normal code, but for inlined code this is something common
		//skip this optimization if strict==true
		 
		return false;
	}

	// See if there is a store of this local, or a dup/store of this
	// local at the given index. If so, an iinc instruction can be
	// used here.
	//
	private boolean iincHelper(BT_CodeAttribute code, int n) {
		BT_InsVector ins = code.getInstructions();
		BT_Ins instr = ins.elementAt(n);
		if (instr instanceof BT_StoreLocalIns
			&& ((BT_StoreLocalIns) instr).target == target) {
			return code.removeInstructionAt(n);
		}

		int i;
		if (instr.opcode == opc_dup) {
			i = findStoreForDup(ins, n + 1, 0);
			if (i >= 0) {
				code.removeInstructionAt(n + 1);
				return code.replaceInstructionsAtWith(
					1,
					n,
					BT_Ins.make(opc_iload, target.localNr));
			}
			i = findStoreForDup(ins, n + 1, 1);
			if (i >= 0) {
				code.removeInstructionAt(i);
				return code.replaceInstructionsAtWith(
					1,
					n,
					BT_Ins.make(opc_iload, target.localNr));
			}
		} else if (instr.opcode == opc_dup_x1) {
			i = findStoreForDup(ins, n + 1, 2);
			if (i >= 0) {
				code.removeInstructionAt(i);
				return code.replaceInstructionsAtWith(
					1,
					n,
					BT_Ins.make(opc_iload, target.localNr));
			}
		} else if (instr.opcode == opc_dup_x2) {
			i = findStoreForDup(ins, n + 1, 3);
			if (i >= 0) {
				code.removeInstructionAt(i);
				return code.replaceInstructionsAtWith(
					1,
					n,
					BT_Ins.make(opc_iload, target.localNr));
			}
		}
		return false;
	}
	private int findStoreForDup(BT_InsVector ins, int n, int startDepth) {
		int depth = startDepth;
		int i;
		for (i = n; i < ins.size(); ++i) {
			BT_Ins instr = ins.elementAt(i);
			if (instr.isBlockMarker())
				return -1; // End of block
			if (instr instanceof BT_LoadLocalIns
				&& ((BT_LoadLocalIns) instr).target == target)
				return -1; // Intervening load of same local
			if (depth == 0) {
				if (instr instanceof BT_StoreLocalIns
					&& ((BT_StoreLocalIns) instr).target == target)
					return i;
				return -1;
			}
			if (depth < 0)
				return -1;
			depth += instr.getStackDiff();
		}
		return -1;
	}

	public boolean isWide() {
		OpcodePair base = getBase();
		return base.base2 == opc_dload || base.base2 == opc_lload;
	}
	public Object clone() {
		BT_LoadLocalIns result = new BT_LoadLocalIns(opcode, -1, target.localNr);
		return result;
	}
	
	/**
	 * @return True for operations that read/write 2 consequent slots
	 */
	public boolean is2Slot() {
		OpcodePair base = getBase();
		int base2 = base.base2;
		return (base2 == opc_lload
			|| base2 == opc_dload);
	}
	
	public boolean isLocalReadIns() {
		return true;
	}
	public boolean isLocalLoadIns() {
		return true;
	}
	
}
