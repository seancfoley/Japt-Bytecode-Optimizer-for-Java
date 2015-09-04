/*
 * Created on Oct 17, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public class BT_DualCodeVisitor extends BT_CodeVisitor {
	BT_CodeVisitor one;
	BT_CodeVisitor two;

	public BT_DualCodeVisitor(BT_CodeVisitor one, BT_CodeVisitor two) {
		this.one = one;
		this.two = two;
	}
	
	void initialize(BT_CodeAttribute codeAttribute) {
		one.code = codeAttribute;
		two.code = codeAttribute;
		super.initialize(codeAttribute);
		
	}
	
	protected void setUp() {
		one.setUp();
		two.setUp();
	}
	
	protected boolean visit(
			BT_Ins instruction, 
			int iin, 
			BT_Ins previousInstruction, 
			int prev_iin, 
			BT_ExceptionTableEntry handler) 
			throws BT_CodeException {
		boolean ret;
		if(one.exited()) {
			if(two.exited()) {
				exit();
				return false;
			} else {
				return two.visit(instruction, iin, previousInstruction, prev_iin, handler);
			}
		} 
		ret = one.visit(instruction, iin, previousInstruction, prev_iin, handler);
		if(!two.exited()) {
			ret |= two.visit(instruction, iin, previousInstruction, prev_iin, handler);
		}
		return ret;
	}
	
	protected void additionalVisit(
			BT_Ins instruction, 
			int iin, 
			BT_Ins previousInstruction, 
			int prev_iin, 
			BT_ExceptionTableEntry handler) throws BT_CodeException  {
		if(one.exited()) {
			if(two.exited()) {
				exit();
				return;
			} else {
				two.additionalVisit(instruction, iin, previousInstruction, prev_iin, handler);
				return;
			}
		} 
		one.additionalVisit(instruction, iin, previousInstruction, prev_iin, handler);
		if(!two.exited()) {
			two.additionalVisit(instruction, iin, previousInstruction, prev_iin, handler);
		}
	}
	
	protected void tearDown() {
		one.tearDown();
		two.tearDown();
	}
}
