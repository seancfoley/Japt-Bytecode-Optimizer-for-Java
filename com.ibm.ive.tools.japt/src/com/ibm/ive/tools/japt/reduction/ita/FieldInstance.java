package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_Field;


/**
 * @author sfoley
 * 
 * Represents a field.
 *
 * As an object propagator, it propagates objects to method invocations.
 * It propagates to methods that read from this field.
 */
public class FieldInstance extends DataMember implements Comparable {
	
	public final Field field;
	
	FieldInstance(Field field) {
		this.field = field;
	}
	
	public boolean isFieldInstance() {
		return true;
	}
	
	public Clazz getDataType() {
		return field.getType();
	}
	
	public Clazz getDefiningClass() {
		return field.getDeclaringClass();
	}
	
	public PropagatedObject getContainingObject() {
		return null;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object object) {
		FieldInstance other = (FieldInstance) object;
		if(this == other) {
			return 0;
		}
		int result = 0;
		if(field != other.field) {
			result = field.compareTo(other.field);
		}
		if(result == 0) {
			/* this is for comparing the same field from two separate objects */
			//TODO this is unsafe, but since fields from two separate objects are never in the same collection it's mostly OK
			result = hashCode() - other.hashCode();
		}
		return result;
	}

	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#initializePropagation()
	 */
	void initializePropagation() {
		PropagatedObject createdInstances[] = field.getCreatedInstances();
		if(createdInstances != null) {
			for(int i=0; i<createdInstances.length; i++) {
				PropagatedObject obj = createdInstances[i];
				/* how could a newly obtained object have been propagated already?  That might happen in select cases where only a single object 
				 * of a given type is instantiated, as might be the case with java.lang.String.
				 */
				if(!hasPropagated(obj)) {
					addInstantiatedObject(obj);
				}
			}
		}
	}
	
	void setAccessed() {
		if(isAccessed()) {
			return;
		}
		super.setAccessed();
		field.setAccessed();
	}
	
	public String toString() {
		BT_Field member = (BT_Field) field.getMember();
		return "instance of " + member.useName() 
			//+ " of type " + member.getFieldType().useName() 
			+ ' ' + PropagatedObject.toIdentifier(this);
	}
	
}
