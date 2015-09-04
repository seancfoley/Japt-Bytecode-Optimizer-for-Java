package com.ibm.xml.parser;

import org.w3c.dom.*;
import java.io.*;

/**
 * The TXDocument class implements the Document interface as defined by the Document Object Model (DOM).
 * <p>The Document object represents the entire XML document. Conceptually, it is the root 
 * of the document tree, and provides the primary access to the document's data.
 */
public class TXDocument extends Parent implements Document
{
	private TXElement rootElement = null;
/**
 * TXDocument constructor comment.
 */
public TXDocument() {
	super();
}
/**
 * Create and return a new <CODE>Attr</CODE>.
 * <p>It should be noted this factory method is not used by the parser. 
 * <p>This method is defined by DOM.
 * @param name      The name of this attribute. 
 * @return          New <var>TXAttribute</var>.
 * @exception org.w3c.dom.DOMException INVALID_NAME_ERR: <VAR>name</var> is invalid.
 */
public Attr createAttribute(String name) throws DOMException {
	if (!Util.checkName(name))
		throw new TXDOMException(DOMException.INVALID_CHARACTER_ERR, 
			com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Invalid_name__{0}", name));
	TXAttribute attr = new TXAttribute(name, null);
	attr.setOwnerDocument(this);
	return attr;
}
/**
 * Create and initialize a <code>TXCDATASection</code> instance using the supplied parameters.
 * <p>This method is defined by DOM.
 * @param data      The actual content of the CDATASection Node.
 * @return          Newly created TXCDATASection.
 * @exception org.w3c.dom.DOMException Never thrown.
 */
public CDATASection createCDATASection(String data) throws DOMException {
	TXCDATASection cdata = new TXCDATASection(data);
	cdata.setOwnerDocument(this);
	return cdata;
}
/**
 * Create and initialize a <code>TXElement</code> instance using the supplied parameters.
 * <p>This method is defined by DOM.
 * @param   name    This Element's tag name (qualified name).
 * @return          Newly created TXElement.
 * @exception org.w3c.dom.DOMException INVALID_NAME_ERR: <VAR>name</var> is invalid.
 */
public Element createElement(String name) throws DOMException {
	if (!Util.checkName(name))
		throw new TXDOMException(DOMException.INVALID_CHARACTER_ERR, 
			com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Invalid_name__{0}", name));
	TXElement el = new TXElement(name);
	el.setOwnerDocument(this);
	return el;
}
/**
 * Create and initialize a <code>TXPI</code> instance using the supplied parameters.
 * <p>This method is defined by DOM.
 * @param name      The first token following the markup.
 * @param data      From the character immediately after <var>name</var> to the 
 *                    character immediately preceding the <code>?&gt;</code>.
 * @return          Newly created TXPI.
 * @exception org.w3c.dom.DOMException INVALID_NAME_ERR: <VAR>name</var> is invalid.
 */
public ProcessingInstruction createProcessingInstruction(String name, String data)
	throws DOMException {
	if (!Util.checkName(name))
		throw new TXDOMException(DOMException.INVALID_CHARACTER_ERR, 
			com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Invalid_PI_target_name__{0}", name));
	TXPI pi = new TXPI(name, data);
	pi.setOwnerDocument(this);
	return pi;
}
/**
 * Create and initialize a <code>TXText</code> instance using the supplied parameters.
 * <p>This method is defined by DOM.
 * @param data      The actual content of the Text Node.
 * @return          Newly created TXText.
 */
public Text createTextNode(String data)
{
	return createTextNode(data, false);
}
/**
 * Create and initialize a <code>TXText</code> instance using the supplied parameters.
 * @param   data                    The actual content of the Text Node.
 * @param  isIgnorableWhitespace    <code>=true</code> space is to be preserved; 
 *                                  <code>=false</code> space is to be ignored.
 * @return      Newly created TXText.
 * @see com.ibm.xml.parser.TXText#setIsIgnorableWhitespace
 */
public TXText createTextNode(String data, boolean isIgnorableWhitespace) {
	TXText te = new TXText(data);
	te.setOwnerDocument(this);
	te.setIsIgnorableWhitespace(isIgnorableWhitespace);
	return te;
}
/**
 * Returns this Document's root Element.
 * @return          Document root Element, or <var>null</var> if no root Element.
 * @see #getRootName
 */
public Element getDocumentElement()
{
	return this.rootElement;
}
/**
 * Returns a <var>NodeList</var> of matches through all child Element Nodes.
 * Searching is done recursively, not just for immediate Child Nodes.
 * The returned NodeList is not "live".
 * The specified qualified name refers to the Element tag name (see Namespace for details).
 * <p>This method is defined by DOM.
 * @param   qName   Qualified name to match against in all subordinate Elements.
 * @return          A <var>NodeList</var> of matched <var>Element</var> Nodes (<var>TXElement</var>).
 *                  If no matches, an empty NodeList is returned.
 * @see com.ibm.xml.parser.TXElement#getElementsByTagName
 * @see com.ibm.xml.parser.Namespace
 */
public NodeList getElementsByTagName(String qName)
{
	return null != rootElement ? rootElement.getElementsByTagName(qName)
		: TXNodeList.emptyNodeList;
}
	/**
	 *
	 */
	public String getNodeName() {
		return Child.NAME_DOCUMENT;
	}
	/**
	 * Returns that this object is a Document Node.
	 * <p>This method is defined by DOM.
	 * @return          Document Node indicator.
	 */
	public short getNodeType() {
		return Node.DOCUMENT_NODE;
	}
	/**
	 * Return all text associated with this Node and its children without considering entities.                                           
	 * <p>This method is defined by Child.
	 * @return          Text associated with all children, or <var>""</var> if no children.
	 * @see com.ibm.xml.parser.Child#toXMLString
	 */
	public String getText() {                   
		return null != this.rootElement ? this.rootElement.getText() : "";
	}
/**
 * Insert a Child Node into the specified position.
 * @param child     The Node being inserted.
 * @param index     0-based index into the list of children.
 * @exception DOMException Thrown if the document's root element is set twice.
 */
public synchronized void insert(Node child, int index)
	throws DOMException
{
	super.insert(child, index);
	
	if (child instanceof TXElement) {
		if (null != this.rootElement)
			throw new TXDOMException(
				DOMException.NO_MODIFICATION_ALLOWED_ERR,
				com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.com.ibm.xml.parser.TXDocument_#insert()__Document_root_Element_was_set_twice._"));
		else
			this.setDocumentElement((Element)child);
	}
}
/**
 * Removes the Child Node indicated by <var>oldChild</var> from this Nodes list of children,
 * and returns it. If <var>oldChild</var>
 * was not a Child of this Node, a <var>DOMException</var> is thrown.
 * <p>This method is defined by DOM.
 * @param oldChild  The Child Node being removed.
 * @return          The Child Node being removed.
 * @exception org.w3c.dom.DOMException Thrown if <var>oldChild</var> is not a Child of this object.
 * @see #replaceChild 
 */
public Node removeChild(Node oldChild)
	throws DOMException
{
	super.removeChild(oldChild);
	if (oldChild == this.getDocumentElement())
		this.setDocumentElement(null);
/*		else if (oldChild == this.getDoctype()) 
		this.doctype = null;*/
	return oldChild;
}
/**
 * Sets this Document's root Element.
 * @return          Document root Element.
 * @see #getDocumentElement
 */
protected void setDocumentElement(Element rootElement)
{
	this.rootElement = (TXElement)rootElement;
}
}