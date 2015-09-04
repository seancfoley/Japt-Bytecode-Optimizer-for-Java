package com.ibm.ive.tools.japt.coldMethod;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.jikesbt.BT_Factory;

/**
 * @author sfoley
 *
 */
public class Messages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.coldMethod.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	Messages(Component component) {
		MIGRATED_METHOD = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.coldMethod.Moved_body_of_method_{0}_to_{1}_5")));
		CREATED_ACCESSOR_METHOD = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.coldMethod.Created_accessor_method_{0}_8")));
		CANNOT_MIGRATE_CLINIT = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.coldMethod.Could_not_move_body_of_static_initializer_in_{0}_{1}_20")));
		CANNOT_MIGRATE_METHOD = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.coldMethod.Could_not_move_body_of_method_{0}__{1}_9")));
		CANNOT_MIGRATE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.coldMethod.Could_not_move_body_of_method_{0}_21")));
		RESTRICTED_ACCESS = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.coldMethod.Cannot_move_body_of_method_{0}_due_to_restricted_access_to_{1}_16")));
		String newLine = BT_Factory.endl();
		SUMMARY = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.coldMethod.Summary_6") + newLine + 
				getString("com.ibm.ive.tools.japt.coldMethod.Moved_body_of_{0}_methods_to_{1}_new_classes_7")));
	}
	
	final LogMessage 
		SUMMARY,
		MIGRATED_METHOD,
		CREATED_ACCESSOR_METHOD,
		CANNOT_MIGRATE,
		CANNOT_MIGRATE_METHOD,
		RESTRICTED_ACCESS,
		CANNOT_MIGRATE_CLINIT;
	
		
	final String 
		EXPAND_PERMISSIONS_LABEL = "expandPermissions",
		COLD_LABEL = "coldMethod",
		WARM_LABEL = "warmMethod",
		ALLOW_ACCESSORS_LABEL = "allowAccessors",
		NOT_WARM_IS_COLD_LABEL = "notWarmIsCold",
		NOT_LOADED = getString("com.ibm.ive.tools.japt.coldMethod.method_not_loaded_10"),
		METHOD_TOO_SMALL = getString("com.ibm.ive.tools.japt.coldMethod.method_code_too_small_11"),
		CLASS_TOO_SMALL = getString("com.ibm.ive.tools.japt.coldMethod.class_code_too_small_12"),
		EXTERNAL_CLASS = getString("com.ibm.ive.tools.japt.coldMethod.external_class_15"),
		COLD_METHOD_CPE = getString("com.ibm.ive.tools.japt.coldMethod.Migrated_method_class_path_entry_19"),
		CANNOT_LOCATE_CONSTRUCTOR = getString("com.ibm.ive.tools.japt.coldMethod.Cannot_locate_chained_constructor_17"),
		STACK_NOT_EMPTY = getString("com.ibm.ive.tools.japt.coldMethod.Stack_not_empty_at_chained_constructor_18");
	
	final String 
		EXPAND_PERMISSIONS = getString("com.ibm.ive.tools.japt.coldMethod.allow_changed_permissions_for_increased_access_2"),
		COLD = getString("com.ibm.ive.tools.japt.coldMethod.move_body_of_named_method_3"),
		WARM = getString("com.ibm.ive.tools.japt.coldMethod.do_not_move_body_of_named_method_4"),
		NOT_WARM_IS_COLD = getString("com.ibm.ive.tools.japt.coldMethod.methods_in_same_classes_as_warm_methods_are_cold_13"),
		ALLOW_ACCESSORS = getString("com.ibm.ive.tools.japt.coldMethod.allow_the_creation_of_accessor_methods_14");

	final String DESCRIPTION = getString("com.ibm.ive.tools.japt.coldMethod.cold_method_1");
	
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
