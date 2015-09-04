package com.ibm.jikesbt;

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.jikesbt.BT_ObjectCode.SubRoutine;
import com.ibm.jikesbt.BT_LocalVariableAttribute.GenericLocalVariable;
import com.ibm.jikesbt.BT_LocalVariableAttribute.LV;
import com.ibm.jikesbt.BT_LocalVariableAttribute.LocalVariable;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

/**
 * Class to inline a called method into a calling method or to inline a jsr.
 * <p>
 * In the following example, addTwo can be inlined:
 * <pre>
 *    int addTwo(int value) {
 *        return value + 2;
 *    }
 *    void foo() {
 *        int x = addTwo(5);
 *    }
 * <pre>
 * The result will be:
 * <pre>
 *    void foo() {
 *        int x = 5 + 2;
 *    }
 * </pre>
 * If the method is optimized by calling {@link BT_CodeAttribute.optimize}, the
 * instructions "5 + 2" will be rewritten by 7. As the store into "x" is directly
 * followed by a return, this instruction will also be optimized away. The result
 * is:
 * <pre>
 *     void foo() {
 *     }
 * </pre>
 * More importantly, that call to foo itself will also be inlined, thereby removing
 * quite a few method calls.
 * <p>
 * If tools
 * that removed unused methods (like Jax) are used, the addTwo and foo methods can be
 * removed from the hosting class to reduce its size.
 * <p>
 * It is the responsibility of the user of this class to determine if it is
 * legal to inline a give method call (observing Java visibility rules and
 * subtle side effects such as class loading/initialization order).
 */
