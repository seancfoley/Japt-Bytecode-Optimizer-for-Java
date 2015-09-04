package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_BytecodeException.BT_InvalidInstructionException;


/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_anewarray instruction that allocates an array of null
 references to objects.
 Also see {@link BT_MultiANewArrayIns}, {@link BT_NewIns}, and {@link
 BT_NewArrayIns}.
 * @author IBM
**/
public final class BT_ANewArrayIns extends BT_NewIns {

	/**
	 * The bytecode for a 'anewarray' instruction actually refers to the element class
	 * and not the array class itself.  This is in contrast to the 'multianewarray' 
	 * instruction and the 'new' instruction, but similar to 'newarray'.
	 * 
	 * The target field of a new instruction always refers to the created class.
	 * So both constructors here translate from the element class referred to in the 
	 * bytecode and the array class that is actually created.
	 */
	
	BT_ANewArrayIns(BT_Class elementClass) {
		super(opc_anewarray, elementClass.getArrayClass());
		if (CHECK_USER && target.isBasicTypeClass)
			expect(Messages.getString("JikesBT.Target_should_not_be_primitive__2") + target);
	}

	/**
	 @param index  The byte offset of the instruction.
	   -1 mean unknown.
	**/
	BT_ANewArrayIns(int opcode, int index, String elementClassName, BT_Repository repo, BT_CodeAttribute code)
			throws BT_InvalidInstructionException {
		super(opcode, index, elementClassName + "[]", repo, code);
		if(target.getElementClass().isPrimitive()) {
			throw new BT_InvalidInstructionException(code, opcode, index);
		}
		if (CHECK_USER && opcode != opc_anewarray)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
	}

	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		return false;
	}
	
	public Object clone() {
		return new BT_ANewArrayIns(target.getElementClass());
	}
	
	public void link(BT_CodeAttribute code) {
		//note that the anewarray instruction references the element class in the constant pool,
		//unlike the new or multianewarray instruction,
		//which is why we use the targetClass object and not the targetObject here
		BT_CreationSite site = target.addCreationSite(this, code);
		if(site != null) {
			code.addCreatedClass(site);
			super.linkClassReference(site, target);
		}
	}
	
	public void unlink(BT_CodeAttribute code) {
		super.unlinkReference(code);
		target.removeCreationSite(this);
		code.removeCreatedClass(this);
	}
	
	public boolean isNewArrayIns() {
		return true;
	}
	
	public BT_Class getWriteTarget() {
		return target.getElementClass();
	}
}
