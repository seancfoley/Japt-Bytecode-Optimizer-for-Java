package com.ibm.ive.tools.japt.obfuscation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedFieldVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;


/**
 * Represents a set of fields and methods amongst numerous classes, all of which can have the same name.
 */
class ReferenceLinkedSet {
	
	private Map nonPrivateFieldMap = new HashMap();  //maps BT_Class to the BT_Field contained in the ConstructedClass, if any
	private Map privateFieldMap = new HashMap();  //maps BT_Class to the BT_Field contained in the ConstructedClass, if any
	private Map nonPrivateMethodMap = new HashMap(); //maps BT_Class to the BT_MethodVector of BT_Methods contained in the the constructed set, if any
	private Map privateMethodMap = new HashMap(); //maps BT_Class to the BT_MethodVector of BT_Methods contained in the the constructed set, if any
	private Set memberClasses = new HashSet(); //all classes which have a field of method in the set
	//private BT_ClassVector memberClasses = new BT_HashedClassVector(); //all classes which have a field of method in the set
	private BT_FieldVector fieldMembers = new BT_HashedFieldVector();
	private BT_MethodVector methodMembers = new BT_HashedMethodVector();
	
	boolean containsNonPrivateMethodFromClassWithParameters(BT_Class clazz, BT_MethodSignature signature) {
		return containsMethodFromClassWithParameters(clazz, signature, nonPrivateMethodMap);
	}
	
	boolean containsMethodFromClassWithParameters(BT_Class clazz, BT_MethodSignature signature) {
		return containsMethodFromClassWithParameters(clazz, signature, privateMethodMap)
		|| containsMethodFromClassWithParameters(clazz, signature, nonPrivateMethodMap);
	}
	
	private boolean containsMethodFromClassWithParameters(BT_Class clazz, BT_MethodSignature signature, Map map) {
		BT_MethodVector methods = (BT_MethodVector) map.get(clazz);
		if(methods == null) {
			return false;
		}
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(RelatedMethodMap.parametersMatch(method.getSignature(), signature)) {
				return true;
			}
		}
		return false;
	}
	
	boolean containsMethodFromClass(BT_Class clazz) {
		return privateMethodMap.containsKey(clazz) || nonPrivateMethodMap.containsKey(clazz);
	}
	
	BT_MethodVector getNonPrivateMethodsFromClass(BT_Class clazz) {
		return (BT_MethodVector) nonPrivateMethodMap.get(clazz);
	}
	
	BT_Field getNonPrivateFieldFromClass(BT_Class clazz) {
		return (BT_Field) nonPrivateFieldMap.get(clazz);
	}

	int size() {
		return fieldMembers.size() + methodMembers.size();		
	}
	
	int classCount() {
		return memberClasses.size();
	}
	
	boolean containsNonPrivateFieldFromClass(BT_Class clazz) {
		return nonPrivateFieldMap.containsKey(clazz);
	}
	
	boolean containsFieldFromClass(BT_Class clazz) {
		return privateFieldMap.containsKey(clazz) || nonPrivateFieldMap.containsKey(clazz);
	}
	
	private void addToMethodMap(BT_Class clazz, BT_Method method, Map map) {
		BT_MethodVector methods = (BT_MethodVector) map.get(clazz);
		if(methods == null) {
			methods = new BT_MethodVector();
			map.put(clazz, methods);
		}
		methods.addElement(method);
	}
	
	void addMemberFromClass(BT_Class clazz, BT_Method method) {
		if(methodMembers.contains(method)) {
			return;
		}
				
		memberClasses.add(clazz);
		methodMembers.addElement(method);
		if(method.isPrivate()) {
			addToMethodMap(clazz, method, privateMethodMap);
		}
		else {
			addToMethodMap(clazz, method, nonPrivateMethodMap);
		}
	}
	
	void addMemberFromClass(BT_Class clazz, BT_Member member) {
		if(member instanceof BT_Field) {
			addMemberFromClass(clazz, (BT_Field) member);
		}
		else {
			addMemberFromClass(clazz, (BT_Method) member);
		}
	}
	
	void addMemberFromClass(BT_Class clazz, BT_Field field) {
		if(fieldMembers.contains(field)) {
			return;
		}
		memberClasses.add(clazz);
		fieldMembers.addElement(field);
		if(field.isPrivate()) {
			privateFieldMap.put(clazz, field);
		}
		else {
			nonPrivateFieldMap.put(clazz, field);
		}
	}
	
	void removeMember(BT_Member member) {
		if(member instanceof BT_Field) {
			removeMember((BT_Field) member);
		}
		else {
			removeMember((BT_Method) member);
		}
	}
	
	void removeMember(BT_Field field) {
		fieldMembers.removeElement(field);
		BT_Class clazz = field.getDeclaringClass();
		if(field.isPrivate()) {
			privateFieldMap.remove(clazz);
		}
		else {
			nonPrivateFieldMap.remove(clazz);
		}
		if(!privateMethodMap.containsKey(clazz) && !nonPrivateMethodMap.containsKey(clazz)) {
			memberClasses.remove(clazz);
		}
	}
	
	void removeMember(BT_Method method) {
		methodMembers.removeElement(method);
		BT_Class clazz = method.getDeclaringClass();
		if(method.isPrivate()) {
			removeFromMethodMap(clazz, method, privateMethodMap);
		}
		else {
			removeFromMethodMap(clazz, method, nonPrivateMethodMap);
		}
		if(!privateFieldMap.containsKey(clazz) && !nonPrivateFieldMap.containsKey(clazz)) {
			memberClasses.remove(clazz);
		}
	}
	
	private void removeFromMethodMap(BT_Class clazz, BT_Method method, Map map) {
		BT_MethodVector methods = (BT_MethodVector) map.get(clazz);
		if(methods != null) {
			methods.removeElement(method);
			if(methods.size() == 0) {
				map.remove(clazz);
			}
		}
	}
	
	void clear() {
		memberClasses.clear();
		methodMembers.removeAllElements();
		fieldMembers.removeAllElements();
		nonPrivateFieldMap.clear();
		privateFieldMap.clear();
		nonPrivateMethodMap.clear();
		privateMethodMap.clear();
	}
	
	
	BT_MethodVector getMethods() {
		return methodMembers;
	}
	
	BT_FieldVector getFields() {
		return fieldMembers;
	}
	
	Iterator iterator() {
		return new Iterator() {
			int index;
			BT_Member ret;
			public boolean hasNext() {
				return index < fieldMembers.size() + methodMembers.size();
			} 
          
 			public Object next() {
 				if(index < fieldMembers.size()) {
 					ret = fieldMembers.elementAt(index);
 				}
 				else {
 					ret = methodMembers.elementAt(index - fieldMembers.size());
 				}
 				index++;
 				return ret;
 			}
          
 			public void remove() {
 				if(ret == null) {
 					return;
 				}
 				removeMember(ret);
 				ret = null;
 				index--;
 			}
		};
	}
}
