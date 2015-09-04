/*
 * Created on Jun 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine;

import java.io.FileNotFoundException;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class QuietCommandLineLogger extends CommandLineLogger {

	/**
	 * @param logFileFullPath
	 * @throws FileNotFoundException
	 */
	public QuietCommandLineLogger(String logFileFullPath)
		throws FileNotFoundException {
		super(logFileFullPath);
	}

	public QuietCommandLineLogger() {}

	public void logProgress(String progress) {}
}
