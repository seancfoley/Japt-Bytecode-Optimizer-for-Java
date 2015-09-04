package com.ibm.ive.tools.japt.reduction.xta;

import java.util.ArrayList;
import java.util.HashSet;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;


/**
 * @author sfoley
 *
 */
public class MethodPotentialCodeTargets implements MethodPotentialTargets {

	private Method method;
	
	/** 
	 * Methods called by this method, which propagates objects from the calling method stack
	 * to the called method stack
	 */
	private ArrayList callableNonVirtualMethods; /* contains instances of type Method */
	
	/** 
	 * Methods called by this method, which propagates objects from the calling method stack
	 * to the called method stack
	 */
	private ArrayList callableVirtualMethods; /* contains instances of type CallableMethod */
	 
	 
	 /**
	  * Fields are populated by methods, 
	  * which propagate objects from the method stack to the field
	  */
	private ArrayList writableFields;
	
	/**
     * Fields are read by methods, 
     * which propagate objects from the field to the method stack
     */
	private ArrayList readableFields;
	
	
	MethodPotentialCodeTargets(Method method) {
		this.method = method;
	}
	
	
	
	/**
	 * The presence of the given object might make possible new non-static method calls and new
	 * non-static field accesses, so we find these new targets.
	 */
	public void findNewTargets(BT_Class objectType) {
		Repository rep = method.getRepository();
		JaptRepository japtRepository = rep.repository;
		
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(method.getBTClass());
		
		if(writableFields != null) { 
			for(int i=0; i<writableFields.size(); i++) {
				Field propagatingField = (Field) writableFields.get(i);
				if(!propagatingField.isDeclaringType(objectType)) {
					continue;
				}
				if(checkEntryPoints && japtRepository.isInternalClass(propagatingField.getBTClass())) {
					lister.foundEntryTo(propagatingField.field, method.underlyingMethod);
				}
				propagatingField.setAccessed();
				if(!propagatingField.isPrimitive()) {
					//propagate the object to the new target
					if(!propagatingField.hasPropagated(objectType) && propagatingField.holdsType(objectType)) {
						propagatingField.addPropagatedObject(objectType);
					}
					method.addWrittenDataMember(propagatingField);
				}
				writableFields.remove(i);
				i--;
				if(writableFields.size() == 0) {
					writableFields = null;
					break;
				}
			}
		}
		
		/*
		 * determine what fields we can now read from
		 */
		if(readableFields != null) {
			for(int i=0; i<readableFields.size(); i++) {
				Field propagatingField = (Field) readableFields.get(i);
				if(!propagatingField.isDeclaringType(objectType)) {
					continue;
				}
				if(checkEntryPoints && japtRepository.isInternalClass(propagatingField.getBTClass())) {
					lister.foundEntryTo(propagatingField.field, method.underlyingMethod);
				}
				propagatingField.setAccessed();
				if(!propagatingField.isPrimitive()) {
					propagatingField.addReadingMethod(method);
				}
				readableFields.remove(i);
				i--;
				if(readableFields.size() == 0) {
					readableFields = null;
					break;
				}
			}
		}
		
		
		/*
		 * we check if we are propagating an object whose
		 * type matches the method's declaring class.  
		 * This means that we know that the method can be called without throwing an exception. 
		 */
		if(callableVirtualMethods != null) {
			for(int i=0; i<callableVirtualMethods.size(); i++) {
				CallableMethod callableMethod = (CallableMethod) callableVirtualMethods.get(i);
				Method propagatableMethod = callableMethod.method;
				BT_Method btMethod = propagatableMethod.underlyingMethod;
				if(!propagatableMethod.isDeclaringType(objectType)) {
					continue;
				}
				
				/* check if the instance of the declaring class overrides or implements the callable method */
				BT_Method overridingMethod;
				
				if(!objectType.equals(btMethod.getDeclaringClass())
				 && (overridingMethod = ((btMethod.getDeclaringClass().isInterface()) ? 
				 		method.getRepository().relatedMethodMap.getImplementingMethod(objectType, btMethod) : 
				 		method.getRepository().relatedMethodMap.getOverridingMethod(objectType, btMethod))) != null) {	
						
						/* the method is overridden or implemented so we must mark this method as live and callable from here */
						Method relatedMethod = method.getRepository().getMethod(overridingMethod);
						
						
						if(relatedMethod.underlyingMethod.isAbstract() || btMethod.isFinal()) {
							//TODO reduction fix involving not targetting explicitly removed items
							relatedMethod.setRequired();
						}
						else {
							//TODO reduction fix involving not targetting explicitly removed items
							//if we haven't called this method before, call it
							if(!method.hasPreviouslyCalled(relatedMethod)) {
								boolean check = checkEntryPoints && japtRepository.isInternalClass(overridingMethod.getDeclaringClass());
								setCalled(check ? lister : null, true, japtRepository, relatedMethod, callableMethod.throughClass);
							}
							
							//propagate the object to the new target
							if(!relatedMethod.hasPropagated(objectType)) {
								relatedMethod.addPropagatedObject(objectType);
							}
						}
						propagatableMethod.setRequired();
				}
				/* we have an instance of the declaring class that does not override the callable method, meaning the method can be called */
				else {
					
					if(btMethod.isAbstract()) {
						propagatableMethod.setRequired();
					}
					else {
						if(!callableMethod.isCalled) { 
							callableMethod.isCalled = true;
							if(!method.hasPreviouslyCalled(propagatableMethod)) {
								boolean check = checkEntryPoints && japtRepository.isInternalClass(btMethod.getDeclaringClass());
								setCalled(check ? lister : null, false, japtRepository, propagatableMethod, callableMethod.throughClass);
							}
						}
						//propagate the object to the new target
						if(!propagatableMethod.hasPropagated(objectType)) {
							propagatableMethod.addPropagatedObject(objectType);
						}
					}
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
				CallableMethod callableMethod = (CallableMethod) callableNonVirtualMethods.get(i);
				Method propagatableMethod = callableMethod.method;
				if(!propagatableMethod.isDeclaringType(objectType)) {
					continue;
				}
				
				if(!method.hasPreviouslyCalled(propagatableMethod)) {
					boolean check = checkEntryPoints && japtRepository.isInternalClass(propagatableMethod.getBTClass());
					setCalled(check ? lister : null, false, japtRepository, propagatableMethod, callableMethod.throughClass);
				}
				
				//propagate the object to the new target
				if(!propagatableMethod.hasPropagated(objectType)) {
					propagatableMethod.addPropagatedObject(objectType);
				}
				callableNonVirtualMethods.remove(i);
				i--;
				if(callableNonVirtualMethods.size() == 0) {
					callableNonVirtualMethods = null;
					break;
				}
			}
		}
		
	}
	
	boolean hasNoPotentialTargets() {
		return 
		//hasNoVirtuals
			(callableVirtualMethods == null || callableVirtualMethods.size() == 0)
			&& (callableNonVirtualMethods == null || callableNonVirtualMethods.size() == 0)
			&& (readableFields == null || readableFields.size() == 0)
			&& (writableFields == null || writableFields.size() == 0);
	}
	
	public void findTargets(BT_CodeAttribute code) {
		Repository rep = method.getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(method.getBTClass());
		
		BT_AccessorVector accessedFields = code.accessedFields;
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		
		HashSet alreadyFoundWrites = new HashSet();
		HashSet alreadyFoundReads = new HashSet();
		
		
		//determine which fields are read from and which are written to
		for(int i=0; i<accessedFields.size(); i++) {
			BT_Accessor acc = accessedFields.elementAt(i);
			BT_Field accField = acc.instruction.target;
			//TODO reduction fix involving not targetting explicitly removed items
			if(acc.isFieldRead()) {
				if(alreadyFoundReads.contains(accField)) {
					continue;
				}
				alreadyFoundReads.add(accField);
				Field field = method.getRepository().getField(accField);
				if(accField.isStatic()) {
					if(checkEntryPoints && japtRepository.isInternalClass(accField.getDeclaringClass())) {
						lister.foundEntryTo(accField, method.underlyingMethod);
					}
					
					field.setAccessed();
					if(!field.isPrimitive()) {
						field.addReadingMethod(method);
					}
				}
				else {
					if(readableFields == null) {
						readableFields = new ArrayList(1);
					}
					readableFields.add(field);
				}
			}
			else {
				if(alreadyFoundWrites.contains(accField)) {
					continue;
				}
				alreadyFoundWrites.add(accField);
				Field field = method.getRepository().getField(accField);
				if(accField.isStatic()) {
					if(checkEntryPoints && japtRepository.isInternalClass(accField.getDeclaringClass())) {
						lister.foundEntryTo(accField, method.underlyingMethod);
					}
					field.setAccessed();
					if(!field.isPrimitive()) {
						method.addWrittenDataMember(field);
					}
				}
				else {
					if(writableFields == null) {
						writableFields = new ArrayList(1);
					}
					writableFields.add(field);
				}
			}
			
		}
		if(readableFields != null) {
			readableFields.trimToSize();
		}
		if(writableFields != null) {
			writableFields.trimToSize();
		}
		
		//determine which methods can potentially be called		
		int size = calledMethods.size();
		HashSet alreadyFound = null;
		if(size > 0) {
			alreadyFound = new HashSet();
			//we add ourselves so that we do not have recursion or 
			//propagating to myself
			alreadyFound.add(method.underlyingMethod);
		}
		for(int i=0; i<size; i++) {
			BT_MethodCallSite site = calledMethods.elementAt(i);
			BT_Method calledMethod = site.getTarget();
			BT_Class throughClass = site.getClassTarget();
			if(alreadyFound.contains(calledMethod)) {
				continue;
			}
			alreadyFound.add(calledMethod);
			Method referencedMethod = method.getRepository().getMethod(calledMethod);
			Clazz refClass = method.getRepository().getClazz(throughClass);
			if(calledMethod.isStatic()) {
				boolean check = checkEntryPoints && japtRepository.isInternalClass(calledMethod.getDeclaringClass());
				setCalled(check ? lister : null, false, japtRepository, referencedMethod, refClass);
			}
			//note that we ignore whether a field is marked final, because treating a virtual
			//call like a non-virtual call would cause reduction to work incorrectly
			else  {
				CallableMethod callable = new CallableMethod(refClass, referencedMethod);
				if (site.instruction.isInvokeVirtualIns() || site.instruction.isInvokeInterfaceIns()) {
					if(callableVirtualMethods == null) {
						callableVirtualMethods = new ArrayList(1);
					}
					callableVirtualMethods.add(callable);
				}
				else {
					if(callableNonVirtualMethods == null) {
						callableNonVirtualMethods = new ArrayList(1);
					}
					/* TODO does not handle invokespecials properly...  if referencedMethod is not private and not a constructor,
					 * and is in a parent class P of the current class, then we must check for a method m that is in a subclass S of
					 * P which is a superclass of the current class.  This preserves binary compatibility.  If all classes in an application
					 * are compiled at the same time this will have no effect.  So the current code will work most of the time.
					 */
					
					callableNonVirtualMethods.add(callable);
				}
			}
		}
		if(callableNonVirtualMethods != null) {
			callableNonVirtualMethods.trimToSize();
		}
		if(callableVirtualMethods != null) {
			callableVirtualMethods.trimToSize();
		}
	}
	
	private void setCalled(EntryPointLister lister, boolean isVirtual, JaptRepository rep, Method calledMethod, Clazz throughClass) {
		//the constant pool may reference the referenced
		//class which might be a child of the method's declaring class,
		//and so it must exist
		//TODO reduction fix involving not targetting explicitly removed items
		throughClass.setRequired();
		if(calledMethod.hasObjectParameters()) {
			method.addCalledMethod(calledMethod);
		}
		calledMethod.setAccessed();
		calledMethod.addCallingMethod(method);
		
		if(lister != null) {
			if(isVirtual) {
				lister.foundOverridingOrImplementingEntryTo(calledMethod.underlyingMethod, method.underlyingMethod);
			}
			else {
				lister.foundEntryTo(calledMethod.underlyingMethod, method.underlyingMethod);
			}
		}
	}
	
	/**
	 * A method that may be called from this method
	 */
	private final class CallableMethod {
		private Clazz throughClass;
		private Method method;
		private boolean isCalled;
		
		CallableMethod(Clazz throughClass, Method method) {
			this.method = method;
			this.throughClass = throughClass;
		}
		
		public boolean equals(Object o) {
			if(getClass() != o.getClass()) {
				return false;
			}
			return method.equals(((CallableMethod) o).method);
		}
	}
		
}
