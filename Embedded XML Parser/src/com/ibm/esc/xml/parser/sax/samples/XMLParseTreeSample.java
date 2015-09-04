package com.ibm.esc.xml.parser.sax.samples;

import java.net.*;
import java.io.*;

import org.w3c.dom.*;

import com.ibm.esc.xml.parser.sax.parsetree.*;

/**
 * This class implements a sample of the SAX XML parser
 * generating a DOM parse tree.
 */
public class XMLParseTreeSample extends Object
{
/**
 * XMLTraceSample constructor comment.
 */
public XMLParseTreeSample() {
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
	if (args.length != 1)
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
		is = System.in;
//	} catch (IOException e)
//	{
//		e.printStackTrace();
//	}
	Reader reader = new BufferedReader(new InputStreamReader(is));
	org.w3c.dom.Document doc = new XMLParseTreeVerboseParser().parse(reader);
//	com.ibm.uvm.tools.DebugSupport.inspect(doc);
//	com.ibm.uvm.tools.DebugSupport.halt();
}
}