package com.ibm.xml.parser;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
/**
 * Type comment
 */
public class TXAttributeList extends Vector implements NamedNodeMap
{
	Element         parentElement   =   null;
/**
 * TXAttributeList constructor comment.
 */
public TXAttributeList() {
	super();
}
/**
 * Returns the number of Attributes in this AttributeList. 
 * <p>This method is defined by DOM.
 * @return          The number of Attributes in this list.
 */
public int getLength() {                   
	return size();
}
/**
 * Returns an Attribute instance whose name matches the specified <var>name</var>.  
 * <p>This method is defined by DOM.
 * @param name      Name to match against in the AttributeList.
 * @return          The matching Attribute instance, or <var>null</var> if no matches.
 * @see #indexOf
 */
public Node getNamedItem(String name) {
	int index = indexOf(name);
	return index < 0 ? null : (Node)elementAt(index);
}
	/**
	 * Returns the index of the Attribute whose name matches the specified <var>name</var>.
	 * @param name      Name to match against in the AttributeList.
	 * @return          0-based index of matching Attribute, or <var>-1</var> if no matches.
	 */
	public synchronized int indexOf(String name) {
		for (int i = 0;  i < size();  i ++) {
			TXAttribute a = (TXAttribute)elementAt(i);
			if (a.getName().equals(name))  return i;
		}
		return -1;
	}
/**
 * Returns the Attribute at the specified <var>index</var>.
 * If the specified <var>index</var> is greater than or equal to the number of Nodes 
 * in this list, a NoSuchAttributeException exception is thrown. 
 * <p>This method is defined by DOM.
 * @param   index   0-based index which identifies the Attribute to return.
 * @return          The Attribute at the specified index.
 */
public Node item(int index) {          
	if (index < 0 || size() <= index)  return null;
	return (Node)elementAt(index);
}
/**
 * Adds or replaces an Attribute. If the Attribute name already exists in this list, the
 * previous Attribute is replaced, and returned. If no Attribute of the same name 
 * exists <var>null</var> is returned, and the specified <var>attribute</var> is added to the 
 * end of this AttributeList.
 * <p>This method is defined by DOM.
 * @param   attribute   The Attribute to add or replace.
 * @return              If replaced, the Attribute that was replaced; otherwise, <var>null</var>.
 * @see #remove
 */
public Node setNamedItem(Node arg) throws DOMException {
	if (arg.getNodeType() != Node.ATTRIBUTE_NODE)
		throw new TXDOMException(DOMException.HIERARCHY_REQUEST_ERR, com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Not_Attribute_node_1"));
	if (this.parentElement.getOwnerDocument() != arg.getOwnerDocument())
		throw new TXDOMException(DOMException.WRONG_DOCUMENT_ERR, com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Specified_child_was_created_from_a_different_document._2"));
	TXAttribute txAttribute = (TXAttribute)arg;

	Attr replaced = null;
	int index = indexOf(txAttribute.getNodeName());
	if (0 > index) {
		/*
		TXAttribute tail = size() > 0 ? (TXAttribute)elementAt(size()-1) : null;
		txAttribute.setNextSibling(null);
		txAttribute.setPreviousSibling(tail);
		if (null != tail)
			tail.setNextSibling(txAttribute);
		*/
		txAttribute.setParentNode(this.parentElement);
		addElement(txAttribute);
	} else {
		replaced = (Attr)elementAt(index);
		setElementAt(txAttribute, index);
		/*
		txAttribute.setPreviousSibling(replaced.getPreviousSibling());
		txAttribute.setNextSibling(replaced.getNextSibling());
		*/
		txAttribute.setParentNode(this.parentElement);
	}
	/*if (null != this.parentElement)
		((Child)this.parentElement).clearDigest();*/
	return replaced;
}
/**
 * Sets this AttributeList and all its Child Attributes to have the   
 * specified <var>parent</var> Node. 
 * @param   parent      The Parent for this AttributeList and its child Attributes.
 */
public void setParent(Element parent) {
	for (int i = 0;  i < getLength();  i ++) {
		TXAttribute attr = (TXAttribute)item(i);
		attr.setParentNode(parent);
	}
	this.parentElement = parent;
}
}