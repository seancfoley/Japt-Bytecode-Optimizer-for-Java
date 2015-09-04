/*
 * Created on Nov 4, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ive.tools.japt.startupPerformance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.startupPerformance.Injection.FieldTypeInjection;
import com.ibm.ive.tools.japt.startupPerformance.Injection.MethodArgInjection;
import com.ibm.ive.tools.japt.startupPerformance.Injection.ReturnTypeInjection;
import com.ibm.ive.tools.japt.startupPerformance.Injection.ThrowInjection;
import com.ibm.jikesbt.BT_BasicBlockMarkerIns;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_ExceptionTableEntryVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldRefIns;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_StackCell;
import com.ibm.jikesbt.BT_StackHistoryProvider;
import com.ibm.jikesbt.BT_StackPool;
import com.ibm.jikesbt.BT_StackShapeVisitor;
import com.ibm.jikesbt.BT_StackShapes;
import com.ibm.jikesbt.BT_StackType;
import com.ibm.jikesbt.BT_TypeHistoryStackCell;
import com.ibm.jikesbt.BT_StackType.ClassType;
import com.ibm.jikesbt.BT_TypeHistoryStackCell.Duplication;
import com.ibm.jikesbt.BT_TypeHistoryStackCell.History;

/**
 * @author Sean Foley
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Method {

    final private Messages messages;
	final private boolean simpleOutput;
	final private BT_StackHistoryProvider provider;
	final private BT_CodeAttribute code;
	final private Logger logger;
		
	int optimizedThrows;
	int optimizedCatches;
	int optimizedSpans;
	int optimizedReturns;
	int optimizedFieldAccesses;
	int optimizedFieldArg;
	int optimizedMethodArgs;
	int optimizedMethodInvokes;
	
	boolean doCatchOptimization;
	boolean doThrowOptimization;
	boolean doUpcastOptimization;
	
	Method(BT_Method method, BT_StackHistoryProvider provider, Logger logger, Messages messages, boolean simpleOutput) {
		this.messages = messages;
		this.simpleOutput = simpleOutput;
		this.provider = provider;
		this.code = method.getCode();
		this.logger = logger;
	}
	
	public String toString() {
		BT_Method m = code.getMethod();
		if(m != null) {
			return m.fullName();
		}
		return super.toString();
	}
    
	
	private ArrayList getSpans() {
		ArrayList spans = new ArrayList();
		Span lastSpan = null;
		BT_ExceptionTableEntryVector excs = code.getExceptionTableEntries();
		for (int k = 0; k < excs.size(); k++){
			BT_ExceptionTableEntry currentEntry = excs.elementAt(k);
			Span newSpan = new Span(
				currentEntry.startPCTarget,
				currentEntry.endPCTarget);
			
			Span currentSpan;
			if(lastSpan == null || !newSpan.equals(lastSpan)) {
				currentSpan = newSpan;
				spans.add(newSpan);
				lastSpan = newSpan;
			} else {
				currentSpan = lastSpan;
			}
			boolean isFinally = (currentEntry.catchType == null);
			if(isFinally) {
				currentSpan.setFinallyEntry(currentEntry);
				lastSpan = null;
			} else {
				//TODO could check for unreachable handlers here (ie any handler in the same span whose catch type the same or a parent class makes this entry unreachable)
				currentSpan.addExceptionEntry(currentEntry);
			}
		}
		return spans;
	}
	
	boolean optimize(BT_StackPool pool) {
		boolean result = false;
		if(doCatchOptimization) {
			result |= doCatchOptimization();
		}
		if(doThrowOptimization || doUpcastOptimization) {
			result |= doUpcastOptimization(pool);
		}
		return result;
	}
	
	/**
	 * Alter grouped catch blocks to catch only java.lang.Throwable.
	 * Use instanceof to determine the exception type and then
	 * jump to the appropriate catch block.
	 */
	boolean doCatchOptimization() {
		if(code.getExceptionTableEntryCount() == 0) {
			return false;
		}
		
		/*
		 * The byte indexes are required for getSpans and for the logging that is done later in this method
		 */
		code.computeInstructionSizes();
		
		boolean alteredTable = false;
		
		/* Find all caught exceptions and group by range 'tried'.
		 * Add catch blocks that already catch java.lang.Throwable to
		 * their own individual groups.
		 */
		ArrayList spans = getSpans();
		
		
		/* Process the grouped try blocks. Add a preamble to check
		 * instanceof the caught exception and goto the appropriate
		 * catch block.
		 */
		BT_ExceptionTableEntryVector catchTable = new BT_ExceptionTableEntryVector();
		spanLoop:
		for (int k = 0; k < spans.size(); k++) {
			
			//TODO it appears as though the only reason nested blocks are not handled is so that we
			//can add the new handler to the end of the method, whereas this is not necessary,
			//we should add the handler anywhere we like
			
			
			Span tmpSpan = (Span) spans.get(k);
			
			if(!tmpSpan.catchesThrowableSubclasses(provider.javaLangThrowable)) {
				tmpSpan.addEntriesTo(catchTable);
				continue spanLoop;
			}
				
			// Checks if a try block is nested in another try block.
			for (int l = 0; l < spans.size(); l++) {
				if (l == k) continue;
				Span theSpan = (Span) spans.get(l);
				if(tmpSpan.intersects(theSpan)) {
					tmpSpan.addEntriesTo(catchTable);
					continue spanLoop;
				}
			}
			
			alteredTable = true;
			
			// Create and insert the preamble.
			BT_BasicBlockMarkerIns blockstart =  BT_Ins.make();
			code.insertInstruction(blockstart);
			BT_ExceptionTableEntry exceptionEntry =
				new BT_ExceptionTableEntry(
					tmpSpan.start,
					tmpSpan.stop,
					blockstart,
					provider.javaLangThrowable,
					code
				);
			catchTable.addElement(exceptionEntry);
			Enumeration enumeration = tmpSpan.getExceptionEntries();
				while(enumeration.hasMoreElements()) {
					BT_ExceptionTableEntry entry = (BT_ExceptionTableEntry) enumeration.nextElement();
					int lineNumber = code.findLineNumber(entry.handlerTarget);
					if(simpleOutput) {
						messages.OPTIMIZED_CATCH_SIMPLE.log(logger, Integer.toString(lineNumber));
					} else {
						if(lineNumber > 0) {
							messages.OPTIMIZED_CATCH_AT_LINE.log(logger, new Object[] {Integer.toString(lineNumber), Integer.toString(entry.handlerTarget.byteIndex), entry.catchType});
						} else {
							messages.OPTIMIZED_CATCH.log(logger, new Object[] {Integer.toString(entry.handlerTarget.byteIndex), entry.catchType});
						}
					}
					BT_Class caught = entry.catchType;
					if(caught.equals(provider.javaLangThrowable)) {
						code.insertInstruction(BT_Ins.make(
								BT_Ins.opc_goto, entry.handlerTarget)
							);
					} else {
						code.insertInstruction(BT_Ins.make(BT_Ins.opc_dup));
						code.insertInstruction(
							BT_Ins.make(BT_Ins.opc_instanceof, caught)
						);
						code.insertInstruction(BT_Ins.make(
							BT_Ins.opc_ifne, entry.handlerTarget)
						);
						code.insertInstructionAt(
							BT_Ins.make(BT_Ins.opc_checkcast, caught),
							code.findInstruction(entry.handlerTarget) + 1
						);
					}
					optimizedCatches++;
				}
			BT_ExceptionTableEntry finallyEntry = tmpSpan.getFinallyEntry();
			if(finallyEntry != null) {
				code.insertInstruction(BT_Ins.make(
						BT_Ins.opc_goto, finallyEntry.handlerTarget)
					);
			} else {
				//code.insertInstruction(BT_Ins.make(BT_Ins.opc_checkcast, provider.javaLangThrowable));
				code.insertInstruction(BT_Ins.make(BT_Ins.opc_athrow));
			}
			
			optimizedSpans++;
			
		}
		
		if(alteredTable) {
			// Replace the code's ExceptionTable with the new one we created
			code.setExceptionTable(catchTable);
			
			//recompute the byte indexes
			code.computeInstructionSizes();
			return true;
		}
		return false;
	}
	
