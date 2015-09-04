package com.ibm.jikesbt;




import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_CodeException.BT_AbstractInstantiationException;
import com.ibm.jikesbt.BT_CodeException.BT_AbstractMethodException;
import com.ibm.jikesbt.BT_CodeException.BT_AccessException;
import com.ibm.jikesbt.BT_CodeException.BT_CircularJSRException;
import com.ibm.jikesbt.BT_CodeException.BT_CodePathException;
import com.ibm.jikesbt.BT_CodeException.BT_IllegalClinitException;
import com.ibm.jikesbt.BT_CodeException.BT_IllegalInitException;
import com.ibm.jikesbt.BT_CodeException.BT_IncompatibleClassException;
import com.ibm.jikesbt.BT_CodeException.BT_IncompatibleFieldException;
import com.ibm.jikesbt.BT_CodeException.BT_IncompatibleMethodException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidReturnException;
import com.ibm.jikesbt.BT_CodeException.BT_MissingConstructorException;
import com.ibm.jikesbt.BT_StackType.ClassType;

/**
 * Represents the instructions of a code attribute.  This includes a representation of the accessed locals,
 * a vector of the exception table entries, and a vector of the instructions themselves.
 * 
 * Access to this object should always go through the containing code attribute, because there are additional sub attributes
 * in the code attribute that can be affected by changes to the code, such as the debug attributes and stackmaps.
 * 
 * If the instruction, locals or exception table vectors are accessed directly for read access, then they should only be changed
 * by using the methods available in BT_CodeAttribute.
 * 
 * @author sfoley
 *
 */
class BT_ObjectCode extends BT_Code {
		
	/**
	 Describes the try/catch blocks in the code.
	**/
	BT_ExceptionTableEntryVector exceptionTableEntries;

	/**
	 The locals for the instructions
	 See {@link BT_CodeAttribute#initLocals}
	**/
	BT_LocalVector locals;
	
	/**
	 The instructions.
	**/
	BT_InsVector ins;
	
	byte[] bytecodes;

	BT_ObjectCode(BT_CodeAttribute owner, int nrExceptions) {
		super(owner);
		ins = new BT_InsVector();
		exceptionTableEntries = new BT_ExceptionTableEntryVector(nrExceptions);
	}
	
	BT_ObjectCode(BT_CodeAttribute owner, int nrInstructions, int nrExceptions, int argSize) {
		super(owner);
		ins = new BT_InsVector(nrInstructions);
		initLocals(argSize);
		exceptionTableEntries = new BT_ExceptionTableEntryVector(nrExceptions);
		
	}
	
	BT_Code dereference(BT_Method method) {
		dereferenceInstructions();
		dereferenceExceptionTable();
		return this;
	}
	
	private void dereferenceInstructions() {
		for (int n = 0; n < ins.size(); n++) {
			ins.elementAt(n).link(codeAttribute); 
		}
	}
	
	private void dereferenceExceptionTable() {
		for (int n = 0; n < exceptionTableEntries.size(); n++) {
			exceptionTableEntries.elementAt(n).dereference(codeAttribute);
		}
	}
	
	BT_InsVector getInstructions() {
		return ins;
	}
	
	int getInstructionSize() {
		return ins.size();
	}
	
	BT_LocalVector getLocals() {
		return locals;
	}
	
	int getExceptionTableEntryCount() {
		return exceptionTableEntries.size();
	}
	
	BT_ExceptionTableEntryVector getExceptionTableEntries() {
		return exceptionTableEntries;
	}
	
	/**
	 * clones this code attribute with a deep clone, and also makes all cloned contained
	 * elements point to the new code attribute.  The contained elements that are cloned include the 
	 * contained attributes (such as stack maps, local variable table and line number table), 
	 * the contained exception table entries, the contained instruction vector
	 * and each instruction in the vector, and the local vector of the instruction vector.
	 */
	protected Object clone() {
		BT_ObjectCode clone = (BT_ObjectCode) super.clone();
		if(ins != null) {
			BT_InsVector clonedIns = clone.ins = (BT_InsVector) ins.clone();
			for(int i=0; i<clonedIns.size(); i++) {
				clonedIns.cloneElementAt(i);
			}
		}
		if(locals != null) {
			clone.locals = (BT_LocalVector) locals.clone();
			for(int i=0; i<clone.locals.size(); i++) {
				clone.locals.cloneElementAt(i);
			}
		}
		
		clone.exceptionTableEntries = 
			(BT_ExceptionTableEntryVector) exceptionTableEntries.clone();
		for(int k=0; k<clone.exceptionTableEntries.size(); k++) {
			BT_ExceptionTableEntry entry = clone.exceptionTableEntries.cloneElementAt(k);
			entry.code = clone;
		}

		BT_InsVector clonedIns = clone.ins;
		
		// Change references to new code
		for (int k = 0; k < ins.size(); k++) {
			BT_Ins from = ins.elementAt(k);
			BT_Ins to = clonedIns.elementAt(k);
			//this will change the references in the instructions and the exception table entries
			clone.changeReferencesFromTo(from, to, true);
		}
		
		// change references to new locals
		clone.incrementLocalsAndParamsAccessWith(0, 0);
			
		return clone;
	}
	
	void initLocals(int argsSize) {
		if(locals == null) {
			locals = new BT_LocalVector(argsSize);
		}
		if (argsSize > 0) {
			locals.elementAt(argsSize - 1);
		}
	}
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total length of all instructions, see BT_Ins.size().
	**/
	int computeInstructionSizes() {
		return ins.setAllByteIndexes();
	}
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total max length of all instructions, see BT_Ins.maxSize().
	**/
	int computeMaxInstructionSizes() {
		return ins.setAllByteIndexesMax();
	}
	
	/**
	 Adds a try/catch/finally block.
	 Note the stack must have the same depth before startInsNr and
	 after EndInsNr??
	
	 @param  endInsNr  The index (not the byte-offset) of the last
	 instruction to be included in the try block.
	**/
	void setExceptionHandler(
		int startInsNr,
		int endInsNr,
		int handlerInsNr,
		BT_Class catchType) throws BT_InstructionReferenceException {
		exceptionTableEntries.addElement(
				new BT_ExceptionTableEntry(
					ins.markBlock(startInsNr, true),
					ins.markBlock(endInsNr, true),
					ins.markBlock(handlerInsNr, true),
					catchType, codeAttribute));
		codeAttribute.resetCachedCodeInfo();
	}
	
	void setExceptionTable(BT_ExceptionTableEntryVector table) {
		exceptionTableEntries = table;
		codeAttribute.resetCachedCodeInfo();
	}
	
