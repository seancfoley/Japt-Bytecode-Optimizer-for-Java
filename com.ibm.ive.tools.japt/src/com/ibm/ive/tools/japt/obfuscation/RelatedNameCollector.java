package com.ibm.ive.tools.japt.obfuscation;

import com.ibm.jikesbt.*;
import java.util.*;
import com.ibm.ive.tools.japt.*;

/**
 * @author sfoley
 *
 */
class RelatedNameCollector implements RelatedClassCollector.RelatedClassVisitor {

	private Set existingMethodNames = new HashSet(); // all the method names in parents or children (or parents of children, parents of parents, etc...) 
	private Set existingFieldNames = new HashSet();  // all the fields names in parents or children (or parents of children, parents of parents, etc...) 
	//private boolean caseSensitive;
	private NameHandler nameHandler;
	
	/**
	 * Constructor for RelatedNameCollector.
	 * @param clazz
	 * @param nameHandler
	 */
	RelatedNameCollector(NameHandler nameHandler) {
		this.nameHandler = nameHandler;
	}
	
	
	/**
	 * one of the classes in the collection has the indicated field
	 */
	boolean fieldNameAlreadyExists(String name) {
		return existingFieldNames.contains(name);
	}
	
	
	/**
	 * One of the classes in the collection has the indicated method.
	 * Only the signature parameters are considered, the return type is ignored.
	 */
	boolean methodNameAlreadyExists(String name, BT_MethodSignature signature) {
		return existingMethodNames.contains(getFullName(name, signature));
	}
	
	void addFieldName(String name) {
		existingFieldNames.add(name);
	}
	
	void addMethodName(String name, BT_MethodSignature sig) {
		existingMethodNames.add(getFullName(name, sig));
	}
	
	public void visit(BT_Class relatedClass, int relation) {
		BT_MethodVector methods = relatedClass.getMethods();
		boolean isParent = (relation == PARENT || relation == PARENT_OF_CHILD);
		for(int i=0; i<methods.size(); i++) {
			BT_Method otherMethod = methods.elementAt(i);
			if(isParent && otherMethod.isPrivate()) {
				continue;
			}
			if(nameHandler.nameIsFixed(otherMethod)) {
				addMethodName(otherMethod.getName(), otherMethod.getSignature());
			}
		}
		BT_FieldVector fields = relatedClass.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field otherField = fields.elementAt(i);
			if(isParent && otherField.isPrivate()) {
				continue;
			}
			if(nameHandler.nameIsFixed(otherField)) {
				addFieldName(otherField.getName());
			}
		}
	}
	
	private String getFullName(String name, BT_MethodSignature sig) {
		StringBuffer buffer = new StringBuffer(name);
		buffer.append(' ');
		BT_ClassVector types = sig.types;
		for(int i=0, size = types.size(); i<size; i++) {
			buffer.append(types.elementAt(i));
		}
		return buffer.toString();
	}
	

}