public final class BT_Inliner extends BT_Base implements BT_Opcodes {
	/**
	 * inline the specified method at the given call site instruction
	 * 
	 * @param code the code to be inlined
	 * @param callerCode the code to be inlined into
	 * @param replacedInstr the call site instruction to be inlined
	 * @param alterTargetClass whether the target class can be altered (to preserve initialization behaviour)
	 * @param provokeInitialization preserve initialization behaviour - calling a static method provokes initialization
	 * of the target class, so when the method is inlined initialization must be provoked by accessing a static field
	 */
	static boolean inline (
		BT_CodeAttribute code,
		BT_CodeAttribute callerCode,
		BT_Ins replacedInstr,
		boolean alterTargetClass,
		boolean provokeInitialization) 
			throws BT_CodeException {
		//  added local overlaying, missing null check and fixed attribute reference updating
		//  fixed local overlaying for inlining methods at non-callsites.
		//  added inlining of methods with exception handlers
		//  added inlining of local variable and line number table attributes

		BT_Method caller = callerCode.getMethod(); 
		BT_Method codeMethod = code.getMethod();
		
		BT_AttributeVector referencingAttributes;
		BT_ClassVector parameterTypes;
		if(codeMethod == null) {
			//TODO we'll just assume that there are no parameters, but a liveness analysis would be better here
			parameterTypes = BT_ClassVector.emptyVector;
			referencingAttributes = BT_AttributeVector.emptyVector;	
		} else {
			// Disallow recursive calls
			if (codeMethod == caller) {
				return false;
			}
			parameterTypes = codeMethod.getSignature().types;
			referencingAttributes = codeMethod.referencingAttributes;
			if(referencingAttributes == null) {
				referencingAttributes = BT_AttributeVector.emptyVector;
			}
		}
		
		BT_InsVector callerIns = callerCode.getInstructions();
		BT_InsVector codeIns = code.getInstructions();
		
		int callerStartIndex = callerIns.indexOf(replacedInstr);
		if (callerStartIndex == -1) {
			return false;
		}

		if (code.getExceptionTableEntryCount() != 0
			&& callerCode.computeStackDepth(callerStartIndex)
				+ replacedInstr.getPoppedStackDiff()
				!= 0) {
			return false;
		}
		
		if(replacedInstr.isInvokeInterfaceIns() && codeMethod == null) {
			//we need to know what type of object is being invoked upon so we
			//can insert a cast that will allow the verifier to know what the 
			//"this" local type is
			return false;
		}
				
		/* handle any explicit intialization that is required to preserve the same behaviour */
		BT_Class classRequiringInitilization = null;
		BT_Field initializingField = null;
		if(codeMethod != null && codeMethod.isStatic() && !caller.cls.isDerivedFrom(codeMethod.cls)) {
			classRequiringInitilization = requiresInitialization(codeMethod.cls); 
			
			if(classRequiringInitilization != null) {
				if(!provokeInitialization) {
					return false;
				}
				BT_Class cls = codeMethod.cls;
				initializingField = findInitializingStaticField(
						classRequiringInitilization,
						cls, 
						caller.cls);
				if(initializingField == null) {
					if(!alterTargetClass) {
						return false;
					}
					
					//we will add a dummy field in the class
					 String dummyName = "_";
					 BT_Field f;
					 while ((f =
						 cls.findInheritedField(dummyName, cls.getRepository().getInt(), true))
						 != null
						 && (!f.isStatic() || f.cls == cls)) {
						 dummyName += '_';
					 }
					initializingField =
						 BT_Field.createField(
							 cls,
							 (short) (BT_Field.PUBLIC | BT_Field.STATIC),
							 cls.getRepository().getInt(),
							 dummyName);
				}
				
			}
			
		}
		
		int localsInc = getLocalsInUse(callerCode, replacedInstr, callerStartIndex);

		// Inline instruction range and related code attributes
		int callerEndIndex =
			inlineInstructionRange(
				code,
				0,
				codeIns.size(),
				callerCode,
				callerStartIndex,
				localsInc);
		
//		 Can only inline line numbers if caller and callee in same source file right now
		boolean inSameSourceFile =
			caller != null
				&& caller.cls != null
				&& codeMethod != null
				&& codeMethod.cls != null
				&& caller.cls.getSourceFile() != null
				&& caller.cls.getSourceFile().equals(codeMethod.cls.getSourceFile());

		
		
		inlineAttributes(
			code,
			0,
			codeIns.size(),
			callerCode,
			callerStartIndex,
			callerEndIndex,
			localsInc,
			inSameSourceFile);

		//TODO add monitorEnters and monitorExits for synchronized methods
		//monitorEnter must occur after initialization for static invokes
		//it must occur after the null check for non-static
		//ie in general it is the last thing to be done
		//PROBLEM if an exception is thrown, must have a finally clause that releases the acquired monitor!!
		//for static invokes must acquire the monitor's object by doing a Class.forName following an ldc on the class name
		//the ldc will not generally add to the constant pool size because the class name always appears inside the constant pool already because of the this_class entry
		//specified in the class file format
		//for non-static, the object's location on the stack is determined by the signature...  for the inlining we move it to
		//a local variable along with all other arguments, so we can access it by an aload
		
		//currently, synchronized methods cannot be inlined, however in this class we operate at the code
		//attribute/bytecode level so we do not check for this, we need to add a boolean argument to the inline
		//method to indicate synchronized
		
		
		// Insert instructions for argument store's into locals
		if (codeMethod != null && codeMethod.isStatic()) {
			
			// force target class to be loaded and initialized if necessary
			if(classRequiringInitilization != null) {
				BT_Ins ins = BT_Ins.make(BT_Ins.opc_pop);
				callerIns.insertElementAt(
					ins, callerStartIndex);
				ins.link(callerCode);
				//ins = BT_Ins.make(BT_Ins.opc_getstatic, dummy);
				ins = BT_Ins.make(BT_Ins.opc_getstatic, initializingField);
				callerIns.insertElementAt(ins, callerStartIndex);
				ins.link(callerCode);
			}
		} else {
			if ((replacedInstr.isInvokeVirtualIns()
				|| replacedInstr.isInvokeInterfaceIns()
				|| replacedInstr.isInvokeSpecialIns())
				) {
					
				// insert a null check instruction sequence after all stores, if required
				BT_Ins instr =  BT_Ins.make();
				callerIns.insertElementAt(instr, callerStartIndex);
				instr.link(callerCode);
				BT_Ins ins = BT_Ins.make(BT_Ins.opc_athrow);
				callerIns.insertElementAt(
					ins,
					callerStartIndex);
				ins.link(callerCode);
				ins = BT_Ins.make(BT_Ins.opc_aconst_null);
				callerIns.insertElementAt(
					ins,
					callerStartIndex);
				ins.link(callerCode);
				ins = BT_Ins.make(BT_Ins.opc_ifnonnull, (BT_BasicBlockMarkerIns) instr);
				callerIns.insertElementAt(
					ins,
					callerStartIndex);
				ins.link(callerCode);
				
				//end of inserting the null check
				
				// insert the store
				// we will store the "this" object here,
				// and below we will store all the method arguments
				ins = BT_Ins.make(BT_Ins.opc_astore, localsInc++);
				callerIns.insertElementAt(ins, callerStartIndex);
				ins.link(callerCode);
				ins = BT_Ins.make(BT_Ins.opc_dup);
				callerIns.insertElementAt(ins, callerStartIndex);
				ins.link(callerCode);
				
				// If it is an invokeinterface, a checkcast must be inserted, just in case 
				// we are making a bad call about what method to inline.
				if (replacedInstr.isInvokeInterfaceIns()) {
					BT_Ins castIns = BT_Ins.make(BT_Opcodes.opc_checkcast, codeMethod.cls);
					callerIns.insertElementAt(
						castIns,
						callerStartIndex);
					castIns.link(callerCode);
				}
			} else {
				// insert just the store
				// we will store the "this" object here,
				// and below we will store all the method arguments
				BT_Ins instr = BT_Ins.make(BT_Ins.opc_astore, localsInc++);
				instr.link(callerCode);
				callerIns.insertElementAt(instr, callerStartIndex);
			}
		}
		
		/**
		 * The arguments to the inlined method invocation need to be moved into the local variable array where
		 * the inlined code will be expecting to see them.
		 */
		for (int k = 0; k < parameterTypes.size(); k++, localsInc++) {
			int opcode = parameterTypes.elementAt(k).getOpcodeForStore();
			BT_Ins instr = BT_Ins.make(opcode, localsInc);
			callerIns.insertElementAt(instr, callerStartIndex);
			instr.link(callerCode);
			if (opcode == opc_lstore || opcode == opc_dstore)
				localsInc++;
		}

		// Change (line number) attribute references to new instruction
		callerCode.changeReferencesFromTo(
			replacedInstr,
			callerIns.elementAt(callerStartIndex),
			true /* recursive inlining is disallowed */);

		//if the inlined method was an enclosing method for some class, 
		//then that is no longer true: there is another method instantiating the class
		for(int i=0; i<referencingAttributes.size(); i++) {
			BT_Attribute att = referencingAttributes.elementAt(i);
			if(att instanceof BT_EnclosingMethodAttribute) {
				//by removing this method it will no longer enclose any classes
				BT_EnclosingMethodAttribute encAtt = (BT_EnclosingMethodAttribute) att;
				encAtt.getOwner().getAttributes().removeElement(encAtt);
			}
		}
		
		// Recompute each instruction's byteIndex
		callerCode.computeMaxInstructionSizes();

		//Reset the max stack and max locals values to be recalculated later
		callerCode.resetCachedCodeInfo();
		

		return true;
	}
	
