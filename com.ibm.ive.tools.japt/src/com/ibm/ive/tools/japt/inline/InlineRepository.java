package com.ibm.ive.tools.japt.inline;

import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;
import java.util.*;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class InlineRepository {

	private HashMap methods = new HashMap();
	private boolean assumeUnknownVirtualTargets;
	private boolean inlineFromAnywhere;
	JaptRepository rep;
	
	InlineRepository(
			JaptRepository rep,
			boolean assumeUnknownVirtualTargets, 
			boolean inlineFromAnywhere) {
		this.assumeUnknownVirtualTargets = assumeUnknownVirtualTargets;
		this.inlineFromAnywhere = inlineFromAnywhere;
		this.rep = rep;
	}
	
	Method getMethod(BT_Method method) {
		Method result = (Method) methods.get(method);
		if(result == null) {
			if(method.isStatic()) {
				result = new StaticMethod(method, this, inlineFromAnywhere);
			}
			else {
				result = new InstanceMethod(method, this, assumeUnknownVirtualTargets, inlineFromAnywhere);
			}
			methods.put(method, result);
		}
		return result;
	}
	
	Iterator iterator() {
		return methods.values().iterator();
	}

}
