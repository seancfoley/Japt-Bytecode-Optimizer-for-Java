/*
 * Created on Feb 23, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.test;

import junit.framework.Assert;

import com.ibm.ive.tools.japt.FileLogger;

public class TestLogger extends FileLogger {
	boolean onReload;
	StringBuffer lineBuffer = new StringBuffer();
	StringBuffer warnBuffer = new StringBuffer();
	
	public void logProgress(String progress) {
		logString(progress);
	}

	public void logError(String error){
		logString(error);
		String line = bufferByLine(lineBuffer, error);
		if(line != null) {
			Assert.fail((onReload ? "reload: " : "initial load: ") + line);
		}
	}
	
	public void logWarning(String warn){
		logString(warn);
		String line = bufferByLine(warnBuffer, warn);
		if(line != null) {
			Assert.fail((onReload ? "reload: " : "initial load: ") + line);
		}
	}
}
