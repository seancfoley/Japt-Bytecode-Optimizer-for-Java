package com.ibm.ive.tools.japt.reduction.rta;

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
 * -objects are propagated on a global scale:  the same set of instantiated objects is assumed
 * available to all methods and fields.  So as soon as a new object is created, it is propagated to 
 * all methods and fields.
 * -since all objects are available to all fields and methods once instantiated, there is no point propagating
 * objects on a local scale, so methods and fields do not keep track of methods called and fields accessed.  
 * Exception objects are not thrown either.  The propagators are not linked together at all.  
 * - the other rules of XTA apply: methods are called and fields are accessed based upon whether there
 * is an object of the appropriate type available.
 * 
 */
public class RTAReducer extends XTAReducer {

	RTARepository rtaRepository;
	
	/**
	 * Constructor for RTAReducer.
	 */
	public RTAReducer(JaptRepository repository, Logger logger, Messages messages) {
		super(repository, logger, messages);
	}
	
	protected Repository constructRepository(JaptRepository rep) {
		return rtaRepository = new RTARepository(this);
	}
	
	protected boolean doIteration(Repository rep, int iterationCounter) {
		boolean one = super.doIteration(rep, iterationCounter);
		boolean two = rtaRepository.migrateObjects();
		return one || two;
	}

	protected void outputIterationMessage(int iterationCounter, int numPropagated) {
		messages.RTA_ITERATION_INFO.log(logger, new String[] {Integer.toString(iterationCounter), Integer.toString(numPropagated)});
	}
}
