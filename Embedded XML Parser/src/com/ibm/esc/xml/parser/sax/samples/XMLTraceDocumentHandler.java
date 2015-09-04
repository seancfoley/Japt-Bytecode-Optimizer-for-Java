package com.ibm.esc.xml.parser.sax.samples;

import org.xml.sax.*;
/**
 * Type comment
 */
public class XMLTraceDocumentHandler implements DocumentHandler
{
	private Locator documentLocator;
/**
 * XMLTraceDocumentHandler constructor comment.
 */
public XMLTraceDocumentHandler() {
	super();
}
/**
 * characters method comment.
 */
public void characters(char[] ch, int start, int length) throws SAXException
{
	System.out.println(
		"<" + documentLocator.getLineNumber() + "@" + documentLocator.getColumnNumber() + ">" +
//		" Text:" + String.copyValueOf(ch, start, length));
		com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser._Text__4") + new String(ch, start, length));

}
/**
 * endDocument method comment.
 */
public void endDocument() throws SAXException
{
	System.out.println(com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.>>End_of_Document_5"));
}
/**
 * endElement method comment.
 */
public void endElement(String name) throws SAXException
{
	System.out.println(
		"<" + documentLocator.getLineNumber() + "@" + documentLocator.getColumnNumber() + ">" +
		com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser._End_of_Element__9") + name);
}
/**
 * ignorableWhitespace method comment.
 */
public void ignorableWhitespace(char[] ch, int start, int length) throws org.xml.sax.SAXException {
}
/**
 * processingInstruction method comment.
 */
public void processingInstruction(String target, String data) throws SAXException 
{
	// Nop
}
/**
 * setDocumentLocator method comment.
 */
public void setDocumentLocator(Locator locator)
{
	this.documentLocator = locator;
}
/**
 * startDocument method comment.
 */
public void startDocument() throws SAXException
{
	System.out.println(com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.>>Start_of_Document_10"));
}
/**
 * startElement method comment.
 */
public void startElement(String name, AttributeList atts) throws SAXException
{
	System.out.println(
		"<" + documentLocator.getLineNumber() + "@" + documentLocator.getColumnNumber() + ">" +
		com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser._Start_of_Element__14") + name);
	for (int i=0; i < atts.getLength(); i++)
	{
		System.out.println(com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser._tname__15") + atts.getName(i) +
			com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser._ttype___16") + atts.getType(i) +
			com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser._tvalue___17") + atts.getValue(i));
	}
}
}