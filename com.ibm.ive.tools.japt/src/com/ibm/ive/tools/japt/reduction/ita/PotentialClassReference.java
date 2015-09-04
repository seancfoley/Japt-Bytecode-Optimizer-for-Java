package com.ibm.ive.tools.japt.reduction.ita;


public class PotentialClassReference {
	/**
	 * the location in the method where the reference occurs:
	 * the method is invoked, or the field or array element is written to, 
	 * or the location in the method which reads the field or array element
	 */
	final InstructionLocation location;
	final Clazz instructionTarget;
	
	PotentialClassReference(Clazz targetClass, InstructionLocation location) {
		this.location = location;
		this.instructionTarget = targetClass;
	}
	
	public String toString() {
		return "access to " + instructionTarget + " at " + location;
	}
}
