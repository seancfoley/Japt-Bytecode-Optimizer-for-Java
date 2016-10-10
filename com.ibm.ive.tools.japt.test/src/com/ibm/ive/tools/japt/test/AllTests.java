package com.ibm.ive.tools.japt.test;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.esc.xml.parser.sax.MicroXMLParser;

import junit.framework.Test;
import junit.framework.TestSuite;

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
public class AllTests {

	//TODO more tests, maybe use eembc cldc stuff, need a test with annotation, enclosing method, stackmaps, other java5 and java6 stuff
	//maybe modena...

	public static TestRun[] readTests(String configFileName, String testFileName, String transformFileName) throws IOException, SAXException {
		FileInputStream configStream = new FileInputStream(configFileName);
		ConfigDocumentHandler configHandler = new ConfigDocumentHandler();
		try {
			MicroXMLParser xmlParser = new MicroXMLParser();
			xmlParser.setDocumentHandler(configHandler);
			xmlParser.parse(new InputSource(configStream));
			FileInputStream testStream = new FileInputStream(testFileName);
			TestDocumentHandler testHandler = new TestDocumentHandler();
			try {
				xmlParser = new MicroXMLParser();
				xmlParser.setDocumentHandler(testHandler);
				xmlParser.parse(new InputSource(testStream));
				
				TestAppHandler testApps[] = testHandler.getApps();
				for(TestAppHandler testApp : testApps) {
					preparePaths(
							Paths.get(testApp.stdoutPath).getParent(), 
							Paths.get(testApp.stderrPath).getParent(), 
							Paths.get(testApp.japtLogPath).getParent(), 
							Paths.get(testApp.japtDir), 
							Paths.get(testApp.japtedJar).getParent());
				}
				
				FileInputStream transformStream = new FileInputStream(transformFileName);
				TransformationDocumentHandler handler = new TransformationDocumentHandler(configHandler, testApps);
				try {
					xmlParser = new MicroXMLParser();
					xmlParser.setDocumentHandler(handler);
					xmlParser.parse(new InputSource(transformStream));
					
					TestRun runs[] = handler.getTestRuns();
					return runs;
				} finally {
					transformStream.close();
				}
			} finally {
				testStream.close();
			}
		} finally {
			configStream.close();
		}
	}
	
	
	static void preparePaths(Path ... paths) {
		for(Path path : paths) {
			File dir = path.toFile();
			if(!dir.exists()) {
				if(!dir.mkdirs()) {
					System.err.println("Could not create " + dir);
				}
			}
		}
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Test for default package");
		try {
			//JaptTester.quitOnFirstFailure = true;
			TestRun[] runs = readTests("config.xml", "tests.xml", "transforms.xml");
			//TestRun[] runs = readTests("config.xml", "tests.xml", "transformstmp.xml");
			//TestRun[] runs = readTests("config.xml", "teststmp.xml", "transformstmp.xml");
			//TestRun[] runs = readTests("configtmp.xml", "teststmp.xml", "transformstmp.xml");
			for(int i=0; i<runs.length; i++) {
				TestRun run = runs[i];
				JaptTester tester = new JaptTester(run);
				suite.addTest(tester);
			}
		} catch(SAXException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return suite;
	}
	
	
}
