package com.ibm.ive.tools.japt.reduction.rta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.ive.tools.japt.reduction.xta.Field;
import com.ibm.ive.tools.japt.reduction.xta.MethodPropagator;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_HashedClassVector;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RTAField extends Field {

	/**
	 * Constructor for RTAField.
	 * @param repository
	 * @param field
	 */
	public RTAField(Repository repository, BT_Field field,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(repository, field, propagatedObjects, allPropagatedObjects);
	}
	
	/**
	 * In RTA we are not interested in the propagation of objects on a local scale, 
	 * so this method overrides its parent and does nothing
	 */
	protected void addReadingMethod(MethodPropagator accessor) {}
	
	/** 
	 * we are not interested in propagating objects since we cannot find new targets or create new objects.
	 */
	protected void propagateObjects() {}
	
	/**
	 * repropagation is not necessary for RTA fields since they have no targets
	 */
	protected boolean requiresRepropagation() {
		return false;
	}
}
