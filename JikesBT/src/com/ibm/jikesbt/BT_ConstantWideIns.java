package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an long or float constant manipulating instruction -- see its subclasses.
 * @author IBM
**/
public abstract class BT_ConstantWideIns extends BT_ConstantIns {
	BT_ConstantWideIns(int opcode, int index) {
		super(opcode, index);
	}
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		//
		// remove load followed by pop
		//
		if (ins.size() > n + 1 && ins.elementAt(n + 1).opcode == opc_pop2) {
			return code.removeInstructionsAt(2, n);
		}
		return false;
	}
	
	
	public int getPoppedStackDiff() {
		return 0;
	}
	
	public int getPushedStackDiff() {
		return 2;
	}
	
	public boolean isDoubleWideConstantIns() {
		return true;
	}
}
