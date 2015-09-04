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

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;

/**
 * RefactorExtension feature class which provides refactoring
 * of redundant interface (implements) declarations in classes.
 */
public class InterfaceRefactor extends GenericRefactor {
	/**
	 * Internal method for embedding copyright
	 */
	static String copyright() {
		return Copyright.IBM_COPYRIGHT;
	}

	/**
	 * Constructs an InterfaceRefactor.  Candidate classes are sorted by default upon refactoring.
	 * @param repository the repository with the loaded classes, usually given through Japt extension.
	 * @param logger the output logger for message output, usually given through Japt extension.
	 */
	public InterfaceRefactor(JaptRepository repository, Logger logger) {	
		super(repository, logger);
		setSort(true);
	}

	private int numTotalImplements = 0;        // count total number of throw declarations found
	private int numTotalClasses = 0;           // count total no. of internal classes
	private int numTotalClzWithImplements = 0; // count total no. of internal classes with implements

	private int numRemovedInterfaces = 0;      // Keeps track of number of removed Interfaces
	private int updatedClassNum = 0;           // Keeps track of number of refactored classes

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.refactor.GenericRefactor#refactor()
	 */
	public void refactor() {
		super.refactor();
		logStatus("Removed " + numRemovedInterfaces + " implements interfaces from "+updatedClassNum+" classes"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logStatus("\tcandidate " + numTotalClzWithImplements + " out of "+numTotalClasses+" internal classes with total "+numTotalImplements+" implements"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.refactor.GenericRefactor#refactor(com.ibm.jikesbt.BT_Class)
	 */
	public void refactor(BT_Class clz) {
		if (clz == null) throw new IllegalArgumentException("clz can not be null"); //$NON-NLS-1$
		numTotalClasses++;
		
		// Get list of extend classes and implements interfaces
		BT_ClassVector declaredList = clz.getParents();
		
		BT_ClassVector removeList = new BT_ClassVector(); 
		BT_Class base = null;
		BT_Class comp = null;

		boolean clzHasImplements = false;
		
		if (DEBUG) {
			logStatus("=== Examining [" + clz + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			logStatus("\tdeclared extends classes/implements interfaces:" + declaredList); //$NON-NLS-1$
		}

		/* Check all declarations for redundancy */
		for(int i=0; i<declaredList.size(); i++) {
			base = declaredList.elementAt(i);
			if (base.isInterface()) {
				clzHasImplements = true;
				numTotalImplements++;
			}
			if (removeList.contains(base)) continue;  // Already removed class
										
			for(int j=i+1; j<declaredList.size(); j++) {
				comp = declaredList.elementAt(j);
				if (removeList.contains(comp)) continue;  // Already removed class

				// super interface declaration can be removed, since
				// their interfaces are contained in child interface
				if (comp.isInterface()) { 
					if (base.isDescendentOf(comp)) {
						logStatusIfVerbose("Removing interface ["+comp+"] since desecendent of declared interface ["+base+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$						
						removeList.addUnique(comp);
						continue;
					}
				}

				// same as above, but checking if base is super interface
				// or not
				if (base.isInterface()) {
					if (comp.isDescendentOf(base)) {
						logStatusIfVerbose("Removing interface ["+base+"] since desecendent of declared interface ["+comp+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$												
						removeList.addUnique(base);
						break;
					}
				}
			}
		}
		
		int _removed = 0;
		// Finally, remove implements declarations found
		if (removeList.size() > 0) {
			_removed = removeAllOccurrencesOf(removeList, declaredList, true);
			if (DEBUG) {
				logStatus("\tRemoved " + _removed + " redundant interface declarations"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				logStatusIfVerbose("\tfrom ["+clz+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			updatedClassNum++;
		} else {
			if (DEBUG) logStatus("\tNo redundant declarations found."); //$NON-NLS-1$
		}
		numRemovedInterfaces+=_removed;
		if (clzHasImplements) numTotalClzWithImplements++;
	}
}
