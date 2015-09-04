package com.ibm.jikesbt;

import java.util.HashSet;

/**
 * This BT_ClassVector contains a hash that allows for a fast contains() operation.
 * This vector cannot contain duplicate classes.  An IllegalArgumentException is thrown when
 * attempting to add a duplicate class.
 * 
 * @author sfoley
 *
 */
public class BT_HashedClassVector extends BT_ClassVector {

	private HashSet hash;
	
	public BT_HashedClassVector() {
		super();
	}

	public BT_HashedClassVector(int capacity) {
		super(capacity);
	}

	public BT_HashedClassVector(int capacity, int capacityIncrement) {
		super(capacity, capacityIncrement);
	}
	
	public Object clone() {
		BT_HashedClassVector vector = (BT_HashedClassVector) super.clone();
		if(hash != null) {
			vector.hash = (HashSet) hash.clone();
		}
		return vector;
	}
	
	public boolean contains(BT_Class object) {
		return hash != null && hash.contains(object);
	}
	
	// A null class or a null class name should not be added.
	public void insertElementAt(BT_Class object, int location) {
		if(hash == null) {
			hash= new HashSet();
		} else if(hash.contains(object)) {
			throw new IllegalArgumentException(Messages.getString("JikesBT.Duplicate_entry_in_BT_HashedClassVector__{0}_1", object));
		}
		super.insertElementAt(object, location);
		//if the object cannot be inserted then an ArrayIndexOutOfBoundsException is thrown
		//so we do not reach the following line
		hash.add(object);
	}
	
	// A null class or a null class name should not be added.
	public final boolean addUnique(BT_Class object) {
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
	
	public void addAll(BT_ClassVector other) {
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
	
	
	public boolean removeElement(BT_Class object) {
		if(!contains(object)) {
			return false;
		}
		if(super.removeElement(object)) {
			hash.remove(object);
			return true;
		}
		return false;
	}
	
	public BT_Class removeElementAt(int location) {
		if(hash != null) {
			hash.remove(elementAt(location));
			return super.removeElementAt(location);
		}
		return null;
	}
}
