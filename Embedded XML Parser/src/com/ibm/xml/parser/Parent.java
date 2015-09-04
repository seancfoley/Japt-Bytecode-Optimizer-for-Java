package com.ibm.xml.parser;

import org.w3c.dom.*;

import com.ibm.esc.xml.parser.Messages;

/**
 * The Parent class extends Child and provides services in support of non-leaf Nodes.
 *
 * @version Revision: 82 1.5 src/com/ibm/xml/parser/Parent.java, parser, xml4j2, xml4j2_0_0 
 * @author TAMURA Kent &lt;kent@trl.ibm.co.jp&gt;
 * @see com.ibm.xml.parser.TXElement
 * @see com.ibm.xml.parser.TXDocument
 * @see com.ibm.xml.parser.DTD
 * @see com.ibm.xml.parser.Child
 * @see org.w3c.dom.Node
 */
public abstract class Parent extends Child
{
	TXNodeList childNodes =   new TXNodeList();
/**
 * Parent constructor comment.
 */
public Parent() {
	super();
}
	/**
	 * Inserts the specified Node as the last Child in this Node's list of children. 
	 * @param newChild  The Child Node being inserted.
	 * @return          The Child Node being inserted.
	 * @see #insertBefore
	 * @see #insertAfter
	 * @see #insertFirst
	 * @see #insert    
	 * @see #insertLast
	 */
	public synchronized Node appendChild(Node newChild) {
		if (newChild != null)
			insert(newChild, this.childNodes.getLength());
		return newChild;
	}
/**
 * Returns a NodeList object that will enumerate all children of this Node. If there 
 * are no children, an empty NodeList is returned. The content of the 
 * returned NodeList is "live" in the sense that changes to the children of the Node 
 * object that it was created from will be immediately reflected in the Nodes returned by 
 * the NodeList; it is not a static snapshot of the content of the Node. Similarly, changes 
 * made to the Nodes returned by the NodeList will be immediately reflected in the tree, 
 * including the set of children of the Node that the NodeList was created from.
 * <p>This method is defined by DOM.
 * @return          A NodeList of all children of this Parent Node.
 * @see #hasChildNodes
 * @see #elements
 * @see #getFirstChild
 * @see #getLastChild
 * @see #getChildrenArray
 */
public NodeList getChildNodes() {
	return childNodes;
}
/**
 * Returns the first Child of this Node. If there are no children, <var>null</var> is returned.
 * <p>This method is defined by DOM.
 * @return          The first Child of this Parent Node, or <var>null</var> if no children.
 * @see #getLastChild
 * @see #hasChildNodes
 * @see #getChildNodes
 * @see #elements
 */
public Node getFirstChild() {
	return 0 < this.childNodes.getLength() ? this.childNodes.item(0) : null;
}
	/**
	 * Returns the last Child of this Node. If there are no children, <var>null</var> is returned.
	 * <p>This method is defined by DOM.
	 *
	 * @return          The last Child of this Parent Node, or <var>null</var> if no children.
	 * @see #getFirstChild
	 * @see #hasChildNodes
	 * @see #getChildNodes
	 * @see #elements
	 */
	public Node getLastChild() {
		int size = this.childNodes.getLength();
		return 0 < size ? this.childNodes.item(size-1) : null;
	}
/**
 * Return all text associated with this Node and its children without considering entities.
 * <p>This method is defined by Child.
 * @return          Text associated with all children, or <var>""</var> if no children.
 * @see com.ibm.xml.parser.Child#toXMLString
 */
public String getText() {
	int length;
	if (childNodes == null || (length = childNodes.getLength()) == 0)
		return "";
	if (length == 1)
		return ((Child)childNodes.item(0)).getText();
	StringBuffer sb = new StringBuffer(128);
	synchronized (childNodes) {
		for (int i = 0;  i < length;  i ++)
			sb.append(((Child)childNodes.item(i)).getText());
	}
	return sb.toString().intern();
}
	/**
	 * Returns <var>true</var> if this Node has any children, or <var>false</var> if this Node has no children at all. 
	 * This method exists both for convenience as well as to allow implementations to be able 
	 * to bypass object allocation, which may be required for implementing getChildNodes().
	 * <p>This method is defined by DOM.
	 * @return          <var>True</var> if any children exist; otherwise, returns <var>false</var>.
	 * @see #getChildNodes
	 * @see #elements
	 * @see #getFirstChild
	 * @see #getLastChild
	 * @see #getChildrenArray
	 */
	public boolean hasChildNodes() {
		return 0 < this.childNodes.getLength();
	}
/**
 * Insert a Child Node into the specified position in this Node's list of children.
 * @param child     The Node being inserted.
 * @param index     0-based index into the list of children.
 * @exception org.w3c.dom.DOMException Thrown if <var>index</var> is not valid.
 * @see #insertBefore
 * @see #insertAfter
 * @see #insertFirst
 * @see #insertLast
 */
public synchronized void insert(Node child, int index)
	throws DOMException
{
	if (child.getParentNode() != null)
		child.getParentNode().removeChild(child);
	//checkChildType(child);
	if (child == this)
		throw new TXDOMException(DOMException.HIERARCHY_REQUEST_ERR,
								 com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Can__t_have_itself_as_child._2"));

	/*TXDocument document = getFactory();
	if (null != document) {
		if (document.isCheckOwnerDocument() && document != child.getOwnerDocument())
			throw new TXDOMException(DOMException.WRONG_DOCUMENT_ERR,
									 "Specified child was created from a different document. The parent is \""+this.getNodeName()+"\", the child is \""+child.getNodeName()+"\".");

		if (document.isCheckNodeLoop()) {
			// Check whether the child is one of ancestors of this.
			Node an = this;
			while (null != (an = an.getParentNode())) {
				if (an == child)
					throw new TXDOMException(DOMException.HIERARCHY_REQUEST_ERR,
											 "Can't have an ancestor as child");
			}
		}
	}*/
	childNodes.insert(index, child);
	((Child)child).setParentNode(this);
	//clearDigest();
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

	public synchronized Node removeChild(Node oldChild) throws DOMException {
		int index = this.childNodes.indexOf(oldChild);
		if (0 > index) {
			throw new TXDOMException(DOMException.NOT_FOUND_ERR,
									 Messages.getString("EmbeddedXMLParser.com.ibm.xml.parser.Parent_#removeChild()__Node_{0}_is_not_found_in_the_child_list", oldChild));
		}
		this.childNodes.remove(index);
		//clearDigest();
		// this.processAfterRemove(oldChild);
		return oldChild;
	}
}