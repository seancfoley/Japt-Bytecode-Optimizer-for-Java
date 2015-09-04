package com.ibm.ive.tools.japt;

import java.io.IOException;
import java.io.OutputStream;

import com.ibm.jikesbt.BT_Item;


/**
 *
 * Represents a class that can accept Japt message output from the program itself
 * or from one of its extensions
 * @author sfoley
 */
public interface Logger {
	
	final String endl = BT_Item.endl();
	
	final OutputStream nullStream = new OutputStream() {
		public void write(int b) throws IOException {}

		public void write(byte[] b, int off, int len) throws IOException {}

		public void write(byte[] b) throws IOException {}
	};
	
	/**
	 * Information about what has been done by japt and its extensions
	 */
	void logInfo(String info);
	
	/**
	 * Information that should be presented to the user directly.  Similar to logInfo,
	 * but used for important information and/or less detailed information: status information.
	 */
	void logStatus(String status);
	
	/**
	 * A warning message regarding the results produced by an extension's execution.
	 * Something might have gone wrong or might have been unexpected.
	 */
	void logWarning(String warning);
	
	/**
	 * An error message regarding the results produced by an extension's execution
	 */
	void logError(String error);
	
	/**
	 * A message giving information regarding the progress of an extension's execution
	 */
	void logProgress(String progress);
	
	/**
	 * Flush all output
	 */
	void flush();
	
	/**
	 * Close this logger
	 */
	void close();
	 
	
}
