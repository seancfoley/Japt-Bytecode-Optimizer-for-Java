/*
 * Created on Feb 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.devirtualization;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSiteVector;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Method {
	
	private static Method[] nilArray = new Method[0];
	BT_Method method;
	BT_Method staticEquivalent;
	private JaptRepository repository;
	private DevirtualizeRepository devirtualizerRepository;
	int staticConversionCount;
	int specialConversionCount;
	
	/**
	 * 
	 */
	public Method(BT_Method method, JaptRepository repository, DevirtualizeRepository rep) {
		this.method = method;
		this.repository = repository;
		this.devirtualizerRepository = rep;
	}
	
	
	/**
	 * 
	 * @return any method for a which a new equivalent static method was created
	 */
	public Method[] convertInvokesToStatic() {
		BT_CodeAttribute code = method.getCode();
		if(code == null || !repository.isInternalClass(method.getDeclaringClass())) {
			return nilArray;
		}
		ArrayList result = new ArrayList();
		BT_MethodCallSiteVector referencedCallSites = (BT_MethodCallSiteVector) code.calledMethods.clone();
		for (int n = 0; n < referencedCallSites.size(); n++) {
			MethodCallSite callSite = new MethodCallSite(code, referencedCallSites.elementAt(n), repository, devirtualizerRepository.assumeUnknownVirtualTargets);

			if (!callSite.canDevirtualizeToStatic()) {
				continue;
			}
			
			BT_Method target = callSite.getVirtualCallSiteTarget();
			
			//in the case of a protected member, changing to static can make it visible
			/* e.g.
			package A;
			class A {
				protected void x() {}
			}
			package B;
			class B extends A {
				B() {
					x(); //legal
					new B().x(); //illegal, but if x() were static it would be legal
				}
			}
			*/
			if (target == null || !methodIsUnconditionallyVisible(target)) {
				continue;
			}
			Method methodTarget = devirtualizerRepository.getMethod(target);
			if (!methodTarget.canDevirtualizeToStatic()) {
				continue;
			}
			
			if (methodTarget.equals(this)) {
				BT_Method staticMethod = getEquivalentStaticMethod();
				if(staticMethod == null) {
					staticMethod = createEquivalentStaticMethod();
					result.add(this);
				}
				
				//Flush the body of the virtual method and
				// have this virtual method call the new static method
				replaceBodyWithMethodCall(staticMethod);
				return (Method[]) result.toArray(new Method[result.size()]);
			}
			
			BT_Method staticMethod = methodTarget.getEquivalentStaticMethod();
			if(staticMethod == null) {
				staticMethod = methodTarget.createEquivalentStaticMethod();
				result.add(methodTarget);
			}
			callSite.convertInvokeToStatic(target, staticMethod);
			//Inside method, invocation of target changed to invocation of staticMethod
			devirtualizerRepository.messages.DEVIRTUALIZED_STATIC.log(
					devirtualizerRepository.logger, 
					new String[] {target.useName(), method.useName(), staticMethod.useName()});
			staticConversionCount++;
			
		}

		if (staticConversionCount > 0) {
			code.computeMaxInstructionSizes();
		}
		return (Method[]) result.toArray(new Method[result.size()]);
	}
	
	public void convertInvokesToSpecial() {
		BT_CodeAttribute code = method.getCode();
		if(code == null || !repository.isInternalClass(method.getDeclaringClass())) {
			return;
		}
		
		BT_MethodCallSiteVector referencedMethods = code.calledMethods;
		for (int n = 0; n < referencedMethods.size(); n++) {
			MethodCallSite callSite = new MethodCallSite(code, referencedMethods.elementAt(n), 
					repository, devirtualizerRepository.assumeUnknownVirtualTargets);
			if (!callSite.canDevirtualizeToSpecial()) {
				continue;
			}
			BT_Method target = callSite.getVirtualCallSiteTarget();
			if (target == null || !methodIsVisible(target)) {
				continue;
			}
			Method methodTarget = devirtualizerRepository.getMethod(target);
			if (!methodTarget.canDevirtualizeToSpecial()) {
				continue;
			}
			callSite.convertInvokeToSpecial(target);
			devirtualizerRepository.messages.DEVIRTUALIZED_SPECIAL.log(
					devirtualizerRepository.logger, new String[] {target.useName(), method.useName()});
			specialConversionCount++;
		}
		if (specialConversionCount > 0) {
			code.computeMaxInstructionSizes();
		}
	}
	
	boolean methodIsVisible(BT_Method referencedMethod) {
		BT_Class fromClass = method.getDeclaringClass();
		return referencedMethod.getDeclaringClass().isVisibleFrom(fromClass) 
		 && referencedMethod.isVisibleFrom(fromClass);
	}
	
	boolean methodIsUnconditionallyVisible(BT_Method referencedMethod) {
		BT_Class fromClass = method.getDeclaringClass();
		return referencedMethod.getDeclaringClass().isVisibleFrom(fromClass) 
		 && referencedMethod.isUnconditionallyVisibleFrom(fromClass);
	}
	
	private boolean canDevirtualize() {
		//when a method is specified it maybe an implicit specification:
		//ie the actual entry point is a child of the method that is actually 
		//specified.  Therefore we assume specified methods have been overridden.
		return !repository.getInternalClassesInterface().isInEntireInterface(method)
				&& !method.isConstructor()
				&& !method.isAbstract()
				&& !method.isStub()
				&& !method.isStatic();
	}
	
	private boolean canDevirtualizeToStatic() {
		//remember not to remove the old referenced method if it overrides something else
		return canDevirtualize()
			&& repository.isInternalClass(method.getDeclaringClass())
			&& !method.isNative() 
			&& !method.isSynchronized()
			&& repository.getRelatedMethodMap().getAllParents(method).isEmpty();
	}
	
	private boolean canDevirtualizeToSpecial() {
		return canDevirtualize();
	}
	
	BT_Method getEquivalentStaticMethod() {
		return staticEquivalent;
	}
	
	private BT_Method createEquivalentStaticMethod() {
		staticEquivalent = method.copyMethodTo(method.getDeclaringClass(), true);
		return staticEquivalent;
	}
	
	private void replaceBodyWithMethodCall(BT_Method toMethod) {
		 method.replaceBodyWithMethodCall(toMethod);
	}
	
	public String toString() {
		return method.useName();
	}
}
