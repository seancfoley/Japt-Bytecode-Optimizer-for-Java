/*
 * Created on Apr 26, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.coldMethod;

import com.ibm.ive.tools.japt.AccessPermissionsChanger;
import com.ibm.ive.tools.japt.AccessPreserver;
import com.ibm.ive.tools.japt.AccessorCreator;
import com.ibm.ive.tools.japt.MigratingReference;
import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSite;
import com.ibm.jikesbt.BT_ClassReferenceSiteVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_InstantiationLocator;
import com.ibm.jikesbt.BT_ItemReference;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_ConstructorLocator.Instantiation;

public class MigratedMethodAccessChanger implements AccessPreserver {
	private final BT_Class newFrom;
	private final BT_Method originalMethod;
	private final ExtensionRepository coldRep;
	final AccessPermissionsChanger accessPermissionsChanger;
	final AccessorCreator accessorCreator;
	private BT_ItemReference restrictedSite;
	
	public MigratedMethodAccessChanger(
			ExtensionRepository coldRep,
			BT_Method originalMethod, 
			BT_Class newClass, 
			boolean canChangePermissions, 
			boolean canUseAccessors) {
		if(coldRep.allowChangedPermissions) {
			accessPermissionsChanger = new AccessPermissionsChanger(newClass);
		} else {
			accessPermissionsChanger = null;
		}
		if(coldRep.allowAccessors) {
			accessorCreator = new AccessorCreator(
					newClass,
					originalMethod.getDeclaringClass(),
					originalMethod,
					coldRep.stackPool);
		} else {
			accessorCreator = null;
		}
		this.coldRep = coldRep;
		this.newFrom = newClass;
		this.originalMethod = originalMethod;
	}
	
	/**
	 * 
	 * @return
	 */
	BT_ItemReference getRestrictedSite() {
		return restrictedSite;
	}
		
	public boolean preserveAccess(BT_CodeAttribute code, BT_InsVector ignoreInstructions) throws BT_CodeException {
		
		//Instantiation instantiations[] = BT_ConstructorLocator.findInstantiations(originalMethod, coldRep.stackPool);
		//TODO verify the following call is working properly and finding the instantiations
		//TODO handle the case where a single new instruction has several corresponsing constructors
		Instantiation instantiations[] = BT_InstantiationLocator.findInstantiations(originalMethod, coldRep.stackPool);
		//boolean failed = false;
		for(int i=0; i<instantiations.length; i++) {
			Instantiation instantiation = instantiations[i];
			//if(!failed) {
				BT_Ins newIns = instantiation.creationSite.getInstruction();
				BT_Ins constructorIns = instantiation.site.getInstruction();
				boolean ignoreNew = ignoreInstructions.contains(newIns);
				boolean ignoreConstructor = ignoreInstructions.contains(constructorIns);
				if(ignoreNew || ignoreConstructor) {
					if(!ignoreNew) {
						//failed = true;
						restrictedSite = instantiation.creationSite;
						return false;
					} else if(!ignoreConstructor) {
						//failed = true;
						restrictedSite = instantiation.site;
						return false;
					}
				} else {
					MigratingReference newReference = new MigratingReference(instantiation.creationSite, newFrom);
					MigratingReference constructorReference = new MigratingReference(instantiation.site, newFrom);
					if(newReference.isAccessible() && constructorReference.isAccessible()) {
						ignoreInstructions.addElement(newIns);
						ignoreInstructions.addElement(constructorIns);
					} else if(coldRep.allowChangedPermissions 
							&& accessPermissionsChanger.canPreserveAccess(newReference)
							&& accessPermissionsChanger.canPreserveAccess(constructorReference)) {
						accessPermissionsChanger.doPreserveAccess(newReference);
						accessPermissionsChanger.doPreserveAccess(constructorReference);
						ignoreInstructions.addElement(newIns);
						ignoreInstructions.addElement(constructorIns);
					} else if(coldRep.allowAccessors 
									&& accessorCreator.preserveAccess(newReference, constructorReference, instantiation)) {
						//TODO add any other instructions as necessary that should now be ignored
						ignoreInstructions.addElement(newIns);
						ignoreInstructions.addElement(constructorIns);
					} else {
						if(newReference.isAccessible()) {
							restrictedSite = instantiation.site;
						} else {
							restrictedSite = instantiation.creationSite;
						}
						//failed = true;
						return false;
					}
				}
			//}
			//xxx cannot return the stacks for a multi instantiation locator until all stacks are analyzed;
			
			//instantiation.returnStacks();
		}
		//if(failed) {
		//	return false;
		//}
		
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		BT_ClassReferenceSiteVector referencedClasses = code.referencedClasses;
		BT_AccessorVector accessedFields = code.accessedFields;
		
		for (int i = 0; i < referencedClasses.size(); i++) {
			BT_ClassReferenceSite classSite = referencedClasses.elementAt(i);
			if(!preserveAccess(classSite, ignoreInstructions)) {
				restrictedSite = classSite;
				return false;
			}
		}
		
		for(int i=0; i<accessedFields.size(); i++) {
			BT_Accessor accessor = accessedFields.elementAt(i);
			if(!preserveAccess(accessor, ignoreInstructions)) {
				restrictedSite = accessor;
				return false;
			}
		}
		
		for(int i=0; i<calledMethods.size(); i++) {
			BT_MethodCallSite site = calledMethods.elementAt(i);
			if(!preserveAccess(site, ignoreInstructions)) {
				restrictedSite = site;
				return false;
			}
		}
		
		return true;
	}
	
	public boolean preserveAccess(BT_ItemReference reference, BT_InsVector ignoreInstructions) {
		return ignoreInstructions.contains(reference.getInstruction())
			|| preserveAccess(new MigratingReference(reference, newFrom));
	}
	
	public boolean preserveAccess(MigratingReference reference) {
		if(reference.isAccessible()) {
			return true;
		}
		if(coldRep.allowChangedPermissions && accessPermissionsChanger.canPreserveAccess(reference)) {
			accessPermissionsChanger.doPreserveAccess(reference);
			return true;
		}
		if(coldRep.allowAccessors && accessorCreator.preserveAccess(reference)) {
			return true;
		}
		return false;
	}
	
	public boolean changesAreRequired() {
		return (accessPermissionsChanger != null && accessPermissionsChanger.changesAreRequired()) 
			|| (accessorCreator != null && accessorCreator.changesAreRequired());
	}
	
	public void doChanges() {
		if(accessPermissionsChanger != null) {
			accessPermissionsChanger.doChanges();
		}
		if(accessorCreator != null) {
			accessorCreator.doChanges();
		}
	}
}
