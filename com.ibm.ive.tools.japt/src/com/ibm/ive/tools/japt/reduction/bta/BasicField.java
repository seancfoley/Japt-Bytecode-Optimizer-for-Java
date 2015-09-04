/*
 * Created on May 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reduction.bta;

import com.ibm.ive.tools.japt.reduction.xta.Field;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Field;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BasicField extends Field {

	/**
	 * @param repository
	 * @param field
	 */
	BasicField(Repository repository, BT_Field field) {
		super(repository, field);
	}

	/** 
	 * we are not interested in propagating objects since we cannot find new targets or create new objects.
	 */
	protected void propagateObjects() {}
	
	protected void addPropagatedObject(BT_Class objectType) {}
		
	/* we do not override addCreated methods, because objects that are created must
	 * have their class marked instantiated, even if we then do not propagate the created object type
	 * afterwards, which we will not since addPropagatedObject is overridden.
	 */
}
