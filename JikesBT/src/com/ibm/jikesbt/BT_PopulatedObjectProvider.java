/*
 * Created on Nov 3, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_PopulatedObjectCell.ParameterObject;
import com.ibm.jikesbt.BT_PopulatedObjectCell.ReceivedObject;
import com.ibm.jikesbt.BT_StackType.ClassType;
import com.ibm.jikesbt.BT_StackType.UninitializedObject;


public class BT_PopulatedObjectProvider extends BT_StackCellProvider {
	public final ParameterObject parameterObjects[];
	public final ReceivedObject stackObjects[];
	private BT_ObjectPool pool;
	BT_Method method;
	
	public BT_PopulatedObjectProvider(BT_Method method) {
		this(method, method.getCode());
	}
	
	public BT_PopulatedObjectProvider(BT_Method method, BT_ObjectPool pool) {
		this(method, method.getCode());
	}
	
	public BT_PopulatedObjectProvider(BT_Method method, BT_CodeAttribute code) {
		this(method, code, new BT_ObjectPool());
	}
	
	public BT_PopulatedObjectProvider(BT_Method method, BT_CodeAttribute code, BT_ObjectPool pool) {
		super(method.cls.getRepository());
		this.method = method;
		BT_MethodSignature signature = method.getSignature();
		int codeSize = code.getInstructionSize();
		stackObjects = new ReceivedObject[codeSize];
		parameterObjects = new ParameterObject[signature.types.size() + (method.isStatic() ? 0 : 1)];
		this.pool = pool;
	}
	
	/**
	 * Returns the class type for the object which is obtained as an argument to the method.
	 * The class represents an object type.
	 * @param clazz
	 * @param instruction the instruction that created the class type, or null if the class type is a parameter type
	 */
	protected BT_LocalCell getArgumentObjectCell(BT_Class clazz, int paramsIndex, int paramsCount) {
		ParameterObject object = pool.getParamObject(paramsIndex);
		parameterObjects[paramsIndex] = object;
		return new BT_PopulatedObjectCell(clazz.classType, object);
	}
	
	protected BT_StackCell getUninitializedObject(BT_NewIns newIns, int instructionIndex, int previousInstructionIndex) {
		ReceivedObject object = pool.getReceivedObject(instructionIndex);
		stackObjects[instructionIndex] = object;
		return new BT_PopulatedObjectCell(new UninitializedObject(newIns), object);
	}
	
	protected BT_LocalCell getUninitializedThisCell(BT_Class clazz, int paramsCount) {
		ParameterObject object = pool.getParamObject(0);
		parameterObjects[0] = object;
		return new BT_PopulatedObjectCell(BT_StackType.UNINITIALIZED_THIS, object);
	}
	
	//used by load from array
	protected BT_StackCell getObjectArrayElementClassCell(ClassType clazzType, int instructionIndex, int previousInstructionIndex) {
		ReceivedObject object = pool.getReceivedObject(instructionIndex);
		stackObjects[instructionIndex] = object;
		return new BT_PopulatedObjectCell(clazzType, object);
	}
	
	//used by the 3 new array instructions, get from field, and return from invoke
	protected BT_StackCell getObjectClassCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex) {
		ReceivedObject object = pool.getReceivedObject(instructionIndex);
		stackObjects[instructionIndex] = object;
		return new BT_PopulatedObjectCell(clazz.classType, object);
	}
	
	protected BT_StackCell getCastedClassCell(BT_Class clazz, BT_StackCell cell, int instructionIndex, int previousInstructionIndex) {
		if(cell.getClassType().isNonNullObjectType()) {
			return new BT_PopulatedObjectCell(clazz.classType, ((BT_PopulatedObjectCell) cell).objects);
		}
		return new BT_PopulatedObjectCell(clazz.classType);
	} 

	protected BT_StackCell getInitializedClassCell(BT_StackType uninitializedType, BT_Class clazz, BT_StackCell cell, boolean isStackTop, int instructionIndex, int previousInstructionIndex) {
		return new BT_PopulatedObjectCell(clazz.classType, ((BT_PopulatedObjectCell) cell).objects);
	}
	
	protected BT_LocalCell getInitializedClassLocalCell(BT_StackType uninitializedType, BT_Class clazz, BT_LocalCell cell, int instructionIndex, int previousInstructionIndex) {
		return new BT_PopulatedObjectCell(clazz.classType, ((BT_PopulatedObjectCell) cell).objects);
	}
	
	protected BT_StackCell getConstantObjectCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex) {
		ReceivedObject object = pool.getReceivedObject(instructionIndex);
		stackObjects[instructionIndex] = object;
		return new BT_PopulatedObjectCell(clazz.classType, object);
	}
	
	protected BT_LocalCell getLocalCell(BT_StackCell stackCell, BT_StoreLocalIns storeIns, int instructionIndex) {
		BT_StackType type = stackCell.getCellType();
		if(type.isNonNullObjectType() || type.isUninitializedObject()) {
			return (BT_PopulatedObjectCell) stackCell;
		}
		return super.getLocalCell(stackCell, storeIns, instructionIndex);
	}
	
	protected BT_StackCell getLoadedReferenceCell(BT_LocalCell cell, int instructionIndex, int previousInstructionIndex) {
		BT_StackType type = cell.getCellType();
		if(type.isNonNullObjectType() || type.isUninitializedObject()) {
			return (BT_PopulatedObjectCell) cell;
		}
		return super.getLoadedReferenceCell(cell, instructionIndex, previousInstructionIndex);
	}

	protected BT_StackCell getExceptionCell(BT_Class clazz, int instructionIndex, int previousInstructionIndex, BT_ExceptionTableEntry handler) {
		ReceivedObject object = pool.getReceivedObject(instructionIndex);
		stackObjects[instructionIndex] = object;
		return new BT_PopulatedObjectCell(clazz.classType, object);
	}
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 */
	protected BT_StackCell getMergedStackCell(BT_StackCell merged, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		BT_StackType mergedType = merged.getCellType();
		if((mergedType.isNonNullObjectType() || mergedType.isUninitializedObject()) && cell2 instanceof BT_PopulatedObjectCell) {
			BT_PopulatedObjectCell mergedCell = (BT_PopulatedObjectCell) merged;
			BT_PopulatedObjectCell prev = (BT_PopulatedObjectCell) cell2;
			if(!BT_PopulatedObjectCell.containsSet(mergedCell.objects, prev.objects)) {
				return new BT_PopulatedObjectCell(merged.getClassType(), merged, cell2);
			}
		}
		return super.getMergedStackCell(merged, cell2, isStackTop, instructionIndex);
	}
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 */
	protected BT_StackCell getMergedStackCell(BT_StackType merged, BT_StackCell cell1, BT_StackCell cell2, boolean isStackTop, int instructionIndex) {
		if(merged.isReturnAddress()) {
			return super.getMergedStackCell(merged, cell1, cell2, isStackTop, instructionIndex);
		}
		return new BT_PopulatedObjectCell(merged.getClassType(), cell1, cell2);
	}
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 */
	protected BT_LocalCell getMergedLocalCell(BT_LocalCell merged, BT_LocalCell cell2, int instructionIndex) {
		BT_StackType mergedType = merged.getCellType();
		if((mergedType.isNonNullObjectType() || mergedType.isUninitializedObject()) && cell2 instanceof BT_PopulatedObjectCell) {
			BT_PopulatedObjectCell mergedCell = (BT_PopulatedObjectCell) merged;
			BT_PopulatedObjectCell prev = (BT_PopulatedObjectCell) cell2;
			if(!BT_PopulatedObjectCell.containsSet(mergedCell.objects, prev.objects)) {
				return new BT_PopulatedObjectCell(merged.getClassType(), merged, cell2);
			}
		}
		return super.getMergedLocalCell(merged, cell2, instructionIndex);
	}
	
	/**
	 * Returns the cell which is the result of a merge of two local cells.
	 */
	protected BT_LocalCell getMergedLocalCell(BT_StackType merged, BT_LocalCell cell1, BT_LocalCell cell2, int instructionIndex) {
		if(merged.isReturnAddress()) {
			return super.getMergedLocalCell(merged, cell1, cell2, instructionIndex);
		}
		return new BT_PopulatedObjectCell(merged.getClassType(), cell1, cell2);
	}
}