	private static int getLocalsInUse(BT_CodeAttribute code, BT_Ins instruction, int iin) 
			throws BT_CodeException {
		int result;
		
		/* some VMs will make assumptions about what types exist in each of the locals that are
		 * a parameter, for instance with checkcast instructions.  So we cannot reuse such locals.
		 * 
		 * For instance:
		 * 
		 void bla(String s) {
		 	foo(); 
		 }
		 void foo(Throwable t) {
		 	if(t instanceof Error) {
		 		throw t;
		 	} else {
		 		handleException((Exception) t);
		 	}
		 }
		 
		 void handleException(Exception e) { ...}
		 * 
		 * If we inline foo into bla, then it is conceivable that we reuse the local variables
		 * reserved for the String s in order to store the Throwable t.  However, the VM will
		 * see that the parameter in the method signature for that local variable is a String 
		 * and so it will remove the instanceof check for Error, assuming that it could not possibly
		 * be an error.
		 * The call to handleException will remain.  
		 * This will end up causing a checkcast exception in the cases where the Throwable t is in fact
		 * an error and the checkcast verifies whether it is an Exception.
		 * 
		 * To prevent this from happening, we should not reuse any of the locals used for parameters (String s in this case,
		 * which is local variable 1).
		 * 
		 * In the future we could refine this by checking within the code for any type checks on parameter locals.
		 */
		BT_Method codeMethod = code.getMethod();
		BT_MethodSignature signature = codeMethod.getSignature();
		int numParams = signature.getArgsSize();
		if(!codeMethod.isStatic()) {
			numParams++;
		}
		
		int maxLocalsAtInstruction = code.getMaxLocalsQuickly();
		if(maxLocalsAtInstruction == 0 || maxLocalsAtInstruction <= numParams) {
			result = 0;
		} else {
			BT_LocalsInUseVisitor vis = new BT_LocalsInUseVisitor(code, BT_StackPool.pool, iin, maxLocalsAtInstruction);
			vis.setKnownMaxLocalsAndExitEarly(maxLocalsAtInstruction);
			vis.populate();
			result = vis.getHighestLocalNotInUse();
			vis.returnStacks();
		} 
		result = Math.max(result, numParams);
		return result;
	}

	static boolean inlineJsrs(BT_CodeAttribute code) throws BT_CodeException {
		ArrayList jsrList = null;
		HashMap subroutines = null;
		BT_InsVector methodIns = code.getInstructions();
		for(int j=0; j<methodIns.size(); j++) {
			BT_Ins instr = methodIns.elementAt(j);
			if (!instr.isJSRIns()) {
				continue;
			}
			if(jsrList == null) {
				jsrList = new ArrayList();
				subroutines = new HashMap();
			}
			
			BT_Ins target = code.getSubroutineStartInstruction(((BT_JumpIns) instr).getTarget());
			SubRoutine jsr = (SubRoutine) subroutines.get(target);
			if(jsr == null) {
				jsr = code.createSubRoutine(target);
				code.visitReachableCode(new JSRVisitor(jsr, code), target, instr, j, jsr);
				subroutines.put(jsr.startInstruction, jsr);
				jsrList.add(jsr);
			}
		}
		if (jsrList == null) {
			return false;
		}
		inlineListedJSRs(code, jsrList);
		return true;
	}
	
	static class JSRVisitor extends BT_CodeVisitor {
		int startJSRIndex;
		BT_InsVector vec;
		
		JSRVisitor(SubRoutine jsr, BT_CodeAttribute codeAtt) {
			vec = codeAtt.getInstructions();
			startJSRIndex = vec.indexOf(jsr.startInstruction);
		}
		
		protected boolean visitHandler(BT_ExceptionTableEntry handler, int instructionIndex) {
			//We want to visit only those handlers contained within the subroutine. 
			//We visit no handlers that contain the subroutine
			int startIndex = vec.indexOf(handler.startPCTarget);
			return startJSRIndex <= startIndex;
		}
	}
	
