package com.ibm.ive.tools.japt.reduction.ita;


/**
 * @author sfoley
 *
 * This class represents an array object instance.  It propagates by delegating propagation to its elements.
 * However, since all elements represent the same type and are equally accessible (the way this algorithm is structured
 * the array indices are ignored when accessing the array), we treat all arrays as if they were arrays of a single element.
 */
public class ArrayObject extends PropagatedObject implements Array {

	/**
	 * This represents (simultaneously) all the elements of the array object, ie the element a[x] for an array 'a' and any index 'x'.
	 * We need only one since they all hold the same type, so what can be propagated to
	 * one can be propagated to all.
	 * 
	 * For purposes of propagation, we can think of it as a field.  Numerous objects may be propagated
	 * to it, and later on they may be removed from it and propagated elsewhere.
	 */
	private ArrayElement arrayElement;
	private final AllocationContext context;
	
	public ArrayObject(Clazz type, AllocationContext context) {
		super(type);
		this.context = context;
	}

	public String toString() {
		String res = super.toString();
		Repository rep = getRepository();
		if(rep.getAllocationContextCount() > 1) {
			res += " in " + context;
		}
		return res;
	}
	
	public AllocationContext getAllocationContext() {
		return context;
	}
	
	public boolean isArray() {
		return true;
	}
	
	public boolean isPrimitiveArray() {
		return type.getElementClass().isPrimitive();
	}
	
	public FieldInstance[] getFields() {
		return null;
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
		if(type.isSharedArrayElement) {
			return type.getSharedArrayElement();
		}
		if(arrayElement == null) {
			Clazz eClass = type.getElementClass();
			arrayElement = new ArrayElement(eClass, this);	
		}
		return arrayElement;
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
		if(arrayElement == null) {
			return;
		}
		type.repository.arrayElementCount++;
	}
	
	public FieldInstance getFieldInstance(Field field) {
		throw new UnsupportedOperationException();
	}
	
	boolean isRuntimeThrowable() {
		return false;
	}
	
	public boolean isThrowable() {
		return false;
	}
	
	public boolean isGeneric() {
		return false;
	}
	
	boolean isGenericInstanceOf(Clazz type) {
		return false;
	}
	
	boolean mightBeGenericInstanceOf(Clazz type) {
		return false;
	}
	
	public PropagatedObject clone(AllocationContext newContext) {
		ArrayObject cloned = new ArrayObject(type, newContext);
		return cloned;
	}
}
