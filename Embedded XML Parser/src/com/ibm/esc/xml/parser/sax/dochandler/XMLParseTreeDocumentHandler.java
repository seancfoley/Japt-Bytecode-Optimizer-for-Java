package com.ibm.esc.xml.parser.sax.dochandler;

import org.xml.sax.*;
import org.w3c.dom.*;

import com.ibm.xml.parser.TXDocument;

/**
 * This Document Handler builds a DOM parse tree
 * of the pased XML file.
 */
public class XMLParseTreeDocumentHandler implements DocumentHandler
{
	private Node currentNode;
	private Document document;
	private ErrorHandler errorHandler;
	
	private Locator documentLocator;
/**
 * XMLParseTreeDocumentHandler constructor comment.
 */
public XMLParseTreeDocumentHandler()
{
	super();
}
  /**
	* Receive notification of character data.
	*
	* <p>The Parser will call this method to report each chunk of
	* character data.  SAX parsers may return all contiguous character
	* data in a single chunk, or they may split it into several
	* chunks; however, all of the characters in any single event
	* must come from the same external entity, so that the Locator
	* provides useful information.</p>
	*
	* <p>The application must not attempt to read from the array
	* outside of the specified range.</p>
	*
	* <p>Note that some parsers will report whitespace using the
	* ignorableWhitespace() method rather than this one (validating
	* parsers must do so).</p>
	*
	* @param ch The characters from the XML document.
	* @param start The start position in the array.
	* @param length The number of characters to read from the array.
	* @exception org.xml.sax.SAXException Any SAX exception, possibly
	*            wrapping another exception.
	* @see #ignorableWhitespace 
	* @see org.xml.sax.Locator
	*/
public void characters (char[] chars, int start, int length)
	throws SAXException
{
	Text text = document.createTextNode(new String(chars, start, length));
	currentNode.appendChild(text);
}
	
  /**
	* Receive notification of the end of a document.
	*
	* <p>The SAX parser will invoke this method only once, and it will
	* be the last method invoked during the parse.  The parser shall
	* not invoke this method until it has either abandoned parsing
	* (because of an unrecoverable error) or reached the end of
	* input.</p>
	*
	* @exception org.xml.sax.SAXException Any SAX exception, possibly
	*            wrapping another exception.
	*/
public void endDocument() throws SAXException
{
	currentNode = null;
}
  /**
	* Receive notification of the end of an element.
	*
	* <p>The SAX parser will invoke this method at the end of every
	* element in the XML document; there will be a corresponding
	* startElement() event for every endElement() event (even when the
	* element is empty).</p>
	*
	* <p>If the element name has a namespace prefix, the prefix will
	* still be attached to the name.</p>
	*
	* @param name The element type name
	* @exception org.xml.sax.SAXException Any SAX exception, possibly
	*            wrapping another exception.
	*/
public void endElement (String name)
	throws SAXException
{
	if (currentNode.getNodeName().equals(name))
	{
		currentNode = currentNode.getParentNode();
	} else
	{
		Node node = retreiveSuperNode(name, currentNode.getParentNode());
		if (node == null)
		{
			if (errorHandler != null)
			{
				errorHandler.warning(new SAXParseException(com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.</{0}>_expected_2", currentNode.getNodeName()), documentLocator));
			}
			// currentNode = currentNode;
		} else
		{
			currentNode = node.getParentNode();
		}
	}
}

public Document getDocument ()
{
	return document;
}
/**
 * ignorableWhitespace method comment.
 */
public void ignorableWhitespace(char[] ch, int start, int length) throws org.xml.sax.SAXException {
}
/**
 * processingInstruction method comment.
 */
public void processingInstruction(String target, String data)
	throws SAXException
{
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
 * setDocumentLocator method comment.
 */
public void setDocumentLocator(Locator documentLocator)
{
	this.documentLocator = documentLocator;
}
  /**
	* Allow an application to register an error event handler.
	*
	* <p>If the application does not register an error event handler,
	* all error events reported by the SAX parser will be silently
	* ignored, except for fatalError, which will throw a SAXException
	* (this is the default behaviour implemented by HandlerBase).</p>
	*
	* <p>Applications may register a new or different handler in the
	* middle of a parse, and the SAX parser must begin using the new
	* handler immediately.</p>
	*
	* @param handler The error handler.
	* @see ErrorHandler
	* @see SAXException
	* @see HandlerBase
	*/
public void setErrorHandler (ErrorHandler errorHandler)
{
	this.errorHandler = errorHandler;
}
  /**
	* Receive notification of the beginning of a document.
	*
	* <p>The SAX parser will invoke this method only once, before any
	* other methods in this interface or in DTDHandler (except for
	* setDocumentLocator).</p>
	*
	* @exception org.xml.sax.SAXException Any SAX exception, possibly
	*            wrapping another exception.
	*/
public void startDocument() throws SAXException
{
	document = new TXDocument();
	currentNode = document;
}
/**
 * startElement method comment.
 */
public void startElement(String name, AttributeList atts)
	throws SAXException
{
	Element element = document.createElement(name);
	currentNode.appendChild(element);
	currentNode = element;
	int length = atts.getLength();
	for (int i = 0; i < length; i++)
	{
		element.setAttribute(atts.getName(i), atts.getValue(i));
	}
}
}