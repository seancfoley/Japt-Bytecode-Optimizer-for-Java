/*
 * Created on Oct 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_CodeException.BT_InconsistentStackDepthException;
import com.ibm.jikesbt.BT_CodeException.BT_StackUnderflowException;



/**
 * @author sfoley
 *
 * This code vistor object simulates the change in the locals and the operand stack
 * as the code in a method is executed.
 * <p>
 */
public class BT_StackDepthVisitor extends BT_CodeVisitor {
	protected BT_InsVector vec;
	private int maxDepth = 0;
	
	//	the stack depth at index i is the depth of the stack before executing the instruction at index i
	protected int stackDepth[];
	
	public BT_StackDepthVisitor() {}
	
	public int getMaxDepth() {
		return maxDepth;
	}
	
	protected void setUp() {
		vec = code.getInstructions();
		stackDepth = new int[vec.size()];
	}
	
	protected void tearDown() {
		vec = null;
		stackDepth = null;
	}
	
	/*
	 * return true if the method has updated the locals and the stack
	 * as required for this instruction, false otherwise
	 */
	protected void additionalVisit(
			BT_Ins instruction,
			int iin,
			BT_Ins previousInstruction,
			int prev_iin,
			BT_ExceptionTableEntry handler) throws BT_InconsistentStackDepthException {
		if(stackDepth[iin] != getStackDepth(prev_iin, handler)) {
			throw new BT_InconsistentStackDepthException(code, instruction, iin);
		}
	}
	
	private int getStackDepth(int prev_iin, BT_ExceptionTableEntry handler) {
		if(prev_iin == ENTRY_POINT) {
			return 0;
		}
		if(handler != null) {
			return 1;
		}
		return stackDepth[prev_iin] + vec.elementAt(prev_iin).getStackDiff();
	}
	
	protected boolean visit(
			BT_Ins instruction,
			int iin,
			BT_Ins previousInstruction,
			int prev_iin,
			BT_ExceptionTableEntry handler) throws BT_StackUnderflowException {
		int nextDepth = getStackDepth(prev_iin, handler);
		if (nextDepth < 0) {
			throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
		} 
		stackDepth[iin] = nextDepth;
		maxDepth = Math.max(maxDepth, nextDepth);
		return true;
	}
}
