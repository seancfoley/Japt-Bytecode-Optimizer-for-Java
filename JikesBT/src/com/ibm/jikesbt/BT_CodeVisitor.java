package com.ibm.jikesbt;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.ibm.jikesbt.BT_ObjectCode.Routine;
import com.ibm.jikesbt.BT_ObjectCode.SubRoutine;

/**
 * Visitor of a BT_CodeAttribute.  The BT_CodeAttribute visitReachable code methods
 * update BT_CodeVisitor objects as the code is visited.  Additionally, users may override
 * key methods in this class to analyze the code while it is being visited.
 * The code must not be changed while it is being visited. 
 * The key methods for overriding are the methods setUp, tearDown, visit, additionalVisit and visitHandler.  
 * @see com.ibm.jikesbt.BT_CodeAttribute.visitReachableCode
 */
public class BT_CodeVisitor {
	/**
	 * The instruction index passed in as the previous instruction index
	 * when the instruction is the first instruction being executed.
	 */
	protected static final int ENTRY_POINT = -1;
	
	/**
	 BT_CodeAttribute that is currently being visited
	**/	
	protected BT_CodeAttribute code;
	
	/* 
	 * altered during visiting
	 */
	private boolean exited;
	boolean[] visited; //visited[iin] == true if we have already visited the instruction at index iin
	private LinkedList[] insHandlers; //a linked list of exception handlers for each instruction index
	private HashMap subRoutines; //maps jsr instruction to SubRoutine objects
	private SubRoutine currentSubRoutine;
	private HashMap handlerSubRoutineMap; //maps BT_ExceptionTableEntry to SubRoutine
	
	/* 
	 * initialized before visiting and never altered during visiting
	 * 
	 * not to be accessed by visitors
	 */
	Routine mainRoutine;
	boolean fromMiddle; // true if the last visit begain from the middle of the code
	protected boolean revisitHandlers; //if true, an exception handler will be visited (or additionally visited) from every reaching instruction
	
	
	
	void initialize(BT_CodeAttribute codeAttribute, Routine mainRoutine) {
		this.mainRoutine = mainRoutine;
		initialize(codeAttribute);	
	}
	
	/**
	 * This method is called before setup and initializes all required
	 * structures needed by the code visitor to perform visiting., so all codeVisitor methods
	 * are accessible from the setup method.
	 */
	void initialize(BT_CodeAttribute codeAttribute) {
		this.code = codeAttribute;
		BT_InsVector ins = code.getInstructions();
		this.visited = new boolean[ins.size()];
		this.insHandlers = new LinkedList[ins.size()];
		BT_ExceptionTableEntryVector exceptionHandlers = code.getExceptionTableEntries();
		exceptionHandlers.removeEmptyRanges();
		for(int i=0; i<exceptionHandlers.size(); i++) {
			BT_ExceptionTableEntry e = exceptionHandlers.elementAt(i);
			int startIndex = ins.indexOf(e.startPCTarget);
			int endIndex = ins.indexOf(e.endPCTarget);
			if(startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
				continue;
			}
			for(int j=startIndex; j<endIndex; j++) {
				List currentHandlers = insHandlers[j];
				if(currentHandlers == null) {
					currentHandlers = insHandlers[j] = new LinkedList();
				}
				currentHandlers.add(e);
			}
		}
	}
	
	void addSubRoutine(SubRoutine routine) {
		if(subRoutines == null) {
			subRoutines = new HashMap();
		}
		subRoutines.put(routine.startInstruction, routine);
	}
	
	/**
	 * gets the subroutine that starts at the given instruction index.
	 * The visitor maintains a list of subroutine objects.
	 * @param startInstruction
	 * @return the subroutine
	 */
	SubRoutine getSubRoutine(BT_Ins startInstruction) {
		SubRoutine routine;
		if(subRoutines != null) {
			routine = (SubRoutine) subRoutines.get(startInstruction);
			if(routine != null) {
				return routine;
			}
		} else {
			subRoutines = new HashMap();
		}
		routine = code.createSubRoutine(startInstruction);
		subRoutines.put(routine.startInstruction, routine);
		return routine;
	}
	
	void setCurrentSubRoutine(SubRoutine sub) {
		currentSubRoutine = sub;
	}
	
	SubRoutine getCurrentSubRoutine() {
		return currentSubRoutine;
	}
	
	Routine getEncompassingRoutine(BT_ExceptionTableEntry handler, Routine currentRoutine) {
		if(!(currentRoutine instanceof SubRoutine)) {
			return currentRoutine;
		}
		SubRoutine currentSub = (SubRoutine) currentRoutine;
		if(handlerSubRoutineMap != null) {
			SubRoutine encompassingRoutine = (SubRoutine) handlerSubRoutineMap.get(handler);
			if(encompassingRoutine != null) {
				return encompassingRoutine;
			}
		}
		//this check is here in case the code visitor entered from somewhere in the middle of the code,
		//or the current subroutine lies within the handler range but the encompassing routine is the main routine
		int handlerStartIndex = code.getInstructions().indexOf(handler.startPCTarget);
		if(currentSub.startIndex > handlerStartIndex) {
			return mainRoutine;
		}
		if(handlerSubRoutineMap == null) {
			handlerSubRoutineMap = new HashMap();
		}
		handlerSubRoutineMap.put(handler, currentSub);
		return currentSub;
	}
	
