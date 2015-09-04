package com.ibm.ive.tools.japt.reduction.rta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.ive.tools.japt.reduction.xta.ArrayElement;
import com.ibm.ive.tools.japt.reduction.xta.Clazz;
import com.ibm.ive.tools.japt.reduction.xta.MethodPropagator;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_HashedClassVector;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RTAArrayElement extends ArrayElement {

	/**
	 * Constructor for RTAArrayElement.
	 * @param repository
	 * @param declaringClass
	 * @param arrayElementType
	 */
	public RTAArrayElement(
		Clazz declaringClass,
		BT_Class arrayElementType, 
		BT_HashedClassVector propagatedObjects, 
		ClassSet allPropagatedObjects) {
			super(declaringClass, arrayElementType, propagatedObjects, allPropagatedObjects);
	}
	
	/**
	 * In RTA we are not interested in the propagation of objects on a local scale, 
	 * so this method overrides its parent and does nothing
	 */
	protected void addReadingMethod(MethodPropagator accessor) {}

	/**
	 * In RTA,array elements need not propagate since they cannot find new targets or create new objects.
	 */
	protected boolean isPropagationRequired() {
		return false; 
	}
	
	/** 
	 * we are not interested in propagating objects since we cannot find new targets or create new objects.
	 */
	protected void propagateObjects() {}
}