	private static void inlineListedJSRs(BT_CodeAttribute code, ArrayList jsrList) throws BT_CodeException {
		int inlineCount= 0;
		BT_InsVector methodIns = code.getInstructions();
		
		do {
			
			//This number indicates the number of nested jsrs we will tolerate.
			//The number will be incremented as we go along, if necessary, however
			//it will likely not be necessary.  We inline jsrs that have no nested
			//jsrs first, so we will likely never have to online a jst which contains a jsr.
			int currentLowestNestedCount = 0; 
			
			int i;
			for(i=jsrList.size() - 1; i>=0; i--) {				
				SubRoutine jsr = (SubRoutine) jsrList.get(i);
				if(jsr.getNestedJSRJumps().length > currentLowestNestedCount) {
					continue;
				}
	
				for(int j=0; j<methodIns.size(); j++) {
					BT_Ins instr = methodIns.elementAt(j);
					if (instr.opcode == BT_Ins.opc_jsr) { 
						BT_JumpIns jsrInstr = (BT_JumpIns) instr;
						if(jsr.isTargetOf(jsrInstr)) {
							if (jsr.rets) {
								inlineFullJsr(jsr, code, jsrInstr);
								BT_Ins newInstruction = BT_Ins.make(opc_aconst_null);
								while(methodIns.elementAt(j).isBlockMarker()) {
									//we will put the aconst_null next to the astore of the jsr,
									//so it will be optimized out later
									j++;
								}
								methodIns.insertElementAt(newInstruction, j);
								newInstruction.link(code);
							} 
							else {
								jsrInstr.unlink(code);	
								BT_Ins jsrStart = jsr.startInstruction;
								int jsrStartIndex = methodIns.indexOf(jsrStart);
								BT_BasicBlockMarkerIns blockMarker;
								boolean newBlockMarker;
								if(jsrStartIndex != 0) {
									BT_Ins previous = methodIns.elementAt(jsrStartIndex - 1);
									if(previous.isBlockMarker()) {
										newBlockMarker = false;
										blockMarker = (BT_BasicBlockMarkerIns) previous;
									} else {
										newBlockMarker = true;
										blockMarker =  BT_Ins.make();
									}
									
								} else {
									newBlockMarker = true;
									blockMarker =  BT_Ins.make();
								}
								
								BT_Ins newInstruction = BT_Ins.make(opc_goto, blockMarker);
								code.changeReferencesFromTo(jsrInstr, newInstruction, false);
								methodIns.setElementAt(newInstruction, j);
								newInstruction.link(code);
								newInstruction = BT_Ins.make(opc_aconst_null);
								methodIns.insertElementAt(newInstruction, j);
								newInstruction.link(code);
								
								//now insert the blockmarker
								if(newBlockMarker) {
									jsrStartIndex = methodIns.indexOf(jsrStart);
									methodIns.insertElementAt(blockMarker, jsrStartIndex);
									blockMarker.link(code);
								}
							}
							inlineCount++;
						}
					}
				}
				jsrList.remove(i);
				
				currentLowestNestedCount = 0;
				for(int k=0; k<jsrList.size(); k++) {
					jsr = (SubRoutine) jsrList.get(k);
					jsr.updateNestedSubRoutines();
				}
				break;
			} //end for
			if(i < 0) { //exhausted the list
				
				//we are unlikely to reach here unless there is a circular jsr to jsr containment path,
				//which would cause an unending loop or a verify error most likely, but is not
				//something that most compilers would ever generate
				currentLowestNestedCount++;
			}
		} while(!jsrList.isEmpty());
									  
		// Recompute each instruction's byteIndex
		code.computeMaxInstructionSizes();
	}


