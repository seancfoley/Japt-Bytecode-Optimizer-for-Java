package com.ibm.ive.tools.commandLine;

/**
 * @author sfoley
 * <p>
 * Represents an error on the command line
 */
public class CommandLineException extends Exception {

	public Exception exception;
	
	public CommandLineException() {}

	public CommandLineException(String detailMessage) {
		super(detailMessage);
	}
	
	public CommandLineException(Exception e) {
		this.exception = e;
	}

}
