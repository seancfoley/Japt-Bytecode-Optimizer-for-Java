/*
 * Created on May 19, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reduction;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class GenericReducer {

	protected final Logger logger;
	public final JaptRepository repository;
	protected EntryPointLister entryPointLister;
	protected boolean doNotMakeClassesAbstract;
	protected boolean alterClasses = true;
	
	/**
	 * 
	 */
	public GenericReducer(JaptRepository repository, Logger logger) {
		this.logger = logger;
		this.repository = repository;
	}

	abstract protected void reduce();
	
}
