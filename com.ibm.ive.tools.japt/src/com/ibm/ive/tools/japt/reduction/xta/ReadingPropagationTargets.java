package com.ibm.ive.tools.japt.reduction.xta;

import java.util.*;

import com.ibm.jikesbt.*;


/**
 * @author sfoley
 *
 */
public class ReadingPropagationTargets {

	/**
	 *  contains objects of type Method which read from this field 
	 */
	private ArrayList readingMethods;
	
	
	
	public String getState() {
		return "Reading methods: " + ((readingMethods == null) ? "0" : readingMethods.size() + ":\n" + MethodPropagationTargets.getListMembers(readingMethods))
			;
	}
	
	void propagate(BT_Class objectType) {
		if(readingMethods == null) {
			return;
		}
		int size = readingMethods.size();
		for(int i=0; i<size; i++) {
			Method method = (Method) readingMethods.get(i);
			if(!method.hasPropagated(objectType)) {
				method.addPropagatedObject(objectType);
			}
		}
	}
	
	boolean isEmpty() {
		return readingMethods == null || readingMethods.size() == 0;
	}
	
	boolean containsAccessor(MethodPropagator accessor) {
		return readingMethods != null && readingMethods.contains(accessor);
	}
	
	void addAccessor(MethodPropagator accessor) {
		if(readingMethods == null) {
			readingMethods = new ArrayList(1);
		}
		readingMethods.add(accessor);
	}
	
	void migrate(ReadingPropagationTargets to) {
		if(to.readingMethods == null) {
			to.readingMethods = readingMethods;
			readingMethods = null;
		}
		else {
			to.readingMethods.addAll(readingMethods);
			//readingMethods.clear();
			readingMethods = null;
		}
	}
}
