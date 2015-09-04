package com.ibm.ive.tools.japt.escape;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.ita.InstructionLocation;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Method;

public class InstantiationSite implements Comparable {
	final BT_Method containingMethod;
	final InstructionLocation location;
	final BT_Class instantiatedType; //note that we count lower dimensions just once each for MultiANewArrayIns
	
	InstantiationSite(BT_Method containingMethod, InstructionLocation location, BT_Class instantiatedType) {
		this.containingMethod = containingMethod;
		this.location = location;
		this.instantiatedType = instantiatedType;
	}
	
	public boolean isInternal() {
		BT_Class clazz = containingMethod.getDeclaringClass();
		JaptRepository rep = (JaptRepository) clazz.getRepository();
		boolean result = rep.isInternalClass(clazz);
		return result;
	}
	
	public int hashCode() {
		return containingMethod.hashCode() + location.hashCode() + instantiatedType.hashCode();
	}
	
	public int compareTo(Object other) {
		InstantiationSite otherSite = (InstantiationSite) other;
		int comparison = containingMethod.compareTo(otherSite.containingMethod);
		if(comparison == 0) {
			comparison = location.compareTo(otherSite.location);
			if(comparison == 0) {
				comparison = instantiatedType.compareTo(otherSite.instantiatedType);
			}
		}
		return comparison;
	}
	
	public boolean equals(Object other) {
		if(other instanceof InstantiationSite) {
			InstantiationSite otherSite = (InstantiationSite) other;
			return containingMethod.equals(otherSite.containingMethod)
				&& location.equals(otherSite.location)
				&& instantiatedType.equals(otherSite.instantiatedType);
		}
		return false;
	}
	
	public String toString() {
		return "instantiation of " + instantiatedType + " at " + location + " in " + containingMethod;
	}
}
