/*
 * Created on Jul 24, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.ibm.jikesbt.BT_StackType.ClassType;

public class BT_TypeHistoryStackCell implements BT_StackCell {
	
	/**
	 * This special cell represents a non-primitive, non-array class type that actually remembers when it was on the
	 * top of the stack.  A given stack location might actually have been on the top of the stack in more than one location
	 * if the type is the result of the merging of two previous types. 
	 * 
	 * This allows us to maintain an accurate history.  When we analyze a stack at any given instruction, we
	 * can use these objects to determine at which instructions each type was on top of the stack.
	 * 
	 * When types on the stack are duplicated by a dup instruction, new BT_TypeHistoryStackCell objects are created.
	 */

	
//	TODO as an alternative to merging histories, could have a list of BT_TypeHistoryStackCell that were merged to produce this item
	//and so that would allow us to associate duplications with instruction histories
	//but this is something to do after refactoring BT_TypeHistoryStackCell
	
	private final ArrayList insHistory; /* contains items of type History */
	private final TreeSet consumedSet = new TreeSet(); /* contains items of type Integer */
	
	private final ArrayList duplications; /* contains items of type Duplication */
	public static final Duplication[] emptyDuplications = new Duplication[0];
	
	private final ClassType classType;
	
	public BT_StackType getCellType() {
		return classType;
	}
	
	public ClassType getClassType() {
		return classType;
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * represents an instruction index at which this class type was on the top of
	 * the stack.
	 * 
	 * Each History may have been duplicated by a dup instruction one or more times, and so we also record
	 * these instances.
	 */
	public static class History {
		final public int index;
		
		History(int index) {
			this.index = index;
		}
		
		public boolean equals(Object o) {
			if(this == o) {
				return true;
			}
			if(!(o instanceof History)) {
				return false;
			}
			History other = (History) o;
			return index == other.index;
		}
		
		public String toString() {
			return "Created at instruction index " + Integer.toString(index);
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 */
	public static class Duplication {
		public final int instructionIndex;
		/* 
		 * the index from the top of stack where the duplicated type is placed, either 0 or 1.
		 * Contrary to the VM spec, the original is considered to tbe the one placed or inserted 
		 * further down the stack, while the duplicate is considered to be the one at the top
		 * of the stack or just below.
		 * 
		 * For a given duplicate, the stackIndexFromTop never changes, however
		 * rememember that certain instructions can create two duplicates, each with a different stackIndexFromTop.
		 */
		public final int stackIndexFromTop;
		
		Duplication(int stackIndexFromTop, int dupInstructionIndex) {
			if(stackIndexFromTop < 0 || stackIndexFromTop > 1) {
				throw new IllegalArgumentException();
			}
			this.instructionIndex = dupInstructionIndex;
			//this.stackType = stackType;
			this.stackIndexFromTop = stackIndexFromTop;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Duplication) {
				Duplication otherDup = (Duplication) o;
				return otherDup.instructionIndex == instructionIndex 
					&& otherDup.stackIndexFromTop == stackIndexFromTop;
			}
			return false;
		}
		
		public String toString() {
			return "duplication at instruction index " + instructionIndex 
				+ " at item " + stackIndexFromTop + " from top of stack ";
		}
	}
	/**
	 * construct a stack type for a type which was put on the top of the stack by the instruction at the given index.
	 * Other than the history, the stack type is a duplicate of the given class type ct.
	 * @param ct
	 * @param index
	 */
	public BT_TypeHistoryStackCell(ClassType ct, int index) {
		this.classType = ct;
		insHistory = new ArrayList(1);
		insHistory.add(new History(index));
		duplications = new ArrayList(0);
	}
	
	/**
	 * construct a stack type which is the result of a merge of two types.
	 * @param ct
	 * @param index
	 */
	public BT_TypeHistoryStackCell(ClassType merged, BT_StackCell type1, BT_StackCell type2) {
		this.classType = merged;
		if(type1 instanceof BT_TypeHistoryStackCell) {
			BT_TypeHistoryStackCell hist1 = (BT_TypeHistoryStackCell) type1;
			if(type2 instanceof BT_TypeHistoryStackCell) {
				BT_TypeHistoryStackCell hist2 = (BT_TypeHistoryStackCell) type2;
				insHistory = merge(hist1.insHistory, hist2.insHistory);
				duplications = merge(hist1.duplications, hist2.duplications);
			} else {
				insHistory = (ArrayList) hist1.insHistory.clone();
				duplications = (ArrayList) hist1.duplications.clone();
			}
			
		} else if(type2 instanceof BT_TypeHistoryStackCell) {
			BT_TypeHistoryStackCell hist2 = (BT_TypeHistoryStackCell) type2;
			insHistory = (ArrayList) hist2.insHistory.clone();
			duplications = (ArrayList) hist2.duplications.clone();
		} else {
			insHistory = new ArrayList(0);
			duplications = new ArrayList(0);
		}
	}

	public boolean isConsumedAtMultiplePlaces() {
		return consumedSet.size() > 1;
	}
	
	/**
	 * Note that this type is consumed by the instruction at the given index.
	 *
	 */
	void setConsumed(int instructionIndex) {
		consumedSet.add(new Integer(instructionIndex));
	}
	
	void addDuplicate(Duplication dup) {
		if(duplications.contains(dup)) {
			return;
		}
		duplications.add(dup);
	}
	
	public void addDuplicate(int duplicateStackIndex, int duplicateInstructionIndex) {
		addDuplicate(new Duplication(duplicateStackIndex, duplicateInstructionIndex));
	}
	
	/**
	 * returns the instruction indexes at which this class type was on the top of the stack.
	 * @return
	 */
	public History[] getHistory() {
		return (History[]) insHistory.toArray(new History[insHistory.size()]);
	}
	
	public int getHistorySize() {
		return insHistory.size();
	}
	
	public Duplication[] getDuplications() {
		if(duplications == null || duplications.size() == 0) {
			return emptyDuplications;
		}
		return (Duplication[]) duplications.toArray(new Duplication[duplications.size()]);
	}
			
	private boolean hasEquivalentHistory(BT_TypeHistoryStackCell other) {
		return isEquivalentList(insHistory, other.insHistory) && isEquivalentList(duplications, other.duplications);
	}
	
	public boolean containsHistory(BT_TypeHistoryStackCell other) {
			return containsList(insHistory, other.insHistory) && containsList(duplications, other.duplications);
		}
	
	private static ArrayList merge(ArrayList list1, ArrayList list2) {
		ArrayList result = new ArrayList(list1.size() + list2.size());
		result.addAll(list1);
		for(int i=0; i<list2.size(); i++) {
			Object o = list2.get(i);
			if(!result.contains(o)) {
				result.add(o);
			}
		}
		return result;
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
		if(o instanceof BT_TypeHistoryStackCell) {
			BT_TypeHistoryStackCell other = (BT_TypeHistoryStackCell) o;
			return classType.isSameClassType(other.classType) && hasEquivalentHistory(other);
		} 
		return false;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer(super.toString());
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
