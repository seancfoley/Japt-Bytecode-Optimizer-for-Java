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
import java.util.HashMap;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_ExceptionsAttribute;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * RefactorExtension feature class which provides refactoring
 * of duplicate and/or redundant throws declarations of methods.
 */
public class ThrowsClauseRefactor extends GenericRefactor {
	/**
	 * Internal method for embedding copyright
	 */
	static String copyright() {
		return Copyright.IBM_COPYRIGHT;
	}

	// Set true will force remove all Exception (throws) declarations
	// Note: Classes altered in this way would no longer satisfy the
	// requirements of the java language specification
	private boolean removeAllOption = false;

	// Set BT_Class object representing java.lang.Error class.
	final private BT_Class clsError = getRepository().linkTo("java.lang.Error"); //$NON-NLS-1$
	// Set BT_Class object representing java.lang.RuntimeException class.
	final private BT_Class clsRE = getRepository().linkTo("java.lang.RuntimeException"); //$NON-NLS-1$

	/**
	 * Constructs a ThrowsRefactor.  Candidate classes are sorted by default upon refactoring.
	 * @param repository the repository with the loaded classes, usually given through Japt extension.
	 * @param logger the output logger for message output, usually given through Japt extension.
	 */
	public ThrowsClauseRefactor(JaptRepository repository, Logger logger) {
		this(repository, logger, false);
	}

	/**
	 * Constructs a ThrowsRefactor with removeAllOption set. 
	 * @param repository the repository with the loaded classes, usually given through Japt extension.
	 * @param logger the output logger for message output, usually given through Japt extension.
	 */
	public ThrowsClauseRefactor(JaptRepository repository, Logger logger, boolean removeAllOption) {
		super(repository, logger);
		setSort(true);
		this.removeAllOption = removeAllOption;
	}

	private int numTotalThrows = 0;        // count total number of throw declarations found
	private int numTotalClasses = 0;       // count total no. of internal classes
	private int numTotalClzWithThrows = 0; // count total no. of internal classes with throws

	private int numRemovedThrows = 0;      // count number of removed throws declarations
	private int numRemovedAttrs = 0;       // count number of removed Exception attributes
	private int updatedClassNum = 0;       // count number of refactored classes

