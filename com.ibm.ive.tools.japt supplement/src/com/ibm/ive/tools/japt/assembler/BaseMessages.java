package com.ibm.ive.tools.japt.assembler;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;

/**
 * @author sfoley
 *
 */
public class BaseMessages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.assembler.BaseExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	BaseMessages(Component component) {}
	
		
	final String 
		USE_EXTENSION_LABEL = "fileExtension";
	
	final String 
		USE_EXTENSION = getString("com.ibm.ive.tools.japt.assembler.use_named_file_extension_1");
	
	
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
