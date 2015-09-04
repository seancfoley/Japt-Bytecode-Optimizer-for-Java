package com.ibm.esc.xml.parser.sax;

// import java.util.Locale;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;

import org.xml.sax.AttributeList;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.esc.xml.parser.AbstractMicroXMLParser;
import com.ibm.esc.xml.parser.XMLAttributeList;
import com.ibm.esc.xml.parser.XMLException;
import com.ibm.xml.parser.TXDOMException;
/**
 * This parser implements the SAX APIs
 */
public class MicroXMLParser extends AbstractMicroXMLParser implements Parser, Locator
{
	private DocumentHandler documentHandler;
	private ErrorHandler errorHandler;
	//private InputSource source;
	private String systemId;
	private String publicId;

public void setLocale(Locale l) {}

/**
 * Method comment
 */
public MicroXMLParser()
{
	super();
	// Set a Defauult DocumentHandler to avoid
	// any kind of test like:
	//		if (documentHandler != null) ...
	setDocumentHandler(new HandlerBase());
}
/**
 */
public void characters (char ch[], int start, int length)
	throws XMLException
{
	try
	{
		documentHandler.characters(ch, start, length);
	} catch (SAXException e)
	{
		throw new XMLException(e);
	}
}
/**
 */
protected void endDocument()
	throws XMLException
{
	try
	{
		documentHandler.endDocument();
	} catch (SAXException e)
	{
		throw new XMLException(e);
	}
}
/**
 */
protected void endElement(String tagName)
	throws XMLException
{
	try
	{
		documentHandler.endElement(tagName);
	} catch (SAXException e)
	{
		throw new XMLException(e);
	}
}
/**
 */
protected void fatalError(int errorID, String parameter)
	throws XMLException
{
	SAXParseException e =
		new SAXParseException(
			errorMsg(errorID, parameter), this);
	if (errorHandler != null)
	{
		try
		{
			errorHandler.fatalError(e);
		} catch (SAXException exception)
		{
			throw new XMLException(exception);
		}
	}
	// If the errorhandler didn't throw the exception
	// it is time to do it
	throw new XMLException(e);
}
  /**
	* Allow an application to register a document event handler.
	*
	* <p>If the application does not register a document handler, all
	* document events reported by the SAX parser will be silently
	* ignored (this is the default behaviour implemented by
	* HandlerBase).</p>
	*
	* <p>Applications may register a new or different handler in the
	* middle of a parse, and the SAX parser must begin using the new
	* handler immediately.</p>
	*
	* @param handler The document handler.
	* @see DocumentHandler
	* @see HandlerBase
	*/
public DocumentHandler getDocumentHandler()
{
	return documentHandler;
}
public String getPublicId()
{
	return publicId;
}
public String getSystemId()
{
	return systemId;
}
/**
 */
protected XMLAttributeList newAttributeList()
{
	return new AttributeListImpl();
}
  /**
	* Parse an XML document from a system identifier (URI).
	* @see org.xml.sax.Parser#parse(java.lang.String)
	*/
public void parse (String systemId)
	throws SAXException
{
	// This parser must be independent from java.net
	throw new SAXException(com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.MicroXMLParser_does_not_support_any_system_identofier_or_URL._1"));
}
  /**
	* Parse an XML document.
	*
	* <p>The application can use this method to instruct the SAX parser
	* to begin parsing an XML document from any valid input
	* source (a character stream, a byte stream, or a URI).</p>
	*
	* <p>Applications may not invoke this method while a parse is in
	* progress (they should create a new Parser instead for each
	* additional XML document).  Once a parse is complete, an
	* application may reuse the same Parser object, possibly with a
	* different input source.</p>
	*
	* @param source The input source for the top-level of the
	*        XML document.
	* @exception org.xml.sax.SAXException Any SAX exception, possibly
	*            wrapping another exception.
	* @exception java.io.IOException An IO exception from the parser,
	*            possibly from a byte stream or character stream
	*            supplied by the application.
	* @see org.xml.sax.InputSource
	* @see #parse(java.lang.String)
	* @see #setEntityResolver
	* @see #setDTDHandler
	* @see #setDocumentHandler
	* @see #setErrorHandler
	*/
public void parse (InputSource source)
	throws SAXException, IOException
{
	setSource(source);
	Reader reader = null;
	if (source.getCharacterStream() != null)
	{
		reader = source.getCharacterStream();
	} else
	if (source.getByteStream() != null)
	{
		reader = new InputStreamReader(source.getByteStream());
	}
	if (reader == null) return;

	setStream(reader);
	try
	{
		doParse();
	} catch (XMLException e)
	{
		Exception we = e.getWrappedException();
		throw (SAXException)we;
	}
}
/**
 */
protected void processingInstruction(String target, String data)
	throws XMLException
{
	try
	{
		documentHandler.processingInstruction(target, data);
	} catch (SAXException e)
	{
		throw new XMLException(e);
	}
}
  /**
	* Allow an application to register a document event handler.
	*
	* <p>If the application does not register a document handler, all
	* document events reported by the SAX parser will be silently
	* ignored (this is the default behaviour implemented by
	* HandlerBase).</p>
	*
	* <p>Applications may register a new or different handler in the
	* middle of a parse, and the SAX parser must begin using the new
	* handler immediately.</p>
	*
	* @param handler The document handler.
	* @see DocumentHandler
	* @see HandlerBase
	*/
public void setDocumentHandler(DocumentHandler documentHandler)
{
	this.documentHandler = documentHandler;
	documentHandler.setDocumentLocator(this);
}
  /**
	* Allow an application to register a DTD event handler.
	*
	* <p>If the application does not register a DTD handler, all DTD
	* events reported by the SAX parser will be silently
	* ignored (this is the default behaviour implemented by
	* HandlerBase).</p>
	*
	* <p>Applications may register a new or different
	* handler in the middle of a parse, and the SAX parser must
	* begin using the new handler immediately.</p>
	*
	* @param handler The DTD handler.
	* @see DTDHandler
	* @see HandlerBase
	*/
/*
 * This current release doesn't manage any DTD handler
 */
public void setDTDHandler (DTDHandler handler)
{
	// nop
}
  /**
	* Allow an application to register a custom entity resolver.
	*
	* <p>If the application does not register an entity resolver, the
	* SAX parser will resolve system identifiers and open connections
	* to entities itself (this is the default behaviour implemented in
	* HandlerBase).</p>
	*
	* <p>Applications may register a new or different entity resolver
	* in the middle of a parse, and the SAX parser must begin using
	* the new resolver immediately.</p>
	*
	* @param resolver The object for resolving entities.
	* @see EntityResolver
	* @see HandlerBase
	*/
/*
 * This current release doesn't support any Entity resolver
 */
public void setEntityResolver (EntityResolver resolver)
{
	// nop
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
	*/
protected void setPublicId (String publicId)
{
	this.publicId = publicId;
}
  /**
	*/
protected void setSource (InputSource source)
{
	//this.source = source;
	setPublicId(source.getPublicId());
	setSystemId(source.getSystemId());
}
  /**
	*/
protected void setSystemId (String systemId)
{
	this.systemId = systemId;
}
/**
 */
protected void startDocument()
	throws XMLException
{
	try
	{
		documentHandler.startDocument();
	} catch (SAXException e)
	{
		throw new XMLException(e);
	}
}
/**
 */
protected void startElement(String tagName, XMLAttributeList attributeList)
	throws XMLException
{
	try
	{
		documentHandler.startElement(tagName, (AttributeList)attributeList);
	} catch (SAXException e)
	{
		throw new XMLException(e);
	} catch (TXDOMException e)
	{
		throw new XMLException(e);
	}
}
/**
 */
protected void warning(int errorID, String parameter)
	throws XMLException
{
	try
	{
		SAXParseException e =
			new SAXParseException(
				errorMsg(errorID, parameter), this);
		if (errorHandler != null)
		{
			errorHandler.warning(e);
		}
	} catch (SAXException exception)
	{
		throw new XMLException(exception);
	}
}
}