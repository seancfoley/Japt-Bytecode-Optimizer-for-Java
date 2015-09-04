package com.ibm.jikesbt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_ObjectCode.SubRoutine;

/**
 * Represents the instructions of a code attribute.
 * 
 * @author sfoley
 *
 */
abstract class BT_Code implements BT_Opcodes, Cloneable {
	
	BT_CodeAttribute codeAttribute;
	

	BT_Code(BT_CodeAttribute owner) {
		this.codeAttribute = owner;
	}
	
	/**
	 * clones this code with a deep clone, and also makes all cloned contained
	 * elements point to the new code attribute.  
	 */
	protected Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total max length of all instructions, see BT_Ins.maxSize().
	**/
	abstract int computeMaxInstructionSizes();
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total length of all instructions, see BT_Ins.size().
	**/
	abstract int computeInstructionSizes();
	
	abstract void removeAllInstructions();
	
	abstract int getExceptionTableEntryCount();
	
	abstract void setExceptionHandler(int startInsNr, int endInsNr, int handlerInsNr, BT_Class catchType) throws BT_InstructionReferenceException;
	
	abstract void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching);
	
	abstract void changeOtherReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching, int excludeHowMany, int excludeIndex);
	
	abstract boolean removeInstruction(BT_Ins in1);
	
	abstract boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns);
	
	abstract boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns1, BT_Ins newIns2);
	
	abstract void removeInstructionsAt(int howMany, int iin);
	
	abstract void replaceInstructionsAtWith(int n, BT_Ins in1);
	
	abstract void replaceInstructionsAtWith(int n, BT_Ins instruction, BT_Ins instruction2);
	
	abstract void replaceInstructionsAtWith(int howMany, int n, BT_Ins instructions[]);
	
	abstract BT_Code dereference(BT_Method method) throws BT_ClassFileException;
	
	
	
	
	abstract int getInstructionSize();
	
	abstract BT_InsVector getInstructions();
	
	abstract BT_ExceptionTableEntryVector getExceptionTableEntries();
	
	abstract void setExceptionTable(BT_ExceptionTableEntryVector table);
	
	abstract void insertExceptionTableEntry(BT_ExceptionTableEntry e, int index);
	
	abstract void insertExceptionTableEntry(BT_ExceptionTableEntry e);
	
	abstract BT_LocalVector getLocals();
	
	abstract int indexOf(BT_Ins inst, int start);
	
	abstract BT_Ins getInstruction(int insIndex);
	
	abstract BT_Ins getPreviousInstruction(int iin);
	
	abstract BT_Ins getPreviousInstruction(BT_Ins in1);
	
	abstract BT_Ins getNextInstruction(int iin);

	abstract BT_Ins getNextInstruction(BT_Ins in1);
	
	abstract BT_Ins getFirstInstruction();
	
	abstract BT_Ins getLastInstruction();
	
	
	abstract void incrementLocalsAndParamsAccessWith(int inc, int start);
	
	abstract void insertInstructionsAt(BT_Ins newIns[], int n);
	
	abstract void insertInstructionAt(BT_Ins in1, int n);

	abstract void insertInstruction(BT_Ins in1);
	
	abstract boolean optimize(BT_Repository rep, boolean strict) throws BT_CodeException;
	
	abstract boolean optimizeAndRemoveDeadCode(BT_Repository rep, boolean strict) throws BT_CodeException;
	
	abstract void visitReachableCode(BT_CodeVisitor codeVisitor) throws BT_CodeException;
	
	abstract void visitReachableCode(BT_CodeVisitor codeVisitor, BT_Ins instr, BT_Ins prevIns, int prevInstrIndex) 
		throws BT_CodeException;
	
	abstract void visitReachableCode(BT_CodeVisitor codeVisitor, BT_Ins instr, BT_Ins prevIns, int prevInstrIndex, SubRoutine sub) 
		throws BT_CodeException;
	
	abstract void verifyRelationships(BT_Method method, boolean strict) throws BT_CodeException;
	
	abstract void print(PrintStream ps, int printFlag, BT_SourceFile source) throws BT_CodeException;
	
	abstract BT_Ins getSubroutineStartInstruction(BT_Ins target);
	
	abstract BT_ClassVector getVerifierRequiredClasses(BT_Method method, BT_StackCell shapes[][]);
	
	abstract boolean isWideningCastTarget(BT_Class intendedType);
	
	abstract boolean wideningCastRequiresClassLoad(BT_Method method, BT_Class stackType, BT_Class intendedType);
	
	abstract SubRoutine createSubRoutine(BT_Ins startInstruction);

	abstract void write(DataOutputStream dos, BT_ConstantPool pool, int codeLen) throws IOException;
	
	abstract void resolve(BT_ConstantPool pool) throws BT_AttributeException, BT_ClassWriteException;
	
}
