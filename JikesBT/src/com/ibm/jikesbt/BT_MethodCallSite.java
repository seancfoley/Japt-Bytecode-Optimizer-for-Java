package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents the relation between calling instruction and the method
 called.

 See {@link BT_CreationSite}.
 * @author IBM
**/
public class BT_MethodCallSite extends BT_ItemReference {
	
	/**
	 The calling instruction.
	**/
	public final BT_MethodRefIns instruction;
	
	public BT_MethodCallSite(BT_CodeAttribute from, BT_MethodRefIns instruction) {
		super(from);
		if(instruction == null) {
			throw new NullPointerException();
		}
		this.instruction = instruction;
	}
	
	public BT_Ins getInstruction() {
		return instruction;
	}
	
	public BT_Method getFrom() {
		return from.getMethod();
	}

	public BT_Method getTarget() {
		return instruction.target;
	}
	
	public BT_Class getClassTarget() {
		return instruction.getClassTarget();
	}
	
	public boolean isSuperInvocation() {
		if (instruction.isInvokeSpecialIns()) {
			BT_Method target = getTarget();
			BT_Method owningMethod = getFrom();
			if(owningMethod != null) {
				return !target.isConstructor() 
							&& ((!target.isPrivate() || target.isStub()) 
								&& target.getDeclaringClass().isAncestorOf(owningMethod.getDeclaringClass()));
			}
		}
		return false;
	}
	
	/**
	 * @return whether this reference refers to a method.
	 */
	public boolean isMethodReference() {
		return true;
	}
	
}
