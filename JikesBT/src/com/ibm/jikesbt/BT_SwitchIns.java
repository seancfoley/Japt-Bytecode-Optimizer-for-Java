package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_JumpOffsetIns.OffsetJumpTarget;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents a switch instruction -- see its subclasses.
 * @author IBM
**/
public abstract class BT_SwitchIns extends BT_Ins {
	
	/**
	 To the default clause.
	**/
	BT_JumpTarget def;
	BT_JumpTarget targets[];

	/**
	 Constructs an instruction that needs to be dereferenced.
	**/
	BT_SwitchIns(int opcode, int index, int defIntTarget, int intTargets[]) {
		super(opcode, index);
		this.def = new OffsetJumpTarget(defIntTarget, true);
		this.targets = new BT_JumpTarget[intTargets.length];
		for(int i=0; i<targets.length; i++) {
			targets[i] = new OffsetJumpTarget(intTargets[i], true);
		}
	}
	
	/**
	 Constructs an instruction that does not need to be dereferenced.
	**/
	BT_SwitchIns(int opcode, BT_JumpTarget def, BT_JumpTarget targets[]) {
		super(opcode, -1);
		this.def = def;
		this.targets = targets;
	}
	
	public BT_BasicBlockMarkerIns[] getAllReferences() {
		return getAllTargets();
	}
	
	public BT_BasicBlockMarkerIns[] getAllTargets() {
		try {
			BT_BasicBlockMarkerIns insTargets[] = new BT_BasicBlockMarkerIns[targets.length + 1];
			System.arraycopy(targets, 0, insTargets, 0, targets.length);
			insTargets[targets.length] = (BT_BasicBlockMarkerIns) def;
			return insTargets;
		} catch(ClassCastException e) {
			throw new IllegalStateException();
		}
	}
	
	public void changeReferencesFromTo(BT_BasicBlockMarkerIns oldIns, BT_BasicBlockMarkerIns newIns) {
		if (def == oldIns)
			def = newIns;
		for (int n = 0; n < targets.length; n++)
			if (targets[n] == oldIns)
				targets[n] = newIns;
	}
	
	public void link(BT_CodeAttribute code) throws BT_InstructionReferenceException {
		for (int n = 0; n < targets.length; n++) {
			targets[n] = targets[n].getJumpTarget(code, this);
		}
		def = def.getJumpTarget(code, this);
	}
		
	public boolean isSwitchIns() {
		return true;
	}
}
