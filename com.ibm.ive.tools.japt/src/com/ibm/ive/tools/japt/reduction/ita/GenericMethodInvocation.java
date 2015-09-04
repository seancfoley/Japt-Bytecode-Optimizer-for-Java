package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

/**
 * @author sfoley
 * 
 * Represents a generic method invocation.
 * 
 * Represents a method invocation for which we cannot determine the body.
 * 
 * Generic method invocations propagate objects nowhere, because we cannot analyze where they go.
 * For a reachability analysis the existence of generic invocations is problematic.  For an escape
 * analysis, a generic invocation is a source of escape.
 *
 */
public class GenericMethodInvocation extends MethodInvocation {
	
	GenericMethodInvocation(Method method, CallingContext context) {
		super(method, context);
	}
	
	ArrayList oldCallingMethods = new ArrayList(1); //contains AccessedPropagator, but will contain at most one element
	ArrayList newCallingMethods = new ArrayList(1); //contains AccessedPropagator, but will contain at most one element
	GenericObject returnedObject;
	GenericObject thrownObject;
	
	void initializePropagation() {
		ObjectSet createdObjs = new ObjectSet();
		boolean storeCreated = getRepository().getPropagationProperties().storeCreatedInMethodInvocations;
		if(storeCreated) {
			createdObjs = new ObjectSet();
		}
		Clazz returnType = method.getReturnType();
		Repository rep = getRepository();
		if(!returnType.isPrimitive()) {
			returnedObject = returnType.instantiateGeneric(rep.getPropagationProperties().provider.getGenericContext());
			if(!hasPropagated(returnedObject)) {
				addInstantiatedObject(returnedObject);
				returnType.addCreated(returnedObject);
				if(storeCreated) {
					CreatedObject created = new CreatedObject(returnedObject, this, null);
					createdObjs.add(created);
				}
			}
			
		}
		Clazz type = rep.getJavaLangThrowable();
		thrownObject = type.instantiateGeneric(rep.getPropagationProperties().provider.getGenericContext());
		if(!hasPropagated(thrownObject)) {
			addInstantiatedObject(thrownObject);
			type.addCreated(thrownObject);
			if(storeCreated) {
				CreatedObject created = new CreatedObject(thrownObject, this, null);
				createdObjs.add(created);
			}
		}
		if(storeCreated) {
			if(createdObjs.size() > 0) {
				createdObjects = createdObjs;
			} else {
				createdObjects = emptyObjectSet;
			}
		}
	}
	
	public ObjectSet getCreatedObjects() {
		return createdObjects;
	}
	
	private boolean isInitialGenericObject(ReceivedObject obj) {
		PropagatedObject object = obj.getObject();
		return object.equals(returnedObject) || object.equals(thrownObject);
	}
	
	protected void propagateNewObject(ReceivedObject obj) throws GenericInvocationException {
		if(!isInitialGenericObject(obj)) {
			return;
		}
		propagateObjectToCallers(obj, oldCallingMethods);
	}
	
	protected void propagateOldObject(ReceivedObject obj) {
		if(!isInitialGenericObject(obj)) {
			return;
		}
		propagateObjectToCallers(obj, newCallingMethods);
	}
	
	protected void addCallingMethod(AccessedPropagator callingMethod) {
		if(hasPropagated()) {
			initializeNewTargets();
			newCallingMethods.add(callingMethod);
			scheduleRepropagation();
		} else {
			initializeTargets();
			oldCallingMethods.add(callingMethod);
		}
	}
	
	private void initializeTargets() {
		if(oldCallingMethods == null) {
			oldCallingMethods = new ArrayList();
		}
	}
	
	private void initializeNewTargets() {
		if(newCallingMethods == null) {
			newCallingMethods = new ArrayList();
		}
	}
	
	protected void migrateTargets() {
		if(newCallingMethods == null) {
			return;
		} else if(oldCallingMethods == null) {
			oldCallingMethods = newCallingMethods;
			newCallingMethods = null;
		} else {
			oldCallingMethods.addAll(newCallingMethods);
			newCallingMethods = null;
		}
	}
		
	MethodInvocation getPreviousCall(
			Method calledMethod,
			CallingContext invokedContext,
			InstructionLocation callLocation,
			boolean generic) {
		return null;
	}
	
	protected void addWrittenDataMember(AccessedPropagator memberWrittenTo) {}
	
	protected void addCalledMethod(AccessedPropagator calledMethod) {}
	
	public boolean isGeneric() {
		return true;
	}
	
	void propagateNewThrownObject(FieldObject obj) {
		/* since we invoke no methods, we can never receive any thrown objects */
	}
	
	void propagateOldThrownObject(FieldObject obj) {
		/* Since we invoke no methods, we can never receive any thrown objects. */
	}
	
	public String toString() {
		return "generic " + super.toString();
	}
}