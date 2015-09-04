/*
 * Created on Mar 4, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FileLogger extends NullLogger {

	static PrintStream nullPrintStream = new PrintStream(nullStream);
	PrintStream fileStream = nullPrintStream;
	
	/**
	 * 
	 */
	public FileLogger() {}
	
	public void setFile(String logFileFullPath) throws FileNotFoundException {
		fileStream = new PrintStream(
						new BufferedOutputStream(
								new FileOutputStream(logFileFullPath)));
	}
	
	public PrintStream getFileStream() {
		flush();
		return fileStream;
	}
	
	
	public void logString(String string) {
		fileStream.print(string);
	}

	public void logInfo(String info) {
		logString(info);
	}
	
	public void logStatus(String status) {
		logString(status);
	}

	public void logWarning(String warning){
		logString(warning);
	}

	public void logError(String error){
		logString(error);
	}

	public void flush() {
		fileStream.flush();
	}

	public void close() {
		fileStream.close();
	}
	
	/**
	 * Data sent to the logger is not sent on a line-by-line basis.
	 * In order to buffer the data line-by-line, construct a StringBuffer
	 * and pass it to this method with the data being logged.  This method
	 * will append logged data to the buffer until a new-line is encountered,
	 * at which point the completed line will be returned as a String, while
	 * the buffer will contain any data to follow on the next line.
	 * If multiple new lines appear then all completed lines are returned as a String.
	 * The returned line(s) will not contain the final terminating new-line character.
	 * @param currentLine
	 * @param s
	 * @return
	 */
	public String bufferByLine(StringBuffer currentLine, String s) {
		int newLine = s.lastIndexOf(endl);
		String result = null;		
	  if(newLine == -1) {
		  currentLine.append(s);
	  }
	  else {
		  currentLine.append(s.substring(0, newLine));
		  result = currentLine.toString();
		  currentLine.setLength(0);
		  if(newLine < s.length() - 1) {
			  currentLine.append(s.substring(newLine + 1));
		  }
	  }
	  return result;
	}
}
