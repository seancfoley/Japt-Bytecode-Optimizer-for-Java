package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an opc_fconst_0, opc_fconst_1, opc_fconst_2, opc_ldc, or opc_ldc_w instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_ConstantFloatIns extends BT_ConstantIns {
	public float value;

	BT_ConstantFloatIns(int op, float value) {
		this(op, -1, value);
	}
	BT_ConstantFloatIns(int op, int index, float value) {
		super(op, index);
		this.value = value;
	}

	public void link(BT_CodeAttribute code) {
		setOpcode();
	}
	
	private boolean setOpcode() {
		// We must differentiate between positive zero and negative
		// zero. Simple comparison of the values compares equal, but
		// the "Float.equals" method differentiates.
		//
		if (value == 0.0f) {
			if ((new Float(value)).equals(new Float(0.0f))) {
				opcode = opc_fconst_0;
			}
			else {
				opcode = opc_ldc;
				return true;
			}
		} else if (value == 1.0f)
			opcode = opc_fconst_1;
		else if (value == 2.0f)
			opcode = opc_fconst_2;
		else {
			opcode = opc_ldc;
			return true;
		}
		return false;
	}

	protected int constantIndex(BT_ConstantPool pool) {
		return pool.indexOfFloat(value);
	}

	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		if(setOpcode()) {
			opcode = BT_Misc.overflowsUnsignedByte(constantIndex(pool)) ? opc_ldc_w : opc_ldc;
		}
	}
	
	public int size() {
		switch (opcode) {
			case opc_fconst_0 :
			case opc_fconst_1 :
			case opc_fconst_2 :
				return 1;
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
		if (opcode != opc_ldc && opcode != opc_ldc_w)
			dos.writeByte(opcode);
		else
			super.write(dos, code, pool);
	}
	public String toString() {
		if (opcode != opc_ldc && opcode != opc_ldc_w) {
			return getPrefix()
				+ BT_Misc.opcodeName[opcode];
		}
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " (float) "
			+ value;
	}
	public String toAssemblerString(BT_CodeAttribute code) {
		if (opcode != opc_ldc && opcode != opc_ldc_w) {
			return BT_Misc.opcodeName[opcode];
		}
		return BT_Misc.opcodeName[opcode]
			+ " (float) "
			+ value;
	}

	String appendValueTo(String other) {
		return other + value;
	}

	public Object clone() {
		return new BT_ConstantFloatIns(opcode, value);
	}
}
