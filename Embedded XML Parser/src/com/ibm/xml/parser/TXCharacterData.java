package com.ibm.xml.parser;

import org.w3c.dom.*;

import com.ibm.esc.xml.parser.Messages;
/**
 * Type comment
 */
public abstract class TXCharacterData extends Child implements CharacterData
{
	String data = null;
/**
 * TXCharacterData constructor comment.
 */
public TXCharacterData() {
	super();
}
/**
 * Append <var>data</var> to the end of the character data in this Text Node.
 * <p>This method is defined by DOM.
 * @param data      Data to append to existing character data in this Text Node.
 * @see #insertData
 * @see #replaceData
 */
public synchronized void appendData(String data) throws DOMException
{
	StringBuffer sb = new StringBuffer(this.data.length()+data.length());
	sb.append(this.data);
	sb.append(data);
	this.data = sb.toString();
	//clearDigest();
}
/**
 * Returns the actual content.
 * <p>This method is defined by DOM.
 * @return          The actual content.
 * @see #setData         
 */
public String getData() {
	return this.data;
}
/**
 * <p>This method is defined by DOM.
 *
 */
public int getLength() {
	return this.data.length();
}
	/**
	 * <p>This method is defined by DOM.
	 */
	public String getNodeValue() {
		return getData();
	}
/**
 * Sets the actual content.
 * <p>This method is defined by DOM.
 * @param data      The actual content.
 * @see #getData         
 */
public void setData(String data) {
	this.data = data;
	//clearDigest();
}
/**
 * Returns a substring in specified range.
 * <p>This method is defined by DOM.
 *
 * @param start Start offset to extract.
 * @param count The number of characters to extract.
 */
public synchronized String substringData(int start, int count) throws DOMException
{
	if (start < 0 || start >= this.data.length())
		throw new TXDOMException(DOMException.INDEX_SIZE_ERR, 
			Messages.getString("EmbeddedXMLParser.Out_of_bounds__{0}__the_length_of_data_is__{1}", new Object[] {Integer.toString(start), Integer.toString(this.data.length())}));
	if (count < 0)
		throw new TXDOMException(DOMException.INDEX_SIZE_ERR, 
			Messages.getString("EmbeddedXMLParser.Invalid_count__{0}", count));
	int end = start+count;
	if (end > this.data.length())  end = this.data.length();
	return this.data.substring(start, end);
}
}