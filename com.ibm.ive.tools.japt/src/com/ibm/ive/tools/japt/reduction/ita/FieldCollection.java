package com.ibm.ive.tools.japt.reduction.ita;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

public class FieldCollection {
	/* 
	 * Because of the implementation of the Comparable interface in
	 * FieldInstance, the use of TreeMap here cannot include two different
	 * field instance for the same field (ie the same field in two separate objects)
	 */
	TreeMap instanceFields;
	
	public int size() {
		if(instanceFields == null) {
			return 0;
		}
		return instanceFields.size();
	}
	
	public FieldInstance getFieldInstance(Field field, boolean generic, PropagatedObject owningObject) {
		if(field.isStatic()) {
			throw new IllegalArgumentException();
		}
		FieldInstance instance;
		if(instanceFields == null) {
			instanceFields = new TreeMap();
			instance = createNewInstance(field, generic, owningObject);
		} else {
			instance = (FieldInstance) instanceFields.get(field);
			if(instance == null) {
				instance = createNewInstance(field, generic, owningObject);
			}
		}
		return instance;
	}

	private FieldInstance createNewInstance(Field field, boolean generic,
			PropagatedObject owningObject) {
		FieldInstance instance;
		instance = field.isShared() ? field.getDeclaringClass().getSharedFieldInstance(field) : 
						(generic ? field.getDeclaringClass().getGenericFieldInstance(field) : 
							new ObjectFieldInstance(field, owningObject));
		instanceFields.put(field, instance);
		return instance;
	}
	
	public ObjectSet getContainedObjects() {
		if(instanceFields == null) {
			return ObjectSet.EMPTY_SET;
		}
		Collection values = instanceFields.values();
		if(values.size() == 0) {
			return ObjectSet.EMPTY_SET;
		}
		Iterator iterator = values.iterator();
		FieldInstance fieldInstance = (FieldInstance) iterator.next();
		ObjectSet set = fieldInstance.getContainedObjects();
		if(values.size() == 1) {
			if(set == null) {
				return ObjectSet.EMPTY_SET;
			}
			return set;
		}
		ObjectSet result = new ObjectSet();
		if(set != null) {
			result.addAll(set);
		}
		while(iterator.hasNext()) {
			fieldInstance = (FieldInstance) iterator.next();
			set = fieldInstance.getContainedObjects();
			if(set != null) {
				result.addAll(set);
			}
		}
		if(result.size() == 0) {
			return ObjectSet.EMPTY_SET;
		}
		return result;
	}
}
