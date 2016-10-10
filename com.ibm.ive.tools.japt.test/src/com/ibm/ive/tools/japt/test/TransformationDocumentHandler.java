/*
 * Created on Jun 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.test;

import java.util.ArrayList;

import org.xml.sax.AttributeList;
import org.xml.sax.SAXException;

import com.ibm.ive.tools.japt.test.ConfigDocumentHandler.JRE;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TransformationDocumentHandler extends DocumentHandler {
	private ArrayList transformationList = new ArrayList();
	private final TestAppHandler testApps[];
	private final JRE jres[];
	TransformationHandler transformationHandler = new TransformationHandler();

	public TransformationDocumentHandler(ConfigDocumentHandler config, TestAppHandler testApps[]) {
		this.jres = config.testJREs;
		if(jres[0].equals(jres[1])) {
			jres[1] = jres[0];
		}
		this.testApps = testApps;
	}

	static class Transformation {
		final String args[];
		final boolean verifyTransformation;
		final boolean optimizeTransformation;
		
		Transformation(String args[], boolean verify, boolean optimize) {
			this.args = args;
			verifyTransformation = verify;
			optimizeTransformation = optimize;
		}
		
		Transformation(String args[]) {
			this(args, false, false);
		}
	}
	
	class TransformationHandler implements CharacterHandler {
		boolean verifyTransformation;
		boolean optimizeTransformation;
		String currentArgs[];
		
		public void handle(String characters) {
			currentArgs = TestDocumentHandler.appendArgs(currentArgs, characters);
		}
	}
	
	public TestRun[] getTestRuns() {
		ArrayList result = new ArrayList(transformationList.size() * testApps.length);
		
		AppRun appRuns[] = new AppRun[testApps.length];
		for(int j=0; j<testApps.length; j++) {
			TestAppHandler testApp = testApps[j];
			appRuns[j] = new AppRun(testApp.appClassPath, testApp.commandLine, testApp.stdoutPath, testApp.stderrPath,
					testApp.fromEndIndex, testApp.startIndex, testApp.endIndex);
		}
		
		for(int i=0; i<transformationList.size(); i++) {
			Transformation transformation = (Transformation) transformationList.get(i);
			String transform[] = transformation.args;
			for(int j=0; j<testApps.length; j++) {
				TestAppHandler testApp = testApps[j];
				TestRun testRun = new TestRun(appRuns[j], jres, testApp.japtApplicationArgs,
						transform, testApp.japtLogPath, testApp.japtedJar, testApp.japtDir);
				testRun.verifyTransformation = transformation.verifyTransformation;
				testRun.optimizeTransformation = transformation.optimizeTransformation;
				result.add(testRun);
			}
		}
		return (TestRun[]) result.toArray(new TestRun[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#endElement(java.lang.String)
	 */
	public void endElement(String name) throws SAXException {
		if (name.equals("transformation")) {
			if(this.handler instanceof TransformationHandler) {
				TransformationHandler handler = (TransformationHandler) this.handler;
				Transformation transformation = 
					new Transformation(handler.currentArgs, handler.verifyTransformation, handler.optimizeTransformation);
				transformationList.add(transformation);
			}
			this.handler = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#startElement(java.lang.String, org.xml.sax.AttributeList)
	 */
	public void startElement(String name, AttributeList attributes) throws SAXException {
		if (name.equals("transformation")) {
			String doVerify = attributes.getValue("verify");
			boolean verify = (doVerify == null) ? false : (Boolean.valueOf(doVerify).booleanValue());
			String doOptimize = attributes.getValue("optimize");
			boolean optimize = (doOptimize == null) ? false : (Boolean.valueOf(doOptimize).booleanValue());
			transformationHandler.currentArgs = null;
			transformationHandler.verifyTransformation = verify;
			transformationHandler.optimizeTransformation = optimize;
			handler = transformationHandler;
		}
	}
	
}
