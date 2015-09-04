package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.reduction.ita.Method.ExceptionTableEntry;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_ExceptionTableEntry.Indices;

/**
 * @author sfoley
 * 
 * Represents a method invocation.
 * 
 * As an object propagator, it propagates objects to other method invocations, fields and array elements.
 * It propagates to methods that have been "invoked", methods that have "invoked" this method, fields that it
 * writes to, and array elements that it writes to.
 *
 */
public abstract class MethodInvocation extends ThrowingPropagator {

	/**
	 * Used to store in a linked list
	 */
	MethodInvocation next;
	
	public final Method method;
	final protected CallingContext context;
	ObjectSet createdObjects;//TODO get rid of this somehow if possible to save space
	static final ObjectSet emptyObjectSet = new ObjectSet();
	
	MethodInvocation(Method method, CallingContext context) {
		this.method = method;
		this.context = context;
	}
		
	public Method getMethod() {
		return method;
	}
	
	public Clazz getDefiningClass() {
		return method.getDeclaringClass();
	}
	
	public abstract ObjectSet getCreatedObjects();
	
	public abstract boolean isGeneric();
	
	protected abstract void addWrittenDataMember(AccessedPropagator memberWrittenTo);
	
	protected abstract void addCalledMethod(AccessedPropagator calledMethod);
	
	protected abstract void addCallingMethod(AccessedPropagator callingMethod);
	
	abstract MethodInvocation getPreviousCall(
			Method calledMethod,
			CallingContext invokedContext,
			InstructionLocation callLocation,
			boolean generic);
	
	public void setAccessed() {
		if(isAccessed()) {
			return;
		}
		super.setAccessed();
		if(method.isStatic()) {
			//TODO this call might be unnecessary, invokeStaticMethod might handle this, or probably should handle this
			method.getDeclaringClass().setInitialized();
		}
		method.setAccessed();
		method.findReferences(context);
		if(getRepository().getPropagationProperties().isReachabilityAnalysis() 
				/* escape analysis does not need to mark required items */
				/* rtsj analysis marks classes at other places */) {
			method.enterVerifierRequiredClasses(context);
		} 
	}
	
	boolean isSame(Method method) {
		return this.method.equals(method);
	}
	
	/**
	 * same as equals but type-safe
	 */
	boolean isSame(MethodInvocation propagator) {
		return this == propagator;
	}
	
	public String toString() {
		BT_Method member = (BT_Method) method.getMember();
		String res = "invocation of " + member.useName() + ' ' + PropagatedObject.toIdentifier(this);
		if(getRepository().getAllocationContextCount() > 1) {
			res += " with context " + context;
		}
		return res;
	}
	
	public Repository getRepository() {
		return method.getRepository();
	}
	
	public boolean useIntraProceduralAnalysis() {
		return method.useIntraProceduralAnalysis();
	}
	
	private boolean mapsToReturnInstruction(ReceivedObject obj) {
		if(isGeneric() || method.cannotBeFollowed() || !useIntraProceduralAnalysis()) {
			return true;
		}
		MethodInvocationLocation location = obj.getLocation();
		if(location == null) {
			/* conditionally created objects and others have no location and can go everywhere */
			return true;
		}
		InstructionLocation returnInstructions[] = method.getObjectReturns();
		for(int i=0; i<returnInstructions.length; i++) {
			InstructionLocation returnLocation = returnInstructions[i];
			if(method.maps(location, returnLocation, StackLocation.getTopCellIndex())) {
				return true;
			}
		}
		return false; 
	}
	
	private boolean isThrown(ReceivedObject obj) {
		PropagatedObject propagatedObject = obj.getObject();
		if(!propagatedObject.isThrowable()) {
			return false;
		}
		if(isGeneric() || method.cannotBeFollowed()) {
			return true;
		}
		if(!useIntraProceduralAnalysis()) {
			return true;
		}
		MethodInvocationLocation location = obj.getLocation();
		if(location == null) {
			/* conditionally created objects and others have no location and can go everywhere */
			return true;
		}
		InstructionLocation throwInstructions[] = method.getThrowInstructions();
		for(int i=0; i<throwInstructions.length; i++) {
			InstructionLocation throwLocation = throwInstructions[i];
			if(method.maps(location, throwLocation, StackLocation.getTopCellIndex())) {
				return true;
			}
		}
		return false; 
	}
	
