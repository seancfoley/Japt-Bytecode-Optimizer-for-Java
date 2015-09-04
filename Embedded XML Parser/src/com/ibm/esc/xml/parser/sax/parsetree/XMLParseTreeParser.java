package com.ibm.esc.xml.parser.sax.parsetree;

import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import com.ibm.esc.xml.parser.XMLException;
import com.ibm.esc.xml.parser.sax.*;
import com.ibm.esc.xml.parser.sax.dochandler.*;
import com.ibm.esc.xml.parser.sax.errorhandler.*;

/**
 * This parser generates a DOM parse tree.
 * It implements the SAX APIs
 */
 
public class XMLParseTreeParser extends MicroXMLParser
{
/**
 * MicroXMLParser constructor comment.
 */
public XMLParseTreeParser()
{
	super();
	XMLParseTreeDocumentHandler docHandler = new XMLParseTreeDocumentHandler();
	setDocumentHandler(docHandler);
	ErrorHandler errHandler = new DefaultErrorHandler();
	setErrorHandler(errHandler);
	docHandler.setErrorHandler(errHandler);
}
/**
 * Method comment
 * @return Document
 * @param reader Reader
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
	return ((XMLParseTreeDocumentHandler)getDocumentHandler()).getDocument();
}
}