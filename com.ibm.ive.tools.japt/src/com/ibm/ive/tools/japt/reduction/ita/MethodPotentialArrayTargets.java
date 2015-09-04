package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;


/**
 * @author sfoley
 *
 */
public class MethodPotentialArrayTargets implements MethodPotentialTargets {
	
	public String toString() {
		return "potential array targets " + PropagatedObject.toIdentifier(this);// + " for " + invocation;
	}
	
	public void findStaticTargets(SpecificMethodInvocation inv) {}
	
	public void findNewTargets(ReceivedObject received, SpecificMethodInvocation invocation) {
		PropagatedObject object = received.getObject();
		if(!object.isArray()) {
			return;
		}
		Array array = (Array)  object;
		if(array.isPrimitiveArray()) {
			return;
		}
		Method method = invocation.getMethod();
		boolean stores = method.storesIntoArrays();
		boolean loads = method.loadsFromArrays();
		
		if(stores || loads) {
			
			ArrayElement element;
			if(!array.getType().isArray()) {
				/* 
				 * We have a generic object of type java.lang.Object, 
				 * java.io.Serializable, or some other parent type of array classes
				 */
				Repository rep = invocation.getRepository();
				Clazz objectArray = rep.getObjectArray();
				GenericObject genericObject = (GenericObject) array;
				GenericObject split = genericObject.getSplitGeneric(objectArray);
				element = split.getArrayElement();
			} else {
				element = array.getArrayElement();
			}
			
			if(stores) {
				if(handleArrayWrites(received.getLocation(), element, invocation)) {
					element.setAccessed();
				}
				if(loads) {
					if(handleArrayReads(received.getLocation(), element, invocation)) {
						element.setAccessed();
					}
				}
			} else {
				if(handleArrayReads(received.getLocation(), element, invocation)) {
					element.setAccessed();
				}
			}	
		}
	}
	
	private boolean handleArrayWrites(MethodInvocationLocation location, ArrayElement element, MethodInvocation invocation) {
		Method method = invocation.getMethod();
		if(method.useIntraProceduralAnalysis()) {
			InstructionLocation writes[] = method.getArrayWrites();
			boolean result = false;
			for(int i=0; i<writes.length; i++) {
				InstructionLocation write = writes[i];
				if(method.maps(location, write, StackLocation.getSecondFromTopCellIndex())) { /* we are writing objects so no need to worry about doubles or longs on the stack */
					invocation.addWrittenDataMember(new AccessedPropagator(element, write));
					result = true;
				}
			}
			return result;
		} else {
			invocation.addWrittenDataMember(new AccessedPropagator(element));
			return true;
		}
	}
	
	private boolean handleArrayReads(MethodInvocationLocation location, ArrayElement element, MethodInvocation invocation) {
		Method method = invocation.getMethod();
		if(method.useIntraProceduralAnalysis()) {
			InstructionLocation reads[] = method.getArrayReads();
			boolean result = false;
			for(int i=0; i<reads.length; i++) {
				InstructionLocation read = reads[i];
				if(method.maps(location, read, StackLocation.getNextToTopCellIndex())) {
					element.addReadingMethod(new AccessedPropagator(invocation, read));
					result = true;
				}
			}
			return result;
		} else {
			element.addReadingMethod(new AccessedPropagator(invocation));
			return true;
		}
	}
}
