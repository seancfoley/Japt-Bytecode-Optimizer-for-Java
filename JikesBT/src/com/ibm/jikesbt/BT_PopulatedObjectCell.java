package com.ibm.jikesbt;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.jikesbt.BT_StackType.ClassType;
import com.ibm.jikesbt.BT_StackType.UninitializedObject;
import com.ibm.jikesbt.BT_StackType.UninitializedThis;

public class BT_PopulatedObjectCell implements BT_StackCell, BT_LocalCell {

	private BT_StackType classType;//either a ClassType, an UninitializedThis, or UninitializedObject
	public final Set objects;
	
	public boolean contains(AcquiredObject obj) {
		return objects.contains(obj);
	}
	
	public static abstract class AcquiredObject implements Comparable {
		int identifier;
		
		AcquiredObject(int identifier) {
			this.identifier = identifier;
		}
		
		public int compareTo(Object object) {
			return identifier - ((AcquiredObject) object).identifier;
		}
		
		public boolean equals(Object object) {
			if(object instanceof AcquiredObject) {
				return identifier == ((AcquiredObject) object).identifier;
			}
			return false;
		}
	}
	
	static class ParameterObject extends AcquiredObject {
		
		ParameterObject(int paramIndex) {
			super(-(paramIndex + 1));
		}
		
		public String toString() {
			return "p" + -(identifier + 1);
		}
	}
	
	static class ReceivedObject extends AcquiredObject {
		ReceivedObject(int instructionIndex) {
			super(instructionIndex);
		}
		
		public String toString() {
			return "i" + identifier;
		}
	}
	
	public BT_PopulatedObjectCell(BT_StackType ct) {
		this(ct, new TreeSet());
	}
	
	public BT_PopulatedObjectCell(BT_StackType ct, Set objects) {
		this.classType = ct;
		this.objects = objects;
	}
	
	public BT_PopulatedObjectCell(UninitializedThis uninit, ParameterObject introducedObject) {
		this((BT_StackType) uninit, introducedObject);
	}
	
	public BT_PopulatedObjectCell(UninitializedObject uninit, ReceivedObject introducedObject) {
		this((BT_StackType) uninit, introducedObject);
	}
	
	public BT_PopulatedObjectCell(ClassType ct, AcquiredObject introducedObject) {
		this((BT_StackType) ct, introducedObject);
	}
	
	private BT_PopulatedObjectCell(BT_StackType ct, AcquiredObject introducedObject) {
		this.classType = ct;
		this.objects = new TreeSet();
		objects.add(introducedObject);
	}
	
	public BT_PopulatedObjectCell(ClassType merged, BT_FrameCell type1, BT_FrameCell type2) {
		this.classType = merged;
		this.objects = new TreeSet();
		if(type1 instanceof BT_PopulatedObjectCell) {
			BT_PopulatedObjectCell cell = (BT_PopulatedObjectCell) type1;
			objects.addAll(cell.objects);
		}
		if(type2 instanceof BT_PopulatedObjectCell) {
			BT_PopulatedObjectCell cell = (BT_PopulatedObjectCell) type2;
			objects.addAll(cell.objects);
		}
	}
	
	public BT_StackType getCellType() {
		return classType;
	}

	public ClassType getClassType() {
		return classType.getClassType();
	}
	
	static boolean containsSet(Set one, Set two) {
		return one.containsAll(two);
	}
	
	static boolean isEquivalentSet(Set one, Set two) {
		return one.size() == two.size() && one.containsAll(two);
	}
	
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof BT_PopulatedObjectCell) {
			BT_PopulatedObjectCell other = (BT_PopulatedObjectCell) o;
			return classType.isSameType(other.classType) && isEquivalentSet(objects, other.objects);
		} 
		return false;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer(classType.toString());
		buffer.append(" {");
		boolean first = true;
		Iterator iterator = objects.iterator();
		while(iterator.hasNext()) {
			if(!first) {
				buffer.append(',');
			} else {
				first = false;
			}
			buffer.append(iterator.next());
		}
		buffer.append('}');
		return buffer.toString();
	}
}
