package com.ibm.ive.tools.japt.reduction.ita;

import java.util.LinkedList;

import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;
import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator.PropagationAction;



/**
 * @author sfoley
 *
 */
public class ReadingPropagationTargets {

	/**
	 *  contains methods which read from this field 
	 */
	private LinkedList readingMethods;
	
	void propagate(ReceivedObject obj, DataMember from) {
		if(readingMethods == null) {
			return;
		}
		int size = readingMethods.size();
		for(int i=0; i<size; i++) {
			AccessedPropagator access = (AccessedPropagator) readingMethods.get(i);
			MethodInvocation method = (MethodInvocation) access.propagator;
			ReceivedObject sentObject;
			PropagatedObject propObject = obj.getObject();
			InstructionLocation location = access.location;
			if(method.useIntraProceduralAnalysis()) {
				/* note the object is on the top of the stack after the field access instruction that retrieves it, whether it be
				 * a field access or an array access 
				 */
				sentObject = new TargetedObject(propObject, 
						from.getRepository().locationPool.getStackLocation(location.instructionIndex + 1, StackLocation.getTopCellIndex()));
			} else {
				sentObject = propObject;
			}
			if(!method.hasPropagated(sentObject)) {
				PropagationAction action = ObjectPropagator.READ;
				if(method.context.readBarrier(propObject, from, method, location, action)) {
					method.addPropagatedObject(sentObject, action, from);
				}
			}
		}
	}
	
	boolean hasReaders() {
		return readingMethods != null && readingMethods.size() > 0;
	}
	
	void addAccessor(AccessedPropagator accessor) {
		if(readingMethods == null) {
			readingMethods = new LinkedList();
		}
		readingMethods.add(accessor);
	}
	
	void migrate(ReadingPropagationTargets to) {
		if(to.readingMethods == null) {
			to.readingMethods = readingMethods;
			readingMethods = null;
		}
		else {
			to.readingMethods.addAll(readingMethods);
			readingMethods = null;
		}
	}
}
