package com.ibm.ive.tools.japt.reduction.ita;



/**
 * 
 * @author sfoley
 *
 */
public class GenericArrayObject extends GenericObject {
	
	private ArrayElement arrayElement;
	
	GenericArrayObject(Clazz baseType, AllocationContext context) {
		super(baseType, context);
	}
	
	public boolean isRuntimeThrowable() {
		return false;
	}
	
	public boolean isThrowable() {
		return false;
	}
	
	public ObjectSet getContainedObjects() {
		if(arrayElement == null) {
			return ObjectSet.EMPTY_SET;
		}
		ObjectSet set = arrayElement.getContainedObjects();
		if(set == null) {
			return ObjectSet.EMPTY_SET;
		}
		return set;
	}
	
	public ArrayElement getArrayElement() {
		if(arrayElement == null) {
			arrayElement = type.getGenericArrayElement(this);
		}
		return arrayElement;
	}
	
	public boolean isArray() {
		return true;
	}
	
	public boolean doPropagation() throws PropagationException {
		if(arrayElement == null) {
			return false;
		}
		if(arrayElement.doPropagation()) {
			type.repository.arrayElementCount++;
			return true;
		}
		return false;
	}
	
	public void doCount() {
		if(arrayElement != null) {
			type.repository.arrayElementCount++;
		}
	}
	
	public PropagatedObject clone(AllocationContext newContext) {
		GenericArrayObject cloned = new GenericArrayObject(type, newContext);
		return cloned;
	}
}
