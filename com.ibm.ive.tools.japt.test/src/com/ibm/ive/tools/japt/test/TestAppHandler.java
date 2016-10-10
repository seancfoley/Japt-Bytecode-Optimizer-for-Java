/*
 * Created on Jun 23, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.test;

import org.xml.sax.AttributeList;
import org.xml.sax.SAXException;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TestAppHandler extends DocumentHandler {

	int fromEndIndex = -1;
	int startIndex = -1;
	int endIndex = -1;
				
	String appClassPath; 
	String commandLine; 
	String japtApplicationArgs[]; 
	String japtLogPath;
	String japtedJar;
	String japtDir;
	String stdoutPath;
	String stderrPath;
	
	CharacterHandler commandLineHandler = new CharacterHandler() {
		public void handle(String characters) {
			String args[] = TestDocumentHandler.appendArgs(null, characters);
			StringBuffer buffer = new StringBuffer();
			for(int i=0; i<args.length; i++) {
				buffer.append('"');
				buffer.append(args[i]);
				buffer.append("\" ");
			}
			commandLine = buffer.toString();
		}
	};
	CharacterHandler japtArgsHandler = new CharacterHandler() {
		public void handle(String characters) {
			japtApplicationArgs = TestDocumentHandler.appendArgs(japtApplicationArgs, characters);
		}
	};
	CharacterHandler classPathHandler = new CharacterHandler() {
		public void handle(String characters) {
			if(appClassPath == null) {
				appClassPath = characters;
			}
			else {
				appClassPath += ';' + characters;
			}
		}
	};

	TestAppHandler(String japtLogPath, String japtedJar, String japtDir, String stdoutPath, String stderrPath) {
		if(japtLogPath == null) {
			japtLogPath = "testJapt.log";
		}
		if(japtedJar == null) {
			japtedJar = "testJapt.jar";
		}
		if(japtDir == null) {
			japtDir = ".";
		}
		this.stdoutPath = stdoutPath;
		this.stderrPath = stderrPath;
		this.japtLogPath = japtLogPath;
		this.japtedJar = japtedJar;
		this.japtDir = japtDir;
	}

	
	boolean validate() {
		return japtApplicationArgs != null && appClassPath != null && commandLine != null;
	}

	
	public void startElement(String name, AttributeList attributes) throws SAXException {
		if(name.equals("commandLine")) {
			handler = commandLineHandler;
		}
		//TODO change name to loadArgs
		else if(name.equals("japtApplicationArgs")) {
			handler = japtArgsHandler;
		}
		else if(name.equals("classPath")) {
			handler = classPathHandler;
		}
	}

	public void endElement(String name) throws SAXException {
		if(name.equals("commandLine") || name.equals("japtApplicationArgs") || name.equals("classPath")) {
			handler = null;
		}
	}
}
