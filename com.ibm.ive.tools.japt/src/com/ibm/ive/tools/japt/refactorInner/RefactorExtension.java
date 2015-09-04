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

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;

/**
 * Extension for Japt, which finds redundancies in classes
 * and refactors them.   
 *   - removes redundant interface declarations in classes
 *   - removes duplicate and/or redundant throws declarations in methods
 *   - refactor anonymous inner classe to outer class
 */
public class RefactorExtension implements CommandLineExtension {
	/**
	 * Internal method for embedding copyright
	 */
	static String copyright() {
		return Copyright.IBM_COPYRIGHT;
	}

	// Extension name
	private String name = "refactorInner"; //$NON-NLS-1$

	// Options
	public FlagOption removeRedundantInterfaces = new FlagOption("removeRedundantInterfaces", "refactor redundant interface declarations"); //$NON-NLS-1$ //$NON-NLS-2$
	public FlagOption removeRedundantThrows = new FlagOption("removeRedundantThrows", "refactor duplicate/redundant throws declarations"); //$NON-NLS-1$ //$NON-NLS-2$
	public FlagOption removeAllThrows = new FlagOption("removeAllThrows", "*force* removal of all throws declarations. Be *aware* that this option may generate classes which no longer satisfy java language specification."); //$NON-NLS-1$ //$NON-NLS-2$	
	public FlagOption mergeInnerClasses = new FlagOption("mergeInnerClasses", "refactor anonymous inner classes"); //$NON-NLS-1$ //$NON-NLS-2$

	// Sub-Options
	public FlagOption verbose = new FlagOption("refactorInnerVerbose", "turns on verbose output for refactor extension"); //$NON-NLS-1$//$NON-NLS-2$

	/**
	 * Constructs a RefactorExtension. 
	 */
	public RefactorExtension() {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.commandLine.CommandLineExtension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {
				removeRedundantInterfaces, removeRedundantThrows, removeAllThrows, mergeInnerClasses, verbose
		};
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) {
		GenericRefactor feature = null;

		if (removeRedundantInterfaces.isFlagged()) {
			feature = new InterfaceRefactor(repository, logger);
			if (verbose.isFlagged()) feature.setVerbose(true);
			feature.refactor();
		}
		if (removeRedundantThrows.isFlagged()) {
			feature = new ThrowsClauseRefactor(repository, logger);
			if (verbose.isFlagged()) feature.setVerbose(true);
			feature.refactor();
		}
		if (removeAllThrows.isFlagged()) {
			feature = new ThrowsClauseRefactor(repository, logger, true);
			if (verbose.isFlagged()) feature.setVerbose(true);
			feature.refactor();
		}
		if (mergeInnerClasses.isFlagged()) {
			feature = new AnonymousInnerClassRefactor(repository, logger);
			if (verbose.isFlagged()) feature.setVerbose(true);
			feature.refactor();
		}
	}
}
