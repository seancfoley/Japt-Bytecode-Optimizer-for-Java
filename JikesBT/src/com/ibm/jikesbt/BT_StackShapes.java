/*
 * Created on Nov 5, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.io.PrintStream;
import java.util.Arrays;

import com.ibm.jikesbt.BT_CodeException.BT_AccessException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_StackUnderflowException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_ExpectedArrayTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_ExpectedObjectTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_ExpectedPrimitiveTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_InvalidArgumentTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_SplitDoubleWordException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_UninitializedObjectTypeException;
import com.ibm.jikesbt.BT_StackType.ClassType;
import com.ibm.jikesbt.BT_StackType.UninitializedObject;

/**
 * @author Sean Foley
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BT_StackShapes implements BT_Opcodes {

	BT_LocalCell initialLocals[]; //the initial local variables when the code is first entered (determined by the method signature and static/non-static property)
	public BT_StackCell stackShapes[][]; //the stack shape at index i is the shape of the stack before executing the instruction at index i
	public BT_LocalCell localShapes[][]; //the locals shape at index i is the shape of the local variables before executing the instruction at index i
	public boolean isMergeInstruction[]; //isMergeInstruction at index i is true if the instruction at index i is reachable from at least two instructions or is the target of an exception handler
	public BT_StackMapFrame frames[]; /* optionally store the frame that is associated with each stack */
	public boolean mergedInitial;
	
	public int maxDepth; //the maximum stack depth
	public int maxLocals; //the maximum number of local variable slots used
	
	boolean hasJSR; //whether the code contains a subroutine
	
	private boolean returnedStacks;
	final private BT_CodeAttribute code;
	final private BT_StackPool pool; //used to cache unused stacks for later use (for performance)
	final private BT_StackCellProvider provider;
	
	private boolean tolerateStubs = true;
	private boolean useExtendedTypeChecking = true;
	
	BT_StackShapes(BT_CodeAttribute code, BT_StackPool pool, BT_StackCellProvider provider) {
		this(code, pool, provider, false);
	}
	
	BT_StackShapes(BT_CodeAttribute code, BT_StackPool pool, BT_StackCellProvider provider, boolean withFrames) {
		this.code = code;
		this.pool = pool;
		this.provider = provider;
		BT_Method method = code.getMethod();
		BT_MethodSignature signature = method.getSignature();
		int codeSize = code.getInstructionSize();
		this.stackShapes = new BT_StackCell[codeSize][];
		this.localShapes = new BT_LocalCell[codeSize][];
		this.isMergeInstruction = new boolean[codeSize];
		this.maxDepth = 0;
    	this.maxLocals = signature.getArgsSize();
    	if(withFrames) {
    		this.frames = new BT_StackMapFrame[codeSize];
    	}
    }
	
	public BT_LocalCell[] getInitialLocals() {
		return initialLocals;
	}
	
	private BT_LocalCell[] getInitialLocalStack() {
   		BT_Method method = code.getMethod();
		BT_MethodSignature sig = method.getSignature();
		boolean isStatic = method.isStatic();
		int thisCount = isStatic ? 0 : 1;
		int totalArgs = sig.getArgsSize() + thisCount;
		int totalParams = sig.types.size() + thisCount;
		this.maxLocals = totalArgs;
		BT_LocalCell newLocals[] = pool.getLocals(totalArgs);
		int localsNum = 0;
		int paramsNum = 0;
		if(!isStatic) {
			if(method.isConstructor()) {
				newLocals[localsNum++] = provider.getUninitializedThisCell(method.cls, totalParams);
			} else {
				newLocals[localsNum++] = provider.getArgumentObjectCell(method.cls, 0, totalParams);
			}
			paramsNum++;
		}
		BT_ClassVector types = sig.types;
		for(int i=0; i<types.size(); i++, paramsNum++) {
			BT_Class type = types.elementAt(i);
			if(type.getSizeForLocal() == 2) {
				newLocals[localsNum++] = type.classType;
				newLocals[localsNum++] = BT_StackType.TOP;
			} else {
				if(type.isPrimitive()) {
					newLocals[localsNum++] = type.classType.convert();
				} else {
					newLocals[localsNum++] = provider.getArgumentObjectCell(type, paramsNum, totalParams);
				}
			}
		}
		this.initialLocals = newLocals;
		return newLocals;
   	}
	
	/**
   	 * When tolerating stubs, verification will ignore cases in which types on the stack
   	 * fail type checks for method calls and field writes because class hierarchies 
   	 * were indeterminate because classes were not loaded.  
   	 * @param tolerateStubs
   	 */
   	public void tolerateStubs(boolean tolerateStubs) {
   		this.tolerateStubs = tolerateStubs;
   	}
   	
   	void useExtendedTypeChecking(boolean val) {
   		this.useExtendedTypeChecking = val;
   	}

	public boolean equals(Object other) {
		if(other instanceof BT_StackShapes) {
			return equals((BT_StackShapes) other);
		}
		return false;
	}
	
	public boolean equals(BT_StackShapes other) {
		if(this == other) {
			return true;
		}
		return code.equals(other.code) && equals(stackShapes, other.stackShapes) < 0 && equals(localShapes, other.localShapes) < 0;
	}
	
	public static int equals(Object first[][], Object second[][]) {
		int length = first.length;
		if(length != second.length) {
			return (length < second.length) ? length : second.length;
		}
		//for(int i=length - 1; i>=0; i--) {
		for(int i=0; i<length; i++) {
			Object one[] = first[i];
			Object two[] = second[i];
			if(one == null) {
				if(two != null) {
					return i;
				}
			} else if(two == null) {
				return i;
			} else {
				if(one.length != two.length) {
					return i;
				}
				for(int j=0; j<one.length; j++) {
					Object cellOne = one[j];
					Object cellTwo = two[j];
					if(cellOne == null) {
						if(cellTwo != null) {
							return i;
						}
					} else if(cellTwo == null) {
						return i;
					} else {
						if(!cellOne.equals(cellTwo)) {
							return i;
						}
					}
				}
			}
		}
		return -1;
	}
	
	public boolean hasJSR() {
   		return hasJSR;
   	}
   	
   	public void print(PrintStream stream) {
    	BT_InsVector ins = code.getInstructions();
    	ins.initializeLabels();
    	stream.println("\t\t\t Initial");
		if(mergedInitial) {
    		stream.print(BT_StackType.toString(initialLocals, BT_StackPool.emptyStack));
    		
    		if(frames != null) {
				BT_StackMapFrame frame = frames[0];
				if(frame != null) {
					stream.print("\t\t\t ");
					stream.println(frame.getName());
				}
			} else {
				stream.println();
			}
    	}
    	stream.print(BT_StackType.toString(localShapes[0], stackShapes[0]));
    	for (int n = 1; n <= ins.size(); n++) {
			BT_Ins in1 = ins.elementAt(n - 1);
			stream.println("\t\t" + (n - 1) + "\t" + in1 + (isMergeInstruction[n - 1] ? " merged" : ""));
			if(n == ins.size()) {
				break;
			}
			if(frames != null) {
				BT_StackMapFrame frame = frames[n];
				if(frame != null) {
					stream.print("\t\t\t ");
					stream.println(frame.getName());
				}
			}
			BT_LocalCell locals[] = localShapes[n];
			BT_StackCell stack[] = stackShapes[n];
	    	stream.print(BT_StackType.toString(locals, stack));
	    }
		
		BT_ExceptionTableEntryVector exceptionTableEntries = code.getExceptionTableEntries();
		for (int n = 0; n < exceptionTableEntries.size(); n++) {
			stream.println("\t\t" + exceptionTableEntries.elementAt(n));
		}
    }
    
    void createInitialStacks() {
    	/* now create the initial stacks */
    	BT_LocalCell initialLocals[] = getInitialLocalStack();
    	localShapes[0] = pool.getDuplicate(initialLocals);
    	stackShapes[0] = BT_StackPool.emptyStack;
    }
    
    /**
     * Remove dead code from the code attribute, using the data from this object.
     */
    public boolean removeDeadCode() {
    	BT_InsVector ins = code.getInstructions();
    	int originalLen = ins.size();
    	if(originalLen != stackShapes.length || originalLen != localShapes.length || originalLen != isMergeInstruction.length) {
    		throw new IllegalStateException();
    	}
    	
    	boolean isNotLive[] = null;
    	int toDelete = 0;
		boolean result = false;
		for(int i=originalLen - 1; i>=0; i--) {
			if(stackShapes[i] != null && localShapes[i] != null) { //the instruction is reachable
				if (toDelete > 0) {
					result |= code.removeInstructionsAt(toDelete, i+1);
					toDelete = 0;
				}
			} else {
				if(isNotLive == null) {
					isNotLive = new boolean[originalLen];
				}
				isNotLive[i] = true;
				toDelete++;
			}
		}
		if(isNotLive != null) { /* there is dead code */
			if (toDelete > 0) { /* remove any dead code remaining */
				result |= code.removeInstructionsAt(toDelete, 0);
			}
			int newCodeSize = ins.size();
			if(newCodeSize >= originalLen) {
				throw new IllegalStateException();
			}
			/* adjust the data in this object to reflect the new code */
			BT_StackCell newStackShapes[][] = new BT_StackCell[newCodeSize][]; //the stack shape at index i is the shape of the stack before executing the instruction at index i
			BT_LocalCell newLocalShapes[][] = new BT_LocalCell[newCodeSize][]; //the locals shape at index i is the shape of the local variables before executing the instruction at index i
			boolean newIsMergeInstruction[] = new boolean[newCodeSize]; //isMergeInstruction at index i is true if the instruction at index i is reachable from at least two instructions
	    	for(int k=0, j = 0; k<originalLen; k++) {
				if(!isNotLive[k]) {
					newStackShapes[j] = stackShapes[k];
					newLocalShapes[j] = localShapes[k];
					newIsMergeInstruction[j] = isMergeInstruction[k];
					j++;
				} else {
					if(stackShapes[k] != null && localShapes[k] != null) {
						throw new IllegalStateException();
					}
				}
			}
			//no need to return any stacks since they are null at the unvisited instruction indices
			this.stackShapes = newStackShapes;
			this.localShapes = newLocalShapes;
			this.isMergeInstruction = newIsMergeInstruction;
		}
		return result;
    }
    
    public void returnStacks() {
    	if(!returnedStacks) {
    		returnedStacks = true;
    		if(stackShapes != null) {
    			pool.returnStacks(stackShapes);
    			Arrays.fill(stackShapes, null);
        	}
    		if(localShapes != null) {
    			pool.returnLocals(localShapes);
    			Arrays.fill(localShapes, null);
        	}
    		pool.returnLocals(initialLocals);
    		initialLocals = null;
    	}
    }
    
    private BT_Class getArrayObjectStackType(BT_StackType type, int depth, BT_Ins instruction, int instructionIndex) 
		throws BT_ExpectedObjectTypeException, BT_ExpectedArrayTypeException {
    	if(!type.isObjectType()) {
			throw new BT_ExpectedArrayTypeException(code, instruction, instructionIndex, type, depth);
		}
    	BT_Class clazz = type.getClassType().getType();
		if(clazz != null) {
			if(!clazz.isArray()) {
				throw new BT_ExpectedArrayTypeException(code, instruction, instructionIndex, type, depth);
			}
		}
		return clazz;
	}
    
    private BT_Class getObjectStackType(BT_Class expected, BT_StackType type, int depth, BT_Ins instruction, int instructionIndex) 
		throws BT_ExpectedObjectTypeException, BT_UninitializedObjectTypeException {
    	//store the object to array or throw the object;
    	checkStackTypeIsObject(expected, type, depth, instruction, instructionIndex);
    	return checkObjectStackType(expected, type, depth, instruction, instructionIndex);
	}
    
    private BT_Class checkObjectStackType(BT_Class expected, BT_StackType type, int depth, BT_Ins instruction, int instructionIndex) 
    		throws BT_ExpectedObjectTypeException {
    	BT_Class clazz = type.getClassType().getType();
    	/* clazz == null means the null type is on the stack, which is ok */
		if(clazz != null) {
			if(!(tolerateStubs && type.isStubObjectType()) && !expected.mightBeInstance(clazz)) {
				throw new BT_ExpectedObjectTypeException(code, instruction, instructionIndex, expected, type, depth);
			}
		}
		return clazz;
    }
    
    private void checkStackTypeIsObject(BT_Class expected, BT_StackCell stack[], int depth, BT_Ins instruction, int instructionIndex) 
		throws BT_ExpectedObjectTypeException, BT_StackUnderflowException, BT_UninitializedObjectTypeException {
		//areturn
    	BT_StackType stackItem = getStackItem(stack, stack.length - depth - 1, instruction, instructionIndex);
		checkStackTypeIsObject(expected, stackItem, depth, instruction, instructionIndex);
		if(!expected.isInterface()) {
			//whenever an interface type is expected, we can accept any object, and that is because merging interfaces results in java.lang.Object,
			//since there is no single type hierarchy to choose from otherwise when merging (ie superclass plus multiple interfaces)
			checkObjectStackType(expected, stackItem, depth, instruction, instructionIndex);
		}
	}
    
    private void checkStackTypeIsRuntimeObject(BT_Class expected, BT_StackCell stack[], int depth, BT_Ins instruction, int instructionIndex) 
		throws BT_ExpectedObjectTypeException, BT_StackUnderflowException, BT_UninitializedObjectTypeException {
		//checkcast, instanceof
    	BT_StackType stackItem = getStackItem(stack, stack.length - depth - 1, instruction, instructionIndex);
		checkStackTypeIsObject(null, stackItem, depth, instruction, instructionIndex);
		//is it possible for the checkcast to succeed or the instanceof expression to be true?  If not, note that (but keep in mind it's not a verify exception)
		//this makes no sense except for simple cases.  
		//1. If A implements X and Y, and Y has no relation to X, it is still valid to cast an item of type X to Y
		//because the item of type X might be A, so there is no reason to assume a relation between X and Y
		//2. Even with just superclass inheritance, you might have an java.lang.Object argument and attempt to cast that arg to a subtype
		//and in fact the case might succeed in some cases, but there is no indication that it can succeed using flow analysis
//		if(!stackItem.isNull() && !stackItem.getClassType().getType().mightBeInstance(expected)) {
//			BT_Repository rep = expected.getRepository();
//			BT_ExpectedObjectTypeException e = new BT_ExpectedObjectTypeException(code, instruction, instructionIndex, expected, stackItem, depth);
//			rep.factory.noteCodeIrregularity(e);
//		}
	}
    
    private void checkStackTypeIsObject(BT_StackCell stack[], int depth, BT_Ins instruction, int instructionIndex) 
		throws BT_ExpectedObjectTypeException, BT_StackUnderflowException, BT_UninitializedObjectTypeException {
    	//comparisons of object != and ==, null checks, monitor enter and monitor exit;
    	BT_StackType stackItem = getStackItem(stack, stack.length - depth - 1, instruction, instructionIndex);
    	checkStackTypeIsObject(null, stackItem, depth, instruction, instructionIndex);
	}
    
    private void checkStackTypeIsObject(BT_Class expected, BT_StackType type, int depth, BT_Ins instruction, int instructionIndex) 
    	throws BT_ExpectedObjectTypeException, BT_UninitializedObjectTypeException {
    	if(!type.isObjectType()) {
    		if(type.isUninitializedObject()) {
    			if(expected == null) {
        			throw new BT_UninitializedObjectTypeException(code, instruction, instructionIndex, type, depth);
        		} else {
        			throw new BT_UninitializedObjectTypeException(code, instruction, instructionIndex, expected, type, depth);
        		}
    		}
    		if(expected == null) {
    			throw new BT_ExpectedObjectTypeException(code, instruction, instructionIndex, type, depth);
    		} else {
    			throw new BT_ExpectedObjectTypeException(code, instruction, instructionIndex, expected, type, depth);
    		}
		}
    }
    
    private void checkStackPrimitiveType(BT_StackCell stack[], int depth, ClassType expectedType, BT_Ins instruction, int instructionIndex) 
    		throws BT_ExpectedPrimitiveTypeException, BT_StackUnderflowException {
    	BT_StackType typeOnStack = getStackItem(stack, stack.length - depth - 1, instruction, instructionIndex);
    	if(!typeOnStack.equals(expectedType)) {
    		throw new BT_ExpectedPrimitiveTypeException(code, instruction, instructionIndex, expectedType.getType(), typeOnStack, depth);
    	}
    }
    
    private void checkNotSplit(BT_StackCell stack[], int depth, BT_Ins instruction, int instructionIndex) 
    		throws BT_SplitDoubleWordException, BT_StackUnderflowException {
    	//we must check that the item at the given depth is not the upper item of a long or double
    	int index = stack.length - depth - 1;
    	if(index == 0) {
    		return;
    	}
    	if(index > 0) {
    		BT_StackType split = stack[index - 1].getCellType();
    		if(split.isTwoSlot()) {
    			throw new BT_SplitDoubleWordException(code, instruction, instructionIndex, split, depth + 1);
    		}
    	} else /* index < 0 */ {
    		throw new BT_StackUnderflowException(code, instruction, instructionIndex);
    	}
    }
    
    private void checkArgument(
			BT_Ins ins,
			int instructionIndex,
			BT_Member target,
			int depth,
			BT_Class arg,
			BT_StackType typeOnStack)
				throws BT_InvalidArgumentTypeException, BT_UninitializedObjectTypeException {
    	if(!typeOnStack.isClassType()) {
    		if(typeOnStack.isUninitializedObject()) {
    			throw new BT_UninitializedObjectTypeException(code, ins, instructionIndex, arg, typeOnStack, depth);
    		}
			if(target instanceof BT_Field) {
				throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
						typeOnStack, (BT_Field) target);
			}
			throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
					arg, typeOnStack, depth, (BT_Method) target);
		}
		if(typeOnStack.isNull()) {
			if(arg.isPrimitive()) {
				if(target instanceof BT_Field) {
					throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
							typeOnStack, (BT_Field) target);
				}
				throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
						arg, typeOnStack, depth, (BT_Method) target);
			}
		} else {
			BT_Class classTypeOnStack = typeOnStack.getClassType().getType();
			if(classTypeOnStack.isPrimitive()) {
				BT_Class convertedArg = arg.classType.convert().type;
				if(!classTypeOnStack.equals(convertedArg)) {
					if(target instanceof BT_Field) {
						throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
								typeOnStack, (BT_Field) target);
					}
					throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
							arg, typeOnStack, depth, (BT_Method) target);
				}
			} else {
				/*
				for interface parameters and types, the type check must be done at runtime.
				this is because merging two interfaces always results in java.lang.Object
				so we do a quick check for Object to handle that one condition
				
				The trouble with interfaces is that a merge can result in various possible choices.
				If a merge might result in X or Y, and you choose X, and then later we must verify
				that the type is Y, then we have a problem, we must check that there exists an object
				that could be X or Y.
				
				
				Suppose two separate types have the same superclass S, 
				and both implement the same two interfaces X and Y.  
				When a merge takes place, their types will be merged to one of the three choices (S, X, Y), 
				but we must choose the object type, because it is not possible to choose between 
				interface types (how to choose?  and how to know both objects were of all 3 types 
				after the merge?).  So it is not possible to do a type check to ensure the merged 
				object is of type X and Y, even though it is, in this case.
				*/
				
				//We could create a modified ClassType that stores all the possible interfaces that
				//could be the result of an interface merge, and then the call to getType() above
				//could be modified to getTypes() which would be a series of interfaces, but it's
				//likely not worth the trouble
				
				
				if(!arg.isInterface()) {
					if(!(tolerateStubs && typeOnStack.isStubObjectType()) && !arg.mightBeInstance(classTypeOnStack)) {
						if(target instanceof BT_Field) {
							throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
									typeOnStack, (BT_Field) target);
						}
						throw new BT_InvalidArgumentTypeException(code, ins, instructionIndex, 
								arg, typeOnStack, depth, (BT_Method) target);
					}
				}
			}
		}
	}
    
    
    /**
     * Performs checking on the stacks that was not needed to construct them.
     * This is only checking that is in fact related to the stacks.  
     * Some generalized checking on the instructions is performed by the loading process and BT_CodeAttribute.verifyRelationships.
     * 
     * This method is meant to be called after the stacks have been populated.  
     * If they have not been populated this method does nothing.
     */
    public void verifyStacks() throws BT_InvalidStackTypeException, BT_StackUnderflowException, BT_AccessException {
    	BT_InsVector insVector = code.getInstructions();
    	BT_Method currentMethod = code.getMethod();
    	for(int i=0; i<insVector.size(); i++) {
    		BT_Ins ins = insVector.elementAt(i);
    		BT_StackCell stack[] = stackShapes[i];
    		if(stack == null) {
    			continue;
    			//this instruction was never visited
    		}
    		
    		/* note: 
    		 * -local instructions have been verified during stack construction 
    		 * -jump targets and all instruction references have been verified during instruction object creation (BT_Ins.make)
    		 * -the constants referred to by constant instructions are verified during instruction object creation (BT_Ins.make)
    		 */
    		
    		int opcode = ins.opcode;
    		switch(opcode) {
    			case opc_dup_x2 :
    				checkNotSplit(stack, 0, ins, i);
    				checkNotSplit(stack, 2, ins, i);
    				break;
    			case opc_swap :
    			case opc_dup_x1 :
    				checkNotSplit(stack, 1, ins, i);
    				/* fall through */
    			case opc_pop :
    			case opc_dup :
    				checkNotSplit(stack, 0, ins, i);
    				break;
				case opc_dup2_x1 :
					checkNotSplit(stack, 2, ins, i);
					/* fall through */
				case opc_pop2 :
				case opc_dup2 :
					checkNotSplit(stack, 1, ins, i);
					break;
				case opc_dup2_x2 :
					checkNotSplit(stack, 1, ins, i);
					checkNotSplit(stack, 3, ins, i);
					break;
				case opc_if_icmpeq :
				case opc_if_icmpne :
				case opc_if_icmplt :
				case opc_if_icmpge :
				case opc_if_icmpgt :
				case opc_if_icmple :
				case opc_iadd :
				case opc_isub :
				case opc_imul :
				case opc_idiv :
				case opc_irem :
				case opc_ishl :
				case opc_ishr :
				case opc_iushr :
				case opc_iand :
				case opc_ixor :
				case opc_ior :
					checkStackPrimitiveType(stack, 1, provider.intClass, ins, i);
					/* fall through */
				case opc_ireturn :
				case opc_i2f :
				case opc_i2l :
				case opc_i2d :
				case opc_ifeq :
				case opc_ifne :
				case opc_iflt :
				case opc_ifge :
				case opc_ifgt :
				case opc_ifle :
				case opc_int2byte :
				case opc_int2char :
				case opc_int2short :
				case opc_tableswitch :
				case opc_lookupswitch :
				case opc_ineg :
					checkStackPrimitiveType(stack, 0, provider.intClass, ins, i);
					break;
				case opc_ladd :
				case opc_lsub :
				case opc_lmul :
				case opc_ldiv :
				case opc_lrem :
				case opc_lcmp :
				case opc_land :
				case opc_lor :
				case opc_lxor :
					checkStackPrimitiveType(stack, 3, provider.longClass, ins, i);
					/* fall through */
				case opc_lreturn :
				case opc_l2i :
				case opc_l2f :
				case opc_l2d :
				case opc_lneg :
					checkStackPrimitiveType(stack, 1, provider.longClass, ins, i);
					break;
				case opc_lshl :
				case opc_lshr :
				case opc_lushr :
					checkStackPrimitiveType(stack, 0, provider.intClass, ins, i);
					checkStackPrimitiveType(stack, 2, provider.longClass, ins, i);
					break;
				case opc_fcmpl :
				case opc_fcmpg :
				case opc_frem :
				case opc_fdiv :
				case opc_fmul :
				case opc_fsub :
				case opc_fadd :
					checkStackPrimitiveType(stack, 1, provider.floatClass, ins, i);
					/* fall through */
				case opc_freturn :
				case opc_f2i :
				case opc_f2d :
				case opc_f2l :
				case opc_fneg :
					checkStackPrimitiveType(stack, 0, provider.floatClass, ins, i);
					break;
				case opc_dcmpl :
				case opc_dcmpg :
				case opc_dadd :
				case opc_dsub :
				case opc_dmul :
				case opc_ddiv :
				case opc_drem :
					checkStackPrimitiveType(stack, 3, provider.doubleClass, ins, i);
					/* fall through */
				case opc_dreturn :
				case opc_d2i :
				case opc_d2f :
				case opc_d2l :
				case opc_dneg :
					checkStackPrimitiveType(stack, 1, provider.doubleClass, ins, i);
					break;
				case opc_arraylength :
					BT_StackType stackItem = getStackItem(stack, stack.length - 1, ins, i);
			    	getArrayObjectStackType(stackItem, 0, ins, i);
					break;
				case opc_iaload :
				case opc_baload :
				case opc_caload :
				case opc_saload :
				case opc_laload :
				case opc_faload :
				case opc_daload :
				case opc_aaload : /* note that aaload has checks in BT_StackShapeVisitor already */
					stackItem = getStackItem(stack, stack.length - 2, ins, i);
					BT_Class clazz = getArrayObjectStackType(stackItem, 1, ins, i);
					checkStackPrimitiveType(stack, 0, provider.intClass, ins, i);
					break;
				case opc_lastore :
				case opc_dastore :
					stackItem = getStackItem(stack, stack.length - 4, ins, i);
					clazz = getArrayObjectStackType(stackItem, 3, ins, i);
					checkStackPrimitiveType(stack, 2, provider.intClass, ins, i);
					switch(opcode) {
						case opc_lastore :
							checkStackPrimitiveType(stack, 1, provider.longClass, ins, i);
							break;
						case opc_dastore :
							checkStackPrimitiveType(stack, 1, provider.doubleClass, ins, i);
							break;
					}
					break;
				case opc_fastore :
				case opc_aastore :
				case opc_iastore :
				case opc_bastore :
				case opc_castore :
				case opc_sastore :
					stackItem = getStackItem(stack, stack.length - 3, ins, i);
					clazz = getArrayObjectStackType(stackItem, 2, ins, i);
					checkStackPrimitiveType(stack, 1, provider.intClass, ins, i);
					switch(opcode) {
						case opc_fastore :
							checkStackPrimitiveType(stack, 0, provider.floatClass, ins, i);
							break;
						case opc_aastore :
							stackItem = getStackItem(stack, stack.length - 1, ins, i);
							if(useExtendedTypeChecking) {
								getObjectStackType(clazz.getElementClass(), stackItem, 0, ins, i);
							} else  {
								checkStackTypeIsObject(null, stackItem, 0, ins, i);
							}
							break;
						case opc_iastore :
						case opc_bastore :
						case opc_castore :
						case opc_sastore :
							checkStackPrimitiveType(stack, 0, provider.intClass, ins, i);
							break;
					}
					break;
				case opc_if_acmpeq :
				case opc_if_acmpne :
					checkStackTypeIsObject(stack, 1, ins, i);
					/* fall through */
				case opc_ifnull :
				case opc_ifnonnull :
				case opc_monitorenter :
				case opc_monitorexit :
					checkStackTypeIsObject(stack, 0, ins, i);
    				break;
				case opc_areturn :
					checkStackTypeIsObject(currentMethod.getSignature().returnType, stack, 0, ins, i);
    				break;
				case opc_instanceof :
    			case opc_checkcast:
    				checkStackTypeIsRuntimeObject(ins.getClassTarget(), stack, 0, ins, i);
    				break;
				case opc_athrow :
					stackItem = getStackItem(stack, stack.length - 1, ins, i);
					getObjectStackType(provider.javaLangThrowable, stackItem, 0, ins, i);
					break;
    			case opc_invokeinterface :
    			case opc_invokevirtual :
    			case opc_invokespecial :
    				BT_MethodRefIns methodRefIns = (BT_MethodRefIns) ins;
    				BT_Method target = methodRefIns.getMethodTarget();
    				BT_MethodSignature targetSignature = target.getSignature();
    				stackItem = getStackItem(stack, stack.length - targetSignature.getArgsSize() - 1, ins, i);
    				if(!stackItem.isObjectType()) {
    					if(stackItem.isUninitializedObject()) {
    						if(!(methodRefIns.isInvokeSpecialIns() && target.isConstructor())) {
    							throw new BT_UninitializedObjectTypeException(code, ins, i, methodRefIns.getClassTarget(), stackItem, targetSignature.getArgsSize());
    						} else if(!stackItem.isUninitializedThis()) {
    							//4.9.2 
    							//UninitializedObject type must match the invokespecial target class which must have that init method
        						//when we resolve inside BT_MethodRefIns we already check that the method is not inherited
        						//so here we just need to check that it matches the preceding new instruction
    							UninitializedObject uninit = (UninitializedObject) stackItem;
    							clazz = uninit.creatingInstruction.getClassTarget();
    							if(!target.getDeclaringClass().equals(clazz)) {
    								throw new BT_UninitializedObjectTypeException(code, ins, i, methodRefIns.getClassTarget(), stackItem, targetSignature.getArgsSize());
    							}
    						}
    					} else {
    						throw new BT_ExpectedObjectTypeException(code, ins, i, stackItem, targetSignature.getArgsSize());
    					}
    				} else {
    					ClassType classType = stackItem.getClassType();
	    				if(!classType.isNull()) {
		    				clazz = classType.type;
		    				/*
		    				for interface parameters, the type check must be done at runtime.
		    				this is because merging classes uses super classes.  So you might
		    				have two classes, both of which implement an interface, but when
		    				merged the merged class does not.
		    				*/
		    				BT_Class targetClass = methodRefIns.getResolvedClassTarget(code);
		    				if(!targetClass.isInterface()) {
			    				if(!(tolerateStubs && classType.isStubObjectType()) && !targetClass.mightBeInstance(clazz)) {
			    					throw new BT_ExpectedObjectTypeException(code, ins, i, targetClass, stackItem, targetSignature.getArgsSize());
			    				}
		    				}
	    				}
    				}
    				checkArguments(ins, i, stack, target, targetSignature);
    				if(opcode != opc_invokeinterface) {
    					/*
    					invokespecial/invokevirtual: If the
    					resolved method is protected (§4.6), and it is a member of a superclass of the
    					current class, and the method is not declared in the same run-time
    					package (§5.3) as the current class, then the class of objectref must
    					be either the current class or a subclass of the current class.
    					*/
    					if(target.isProtected()) {
    						BT_Class currentClass = currentMethod.getDeclaringClass();
    						BT_Class methodClass = target.getDeclaringClass();
    						if(methodClass.isClassAncestorOf(currentClass)) {
    							if(!currentClass.isInSamePackage(methodClass)) {
    								 if(!stackItem.isNull() && !stackItem.isUninitializedThis()) {
    									 BT_Class receiverObjectClass;
    									 if(stackItem.isUninitializedObject()) {
    										 UninitializedObject uninitType = (UninitializedObject) stackItem;
    										 receiverObjectClass = uninitType.creatingInstruction.getClassTarget();
    									 } else {
    										 receiverObjectClass = stackItem.getClassType().getType();
    									 }
    									 if(!receiverObjectClass.isArray() /* call to clone() */ && !(currentClass.equals(receiverObjectClass) || currentClass.mightBeInstance(receiverObjectClass))) {
    										 throw new BT_AccessException(code, ins, i, target);
    									 }
    								 }
    							}
    						}
    					}
    				}
    				break;
				case opc_invokestatic :	
					methodRefIns = (BT_MethodRefIns) ins;
					target = methodRefIns.getMethodTarget();
					targetSignature = target.getSignature();
					checkArguments(ins, i, stack, target, targetSignature);
					break;
				case opc_putfield :
					BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) ins;
    				BT_Field targetField = fieldRefIns.getFieldTarget();
    				clazz = targetField.getFieldType();
    				int argSize = clazz.getSizeForLocal();
    				stackItem = getStackItem(stack, stack.length - argSize, ins, i);
    				checkArgument(ins, i, targetField, 0, clazz, stackItem);
    				stackItem = getStackItem(stack, stack.length - argSize - 1, ins, i);
    				checkAccessedObjectField(ins, i, stackItem, fieldRefIns, argSize);
					break;
				case opc_getfield :
	    			fieldRefIns = (BT_FieldRefIns) ins;
	    			stackItem =  getStackItem(stack, stack.length - 1, ins, i);
	    			checkAccessedObjectField(ins, i, stackItem, fieldRefIns, 0);
					break;
	    		case opc_putstatic :
					fieldRefIns = (BT_FieldRefIns) ins;
    				targetField = fieldRefIns.getFieldTarget();
    				clazz = targetField.getFieldType();
    				stackItem = getStackItem(stack, stack.length - clazz.getSizeForLocal(), ins, i);
    				checkArgument(ins, i, targetField, 0, clazz, stackItem);
					break;
				/*	
				 * No checks are required on the following opcodes, they have been fully verified already
				 * either by the loading process or by the process of generating the stack maps 
				 */
				/*
				case opc_nop:
	    		case opc_aconst_null:
	    		case opc_iconst_m1 :
	    		case opc_iconst_0 :
	    		case opc_iconst_1 :
	    		case opc_iconst_2:
	    		case opc_iconst_3:
	    		case opc_iconst_4 :
	    		case opc_iconst_5 :
	    		case opc_lconst_0 :
	    		case opc_lconst_1 :
	    		case opc_fconst_0 :
	    		case opc_fconst_1 :
	    		case opc_fconst_2 :
	    		case opc_dconst_0:
	    		case opc_dconst_1:
	    		case opc_bipush:
	    		case opc_sipush :
	    		case opc_ldc :
	    		case opc_ldc_w :
	    		case opc_ldc2_w :
	    		case opc_iload :
	    		case opc_lload :
	    		case opc_fload :
	    		case opc_dload :
	    		case opc_aload :
	    		case opc_iload_0 :
	    		case opc_iload_1 :
	    		case opc_iload_2 :
	    		case opc_iload_3 :
	    		case opc_lload_0 :
	    		case opc_lload_1 :
	    		case opc_lload_2 :
	    		case opc_lload_3 :
	    		case opc_fload_0 :
	    		case opc_fload_1 :
	    		case opc_fload_2 :
	    		case opc_fload_3 :
	    		case opc_dload_0 :
	    		case opc_dload_1 :
	    		case opc_dload_2 :
	    		case opc_dload_3 :
	    		case opc_aload_0 :
	    		case opc_aload_1 :
	    		case opc_aload_2 :
	    		case opc_aload_3 :
	    		case opc_istore:
	    		case opc_lstore :
	    		case opc_fstore :
	    		case opc_dstore :
	    		case opc_astore :
	    		case opc_istore_0 :
	    		case opc_istore_1 :
	    		case opc_istore_2 :
	    		case opc_istore_3 :
	    		case opc_lstore_0 :
	    		case opc_lstore_1 :
	    		case opc_lstore_2 :
	    		case opc_lstore_3 :
	    		case opc_fstore_0 :
	    		case opc_fstore_1:
	    		case opc_fstore_2:
	    		case opc_fstore_3 :
	    		case opc_dstore_0 :
	    		case opc_dstore_1 :
	    		case opc_dstore_2 :
	    		case opc_dstore_3 :
	    		case opc_astore_0 :
	    		case opc_astore_1 :
	    		case opc_astore_2 :
	    		case opc_astore_3 :
	    		case opc_iinc :
		    	case opc_goto :
	    		case opc_jsr :
	    		case opc_ret :
	    		case opc_return :
	    		case opc_getstatic :
	    		case opc_new :
	    		case opc_newarray :
	    		case opc_anewarray :
	    		case opc_wide :
	    		case opc_multianewarray :
	    		case opc_goto_w :
	    		case opc_jsr_w :
	    		*/
	    		default:
					break;
    		} /* end switch */
    	} /* end loop through instructions */
    }
    
    BT_StackType getStackItem(BT_StackCell stack[], int stackIndex, BT_Ins ins, int instructionIndex) throws BT_StackUnderflowException {
    	if(stack.length <= stackIndex || stackIndex < 0) {
			throw new BT_StackUnderflowException(code, ins, instructionIndex);
		}
		return stack[stackIndex].getCellType();
    }

	/**
	 * @param ins
	 * @param i
	 * @param stack
	 * @param target
	 * @param targetSignature
	 * @throws BT_InvalidArgumentTypeException
	 */
	private void checkArguments(BT_Ins ins, int i, BT_StackCell[] stack, BT_Method target, BT_MethodSignature targetSignature) 
		throws BT_InvalidArgumentTypeException, BT_UninitializedObjectTypeException, BT_StackUnderflowException {
		BT_ClassVector argTypes = targetSignature.types;
		for(int k=argTypes.size() - 1, depth = -1; k>=0; k--) {
			BT_Class clazz = argTypes.elementAt(k);
			depth += clazz.getSizeForLocal();
			BT_StackType stackItem = getStackItem(stack, stack.length - depth - 1, ins, i);
			checkArgument(ins, i, target, depth, clazz, stackItem);
		}
	}

	public static boolean isAllowedUninitializedFieldAccess(BT_StackType accessedObjectType, BT_FieldRefIns fieldRefIns) {
		/* this case is a bug in the sun compiler, which seems to allow assigning to compiler-generated synthetic fields before the super constructor is called, as shown below:
		  class java.util.AbstractList$Itr extends java.lang.Object implements java.util.Iterator {
				final java.util.AbstractList this$0;
			}
			private void <init>(java.util.AbstractList)
			    0 JBaload0 
			    1 JBaload1 
			    2 JBputfield 63 java.util.AbstractList$Itr.this$0 Ljava.util.AbstractList;
			    5 JBaload0 
			    6 JBinvokespecial 65 java.lang.Object.<init>()V
			    
			This has been seen in other places as well, such as in auto-generated code, not just the sun compiler.
		 */
		/* BT_Field field = fieldRefIns.getFieldTarget(); */
		return fieldRefIns.isFieldWriteIns() 
			/* for older class file versions, account for the fact that the synthetic attribute might have been removed */
			/* && (field.isSynthetic() || !field.getVersion().canUseSyntheticFlag()) this has been seen with non-synthetic fields */
			&& accessedObjectType.isUninitializedThis();
	}
	
	/**
	 * @param i
	 * @param ins
	 * @param invokedObjectType
	 * @param fieldRefIns
	 * @throws BT_ExpectedObjectTypeException
	 */
	private void checkAccessedObjectField(BT_Ins ins, int instructionIndex, BT_StackType accessedObjectType, BT_FieldRefIns fieldRefIns, int stackDepth) 
		throws BT_ExpectedObjectTypeException, BT_UninitializedObjectTypeException, BT_AccessException {
		if(!accessedObjectType.isObjectType()) {
			if(isAllowedUninitializedFieldAccess(accessedObjectType, fieldRefIns)) {
				/* first we check if the code is possibly accessing a field in the current class and the current method is a constructor (note the field is not static) */
				BT_UninitializedObjectTypeException e = new BT_UninitializedObjectTypeException(code, ins, instructionIndex, fieldRefIns.getClassTarget(), accessedObjectType, stackDepth);
				BT_Method currentMethod = code.getMethod();
				BT_Class currentClass = code.getMethod().cls;
				if(!(fieldRefIns.getResolvedClassTarget(code).mightBeInstance(currentClass) && currentMethod.isConstructor())) {
					throw e;
				}
				//if the target is synthetic then let's not bother saying anything,
				//apparently compilers think it's fine so it must run ok
				BT_Field field = fieldRefIns.getFieldTarget();
				if(!field.isSynthetic() && field.getVersion().canUseSyntheticFlag()) {
					BT_Repository rep = currentClass.getRepository();
					rep.factory.noteCodeIrregularity(e);
				}
				return;
			} else {
				if(accessedObjectType.isUninitializedObject()) {
					throw new BT_UninitializedObjectTypeException(code, ins, instructionIndex, fieldRefIns.getClassTarget(), accessedObjectType, stackDepth);
				}
				throw new BT_ExpectedObjectTypeException(code, ins, instructionIndex, accessedObjectType, stackDepth);
			}
		}
		ClassType classType = accessedObjectType.getClassType();
		if(!classType.isNull()) {
			BT_Class receiverObjectClass = classType.type;
			if(!fieldRefIns.getResolvedClassTarget(code).mightBeInstance(receiverObjectClass)) {
				throw new BT_ExpectedObjectTypeException(code, ins, instructionIndex, accessedObjectType, stackDepth);
			}
		}
		/*
		putfield/getfield: If the field is protected (§4.6), and it is a member of a superclass of the
		current class, and the field is not declared in the same run-time
		package (§5.3) as the current class, then the class of objectref must
		be either the current class or a subclass of the current class.
		 */
		BT_Field targetField = fieldRefIns.getFieldTarget();
		if(targetField.isProtected()) {
			BT_Method currentMethod = code.getMethod();
			BT_Class currentClass = currentMethod.getDeclaringClass();
			BT_Class fieldClass = targetField.getDeclaringClass();
			if(fieldClass.isClassAncestorOf(currentClass)) {
				if(!currentClass.isInSamePackage(fieldClass)) {
					 if(!classType.isNull()) {
						 BT_Class receiverObjectClass = classType.getType();
						 if(!(currentClass.equals(receiverObjectClass) || currentClass.mightBeInstance(receiverObjectClass))) {
							 throw new BT_AccessException(code, ins, instructionIndex, targetField);
						 }
					 }
				}
			}
		}
	}
}
