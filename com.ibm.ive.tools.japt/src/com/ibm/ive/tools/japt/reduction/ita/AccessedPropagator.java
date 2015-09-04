package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_CodeAttribute;


public class AccessedPropagator {
	/**
	 * a method invocation, field, or array element 
	 */
	public final ObjectPropagator propagator;
	
	/**
	 * the location in the method where the method is invoked, or the field or array element is written to, 
	 * or
	 * the location in the method which reads the field or array element
	 */
	public final InstructionLocation location;
	
	public AccessedPropagator(ObjectPropagator propagator, InstructionLocation location) {
		this.propagator = propagator;
		this.location = location;
	}
	
	public AccessedPropagator(ObjectPropagator propagator) {
		this(propagator, null);
	}
	
	public String toString() {
		return propagator + " accessed at " + location;
	}
	
	public String toString(Method within) {
		BT_CodeAttribute code = within.getCode();
		int num = code.findLineNumber(location.instruction);
		if(num > 0) {
			return propagator + " accessed from " + within + 
				" at line number " + num + " in " + 
				within.getDeclaringClass().getUnderlyingType().getSourceFile();
		}
		return toString();
	}
	
	public boolean equals(Object o) {
		if(o instanceof AccessedPropagator) {
			AccessedPropagator other = (AccessedPropagator) o;
			if(!propagator.equals(propagator)) {
				return false;
			}
			if(location == null) {
				return other.location == null;
			}
			return location.equals(other.location);
		}
		return false;
	}
}
