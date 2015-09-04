package com.ibm.ive.tools.japt.test;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.ibm.ive.tools.commandLine.CommandLineException;

/*
 * Created on Jun 15, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class JaptTester extends TestCase {

	private TestRun testRun;
	private static boolean quitAll;
	static boolean quitOnFirstFailure;
	
	public JaptTester(TestRun testRun) {
		super(testRun.toString());
		this.testRun = testRun;
	}
	
	public void runTest() throws IOException, InterruptedException {
		if(quitAll) {
			return;
		}
		try {
			doTest();
		} catch(AssertionFailedError e) {
			handleFailure();
			throw e;
		} catch(RuntimeException e) {
			handleFailure();
			throw e;
		} catch(Error e) {
			handleFailure();
			throw e;
		} catch(IOException e) {
			handleFailure();
			throw e;
		} catch(InterruptedException e) {
			handleFailure();
			throw e;
		}
	}

	/**
	 * 
	 */
	private void handleFailure() {
		if(quitOnFirstFailure) {
			quitAll = true;
		}
	}
	
	/**
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void doTest() throws InterruptedException, IOException {
		//create the logger
		TestLogger logger = new TestLogger(); 
		logger.setFile(testRun.japtLogPath);
		
		try {
			//run app and japted app in separate vms and compare the output
			compareRuns(testRun.getFirstApp(), testRun.runFirstJapt(logger), "first run: ");
			logger.flush();
			
			//now run a second time, running japt on the output of japt from the first run
			compareRuns(testRun.getSecondApp(), testRun.runSecondJapt(logger), "second run: ");
		} catch(CommandLineException e) {
			fail(e.toString());
		} finally {
			System.out.flush();
			logger.close();
		}
		//clean-up does not occur if an exception is thrown, as planned
		testRun.cleanUp();
	}

	/**
	 * @param appRun
	 * @param japtedRun
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void compareRuns(AppInvocation appRun, String japtedRun, String prefix) throws InterruptedException, IOException {
		ProcessThread japtThread = new ProcessThread(japtedRun);
		japtThread.start();
		
		appRun.runApp();
		appRun.waitFor();
		//wait for 15 minutes at most
		japtThread.join(15 * 60 * 1000);
		
		//the output should be the same
		String actualOutput = appRun.getOutput();
		String actualErrorOutput = appRun.getErrorOutput();
		
		String japtOutput = japtThread.getOutput();
		String japtErrorOutput = japtThread.getErrorOutput();
		
		int endIndex = appRun.app.endIndex;
		if(endIndex >= 0) {
			actualOutput = actualOutput.substring(0, Math.min(endIndex, actualOutput.length()));
			japtOutput = japtOutput.substring(0, Math.min(endIndex, japtOutput.length()));
		}
		int startIndex = appRun.app.startIndex;
		if(startIndex > 0) {
			actualOutput = actualOutput.substring(Math.min(startIndex, actualOutput.length()));
			japtOutput = japtOutput.substring(Math.min(startIndex, actualOutput.length()));
		}
		int fromEndIndex = appRun.app.fromEndIndex;
		if(fromEndIndex >= 0) {
			actualOutput = actualOutput.substring(Math.max(0, actualOutput.length() - fromEndIndex));
			japtOutput = japtOutput.substring(Math.max(0, japtOutput.length() - fromEndIndex));
		}
	
		assertEquals(prefix + "app error output and japted app error output do not match: ", actualErrorOutput, japtErrorOutput);
		assertEquals(prefix + "app output and japted app output do not match: ", actualOutput, japtOutput);
	}

}
