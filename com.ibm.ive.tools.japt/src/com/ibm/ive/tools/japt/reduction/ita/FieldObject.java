package com.ibm.ive.tools.japt.reduction.ita;

public interface FieldObject extends ReceivedObject {

	FieldInstance getFieldInstance(Field field);
	
	boolean isThrowable();
	
	boolean isRuntimeThrowable();
	
	Clazz getType();
	
	boolean isGeneric();
	
	boolean isGenericInstanceOf(Clazz type);
	
	boolean mightBeGenericInstanceOf(Clazz type);
	
	public boolean isInstanceOf(Clazz type);
	
	public boolean mightBeInstanceOf(Clazz type);
}