	/**
	 * Inline a range of instructions at a given position in the same code.
	 * This method is intended to be used for inlining subroutine code at a jsr instruction.
	 * Exception handlers and line number table entries are duplicated as necessary.
	 *
	 * @param code the target code for inlining
	 * @param toIndex the index of the instruction in code to be 
	 *        substituted by the inlined code.
	 * @param fromStartIndex the index of the first instruction in code to be 
	 *        inlined.
	 * @param fromEndIndex the index of the first instruction after the code to be 
	 *        inlined.
	 */
	private static void inlineFullJsr(
		SubRoutine jsr,
		BT_CodeAttribute code,
		BT_JumpIns replacedInstr) throws BT_CodeException {
		
		BT_InsVector codeIns = code.getInstructions();
		BT_Ins fromStartInstr = jsr.startInstruction;
		
		//for jsr inlining, we need to split any entries of the exception table 
		//that will span the inlined code into two new entries:
		//one entry before the inlined code and one following the inlined code
		//the code we are inlining will then not be inserted into a new exception range
		
		BT_ExceptionTableEntryVector ranges = code.getExceptionTableEntries();
		for(int i=ranges.size() - 1; i>=0; i--) {
			BT_ExceptionTableEntry firstEntry = ranges.elementAt(i);
			int startIndex = codeIns.indexOf(firstEntry.startPCTarget);
			int endIndex = codeIns.indexOf(firstEntry.endPCTarget);
			int replacedIndex = codeIns.indexOf(replacedInstr);
			if(startIndex >= endIndex) {
				continue;
			}
			
			int fromStartIndex = codeIns.indexOf(fromStartInstr);
			if((replacedIndex >= startIndex && replacedIndex < endIndex) /* the handler spans the inlining instruction */
				&& (fromStartIndex < startIndex || fromStartIndex >= endIndex)) { /* the handler does not span the inlined code */
				BT_BasicBlockMarkerIns endPCTarget = firstEntry.endPCTarget;
				int secondIndex;
				
				if(startIndex < replacedIndex) {
					secondIndex = i + 1;
					if(replacedIndex > 0) {
						BT_Ins prev = codeIns.elementAt(replacedIndex - 1);
						if(prev.isBlockMarker()) {
							firstEntry.endPCTarget = (BT_BasicBlockMarkerIns) prev;
						} else {
							BT_BasicBlockMarkerIns newBlockMark =  BT_Ins.make();
							firstEntry.endPCTarget = newBlockMark;
							codeIns.insertElementAt(newBlockMark, replacedIndex);
							replacedIndex++;
							endIndex++;
						}
					} else {
						BT_BasicBlockMarkerIns newBlockMark =  BT_Ins.make();
						firstEntry.endPCTarget = newBlockMark;
						codeIns.insertElementAt(newBlockMark, replacedIndex);
						replacedIndex++;
						endIndex++;
					}
				} else {
					secondIndex = i;
					ranges.removeElementAt(i);
				}
				int nextStartIndex = replacedIndex + 1;
				BT_Ins next = codeIns.elementAt(nextStartIndex);
				BT_BasicBlockMarkerIns nextStart;
				if(next.isBlockMarker()) {
					nextStart = (BT_BasicBlockMarkerIns) next;
				} else {
					nextStart = BT_Ins.make();
					codeIns.insertElementAt(nextStart, nextStartIndex);
					endIndex++;
				}
				if(nextStartIndex < endIndex) {
					BT_ExceptionTableEntry secondEntry =
						new BT_ExceptionTableEntry(
							nextStart,
							endPCTarget,
							firstEntry.handlerTarget,
							firstEntry.catchType, code);	
					ranges.insertElementAt(secondEntry, secondIndex);
				}
			}
		}
		
		inlineJsr(code, fromStartInstr, codeIns.indexOf(jsr.endInstruction) + 1, code, replacedInstr, jsr);
	}
		
	private static void inlineJsr(
			BT_CodeAttribute code,
			BT_Ins fromStartInstr,
			int fromEndIndex,
			BT_CodeAttribute callerCode,
			BT_JumpIns replacedInstr,
			SubRoutine jsr) throws BT_CodeException {
		
		BT_InsVector codeIns = code.getInstructions();
		int toStartIndex = codeIns.indexOf(replacedInstr);
		int fromStartIndex = codeIns.indexOf(fromStartInstr);
		
		//find the reachable instructions
		JSRVisitor visitor = new JSRVisitor(jsr, code);
		code.visitReachableCode(visitor, jsr.startInstruction, replacedInstr, toStartIndex, jsr);
		
		
		int toEndIndex =
			inlineInstructionRange(
				code,
				fromStartIndex,
				fromEndIndex,
				code,
				toStartIndex,
				0,
				jsr,
				visitor);
				
		// Code has moved if inlined into itself at position before fromStartIndex
		int newFromStartInstrIndex;
		int offset;
		newFromStartInstrIndex = fromStartIndex;
		offset = 0;
		
		inlineAttributes(
			code,
			newFromStartInstrIndex, 
			fromEndIndex + offset,
			code,
			toStartIndex,
			toEndIndex,
			0,
			true);


		// Change (line number) attribute references to new instruction
		code.changeReferencesFromTo(
			replacedInstr,
			codeIns.elementAt(toStartIndex), false);
	}
	

	/**
	 * Inline a range of instructions at a given position in some code.
	 * Ret or return instructions in the source code will be replaced by jumps
	 * to the first instruction following the inlined code.
	 *
	 * @param codeIns the source for inlining
	 * @param codeStartIndex the index of the first instruction in codeIns to be 
	 *        inlined.
	 * @param codeEndIndex the index of the first instruction after the code to be 
	 *        inlined.
	 * @param callerIns the target for inlining
	 * @param callerIndex the index of the instruction in callerIns to be 
	 *        substituted by the inlined code.
	 * @param localsInc offset to increment locals access of inlined code
	 * @return the index of the first instruction following the inlined code in the target
	 */
	private static int inlineInstructionRange(
		BT_CodeAttribute code,
		int codeStartIndex,
		int codeEndIndex,
		BT_CodeAttribute callerCode,
		int callerIndex,
		int localsInc) throws BT_CodeException {
			return inlineInstructionRange(code, codeStartIndex, codeEndIndex, callerCode, callerIndex, localsInc, null, null);
	}
		
