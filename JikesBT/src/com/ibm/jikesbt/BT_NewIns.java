package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_BytecodeException.BT_InvalidInstructionException;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_new instruction that creates an object, or, by
 subclass {@link BT_ANewArrayIns} an "anewarray" instruction or 
 {@link BT_MultiANewArrayIns} a "multianewarray instruction.
 Also see {@link BT_NewArrayIns}.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public class BT_NewIns extends BT_ClassRefIns {
	
	BT_NewIns(BT_Class target) {
		super(opc_new, target);
	}
	
	BT_NewIns(int opcode) {
		super(opcode);
		if (CHECK_USER && opcode != opc_newarray)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
	}
	
	BT_NewIns(int opcode, BT_Class target) {
		super(opcode, target);
		if (CHECK_USER && opcode != opc_anewarray && opcode != opc_new)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
	}
	
	BT_NewIns(int opcode, int index) {
		super(opcode, index);
		if (CHECK_USER && opcode != opc_newarray)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
	}
	
	BT_NewIns(int opcode, int index, String className, BT_Repository repo, BT_CodeAttribute code)
		throws BT_InvalidInstructionException {
		super(opcode, index, className, repo);
		if (CHECK_USER && opcode != opc_anewarray && opcode != opc_new)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
		if (opcode == opc_new && target.isArray())
			throw new BT_InvalidInstructionException(code, opcode, index, Messages.getString("JikesBT.new_instruction_cannot_create_arrays_3"));
	}

	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		return false;
	}

	public void link(BT_CodeAttribute code) {
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
	
	
	public boolean isNewIns() {
		return true;
	}
	
	public Object clone() {
		return new BT_NewIns(target);
	}
}
