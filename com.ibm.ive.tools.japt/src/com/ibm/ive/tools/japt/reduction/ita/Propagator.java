package com.ibm.ive.tools.japt.reduction.ita;


/**
 * @author sfoley
 * 
 * Represents any object that propagates the ITA algorithm.
 *  
 * A propagator will cause any of its constituent elements to propagate themselves.
 * 
 * Propagation means to take the next step(s) in execution of the algorithm.
 *
 */
interface Propagator {

	
	/**
	 * Propagate.  Will propagate things that were propagated to this
	 * object by other propagators.
	 * @return true if something was propagated, false otherwise
	 */
	abstract boolean doPropagation() throws PropagationException;
	
}
