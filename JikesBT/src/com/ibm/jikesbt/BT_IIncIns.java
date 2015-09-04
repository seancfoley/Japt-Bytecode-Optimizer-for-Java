package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an opc_iinc instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_IIncIns extends BT_Ins {

	/**
	 The local being manipulated.
	**/
	public BT_Local target;

	/**
	 The increment.
	**/
	public short constant;

	BT_IIncIns(
		int opcode,
		int index,
		int localNr,
		short constant) {
		this(opcode, index, localNr, constant, false);
	}

	BT_IIncIns(
		int opcode,
		int index,
		int localNr,
		short constant,
		boolean wide) {
		super(opcode, index);
		this.constant = constant;
		synchronized(BT_LocalIns.staticLocals) {
			target = BT_LocalIns.staticLocals.elementAt(localNr);
		}
	}

	public Object clone() {
		BT_IIncIns i = new BT_IIncIns(opcode, -1, target.localNr, constant);
		return i;
	}

	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		setTarget(code.getLocals(), target.localNr);
	}

	private boolean isWide() {
		return BT_Misc.overflowsUnsignedByte(target.localNr) || BT_Misc.overflowsSignedByte(constant);
	}
	public int size() {
		return isWide() ? 6 : 3;
	}
	
	public int maxSize() {
		return size();
	}
	
	public void link(BT_CodeAttribute code) {
		setTarget(code.getLocals(), target.localNr);
	}
	
	private void setTarget(BT_LocalVector locals, int localNumber) {
		target = locals.elementAt(localNumber);
	}

	/**
	 @param  inc  The increment.
	**/
	public void incrementLocalsAccessWith(
		int inc,
		int start,
		BT_LocalVector locals) {
		if (target.localNr < start)
			return;
		setTarget(locals, target.localNr + inc);
	}
	
	
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		if (isWide()) {
			dos.writeByte(opc_wide);
			dos.writeByte(opcode);
			dos.writeShort(target.localNr);
			dos.writeShort(constant);
		} else {
			dos.writeByte(opcode);
			dos.writeByte(target.localNr);
			dos.writeByte(constant);
		}
	}
	public String toString() {
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ ' '
			+ target.localNr
			+ ' '
			+ constant;
	}
	public String toAssemblerString(BT_CodeAttribute code) {
		String s = BT_Misc.opcodeName[opcode]
			+ ' '
			+ target.localNr
			+ ' '
			+ constant;
		if (constant < 0)
			s += Messages.getString("JikesBT._//_subtract_{0}_from_{1}_7", new Object[] {Integer.toString((-constant)), code.getLocalName(this, target.localNr)});
		else
			s += Messages.getString("JikesBT._//_add_{0}_to_{1}_9", new Object[] {Integer.toString(constant), code.getLocalName(this, target.localNr)});
		return s;
	}
	
	public boolean isLocalReadIns() {
		return true;
	}
	
	public boolean isLocalWriteIns() {
		return true;
	}
}
