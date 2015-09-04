package com.ibm.ive.tools.japt.reduction.ita;

public interface ContextProvider {	
	AllocationContext getGenericContext();
	
	CallingContext getJavaLangThreadContext();
	
	CallingContext getInitialCallingContext();
	
	CallingContext getInitializingContext();
	
	CallingContext getFinalizingContext(ConstructedObject object);
}
