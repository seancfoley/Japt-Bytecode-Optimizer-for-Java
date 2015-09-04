package com.ibm.ive.tools.japt.reduction.ita;


public class PotentialTarget extends PotentialClassReference {
	final Member target;
	
	PotentialTarget(Clazz targetClass, Member target, InstructionLocation location) {
		super(targetClass, location);
		this.target = target;
		
	}
	
	public String toString() {
		return "access to " + target + " through " + instructionTarget + " at " + location;
	}
}
