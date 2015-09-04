package com.ibm.esc.xml.parser.sax.errorhandler;

import org.xml.sax.*;
/**
 * Implements a Default SAX ErrorHandler
 */
public class DefaultErrorHandler implements ErrorHandler
{
/**
 * SampleErrorHandler constructor comment.
 */
public DefaultErrorHandler() {
	super();
}
/**
 * error method comment.
 */
public void error(SAXParseException exception) throws SAXException
{
	System.out.println(
		"(" + exception.getLineNumber() + "@" + exception.getColumnNumber() + com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.)_ERROR___3") + 
		exception.getMessage());
}
/**
 * fatalError method comment.
 */
public void fatalError(SAXParseException exception) throws SAXException
{
	System.out.println(
		"(" + exception.getLineNumber() + "@" + exception.getColumnNumber() + com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.)_FATAL_ERROR___6") + 
		exception.getMessage());
}
/**
 * warning method comment.
 */
public void warning(SAXParseException exception) throws SAXException
{
	System.out.println(
		"(" + exception.getLineNumber() + "@" + exception.getColumnNumber() + com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.)_WARNING___9") + 
		exception.getMessage());
}
}