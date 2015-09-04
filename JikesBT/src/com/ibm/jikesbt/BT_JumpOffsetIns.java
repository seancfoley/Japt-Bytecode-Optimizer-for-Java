package com.ibm.jikesbt;

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an
 opc_goto,
 opc_goto_w,
 opc_if_acmpeq,
 opc_if_acmpne,
 opc_if_icmpeq,
 opc_if_icmpge,
 opc_if_icmpgt,
 opc_if_icmple,
 opc_if_icmplt,
 opc_if_icmpne,
 opc_ifeq,
 opc_ifge,
 opc_ifgt,
 opc_ifle,
 opc_iflt,
 opc_ifne,
 opc_ifnonnull,
 opc_ifnull,
 opc_jsr,
 or
 opc_jsr_w
 instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_JumpOffsetIns extends BT_JumpIns {
	
	
	BT_JumpOffsetIns(int opcode, int offset) {
		this(opcode, -1, offset);
	}
	
	BT_JumpOffsetIns(int opcode, int byteIndex, int offset) {
		super(opcode, byteIndex, new OffsetJumpTarget(offset));
	}
	
	public BT_JumpOffsetIns(int opcode, int byteIndex, BT_BasicBlockMarkerIns target) {
		super(opcode, byteIndex, target);
	}
	
	BT_JumpOffsetIns(int opcode, int byteIndex, BT_JumpTarget target) {
		super(opcode, byteIndex, target);
	}
	
	public Object clone() {
		return new BT_JumpOffsetIns(opcode, -1, target);
	}
	
	public boolean isJumpIns() {
		return true;
	}
	
	public boolean isGoToIns() {
		return opcode == opc_goto || opcode == opc_goto_w;
	}
	
	public boolean isJSRIns() {
		return opcode == opc_jsr || opcode == opc_jsr_w;
	}

	boolean nextIsTarget(BT_CodeAttribute code, int nextIndex) {
		BT_InsVector ins = code.getInstructions();
		int size = ins.size();
		//we keep iterating through all the following block markers
		//until we find the target or something else
		while(size > nextIndex) {
			BT_Ins next = ins.elementAt(nextIndex);
			if(!next.isBlockMarker()) { 
				/* the target is a block marker */
				return false;
			}
			if(next == target) {
				return true;
			}
			nextIndex++;
		}
		return false;
	}
	
	/**
	 * returns the next non-block-marker if it has no successor,
	 * otherwise returns null.
	 */
	static BT_Ins nextHasNoSuccessor(BT_CodeAttribute code, BT_Ins next) {
		int nextIndex = -1;
		BT_InsVector instructions = code.getInstructions();
		while(true) {
			if(next.hasNoSuccessor()) {
				return next;
			}
			if(!next.isBlockMarker()) {
				return null;
			}
			if(nextIndex < 0) {
				nextIndex = instructions.indexOf(next);
				if(nextIndex < 0) {
					return null;
				}
			}
			nextIndex++;
			if(nextIndex >= instructions.size()) {
				return null;
			}
			next = instructions.elementAt(nextIndex);
		}
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {

		//
		// remove a jump to the next location
		//
		if (!strict && nextIsTarget(code, n + 1)) {
			switch (opcode) {
				case opc_goto :
					//
					// unconditional jump is stack neutral, so we just remove it
					//
					return code.removeInstructionAt(n);
				case opc_if_icmpeq :
				case opc_if_icmpne :
				case opc_if_icmplt :
				case opc_if_icmpge :
				case opc_if_icmpgt :
				case opc_if_icmple :
				case opc_if_acmpeq :
				case opc_if_acmpne :
					//
					// icmp and acmp jumps have two words on the stack, so we
					// need to remove the compare-and-jump with two pop's
					//
					return code.replaceInstructionsAtWith(
						1,
						n,
						BT_Ins.make(opc_pop),
						BT_Ins.make(opc_pop));
				case opc_ifnull :
				case opc_ifnonnull :
				case opc_ifeq :
				case opc_ifne :
				case opc_iflt :
				case opc_ifge :
				case opc_ifgt :
				case opc_ifle :
					//
					// if* jumps have one word on the stack, so we
					// need to remove this compare-and-jump with one pop
					//
					return code.replaceInstructionsAtWith(
						1,
						n,
						BT_Ins.make(opc_pop));
			}
		}
		
		//
		// replace an unconditional jump to a return or throw with the same
		//
		if (opcode == opc_goto || opcode == opc_goto_w) {
			if(target instanceof BT_Ins) {
				BT_Ins next = nextHasNoSuccessor(code, (BT_Ins) target);
				if(next != null && !next.isRetIns()) { /* note: can't have multiple rets for same jsr */
					return code.replaceInstructionsAtWith(
						1,
						n,
						(BT_Ins) next.clone());
				}
			}
		}
		return false;
	}
	
	/**
	 Returns the size of the instruction.
	**/
	//
	// need to transform between wide and normal.
	//
	public int size() {
		//possible sizes of this instruction: 3, 5, 6, 8, 9, 11
		if (target != null && target instanceof BT_Ins) {
			//the byte index in target is already relative if we have not dereferenced yet
			int offset = target.getByteIndex() - byteIndex;
			boolean wide = BT_Misc.overflowsSignedShort(offset);
			switch (opcode) {
				case opc_goto :
				case opc_goto_w :
					opcode = wide ? opc_goto_w : opc_goto;
					break;
				case opc_jsr :
				case opc_jsr_w :
					opcode = wide ? opc_jsr_w : opc_jsr;
					break;
				default: {
					if(wide) {
						//we must translate this instruction into a series of instructions,
						//because there is no wide equivalent for a conditional jump
						if(getNegatedOpcode(opcode) > 0) {
							//we translate into a combo of this instruction negated and a goto
							int gotoOffset = offset - 3;
							boolean gotoIsWide = BT_Misc.overflowsSignedShort(gotoOffset);
							return gotoIsWide ? 8 : 6;
						}
						//we translate into a combo of this instruction and 2 gotos
						int gotoOffset = offset - 6;
						boolean secondGotoIsWide = BT_Misc.overflowsSignedShort(gotoOffset);
						return secondGotoIsWide ? 11 : 9;
					}
					break;
				}
			}
		}
		return super.size();
	}
	
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		int size = size();
	
		//we ensure that the target has been dereferenced to an instruction by calling target.getInstructionByteIndex()
		int offset = target.getInstructionByteIndex() - byteIndex;
	
		switch (opcode) {
			case opc_goto :
			case opc_jsr :
				if (size != 3)
					throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.unexpected_instruction_size_of_{0}._Expected_{1}_for_{2}",
						new Object[] {Integer.toString(size), "3", this}));
				dos.writeByte(opcode);
				dos.writeShort(offset);
				break;
			case opc_jsr_w :
			case opc_goto_w :
				if (size != 5)
					throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.unexpected_instruction_size_of_{0}._Expected_{1}_for_{2}",
						new Object[] {Integer.toString(size), "5", this}));
				dos.writeByte(opcode);
				dos.writeInt(offset);
				break;
			default:
				boolean wide = BT_Misc.overflowsSignedShort(offset);
				if (wide) {
					//we must translate this instruction into a series of instructions,
					//which is a little easier if we can negate the opcode
					int negatedOpcode = getNegatedOpcode(opcode);
					if(negatedOpcode > 0) {
						//we translate into a combo of this instruction negated and a goto
						int gotoOffset = offset - 3; //original offset minus size of a conditional jump
						boolean gotoIsWide = BT_Misc.overflowsSignedShort(gotoOffset);
						int expectedSize = gotoIsWide ? 8 : 6;
						if(size != expectedSize) {
							throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.unexpected_instruction_size_of_{0}._Expected_{1}_for_{2}",
								new Object[] {Integer.toString(size), Integer.toString(expectedSize), this}));
						}
						dos.writeByte(negatedOpcode);
						dos.writeShort(expectedSize); //offset to old next instruction, which now becomes the instruction after the next instruction
						if(gotoIsWide) {
							dos.writeByte(opc_goto_w); //new next instruction
							dos.writeInt(gotoOffset); //offset to original target from the goto_w instruction
						} else {
							dos.writeByte(opc_goto); //new next instruction
							dos.writeShort(gotoOffset); //offset to original target
						}
					} else {
						//we translate into a combo of this instruction and 2 gotos
						int gotoOffset = offset - 6; //original offset minus size of a conditional jump and a goto
						boolean gotoIsWide = BT_Misc.overflowsSignedShort(gotoOffset);
						int expectedSize = gotoIsWide ? 11 : 9;
						if(size != expectedSize) {
							throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.unexpected_instruction_size_of_{0}._Expected_{1}_for_{2}",
								new Object[] {Integer.toString(size), Integer.toString(expectedSize), this}));
						}
						//there is no wide instruction for conditional jumps, so we must replace with
						//a combination of the conditional jump, a goto and a goto wide.
						dos.writeByte(opcode);
						dos.writeShort(6); //offset to second goto instruction
						dos.writeByte(opc_goto);
						dos.writeShort(expectedSize - 3); //offset to instruction following second goto
						if(gotoIsWide) {
							dos.writeByte(opc_goto_w);
							dos.writeInt(gotoOffset); //the offset to original target
						} else {
							dos.writeByte(opc_goto);
							dos.writeShort(gotoOffset);
						}
					}
				}
				else {
					if (size != 3)
						throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.unexpected_instruction_size_of_{0}._Expected_{1}_for_{2}",
							new Object[] {Integer.toString(size), "3", this}));
					dos.writeByte(opcode);
					dos.writeShort(offset);
				}
				break;
		}
	}
	
	public static int getNegatedOpcode(int opcode) {
		int negatedOpcode;
		switch (opcode) {
			case opc_if_icmpeq :
				negatedOpcode = opc_if_icmpne;
				break;
			case opc_if_icmpne :
				negatedOpcode = opc_if_icmpeq;
				break;
			case opc_if_icmplt :
				negatedOpcode = opc_if_icmpge;
				break;
			case opc_if_icmpge :
				negatedOpcode = opc_if_icmplt;
				break;
			case opc_if_icmpgt :
				negatedOpcode = opc_if_icmple;
				break;
			case opc_if_icmple :
				negatedOpcode = opc_if_icmpgt;
				break;
			case opc_if_acmpeq :
				negatedOpcode = opc_if_acmpne;
				break;
			case opc_if_acmpne :
				negatedOpcode = opc_if_acmpeq;
				break;
			case opc_ifnull :
				negatedOpcode = opc_ifnonnull;
				break;
			case opc_ifnonnull :
				negatedOpcode = opc_ifnull;
				break;
			case opc_ifeq :
				negatedOpcode = opc_ifne;
				break;
			case opc_ifne :
				negatedOpcode = opc_ifeq;
				break;
			case opc_iflt :
				negatedOpcode = opc_ifge;
				break;
			case opc_ifge :
				negatedOpcode = opc_iflt;
				break;
			case opc_ifgt :
				negatedOpcode = opc_ifle;
				break;
			case opc_ifle :
				negatedOpcode = opc_ifgt;
				break;
			default :
				negatedOpcode = -1;
		}
		return negatedOpcode;
	}
	
	static class OffsetJumpTarget implements BT_JumpTarget {
		int relativeOffset;
		boolean absolute;
		
		OffsetJumpTarget(int offset) {
			this(offset, false);
		}
		
		OffsetJumpTarget(int offset, boolean absolute) {
			this.relativeOffset = offset;
			this.absolute = absolute;
		}
		
		public BT_BasicBlockMarkerIns getJumpTarget(BT_CodeAttribute code, BT_Ins fromInstruction) 
			throws BT_InstructionReferenceException {
				if(absolute) {
					return code.getInstructions().findBasicBlock(code, 
						fromInstruction, 
						relativeOffset);
				} else {
					return code.getInstructions().findBasicBlock(code, 
						fromInstruction, 
						fromInstruction.byteIndex + relativeOffset);
				}
		}
		
		public boolean isAbsoluteByteIndex() {
			return absolute;	
		}
		
		public int getByteIndex() {
			return relativeOffset;
		}
		
		public int getInstructionByteIndex() {
			throw new IllegalStateException();
		}
		
		public String getLabel() {
			throw new IllegalStateException();
		}
	}
	
}
