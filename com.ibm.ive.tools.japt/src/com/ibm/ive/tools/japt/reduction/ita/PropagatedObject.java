package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ClassProperties;
import com.ibm.ive.tools.japt.reduction.ita.ObjectSet.ObjectSetEntry;
import com.ibm.jikesbt.BT_Class;


/**
 * @author sfoley
 *
 * Represents an object instance that has been propagated from one object propagator to another.
 * The object propagators are methods, fields and array elements.
 * 
 * It is not only something being propagated, it also acts as a propagator at the same time, by 
 * propagating its constituent propagators, which are either non-static fields or array elements.
*/

public abstract class PropagatedObject extends ObjectSetEntry implements Propagator, ReceivedObject {
	
	final Clazz type;
	
	PropagatedObject(Clazz type) {
		if(type == null) {
			throw new NullPointerException();
		}
		this.type = type;
	}
	
	public Clazz getType() {
		return type;
	}
	
	/**
	 * 
	 * @return the allocation context
	 */
	public abstract AllocationContext getAllocationContext();
	
	public abstract boolean isArray();
	
	public abstract boolean isGeneric();
	
	abstract void doCount();
	
	abstract boolean isGenericInstanceOf(Clazz type);
	
	/*
	 * returns whether this object is generic and the type might be a subtype of this type
	 */
	abstract boolean mightBeGenericInstanceOf(Clazz type);
	
	public boolean mightBeInstanceOf(Clazz type) {
		return getType().mightBeInstanceOf(type);
	}
	
	public boolean isInstanceOf(Clazz type) {
		return getType().isInstanceOf(type);
	}
	
	public abstract FieldInstance[] getFields();
	
	public abstract ObjectSet getContainedObjects();
	
	public abstract boolean isThrowable();

	public static String toIdentifier(Object o) {
		return Integer.toHexString(o.hashCode());
	}
	
	boolean isJavaLangObject() {
		ClassProperties properties = type.repository.getClassProperties();
		BT_Class underlyingType = type.getUnderlyingType();
		return properties.javaLangObject.equals(underlyingType);
	}
	
	public String getName() {
		return type.getName() + " instance " + toIdentifier(this);
	}
	
	public String toString() {
		return getName();
	}
	
	/**
	 * implements ReceivedObject
	 */
	public final MethodInvocationLocation getLocation() {
		return null;
	}
	
	/**
	 * implements ReceivedObject
	 */
	public PropagatedObject getObject() {
		return this;
	}
	
	Repository getRepository() {
		return getType().repository;
	}
	
	/**
	 * compares to any other ReceivedObject
	 */
	public int compareTo(Object object) {
		if(object instanceof WrappedObject) {
			object = ((WrappedObject) object).getObject();
		}
		ReceivedObject other = (ReceivedObject) object;
		PropagatedObject otherProp = other.getObject();
		return TargetedObject.compare(this, null, otherProp, other.getLocation());
	}
	
	public boolean equals(Object object) {
		if(object instanceof WrappedObject) {
			object = ((WrappedObject) object).getObject();
		}
		return super.equals(object);
	}
	
	abstract public PropagatedObject clone(AllocationContext newContext);
}
