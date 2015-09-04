package com.ibm.xml.parser;

import org.w3c.dom.*;

/**
 * The Child class implements the Node interface as defined by the Document
 * Object Model (DOM).
 * <p>
 * Node is the base type of most objects in the Document Object Model. It 
 * may have an arbitrary number (including zero) of sequentially ordered
 * Child Nodes. It usually has a parent Node; the exception being that the
 * root Node in a document hierarchy has no parent.
 * <p>
 * The child node has an added property, <i>userData</i>, that allows
 * the programmer to attach application specific data to any node in the
 * document tree that extends Child.
 */
 
public abstract class Child implements Node
{
	private Node parentNode = null;
	private Node previousSibling = null;
	private Node nextSibling = null;
	TXDocument ownerDocument = null;
												// Defined in DOM spec.
	public static final String NAME_DOCUMENT    = "#document";
	public static final String NAME_COMMENT     = "#comment";
	public static final String NAME_TEXT        = "#text";
	public static final String NAME_CDATA       = "#cdata-section";
												// Not defined in DOM.
	public static final String NAME_ATTDEF      = "#attribute-definition";
	public static final String NAME_ATTLIST     = "#attribute-definition-list";
	public static final String NAME_ELEMENT_DECL= "#element-declaration";
/**
 * Child constructor comment.
 */
public Child() {
	super();
}
	/**
	 * Throws a <var>DOMException</var> because Child Nodes are leaf Nodes.
	 * If the Child class is extended by non-leaf Nodes, this method should be overridden.
	 * <p>This method is defined by DOM.
	 *
	 * @see com.ibm.xml.parser.Parent#removeChild
	 */
	public Node appendChild(Node newChild) throws DOMException {
		throw new TXDOMException(DOMException.HIERARCHY_REQUEST_ERR,
							   com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.com.ibm.xml.parser.Child_#appendChild()__Can__t_insert_any_nodes_to_this._8"));
	}
	/**
	 * Returns <VAR>null</VAR>.  
	 * <p>This method is defined by DOM.
	 * @see com.ibm.xml.parser.TXElement#getAttributes
	 */
	public NamedNodeMap getAttributes() {
		return null;
	}
	/**
	 * Returns an empty NodeList object because Child Nodes are leaf Nodes.  
	 * If the Child class is extended by non-leaf Nodes, this method should be overridden.
	 * <p>This method is defined by DOM.
	 *
	 * @return          Always returns an empty NodeList.
	 * @see com.ibm.xml.parser.Parent#getChildNodes
	 */
	public NodeList getChildNodes() {
		return TXNodeList.emptyNodeList;
	}
	/**
	 * Returns <var>null</var> because Child Nodes are leaf Nodes.
	 * If the Child class is extended by non-leaf Nodes, this method should be overridden.
	 * <p>This method is defined by DOM.
	 *
	 * @return          Always returns <var>null</var>.
	 * @see com.ibm.xml.parser.Parent#getFirstChild
	 */
	public Node getFirstChild() {
		return null;
	}
	/**
	 * Returns <var>null</var> because Child Nodes are leaf Nodes.
	 * If the Child class is extended by non-leaf Nodes, this method should be overridden.
	 * <p>This method is defined by DOM.
	 *
	 * @return          Always returns <var>null</var>.
	 * @see com.ibm.xml.parser.Parent#getLastChild
	 */
	public Node getLastChild() {
		return null;
	}
	/**
	 * Returns the Node immediately following this Node in a breadth-first traversal of the tree. If
	 * there is no such Node, <var>null</var> is returned.
	 * <p>This method is defined by DOM.
	 *
	 * @return          The Child Node immediately following this object in the parent's Child list,
	 *                    or <var>null</var> if this object is the last sibling.
	 * @see #getPreviousSibling
	 */
	public Node getNextSibling() {
		return this.nextSibling;
	}
	/**
	 * <p>This method is defined by DOM.
	 *
	 */
	public String getNodeValue() {
		return null;
	}
	/**
	 * <p>This method is defined by DOM.
	 *
	 */
public Document getOwnerDocument()
{
	return ownerDocument;
}
	/**
	 * Returns the parent of the given Node instance. If this Node is the root of the document object tree,
	 * or if the Node has not been added to a document tree, <var>null</var> is returned.
	 * <p>This method is defined by DOM.
	 * @return          Parent Node associated with this object, or <var>null</var> if no parent.
	 */
	public Node getParentNode() {
		return parentNode;
	}
	/**
	 * Returns the Node immediately preceding this Node in a breadth-first traversal of the tree. If
	 * there is no such Node, <var>null</var> is returned.
	 * <p>This method is defined by DOM.
	 *
	 * @return          The Child Node immediately preceding this object in the Parent's Child list,
	 *                    or <var>null</var> if this object is the first sibling.
	 * @see #getNextSibling
	 */
	public Node getPreviousSibling() {
		return this.previousSibling;
	}
/**
 * Returns all text associated with this Node without considering entities.                                           
 * This method is intended to be overridden by DOM-defined Node types.
 * @return          Always returns <var>""</var>.
 * @see com.ibm.xml.parser.TXText#getText
 * @see #toXMLString
 */
public String getText() {
	return "";
}
	/**
	 * Returns <var>false</var> because Child Nodes are leaf Nodes.  
	 * If the Child class is extended by non-leaf Nodes, this method should be overridden.
	 * <p>This method is defined by DOM.
	 *
	 * @return          Always returns <var>false</var>.
	 * @see com.ibm.xml.parser.Parent#hasChildNodes
	 */
	public boolean hasChildNodes() {
		return false;
	}
	/**
	 * Throws a <var>DOMException</var> because Child Nodes are leaf Nodes.
	 * If the Child class is extended by non-leaf Nodes, this method should be overridden.
	 * <p>This method is defined by DOM.
	 *
	 * @param   oldChild    Not used.
	 * @return              Nothing is ever returned (an exception is thrown).
	 * @exception org.w3c.dom.DOMException Thrown to indicate that no children exist in leaf Nodes.
	 * @see com.ibm.xml.parser.Parent#removeChild
	 */
	public Node removeChild(Node oldChild) throws DOMException {
		throw new TXDOMException(DOMException.NOT_FOUND_ERR,
							   com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.com.ibm.xml.parser.Child_#removeChild()__Can__t_insert_any_nodes_to_this._10"));
	}
void setNextSibling(Node nextSibling)
{
	this.nextSibling = nextSibling;
}
	/**
	 * <p>This method is defined by DOM.
	 *
	 */
	public void setNodeValue(String arg) {
		throw new TXDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.setNodeValue(String)_isn__t_supported_in_this_class._11"));
	}

public void setOwnerDocument(TXDocument ownerDocument)
{
	this.ownerDocument = ownerDocument;
}
protected void setParentNode(Node parentNode)
{
		this.parentNode = parentNode;
}
void setPreviousSibling(Node prevSibling)
{
	this.previousSibling = prevSibling;
}
}