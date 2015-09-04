package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_Ins;


public class InstructionLocation implements Comparable {
	/**
	 * the index unto the instruction array.
	 */
	public final int instructionIndex;
	
	/**
	 * The instruction.
	 */
	public final BT_Ins instruction;
	
	InstructionLocation(BT_Ins instruction, int instructionIndex) {
		this.instructionIndex = instructionIndex;
		this.instruction = instruction;
	}
	
	public int compareTo(Object other) {
		InstructionLocation otherLocation = (InstructionLocation) other;
		int result = instructionIndex - otherLocation.instructionIndex;
		if(result == 0) {
			result = instruction.opcode - otherLocation.instruction.opcode;
			if(result == 0) {
				//TODO using hashCode is very slightly unsafe
				result = instruction.hashCode() - otherLocation.instruction.hashCode();
			}
		}
		return result;
	}
	
	public boolean equals(Object o) {
		if(o instanceof InstructionLocation) {
			InstructionLocation other = (InstructionLocation) o;
			return instruction.equals(other.instruction) 
				&& instructionIndex == other.instructionIndex;
		}
		return false;
	}
	
	public int hashCode() {
		return instruction.hashCode() + instructionIndex;
	}
	
	public String toString() {
		return "index " + Integer.toString(instructionIndex) + " bytecode " + instruction;
	}
	
}
