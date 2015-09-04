package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */



import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;

/**
 Represents an jump instruction -- see its subclass.
 * @author IBM
**/
public abstract class BT_JumpIns extends BT_Ins {
	BT_JumpTarget target;
	
	BT_JumpIns(int opcode, int index, BT_JumpTarget target) {
		super(opcode, index);
		this.target = target;
	}
	
	public void changeReferencesFromTo(BT_BasicBlockMarkerIns oldIns, BT_BasicBlockMarkerIns newIns) {
		if (target == oldIns) {
			target = newIns;
		}
	}

	public void link(BT_CodeAttribute code) throws BT_InstructionReferenceException {
		target = target.getJumpTarget(code, this);
	}
	
	public String toString() {
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " "
			+ (((target instanceof BT_Ins) ? target.getInstructionByteIndex() : 0) + "[" + target + "]");
	}
	
	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode] + " " + target.getLabel();
	}
	
	public BT_BasicBlockMarkerIns[] getAllReferences() {
		return new BT_BasicBlockMarkerIns[] {getTarget()};
	}
	
	/**
	 * @throw IllegalStateException if the target is not an instruction
	 */
	public BT_BasicBlockMarkerIns getTarget() {
		if(target instanceof BT_Ins) {
			return (BT_BasicBlockMarkerIns) target;
		}
		throw new IllegalStateException();
	}
	
	
}
