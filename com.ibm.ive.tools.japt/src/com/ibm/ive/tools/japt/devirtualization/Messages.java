package com.ibm.ive.tools.japt.devirtualization;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.MsgHelp;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.ive.tools.japt.JaptMessage.ProgressMessage;

/**
 * @author sfoley
 * <p>
 * A centralized location for name compression messages, handy for internationalization (i18n)
 */
class Messages  {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.devirtualization.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	Messages(Component component) {
		DEVIRTUALIZE_SUMMARY = new InfoMessage(component, 
				new FormattedString(getString("com.ibm.ive.tools.japt.devirtualization.Devirtualized_{0}_callsites_into_static_invokes_and_{1}_callsites_into_special_invokes_of_{2}_total_callsites_5")));
		MAKE_FINAL_SUMMARY = new InfoMessage(component, 
				new FormattedString(getString("com.ibm.ive.tools.japt.devirtualization.Made_{0}_of_{1}_non-final_non-abstract_classes_final_and_{2}_of_{3}_non-static_non-constructor_non-abstract_methods_final_6")));
		MADE_FINAL_CLASS = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.devirtualization.Made_class_{0}_final_7")));
		MADE_FINAL_METHOD = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.devirtualization.Made_method_{0}_final_8")));
		DEVIRTUALIZED_STATIC = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.devirtualization.Inside_{1_devirtualized_invocation_of_{0}_to_static_invocation_of_{2}_9")));
		DEVIRTUALIZED_SPECIAL = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.devirtualization.Inside_{1}_devirtualized_invocation_of_{0}_to_special_invocation_10")));
		REMOVED_UNREFERENCED_METHOD = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.devirtualization.Removed_unreferenced_method_{0}_11")));
		STARTED_STATIC_DEVIRTUALIZING = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.devirtualization.Devirtualizing_non-static_method_invocations_into_static_invocations_12"));
		STARTED_SPECIAL_DEVIRTUALIZING = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.devirtualization.Devirtualizing_virtual_and_interface_method_invocations_into_special_invocations_13"));
		STARTED_MAKING_METHODS_FINAL = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.devirtualization.Making_methods_not_overridden_in_a_subclass_final_14"));
		STARTED_MAKING_CLASSES_FINAL = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.devirtualization.Making_non-extended_classes_final_15"));
		REMOVING_UNREFERENCED_METHODS = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.devirtualization.Removing_nreferenced_devirtualized_methods_16"));
	}
	
	
	final LogMessage DEVIRTUALIZE_SUMMARY,
		MAKE_FINAL_SUMMARY,
		MADE_FINAL_CLASS,
		MADE_FINAL_METHOD,
		REMOVED_UNREFERENCED_METHOD,
		DEVIRTUALIZED_STATIC,
		DEVIRTUALIZED_SPECIAL,
		STARTED_STATIC_DEVIRTUALIZING,
		STARTED_SPECIAL_DEVIRTUALIZING,
		STARTED_MAKING_METHODS_FINAL,
		STARTED_MAKING_CLASSES_FINAL,
		REMOVING_UNREFERENCED_METHODS;
	
	final String DESCRIPTION = "devirtualization";
		
	final String DEVIRTUALIZE_STATIC_LABEL = "devirtualizeToStatic",
		DEVIRTUALIZE_SPECIAL_LABEL = "devirtualizeToSpecial",
		ASSUME_UNKNOWN_LABEL = "assumeUnknownVirtuals",
		MAKE_FINAL_LABEL = "makeFinal";
	
	
	final String DEVIRTUALIZE_STATIC = getString("com.ibm.ive.tools.japt.devirtualization.devirtualize_static_1"),
		DEVIRTUALIZE_SPECIAL = getString("com.ibm.ive.tools.japt.devirtualization.devirtualize_special_2"),
		ASSUME_UNKNOWN = getString("com.ibm.ive.tools.japt.devirtualization.assume_virtual_method_calls_have_unknown_targets_3"),
		MAKE_FINAL = getString("com.ibm.ive.tools.japt.devirtualization.make_non-static_methods_that_are_not_overridden_final_4");

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
