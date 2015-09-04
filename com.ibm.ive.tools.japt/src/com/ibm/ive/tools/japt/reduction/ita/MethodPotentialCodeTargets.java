package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldRefIns;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_MethodSignature;


/**
 * @author sfoley
 *
 */
public class MethodPotentialCodeTargets implements MethodPotentialTargets {

//	/**
//	 * The method propagator (static method or method in an object instance) that will do the propagating
//	 */
//	private SpecificMethodInvocation invocation;
	
	/** 
	 * Methods called by this method, which propagates objects from the calling method stack
	 * to the called method stack
	 */
	private ArrayList callableNonVirtualMethods; //contains PotentialTarget objects which contain Method objects
	
	/** 
	 * Methods called by this method, which propagates objects from the calling method stack
	 * to the called method stack
	 */
	private ArrayList callableVirtualMethods; //contains PotentialTarget objects which contain Method objects
	 
	 
	 /**
	  * Fields are populated by methods, 
	  * which propagate objects from the method stack to the field
	  */
	private ArrayList writableFields; //contains PotentialTarget objects which contain Field objects
	
	/**
	  * Fields are read by methods, 
	  * which propagate objects from the field to the method stack
	  */
	private ArrayList readableFields; //contains PotentialTarget objects which contain Field objects
	
	private static final AccessedPropagator emptyProps[] = new AccessedPropagator[0];
	private static final PotentialTarget emptyTargets[] = new PotentialTarget[0];
	
	public String toString() {
		return "potential code targets " + PropagatedObject.toIdentifier(this);
	}
	
	/**
	 * This might return true if all accessible fields were primitive and there
	 * are no accessible methods, because in this case the fields cannot become propagators,
	 * and so once they have been marked accessed they are removed.
	 */
	boolean hasNoMoreSources() {
		return 
			(callableVirtualMethods == null || callableVirtualMethods.size() == 0)
			&& (callableNonVirtualMethods == null || callableNonVirtualMethods.size() == 0)
			&& (readableFields == null || readableFields.size() == 0)
			&& (writableFields == null || writableFields.size() == 0);
	}
	
	private void examineFieldRead(Repository repository, Clazz targetClass, Field field, 
			ArrayList readStaticFields, boolean checkEntryPoints,
			InstructionLocation instructionLocation, MethodInvocation invocation) {
		JaptRepository japtRepository = repository.repository;
		EntryPointLister lister = repository.entryPointLister;
		BT_Field accField = field.getField();
		Clazz declaringClass = field.getDeclaringClass();
		
		if(accField.isStatic()) {
			if(checkEntryPoints && japtRepository.isInternalClass(accField.getDeclaringClass())) {
				lister.foundEntryTo(accField, invocation.getMethod().getMethod());
			}
			targetClass.setRequired();
			invocation.context.enter(targetClass, field);
			targetClass.enterVerifierRequiredClasses(invocation.context);
			if(field.getType().isPrimitive()) {
				/* since the field is incapable of propagating, we don't bother creating a propagator */
				field.setAccessed();
				field.getDeclaringClass().setInitialized();
			} else {
				StaticFieldInstance staticField = declaringClass.getStaticFieldInstance(accField);
				staticField.setAccessed();
				readStaticFields.add(new AccessedPropagator(staticField, instructionLocation));
			}
		} else {
			if(readableFields == null) {
				readableFields = new ArrayList(1);
			}
			readableFields.add(new PotentialTarget(targetClass, field, instructionLocation));
		}
	}
	
	private void examineFieldWrite(Repository repository, Clazz targetClass, Field field, 
			ArrayList writtenStaticFields, boolean checkEntryPoints, 
			InstructionLocation instructionLocation, MethodInvocation invocation) {
		JaptRepository japtRepository = repository.repository;
		EntryPointLister lister = repository.entryPointLister;
		BT_Field accField = field.getField();
		Clazz declaringClass = field.getDeclaringClass();
		
		if(accField.isStatic()) {
			if(checkEntryPoints && japtRepository.isInternalClass(accField.getDeclaringClass())) {
				lister.foundEntryTo(accField, invocation.getMethod().getMethod());
			}
			targetClass.setRequired();
			invocation.context.enter(targetClass, field);
			targetClass.enterVerifierRequiredClasses(invocation.context);
			if(field.getType().isPrimitive()) {
				/* since the field is incapable of propagating, we don't bother creating a propagator */
				field.setAccessed();
				field.getDeclaringClass().setInitialized();
			} else {
				StaticFieldInstance staticField = declaringClass.getStaticFieldInstance(accField);
				staticField.setAccessed();
				writtenStaticFields.add(new AccessedPropagator(staticField, instructionLocation));
			}
		} else {
			if(writableFields == null) {
				writableFields = new ArrayList(1);
			}
			writableFields.add(new PotentialTarget(targetClass, field, instructionLocation));
		}
	}
	