	void propagateObjectToCallers(ReceivedObject obj, ArrayList callingMethods) {
		tryReturnNewObject(obj, callingMethods);
		tryThrowNewObject(obj, callingMethods);
	}
	
	public boolean hasReceivedArg(ReceivedObject obj, MethodInvocation from, InstructionLocation callerLocation) {
		return hasPropagated(obj);
	}
	
	public boolean hasInvoked(ReceivedObject obj, SpecificMethodInvocation from, InstructionLocation callerLocation) {
		return hasPropagated(obj);
	}
	
	private void tryReturnNewObject(ReceivedObject obj, ArrayList callingMethods) {
		Clazz returnType = method.getReturnType();
		if(returnType.isPrimitive()) {
			return;
		}
		PropagatedObject propObject = obj.getObject();
		boolean isInstance = propObject.mightBeInstanceOf(returnType);
		//boolean isInstance = propObject.isInstanceOf(returnType);
		if(!isInstance && !propObject.mightBeGenericInstanceOf(returnType)) {
			return;
		}
		boolean isReturned = mapsToReturnInstruction(obj);
		if(!isReturned) {
			return;
		}
		returnNewObject(callingMethods, obj, isInstance);
	}

	/**
	 * overidden in subclasses
	 * @param callingMethods
	 * @param obj
	 * @param isInstance
	 */
	protected void returnNewObject(ArrayList callingMethods, ReceivedObject obj, boolean isInstance) {
		PropagatedObject propObject = obj.getObject();
		PropagatedObject toSend;
		Clazz returnType = method.getReturnType();
		if(!isInstance) {
			GenericObject current = (GenericObject) propObject;
			GenericObject split = current.getSplitGeneric(returnType);
			toSend = split;
		
		} else {
			toSend = propObject;
		}
		
		Repository rep = getRepository();
		if(rep.getPropagationProperties().isEscapeAnalysis() && rep.isInterfaceMethod(this)) {
			rep.addUnreceivedReturned(propObject);
		}
		for(int j=0; callingMethods != null && j<callingMethods.size(); j++) {
			AccessedPropagator callingMethod = (AccessedPropagator) callingMethods.get(j);
			MethodInvocation methodInvocation = (MethodInvocation) callingMethod.propagator;
			ReceivedObject sentObject;
			if(methodInvocation.useIntraProceduralAnalysis()) {
				InstructionLocation location = callingMethod.location;
				/* note the object is on the top of the stack after method invocation instruction that receives it */
				
				/* see if the object is popped immediately, and if so, don't propagate */
				int nextInstructionIndex = location.instructionIndex + 1;
				if(objectIsPopped(methodInvocation.getMethod(), nextInstructionIndex, returnType)) {
					continue;
				}
				sentObject = new TargetedObject(toSend, 
						getRepository().locationPool.getStackLocation(nextInstructionIndex, StackLocation.getTopCellIndex()));
				
				
			} else {
				sentObject = toSend;
			}
			if(!methodInvocation.hasPropagated(sentObject)) {
				methodInvocation.addPropagatedObject(sentObject, ObjectPropagator.RETURNED, this);
			}
		}
	}
	
	private boolean objectIsPopped(Method caller, int instructionIndex, Clazz type) {
		BT_CodeAttribute code = caller.getCode();
		BT_InsVector insv = code.getInstructions();
		BT_Ins ins = insv.elementAt(instructionIndex);
		boolean result = type.getUnderlyingType().getOpcodeForPop() == ins.opcode;
		return result;
	}
	
	private void tryThrowNewObject(ReceivedObject obj, ArrayList callingMethods) {
		boolean isThrown = isThrown(obj);
		if(!isThrown) {
			return;
		}
		FieldObject propagatedObject = (FieldObject) obj.getObject();
		throwNewObject(propagatedObject, callingMethods);
	}
	
