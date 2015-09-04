package com.ibm.ive.tools.japt.reduction.ita;


public class GenericInvocationException extends PropagationException {
	
	public GenericInvocationException(Method callingMethod, Method invokedMethod) {
		super(callingMethod, invokedMethod);
	}

	public GenericInvocationException(Method callingMethod, Method invokedMethod, String detailMessage) {
		super(callingMethod, invokedMethod, detailMessage);
	}
	
	public GenericInvocationException(Method invokedMethod, String detailMessage) {
		super(invokedMethod, detailMessage);
	}
}
