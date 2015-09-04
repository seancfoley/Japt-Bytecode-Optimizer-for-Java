package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;

/**
 Represents a pseudo-instruction that marks the beginning and/or end of
 a basic block, that is determined by exception blocks and jump
 instructions. Each instruction in an instruction vector that is a jump
 (e.g., goto, lookupswitch or exception handler range) will directly
 point to an instance of BT_BasicBlockMarkerIns.
 <p>
 This means that only this instruction can be reached by an instruction 
 other than preceding instruction.  It is the only instruction that can
 be reached from more than one place.
 <p>
 The existence of this class is debatable to some people, but there is
 great benefits to the use of this pseudo instruction. 
 For instance, it allows peephole optimization
 of instructions without being worried about replacing non-block marker
 instructions.
 <p>
 Instances of this class are never written to a .class file, of course.
 Note that {size} returns 0, and {write} does not do anything.
 <p>
 Created by {@link BT_InsVector#findBasicBlock}
 * @author IBM
**/
public class BT_BasicBlockMarkerIns extends BT_Ins implements BT_JumpTarget {

    private String label;

	public BT_BasicBlockMarkerIns() {
		super(opc_xxxunusedxxx, -1);
	}

	public BT_BasicBlockMarkerIns(String s) {
        super(opc_xxxunusedxxx, -1);
	    setLabel(s);
    }
	
	public void setLabel(String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		BT_Ins next = (ins.size() > n + 1) ? ins.elementAt(n + 1) : null;
		//
		// replace two basic block markers with one...
		//
		if (next != null && next.isBlockMarker()) {
			//don't worry, the following call will ensure anything pointing to the
			//removed block marker will now point to the remaining block marker
			return code.removeInstructionAt(n);
		}
		return false;
	}

	public int size() {
		return 0;
	}

	public int getPoppedStackDiff() {
		return 0;
	}
	
	public int getPushedStackDiff() {
		return 0;
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool) {}

	public Object clone() {
		return new BT_BasicBlockMarkerIns();
	}

	public String toString() {
		if(label != null) {
			return label;
		}
		return getPrefix() + Messages.getString("JikesBT.block__6");
	}

	public String toAssemblerString(BT_CodeAttribute code) {
		if(label != null) {
			return label + ":";
		}
		return null;
	}
	
	public int hashCode() {
		if(label != null) {
			return label.hashCode();
		}
		return super.hashCode();
	}
	
	public boolean equals(Object o) {
		if(o instanceof BT_BasicBlockMarkerIns) { 
			if(label != null) {
				return label.equals(((BT_BasicBlockMarkerIns) o).label);
			}
			return o == this;
		}
		return false;		
	}
	
	public boolean isBlockMarker() {
		return true;
	}

	/*
	 *  methods for BT_JumpTarget implementation
	 */
	
	public BT_BasicBlockMarkerIns getJumpTarget(BT_CodeAttribute code, BT_Ins fromInstruction) {
		return this;
	}
	
	public int getByteIndex() {
		return byteIndex;
	}

	public boolean isAbsoluteByteIndex() {
		return true;	
	}

	public int getInstructionByteIndex() {
		return byteIndex;
	}

}