	public void refactor() {
		super.refactor();
		logStatus("Removed " + numRemovedThrows + " throw declarations"); //$NON-NLS-1$ //$NON-NLS-2$
		logStatus("\tand " + numRemovedAttrs  + " throw attributes"); //$NON-NLS-1$ //$NON-NLS-2$
		logStatus("\tfrom " + updatedClassNum + " classes"); //$NON-NLS-1$ //$NON-NLS-2$
		logStatus("\tcandidate " + numTotalClzWithThrows + " out of "+numTotalClasses+" internal classes with total "+numTotalThrows+" throws"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.refactor.GenericRefactor#refactor(com.ibm.jikesbt.BT_Class)
	 */
	public void refactor(BT_Class clz) {
		if (clz == null) throw new IllegalArgumentException("clz can not be null"); //$NON-NLS-1$
		numTotalClasses++;

		int removedNum = 0;
		boolean clzUpdated = false;
		boolean clzIsCandidate = false;
		
		BT_MethodVector bmv = clz.getMethods();
		if (bmv == null) return;

		Enumeration methods = bmv.elements();
		BT_Method mtd;
		BT_ClassVector clv;
		while (methods.hasMoreElements()) {
			mtd = (BT_Method) methods.nextElement();
			
			// Routine to collect statistics of refactor
			clv = mtd.getDeclaredExceptionsVector();
			if ((clv != null) && (clv.size()>0)) {
				clzIsCandidate = true;
				numTotalThrows += clv.size();
			}
			
			// Actual refactor logic
			if (removeAllOption) {
				removedNum = removeAllThrows(mtd);
			} else {
				removedNum = removeRedundantThrows(mtd);
			}
			
			// Routine to remove Exception Attribute when necessary,
			// and collect statistics of refactor
			if (removedNum > 0) {
				clzUpdated = true;
				numRemovedAttrs  += removeExceptionAttributesIfEmpty(mtd);
				numRemovedThrows += removedNum;
				clzIsCandidate = true;
				logStatusIfVerbose("\tfrom Method="+mtd); //$NON-NLS-1$
			}
		}
		if (clzUpdated) {
			updatedClassNum++;
		}
		if (clzIsCandidate) numTotalClzWithThrows++;
	}

    /**
	 * Removes Exception Attributes from a given method which has no declarared throws.
	 * @param mtd BT_Method object to remove Exception Attributes from.
	 * @return number of removed Exception Attributes
	 */
	public int removeExceptionAttributesIfEmpty(BT_Method mtd) {
		if (mtd == null) throw new IllegalArgumentException("mtd can not be null"); //$NON-NLS-1$
		
		int removedAttrs = 0;
		boolean result;
		
		BT_ClassVector exceptionClasses = mtd.getDeclaredExceptionsVector();
		if (DEBUG) logStatus(mtd, " throws:"+exceptionClasses); //$NON-NLS-1$
		if (exceptionClasses == null || exceptionClasses.size() != 0) return 0; // do nothing. 0 classes removed
	
		// Remove Exceptions attributes
		BT_ExceptionsAttribute ea = (BT_ExceptionsAttribute) mtd.getAttributes().getAttribute(BT_ExceptionsAttribute.ATTRIBUTE_NAME);
		if (ea != null) { 
			result = mtd.getAttributes().removeElement(ea); // ea.remove();
			if (result) {
				removedAttrs++;
				logStatusIfVerbose(" Removed attributes ["+ea.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return removedAttrs;
	}

	/**
	 * Removes all throws declarations from a given method.
     * @param mtd BT_Method object to remove all throws declarations from.
     * @return number of removed throws.
     */
	public int removeAllThrows(BT_Method mtd) {
		if (mtd == null) throw new IllegalArgumentException("mtd can not be null"); //$NON-NLS-1$

		BT_ClassVector exceptionClasses = mtd.getDeclaredExceptionsVector();
		if (exceptionClasses == null || exceptionClasses.size() == 0) return 0; // do nothing. 0 classes removed

		int removedNum = exceptionClasses.size();
		if (removedNum > 0) logStatusIfVerbose(mtd, " removing all throws:"+exceptionClasses); //$NON-NLS-1$
		exceptionClasses.removeAllElements();
		return removedNum;
	}

	/**
	 * Removes redundant throws declaration from a given method.
	 * @param mtd BT_Method object to remove redundant throws from.
	 * @return number of removed redundant throws.
	 */
	public int removeRedundantThrows(BT_Method mtd) {
		if (mtd == null) throw new IllegalArgumentException("mtd can not be null"); //$NON-NLS-1$
		
		BT_ClassVector exceptionClasses = mtd.getDeclaredExceptionsVector();
		if (DEBUG) logStatus(mtd, " throws:"+exceptionClasses); //$NON-NLS-1$
		if (exceptionClasses == null || exceptionClasses.size() == 0) return 0; // do nothing. 0 classes removed		
	
		int removed = removeRedundantThrows(exceptionClasses);
		if (DEBUG) logStatus(mtd, " Removed " + removed + " redundant throws declarations"); //$NON-NLS-1$ //$NON-NLS-2$
		return removed;
	}

	/**
	 * Removes redundant throws declaration (including duplicates) from all methods of classes
	 * in the given class list.  Note again that this method removes all duplicates as well.
	 * @param cv BT_ClassVector object containing list of classes to remove redundant throws from.
	 * @return number of removed redundant throws.
	 */
	public int removeRedundantThrows(BT_ClassVector cv) {
		if (cv == null) throw new IllegalArgumentException("cv can not be null"); //$NON-NLS-1$
		if (cv.isEmpty()) return 0;
		
		int removed = 0; //holds tally of removed items
	
		// Remove Duplicate throws before any refactoring of redundant throws
		removed = removeDuplicateThrows(cv);
		if (DEBUG) logStatus("  Removed "+removed+" duplicate throws declarations"); //$NON-NLS-1$ //$NON-NLS-2$
	
		// Now remove redundancies
		BT_ClassVector removeList = new BT_ClassVector();
		BT_Class base = null;
		BT_Class comp = null;
		for (int i=0; i<cv.size(); i++) {
			base = cv.elementAt(i);
			if (DEBUG) logStatus(" base="+base); //$NON-NLS-1$
			
			// Already listed as removed, skip it
			if (removeList.contains(base)) {
				if (DEBUG) logStatus("  skipping ["+base+"] since already marked as redundant"); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}
			
			if ((base == clsError) || base.isDescendentOf(clsError)
					|| (base == clsRE) || base.isDescendentOf(clsRE)) {
				logStatusIfVerbose(" Removing throws ["+base+"] since desecendent of either ["+clsError+"] or ["+clsRE+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				removeList.addUnique(base);
				continue;
			}
			for (int j=i+1; j<cv.size(); j++) {
				comp = cv.elementAt(j);
				if (DEBUG) logStatus("  comp="+comp); //$NON-NLS-1$
				
				// Already listed as removed, skip it
				if (removeList.contains(comp)) {
					if (DEBUG) logStatus("  skipping ["+comp+"] since already marked as redundant"); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
				
				// Should never occur since we eliminated duplicates in above
				// but if it happens, warn only and ignore it for safety.
				if (base == comp) {
					logWarning("  duplicate declarations found:"+base); //$NON-NLS-1$
					continue; // ignore duplicates
				}
				
				// Remove class if its a sub-class of another Exception in throws clause
				if (base.isDescendentOf(comp)) {
					logStatusIfVerbose(" Removing throws ["+base+"] since desecendent of declared Exception ["+comp+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					removeList.addUnique(base);
					break;
				}
				// Remove its sub-class Exception in the throws clause
				if (base.isAncestorOf(comp)) {
					logStatusIfVerbose(" Removing throws ["+comp+"] since desecendent of declared Exception ["+base+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					removeList.addUnique(comp);
					continue;
				}
			}
		}
		
		int _removed = 0;
		// Finally, remove declarations found from the throws clause
		if (removeList.size() > 0) _removed = removeAllOccurrencesOf(removeList, cv, true);
		if (DEBUG) logStatus("  Removed " + _removed + " redundant throws declarations"); //$NON-NLS-1$ //$NON-NLS-2$
		removed += _removed;
	
		return removed;
	}

	/**
	 * Removes duplicate throws declaration from a given method.
	 * @param mtd BT_Method object to remove duplicate throws from.
	 * @return number of removed duplicate throws.
	 */
	public int removeDuplicateThrows(BT_Method mtd) {
		if (mtd == null) throw new IllegalArgumentException("mtd can not be null"); //$NON-NLS-1$

		BT_ClassVector exceptionClasses = mtd.getDeclaredExceptionsVector();
		if (exceptionClasses == null || exceptionClasses.size() == 0) return 0; // do nothing. 0 classes removed

		return removeDuplicateThrows(exceptionClasses);
	}

	/**
	 * Removes duplicate throws declaration from all methods of classes
	 * in the given class list.
	 * @param cv BT_ClassVector object containing list of classes to remove duplicate throws from.
	 * @return number of removed duplicate throws.
	 */
	public int removeDuplicateThrows(BT_ClassVector cv) {
		if (cv == null) throw new IllegalArgumentException("cv can not be null"); //$NON-NLS-1$

		BT_ClassVector dupList = new BT_ClassVector();
		HashMap dupNum = new HashMap();
		int occurred = 0;
		int removed = 0;
		
		BT_Class base = null;
		BT_Class comp = null;		

		/* Look for duplicate classes */
		for (int i=0; i<cv.size(); i++) {
			base = cv.elementAt(i);
			if (dupList.contains(base)) { // Already listed to have duplicate so increment num of occurrences
				occurred = ((Integer)dupNum.get(base)).intValue() + 1;
				dupNum.put(base, new Integer(occurred));
				continue;
			}

			for (int j=i+1; j<cv.size(); j++) {
				comp = cv.elementAt(j);
				if (base == comp) { // Found duplicate
					dupList.addUnique(base);
					dupNum.put(base, new Integer(0));
					break;
				}
			}
		}
		
		int _removed = 0;
		/* Remove duplicates */
		for (int i=0; i<dupList.size(); i++) {
			_removed = 0;
			base = dupList.elementAt(i);
			occurred = ((Integer)dupNum.get(base)).intValue();
			for (int j=0; j<occurred; j++) {
				if (cv.removeElement(base)) {
					_removed++;
				}
			}
			logStatusIfVerbose(" Removed "+_removed+" duplicate occurrence(s) of "+base); //$NON-NLS-1$ //$NON-NLS-2$
			removed += _removed;
		}
		return removed;
	}
}
