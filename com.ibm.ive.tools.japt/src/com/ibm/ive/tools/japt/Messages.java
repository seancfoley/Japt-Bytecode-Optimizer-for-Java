package com.ibm.ive.tools.japt;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.ive.tools.japt.JaptMessage.ProgressMessage;
import com.ibm.ive.tools.japt.JaptMessage.StatusMessage;
import com.ibm.ive.tools.japt.JaptMessage.WarningMessage;

/**
 * @author sfoley
 *
 */
public class Messages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.ExternalMessages"; //$NON-NLS-1$
	private ResourceBundle bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	
	public Messages(Component component) {
		INVALID_IDENTIFIER = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Invalid_identifier__{0}_1")));
		INVALID_IDENTIFIER_FROM = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Invalid_identifier__{0}_from_{1}_37")));
		COULD_NOT_FIND_CLASS = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Could_not_find_class_{0}_2")));
		COULD_NOT_LOAD_CLASS = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Could_not_load_class_{0}_from_{1}_3")));
		COULD_NOT_LOAD_ATTRIBUTE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Could_not_load_attribute_{0}_in_{1}__{2}_56")));
		COULD_NOT_WRITE_ATTRIBUTE = new ErrorMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.Could_not_write_attribute_{0}_in_{1}__{2}_57")));
		EXECUTING_EXTENSION = new StatusMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Executing_extension__{0}_5")));
		COMPLETED_EXECUTING_EXTENSION = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Completed_executing_extension___{0}___in_{1}_6")));
		ERROR_EXECUTING_EXTENSION = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Error_executing_extension__{0}__{1}_8")));
		ERROR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Error__{0}_56")));
		INCLUDED_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Included_{0}_{1}_9"))); 
		INCLUDED_METHOD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Included_method_{0}_10"))); 
		INCLUDED_FIELD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Included_field_{0}_11")));
		CREATED_INTERNAL_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Created_internal_{0}_{1}_inside_class_path_entry__{2}_41")));
		LOADED_INTERNAL_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Loaded_internal_{0}_{1}_from_{2}_13")));
		LOADED_EXTERNAL_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Loaded_external_{0}_{1}_from_{2}_14")));
		DEREFERENCING_CLASSES = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Dereferencing_{0}_classes_15")));
		DEREFERENCED_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Dereferenced_{0}_{1}_16")));
		SECONDS = new Message(new FormattedString(
			getString("com.ibm.ive.tools.japt.{0}.{1}_seconds_17"))); 
		MINUTE_SECONDS = new Message(new FormattedString(
			getString("com.ibm.ive.tools.japt.{0}_minute_and_{1}.{2}_seconds_18"))); 
		MINUTES_SECONDS = new Message(new FormattedString(
			getString("com.ibm.ive.tools.japt.{0}_minutes_and_{1}.{2}_seconds_19")));
		COULD_NOT_LOAD_RESOURCE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Could_not_load_resource_{0}_from_{1}_20")));
		LOADED_RESOURCE = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Loaded_resource_{0}_from_{1}_21")));
		OPTIMIZING = new StatusMessage(component, getString("com.ibm.ive.tools.japt.Optimizing_methods_55"));
		VERIFYING = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.Verifying_classes_23"));
		VERIFICATION_FAILURE = new ErrorMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.{0}_verification_error_24")));
		VERIFICATION_FAILURE_MSG = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Method_{0}_verification_error__{1}_22")));
		VERIFY_FAILURE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Invalid_class_file_format_for_class_{0}_in_entry_{1}__{2}_{3}_46")));
		UNRESOLVED_METHOD = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Could_not_resolve_method_{0}_from_{1}_26")));
		UNRESOLVED_FIELD = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Could_not_resolve_field_{0}_from_{1}_27")));
		INDETERMINATE_CLASS_FORNAME = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.indeterminate_call_to_Class_forName_detected_inside_{0}_28")));
		CLASS_OBJECT_ACCESS = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.java_lang_Class_object_access_detected_inside_{0}_29")));
		COMPLETED_EXECUTION_IN = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Completed_in_{0}_30")));
		NO_MATCH_FROM = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.No_matches_found_for_{0}_from_{1}_38")));
		NO_MATCH = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.No_matches_found_for_{0}_39")));
		READ_FAILURE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Could_not_read_class_{0}_from_file_{1}__{2}_47")));
		CLASS_PATH_PROBLEM = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Class_path_error_for_{0}__{1}_51")));
		INVALID_SUB_ERROR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.invalid_subroutine_in_{0}__{1}_54")));
		CODE_IRREGULARITY = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Malformed_code__{0}_64")));
		WARNING = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Warning__{0}_57")));
		EXCEPTION_CLOSING  = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Exception_closing__{0}_58")));
		EXCESSIVE_VALUE = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.Value_of_{0}_for_{1}_of_{2}_in_class_file_of_{3}_is_larger_than_required_value_of_{4}_59")));
		CHANGED_ACCESS_PERMISSION = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.Changed_access_permission_of_{0}_from_{1}_to_{2}_for_access_from_{3}_60"))); 
		CHANGED_FINAL_ACCESS = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.Removed_final_status_of_{0}_for_access_from_{1}_61"))); 
		USED_ACCESSOR = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.Used_accessor_method_{0}_for_access_to_{1}_from_{2}_62")));
		INLINED_METHOD_JSRS = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Inlined_jsrs_in_method_{0}_63"))); 
	}
	
	final Message 
		SECONDS,
		MINUTE_SECONDS,
		MINUTES_SECONDS;
	
	final LogMessage 
		CHANGED_ACCESS_PERMISSION,
		CHANGED_FINAL_ACCESS,
		USED_ACCESSOR,
		INVALID_SUB_ERROR,
		CLASS_PATH_PROBLEM,
		INVALID_IDENTIFIER,
		INVALID_IDENTIFIER_FROM,
		COULD_NOT_WRITE_ATTRIBUTE,
		COULD_NOT_LOAD_CLASS,
		COULD_NOT_LOAD_RESOURCE,
		CREATED_INTERNAL_CLASS,
		LOADED_INTERNAL_CLASS,
		LOADED_RESOURCE,
		DEREFERENCING_CLASSES,
		LOADED_EXTERNAL_CLASS,
		INCLUDED_CLASS,
		INCLUDED_FIELD,
		INCLUDED_METHOD,
		EXECUTING_EXTENSION,
		COMPLETED_EXECUTING_EXTENSION,
		ERROR_EXECUTING_EXTENSION,
		ERROR,
		EXCEPTION_CLOSING,
		WARNING,
		OPTIMIZING,
		VERIFYING,
		VERIFICATION_FAILURE,
		VERIFICATION_FAILURE_MSG,
		VERIFY_FAILURE,
		READ_FAILURE,
		INDETERMINATE_CLASS_FORNAME,
		CLASS_OBJECT_ACCESS,
		COMPLETED_EXECUTION_IN,
		INLINED_METHOD_JSRS;
	
	public final LogMessage 
		DEREFERENCED_CLASS;
	
	public final JaptMessage 
		CODE_IRREGULARITY,
		NO_MATCH_FROM,
		NO_MATCH,
		UNRESOLVED_METHOD,
		UNRESOLVED_FIELD,
		COULD_NOT_FIND_CLASS,
		EXCESSIVE_VALUE;
	
	public final ErrorMessage 
		COULD_NOT_LOAD_ATTRIBUTE;
	
	public final String DESCRIPTOR = getString("com.ibm.ive.tools.japt.japt_42");
	
	final String NO_CLASS_PATH_ENTRY = getString("com.ibm.ive.tools.japt.class_path_entry_inexistent_52");
	
	
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
