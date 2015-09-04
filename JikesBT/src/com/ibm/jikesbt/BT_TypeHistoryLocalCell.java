/*
 * Created on Jul 23, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.util.ArrayList;
import java.util.List;

import com.ibm.jikesbt.BT_StackType.ClassType;

public class BT_TypeHistoryLocalCell implements BT_LocalCell {

	private BT_StackType stackType;
	private ArrayList insHistory; /* contains items of type LocalStore */

	public BT_TypeHistoryLocalCell(BT_StackType type, BT_StoreLocalIns ins, int instructionIndex) {
		this.stackType = type;
		ArrayList history = new ArrayList(2);
		this.insHistory = history;
		history.add(new LocalStore(ins, instructionIndex));
	}
	
	public BT_TypeHistoryLocalCell(BT_StackType type, BT_IIncIns ins, int instructionIndex) {
		this.stackType = type;
		ArrayList history = new ArrayList(2);
		this.insHistory = history;
		history.add(new LocalStore(ins, instructionIndex));
	}
	
	public BT_TypeHistoryLocalCell(BT_StackType type, BT_LocalCell type1, BT_LocalCell type2) {
		this.stackType = type;
		ArrayList history = new ArrayList(2);
		this.insHistory = history;
		addHistory(type1);
		addHistory(type2);
	}
	
	public BT_StackType getCellType() {
		return stackType;
	}
	
	public ClassType getClassType() {
		return (ClassType) stackType;
	}
	
	static boolean containsList(List list1, List list2) {
		return list1.containsAll(list2);
	}
	
	static boolean isEquivalentList(List list1, List list2) {
		return list1.size() == list2.size() && list1.containsAll(list2);
	}
	
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof BT_TypeHistoryLocalCell) {
			BT_TypeHistoryLocalCell other = (BT_TypeHistoryLocalCell) o;
			return stackType.equals(other.stackType)
				&& isEquivalentList(insHistory, other.insHistory);
		}
		return false;
	}
	
	public boolean containsHistory(BT_TypeHistoryLocalCell other) {
		return containsList(insHistory, other.insHistory);
	}
	
	static class LocalStore {
		final BT_Ins storeInstruction;
		final int instructionIndex;
		
		LocalStore(BT_StoreLocalIns instruction, int index) {
			this.storeInstruction = instruction;
			this.instructionIndex = index;
		}
		
		LocalStore(BT_IIncIns instruction, int index) {
			this.storeInstruction = instruction;
			this.instructionIndex = index;
		}
		
		public boolean equals(Object o) {
			if(o instanceof LocalStore) {
				LocalStore other = (LocalStore) o;
				return other.instructionIndex == instructionIndex 
					&& other.storeInstruction == storeInstruction;
			}
			return false;
		}
	}
		
	/**
	 * returns the instruction indexes at which this class type was on the top of the stack.
	 * @return
	 */
	public LocalStore[] getHistory() {
		return (LocalStore[]) insHistory.toArray(new LocalStore[insHistory.size()]);
	}
	
	/**
	 * add the history of the given type to this type
	 *
	 */
	private void addHistory(BT_LocalCell other) {
		if(other instanceof BT_TypeHistoryLocalCell) {
			addHistory((BT_TypeHistoryLocalCell) other);
		}
	}
	
	/**
	 * add the history of the given type to this type.
	 * Two types have been merged, and we need to remember the history.
	 *
	 */
	private void addHistory(BT_TypeHistoryLocalCell other) {
		ArrayList otherHistory = other.insHistory;
		for(int i=0; i<otherHistory.size(); i++) {
			Object o = otherHistory.get(i);
			int index = insHistory.indexOf(o);
			if(index < 0) {
				insHistory.add(o);
			} 
		}
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer(stackType.toString());
		buffer.append(' ');
		boolean first = true;
		for(int i=0; i<insHistory.size(); i++) {
			if(!first) {
				buffer.append(',');
			}
			first = false;
			buffer.append(insHistory.get(i));
		}
		return buffer.toString();
	}

}
