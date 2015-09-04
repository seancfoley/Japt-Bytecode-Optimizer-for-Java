package com.ibm.jikesbt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_ObjectCode.SubRoutine;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 * Represents the instructions of a code attribute.
 * 
 * @author sfoley
 *
 */
class BT_ByteCode extends BT_Code {
	
	BT_ObjectCode derefCode;
	LoadLocation loadedFrom;
	
	int codeLen;
	byte[] bytecodes;
	int nrExceptions;

	BT_ByteCode(BT_CodeAttribute owner, LoadLocation loadedFrom) {
		super(owner);
		this.loadedFrom = loadedFrom;
	}
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total max length of all instructions, see BT_Ins.maxSize().
	**/
	int computeMaxInstructionSizes() {
		return codeLen;
	}
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total length of all instructions, see BT_Ins.size().
	**/
	int computeInstructionSizes() {
		return codeLen;
	}
	
	int getExceptionTableEntryCount() {
		return codeLen;
	}
	
	void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching) {
		return;
	}
	
	void changeOtherReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching, int excludeHowMany, int excludeIndex) {
		return;
	}
	
	boolean removeInstruction(BT_Ins in1) {
		return false;
	}
	
	boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns) {
		return false;
	}
	
	boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns1, BT_Ins newIns2) {
		return false;
	}
	
	BT_Code dereference(BT_Method method) throws BT_ClassFileException {
		BT_ObjectCode derefCode = this.derefCode = new BT_ObjectCode(codeAttribute, nrExceptions);
		derefCode.initLocals(method.getArgsSize());
		try {
			initExceptionTable(derefCode, bytecodes, 10 + codeLen, 
					nrExceptions, method.cls.repository, method.cls.pool);
			BT_Ins in1 = null;
			int offset = 0;
			int CODE_OFFSET = 8; //the offest into the bytecodes array at which the bytecodes start
			while (offset < codeLen) {
				in1 = BT_Ins.make(bytecodes, CODE_OFFSET + offset, codeAttribute, method, loadedFrom);
				in1.setByteIndex(offset);
				offset += in1.size();
				if (offset > codeLen) {
					throw new BT_ClassFileException(
							Messages.getString("JikesBT.bytecode_incomplete_in_method_{0}_16", 
									method.fullName()));
				}
				derefCode.ins.addElement(in1);
			}
			derefCode.dereference(method);
		} catch(BT_BytecodeException e) {
			throw new BT_ClassFileException(e);
		} catch(BT_ConstantPoolException e) {
			throw new BT_ClassFileException(e);
		} catch(BT_DescriptorException e) {
			throw new BT_ClassFileException(e);
		}
		derefCode.trimToSize();
		if (method.cls.repository.factory.keepBytecodes) {
			derefCode.bytecodes = bytecodes;
		}
		return derefCode;
	}
	
	/**
	 Read exception table (that describes try-catch blocks),
	 and label the right instructions.
	 @return  offset to the unprocessed part of "data"
	**/
	private void initExceptionTable(BT_ObjectCode derefCode, byte data[], int offset, 
			int nrExceptions, BT_Repository repo, BT_ConstantPool pool)
		throws BT_ClassFileException, BT_ConstantPoolException, BT_DescriptorException {
		int end = offset + 8 * nrExceptions;
		if (data.length < end)
			throw new BT_ClassFileException(
			Messages.getString("JikesBT.{0}_attribute_length_2", BT_CodeAttribute.ATTRIBUTE_NAME));
		for (int n = offset; n < end; n += 8) {
			int startPC = BT_Misc.bytesToUnsignedShort(data, n);
			int endPC = BT_Misc.bytesToUnsignedShort(data, n + 2);
			int handlerPC = BT_Misc.bytesToUnsignedShort(data, n + 4);
			if (endPC <= startPC
				/*|| (handlerPC >= startPC && handlerPC < endPC)*/) // JVM spec doesn't explicitly forbid this
				throw new BT_ClassFileException(
						BT_CodeAttribute.ATTRIBUTE_NAME + Messages.getString("JikesBT._contains_invalid_exception_handler_20"));
			int index = BT_Misc.bytesToUnsignedShort(data, n + 6);
			String catchName = (index == 0) ? null : pool.getClassNameAt(index, BT_ConstantPool.CLASS);
			derefCode.insertExceptionTableEntry(new BT_ExceptionTableEntry(
					startPC,
					endPC,
					handlerPC,
					catchName, repo, derefCode));
		}
	}
	
	BT_LocalVector getLocals() {
		if(derefCode != null) {
			return derefCode.getLocals();
		}
		throw new IllegalStateException();
	}
	
	BT_InsVector getInstructions(){
		if(derefCode != null) {
			return derefCode.getInstructions();
		}
		throw new IllegalStateException();
	}
	
	int getInstructionSize(){
		if(derefCode != null) {
			return derefCode.getInstructionSize();
		}
		throw new IllegalStateException();
	}
	
	BT_ExceptionTableEntryVector getExceptionTableEntries() {
		if(derefCode != null) {
			return derefCode.getExceptionTableEntries();
		}
		throw new IllegalStateException();
	}
	
	void setExceptionHandler(int startInsNr, int endInsNr, int handlerInsNr, BT_Class catchType) throws BT_InstructionReferenceException {
		throw new UnsupportedOperationException();
	}
	
	void removeAllInstructions() {
		throw new UnsupportedOperationException();
	}
	
	void removeInstructionsAt(int howMany, int iin) {
		throw new UnsupportedOperationException();
	}
	
	void replaceInstructionsAtWith(int n, BT_Ins in1) {
		throw new UnsupportedOperationException();
	}
	
	void replaceInstructionsAtWith(int n, BT_Ins instruction, BT_Ins instruction2) {
		throw new UnsupportedOperationException();
	}
	
	void replaceInstructionsAtWith(int howMany, int n, BT_Ins instructions[]) {
		throw new UnsupportedOperationException();
	}
	
	void setExceptionTable(BT_ExceptionTableEntryVector table) {
		throw new UnsupportedOperationException();
	}
	
	void insertExceptionTableEntry(BT_ExceptionTableEntry e, int index) {
		throw new UnsupportedOperationException();
	}
	
	void insertExceptionTableEntry(BT_ExceptionTableEntry e) {
		throw new UnsupportedOperationException();
	}
	
	int indexOf(BT_Ins inst, int start) {
		throw new UnsupportedOperationException();
	}
	
	BT_Ins getInstruction(int insIndex) {
		throw new UnsupportedOperationException();
	}
	
	BT_Ins getPreviousInstruction(int iin) {
		throw new UnsupportedOperationException();
	}
	
	BT_Ins getPreviousInstruction(BT_Ins in1) {
		throw new UnsupportedOperationException();
	}
	
	BT_Ins getNextInstruction(int iin) {
		throw new UnsupportedOperationException();
	}

	BT_Ins getNextInstruction(BT_Ins in1) {
		throw new UnsupportedOperationException();
	}
	
	BT_Ins getFirstInstruction() {
		throw new UnsupportedOperationException();
	}
	
	BT_Ins getLastInstruction() {
		throw new UnsupportedOperationException();
	}
	
	void incrementLocalsAndParamsAccessWith(int inc, int start) {
		throw new UnsupportedOperationException();
	}
	
	void insertInstructionsAt(BT_Ins newIns[], int n) {
		throw new UnsupportedOperationException();
	}
	
	void insertInstructionAt(BT_Ins in1, int n) {
		throw new UnsupportedOperationException();
	}

	void insertInstruction(BT_Ins in1) {
		throw new UnsupportedOperationException();
	}
	
	boolean optimize(BT_Repository rep, boolean strict) throws BT_CodeException {
		throw new UnsupportedOperationException();
	}
	
	boolean optimizeAndRemoveDeadCode(BT_Repository rep, boolean strict) throws BT_CodeException {
		throw new UnsupportedOperationException();
	}
	
	void visitReachableCode(BT_CodeVisitor codeVisitor) throws BT_CodeException {
		throw new UnsupportedOperationException();
	}
	
	void visitReachableCode(BT_CodeVisitor codeVisitor, BT_Ins instr, BT_Ins prevIns, int prevInstrIndex) 
		throws BT_CodeException {
		throw new UnsupportedOperationException();
	}
	
	void visitReachableCode(BT_CodeVisitor codeVisitor, BT_Ins instr, BT_Ins prevIns, int prevInstrIndex, SubRoutine sub) 
		throws BT_CodeException {
		throw new UnsupportedOperationException();
	}
	
	void verifyRelationships(BT_Method method, boolean strict) throws BT_CodeException {
		throw new UnsupportedOperationException();
	}
	
	void print(PrintStream ps, int printFlag, BT_SourceFile source) throws BT_CodeException {
		throw new UnsupportedOperationException();
	}
	
	BT_Ins getSubroutineStartInstruction(BT_Ins target) {
		throw new UnsupportedOperationException();
	}
	
	BT_ClassVector getVerifierRequiredClasses(BT_Method method, BT_StackCell shapes[][]) {
		throw new UnsupportedOperationException();
	}
	
	boolean isWideningCastTarget(BT_Class intendedType) {
		throw new UnsupportedOperationException();
	}
	
	boolean wideningCastRequiresClassLoad(BT_Method method, BT_Class stackType, BT_Class intendedType) {
		throw new UnsupportedOperationException();
	}
	
	SubRoutine createSubRoutine(BT_Ins startInstruction) {
		throw new UnsupportedOperationException();
	}

	void write(DataOutputStream dos, BT_ConstantPool pool, int codeLen) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	void resolve(BT_ConstantPool pool) throws BT_AttributeException, BT_ClassWriteException {
		throw new BT_ClassWriteException();
	}
}
