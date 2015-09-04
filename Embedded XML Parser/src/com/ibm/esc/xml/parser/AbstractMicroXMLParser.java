package com.ibm.esc.xml.parser;


import java.util.Hashtable;
import java.io.IOException;
import java.io.EOFException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.StringWriter;

import com.ibm.esc.xml.core.*;

/**
 * This class implements a non-validating XML parser
 *
 * The assumptions of this parser are:
 *		- the XML file is lexicaly and syntaxy correct
 *		- the authorized tokens are:
 *			- <tag_name {tag-attributes}{/}>
 *			- </tag_name>
 *			- <!processing-instruction>
 */
public abstract class AbstractMicroXMLParser
{
	private Reader stream;
	private MicroXMLScanner scanner;
	private boolean startedDocument = false;
	private boolean keySensitive = true;
	private XMLAttributeList emptyAttributeList;
	private CharDecoder charDecoder;
	
	// Parse Error IDs
	public static final int ERROR_ATTRIBUT_VALUE_EXPECTED = 0;
	public static final int ERROR_ATTRIBUT_NAME_EXPECTED = 1;
	public static final int ERROR_TAG_EXPECTED = 2;
	public static final int ERROR_TAG_NAME_EXPECTED = 3;
	public static final int ERROR_WRONG_TAG_HEADER = 4;
	public static final int ERROR_GT_EXPECTED = 5;
	public static final int ERROR_TAG_COMMENT_EXPECTED = 6;
	public static final int ERROR_WRONG_PI = 7;
	public static final int ERROR_EQUAL_EXPECTED = 8;
	public static final int ERROR_END_OF_TAG_EXPECTED = 9;
	public static final int ERROR_END_QUOTE_EXPECTED = 10;
	public static final int ERROR_ILLEGAL_CHARACTER = 11;
	
	
	public static final int WARNING_UNKNOWN_CHARACTER_DEF = 50;
	public static final int WARNING_END_TAG_EXPECTED = 51;
	public static final int ERROR_UNMANAGED_STATE = 99;
	
/**
 * Method comment
 */
protected AbstractMicroXMLParser()
{
	super();
	// Set a Default DocumentHandler to avoid
	// any kind of test like:
	//		if (documentHandler != null) ...
	setCharDecoder(CharDecoder.htmlCharacters());
	emptyAttributeList = newAttributeList();
}
protected abstract void characters (char ch[], int start, int length)
	throws XMLException;
/**
 * Retrieve all the characters &xxxx; and replace them
 * by their value.
 */
protected String decode(String toDecode)
	throws XMLException
{
	int startIndex = toDecode.indexOf('&', 0);
	if (startIndex == -1) return toDecode;

	int stopIndex = -1;
	String charName, charValue;
	StringWriter result = new StringWriter();
	
	while (startIndex != -1)
	{
		result.write(toDecode.substring(stopIndex + 1,startIndex));
		stopIndex = toDecode.indexOf(';', startIndex + 1);
		if (stopIndex == -1)
		{
			result.write('&');
			stopIndex = startIndex;
		} else
		{
			charName = toDecode.substring(startIndex + 1, stopIndex);
			charValue = charDecoder.getCharValue(charName);
			if (charValue == null)
			{
				warning(WARNING_UNKNOWN_CHARACTER_DEF, "&" + charName + ";");
				result.write('&');
				result.write(charName);
				result.write(';');
			} else
			{
				result.write(charValue);
			}
		}
		startIndex = toDecode.indexOf('&', stopIndex + 1);
	}
	result.write(toDecode.substring(stopIndex + 1));
	return result.toString();
}

protected void doParse ()
	throws XMLException, IOException
{
	startDocument();
	parseProlog();
	parseDocument();
	endDocument();
}
/**
 */
protected abstract void endDocument() throws XMLException;
/**
 */
protected abstract void endElement(String tagName) throws XMLException;
/**
 * Return a String describing the parsing error.
 * This method can be overwrite by the subclasses
 * to provide a better decription.
 * By default it is the error number.
 */
protected String errorMsg(int errorID, String parameter)
{
	String errorMsg = Messages.getString("EmbeddedXMLParser.ERR_3") + errorID;
	if (parameter != null)
	{
		errorMsg = errorMsg + ": " + parameter;
	}
	return errorMsg;
}
/**
 */
protected abstract void fatalError(int errorID, String parameter) throws XMLException;
public int getColumnNumber()
{
	return scanner.getColumnCounter();
}
/**
 */
protected XMLAttributeList getEmptyAttributeList()
{
	return emptyAttributeList;
}
public int getLineNumber()
{
	return scanner.getLineCounter();
}
/**
 */
protected abstract XMLAttributeList newAttributeList();
/**
 */
protected XMLAttributeList parseAttributeListStartingWith(String attributeName)
	throws XMLException, IOException
{
	String attributeValue;
	
	XMLAttributeList attributList= newAttributeList();
	String attName = attributeName;
	while (true)
	{
		attributeValue = null;
		scanner.skipNextSpaces();
		if (scanner.skippedChar('='))
		{
			scanner.skipNextSpaces();
			if (scanner.skippedChar('"'))
			{
				try
				{
					attributeValue = decode(scanner.scanUpTo('"'));
					scanner.skipNextChar();		// Skip '"'
				} catch (EOFException e)
				{
					fatalError(ERROR_END_QUOTE_EXPECTED, null);
				}
			} else
			if (scanner.skippedChar('\''))
			{
				try
				{
					attributeValue = decode(scanner.scanUpTo('\''));
					scanner.skipNextChar();		// Skip '\''
				} catch (EOFException e)
				{
					fatalError(ERROR_END_QUOTE_EXPECTED, null);
				}
			} else
			{
				attributeValue = scanner.scanAttributeValue();
				// In this case, the attribute value cannot be empty!!!
				if (attributeValue.length() == 0)
				{
					fatalError(ERROR_ATTRIBUT_VALUE_EXPECTED, null);
				}
				attributeValue = decode(attributeValue);
			}
		}
		
		attributList.addAttribute(
			// Return always a canonical representation for the attribute name
			(keySensitive?attName:attName.toUpperCase()).intern(),
			XMLAttributeList.CDATA,
			attributeValue);
		scanner.skipNextSpaces();
		attName = scanner.scanName();
		if (attName == null)
		{
			return attributList;
		}
	}
}
/**
 */
protected void parseDocTypeDeclaration()
	throws XMLException, IOException
{
	if (scanner.skippedChar('-'))	// <!-
	{
		if (scanner.skippedChar('-'))
		{
			try
			{
				while (true)
				{
					scanner.skipDataUpTo('-');
					if (scanner.skippedChar('-'))
					{
						if (scanner.skippedChar('>'))
						{
							return;
						} else
						{
							fatalError(ERROR_TAG_COMMENT_EXPECTED, null);
						}
					}
				}
			} catch (EOFException e)
			{
				fatalError(ERROR_TAG_COMMENT_EXPECTED, null);
			}
		} else
		{
			fatalError(ERROR_TAG_COMMENT_EXPECTED, null);
		}
	} else
	{
		String docTypeID = scanner.scanName();
		if (docTypeID.equals("DOCTYPE"))
		{
			scanner.skipDataUpTo('>');
		} else
		{
			fatalError(ERROR_WRONG_TAG_HEADER, null);
		}
	}
}
/**
 */
protected void parseDocument()
	throws XMLException, IOException
{
	try
	{
		while (!scanner.isEOF())
		{
			if (scanner.skippedChar('<'))
			{
				parseTag();
			} else
			{
				parsePCDATA();
			}
		}
	} catch (EOFException e)
	{
		// return;
	}
}
/**
 */
protected void parsePCDATA()
	throws XMLException, IOException
{
	String pcData = scanner.scanUpTo('<');
	if (pcData.length() == 0)
	{
		fatalError(ERROR_ILLEGAL_CHARACTER, null);
	}
	// Got PCDATA
	String data = decode(pcData);
	characters(data.toCharArray(), 0, data.length());
}
/**
 */
protected void parseProcessingInstruction()
	throws XMLException, IOException
{
	String target = scanner.scanName();
	if (target != null)
	{
		if (!scanner.skippedSpace())
		{
			fatalError(ERROR_WRONG_PI, null);
		}
		try
		{
			String data = "";
			while (true)
			{
				data += scanner.scanIncludingIllegalsUpTo('?');
				scanner.skipNextChar(); // Skip '?'
				if (scanner.skippedChar('>'))
				{
					processingInstruction(target, data);
					return;
				} else
				{
					data += "?";
				}
			}
		} catch (EOFException e)
		{
			fatalError(ERROR_WRONG_PI, null);
		}
	} else
	{
		fatalError(ERROR_WRONG_PI, null);
	}
}
/**
 */
protected void parseProlog()
	throws XMLException, IOException
{
	startedDocument = false;
	try
	{
		scanner.skipNextSpaces();
		while (!startedDocument)
		{
			if (scanner.skippedChar('<'))
			{
				parseTag();
				startedDocument = true;
			} else
			{
				fatalError(ERROR_TAG_EXPECTED, null);
			}
		}
	} catch (EOFException e)
	{
		// return;
	}
}
/**
 */
protected void parseTag()
	throws XMLException, IOException
{
	if (scanner.skippedChar('/'))	// End Tag
	{
		String tagName = scanner.scanName();
		if (tagName != null)
		{
			if (scanner.skippedChar('>'))
			{
				// Return always a canonical representation for the tag name
				endElement((keySensitive?tagName:tagName.toUpperCase()).intern());
			} else
			{
				fatalError(ERROR_GT_EXPECTED, null);
			}
			
		} else
		{
			fatalError(ERROR_TAG_NAME_EXPECTED, null);
		}
	} else
	if (scanner.skippedChar('?'))
	{
		parseProcessingInstruction();
	} else
	if (scanner.skippedChar('!'))
	{
		parseDocTypeDeclaration();
		// Read without any validation
		//			<!-- ...-->
		//			<!DOCTYPE ...>
		//			...
		//scanner.skipDataUpTo('>');
	} else	// Tag
	{
		String tagName = scanner.scanName();
		if (tagName != null)
		{
			parseTagNamed(tagName);
		} else
		{
			fatalError(ERROR_WRONG_TAG_HEADER, null);
		}
	}
}
/**
 */
protected void parseTagNamed(String tagName)
	throws XMLException, IOException
{
	// Return always a canonical representation for the tag name
	String tName = (keySensitive?tagName:tagName.toUpperCase()).intern();
	XMLAttributeList attributList;
	scanner.skipNextSpaces();
	String attributeName = scanner.scanName();
	if (attributeName != null)
	{
		attributList = parseAttributeListStartingWith(attributeName);
	} else
	{
		attributList = getEmptyAttributeList();
	}
	if (scanner.skippedChar('>'))
	{
		// Got Tag
		startElement(tName, attributList);
	} else
	if (scanner.skippedChar('/'))
	{
		if (scanner.skippedChar('>'))
		{
			//Got Empty Tag
			startElement(tName, attributList);
			endElement(tagName);
		} else
		{
			fatalError(ERROR_GT_EXPECTED, null);
		}
	} else
	{
		fatalError(ERROR_GT_EXPECTED, null);
	}
}
/**
 */
protected abstract void processingInstruction(String target, String data)
	throws XMLException;
/**
 */
public void setCharDecoder(CharDecoder charDecoder)
{
	this.charDecoder = charDecoder;
}
/**
 */
public void setKeySensitive(boolean keySensitive)
{
	this.keySensitive = keySensitive;
}
/**
 */
protected void setStream(Reader stream)
{
	this.stream = stream;
	this.scanner = new MicroXMLScanner(stream);
}
/**
 */
protected abstract void startDocument() throws XMLException;
/**
 */
protected abstract void startElement(String tagName, XMLAttributeList attributeList)
	throws XMLException;
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