package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.util.HashMap;

/**
 BT_ClassTable is a {@link BT_ClassVector} that also has a fast
 find via hash table.
 Updates to the vector are overridden so that the hash table can be kept
 up to date.
 Duplicate class names are not allowed in the vector.
 
 Do not use this class to create a table of a set of classes whose names may change without 
 corresponding calls to renameClass in the table.
 * @author IBM
**/
public class BT_ClassTable extends BT_ClassVector {
	public void insertElementAt(BT_Class object, int location) {
		if (BT_Base.CHECK_USER && hash.containsKey(object.name))
			throw new IllegalArgumentException(Messages.getString("JikesBT.Duplicate_class_name_in_repository_1"));
		hash.put(object.name, object);
		super.insertElementAt(object, location);
	}
	
	public BT_Class findClass(String name) {
		if (CHECK_USER && name == null)
			expect(Messages.getString("JikesBT.name__null_2"));
		
		BT_Class res = (BT_Class) hash.get(name);
		if(res != null) {
			String actualName = res.name;
			
			//this test fails if I changed the name and then removed it from the table
			//and then changed the name back again...  but let's assume that never happens
			if(name.equals(actualName)) {
				return res;
			} else {
				hash.remove(name);
			}
		}
		
		//check if it is the vector somewhere (with a different key)
		res = super.findClass(name);
		if(res != null) {
			hash.put(name, res);
		}
		return res;
	}
	
	public void removeAllElements() {
		hash.clear();
		super.removeAllElements();
	}
	public boolean removeElement(BT_Class object) {
		hash.remove(object.name);
		return super.removeElement(object);
	}
	public BT_Class removeElementAt(int location) {
		if (location < elementCount)
			hash.remove(elementData[location].name);
		return super.removeElementAt(location);
	}
	public void setElementAt(BT_Class object, int location) {
		if (location < elementCount) {
			hash.remove(elementData[location].name);
			hash.put(object.name, object);
		}
		super.setElementAt(object, location);
	}
	public void setSize(int length) {
		for (int i = length; i < elementCount; i++)
			hash.remove(elementData[i].name);
		super.setSize(length);
	}

	private HashMap hash = new HashMap(10);

	public void renameClass(BT_Class object, String name) {
		hash.remove(object.name);
		hash.put(name, object);
	}
	
	public Object clone() {
		BT_ClassTable table = (BT_ClassTable) super.clone();
		table.hash = (HashMap) hash.clone();
		return table;
	}
	
}