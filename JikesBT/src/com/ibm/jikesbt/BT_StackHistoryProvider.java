/*
 * Created on Nov 3, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_StackType.ClassType;


public class BT_StackHistoryProvider extends BT_StackCellProvider {

	public BT_StackHistoryProvider(BT_Repository repository) {
		super(repository);
	}

	protected BT_StackCell getNullClassCell(int instructionIndex, int previousInstructionIndex) {
		return new BT_TypeHistoryStackCell(BT_StackType.NULL, instructionIndex);
	}
	
	protected BT_StackCell getObjectArrayElementClassCell(ClassType clazzType, int instructionIndex, int previousInstructionIndex) {
		return new BT_TypeHistoryStackCell(clazzType, instructionIndex);
	}
	
	protected BT_StackCell getObjectClassCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex) {
		return new BT_TypeHistoryStackCell(clazz.classType, instructionIndex);
	}
	
	protected BT_StackCell getCastedClassCell(BT_Class clazz, BT_StackCell cell, int instructionIndex, int previousInstructionIndex) {
		return new BT_TypeHistoryStackCell(clazz.classType, instructionIndex);
	}

	/**
	 * Returns the class type for the object which is obtained by the given instruction.  
	 * This object is NOT necessarily on the top of the stack.
	 * The object was created because a constructor altered the state of an uninitalized object
	 * on the stack and in the locals to an initialized object.
	 * 
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getInitializedClassCell(BT_StackType uninitializedType, BT_Class clazz, BT_StackCell cell, boolean isStackTop, int instructionIndex, int previousInstructionIndex) {
		if(isStackTop) {
			return new BT_TypeHistoryStackCell(clazz.classType, instructionIndex);
		}
		return clazz.classType;
	}
	
	protected BT_StackCell getConstantObjectCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex) {
		return new BT_TypeHistoryStackCell(clazz.classType, instructionIndex);
	}
	
	/**
	 * Updates history before returning the type.
	 */
	protected BT_StackCell getLoadedReferenceCell(BT_LocalCell cell, int instructionIndex, int previousInstructionIndex) {
		BT_StackType type = cell.getCellType();
		if(type.isNull() || type.isUninitializedObject()) {
			return type;
		}
		return new BT_TypeHistoryStackCell(type.getClassType(), instructionIndex);
	}

	/**
	 * Updates history before returning the type.
	 */
	protected BT_StackCell getExceptionCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex, BT_ExceptionTableEntry handler) {
		return new BT_TypeHistoryStackCell(clazz.classType, instructionIndex);
		
	}

	/**
	 * Returns the type that is a duplicate of the given type on the stack.
	 * The duplicate will be the one that is closest to the top of the stack,
	 * the original will be the one further down the stack.
	 * The stack index of the top of the stack is 0, the one below is 1, and so on.
	 * The class represents an object type.
	 * 
	 * Each BT_TypeHistoryStackCell C keeps track of both its history of being placed on the top of the stack
	 * and its history of being duplicated to another BT_TypeHistoryStackCell D.  That way, if we insert checkcasts
	 * to change the type of C, we can also insert additional checkcasts to reinstate the type of D.
	 * 
	 * @param type
	 * @param duplicateStackIndex the stack index of the duplicate measured from the top of the stack, 
	 *  so index 0 points to the top, index 1 is the next one, and so on.
	 * @param originalStackIndex the stack index of the original, as measured from the top of the stack.  This index
	 *  is always larger than duplicateStackIndex
	 *  @return the BT_StackType that will appear in the new stack at duplicateStackIndex
	 */
	protected BT_StackCell getDuplicateObjectCell(BT_StackCell cell, int duplicateStackIndex, int originalStackIndex, int instructionIndex, int previousInstructionIndex) {
		if(cell instanceof BT_TypeHistoryStackCell) {
			BT_TypeHistoryStackCell duplicated = (BT_TypeHistoryStackCell) cell;
			duplicated.addDuplicate(duplicateStackIndex, instructionIndex);
		}
		BT_TypeHistoryStackCell duplicate = new BT_TypeHistoryStackCell(cell.getClassType(), instructionIndex);
		return duplicate;
	}
	
	/**
	 * Note that the given ClassType is consumed by the instruction at the given index.
	 *
	 */
	protected void consumeCell(BT_StackCell cell, int instructionIndex) {
		BT_StackType type = cell.getCellType();
		if(type.isNonNullObjectType() && cell instanceof BT_TypeHistoryStackCell) {
			BT_TypeHistoryStackCell historyType = (BT_TypeHistoryStackCell) cell;
			historyType.setConsumed(instructionIndex);
		}
	}
	
	/**
	 * Returns the type for the object which is the result of a merge of two local types.
	 * @param mergedType both the merged type and one of the original two types being merged
	 * @param type2 the other merged type
	 */
	protected BT_StackCell getMergedStackCell(BT_StackCell merged, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		if(cell2 instanceof BT_TypeHistoryStackCell) {
			BT_TypeHistoryStackCell typeHist2 = (BT_TypeHistoryStackCell) cell2;
			
			//check to ensure that the merged type also has the same history (ie propagate the history),
			//if not we create a new merged type (we must not alter the existing one to ensure changes are propagated by the visitor
			if(merged instanceof BT_TypeHistoryStackCell) {
				BT_TypeHistoryStackCell mergedStoredType = (BT_TypeHistoryStackCell) merged;
				if(mergedStoredType.containsHistory(typeHist2)) {
					return merged;
				}
			} 
		}
		
		return getMergedStackCell(merged.getCellType(), merged, cell2, isStackTop, instructionIndex);
	}
	
	/**
	 * Returns the type for the object which is the result of a merge of two local types.
	 */
	protected BT_StackCell getMergedStackCell(BT_StackType merged, BT_StackCell cell1, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		if(merged.isNonNullObjectType()) {
			if(isStackTop) {
				return new BT_TypeHistoryStackCell(merged.getClassType(), instructionIndex);
			}
			return new BT_TypeHistoryStackCell(merged.getClassType(), cell1, cell2);
		}
		return merged;
	}
}
