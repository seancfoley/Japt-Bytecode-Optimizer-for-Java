package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an instruction that refers to a class -- see its subclasses.
 * @author IBM
**/
public abstract class BT_ClassRefIns extends BT_Ins {

	/**
	 The class being manipulated.
	 See {@link BT_ClassRefIns#getClassTarget}.
	 
	 This is always the class being referenced or the class being both referenced and created.
	 
	 For checkcast/instanceof, new, multianewarray target this is also the class referred to in the bytecodes.
	 
	 For newarray and anewarray the element class of target is the class referred to in bytecodes.
	**/
	public BT_Class target;
	
	BT_ClassRefIns(int opcode) {
		super(opcode, -1);
		if (CHECK_USER
			&& opcode != opc_checkcast
			&& opcode != opc_new
			&& opcode != opc_anewarray
			&& opcode != opc_newarray
			&& opcode != opc_instanceof
			&& opcode != opc_multianewarray)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
	}
	
	// Checks the opcode.
	BT_ClassRefIns(int opcode, BT_Class target) {
		super(opcode, -1);
		if (CHECK_USER
			&& opcode != opc_checkcast
			&& opcode != opc_new
			&& opcode != opc_newarray
			&& opcode != opc_anewarray
			&& opcode != opc_instanceof
			&& opcode != opc_multianewarray)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
		this.target = target;
	}

	BT_ClassRefIns(int opcode, int index) {
		super(opcode, index);
	}
	/**
	 @param index  The byte offset of the instruction.
	   -1 mean unknown.
	**/
	BT_ClassRefIns(int opcode, int index, String className, BT_Repository repo) {
		super(opcode, index);
		if (CHECK_USER
			&& opcode != opc_checkcast
			&& opcode != opc_new
			&& opcode != opc_newarray
			&& opcode != opc_anewarray
			&& opcode != opc_instanceof
			&& opcode != opc_multianewarray)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
		target = repo.forName(className);
	}

	/**
	 * refers to the referenced type or the allocated type for a new instruction.
	 */
	public BT_Class getClassTarget() {
		return target;
	}
	/**
	 Just gets field {@link BT_ClassRefIns#target}.
	 Same as {@link BT_ClassRefIns#getClassTarget} but using a different naming convention.
	**/
	public BT_Class getTarget() {
		return target;
	}
	
	/**
	 * Same as getClassTarget since the target for a class reference instruction cannot change.
	 * It is method and field reference instructions whose class targets can change while referencing the
	 * same methods or field.
	 */
	public BT_Class getResolvedClassTarget(BT_CodeAttribute code) {
		return target;
	}

	public void resetTarget(BT_Class m, BT_CodeAttribute owner) {
		if (m != target) {
			if(target != null) {
				unlink(owner);
			}
			target = m;
			link(owner);
		}
	}
	
//	public BT_ClassReferenceSite findReferenceSite() {
//		return target.findReferenceSite(this);
//	}

	public void link(BT_CodeAttribute code) {
		BT_ClassReferenceSite site = target.addReferenceSite(this, code);
		if(site != null) {
			code.addReferencedClass(site);
		}
	}
	
	public boolean isClassRefIns() {
		return true;
	}
	
	protected static void linkClassReference(BT_ClassReferenceSite site, BT_Class target) {
		if(site == target.addReferenceSite(site)) {
			site.from.addReferencedClass(site);
		}
	}
	
	public void unlink(BT_CodeAttribute code) {
		unlinkReference(code);
	}
	
	protected void unlinkReference(BT_CodeAttribute code) {
		target.removeClassReferenceSite(this);
		code.removeReferencedClass(this);
	}
	
	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		pool.indexOfClassRef(getWriteTarget());
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		dos.writeShort(pool.indexOfClassRef(getWriteTarget()));
		if (size() != 3)
			throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.Write/size_error_{0}_3", this));
	}
	
	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode]
			+ " "
			+ getWriteTarget().name;
	}
	
	public String toString() {
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " "
			+ getWriteTarget().name;
	}
	
	public String getInstructionTarget() {
		return getTarget().useName();
	}
	
	public BT_Class getWriteTarget() {
		return target;
	}
	
}