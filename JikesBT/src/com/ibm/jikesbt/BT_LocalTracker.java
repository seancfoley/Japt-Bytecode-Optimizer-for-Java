/*
 * Created on Nov 3, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_TypeHistoryLocalCell.LocalStore;

public class BT_LocalTracker extends BT_StackCellProvider {

	final BT_InsVector liveStores = new BT_InsVector();
	
	public BT_LocalTracker(BT_Repository repository) {
		super(repository);
	}
	
	public boolean isLiveStore(BT_StoreLocalIns storeIns) {
		return liveStores.contains(storeIns);
	}
	
	public boolean isLiveStore(BT_IIncIns iincIns) {
		return liveStores.contains(iincIns);
	}
	
	/**
	 * Returns the type for the given type that has been loaded from a local variable
	 * by the instruction at the given instruction index.
	 * @param loadedType
	 * @param instructionIndex
	 * @return
	 */
	protected BT_StackType loadCell(BT_LocalCell loadedType, int instructionIndex) {
		if(loadedType instanceof BT_TypeHistoryLocalCell) {
			BT_TypeHistoryLocalCell stored = (BT_TypeHistoryLocalCell) loadedType;
			LocalStore[] history = stored.getHistory();
			for(int i=0; i<history.length; i++) {
				LocalStore hist = history[i];
				liveStores.addElement(hist.storeInstruction);
			}
		}
		return loadedType.getCellType();
	}
	
	
	/**
	 * Returns the type for the object which is the result of a merge of two local types.
	 * @param mergedType both the merged type and one of the original two types being merged
	 * @param type2 the other merged type
	 */
	protected BT_LocalCell getMergedLocalCell(BT_LocalCell mergedType, BT_LocalCell type2, int instructionIndex) {
		if(type2 instanceof BT_TypeHistoryLocalCell) {
			BT_TypeHistoryLocalCell storedType = (BT_TypeHistoryLocalCell) type2;
			
			//check to ensure that the merged type also has the same history (ie propagate the history),
			//if not we create a new merged type (we must not alter the existing one to ensure changes are propagated by the visitor
			if(mergedType instanceof BT_TypeHistoryLocalCell) {
				BT_TypeHistoryLocalCell mergedStoredType = (BT_TypeHistoryLocalCell) mergedType;
				if(!mergedStoredType.containsHistory(storedType)) {
					return new BT_TypeHistoryLocalCell(mergedType.getCellType(), mergedType, type2);
				}
			} else {
				return new BT_TypeHistoryLocalCell(mergedType.getCellType(), mergedType, type2);
			}
		}
		
		return mergedType;
	}
	
	/**
	 * Returns the type for the object which is the result of a merge of two local types.
	 */
	protected BT_LocalCell getMergedLocalCell(BT_StackType mergedType, BT_LocalCell type1, BT_LocalCell type2, int instructionIndex) {
		if(type1 instanceof BT_TypeHistoryLocalCell || type2 instanceof BT_TypeHistoryLocalCell) {
			return new BT_TypeHistoryLocalCell(mergedType, type1, type2);
		}
		return mergedType;
	}
	
	
	
	/**
	 * Returns the type for the given type that has been stored into a local variable.
	 * by the instruction at the given instruction index.
	 * @param storedType
	 * @param instructionIndex
	 * @return
	 */
	protected BT_LocalCell getLocalCell(BT_LocalCell localCell, BT_IIncIns iincIns, int instructionIndex) {
		return new BT_TypeHistoryLocalCell(localCell.getCellType(), iincIns, instructionIndex);
	}
	
	/**
	 * Returns the type for the given type that has been stored into a local variable.
	 * by the instruction at the given instruction index.
	 * @param storedType
	 * @param instructionIndex
	 * @return
	 */
	protected BT_LocalCell getLocalCell(BT_StackCell stackCell, BT_StoreLocalIns storeIns, int instructionIndex) {
		return new BT_TypeHistoryLocalCell(stackCell.getCellType(), storeIns, instructionIndex);
	}
}
