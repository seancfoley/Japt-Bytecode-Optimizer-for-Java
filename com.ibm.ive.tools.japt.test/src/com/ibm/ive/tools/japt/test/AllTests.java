package com.ibm.ive.tools.japt.test;
import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.esc.xml.parser.sax.MicroXMLParser;

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
				FileInputStream transformStream = new FileInputStream(transformFileName);
				TransformationDocumentHandler handler = new TransformationDocumentHandler(configHandler, testHandler);
				try {
					xmlParser = new MicroXMLParser();
					xmlParser.setDocumentHandler(handler);
					xmlParser.parse(new InputSource(transformStream));
					return handler.getTestRuns();
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
