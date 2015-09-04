package com.ibm.esc.xml.parser;

import java.io.*;
import com.ibm.esc.xml.core.*;
/**
 * Type comment
 */
public class MicroXMLScanner
{
	private Reader reader;
	private char[] rBuffer;
	private int nextRCharIndex, rCapacity;
	private char nextRChar;
	private char[] wBuffer;
	private int lineCounter = 0;
	private int columnCounter = 0;
	
	private static int defaultCharBufferSize = 8192; //8Kb
	public static final char EOF_CHARACTER = '\uFFFF';
/**
 * MicroXMLScanner constructor comment.
 */
protected MicroXMLScanner() {
	super();
}
/**
 * MicroXMLScanner constructor comment.
 */
public MicroXMLScanner(Reader reader)
{
	this(reader, defaultCharBufferSize);
}
/**
 * MicroXMLScanner constructor comment.
 */
public MicroXMLScanner(Reader reader, int bufferSize)
{
	setReader(reader);	
	if (bufferSize <= 0)
	    throw new IllegalArgumentException(Messages.getString("EmbeddedXMLParser.Buffer_size_<__0_1"));
	rBuffer = new char[bufferSize];
	nextRCharIndex = 0;
	try
	{
		fillBuffer();
	} catch (IOException e)
	{
	}
	wBuffer = new char[bufferSize];
}
/**
 */
protected void fillBuffer()
	 throws IOException
{
	if (rCapacity == -1)
	{
		// Last access generated an EOF
		throw new EOFException();
	}
	int fillResult = reader.read(rBuffer);
	if (fillResult == -1)
	{
		rCapacity = -1;
		nextRChar = EOF_CHARACTER;
	} else
	{
		rCapacity = fillResult;
		nextRCharIndex = 0;
		nextRChar = rBuffer[nextRCharIndex];
	}
}
/**
 */
public boolean isEOF()
{
	return rCapacity == -1;
}
/**
 */
public int getColumnCounter()
{
	return columnCounter;
}
/**
 */
public int getLineCounter()
{
	return lineCounter;
}
/**
 */
public int getLineNumber()
{
	return lineCounter;
}
/**
 * Scan text up to a space, an illegal value EXCLUDED
 */
public String scanAttributeValue()
	throws IOException
{
	int wCharIndex = 0;
	boolean stop = false;
	while (!stop)
	{
		if ((XMLChar.isSpace(nextRChar)) || (XMLChar.isIllegal(nextRChar)))
		{
			stop = true;
		} else
		{
			columnCounter++;
			
			wBuffer[wCharIndex] = nextRChar;
			wCharIndex++;
			if (wCharIndex > wBuffer.length)
			{
				char newWBuffer[] = new char[Math.max(wBuffer.length << 1, wCharIndex)];
				System.arraycopy(wBuffer, 0, newWBuffer, 0, wBuffer.length);
				wBuffer = newWBuffer;
			}
			nextRCharIndex++;
			if (nextRCharIndex >= rCapacity) fillBuffer();
			nextRChar = rBuffer[nextRCharIndex];
		}
	}
	if (wCharIndex == 0)
	{
		return new String();
	} else
	{
		return new String(wBuffer, 0, wCharIndex);
	}
}
/**
 * @chr cannot be '\n'
 * Scan text up to @chr EXCLUDED
 */
public String scanIncludingIllegalsUpTo(char chr)
	throws IOException
{
	int wCharIndex = 0;
	boolean stop = false;
	while (!stop)
	{
		if ((nextRChar == chr) || (nextRChar == EOF_CHARACTER))
		{
			stop = true;
		} else
		{
			if (nextRChar == '\n')
			{
				lineCounter++;
				columnCounter = 0;
			} else
			{
				columnCounter++;
			}
			wBuffer[wCharIndex] = nextRChar;
			wCharIndex++;
			if (wCharIndex > wBuffer.length)
			{
				char newWBuffer[] = new char[Math.max(wBuffer.length << 1, wCharIndex)];
				System.arraycopy(wBuffer, 0, newWBuffer, 0, wBuffer.length);
				wBuffer = newWBuffer;
			}
			nextRCharIndex++;
			if (nextRCharIndex >= rCapacity) fillBuffer();
			nextRChar = rBuffer[nextRCharIndex];
		}
	}
	if (wCharIndex == 0)
	{
		return new String();
	} else
	{
		return new String(wBuffer, 0, wCharIndex);
	}
}
/**
 */
public String scanName()
	throws IOException
{
	int wCharIndex = 0;
	while (XMLChar.isNameChar(nextRChar))
	{
		wBuffer[wCharIndex] = nextRChar;
		columnCounter++;
		nextRCharIndex++;
		if (nextRCharIndex >= rCapacity) fillBuffer();
		nextRChar = rBuffer[nextRCharIndex];
		wCharIndex++;
		if (wCharIndex > wBuffer.length)
		{
			char newWBuffer[] = new char[Math.max(wBuffer.length << 1, wCharIndex)];
			System.arraycopy(wBuffer, 0, newWBuffer, 0, wBuffer.length);
			wBuffer = newWBuffer;
		}
	}
	if (wCharIndex == 0)
	{
		return null;
	} else
	{
		return new String(wBuffer, 0, wCharIndex);
	}
}
/**
 * @chr cannot be '\n'
 * Scan text up to @chr EXCLUDED
 */
public String scanUpTo(char chr)
	throws IOException
{
	int wCharIndex = 0;
	boolean stop = false;
	while (!stop)
	{
		if ((nextRChar == chr) || (XMLChar.isIllegal(nextRChar)))
		{
			stop = true;
		} else
		{
			if (nextRChar == '\n')
			{
				lineCounter++;
				columnCounter = 0;
			} else
			{
				columnCounter++;
			}
			wBuffer[wCharIndex] = nextRChar;
			wCharIndex++;
			if (wCharIndex > wBuffer.length)
			{
				char newWBuffer[] = new char[Math.max(wBuffer.length << 1, wCharIndex)];
				System.arraycopy(wBuffer, 0, newWBuffer, 0, wBuffer.length);
				wBuffer = newWBuffer;
			}
			nextRCharIndex++;
			if (nextRCharIndex >= rCapacity) fillBuffer();
			nextRChar = rBuffer[nextRCharIndex];
		}
	}
	if (wCharIndex == 0)
	{
		return new String();
	} else
	{
		return new String(wBuffer, 0, wCharIndex);
	}
}
/**
 */
protected void setReader(Reader reader)
{
	this.reader = reader;
}
/**
 * @chr cannot be '\n'
 * Skip text up to @chr INCLUDED
 */
public void skipDataUpTo(char chr)
	throws IOException
{
	int openSquareBrackets = 0;
	int openAngleBrackets = 0;
	boolean stop = false;
	while (!stop)
	{
		if (nextRChar == chr)
		{
			columnCounter++;
			stop = (openSquareBrackets == 0) && (openAngleBrackets == 0);
		} else
		if (nextRChar == '\n')
		{
			lineCounter++;
			columnCounter = 0;
		} else
		if (nextRChar == '[')
		{
			columnCounter++;
			openSquareBrackets++;
		} else
		if (nextRChar == '<')
		{
			columnCounter++;
			openAngleBrackets++;
		} else
		if (nextRChar == ']')
		{
			columnCounter++;
			openSquareBrackets--;
		} else
		if (nextRChar == '>')
		{
			columnCounter++;
			openAngleBrackets--;
		} else
		{
			columnCounter++;
		}
		nextRCharIndex++;
		if (nextRCharIndex >= rCapacity) fillBuffer();
		nextRChar = rBuffer[nextRCharIndex];
	}
}
/**
 * @chr should not be \n
 */
public void skipNextChar()
	throws IOException
{
	columnCounter++;
	nextRCharIndex++;
	if (nextRCharIndex >= rCapacity) fillBuffer();
	nextRChar = rBuffer[nextRCharIndex];
}
/**
 */
public void skipNextSpaces()
	throws IOException
{
	while (true)
	{
		if (nextRChar == ' ' || nextRChar == '\t' || (nextRChar == '\r'))
		{
			columnCounter++;
		} else
		if (nextRChar == '\n')
		{
			lineCounter++;
			columnCounter = 0;
		} else
		{
			return;
		}
		nextRCharIndex++;
		if (nextRCharIndex >= rCapacity) fillBuffer();
		nextRChar = rBuffer[nextRCharIndex];
	}
}
/**
 * If the @nextChar is a @chr (this char should not be a White Space),
 * skips it and return true
 * else return false.
 */
public boolean skippedChar(char chr)
	throws IOException
{
	if (nextRChar != chr) return false;
	columnCounter++;
	nextRCharIndex++;
	if (nextRCharIndex >= rCapacity) fillBuffer();
	nextRChar = rBuffer[nextRCharIndex];
	return true;
}
/**
 * If the @nextChar is a White Space, skips it and return true
 * else return false.
 */
public boolean skippedSpace()
	throws IOException
{
	int ch = nextRChar;
	if (ch > 0x20) return false;
	if (ch == 0x20 || ch == 0x09) // ' ' || '\t'
	{
		columnCounter++;
	} else
	if (ch == 0x0A)	// '\n'
	{
		lineCounter++;
		columnCounter = 0;
	} else
	{
		return false;
	}
	nextRCharIndex++;
	if (nextRCharIndex >= rCapacity) fillBuffer();
	nextRChar = rBuffer[nextRCharIndex];
	return true;
}
}