	/**
	 Changes all references to one instructions into references to another.
	 If oldIns is a BT_BasicBlockMarker, then newIns must be a BT_BasicBlockMarker.
	 References to block markers can only be changed to references to other block markers.
	 @param switching true if either oldIns or newIns are not in this code attribute
	 */
	void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching) {
		if(oldIns.isBlockMarker()) {
			BT_BasicBlockMarkerIns oldBlockMarker = (BT_BasicBlockMarkerIns) oldIns;
			if(newIns != null && !newIns.isBlockMarker()) {
				throw new IllegalArgumentException("block markers can only map to block markers");
			}
			BT_BasicBlockMarkerIns newBlockMarker = (BT_BasicBlockMarkerIns) newIns;
			for (int k = 0; k < ins.size(); k++) {
				ins.elementAt(k).changeReferencesFromTo(oldBlockMarker, newBlockMarker);
			}
			for (int n = 0; n < exceptionTableEntries.size(); n++) {
				exceptionTableEntries.elementAt(n).changeReferencesFromTo(oldBlockMarker, newBlockMarker);
			}
		}
	}
	
	/**
	 Changes all references to one instructions into references to another.
	 If oldIns is a BT_BasicBlockMarker, then newIns must be a BT_BasicBlockMarker.
	 References to block markers can only be changed to references to other block markers.
	 @param switching true if either oldIns or newIns are not in this code attribute
	 */
	void changeOtherReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching, int excludeHowMany, int excludeIndex) {
		if(oldIns.isBlockMarker()) {
			BT_BasicBlockMarkerIns oldBlockMarker = (BT_BasicBlockMarkerIns) oldIns;
			if(newIns != null && !newIns.isBlockMarker()) {
				throw new IllegalArgumentException("block markers can only map to block markers");
			}
			BT_BasicBlockMarkerIns newBlockMarker = (BT_BasicBlockMarkerIns) newIns;
			for (int k = 0; k < excludeIndex; k++) {
				BT_Ins inst = ins.elementAt(k);
				inst.changeReferencesFromTo(oldBlockMarker, newBlockMarker);
			}
			for (int k = excludeIndex + excludeHowMany; k < ins.size(); k++) {
				BT_Ins inst = ins.elementAt(k);
				inst.changeReferencesFromTo(oldBlockMarker, newBlockMarker);
			}
			for (int n = 0; n < exceptionTableEntries.size(); n++) {
				/* 
				 * If both indices of the span are in the excluded range, then the span will become empty. 
				 * If we remove the target of the excluded range but the span remains, then we will point to the
				 * new block marker.
				 */
				exceptionTableEntries.elementAt(n).changeReferencesFromTo(oldBlockMarker, newBlockMarker);
			}
		}
	}
	
	/**
	 Changes all instructions that refer to one of a range of instructions
	 so they will refer to a single "new" instruction, but does not affect the instructions
	 in the range themselves.
	 Useful if one instruction is replacing several.
	
	 @param howMany  The number of instructions in the "old" range.
	 @param n        The index of the first instruction in the "old" range.
	 @param newIns   The "new" instruction.
	**/
	private void changeOtherReferencesAtTo(int howMany, int n, BT_Ins newIns) {
		for (int k = n; k < n + howMany; k++) {
			codeAttribute.changeOtherReferencesFromTo(ins.elementAt(k), newIns, false, howMany, n);
		}
	}
	
	BT_Ins getPreviousInstruction(int iin) {
		return ins.elementAt(iin - 1);
	}
	
	/**
	 Finds the given instruction and returns its predecessor.
	**/
	BT_Ins getPreviousInstruction(BT_Ins in1) {
		for (int n = 1; n < ins.size(); n++) {
			if (in1 == ins.elementAt(n)) {
				return ins.elementAt(n - 1);
			}
		}
		return null;
	}

	BT_Ins getNextInstruction(int iin) {
		return ins.elementAt(iin + 1);
	}
	
	/**
	 Finds the given instruction and returns its successor.
	**/
	BT_Ins getNextInstruction(BT_Ins in1) {
		for (int n = 0; n < ins.size() - 1; n++) {
			if (in1 == ins.elementAt(n)) {
				return ins.elementAt(n + 1);
			}
		}
		return null;
	}
	
	/**
	 Returns the first instruction.
	**/
	BT_Ins getFirstInstruction() {
		return ins.firstElement();
	}
	
	/**
	 Returns the last instruction.
	**/
	BT_Ins getLastInstruction() {
		return ins.lastElement();
	}

	/**
	 * @throws IndexOutOfBoundsException
	 * @param insIndex
	 * @return
	 */
	BT_Ins getInstruction(int insIndex) {
		return ins.elementAt(insIndex);
	}
	
	boolean contains(BT_Ins inst) {
		return ins.contains(inst);
	}
	
	int indexOf(BT_Ins inst, int start) {
		return ins.indexOf(inst, start);
	}
	
	/**
	 Removes the instruction from the instruction vector.
	 This method should be used rather than removing the instructions
	 from the instruction vector directly, since it also calls the
	 "remove" method of the instructions to back out of any
	 relationships the instructions have with other objects.
	 @param in1 the instruction to be removed
	**/
	boolean removeInstruction(BT_Ins in1) {
		for (int n = 0; n < ins.size(); n++) {
			if (in1 == ins.elementAt(n)) {
				removeInstructionsAt(1, n);
				return true;
			}
		}
		return false;
	}
	
	/**
	 Replaces an instruction with a new instruction.
	
	 The "remove" method is called on the replaced instruction so it
	 can back out of any relationships it has with other objects.
	
	 <p> The same instruction object should not be used to represent more
	 than one instruction;  use the clone() method to create a new
	 instruction object for each instruction.
	
	 <p> Related: {@link BT_Method#replaceInstructionWith}.
	**/
	boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns) {
		for (int n = 0; n < ins.size(); n++) {
			if (oldIns == ins.elementAt(n)) {
				replaceInstructionsAtWith(n, newIns);
				return true;
			}
		}
		return false;
	}
	
	/**
	 Replaces an instruction with 2 new instructions.
	
	 The "remove" method is called on the replaced instruction so it
	 can back out of any relationships it has with other objects.
	
	 <p> The same instruction object should not be used to represent more
	 than one instruction; use the clone() method to create a new
	 instruction object for each instruction.
	**/
	boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns1, BT_Ins newIns2) {
		for (int n = 0; n < ins.size(); n++) {
			if (oldIns == ins.elementAt(n)) {
				replaceInstructionsAtWith(n, newIns1, newIns2);
				return true;
			}
		}
		return false;
	}
	
	private boolean rangeContainsBlockMarker(int howMany, int iin) {
		for(int k = 0; k < howMany && iin < ins.size(); iin++, k++) {
			if(ins.elementAt(iin).isBlockMarker()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 Removes instructions from the instruction vector.
	 This method should be used rather than removing the instructions from the
	 instruction vector directly, since it also calls the "remove" method of the
	 instructions to back out of any relationships the instructions have with
	 other objects.
	
	 @param  howMany  number of instructions to remove
	 @param  iin  index in the instruction vector of the first instruction to be
	   removed
	**/
	void removeInstructionsAt(int howMany, int iin) {
		// a new reference for anything that points to instruction between [iin,iin+howMany>
		BT_Ins newRef;
		
		//if we remove a block marker instruction, then we must change all references to another
		//block marker, because some instructions and attributes can point only to block markers
		boolean addBlockMarker = false;
		if (iin + howMany >= ins.size()) {
			if(iin == 0) {
				removeAllInstructions();
				return;
			}
			newRef = ins.elementAt(iin - 1);
			if(!newRef.isBlockMarker() && rangeContainsBlockMarker(howMany, iin)) {
				newRef = BT_Ins.make();
				addBlockMarker = true;
			} 
		} else {
			// when not removing from the end of the code 
			// the new reference for instructions is the next after the code removed
			newRef = ins.elementAt(iin + howMany);
			if(!newRef.isBlockMarker() && rangeContainsBlockMarker(howMany, iin)) {
				//first check if there is a block marker just before the removed code
				if(iin > 0 && ins.elementAt(iin - 1).isBlockMarker()) {
					newRef = ins.elementAt(iin - 1);
				} else {
					newRef = BT_Ins.make();
					addBlockMarker = true;
				}
			}
		}
		
		changeOtherReferencesAtTo(howMany, iin, newRef);
		
		for (int k = 0; k < howMany && ins.size() > iin; k++) {
			ins.elementAt(iin).unlink(codeAttribute);
			ins.removeElementAt(iin);
		}
		if(addBlockMarker) {
			if(ins.size() == iin) {
				ins.insertElementAt(newRef, iin - 1);
			} else {
				ins.insertElementAt(newRef, iin);
			}
		}
		codeAttribute.resetCachedCodeInfo();
	}
	
	/**
	 Replaces a range of instructions with 2 new instructions.
	
	 The "unlink" method is called on the replaced instructions so they
	 can back out of any relationships they have with other objects.
	 
	 The "link" method is called in the inserted instructions.
	 
	 <p> The same instruction object should not be used to represent more
	 than one instruction; use the clone() method to create a new
	 instruction object for each instruction.
	**/
	void replaceInstructionsAtWith(int howMany, int n, BT_Ins instructions[]) {
		if(n + howMany > ins.size()) {
			howMany = ins.size() - n;
		}
		
		/* first we insert the new instructions */
		int insertionIndex = n + howMany;
		for(int i=instructions.length - 1; i>= 0; i--) {
			BT_Ins in1 = instructions[i];
			in1.link(codeAttribute);
			ins.insertElementAt(in1, insertionIndex);
		}
		BT_Ins first = instructions[0];
		
		/* make the newly referenced instruction a block marker if the range contains a blockmarker */
		if(!first.isBlockMarker() && rangeContainsBlockMarker(howMany, n)) {
			first = BT_Ins.make();
			ins.insertElementAt(first, insertionIndex);
		}
		
		changeOtherReferencesAtTo(howMany, n, first);
		for (int k = 0; k < howMany; k++) {
			ins.elementAt(n).unlink(codeAttribute);
			ins.removeElementAt(n);
		}
		codeAttribute.resetCachedCodeInfo();
	}
	
	void replaceInstructionsAtWith(int n, BT_Ins instruction) {
		replaceInstructionsAtWith(n, instruction, null, null);
	}
	
	void replaceInstructionsAtWith(int n, BT_Ins instruction, BT_Ins instruction2) {
		replaceInstructionsAtWith(n, instruction, instruction2, null);
	}
	
	void replaceInstructionsAtWith(int n, BT_Ins instruction, BT_Ins instruction2, BT_Ins instruction3) {
		if(n >= ins.size()) {
			return;
		}
		
		/* first we insert the new instructions */
		if(instruction3 != null) {
			instruction3.link(codeAttribute);
			ins.insertElementAt(instruction3, n + 1);
		}
		
		if(instruction2 != null) {
			instruction2.link(codeAttribute);
			ins.insertElementAt(instruction2, n + 1);
		}
		
		instruction.link(codeAttribute);
		ins.insertElementAt(instruction, n + 1);
		
		
		BT_Ins first = instruction;
		/* make the newly referenced instruction a block marker if the range contains a blockmarker */
		if(!first.isBlockMarker() && ins.elementAt(n).isBlockMarker()) {
			first = BT_Ins.make();
			ins.insertElementAt(first, n + 1);
		}
		codeAttribute.changeOtherReferencesFromTo(ins.elementAt(n), first, false, 1, n);
		ins.elementAt(n).unlink(codeAttribute);
		ins.removeElementAt(n);
		codeAttribute.resetCachedCodeInfo();
	}
	
	/**
	 Removes all instructions from the instruction vector.
	 This method should be used rather than removing the instructions
	 from the instruction vector directly, since it also calls the
	 "remove" method of the instructions to back out of any
	 relationships the instructions have with other objects.
	**/
	void removeAllInstructions() {
		for (int k = 0; k < ins.size(); k++) {
			ins.elementAt(k).unlink(codeAttribute);
		}
		ins.removeAllElements();
		codeAttribute.resetCachedCodeInfo();
	}
	
	/**
	 * @param inc
	 * @param start
	 */
	void incrementLocalsAndParamsAccessWith(int inc, int start) {
		int size = locals.size();
		for (int k = 0; k < inc; k++) { // Per new local
			locals.addElement(new BT_Local(size + k)); // Make room
		}
		for (int k = 0; k < ins.size(); k++) { // Per instruction
			ins.elementAt(k).incrementLocalsAccessWith(inc, start, locals); // Adjust each instruction
		}
		if(inc != 0) {
			codeAttribute.resetMaxLocals();
		}
	}
	
	/**
	 Inserts instructions into the instruction vector.
	
	 The same instruction object should not be used to represent more
	 than one instruction; use the clone() method to create a new
	 instruction object for each instruction.
	**/
	void insertInstructionsAt(BT_Ins newIns[], int n) {
		for (int k = newIns.length - 1; k >= 0; k--) {
			BT_Ins in1 = newIns[k];
			in1.link(codeAttribute);
			ins.insertElementAt(in1, n);
		}
		codeAttribute.resetCachedCodeInfo();
	}
	
	void insertExceptionTableEntry(BT_ExceptionTableEntry e, int index) {
		exceptionTableEntries.insertElementAt(e, index);
	}
	
	void insertExceptionTableEntry(BT_ExceptionTableEntry e) {
		exceptionTableEntries.addElement(e);
	}
	
	/**
	 Inserts an instruction into the instruction vector and dereferences it.
	
	 The same instruction object should not be used to represent more than one
	 instruction (because an offset is stored in it).
	 Use the clone() method to create a new instruction object for each
	 similar instruction.
	
	 @param  n       The number of the instruction (not its byte
	                 offset) before which to insert.
	 @return Always true
	**/
	void insertInstructionAt(BT_Ins in1, int n) {
		ins.insertElementAt(in1, n);
		in1.link(codeAttribute);
		codeAttribute.resetCachedCodeInfo();
	}

	/**
	 Inserts a new last instruction.
	**/
	void insertInstruction(BT_Ins in1) {
		insertInstructionAt(in1, ins.size());
	}
	
	void trimToSize() {
		ins.trimToSize();
		locals.trimToSize();
	}
	
	/**
	 Applies some local optimizations.
	 
	 @param strict Strict preservation of semantics with respect to bytecode verification
	**/
	boolean optimize(BT_Repository rep, boolean strict) throws BT_CodeException {
		boolean result = false;
		//TODO check for unnecessary check casts:
		//do not move those that make a type less specific since that is done by the casting optimization
		//but we can change those that cast from a type to the same type
		//so if we use the visitor without merge candidates, then casting from a stack type to the same type should be removed
		
		verifyAndOptimizeGlobally(rep, true, strict);
		
		for (int n = ins.size() - 1; n >= 0; n--) {
			BT_Ins instruction = ins.elementAt(n);
			if (instruction.optimize(codeAttribute, n, strict)) {
				result = true;
				// Try this instruction and the following instruction
				// again. Make sure that we are within the bounds of the
				// vector, since multiple instructions may have been
				// removed.
				n += 2; // try again on the next instruction
				if (n > ins.size()) {
					n = ins.size(); // try again on the last instruction
				}
			}
		}
		
		return result;
	}
	
	private boolean verifyAndOptimizeGlobally(BT_Repository repository, boolean verify, boolean strict) throws BT_CodeException {
		//TODO whenever we create a stack shape visitor, 
		//we assume the method field in BT_CodeAttribute is assigned, 
		//and same goes for each type of BT_StackCellProvider...
		
		boolean result = false;
		if(verify || !strict) {
			BT_LocalTracker provider = new BT_LocalTracker(repository);
			BT_StackShapeVisitor visitor 
				= new BT_StackShapeVisitor(codeAttribute, BT_StackPool.pool, provider);
			/* 
			 * we do not use merge candidates because there is no need 
			 */
			visitor.useMergeCandidates(false);
			BT_StackShapes shapes = visitor.populate();
			if(shapes == null) {
				return result;
			}
			if(verify) {
				shapes.tolerateStubs(!strict);
				shapes.verifyStacks();
			}
			if(!strict) {
				//remove dead stores
				for(int i=0; i<ins.size(); i++) {
					BT_Ins instruction = ins.elementAt(i);
					if(instruction.isLocalWriteIns()) {
						if(instruction instanceof BT_StoreLocalIns) {
							BT_StoreLocalIns storeIns = (BT_StoreLocalIns) instruction;
							if(provider.isLiveStore(storeIns)) {
								continue;
							}
							boolean is2Slot = storeIns.is2Slot();
							result = true;
							replaceInstructionsAtWith(i, is2Slot ? BT_Ins.make(opc_pop2) : BT_Ins.make(opc_pop));
						} else {
							BT_IIncIns iincIns = (BT_IIncIns) instruction;
							if(provider.isLiveStore(iincIns)) {
								continue;
							}
							result = true;
							removeInstructionsAt(1, i);
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 Does verification of the relationship of the code to various items
	 such as the return type, invoked methods and accessed fields. 
	 @param strict if true, verification will assume that other classes that are referenced will not change and are correct as they are
	 @throws BT_CodeException if errors are found.
	**/
	void verifyRelationships(BT_Method method, boolean strict) throws BT_CodeException {
		BT_InsVector insVector = ins;
		BT_MethodSignature signature = method.getSignature();
		for(int i=0; i<insVector.size(); i++) {
			BT_Ins instruction = insVector.elementAt(i);
			int opcode = instruction.opcode;
			switch(opcode) {
				case opc_invokeinterface:
				case opc_invokevirtual:
					BT_MethodRefIns methodRefIns = (BT_MethodRefIns) instruction;
					BT_Method targetMethod = methodRefIns.getMethodTarget();
					if(targetMethod.isStaticInitializer()) {
						throw new BT_IllegalClinitException(codeAttribute, instruction, i);
					}
					if(targetMethod.isConstructor()) {
						throw new BT_IllegalInitException(codeAttribute, instruction, i);
					}
					if(strict) {
						BT_Class targetClass = methodRefIns.getClassTarget();
						boolean isCorrectType = (opcode == opc_invokevirtual) ? !targetClass.isInterface() : targetClass.isInterface();
						if(!isCorrectType) {
							throw new BT_IncompatibleClassException(codeAttribute, instruction, i, targetClass);
						}
						if(!targetMethod.isStub() && targetMethod.isStatic()) {
							throw new BT_IncompatibleMethodException(codeAttribute, instruction, i, targetMethod);
						}
						if(targetMethod.isAbstract() && !targetClass.isAbstract()) {
							/* 
							 * we ignore the simplest case which is handled by the JikesBT load process,
							 * which is the case in which the target class owns the target method, which 
							 * means that a class with an abstract method is not abstract.
							 */
							if(!targetClass.equals(targetMethod.getDeclaringClass())) {
								throw new BT_AbstractMethodException(codeAttribute, instruction, i, targetMethod);
							}
						}
						checkAccess(instruction, i, targetMethod, method);
					}
					break;
				case opc_invokestatic:
					methodRefIns = (BT_MethodRefIns) instruction;
					targetMethod = methodRefIns.getMethodTarget();
					if(targetMethod.isStaticInitializer()) {
						throw new BT_IllegalClinitException(codeAttribute, instruction, i);
					}
					if(targetMethod.isConstructor()) {
						throw new BT_IllegalInitException(codeAttribute, instruction, i);
					}
					if(strict) {
						BT_Class targetClass = methodRefIns.getClassTarget();
						if(targetClass.isInterface()) {
							throw new BT_IncompatibleClassException(codeAttribute, instruction, i, targetClass);
						}
						if(!targetMethod.isStub() && !targetMethod.isStatic()) {
							throw new BT_IncompatibleMethodException(codeAttribute, instruction, i, targetMethod);
						}
						checkAccess(instruction, i, targetMethod, method);
					}
					break;
				case opc_invokespecial:
					methodRefIns = (BT_MethodRefIns) instruction;
					targetMethod = methodRefIns.getMethodTarget();
					if(targetMethod.isStaticInitializer()) {
						throw new BT_IllegalClinitException(codeAttribute, instruction, i);
					}
					if(strict) {
						BT_Class targetClass = methodRefIns.getClassTarget();
						if(targetClass.isInterface()) {
							throw new BT_IncompatibleClassException(codeAttribute, instruction, i, targetClass);
						}
						if(targetMethod.isConstructor()) {
							if(!targetClass.equals(targetMethod.getDeclaringClass())) {
								throw new BT_MissingConstructorException(codeAttribute, instruction, i, targetMethod);
							}
						}
						if(!targetMethod.isStub() && targetMethod.isStatic()) {
							throw new BT_IncompatibleMethodException(codeAttribute, instruction, i, targetMethod);
						}
						if(targetMethod.isAbstract()) {
							throw new BT_AbstractMethodException(codeAttribute, instruction, i, targetMethod);
						}
						checkAccess(instruction, i, targetMethod, method);
					}
					break;
				case opc_ireturn:
				case opc_lreturn:
				case opc_freturn:
				case opc_dreturn:
				case opc_areturn:
				case opc_return:
					if(opcode != signature.returnType.getOpcodeForReturn()) {
						throw new BT_InvalidReturnException(codeAttribute, instruction, i, signature.returnType);
					}
					break;
				case opc_getfield:
				case opc_putfield:
					BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) instruction;
					BT_Field targetField = fieldRefIns.getFieldTarget();
					if(strict) {
						checkAccess(instruction, i, targetField, method);
						BT_Class targetClass = fieldRefIns.getClassTarget();
						if(!targetClass.isStub() && targetField.isStatic()) {
							throw new BT_IncompatibleFieldException(codeAttribute, instruction, i, targetField);
						}
					}
					if(opcode == opc_putfield) {
						if(targetField.isFinal()) {
							if(strict) {
								BT_Class currentClass = method.getDeclaringClass();
								if(!targetField.getDeclaringClass().equals(currentClass)) {
									throw new BT_AccessException(codeAttribute, instruction, i, targetField);
								}
							}
							if(!method.isConstructor()) {
								throw new BT_AccessException(codeAttribute, instruction, i, targetField);
							}
						}
					}
					break;
				case opc_getstatic:
				case opc_putstatic:
					fieldRefIns = (BT_FieldRefIns) instruction;
					targetField = fieldRefIns.getFieldTarget();
					if(strict) {
						checkAccess(instruction, i, targetField, method);
						BT_Class targetClass = fieldRefIns.getClassTarget();
						if(!targetClass.isStub() && !targetField.isStatic()) {
							throw new BT_IncompatibleFieldException(codeAttribute, instruction, i, targetField);
						}
					}
					if(opcode == opc_putstatic) {
						if(targetField.isFinal()) {
							if(strict) {
								BT_Class currentClass = method.getDeclaringClass();
								if(!targetField.getDeclaringClass().equals(currentClass)) {
									throw new BT_AccessException(codeAttribute, instruction, i, targetField);
								}
							}
							if(!method.isStaticInitializer()) {
								throw new BT_AccessException(codeAttribute, instruction, i, targetField);
							}
						}
					}
					break;
				case opc_new:
					if(strict) {
						BT_NewIns newIns = (BT_NewIns) instruction;
						BT_Class targetClass = newIns.getTarget();
						if(targetClass.isInterface() || targetClass.isAbstract()) {
							throw new BT_AbstractInstantiationException(codeAttribute, newIns, i, targetClass);
						}
					}
				default:
					break;
			}
		}
	}
	
	private void checkAccess(BT_Ins ins, int instructionIndex, BT_Member member, BT_Method fromMethod) throws BT_AccessException {
		//first we check to see if the member is still owned by the class (has not been removed)
		//a field might have been removed if the code accessing the field is unreachable
		if(member.cls.contains(member)) {
			if(!ins.getClassTarget().isVisibleFrom(fromMethod.cls)
				|| !member.isVisibleFrom(fromMethod.cls)) {
					throw new BT_AccessException(codeAttribute, ins, instructionIndex, member);
			}
		}
	}
	
	/**
	Eliminates dead code of the method.

	The method consists of three parts: 
		1) marking alive code 
		2) eliminating code which wasn't marked alive in the previous step
		3) removing empty exceptions (empty exception ranges - not allowed when verifying)

	@see removeDeadCode
   **/
	private boolean eliminateDeadCode() throws BT_CodePathException {
		BT_CodeVisitor visitor = new BT_CodeVisitor();
		try {
			codeAttribute.visitReachableCode(visitor);
		} catch(BT_CodeException e) {
			throw (BT_CodePathException) e;
		}
		boolean result = removeDeadCode(ins, visitor);
		boolean result2 = exceptionTableEntries.removeEmptyRanges();
		return result || result2;
	}
	
	private boolean removeDeadCode(BT_InsVector ins, BT_CodeVisitor visitor) {
		// go from the end towards the start of the method --
		// code removal won't change indices of instructions
		int toDelete = 0;
		boolean result = false;
		for(int i=ins.size() - 1; i>=0; i--) {
			if(visitor.isVisited(i)) { //the instruction is reachable
				if (toDelete > 0) {
					removeInstructionsAt(toDelete, i+1);
					result = true;
					toDelete = 0;
				}
			} else {
				toDelete++;
			}
		}
		if (toDelete > 0) {
			removeInstructionsAt(toDelete, 0);
			return true;
		}
		return result;
	}
	
	/**
	 * Optimize, then run dead code elimination.
	 *
	 * @param strict This parameter is passed to BT_CodeAttribute.optimize. 
	 * When false dead code elimination is enabled.
	 */
	boolean optimizeAndRemoveDeadCode(BT_Repository rep, boolean strict) throws BT_CodeException {
		if(strict) {
			boolean result = optimize(rep, true);
			//some optimizations can create dead code
			if(eliminateDeadCode()) {
				do {
					optimize(rep, true);
				} while(eliminateDeadCode());
				result = true;
			}
			return result;
		}
		
		boolean result = optimize(rep, false);
		if(eliminateDeadCode()) {
			do {
				optimize(rep, false);
			} while(eliminateDeadCode());
			result = true;
		}
		return result;	
	}
	
	void resolve(BT_ConstantPool pool) throws BT_AttributeException, BT_ClassWriteException {
		// Re-compute instruction offsets as we resolve them, since
		// their sizes may change during resolve processing.
		for (int n = 0; n < ins.size(); n++) {
			BT_Ins in1 = ins.elementAt(n);
			in1.resolve(codeAttribute, pool);
		}
		ins.setAllByteIndexes();
		exceptionTableEntries.removeEmptyRanges();
		int exceptionTableSize = exceptionTableEntries.size();
		if(BT_Misc.overflowsUnsignedShort(exceptionTableSize)) {
			throw new BT_AttributeException(BT_CodeAttribute.ATTRIBUTE_NAME, Messages.getString("JikesBT.{0}_count_too_large_109", "exception table entry"));
		}
		for (int n = 0; n < exceptionTableSize; n++) {
			exceptionTableEntries.elementAt(n).resolve(ins, pool);
		}
	}
	
	/**
	   Visits reachable code using visitor BT_CodeVisitor
		
		The visitReachableCode method will throw only the BT_CodePathException exception
	 	if codeVisitor is not a subclass of BT_CodeVisitor.  When the visitor is a subclass of BT_CodeVisitor
	 	then the visitor may cause other subclasses of BT_CodeException to be thrown by this method.
	 
	 **/
	void visitReachableCode(BT_CodeVisitor codeVisitor) throws BT_CodeException {
		if(ins.size() > 0) {
			Routine routine = new Routine(ins.firstElement());
			codeVisitor.initialize(codeAttribute, routine);
			codeVisitor.setUp();
			recurseVisitReachableCode(
				codeVisitor, 
				ins.firstElement(), 
				null, 
				BT_CodeVisitor.ENTRY_POINT, 
				routine, 
				null);
			codeVisitor.tearDown();
		}
	}

	/**
	 Visits reachable code starting from a particular instruction.
	
	 The visitReachableCode method will throw only the BT_CodePathException exception
	 if codeVisitor is not a subclass of BT_CodeVisitor.  When the visitor is a subclass of BT_CodeVisitor
	 then the visitor may cause other subclasses of BT_CodeException to be thrown by this method.
	 
	 @param instr the instruction at which to start.
	 @param codeVisitor the code visitor object to use
	 @param prevIns the previous instruction visited (may be null if prevInstrIndex is ENTRY_POINT)
	 @param prevInstrIndex the index of the previous instruction (may be ENTRY_POINT)
	 @see com.ibm.jikesbt.BT_CodeVisitor
	**/
	void visitReachableCode(
			BT_CodeVisitor codeVisitor, 
			BT_Ins instr, 
			BT_Ins prevIns,
			int prevInstrIndex) throws BT_CodeException {
		codeVisitor.fromMiddle = (instr != ins.firstElement());
		Routine routine = new Routine(ins.firstElement());
		codeVisitor.initialize(codeAttribute, routine);
		codeVisitor.setUp();
		recurseVisitReachableCode(
			codeVisitor, 
			instr, 
			prevIns, 
			prevInstrIndex, 
			routine, 
			null);
		codeVisitor.tearDown();
	}
	
	/**
	 Visits reachable code within the given subroutine, starting from the given instruction.
	 Both the visitor and the subroutine will be updated as visiting takes place.
	
	 The visitReachableCode method will throw only the BT_CodePathException exception
	 if codeVisitor is not a subclass of BT_CodeVisitor.  When the visitor is a subclass of BT_CodeVisitor
	 then the visitor may cause other subclasses of BT_CodeException to be thrown by this method.
	 
	 @param instr the instruction at which to start.
	 @param codeVisitor the code visitor object to use
	 @param prevIns the previous instruction visited (may be null)
	 @param prevInstrIndex the index of the previous instruction
	 @param subroutine the subroutine within which the instruction lies
	 @throws BT_CodeException
	 @see com.ibm.jikesbt.BT_CodeVisitor
	**/
	void visitReachableCode(
			BT_CodeVisitor codeVisitor, 
			BT_Ins instr, 
			BT_Ins prevIns,
			int prevInstrIndex,
			SubRoutine sub) throws BT_CodeException {
		codeVisitor.fromMiddle = (instr != ins.firstElement());
		codeVisitor.addSubRoutine(sub);
		codeVisitor.setCurrentSubRoutine(sub);
		codeVisitor.initialize(codeAttribute, new Routine(ins.firstElement()));
		codeVisitor.setUp();
		recurseVisitReachableCode(
			codeVisitor, 
			instr, 
			prevIns, 
			prevInstrIndex, 
			sub, 
			null);
		codeVisitor.tearDown();
	}

	private void recurseVisitReachableCode(
			BT_CodeVisitor codeVisitor, 
			BT_Ins instr, 
			BT_Ins prevIns,
			int prevInstrIndex,
			Routine routine,
			BT_ExceptionTableEntry handler) throws BT_CodeException {
		int instructionIndex = ins.indexOf(instr);
		while(true) {
			if(codeVisitor.exited()) {
				return;
			}
			if (codeVisitor.isVisited(instructionIndex)) {// We've been here before
				codeVisitor.additionalVisit(instr, instructionIndex, prevIns, prevInstrIndex, handler);
				return;
			}
			boolean continueToNext = codeVisitor.visit(instr, instructionIndex, prevIns, prevInstrIndex, handler); 
			codeVisitor.markVisited(instructionIndex); // Mark that we've been here
			routine.markVisited(instr, instructionIndex); // Mark that we've been here
			
			if(!continueToNext || codeVisitor.exited()) {
				return;
			}
			handler = null;
			//visit any visitable exception handlers
			BT_ExceptionTableEntry triggeredHandlers[] = 
				codeVisitor.getTriggeredExceptionHandlers(instructionIndex);
			for(int n=0; n<triggeredHandlers.length; n++) {
				BT_ExceptionTableEntry tableEntry = triggeredHandlers[n];
				
				
				if(!codeVisitor.visitHandler(tableEntry, instructionIndex)) {
					continue;
				}
				if(!codeVisitor.revisitHandlers) {
					codeVisitor.removeVisitableHandler(tableEntry);
				}
				
				//We need to figure out which subroutine this handler lies within.
				//since the current subroutine might lie entirely within the handler's range,
				//or the handler's range might encompass the entire subroutine.
				Routine handlerRoutine = codeVisitor.getEncompassingRoutine(tableEntry, routine);
				
				SubRoutine current = codeVisitor.getCurrentSubRoutine();
				if(handlerRoutine instanceof SubRoutine) {
					codeVisitor.setCurrentSubRoutine((SubRoutine) handlerRoutine);
				}
				
				recurseVisitReachableCode(
					codeVisitor, 
					tableEntry.handlerTarget, 
					instr, 
					instructionIndex, 
					handlerRoutine, 
					tableEntry);
				
				codeVisitor.setCurrentSubRoutine(current);
				
			}
			if(instr.hasNoSuccessor()) {
				if (instr.isRetIns()) {
					routine.markRet(instr);
					if(codeVisitor.fromMiddle && routine == codeVisitor.mainRoutine) {
						/* we do not know where the visitor may go from this point onwards,
						 * because we started visiting in the middle of the code.
						 */
						BT_InsVector nextInstructions = findNextInstructions((BT_RetIns) instr);
						
						// now we know where the current ret instruction can go, so we go there
						for(int i=0; i<nextInstructions.size(); i++) {
							BT_Ins next = nextInstructions.elementAt(i);
							recurseVisitReachableCode(codeVisitor, next, instr, instructionIndex, routine, null);
						}
					}
				} else if (instr.isAThrowIns()) {
					routine.markThrow();
				} else if (instr.isReturnIns()) {
					routine.markReturn();
				}
				return;
			}
			if (instr.isSwitchIns()) {
				BT_SwitchIns s = (BT_SwitchIns) instr;
				BT_Ins targets[] = s.getAllTargets();
				for (int k = 0; k < targets.length; k++) {
					recurseVisitReachableCode(
						codeVisitor, 
						targets[k], 
						instr,
						instructionIndex,
						routine,
						null);
				}
				return;
			}
			if (instr.isJumpIns()) {
				BT_JumpIns jumpIns = (BT_JumpIns) instr;
				BT_Ins targetIns = jumpIns.getTarget();
				if (instr.isGoToIns()) {
					prevIns = instr;
					instr = targetIns;
					prevInstrIndex = instructionIndex;
					instructionIndex = ins.indexOf(instr);
					// Continue at the target instruction
					continue;
				} 
				if (instr.isJSRIns()) {
					BT_Ins startInstruction = getSubroutineStartInstruction(targetIns);
					SubRoutine jsr = codeVisitor.getSubRoutine(startInstruction);
					if(!routine.jumpTo(jsr, jumpIns)) {
						throw new BT_CircularJSRException(codeAttribute, instr, instructionIndex);
					}
					
					SubRoutine current = codeVisitor.getCurrentSubRoutine();
					codeVisitor.setCurrentSubRoutine(jsr);
					
					recurseVisitReachableCode(
						codeVisitor, 
						targetIns, 
						instr, 
						instructionIndex, 
						jsr, 
						null);
					
					codeVisitor.setCurrentSubRoutine(current);
					
					BT_InsVector retInstructions = jsr.retInstructions;
					if(retInstructions != null && retInstructions.size() > 0) {
						int nextIndex = instructionIndex + 1;
						if(nextIndex >= ins.size()) {
							throw new BT_CodePathException(codeAttribute, instr, instructionIndex);
						}
						BT_Ins nextInstruction = ins.elementAt(nextIndex);
						for(int i=0; i<retInstructions.size(); i++) {
							BT_Ins retInstruction = retInstructions.elementAt(i);
							recurseVisitReachableCode(
								codeVisitor, 
								nextInstruction, 
								retInstruction, 
								ins.indexOf(retInstruction), 
								routine, 
								null);
						}
					}
					return;
				} else {
					// this is a conditional jump
					recurseVisitReachableCode(
						codeVisitor, 
						targetIns, 
						instr, 
						instructionIndex, 
						routine, 
						null);
				}
			}
			prevInstrIndex = instructionIndex;
			prevIns = instr;
			int nextIndex = instructionIndex + 1;
			if(nextIndex >= ins.size()) {
				throw new BT_CodePathException(codeAttribute, instr, instructionIndex);
			}
			instructionIndex = nextIndex;
			instr = ins.elementAt(instructionIndex);
		}
	}
	
	private BT_InsVector findNextInstructions(BT_RetIns instr) throws BT_CodePathException {
		if(codeAttribute == null) {
			throw new IllegalStateException();
		}
		class FindNextInstructionVisitor extends BT_CodeVisitor {
			BT_RetIns retIns;
			BT_InsVector nextInstructions = new BT_InsVector();
			
			FindNextInstructionVisitor(BT_RetIns retIns) {
				this.retIns = retIns;
			}
			
			protected boolean visit(
					BT_Ins ins, 
					int iin, 
					BT_Ins previousInstruction, 
					int prev_iin, 
					BT_ExceptionTableEntry handler) {
					if(previousInstruction == retIns) {
						nextInstructions.addElement(ins);
					}
					return true;
				}
			
			protected void additionalVisit(BT_Ins ins, int iin, BT_Ins previousInstruction, int prev_iin, BT_ExceptionTableEntry handler) {
				if(previousInstruction == retIns) {
					nextInstructions.addElement(ins);
				}
			}
		}
		
		FindNextInstructionVisitor findNextInstructionVisitor = new FindNextInstructionVisitor(instr);
		try {
			visitReachableCode(findNextInstructionVisitor);
			return findNextInstructionVisitor.nextInstructions;
		} catch(BT_CodeException e) {
			throw (BT_CodePathException) e;
		}
	}
	
	public String toString() {
		return Messages.getString("JikesBT.<BT_CodeAttribute_with_{0}_instructions>_85", ins.size());
	}
	
	void write(DataOutputStream dos, BT_ConstantPool pool, int codeLen) throws IOException {
		dos.writeInt(codeLen);
		int bytesWritten = dos.size();
		for (int n = 0; n < ins.size(); n++) {
			ins.elementAt(n).write(dos, codeAttribute, pool);
		}
		int codeBytesWritten = dos.size() - bytesWritten;
		if(codeLen != codeBytesWritten) { //should never reach here
			throw new RuntimeException("calculated code size and actual code size do not match");
		}
		// Write the exception-table that describes try-catch blocks in the code.
		// Note that this is different from the "Exceptions" attribute that belongs to the method.
		int excSize = exceptionTableEntries.size();
		dos.writeShort(excSize);
		for (int n = 0; n < excSize; n++) {
			exceptionTableEntries.elementAt(n).write(dos, pool);
		}
	}
	
	/**
	 Prints the instructions and side information.
	 Has a side-effect of resetting all instructions' offsets unless
	 {@link BT_Misc#PRINT_ZERO_OFFSETS} is specified.
	
	 @param  printFlag  Should be 0 or {@link BT_Misc#PRINT_ZERO_OFFSETS} that can be
	   used to make comparing files easier.
	   Other bits are ignored.
	   @throws BT_CodeException if the max stack or max locals are being printed and one or the other could not be calculated.
	**/
	void print(PrintStream ps, int printFlag, BT_SourceFile source) throws BT_CodeException {
		ins.setAllByteIndexes();
		
		
		for (int n = 0, nLabels = 0; n < ins.size(); n++) {
			BT_Ins in1 = ins.elementAt(n);
			if(in1.isBlockMarker()) {
				((BT_BasicBlockMarkerIns)in1).setLabel("label_"+(nLabels++));
			}	
		}
		
		if ((printFlag & BT_Misc.PRINT_ZERO_OFFSETS) != 0) {
			// Print instruction 0s instead of offsets
			for (int n = 0; n < ins.size(); n++) {
				BT_Ins in1 = ins.elementAt(n);
				int save = in1.byteIndex; // Save
				in1.setByteIndex(0); // Temp change (ugh)
				ps.println("\t" + in1);
				in1.setByteIndex(save); // Restore
			}
		} else if ((printFlag & BT_Misc.PRINT_IN_ASSEMBLER_MODE) != 0) {
			for (int lastLineNumber = 0, n = 0; n < ins.size(); n++) {
				BT_Ins in1 = ins.elementAt(n);
				if (in1.isBlockMarker()) {
					ps.println("\t" + in1.toAssemblerString(codeAttribute));
				}
				else {
					ps.println("\t\t" + in1.toAssemblerString(codeAttribute));
					if(source != null) {
						int lineNumber = codeAttribute.findLineNumber(in1);
						if(lineNumber > 0 && lineNumber != lastLineNumber) {
							codeAttribute.printSourceLine(ps, lineNumber, source);
							lastLineNumber = lineNumber;
						}
					}
				}
				
			}
		} else {
			for (int n = 0; n < ins.size(); n++) {
				BT_Ins in1 = ins.elementAt(n);
				ps.println("\t\t" + n+"\t"+in1);
			}
		}

		for (int n = 0; n < exceptionTableEntries.size(); n++) {
			if ((printFlag & BT_Misc.PRINT_IN_ASSEMBLER_MODE) != 0) {
				ps.println("\t\t" + exceptionTableEntries.elementAt(n).toAssemblerString());
			} else {
				ps.println("\t\t" + exceptionTableEntries.elementAt(n));
			}
		}
	}
	
	BT_ClassVector getVerifierRequiredClasses(BT_Method method, BT_StackCell shapes[][]) {
		BT_Class javaLangThrowable = method.cls.getRepository().findJavaLangThrowable();
		BT_HashedClassVector requiredClasses = new BT_HashedClassVector();
		BT_Class returnType = method.getSignature().returnType;
		for(int i = 0; i < ins.size(); i++ ) {
			BT_Ins instruction = ins.elementAt(i);
			BT_StackCell[] stackShapes = shapes[i];
			if( stackShapes == null || stackShapes.length <= 0 ) {
				/* stackShapes is null if an instruction was never visited because it is unreachable */
				continue;
			}
			
			// void foo() {
			//     throw new subclassOfThrowable();
			// }
			if(instruction.isAThrowIns()) {
				ClassType stackClassType = stackShapes[stackShapes.length - 1].getClassType();
				if(requiresClassCheck(method, javaLangThrowable, stackClassType)) {
					requiredClasses.addUnique(stackClassType.getType());
				}
			}
			
			// return case is as follows: 
			// 
			// A getA() {
			//     return new subclassOfA();
			// }
			//
			else if(instruction.isReturnIns() ) {
				if(!returnType.isPrimitive()) {
					ClassType stackClassType = stackShapes[stackShapes.length - 1].getClassType();
					if(requiresClassCheck(method, returnType, stackClassType)) {
						requiredClasses.addUnique(stackClassType.getType());
					}
				}
        	}
        	
        	// field case is as follows: 
			// A a;
			// void foo() {
			//     a = new subclassOfA();
			// }
			//
			else if(instruction.isFieldAccessIns()) {
				BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) instruction;
				if(fieldRefIns.isFieldWriteIns()) {
					BT_Field target = fieldRefIns.getFieldTarget();
					BT_Class fieldType = target.getFieldType();
					if(!fieldType.isPrimitive()) {
						ClassType stackClassType = stackShapes[stackShapes.length - 1].getClassType();
						if(requiresClassCheck(method, fieldType, stackClassType)) {
							requiredClasses.addUnique(stackClassType.getType());
						}
					}
					if(!fieldRefIns.isStaticFieldAccessIns()) {
						BT_StackType accessedObjectType = stackShapes[stackShapes.length - fieldType.getSizeForLocal() - 1].getCellType();
						if(BT_StackShapes.isAllowedUninitializedFieldAccess(accessedObjectType, fieldRefIns)) {
							//Nothing to do, the class of the owner accessed field must match the class being constructed
						} else {
							ClassType stackClassType = accessedObjectType.getClassType();
							if(requiresClassCheck(method, fieldRefIns.getResolvedClassTarget(codeAttribute), stackClassType)) {
								requiredClasses.addUnique(stackClassType.getType());
							}
						}
					}
				} else if(!fieldRefIns.isStaticFieldAccessIns()) {
					ClassType stackClassType = stackShapes[stackShapes.length - 1].getClassType();
					if(requiresClassCheck(method, fieldRefIns.getResolvedClassTarget(codeAttribute), stackClassType)) {
						requiredClasses.addUnique(stackClassType.getType());
					}
				}
        	}
	
			// method arguments case:
			//
			// void foo(A a) {
			//     foo(new subclassOfA());
			// }
			
			else if(instruction.isInvokeIns() ) {
				
				BT_Method invokedMethod = ((BT_MethodRefIns) instruction).getMethodTarget();
				BT_ClassVector types = invokedMethod.getSignature().types;
				int typesSize = types.size();
				int stackIndex = stackShapes.length;
				for(int signatureIndex = typesSize - 1; signatureIndex >= 0; signatureIndex--) {
					BT_Class type = types.elementAt(signatureIndex);
					stackIndex -= type.getSizeForLocal();
					if(!type.isPrimitive()) {
						continue;
					}
					ClassType classType = stackShapes[stackIndex].getClassType();
					if(requiresClassCheck(method, type, classType)) {
						requiredClasses.addUnique(classType.getType());
					}
				} // for( int t ...
				if(instruction.isInvokeVirtualIns() || instruction.isInvokeInterfaceIns()) {
					BT_StackCell stackCell = stackShapes[--stackIndex];
					BT_StackType stackType = stackCell.getCellType();
					if(stackType.isClassType() /* should always be true */) {
						ClassType classType = stackCell.getClassType();
						if(requiresClassCheck(method, instruction.getResolvedClassTarget(codeAttribute), classType)) {
							requiredClasses.addUnique(classType.getType());
						}
					}
				}
			}
		} // for( int i...
		return requiredClasses;
	}
	
	private boolean requiresClassCheck(BT_Method method, BT_Class toType, ClassType fromType) {
        return !fromType.isNull() 
        	&& isWideningCastTarget(toType)
        	&& wideningCastRequiresClassLoad(method, fromType.getType(), toType);
    }
	
	/**
	 * determines whether the verifier will decide that a check is required to determine
	 * if a widening cast is allowed, or if it will be rejected by the verifier.
	 * 
	 * @param intendedType
	 * @param javaLangObject
	 * @return
	 */
	boolean isWideningCastTarget(BT_Class intendedType) {
		if(intendedType.isArray()) {
			BT_Class elementType = intendedType.getElementClass();
			return isWideningCastTarget(elementType);
		}
		return !intendedType.isPrimitive() && !intendedType.isJavaLangObject();
	}
	
	/**
	 * determines whether the verifier will decide that a check is required to determine
	 * if a widening cast is allowed, or if it will be rejected by the verifier..
	 * @param stackType
	 * @param intendedType
	 * @return
	 */
	boolean wideningCastRequiresClassLoad(BT_Method method, BT_Class stackType, BT_Class intendedType) {
		/* check if both classes are loaded */
		/* If both classes are either the same or a superclass of the class being verified (which is the class
		 * which declares the method which owns this code attribute), then they have both been loaded.
		 */
		if(intendedType.equals(stackType)) {
			return false;
		}
		BT_Class declaringClass = method.getDeclaringClass();
		if((stackType.equals(declaringClass) || stackType.isClassAncestorOf(declaringClass))
				&& (intendedType.equals(declaringClass) || intendedType.isClassAncestorOf(declaringClass))) {
			return false;
		}
		return true;
	}

	BT_Ins getSubroutineStartInstruction(BT_Ins target) {
		int targetIndex = ins.indexOf(target);
		while(target.isBlockMarker()) {
			if(++targetIndex == ins.size()) {//do not go past the end
				break;
			}
			target = ins.elementAt(targetIndex);
		}
		return target;
	}
	
	static final JSRJump noJumps[] = new JSRJump[0];
	static final SubRoutine noSubRoutine[] = new SubRoutine[0];
	
	static class JSRJump {
		BT_JumpIns jsrInstruction;
		SubRoutine target;
		
		JSRJump(BT_JumpIns jsrInstruction, SubRoutine target) {
			this.jsrInstruction = jsrInstruction;
			this.target = target;
		}
	}
	
	/**
	 * This class represents the current instruction vector as a series
	 * of executable instructions.  It is used by the visitReachableCode
	 * methods.
	 * A routine starts with a start instruction, and encompasses all instructions
	 * reachable from that start instruction, as well as all instructions that can
	 * be found in-between reachable instructions.  Therefore, it is a continuous sequence of
	 * instructions, the boundaries of the sequence reachable from a given start instruction.
	 */
	class Routine {
		boolean rets;
		boolean returns;
		boolean throwz;
		BT_Ins endInstruction;
		BT_Ins startInstruction; /* this is the first non-block marker at the start of the routine */
		private BT_InsVector circularJSRInstructions;
		int startIndex;
		private int endIndex; 
		LinkedList nestedSubRoutines; //contains JSRJump objects
		
		Routine(BT_Ins startInstruction) {
			this.startInstruction = startInstruction;
			this.startIndex = ins.indexOf(startInstruction);
			this.endInstruction = startInstruction;
		}
		
		/**
		 * we make this JSR object aware that a nested JSR instruction has been inlined 
		 * by calling this method
		 */
		void updateNestedSubRoutines() {
			if(nestedSubRoutines == null) {
				return;
			}
			for(int i=nestedSubRoutines.size() - 1; i >= 0; i--) {
				JSRJump jump = (JSRJump) nestedSubRoutines.get(i);
				BT_Ins jsrIns = jump.jsrInstruction;
				if(!ins.contains(jsrIns)) {
					nestedSubRoutines.remove(i);
				} else {
					jump.target.updateNestedSubRoutines();
				}
			}
		}
		
		JSRJump[] getNestedJSRJumps() {
			if(nestedSubRoutines == null) {
				return noJumps;
			}
			return (JSRJump[]) nestedSubRoutines.toArray(new JSRJump[nestedSubRoutines.size()]);
		}
		
		void getNestedSubRoutines(ArrayList nested) {
			for(int i=0; i<nestedSubRoutines.size(); i++) {
				JSRJump jump = (JSRJump) nestedSubRoutines.get(i);
				SubRoutine subRoutine = jump.target;
				if(!nested.contains(subRoutine)) {
					nested.add(subRoutine);
					if(subRoutine.nestedSubRoutines != null) {
						subRoutine.getNestedSubRoutines(nested);
					}
				}
			}
		}
		
		/** 
		 * @return an array of all subroutines reachable from this routine
		 */
		public SubRoutine[] getNestedSubRoutines() {
			if(nestedSubRoutines == null) {
				return noSubRoutine;
			}
			ArrayList nested = new ArrayList();
			getNestedSubRoutines(nested);
			return (SubRoutine[]) nested.toArray(new SubRoutine[nested.size()]);
		}
		
		void markThrow() {
			throwz = true;
		}
		
		void markReturn() {
			returns = true;
		}
		
		void markRet(BT_Ins in) {
			/*
			 * Normally when visiting from the start of the code, a ret should not be encountered
			 * in the original main routine.
			 * But when visiting from the middle of code, there is no way to know that visiting
			 * has begun within a subroutine and therefore a ret can be encountered in the
			 * main routine.
			 */
		}
		
		/**
		 * Mark the instruction as visited. Note that no instruction will be visited more than one time.
		 **/
		void markVisited(BT_Ins ins, int instructionIndex) {
			if(instructionIndex > endIndex) {
				endInstruction = ins;
				endIndex = instructionIndex;
			}
		}
		
		/**
		 * @return if the the subroutine or a nested subroutine is me
		 */
		boolean isCircular(SubRoutine sub) {
			if(sub.startInstruction == startInstruction) {
				return true;
			}
			if(sub.nestedSubRoutines != null) {
				for(int i=0; i<sub.nestedSubRoutines.size(); i++) {
					JSRJump jump = (JSRJump) sub.nestedSubRoutines.get(i);
					SubRoutine nested = jump.target;
					if(isCircular(nested)) {
						return true;
					}
				}
			}
			return false;
		}
		
		/**
		 * @param jsrIns the jsr instruction
		 * @param target the targetted subroutine
		 * @return whether the jump is permissible
		 */
		boolean jumpTo(SubRoutine target, BT_JumpIns jsrIns) {
			if(isCircular(target)) {
				if(circularJSRInstructions == null) {
					circularJSRInstructions = new BT_InsVector(1);
				}
				circularJSRInstructions.addElement(jsrIns);
				return false;
			}
			if(nestedSubRoutines == null) {
				nestedSubRoutines = new LinkedList();
			}	
			nestedSubRoutines.add(new JSRJump(jsrIns, target));
			return true;
		}
		
		public boolean equals(Object o) {
			return (o instanceof Routine) && startInstruction == ((Routine) o).startInstruction;
		}
		
		public String toString() {
			return "start: " + startInstruction + '\n'
				+ "end: " + endInstruction;
		}
	}
	
	SubRoutine createSubRoutine(BT_Ins startInstruction) {
		return new SubRoutine(startInstruction);
	}
	
	/**
	 * represents a subset of the current instruction vector as
	 * a series of executable instructions.  The subroutine is exited
	 * when control reaches a throw, return or ret instruction.
	 * In the case of a throw instruction, control may be returned to the subroutine
	 * if the exception is caught within it.  An exception is considered to be
	 * caught within a subroutine if all instructions within the exception handler's
	 * range lie within the subroutine.
	 */
	public class SubRoutine extends Routine {
		BT_InsVector retInstructions;
		
		SubRoutine(BT_Ins startInstruction) {
			super(getSubroutineStartInstruction(startInstruction));
		}
		
		void markRet(BT_Ins in) {
			if(retInstructions == null) {
				retInstructions = new BT_InsVector(3);
			} else if(retInstructions.contains(in)) {
				return;
			}
			rets = true;
			retInstructions.addElement(in);
		}
		
		boolean isTargetOf(BT_JumpIns jsrIns) {
			return getSubroutineStartInstruction(jsrIns.getTarget()) == startInstruction;
		}
		
		boolean retsAt(BT_Ins ins) {
			return 
				ins.isRetIns() && retInstructions != null && retInstructions.contains(ins);
		}
		
		public boolean equals(Object o) {
			return (o instanceof SubRoutine) && super.equals(o);
		}
	}
}
