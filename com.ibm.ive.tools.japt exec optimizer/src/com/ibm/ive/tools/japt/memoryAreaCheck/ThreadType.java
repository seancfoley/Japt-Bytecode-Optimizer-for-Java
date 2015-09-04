package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.jikesbt.BT_Class;


public abstract class ThreadType implements Comparable {
	
	static int counter;
	int count;
	
	public ThreadType() {
		count = counter++;
	}
	
	public boolean equals(Object o) {
		if(o instanceof ThreadType) {
			return isSame((ThreadType) o);
		}
		return false;
	}
	
	public int hashCode() {
		return count;
	}
	
	public int compareTo(Object obj) {
		ThreadType other = (ThreadType) obj;
		return count - other.count;
	}
	
	public boolean isSame(ThreadType other) {
		return this == other;
	}
	
	public abstract boolean isRealTime();
	
	public abstract boolean isNoHeapRealTime();
	
	public abstract BT_Class canReference(AllocationContext context);
}