//	TODO here we can match classes to a set of classes from a command line option
	//use "-cast className" and "-noCast className"
	//or "-castTo clasName" and "-noCastTo className"
	private boolean isWideningCastTarget(BT_Class intendedType) {
		/*
		 * We do not excluded casts to code.method.getDeclaringClass() because these casts avoid the loading of subclasses
		 */
		return code.isWideningCastTarget(intendedType);
	}
	
	public boolean isCast(BT_Class stackType, BT_Class intendedType) {
		/* 
		 * Suppose intendedType and stackType are both either code.method.getDeclaringClass() or a superclass.  
		 * In this case, if we knew that all superclasses of code.method.getDeclaringClass() were loaded we could avoid the cast,
		 * considering the intendedType and the stackType were both loaded.  
		 * 
		 * But for a given class X, we do not know that superclasses of X are loaded when X is loaded. 
		 * We only know superclasses of X are loaded when X is initialized.  
		 * So we do not exclude this case.
		 */
		return code.wideningCastRequiresClassLoad(stackType, intendedType);
	}
	
	private CodeInjection getSwapInjection(int instructionIndex, BT_Class toType) {
		BT_Ins newIns[] = new BT_Ins[] {
			BT_Ins.make(BT_Ins.opc_swap),
			BT_Ins.make(BT_Ins.opc_checkcast, toType),
			BT_Ins.make(BT_Ins.opc_swap)
		};
		return new CodeInjection(code, instructionIndex, newIns, null);
	}

	private CodeInjection getSwapInjection(
			int instructionIndex,
        	BT_Class toType,
        	ClassType fromType) {
        	if(!fromType.isNull() && isCast(fromType.getType(), toType)) {
        		return getSwapInjection(instructionIndex, toType);
        	}
        	return null;
    }
    
	private CodeInjection getInjection(
			int instructionIndex,
        	BT_Class toType,
        	ClassType fromType) {
		BT_InsVector inst = code.getInstructions();
        BT_Ins prevIns = (instructionIndex > 0) ? inst.elementAt(instructionIndex - 1) : null;
        if(prevIns != null && prevIns.isCheckCastIns() ) {
        	if(isCast(prevIns.getClassTarget(), toType)) {
        		BT_Ins newIns = BT_Ins.make(BT_Ins.opc_checkcast, toType);
        		return new CodeInjection(code, instructionIndex - 1, newIns, prevIns);
        	}
        } else {
        	if(!fromType.isNull() && isCast(fromType.getType(), toType)) {
        		BT_Ins newIns = BT_Ins.make(BT_Ins.opc_checkcast, toType);
        		return new CodeInjection(code, instructionIndex, newIns, null);
        	}
        }
        return null;
    }
	
	boolean doUpcastOptimization(BT_StackPool pool) {
		// generate stack shapes
		BT_StackShapeVisitor visitor = new BT_StackShapeVisitor(code, pool, provider);
		/* 
		 * we do not use merge candidates because we do not know what the stack maps look like,
		 * so we err on the conservative side (which means potentially more checkcasts) 
		 */
		visitor.useMergeCandidates(false);
		/*
		 * we do not ignore upcasts because we wish to see what the verifier will see
		 */
		visitor.ignoreUpcasts(false);
		
		BT_StackShapes shapes;
		try {
			shapes = visitor.populate();
			
			/*
			 * verify the code as much as possible
			 */
			shapes.verifyStacks();
		} catch(BT_CodeException e) {
			//ignore code that is not well-structured
			return false;
		}
		BT_Class returnType = code.getMethod().getSignature().returnType;
		ArrayList injectList = new ArrayList(); 
		BT_InsVector insVec = code.getInstructions();
		for(int i = 0; i < insVec.size(); i++ ) {
			BT_Ins ins = insVec.elementAt(i);
			BT_StackCell[] stackShapes = shapes.stackShapes[i];
			if( stackShapes == null || stackShapes.length <= 0 ) {
				/* stackShapes is null if an instruction was never visited because it is unreachable */
				continue;
			}
			
			// Add a checkcast to java.lang.Throwable in front of every
			// athrow opcode.
			// void foo() {
			//     throw new subclassOfThrowable();
			// }
			if(ins.isAThrowIns()) {
				if(!(doThrowOptimization || doUpcastOptimization)) {
					continue;
				}
				ClassType stackClassType = stackShapes[stackShapes.length - 1].getClassType();
				CodeInjection params = getInjection(i, provider.javaLangThrowable, stackClassType);
				if(params != null) {
					injectList.add(new ThrowInjection(params));
					optimizedThrows++;
				}
			}
			
			// return case is as follows: 
			// 
			// A getA() {
			//     return new subclassOfA();
			// }
			//
			else if( ins.isReturnIns() ) {
				if(!doUpcastOptimization) {
					continue;
				}
				if(isWideningCastTarget(returnType)) {
					ClassType stackClassType = stackShapes[stackShapes.length - 1].getClassType();
					CodeInjection params = getInjection(i, returnType, stackClassType);
	       			if(params != null) {
	       				injectList.add(new ReturnTypeInjection(params));
	       				optimizedReturns++;
	       			}
				}
        	}
        	
        	// field case is as follows: 
			// A a;
			// void foo() {
			//     a = new subclassOfA();
			// }
			//
			else if(ins.isFieldAccessIns()) {
				if(!doUpcastOptimization) {
					continue;
				}
				boolean optimizedFieldAccess = false;
				BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) ins;
				BT_Field target = fieldRefIns.getFieldTarget();
				if(fieldRefIns.isFieldWriteIns()) {
					BT_Class fieldType = target.getFieldType();
					if(isWideningCastTarget(fieldType)) {
						optimizedFieldAccess |= createArgInjection(target, injectList, shapes.stackShapes, stackShapes, fieldType, i, 1);
					}
					if(!fieldRefIns.isStaticFieldAccessIns() && !isProtectedAccessFromSubclass(target)) {
						BT_StackType accessedObjectType = stackShapes[stackShapes.length - fieldType.getSizeForLocal() - 1].getCellType();
						if(BT_StackShapes.isAllowedUninitializedFieldAccess(accessedObjectType, fieldRefIns)) {
							//Nothing to do, the class of the owner accessed field must match the class being constructed
						} else {
							BT_Class classTarget = fieldRefIns.getResolvedClassTarget(code);
							if(isWideningCastTarget(classTarget)) {
								int depth = fieldType.getSizeForLocal() + 1;
								optimizedFieldAccess |= createArgInjection(target, injectList, shapes.stackShapes, stackShapes, classTarget, i, depth);
							}
						}
					}
				} 
				else if(!fieldRefIns.isStaticFieldAccessIns() && !isProtectedAccessFromSubclass(target)) {
					BT_Class classTarget = fieldRefIns.getResolvedClassTarget(code);
					if(isWideningCastTarget(classTarget)) {
						optimizedFieldAccess |= createArgInjection(target, injectList, shapes.stackShapes, stackShapes, classTarget, i, 1);
					}
				}
				if(optimizedFieldAccess) {
					optimizedFieldAccesses++;
				}
        	}
	
			// method arguments case:
			//
			// void foo(A a) {
			//     foo(new subclassOfA());
			// }
			
			else if( ins.isInvokeIns() ) {
				if(!doUpcastOptimization) {
					continue;
				}
				
				boolean optimizedMethod = false;
				BT_Method invokedMethod = ((BT_MethodRefIns) ins).getMethodTarget();
				BT_ClassVector types = invokedMethod.getSignature().types;
				int typesSize = types.size();
				int stackIndex = stackShapes.length;
				for(int signatureIndex = typesSize - 1; signatureIndex >= 0; signatureIndex--) {
					BT_Class type = types.elementAt(signatureIndex);
					stackIndex -= type.getSizeForLocal();
					if(!isWideningCastTarget(type)) {
						continue;
					}
					optimizedMethod |= createArgInjection(
						invokedMethod,
						injectList, 
						shapes.stackShapes,
						stackShapes,
						type,
						i,
						stackShapes.length - stackIndex);
				} // for( int t ...
				/* 
				 * note that upcasting the invoked class is not beneficial if the instruction
				 * is actually executed at startup since we then know that an instance of the 
				 * invoked class must exist and is therefore loaded.  But that is only if the
				 * instruction has been executed, which is not the case for many verified instructions.
				 */
				if(ins.isInvokeVirtualIns() /* || ins.isInvokeInterfaceIns() the verifier delays interface checks until runtime */) {
					/* invokestatics do not have a "this" on the stack */
					
					/* 
					 * For invokspecials, there are 3 cases:
					 * 1. private invoke: invoke on same class as method being verified, so the class is already loaded,
					 *    and the target class always matches class owning the method anwyay, so no cast is needed
					 * 2. super invoke: invoke on parent class of method being verified, so the parent class is already loaded
					 * 3. constructor invoke: the type on the stack ALWAYS matches the target class, so no cast is needed
					 * In all 3 cases there is no need to insert a check cast for invokespecials.
					 */
							
					--stackIndex;
					if(!isProtectedAccessFromSubclass(invokedMethod)) {
						BT_Class classTarget = ins.getResolvedClassTarget(code);
						if(isWideningCastTarget(classTarget)) {
							optimizedMethod |= createArgInjection(
								invokedMethod,
								injectList,
								shapes.stackShapes,
								stackShapes,
								classTarget,
								i,
								stackShapes.length - stackIndex);
						}
					}
				}
				if(optimizedMethod) {
					optimizedMethodInvokes++;
				}
			}
			
		} // for( int i...
		shapes.returnStacks();
		int numInjections = injectList.size();
		if(numInjections == 0) {
			return false;
		}
		doInjections((Injection[]) injectList.toArray(new Injection[numInjections]));
		return true;
	}
	
	/**
	 * There is a special case in which we cannot insert a checkcast if we are accessing a protected method from a separate package.
	 * Inserting the cast fools the verifier into thinking something other than the "this" object is on the stack, 
	 * causing a verify error.
	 * 
	 * For this reason, the upcast optimization works better with -noResolveRuntimeRefs
	 */
	boolean isProtectedAccessFromSubclass(BT_Member accessedMember) {
		return accessedMember.isProtected() 
			&& !accessedMember.getDeclaringClass().isInSamePackage(code.getMethod().getDeclaringClass());
	}
	
	boolean createArgInjection(
		BT_Member accessedMember,
		ArrayList injectList,
		BT_StackCell allStacks[][],
		BT_StackCell stackShapes[],
		BT_Class typeInSignature,
		int instructionIndex,
		int depth) {
					
		BT_StackType stackType = stackShapes[stackShapes.length - depth].getCellType();
		if(!stackType.isClassType() /* should always be ClassType unless the code is malformed */) {
			return false;
		}
		BT_StackCell cellOnStack = stackShapes[stackShapes.length - depth];
		ClassType typeOnStack = cellOnStack.getClassType();
		Injection injection;
		if(depth == 1 /* top of stack */) {
			CodeInjection params = getInjection(instructionIndex, typeInSignature, typeOnStack);
			if(params != null) {
				if(accessedMember instanceof BT_Method) {
					injection = new MethodArgInjection((BT_Method) accessedMember, instructionIndex, params);
					optimizedMethodArgs++;
				} else {
					injection = new FieldTypeInjection((BT_Field) accessedMember, instructionIndex, params);
					optimizedFieldArg++;
				}
				injectList.add(injection);
				return true;
			}
		} else {
			Duplication dups[];
			BT_TypeHistoryStackCell stackClassType;
			if(cellOnStack instanceof BT_TypeHistoryStackCell) {
				stackClassType = (BT_TypeHistoryStackCell) cellOnStack;
				dups = stackClassType.getDuplications();
			} else {
				/*
				 * NOTE:
				 * Suppose we have a BT_StackType T somewhere on the stack representing a non-primitive class type.
				 * The only way T will not be an object of type BT_TypeHistoryStackCell is if the type was never
				 * on the top of the stack.  
				 * 
				 * There is one circumstance where this is possible:  
				 * 1. an uninitialized object C is placed on the stack by the new instruction
				 * 2. another object X is placed on top of this object
				 * 3. one of the specialized dup instructions puts a copy of C on top of the stack
				 * 4. the constructor for the object C is called, which removes the uninitialized copy of C on the top of the stack, 
				 *    so that X is now on top of the stack, and below we have the newly initialized object C.
				 * 
				 * The object C has never been on top of the stack, and in fact may never be on top of the stack.
				 * 
				 * This circumstance is very rare, in fact most compilers will never do this.
				 * 
				 */
				dups = BT_TypeHistoryStackCell.emptyDuplications;
				stackClassType = null;
			}
			
			if(depth == 2 /*  just below top of stack */) {
				/*
				 * if the history has size one exactly, it is never duplicated,
				 * and the BT_TypeHistoryStackCell element is not consumed in more than one place, then
				 * we do a BT_TypeHistoryStackCell injection.
				 * 
				 * Otherwise we do a swap-checkcast-swap
				 */
				if(stackClassType != null) {
					if(!stackClassType.isConsumedAtMultiplePlaces()) {
						if(stackClassType.getHistorySize() == 1) {
							if(dups.length == 0) {
								History history[] = stackClassType.getHistory();
								if(createHistoricalInjections(
										instructionIndex, 
										accessedMember,
										injectList,
										typeInSignature,
										typeOnStack,
										history,
										dups,
										allStacks)) {
									if(accessedMember instanceof BT_Method) {
										optimizedMethodArgs++;
									} else {
										optimizedFieldArg++;
									}
									return true;
								} else {
									return false;
								}
							}
						}
					}
				}
				CodeInjection params = getSwapInjection(instructionIndex, typeInSignature, typeOnStack);
				if(params != null) {
					if(accessedMember instanceof BT_Method) {
						injection = new MethodArgInjection((BT_Method) accessedMember, instructionIndex, params);
						optimizedMethodArgs++;
					} else {
						injection = new FieldTypeInjection((BT_Field) accessedMember, instructionIndex, params);
						optimizedFieldArg++;
					}
					injectList.add(injection);
					return true;
				}
			} else {
				if(stackClassType != null && tryCreateHistoricalInjections(instructionIndex, accessedMember, injectList, typeInSignature, stackClassType, dups, allStacks)) {
					if(accessedMember instanceof BT_Method) {
						optimizedMethodArgs++;
					} else {
						optimizedFieldArg++;
					}
					return true;	
				}
			}
		}
		return false;
	}
	
	private boolean tryCreateHistoricalInjections(
			int accessIndex,
			BT_Member accessedMember,
			ArrayList injectList,
			BT_Class toType,
			BT_TypeHistoryStackCell fromType,
			Duplication duplications[],
			BT_StackCell stacks[][]) {
		if(fromType.isConsumedAtMultiplePlaces()) {
			return false;
		}
		History history[] = fromType.getHistory();
		boolean result = createHistoricalInjections(accessIndex, accessedMember, injectList, toType, fromType.getClassType(), history, duplications, stacks);
		return result;
	}

	/**
	 * @param accessIndex
	 * @param accessedMember
	 * @param injectList
	 * @param toType
	 * @param fromType
	 * @param result
	 * @param history
	 * @return
	 */
	private boolean createHistoricalInjections(
			int accessIndex, 
			BT_Member accessedMember,
			ArrayList injectList,
			BT_Class toType,
			ClassType fromType,
			History[] history,
			Duplication duplications[],
			BT_StackCell stacks[][]) {
		boolean result = false;
		BT_InsVector inst = code.getInstructions();
		for(int k=0; k<history.length; k++) {
			History hist = history[k];
			int historyIndex = hist.index;
			
			BT_Ins instructionAtIndex = inst.elementAt(historyIndex);
			while(instructionAtIndex.isBlockMarker()) {/* do not just skip the first block marker, treat any succession as if they were a single one */
				historyIndex++;
				instructionAtIndex = inst.elementAt(historyIndex);
			}
				
			CodeInjection params = getInjection(historyIndex, toType, fromType);
			if(params != null) {
				Injection injection;
				if(accessedMember instanceof BT_Method) {
					injection = new MethodArgInjection((BT_Method) accessedMember, accessIndex,  params);
				} else {
					injection = new FieldTypeInjection((BT_Field) accessedMember, accessIndex, params);
				}
				injectList.add(injection);
				result = true;
			}
		}
		/* 
		 * We have changed the type with our checkcasts.  If the type is duplicated later with a dup instruction,
		 * we add another checkcast to cst it back to the type it was previously.
		 */
		if(result) {
			for(int n=0; n<duplications.length; n++) {
				Duplication dup = duplications[n];
				BT_StackCell stackContainingDup[] = stacks[dup.instructionIndex];
				BT_StackType dupType = stackContainingDup[(stackContainingDup.length - 1) - dup.stackIndexFromTop].getCellType();
				if(dupType.isNonNullObjectType()) {
					ClassType dupClassType = dupType.getClassType();
					BT_Class dupClass = dupClassType.getType();
					if(dup.stackIndexFromTop == 0) {
						if(!toType.equals(dupClass)) {
							BT_Ins newIns = BT_Ins.make(BT_Ins.opc_checkcast, dupClass);
							CodeInjection dupParams = new CodeInjection(code, dup.instructionIndex, newIns, null);
							injectList.add(new Injection(dupParams));
						}
					} else if(dup.stackIndexFromTop == 1) {
						/* do the swap, checkcast, swap */
						CodeInjection params = getSwapInjection(dup.instructionIndex, dupClass);
						if(params != null) {
							injectList.add(new Injection(params));
						}
					} else {
						/* should never reach here, the duplicated types are always one of thje top two stack items */
						throw new IllegalArgumentException(); 
					}
				}
			}
		}
		
		return result;
	}
	
	private void doInjections(Injection injections[]) {
		// sort by index
		Arrays.sort(injections);
		
		// must go in reverse order so that each instruction index in the Injection objects remains accurate
		for( int i = injections.length - 1; i >= 0; i--) {
			Injection injection = injections[i];
			injection.inject();
		}
		
		// we compute the instruction byte indexes now because the log methods use this info
		code.computeInstructionSizes();
		
		for(int i = 0; i < injections.length; i++) {
			Injection injection = injections[i];
			if(simpleOutput) {
				injection.logParseable(messages, logger);
			} else {
				injection.log(code, messages, logger);
			}
		}
	}
	
}
