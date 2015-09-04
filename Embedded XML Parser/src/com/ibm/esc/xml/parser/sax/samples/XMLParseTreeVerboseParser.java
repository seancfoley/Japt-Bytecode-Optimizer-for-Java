package com.ibm.esc.xml.parser.sax.samples;

/**
 * Type comment
 */
public class XMLParseTreeVerboseParser extends com.ibm.esc.xml.parser.sax.parsetree.XMLParseTreeParser {
/**
 * XMLParseTreeVerboseParser constructor comment.
 */
public XMLParseTreeVerboseParser() {
	super();
}
/**
 * Return a String describing the parsing error.
 * This method can be overwrite by the subclasses
 * to provide a better decription.
 * By default it is the error number.
 */
protected String errorMsg(int errorID, String parameter)
{
	String errorMsg = "";
	switch (errorID)
	{
		case WARNING_END_TAG_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.End_tag_expected_2");
			break;
		case WARNING_UNKNOWN_CHARACTER_DEF:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Unknown_character_3");
			break;
		case ERROR_ATTRIBUT_VALUE_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Attribut_Value_expected_4");
			break;
		case ERROR_ATTRIBUT_NAME_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Attribut_name_expected_5");
			break;
		case ERROR_END_OF_TAG_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.End_of_tag_expected_6");
			break;
		case ERROR_EQUAL_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.______expected_7");
			break;
		case ERROR_GT_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.__>___expected_8");
			break;
		case ERROR_TAG_COMMENT_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.XML_comment_expected_9");
			break;
		case ERROR_TAG_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Tag_expected_10");
			break;
		case ERROR_TAG_NAME_EXPECTED:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Tag_name_expected_11");
			break;
		case ERROR_UNMANAGED_STATE:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Unknown_state_12");
			break;
		case ERROR_WRONG_TAG_HEADER:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Wrong_tag_header_13");
			break;
		case ERROR_WRONG_PI:
			errorMsg = com.ibm.esc.xml.parser.Messages.getString("EmbeddedXMLParser.Wrong_Processing_Instruction_14");
			break;
	}
			
	if (parameter != null)
	{
		errorMsg = errorMsg + ": " + parameter;
	}
	return errorMsg;
}
}