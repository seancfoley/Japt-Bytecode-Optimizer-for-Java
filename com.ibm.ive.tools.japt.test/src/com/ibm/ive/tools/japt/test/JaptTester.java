package com.ibm.ive.tools.japt.test;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

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
	private void compareRuns(AppInvocation appInvocation, String japtedRun, String prefix) throws InterruptedException, IOException {
		ProcessThread japtThread = new ProcessThread(japtedRun);
		japtThread.start();
		
		boolean invoked = appInvocation.runApp();
		appInvocation.waitFor();
		//wait for 15 minutes at most
		japtThread.join(15 * 60 * 1000);
		
		//the output should be the same
		String actualOutput = appInvocation.getOutput();
		String actualErrorOutput = appInvocation.getErrorOutput();
		AppRun appRun = appInvocation.app;
		if(invoked) {
			saveAppOutput(actualOutput, appRun.stdOutPath);
			saveAppOutput(actualErrorOutput, appRun.stdErrPath);
		}
		
		String japtOutput = japtThread.getOutput();
		String japtErrorOutput = japtThread.getErrorOutput();
		
		int endIndex = appRun.endIndex;
		if(endIndex >= 0) {
			actualOutput = actualOutput.substring(0, Math.min(endIndex, actualOutput.length()));
			japtOutput = japtOutput.substring(0, Math.min(endIndex, japtOutput.length()));
		}
		int startIndex = appRun.startIndex;
		if(startIndex > 0) {
			actualOutput = actualOutput.substring(Math.min(startIndex, actualOutput.length()));
			japtOutput = japtOutput.substring(Math.min(startIndex, actualOutput.length()));
		}
		int fromEndIndex = appRun.fromEndIndex;
		if(fromEndIndex >= 0) {
			actualOutput = actualOutput.substring(Math.max(0, actualOutput.length() - fromEndIndex));
			japtOutput = japtOutput.substring(Math.max(0, japtOutput.length() - fromEndIndex));
		}
	
		assertEquals(prefix + "app error output and japted app error output do not match: ", actualErrorOutput, japtErrorOutput);
		assertEquals(prefix + "app output and japted app output do not match: ", actualOutput, japtOutput);
	}

	private void saveAppOutput(String output, String path)
			throws FileNotFoundException {
		if(path != null && path.length() > 0) {
			PrintStream fileStream = new PrintStream(
					new BufferedOutputStream(
							new FileOutputStream(path)));
			try {
				fileStream.print(output);
			} finally {
				fileStream.close();
			}
		}
	}

}
