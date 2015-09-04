package com.ibm.xml.parser;

import org.w3c.dom.*;
import java.io.*;
/**
 * Type comment
 */
public class TXAttribute extends Parent implements Attr
{
	private String name = null;
	private String value = null;
	private boolean isEmptyValueAttibute = true;
/**
 * Constructor.
 * @param name      The name of this attribute.
 * @param value     The string value of this attribute.
 */
public TXAttribute(String name, String value) {
	this.name = name;
	this.value = value;
}
/**
 * Return a <CODE>boolean</CODE> telling if
 * the attribute was declared with an empty value.
 */
public boolean getIsEmptyValueAttibute()
{
	return isEmptyValueAttibute;
}
/**
 * Returns the qualified name of this attribute.  If the attribute name has a namespace prefix, 
 * the prefix will still be attached.
 * <p>This method is defined by DOM.
 * @return          The name of this attribute (should never be null)
 */
public String getName() {
	return this.name;
}
/**
 * <p>This method is defined by DOM.
 * @see #getName
 */
public String getNodeName() {
	return this.name;
}
/**
 * Returns that this object is an Attribute Node.
 * <p>This method is defined by DOM.
 * @return          <CODE>Attr</CODE> Node indicator.
 */
public short getNodeType() {
	return Node.ATTRIBUTE_NODE;
}
/**
 * Returns the value of this attribute.
 * <p>This method is defined by DOM.
 * @return          The value of this attribute, or "" if no value.
 * @see #getTypedValue
 * @see #setValue
 * @see #toString
 */
public String getValue() {
	if (this.value != null)
		return this.value;
	this.value = getText();
	return this.value;
}
/**
 * Set a <CODE>boolean</CODE> telling if
 * the attribute was declared with an empty value.
 */
public void setIsEmptyValueAttibute(boolean isEmptyValueAttibute)
{
	this.isEmptyValueAttibute = isEmptyValueAttibute;
}
/**
 * Sets the value of this attribute.
 * <p>This method is defined by DOM.
 * @param value     The value of this attribute.
 */
public void setNodeValue(String value) {
	this.value = value;
	if (value != null) {
		synchronized (this.childNodes) {
			while (this.getFirstChild() != null)
				this.removeChild(this.getFirstChild());
			//checkFactory();
			this.appendChild(this.ownerDocument.createTextNode(value));
			//this.appendChild(new TXText(value));
		}
	}
	//clearDigest();
}
/**
 * Sets the value of this attribute.
 * @param value     The value of this attribute.
 * @see #getValue
 * @see #setNodeValue
 * @see #getTypedValue
 */
public void setValue(String value)
{
	if (value == null)
	{
		setIsEmptyValueAttibute(true);
		setNodeValue("");
	} else
	{
		setIsEmptyValueAttibute(false);
		setNodeValue(value);
	}
}
}