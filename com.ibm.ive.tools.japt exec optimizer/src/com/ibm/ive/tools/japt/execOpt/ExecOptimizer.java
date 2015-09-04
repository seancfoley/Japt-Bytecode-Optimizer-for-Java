package com.ibm.ive.tools.japt.execOpt;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.Japt;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Messages;
import com.ibm.ive.tools.japt.commandLine.ExtensionDescriptor;

public class ExecOptimizer implements Component {
	public Messages messages = new Messages(this);
	
	public final String programName = "App Execution Optimizer";
	public final String packageName = "appExecOptimizer";
	public final String fullProgramName = "IBM Real Time Application Execution Optimizer for Java";
	public final String version = "1.0.0"; 
	public final String build = "20090520";
	
	/**
	 * collects the data required to start the Japt application.  This default
	 * implementation collects the data from the command line.  One the data
	 * is collected the runJapt method is called.
	 */
	public void run(JaptRepository rep, Logger logger, ExtensionDescriptor extensions[]) {
		long startTime = System.currentTimeMillis();
		Japt japt = new Japt(rep);
		japt.executeExtensions(extensions, logger);
		japt.logCompleted(logger, System.currentTimeMillis() - startTime);
	}
	
	public String getName() {
		return programName;
		//return messages.DESCRIPTOR;
	}
}
