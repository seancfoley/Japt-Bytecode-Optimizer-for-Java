package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Represents an opc_invokeinterface instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_InvokeInterfaceIns extends BT_MethodRefIns {

	BT_InvokeInterfaceIns(BT_Method target, BT_Class targetClass) {
		super(opc_invokeinterface, -1, target, targetClass);
		if (CHECK_USER && !targetClass.isInterface())
			expect(
				Messages.getString("JikesBT.InvokeInterface_target_should_be_in_an_interface__{0}_1", target));
	}
	
	BT_InvokeInterfaceIns(BT_Method target) {
		this(target, target.getDeclaringClass());
	}

	BT_InvokeInterfaceIns(
		int opcode,
		int index,
		int poolIndex,
		short count,
		short reserved,
		BT_Method inM,
		LoadLocation loadedFrom)
			throws BT_DescriptorException, BT_ConstantPoolException, BT_ClassFileException {
				super(opcode, index, poolIndex, inM, loadedFrom);
		if(target.getSignature().getArgsSize() + 1 != count || reserved != 0)
			throw new BT_ClassFileException(
				Messages.getString("JikesBT.invalid_invokeinterface_instruction_in_method_{0}_2")
					+ inM.fullName());
	}

	int getCPIndex(BT_CodeAttribute code, BT_ConstantPool pool) {
		return pool.indexOfInterfaceMethodRef(getResolvedClassTarget(code), target);
	}
	
	public Object clone() {
		return new BT_InvokeInterfaceIns(target, targetClass);
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		dos.writeShort(getCPIndex(code, pool));
		dos.writeByte(target.getSignature().getArgsSize() + 1);
		dos.writeByte(opc_nop);
		if (size() != 5)
			throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.Write/size_error_{0}_3", this));
	}
	public String toString() {
		return getPrefix()
			+ BT_Misc.opcodeName[opcode & 0xff]
			+ " "
			+ target.getSignature().returnType
			+ " "
			+ targetClass.getName() + '.' + target.qualifiedName()
			//+ target.useName()
			+ " "
			+ Messages.getString("JikesBT._nargs___6")
			+ (target.getSignature().getArgsSize() + 1);
	}
}
