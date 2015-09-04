package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator.PropagationAction;

public interface CallingContext {
	
	boolean isSame(CallingContext other);
	
	int hashCode();
	
	boolean cannotBeFollowed(Clazz targetClass, Method called);
	
	/* enter this context for the first time by accessing the given class */
	void enter(Clazz targetClass);
	
	/* enter this context for the first time by invoking the given method */
	void enter(Clazz targetClass, Method method);
	
	/* enter this context for the first time by accessing the given field */
	void enter(Clazz targetClass, Field method);
	
	/* get the new context obtained when invoking the given method */
	CallingContext getInvokedContext(
			PropagatedObject invokedObject, 
			Clazz targetClass,
			Method invoked, 
			MethodInvocation from, 
			InstructionLocation fromLocation);
	
	AllocationContext getAllocationContext();
	
	/**
	 * 
	 * @param object
	 * @param from
	 * @param reader
	 * @return whether the read can proceed
	 */
	boolean readBarrier(
			PropagatedObject object, 
			DataMember from, 
			MethodInvocation reader, 
			InstructionLocation readerLocation,
			PropagationAction action);
	
	boolean writeBarrier(
			PropagatedObject object, 
			PropagatedObject toObject, 
			DataMember to, 
			MethodInvocation writer, 
			InstructionLocation writerLocation, 
			PropagationAction action);
	
}
