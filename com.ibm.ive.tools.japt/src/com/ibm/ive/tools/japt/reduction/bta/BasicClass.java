/*
 * Created on Jul 20, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reduction.bta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.ive.tools.japt.reduction.xta.Clazz;
import com.ibm.ive.tools.japt.reduction.xta.Method;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Class;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BasicClass extends Clazz {
	HashMap callableOverridingMethods;
	
	BasicClass(Repository r, BT_Class clazz) {
		super(r, clazz);
	}
	
	public void setInstantiated() {
		if(isInstantiated()) {
			return;
		}
		super.setInstantiated();
		
		//BasicMethodPotentialTargets.trace(clazz);
		
		if(callableOverridingMethods == null) {
			return;
		}
		
		//The following logic makes the bta algorithm a little more like rta, but this is how
		//bta is implemented in jxelink 2.2 so we imitate the behaviour
		Set entrySet = callableOverridingMethods.entrySet();
		Iterator iterator = entrySet.iterator();
		while(iterator.hasNext()) {
			Map.Entry entry = (Map.Entry) iterator.next();
			Method caller = (Method) entry.getKey();
			ArrayList overridingMethods = (ArrayList) entry.getValue();
			for(int j=0; j<overridingMethods.size(); j++) {
				Method overridingMethod = (Method) overridingMethods.get(j);
				BasicMethodPotentialTargets.markOverridingMethod(caller, overridingMethod);
			}
		}
		callableOverridingMethods = null;
		
	}
	
	HashMap getFutureCallables() {
		if(isInstantiated()) {
			//should never reach here
			throw new RuntimeException();
		}
		if(callableOverridingMethods == null) {
			callableOverridingMethods = new HashMap(clazz.getMethods().size());
		}
		return callableOverridingMethods;
	}
}
