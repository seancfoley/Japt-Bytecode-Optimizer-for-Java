package com.ibm.ive.tools.japt.reduction.ita;

/**
 * Instantiators allow us to instantiate specific Japt types to represent specific class types.
 * 
 * @author sfoley
 *
 */
public interface Instantiator {
	SpecificMethodInvocation create(Method method, int depth, CallingContext context);
	
	ConstructedObject instantiate(Clazz type, AllocationContext context);
	
	GenericObject instantiateGeneric(Clazz type, AllocationContext context);
	
	static final Instantiator DEFAULT_INSTANTIATOR = new Instantiator() {
		public SpecificMethodInvocation create(Method method, int depth, CallingContext context) {
			return new SpecificMethodInvocation(method, depth, context);
		}
		
		public ConstructedObject instantiate(Clazz type, AllocationContext context) {
			return new ConstructedObject(type, context);
		}
		
		public GenericObject instantiateGeneric(Clazz type, AllocationContext allocationContext) {
			if(GenericObject.isArrayType(type)) {
				if(type.isArray()) {
					return new GenericArrayObject(type, allocationContext);
				}
				return new GenericObject(type, allocationContext);
			}
			return new GenericConstructedObject(type, allocationContext);
		}
	};
}
