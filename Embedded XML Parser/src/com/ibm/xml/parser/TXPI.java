package com.ibm.xml.parser;

import org.w3c.dom.*;
import com.ibm.esc.xml.core.*;
/**
 * Type comment
 */
public class TXPI extends Child implements ProcessingInstruction
{
	private String  name            = null;
	private String  data            = null;
/**
 * Constructor.
 * @param name      The first token following the markup.
 * @param data      From the first non white space character after PI target (<var>name</var>)
 *                  to the character immediately preceding the <code>?&gt;</code>.
 */
public TXPI(String name, String data) {
	this.name = name;
	if (data.length() > 0) {
		int start = 0;
		while (start < data.length() && XMLChar.isSpace(data.charAt(start)))
			start ++;
		this.data = data.substring(start);
	} else
		this.data = data;
}
/**
 * Returns the data of the PI.  The PI data is from the character immediately after the 
 * PI name to the character immediately preceding the <code>?&gt;</code>.
 * <p>This method is defined by DOM.
 * @return          The PI data.
 * @see #setData
 */
public String getData() {
	return this.data;
}
/**
 * <p>This method is defined by DOM.
 *
 */
public String getNodeName() {
	return getTarget();
}
/**
 * Returns that this object is a PI Node.
 * <p>This method is defined by DOM.
 * @return          PI Node indicator.
 */
public short getNodeType() {
	return Node.PROCESSING_INSTRUCTION_NODE;
}
/**
 * <p>This method is defined by DOM.
 *
 * @see #getName
 */
public String getTarget() {
	return this.name;
}
	/**
	 * Return all text associated with this Node without considering entities.                                           
	 * <p>This method is defined by Child.
	 * @return          Always returns <var>""</var>.
	 * @see com.ibm.xml.parser.Child#toXMLString
	 */
	public String getText() {
		return "";
	}
/**
 * Sets the data of the PI.  The PI data is from the character immediately after the 
 * PI name to the character immediately preceding the <code>?&gt;</code>.
 * <p>This method is defined by DOM.
 * @param data      The PI data.
 * @see #getData
 */
public void setData(String data) {
	this.data = data;
	//clearDigest();
}
}