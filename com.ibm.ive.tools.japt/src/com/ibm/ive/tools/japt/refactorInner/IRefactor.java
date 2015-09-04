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

import com.ibm.ive.tools.japt.ExtensionException;

/**
 * All RefactorExtension feature (option) providers should 
 * implement this interface
 */
public interface IRefactor {
	/**
	 *  Internal variable for embedding copyright
	 */ 
	static final String copyright = Copyright.IBM_COPYRIGHT_SHORT;
	
	/**
	 * Removes redundant code (interfaces, throws clauses, inner classes, etc.. )
	 * in classes
	 * @throws ExtensionException the extension failed to complete its class and resource manipulations
	 */ 
	public void refactor() throws ExtensionException;
}