	private void examineMethod(Repository repository, Clazz targetClass, Method referencedMethod, 
			ArrayList calledStaticMethods, boolean checkEntryPoints, 
			InstructionLocation location) {
		BT_Ins instruction = location.instruction;
		if(referencedMethod.isStatic()) {
			calledStaticMethods.add(new PotentialTarget(targetClass, referencedMethod, location));
		}
		//note that we ignore whether a method is marked final, because treating a virtual
		//call like a non-virtual call would cause reduction to work incorrectly (why?  I cannot remember :-) )
		else if (instruction.isInvokeVirtualIns() || instruction.isInvokeInterfaceIns()) {
			if(callableVirtualMethods == null) {
				callableVirtualMethods = new ArrayList(1);
			}
			callableVirtualMethods.add(new PotentialTarget(targetClass, referencedMethod, location));
		} else {
			/* TODO does not handle invokespecials properly...  if referencedMethod is not private and not a constructor,
			 * and is in a parent class P of the current class, then we must check for a method m that is in a subclass S of
			 * P which is a superclass of the current class.  This preserves binary compatibility.  So the current code will work
			 * most of the time.
			 */
			if(callableNonVirtualMethods == null) {
				callableNonVirtualMethods = new ArrayList(1);
			}
			callableNonVirtualMethods.add(new PotentialTarget(targetClass, referencedMethod, location));
		}
	}
	

