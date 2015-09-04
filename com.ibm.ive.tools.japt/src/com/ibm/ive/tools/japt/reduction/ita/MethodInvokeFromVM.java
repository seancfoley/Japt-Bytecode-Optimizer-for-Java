package com.ibm.ive.tools.japt.reduction.ita;


public class MethodInvokeFromVM extends MethodInvoke {

	final CallingContext invokedContext;
	
	public MethodInvokeFromVM(
			Repository rep, 
			CallingContext invokedContext, 
			Method called,
			Clazz targetClass) {
		super(rep, called, targetClass);
		if(invokedContext == null) {
			throw new NullPointerException();
		}
		this.invokedContext = invokedContext;
	}
	
	CallingContext getInvokedContext(PropagatedObject obj) {
		//invokedContext.enter(targetClass, called);
		return invokedContext;
	}
		
	int getInvokedDepth() {
		return 0;
	}
	
	public void invokeStaticMethod() {
		called.getDeclaringClass().enterVerifierRequiredClasses(invokedContext);
		super.invokeStaticMethod();
	}
}
