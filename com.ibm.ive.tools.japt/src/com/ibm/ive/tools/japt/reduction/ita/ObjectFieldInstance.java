package com.ibm.ive.tools.japt.reduction.ita;

public class ObjectFieldInstance extends FieldInstance {

	final PropagatedObject containingObject;
	
	public ObjectFieldInstance(Field field, PropagatedObject containingObject) {
		super(field);
		this.containingObject = containingObject;
	}
	
	public PropagatedObject getContainingObject() {
		return containingObject;
	}

}
