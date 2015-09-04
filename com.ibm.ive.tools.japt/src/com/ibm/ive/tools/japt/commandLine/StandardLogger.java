package com.ibm.ive.tools.japt.commandLine;

import java.io.PrintStream;

import com.ibm.ive.tools.japt.Logger;

/**
 * A logger implementation.  Errors are output to a single error print stream, while
 * information and warnings are output to an output print stream
 * @author sfoley
 */
public class StandardLogger implements Logger {

	private PrintStream outStream, errStream;
	
	/**
	 * A standard logger prints information and warnings to 
	 * the output stream and errors to the error stream
	 */
	public StandardLogger(PrintStream outStream, PrintStream errStream) {
		this.outStream = outStream;
		this.errStream = errStream;
	}
	
	/**
	 * A standard logger using the fields out and err of java.lang.System
	 */
	public StandardLogger() {
		this(System.out, System.err);
	}
	
	public PrintStream getOutputStream() {
		return outStream;		
	}

	public PrintStream getErrorStream() {
		return errStream;
	}
	
	private void outputToStream(PrintStream stream, String string) {
		stream.print(string);
		stream.flush();
	}
	
	public void logProgress(String progress) {
		outputToStream(outStream, progress);
	}
	
	public void logStatus(String status) {
		outputToStream(outStream, status);
	}
	
	public void logInfo(String info) {
		outputToStream(outStream, info);
	}
	
	public void logWarning(String warning) {
		outputToStream(outStream, warning);
	}
	
	public void logError(String error) {
		outputToStream(errStream, error);
	}
	
	
	public void flush() {
		errStream.flush();
		outStream.flush();
	}
	
	public void close() {
		flush();
		errStream.close();
		outStream.close();
	}

}
