/*
 * Created on Oct 26, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.test;

import java.io.IOException;

import com.ibm.ive.tools.japt.test.ConfigDocumentHandler.JRE;

public class AppInvocation {
	private boolean wasRun;
	private boolean done;
	private ProcessThread thread;
	AppRun app;
	JRE jre;
	
	AppInvocation(AppRun app, JRE jre) {
		this.app = app;
		this.jre = jre;
	}
	
	String getCommandLine() {
		String res = jre.java;
		if(jre.classPath != null && jre.classPath.length() > 0) {
			res += " -Xbootclasspath/p:" + jre.classPath;
		}
		res += " -cp \"" + app.appClassPath + "\" " + app.commandLine;
		return res;
	}
	
	void waitFor() throws InterruptedException {
		runApp();
		if(!done) {
			thread.join();
			done = true;
		}
	}
	
	boolean runApp() {
		boolean invoked = false;
		if(!wasRun) {
			synchronized(this) {
				if(!wasRun) {
					wasRun = invoked = true;
					String commandLine = getCommandLine();
					thread = new ProcessThread(commandLine);
					thread.start();
				}
			}
		}
		return invoked;
	}
	
	void waitTilDone() {
		while(!done) {
			try {
				waitFor();
			} catch(InterruptedException e) {}
		}
	}
	
	String getOutput() throws IOException, InterruptedException {
		return thread.getOutput();
	}
	
	String getErrorOutput() throws IOException, InterruptedException {
		return thread.getErrorOutput();
	}
}
