package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an opc_dconst_0, opc_dconst_1, or opc_ldc2_w instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_ConstantDoubleIns extends BT_ConstantWideIns {
	public double value;

	BT_ConstantDoubleIns(int op, double value) {
		this(op, -1, value);
	}

	/**
	 @param index  The byte offset of the instruction.
	   -1 mean unknown.
	**/
	BT_ConstantDoubleIns(int op, int index, double value) {
		super(op, index);
		this.value = value;
		setOpcode();
	}

	public void link(BT_CodeAttribute code) {
		setOpcode();
	}

	private void setOpcode() {
		// We must differentiate between positive zero and negative
		// zero. Simple comparison of the values compares equal, but
		// the "Double.equals" method differentiates.
		//
		if (value == 0.0) {
			if ((new Double(value)).equals(new Double(0.0)))
				opcode = opc_dconst_0;
			else
				opcode = opc_ldc2_w;
		} else if (value == 1.0)
			opcode = opc_dconst_1;
		else
			opcode = opc_ldc2_w;
	}

	public int size() {
		if (opcode == opc_dconst_0 || opcode == opc_dconst_1)
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
		BT_Ins next = ins.elementAt(n + 1);
		//
		// replace two identical instructions with a dup2
		//
		if (ins.size() > n + 1
			&& next instanceof BT_ConstantDoubleIns
			&& ((BT_ConstantDoubleIns) next).value == value) {
			return code.replaceInstructionsAtWith(
				1,
				n + 1,
				BT_Ins.make(opc_dup2));
		}
		return super.optimize(code, n, strict);
	}

	protected int constantIndex(BT_ConstantPool pool) {
		return pool.indexOfDouble(value);
	}

	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		setOpcode();
		if (opcode == opc_ldc2_w)
			constantIndex(pool);
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		if (opcode == opc_dconst_0 || opcode == opc_dconst_1)
			dos.writeByte(opcode);
		else {
			dos.writeByte(opc_ldc2_w);
			dos.writeShort(constantIndex(pool));
			if (size() != 3)
				throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.Write/size_error_{0}_3", this));
		}
	}

	public String toString() {
		if (opcode == opc_dconst_0 || opcode == opc_dconst_1) {
			return getPrefix()
				+ BT_Misc.opcodeName[opcode];
		}
		
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " (double) "
			+ value;
	}

	public String toAssemblerString(BT_CodeAttribute code) {
		if (opcode == opc_dconst_0 || opcode == opc_dconst_1) {
			return BT_Misc.opcodeName[opcode];
		}
		
		return BT_Misc.opcodeName[opcode]
			+ " (double) "
			+ value;
	}

	String appendValueTo(String other) {
		return other + value;
	}

	public Object clone() {
		return new BT_ConstantDoubleIns(opcode, value);
	}
}