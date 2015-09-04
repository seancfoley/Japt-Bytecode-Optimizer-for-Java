package com.ibm.esc.xml.parser;

/**
 * Default Exception handled by the AbstractMicroXMLParser
 */
public class XMLException extends Exception
{
	private Exception wrappedException;
/**
 * XMLException constructor comment.
 */
public XMLException() {
	super();
}
/**
 * XMLException constructor comment.
 * @param s java.lang.String
 */
public XMLException(Exception wrappedException)
{
	super();
	setWrappedException(wrappedException);
}
/**
 * XMLException constructor comment.
 * @param s java.lang.String
 */
public XMLException(String s) {
	super(s);
}
/**
 */
public Exception getWrappedException()
{
	return wrappedException;
}
/**
 */
public void setWrappedException(Exception wrappedException)
{
	this.wrappedException = wrappedException;
}
}