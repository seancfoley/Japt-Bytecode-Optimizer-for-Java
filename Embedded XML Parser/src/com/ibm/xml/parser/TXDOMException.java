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

import org.w3c.dom.DOMException;

/**
 *
 * @version Revision: 03 1.4 src/com/ibm/xml/parser/TXDOMException.java, parser, xml4j2, xml4j2_0_0 
 * @author TAMURA Kent &lt;kent@trl.ibm.co.jp&gt;
 * @see org.w3c.dom.DOMException
 */
public class TXDOMException extends DOMException {
public TXDOMException(short code, String message)
{
	super(code, message);
}
}