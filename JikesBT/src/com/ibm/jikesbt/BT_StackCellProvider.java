/*
 * Created on Nov 3, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_StackType.ClassType;
import com.ibm.jikesbt.BT_StackType.UninitializedObject;

public class BT_StackCellProvider {

	final protected ClassType intClass;
	final protected ClassType floatClass;
	final protected ClassType longClass;
	final protected ClassType doubleClass;
	
	final public BT_Class javaLangObject;
	final public BT_Class javaLangString;
	final public BT_Class javaLangClass;
	final public BT_Class javaLangThrowable;
	
	public BT_StackCellProvider(BT_Repository repository) {
		this.floatClass = repository.getFloat().classType;
		this.intClass = repository.getInt().classType;
		this.longClass = repository.getLong().classType;
		this.doubleClass = repository.getDouble().classType;
		this.javaLangObject = repository.findJavaLangObject();
		this.javaLangString = repository.findJavaLangString();
		this.javaLangThrowable = repository.findJavaLangThrowable();
		this.javaLangClass = repository.findJavaLangClass();
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
		return clazzType;
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given instruction.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getObjectClassCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex) {
		return clazz.classType;
	}
	
	protected BT_StackCell getUninitializedObject(BT_NewIns newIns, int instructionIndex, int previousInstructionIndex) {
		return new UninitializedObject(newIns);
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given cast instruction.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getCastedClassCell(BT_Class clazz, BT_StackCell cell, int instructionIndex, int previousInstructionIndex) {
		return clazz.classType;
	}
	
	/**
	 * Returns the class type for the object which is obtained as an argument to the method.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_LocalCell getArgumentObjectCell(BT_Class clazz, int paramsIndex, int paramsCount) {
		return clazz.classType;
	}
	
	protected BT_LocalCell getUninitializedThisCell(BT_Class clazz, int paramsCount) {
		return BT_StackType.UNINITIALIZED_THIS;
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given instruction.  This object is NOT necessarily on the top of the stack.
	 * 
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_StackCell getInitializedClassCell(BT_StackType uninitializedType, BT_Class clazz, BT_StackCell cell, 
			boolean isStackTop, int instructionIndex, int previousInstructionIndex) {
		return clazz.classType;
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given instruction.  This object is NOT necessarily on the top of the stack.
	 * 
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_LocalCell getInitializedClassLocalCell(BT_StackType uninitializedType, BT_Class clazz, BT_LocalCell cell, 
			int instructionIndex, int previousInstructionIndex) {
		return clazz.classType;
	}
	
	/**
	 * Returns the class type for the object which is obtained by the given load instruction.
	 * The class represents a reference type: an object, null or an uninitialized object.
	 * @param type
	 * @param instruction the instruction that loaded the class type from a local variable
	 */
	protected BT_StackCell getLoadedReferenceCell(BT_LocalCell cell, int instructionIndex, int previousInstructionIndex) {
		return cell.getCellType();
	}
	
	/**
	 * Returns the type which is obtained by the given load instruction.
	 * The type is a non-reference type.
	 * @param type
	 * @param instruction the instruction that loaded the class type from a local variable
	 */
	protected BT_StackCell getLoadedPrimitiveCell(BT_LocalCell cell, int instructionIndex, int previousInstructionIndex) {
		return cell.getCellType();
	}
	
	/**
	 * Returns the class type for the object.
	 * The class represents an exception type.
	 * @param clazz
	 * @param instructionIndex the index of the handler's target, BT_ExceptionTableEntry.handlerTarget
	 */
	protected BT_StackCell getExceptionCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex, BT_ExceptionTableEntry handler) {
		return clazz.classType;
	}
	
	/**
	 * Returns the class type for the object.
	 * The class represents a constant object type, either java.lang.String or java.lang.Class.
	 * @param clazz
	 */
	protected BT_StackCell getConstantObjectCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex) {
		return clazz.classType;
	}
	
	/**
	 * Returns the cell that is a duplicate of the given cell on the stack.
	 * The duplicate will be the one that is closest to the top of the stack,
	 * the original will be the one further down the stack.
	 * The stack index of the top of the stack is 0, the one below is 1, and so on.
	 * The class represents an object type.
	 * 
	 * @param type
	 * @param duplicateStackIndex the stack index of the duplicate measure from the top of the stack, 
	 *   so index 0 points to the top, index 1 is the next one, and so on.
	 * @param originalStackIndex the stack index of the original, as measured from the top of the stack.  This index
	 * is always larger than duplicateStackIndex
	 */
	protected BT_StackCell getDuplicateObjectCell(BT_StackCell cell, int duplicateStackIndex, int originalStackIndex, int instructionIndex, int previousInstructionIndex) {
		return cell;
	}
	
	/**
	 * Returns the type for the given type that has been stored into a local variable.
	 * by the instruction at the given instruction index.
	 * @param storedType
	 * @param instructionIndex
	 * @return
	 */
	protected BT_LocalCell getLocalCell(BT_LocalCell localCell, BT_IIncIns iincIns, int instructionIndex) {
		return localCell.getCellType();
	}
	
	
	/**
	 * Returns the type for the given type that has been stored into a local variable.
	 * by the instruction at the given instruction index.
	 * @param storedType
	 * @param instructionIndex
	 * @return
	 */
	protected BT_LocalCell getLocalCell(BT_StackCell stackCell, BT_StoreLocalIns storeIns, int instructionIndex) {
		return stackCell.getCellType();
	}
	
	
	/**
	 * Returns the cell which is the result of a merge of two stack cells.
	 * @param mergedType both the merged type and one of the original two types being merged
	 * @param type2 the other merged type
	 */
	protected BT_StackCell getMergedStackCell(BT_StackCell merged, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		return merged;
	}
	
	/**
	 * Returns the cell which is the result of a merge of two stack cells.
	 */
	protected BT_StackCell getMergedStackCell(BT_StackType merged, BT_StackCell cell1, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		/* type merges involve either non-primitive class types, the null type, or the merge of two return addresses */
		return merged;
	}
	
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 * @param mergedType both the merged type and one of the original two types being merged
	 * @param type2 the other merged type
	 */
	protected BT_LocalCell getMergedLocalCell(BT_LocalCell mergedType, BT_LocalCell type2, int instructionIndex) {
		return mergedType;
	}
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 */
	protected BT_LocalCell getMergedLocalCell(BT_StackType mergedType, BT_LocalCell type1, BT_LocalCell type2, int instructionIndex) {
		/* type merges involve either non-primitive class types, the null type, or the merge of two return addresses */
		return mergedType;
	}
	
	
	
	/**
	 * Makes note that the given cell is consumed by the instruction at the given index.
	 */
	protected void consumeCell(BT_StackCell cell, int instructionIndex) {}

	/**
	 * Returns the type for the given type that has been loaded from a local variable
	 * by the instruction at the given instruction index.  It is called every time a local
	 * variable is read.
	 * @param loadedType
	 * @param instructionIndex
	 * @return
	 */
	protected BT_StackType loadCell(BT_LocalCell loadedType, int instructionIndex) {
		return loadedType.getCellType();
	}
	
}
