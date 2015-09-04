/*
 * Created on Feb 5, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.test;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;



public abstract class DocumentHandler implements org.xml.sax.DocumentHandler {
	CharacterHandler handler;
	
	interface CharacterHandler {
		void handle(String characters);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator arg0) {}

	
	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
		throws SAXException {}

	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String arg0, String arg1)
		throws SAXException {}

	
	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {}

	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {}

	/* (non-Javadoc)
	 * @see org.xml.sax.DocumentHandler#characters(char[], int, int)
	 */
	public void characters(char[] chars, int start, int length) throws SAXException {
		if(handler != null) {
			handler.handle(new String(chars, start, length).trim());
		}
	}
}
