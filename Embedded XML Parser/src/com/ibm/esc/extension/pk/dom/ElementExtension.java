package com.ibm.esc.extension.pk.dom;

import org.w3c.dom.*;
import com.ibm.xml.parser.*;
/**
 * Add several static methods we would like
 * to have on org.w3c.dom.Element
 */
public class ElementExtension {
/**
 * Return the 'int' value of the attribute named @attName.
 * If this attribute is absent, return the @defaultValue
 */
public static int getIntAttribute(Element tag, String attName, int defaultValue)
{
	String stringValue = tag.getAttribute(attName);
	int value;
	if (stringValue.length() == 0)
	{
		return defaultValue;
	} else
	{
		return Integer.parseInt(stringValue);
	}
}
/**
 * Return the 'String' value of the attribute named @attName.
 * If this attribute is absent, return the @defaultValue
 */
public static String getStringAttribute(Element tag, String attName, String defaultValue)
{
	// In this case an empty attribute is considerated as "no" attribute.
	return getStringAttribute(tag, attName, defaultValue, defaultValue);
}
/**
 * Return the 'String' value of the attribute named @attName.
 * If this attribute is absent, return the @defaultValue
 */
public static String getStringAttribute(Element tag, String attName, String defaultValue, String defaultEmptyValue)
{
	Attr attribute = tag.getAttributeNode(attName);
	if (attribute == null) return defaultValue;
	if (((TXAttribute)attribute).getIsEmptyValueAttibute()) return defaultEmptyValue;
	return attribute.getValue();
}
}