package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an opc_ldc2_w, opc_lconst_0, or opc_lconst_1 instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_ConstantLongIns extends BT_ConstantWideIns {
	public long value;

	BT_ConstantLongIns(int op, long value) {
		this(op, -1, value);
	}
	BT_ConstantLongIns(int op, int index, long value) {
		super(op, index);
		this.value = value;
	}

	public void link(BT_CodeAttribute code) {
		setOpcode();
	}

	/* @return whether a constant pool resolution is required */
	private boolean setOpcode() {
		if (value == 0L)
			opcode = opc_lconst_0;
		else if (value == 1L)
			opcode = opc_lconst_1;
		else {
			opcode = opc_ldc2_w;
			return true;
		}
		return false;
	}

	public int size() {
		if (opcode == opc_lconst_0 || opcode == opc_lconst_1)
			return 1;
		else
			return 3;
	}
	
	public int maxSize() {
		setOpcode();
		return size();
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		BT_Ins next = (ins.size() > n + 1) ? ins.elementAt(n + 1) : null;

		//
		// replace two identical loads with one load and dup2
		//
		if (next != null
			&& next instanceof BT_ConstantLongIns
			&& ((BT_ConstantLongIns) next).value == value) {
			return code.replaceInstructionsAtWith(
				1,
				n + 1,
				BT_Ins.make(opc_dup2));
		}
		return super.optimize(code, n, strict);
	}

	protected int constantIndex(BT_ConstantPool pool) {
		return pool.indexOfLong(value);
	}

	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		if(setOpcode()) {
			constantIndex(pool);
		}
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		if (opcode == opc_lconst_0 || opcode == opc_lconst_1)
			dos.writeByte(opcode);
		else {
			dos.writeByte(opc_ldc2_w);
			dos.writeShort(constantIndex(pool));
			if (size() != 3)
				throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.Write/size_error_{0}_3", this));
		}
	}
	public String toString() {
		if (opcode == opc_lconst_0 || opcode == opc_lconst_1) {
			return getPrefix() + BT_Misc.opcodeName[opcode];
		}
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " (long) "
			+ value;
	}
	public String toAssemblerString(BT_CodeAttribute code) {
		if (opcode == opc_lconst_0 || opcode == opc_lconst_1) {
			return BT_Misc.opcodeName[opcode];
		}
		return BT_Misc.opcodeName[opcode]
			+ " (long) "
			+ value;
	}

	String appendValueTo(String other) {
		return other + value;
	}
	public Object clone() {
		return new BT_ConstantLongIns(opcode, value);
	}
}
