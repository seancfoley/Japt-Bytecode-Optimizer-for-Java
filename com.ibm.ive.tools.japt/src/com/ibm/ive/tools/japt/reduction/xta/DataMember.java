package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_HashedClassVector;

/**
 * @author sfoley
 *
 */
public abstract class DataMember extends Propagator {

	private ReadingPropagationTargets targets;
	private ReadingPropagationTargets newTargets;
	

	/**
	 * Constructor for DataMember.
	 * @param repository
	 * @param clazz
	 */
	public DataMember(Clazz declaringClass) {
		super(declaringClass);
	}

	public DataMember(Clazz declaringClass,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(declaringClass, propagatedObjects, allPropagatedObjects);
	}
	
	public String getState() {
		return super.getState()
			+ "Targets:\n" + targets.getState()
			+ "New targets:\n" + newTargets.getState()
			;
	}
	
	/**
	 * We know this field will contain an object of the type objectType.  Propagate
	 * this object to ay methods which access this field.
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#propagateNewObject(BT_Class)
	 */
	protected void propagateNewObject(BT_Class objectType) {
		if(targets != null) {
			targets.propagate(objectType);
		}
	}
	
	/**
	 * There are new targets for propagation, so  we must repropagate to these new
	 * targets an object that has been previously propagated.
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#propagateOldObject(BT_Class)
	 */
	protected void propagateOldObject(BT_Class objectType) {
		if(newTargets != null) {
			newTargets.propagate(objectType);
		}
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#initializePropagation()
	 */
	void initializePropagation() {}
	
	boolean hasPreviouslyBeenReadBy(MethodPropagator accessor) {
		return (targets != null && targets.containsAccessor(accessor)) 
			|| (newTargets != null && newTargets.containsAccessor(accessor));
	}
	
	/**
	 * Add the method to the list of targets to which objects may be propagated.
	 */
	protected void addReadingMethod(MethodPropagator accessor) {
		if(isPropagationInitialized() && hasPropagated()) {
			if(newTargets == null) {
				newTargets = new ReadingPropagationTargets();
			}
			newTargets.addAccessor(accessor);
			scheduleRepropagation();
			return;
		}
		if(targets == null) {
			targets = new ReadingPropagationTargets();
		}
		targets.addAccessor(accessor);
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#migrateTargets()
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
	
	/**
	 * @return whether this member can hold an object of the given type
	 */
	abstract boolean holdsType(BT_Class type);
		
}
