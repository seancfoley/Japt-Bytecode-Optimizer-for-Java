/*
 * Created on May 24, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSite;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_ItemReference;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_MethodCallSite;

/**
 * 
 * @author sfoley
 *
 * This class describes the movement of a class/field/method reference from a given class to a separate distinct class.
 * In other words, the referencing class is changing, and we may wish to ask changes about the reference regarding its 
 * status at the new location.  The two locations (original and new) are within distinct classes, not the same class.
 */
public class MigratingReference {
	final BT_ItemReference site;
	final BT_Class newFrom;
	
	private Boolean siteClassIsVisible;
	private Boolean siteIsUnconditionallyVisible;
	private Boolean siteIsFinalWrite;
	private Boolean siteIsSpecial;
	private Boolean siteIsConstructor;
	private Boolean siteIsStatic;
	
	public MigratingReference(BT_ItemReference site, BT_Class newFrom) {
		this.site = site;
		this.newFrom = newFrom;
	}
	
	public String toString() {
		return " migration of " + site + " to " + newFrom;
	}
	
	public boolean isClassReference() {
		return site.isClassReference();
	}
	
	public BT_Class getClassTarget() {
		return site.getClassTarget();
	}
	
	public BT_Member getMemberTarget() {
		if(site instanceof BT_ClassReferenceSite) {
			return null;
		} 
		if(site instanceof BT_Accessor) {
			return ((BT_Accessor) site).getTarget();
		}
		return ((BT_MethodCallSite) site).getTarget();
	}
	
	public boolean siteClassIsVisible() {
		if(siteClassIsVisible == null) {
			BT_Class throughClass = site.getClassTarget();
			boolean val = throughClass.isVisibleFrom(newFrom);
			siteClassIsVisible = val ? Boolean.TRUE : Boolean.FALSE;
		}
		return siteClassIsVisible.booleanValue();
	}
	
	/*
	Invocations to private methods and invocations to super classes 
	using invokespecials cannot be moved to a separate class.
	
	An invokespecial to a superclass should never be moved to
	a class outside of the class it is in, unless that class is also a subclass
	of the same parent class, and the invokespecial is performed on the "this" object
	in both cases...  
	This will cause errors with the some verifiers, unless it is a constructor.
	Technically there is no reason why this is not appropriate. Even though it would
	not be possible to generate such code with a compiler, the code should still work fine,
	but we can't argue with a verifier.
	
	For example, if class C attempts to inline B.m, and B.m has a call to superclass
	method A.n, then that will be changed to an invokespecial of A.n from C.  But this
	now means that the call becomes overridable (virtual) and should then be an 
	invokevirtual, but then that can change program behaviour because it becomes overridable.  
	If C is also a subclass of A, then unless it has the exact same parent as B we cannot assure there
	will be an intervening overriding method (invokespecials must check for this occurrence)
	
	
	The following scenario is the only one that is OK:
	 class A extends B {
	 	void a() {
	 		amethod(); //we can inline this call
	 		//even though amethod calls somemethod, because somemethod is in a parent class
	 		//of this class (since this class extends B) and the somemethod call will be performed on 
	 		//the "this" object after the inlining
	 		//in addition, there is no intervening overriding method (ie there is no somemethod() defined in 
	 		//class B 
	 		
	 		new B().amethod(); //we cannot inline this call
	 		 //the invokespecial instruction prevents this
	 		 //the method somemethod is invoked on the B instance here and the "this" object in amethod,
	 		 //it needs to be the "this" object in both places
	 	}
	 }
	 
	 class B extends C {
	 	void amethod() {
	 		super.somemethod();
	 	}
	 }
	 
	 class D extends C {
	 	void d() {
	 		new B().amethod(); //we cannot inline this call
	 		 //the invokespecial instruction prevents this
	 		 //the method somemethod is invoked on the B instance here and the "this" object in amethod,
	 		 //it needs to be the "this" object in both places
	 		 //this inline is illegal even though we can be sure that the invokspecial somemethod call resolves to the same
	 		 //method whether called from here or from amethod
	 	}
	 }
	 
	 //class A does not need to extend B but needs to be a subclass of B, and there must be no
	 //intervening overriding method of C.somemethod
	 
	 //there are also possible visibility problems but they are checked elsewhere
	   
	 //TODO a variation of the BT_StackShapeVisitor could handle these checks
	  */
	
	/**
	 * @return whether the site is an invokespecial.
	 */
	public boolean siteIsSpecialInvocation() {
		if(siteIsSpecial == null) {
			boolean val = site.getInstruction().isInvokeSpecialIns();
			siteIsSpecial = val ? Boolean.TRUE : Boolean.FALSE;
		}
		return siteIsSpecial.booleanValue();
	}
	
	/**
	 * 
	 * @return whether the site references a static method or field
	 */
	public boolean siteIsStaticReference() {
		if(siteIsStatic == null) {
			BT_Ins ins = site.getInstruction();
			boolean val = ins.isInvokeStaticIns() || ins.isStaticFieldAccessIns();
			siteIsStatic = val ? Boolean.TRUE : Boolean.FALSE;
		}
		return siteIsStatic.booleanValue();
	}
	
	/**
	 * @return whether the site invokes a constructor.
	 */
	public boolean siteIsConstructorInvocation() {
		if(siteIsConstructor == null) {
			boolean val = getMemberTarget().isConstructor();
			siteIsConstructor = val ? Boolean.TRUE : Boolean.FALSE;
		}
		return siteIsConstructor.booleanValue();
	}
	
	/**
	 * @return whether the site access is visible from the new class.
	 */
	public boolean siteIsUnconditionallyVisible() {
		/* 
		 * Note that whether a constructor is protected is irrelevant since we never move a super constructor call,
		 * so we use isUnconditionallyVisibleFrom which treats protected constructors like package-access constructors.
		 * 
		 * Also note that if we are migrating a protected item to a new class, then we are making the assumption that it
		 * is not also a subclass of the same class, and therefore protected access through a subclass is irrelevant.
		 */
		if(siteIsUnconditionallyVisible == null) {
			BT_Member to = getMemberTarget();
			boolean val = (to == null || to.isUnconditionallyVisibleFrom(newFrom));
			siteIsUnconditionallyVisible = val ? Boolean.TRUE : Boolean.FALSE;
		}
		return siteIsUnconditionallyVisible.booleanValue();
	}
	
	public boolean siteWritesToFinal() {
		if(siteIsFinalWrite == null) {
			if(site instanceof BT_Accessor) {
				BT_Accessor accessor = (BT_Accessor) site;
				BT_Field to = accessor.getTarget();
				boolean val = to.isFinal() && !accessor.isFieldRead();
				siteIsFinalWrite = val ? Boolean.TRUE : Boolean.FALSE;
			} else {
				siteIsFinalWrite = Boolean.FALSE;
			}
		}
		return siteIsFinalWrite.booleanValue();
	}
	
	public boolean isAccessible() {
		return siteClassIsVisible() && siteIsUnconditionallyVisible() && !siteWritesToFinal() && (!siteIsSpecialInvocation() || siteIsConstructorInvocation());
	}
	
}
