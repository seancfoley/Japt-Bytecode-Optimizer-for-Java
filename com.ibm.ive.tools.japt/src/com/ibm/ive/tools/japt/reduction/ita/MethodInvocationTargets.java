package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;
import com.ibm.jikesbt.BT_MethodSignature;



/**
 * @author sfoley
 *
 */
public class MethodInvocationTargets {

	/**
	 * Objects can be propagated as the return type of a method to the caller.
	 * Also, exception objects can be thrown from one method to another.
	 * 
	 * Contains instances of MethodPropagator
	 */
	private ArrayList callingMethods; //contains AccessedPropagator (the instruction and instruction index refer to the calling method)
	
	/**
	 * Objects are propagated as arguments in a method call.
	 * 
	 * Contains instances of MethodPropagator
	 */
	ArrayList calledMethods; //contains AccessedPropagator
	
	/**
	 * Objects are propagated when writing to a DataMember.
	 * 
	 * Contains instances of DataMember
	 */
	private ArrayList writtenMembers; //contains AccessedPropagator
	
	void addWrittenDataMember(AccessedPropagator memberWrittenTo) {
		if(writtenMembers == null) {
			writtenMembers = new ArrayList(1);
		}
		writtenMembers.add(memberWrittenTo);
	}
	
	AccessedPropagator getCallTo(
			Method calledMethod,
			CallingContext invokedContext,
			InstructionLocation callLocation,
			boolean generic, 
			MethodInvocation method) {
		if(calledMethods == null) {
			return null;
		}
		for(int i=0; i<calledMethods.size(); i++) {
			AccessedPropagator access = (AccessedPropagator) calledMethods.get(i);
			MethodInvocation call = (MethodInvocation) access.propagator;
			if(method.getRepository().getPropagationProperties().useIntraProceduralAnalysis() 
					&& !access.location.equals(callLocation)) {
				continue;
			}
			if(call.getMethod().equals(calledMethod) && generic == call.isGeneric() && invokedContext.isSame(call.context)) {
				return access;
			}
		}
		return null;
	}
	
	void addCalledMethod(AccessedPropagator calledMethod) {
		if(calledMethods == null) {
			calledMethods = new ArrayList(1);
		}
		calledMethods.add(calledMethod);
	}
	
	void addCallingMethod(AccessedPropagator callingMethod) {
		if(callingMethods == null) {
			callingMethods = new ArrayList(1);
		}
		callingMethods.add(callingMethod);
	}
	
	void migrate(MethodInvocationTargets to) {
		if(to.callingMethods == null) {
			to.callingMethods = callingMethods;
			callingMethods = null;
		}
		else if(callingMethods != null) {
			to.callingMethods.addAll(callingMethods);
			callingMethods = null;
		}
		if(to.calledMethods == null) {
			to.calledMethods = calledMethods;
			calledMethods = null;
		}
		else if(calledMethods != null) {
			to.calledMethods.addAll(calledMethods);
			calledMethods = null;	
		}
		if(to.writtenMembers == null) {
			to.writtenMembers = writtenMembers;
			writtenMembers = null;
		}
		else if(writtenMembers != null) {
			to.writtenMembers.addAll(writtenMembers);
			writtenMembers = null;
		}
	}
	
	boolean isEmpty() {
		return (callingMethods == null || callingMethods.size() == 0)
			&& (calledMethods == null || calledMethods.size() == 0)
			&& (writtenMembers == null || writtenMembers.size() == 0);
	}
	
	/**
	 * We know that the given object can be present in the stack of this method.  So
	 * we know that it can then be propagated by this method to any field written to or any method called
	 * in the body of this method, or any method which catches a throwable object thrown by this method.
	 */
	void propagateObject(ReceivedObject obj, MethodInvocation method) {
		MethodInvocationLocation origin = obj.getLocation();
		PropagatedObject object = obj.getObject();
		if(writtenMembers != null) {
			int size = writtenMembers.size();
			for(int i=0; i<size; i++) {
				AccessedPropagator access = (AccessedPropagator) writtenMembers.get(i);
				method.propagateToDataMember(
						object,
						origin,
						(DataMember) access.propagator,
						access.location);
			}
		}
		
		/*
		 * we check if we are propagating an object whose
		 * type matches a method parameter. 
		 */
		if(calledMethods != null) {
			int size = calledMethods.size();
			Method caller = method.getMethod();
			for(int i=0; i<size; i++) {
				AccessedPropagator access = (AccessedPropagator) calledMethods.get(i);
				MethodInvocation propagatableMethod = (MethodInvocation) access.propagator;
				InstructionLocation location = access.location;
				Method called = propagatableMethod.method;
				BT_MethodSignature calledSig = called.getSignature();
				Clazz paramTypes[] = called.typesPropagatable;
				for(int n=0; n<paramTypes.length; n++) {
					Clazz argType = paramTypes[n];
					if(argType == null) {
						continue;
					}
					//boolean isInstance = argType.isInstance(object.getType());
					boolean isInstance = argType.mightBeInstance(object.getType());
					if(!isInstance && !object.mightBeGenericInstanceOf(argType)) {
						continue;
					}
					if(caller.useIntraProceduralAnalysis()) {
						int stackIndex = StackLocation.getParamCellIndex(calledSig, n);
						if(caller.maps(origin, location, stackIndex)) {
							ReceivedObject sentObject = new TargetedObject(object, 
									method.getRepository().locationPool.getParamLocation(n + (called.isStatic() ? 0 : 1)));
							method.propagateArgument(
									sentObject,
									propagatableMethod,
									argType,
									isInstance,
									location);
						}
					} else {
						method.propagateArgument(
								object,
								propagatableMethod,
								argType,
								isInstance,
								location);
					}	
				}
			}
		}
		method.propagateObjectToCallers(obj, callingMethods);
	}
	
	/**
	 * Propagate an object which never actually exists on the method stack. The object is
	 * thrown from a called method and is never caught within this method (or is caught by 
	 * a finally cause and rethrown).  These objects must be propagated to methods which
	 * call this method and nowhere else.
	 */
	void propagateThrownObject(FieldObject obj, MethodInvocation method) {
		method.throwNewObject(obj, callingMethods);
	}
}