	private static int inlineInstructionRange(
		BT_CodeAttribute code,
		int codeStartIndex,
		int codeEndIndex,
		BT_CodeAttribute callerCode,
		int callerIndex,
		int localsInc,
		SubRoutine jsr,
		JSRVisitor visitor) throws BT_CodeException {

		BT_InsVector codeIns = code.getInstructions();
		BT_InsVector callerIns = callerCode.getInstructions();
		
		int length = codeEndIndex - codeStartIndex;
		BT_Ins clonedIns[] = new BT_Ins[length + 1]; //need an extra space for the jumpTarget below

		// jumpTarget will be created if ret instruction is detected before the last
		// instruction of codeIns
		// this allows optimizations to be performed on inlined code
		BT_Ins jumpTarget = null;

		// Clone instructions into a temp array first because codeIns and callerIns can be the same
		for (int k = 0; k < length; k++) {
			BT_Ins newInstr;
			BT_Ins instr = codeIns.elementAt(codeStartIndex + k);
			if(jsr != null && visitor != null && !visitor.isVisited(codeStartIndex + k)) {
				newInstr = instr.isBlockMarker() ? BT_Ins.make() : BT_Ins.make(opc_nop);
				newInstr.link(callerCode);
			} else if ((jsr != null) ? jsr.retsAt(instr) : instr.isReturnIns()) {
				// Convert ret or return to goto (or nop if last instruction).
				if (k == length - 1) {
					newInstr = BT_Ins.make(opc_nop);
				}
				else {
					if (jumpTarget == null) {
						// Locate (or create) the jump target for substituting ret or return instructions
						if(jsr == null) {
							jumpTarget = callerIns.elementAt(callerIndex + 1);
							if(jumpTarget.isBlockMarker()) {
								jumpTarget.byteIndex = -1;
							} else {
								jumpTarget =  BT_Ins.make();
								clonedIns[length] = jumpTarget;
							}
						} else {
							jumpTarget =  BT_Ins.make();
							clonedIns[length] = jumpTarget;
						}
					}
					newInstr = BT_Ins.make(opc_goto, (BT_BasicBlockMarkerIns) jumpTarget);
				}
			} else {
				newInstr = (BT_Ins) instr.clone();
				newInstr.incrementLocalsAccessWith(
					localsInc,
					0,
					callerCode.getLocals());
				// Calling dereference causes callsites to be added to target
				newInstr.link(callerCode);
			}
			clonedIns[k] = newInstr;
		}

		//Change references to new code
		boolean alteredIndex = false;
		for (int k = 0; k < codeIns.size(); k++) {
			BT_Ins instruction = codeIns.elementAt(k);
			if(instruction.isBlockMarker()) {
				alteredIndex = true;
				instruction.byteIndex = k;
			}
		}
		if(alteredIndex) {
			BT_BasicBlockMarkerIns singleRef[] = new BT_BasicBlockMarkerIns[1];
			for (int n = 0; n < length; n++) {
				BT_Ins instruction = clonedIns[n];
				BT_BasicBlockMarkerIns references[];
				if(instruction.isJumpIns()) {
					singleRef[0] = ((BT_JumpIns) instruction).getTarget();
					references = singleRef;
				} else {
					references = instruction.getAllReferences();
				}
				if(references != null) {
					for(int m=0; m<references.length; m++) {
						
						//we need to make an exception in this case for the goto that replaced the ret, which points to the inlined range but needs no remapping
						//it is the byte index of the reference that matters here.  
						
						//this code does not work well with jsrs.
						
						BT_BasicBlockMarkerIns reference = references[m];
						int targetInsIndex = reference.byteIndex;
						if(targetInsIndex >= codeStartIndex && targetInsIndex < codeEndIndex) {
							BT_BasicBlockMarkerIns newTarget = (BT_BasicBlockMarkerIns) clonedIns[targetInsIndex - codeStartIndex];
							instruction.changeReferencesFromTo(reference, newTarget);
						}
					}
				}
			}
		
			//restore the byte indices
			codeIns.setAllByteIndexes();
		}
		
		// Copy and relocate exception handlers into caller (and retain proper order)
		int last = 0;
		BT_ExceptionTableEntryVector excs = code.getExceptionTableEntries();
		for (int m = excs.size() - 1; m >= last; m--) {
			BT_ExceptionTableEntry entry = excs.elementAt(m);
			
			int startIndex = codeIns.indexOf(entry.startPCTarget);
			int endIndex = codeIns.indexOf(entry.endPCTarget);
			if (startIndex >= codeStartIndex && startIndex < codeEndIndex && endIndex <= codeEndIndex) {
				BT_ExceptionTableEntry callerEntry = (BT_ExceptionTableEntry) entry.clone();
				callerEntry.code = callerCode.code;
				for (int k = codeStartIndex; k < codeEndIndex; k++) {
					//for every exception table entry, we replaces references to the old code with references to the new
					BT_Ins from = codeIns.elementAt(k);
					BT_Ins to = clonedIns[k - codeStartIndex];
					if(from.isBlockMarker()) {
						callerEntry.changeReferencesFromTo((BT_BasicBlockMarkerIns) from, (BT_BasicBlockMarkerIns) to);
					}
				}
				
				if(endIndex == codeEndIndex) {
					BT_Ins instructionFollowingInliningIns = codeIns.elementAt(callerIndex + 1);
					if(instructionFollowingInliningIns.isBlockMarker() /* should be true since block markers follow jsrs */) {
						callerEntry.endPCTarget = (BT_BasicBlockMarkerIns) instructionFollowingInliningIns;
					}
				}
				callerCode.insertExceptionTableEntry(callerEntry, 0);
				if (callerCode == code) {
					last++;
					m++;
				}
			}
		}
		
		// Remove the instruction we're inlining
		// later we will change references from the old instruction to the instruction replacing it
		BT_Ins instr = callerIns.elementAt(callerIndex);
		instr.unlink(callerCode);
		callerIns.removeElementAt(callerIndex);
		
		// Insert the instructions into the caller
		for (int k = clonedIns[length] == null ? length - 1 : length;
			k >= 0;
			k--) {
			callerIns.insertElementAt(clonedIns[k], callerIndex);
		}

		int callerEndIndex = callerIndex + length;

		return callerEndIndex;
	}

	

