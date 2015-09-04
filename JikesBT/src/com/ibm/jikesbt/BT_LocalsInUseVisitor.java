/*
 * Created on Aug 7, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.ibm.jikesbt.BT_ObjectCode.SubRoutine;
import com.ibm.jikesbt.BT_CodeException.BT_UninitializedLocalException;
import com.ibm.jikesbt.BT_StackType.UnknownType;



/**
 * @author Sean Foley
 *
 * Determines the locals that are in use at a given instruction index.  A local is in use, if at run-time, 
 * the value contained within the local might be read after the instruction at the given index is executed.
 * This does not take into account the instruction at the given instruction index.
 * In other words, it is safe to overwrite the value in the given local after the given instruction index.
 */
public class BT_LocalsInUseVisitor extends BT_CodeVisitor implements BT_Opcodes {
	final private int instructionIndex;
	final static private UnknownType initialValue = BT_StackType.UNKNOWN_TYPE;
	final private BT_StackPool pool;  //used to cache unused stacks for later use (for performance)
	private HashMap subRoutineMap; //maps objects of type SubRoutine to a List of Boolean : represents the locals altered by each subroutine
	private boolean returnedStacks;
    
	private boolean changed[]; //changed[i] is true if the shapes at index have changed since the last visit
	private boolean newlyChanged[]; //newlyChanged[i] is true if the shapes at index i have changed during this visit
	public boolean done; //whether another visit is required after the current visit
	public BT_LocalCell localShapes[][]; //the locals shape at index i is the shape of the local variables before executing the instruction at index i
	
	private int maxLocalsAtIndex;
	
	private ArrayList localIsInUse = new ArrayList();
	public int maxLocals;
	public int localsInUse; //keeps track of the number is elements of localIsInUse that are true
	
	private boolean firstVisit;
	
	private int highestPossibleInUse;
	private boolean canExitEarly;
	private boolean foundHighest;
	
	BT_LocalsInUseVisitor(BT_CodeAttribute code, BT_StackPool pool) {
		this.instructionIndex = 0;
		this.pool = pool;
		this.code = code;
		int codeSize = code.getInstructionSize();
		this.localShapes = new BT_LocalCell[codeSize][];
		super.revisitHandlers = true;
		initializeMethodArguments(code, pool);
	}
	
	BT_LocalsInUseVisitor(BT_CodeAttribute code, BT_StackPool pool, int instructionIndex, int maxLocalsAtIndex) {
		this.instructionIndex = instructionIndex;
		this.pool = pool;
		this.code = code;
		this.maxLocals = maxLocalsAtIndex;
		this.maxLocalsAtIndex = maxLocalsAtIndex;
		int codeSize = code.getInstructionSize();
		this.localShapes = new BT_LocalCell[codeSize][];
		super.revisitHandlers = true;
		if(instructionIndex == 0) {
			initializeMethodArguments(code, pool);
		} else {
			BT_LocalCell newLocals[] = pool.getLocals(maxLocalsAtIndex);
			localShapes[instructionIndex] = newLocals;
			for(int i=0; i<newLocals.length; i++) {
				newLocals[i] = initialValue;
			}
		}
	}
	
	public int getMaxLocals() {
		return maxLocals;
	}
	
	/**
	 * this method causes the visitor to complete when the highest local has been found to be in use.
	 * @param maxLocals
	 */
	public void setKnownMaxLocalsAndExitEarly(int maxLocals) {
		highestPossibleInUse = maxLocals - 1;
		canExitEarly = true;
	}
	
	/**
	 * @param code
	 * @param pool
	 * @param instructionIndex
	 */
	private void initializeMethodArguments(BT_CodeAttribute code, BT_StackPool pool) {
		BT_Method method = code.getMethod();
		BT_MethodSignature sig = method.getSignature();
		boolean isStatic = method.isStatic();
		maxLocalsAtIndex = maxLocals = sig.getArgsSize() + (isStatic ? 0 : 1);
		BT_LocalCell newLocals[] = pool.getLocals(maxLocals);
		localShapes[0] = newLocals;
		for(int i=0; i<newLocals.length; i++) {
			newLocals[i] = initialValue;
		}
	}
	
	private boolean isNotChanged(int iin) {
		return changed != null && !changed[iin] && !newlyChanged[iin];
	}
	
	private void markChange(boolean isChanged, int iin, boolean again) {
		if(isChanged) {
			if(again) {
				newlyChanged[iin] = true;
				done = false;
			} else {
				changed[iin] = true;
			}
		}
	}
	
