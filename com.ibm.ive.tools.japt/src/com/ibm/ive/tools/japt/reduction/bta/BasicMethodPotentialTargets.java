/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reduction.bta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.ive.tools.japt.reduction.xta.Clazz;
import com.ibm.ive.tools.japt.reduction.xta.Field;
import com.ibm.ive.tools.japt.reduction.xta.Method;
import com.ibm.ive.tools.japt.reduction.xta.MethodPotentialTargets;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BasicMethodPotentialTargets implements MethodPotentialTargets {

	private Method method;
	private RelatedMethodMap map;
	
	/**
	 * 
	 */
	public BasicMethodPotentialTargets(Method method, RelatedMethodMap map) {
		this.method = method;
		this.map = map;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.reduction.MethodPotentialTargets#findNewTargets(com.ibm.jikesbt.BT_Class)
	 */
	public void findNewTargets(BT_Class objectType) {
		//no such thing as a new target in BTA:  when something becomes reachable it is accessed/invoked right away	
	}
	
	
	/**
	 * In basic reduction we mark as accessed all possible targets right away.
	 */
	public void findTargets(BT_CodeAttribute code) {
		Repository rep = method.getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(method.getBTClass());
		BT_AccessorVector accessedFields = code.accessedFields;
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
	
		HashSet alreadyFound = new HashSet();
	
		//TODO reduction fix involving not targetting explicitly removed items
		//determine which fields are read from and which are written to
		for(int i=0; i<accessedFields.size(); i++) {
			BT_Accessor acc = accessedFields.elementAt(i);
			BT_Field accField = acc.instruction.target;
			if(alreadyFound.contains(accField)) {
				continue;
			}
			alreadyFound.add(accField);
			Field field = method.getRepository().getField(accField);
			field.setAccessed();
			if(checkEntryPoints && japtRepository.isInternalClass(accField.getDeclaringClass())) {
				lister.foundEntryTo(accField, method.underlyingMethod);
			}
		}
	  
	
	  //determine which methods can potentially be called		
		int size = calledMethods.size();
		if(size > 0) {
			alreadyFound.clear();
			//we add ourselves so that we do not have recursion or 
			//propagating to myself
			alreadyFound.add(method.underlyingMethod);
		}
		for(int i=0; i<size; i++) {
			BT_MethodCallSite site = calledMethods.elementAt(i);
			BT_Method calledMethod = site.getTarget();
			BT_Class targetClass = site.getClassTarget();
			
			if(alreadyFound.contains(calledMethod)) {
				continue;
			}
			alreadyFound.add(calledMethod);
			Method referencedMethod = method.getRepository().getMethod(calledMethod);
			Clazz refClass = method.getRepository().getClazz(targetClass);
			
			if(checkEntryPoints && japtRepository.isInternalClass(calledMethod.getDeclaringClass())) {
				lister.foundEntryTo(calledMethod, method.underlyingMethod);
			}
			
			referencedMethod.setAccessed();
			refClass.setRequired();
			BT_MethodVector relatedMethods = map.getOtherRelatedMethods(calledMethod);
			if(relatedMethods == null) {
				continue;
			}
			//access all overriding or implementing methods
			BT_Class referencedClass = calledMethod.getDeclaringClass();
			BT_MethodVector callableMethods;
			if(referencedClass.isInterface()) {
				callableMethods = map.getImplementingMethods(calledMethod);
			}
			else {
				callableMethods = map.getOverridingMethods(calledMethod);
			}
			for(int k=0; k<callableMethods.size(); k++) {
				BT_Method callableMethod = callableMethods.elementAt(k);
				markOverridingMethod(checkEntryPoints, rep.getMethod(callableMethod));
			}
		}
	}
	
	void markOverridingMethod(boolean checkEntryPoints, Method overridingMethod) {
		markOverridingMethod(method, checkEntryPoints, overridingMethod);
	}

	static void markOverridingMethod(Method method, Method overridingMethod) {
		Repository rep = method.getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(method.getBTClass());
		markOverridingMethod(method, checkEntryPoints, overridingMethod);
	}
	
	/**
	 * @param japtRepository
	 * @param lister
	 * @param checkEntryPoints
	 * @param relatedMethod
	 */
	static void markOverridingMethod(Method method, boolean checkEntryPoints, Method overridingMethod) {
		Repository rep = method.getRepository();
		JaptRepository japtRepository = rep.repository;
		BT_Method relatedBTMethod = overridingMethod.underlyingMethod;
		
		//trace(relatedBTMethod);
		
		//The following logic makes the bta algorithm a little more like rta, but this is how
		//bta is implemented in jxelink 2.2 so we imitate the behaviour (see also class BasicClass)
		Clazz overridingClass = overridingMethod.getDeclaringClass();
		if(!overridingClass.isInstantiated()) {
			
			HashMap futureCallables = ((BasicClass) overridingClass).getFutureCallables();
			ArrayList list = (ArrayList) futureCallables.get(method);
			if(list == null) {
				list = new ArrayList(1);
				futureCallables.put(method, list);
			}
			list.add(overridingMethod);
			//System.out.println("call to " + overridingMethod + " from " + method + " is stored in " + overridingClass);
			return;
		}
		
		if(checkEntryPoints && japtRepository.isInternalClass(relatedBTMethod.getDeclaringClass())) {
			EntryPointLister lister = rep.entryPointLister;
			lister.foundOverridingOrImplementingEntryTo(relatedBTMethod, method.underlyingMethod);
		}
		overridingMethod.setAccessed();
	}
	
//	static void trace(BT_Method method) {
//		if(false 
//				||method.useName().equals("com.ibm.ive.tools.japt.reduction.SimpleTreeSet$Iterator.next()")
//				|| method.useName().equals("com.ibm.ive.tools.japt.reduction.ClassSet$ClassIterator.next()")
//				||method.useName().equals("com.ibm.ive.tools.japt.reduction.SimpleTreeSet$Iterator.hasNext()")
//				|| method.useName().equals("com.ibm.ive.tools.japt.reduction.ClassSet$ClassIterator.hasNext()")
//				
//		) {
//			System.out.println("hi " + method);
//		}
//	}
//	
//	static void trace(BT_Class method) {
//		if(method.useName().equals("com.ibm.ive.tools.japt.reduction.SimpleTreeSet$Iterator")
//				|| method.useName().equals("com.ibm.ive.tools.japt.reduction.ClassSet$ClassIterator")) {
//			System.out.println("hi2 " + method);
//		}
//	}
}
