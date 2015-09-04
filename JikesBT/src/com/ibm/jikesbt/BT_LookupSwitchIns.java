package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InvalidInstructionException;

/**
 Represents an opc_lookupswitch instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_LookupSwitchIns extends BT_SwitchIns {
	public int values[];
	
	public BT_LookupSwitchIns(
		int opcode,
		BT_BasicBlockMarkerIns def,
		int values[],
		BT_BasicBlockMarkerIns targets[]) {
		this(opcode, (BT_JumpTarget) def, values, (BT_JumpTarget[]) targets);
	}
	
	private BT_LookupSwitchIns(
		int opcode,
		BT_JumpTarget def,
		int values[],
		BT_JumpTarget targets[]) {
		super(opcode, def, targets);
		this.values = values;
	}
	
	BT_LookupSwitchIns(
		int opcode,
		int index,
		int defIntTarget,
		int values[],
		int intTargets[]) {
		super(opcode, index, defIntTarget, intTargets);
		this.values = values;
	}
	
	public static BT_LookupSwitchIns make(
		BT_CodeAttribute code,
		int opcode,
		int offset,
		byte data[],
		BT_Method item) throws BT_InvalidInstructionException {
		int start = ((offset / 4) + 1) * 4;
		if (start + 8 > data.length)
			throw new BT_InvalidInstructionException(code, opcode, offset,
				Messages.getString("JikesBT.Truncated_instruction_at_{0}_of_method_{1}_1",
					new Object[] {Integer.toString(offset), item.fullName()}));
		int def = offset - 8 + BT_Misc.bytesToInt(data, start);
		int npairs = BT_Misc.bytesToInt(data, start + 4);
		if (start + (npairs + 1) * 8 > data.length)
			throw new BT_InvalidInstructionException(code, opcode, offset,
				Messages.getString("JikesBT.Truncated_instruction_at_{0}_of_method_{1}_1",
					new Object[] {Integer.toString(offset), item.fullName()}));
		int values[] = new int[npairs];
		int intTargets[] = new int[npairs];
		int lastValue = 0;
		for (int m = 0; m < npairs; m++) {
			int value = BT_Misc.bytesToInt(data, start + 8 + (m * 8));
			if (m > 0 && value <= lastValue)
				throw new BT_InvalidInstructionException(code, opcode, offset,
					Messages.getString("JikesBT.Invalid_lookupswitch_instruction_at_{0}_of_method_{1}_5",
						new Object[] {Integer.toString(offset), item.fullName()}));
			values[m] = value;
			lastValue = value;
			intTargets[m] =
				offset - 8 + BT_Misc.bytesToInt(data, start + 8 + (m * 8) + 4);
		}
		return new BT_LookupSwitchIns(
			opcode,
			offset - 8,
			def,
			values,
			intTargets);
	}
	
	public int size() {
		int start = ((byteIndex / 4) + 1) * 4;
		int size = (start - byteIndex) + 8 + values.length * 8;
		return size;
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		int start = ((byteIndex / 4) + 1) * 4;
		for (int n = byteIndex + 1; n < start; n++)
			dos.writeByte(opc_nop);
		dos.writeInt(def.getInstructionByteIndex() - byteIndex);
		dos.writeInt(values.length);
		for (int n = 0; n < targets.length; n++) {
			dos.writeInt(values[n]);
			dos.writeInt(targets[n].getInstructionByteIndex() - byteIndex);
		}
	}
	public Object clone() {
		BT_JumpTarget t[] = new BT_JumpTarget[targets.length];
		for (int n = 0; n < t.length; n++)
			t[n] = targets[n];
		int v[] = new int[values.length];
		for (int n = 0; n < v.length; n++)
			v[n] = values[n];
		return new BT_LookupSwitchIns(opcode, def, v, t);
	}
	public String toString() {
		String s = getPrefix() + BT_Misc.opcodeName[opcode];
		if (targets != null) {
			for (int n = 0; n < values.length; n++)
				s += endl()	+ "\t\t    "
					+ Messages.getString("JikesBT.when_value_is_{0}_goto_instruction_at_{1}_6", 
						new Object[] {Integer.toString(values[n]), Integer.toString(targets[n].getByteIndex())}); 
			s += endl() + "\t\t    " 
				+ Messages.getString("JikesBT.else_goto_instruction_at_{0}_7", def.getByteIndex());
		}
		return s;
	}
	public String toAssemblerString(BT_CodeAttribute code) {
		StringBuffer s = new StringBuffer(BT_Misc.opcodeName[opcode]);
		s.append(" ");
		s.append(def.getLabel());
		s.append(" { ");
		for (int n = 0; n < values.length; n++) {
			s.append(values[n]);
			s.append(" ");
			s.append(targets[n].getLabel());
			s.append(" ");
		}
		s.append("}");
		return s.toString();
	}
}
