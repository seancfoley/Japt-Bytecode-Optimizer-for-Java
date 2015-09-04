package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.jikesbt.BT_AttributeVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Member;

/**
 * @author sfoley
 *
 */
public class Field extends Member {
	
	private BT_Field field;
	private Clazz type;
	final int index;
	private PropagatedObject[] createdInstances;
	private static PropagatedObject emptyInstances[] = new PropagatedObject[0];
	private static final short shared = 0x4;/* share the same instance field amongst all objects, the same way static fields are shared */
	
	/**
	 * Constructor for Field.
	 * @param member
	 */
	public Field(BT_Field field, Clazz declaringClass, Clazz type) {
		super(declaringClass);
		this.field = field;
		this.type = type;
		BT_FieldVector fields = declaringClass.getUnderlyingType().getFields();
		boolean isStatic = isStatic();
		int index = 
			(isStatic || declaringClass.equals(declaringClass.repository.getJavaLangObject())) ? 
					0 : declaringClass.getSuperClass().instanceFieldCount;
		int i = 0;
		do {
			BT_Field f = fields.elementAt(i++);
			if(field.equals(f)) {
				break;
			}
			if(!(f.isStatic() ^ isStatic)) {
				index++;
			}
		} while(true);
		this.index = index;
	}
	
	boolean isShared() {
		return (flags & shared) != 0;
	}
	
	void setShared() {
		flags |= shared;
	}
	
	BT_Member getMember() {
		return field;
	}
	
	public BT_Field getField() {
		return field;
	}
	
	Clazz getType() {
		return type;
	}
	
	BT_AttributeVector getAttributes() {
		return field.attributes;
	}
	
	
	//We do not create a new set of objects for each field instance, otherwise that would
	//recursively create possibly an infinite number of field instances.  Instead, we have
	//a single set of objects for each field.  However, they must be created when one of the
	//field instances is being initialized, not when the field is being created, otherwise
	//this might cause an object to be added to a propagator at the same time it is propagating
	//through its objects
	PropagatedObject[] getCreatedInstances() {
		if(createdInstances == null && !getRepository().getPropagationProperties().isRTSJAnalysis()) {
			BT_ClassVector conditionals = getRepository().getConditionallyCreatedObjects(field);
			if(conditionals.size() == 0) {
				createdInstances = emptyInstances;
			} else {
				createdInstances = new PropagatedObject[conditionals.size()];
				for(int i=0; i<conditionals.size(); i++) {
					Repository rep = getRepository();
					createdInstances[i] = rep.getClazz(conditionals.elementAt(i)).instantiate(rep.getHeap());
				}
			}
		}
		return createdInstances;
	}
	
	/**
	 * @return whether the field can hold an object of the given type
	 */
	boolean holdsType(Clazz type) {
		return type.isInstanceOf(field.getFieldType());
	}
	
	void setRequired() {
		if(isRequired()) {
			return;
		}
		super.setRequired();
		if(type.isPrimitive())  {
			return;
		}
		
		Repository rep = getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(field.getDeclaringClass());
		if(checkEntryPoints) {
			BT_Class type = field.getFieldType();
			if(japtRepository.isInternalClass(type)) {
				lister.foundEntryTo(type, field);
			}
		}
		
		type.setRequired();
	}
	
	String getName() {
		return field.getName();
	}
}
