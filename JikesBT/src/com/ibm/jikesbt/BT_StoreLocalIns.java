package com.ibm.jikesbt;




/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an
 opc_astore, opc_astore_0,  opc_astore_1, opc_astore_2,  opc_astore_3,
 opc_istore, opc_istore_0,  opc_istore_1, opc_istore_2,  opc_istore_3,
 opc_lstore, opc_lstore_0,  opc_lstore_1, opc_lstore_2,  opc_lstore_3,
 opc_fstore, opc_fstore_0,  opc_fstore_1, opc_fstore_2,  opc_fstore_3,
 opc_dstore, opc_dstore_0,  opc_dstore_1, opc_dstore_2,  or opc_dstore_3
 instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_StoreLocalIns extends BT_LocalIns {

	BT_StoreLocalIns(
		int opcode,
		int index,
		int localNr) {
		super(opcode, index, localNr);
	}

	BT_StoreLocalIns(
		int opcode,
		int index,
		int localNr,
		boolean wide) {
		super(opcode, index, localNr);
	}

	public Object clone() {
		BT_StoreLocalIns result = new BT_StoreLocalIns(opcode, -1, target.localNr);
		return result;
	}

	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		OpcodePair base = getBase();
		
		//
		// check for <constant>, astore_x with no other astore_x
		// then we can do constant propagation, replace astore_x by <constant>
		//
		// This is disabled because there is no flow analysis, so if local x is read before the <constant>, then
		// then we cannot replace with <constant> 
		if (false && n > 0 && ins.elementAt(n - 1).isConstantIns()) {
			BT_Ins constant = ins.elementAt(n - 1);
			boolean foundOtherStore = false;
			for (int k = 0; k < ins.size() && !foundOtherStore; k++) {
				if (k != n) {
					BT_Ins other = ins.elementAt(k);
					if (other.equals(this)) {
						foundOtherStore = true;
					} else {
						if (other instanceof BT_IIncIns
							&& ((BT_IIncIns) other).target == target) {
							foundOtherStore = true;
						} else {
							if (other instanceof BT_RetIns
								&& ((BT_RetIns) other).target == target) {
								foundOtherStore = true;
							}
						}
					}
				}
			}
			if (!foundOtherStore) {
				for (int k = 0; k < ins.size(); k++) {
					if (isCorrespondingLoadIns(ins.elementAt(k))) {
						code.replaceInstructionsAtWith(
							1,
							k,
							(BT_Ins) constant.clone());
					}
				}
				return code.removeInstructionsAt(2, n - 1);
			}
		}

		//
		// check for store_x, load_x and replace with dup_x, store_x
		//
		if (ins.size() > n + 1
			&& ins.elementAt(n + 1).isLocalLoadIns()) {
			
			BT_LoadLocalIns nextIns = (BT_LoadLocalIns) ins.elementAt(n + 1);
			
			
			// Check that they refer to the same local and are of the same type
			if (nextIns.target.localNr == target.localNr
				&& base.base2 - BT_Ins.opc_istore
					== nextIns.getBase().base2 - BT_Ins.opc_iload) {
				code.removeInstructionAt(n + 1);
				code.insertInstructionAt(
					BT_Ins.make(
						nextIns.getStackDiff() == 1 ? opc_dup : opc_dup2),
					n);
				return true;
			}
		}

		// For the following optimizations we can't be sure that the top of the stack contains the correct type.
		if (strict)
			return false;

		//
		// check for store followed by a return
		//
		if (ins.size() > n + 1 && ins.elementAt(n + 1).isReturnIns()) {
			return code.replaceInstructionsAtWith(
				1,
				n,
				(base.base2 == opc_dstore || base.base2 == opc_lstore)
					? BT_Ins.make(opc_pop2)
					: BT_Ins.make(opc_pop));
		}

		//Note that dead stores are now located globally in BT_CodeAttribute.verifyAndOptimizeGlobally
		return false;
	}

	public boolean isLocalWriteIns() {
		return true;
	}
	
	boolean isCorrespondingLoadIns(BT_Ins ins) {
		return ins instanceof BT_LoadLocalIns
			&& ((BT_LoadLocalIns) ins).target.localNr == target.localNr;
	}
	
	/**
	 * @return True for operations that read/write 2 consequent slots
	 */
	public boolean is2Slot() {
		OpcodePair base = getBase();
		int base2 = base.base2;
		return (base2 == opc_lstore
			|| base2 == opc_dstore);
	}
	
}
