/*
 * Created on Apr 9, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.coldMethod;

import java.util.Collection;
import java.util.HashMap;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.SyntheticClassPathEntry;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_StackPool;

public class ExtensionRepository {
	private final HashMap classes = new HashMap(); //maps BT_Class to Clazz
	final JaptRepository repository;
	final ClassPathEntry coldClassesEntry;
	final Logger logger;
	final Messages messages;
	
	/**
	 * whether we are allowed to change access permissions of items in internal classes.
	 */
	boolean allowChangedPermissions;
	
	/**
	 * whether we are allowed to use accessor methods in internal classes.
	 */
	boolean allowAccessors;
	
	RelatedMethodMap relatedMethodMap;
	BT_StackPool stackPool = new BT_StackPool();
	
	ExtensionRepository(JaptRepository rep, Messages messages, Logger logger) {
		this.repository = rep;
		this.logger = logger;
		relatedMethodMap = rep.getRelatedMethodMap();
		coldClassesEntry = new SyntheticClassPathEntry(messages.COLD_METHOD_CPE);
		rep.appendInternalClassPathEntry(coldClassesEntry);
		this.messages = messages;
	}
	
	public Clazz hasClazz(BT_Class clazz) {
		return (Clazz) classes.get(clazz);
	}
	
	public Clazz getClazz(BT_Class clazz) {
		Clazz result = (Clazz) classes.get(clazz);
		if(result == null) {
			result = new Clazz(clazz);
			classes.put(clazz, result);
		}
		return result;
	}
	
	public Clazz[] getClazzes() {
		Collection clazzes = classes.values();
		return (Clazz[]) clazzes.toArray(new Clazz[clazzes.size()]);
	}
	
}
