/*
 * Created on Apr 25, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_ItemReference;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_MemberVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

public class AccessPermissionsChanger extends SimpleAccessPreserver {
	BT_ClassVector invisibleClasses;
	BT_FieldVector finalMembers;
	BT_MemberVector invisibleMembers;
	JaptRepository repository;
	
	public AccessPermissionsChanger(BT_Class newClass) {
		super(newClass);
		repository = (JaptRepository) newClass.getRepository();
	}
	
	public boolean canChangeMemberAccess(BT_Member member) {
		if(member instanceof BT_Method) {
			return canChangeMethodAndKids((BT_Method) member);
		} else {
			return canChangeClass(member.getDeclaringClass());
		}
	}
	
	public boolean canChangeClass(BT_Class clazz) {
		return repository.isInternalClass(clazz);
	}
	
	public boolean canChangeMethodAndKids(BT_Method method) {
		/* 
		 * Note that we know the method is not a private non-static non-constructor method 
		 * because we have ruled that out in canPreserveAccess.
		 * 
		 * The becomeVisibleFrom method will therefore change the base method to public.  So we must ensure the method
		 * and its kids can all become public.
		 * 
		 */
		if(!canChangeClass(method.getDeclaringClass())) {
			return false;
		}
		BT_MethodVector kids = method.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Method kid = kids.elementAt(i);
			if(!kid.isPublic() && !canChangeMethodAndKids(kid)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canChangeFinalStatus(BT_Field field) {
		return canChangeClass(field.getDeclaringClass());
	}
	
	public boolean preserveAccess(MigratingReference reference) {
		if(canPreserveAccess(reference)) {
			doPreserveAccess(reference);
			return true;
		}
		return false;
	}
	
	public boolean canPreserveAccess(MigratingReference reference) {
		BT_Class throughClass = reference.site.getClassTarget();
		BT_Member target = reference.getMemberTarget();
		if(reference.siteIsSpecialInvocation() && !reference.siteIsConstructorInvocation()) {
			/* we cannot increase the access permissions for an invokespecial unless it is a constructor */
			/* if it is a private invocation, we cannot increase permissions because that changes the invocation instruction */
			/* if it is a super invocation, the invocation instruction cannot be moved to a different class, it must remain in a subclass of the invoked method */
			return false;
		}
		boolean siteClassIsVisible = reference.siteClassIsVisible();
		if(!siteClassIsVisible && !canChangeClass(throughClass)) {
			return false;
		}
		boolean siteIsVisible = reference.siteIsUnconditionallyVisible();
		if(!siteIsVisible && !(canChangeClass(target.getDeclaringClass()) && canChangeMemberAccess(target))) {
			return false;
		}
		boolean siteWritesToFinal = reference.siteWritesToFinal();
		if(siteWritesToFinal && !(canChangeClass(target.getDeclaringClass()) && canChangeFinalStatus((BT_Field) target))) {
			return false;
		}
		return true;
	}
	
	public void doPreserveAccess(MigratingReference reference) {
		if(!reference.siteIsUnconditionallyVisible()) {
			increaseVisibilityOf(reference.getMemberTarget(), reference.site);
		}
		if(reference.siteWritesToFinal()) {
			changeFinalStatusOf((BT_Field) reference.getMemberTarget(), reference.site);
		}
		if(!reference.siteClassIsVisible()) {
			increaseVisibilityOf(reference.site.getClassTarget(), reference.site);
		}
	}
	
	void increaseVisibilityOf(BT_Class clazz, BT_ItemReference site) {
		if(invisibleClasses == null) {
			invisibleClasses = new BT_ClassVector(
					site.from.accessedFields.size() + site.from.calledMethods.size() + site.from.referencedClasses.size() 
					/* no resizes will be required */);
			invisibleClasses.addElement(clazz);
		} else {
			invisibleClasses.addUnique(clazz);
		}
	}
	
	void increaseVisibilityOf(BT_Member member, BT_ItemReference site) {
		if(invisibleMembers == null) {
			invisibleMembers = new BT_MemberVector(site.from.accessedFields.size() + site.from.calledMethods.size() 
					/* no resizes will be required */);
			invisibleMembers.addElement(member);
		} else {
			invisibleMembers.addUnique(member);
		}
		
	}
	
	void changeFinalStatusOf(BT_Field member, BT_ItemReference site) {
		if(finalMembers == null) {
			finalMembers = new BT_FieldVector(site.from.accessedFields.size()/* no resizes will be required */);
			finalMembers.addElement(member);
		} else {
			finalMembers.addUnique(member);
		}
	}
	
	public boolean changesAreRequired() {
		return invisibleClasses != null 
			|| finalMembers != null 
			|| invisibleMembers  != null;
	}
	
	public void doChanges() {
		if(invisibleClasses != null) {
			JaptFactory factory = repository.getFactory();
			for(int i=0; i<invisibleClasses.size(); i++) {
				BT_Class clazz = invisibleClasses.elementAt(i);
				short oldPermission = clazz.getAccessPermission();
				clazz.becomeVisibleFrom(newFrom);
				factory.noteAccessPermissionsChanged(clazz, oldPermission, newFrom);
			}
			invisibleClasses = null;
		}
		if(invisibleMembers != null) {
			JaptFactory factory = repository.getFactory();
			for(int i=0; i<invisibleMembers.size(); i++) {
				BT_Member member = invisibleMembers.elementAt(i);
				if(member instanceof BT_Method) {
					setVisible((BT_Method) member);
				} else {
					short oldPermission = member.getAccessPermission();
					member.becomeVisibleFrom(newFrom);
					factory.noteAccessPermissionsChanged(member, oldPermission, newFrom);
				}
			}
			invisibleMembers = null;
		}
		if(finalMembers != null) {
			JaptFactory factory = repository.getFactory();
			for(int i=0; i<finalMembers.size(); i++) {
				BT_Member member = finalMembers.elementAt(i);
				factory.noteFinalAccessChanged(member, newFrom);
				member.setFinal(false);
			}
			finalMembers = null;
		}
	}
	
	private void setVisible(BT_Method method) {
		short oldPermission = method.getAccessPermission();
		method.becomeVisibleFrom(newFrom);
		repository.getFactory().noteAccessPermissionsChanged(method, oldPermission, newFrom);
		BT_MethodVector kids = method.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Method kid = kids.elementAt(i);
			if(!kid.isPublic()) {
				setVisible(kid);
			}
		}
	}
	
	
}