	public synchronized void populate() throws BT_CodeException {
		do {
    		firstVisit = true;
    		code.visitReachableCode(this, code.getInstructions().elementAt(instructionIndex), null, ENTRY_POINT);
		} while(!done);
	}

    public synchronized void populateWithOutput(PrintStream stream) throws BT_CodeException {
    	do {
    		firstVisit = true;
			code.visitReachableCode(this, code.getInstructions().elementAt(instructionIndex), null, ENTRY_POINT);
    		print(stream);
			stream.println();
		} while(!done);
	}
    
    public synchronized void print(PrintStream stream) {
    	BT_InsVector ins = code.getInstructions();
    	for (int n = 0, nLabels = 0; n < ins.size(); n++) {
			BT_Ins in1 = ins.elementAt(n);
			if(in1.isBlockMarker()) {
				((BT_BasicBlockMarkerIns)in1).setLabel("label_" + (nLabels++));
			}	
		}
		
		String spaces = "                                        ";
		String filler = "----------------------------------------";
		printStacks(stream, spaces, filler, 0);
		for (int n = 1; n <= ins.size(); n++) {
			BT_Ins in1 = ins.elementAt(n - 1);
			stream.println("\t\t" + (n - 1) + "\t" + in1);
			if(n == ins.size()) {
				break;
			}
			printStacks(stream, spaces, filler, n);
		}
		
		BT_ExceptionTableEntryVector exceptionTableEntries = code.getExceptionTableEntries();
		for (int n = 0; n < exceptionTableEntries.size(); n++) {
			stream.println("\t\t" + exceptionTableEntries.elementAt(n));
		}
    }

    private void printStacks(PrintStream stream, String spaces, String filler, int n) {
    	BT_LocalCell locals[] = localShapes[n];
        if(locals == null) {
    		//this should only occur DURING a visit
        	return;
        }
        String tabs = "\t\t\t";
        for(int i=0; i<locals.length; i++) {
        	BT_LocalCell type = locals[i];
        	String local;
        	if(type == null) {
        		local = "";
        	} else {
        		local = type.toString();
        	}
        	stream.print(tabs);
        	stream.print(" [");
        	printElement(local, filler, stream);
        	stream.println(']');
        }
    }
    
    private static void printElement(String stackElement, String spaces, PrintStream stream) {
    	if(stackElement.length() > spaces.length()) {
			stream.print(stackElement.substring(0, spaces.length()));
		} else {
			stream.print(stackElement);
			stream.print(spaces.substring(stackElement.length()));
		}
    }
    
	protected void setUp() {
    	super.setUp();
    	if(newlyChanged == null) {
			newlyChanged = new boolean[code.getInstructionSize()];
		} else {
			for(int i=0; i<newlyChanged.length; i++) {
				newlyChanged[i] = false;
			}
		}
		done = true;
    }
	
	protected void tearDown() {
    	changed = newlyChanged;
    	newlyChanged = null;
    	super.tearDown();
    }
	
	protected void additionalVisit(
			BT_Ins instruction, 
			int iin, 
			BT_Ins previousInstruction, 
			int prev_iin, 
			BT_ExceptionTableEntry handler)
				throws BT_UninitializedLocalException {
		handleVisit(instruction, iin, previousInstruction, prev_iin, handler, true);
	}
	
    protected boolean visit(
    		BT_Ins instruction, 
    		int iin, 
    		BT_Ins previousInstruction, 
    		int prev_iin, 
    		BT_ExceptionTableEntry handler)
    			throws BT_UninitializedLocalException {
    	if(firstVisit) {
    		firstVisit = false;
    		return true;
    	}
    	return handleVisit(instruction, iin, previousInstruction, prev_iin, handler, false);
	}
    
    private boolean handleVisit(
    		BT_Ins instruction,
    		int iin,
    		BT_Ins previousInstruction,
    		int prev_iin,
    		BT_ExceptionTableEntry handler,
    		boolean again) throws BT_UninitializedLocalException {
		if(isNotChanged(prev_iin)) {
			return true;
		}
		if(handler != null) {
			//we have arrived at this instruction via an exception
			BT_LocalCell previousLocals[] = localShapes[prev_iin];
			BT_LocalCell newLocals[] = pool.getLocals(previousLocals.length);
			System.arraycopy(previousLocals, 0, newLocals, 0, previousLocals.length);
			merge(iin, newLocals, again);
			return true;
		} else {
			BT_LocalCell newLocals[] = getLocals(prev_iin, previousInstruction);
			boolean returningFromRet = previousInstruction.isRetIns();
			BT_LocalCell retLocals[] = null;
			if(returningFromRet) {
				retLocals = newLocals;
				newLocals = mergeJSRAndRetLocals(iin, retLocals);
				try {
					merge(iin, newLocals, again);
					if(foundHighest) {
						exit();
						done = true;
						return false;
					}
					return true;
				} finally {
					returnStack(retLocals);
				}
			} else {
				merge(iin, newLocals, again);
				if(foundHighest) {
					exit();
					done = true;
					return false;
				}
				return true;
			}
		}
	}
    