	private Statics examineAccesses(
			BT_AccessorVector accessedFields,
			BT_MethodCallSiteVector calledMethods, SpecificMethodInvocation invocation) {
		
		Repository repository = invocation.getRepository();
		JaptRepository japtRepository = repository.repository;
		EntryPointLister lister = repository.entryPointLister;
		Method method = invocation.getMethod();
		BT_Class declaringBTClass = method.getDeclaringClass().getUnderlyingType();
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(declaringBTClass);
		
		ArrayList readStaticFields = new ArrayList(accessedFields.size());
		ArrayList writtenStaticFields = new ArrayList(accessedFields.size());
		ArrayList calledStaticMethods = new ArrayList(calledMethods.size());
		
		InstructionLocation locations[] = method.getFieldReads();
		for(int i=0; i<locations.length; i++) {
			InstructionLocation location = locations[i];
			BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) location.instruction;
			BT_Field accField = fieldRefIns.getTarget();
			Clazz declaringClass = repository.getClazz(accField.getDeclaringClass());
			Field field = declaringClass.getField(accField);
			Clazz targetClass = repository.getClazz(fieldRefIns.getClassTarget());
			examineFieldRead(repository, targetClass, field, readStaticFields, checkEntryPoints, location, invocation);
		}
		locations = method.getFieldWrites();
		for(int i=0; i<locations.length; i++) {
			InstructionLocation location = locations[i];
			BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) location.instruction;
			BT_Field accField = fieldRefIns.getTarget();
			Clazz declaringClass = repository.getClazz(accField.getDeclaringClass());
			Field field = declaringClass.getField(accField);
			Clazz targetClass = repository.getClazz(fieldRefIns.getClassTarget());
			examineFieldWrite(repository, targetClass, field, writtenStaticFields, checkEntryPoints, location, invocation);
		}
		locations = method.getInvocations();
		for(int i=0; i<locations.length; i++) {
			InstructionLocation location = locations[i];
			BT_MethodRefIns methodRefIns = (BT_MethodRefIns) location.instruction;
			BT_Method calledMethod = methodRefIns.getTarget();
			BT_Class dec = calledMethod.getDeclaringClass();
			if(!dec.getMethods().contains(calledMethod)) {
				//The method does not exist, so do not propagate to it.
				//It might have been removed by a previous reduction.
				continue;
			}
			Clazz declaringClass = repository.getClazz(dec);
			Method referencedMethod = declaringClass.getMethod(calledMethod);
			Clazz targetClass = repository.getClazz(methodRefIns.getClassTarget());
			examineMethod(repository, targetClass, referencedMethod, calledStaticMethods, checkEntryPoints, location);
		}
		Statics statics = new Statics();
		if(readStaticFields.size() > 0) {
			statics.readStaticFields = (AccessedPropagator[]) readStaticFields.toArray(new AccessedPropagator[readStaticFields.size()]);
		} else {
			statics.readStaticFields = emptyProps;
		}
		if(writtenStaticFields.size() > 0) {
			statics.writtenStaticFields = (AccessedPropagator[]) writtenStaticFields.toArray(new AccessedPropagator[writtenStaticFields.size()]);
		} else {
			statics.writtenStaticFields = emptyProps;
		}
		if(calledStaticMethods.size() > 0) {
			statics.calledStaticMethods = (PotentialTarget[]) calledStaticMethods.toArray(new PotentialTarget[calledStaticMethods.size()]);
		} else {
			statics.calledStaticMethods = emptyTargets;
		}
		return statics;
	}
	
	private Statics initialize(SpecificMethodInvocation invocation) {
		Method method = invocation.getMethod();
		BT_CodeAttribute code = method.getCode();
		BT_AccessorVector accessedFields = code.accessedFields;
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		Statics statics;
		if(calledMethods.size() == 0 && accessedFields.size() == 0) {
			statics = new Statics();
			statics.readStaticFields = emptyProps;
			statics.writtenStaticFields = emptyProps;
			statics.calledStaticMethods = emptyTargets;
			return statics;
		}
		statics = examineAccesses(accessedFields, calledMethods, invocation);
		if(readableFields != null) {
			readableFields.trimToSize();
		}
		if(writableFields != null) {
			writableFields.trimToSize();
		}
		if(callableNonVirtualMethods != null) {
			callableNonVirtualMethods.trimToSize();
		}
		if(callableVirtualMethods != null) {
			callableVirtualMethods.trimToSize();
		}
		return statics;
	}
	
	static FieldInstance getPropagatingField(
			CallingContext context,
			boolean isInstance,
			FieldObject object,
			Clazz targetClass,
			Field field,
			Clazz declaringClass) {
		FieldObject propagatedObject;
		if(isInstance) {
			propagatedObject = object;
		} else {
			GenericObject current = (GenericObject) object;
			GenericObject split = current.getSplitGeneric(declaringClass);
			propagatedObject = split;
		}
		FieldInstance propagatingField = propagatedObject.getFieldInstance(field);
		propagatingField.setAccessed();
		context.enter(targetClass, field);
		return propagatingField;
	}
	
	/**
	 * The presence of the given object might make possible new non-static method calls and new
	 * non-static field accesses, so we find these new targets.
	 */
	 public void findNewTargets(ReceivedObject received, SpecificMethodInvocation invocation) throws GenericInvocationException {
		PropagatedObject object = received.getObject();
		boolean isGenericObject = object.isGeneric();
		Clazz objectType = object.getType();
		
	 	if(isGenericObject ? objectType.isArray() : object.isArray()) {
	 		return;
	 	}
	 	
	 	Repository rep = invocation.getRepository();
	 	Method method = invocation.getMethod();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(method.getDeclaringClass().getUnderlyingType());
		MethodInvocationLocation receivedLocation = received.getLocation();
		
	 	if(writableFields != null) { 
			for(int i=0; i<writableFields.size(); i++) {
				PotentialTarget potentialTarget = (PotentialTarget) writableFields.get(i);
				Field field = (Field) potentialTarget.target;
				Clazz declaringClass = field.getDeclaringClass();
				boolean isInstance = declaringClass.isInstance(objectType);
				//boolean isInstance = declaringClass.mightBeInstance(objectType);we can't support an unlimited number of fields in objectType
				if(!isInstance && !object.mightBeGenericInstanceOf(declaringClass)) {
					continue;
				}
				Clazz fieldType = field.getType();
				int stackIndex = (fieldType.getSizeForLocal() == 2) ? StackLocation.getSecondFromTopCellIndex() : StackLocation.getNextToTopCellIndex();
				if(!method.maps(receivedLocation, potentialTarget.location, stackIndex)) {
					continue;
				}
				if(checkEntryPoints && japtRepository.isInternalClass(field.getDeclaringClass().getUnderlyingType())) {
					lister.foundEntryTo(field.getField(), method.getMethod());
				}
				Clazz targetClass = potentialTarget.instructionTarget;
				targetClass.setRequired();
				if(fieldType.isPrimitive()) {
					field.setAccessed();
					invocation.context.enter(targetClass, field);
					
					/* now that we've marked it as accessed, since it is incapable of propagating we might as well get rid of it */
					writableFields.remove(i);
					i--;
					if(writableFields.size() == 0) {
						writableFields = null;
						break;
					}
				} else {
					FieldInstance propagatingField = getPropagatingField(invocation.context, isInstance, (FieldObject) object, targetClass, field, declaringClass);
					AccessedPropagator accessedField = new AccessedPropagator(propagatingField, potentialTarget.location);
					invocation.addWrittenDataMember(accessedField);
					invocation.propagateToDataMember(object, receivedLocation, propagatingField, potentialTarget.location);
				}		
			}
		}
		
		/*
		 * determine what fields we can now read from
		 */
		if(readableFields != null) {
			for(int i=0; i<readableFields.size(); i++) {
				PotentialTarget potentialTarget = (PotentialTarget) readableFields.get(i);
				Field field = (Field) potentialTarget.target;
				Clazz declaringClass = field.getDeclaringClass();
				boolean isInstance = declaringClass.isInstance(objectType);
				//boolean isInstance = declaringClass.mightBeInstance(objectType); we can't support an unlimited number of fields in objectType
				if(!isInstance && !object.mightBeGenericInstanceOf(declaringClass)) {
					continue;
				}
				InstructionLocation potentialTargetLocation = potentialTarget.location;
				if(!method.maps(receivedLocation, potentialTargetLocation, StackLocation.getTopCellIndex())) {
					continue;
				}
				if(checkEntryPoints && japtRepository.isInternalClass(field.getDeclaringClass().getUnderlyingType())) {
					lister.foundEntryTo(field.getField(), method.getMethod());
				}
				Clazz targetClass = potentialTarget.instructionTarget;
				targetClass.setRequired();
				if(field.getType().isPrimitive()) {
					field.setAccessed();
					invocation.context.enter(targetClass, field);
					
					/* 
					 * now that we've marked it as accessed, since it is incapable
					 * of propagating we might as well get rid of it
					 */
					readableFields.remove(i);
					i--;
					if(readableFields.size() == 0) {
						readableFields = null;
						break;
					}
				} else {
					FieldInstance propagatingField = getPropagatingField(invocation.context, isInstance, (FieldObject) object, targetClass, field, declaringClass);
					AccessedPropagator accessedField = new AccessedPropagator(invocation, potentialTargetLocation);
					propagatingField.addReadingMethod(accessedField);
				}
			}
		}
		
		
		PropagationProperties props = rep.getPropagationProperties();
		/*
		 * we check if we are propagating an object whose
		 * type matches the method's declaring class.  
		 * This means that we know that the method can be called without throwing an exception. 
		 */
		if(callableVirtualMethods != null) {
			for(int i=0; i<callableVirtualMethods.size(); i++) {
				PotentialTarget potentialTarget = (PotentialTarget) callableVirtualMethods.get(i);
				Method propagatableMethod = (Method) potentialTarget.target;
				Clazz target = potentialTarget.instructionTarget;
				boolean isInstance = target.isInstance(objectType);
				//boolean isInstance = target.mightBeInstance(objectType);we can't support an unlimited number of methods for objectType
				boolean isGenericInstance;
				if(!isInstance) {
					isGenericInstance = object.mightBeGenericInstanceOf(target);
					if(!isGenericInstance) {
						continue;
					}
				}
				InstructionLocation potentialTargetLocation = potentialTarget.location;
				int cellIndex = StackLocation.getInvokedCellIndex(propagatableMethod.getSignature());
				if(!method.maps(receivedLocation, potentialTargetLocation, cellIndex)) {
					continue;
				}
				Clazz targetClass = potentialTarget.instructionTarget;
				PropagatedObject sent = isInstance ? object : ((GenericObject) object).getSplitGeneric(targetClass);
				boolean isGenericInvocation = object.isGeneric() 
					&& propagatableMethod.isGenerallyOverridable() 
					//&& !props.isRTSJAnalysis()
					;
				if(isGenericInvocation) {
					if(props.isReachabilityAnalysis()) {
						throw new GenericInvocationException(method, propagatableMethod, "invoking " + propagatableMethod + " of " + sent);
					}
					MethodInvokeInstruction invoker = new MethodInvokeInstruction(invocation, potentialTargetLocation, propagatableMethod, targetClass);
					invoker.invokeInstanceMethod(sent, true);
				} else {
					VirtualMethodInvoke virtualInvoker = new VirtualMethodInvoke(invocation, potentialTargetLocation, propagatableMethod, targetClass);
					virtualInvoker.invokeVirtualMethod(sent);
				}
			}
		}
		
		/*
		 * we check if we are propagating an object whose
		 * type matches the method's declaring class.  
		 * This means that we know that the method can be called without throwing an exception. 
		 */
		if(callableNonVirtualMethods != null) {
			for(int i=0; i<callableNonVirtualMethods.size(); i++) {
				PotentialTarget potentialTarget = (PotentialTarget) callableNonVirtualMethods.get(i);
				Method propagatableMethod = (Method) potentialTarget.target;
				Clazz target = potentialTarget.instructionTarget;
				//boolean isInstance = target.mightBeInstance(objectType);we can't support an unlimited number of methods for objectType
				boolean isInstance = target.isInstance(objectType);
				if(!isInstance && !object.mightBeGenericInstanceOf(target)) {
					continue;
				}
				InstructionLocation potentialTargetLocation = potentialTarget.location;
				BT_MethodSignature sig = propagatableMethod.getSignature();
				int cellIndex = StackLocation.getInvokedCellIndex(sig);
				if(!method.maps(receivedLocation, potentialTargetLocation, cellIndex)) {
					continue;
				}
				Clazz targetClass = potentialTarget.instructionTarget;
				PropagatedObject sent = isInstance ? object : ((GenericObject) object).getSplitGeneric(targetClass);
				MethodInvokeInstruction invoker = new MethodInvokeInstruction(invocation, potentialTargetLocation, propagatableMethod, targetClass);
				invoker.invokeInstanceMethod(sent, false);
			}
		}
	}	
	
	 static class Statics {
		AccessedPropagator readStaticFields[];
		AccessedPropagator writtenStaticFields[];
		PotentialTarget calledStaticMethods[]; //should be static methods, need to create a new invocation each time
	 }
	 
	
	 /**
	 * Whenever a method is called, whether instance or static, this is called to ensure that any static method calls within the body
	 * are registered as having taken place.  Because the same method can be called from many different object instances, we 
	 * first determine if this is the very first call, and if so the static targets are calculated.  Subsequent instances can easily
	 * iterate through the list.
	 */
	public void findStaticTargets(SpecificMethodInvocation invocation) {
		Statics statics = initialize(invocation);
		AccessedPropagator readStaticFields[] = statics.readStaticFields;
		AccessedPropagator writtenStaticFields[] = statics.writtenStaticFields;
		PotentialTarget calledStaticMethods[] = statics.calledStaticMethods;
		for(int i=0; i<readStaticFields.length; i++) {
			AccessedPropagator accessed = readStaticFields[i];
			StaticFieldInstance field = (StaticFieldInstance) accessed.propagator;
			field.addReadingMethod(new AccessedPropagator(invocation, accessed.location));
		}
		for(int i=0; i<writtenStaticFields.length; i++) {
			AccessedPropagator accessed = writtenStaticFields[i];
			invocation.addWrittenDataMember(accessed);
		}
		for(int i=0; i<calledStaticMethods.length; i++) {
			PotentialTarget calledMethod = calledStaticMethods[i];
			Method target = (Method) calledMethod.target;
			InstructionLocation calledLocation = calledMethod.location;
			MethodInvokeInstruction invoker = new MethodInvokeInstruction(invocation, calledLocation, target, calledMethod.instructionTarget);
			invoker.invokeStaticMethod();
		}
	}
}
