/*
 * Created on Aug 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.remapping;

import com.ibm.ive.tools.japt.JaptClass;
import com.ibm.ive.tools.japt.JaptMethod;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_CreationSite;
import com.ibm.jikesbt.BT_CreationSiteVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_NewIns;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SiteMapper {

	private static final int READ = 0;
	private static final int WRITE = 1;
	private static final int BOTH = 2;
	
	/** map only with these identified classes */
	private BT_ClassVector containingClasses;
	
	/** map only within these identified methods */
	private BT_MethodVector containingMethods;
	
	/** make method invocations special where ambiguity exists */
	boolean makeSpecial;
	
	/** increase access permissions to make a mapping possible */
	boolean overridePermissions;
	
	/** check whether class and primitive types are compatible */
	boolean checkTypes;
	
	/** change all class reference instructions **/
	boolean includeReferenceSites;
	
	JaptRepository repository;
	
	private Messages messages;
	
	private Logger logger;
	
	
	
	/**
	 * 
	 */
	public SiteMapper(Messages messages, Logger logger, JaptRepository repository,
			BT_ClassVector containingClasses, BT_MethodVector containingMethods) {
		this.messages = messages;
		this.containingClasses = containingClasses;
		this.containingMethods = containingMethods;
		this.repository = repository;
		this.logger = logger;
	}
	
	/**
	 * @param specifiedFields
	 * @param target
	 */
	void mapField(BT_FieldVector specifiedFields, BT_Field target) {
		for(int j=0; j<specifiedFields.size(); j++) {
			mapField(specifiedFields.elementAt(j), target);
		}
	}
	
	void mapClass(BT_ClassVector targets, BT_Class newTarget) {
		for(int j=0; j<targets.size(); j++) {
			mapClass(targets.elementAt(j), newTarget);
		}
	}
	
	void mapFieldReads(BT_FieldVector specifiedFields, BT_Field target) {
		for(int j=0; j<specifiedFields.size(); j++) {
			mapFieldReads(specifiedFields.elementAt(j), target);
		}
	}
	
	void mapFieldWrites(BT_FieldVector specifiedFields, BT_Field target) {
		for(int j=0; j<specifiedFields.size(); j++) {
			mapFieldWrites(specifiedFields.elementAt(j), target);
		}
	}

	/**
	 * remaps all calls to the indicated methods to the indicated target.
	 * @param specifiedMethods
	 * @param target
	 */
	void mapMethod(BT_MethodVector specifiedMethods, BT_Method target) {
		for(int j=0; j<specifiedMethods.size(); j++) {
			mapMethod(specifiedMethods.elementAt(j), target);
		}
	}
	
	void mapClass(BT_Class target, BT_Class newTarget) {
		if(target.equals(newTarget)) {
			return;
		}
		BT_CreationSiteVector creationSites = (BT_CreationSiteVector) target.creationSites.clone();
		if(creationSites == null) {
			throw new Error(); //must load with creation sites enabled
		}
		//BT_ClassReferenceSiteVector referenceSites = target.referenceSites;
		//BT_ClassVector asArrayTypes = target.asArrayTypes;
		//BT_FieldVector asFieldTypes = target.asFieldTypes;
		//BT_SignatureSiteVector asSignatureTypes = target.asSignatureTypes;
		
//		the options will be to map just the creation sites, the reference sites (which include the creation sites),
//		or all the sites...
//		
//		there is also the problem of method calls and field accesses... ie all invokes, get/putfields, get/putstatics
//		how do we handle those babies?  maybe we should just change their declaring class...
		//We'll just start with the creation sites...
		
		boolean mapAttempted = false;
		for(int i=0; i<creationSites.size(); i++) {
			BT_CreationSite site = creationSites.elementAt(i);
			if(!satisfiesContainment(site.getFrom())) {
				continue;
			}
			mapAttempted = true;
			mapCreationSite(site, target, newTarget);
		}
		if(!mapAttempted) {
			messages.NO_INSTANTIATIONS_TO_MAP.log(logger, target);
		}
	}
	
	void mapCreationSite(BT_CreationSite site, BT_Class target, BT_Class newTarget) {
		BT_Ins instruction = site.instruction;
		if(instruction.isLoadConstantStringIns()) {
			messages.NOT_MAPPED_INSTANTIATION.log(logger, new Object[] {target, site.getFrom(), newTarget, messages.LOAD_CONSTANT});
			return;
		}
		
		if(instruction.isNewArrayIns()) {
//			TODO handle arrays, will need to replace isClassAncestorOf by a check that both are array types and an isInstance
			//then we'll skip the constructor remapping... also need to check special differentiation between referenced class and target specific to the new array instructions
			return;
		}
		
		
		if(checkTypes && !target.isClassAncestorOf(newTarget)) {
			messages.NOT_MAPPED_INSTANTIATION.log(logger, new Object[] {target, site.getFrom(), newTarget, messages.TYPES_INCOMPATIBLE});
			return;
		}
		BT_NewIns newIns = (BT_NewIns) instruction;
		BT_MethodCallSite callSite;
		try {
			callSite = findConstructorInvocation(site, newIns);
			if(callSite == null) {
				messages.NOT_MAPPED_INSTANTIATION.log(logger, new Object[] {target, site.getFrom(), newTarget, messages.NO_CONSTRUCTOR_INVOCATION_FOUND});
				return;
			}
		} catch(BT_CodeException e) {
			messages.NOT_MAPPED_INSTANTIATION.log(logger, new Object[] {target, site.getFrom(), newTarget, messages.NO_CONSTRUCTOR_INVOCATION_FOUND});
			return;
		}
		BT_Method prevConstructor = callSite.getTarget();
		CallSite cSite = new CallSite(callSite, messages, true, overridePermissions, true);
		BT_MethodVector constructors = new BT_MethodVector();
		BT_MethodVector methods = newTarget.getMethods();
		for(int j=0; j<methods.size(); j++) {
			BT_Method method = methods.elementAt(j);
			if(method.isConstructor()) {
				constructors.addElement(method);
			}
		}
		BT_Method constructor = null;
		for(int j=constructors.size()-1; j>=0; j--) {
			BT_Method cons = constructors.elementAt(j);
			if(cons.getSignature().equals(prevConstructor.getSignature())) {
				constructor = cons;
				break;
			}
			else if(!cSite.signaturesCompatible((JaptMethod) cons)) {
				constructors.removeElementAt(j);
			}
		}
		if(constructor == null) {
			String reason = findMaximallySpecific(callSite, constructors);
			if(reason != null) {
				messages.NOT_MAPPED_INSTANTIATION.log(logger, new Object[] {target, site.getFrom(), newTarget, reason});
				return;
			}
			constructor = constructors.firstElement();
		}
		cSite.allowConstructorMapping(true);
		//indicate that only an instance of newTarget can be passed to the constructor
		cSite.setKnownArgument(0, newTarget);
		String reason = cSite.remap((JaptMethod) constructor);
		
		if(reason == null) {
			newIns.target = newTarget;
			messages.MAPPED_INSTANTIATION.log(logger, new Object[] {target, site.getFrom(), newTarget});
		}
		else {
			messages.NOT_MAPPED_INSTANTIATION.log(logger, new Object[] {target, site.getFrom(), newTarget, reason});
		}
	}
	
	/**
	 * narrows the vector of constructors so that it contains a single element, the constructor to be invoked
	 * @param from the callsite
	 * @param constructors
	 * @return if there is no maximally specific constructor, a string is returned indicating the reason, otherwise null is returned
	 */
	private String findMaximallySpecific(BT_MethodCallSite from, BT_MethodVector methods) {
		if(methods.size() == 0) {
			return messages.NO_MATCHING_CONSTRUCTOR;
		}
		if(methods.size() == 1) {
			return null;
		}
		//need to find the best match according to java language rules, see 15.12.2.2 of the java language spec
		//remove non-accessible
		BT_MethodVector copy = (BT_MethodVector) methods.clone();
		for(int i=methods.size()-1; i>=0; i--) {
			BT_Method constructor = methods.elementAt(i);
			if(!constructor.isVisibleFrom(from.getFrom().cls)) {
				methods.removeElementAt(i);
			}
		}
		if(methods.size() == 0) {
			//if nothing is accessible, 
			//then just pick the most specific one, 
			//since we might attempt to increase access permissions later
			for(int i=0; i<copy.size(); i++) {
				methods.addElement(copy.elementAt(i));
			}
		}
		//as specified in the java lang spec, we find the maximally specific methods
		topLoop:
		for(int i=methods.size()-1; i>=0; i--) {
			BT_Method currentConstructor = methods.elementAt(i);
			BT_MethodSignature currentSig = currentConstructor.getSignature();
			for(int j=0; i<methods.size(); j++) {
				if(j==i) {
					continue;
				}
				BT_Method comparedConstructor = methods.elementAt(j);
				BT_MethodSignature comparedSig = comparedConstructor.getSignature();
				//is comparedSig more specific?
				boolean moreSpecific = true;
				for(int k=0; k<currentSig.types.size(); k++) {
					if(!((JaptClass) currentSig.types.elementAt(k)).isInstance(comparedSig.types.elementAt(k))) {
						//compareSig is not more specific
						moreSpecific = false;
						break;
					}
				}
				if(moreSpecific) {
					methods.removeElementAt(i);
					//remove the currentSig from our set, there is another one more specific
					continue topLoop;
				}
			}
		}
		if(methods.size() > 1) {
			return messages.AMBIGUOUS_INVOCATION;
		}
		return null;
	}
	
	BT_MethodCallSite findConstructorInvocation(BT_CreationSite site, BT_NewIns newIns) throws BT_CodeException {
		BT_CodeAttribute code = site.from;
		if(code == null) {
			return null;
		}
		return code.findConstructorInvocation(newIns);
	}
	
	void mapMethod(BT_Method target, BT_Method newTarget) {
		if(target.equals(newTarget)) {
			return;
		}
		BT_MethodCallSiteVector callSites = (BT_MethodCallSiteVector) target.callSites.clone();
		boolean mapAttempted = false;
		for(int i=0; i<callSites.size(); i++) {
			BT_MethodCallSite callSite = callSites.elementAt(i);
			if(!satisfiesContainment(callSite.getFrom())) {
				continue;
			}
			mapAttempted = true;
			CallSite site = new CallSite(callSite, messages, makeSpecial, overridePermissions, checkTypes);
			String reason = site.remap((JaptMethod) newTarget);
			if(reason == null) {
				messages.MAPPED_CALLSITE.log(logger, new Object[] {target, callSite.getFrom(), newTarget});
			} else {
				messages.NOT_MAPPED_CALLSITE.log(logger, new Object[] {target, callSite.getFrom(), newTarget, reason});
			}
		}
		if(!mapAttempted) {
			messages.NO_CALLSITES_TO_MAP.log(logger, target);
		}
	}
	
	void mapFieldReads(BT_Field target, BT_Field newTarget) {
		mapField(target, newTarget, READ);
	}
	
	void mapFieldWrites(BT_Field target, BT_Field newTarget) {
		mapField(target, newTarget, WRITE);
	}
	
	void mapField(BT_Field target, BT_Field newTarget) {
		mapField(target, newTarget, BOTH);
	}
	
	private void mapField(BT_Field target, BT_Field newTarget, int accessType) {
		if(target.equals(newTarget)) {
			return;
		}
		BT_AccessorVector sites = (BT_AccessorVector) target.accessors.clone();
		boolean mapAttempted = false;
		for(int i=0; i<sites.size(); i++) {
			BT_Accessor site = sites.elementAt(i);
			switch(accessType) {
				case READ:
					if(!site.instruction.isFieldReadIns()) {
						continue;
					}
					break;
				case WRITE:
					if(site.instruction.isFieldReadIns()) {
						continue;
					}
				default: //do nothing
			}
			if(!satisfiesContainment(site.getFrom())) {
				continue;
			}
			mapAttempted = true;
			Accessor accessor = new Accessor(site, messages, overridePermissions, checkTypes);
			String reason = accessor.remap(newTarget);
			if(reason == null) {
				messages.MAPPED_ACCESSOR.log(logger, new Object[] {target, site.from, newTarget});
			}
			else {
				messages.NOT_MAPPED_ACCESSOR.log(logger, new Object[] {target, site.from, newTarget});
			}
		}
		if(!mapAttempted) {
			messages.NO_ACCESSORS_TO_MAP.log(logger, target);
		}
	}
	
	/**
	 * determines if a given method satisfies the mapper's containment criteria
	 * @param location
	 * @return
	 */
	boolean satisfiesContainment(BT_Method location) {
		if(containingClasses != null) {
			if(containingMethods != null) {
				return containingMethods.contains(location) || containingClasses.contains(location.cls);
			}
			return containingClasses.contains(location.cls);
		} else if(containingMethods != null) {
			return containingMethods.contains(location);
		}
		return true;
	}
	

}
