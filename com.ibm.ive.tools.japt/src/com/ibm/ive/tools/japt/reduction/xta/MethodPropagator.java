package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_HashedClassVector;

/**
 * @author sfoley
 *
 */
public abstract class MethodPropagator extends ThrowingPropagator {

	private MethodPropagationTargets targets;
	private MethodPropagationTargets newTargets;
	protected MethodPotentialTargets potentialTargets;
	
	protected static final MethodPotentialTargets noPotentialTargets = new MethodPotentialTargets() {
		public void findNewTargets(BT_Class objectType) {}
		
		public void findTargets(BT_CodeAttribute code) {}
	};
	
	MethodPropagator(Clazz declaringClass) {
		super(declaringClass);
	}
	
	MethodPropagator(Clazz declaringClass,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(declaringClass, propagatedObjects, allPropagatedObjects);
	}
	
	public String getState() {
		return super.getState()
			+ "Targets:\n" + ((targets == null) ? "none\n" : targets.getState())
			+ "New targets:\n" + ((newTargets == null) ? "none\n" : newTargets.getState())
			;
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#migrate()
	 */
	protected void migrateTargets() {
		if(newTargets == null) {
			return;
		}
		else if(targets == null) {
			targets = newTargets;
			newTargets = null;
		}
		else {
			newTargets.migrate(targets);
		}
	}
	
	private void initializeTargets() {
		if(targets == null) {
			targets = new MethodPropagationTargets(this);
		}
	}
	
	private void initializeNewTargets() {
		if(newTargets == null) {
			newTargets = new MethodPropagationTargets(this);
		}
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#propagateNewObject(BT_Class)
	 */
	protected void propagateNewObject(BT_Class objectType) {
		//trace("propagating new object: " + objectType);
		potentialTargets.findNewTargets(objectType);
		if(targets != null) {
			targets.propagateObject(objectType);
		}
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#propagateOldObject(BT_Class)
	 */
	protected void propagateOldObject(BT_Class objectType) {
		//trace("propagating new object: " + objectType);
		if(newTargets != null) {
			newTargets.propagateObject(objectType);
		}
	}

	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.ThrowingPropagator#propagateThrownObjectToAllTargets(BT_Class)
	 */
	void propagateThrownObjectToAllTargets(BT_Class objectType) {
		if(targets != null) {
			targets.propagateThrownObject(objectType);
		}
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.ThrowingPropagator#propagateThrownObjectToNewTargets(BT_Class)
	 */
	void propagateThrownObjectToNewTargets(BT_Class objectType) {
		if(newTargets != null) {
			newTargets.propagateThrownObject(objectType);
		}
	}
	
	/*
	 * We have the havePreviouslyCalled(Method) method because with virtual calls we
	 * cannot keep an exact list of possible method invocations.  We need never make the 
	 * same check for static invocations since we know immediately the exact set of possible
	 * static methods than can be called by looking at the method code.
	 * 
	 * We need never make the 
	 * same check for field accesses since we know immediately the exact set of possible
	 * fields that can be accessed by looking at the method code.
	 * 
	 * For array elements, we cannot keep an exact list of array elements written to or read
	 * from, so it is necessary to use hasPreviouslyWrittenTo and hasPreviouslyReadFrom
	 * 
	 */
	
	boolean hasPreviouslyCalled(Method instance) {
		return (targets != null && targets.containsCalledMethod(instance)) 
			|| (newTargets != null && newTargets.containsCalledMethod(instance));
	}
	
	boolean hasPreviouslyWrittenTo(DataMember member) {
		return (targets != null && targets.containsWrittenDataMember(member)) 
			|| (newTargets != null && newTargets.containsWrittenDataMember(member));
	}
	
	protected void addCallingMethod(Method method) {
		if(isPropagationInitialized()) {
			if(hasPropagated() && returnsObjects()) {
				initializeNewTargets();
				newTargets.addCallingMethod(method);
				scheduleRepropagation();
				if(hasThrownPropagated()) {
					scheduleThrowingRepropagation();
				}
				return;
			}
			else if(hasThrownPropagated()) {
				initializeNewTargets();
				newTargets.addCallingMethod(method);
				scheduleThrowingRepropagation();
				return;
			}
		}
		initializeTargets();
		targets.addCallingMethod(method);
	}
	
	protected void addWrittenDataMember(DataMember memberWrittenTo) {
		if(hasPropagated()) {
			scheduleRepropagation();
			initializeNewTargets();
			newTargets.addWrittenDataMember(memberWrittenTo);
		}
		else {
			initializeTargets();
			targets.addWrittenDataMember(memberWrittenTo);
		}
	}
	
	protected void addCalledMethod(Method calledMethod) {
		if(hasPropagated()) {
			scheduleRepropagation();
			initializeNewTargets();
			newTargets.addCalledMethod(calledMethod);
		}
		else {
			initializeTargets();
			targets.addCalledMethod(calledMethod);
		}
	}
	
	
	abstract boolean returnsObjects();
	
	abstract boolean isThrowableObject(BT_Class throwable);
	
	abstract boolean canThrow(BT_Class throwable);
	
	abstract boolean returns(BT_Class c);
}
