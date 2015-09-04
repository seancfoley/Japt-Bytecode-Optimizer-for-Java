package com.ibm.ive.tools.japt.reduction.ita;


/**
 * @author sfoley
 *
 * This class represents an object instance.  It propagates by delegating propagation to each of its instance fields.
 */
public class ConstructedObject extends PropagatedObject implements FieldObject {
	
	private FieldInstance fields[] = null;
	private final AllocationContext context;
	
	private ConstructedObject(Clazz type, AllocationContext context, boolean invokeFinalizer) {
		super(type);
		this.context = context;
		if(invokeFinalizer) {
			Method method = type.getFinalizer(); 
			if(method != null) {
				ContextProvider provider = type.repository.getPropagationProperties().provider;
				CallingContext callingContext = provider.getFinalizingContext(this);
				if(callingContext != null) { /* if the object is immortal, then it will not be finalized */
					Clazz clazz = method.getDeclaringClass();
					MethodInvoke invoker = new MethodInvokeFromVM(method.getRepository(), callingContext, method, clazz);
					invoker.invokeInstanceMethod(this, false);
				}
			}
		}
	}
	
	public FieldInstance[] getFields() {
		return fields;
	}
	
	public String toString() {
		String res = super.toString();
		Repository rep = getRepository();
		if(rep.getAllocationContextCount() > 1) {
			res += " in " + context;
		}
		return res;
	}
	
	ConstructedObject(Clazz type, AllocationContext context) {
		this(type, context, type.repository.isFinalizationEnabled);
	}
	
	public AllocationContext getAllocationContext() {
		return context;
	}
	
	public boolean isThrowable() {
		return type.isThrowable();
	}
	
	public boolean isRuntimeThrowable() {
		return getType().isRuntimeThrowable();
	}
	
	public boolean isArray() {
		return false;
	}
	
	boolean isPrimitiveArray() {
		return false;
	}
	
	public ArrayElement getArrayElement() {
		throw new UnsupportedOperationException();
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
						new ObjectFieldInstance(field, this);
		return instance;
	}
	
	public ObjectSet getContainedObjects() {
		return getContainedObjects(fields);
	}
	
	public static ObjectSet getContainedObjects(FieldInstance fields[]) {
		if(fields == null) {
			return ObjectSet.EMPTY_SET;
		}
		//Collection values = instanceFields.values();
		if(fields.length == 0) {
			return ObjectSet.EMPTY_SET;
		}
		int index = 0;
		FieldInstance first;
		do {
			first = fields[index];
		} while(first == null && ++index < fields.length);
		if(first == null) {
			return ObjectSet.EMPTY_SET;
		}
		ObjectSet set = first.getContainedObjects();
		if(fields.length == 1 || index == fields.length) {
			if(set == null || set.size() == 0) {
				return ObjectSet.EMPTY_SET;
			}
			return set;
		}
		ObjectSet result = new ObjectSet();
		if(set != null && set.size() > 0) {
			result.addAll(set);
		}
		do {
			FieldInstance next = fields[index];
			if(next != null) {
				set = next.getContainedObjects();
				if(set != null && set.size() > 0) {
					result.addAll(set);
				}
			}
		} while(++index < fields.length);
		if(result.size() == 0) {
			return ObjectSet.EMPTY_SET;
		}
		return result;
	}
	
	public boolean doPropagation() throws PropagationException {
		if(fields == null) {
			return false;
		}
		int numPropagated = Repository.doPropagation(fields);
		if(numPropagated > 0) {
			Repository rep = type.repository;
			rep.instanceFieldCount += numPropagated;
			return true;
		}
		return false;
	}
	
	public void doCount() {
		if(fields == null) {
			return;
		}
		Repository rep = type.repository;
		rep.instanceFieldCount += Repository.doCount(fields);
	}
	
	public boolean isGeneric() {
		return false;
	}
	
	public boolean isGenericInstanceOf(Clazz type) {
		return false;
	}
	
	public boolean mightBeGenericInstanceOf(Clazz type) {
		return false;
	}
	
	public PropagatedObject clone(AllocationContext newContext) {
		ConstructedObject cloned = new ConstructedObject(type, newContext);
		return cloned;
	}
	
}
