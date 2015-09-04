package com.ibm.ive.tools.japt.reduction.ita;


public class VirtualMethodInvoke {
	final Repository rep;
	final SpecificMethodInvocation caller;
	final CallingContext invokedContext;
	final InstructionLocation callerLocation;
	final Method instructionTargetMethod;
	final Clazz targetClass;
	
	VirtualMethodInvoke(Repository rep, CallingContext invokedContext, Method target, Clazz targetClass) {
		this(rep, null, null, invokedContext, target, targetClass);
	}
	
	VirtualMethodInvoke(SpecificMethodInvocation caller, InstructionLocation callerLocation, Method target, Clazz targetClass) {
		this(caller.getRepository(), caller, callerLocation, null, target, targetClass);
	}
	
	private VirtualMethodInvoke(
			Repository rep, 
			SpecificMethodInvocation caller, 
			InstructionLocation callerLocation,
			CallingContext invokedContext,
			Method target, 
			Clazz targetClass) {
		this.rep = rep;
		this.instructionTargetMethod = target;
		this.targetClass = targetClass;
		this.caller = caller;
		this.callerLocation = callerLocation;
		this.invokedContext = invokedContext;
	}
	
	public void invokeVirtualMethod(PropagatedObject object) {
		Clazz objectType = object.getType();
		Clazz declaringClass = instructionTargetMethod.getDeclaringClass();
		instructionTargetMethod.setRequired();
		
		 /* check if the instance of the declaring class overrides or implements the callable method */
		Method invokedBaseMethod;
		if(objectType.isSame(declaringClass) 
			|| !instructionTargetMethod.isOverridable() 
			|| (invokedBaseMethod = instructionTargetMethod.getOverridingMethod(objectType)) == null) {
			invokedBaseMethod = instructionTargetMethod;
		}
		invokeMethod(object, targetClass, invokedBaseMethod);
		if(object.isGeneric()) {
			/* 
			 * We have chosen to avoid a generic invocation for generic objects being invoked.
			 * Instead we are trying a virtual invocation.
			 * 
			 * This may have been done because we are aware of all overriding methods,
			 * because the method's package is closed.
			 * 
			 * Also, RTSJ analysis may choose to use generic objects, which have no memory area, and to not use
			 * generic invocations, which have less benefit for RTSJ analysis.
			 * 
			 * So here we attempt an instance invocation for every possible target known for the virtual invocation.
			 */
			Method additionals[] = invokedBaseMethod.getOverridingMethods();
			for(int k=0; k<additionals.length; k++) {
				Method additional = additionals[k];
				invokeMethod(object, targetClass, additional);
			}
		}	
	 }

	private void invokeMethod(PropagatedObject object, Clazz targetClass,
			Method additional) {
		MethodInvoke invoker;
		if(caller == null) {
			invoker = new MethodInvokeFromVM(rep, invokedContext, additional, targetClass);
		} else {
			invoker = new MethodInvokeInstruction(caller, callerLocation, additional, targetClass);
		}
		invoker.invokeInstanceMethod(object, false);
	}
}
