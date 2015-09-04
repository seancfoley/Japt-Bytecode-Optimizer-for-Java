package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an instruction that manipulates integers --
 opc_iconst_0, opc_iconst_1, opc_iconst_2,
 opc_iconst_3, opc_iconst_4, opc_iconst_5,
 opc_iconst_m1,
 opc_ldc, opc_ldc_w,
 opc_bipush, or opc_sipush.

 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_ConstantIntegerIns extends BT_ConstantIns {
	public int value;

	BT_ConstantIntegerIns(int op, int value) {
		this(op, -1, value);
	}
	BT_ConstantIntegerIns(int op, int index, int value) {
		super(op, index);
		this.value = value;
	}

	public void link(BT_CodeAttribute code) {
		setOpcode();
	}

	/**
	 * @return whether further constant pool resolution is required
	 */
	private boolean setOpcode() {
		if (value >= -1 && value <= 5) {
			opcode = (byte) (opc_iconst_0 + value);
		} else if (!BT_Misc.overflowsSignedByte(value)) {
			opcode = opc_bipush;
		} else if (!BT_Misc.overflowsSignedShort(value)) {
			opcode = opc_sipush;
		} else {
			opcode = opc_ldc;
			return true;
		}
		return false;
	}

	protected int constantIndex(BT_ConstantPool pool) {
		return pool.indexOfInteger(value);
	}

	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		if(setOpcode()) {
			opcode = BT_Misc.overflowsUnsignedByte(constantIndex(pool)) ? opc_ldc_w : opc_ldc;
		}
	}
	public int getIntValue() {
		return value;
	}
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		setOpcode();
	    BT_InsVector ins = code.getInstructions();
		BT_Ins next = (ins.size() > n + 1) ? ins.elementAt(n + 1) : null;

		//
		// replace two identical loads with one load and a dup
		//
		if (next != null
			&& next instanceof BT_ConstantIntegerIns
			&& value == next.getIntValue()) {
			return code.replaceInstructionsAtWith(1, n + 1, make(opc_dup));
		}

		//
		// load a constant + negating is the same as loading the negative
		//
		if (next != null && next.opcode == opc_ineg) {
			return code.replaceInstructionsAtWith(
				2,
				n,
				make(opc_ldc, -getIntValue()));
		}

		//
		// constant folding, compute expressions at analysis time
		// special case: constant value is 0
		//
		if (next != null && value == 0) {
			switch (next.opcode) {
				case opc_imul :
				case opc_iand :
					//
					// X * 0 = 0
					//
					return code.replaceInstructionsAtWith(
						2,
						n,
						make(opc_pop),
						make(opc_ldc, 0));
				case opc_idiv :
				case opc_irem :
					//
					// X / 0 = ???  division by zero!
					//
					return false;
				case opc_iadd :
				case opc_isub :
				case opc_ishl :
				case opc_ishr :
				case opc_iushr :
				case opc_ior :
					//
					// X + - << >> >>> ^ 0 = X
					//
					return code.removeInstructionsAt(2, n);
			}
		}

		//
		// constant folding, compute expressions at analysis time
		// special case: constant value is 1
		//
		if (next != null && value == 1) {
			switch (next.opcode) {
				case opc_irem :
					//
					// X % 1 = 0
					//
					return code.replaceInstructionsAtWith(
						2,
						n,
						make(opc_pop),
						make(opc_ldc, 0));
				case opc_imul :
				case opc_idiv :
					//
					// X / 1 = X
					//
					return code.removeInstructionsAt(2, n);
			}
		}

		//
		// strength reduction, compute expressions at analysis time
		// special case: constant value is 2
		//
		if (next != null && value == 2) {
			switch (next.opcode) {
				case opc_imul :
					return code.replaceInstructionsAtWith(
						2,
						n,
						make(opc_iconst_1),
						make(opc_ishl));
			}
		}

		//
		// strength reduction, compute expressions at analysis time
		// special case: constant value is 4
		//
		if (next != null && value == 4) {
			switch (next.opcode) {
				case opc_imul :
					return code.replaceInstructionsAtWith(
						2,
						n,
						make(opc_iconst_2),
						make(opc_ishl));
			}
		}

		//
		// strength reduction, compute expressions at analysis time
		// special case: constant value is 8
		//
		if (next != null && value == 8) {
			switch (next.opcode) {
				case opc_imul :
					return code.replaceInstructionsAtWith(
						2,
						n,
						make(opc_iconst_3),
						make(opc_ishl));
			}
		}

		//
		// constant folding: expression evaluation
		//
		if (ins.size() > n + 2 && next instanceof BT_ConstantIntegerIns) {
			switch (ins.elementAt(n + 2).opcode) {
				case opc_iadd :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value + next.getIntValue()));
				case opc_isub :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value - next.getIntValue()));
				case opc_imul :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value * next.getIntValue()));
				case opc_idiv :
					int nextValue = next.getIntValue();
					if (nextValue == 0) {
						//division by zero
						return false;
					}
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value / nextValue));
				case opc_irem :
					nextValue = next.getIntValue();
					if (nextValue == 0) {
						//division by zero
						return false;
					}
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value % nextValue));
				case opc_ishl :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value << next.getIntValue()));
				case opc_ishr :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value >> next.getIntValue()));
				case opc_iushr :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value >>> next.getIntValue()));
				case opc_iand :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value & next.getIntValue()));
				case opc_ior :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value | next.getIntValue()));
				case opc_ixor :
					return code.replaceInstructionsAtWith(
						3,
						n,
						make(opc_ldc, value ^ next.getIntValue()));
			}
		}

		//
		// push, load, store, pop  ->  load, store
		//
		if (ins.size() > n + 3
			&& next instanceof BT_LoadLocalIns
			&& ins.elementAt(n + 2) instanceof BT_StoreLocalIns
			&& ins.elementAt(n + 3).opcode == opc_pop) {
			code.removeInstructionsAt(1, n + 3);
			return code.removeInstructionsAt(1, n);
		}

		// "push_const, if[eq|ne]" -> "goto" | "nop"
		// customized for if (const_bool) { } java statements,
		// that are results of code manipulation
		if (next != null && next instanceof BT_JumpIns) {
			if (next.opcode == opc_ifeq
				&& value == 0
				|| next.opcode == opc_ifne
				&& value != 0) {
				return code.replaceInstructionsAtWith(
					2,
					n,
					new BT_JumpOffsetIns(
						BT_Ins.opc_goto,
						-1,
						((BT_JumpIns) next).target));
			} else if (
				next.opcode == opc_ifeq
					&& value != 0
					|| next.opcode == opc_ifne
					&& value == 0) {
				return code.removeInstructionsAt(2, n);
			}
		}
		return super.optimize(code, n, strict);
	}
	public String toString() {
		if (value >= -1 && value <= 5) //iconst_x
			return getPrefix() + BT_Misc.opcodeName[opc_iconst_0 + value];
		else if (!BT_Misc.overflowsSignedByte(value)) { //bipush
			return getPrefix() + BT_Misc.opcodeName[opc_bipush] + " " + value;
		} else if (!BT_Misc.overflowsSignedShort(value)) { //sipush
			return getPrefix() + BT_Misc.opcodeName[opc_sipush] + " " + value;
		} else  {//ldc
			return getPrefix() + BT_Misc.opcodeName[opcode] + " (int) " + value;
		}
	}

	public String toAssemblerString(BT_CodeAttribute code) {
		if (value >= -1 && value <= 5) //iconst_x
			return BT_Misc.opcodeName[opc_iconst_0 + value];
		else if (!BT_Misc.overflowsSignedByte(value)) { //bipush
			return BT_Misc.opcodeName[opc_bipush] + " " + value;
		} else if (!BT_Misc.overflowsSignedShort(value)) { //sipush
			return BT_Misc.opcodeName[opc_sipush] + " " + value;
		} else { //ldc
			return BT_Misc.opcodeName[opcode] + " (int) " + value;
		}
	}

	public int size() {
		switch (opcode) {
			case opc_iconst_m1 :
			case opc_iconst_0 :
			case opc_iconst_1 :
			case opc_iconst_2 :
			case opc_iconst_3 :
			case opc_iconst_4 :
			case opc_iconst_5 :
				return 1;
			case opc_bipush :
				return 2;
			case opc_sipush :
				return 3;
			case opc_ldc :
				return 2;
			case opc_ldc_w :
				return 3;
		}
		throw new IllegalStateException(Messages.getString("JikesBT.unexpected_{0}_1", BT_Misc.opcodeName[opcode]));
	}

	public int maxSize() {
		if(setOpcode()) {
			return 3;
		}
		return size();
	}
	
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		// Use setOpcode() here?
		if (value >= -1 && value <= 5)
			dos.writeByte(opc_iconst_0 + value);
		else if (!BT_Misc.overflowsSignedByte(value)) {
			dos.writeByte(opc_bipush);
			dos.writeByte(value);
		} else if (!BT_Misc.overflowsSignedShort(value)) {
			dos.writeByte(opc_sipush);
			dos.writeShort(value);
		} else
			super.write(dos, code, pool);
	}

	String appendValueTo(String other) {
		return other + value;
	}
	public Object clone() {
		return new BT_ConstantIntegerIns(opcode, value);
	}
}
