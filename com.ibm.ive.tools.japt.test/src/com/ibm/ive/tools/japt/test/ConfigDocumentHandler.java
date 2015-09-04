/*
 * Created on Jun 22, 2004
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
public class ConfigDocumentHandler extends DocumentHandler {

	JRE testJREs[] = new JRE[] {new JRE(), new JRE()};
	private JRE current = testJREs[0];
	
	public static class JRE {
		String java;
		String classPath;
		String jre;
		String classVersion;
		
		public boolean equals(Object o) {
			if(o instanceof JRE) {
				JRE other = (JRE) o;
				return other.java.equals(java) && isSame(other.classPath, classPath) && isSame(other.jre, jre) 
					&& isSame(other.classVersion, classVersion);
			}
			return false;
		}
		
		public static boolean isSame(String one, String two) {
			if(one == null) {
				return two == null;
			}
			return one.equals(two);
		}
	}
	
	static class Transformation {
		String args[];
		boolean verifyTransformation;
		boolean optimizeTransformation;
		
		Transformation(String args[]) {
			this.args = args;
		}
	}
	
	
	CharacterHandler classPathHandler = new CharacterHandler() {
		public void handle(String characters) {
			if(current.classPath == null) {
				current.classPath = characters;
			}
			else {
				current.classPath += ';' + characters;
			}
		}
	};
	
	CharacterHandler jreHandler = new CharacterHandler() {
		public void handle(String characters) {
			current.jre = characters;
		}
	};
	
	CharacterHandler j9Handler = new CharacterHandler() {
		public void handle(String characters) {
			current.java = characters;
		}
	};
	
	CharacterHandler versionHandler = new CharacterHandler() {
		public void handle(String characters) {
			current.classVersion = characters;
		}
	};
	
	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#endElement(java.lang.String)
	 */
	public void endElement(String name) throws SAXException {
		if (name.equalsIgnoreCase("javaClassPath") 
				|| name.equalsIgnoreCase("jre") 
				|| name.equalsIgnoreCase("java") 
				|| name.equalsIgnoreCase("classVersion")) {
			handler = null;
		}
	}

	
	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#startElement(java.lang.String, org.xml.sax.AttributeList)
	 */
	public void startElement(String name, AttributeList attributes) throws SAXException {
		if(name.equalsIgnoreCase("secondRun")) {
			current = testJREs[1];
		} else if(name.equalsIgnoreCase("firstRun")) {
			current = testJREs[0];
		} else if(name.equalsIgnoreCase("alternative")) {
			current = new JRE();
		} else if (name.equalsIgnoreCase("javaClassPath")) {
			handler = classPathHandler;
		} else if (name.equalsIgnoreCase("jre")) {
			handler = jreHandler;
		} else if (name.equalsIgnoreCase("java")) {
			handler = j9Handler;
		} else if (name.equalsIgnoreCase("classVersion")) {
			handler = versionHandler;
		}
	}
	
	
}