    private BT_LocalCell[] copyLocals(BT_LocalCell previousLocals[], int previousLocalsLength) {
    	BT_LocalCell newLocals[] = pool.getLocals(previousLocalsLength);
		System.arraycopy(previousLocals, 0, newLocals, 0, previousLocalsLength);
		return newLocals;
	}
    
    private BT_LocalCell[] getLocals(int prev_iin, BT_Ins previousInstruction) 
    		throws BT_UninitializedLocalException {
    	BT_LocalCell previousLocals[] = localShapes[prev_iin];
		int previousLocalsLength = previousLocals.length;
		BT_LocalCell newLocals[];
		int opcode = previousInstruction.opcode;
		switch (opcode) {	
			case opc_iload_0 :
			case opc_iload_1 :
			case opc_iload_2 :
			case opc_iload_3 :
			case opc_iload :
			case opc_fload_0 :
			case opc_fload_1 :
			case opc_fload_2 :
			case opc_fload_3 :
			case opc_fload :
			case opc_aload_0 :
			case opc_aload_1 :
			case opc_aload_2 :
			case opc_aload_3 :
			case opc_aload :
				BT_LoadLocalIns loadIns = (BT_LoadLocalIns) previousInstruction;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				BT_Local target = loadIns.target;
				int localNum = target.localNr;
				if(localNum >= previousLocalsLength) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				markLocalRead(newLocals, localNum);
				break;
			case opc_lload_0 :
			case opc_lload_1 :
			case opc_lload_2 :
			case opc_lload_3 :
			case opc_lload :
			case opc_dload_0 :
			case opc_dload_1 :
			case opc_dload_2 :
			case opc_dload_3 :
			case opc_dload :
				loadIns = (BT_LoadLocalIns) previousInstruction;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				target = loadIns.target;
				localNum = target.localNr;
				if(localNum + 1 >= previousLocalsLength) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				markLocalRead(newLocals, localNum);
				markLocalRead(newLocals, localNum + 1);
				break;
			case opc_istore_0 :
			case opc_istore_1 :
			case opc_istore_2 :
			case opc_istore_3 :
			case opc_istore :
			case opc_fstore_0 :
			case opc_fstore_1 :
			case opc_fstore_2 :
			case opc_fstore_3 :
			case opc_fstore :
			case opc_astore_0 :
			case opc_astore_1 :
			case opc_astore_2 :
			case opc_astore_3 :
			case opc_astore :
				BT_StoreLocalIns storeIns = (BT_StoreLocalIns) previousInstruction;
				SubRoutine currentSubRoutine = getCurrentSubRoutine();
				localNum = storeIns.target.localNr;
				if(currentSubRoutine != null) {
					ArrayList list = (ArrayList) subRoutineMap.get(currentSubRoutine);
					while(list.size() <= storeIns.target.localNr) {
						list.add(null);
					}
					list.set(localNum, Boolean.TRUE);
				}
				if(localNum >= previousLocalsLength) {
					//if(highestLocal >= maxLocals) {
					//	throw new BT_MaxLocalsExceededException(code, storeIns, prev_iin);
					//}
					newLocals = pool.getLocals(localNum + 1);
				} else {
					newLocals = pool.getLocals(previousLocalsLength);
				}
				System.arraycopy(previousLocals, 0, newLocals, 0, previousLocalsLength);
				markLocalWrite(newLocals, localNum);
				break;
			case opc_lstore_0 :
			case opc_lstore_1 :
			case opc_lstore_2 :
			case opc_lstore_3 :
			case opc_lstore :
			case opc_dstore_0 :
			case opc_dstore_1 :
			case opc_dstore_2 :
			case opc_dstore_3 :
			case opc_dstore :
				storeIns = (BT_StoreLocalIns) previousInstruction;
				currentSubRoutine = getCurrentSubRoutine();
				localNum = storeIns.target.localNr;
				int highestLocal = localNum + 1;
				if(currentSubRoutine != null) {
					ArrayList list = (ArrayList) subRoutineMap.get(currentSubRoutine);
					while(list.size() <= highestLocal) {
						list.add(null);
					}
					list.set(localNum, Boolean.TRUE);
					list.set(highestLocal, Boolean.TRUE);
				}
				if(highestLocal >= previousLocalsLength) {
					//if(highestLocal >= maxLocals) {
					//	throw new BT_MaxLocalsExceededException(code, storeIns, prev_iin);
					//}
					newLocals = pool.getLocals(highestLocal + 1);
				} else {
					newLocals = pool.getLocals(previousLocalsLength);
				}
				System.arraycopy(previousLocals, 0, newLocals, 0, previousLocalsLength);
				markLocalWrite(newLocals, localNum);
				markLocalWrite(newLocals, highestLocal);
				break;
			case opc_jsr :
			case opc_jsr_w :
				currentSubRoutine = getCurrentSubRoutine();
				if(subRoutineMap == null) {
					subRoutineMap = new HashMap();
					ArrayList localList = new ArrayList(maxLocals);
					subRoutineMap.put(currentSubRoutine, localList);
				} else if(subRoutineMap.get(currentSubRoutine) == null) {
					ArrayList localList = new ArrayList(maxLocals);
					subRoutineMap.put(currentSubRoutine, localList);
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_iinc :
				BT_IIncIns iincIns = (BT_IIncIns) previousInstruction;
				target = iincIns.target;
				localNum = target.localNr;
				currentSubRoutine = getCurrentSubRoutine();
				if(currentSubRoutine != null) {
					ArrayList list = (ArrayList) subRoutineMap.get(currentSubRoutine);
					while(list.size() <= localNum) {
						list.add(null);
					}
					list.set(localNum, Boolean.TRUE);
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				markLocalRead(newLocals, localNum);
				markLocalWrite(newLocals, localNum);
				break;
			case opc_ret :
				BT_RetIns retIns = (BT_RetIns) previousInstruction;
				target = retIns.target;
				localNum = target.localNr;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				markLocalRead(newLocals, localNum);
				break;
			case opc_ireturn :
			case opc_lreturn :
			case opc_freturn :
			case opc_dreturn :
			case opc_areturn :
			case opc_return :
			case opc_athrow :
				//we should never reach here
				throw new IllegalArgumentException("invalid previous instruction");
			default:
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
		}//end switch
		return newLocals;
	}
    
    private void merge(int iin, BT_LocalCell newLocals[], boolean again) {
		boolean changedLocals = merge(localShapes, newLocals, iin);
		markChange(changedLocals, iin, again);
	}
    
    private boolean merge(BT_LocalCell stackShapes[][], BT_LocalCell newStack[], int iin)  {
    	BT_LocalCell existingStack[] = stackShapes[iin];
		if(existingStack == null) {
			stackShapes[iin] = newStack;
			return false;
		}
		int newLength = newStack.length;
		int existingLength = existingStack.length;
		if(newLength < existingLength) {
			mergeStacks(existingStack, newStack);
			saveStack(newStack, stackShapes, iin);
			return true;
		}
		boolean changedStack = mergeStacks(newStack, existingStack);
		returnStack(newStack);
		return changedStack;
		
	}
    
    /**
     * We want to maintian the locals so that local[i]==initialValue
     * if there exists a single path by which local[i] can still have the initial
     * value in position i.
     * 
     * So when merging a and b, if either a or b is initialValue then the
     * result is initialValue.
     * 
	 * @param stack1 the stack to be merged with stack2
	 * @param stack2 the stack to be merged into
	 * @return whether a merge of two types occurred
	 */
	private boolean mergeStacks(BT_LocalCell stack1[], BT_LocalCell stack2[]) {
		boolean ret = false;
		int length = stack2.length;
		for(int i=0; i<length; i++) {
			BT_LocalCell existingType = stack2[i];
			if(existingType == null) {
				BT_LocalCell type = stack1[i];
				if(type != null) {
					ret = true;
					stack2[i] = type;
				}
			}
		}
		return ret;
	}
    
    private void saveStack(BT_LocalCell stack[], BT_LocalCell stacks[][], int index) {
		//checkExistence(stack, localShapes);
    	BT_LocalCell saved[] = stacks[index];
		stacks[index] = stack;
		if(saved != null) {
			returnStack(saved);
		}
	}
    
    private void returnStack(BT_LocalCell stack[]) {
		//if (checkExistence(stack, localShapes)) return;
		pool.returnLocals(stack);
	}
    
//    private boolean checkExistence(Type stack[], Type stacks[][]) {
//		if(stack.length == 0) {
//			return false;
//		}
//		for(int i=0; i<stacks.length; i++) {
//			if(stack == stacks[i]) {
//				throw new RuntimeException("returning or saving a stack currently saved");
//			}
//		}
//		return false;
//	}
    
    private BT_LocalCell[] mergeJSRAndRetLocals(int iin, BT_LocalCell retLocals[]) {
        //find the jsr instruction
        int jsrIndex = iin;
        BT_Ins ins;
        BT_InsVector inst = code.getInstructions();
        do {
        	ins = inst.elementAt(--jsrIndex);
        } while(ins.isBlockMarker());
        BT_JumpIns jsrIns = (BT_JumpIns) ins;
        BT_LocalCell jsrLocals[] = localShapes[jsrIndex];
        
        BT_LocalCell[] newLocals;
        
        //we need to do a special merge with the locals
        if(jsrLocals == null) {/* if we entered the code in the middle of the subroutine */
        	newLocals = pool.getLocals(maxLocalsAtIndex);
            System.arraycopy(retLocals, 0, newLocals, 0, newLocals.length);
        } else {
         
	        //get the subroutine object
	        BT_Ins startInstruction = code.getSubroutineStartInstruction(jsrIns.getTarget());
	        SubRoutine sub = getSubRoutine(startInstruction);
	        
	        //get the boolean array that tells us which locals were changed in the subroutine
	        //AHA, there may be no subroutine map if we entered in the middle!  So what to do?
	        ArrayList wasChangedInSubRoutine = (ArrayList) subRoutineMap.get(sub);
	        
	        int newLocalStackLength = Math.max(jsrLocals.length, retLocals.length);
	        boolean wasChanged[] = new boolean[newLocalStackLength];
	        for(int i=0; i<wasChanged.length && i < wasChangedInSubRoutine.size(); i++) {
	        	wasChanged[i] = (wasChangedInSubRoutine.get(i) == Boolean.TRUE);
	        }
	        
	        //account for any nested subroutines
            SubRoutine nestedSubs[] = sub.getNestedSubRoutines();
            for(int i=0; i<nestedSubs.length; i++) {
            	SubRoutine nested = nestedSubs[i];
            	ArrayList wasChangedInNested = (ArrayList) subRoutineMap.get(nested);
            	for(int j=0; j<wasChanged.length && j < wasChangedInNested.size(); j++) {
            		wasChanged[j] |= (wasChangedInNested.get(j) == Boolean.TRUE);
            	}	
            }
            
            newLocals = pool.getLocals(newLocalStackLength);
            
            
            //for those locals that were altered in the subroutine, we take the 
            //subroutine locals, otherwise we take the locals before the jsr
            int i;
            for(i = 0; i<newLocals.length; i++) {
            	BT_LocalCell originatingLocals[] = wasChanged[i] ? retLocals : jsrLocals;
            	newLocals[i] = (i < originatingLocals.length) ? originatingLocals[i] : null;
            }
        }
        return newLocals;
    }
	
    private void markLocalWrite(BT_LocalCell newLocals[], int localNum) {
    	newLocals[localNum] = null;
    	maxLocals = Math.max(maxLocals, localNum + 1);
    }
    
	private void markLocalRead(BT_LocalCell newLocals[], int localNum) {
		if(newLocals[localNum] == initialValue) {
			if(localNum >= localIsInUse.size()) {
				while(localNum > localIsInUse.size()) {
					localIsInUse.add(null);
				}
				localIsInUse.add(Boolean.TRUE);
				localsInUse++;
			} else if(localIsInUse.get(localNum) != Boolean.TRUE) {
				localIsInUse.set(localNum, Boolean.TRUE);
				localsInUse++;
			}
			
			
			if(canExitEarly && localNum >= highestPossibleInUse) {
				foundHighest = true;
			}
			
			
		}
		maxLocals = Math.max(maxLocals, localNum + 1);
	}
	
	public int getHighestLocalNotInUse() {
		for(int i=localIsInUse.size() - 1; i>=0; i--) {
			if(localIsInUse.get(i) == Boolean.TRUE) {
				return i + 1;
			}
		}
		return -1;
	}
	
	boolean localIsInUse(int localNum) {
		return localNum < localIsInUse.size()
			&& localIsInUse.get(localNum) == Boolean.TRUE;
	}
	
	public void returnStacks() {
    	if(!returnedStacks) {
    		returnedStacks = true;
    		pool.returnLocals(localShapes);
    		Arrays.fill(localShapes, null);
    	}
    }
	
}



