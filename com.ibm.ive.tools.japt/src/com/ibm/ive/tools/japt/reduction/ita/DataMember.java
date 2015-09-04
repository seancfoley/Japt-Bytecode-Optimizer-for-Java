package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_Class;


/**
 * @author sfoley
 * 
 * Represents a propagator that propagates objects by simple data storage, which in java
 * is either a field or an array element.
 *
 */
public abstract class DataMember extends ObjectPropagator {

	protected ReadingPropagationTargets targets;
	protected ReadingPropagationTargets newTargets;
	
	public abstract PropagatedObject getContainingObject();
	
	/**
	 * We know this field will contain an object of the type objectType.  Propagate
	 * this object to ay methods which access this field.
	 */
	void propagateNewObject(ReceivedObject obj) {
		if(targets == null) {
			return;
		}
		targets.propagate(obj, this);
	}
	
	/**
	 * There are new targets for propagation, so  we must repropagate to these new
	 * targets an object that has been previously propagated.
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#propagateOldObject(BT_Class)
	 */
	void propagateOldObject(ReceivedObject obj) {
		if(newTargets == null) {
			return;
		}
		newTargets.propagate(obj, this);
	}

	/**
	 * Add the method to the list of targets to which objects may be propagated.
	 */
	void addReadingMethod(AccessedPropagator accessor) {
		if(isInitialized() && hasPropagated()) {
			if(newTargets == null) {
				newTargets = new ReadingPropagationTargets();
			}
			newTargets.addAccessor(accessor);
			scheduleRepropagation();
			return;
		}
		//note that this field has already been marked as accessed
		if(targets == null) {
			targets = new ReadingPropagationTargets();
		}
		targets.addAccessor(accessor);
	}
	
	public boolean hasReaders() {
		return (newTargets != null && newTargets.hasReaders())
			|| (targets != null && targets.hasReaders());
	}
	
	void migrateTargets() {
		if(newTargets == null) {
			return;
		} else if(targets == null) {
			targets = newTargets;
			newTargets = null;
		} else {
			newTargets.migrate(targets);
			newTargets = null;
		}
	}
	
	/**
	 * @return the type that can be stored in this data member.
	 */
	public abstract Clazz getDataType();
	
	Repository getRepository() {
		return getDataType().repository;
	}
	
	PropagationProperties getPropagationProperties() {
		return getRepository().getPropagationProperties();
	}
	
	public abstract boolean isFieldInstance();
		
}
