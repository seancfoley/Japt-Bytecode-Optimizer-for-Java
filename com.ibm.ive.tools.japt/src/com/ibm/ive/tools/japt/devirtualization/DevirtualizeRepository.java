package com.ibm.ive.tools.japt.devirtualization;

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
public class DevirtualizeRepository {

	private HashMap methods = new HashMap();
	boolean assumeUnknownVirtualTargets;
	private JaptRepository rep;
	Logger logger;
	Messages messages;
	
	DevirtualizeRepository(
			JaptRepository rep,
			Logger logger,
			Messages messages,
			boolean assumeUnknownVirtualTargets) {
		this.rep = rep;
		this.assumeUnknownVirtualTargets = assumeUnknownVirtualTargets;
		this.logger = logger;
		this.messages = messages;
	}
	
	Method getMethod(BT_Method method) {
		Method result = (Method) methods.get(method);
		if(result == null) {
			result = new Method(method, rep, this);
			methods.put(method, result);
		}
		return result;
	}
	
	Iterator iterator() {
		return methods.values().iterator();
	}

}
