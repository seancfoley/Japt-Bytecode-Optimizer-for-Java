package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an opc_ret instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_RetIns extends BT_Ins {
	
	public BT_Local target;
	
	BT_RetIns(int opcode, int index, int localNr) {
		this(opcode, index, localNr, false);
	}
	BT_RetIns(int opcode, int index, int localNr, boolean wide) {
		super(opcode, index);
		synchronized(BT_LocalIns.staticLocals) {
			target = BT_LocalIns.staticLocals.elementAt(localNr);
		}
	}
	public Object clone() {
		return new BT_RetIns(opcode, -1, target.localNr);
	}
	
	public void link(BT_CodeAttribute code) {
		target = code.getLocals().elementAt(target.localNr);
	}
	
	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		target = code.getLocals().elementAt(target.localNr);
	}

	public int size() {
		return isWide() ? 4 : 2;
	}
	
	public int maxSize() {
		return size();
	}
	
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		if (isWide()) {
			dos.writeByte(opc_wide);
			dos.writeByte(opc_ret);
			dos.writeShort(target.localNr);
		} else {
			dos.writeByte(opc_ret);
			dos.writeByte(target.localNr);
		}
	}

	private boolean isWide() {
		return BT_Misc.overflowsUnsignedByte(target.localNr);
	}
	
	public void incrementLocalsAccessWith(
		int inc,
		int start,
		BT_LocalVector locals) {
		if (target.localNr < start)
			return;
		target = locals.elementAt(target.localNr + inc);
	}
	
	public String toString() {
		return super.toString() + " " + target.localNr;
	}
	public String toAssemblerString(BT_CodeAttribute code) {
		return super.toAssemblerString(code) + " " + target.localNr;
	}
	
	public boolean isLocalReadIns() {
		return true;
	}
	
	public boolean isRetIns() {
		return true;
	}
}
