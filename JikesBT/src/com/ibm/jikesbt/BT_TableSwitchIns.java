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
 Represents an opc_tableswitch instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_TableSwitchIns extends BT_SwitchIns {
	public int low, high;
	
	
	public BT_TableSwitchIns(
		int opcode,
		int low,
		int high,
		BT_BasicBlockMarkerIns def,
		BT_BasicBlockMarkerIns targets[]) {
		this(opcode, low, high, (BT_JumpTarget) def, (BT_JumpTarget[]) targets);
	}
	
	public BT_TableSwitchIns(
		int opcode,
		int low,
		int high,
		BT_JumpTarget def,
		BT_JumpTarget targets[]) {
		super(opcode, def, targets);
		this.low = low;
		this.high = high;
	}
	
	BT_TableSwitchIns(
		int opcode,
		int index,
		int low,
		int high,
		int defIntTarget,
		int intTargets[]) {
		super(opcode, index, defIntTarget, intTargets);
		this.low = low;
		this.high = high;
	}
	
	public Object clone() {
		BT_JumpTarget t[] = new BT_JumpTarget[targets.length];
		for (int n = 0; n < t.length; n++)
			t[n] = targets[n];
		return new BT_TableSwitchIns(opcode, low, high, def, t);
	}
	
	public static BT_TableSwitchIns make(
		BT_CodeAttribute code, 
		int opcode,
		int offset,
		byte data[],
		BT_Method item) throws BT_InvalidInstructionException {
		int start = ((offset / 4) + 1) * 4;
		if (start + 12 > data.length)
			throw new BT_InvalidInstructionException(code, opcode, offset,
				Messages.getString("JikesBT.Truncated_instruction_at_{0}_of_method_{1}_1", 
					new Object[] {Integer.toString(offset), item.fullName()}));
		int def = offset - 8 + BT_Misc.bytesToInt(data, start);
		int low = BT_Misc.bytesToInt(data, start + 4);
		int high = BT_Misc.bytesToInt(data, start + 8);
		if (start + 12 + (high - low + 1) * 4 > data.length)
			throw new BT_InvalidInstructionException(code, opcode, offset,
				Messages.getString("JikesBT.Truncated_instruction_at_{0}_of_method_{1}_1", 
					new Object[] {Integer.toString(offset), item.fullName()}));
		int intTargets[] = new int[high - low + 1];
		for (int m = 0; m < high - low + 1; m++)
			intTargets[m] =
				offset - 8 + BT_Misc.bytesToInt(data, (start + 12) + (m * 4));
		return new BT_TableSwitchIns(
			opcode,
			offset - 8,
			low,
			high,
			def,
			intTargets);
	}
	
	public int size() {
		int start = ((byteIndex / 4) + 1) * 4;
		int size = (start - byteIndex) + 12 + (high - low + 1) * 4;
		return size;
	}
	
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		int start = ((byteIndex / 4) + 1) * 4;
		for (int n = byteIndex + 1; n < start; n++)
			dos.writeByte(opc_nop);
		dos.writeInt(def.getInstructionByteIndex() - byteIndex);
		dos.writeInt(low);
		dos.writeInt(high);
		for (int n = 0; n < targets.length; n++)
			dos.writeInt(targets[n].getInstructionByteIndex() - byteIndex);
	}
	
	public String toString() {
		String s = getPrefix() + BT_Misc.opcodeName[opcode];
		if (targets != null) {
			for (int n = 0; n < targets.length; n++)
				s += endl()
					+ Messages.getString("JikesBT._t_t____when_value_is_{0}_goto_instruction_at_{1}_6",
						new Object[] {Integer.toString((low + n)), Integer.toString(targets[n].getByteIndex())});
			s += endl() + Messages.getString("JikesBT._t_t____else_goto_instruction_at_{0}_8", Integer.toString(def.getByteIndex()));
		}
		return s;
	}
	
	public String toAssemblerString(BT_CodeAttribute code) {
		StringBuffer s = new StringBuffer(BT_Misc.opcodeName[opcode]);
		s.append(" ");
		s.append(def.getLabel());
		s.append(" { ");
		s.append(low);		
		s.append(" ");
		s.append(high);		
		s.append(" ");
		for (int n = 0; n < targets.length; n++) {
			s.append(targets[n].getLabel());
			s.append(" ");
		}
		s.append("}");
		return s.toString();
	}
}
