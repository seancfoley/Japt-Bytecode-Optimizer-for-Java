package com.ibm.ive.tools.japt.reduction.ita;

/**
 * @author sfoley
 *
 */
public interface MethodPotentialTargets {
	
	void findNewTargets(ReceivedObject obj, SpecificMethodInvocation inv) throws GenericInvocationException;
	
	void findStaticTargets(SpecificMethodInvocation inv);
}