	/**
	 * gets the exception handlers triggered by visiting the instruction at the given
	 * instruction index.
	 */
	BT_ExceptionTableEntry[] getTriggeredExceptionHandlers(int iin) {
		LinkedList triggeredHandlers = insHandlers[iin];
		if(triggeredHandlers == null) {
			return BT_ExceptionTableEntry.emptyHandlers;
		}
		return (BT_ExceptionTableEntry[]) 
			triggeredHandlers.toArray(new BT_ExceptionTableEntry[triggeredHandlers.size()]);
	}

    void removeVisitableHandler(BT_ExceptionTableEntry e) {
        BT_InsVector ins = code.getInstructions();
        int startIndex = ins.indexOf(e.startPCTarget);
        int endIndex = ins.indexOf(e.endPCTarget);
        for(int m=startIndex; m<endIndex; m++) {
        	List currentHandlers = insHandlers[m];
        	if(currentHandlers != null && currentHandlers.remove(e)) {
        		if(currentHandlers.size() == 0) {
        			insHandlers[m] = null;
        		}
        	}
        }
    }
	
	
	/**
	 * Marks the instruction as visited. 
	 * After this call isVisited(iin) will return true.
	 * @param iin Instruction index in the instruction array
	 **/
	void markVisited(int iin) {
		visited[iin] = true;
	}
	
	/**
	 * Calling this method causes the code visiting to stop.
	 */
	final protected void exit() {
		exited = true;
	}
	
	
	/**
	 * @param iin Instruction number in <code>codeAttribute.ins</code> array
	 * @return true if the instruction at the given index has been visited.
	 **/
	final public boolean isVisited(int iin) {
		return visited[iin];
	}
	
	
	/**
	 * @return whether exit has been called on this visitor
	 */
	final public boolean exited() {
		return exited;
	}
	
	/**
	 * returns all the subroutines that have been found
	 * @return the array of subroutines
	 */
	final public SubRoutine[] getSubRoutines() {
		if(subRoutines == null) {
			return new SubRoutine[0];
		}
		Collection collection = subRoutines.values();
		return (SubRoutine[]) collection.toArray(new SubRoutine[collection.size()]);
	}
	
	/**
	 Prepares the visitor for visiting.  Meant to be overridden, this implementation does nothing.
	 The field code will be initialized when this method is called.
	 **/
	protected void setUp() {}

	/**
	 * Visits the instruction. It is guaranteed that no instruction will be visited more than one time.
	 * This implementation simply returns true.
	 *
	 * @param instruction the instruction being visited
	 * @param iin the index of the instruction
	 * @param previousInstruction the previous instruction or null
	 * @param prev_iin the index of the previous instruction or ENTRY_POINT
	 * @param handler the instruction handler that caused this instruction to be reached 
	 * 	or null if this instruction is not reached by a try clause
	 * @return whether to proceed to the next instruction(s).  Note that for the given instruction
	 **/
	protected boolean visit(
			BT_Ins instruction, 
			int iin, 
			BT_Ins previousInstruction, 
			int prev_iin, 
			BT_ExceptionTableEntry handler) 
			throws BT_CodeException {
		return true;
	}
	
	/**
	 * This method is called when the indicated instruction has been visited before,
	 * but not when arriving from the indicated previous instruction.  
	 * Meant to be overridden, this implementation does nothing.
	 * @param instruction the instruction being visited
	 * @param iin the index of the instruction
	 * @param previousInstruction the previous instruction or null
	 * @param prev_iin the index of the previous instruction or ENTRY_POINT
	 * @param handler the instruction handler that caused this instruction to be reached 
	 *  or null if this instruction is not reached by a try clause
	 */
	protected void additionalVisit(
			BT_Ins instruction, 
			int iin, 
			BT_Ins previousInstruction, 
			int prev_iin, 
			BT_ExceptionTableEntry handler) throws BT_CodeException  {}
	
	/**
	 * Returns whether the code visitor should visit the given instruction handler at the given index.
	 * Note that this method will be queried for each instruction with the range of the handler,
	 * after visit has been called for that instruction, until the handler has been visited.  
	 * Once a handler has been visited, this method will not be queried for that handler again.
	 * This implementation returns true at all times, so each handler will be visited as soon
	 * as the visitor has visited an instruction inside the handler's range.
	 * @param handler the handler
	 * @param instructionIndex the index of the instruction
	 * @return whether to visit the instruction handler
	 */
	protected boolean visitHandler(BT_ExceptionTableEntry handler, int instructionIndex) {
		return true;
	}
	
	/**
	 * Called after visiting.  Meant to be overridden, this implementation does nothing.
	 **/
	protected void tearDown() {}
}
