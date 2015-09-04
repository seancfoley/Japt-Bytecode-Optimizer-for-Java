package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_StackType.ClassType;

/**
 * This provider is used to determine whether a given stack or local slot can be null.
 * 
 * @author sfoley
 *
 */
public class BT_PopulatedNullProvider extends BT_StackCellProvider {
	
	public BT_PopulatedNullProvider(BT_Repository repository) {
		super(repository);
	}

	/**
	 * Returns the class type for the object which is obtained by the given instruction.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getNullClassCell(int instructionIndex, int previousInstructionIndex) {
		return BT_StackType.NULL;
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given instruction.
	 * The class represents an object type.
	 * @param clazzType
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getObjectArrayElementClassCell(ClassType clazzType, int instructionIndex, int previousInstructionIndex) {
		return BT_StackType.NULL;
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given instruction.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getObjectClassCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex) {
		return BT_StackType.NULL;
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given cast instruction.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getCastedClassCell(BT_Class clazz, BT_StackCell cell, int instructionIndex, int previousInstructionIndex) {
		if(cell.getCellType().isNull()) {
			return BT_StackType.NULL;
		}
		return super.getCastedClassCell(clazz, cell, instructionIndex, previousInstructionIndex);
	}
	
	/**
	 * Returns the class type for the object which is obtained as an argument to the method.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_LocalCell getArgumentObjectCell(BT_Class clazz, int paramsIndex, int paramsCount) {
		return BT_StackType.NULL;
	}
	
	/**
	 * Returns the cell which is the result of a merge of two stack cells.
	 * @param mergedType both the merged type and one of the original two types being merged
	 * @param type2 the other merged type
	 */
	protected BT_StackCell getMergedStackCell(BT_StackCell merged, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		if(cell2.getCellType().isNull()) {
			return BT_StackType.NULL;
		}
		return merged;
	}
	
	/**
	 * Returns the cell which is the result of a merge of two stack cells.
	 */
	protected BT_StackCell getMergedStackCell(BT_StackType merged, BT_StackCell cell1, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		/* type merges involve either non-primitive class types, the null type, or the merge of two return addresses */
		if(cell2.getCellType().isNull() || cell1.getCellType().isNull()) {
			return BT_StackType.NULL;
		}
		return merged;
	}
	
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 * @param mergedType both the merged type and one of the original two types being merged
	 * @param type2 the other merged type
	 */
	protected BT_LocalCell getMergedLocalCell(BT_LocalCell mergedType, BT_LocalCell type2, int instructionIndex) {
		if(type2.getCellType().isNull()) {
			return BT_StackType.NULL;
		}
		return mergedType;
	}
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 */
	protected BT_LocalCell getMergedLocalCell(BT_StackType mergedType, BT_LocalCell type1, BT_LocalCell type2, int instructionIndex) {
		/* type merges involve either non-primitive class types, the null type, or the merge of two return addresses */
		if(type2.getCellType().isNull() || type1.getCellType().isNull()) {
			return BT_StackType.NULL;
		}
		return mergedType;
	}
}
