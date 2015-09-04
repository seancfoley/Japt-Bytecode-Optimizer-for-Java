/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reduction.bta;

import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.reduction.xta.Method;
import com.ibm.ive.tools.japt.reduction.xta.MethodPotentialTargets;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Method;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BasicMethod extends Method {

	RelatedMethodMap map;
	
	/**
	 * @param repository
	 * @param method
	 * @param propagatedObjects
	 * @param allPropagatedObjects
	 */
	BasicMethod(Repository repository, BT_Method method) {
		super(repository, method);
		this.map = repository.relatedMethodMap;
	}
	
	protected MethodPotentialTargets getPotentialTargets(BT_CodeAttribute code) {
		int accessedFields = code.accessedFields.size();
		int calledMethods = code.calledMethods.size();
		if(accessedFields == 0 && calledMethods == 0) {
			return noPotentialTargets;
		}
		return new BasicMethodPotentialTargets(this, map);
	}
	
	protected MethodPotentialTargets getPotentialNativeTargets() {
		return noPotentialTargets;
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
