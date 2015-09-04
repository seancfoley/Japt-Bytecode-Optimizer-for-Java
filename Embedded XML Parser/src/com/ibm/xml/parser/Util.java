package com.ibm.xml.parser;

import com.ibm.esc.xml.core.*;
/**
 * Util is a collection of XML4J utility routines which check the conformance of various 
 * XML-defined values (XML name, language ID, encoding ID), and which provide services for 
 * converting strings to XML format. 
 *
 * @version Revision: 09 1.5 src/com/ibm/xml/parser/Util.java, parser, xml4j2, xml4j2_0_0 
 * @author TAMURA Kent &lt;kent@trl.ibm.co.jp&gt;
 */
public class Util {
	
	/**
	 * Returns whether the specified <var>string</var> consists of only XML whitespace.
	 * Refer to <A href="http://www.w3.org/TR/1998/REC-xml-19980210#NT-S">
	 * the definition of <CODE>S</CODE></A> for details.
	 * @param   string  String to be checked if it constains all XML whitespace.
	 * @return          =true if name is all XML whitespace; otherwise =false.
	 */
	public static boolean checkAllSpace(String string) {
		for (int s = 0;  s < string.length();  s ++) {
			if (!XMLChar.isSpace(string.charAt(s)))  return false;
		}
		return true;
	}
	/**
	 * Returns whether the specified <var>name</var> conforms to <CODE>Name</CODE> in XML 1.0.
	 * Refer to <A href="http://www.w3.org/TR/1998/REC-xml-19980210#NT-Name"> 
	 * the definition of <CODE>Name</CODE></A> for details.
	 * @param   name    Name to be checked as a valid XML Name.
	 * @return          =true if name complies with XML spec; otherwise =false.
	 */
	public static boolean checkName(String name) {
		if (1 > name.length())  return false;
		char ch = name.charAt(0);
		if (!(XMLChar.isLetter(ch) || '_' == ch || ':' == ch))  return false;
		for (int i = 1;  i < name.length();  i ++) {
			if (!XMLChar.isNameChar(name.charAt(i)))  return false;
		}
		return true;
	}
}