	/**
	 * Inline local variable, line number and exception handler attributes
	 * related to the range of inlined instructions.
	 *
	 * @param code the source for inlining
	 * @param codeStartIndex the index of the first instruction in codeIns to be 
	 *        inlined.
	 * @param codeEndIndex the index of the first instruction after the code to be 
	 *        inlined.
	 * @param caller the target for inlining
	 * @param callerStartIndex the index of the first inlined instruction in callerIns.
	 * @param callerEndIndex the index of the first instruction in callerIns after the inlined instructions.
	 * @param localsInc offset to increment locals access of inlined code
	 */
	private static void inlineAttributes(
		BT_CodeAttribute code,
		int calleeStartIndex,
		int calleeEndIndex,
		BT_CodeAttribute callerCode,
		int callerStartIndex,
		int callerEndIndex,
		int localsInc,
		boolean inSameSourceFile) {
		BT_InsVector callerIns = callerCode.getInstructions();
		BT_InsVector codeIns = code.getInstructions();

		
		for (int i = 0; i < code.attributes.size(); i++) {
			BT_Attribute attr = code.attributes.elementAt(i);

			if (attr instanceof BT_LocalVariableAttribute) {
				BT_LocalVariableAttribute callerAttr = null;
				// Clone and relocate local variable attributes
				BT_LocalVariableAttribute calleeAttr = (BT_LocalVariableAttribute) attr;
				BT_LocalVariableAttribute.LV[] localVariables = calleeAttr.localVariables;
				BT_LocalVariableAttribute.LV[] clonedVariables = new BT_LocalVariableAttribute.LV[localVariables.length];
				int cloned = 0;
				BT_LocalVector callerLocals = callerCode.getLocals();
				for (int k = 0; k < localVariables.length; k++) {
					BT_LocalVariableAttribute.LV cLV = localVariables[k];
					if(!cLV.isDereferenced()) {
						continue;
					}
					BT_LocalVariableAttribute.DereferencedLocal calleeLV = 
						(BT_LocalVariableAttribute.DereferencedLocal) cLV;
					
					int startIndex = codeIns.indexOf(calleeLV.startIns);
					if (startIndex >= calleeStartIndex && startIndex < calleeEndIndex) {
						int endIndex = (calleeLV.beyondIns == null) ? 
								-1 : codeIns.indexOf(calleeLV.beyondIns);
						if (endIndex == -1) {
							if (codeIns.size() + callerStartIndex < callerIns.size()) {
								endIndex = codeIns.size();
							}
						}
						int callerLocalIndex = calleeLV.getLocalIndex() + localsInc;
						BT_Local clonedLocal = callerLocals.elementAt(callerLocalIndex);//get from localIndex
						if(callerAttr == null) {
							callerAttr = getCallerAtt(callerCode, calleeAttr.getName(), calleeAttr.isLocalTable());
						}
						String name = getLocalName(callerCode, code, callerAttr, calleeLV.nameS);
						BT_LocalVariableAttribute.LV callerLV;
						BT_Ins startInstruction = 
							callerIns.elementAt(startIndex - calleeStartIndex + callerStartIndex);
						BT_Ins beyondInstruction = ((endIndex == -1) ? null : 
							callerIns.elementAt(endIndex - calleeStartIndex + callerStartIndex));
						if(calleeAttr.isLocalTable()) {
							callerLV = new LocalVariable(
									startInstruction,
									beyondInstruction,
									name,
									clonedLocal,
									((LocalVariable) calleeLV).descriptorC);
						} else {
							callerLV = new GenericLocalVariable(
									startInstruction,
									beyondInstruction,
									name,
									clonedLocal,
									((GenericLocalVariable) calleeLV).descriptorName);
						}
						clonedVariables[cloned++] = callerLV;
					}
				}
				if (cloned > 0) {
					// Merge cloned local variable attributes into caller's first element
					int callerLen = callerAttr.localVariables.length;
					localVariables =
						new BT_LocalVariableAttribute.LV[callerLen + cloned];
					for (int k = 0; k < callerLen; k++) {
						localVariables[k] = callerAttr.localVariables[k];
					}
					for (int k = 0; k < cloned; k++) {
						localVariables[callerLen + k] = clonedVariables[k];
					}
					callerAttr.localVariables = localVariables;
				}

			} else if (
				inSameSourceFile && attr instanceof BT_LineNumberAttribute) {

				// Clone and relocate the line number attribute
				BT_LineNumberAttribute calleeAttr =
					(BT_LineNumberAttribute) attr;
				BT_LineNumberAttribute.PcRange[] pcRanges = calleeAttr.getRanges();
				BT_LineNumberAttribute.PcRange[] clonedRanges =
					new BT_LineNumberAttribute.PcRange[pcRanges.length];
				int cloned = 0;
				for (int k = 0; k < pcRanges.length; k++) {
					BT_LineNumberAttribute.PcRange cRange = pcRanges[k];
					if(!cRange.isDereferenced()) {
						continue;
					}
					BT_LineNumberAttribute.DereferencedPCRange calleePcRange = 
						(BT_LineNumberAttribute.DereferencedPCRange) cRange;
					int startIndex = codeIns.indexOf(calleePcRange.startIns);
					if (startIndex >= calleeStartIndex
						&& startIndex < calleeEndIndex) {
						BT_LineNumberAttribute.PcRange callerPcRange =
							new BT_LineNumberAttribute.DereferencedPCRange(
								callerIns.elementAt(
									startIndex
										- calleeStartIndex
										+ callerStartIndex),
								calleePcRange.lineNumber);
						clonedRanges[cloned++] = callerPcRange;
					}
				}
				if (cloned > 0) {
					// Find first line number attributes of caller
					BT_LineNumberAttribute callerAttr = null;
					for (int k = 0; k < callerCode.attributes.size(); k++) {
						BT_Attribute a = callerCode.attributes.elementAt(k);
						if (a instanceof BT_LineNumberAttribute) {
							callerAttr = (BT_LineNumberAttribute) a;
							break;
						}
					}
					// If caller has no line number attributes, create empty one
					if (callerAttr == null) {
						callerAttr = new BT_LineNumberAttribute(0, callerCode);
						callerCode.attributes.addElement(callerAttr);
					}
					// Merge cloned line number attributes into caller's first element
					BT_LineNumberAttribute.PcRange originalRanges[] = callerAttr.getRanges();
					int callerLen = originalRanges.length;
					pcRanges =
						new BT_LineNumberAttribute.PcRange[callerLen + cloned];
					for (int k = 0; k < callerLen; k++) {
						pcRanges[k] = (BT_LineNumberAttribute.PcRange) originalRanges[k].clone();
					}
					for (int k = 0; k < cloned; k++) {
						pcRanges[callerLen + k] = clonedRanges[k];
					}
					callerAttr.setRanges(pcRanges);
				}
			}

		}
	}
	
