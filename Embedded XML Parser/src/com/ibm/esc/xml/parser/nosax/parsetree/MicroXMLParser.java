package com.ibm.esc.xml.parser.nosax.parsetree;

import java.io.*;

import org.w3c.dom.*;
import com.ibm.xml.parser.TXDocument;

import com.ibm.esc.xml.parser.*;

/**
 * This parser generates a DOM parse tree.
 * It DOESN'T implement the SAX APIs
 */

 public class MicroXMLParser extends AbstractMicroXMLParser
{
	private Document document;
	private Node currentNode;
/**
 * MicroXMLParser constructor comment.
 */
public MicroXMLParser()
{
	super();
}
/**
 * characters method comment.
 */
protected void characters(char[] ch, int start, int length)
	throws XMLException
{
//	Text text = document.createTextNode(String.copyValueOf(ch));
	Text text = document.createTextNode(new String(ch));
	currentNode.appendChild(text);
}
/**
 * endDocument method comment.
 */
protected void endDocument() throws XMLException
{
	currentNode = null;
}
/**
 * endElement method comment.
 */
protected void endElement(String tagName)
	throws XMLException
{
	if (currentNode.getNodeName().equals(tagName))
	{
		currentNode = currentNode.getParentNode();
	} else
	{
		Node node = retreiveSuperNode(tagName, currentNode.getParentNode());
		if (node == null)
		{
			warning(WARNING_END_TAG_EXPECTED, "</" + currentNode.getNodeName() +">");
		} else
		{
			currentNode = node.getParentNode();
		}
	}
}
/**
 * fatalError method comment.
 */
protected void fatalError(int errorID, String parameter)
	throws XMLException
{
	String errorMsg = errorMsg(errorID, parameter);
	warning(errorID, parameter);
	throw new XMLException(errorMsg);
}
/**
 * newAttributeList method comment.
 */
protected XMLAttributeList newAttributeList()
{
	return new XMLAttributeList();
}
/**
 * Method comment
 * @return org.w3c.dom.Document
 * @param reader java.io.Reader
 */
public Document parse(Reader reader)
{
	setStream(reader);
	try
	{
		doParse();
	} catch (XMLException e)
	{
		// nop
	} catch (IOException e)
	{
		// nop
	}
	return document;
}
/**
 */
protected void processingInstruction(String target, String data)
	throws XMLException
{
	// nop - Not supported yet
}
/**
 */
protected Node retreiveSuperNode(String tagName, Node node)
{
	if (node == null)
	{
		return null;
	} else
	if (node.getNodeName().equals(tagName))
	{
		return node;
	} else
	{
		return retreiveSuperNode(tagName, node.getParentNode());
	}
}
/**
 * startDocument method comment.
 */
protected void startDocument() throws XMLException
{
	document = new TXDocument();
	currentNode = document;
}
/**
 * startElement method comment.
 */
protected void startElement(String tagName, XMLAttributeList attributeList)
	throws XMLException
{
	Element element = document.createElement(tagName);
	currentNode.appendChild(element);
	currentNode = element;
	for (int i = 0; i < attributeList.getLength(); i++)
	{
		element.setAttribute(attributeList.getName(i), attributeList.getValue(i));
	}
}
/**
 * warning method comment.
 */
protected void warning(int errorID, String parameter)
	throws XMLException
{
	System.out.println(
		"<" + getLineNumber() + "," + getColumnNumber() + "> "
		+ errorMsg(errorID, parameter));
}
}