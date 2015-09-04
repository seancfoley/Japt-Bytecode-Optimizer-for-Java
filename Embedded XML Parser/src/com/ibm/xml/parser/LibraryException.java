package com.ibm.xml.parser;

/*
 * (C) Copyright IBM Corp. 1998,1999  All rights reserved.
 *
 * US Government Users Restricted Rights Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */

/**
 * XML4J parser exception which signals that the XML parser has detected an internal error
 * of some sort.  Often this means that the document object hierarchy includes a Node(s)
 * which is invalid or corrupted due to invalid actions performed by a DOM application.
 *
 * @version Revision: 74 1.4 src/com/ibm/xml/parser/LibraryException.java, parser, xml4j2, xml4j2_0_0 
 * @author TAMURA Kent &lt;kent@trl.ibm.co.jp&gt;
 * @see java.lang.RuntimeException
 */
public class LibraryException extends RuntimeException {

	/**
	 * Constructor for exception with no detail message.
	 */
	public LibraryException() {
		super();
	}
	/**
	 * Constructor for exception with detail message.
	 * @param msg       The detail message for the exception.
	 */
	public LibraryException(String msg) {
		super(msg);
	}
}