	private static String getLocalName(BT_CodeAttribute caller, BT_CodeAttribute callee, BT_LocalVariableAttribute existingAtt, String newLocal) {
		String originalName = newLocal;
		LV[] localVariables = existingAtt.localVariables;
		boolean nameExists;
		int index = 1;
		do {
			nameExists = false;
			for(int i=0; i<localVariables.length; i++) {
				LV local = localVariables[i];
				if(local.nameS.equals(newLocal)) {
					newLocal = originalName + '_' + index;
					index++;
					nameExists = true;
				}
			}
		} while(nameExists);
		return newLocal;
	}

	private static BT_LocalVariableAttribute getCallerAtt(
			BT_CodeAttribute callerCode, 
			String name, 
			boolean isLocalTable) {
		//Find first local variable attributes of caller
		BT_LocalVariableAttribute callerAttr = null;
		for (int k = 0; k < callerCode.attributes.size(); k++) {
			BT_Attribute a = callerCode.attributes.elementAt(k);
			if (a instanceof BT_LocalVariableAttribute) {
				BT_LocalVariableAttribute localAtt = (BT_LocalVariableAttribute) a;
				if(localAtt.isLocalTable() == isLocalTable) {
					callerAttr = (BT_LocalVariableAttribute) a;
					break;
				}
			}
		}
		// If caller has no local variable attributes, create empty one
		if (callerAttr == null) {
			callerAttr =
				new BT_LocalVariableAttribute(name, 
						0, 
						callerCode);
			callerCode.attributes.addElement(callerAttr);
		}
		return callerAttr;
	}


	/**
	 * Return the top level class which requires initialization, or null if no such class
	 */
	private static BT_Class requiresInitialization(BT_Class cls) {
		BT_Method m =
			cls.findInheritedMethod(
				BT_Method.STATIC_INITIALIZER_NAME,
				cls.getRepository().basicSignature,
				false);
		if(m == null || m.cls.equals(cls.getRepository().findJavaLangObject())) {
			return null;
		}
		return m.cls;
	}
	
	private static BT_Field findInitializingStaticField(
		BT_Class classRequiringInitialization,
		BT_Class cls,
		BT_Class referent) {
	
		do {
			for (int i = 0; i < cls.fields.size(); ++i) {
				BT_Field f = cls.fields.elementAt(i);
				if (f.isStatic()
					&& f.isVisibleFrom(referent)
					&& f.getOpcodeForPop() == BT_Ins.opc_pop
					&& !f.isStub()
						//final fields are frequently inlined which prevents class initialization
					&& !f.isFinal())
					return f;
			}
			if(cls.equals(classRequiringInitialization)) {
				return null;
			}
			cls = cls.getSuperClass();
		} while(cls != null && !cls.equals(cls.getRepository().findJavaLangObject()));
		return null;
	}
	

}
