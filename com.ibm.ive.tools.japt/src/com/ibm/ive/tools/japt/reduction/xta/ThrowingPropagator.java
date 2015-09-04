package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.ive.tools.japt.reduction.SimpleTreeSet;
import com.ibm.ive.tools.japt.reduction.ClassSet.ClassIterator;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedClassVector;

/**
 * @author sfoley
 * 
 * A throwing propagator is a special kind of propagator.  Those objects propagated, which have been "thrown", are never
 * accessible to the method in question.  Instread the method is used as a conduit to other methods.
 * <p>
 * Consider it as follows:  <br>
 * If there are three propagators: a, b,c<b>
 * a propagates to b which propagates to c, and c throws an object o,
 * then that object o might land in a, with b acting as the throwing propagator from c to a,
 * although the object o is never actually propagated in the normal sense, making itself accessible to b.
 *
 */
abstract class ThrowingPropagator extends Propagator {

	private BT_ClassVector propagatedThrownObjects;
	private SimpleTreeSet allPropagatedThrownObjects;
	private static final short requiresThrowingRepropagation = 0x10;
	
	public ThrowingPropagator(Clazz declaringClass) {
		super(declaringClass);
	}
	
	public ThrowingPropagator(Clazz declaringClass,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(declaringClass, propagatedObjects, allPropagatedObjects);
	}
	
	public String getState() {
		return super.getState()
			+ "New thrown objects to pass: " + + ((propagatedThrownObjects == null) ? 0 : propagatedThrownObjects.size()) +  ((propagatedThrownObjects == null) ? "\n" : ":\n" + getListMembers(propagatedThrownObjects) + '\n')
			+ "Thrown objects previously passed: " + ((allPropagatedThrownObjects == null) ? 0 : allPropagatedThrownObjects.size()) + "\n"
			;
	}
	
	void scheduleThrowingRepropagation() {
		flags |= requiresThrowingRepropagation;
	}
	
	protected void addThrownPropagatedObject(BT_Class objectType) {
		//trace("adding thrown " + objectType);
		if(propagatedThrownObjects == null) {
			propagatedThrownObjects = new BT_ClassVector(); //BT_HashedClassVector?
			propagatedThrownObjects.addElement(objectType);
			return;
		}
		propagatedThrownObjects.addUnique(objectType);
	}
	
	boolean isThrownPropagated(BT_Class objectType) {
		return allPropagatedThrownObjects != null && allPropagatedThrownObjects.contains(objectType);
	}
	
	boolean hasThrownPropagated() {
		return allPropagatedThrownObjects != null && !allPropagatedThrownObjects.isEmpty();
	}
			
	abstract void propagateThrownObjectToAllTargets(BT_Class objectType);
	
	abstract void propagateThrownObjectToNewTargets(BT_Class objectType);
	
	/*
	 * Overriding methods:
	 */
	protected void propagateObjects() {
		super.propagateObjects();
		if(propagatedThrownObjects == null) {
			return;
		}
		
		if(allPropagatedThrownObjects == null) {
			allPropagatedThrownObjects = new SimpleTreeSet();
		}
		
		for(int i=0; i<propagatedThrownObjects.size(); i++) {
			BT_Class propagatedObject = propagatedThrownObjects.elementAt(i);
			propagateThrownObjectToAllTargets(propagatedObject);
			allPropagatedThrownObjects.add(propagatedObject);
		}
		propagatedThrownObjects.removeAllElements();
	}
	
	protected void repropagateObjects() {
		if(super.requiresRepropagation()) {
			super.repropagateObjects();
		}
		if((flags & requiresThrowingRepropagation) != 0) {
			ClassIterator it = allPropagatedThrownObjects.iterator();
			while(it.hasNext()) {
				BT_Class object = it.next();
				propagateThrownObjectToNewTargets(object);
			}
			flags &= ~requiresThrowingRepropagation;
		}
	}
	
	protected boolean requiresRepropagation() {
		return ((flags & requiresThrowingRepropagation) != 0) || super.requiresRepropagation();
	}
	
	protected boolean somethingNewToPropagate() {
		return super.somethingNewToPropagate() || (propagatedThrownObjects != null && propagatedThrownObjects.size() > 0);
	}
}
