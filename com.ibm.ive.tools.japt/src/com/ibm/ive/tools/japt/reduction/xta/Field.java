package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_HashedClassVector;

/**
 * @author sfoley
 *
 */
public class Field extends DataMember {
	
	BT_Field field;
	
	/**
	 * Constructor for Field.
	 * @param member
	 */
	public Field(Repository repository, BT_Field field) {
		super(repository.getClazz(field.getDeclaringClass()));
		this.field = field;
	}
	
	public Field(Repository repository, BT_Field field,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(repository.getClazz(field.getDeclaringClass()), propagatedObjects, allPropagatedObjects);
		this.field = field;
	}
	
	boolean isPrimitive() {
		return field.getFieldType().isPrimitive();
	}
	
	boolean isConstantStringField() {
		return field.isStatic() && field.getFieldType().equals(getRepository().properties.javaLangString) && field.attributes.getAttribute("ConstantValue") != null;
	}
	
	/**
	 * A static field may contain a constant string obtained from the constant pool when created, 
	 * as opposed to having a null initial value.
	 */
	void initializePropagation() {
		if(isConstantStringField()) {
			addCreatedObject(getRepository().properties.javaLangString);
		}
		addConditionallyCreatedObjects(field);
	}
	
	/**
	 * @return whether the field can hold an object of the given type
	 */
	boolean holdsType(BT_Class type) {
		return getRepository().isCompatibleType(field.getFieldType(), type);
	}
	
	 
	/**
	 * The field is included as part of an API so we must assume it can be accessed
	 * with any compatible arguments.
	 */
//	void propagateFromUnknownSource() {
//		super.propagateFromUnknownSource();
//		BT_ClassVector unknowns = new BT_HashedClassVector();
//		if(!field.type.isBasicTypeClass) {
//			unknowns.addUnique(field.type);
//			for(int i=0; i<unknowns.size(); i++) {
//				BT_Class creation = unknowns.elementAt(i); //adding all subtypes of a field, when the field is of type Object, includes ALL fields, which in turn generally prevents the removal of all objects
//				Clazz clazz = addCreatedObject(creation);
//				clazz.includeAllConstructors();
//			}
//		}
//		
//		Clazz clazz = getDeclaringClass();
//		if(field.isStatic()) {
//			clazz.setInitialized();
//		}
//		else {
//			clazz.setInstantiated();
//			clazz.includeAllConstructors();
//		}
//	}
	
	/**
	 * The field is included as part of an API so we must assume it can be accessed
	 * with any compatible arguments.
	 */
	void propagateFromUnknownSource() {
		super.propagateFromUnknownSource();
		Clazz clazz = getDeclaringClass();
		if(!field.isStatic()) {
			addCreatedObject(clazz.getBTClass());
			clazz.setInstantiated();
			clazz.includeConstructors();
		}
	}
	
	public void setAccessed() {
		if(isAccessed()) {
			return;
		}
		super.setAccessed();
		if(field.isStatic()) {
			declaringClass.setInitialized();
		}
	}
	
	public void setRequired() {
		if(isRequired()) {
			return;
		}
		super.setRequired();
		BT_Class type = field.getFieldType();
		if(type.isBasicTypeClass)  {
			return;
		}
		
		Repository rep = getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(getBTClass());
		if(checkEntryPoints && japtRepository.isInternalClass(type)) {
			lister.foundEntryTo(type, field);
		}
		
		rep.getClazz(type).setRequired();
	}

	public String toString() {
		return getName();
	}
	
	public String getName() {
		return field.toString();
	}
	
}
