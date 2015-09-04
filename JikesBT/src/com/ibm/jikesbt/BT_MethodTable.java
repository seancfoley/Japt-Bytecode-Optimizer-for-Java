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
public class BT_MethodTable extends BT_MethodVector {
	private HashMap hash = new HashMap(10);

	public void insertElementAt(BT_Method object, int location) {
		if (BT_Base.CHECK_USER && hash.containsKey(getKey(object)))
			throw new IllegalArgumentException(Messages.getString("JikesBT.Duplicate_method_{0}_in_class_{1}_1", 
				new Object[] {object.getName(), object.getDeclaringClass().getName()}));
		hash.put(getKey(object), object);
		super.insertElementAt(object, location);
	}
	
	public void addAll(BT_MethodVector other) {
		for(int i=0; i<other.size(); i++) {
			addElement(other.elementAt(i));
		}
	}
	
	public BT_Method findMethod(String name,
		String sig,
		String key) {
		BT_Method res = (BT_Method) hash.get(key);
		if(res != null) {
			//this test fails if I changed the name and then removed it from the table
			//and then changed the name back again...  but let's assume that never happens
			
			String actualKey = getKey(res);
			if(key.equals(actualKey)) {
				return res;
			} else {
				hash.remove(key);
			}
		}
		//check if it is the vector somewhere (with a different key)
		res = super.findMethod(name, sig);
		if(res != null) {
			hash.put(getKey(res), res);
		}
		return res;
	}

	public BT_Method findMethod(
		String name,
		String sig) {
		return findMethod(name, sig, getKey(name, sig));
	}
	
	public void removeAllElements() {
		hash.clear();
		super.removeAllElements();
	}
	
	public boolean removeElement(BT_Method object) {
		hash.remove(getKey(object));
		return super.removeElement(object);
	}
	
	public void removeElementAt(int location) {
		if (location < elementCount)
			hash.remove(getKey(elementData[location]));
		super.removeElementAt(location);
	}
	
	public void setElementAt(BT_Method object, int location) {
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
	
	String getKey(BT_Method method) {
		return getKey(method.name, method.getDescriptor());
	}
	
	String getKey(
		String methodName,
		String sig) {
		return methodName + ' ' + sig;
	}
	
	public Object clone() {
		BT_MethodTable table = (BT_MethodTable) super.clone();
		table.hash = (HashMap) hash.clone();
		return table;
	}

}
