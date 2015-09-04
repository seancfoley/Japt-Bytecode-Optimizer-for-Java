package com.ibm.jikesbt;

import java.util.HashSet;

/**
 * A BT_MethodVector with a hash enabling fast contains() and addUnique methods.
 * @author sfoley
 *
 */
public class BT_HashedMethodVector extends BT_MethodVector {

	private HashSet hash;
	
	public BT_HashedMethodVector() {}

	public BT_HashedMethodVector(int capacity) {
		super(capacity);
	}

	public BT_HashedMethodVector(int capacity, int capacityIncrement) {
		super(capacity, capacityIncrement);
	}
	
	public Object clone() {
		BT_HashedMethodVector vector = (BT_HashedMethodVector) super.clone();
		if(hash != null) {
			vector.hash = (HashSet) hash.clone();
		}
		return vector;
	}
	
	public boolean contains(BT_Method object) {
		return hash != null && hash.contains(object);
	}
	
	// A null class or a null class name should not be added.
	public void insertElementAt(BT_Method object, int location) {
		if(hash == null) {
			hash = new HashSet();
		} else if(hash.contains(object)) {
			throw new IllegalArgumentException(Messages.getString("JikesBT.Duplicate_entry_in_BT_HashedMethodVector__{0}_1", object));
		}
		super.insertElementAt(object, location);
		//if the object cannot be inserted then an ArrayIndexOutOfBoundsException is thrown
		//so we do not reach the following line
		hash.add(object);
	}
	
	// A null class or a null class name should not be added.
	public final boolean addUnique(BT_Method object) {
		if (contains(object)) {
			return false;
		}
		if(hash == null) {
			hash = new HashSet();
		}
		super.insertElementAt(object, elementCount);
		hash.add(object);
		return true;
	}
	
	public void addAll(BT_MethodVector other) {
		for(int i=0; i<other.size(); i++) {
			addElement(other.elementAt(i));
		}
	}
	
	public void removeAllElements() {
		if(hash != null) {
			super.removeAllElements();
			hash.clear();
		}
	}
	
	
	public boolean removeElement(BT_Method object) {
		if(!contains(object)) {
			return false;
		}
		if(super.removeElement(object)) {
			hash.remove(object);
			return true;
		}
		return false;
	}
	
	public void removeElementAt(int location) {
		if(hash != null) {
			hash.remove(elementAt(location));
			super.removeElementAt(location);
		}
	}

}
