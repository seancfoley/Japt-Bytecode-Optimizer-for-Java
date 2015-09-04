package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_HashedClassVector;

/**
 * @author sfoley
 *
 */
public class ArrayElement extends DataMember {

	/**
	 * The type of this array element
	 */
	BT_Class arrayElementType;
	
	/**
	 * If this array element is an array itself, then this is the ArrayElement that it can hold.
	 */
	ArrayElement subType;
	
	/**
	 * Constructor for ArrayElement.
	 * @param repository
	 * @param clazz
	 */
	protected ArrayElement(Clazz declaringClass, BT_Class arrayElementType) {
		super(declaringClass);
		this.arrayElementType = arrayElementType;
	}
	
	protected ArrayElement(Clazz declaringClass, BT_Class arrayElementType,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(declaringClass, propagatedObjects, allPropagatedObjects);
		this.arrayElementType = arrayElementType;
	}

	/**
	 * This is essentially equivalent to including the parent class
	 */
	void propagateFromUnknownSource() {
		getDeclaringClass().setRequired();
	}
	
	
	/**
	 * @return whether the array element can hold an object of the given type
	 */
	boolean holdsType(BT_Class type) {
		return getRepository().isCompatibleType(arrayElementType, type);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}
	
	public String getName() {
		return "array element of " + declaringClass.toString();
	}

}
