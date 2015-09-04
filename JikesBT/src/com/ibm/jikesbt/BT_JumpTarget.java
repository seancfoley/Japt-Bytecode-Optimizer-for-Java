/*
 * Created on Oct 30, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;

interface BT_JumpTarget {

	/**
	 * 
	 * @param code
	 * @param fromInstruction
	 * @return the target instruction for this target
	 * @throws BT_InstructionReferenceException
	 */
	BT_BasicBlockMarkerIns getJumpTarget(BT_CodeAttribute code, BT_Ins fromInstruction) 
		throws BT_InstructionReferenceException;
	
	/**
	 * Get the byte index of this target, whether it be relative from the jump or absolute
	 * from the beginning of the method.
	 */
	int getByteIndex();
	
	/**
	 * Returns whether the value returned by getByteIndex is absolute.
	 */
	boolean isAbsoluteByteIndex();
	
	/**
	 * If this object is an instruction, return the byte index, otherwise throws IllegalStateException
	 */
	int getInstructionByteIndex();
	
	/**
	 * 
	 * @return the label for the instruction when printing to an output stream
	 */
	String getLabel();

}
