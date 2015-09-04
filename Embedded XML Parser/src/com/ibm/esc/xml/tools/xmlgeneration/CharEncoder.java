package com.ibm.esc.xml.tools.xmlgeneration;

import java.util.*;

import com.ibm.esc.xml.parser.*;

/**
 *	Encodes special XML characters like:
 *			&lt;
 *			&amp;
 *			&gt;
 *			&apos;
 *			&quot;
 *			&nbsp;
 */
public class CharEncoder
{
	private Hashtable chars = new Hashtable();
/**
 * CharEncoder constructor comment.
 */
public CharEncoder() {
	super();
}
/**
 * CharEncoder constructor comment.
 */
public CharEncoder(CharDecoder charDecoder)
{
	this();
	Hashtable table = charDecoder.getTable();
	Enumeration keys = table.keys();
	Object key, value;
	while (keys.hasMoreElements())
	{
		key = keys.nextElement();
		value = table.get(key);
		chars.put(value, key);
	}		
}
/**
 * Return the associated encoding name
 */
public String getCharKey(char c)
{
	Object key = chars.get(String.valueOf(c));
	if (key == null) return null;
	return (String)key;
}
/**
 * Set a CharEncoder with the main HTML characters id
 */
public static CharEncoder htmlCharacters()
{
	return new CharEncoder(CharDecoder.htmlCharacters());
}
}