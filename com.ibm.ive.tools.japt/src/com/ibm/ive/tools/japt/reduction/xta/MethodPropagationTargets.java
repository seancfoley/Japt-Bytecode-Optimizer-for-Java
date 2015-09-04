package com.ibm.ive.tools.japt.reduction.xta;

import java.util.*;

import com.ibm.jikesbt.*;

/**
 * @author sfoley
 *
 */
public class MethodPropagationTargets {

	/**
	 * Objects can be propagated as the return type of a method to the caller.
	 * Also, exception objects can be thrown from one method to another.
	 * 
	 * Contains instances of Method
	 */
	private ArrayList callingMethods;
	
	/**
	 * Objects are propagated as arguments in a method call.
	 * 
	 * Contains instances of Method
	 */
	private ArrayList calledMethods;
	
	/**
	 * Objects are propagated when writing to a DataMember.
	 * 
	 * Contains instances of DataMember
	 */
	private ArrayList writtenMembers;

	/**
	 * the method which targets these targets
	 */
	private MethodPropagator method;
	
	static String getListMembers(List list) {
		StringBuffer res = new StringBuffer();
		for(int i=0; i<list.size(); i++) {
			res.append(list.get(i));
			res.append('\n');
		}
		return res.toString();
	}
	
	public String getState() {
		return "Calling methods: " + ((callingMethods == null) ? "0" : callingMethods.size() + ":\n" + getListMembers(callingMethods)) + '\n'
			+ "Called methods: " + ((calledMethods == null) ? "0" : calledMethods.size() + ":\n" + getListMembers(calledMethods)) + '\n'
			+ "Written members: " + ((writtenMembers == null) ? "0" : writtenMembers.size() + ":\n" + getListMembers(writtenMembers)) + '\n'
			;
	}
	
	MethodPropagationTargets(MethodPropagator method) {
		this.method = method;
	}
	
	void addWrittenDataMember(DataMember memberWrittenTo) {
		if(writtenMembers == null) {
			writtenMembers = new ArrayList(1);
		}
		writtenMembers.add(memberWrittenTo);
	}
	
	boolean containsCalledMethod(Method calledMethod) {
		return calledMethods != null && calledMethods.contains(calledMethod);
	}
	
	boolean containsWrittenDataMember(DataMember member) {
		return writtenMembers != null && writtenMembers.contains(member);
	}
	
	void addCalledMethod(Method calledMethod) {
		if(calledMethods == null) {
			calledMethods = new ArrayList(1);
		}
		calledMethods.add(calledMethod);
	}
	
	void addCallingMethod(Method callingMethod) {
		if(callingMethods == null) {
			callingMethods = new ArrayList(1);
		}
		callingMethods.add(callingMethod);
	}
	
	void migrate(MethodPropagationTargets to) {
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
		//callingMethods.clear();
		//calledMethods.clear();
		//writtenMembers.clear();
	}
	
	boolean isEmpty() {
		return (callingMethods == null || callingMethods.size() == 0)
			&& (calledMethods == null || calledMethods.size() == 0)
			&& (writtenMembers == null || writtenMembers.size() == 0);
	}
	
	/**
	 * We know that an object of type objectType can be present in the stack of this method.  So
	 * we know that it can then be propagated by this method to any field written to or any method called
	 * in the body of this method, or any method which catches a throwable object thrown by this method.
	 * 
	 * @see com.ibm.ive.tools.japt.reduction.xta.Member#propagateObject(BT_Class)
	 */
	void propagateObject(BT_Class objectType) {
		int size;
		if(writtenMembers != null) {
			size = writtenMembers.size();
			for(int i=0; i<size; i++) {
				DataMember propagatingDataMember = (DataMember) writtenMembers.get(i);
				
				/* for non-static methods, we see if at any time we have had available an object whose type is the
				 * type of the field's declaring class
				 */
				if(!propagatingDataMember.hasPropagated(objectType) && propagatingDataMember.holdsType(objectType)) {
					propagatingDataMember.addPropagatedObject(objectType);
				}
			}
		}
		
		/*
		 * we check if we are propagating an object whose
		 * type matches the method's declaring class.  
		 * This means that we know that the method can be called without throwing an exception. 
		 */
		if(calledMethods != null) {
			size = calledMethods.size();
			for(int i=0; i<size; i++) {
				Method propagatableMethod = (Method) calledMethods.get(i);
				
				/* if the method is static check if the object is a possible argument */
				if(!propagatableMethod.hasPropagated(objectType) && propagatableMethod.acceptsArgument(objectType)) {
					propagatableMethod.addPropagatedObject(objectType);
				}
			}
		}
		
		/*
		 * propagate object as return type to methods that call this method
		 */
		if(callingMethods != null && method.returnsObjects() && method.returns(objectType)) {
			for(int i=0; i<callingMethods.size(); i++) {
				Method method = (Method) callingMethods.get(i);
				if(!method.hasPropagated(objectType)) {
					method.addPropagatedObject(objectType);
				}
			}
		}
		
		/*
		 * throw the object to methods that call this method
		 */
		if(method.isThrowableObject(objectType) && method.canThrow(objectType)) {
			propagateThrownObject(objectType);
		}
		
		
	}
	
	/**
	 * Propagate an object which never actually exists on the method stack. The object is
	 * thrown from a called method and is never caught within this method (or is caught by 
	 * a finally cause and rethrown).  These objects must be propagated to methods which
	 * call this method and nowhere else.
	 */
	void propagateThrownObject(BT_Class objectType) {
		if(callingMethods == null) {
			return;
		}
		int size = callingMethods.size();
		for(int i=0; i<size; i++) {
			Method propagatableMethod = (Method) callingMethods.get(i);
			if(propagatableMethod.catches(objectType)) {
				if(!propagatableMethod.hasPropagated(objectType)) {
					propagatableMethod.addPropagatedObject(objectType);
				}
				//here we 'rethrow' the object because we cannot determine if it will always be caught
				//we first check if the method can throw, because it will be handled inside propagateObject in that case
				if(!propagatableMethod.canThrow() && propagatableMethod.canPassThrown(objectType)) {
					 if(!propagatableMethod.isThrownPropagated(objectType)) {
						propagatableMethod.addThrownPropagatedObject(objectType);
					 }
				}
				continue;
			}
			//TODO we know that the method cannot catch the thrown object, so we could check here if
			//it could throw this object as well (check the method signature declared exceptions), 
			//otherwise we can assume that it is caught in the current method somehow
			if(!propagatableMethod.isThrownPropagated(objectType)) {
				propagatableMethod.addThrownPropagatedObject(objectType);
			}
		}
	}
	
}
