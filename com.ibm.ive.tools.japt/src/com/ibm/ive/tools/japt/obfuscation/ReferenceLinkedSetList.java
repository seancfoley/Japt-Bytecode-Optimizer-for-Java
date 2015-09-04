package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;
import com.ibm.jikesbt.*;

/**
 * Used by ReferenceLinkedSetCreator in order to construct 
 * instances of ReferenceLinkedSet.  Essentially contains lists of 'targets',
 * which are classes, fields or methods that have been targetted for finding more fields and
 * methods for the ReferenceLinkedSet.  The different types of targets are
 * listed separately in order to have fine control of the construction of the sets.
 */
class ReferenceLinkedSetList {

	private LinkedList methodTargetList = new LinkedList();
	private LinkedList fieldTargetList = new LinkedList();
	private LinkedList classMethodTargetList = new LinkedList();
	private LinkedList classFieldTargetList = new LinkedList();
	private LinkedList classTargetList = new LinkedList();
	private LinkedList classMemberTargetList = new LinkedList();
	
//	private ArrayList methodTargetList = new ArrayList(); //contains BT_Method
//	private ArrayList fieldTargetList = new ArrayList(); //contains BT_Field
//	private ArrayList classMethodTargetList = new ArrayList(); //contains ClassMethodTarget
//	private ArrayList classFieldTargetList = new ArrayList(); //contains ClassFieldTarget
//	private ArrayList classTargetList = new ArrayList(); //contains BT_Class
//	private ArrayList classMemberTargetList = new ArrayList(); //contains BT_Class
	
	/** we use hash sets to make lookup quicker and to ensure no target
	 * is ever listed twice
	 */
	private HashSet methodTargetSet = new HashSet(100);
	private HashSet fieldTargetSet = new HashSet(100);
	private HashSet classMethodTargetSet = new HashSet(100);
	private HashSet classFieldTargetSet = new HashSet(100);
	private HashSet classTargetSet = new HashSet(100);
	private HashSet classMemberTargetSet = new HashSet(100);
	
	void addMethodTarget(BT_Method method) {
		if(!methodTargetSet.contains(method)) {
			methodTargetList.add(method);
			methodTargetSet.add(method);
		}
	}
	
	void addFieldTarget(BT_Field field) {
		if(!fieldTargetSet.contains(field)) {
			fieldTargetList.add(field);
			fieldTargetSet.add(field);
		}
	}

	void addClassTarget(BT_Class clazz, BT_MethodSignature sig) {
		ClassMethodTarget target = new ClassMethodTarget();
		target.signature = sig;
		target.clazz = clazz;
		if(!classMethodTargetSet.contains(target)) {
			classMethodTargetList.add(target);	
			classMethodTargetSet.add(target);	
		}
	}
	
	void addClassTarget(BT_Class clazz, BT_Class fieldType) {
		ClassFieldTarget target = new ClassFieldTarget();
		target.fieldType = fieldType;
		target.clazz = clazz;
		if(!classFieldTargetSet.contains(target)) {
			classFieldTargetList.add(target);
			classFieldTargetSet.add(target);
		}
	}
	
	void addClassTarget(BT_Class clazz) {
		if(!classTargetSet.contains(clazz)) {
			classTargetList.add(clazz);
			classTargetSet.add(clazz);
		}
	}
	
	void addClassMemberTarget(BT_Class clazz) {
		if(!classMemberTargetSet.contains(clazz)) {
			classMemberTargetList.add(clazz);
			classMemberTargetSet.add(clazz);
		}
	}
	
	boolean isEmpty() {
		return methodTargetList.isEmpty() && fieldTargetList.isEmpty() 
		&& classMethodTargetList.isEmpty() && classFieldTargetList.isEmpty()
		&& classMemberTargetList.isEmpty();
	}
	
	BT_Method getNextMethodTarget() {
		if(methodTargetList.isEmpty()) {
			return null;
		}
		BT_Method method = (BT_Method) methodTargetList.remove(0);
		return method;
	}
	
	BT_Field getNextFieldTarget() {
		if(fieldTargetList.isEmpty()) {
			return null;
		}
		BT_Field field = (BT_Field) fieldTargetList.remove(0);
		return field;
	}
	
	
	ClassMethodTarget getNextClassMethodTarget() {
		if(classMethodTargetList.isEmpty()) {
			return null;
		}
		ClassMethodTarget target = (ClassMethodTarget) classMethodTargetList.remove(0);
		return target;
	}
	
	ClassFieldTarget getNextClassFieldTarget() {
		if(classFieldTargetList.isEmpty()) {
			return null;
		}
		ClassFieldTarget target = (ClassFieldTarget) classFieldTargetList.remove(0);
		return target;
	}
	
	BT_Class getNextClassTarget() {
		if(classTargetList.isEmpty()) {
			return null;
		}
		BT_Class clazz = (BT_Class) classTargetList.remove(0);
		return clazz;
	}
	
	BT_Class getNextClassMemberTarget() {
		if(classMemberTargetList.isEmpty()) {
			return null;
		}
		BT_Class clazz = (BT_Class) classMemberTargetList.remove(0);
		return clazz;
	}
	
	boolean hasClassMethodTargets() {
		return !classMethodTargetList.isEmpty();
	}
	
	boolean hasClassFieldTargets() {
		return !classMethodTargetList.isEmpty();
	}
	
	class ClassMethodTarget {
		BT_Class clazz;
		BT_MethodSignature signature;
		
		public boolean equals(Object o) {
			if(o instanceof ClassMethodTarget) {
				ClassMethodTarget other = (ClassMethodTarget) o;
				return clazz.equals(other.clazz) && signature.equals(other.signature);
			}
			return false;
		}
		
	}
	
	class ClassFieldTarget {
		BT_Class clazz;
		BT_Class fieldType;
		
		public boolean equals(Object o) {
			if(o instanceof ClassFieldTarget) {
				ClassFieldTarget other = (ClassFieldTarget) o;
				return clazz.equals(other.clazz) && fieldType.equals(other.fieldType);
			}
			return false;
		}
	}
	
	
}
