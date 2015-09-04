package com.ibm.xml.parser;

import org.w3c.dom.*;
/**
 * Type comment
 */
public class TXCDATASection extends TXText implements CDATASection {
/**
 * Constructor.
 * @param data      The actual content of the CDATASection Node.
 */
public TXCDATASection(String data) {
	super(data);
}
}