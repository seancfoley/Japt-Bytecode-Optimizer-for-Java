/*
 * Created on Jul 16, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.ibm.jikesbt.BT_CodeException.BT_InconsistentStackDepthException;
import com.ibm.jikesbt.BT_CodeException.BT_InconsistentStackTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidLoadException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_ExpectedArrayTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_ExpectedObjectTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_ExpectedUninitializedTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_UninitializedObjectTypeException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStoreException;
import com.ibm.jikesbt.BT_CodeException.BT_LocalsOverflowException;
import com.ibm.jikesbt.BT_CodeException.BT_StackOverflowException;
import com.ibm.jikesbt.BT_CodeException.BT_StackUnderflowException;
import com.ibm.jikesbt.BT_CodeException.BT_UninitializedLocalException;
import com.ibm.jikesbt.BT_ObjectCode.SubRoutine;
import com.ibm.jikesbt.BT_StackType.ClassType;
import com.ibm.jikesbt.BT_StackType.ReturnAddress;
import com.ibm.jikesbt.BT_StackType.StubType;
import com.ibm.jikesbt.BT_StackType.UninitializedObject;

/**
 * @author Sean Foley
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BT_StackShapeVisitor extends BT_CodeVisitor implements BT_Opcodes {
	final protected BT_StackPool pool; //used to cache unused stacks for later use (for performance)
	final private BT_StackCellProvider provider;
	final protected BT_StackShapes stackShapes; /* this is the object this visitor populates */
	
	private boolean done; //whether shapes need to be changed since the last visit
	private boolean changed[]; //changed[i] is true if the shapes at index have changed since the last visit
	private boolean newlyChanged[]; //newlyChanged[i] is true if the shapes at index i have changed during this visit
	private BT_CodeAttribute constructorCode; // this field is used to ensure that this visitor only used with the same code attribute it was constructed with
	
	private HashMap subRoutineMap; //maps objects of type SubRoutine to a List of Boolean : represents the locals altered by each subroutine
	
	final private BT_StackCell newStacks[][]; //the stack at index i is the effect of instruction i on its stack stackShapes[i]
	final private BT_LocalCell newLocalStacks[][]; //the stack at index i is the effect of instruction i on its locals localShapes[i]
	
	
	private boolean useMergeCandidates = true;
	private boolean ignoreUpcasts = true;
	
	private BT_HashedClassVector mergeCandidates;
	
	private int absoluteLocals = -1;
	private int absoluteStackDepth = -1;
	
   	public BT_StackShapeVisitor(BT_CodeAttribute code) {
    	this(code, new BT_StackPool());
    }
    
	public BT_StackShapeVisitor(BT_CodeAttribute code, BT_StackPool pool) {
		this(code, pool, new BT_StackCellProvider(code.getMethod().cls.repository));
	}
	
   	public BT_StackShapeVisitor(BT_CodeAttribute code, BT_StackPool pool, BT_StackCellProvider provider) {
   		stackShapes = new BT_StackShapes(code, pool, provider);
   		super.revisitHandlers = true;
    	this.provider = provider;
    	this.pool = pool;
		this.code = constructorCode = code;
		int codeSize = code.getInstructionSize();
		this.newStacks = new BT_StackCell[codeSize][];
    	this.newLocalStacks = new BT_LocalCell[codeSize][];
	}
   	
   	public void setAbsoluteMaxStacks(int maxLocals, int maxStack) {
   		if(maxLocals < 0 || maxStack < 0) {
   			throw new IllegalArgumentException();
   		}
   		this.absoluteLocals = maxLocals;
   		this.absoluteStackDepth = maxStack;
   	}
   	
   	/**
   	 * When ignoring upcasts, the visitor will create more accurate stack shapes, since an upcast
   	 * casts from the more specific to the more general.
   	 * 
   	 * But when trying to emulate a verifier, the visitor should not ignore upcasts because a verifier
   	 * cannot differentiate between upcasts and downcasts and it will cast all types.
   	 * @param ignore
   	 */
   	public void ignoreUpcasts(boolean ignore) {
   		ignoreUpcasts = ignore;
   	}
   	
   	/**
   	 * When using merge candidates, the visitor will merge classes to merge candidates only, which
   	 * consist of all classes that might be consumed from the stack.
   	 * 
   	 * When trying to emulate a verifier, such as when constructing stack maps,
   	 * merge candidates should be used, because non-merge candidates
   	 * cannot always be loaded from the same class loader that the verifier is using.
   	 * @param ignore
   	 */
   	public void useMergeCandidates(boolean useMergeCandidates) {
   		this.useMergeCandidates = useMergeCandidates;
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
    
    protected void returnStacks() {
    	pool.returnStacks(newStacks);
		pool.returnLocals(newLocalStacks);
		Arrays.fill(newStacks, null);
		Arrays.fill(newLocalStacks, null);
    }
    
    public BT_StackShapes populate() throws BT_CodeException {
    	if(code.getInstructionSize() == 0) {
    		return null;
    	}
    	stackShapes.createInitialStacks();
    	try {
    		if(absoluteLocals >= 0 && stackShapes.maxLocals > absoluteLocals) {
    			throw new BT_LocalsOverflowException(code, code.getFirstInstruction(), 0);
    		}
	    	do {
	    		code.visitReachableCode(this);
			} while(!done);
		} catch(BT_CodeException e) {
    		stackShapes.returnStacks();
    		throw e;
    	} finally {
    		returnStacks();
    	}
		return stackShapes;
	}

    public BT_StackShapes populateWithOutput(PrintStream stream) throws BT_CodeException {
    	stackShapes.createInitialStacks();
    	try {
    		if(absoluteLocals >= 0 && stackShapes.maxLocals > absoluteLocals) {
    			throw new BT_LocalsOverflowException(code, code.getFirstInstruction(), 0);
    		}
	    	do {
	    		code.visitReachableCode(this);
	    		stackShapes.print(stream);
	    		stream.println();
	    	} while(!done);
	    } catch(BT_CodeException e) {
    		stackShapes.returnStacks();
    		throw e;
    	} finally {
    		returnStacks();
    	}
		return stackShapes;
	}

    protected void additionalVisit(
    		BT_Ins instruction,
    		int iin,
    		BT_Ins previousInstruction,
    		int prev_iin,
    		BT_ExceptionTableEntry handler) 
    			throws BT_CodeException {
		stackShapes.isMergeInstruction[iin]= true;
		handleVisit(instruction, iin, previousInstruction, prev_iin, handler, true);
	}
	
    protected boolean visit(
    		BT_Ins instruction,
    		int iin,
    		BT_Ins previousInstruction,
    		int prev_iin,
    		BT_ExceptionTableEntry handler) 
    			throws BT_CodeException {
		//stackShapes[iin] is the shape of the stack before the current instruction is executed
		if(prev_iin == ENTRY_POINT) {
			if(constructorCode != code) {
	    		throw new IllegalArgumentException(
					"cannot use this visitor on a code attribute that this visitor was not constructed with");
	    	}
			return true;
		} 
		return handleVisit(instruction, iin, previousInstruction, prev_iin, handler, false);
	}
	
	static class StackShapes {
		BT_StackCell newStack[];
		BT_LocalCell newLocals[]; 
		
		void reset() {
			newStack = null;
			newLocals = null;
		}
	}
	
    /* this object is cached to improve performance */
    private StackShapes savedShapes = new StackShapes();
	
	private boolean handleVisit(
			BT_Ins instruction,
			int iin,
			BT_Ins previousInstruction,
			int prev_iin,
			BT_ExceptionTableEntry handler,
			boolean again) 
				throws BT_CodeException {
		if(isNotChanged(prev_iin)) {
			return true;
		}
		StackShapes shapes = savedShapes;
		shapes.reset();
		if(handler != null) {
			//we have arrived at this instruction via an exception
			stackShapes.isMergeInstruction[iin] = true;/* typically additionalVisit will also be called, but not if just a single instruction in the range */
			getStacksForHandler(shapes, iin, prev_iin, handler);
			mergeHandler(instruction, iin, shapes.newStack, shapes.newLocals, again);
			return true;
		} else {
			BT_StackCell newStack[] = newStacks[prev_iin];
			BT_LocalCell newLocals[] = newLocalStacks[prev_iin];
			/* 
			 * newStack and newLocals are the previously stored effect of instruction prev_iin on the stack. 
			 * Since a single instruction might have more than one successor, storing this information saves time.
			 * Whenever the stacks preceding a given instruction are changed, then newStacks and newLocals for that
			 * same instruction are set to null, so the effect of the instruction on the stacks is recalculated.
			 */
			if((newStack != null) || (newLocals != null)) {
				shapes.newStack = newStack;
				shapes.newLocals = newLocals;
			} else {
				getStacks(shapes, iin, prev_iin, previousInstruction);
				
				//now remember the effect of previousInstruction on the stack
				saveStack(shapes.newStack, newStacks, prev_iin);
				saveLocals(shapes.newLocals, newLocalStacks, prev_iin);
			}
			
			boolean returningFromRet = previousInstruction.isRetIns();
			if(returningFromRet) {
				shapes.newLocals = mergeJSRAndRetLocals(iin, shapes.newLocals);
			}
			try {
				merge(instruction, iin, shapes.newStack, shapes.newLocals, again);
				return true;
			} finally {
				if(returningFromRet) {
					returnLocals(shapes.newLocals);
				}
			}
		}
	}

    private BT_LocalCell[] mergeJSRAndRetLocals(int retTargetIndex, BT_LocalCell retLocals[]) {
        //find the jsr instruction
        int jsrIndex = retTargetIndex;
        BT_Ins ins;
        BT_InsVector inst = code.getInstructions();
        do { /* jsrs are always preceded by a block marker */
        	ins = inst.elementAt(--jsrIndex);
        } while(ins.isBlockMarker());
        if(jsrIndex == 0) {
        	stackShapes.mergedInitial = true;
		}
        BT_JumpIns jsrIns = (BT_JumpIns) ins;
        BT_LocalCell jsrLocals[] = stackShapes.localShapes[jsrIndex];
        
        //we need to do a special merge with the locals
        int newLocalStackLength = Math.max(jsrLocals.length, retLocals.length);
        BT_LocalCell[] newLocals = pool.getLocals(newLocalStackLength);
        boolean wasChanged[] = new boolean[newLocalStackLength];
        
        //get the subroutine object
        BT_Ins startInstruction = code.getSubroutineStartInstruction(jsrIns.getTarget());
        SubRoutine sub = getSubRoutine(startInstruction);
        
        //get the boolean array that tells us which locals were changed in the subroutine
        ArrayList wasChangedInSubRoutine = (ArrayList) subRoutineMap.get(sub);
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
        
        //for those locals that were altered in the subroutine, we take the 
        //subroutine locals, otherwise we take the locals before the jsr
        int i;
        for(i = 0; i<newLocals.length; i++) {
        	BT_LocalCell originatingLocals[] = wasChanged[i] ? retLocals : jsrLocals;
        	newLocals[i] = (i < originatingLocals.length) ? originatingLocals[i] : null;
        }
        if(i > 0 && newLocals[i - 1] == null) {
        	do {
        		i--;
        	} while(i > 0 && newLocals[i - 1] == null);
        	BT_LocalCell[] truncatedLocals = pool.getLocals(i);
        	System.arraycopy(newLocals, 0, truncatedLocals, 0, i);
        	returnLocals(newLocals);
        	newLocals = truncatedLocals;
        }
        return newLocals;
    }
	
	private boolean isNotChanged(int iin) {
		return changed != null && !changed[iin] && !newlyChanged[iin];
	}
	
	private BT_LocalCell[] getLocalStack(
			int highestLocal, 
			int previousLocalsLength, 
			BT_LocalIns instruction, 
			int instructionIndex) {
		if(highestLocal >= previousLocalsLength) {
			return pool.getLocals(highestLocal + 1);
		} else {
			return pool.getLocals(previousLocalsLength);
		}
	}
	
	private BT_LocalCell[] copyLocals(BT_LocalCell previousLocals[], int previousLocalsLength) {
		BT_LocalCell newLocals[] = pool.getLocals(previousLocalsLength);
		System.arraycopy(previousLocals, 0, newLocals, 0, previousLocalsLength);
		return newLocals;
	}
	
	/*
	 * Note that there should be no loading activity caused within this class.  This is because the creation of stack maps
	 * often occurs after a number of transformations have occurred (e.g renaming), and any loading can cause incoherency.
	 * A newly loaded class might have references that are no longer accurate.
	 */
	private void getStacks(StackShapes shapes, int iin, int prev_iin, BT_Ins previousInstruction) 
			throws BT_StackUnderflowException, 
				BT_UninitializedLocalException, 
				BT_InvalidLoadException,
				BT_InvalidStoreException,
				BT_InvalidStackTypeException,
				BT_StackOverflowException,
				BT_LocalsOverflowException {
		/* 
		 * Note: we are attempting to calculate the shape of the stack before the instruction at index iin
		 * is executed, so we are analyzing the behaviour of the instruction at index prev_iin, previousInstruction
		 */
		
		BT_StackCell previousStack[] = stackShapes.stackShapes[prev_iin];
		int stackDiff = previousInstruction.getStackDiff();
		int previousStackLength = previousStack.length;
		int nextDepth = previousStackLength + stackDiff;
		if (nextDepth < 0) {
			throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
		}
		if(absoluteStackDepth >= 0 && nextDepth > absoluteStackDepth) {
			throw new BT_StackOverflowException(code, previousInstruction, prev_iin);
		}
		if(stackDiff < 0) {
			for(int i=-1; i>=stackDiff; i--) {
				BT_StackCell cell = previousStack[previousStackLength + i];
				provider.consumeCell(cell, prev_iin);
			}
		}
		stackShapes.maxDepth = Math.max(stackShapes.maxDepth, nextDepth);
		BT_LocalCell previousLocals[] = stackShapes.localShapes[prev_iin];
		BT_StackCell newStack[] = pool.getStack(nextDepth);
		int previousLocalsLength = previousLocals.length;
		BT_LocalCell newLocals[];
		
		/* the top of the stack is the end of the array, while the highest local index is the end of the local array */
		int previousTop = previousStack.length - 1;
		int newTop = newStack.length - 1;
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
				BT_LoadLocalIns loadIns = (BT_LoadLocalIns) previousInstruction;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				if(loadIns.target.localNr >= previousLocalsLength) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				BT_LocalCell localCell = previousLocals[loadIns.target.localNr];
				if(localCell == null) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				BT_StackType type = provider.loadCell(localCell, prev_iin);
				if(type.isClassType()) {
					ClassType ct = type.getClassType();
					if(ct.isNull()) {
						throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
					}
					BT_Class clazz = ct.type;
					if(clazz.getOpcodeForLoadLocal() != loadIns.getBaseOpcode()) {
						throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
					}
				} else {
					throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
				}
				newStack[newTop] = provider.getLoadedPrimitiveCell(localCell, iin, prev_iin);
				break;
			case opc_aload_0 :
			case opc_aload_1 :
			case opc_aload_2 :
			case opc_aload_3 :
			case opc_aload :
				loadIns = (BT_LoadLocalIns) previousInstruction;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				if(loadIns.target.localNr >= previousLocalsLength) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				localCell = previousLocals[loadIns.target.localNr];
				if(localCell == null) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				type = provider.loadCell(localCell, prev_iin);
				if(!type.isObjectType() && !type.isUninitializedObject()) {
					throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
				}
				newStack[newTop] = provider.getLoadedReferenceCell(localCell, iin, prev_iin);
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
				System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
				if(loadIns.target.localNr + 1 >= previousLocalsLength) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				int targetLocal = loadIns.target.localNr;
				localCell = previousLocals[targetLocal];
				if(localCell == null) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				type = provider.loadCell(localCell, prev_iin);
				if(type.isClassType()) {
					ClassType ct = type.getClassType();
					if(ct.isNull()) {
						throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
					}
					BT_Class clazz = ct.type;
					if(clazz.getOpcodeForLoadLocal() != loadIns.getBaseOpcode()) {
						throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
					}
				} else {
					throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
				}
				newStack[newTop - 1] = provider.getLoadedPrimitiveCell(localCell, iin, prev_iin);
				localCell = previousLocals[targetLocal + 1];
				if(localCell == null) {
					throw new BT_UninitializedLocalException(code, loadIns, prev_iin);
				}
				type = provider.loadCell(localCell, prev_iin);
				if(!type.isTop()) {
					throw new BT_InvalidLoadException(code, loadIns, prev_iin, type);
				}
				newStack[newTop] = provider.getLoadedPrimitiveCell(localCell, iin, prev_iin);
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
				if(previousStack.length <= 0) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				BT_StoreLocalIns storeIns = (BT_StoreLocalIns) previousInstruction;
				type = previousStack[previousTop].getCellType();
				if(type.isClassType()) {
					ClassType ct = type.getClassType();
					if(ct.isNull()) {
						if(storeIns.getBaseOpcode() != opc_astore) {
							throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
						}
					} else {
						BT_Class clazz = ct.type;
						if(clazz.getOpcodeForStoreLocal() != storeIns.getBaseOpcode()) {
							throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
						}
					}
				} else if(type.isUninitializedObject() || type.isReturnAddress()) {
					if(storeIns.getBaseOpcode() != opc_astore) {
						throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
					}
				} else {
					throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
				}

				targetLocal = storeIns.target.localNr;
				stackShapes.maxLocals = Math.max(targetLocal + 1, stackShapes.maxLocals);
				if(absoluteLocals >= 0 && stackShapes.maxLocals > absoluteLocals) {
	    			throw new BT_LocalsOverflowException(code, storeIns, prev_iin);
	    		}
				SubRoutine currentSubRoutine = getCurrentSubRoutine();
				if(currentSubRoutine != null) {
					ArrayList list = (ArrayList) subRoutineMap.get(currentSubRoutine);
					while(list.size() <= targetLocal) {
						list.add(null);
					}
					list.set(targetLocal, Boolean.TRUE);
				}
				
				newLocals = getLocalStack(targetLocal, previousLocalsLength, storeIns, prev_iin);
				System.arraycopy(previousLocals, 0, newLocals, 0, previousLocalsLength);
				System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
				newLocals[targetLocal] = provider.getLocalCell(previousStack[previousTop], storeIns, prev_iin);
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
				if(previousStack.length <= 1) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				storeIns = (BT_StoreLocalIns) previousInstruction;
				type = previousStack[previousTop - 1].getCellType();
				if(type.isClassType()) {
					ClassType ct = type.getClassType();
					if(ct.isNull()) {
						throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
					}
					BT_Class clazz = ct.type;
					if(clazz.getOpcodeForStoreLocal() != storeIns.getBaseOpcode()) {
						throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
					}
				} else {
					throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
				}
				type = previousStack[previousTop].getCellType();
				if(!type.isTop()) {
					throw new BT_InvalidStoreException(code, storeIns, prev_iin, type);
				}
				
				targetLocal = storeIns.target.localNr;
				int highestLocal = targetLocal + 1;
				
				currentSubRoutine = getCurrentSubRoutine();
				if(currentSubRoutine != null) {
					ArrayList list = (ArrayList) subRoutineMap.get(currentSubRoutine);
					while(list.size() <= highestLocal) {
						list.add(null);
					}
					list.set(targetLocal, Boolean.TRUE);
					list.set(highestLocal, Boolean.TRUE);
				}
				
				stackShapes.maxLocals = Math.max(highestLocal + 1, stackShapes.maxLocals);
				if(absoluteLocals >= 0 && stackShapes.maxLocals > absoluteLocals) {
	    			throw new BT_LocalsOverflowException(code, storeIns, prev_iin);
	    		}
				newLocals = getLocalStack(highestLocal, previousLocalsLength, storeIns, prev_iin);
				System.arraycopy(previousLocals, 0, newLocals, 0, previousLocalsLength);
				System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
				newLocals[targetLocal] = provider.getLocalCell(previousStack[previousTop - 1], storeIns, prev_iin);
				newLocals[highestLocal] = provider.getLocalCell(previousStack[previousTop], storeIns, prev_iin);
				break;
			case opc_jsr :
			case opc_jsr_w :
				currentSubRoutine = getCurrentSubRoutine();
				if(subRoutineMap == null) {
					subRoutineMap = new HashMap();
					ArrayList localList = new ArrayList(stackShapes.maxLocals);
					subRoutineMap.put(currentSubRoutine, localList);
				} else if(subRoutineMap.get(currentSubRoutine) == null) {
					ArrayList localList = new ArrayList(stackShapes.maxLocals);
					subRoutineMap.put(currentSubRoutine, localList);
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				newStack[newTop] = new ReturnAddress(code.getNextInstruction(prev_iin));
				stackShapes.hasJSR = true;
				break;
			case opc_new :
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				BT_NewIns newIns = (BT_NewIns) previousInstruction;
				BT_Class targetClass = newIns.getTarget();
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				newStack[newTop] = provider.getUninitializedObject(newIns, iin, prev_iin);
				break;
			case opc_anewarray :
			case opc_newarray : 
			case opc_multianewarray :
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				newIns = (BT_NewIns) previousInstruction;
				targetClass = newIns.getTarget();
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				newStack[newTop] = provider.getObjectClassCell(targetClass, iin, prev_iin);
				break;
			case opc_aconst_null :
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				newStack[newTop] = provider.getNullClassCell(iin, prev_iin);
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_aaload :
				if(previousStack.length <= 1) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				type = previousStack[previousTop - 1].getCellType();
				if(!type.isObjectType()) {
					throw new BT_ExpectedArrayTypeException(code, previousInstruction, prev_iin, type, 1);
				}
				ClassType arrayClass = type.getClassType();
				if(arrayClass.isNull()) {
					/* we do not know what type of array class we have, but we know it is
					 * an object, so we put the null type on the stack, considering that
					 * is the most general type available
					 */
					newStack[newTop] = provider.getNullClassCell(iin, prev_iin);
				} else {
					BT_Class array = arrayClass.type;
					if(!array.isArray()) {
						throw new BT_ExpectedArrayTypeException(code, previousInstruction, prev_iin, type, 1);
					}
					ClassType elementClass;
					if(arrayClass.isStubObjectType()) {
						StubType arrayStub = arrayClass.getStubType();
						elementClass = new StubType(array.getElementClass()).addStubs(arrayStub.getStubs());
					} else {
						elementClass = array.getElementClass().classType;
					}
					newStack[newTop] = provider.getObjectArrayElementClassCell(elementClass, iin, prev_iin);
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				break;
			case opc_getfield :
			case opc_getstatic :
				BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) previousInstruction;
				BT_Field fieldTarget = fieldRefIns.getFieldTarget();
				BT_Class targetFieldType = fieldTarget.getFieldType();
				if(targetFieldType.getSizeForLocal() == 2) {
					System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
					newStack[newTop - 1] = targetFieldType.classType;
					newStack[newTop] = BT_StackType.TOP;
				} else {
					System.arraycopy(previousStack, 0, newStack, 0, newTop);
					if(targetFieldType.isPrimitive()) {
						newStack[newTop] = targetFieldType.classType.convert();
					} else {
						newStack[newTop] = provider.getObjectClassCell(targetFieldType, iin, prev_iin);
					}
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_invokeinterface :
			case opc_invokevirtual :
			case opc_invokestatic :	
				BT_MethodRefIns methodRefIns = (BT_MethodRefIns) previousInstruction;
				BT_Method targetMethod = methodRefIns.getMethodTarget();
				if(targetMethod.isVoidMethod()) {
					System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
				} else {
					BT_Class returnType = targetMethod.getSignature().returnType;
					if(returnType.getSizeForLocal() == 2) {
						System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
						newStack[newTop - 1] = returnType.classType;
						newStack[newTop] = BT_StackType.TOP;
					} else {
						System.arraycopy(previousStack, 0, newStack, 0, newTop);
						if(returnType.isPrimitive()) {
							newStack[newTop] = returnType.classType.convert();
						} else {
							newStack[newTop] = provider.getObjectClassCell(returnType, iin, prev_iin);
						}
					}
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_invokespecial :
				methodRefIns = (BT_MethodRefIns) previousInstruction;
				targetMethod = methodRefIns.getMethodTarget();
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				BT_StackCell previousCell;
				if(targetMethod.isVoidMethod()) {
					System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
					if(targetMethod.isConstructor()) {
						//replace any uninitialized types matching the constructed type with the corresponding initialized types
						BT_MethodSignature sig = targetMethod.getSignature();
						int argsSize =  sig.getArgsSize();
						previousCell = previousStack[previousTop - argsSize];
						BT_StackType uninit = previousCell.getCellType();
						if(!uninit.isUninitializedObject()) {
							//calling constructor on invalid type
							throw new BT_ExpectedUninitializedTypeException(code, methodRefIns, prev_iin, 
									methodRefIns.getClassTarget(), uninit, argsSize);
						}						
						for(int i=0; i<newStack.length; i++) {
							previousCell = newStack[i];
							if(previousCell.getCellType().equals(uninit)) {
								if(uninit.isUninitializedThis()) {
									newStack[i] = provider.getInitializedClassCell(uninit, code.getMethod().cls, previousCell, i == newTop, iin, prev_iin);
								} else {
									UninitializedObject other = (UninitializedObject) uninit;
									newStack[i] = provider.getInitializedClassCell(uninit, other.creatingInstruction.target, previousCell, i == newTop, iin, prev_iin);
								}
							}
						}
						/* it's highly unlikely that we have an instance of the constructed type in the locals,
						 * but we check for that anyway
						 */
						for(int i=0; i<newLocals.length; i++) {
							BT_LocalCell previousLocalCell = newLocals[i];
							if(previousLocalCell != null && previousLocalCell.getCellType().equals(uninit)) {
								if(uninit.isUninitializedThis()) {
									newLocals[i] = provider.getInitializedClassLocalCell(uninit, code.getMethod().cls, previousLocalCell, iin, prev_iin);
								} else {
									UninitializedObject other = (UninitializedObject) uninit;
									newLocals[i] = provider.getInitializedClassLocalCell(uninit, other.creatingInstruction.target, previousLocalCell, iin, prev_iin);
								}
							}
						}
					}
				} else {
					BT_Class returnType = targetMethod.getSignature().returnType;
					if(returnType.getSizeForLocal() == 2) {
						System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
						newStack[newTop - 1] = returnType.classType;
						newStack[newTop] = BT_StackType.TOP;
					} else {
						System.arraycopy(previousStack, 0, newStack, 0, newTop);
						if(returnType.isPrimitive()) {
							newStack[newTop] = returnType.classType.convert();
						} else {
							newStack[newTop] = provider.getObjectClassCell(returnType, iin, prev_iin);
						}
					}
				}
				break;
			case opc_ldc :
			case opc_ldc_w :
				if(previousInstruction instanceof BT_ConstantStringIns) {
					newStack[newTop] = provider.getConstantObjectCell(provider.javaLangString, iin, prev_iin);
				} else if(previousInstruction instanceof BT_ConstantClassIns) {
					newStack[newTop] = provider.getConstantObjectCell(provider.javaLangClass, iin, prev_iin);
				} else if(previousInstruction instanceof BT_ConstantIntegerIns) {
					newStack[newTop] = provider.intClass;
				} else if(previousInstruction instanceof BT_ConstantFloatIns) {
					newStack[newTop] = provider.floatClass;
				} else {//should never reach here, would indicate a bug in our code
					throw new RuntimeException("invalid ldc");
				}
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_ldc2_w :
				if(previousInstruction instanceof BT_ConstantDoubleIns) {
					newStack[newTop - 1] = provider.doubleClass;
				} else if(previousInstruction instanceof BT_ConstantLongIns) {
					newStack[newTop - 1] = provider.longClass;
				} else {//should never reach here, would indicate a bug in our code
					throw new RuntimeException("invalid ldc2");
				}
				System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
				newStack[newTop] = BT_StackType.TOP;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_dup :
				if(previousStack.length <= 0) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				previousCell = previousStack[previousTop];
				BT_StackType previous = previousCell.getCellType();
				if(previous.isNonNullObjectType()) {
					newStack[newTop] = provider.getDuplicateObjectCell(previousCell, 0, 1, iin, prev_iin);
				} else {
					newStack[newTop] = previousCell;
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_dup2 :
				if(previousStack.length <= 1) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
				previousCell = previousStack[previousTop];
				previous = previousCell.getCellType();
				BT_StackCell secondPreviousCell = previousStack[previousTop - 1];
				BT_StackType secondPrevious = secondPreviousCell.getCellType();
				if(secondPrevious.isTwoSlot()) {
					newStack[newTop] = previousCell;
					newStack[newTop - 1] = secondPreviousCell;
				} else {
					if(previous.isNonNullObjectType()) {
						newStack[newTop] = provider.getDuplicateObjectCell(previousCell, 0, 2, iin, prev_iin);
					} else {
						newStack[newTop] = previousCell;
					}
					if(secondPrevious.isNonNullObjectType()) {
						newStack[newTop - 1] = provider.getDuplicateObjectCell(secondPreviousCell, 1, 3, iin, prev_iin);
					} else {
						newStack[newTop - 1] = secondPreviousCell;
					}
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_dup_x1 :
				if(previousStack.length <= 1) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				System.arraycopy(previousStack, 0, newStack, 0, previousTop - 1);
				previousCell = previousStack[previousTop];
				previous = previousCell.getCellType();
				if(previous.isNonNullObjectType()) {
					newStack[newTop] = provider.getDuplicateObjectCell(previousCell, 0, 2, iin, prev_iin);
				} else {
					newStack[newTop] = previousCell;
				}
				newStack[newTop - 1] = previousStack[previousTop - 1];
				newStack[newTop - 2] = previousCell;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_dup_x2 :
				if(previousStack.length <= 2) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				System.arraycopy(previousStack, 0, newStack, 0, previousTop - 2);
				previousCell = previousStack[previousTop];
				previous = previousCell.getCellType();
				if(previous.isNonNullObjectType()) {
					newStack[newTop] = provider.getDuplicateObjectCell(previousCell, 0, 3, iin, prev_iin);
				} else {
					newStack[newTop] = previousCell;
				}
				newStack[newTop - 1] = previousStack[previousTop - 1];
				newStack[newTop - 2] = previousStack[previousTop - 2];
				newStack[newTop - 3] = previousCell;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_dup2_x1 :
				if(previousStack.length <= 2) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				System.arraycopy(previousStack, 0, newStack, 0, previousTop - 2);
				previousCell = previousStack[previousTop];
				previous = previousCell.getCellType();
				secondPreviousCell = previousStack[previousTop - 1];
				secondPrevious = secondPreviousCell.getCellType();
				if(secondPrevious.isTwoSlot()) {
					newStack[newTop] = previousCell;
					newStack[newTop - 1] = secondPreviousCell;
				} else {
					if(previous.isNonNullObjectType()) {
						newStack[newTop] = provider.getDuplicateObjectCell(previousCell, 0, 3, iin, prev_iin);
					} else {
						newStack[newTop] = previousCell;
					}
					if(secondPrevious.isNonNullObjectType()) {
						newStack[newTop - 1] = provider.getDuplicateObjectCell(secondPreviousCell, 1, 4, iin, prev_iin);
					} else {
						newStack[newTop - 1] = secondPreviousCell;
					}
				}
				newStack[newTop - 2] = previousStack[previousTop - 2];
				newStack[newTop - 3] = previousCell;
				newStack[newTop - 4] = secondPreviousCell;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_dup2_x2 :
				if(previousStack.length <= 3) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				System.arraycopy(previousStack, 0, newStack, 0, previousTop - 3);
				previousCell = previousStack[previousTop];
				previous = previousCell.getCellType();
				secondPreviousCell = previousStack[previousTop - 1];
				secondPrevious = secondPreviousCell.getCellType();
				if(secondPrevious.isTwoSlot()) {
					newStack[newTop] = previousCell;
					newStack[newTop - 1] = secondPreviousCell;
				} else {
					if(previous.isNonNullObjectType()) {
						newStack[newTop] = provider.getDuplicateObjectCell(previousCell, 0, 4, iin, prev_iin);
					} else {
						newStack[newTop] = previousCell;
					}
					if(secondPrevious.isNonNullObjectType()) {
						newStack[newTop - 1] = provider.getDuplicateObjectCell(secondPreviousCell, 1, 5, iin, prev_iin);
					} else {
						newStack[newTop - 1] = secondPreviousCell;
					}
				}
				newStack[newTop - 2] = previousStack[previousTop - 2];
				newStack[newTop - 3] = previousStack[previousTop - 3];
				newStack[newTop - 4] = previousCell;
				newStack[newTop - 5] = secondPreviousCell;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_swap :
				if(previousStack.length <= 1) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				System.arraycopy(previousStack, 0, newStack, 0, previousTop - 1);
				newStack[newTop] = previousStack[previousTop - 1];
				newStack[newTop - 1] = previousStack[previousTop];
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_lconst_0 :
			case opc_lconst_1 :
			case opc_d2l :
			case opc_i2l :
			case opc_f2l :
			case opc_laload :
				System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
				newStack[newTop] = BT_StackType.TOP;
				newStack[newTop - 1] = provider.longClass;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_dconst_0 :
			case opc_dconst_1 :
			case opc_f2d :
			case opc_i2d :
			case opc_l2d :
			case opc_daload :
				System.arraycopy(previousStack, 0, newStack, 0, newTop - 1);
				newStack[newTop] = BT_StackType.TOP;
				newStack[newTop - 1] = provider.doubleClass;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_instanceof :
			case opc_lcmp :
			case opc_fcmpl :
			case opc_fcmpg :
			case opc_dcmpl :
			case opc_dcmpg :
			case opc_d2i :
			case opc_l2i :
			case opc_f2i :
			case opc_arraylength :
			case opc_bipush :
			case opc_sipush :
			case opc_iconst_m1 :
			case opc_iconst_0 :
			case opc_iconst_1 :
			case opc_iconst_2 :
			case opc_iconst_3 :
			case opc_iconst_4 :
			case opc_iconst_5 :
			case opc_iaload :
			case opc_baload :
			case opc_caload :
			case opc_saload :
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				newStack[newTop] = provider.intClass;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_fconst_0 :
			case opc_fconst_1 :
			case opc_fconst_2 :
			case opc_i2f :
			case opc_d2f :
			case opc_l2f :
			case opc_faload :
				System.arraycopy(previousStack, 0, newStack, 0, newTop);
				newStack[newTop] = provider.floatClass;
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_checkcast :
				if(previousStack.length <= 0) {
					throw new BT_StackUnderflowException(code, previousInstruction, prev_iin);
				}
				BT_CheckCastIns checkCastIns = (BT_CheckCastIns) previousInstruction;
				previousCell = previousStack[previousTop];
				BT_StackType stackTop = previousCell.getCellType();
				BT_Class checkedClass = checkCastIns.getTarget();
				if(!stackTop.isObjectType()) {
					if(stackTop.isUninitializedObject()) {
						throw new BT_UninitializedObjectTypeException(code, checkCastIns, prev_iin, checkedClass, stackTop, 0);
					}
					throw new BT_ExpectedObjectTypeException(code, checkCastIns, prev_iin, stackTop, 0);
				}
				ClassType stackTopClassType = stackTop.getClassType();
				BT_Class stackTopClass = stackTopClassType.type;
				if((stackTopClass == null || stackTopClass.isInstanceOf(checkedClass)) && ignoreUpcasts) {
					//avoid an upcast
					System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
				} else {
					System.arraycopy(previousStack, 0, newStack, 0, newTop);
					newStack[newTop] = provider.getCastedClassCell(checkedClass, previousCell, iin, prev_iin);
				}
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				break;
			case opc_iinc :
				BT_IIncIns iincIns = (BT_IIncIns) previousInstruction;
				targetLocal = iincIns.target.localNr;
				localCell = previousLocals[targetLocal];
				type = provider.loadCell(localCell, prev_iin);
				if(type.isClassType()) {
					ClassType ct = type.getClassType();
					if(ct.isNull()) {
						throw new BT_InvalidLoadException(code, iincIns, prev_iin, type);
					}
					BT_Class clazz = ct.type;
					if(clazz.getOpcodeForLoadLocal() != opc_iload) {
						throw new BT_InvalidLoadException(code, iincIns, prev_iin, type);
					}
				} else {
					throw new BT_InvalidLoadException(code, iincIns, prev_iin, type);
				}
				
				currentSubRoutine = getCurrentSubRoutine();
				if(currentSubRoutine != null) {
					ArrayList list = (ArrayList) subRoutineMap.get(currentSubRoutine);
					while(list.size() <= targetLocal) {
						list.add(null);
					}
					list.set(targetLocal, Boolean.TRUE);
				}
				System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
				newLocals = copyLocals(previousLocals, previousLocalsLength);
				newLocals[targetLocal] = provider.getLocalCell(localCell, iincIns, prev_iin);
				break;
			case opc_ret :
				BT_RetIns retIns = (BT_RetIns) previousInstruction;
				targetLocal = retIns.target.localNr;
				localCell = previousLocals[targetLocal];
				provider.loadCell(localCell, prev_iin);
				/* fall through */
			case opc_putfield :
			case opc_putstatic :
			case opc_ifeq :
			case opc_ifne :
			case opc_iflt :
			case opc_ifge :
			case opc_ifgt :
			case opc_ifle :
			case opc_if_icmpeq :
			case opc_if_icmpne :
			case opc_if_icmplt :
			case opc_if_icmpge :
			case opc_if_icmpgt :
			case opc_if_icmple :
			case opc_if_acmpeq :
			case opc_if_acmpne :
			case opc_ifnull :
			case opc_ifnonnull :
			case opc_tableswitch :
			case opc_lookupswitch :
			case opc_iastore :
			case opc_lastore :
			case opc_fastore :
			case opc_dastore :
			case opc_aastore :
			case opc_bastore :
			case opc_castore :
			case opc_sastore :
			case opc_int2byte :
			case opc_int2char :
			case opc_int2short :
			case opc_fneg :
			case opc_frem :
			case opc_fdiv :
			case opc_fmul :
			case opc_fsub :
			case opc_fadd :
			case opc_dadd :
			case opc_dsub :
			case opc_dmul :
			case opc_ddiv :
			case opc_drem :
			case opc_dneg :
			case opc_ladd :
			case opc_lsub :
			case opc_lmul :
			case opc_ldiv :
			case opc_lrem :
			case opc_lneg :
			case opc_lshl :
			case opc_lshr :
			case opc_lushr :
			case opc_land :
			case opc_lor :
			case opc_lxor :
			case opc_iadd :
			case opc_isub :
			case opc_imul :
			case opc_idiv :
			case opc_irem :
			case opc_ineg :
			case opc_ishl :
			case opc_ishr :
			case opc_iushr :
			case opc_iand :
			case opc_ixor :
			case opc_ior :
			case opc_goto :
			case opc_goto_w :
			case opc_pop :
			case opc_pop2 :
			case opc_nop :
			case opc_wide :
			case opc_monitorenter :
			case opc_monitorexit :
				System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
				newLocals = copyLocals(previousLocals, previousLocalsLength);
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
				if(previousInstruction.isBlockMarker()) {
					System.arraycopy(previousStack, 0, newStack, 0, newTop + 1);
					newLocals = copyLocals(previousLocals, previousLocalsLength);
				} else {
					//we should never reach here
					throw new RuntimeException("unknown instruction");
				}
				break;
		}//end switch
			
		shapes.newStack = newStack;
		shapes.newLocals = newLocals;
	}
	
	private void getStacksForHandler(StackShapes shapes, int iin, int prev_iin, BT_ExceptionTableEntry handler) {
		BT_LocalCell previousLocals[] = stackShapes.localShapes[prev_iin];
		BT_LocalCell newLocals[] = pool.getDuplicate(previousLocals);
		BT_StackCell newStack[] = pool.getStack(1);
		if(handler.catchType == null) {
			newStack[0] = provider.getExceptionCell(provider.javaLangThrowable, iin, prev_iin, handler);
		} else {
			newStack[0] = provider.getExceptionCell(handler.catchType, iin, prev_iin, handler);
		}
		shapes.newStack = newStack;
		shapes.newLocals = newLocals;
	}
	
	private void mergeHandler(BT_Ins instruction, int iin, BT_StackCell newStack[], BT_LocalCell newLocals[], boolean again) 
			throws BT_CodeException {
		boolean changedStack = mergeHandlerStack(stackShapes.stackShapes, newStack, instruction, iin);
		boolean changedLocals = mergeHandlerLocals(stackShapes.localShapes, newLocals, instruction, iin);
		markChanges(changedStack || changedLocals, iin, again);
	}
	
	private void markChanges(boolean isChanged, int iin, boolean again) {
		if(isChanged) {
			if(again) {
				newlyChanged[iin] = true;
				done = false;
				if(iin == 0) {
					stackShapes.mergedInitial = true;
				}
			} else {
				changed[iin] = true;
			}
			saveStack(null, newStacks, iin);
			saveLocals(null, newLocalStacks, iin);
		}
	}

	/* same as mergeLocals, except that we know newStack will not be used elsewhere, so we must use it or return it to the pool */
	private boolean mergeHandlerLocals(BT_LocalCell stackShapes[][], BT_LocalCell newStack[], BT_Ins instruction, int iin) 
		throws BT_CodeException {
		BT_LocalCell existingStack[] = stackShapes[iin];
		if(existingStack == null) {
			stackShapes[iin] = newStack;
			return false;
		}
		int newLength = newStack.length;
		int existingLength = existingStack.length;
		if(newLength < existingLength) {
			mergeLocalTypes(instruction, iin, existingStack, newStack);
			saveLocals(newStack, stackShapes, iin);
			return true;
		}
		boolean changed = mergeLocalTypes(instruction, iin, newStack, existingStack);
		returnLocals(newStack);
		return changed;
	}
	
	/* same as mergeStacks, except that we know newStack will not be used elsewhere, so we must use it or return it to the pool */
	private boolean mergeHandlerStack(BT_StackCell stackShapes[][], BT_StackCell newStack[], BT_Ins instruction, int iin) 
		throws BT_CodeException {
		BT_StackCell existingStack[] = stackShapes[iin];
		if(existingStack == null) {
			stackShapes[iin] = newStack;
			return false;
		}
		int newLength = newStack.length;
		int existingLength = existingStack.length;
		if(newLength != existingLength) {
			throw new BT_InconsistentStackDepthException(code, instruction, iin);
		}
		boolean changedStack = mergeStackTypes(instruction, iin, newStack, existingStack);
		returnStack(newStack);
		return changedStack;
	}
	
	/**
	 * returns true if a merge took place (which means subsequent stacks will also need updating).
	 */
	private boolean mergeStacks(BT_StackCell stackShapes[][], BT_StackCell newStack[], BT_Ins instruction, int iin) 
		throws BT_CodeException {
		BT_StackCell existingStack[] = stackShapes[iin];
		int newLength = newStack.length;
		if(existingStack == null) {
			/* the newStack that we have been provided here will be cached and used elsewhere, so we must create a copy */
			BT_StackCell copiedStack[] = pool.getStack(newLength);
			stackShapes[iin] = copiedStack;
			System.arraycopy(newStack, 0, copiedStack, 0, newLength);
			return false;
		}
		if(existingStack.length != newLength) {
			throw new BT_InconsistentStackDepthException(code, instruction, iin);
		}
		return mergeStackTypes(instruction, iin, newStack, existingStack);
	}
	
	/**
	 * returns true if a merge took place (which means subsequent stacks will also need updating).
	 */
	private boolean mergeLocals(BT_LocalCell stackShapes[][], BT_LocalCell newStack[], BT_Ins instruction, int iin) 
		throws BT_CodeException {
		BT_LocalCell existingStack[] = stackShapes[iin];
		int newLength = newStack.length;
		if(existingStack == null) {
			/* the newStack that we have been provided here will be cached and used elsewhere, so we must create a copy */
			BT_LocalCell copiedStack[] = pool.getLocals(newLength);
			stackShapes[iin] = copiedStack;
			System.arraycopy(newStack, 0, copiedStack, 0, newLength);
			return false;
		}
		int existingLength = existingStack.length;
		if(newLength < existingLength) {
			BT_LocalCell mergedStack[] = pool.getLocals(newLength);
			System.arraycopy(existingStack, 0, mergedStack, 0, newLength);
			saveLocals(mergedStack, stackShapes, iin);
			mergeLocalTypes(instruction, iin, newStack, mergedStack);
			return true;
		}
		return mergeLocalTypes(instruction, iin, newStack, existingStack);
	}
	
	private void merge(BT_Ins instruction, int iin, BT_StackCell newStack[], BT_LocalCell newLocals[], boolean again) 
			throws BT_CodeException {
		boolean changedStack = mergeStacks(stackShapes.stackShapes, newStack, instruction, iin);
		boolean changedLocals = mergeLocals(stackShapes.localShapes, newLocals, instruction, iin);
		markChanges(changedStack || changedLocals, iin, again);
	}
	
	private void saveStack(BT_StackCell stack[], BT_StackCell stacks[][], int index) {
		BT_StackCell saved[] = stacks[index];
		stacks[index] = stack;
		if(saved != null) {
			returnStack(saved);
		}
	}
	
	private void returnStack(BT_StackCell stack[]) {
		pool.returnStack(stack);
	}
	
	private void saveLocals(BT_LocalCell stack[], BT_LocalCell stacks[][], int index) {
		BT_LocalCell saved[] = stacks[index];
		stacks[index] = stack;
		if(saved != null) {
			returnLocals(saved);
		}
	}
	
	private void returnLocals(BT_LocalCell stack[]) {
		pool.returnLocals(stack);
	}
	
	/**
	 * @param stack1 the stack to be merged with stack2
	 * @param stack2 the stack to be merged into
	 * @return whether a merge of two types occurred
	 */
	private boolean mergeStackTypes(
			BT_Ins instruction,
			int instructionIndex,
			BT_StackCell stack1[],
			BT_StackCell stack2[]) throws BT_CodeException {
		boolean ret = false;
		int length = stack2.length;
		for(int i=0; i<length; i++) {
			int stackDepth = length - i - 1;
			BT_StackCell existingCell = stack2[i];
			BT_StackCell newCell = stack1[i];
			if(existingCell == null) {
				throw new BT_InconsistentStackTypeException(code, instruction, instructionIndex, 
						newCell == null ? null : newCell.getCellType(), null, stackDepth);
			}
			BT_StackType existingType = existingCell.getCellType();
			if(newCell == null) {
				throw new BT_InconsistentStackTypeException(code, instruction, instructionIndex, null , existingType, stackDepth);
			}
			BT_StackType newType = newCell.getCellType();
			BT_StackType type = mergeTypes(
					instruction, 
					instructionIndex,
					newType,
					existingType);
			if(type == null) {
				throw new BT_InconsistentStackTypeException(code, instruction, instructionIndex, newType, existingType, stackDepth);
			}
			if(!type.equals(existingType)) {
				ret = true;
				stack2[i] = provider.getMergedStackCell(type, existingCell, newCell, stackDepth == 0, instructionIndex);
			} else {
				BT_StackCell mergedCell = provider.getMergedStackCell(existingCell, newCell, stackDepth == 0, instructionIndex);
				/* 
				 * In the case where the BT_StackType objects are acting as the BT_StackCell objects, the call below will be quick,
				 * because we know that they are equal by the above comparison, and since most such objects are shared 
				 * they are equal by identity, which is the first check done by the equals method.  So the check below likely
				 * will repeat the very same identity comparison.  
				 */
				if(!mergedCell.equals(existingCell)) {
					ret = true;
					stack2[i] = mergedCell;
				}
			}
		}
		return ret;
	}
	
	/**
	 * @param stack1 the stack to be merged with stack2
	 * @param stack2 the stack to be merged into
	 * @return whether a merge of two types occurred
	 */
	private boolean mergeLocalTypes(
			BT_Ins instruction,
			int instructionIndex,
			BT_LocalCell stack1[],
			BT_LocalCell stack2[]) throws BT_CodeException {
		boolean ret = false;
		int length = stack2.length;
		for(int i=0; i<length; i++) {
			BT_LocalCell existingLocalCell = stack2[i];
			if(existingLocalCell == null) {
				continue;
			} 
			BT_LocalCell newLocalCell = stack1[i];
			if(newLocalCell == null) {
				if(existingLocalCell != null) {
					ret = true;
					stack2[i] = null;
					continue;
				}
			} 
			BT_StackType existingType = existingLocalCell.getCellType();
			BT_StackType type = mergeTypes(
					instruction, 
					instructionIndex,
					newLocalCell.getCellType(),
					existingType);
			if(type == null) {
				if(existingType != null) {
					ret = true;
					stack2[i] = null;
				}
			} else if(!type.equals(existingType)) {
				ret = true;
				stack2[i] = provider.getMergedLocalCell(type, existingLocalCell, newLocalCell, instructionIndex);
				
			} else {
				BT_LocalCell mergedLocalCell = provider.getMergedLocalCell(existingLocalCell, newLocalCell, instructionIndex);
				if(!mergedLocalCell.equals(existingLocalCell)) {
					ret = true;
					stack2[i] = mergedLocalCell;
				}
			}
		}
		return ret;
	}
	
	/**
	 * @param typ1 the first type to merge
	 * @param type2 the second type to merge
	 * @return the merged type which will be type2 if no merge was required, 
	 * 	or null if the merge failed and isLocal is true
	 * @throws MergeFailureException if the merge failed and isLocal is false
	 */
	protected BT_StackType mergeTypes(
			BT_Ins instruction,
			int instructionIndex,
			BT_StackType type1,
			BT_StackType existingType) throws BT_CodeException {
		if(type1.isSameType(existingType)) {
			//ReturnAddress and StubType: they are considered the same type but not always equals
			//for all others, isSameType and equals is the same
			if(existingType.isReturnAddress()) {
				ReturnAddress merged = (ReturnAddress) existingType;
				ReturnAddress newReturnAddress = new ReturnAddress(merged, (ReturnAddress) type1);
				return newReturnAddress;
			}
			if(existingType.isStubObjectType()) {
				StubType existing = existingType.getStubType();
				StubType t1 = type1.getStubType();
				if(!existing.getStubs().contains(t1.getStubs())) {
					return new StubType(existingType.getStubType().getType()).addStubs(existing.getStubs()).addStubs(t1.getStubs());
				}
			}
			return existingType;
		}
		
		if(!type1.isObjectType() || !existingType.isObjectType()) {
			return null;
		}
		
		//find the common superclass
		if(type1.isNull()) {
			return existingType;
		}
		if(existingType.isNull()) {
			return type1;
		}
		
		ClassType classType1 = type1.getClassType();
		ClassType classType2 = existingType.getClassType();
		BT_Class class1 = classType1.type;
		BT_Class class2 = classType2.type;
		
		//at this point we know they are both objects and neither is null
		ClassType result = findCommonClass(class1, class2);
		
		if(type1.isStubObjectType()) {
			//merging type x with a stub must be a stub unless the merged type is x itself and x is not a stub
			if(existingType.isStubObjectType() || !result.equals(existingType)) {
				//we know result is not a stub type because it's been merged with java.lang.Object 
				//or merged with an array with java.lang.Object as element type
				StubType stubResult = new StubType(result.getType());
				StubType t1 = type1.getStubType();
				stubResult.addStubs(t1.getStubs());
				if(existingType.isStubObjectType()) {
					StubType existing = existingType.getStubType();
					stubResult.addStubs(existing.getStubs());
				}
				return stubResult;
			}
		}
		
		if(existingType.isStubObjectType() && !result.equals(type1)) {
			//we know result is not a stub type because it's been merged with java.lang.Object 
			//or merged with an array with java.lang.Object as element type
			StubType stubResult = new StubType(result.getType());
			StubType existing = existingType.getStubType();
			stubResult.addStubs(existing.getStubs());
			return stubResult;
		}
		
		return result;
	}
	
	
	/**
	 * 
	 * @param one
	 * @param two
	 * @return the common class
	 */
	private ClassType findCommonClass(BT_Class class1, BT_Class class2) throws BT_CodeException {
		if(class1.isArray()) {
			if(class2.isArray()) {
				BT_Class elementClass1 = class1.getElementClass();
				BT_Class elementClass2 = class2.getElementClass();
				if(elementClass1.isPrimitive() || elementClass2.isPrimitive()) {
					return provider.javaLangObject.classType;
				}
				ClassType commonElementClass = findCommonClass(elementClass1, elementClass2);
				if(commonElementClass.isStubObjectType()) {
					StubType elementStub = commonElementClass.getStubType(); 
					BT_Class arrayClass = commonElementClass.type.getArrayClass();
					StubType stub = new StubType(arrayClass);
					return stub.addStubs(elementStub.getStubs());
				}
				return commonElementClass.type.getArrayClass().classType;
			}
			if(class2.isInterface()) {
				if(class2.isArrayInterface()) {
					return class2.classType;
				}
			}
			return provider.javaLangObject.classType;
		}
		if(class2.isArray()) {
			if(class1.isInterface()) {
				if(class1.isArrayInterface()) {
					return class1.classType;
				}
			}
			return provider.javaLangObject.classType;
		}
		if(class1.isInterface()) {
			if(!class2.isInterface()) {
				if(class1.isInterfaceAncestorOf(class2)) {
					return class1.classType;
				}
				if(class2.isStubOrHasParentStub()) {
					return new StubType(class1.getRepository()).addStub(class2);
				}
				return provider.javaLangObject.classType;
			}
			//merging two interfaces
			/*
			 Consider the following:
			 interface C {}
			 interface D {}
			 interface A extends C, D {}
			 interface B extends C, D {}
			 When merging A and B to a common interface, we cannot choose both C and D. 
			 
			 So we must merge to Object.
			 Type verification for interfaces is done at runtime.
			 */
			return provider.javaLangObject.classType;
		}
		if(class2.isInterface()) {
			if(class2.isInterfaceAncestorOf(class1)) {
				return class2.classType;
			}
			if(class1.isStubOrHasParentStub()) {
				return new StubType(class1.getRepository()).addStub(class1);
			}
			return provider.javaLangObject.classType;
		}
		return findCommonSuperClass(class1, class2);
	}
	
	private ClassType findCommonSuperClass(BT_Class class1, BT_Class class2) throws BT_CodeException {
		/* note: we only match to a merge candidate if the result is not class1 or class2 or java.lang.Object */
		if(class1.equals(class2) || class1.isClassAncestorOf(class2)) {
			return class1.classType;
		}
		do {
			class1 = class1.getSuperClass();
			if(class1.equals(class2)) {
				return class2.classType;
			}
		} while(!class1.isClassAncestorOf(class2));
		if(class1.isStubOrHasSuperClassStub() || class2.isStubOrHasSuperClassStub()) {
			return new StubType(class1.getRepository()).addStub(class1).addStub(class2);
		}
		if(class1.equals(provider.javaLangObject)) {
			return class1.classType;
		}
		return matchToMergeCandidate(class1).classType;
	}
	
	//so when we merge, if a non-trivial merge, we find a class that is consumed somewhere in the method
	private BT_Class matchToMergeCandidate(BT_Class clazz) throws BT_CodeException {
		if(!useMergeCandidates) {
			return clazz;
		}
		buildMergeCandidates();
		while(!mergeCandidates.contains(clazz)) {
			clazz = clazz.getSuperClass();
			if(clazz == null) {
				return provider.javaLangObject;
			}
		}
		return clazz;
	}
	
	private void buildMergeCandidates() throws BT_CodeException {
		if(mergeCandidates != null) {
			return;
		}
		
		/* merge candidates are types that are consumed from the stack by an instruction */
		/* types that are added to the stack are not merge candidates */
		mergeCandidates = new BT_HashedClassVector();
		BT_CodeVisitor visitor = new BT_CodeVisitor() {
			boolean foundCanThrow = false;
			boolean foundReturn = false;
			
			protected boolean visit(
					BT_Ins instruction, 
					int iin, 
					BT_Ins previousInstruction, 
					int prev_iin, 
					BT_ExceptionTableEntry handler) {
				switch(instruction.opcode) {
					case opc_athrow:
						if(!foundCanThrow) {
							/* java.lang.Throwable if throws objects */
							mergeCandidates.addUnique(provider.javaLangThrowable);
						}
						foundCanThrow = true;
						break;
					case opc_areturn:
						if(!foundReturn) {
							/* return type is returns objects */
							mergeCandidates.addUnique(code.getMethod().getSignature().returnType);
						}
						foundReturn = true;
						break;
					case opc_invokeinterface:
					case opc_invokevirtual:
					case opc_invokespecial:
						if(!(instruction.isInvokeSpecialIns() && instruction.getMethodTarget().isConstructor())) {
							/* the invoke target class if the target method is not a constructor */
							mergeCandidates.addUnique(instruction.getResolvedClassTarget(code));
						}
						/* fall through */
					case opc_invokestatic:
						BT_Method target = instruction.getMethodTarget();
						BT_MethodSignature signature = target.getSignature();
						BT_ClassVector types = signature.types;
						for(int i=0; i<types.size(); i++) {
							BT_Class type = types.elementAt(i);
							if(!type.isPrimitive()) {
								/* parameters of an invoke */
								mergeCandidates.addUnique(type);
							}
						}
						break;
					case opc_getfield:
						/* target class of getfield or putfield */
						mergeCandidates.addUnique(instruction.getResolvedClassTarget(code));
						break;
					case opc_putfield:
						mergeCandidates.addUnique(instruction.getResolvedClassTarget(code));
						/* fall through */
					case opc_putstatic:
						/* type of a putfield or putstatic */
						mergeCandidates.addUnique(instruction.getFieldTarget().getFieldType());
						break;
					case opc_checkcast:
					case opc_instanceof:
						/* the target class of an instanceof or checkcast NOT NECESSARY */
						//mergeCandidates.addUnique(instruction.getClassTarget());
					default:
						break;
				}
				return true;
			}
		};
		code.visitReachableCode(visitor);
	}
	
	
}


