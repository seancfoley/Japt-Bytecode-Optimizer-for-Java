package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.util.HashMap;

/**
 BT_RepositoryClasses is a {@link BT_ClassVector} that also has a fast
 find via hash table.
 Updates to the vector are overridden so that the hash table can be kept
 up to date.
 Duplicate class names are not allowed in the vector.
 * @author IBM
**/
public class BT_FieldTable extends BT_FieldVector {
	private HashMap hash = new HashMap(10);

	public void insertElementAt(BT_Field object, int location) {
		if (BT_Base.CHECK_USER && hash.containsKey(getKey(object)))
			throw new IllegalArgumentException(Messages.getString("JikesBT.Duplicate_field_in_class_1"));
		hash.put(getKey(object), object);
		super.insertElementAt(object, location);
	}
	
	public BT_Field findField(
			String fieldName,
			String type,
			String key) {
		BT_Field res = (BT_Field) hash.get(key);
		if(res != null) {
			String actualKey = getKey(res);
			
			//this test fails if I changed the name and then removed it from the table
			//and then changed the name back again...  but let's assume that never happens
			if(key.equals(actualKey)) {
				return res;
			} else {
				hash.remove(key);
			}
		}
		//check if it is the vector somewhere (with a different key)
		res = super.findField(fieldName, type);
		if(res != null) {
			hash.put(getKey(res), res);
		}
		return res;
	}
	
	public BT_Field findField(
		String fieldName,
		String type) {
		return findField(fieldName, type, getKey(fieldName, type));
	}
	
	public void removeAllElements() {
		hash.clear();
		super.removeAllElements();
	}
	
	public boolean removeElement(BT_Field object) {
		hash.remove(getKey(object));
		return super.removeElement(object);
	}
	
	public void removeElementAt(int location) {
		if (location < elementCount)
			hash.remove(getKey(elementData[location]));
		super.removeElementAt(location);
	}
	
	public void setElementAt(BT_Field object, int location) {
		if (location < elementCount) {
			hash.remove(getKey(elementData[location]));
			hash.put(getKey(object), object);
		}
		super.setElementAt(object, location);
	}
	
	public void setSize(int length) {
		for (int i = length; i < elementCount; i++)
			hash.remove(getKey(elementData[i]));
		super.setSize(length);
	}
	
	String getKey(BT_Field field) {
		return getKey(field.name, field.getTypeName());
	}
	
	String getKey(String fieldName, String typeName) {
		return fieldName + ' ' + typeName;
	}
	
	public Object clone() {
		BT_FieldTable table = (BT_FieldTable) super.clone();
		table.hash = (HashMap) hash.clone();
		return table;
	}
}