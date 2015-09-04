package com.ibm.ive.tools.japt.reduction.ita;



/**
 * 
 * @author sfoley
 *
 */
public class GenericConstructedObject extends GenericObject {
	private FieldInstance fields[];
	
	//objects of final classes can have no origin, because they cannot become more specific
	GenericConstructedObject(Clazz baseType, AllocationContext context) {
		super(baseType, context);
	}
	
	public boolean isArray() {
		return false;
	}
	
	public FieldInstance[] getFields() {
		return fields;
	}
	
	public ObjectSet getContainedObjects() {
		return ConstructedObject.getContainedObjects(fields);
	}
	
	public boolean doPropagation() throws PropagationException {
		return false;
	}
	
	public void doCount() {
		if(fields == null) {
			return;
		}
		Repository rep = type.repository;
		rep.instanceFieldCount += Repository.doCount(fields);
	}
	
	public FieldInstance getFieldInstance(Field field) {
		if(field.isStatic()) {
			throw new IllegalArgumentException();
		}
		FieldInstance instance;
		if(fields == null) {
			fields = new FieldInstance[type.instanceFieldCount];
			instance = createNewInstance(field);
		} else {
			instance = fields[field.index];
			if(instance == null) {
				instance = createNewInstance(field);
			}
		}
		fields[field.index] = instance;
		return instance;
	}
	
	private FieldInstance createNewInstance(Field field) {
		FieldInstance instance = field.isShared() ? field.getDeclaringClass().getSharedFieldInstance(field) : 
			field.getDeclaringClass().getGenericFieldInstance(field);
		return instance;
	}
	
	public PropagatedObject clone(AllocationContext newContext) {
		GenericConstructedObject cloned = new GenericConstructedObject(type, newContext);
		return cloned;
	}
}
