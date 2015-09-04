package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ClassProperties;
import com.ibm.jikesbt.BT_Class;


/**
 * Generic objects are passed around like others.  But you cannot call a non-final virtual method on a generic object because there
 * are any number of overridding methods.  Finalizers for generics are ignored, since generics are used to determine reachability from a given point in the
 * program quickly, and reachibility from finalizers is not analyzed.
 *  
 * Generic objects are accepted by any propagator whose type is an instance of the generic type.
 * 
 * When a generic object is propagated from a propagator S, it propagates itself only if the generic object's type is a direct
 * match to the type accepted by the propagator P.  If the match is not exact, then a new generic object is created,
 * and this new object starts propagation from this generic object's original propagator O.  Eventually this new generic object
 * will wind its way back to the same propagator S, because if the more generic type can get there, then so can the more
 * specific type.  However, there will be places the new specific object will go that the generic object will not
 * go (it will be propagated to P, for instance).
 * 
 * A generic object is considered equal to any other generic object of the same type.  It is not considered equal to any non-generic object.
 * 
 * @author sfoley
 *
 */
public class GenericObject extends PropagatedObject implements Array, FieldObject {
	
	private final AllocationContext context;
	
	/* objects of final classes can have no origin, because they cannot become more specific, and thus they will never split */
	GenericObject(Clazz baseType, AllocationContext context) {
		super(baseType);
		this.context = context;
	}
	
	public AllocationContext getAllocationContext() {
		return context;
	}
	
	public int hashCode() {
		int result = type.hashCode();
		return result;
	}
	
	public boolean equals(Object other) {
		if(other instanceof WrappedObject) {
			other = ((WrappedObject) other).getObject();
		}
		if(other instanceof GenericObject) {
			GenericObject otherGeneric = (GenericObject) other;
			return type.equals(otherGeneric.type);
		}
		return false;
	}
	
	public int compareTo(Object object) {
		if(object instanceof WrappedObject) {
			object = ((WrappedObject) object).getObject();
		}
		ReceivedObject other = (ReceivedObject) object;
		if(other instanceof GenericObject) {
			GenericObject otherGeneric = (GenericObject) other;
			return type.getUnderlyingType().compareTo(otherGeneric.type.getUnderlyingType());
		}
		//Use the same comparison mechanism for generic objects to compare themselves to other objects,
		//because that is how other objects compare themselves to generics
		return super.compareTo(object);
	}
	
	public boolean isGeneric() {
		return true;
	}

	boolean mightBeGenericInstanceOf(BT_Class holdingType) {
		return type.mightBeInstance(holdingType);
	}
	
	public boolean mightBeGenericInstanceOf(Clazz holdingType) {
		return mightBeGenericInstanceOf(holdingType.getUnderlyingType());
	}
	
	public boolean isGenericInstanceOf(BT_Class holdingType) {
		return type.isInstance(holdingType);
	}
	
	public boolean isGenericInstanceOf(Clazz holdingType) {
		return isGenericInstanceOf(holdingType.getUnderlyingType());
	}
	
	/**
	 * This object must be split into a generic array object or a generic constructed object before propagation is possible.
	 */
	public boolean doPropagation() throws PropagationException {
		return false;
	}
	
	public void doCount() {}
	
	public ObjectSet getContainedObjects() {
		return ObjectSet.EMPTY_SET;
	}
	
	public boolean isRuntimeThrowable() {
		if(type.isRuntimeThrowable()) {
			return true;
		}
		ClassProperties props = type.repository.getClassProperties();
		BT_Class javaLangError = props.javaLangError;
		BT_Class javaLangRuntimeException = props.javaLangRuntimeException;
		return isGenericInstanceOf(javaLangRuntimeException) || isGenericInstanceOf(javaLangError);
	}
	
	public boolean isThrowable() {
		return type.isThrowable() || isGenericInstanceOf(type.repository.getClassProperties().javaLangThrowable);
	}
	
	/**
	 * This object will be split prior to providing an array element, even if identified as an array.
	 */
	public boolean isArray() {
		return isArrayType(type);
	}
	
	public boolean isPrimitiveArray() {
		if(type.isArray()) {
			return type.getUnderlyingType().arrayType.isPrimitive();
		}
		return isArrayParent(type);
	}
	
	static boolean isArrayType(Clazz type) {
		return type.isArray() || isArrayParent(type);
	}
	
	static boolean isArrayParent(Clazz type) {
		ClassProperties properties = type.repository.getClassProperties();
		BT_Class underlyingType = type.getUnderlyingType();
		return properties.javaLangObject.equals(underlyingType) 
			|| properties.javaIoSerializable.equals(underlyingType) 
			|| properties.javaLangCloneable.equals(underlyingType);
	}
	
	/**
	 * This object will be split prior to providing an array element, even if identified as an array.
	 */
	public ArrayElement getArrayElement() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * We rely on the fact that java.lang.Object or any of the interface types do not have non-static fields.
	 */
	public FieldInstance getFieldInstance(Field field) {
		throw new UnsupportedOperationException();
	}
	
	public FieldInstance[] getFields() {
		return null;
	}
	
	public String getName() {
		String one = "generic ";
		String two = super.getName();
		StringBuffer buffer = new StringBuffer(one.length() + two.length());
		buffer.append(one);
		buffer.append(two);
		return buffer.toString();
	}
	
	public String toString() {
		String res = getName();
		Repository rep = getRepository();
		if(rep.getAllocationContextCount() > 1) {
			res += " in " + context;
		}
		return res;
	}
	
	GenericObject getSplitGeneric(Clazz asType) {
		//create a new Generic object that will be propagated
		GenericObject split = asType.instantiateGeneric(context);
		asType.addCreated(split);
		return split;
	}
	
	public PropagatedObject clone(AllocationContext newContext) {
		GenericObject cloned = new GenericObject(type, newContext);
		return cloned;
	}
}
