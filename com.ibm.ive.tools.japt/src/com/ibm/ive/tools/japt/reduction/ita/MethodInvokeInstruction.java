package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;

public class MethodInvokeInstruction extends MethodInvoke {
	final SpecificMethodInvocation caller;
	final InstructionLocation callerLocation;
	final CallingContext currentContext;
	
	MethodInvokeInstruction(
			SpecificMethodInvocation caller,
			InstructionLocation callerLocation,
			Method called,
			Clazz targetClass) {
		super(caller.getMethod().getRepository(), called, targetClass);
		this.caller = caller;
		this.currentContext = caller.context;
		this.callerLocation = callerLocation;
	}
	
	CallingContext getInvokedContext(PropagatedObject obj) {
		return currentContext.getInvokedContext(obj, targetClass, called, caller, callerLocation);
	}
	
	boolean cannotBeFollowed() {
		return super.cannotBeFollowed() || currentContext.cannotBeFollowed(targetClass, called);
	}
	
	int getInvokedDepth() {
		return caller.getDepth() + 1;
	}
	 
	void propagateToLocation(ReceivedObject object, MethodInvocation target) {
		if(!target.hasInvoked(object, caller, callerLocation)) {
			target.addPropagatedObject(object, ObjectPropagator.INVOKED, caller);
		}
	}
	 
	MethodInvocation invokeInstanceMethod(CallingContext invokedContext, boolean isGeneric) {
		MethodInvocation invocation = caller.getPreviousCall(called, invokedContext, callerLocation, isGeneric);
		if(invocation == null) {
			invocation = invokeMethod(invokedContext, isGeneric);
		}
		return invocation;
	 }
	
	MethodInvocation invokeMethod(CallingContext invokedContext, boolean isGeneric) {
		EntryPointLister lister = rep.entryPointLister;
		if(lister != null) {
			Method callerMethod = caller.getMethod();
			Clazz callerDeclaringClass = callerMethod.getDeclaringClass();
			Clazz calledDeclaringClass = called.getDeclaringClass();
			JaptRepository japtRepository = rep.repository;
			if(!japtRepository.isInternalClass(callerDeclaringClass.getUnderlyingType())
				&& japtRepository.isInternalClass(calledDeclaringClass.getUnderlyingType())) {
					lister.foundEntryTo(called.getMethod(), callerMethod.getMethod());
			}
		}
		MethodInvocation methodInvocation = super.invokeMethod(invokedContext, isGeneric); 
		AccessedPropagator accessedInvocation = new AccessedPropagator(methodInvocation, callerLocation);
		caller.addCalledMethod(accessedInvocation);
		AccessedPropagator accessingMethod = new AccessedPropagator(caller, callerLocation);
		methodInvocation.addCallingMethod(accessingMethod);
		return methodInvocation;
	}
	
	public void invokeStaticMethod() {
		called.getDeclaringClass().enterVerifierRequiredClasses(currentContext);
		super.invokeStaticMethod();
	}
}
