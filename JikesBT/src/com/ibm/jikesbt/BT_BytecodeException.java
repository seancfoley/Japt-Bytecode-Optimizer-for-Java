/*
 * Created on Sep 27, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

/**
 * 
 * @author sfoley
 *
 * Describes an error in the structure of the code in a BT_CodeAttribute.  This may entail
 * either the bytecodes or the exception table entries.  These errors would result in VerifyErrors
 * if the code were to be verified by a java virtual machine.
 */
public abstract class BT_BytecodeException extends BT_ClassFormatRuntimeException {
	public final BT_CodeAttribute code;
	public final int byteIndex;
	
	public BT_BytecodeException(BT_CodeAttribute code, int byteIndex) {
		super();
		this.code = code;
		this.byteIndex = byteIndex;
	}
	
	public BT_BytecodeException(BT_CodeAttribute code, int byteIndex, String explanation) {
		super(explanation);
		this.code = code;
		this.byteIndex = byteIndex;
	}
	
	public static class BT_InstructionReferenceException extends BT_BytecodeException {
		
		/**
		 * the attribute which contains the reference
		 */
		public final BT_Attribute referringAttribute;
		
		/**
		 * the instruction making the reference (may be null)
		 */
		public final BT_Ins referringInstruction;
		
		/**
		 * the exception table entry making the reference (may be null)
		 */
		public final BT_ExceptionTableEntry referringExceptionTableEntry;
		
		/**
		 * 
		 * @param code the code containing the referring exception table entry
		 * @param referringExceptionTableEntry the referring exception table entry
		 * @param byteIndex the byte index of the missing instruction
		 * @param explanation
		 */
		public BT_InstructionReferenceException(
				BT_CodeAttribute code, 
				BT_ExceptionTableEntry referringExceptionTableEntry,
				int byteIndex,
				String explanation) {
			super(code, byteIndex, explanation);
			this.referringAttribute = code;
			this.referringInstruction = null;
			this.referringExceptionTableEntry = referringExceptionTableEntry;
		}
		
		/**
		 * 
		 * @param code the code containing the referring instruction
		 * @param referringInstruction the referring instruction
		 * @param byteIndex the byte index of the missing instruction
		 * @param explanation
		 */
		public BT_InstructionReferenceException(BT_CodeAttribute code, BT_Ins referringInstruction, int byteIndex, String explanation) {
			super(code, byteIndex, explanation);
			this.referringAttribute = code;
			this.referringInstruction = referringInstruction;
			this.referringExceptionTableEntry = null;
		}

		/**
		 * 
		 * @param code the code containing the instruction
		 * @param referringAttribute the referring attribute
		 * @param byteIndex the byte index of the missing instruction
		 * @param explanation
		 */
		public BT_InstructionReferenceException(BT_CodeAttribute code, BT_Attribute referringAttribute, int byteIndex,
				String explanation) {
			super(code, byteIndex, explanation);
			this.referringAttribute = referringAttribute;
			this.referringExceptionTableEntry = null;
			this.referringInstruction = null;
		}
	}

	public static class BT_InvalidInstructionException extends BT_BytecodeException {
		public final int opcode;
		
		public BT_InvalidInstructionException(BT_CodeAttribute code, int opcode, int byteIndex) {
			super(code, byteIndex);
			this.opcode = opcode;
		}

		public BT_InvalidInstructionException(BT_CodeAttribute code, int opcode, int byteIndex,
				String explanation) {
			super(code, byteIndex, explanation);
			this.opcode = opcode;
		}

	}
}
