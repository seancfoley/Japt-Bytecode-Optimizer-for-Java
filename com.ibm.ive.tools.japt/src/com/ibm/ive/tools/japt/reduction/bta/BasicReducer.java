package com.ibm.ive.tools.japt.reduction.bta;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.ive.tools.japt.reduction.xta.XTAReducer;

/**
 * @author sfoley
 *
 * Basic reduction can be attained from XTA reduction by overridding key behaviours.
 * Key elements:
 * -objects are not propagated at all
 * -all fields are accessed and all methods are called as soon as they are reached, there is no
 * evaluation at any time to determine if a method is callable, it is assumed to be callable if it is
 * reachable
 * 
 */
public class BasicReducer extends XTAReducer {

	BasicRepository basicRepository;
	
	/**
	 * Constructor for RTAReducer.
	 */
	public BasicReducer(JaptRepository repository, Logger logger, Messages messages) {
		super(repository, logger, messages);
	}
	
	protected Repository constructRepository(JaptRepository rep) {
		return basicRepository = new BasicRepository(this);
	}
	
	protected void outputIterationMessage(int iterationCounter, int numPropagated) {
		messages.BASIC_ITERATION_INFO.log(logger, new String[] {Integer.toString(iterationCounter), Integer.toString(numPropagated)});
	}
}
