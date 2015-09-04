package com.ibm.ive.tools.japt.reduction.rta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.ive.tools.japt.reduction.xta.DataMember;
import com.ibm.ive.tools.japt.reduction.xta.Method;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Method;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RTAMethod extends Method {

	protected BT_ClassVector newlyPropagatedObjects;
	
	/**
	 * Constructor for RTAMethod.
	 * @param repository
	 * @param method
	 */
	public RTAMethod(Repository repository, BT_Method method, BT_HashedClassVector newlyPropagatedObjects,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(repository, method,  propagatedObjects, allPropagatedObjects);
		this.newlyPropagatedObjects = newlyPropagatedObjects;
	}

	/**
	 *  In XTA, when an old object is repropagated, it has previously been propagated
	 * to this same propagator, so we need not look for more new targets, we simply wish to propagate to the
	 * existing new targets.  In RTA, the object has not been propagated to this propagator before, so
	 * we need to look for new targets, but we are not interested in propagating objects on a local scale so we
	 * do nothing more..
	 */
	protected void propagateOldObject(BT_Class objectType) {
		potentialTargets.findNewTargets(objectType);
	}
	
	
	
	/**
	 * In RTA we are not interested in the propagation of objects on a local scale, 
	 * so this method overrides its parent and does nothing
	 */
	protected void addCallingMethod(Method callingMethod) {}
	
	/**
	 * In RTA we are not interested in the propagation of objects on a local scale, 
	 * so this method overrides its parent and does nothing
	 */
	protected void addWrittenDataMember(DataMember memberWrittenTo) {}
	
	/**
	 * In RTA we are not interested in the propagation of objects on a local scale, 
	 * so this method overrides its parent and does nothing
	 */
	protected void addCalledMethod(Method calledMethod) {}
	
	/**
	 * We need not keep track of thrown objects in RTA, whenever an object is created
	 * it is available to all method propagators so throwing objects around is unnecessary.
	 */
	protected void addThrownPropagatedObject(BT_Class objectType) {}
	
	/**
	 * Propagate those objects which have not been propagated yet
	 */
	protected void propagateObjects() {
		if(propagatedObjects == null || propagatedObjects.size() == 0) {
			return;
		}
		for(int i=0; i<propagatedObjects.size(); i++) {
			BT_Class propagatedObject = propagatedObjects.elementAt(i);
			propagateNewObject(propagatedObject);
		}
	}
	
	/**
	 * the member is known to hold an object of the indicated type, 
	 * so propagate this object to other members accessed by this member in the next iteration.
	 */
	protected void addPropagatedObject(BT_Class objectType) {
		if(!propagatedObjects.contains(objectType)) {
			newlyPropagatedObjects.addUnique(objectType);
		}
	}
		
}
