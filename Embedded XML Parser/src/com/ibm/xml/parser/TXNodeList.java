package com.ibm.xml.parser;

import org.w3c.dom.*;
import java.util.*;
import java.io.*;

/**
 * The TXNodeList class is used to represent collections of Node 
 * objects which need to be accessed by name, ordinal, etc. 
 * For example, the Parent class uses TXNodeList to organize and access 
 * its Child Nodes.
 *
 * @version Revision: 05 1.5 src/com/ibm/xml/parser/TXNodeList.java, parser, xml4j2, xml4j2_0_0 
 * @author TAMURA Kent &lt;kent@trl.ibm.co.jp&gt;
 * @see com.ibm.xml.parser.Parent
 */
public class TXNodeList implements NodeList
{
	private Vector nodes   =   new Vector();
	static class EmptyNodeListImpl implements NodeList {
		public Node item(int index) {
			throw new ArrayIndexOutOfBoundsException();
		}
		public int getLength() {
			return 0;
		}
	}
	static EmptyNodeListImpl emptyNodeList = new EmptyNodeListImpl();
	static class VectorNodeList implements NodeList {
		Vector data;

		VectorNodeList(Vector v) {
			this.data = v;
		}

		public Node item(int index) {
			return (Node)this.data.elementAt(index);
		}

		public int getLength() {
			return this.data.size();
		}
	}
/**
 * TXNodeList constructor comment.
 */
public TXNodeList() {
	super();
}
/**
 * Inserts a new Node at the end of this NodeList.
 * @param   newNode     The new Node to insert at the end.
 * @see #insert
 * @see #replace     
 * @see #remove
 */
public void append(Node newNode) {
	insert(this.nodes.size(), newNode);
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
	Enumeration enumeration = nodes.elements();
	Object element;
	while (enumeration.hasMoreElements())
	{
		element = enumeration.nextElement();
		if (element instanceof TXElement)
		{
			((TXElement)element).getElementsByTagName(qName, nodeList);
		}
	}
	return nodeList;
}
/**
 * Returns the number of Nodes in this TXNodeList.
 * <p>This method is defined by DOM.
 * @return      The number of Nodes in this TXNodeList.
 */
public int getLength() {
	return this.nodes.size();
}
	/**
	 * Searches for the first occurence of the specified Node, testing 
	 * for equality using the Vector <code>equals</code> method. 
	 * @param   node   Node to search for in this list.
	 * @return  The 0-based index of the first occurrence of a matching Node in this list, or -1 if no matching Node.
	 * @see     java.lang.Object#equals(java.lang.Object)
	 * @see     #item
	 */
	public int indexOf(Node node) {
		return this.nodes.indexOf(node);
	}
	/**
	 * Inserts a new Node at the specified index.
	 * @param   index       0-based index of where to insert <var>newNode</var>.
	 * @param   newNode     The new Node to insert at the specified index.
	 * @see #replace     
	 * @see #remove
	 * @see #addElement
	 * @exception  org.w3c.dom.DOMException  Thrown if an invalid index is specified.
	 */
	public void insert(int index, Node newNode) throws DOMException {
		if (index < 0 || index > this.nodes.size()) {
			throw new TXDOMException(DOMException.INDEX_SIZE_ERR,
			com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.com.ibm.xml.parser.TXNodeList_#insert()__Wrong_index__{0}", index));
		}
		Child prev = 0 < index ? (Child)this.nodes.elementAt(index-1) : null;
		Child next = this.nodes.size() > index ? (Child)this.nodes.elementAt(index) : null;
		this.nodes.insertElementAt(newNode, index);
		Child child = (Child)newNode;
		child.setPreviousSibling(prev);
		if (null != prev)  prev.setNextSibling(child);
		child.setNextSibling(next);
		if (null != next)  next.setPreviousSibling(child);
	}
/**
 * Returns the Node at the specified index.
 * <p>This method is defined by DOM.
 * @param      index   0-based index into this list.
 * @return     The Node at the specified index.
 * @see #indexOf
 */
public Node item(int index) {
	return (Node)this.nodes.elementAt(index);
}
/**
 * Removes a Node at the specified index.
 * @param   index       0-based index of where to remove a Node.
 * @return              The Node removed at the specified index.
 * @see #replace     
 * @see #insert      
 * @exception  org.w3c.dom.DOMException  Thrown if an invalid index is specified.
 */
public Node remove(int index) throws DOMException {
	if (index < 0 || this.nodes.size() <= index) {
		throw new TXDOMException(DOMException.INDEX_SIZE_ERR,
			com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.com.ibm.xml.parser.TXNodeList_#remove()__Wrong_index__{0}", index));
	}
	Child old = (Child)this.nodes.elementAt(index);
	Child prev = (Child)old.getPreviousSibling();
	Child next = (Child)old.getNextSibling();
	this.nodes.removeElementAt(index);
	old.setParentNode(null);
	old.setPreviousSibling(null);
	old.setNextSibling(null);
	if (null != prev)  prev.setNextSibling(next);
	if (null != next)  next.setPreviousSibling(prev);
	return old;
}
}