/*
 * Created on Apr 24, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSite;
import com.ibm.jikesbt.BT_ClassReferenceSiteVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;

/**
 * 
 * @author sfoley
 *
 * This class facilitates moving a code attribute from one method to another method, whether being copied or inlined.
 * 
 * There are three things that need to be checked:
 * 
 * 1. access permissions of method invocations, field accesses, and class references
 * 
 * 2. method invocations using invoke special instructions have special semantics depending on what their target is and where they are accessed from.
 * 	-an invoke special of a constructor of a newly created objected must occur in the same method following the new instruction
 * 	-an invoke special of a super constructor must occur in a constructor
 *  -an invoke special of a method must either access a method in a super class or in the same class.
 *  
 * 3. constructors and static initializers assigning to final fields
 * 
 * Clients pass an object of VisibilityChanger to an instance of this class.  The given changer objects will decide what it can
 * or cannot do to facilitate the migration of code.  When all invocations, accesses and references have been checked by 
 * the AccessChecker, it may or may not call upon the VisibilityChanger object to make all the required changes that is has
 * already indicated that it is capable of making to allow migration of the code.
 * 
 */
public class AccessChecker {
	private final BT_Class oldFrom;
	private final BT_Class newFrom;
	private final BT_Method originalMethod;
	private final BT_InsVector ignoreInstructions;
	private final AccessPreserver accessPreserver;
	private static final BT_InsVector emptyVector = new BT_InsVector(0);
	
	private boolean checkedCanMakeLegal;
	private boolean canMakeLegal;
	private boolean madeLegal;
	private boolean checkedIsLegal;
	
	public AccessChecker(
			BT_Method originalMethod, 
			BT_Class originalClass, 
			BT_Class newClass, 
			AccessPreserver accessPreserver) {
		this(originalMethod, originalClass, newClass, accessPreserver, emptyVector);
	}
	
	/**
	 * 
	 * @param originalMethod the method owning the code being examined
	 * @param originalClass the class containing original method
	 * @param newClass the class to which the code will be moved
	 * @param accessPreserver the object which will analyze and make necessary changes
	 */
	public AccessChecker(
			BT_Method originalMethod, 
			BT_Class originalClass, 
			BT_Class newClass, 
			AccessPreserver accessPreserver,
			BT_InsVector ignoreInstructions) {
		if(originalMethod.isStub() || originalMethod.isAbstract() || originalMethod.isNative()) {
			throw new IllegalArgumentException();
		}
		this.ignoreInstructions = ignoreInstructions;
		this.originalMethod = originalMethod;
		this.oldFrom = originalClass;
		this.newFrom = newClass;
		this.accessPreserver = accessPreserver;
	}
	
	public boolean makeLegal() {
		if(madeLegal) {
			return true;
		}
		if(!canMakeLegal()) {
			return false;
		}
		accessPreserver.doChanges();
		madeLegal = true;
		return true;
	}
	
	public boolean canMakeLegal() {
		checkAccess(accessPreserver);
		return canMakeLegal;
	}
	
	public boolean isLegal() {
		if(checkedCanMakeLegal) {
			//we have checked whether we can make changes to make sites legal
			return canMakeLegal 
				&& !accessPreserver.changesAreRequired();
		}
		if(checkedIsLegal) {
			//we have checked whether sites are legal, by checking if can make sites legal by changing nothing
			return canMakeLegal;
		}
		//check whether sites are legal, by checking if we can make sites legal by changing nothing
		checkLegal(new SimpleAccessPreserver(newFrom));
		if(canMakeLegal) {
			//no changes are needed to make sites legal
			checkedCanMakeLegal = true;
			return true;
		} 
		//sites are not legal
		checkedIsLegal = true;
		return false;
	}
	
	private void checkAccess(AccessPreserver changer) {
		if(checkedCanMakeLegal) {
			return;
		}
		checkedCanMakeLegal = true;
		checkLegal(changer);
	}
	
	private void checkLegal(AccessPreserver changer) {
		if(newFrom.equals(oldFrom)) {
			canMakeLegal = true;
			return;
		}
		
		BT_CodeAttribute code = originalMethod.getCode();
		BT_ClassReferenceSiteVector referencedClasses = code.referencedClasses;
		BT_AccessorVector accessedFields = code.accessedFields;
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		
		/* check if the code references anywhere outside itself */
		int totalSize = referencedClasses.size() + accessedFields.size() + calledMethods.size();
		if(totalSize == 0) {
			canMakeLegal = true;
			return;
		} 
		
		/* now we check to see if the original code breaks rules with these references */
		for (int i = 0; i < referencedClasses.size(); i++) {
			BT_ClassReferenceSite classSite = referencedClasses.elementAt(i);
			if(ignoreInstructions.contains(classSite.instruction)) {
				continue;
			}
			BT_Class to = classSite.getTarget();
			if(!to.isVisibleFrom(oldFrom)) {
				return;
			}
		}
		for(int i=0; i<accessedFields.size(); i++) {
			BT_Accessor accessor = accessedFields.elementAt(i);
			if(ignoreInstructions.contains(accessor.instruction)) {
				continue;
			}
			BT_Field to = accessor.getTarget();
			BT_Class throughClass = accessor.getClassTarget();
			if(!to.isVisibleFrom(oldFrom) || !throughClass.isVisibleFrom(oldFrom)) {
				return;
			}
			if(!originalMethod.isConstructor() 
					&& !originalMethod.isStaticInitializer() 
					&& to.isFinal() 
					&& !accessor.isFieldRead()) {
				return;
			}
		}
		for(int i=0; i<calledMethods.size(); i++) {
			BT_MethodCallSite site = calledMethods.elementAt(i);
			if(ignoreInstructions.contains(site.instruction)) {
				continue;
			}
			BT_Method to = site.getTarget();
			BT_Class throughClass = site.getClassTarget();
			if(!to.isVisibleFrom(oldFrom) || !throughClass.isVisibleFrom(oldFrom)) {
				return;
			}
		}
		
		/* the code looks fine, check to see if it can be migrated */
		try {
			canMakeLegal = changer.preserveAccess(code, ignoreInstructions);
		} catch(BT_CodeException e) {
			canMakeLegal = false;
			oldFrom.getRepository().factory.noteCodeException(e);
		}
	}
}
