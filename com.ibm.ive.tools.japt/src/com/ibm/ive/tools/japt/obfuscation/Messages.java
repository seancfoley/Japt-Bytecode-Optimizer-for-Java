package com.ibm.ive.tools.japt.obfuscation;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.MsgHelp;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.ive.tools.japt.JaptMessage.WarningMessage;

/**
 * @author sfoley
 * <p>
 * A centralized location for name compression messages, handy for internationalization (i18n)
 */
class Messages  {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.obfuscation.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	Messages(Component component) {
		FIXED_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.obfuscation.Froze_name_of_class__{0}_1")));
		FIXED_FIELD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.obfuscation.Froze_name_of_field_{0}_2")));
		FIXED_METHOD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.obfuscation.Froze_name_of_method_{0}_3")));
		CLASS_NAME_MAPPED = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.obfuscation.{0}_name_{1}_mapped_to_{2}_4")));
 		FIELD_NAME_MAPPED = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.obfuscation.Field_name_{0}.{1}_mapped_to_{2}_5")));
		METHOD_NAME_MAPPED = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.obfuscation.Method_name_{0}.{1}_mapped_to_{2}_6")));
		FIXED_PACKAGE = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.obfuscation.Froze_package_name_of_class__{0}_8")));
		MISSING_CLASSES = new WarningMessage(component, 
			getString("com.ibm.ive.tools.japt.obfuscation.Missing_classes_may_cause_name_compression_errors_9"));
		COULD_NOT_OPEN_LOG = new ErrorMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.obfuscation.Could_not_open_log_file_{0}_24")));
	}
	
	
	final LogMessage FIXED_CLASS,
		FIXED_FIELD,
		FIXED_METHOD,
		CLASS_NAME_MAPPED,
 		FIELD_NAME_MAPPED,
		METHOD_NAME_MAPPED,
		FIXED_PACKAGE,
		MISSING_CLASSES,
		COULD_NOT_OPEN_LOG;
		
	final String DESCRIPTION = getString("com.ibm.ive.tools.japt.obfuscation.obfuscation_25");
		
	final String EXPAND_PERMISSIONS_LABEL = "expandPermissions",
		CASE_SENSITIVE_LABEL = "caseSensitiveClassNames",
		REUSE_CONSTANT_POOL_LABEL = "reuseStrings",
		BASE_NAME_LABEL = "obfuscatedClsBaseName",
		PACKAGE_BASE_NAME_LABEL = "obfuscatedPackageName",
		OBFUSCATED_LOG_LABEL = "obfuscatedLog",
		NO_RENAME_PACKAGE_LABEL = "preservePackageMembers",
		PREPEND_LABEL = "prepend";
	
	final String EXPAND_PERMISSIONS = getString("com.ibm.ive.tools.japt.obfuscation.allow_changed_permissions_for_increased_access_16"),
		CASE_SENSITIVE = getString("com.ibm.ive.tools.japt.obfuscation.make_new_class_names_case-sensitive_17"),
		REUSE_CONSTANT_POOL = getString("com.ibm.ive.tools.japt.obfuscation.reuse_strings_used_for_other_purposes_20"),
		BASE_NAME = getString("com.ibm.ive.tools.japt.obfuscation.set_given_obfuscated_class_name_21"),
		PACKAGE_BASE_NAME = getString("com.ibm.ive.tools.japt.obfuscation.set_given_obfuscated_package_name_22"),
		OBFUSCATED_LOG = getString("com.ibm.ive.tools.japt.obfuscation.create_named_obfuscation_log_file_23"),
		NO_RENAME_PACKAGE = getString("com.ibm.ive.tools.japt.obfuscation.do_not_change_package_members_26"),
		PREPEND = getString("com.ibm.ive.tools.japt.obfuscation.prepend_to_names_instead_of_changing_names_27");
	
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
