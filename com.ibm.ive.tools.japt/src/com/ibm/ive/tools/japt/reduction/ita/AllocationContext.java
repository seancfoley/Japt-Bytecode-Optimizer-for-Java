package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_Class;

public abstract class AllocationContext implements Comparable {
	public int count;
	
	public AllocationContext(int count) {
		this.count = count;
	}
	
	/**
	 * @param other
	 * @return the resulting error if the reference is illegal
	 */
	public BT_Class canReference(AllocationContext other) {
		return other.canBeReferencedBy(this);
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof AllocationContext) {
			return isSame((AllocationContext) obj);
		}
		return false;
	}
	
	public int compareTo(Object obj) {
		AllocationContext other = (AllocationContext) obj;
		return count - other.count;
	}
	
	public boolean isSame(AllocationContext other) {
		return this == other;
	}
	
	/**
	 * @param other
	 * @return the resulting error if the reference is illegal
	 */
	public abstract BT_Class canBeReferencedBy(AllocationContext other);
}
