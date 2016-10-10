/*
 * Created on Jun 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import org.xml.sax.AttributeList;
import org.xml.sax.SAXException;

import com.ibm.ive.tools.japt.commandLine.OptionsParser;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TestDocumentHandler extends DocumentHandler {

	private ArrayList testAppList = new ArrayList();
	private TestAppHandler currentApp;
	
	public TestDocumentHandler() {}

	static String[] appendArgs(String current[], String arguments) {
		Reader reader = new StringReader(arguments);
		String args[];
		try {
			args = OptionsParser.parseTokens(reader);
		} catch(IOException e) { args = null; }
		if(current != null) {
			String tmpArgs[] = new String[args.length + current.length];
			System.arraycopy(current, 0, tmpArgs, 0, current.length);
			System.arraycopy(args, 0, tmpArgs, current.length, args.length);
			return tmpArgs;
		}
		return args;
	}
	
	TestAppHandler[] getApps() {
		return (TestAppHandler[]) testAppList.toArray(new TestAppHandler[testAppList.size()]);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#endElement(java.lang.String)
	 */
	public void endElement(String name) throws SAXException {
		if (name.equals("testApp")) {
			if(currentApp != null) {
				if(currentApp.validate()) {
					testAppList.add(currentApp);
				}
				currentApp = null;
			}
		} else if(currentApp != null) {
			currentApp.endElement(name);
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#startElement(java.lang.String, org.xml.sax.AttributeList)
	 */
	public void startElement(String name, AttributeList attributes) throws SAXException {
		if (name.equals("testApp")) {
			currentApp = new TestAppHandler(
					attributes.getValue("log"),
					attributes.getValue("jar"),
					attributes.getValue("dir"),
					attributes.getValue("stdout"),
					attributes.getValue("stderr"));
			String fromEndIndex = attributes.getValue("outputIndexFromEnd");
			String startIndex = attributes.getValue("outputStartIndex");
			String endIndex = attributes.getValue("outputEndIndex");
			if(fromEndIndex != null) {
				currentApp.fromEndIndex = Integer.parseInt(fromEndIndex);
			}
			if(startIndex != null) {
				currentApp.startIndex = Integer.parseInt(startIndex);
			}
			if(endIndex != null) {
				currentApp.endIndex = Integer.parseInt(endIndex);
			}
		} else if(currentApp != null){
			currentApp.startElement(name, attributes);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#characters(char[], int, int)
	 */
	public void characters(char[] chars, int start, int length) throws SAXException {
		if(currentApp != null) {
			currentApp.characters(chars, start, length);
		} else {
			super.characters(chars, start, length);
		}
	}
}
