/*
 * Created on May 4, 2007
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
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;

public class SimpleAccessPreserver implements AccessPreserver {
	
	final BT_Class newFrom;
	
	public SimpleAccessPreserver(BT_Class newFrom) {
		this.newFrom = newFrom;
	}
	
	public boolean preserveAccess(MigratingReference reference) {
		return reference.isAccessible();
	}
	
	public boolean preserveAccess(BT_CodeAttribute code, BT_InsVector ignoreInstructions) {
		BT_ClassReferenceSiteVector referencedClasses = code.referencedClasses;
		BT_AccessorVector accessedFields = code.accessedFields;
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		
		for (int i = 0; i < referencedClasses.size(); i++) {
			BT_ClassReferenceSite classSite = referencedClasses.elementAt(i);
			if(ignoreInstructions.contains(classSite.instruction)) {
				continue;
			}
			if(!preserveAccess(new MigratingReference(classSite, newFrom))) {
				return false;
			}
		}
		
		for(int i=0; i<accessedFields.size(); i++) {
			BT_Accessor accessor = accessedFields.elementAt(i);
			if(ignoreInstructions.contains(accessor.instruction)) {
				continue;
			}
			if(!preserveAccess(new MigratingReference(accessor, newFrom))) {
				return false;
			}
		}
		
		for(int i=0; i<calledMethods.size(); i++) {
			BT_MethodCallSite site = calledMethods.elementAt(i);
			if(ignoreInstructions.contains(site.instruction)) {
				continue;
			}
			if(!preserveAccess(new MigratingReference(site, newFrom))) {
				return false;
			}
		}
		return true;
	}
	
	public boolean changesAreRequired() {
		return false;
	}
	
	public void doChanges() {
		return;
	}
}
