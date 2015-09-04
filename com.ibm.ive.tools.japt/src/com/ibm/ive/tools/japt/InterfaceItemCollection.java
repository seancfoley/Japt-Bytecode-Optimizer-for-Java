/*
 * Created on Jun 13, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_HashedFieldVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class InterfaceItemCollection {

	
	/**
	 * classes that are in this targetedClasses vector are also in the
	 * specified classes vector.  The main purpose is to indicate classes
	 * that reduction will think can be instantiated eg "includeClass aClass" or "includeField aClass.field"
	 * as opposed to things like parameter classes like someClass in "includeMethod aMethod(someClass)"
	 * or other identified classes
	 * 
	 * These are used in reduction to specify objects that come into existence when the reducer
	 * activates a given field or method.
	 */
	private BT_ClassVector targetedClasses = new BT_HashedClassVector();
	
	
	private BT_ClassVector specifiedClasses = new BT_HashedClassVector();
	private BT_MethodVector specifiedMethods = new BT_HashedMethodVector();
	private BT_FieldVector specifiedFields = new BT_HashedFieldVector();
	
	//TODO change the message when adding a conditional interface item...
	//so probably should not have the factory here, should be in subclasses...
	private JaptFactory factory;
	
	/**
	 * 
	 */
	public InterfaceItemCollection(JaptFactory factory) {
		this.factory = factory;
	}
	
	public boolean isEmpty() {
		return targetedClasses.size() == 0
			&& specifiedClasses.size() == 0
			&& specifiedMethods.size() == 0
			&& specifiedFields.size() == 0;
	}
	
	/**
	 * @return classes that are a part of the interface to the internal classes in 
	 * the japt repository, and are additionally classes that may be instantiated or declare
	 * a method that has been invoked, or a field that has been accessed.
	 */
	public BT_ClassVector getTargetedClasses() {
		return targetedClasses;
	}
	
	/**
	 * @return all classes that are a part of the interface to the internal classes in 
	 * the japt repository
	 */
	public BT_ClassVector getInterfaceClasses() {
		return specifiedClasses;
	}
	
	/**
	 * @return all methods that are a part of the interface to the internal classes in 
	 * the japt repository
	 */
	public BT_MethodVector getInterfaceMethods() {
		return specifiedMethods;
	}
	
	/**
	 * @return all fields that are a part of the interface to the internal classes in 
	 * the japt repository
	 */
	public BT_FieldVector getInterfaceFields() {
		return specifiedFields;
	}
	
	public void addTargetedClassToInterface(BT_Class clazz) {
		if(clazz.isPrimitive()) {
			return;
		}
		targetedClasses.addUnique(clazz);
	}
	
	public void addToInterface(BT_Method method) {
		if(!method.getDeclaringClass().isArray() && !specifiedMethods.contains(method)) {
			factory.noteMethodSpecified(method);
			specifiedMethods.addElement(method);
		}
		//specify the signature elements, which are a part of identifying the method
		BT_MethodSignature sig = method.getSignature();
		BT_ClassVector types = sig.types;
		BT_Class returnType = sig.returnType;
		for(int k=0; k<types.size(); k++) {
			BT_Class type = types.elementAt(k);
			addToInterface(type);
		}
		addToInterface(returnType);
	}
	
	public void addToInterface(BT_Field field) {
		if(!field.getDeclaringClass().isArray() && !specifiedFields.contains(field)) {
			factory.noteFieldSpecified(field);
			specifiedFields.addElement(field);
		}
		//specify the type, which is a part of identifying the method
		BT_Class type = field.getFieldType();
		addToInterface(type);
	}
	
	public void addToInterface(BT_Class clazz) {
		if(clazz.isPrimitive()) {
			return;
		}
		if(clazz.isArray()) {
			clazz = clazz.arrayType;
		}
		if(!specifiedClasses.contains(clazz)) {
			factory.noteClassSpecified(clazz);
			specifiedClasses.addElement(clazz);
		}
	}
	
	public boolean isInInterface(BT_Class clazz) {
		return specifiedClasses.contains(clazz);
	}
	
	public boolean isInInterface(BT_Method method) {
		return specifiedMethods.contains(method);
	}
	
	public boolean isInInterface(BT_Field field) {
		return specifiedFields.contains(field);
	}
	
	public void removeFromInterface(BT_Class clazz) {
		specifiedClasses.removeElement(clazz);
		targetedClasses.removeElement(clazz);
	}
	
	public void removeFromInterface(BT_Method meth) {
		specifiedMethods.removeElement(meth);
	}
	
	//TODO note that when we add a field or a method to the interface,
	//we add the class types in the signature to the interface, but when
	//we remove it, we do not remove the class types of the signature,
	//but this is no simple task, since we need to know if the class
	//types are also there for some other reason
	public void removeFromInterface(BT_Field field) {
		specifiedFields.removeElement(field);
	}

}
