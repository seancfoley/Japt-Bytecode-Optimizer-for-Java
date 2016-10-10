/*
 * Created on Jan 13, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.test;

import java.util.HashMap;

import com.ibm.ive.tools.japt.test.ConfigDocumentHandler.JRE;


public class AppRun {
	
	final String appClassPath; 
	final String commandLine;
	final String stdOutPath;
	final String stdErrPath;
	final int fromEndIndex;
	final int startIndex;
	final int endIndex;
	private HashMap appInvocations = new HashMap();
	
	AppRun(String appClassPath, String commandLine, String stdOutPath, String stdErrPath,
			int fromEndIndex, int startIndex, int endIndex) {
		this.appClassPath = appClassPath;
		this.commandLine = commandLine;
		this.fromEndIndex = fromEndIndex;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.stdOutPath = stdOutPath;
		this.stdErrPath = stdErrPath;
	}
	
	
	AppInvocation getAppInvocation(JRE jre) {
		AppInvocation result = (AppInvocation) appInvocations.get(jre);
		if(result == null) {
			result = new AppInvocation(this, jre);
			appInvocations.put(jre, result);
		}
		return result;
	}
	
}
