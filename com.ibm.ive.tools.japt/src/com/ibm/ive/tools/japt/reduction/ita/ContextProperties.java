package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator.PropagationAction;
import com.ibm.jikesbt.BT_Class;

public class ContextProperties {
	public int allocationContextCount;
	
	public final AllocationContext HEAP = new AllocationContext(allocationContextCount++) {
		public BT_Class canBeReferencedBy(AllocationContext other) {
			return null;
		}
		
		public String toString() {
			return "heap memory";
		}	
	};
	
	/* This object defines a context in which all allocation are from the heap, and all method invocations
	 * have the same calling context.
	 */
	public final CallingContext SIMPLE_CONTEXT = new SimpleCallingContext();
	
	public class SimpleCallingContext implements CallingContext {
		
		public String toString() {
			return HEAP.toString();
		}
		
		public boolean isSame(CallingContext other) {
			return other == this;
		}
		
		public boolean cannotBeFollowed(Clazz targetClass, Method called) {
			return false;
		}
		
		public CallingContext getInvokedContext(
				PropagatedObject invokedObject,
				Clazz targetClass,
				Method invoked, 
				MethodInvocation from, 
				InstructionLocation fromLocation) {
			return this;
		}
		
		public AllocationContext getAllocationContext() {
			return HEAP;
		}
		
		public boolean readBarrier(
				PropagatedObject object,
				DataMember from,
				MethodInvocation reader, 
				InstructionLocation readerLocation,
				PropagationAction action) {
			return true;
		}
		
		public boolean writeBarrier(
				PropagatedObject object,
				PropagatedObject toObject,
				DataMember to,
				MethodInvocation writer, 
				InstructionLocation writerLocation,
				PropagationAction action) {
			return true;
		}
		
		public void enter(Clazz targetClass) {}
		
		public void enter(Clazz targetClass, Method method) {}
		
		public void enter(Clazz targetClass, Field method) {}
	}
	
	public class SimpleContextProvider implements ContextProvider {
		public AllocationContext getGenericContext() {
			return HEAP;
		}
		
		public CallingContext getInitialCallingContext() {
			return SIMPLE_CONTEXT;
		}
		
		public CallingContext getJavaLangThreadContext() {
			return SIMPLE_CONTEXT;
		}
		
		public CallingContext getInitializingContext() {
			return SIMPLE_CONTEXT;
		}
		
		public CallingContext getFinalizingContext(ConstructedObject object) {
			return SIMPLE_CONTEXT;
		}
	}
}
