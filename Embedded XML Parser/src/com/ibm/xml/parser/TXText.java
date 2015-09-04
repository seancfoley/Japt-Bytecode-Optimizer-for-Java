package com.ibm.xml.parser;

import org.w3c.dom.*;
import java.io.*;
/**
 * Type comment
 */
public class TXText extends TXCharacterData implements Text
{
	private boolean isIgnorableWhitespace    = false;
/**
 * Constructor.
 * @param data      The actual content of the Text Node.
 */
public TXText(String data) {
	this.data = data;
}
/**
 *
 */
public String getNodeName() {
	return Child.NAME_TEXT;
}
/**
 * Returns that this object is a Text Node. 
 * <p>This method is defined by DOM.
 * @return          Text Node indicator.
 */
public short getNodeType() {
	return Node.TEXT_NODE;
}
/**
 * Returns all text associated with this Node without considering entities.                                           
 * <p>This method is defined by Child.
 * @return          Text associated with this object, or <var>""</var> if no Text.
 * @see com.ibm.xml.parser.Child#toXMLString
 * @see #getData
 */
public String getText() {                   
	return getData();
}
/**
 * Sets, at the Text level, whether space is to be preserved.  
 * This value is used, for example, to determine if space is to be preserved
 * in Text Nodes during printWithFormat() operations.
 * <p>By default, this Text Node is not ignorable whitespace.  The XML4J parser
 * may, depending on the value if its <var>isPreserveSpace</var>, override this default
 * setting if no significant text is detected for this Text Node.
 * @param  isIgnorableWhitespace    <code>=true</code> space is to be preserved; 
 *                                  <code>=false</code> space is to be ignored.
 */
public void setIsIgnorableWhitespace(boolean isIgnorableWhitespace)
{
	this.isIgnorableWhitespace = isIgnorableWhitespace;
}
}