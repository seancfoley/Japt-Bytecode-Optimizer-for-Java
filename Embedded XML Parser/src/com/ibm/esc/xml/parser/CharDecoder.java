package com.ibm.esc.xml.parser;

import java.util.Hashtable;

/**
 *	Decodes special XML characters like:
 *			&lt;
 *			&amp;
 *			&gt;
 *			&apos;
 *			&quot;
 *			&nbsp;
 */
public class CharDecoder
{
	private Hashtable chars = new Hashtable();
/**
 * Method comment
 */
public CharDecoder()
{
	super();
}
/**
 * Method comment
 */
public String getCharValue(String name)
{
	Object value = chars.get(name);
	if (value == null) return null;
	return (String)value;
}
/**
 * Return a copy of the current character table.
 * @return java.util.Hashtable
 */
public Hashtable getTable()
{
	return (Hashtable)chars.clone();
}
/**
 * Set a CharDecoder with the main HTML characters id
 */
public static CharDecoder htmlCharacters()
{
	CharDecoder decoder = new CharDecoder();
	decoder.setCharValue("amp",		"&");
	decoder.setCharValue("lt",		"<");
	decoder.setCharValue("gt",		">");
	decoder.setCharValue("apos",	"'");
	decoder.setCharValue("quot", 	"\"");
	decoder.setCharValue("nbsp", 	"\u00a0"); // non breaking space
	return decoder;	
}
/**
 * Method comment
 */
public void setCharValue(String name, String value)
{
	chars.put(name,value);
}
}