package com.ibm.ive.tools.japt.assembler;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.ProgressMessage;

/**
 * @author sfoley
 *
 */
public class DisassemblerMessages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.assembler.DisassemblerExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	DisassemblerMessages(Component component) {
		CREATED_JAR = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Created_{0}_1")));	
		ERROR_WRITING_JAR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Error_writing_to_jar_{0}__{1}_2")));
		ERROR_WRITING = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Error_writing_{0}_3")));
	}
	
	final LogMessage 
		CREATED_JAR,
		ERROR_WRITING_JAR,
		ERROR_WRITING;
		
	final String 
		DISASSEMBLE_LABEL = "disassemble",
		TARGET_LABEL = "output";
	
	final String 
		DISASSEMBLE = getString("com.ibm.ive.tools.japt.assembler.disassemble_named_classes_9"),
		TARGET = getString("com.ibm.ive.tools.japt.assembler.name_of_output_10");
	
	String DESCRIPTION = getString("com.ibm.ive.tools.japt.assembler.disassemble_17");
	
	/**
	 * @param 		key	String
	 * 					the key to look up
	 * @return		String
	 * 					the message for that key in the system message bundle
	 */
	public String getString(String key) {
		if(bundle != null) {
			return bundle.getString(key);
		}
		return '!' + key + '!';
	}
}
