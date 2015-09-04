package com.ibm.esc.xml.tools.xmlgeneration;

import java.io.*;

import org.w3c.dom.*;
import com.ibm.xml.parser.*;
import com.ibm.esc.xml.parser.*;
/**
 * Type comment
 */
public class XMLGenerator
{
	private CharEncoder charEncoder;
/**
 * XMLGenerator constructor comment.
 */
public XMLGenerator()
{
	super();
	charEncoder = CharEncoder.htmlCharacters();
}
/**
 */
protected String encode(String text)
{
	if (charEncoder == null) return text;
	StringWriter sw = new StringWriter();
	char c;
	String code;
	for (int i=0; i< text.length(); i++)
	{
		c = text.charAt(i);
		code = charEncoder.getCharKey(c);
		if (code == null)
		{
			sw.write(c);
		} else
		{
			sw.write('&');
			sw.write(code);
			sw.write(';');
		}
	}
	return sw.toString();
	
}
/**
 */
public void setCharEncoder(CharEncoder charEncoder)
{
	this.charEncoder = charEncoder;
}
/**
 * XMLGenerator constructor comment.
 */
public void toXMLString(Attr attribute, Writer writer)
	throws IOException
{
	writer.write(attribute.getName());
	if (!((TXAttribute)attribute).getIsEmptyValueAttibute())
	{
		writer.write("=\"");
		writer.write(encode(attribute.getValue()));
		writer.write("\"");
	}
}
/**
 * XMLGenerator constructor comment.
 */
public void toXMLString(Document document, Writer writer)
	throws IOException
{
	Element element = document.getDocumentElement();
	if (element != null)
	{
		toXMLString(element, writer);
	}	
}
/**
 * XMLGenerator constructor comment.
 */
public void toXMLString(Element element, Writer writer)
	throws IOException
{
	writer.write("<");
	writer.write(element.getTagName());
	if (element.getAttributes() != null)
	{
		toXMLString((NamedNodeMap)(element.getAttributes()), writer);
	}
	writer.write(">");
	if (element.getChildNodes().getLength() != 0)
	{
		toXMLString((NodeList)(element.getChildNodes()), writer);
	}
	writer.write("</" + element.getTagName() + ">");
}
/**
 * XMLGenerator constructor comment.
 */
public void toXMLString(NamedNodeMap attributeList, Writer writer)
	throws IOException
{
	for (int i = 0;  i < attributeList.getLength();  i ++)
	{
		writer.write(" ");
		toXMLString(((Attr)(attributeList.item(i))), writer);
	}
}
/**
 */
public String toXMLString(Node node)
{
	StringWriter sw = new StringWriter();
	try
	{
		toXMLString(node, sw);
	} catch (IOException e)
	{
		// nop
	}
	sw.flush();
	return sw.toString();
}
/**
 * XMLGenerator constructor comment.
 */
public void toXMLString(Node node, Writer writer)
	throws IOException
{
	if (node instanceof Element)
	{
		toXMLString((Element)node, writer);
	} else
	if (node instanceof Text)
	{
		toXMLString((Text)node, writer);
	} else
	if (node instanceof Document)
	{
		toXMLString((Document)node, writer);
	} else
	if (node instanceof NamedNodeMap)
	{
		toXMLString((NamedNodeMap)node, writer);
	} else
	if (node instanceof NodeList)
	{
		toXMLString((NodeList)node, writer);
	} else
	{
		// nop
	}
}
/**
 * XMLGenerator constructor comment.
 */
public void toXMLString(NodeList nodeList, Writer writer)
	throws IOException
{
	Node node;
	for (int i = 0;  i < nodeList.getLength();  i ++)
	{
		node = nodeList.item(i);
		if (node instanceof Element)
		{
			toXMLString((Element)node, writer);
		} else
		{
			toXMLString((Text)node, writer);
		}
	}
}
/**
 * XMLGenerator constructor comment.
 */
public void toXMLString(Text text, Writer writer)
	throws IOException
{
	writer.write(encode(text.getNodeValue()));
}
}