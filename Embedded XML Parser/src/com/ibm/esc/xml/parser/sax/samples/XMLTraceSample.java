package com.ibm.esc.xml.parser.sax.samples;

import java.net.*;
import java.io.*;

import org.xml.sax.*;
import com.ibm.esc.xml.parser.sax.*;
import com.ibm.esc.xml.parser.sax.errorhandler.*;
/**
 * This class implements a sample of the SAX XML parser
 * generating a trace of the parsing on the Console.
 */
public class XMLTraceSample extends Object
{
/**
 * XMLTraceSample constructor comment.
 */
public XMLTraceSample() {
	super();
}
/**
 * Starts the application.
 * @param args an array of command-line arguments
 */
public static void main(java.lang.String[] args)
{
//	URL url = null;
	InputStream is = null;
	// First, check the command-line usage.
	if (args.length == 0)
	{
	  System.err.println(com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Usage__java_<url>_1"));
	  System.exit(2);
	}
//	try
//	{
//		url = new URL(args[0]);
//	} catch (MalformedURLException e)
//	{
//		e.printStackTrace();
//	}
	// Open a stream which supports the Locator Interface APIs
//	try
//	{
//		is = url.openStream();
//	} catch (IOException e)
//	{
//		e.printStackTrace();
//	}
	is = System.in;
	
	Reader reader = new BufferedReader(new InputStreamReader(is));
	
	InputSource source = new InputSource(reader);
	// Create the XML Parser
	Parser parser = new MicroXMLParser();
	// Create the DocumentHandler
	//DocumentHandler docHandler = new XMLTraceDocumentHandler();
	DocumentHandler docHandler = new XMLEmptyDocumentHandler();
	parser.setDocumentHandler(docHandler);
	parser.setErrorHandler(new DefaultErrorHandler());
	long start = System.currentTimeMillis();
	try
	{
		parser.parse(source);
	} catch (SAXException e)
	{
		e.printStackTrace();
	} catch (IOException e)
	{
		e.printStackTrace();
	}
	long stop = System.currentTimeMillis();
	System.out.println(com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Time_to_run___{0}_ms", new Object[] {Long.toString(stop - start)}));
}
}