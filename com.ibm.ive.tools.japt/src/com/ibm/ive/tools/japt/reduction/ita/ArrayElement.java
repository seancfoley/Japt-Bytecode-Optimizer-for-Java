package com.ibm.ive.tools.japt.reduction.ita;


/**
 * @author sfoley
 * 
 * Represents an array element.
 * 
 * As an object propagator, it propagates objects to method invocations.
 * It propagates to methods that read from this array element, or in other words,
 * read an element from the array that owns this array element.
 *
 */
public class ArrayElement extends DataMember {

	/**
	 * The type of this array element
	 */
	final Clazz arrayElementType;
	final PropagatedObject containingObject;
	
	
	/**
	 * Constructor for ArrayElement.
	 * @param repository
	 * @param clazz
	 */
	ArrayElement(Clazz arrayElementType, PropagatedObject containingObject) {
		this.arrayElementType = arrayElementType;
		this.containingObject = containingObject;
	}

	public PropagatedObject getContainingObject() {
		return containingObject;
	}
	
	public Clazz getDefiningClass() {
		return arrayElementType.getArrayClass();
	}
	
	public boolean isFieldInstance() {
		return false;
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#initializePropagation()
	 */
	void initializePropagation() {}

	
	public Clazz getDataType() {
		return arrayElementType;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "array element of type " 
			+ arrayElementType + ' ' + PropagatedObject.toIdentifier(this);
	}
	
	void setAccessed() {
		if(isAccessed()) {
			return;
		}
		super.setAccessed();
	}
	
	
}