	void throwNewObject(FieldObject propagatedObject, ArrayList callingMethods) {
		boolean isInstanceOfDeclaredException = false;
		ArrayList acceptingDeclaredTypes = null;
		if(propagatedObject.isGeneric()) {
			Clazz declaredExceptions[] = method.getDeclaredExceptions();
			Repository rep = method.getRepository();
			for(int k=0; k<declaredExceptions.length + 2; k++) {
				Clazz type;
				switch(k) {
					case 0:
						type = rep.getJavaLangRuntimeException();
						break;
					case 1:
						type = rep.getJavaLangError();
						break;
					default:
						type = declaredExceptions[k - 2];
				}
				//isInstanceOfDeclaredException = type.isInstance(propagatedObject.getType());
				isInstanceOfDeclaredException = type.mightBeInstance(propagatedObject.getType());
				if(isInstanceOfDeclaredException || propagatedObject.mightBeGenericInstanceOf(type)) {
					if(acceptingDeclaredTypes == null) {
						acceptingDeclaredTypes = new ArrayList(declaredExceptions.length + 2);
					}
					acceptingDeclaredTypes.add(type);
					if(isInstanceOfDeclaredException) {
						/* 
						 * There is no overlap amongst the exceptions (since overlap was eliminated in the Method class), 
						 * so if we are an instance of one, we cannot be an instance of another 
						 */
						break;
					}
				}	
			}
		} else {
			isInstanceOfDeclaredException = method.canThrow(propagatedObject);
		}
		//if(callingMethods == null || callingMethods.size() == 0) {
			Repository rep = getRepository();
			/* here we throw from the initial method of the escape analysis */
			if(rep.getPropagationProperties().isEscapeAnalysis() && rep.isInterfaceMethod(this)) {
				if(isInstanceOfDeclaredException) {
					rep.addUncaughtException(propagatedObject);
				} else if(propagatedObject.isGeneric()) {
					for(int k=0; acceptingDeclaredTypes != null && k<acceptingDeclaredTypes.size(); k++) {
						Clazz type = (Clazz) acceptingDeclaredTypes.get(k);
						GenericObject current = (GenericObject) propagatedObject;
						GenericObject split = current.getSplitGeneric(type);
						rep.addUncaughtException(split);
					}
				}
			}
		//} else {
			for(int j=0; callingMethods != null && j<callingMethods.size(); j++) {
				AccessedPropagator callingMethod = (AccessedPropagator) callingMethods.get(j);
				if(isInstanceOfDeclaredException) {
					propagateThrownObject(propagatedObject, callingMethod);
				} else if(propagatedObject.isGeneric()) {
					for(int k=0; acceptingDeclaredTypes != null && k<acceptingDeclaredTypes.size(); k++) {
						Clazz type = (Clazz) acceptingDeclaredTypes.get(k);
						propagateThrownSplitGeneric((GenericObject) propagatedObject, callingMethod, type);
					}
				}
			}
		//}
	}
	
	void propagateThrownSplitGeneric(
			GenericObject current,
			AccessedPropagator callingMethod,
			Clazz asType
			) {
		GenericObject split = current.getSplitGeneric(asType);
		MethodInvocation propagatableMethod = (MethodInvocation) callingMethod.propagator;
		InstructionLocation callingMethodLocation = callingMethod.location;
		propagateThrownObject(split, propagatableMethod, callingMethodLocation);
	}
	
	void propagateThrownObject(FieldObject obj, AccessedPropagator callingMethod) {
		propagateThrownObject(obj, (MethodInvocation) callingMethod.propagator, callingMethod.location);
	}
	
