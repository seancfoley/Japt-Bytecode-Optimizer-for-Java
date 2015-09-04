/*
 * Created on Oct 5, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine;

import java.io.File;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.Option;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface OptionsFileReader {

	Option emptyOptions[] = new Option[0];
		
	void readOptionsFile(File file, String args[], Option initialOptions[]) throws CommandLineException;
	
}
