/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Japt Refactor Extension
 *
 * Copyright IBM Corp. 2008
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U. S. Copyright Office.
 */
package com.ibm.ive.tools.japt.refactorInner;

import java.util.Enumeration;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Method;

/**
 * Adaptor class with common refactor related
 * utility methods
 */
abstract public class GenericRefactor implements IRefactor {
	/**
	 * Internal method for embedding copyright
	 */
	static String copyright() {
		return Copyright.IBM_COPYRIGHT;
	}

	// DEBUG flag
	final static boolean DEBUG = false;

	// Verbose flag
	private boolean verbose = false;

	private JaptRepository repository = null;
	private Logger logger = null;

	private String logPrefix = "[refactorInner] ";  //$NON-NLS-1$ //Default log Prefix
	static String LINESEP = System.getProperty("line.separator"); //$NON-NLS-1$
	private boolean sortClasses = false;     // set true will sort candidate classes upon refactoring

	/**
	 * Constructs a GenericRefactor.
	 * @param repository the repository with the loaded classes, usually given through Japt extension.
	 * @param logger the output logger for message output, usually given through Japt extension.
	 */
	public GenericRefactor(JaptRepository repository, Logger logger) {
		if (repository == null || logger == null) throw new IllegalArgumentException("repository or logger cannot be null"); //$NON-NLS-1$
		this.repository = repository;
		this.logger = logger;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.refactor.IRefactor#refactor()
	 */
	public void refactor() {
		BT_ClassVector cv = getCandidateClasses();
		if (sortClasses) cv.sort();
		refactor(cv);
	}

	/**
	 * Refactors each of the classes in the given class list (BT_ClassVector).
	 * @param cv BT_ClassVector object with list of classes to refactor.
	 */
	public void refactor(BT_ClassVector cv) {
		Enumeration classes = cv.elements();
		while (classes.hasMoreElements()) {
			refactor((BT_Class)classes.nextElement());
		}
	}

	/**
	 * Refactor the given class.
	 * @param clz BT_Class object to refactor.
	 */ 
	abstract public void refactor(BT_Class clz);

	/**
	 * Check if the given class is a candidate to be refactored or not.
	 * @param clz BT_Class object to check.
	 * @return true if given class should be refactored.
	 */
	public boolean isCandidateClass(BT_Class clz) {
		if (clz != null) {
			return (!clz.isStub() && !clz.isArray() && !clz.isPrimitive())
				&& !clz.getName().equals("java.lang.Object"); //$NON-NLS-1$
		}
		return false;
	}
	
	/**
	 * Returns a BT_ClassVector containing classes to be refactored.
	 * @return BT_ClassVector containing classes to be refactored.
	 */
	public BT_ClassVector getCandidateClasses() {
		BT_ClassVector cv = repository.getInternalClasses();
		BT_ClassVector nonCandidates = new BT_ClassVector();
		
		BT_Class clz = null;
		Enumeration classes = cv.elements();
		while (classes.hasMoreElements()) {
			clz = (BT_Class) classes.nextElement();
			if (!isCandidateClass(clz)) nonCandidates.addElement(clz);
		}
		removeAllOccurrencesOf(nonCandidates, cv, false);
		return cv;
	}

	/**
	 * Removes all occurrences of classes in removeList from in.
	 * @param removeList BT_ClassVector object containing classes to be removed from in.
	 * @param in Target BT_ClassVector object from which clasess will be removed.
	 * @param log Set true will enable verbose output to Japt Logger.
	 * @return number of classes removed.
	 */
	public int removeAllOccurrencesOf(BT_ClassVector removeList, BT_ClassVector in, boolean log) {
		if (removeList == null) throw new IllegalArgumentException("removeList can not be null"); //$NON-NLS-1$
		if (in == null) throw new IllegalArgumentException("in can not be null"); //$NON-NLS-1$
		
		int removed = 0;
		boolean result = true;
		BT_Class cls = null;
		Enumeration removeClasses = removeList.elements();
		while (removeClasses.hasMoreElements()) {
			cls = (BT_Class)removeClasses.nextElement();
			result = in.removeElement(cls);
			if (result) {
				if (log && DEBUG) logStatus("\tRemoved " + cls); //$NON-NLS-1$
				removed++;
			}
		}
		return removed;
	}

	public JaptRepository getRepository() {
		return repository;
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 * Sets prefix String to be placed before all log output.
	 * @param prefix String to be placed before all log output.
	 */
	final public void setLogPrefix(String prefix) {
		logPrefix = prefix;
	}

	/**
	 * Logs msg to Japt Logger Status stream.
	 * @param msg Log message string.
	 */
	public void logStatus(String msg) {
		logger.logStatus(logPrefix + msg + LINESEP);
	}

	/**
 	 * Logs msg to Japt Logger Status stream with method information
	 * @param mtd Method object of which information should be included in log.
	 * @param msg Log message string.
	 */
	public void logStatus(BT_Method mtd, String msg) {
		logStatus("Method["+mtd.fullName()+mtd.getSignature()+"]"+ msg); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Logs msg to Japt Logger Waring stream.
	 * @param msg Log message string.
	 */
	public void logWarning(String msg) {
		logger.logWarning(logPrefix + msg + LINESEP);
	}

	/**
	 * Logs msg to Japt Logger Warning stream with method information
	 * @param mtd Method object of which information should be included in log.
	 * @param msg Log message string.
	 */
	public void logWarning(BT_Method mtd, String msg) {
		logWarning("Method["+mtd.fullName()+mtd.getSignature()+"]"+ msg); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Sets verbose on/off.
	 * @param on Set true to turn on verbose
	 */
	final public void setVerbose(boolean on) {
		verbose = on;
	}

	/**
	 * Gets verbose on/off.
	 * Note: DEBUG flag turns verbose on automatically
	 * @return true - if verbose flag is on, false - if verbose flag is off
	 */	
	final public boolean getVerbose() {
		if (DEBUG) return true;
		return verbose;
	}
	
	/**
	 * Logs msg to Japt Logger Status stream, only if verbose flag is set to true
	 * Note: DEBUG flag turns verbose on automatically
	 * @param msg Log message string.
	 */
	public void logStatusIfVerbose(String msg) {
		if (DEBUG || verbose) logStatus(msg);
	}

	/**
 	 * Logs msg to Japt Logger Status stream with method information, only if verbose flag is set to true
 	 * Note: DEBUG flag turns verbose on automatically
	 * @param mtd Method object of which information should be included in log.
	 * @param msg Log message string.
	 */
	public void logStatusIfVerbose(BT_Method mtd, String msg) {
		if (DEBUG || verbose) logStatus("Method["+mtd.fullName()+mtd.getSignature()+"]"+ msg); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Toggle sort flag for controlling whether to sort candidate classes upon call to refactor() or not.
	 * @param sort set true will sort candidate classes list upon refactoring (call to refactor()).
	 */
	final public void setSort(boolean sort) {
		sortClasses = sort;
	}
}
