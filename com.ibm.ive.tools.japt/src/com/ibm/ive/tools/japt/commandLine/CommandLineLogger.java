/*
 * Created on Mar 4, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import com.ibm.ive.tools.japt.FileLogger;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CommandLineLogger extends FileLogger {

	private PrintStream outStream, errStream;
	
	/**
	 * A logger using the fields out and err of java.lang.System
	 */
	public CommandLineLogger(String logFileFullPath) throws FileNotFoundException {
		this();
		setFile(logFileFullPath);
	}
	
	public CommandLineLogger() {
		this.outStream = System.out;
		this.errStream = System.err;
	}
	
	public PrintStream getOutputStream() {
		flush();
		return outStream;		
	}

	public PrintStream getErrorStream() {
		flush();
		return errStream;
	}
	
	private void outputToStream(PrintStream stream, String string) {
		stream.print(string);
		stream.flush();
	}

	public void logStatus(String status) {
		outputToStream(outStream, status);
		super.logStatus(status);
	}
	
	public void logProgress(String progress) {
		outputToStream(outStream, progress);
	}
	
	public void logWarning(String warning){
		outputToStream(outStream, warning);
		super.logWarning(warning);
	}

	public void logError(String error){
		outputToStream(errStream, error);
		super.logError(error);
	}

	public void flush() {
		super.flush();
		errStream.flush();
		outStream.flush();
	}
}
