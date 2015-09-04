package com.ibm.esc.extension.pk.dom;

import org.w3c.dom.*;
/**
 * Add several static methods we would like
 * to have on org.w3c.dom.Node
 */
public class NodeExtension {
/**
 */
public static Node getChildNamed(Node node, String name)
{
	NodeList children = node.getChildNodes();
	Node element;
	for (int i=0; i<children.getLength(); i++)
	{
		element = children.item(i);
		if (element.getNodeName().equals(name))
		{
			return element;
		}
	}
	return null;
}
/**
 */
public static String getHTMLTitle(Node document)
{
	Node headNode = getChildNamed(document, "HEAD");
	if (headNode == null) return null;
	
	Node titleNode = getChildNamed(headNode, "TITLE");
	if (titleNode == null) return null;

	Node textNode = getChildNamed(titleNode, "#text");
	if (textNode == null) return null;

	return textNode.getNodeValue();
}
/**
 * Search a node which has the name @name and the value @value.
 */
public static Node searchNode(Node node, String name, String value)
{
	if ((node.getNodeName().equals(name)) &&
		(node.getNodeValue() != null) &&
		(node.getNodeValue().equals(value)))
	{
		return node;
	}
	NodeList children = node.getChildNodes();
	Node child, found;
	for (int i=0; i<children.getLength(); i++)
	{
		child = children.item(i);
		found = searchNode(child, name, value);
		if (found != null)
		{
			return found;
		}
	}
	return null;
}
}