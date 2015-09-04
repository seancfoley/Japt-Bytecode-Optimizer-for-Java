package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */
 
/**
 * Instances of this class are stored in each {BT_Field} so that {BT_Field#accessors} 
 * will keep a list of each location where the field is accessed from. The
 * vector containing the accessors is created when methods are read from a given class
 * and allows for quick access to all the instructions that access a field.
 * <p>
 * Sample code:
 * <pre>
 *    BT_Field field = ...	
 *    BT_AccessorVector whoUsesSystemOut = field.accessors;
 *    for (int n=0; n<whoUsesSystemOut.size(); n++) {
 *		BT_Accessor user = whoUsesSystemOut.elementAt(n);
 *		System.out.println("   "+user.from+" uses "+field);
 *    }
 * </pre>
 * @author IBM
 */
public class BT_Accessor extends BT_ItemReference {
	
	public final BT_FieldRefIns instruction;

	public BT_Accessor(BT_CodeAttribute from, BT_FieldRefIns instruction) {
		super(from);
		if(instruction == null) {
			throw new NullPointerException();
		}
		this.instruction = instruction;
	}
	
	public BT_Ins getInstruction() {
		return instruction;
	}
	
	public boolean isFieldRead() {
		return instruction.isFieldReadIns();
	}
	
	public boolean isStaticAccess() {
		switch(instruction.opcode) {
			case BT_Opcodes.opc_getstatic:
			case BT_Opcodes.opc_putstatic:
				return true;

			case BT_Opcodes.opc_getfield:
			case BT_Opcodes.opc_putfield:
				return false;
			default:
				throw new RuntimeException();
		}
	}
	
	public BT_Field getTarget() {
		return instruction.target;
	}
	
	public BT_Class getClassTarget() {
		return instruction.getClassTarget();
	}
		
	/**
	 * 
	 * @return whether this reference refers to a field.
	 */
	public boolean isFieldReference() {
		return true;
	}
}

