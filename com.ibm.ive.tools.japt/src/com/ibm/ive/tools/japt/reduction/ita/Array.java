package com.ibm.ive.tools.japt.reduction.ita;

public interface Array extends ReceivedObject {
	ArrayElement getArrayElement();
	
	boolean isPrimitiveArray();
	
	Clazz getType();
}
