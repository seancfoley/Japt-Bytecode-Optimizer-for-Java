package com.ibm.ive.tools.japt.assembler;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;

/**
 * @author sfoley
 *
 */
public class AssemblerMessages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.assembler.AssemblerExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	AssemblerMessages(Component component) {
		ERROR_READING_FILE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Error_reading_file_{0}__{1}_4")));
		ERROR_READING = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Error_reading_{0}_5")));
		ERROR_SOURCE_LINE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Error_at_line_{0}_in_{1}_6")));
		ERROR_SOURCE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Error_reading_{0}_7")));
		DUP_CLASS = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Duplicate_class__{0}_14")));
		CLASS_ERROR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Error_loading_{0}__{1}_15")));
		VERSION_ERROR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.assembler.Invalid_class_version__{0}_19")));
	}
	
	final LogMessage 
		ERROR_READING_FILE,
		ERROR_READING,
		ERROR_SOURCE_LINE,
		ERROR_SOURCE,
		DUP_CLASS,
		CLASS_ERROR,
		VERSION_ERROR;
		
	final String 
		RESET_LABEL = "resetClassPath",
		CLASS_VERSION_LABEL = "classVersion",
		LOAD_LABEL = "loadAssemblyFile",
		INTERNAL_CLASS_PATH_LABEL = "acp",
		FILE_EXTENSION_LABEL = "jarExtension";
	
	final String 
		RESET = getString("com.ibm.ive.tools.japt.assembler.remove_existing_class_search_path_entries_8"),
		LOAD = getString("com.ibm.ive.tools.japt.assembler.load_named_assembly_file_or_archive_11"),
		INTERNAL_CLASS_PATH = getString("com.ibm.ive.tools.japt.assembler.append_named_jar_zip_dir_to_internal_assembly_file_class_search_path_12"),
		FILE_EXTENSION = getString("com.ibm.ive.tools.japt.assembler.handle_files_with_named_extension_as_jar_files_16"),
		CLASS_VERSION = getString("com.ibm.ive.tools.japt.assembler.assign_given_class_version_18");
	
	String DESCRIPTION = getString("com.ibm.ive.tools.japt.assembler.assemble_13");
	
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