	void propagateThrownObject(FieldObject obj, MethodInvocation propagatableMethod, InstructionLocation callingMethodLocation) {
		Method callingMeth = propagatableMethod.getMethod();
		int callingIndex = callingMethodLocation.instructionIndex;
		
		/* find the handlers for the object (there can be just one if the object is not generic */
		ExceptionTableEntry entries[] = callingMeth.getExceptionTable();
		boolean isInstance = false;
		for(int i=0; i<entries.length; i++) {
			ExceptionTableEntry entry = entries[i];
			Indices indices = entry.indices;
			if(callingIndex >= indices.start && callingIndex < indices.end) {
				Clazz catchType = entry.catchType;
				if(catchType == null) {
					break;
					/*
					 * technically, the exception object is placed on the operand 
					 * stack, so it is propagated, but the finally clause does not allow 
					 * the programmer to access any thrown exception objects, so we can
					 * assume that the object is not propagated to the method.
					 * 
					 * Note: generated bytecode might act differently.
					 */
				}
				Clazz objectType = obj.getType();
				//isInstance = catchType.isInstance(objectType);xx;
				isInstance = catchType.mightBeInstance(objectType);
				if(isInstance || obj.isGenericInstanceOf(objectType)) {
					ReceivedObject sentObject = null;
					boolean isPopped = false;
					if(callingMeth.useIntraProceduralAnalysis()) {
						InstructionLocation handlerLocation = entry.getCatchLocation();
						int instructionIndex = handlerLocation.instructionIndex;
						isPopped = objectIsPopped(callingMeth, instructionIndex, catchType);
						if(!isPopped) {
							/* note the object is on the top of the stack when caught */
							sentObject = new TargetedObject(obj.getObject(), 
								getRepository().locationPool.getStackLocation(instructionIndex, StackLocation.getTopCellIndex()));
								//new StackLocation(StackLocation.getTopCellIndex(), handlerLocation.instructionIndex));
						}
					} else {
						sentObject = obj.getObject();
					}
					if(!isPopped) {
						propagateToLocation(
								ObjectPropagator.CAUGHT,
								sentObject,
								propagatableMethod,
								catchType,
								isInstance);
					}
					if(isInstance) {
						break;
					}
				} /* instance check */	
			} /* within range */
		} /* for each handler */
		if(!isInstance) { /* the object(s) is not always caught */
			if(!propagatableMethod.isThrownPropagated(obj)) {
				propagatableMethod.addThrownPropagatedObject(obj, this);
			}
		}
	}
	
	void propagateArgument(
			ReceivedObject object,
			MethodInvocation target,
			Clazz asType,
			boolean isInstance,
			InstructionLocation fromLocation) {
		if(isInstance) {
			if(!target.hasReceivedArg(object, this, fromLocation)) {
				target.addPropagatedObject(object, INVOCATION_ARGUMENT, this);
			}
		} else {
			ReceivedObject sent = splitGeneric(object, asType);
			if(!target.hasReceivedArg(sent, this, fromLocation)) {
				target.addPropagatedObject(sent, INVOCATION_ARGUMENT, this);
			}
		}
	}
		
	void propagateToLocation(
			PropagationAction action,
			ReceivedObject object,
			ObjectPropagator target,
			Clazz asType,
			boolean isInstance) {
		if(isInstance) {
			if(!target.hasPropagated(object)) {
				target.addPropagatedObject(object, action, this);
			}
		} else {
			ReceivedObject sent = splitGeneric(object, asType);
			if(!target.hasPropagated(sent)) {
				target.addPropagatedObject(sent, action, this);
			}
		}
	}
	
	private static ReceivedObject splitGeneric(ReceivedObject object, Clazz asType) {
		ReceivedObject sent;
		GenericObject current = (GenericObject) object.getObject();
		GenericObject split = current.getSplitGeneric(asType);
		MethodInvocationLocation loc = object.getLocation();
		if(loc == null) {
			sent = split;
		} else {
			sent = new TargetedObject(split, loc);
		}
		return sent;
	}
	
	void propagateToDataMember(
			PropagatedObject object,
			MethodInvocationLocation receivedLocation,
			DataMember targetDataMember, /* a field or an array element */
			InstructionLocation targetLocation) {
		/* Now we check if we can propagate the object to the field itself */
		
		
		/* 
		 * No matter whether the field is a static field, a non-static field, or an array element, 
		 * the object must end up at the top of the stack to be propagated.
		 */
		if(!method.maps(receivedLocation, targetLocation, StackLocation.getTopCellIndex())) {
			return;
		}
		Clazz type = targetDataMember.getDataType();
		boolean isInstance = type.mightBeInstance(object.getType());
		if(isInstance || object.mightBeGenericInstanceOf(type)) {
			PropagatedObject owningObject = targetDataMember.getContainingObject();
			PropagationAction action = ObjectPropagator.WRITTEN;
			if(context.writeBarrier(object, owningObject, targetDataMember, this, targetLocation, action)) {
				if(!targetDataMember.hasPropagated(object)) {
					propagateToLocation(
						action,
						object,
						targetDataMember,
						type,
						isInstance);
				}
			}
		}
	}
	
	
}
