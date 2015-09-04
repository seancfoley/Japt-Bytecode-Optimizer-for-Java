package com.ibm.ive.tools.japt.reduction.ita;

/**
 * @author sfoley
 *
 */
public class ConstantStaticFieldInstance extends StaticFieldInstance {
	
	ConstantStaticFieldInstance(Field field) {
		super(field);
	}
		
	/**
	 * A static field may contain a constant string obtained from the constant pool when created, 
	 * as opposed to having a null initial value.
	 */
	void initializePropagation() {
		super.initializePropagation();
		ContextProvider provider = getPropagationProperties().provider;
		Clazz t = field.getType();
		if(t.isIgnoredInstantiation()) {
			return;
		}
		PropagatedObject obj = t.instantiate(provider.getInitializingContext().getAllocationContext());
		//you might be wondering, how could an instantiatedobject have been
		//propagated already?  That might happen in select cases where only a single object 
		//of a given type is instantiated, as might be the case with java.lang.String.
		if(!hasPropagated(obj)) {
			addInstantiatedObject(obj);	
		}
	}
	
	public String toString() {
		return "constant " + super.toString();
	}
}
