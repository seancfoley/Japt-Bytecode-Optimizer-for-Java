package com.ibm.xml.parser;

import org.w3c.dom.*;
import java.io.*;


/**
 * The TXElement class implements the Element interface as defined by the Document Object Model (DOM),
 * and implements the namespace interface as defined by the W3C.
 * <p>By far the vast majority (apart from text) of Node types that authors will
 * generally encounter when traversing a document will be Element Nodes. An Element
 * consists of its start tag, any attributes, any children, and its end tag.
 */
public class TXElement extends Parent implements Element
{
	String name = null;
	TXAttributeList attributes      = null;
	boolean         isPreserveSpace = false;
/**
 * Constructor.
 * @param tagName   this <CODE>Element</CODE>'s tag name (qualified name).
 *                  In the example <code>&lt;elementExample id="demo"&gt; ... &lt;/elementExample&gt;</code>,
 *                  the tag name is <code>elementExample</code>.
 */
public TXElement(String tagName) {
	setTagName(tagName);
}
/**
 * Returns an attribute's value from this <CODE>Element</CODE> that matches the specified
 * attribute name.
 * Both implicitly-defined (DTD) and explicitly-defined attributes are considered.
 * <p>This method is defined by DOM.
 * @param   name    The name to match in this <CODE>Element</CODE>'s list of attributes.
 * @return          The string value of the matching attribute; otherwise, <var>empty string</var>.
 * @see #getAttributeNode
 * @see #setAttribute
 * @see #setAttributeNode
 * @see #getLanguage
 */
public String getAttribute(String name) {
	if (null != this.attributes) {
		TXAttribute a = (TXAttribute)this.attributes.getNamedItem(name);
		return null == a ? "" : a.getValue();
	}
	return "";
}
/**
 * Returns an attribute's value from this <CODE>Element</CODE> that matches the specified
 * attribute name.
 * Both implicitly-defined (DTD) and explicitly-defined attributes are considered.
 * <p>This method is defined by DOM.
 * @param   name    The name to match in this <CODE>Element</CODE>'s list of attributes.
 * @return          The matching Attribute Node; otherwise, <var>null</var>.
 * @see #getAttribute
 * @see #setAttribute
 * @see #setAttributeNode
 */
public Attr getAttributeNode(String name) {
	if (null != this.attributes)
		return (Attr)this.attributes.getNamedItem(name);
	return null;
}
/**
 * Returns a <var>NodeList</var> of matches through all child Element Nodes.
 * Searching is done recursively, not just for immediate Child Nodes.
 * The specified qualified name refers to the Element tag name (see Namespace for details).
 * <p>This method is defined by DOM.
 * @param   qName   Qualified name to match against in all subordinate Elements. If it is "*", this method return all subordinate Elements.
 * @return          A <var>NodeList</var> of matched <var>Element</var> Nodes (<var>TXElement</var>).
 *                  If no matches, an empty NodeList is returned.
 *
 * PK: To simplify the class dependencies, this method was written recursivaly...
 */
public NodeList getElementsByTagName(String qName)
{
	return childNodes.getElementsByTagName(qName, new TXNodeList());
}
/**
 * Returns a <var>NodeList</var> of matches through all child Element Nodes.
 * Searching is done recursively, not just for immediate Child Nodes.
 * The specified qualified name refers to the Element tag name (see Namespace for details).
 * <p>This method is defined by DOM.
 * @param   qName   Qualified name to match against in all subordinate Elements. If it is "*", this method return all subordinate Elements.
 * @return          A <var>NodeList</var> of matched <var>Element</var> Nodes (<var>TXElement</var>).
 *                  If no matches, an empty NodeList is returned.
 *
 * PK: To simplify the class dependencies, this method was written recursivaly...
 */
protected NodeList getElementsByTagName(String qName, TXNodeList nodeList)
{
	if (getTagName().equals(qName))
	{
		nodeList.append(this);
	}
	childNodes.getElementsByTagName(qName, new TXNodeList());
	return nodeList;
}
/**
 *
 * @see #getTagName
 * @see #getName
 */
public String getNodeName() {
	return getTagName();
}
/**
 * Returns that this object is an Element Node.
 * <p>This method is defined by DOM.
 * @return          Element Node indicator.
 */
public short getNodeType() {
	return Node.ELEMENT_NODE;
}
/**
 * Returns this <CODE>Element</CODE>'s name.
 * In the example <code>&lt;elementExample id="demo"&gt; ... &lt;/elementExample&gt;</code>,
 * the tag name is <code>elementExample</code>.  If the Element's name has a namespace
 * prefix, the prefix will still be attached.
 * <p>This method is defined by DOM.
 * @return          The string that is this <CODE>Element</CODE>'s name, or <var>null</var> if no name.
 * @see #setTagName
 * @see #getName
 * @see #getNodeName
 */
public String getTagName() {
	return this.name;
}
/**
 * Returns, at the Element level, whether space is to be preserved.  
 * This value is used, for example, to determine if space is to be preserved
 * in Text Nodes during printWithFormat() operations.
 * @return                  <code>=true</code> space is to be preserved; 
 *                          <code>=false</code> space is to be ignored.
 * @see #setPreserveSpace
 * @see com.ibm.xml.parser.Parser#setPreserveSpace
 * @see com.ibm.xml.parser.TXText#setIsIgnorableWhitespace
 * @see com.ibm.xml.parser.TXDocument#printWithFormat
 */
public boolean isPreserveSpace() {
	return this.isPreserveSpace;
}
private void makeAttributeList() {
	if (null == this.attributes) {
		//checkFactory();
		this.attributes = new TXAttributeList();
		attributes.setParent(this);
	}
}
/**
 * Puts all Text Nodes in the sub-tree underneath this <CODE>Element</CODE> into a "normal"
 * form where only markup (e.g., tags, comments, PIs, CDATASections, and entity
 * references) separates Text Nodes.  This has the effect of combining Text Nodes
 * that have been separated due to document manipulation.
 * <p>This method is defined by DOM.
 */
public void normalize() {
	TXElement.normalize(this, this.isPreserveSpace());
}
private static void normalize(Node par, boolean preservespace) {
	Node prev = par.getFirstChild();
	if (prev == null)  return;
	if (prev.getNodeType() == Node.ELEMENT_NODE) {
		TXElement.normalize(prev, ((TXElement)prev).isPreserveSpace());
	} else if (prev.getNodeType() == Node.ENTITY_REFERENCE_NODE)
		TXElement.normalize(prev, preservespace);
	Node current;
	while ((current = prev.getNextSibling()) != null) {
		int type = current.getNodeType();
		if (type == Node.TEXT_NODE && prev.getNodeType() == Node.TEXT_NODE) {
			((Text)current).setData(prev.getNodeValue()+current.getNodeValue());
			par.removeChild(prev);
			if (!preservespace && Util.checkAllSpace(current.getNodeValue())) {
				Node p = current.getPreviousSibling();
				Node n = current.getNextSibling();
				if ((p == null || p.getNodeType() != Node.TEXT_NODE)
					&& (n == null || n.getNodeType() != Node.TEXT_NODE))
					((TXText)current).setIsIgnorableWhitespace(true);
			}
		} else if (type == Node.ELEMENT_NODE) {
			TXElement.normalize(current, ((TXElement)current).isPreserveSpace());
		} else if (type == Node.ENTITY_REFERENCE_NODE) {
			TXElement.normalize(current, preservespace);
		}
		prev = current;
	}
}
/**
 * Adds a new attribute name/value pair to this <CODE>Element</CODE> using the appropriate
 * factory. If an attribute by that name is already present in this <CODE>Element</CODE>,
 * it's value is changed to be that of <var>value</var>.
 * Both implicitly-defined (DTD) and explicitly-defined attributes are considered.
 * <p>This method is defined by DOM.
 * @param   name    The name of the attribute to create or update.
 * @param   value   The value of the created or updated attribute.
 * @see #getAttribute
 * @see #getAttributeNode
 * @see #setAttributeNode
 */
public void setAttribute(String name, String value) throws DOMException
{
	//checkFactory();
	Attr attr = this.getAttributeNode(name);
	if (attr == null) {
		attr = ownerDocument.createAttribute(name);
		setAttributeNode(attr);
		attr.setValue(value);
	} else {
		attr.setValue(value);
		//clearDigest();
	}
}
/**
 * Adds a new attribute to this <CODE>Element</CODE> using the appropriate
 * factory. If an attribute by that name is already present in this <CODE>Element</CODE>,
 * it's value is changed to be that of the specified <var>attribute</var>'s value.
 * Both implicitly-defined (DTD) and explicitly-defined attributes are considered.
 * <p>This method is defined by DOM.
 * @param   attribute   The attribute to create or update.
 * @return              The attribute that was created or updated.
 * @see #getAttribute
 * @see #getAttributeNode
 * @see #setAttribute
 */
public Attr setAttributeNode(Attr attribute) {
	makeAttributeList();
	return (Attr)this.attributes.setNamedItem(attribute);
}
/**
 * Sets this <CODE>Element</CODE>'s name.
 * In the example <code>&lt;elementExample id="demo"&gt; ... &lt;/elementExample&gt;</code>,
 * the tag name is <code>elementExample</code>.
 * @param   tagName The string that is this <CODE>Element</CODE>'s name.
 * @see #getTagName
 */
public void setTagName(String tagName)
{
	this.name = tagName;
	// clearDigest();
}
}