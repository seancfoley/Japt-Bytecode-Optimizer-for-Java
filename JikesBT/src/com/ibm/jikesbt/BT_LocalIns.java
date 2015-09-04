package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an instruction that references a local variable -- see its subclasses.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public abstract class BT_LocalIns extends BT_Ins {
	
	public static class OpcodePair {
		private OpcodePair(int base1, int base2) {
			this.base1 = base1;
			this.base2 = base2;
		}
		
		public final int base1; //one of opc_aload_0, opc_iload_0, opc_lload_0, opc_fload_0, opc_dload_0
		public final int base2; //one of opc_aload, opc_iload, opc_lload, opc_fload, opc_dload
	}
	
	static final OpcodePair aloadPair = new OpcodePair(opc_aload_0, opc_aload);
	static final OpcodePair iloadPair = new OpcodePair(opc_iload_0, opc_iload);
	static final OpcodePair lloadPair = new OpcodePair(opc_lload_0, opc_lload);
	static final OpcodePair floadPair = new OpcodePair(opc_fload_0, opc_fload);
	static final OpcodePair dloadPair = new OpcodePair(opc_dload_0, opc_dload);
	
	static final OpcodePair astorePair = new OpcodePair(opc_astore_0, opc_astore);
	static final OpcodePair istorePair = new OpcodePair(opc_istore_0, opc_istore);
	static final OpcodePair lstorePair = new OpcodePair(opc_lstore_0, opc_lstore);
	static final OpcodePair fstorePair = new OpcodePair(opc_fstore_0, opc_fstore);
	static final OpcodePair dstorePair = new OpcodePair(opc_dstore_0, opc_dstore);
	
	static final BT_LocalVector staticLocals = new BT_LocalVector();
	
	public BT_Local target;
	
	BT_LocalIns(int op, int index, int localNr) {
		super(op, index);
		synchronized(staticLocals) {
			target = staticLocals.elementAt(localNr);
		}
	}
	
	public int getBaseOpcode() {
		return getBase(opcode).base2;
	}
	
	public OpcodePair getBase() {
		return getBase(opcode);
	}
	
	public static int getBaseOpcode(int opcode) {
		return getBase(opcode).base2;
	}
	
	static OpcodePair getBase(int opcode) {
		switch(opcode) {
			case opc_aload:
			case opc_aload_0:
			case opc_aload_1:
			case opc_aload_2:
			case opc_aload_3:
				return aloadPair;
			case opc_iload:
			case opc_iload_0:
			case opc_iload_1:
			case opc_iload_2:
			case opc_iload_3:
				return iloadPair;
			case opc_fload:
			case opc_fload_0:
			case opc_fload_1:
			case opc_fload_2:
			case opc_fload_3:
				return floadPair;
			case opc_lload:
			case opc_lload_0:
			case opc_lload_1:
			case opc_lload_2:
			case opc_lload_3:
				return lloadPair;
			case opc_dload:
			case opc_dload_0:
			case opc_dload_1:
			case opc_dload_2:
			case opc_dload_3:
				return dloadPair;
			case opc_astore:
			case opc_astore_0:
			case opc_astore_1:
			case opc_astore_2:
			case opc_astore_3:
				return astorePair;
			case opc_istore:
			case opc_istore_0:
			case opc_istore_1:
			case opc_istore_2:
			case opc_istore_3:
				return istorePair;
			case opc_fstore:
			case opc_fstore_0:
			case opc_fstore_1:
			case opc_fstore_2:
			case opc_fstore_3:
				return fstorePair;
			case opc_lstore:
			case opc_lstore_0:
			case opc_lstore_1:
			case opc_lstore_2:
			case opc_lstore_3:
				return lstorePair;
			case opc_dstore:
			case opc_dstore_0:
			case opc_dstore_1:
			case opc_dstore_2:
			case opc_dstore_3:
				return dstorePair;
			default:
				throw new RuntimeException("invalid load or store opcode");
		}
	}
	
	public void link(BT_CodeAttribute code) {
		setTarget(code.getLocals(), target.localNr);
	}
	
	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		setTarget(code.getLocals(), target.localNr);
		setOpcode();
	}

	public void incrementLocalsAccessWith(
		int inc,
		int start,
		BT_LocalVector locals) {
		if (target.localNr < start)
			return;
		setTarget(locals, target.localNr + inc);
		setOpcode();
	}

	private void setOpcode() {
		OpcodePair base = getBase();
		opcode = target.localNr <= 3 ? base.base1 + target.localNr : base.base2;
	}
	
	private boolean isWide() {
		return BT_Misc.overflowsUnsignedByte(target.localNr);
	}
	
	private void setTarget(BT_LocalVector locals, int localNumber) {
		target = locals.elementAt(localNumber);

		// If this local occupies two local slots, touch the second
		// to make sure it exists.
		//
		if (is2Slot()) locals.elementAt(localNumber + 1);
	}
	public int size() {
		OpcodePair base = getBase();
		if (opcode != base.base2)
			return 1;
		return isWide() ? 4 : 2;
	}
	
	public int maxSize() {
		setOpcode();
		return size();
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		OpcodePair base = getBase();
		if (opcode != base.base2 /* opcode is not aload, iload, ...., astore, ....*/) {
			dos.writeByte(base.base1 + target.localNr);
		} else {
			if (isWide()) {
				dos.writeByte(opc_wide);
				dos.writeByte(base.base2);
				dos.writeShort(target.localNr);
			} else {
				dos.writeByte(base.base2);
				dos.writeByte(target.localNr);
			}
		}
	}
	public String toString() {
		String s = super.toString();
		OpcodePair base = getBase();
		if (opcode == base.base2)
			s += " " + target.localNr;
		return s;
	}	
	public String toAssemblerString(BT_CodeAttribute code) {
		String s = super.toAssemblerString(code);
		OpcodePair base = getBase();
		if (opcode == base.base2)
			s += " " + target.localNr;
		return s;
	}	
	
	/**
	 * @return True for operations that read/write 2 consequent slots
	 */
	public boolean is2Slot() {
		OpcodePair base = getBase();
		int base2 = base.base2;
		return (base2 == opc_lload
			|| base2 == opc_dload
			|| base2 == opc_lstore
			|| base2 == opc_dstore);
	}
}
