package com.ibm.ive.tools.japt.instrument;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.WarningMessage;

/**
 * @author sfoley
 *
 */
public class Messages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.instrument.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	Messages(Component component) {
		CANNOT_INSTRUMENT_CLASS = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.instrument.Cannot_instrument_array__interface__or_external_class_{0}_6")));
		CANNOT_INSTRUMENT_METHOD_CLASS = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.instrument.Cannot_instrument_method_{0}_of_array__interface__or_external_class_{1}_18")));
		NOT_FOUND_RUNTIME_OBSERVER = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.instrument.Runtime_observer_class_{0}_not_found_19")));
		INVALID_RUNTIME_OBSERVER = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.instrument.Class_{0}_not_an_instance_of_{1}_7")));
		NO_CODE_TO_INSTRUMENT = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.instrument.No_code_to_instrument_for_method_{0}_15")));
		CANNOT_INSTRUMENT = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.instrument.Cannot_instrument_{0}_{1}_16")));
		NO_CONSTRUCTOR_INVOCATION = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.instrument.No_constructor_invocation_in_{0}_17")));
	}
	
	
	final LogMessage 
		CANNOT_INSTRUMENT_CLASS,
		CANNOT_INSTRUMENT_METHOD_CLASS,
		INVALID_RUNTIME_OBSERVER,
		NOT_FOUND_RUNTIME_OBSERVER,
		NO_CODE_TO_INSTRUMENT,
		CANNOT_INSTRUMENT,
		NO_CONSTRUCTOR_INVOCATION;
	
	final String 
		INSTRUMENT_CLASS_LABEL = "instrumentClass",
		INSTRUMENT_ACCESSIBLE_CLASS_LABEL = "instrumentAccClass",
		INSTRUMENT_METHOD_LABEL = "instrumentMethod",
		INSTRUMENT_BY_OBJECT_LABEL = "instrumentByObject",
		RUNTIME_OBSERVER_CLASS_LABEL = "observerClass";
	
	//com.ibm.ive.tools.japt.load.include_public/protected_members_of_named_class(es)_58
	final String 
		INSTRUMENT_CLASS = getString("com.ibm.ive.tools.japt.instrument.instrument_methods_in_named_classes_2"),
		INSTRUMENT_ACCESSIBLE_CLASS = getString("com.ibm.ive.tools.japt.instrument.instrument_public/protected_methods_in_named_classes_20"),
		INSTRUMENT_METHOD = getString("com.ibm.ive.tools.japt.instrument.instrument_named_methods_3"),
		INSTRUMENT_BY_OBJECT = getString("com.ibm.ive.tools.japt.instrument.instrument_individual_objects_4"),
		RUNTIME_OBSERVER_CLASS = getString("com.ibm.ive.tools.japt.instrument.instance_of_{0}_5"),
		CLASS_NOT_FOUND = getString("com.ibm.ive.tools.japt.instrument.Class_not_found_8"),
		COULD_NOT_INSTANTIATE = getString("com.ibm.ive.tools.japt.instrument.Count_not_instantiate_10"),
		INVALID_TYPE = getString("com.ibm.ive.tools.japt.instrument.Invalid_type__not_instance_of_11"),
		ERROR_LOADING = getString("com.ibm.ive.tools.japt.instrument.Error_loading__linking_or_initializing_9"),
		CREATED_OBJECT_OBSERVER = getString("com.ibm.ive.tools.japt.instrument.Created_object_observer_12"),
		CREATED_CLASS_OBSERVER = getString("com.ibm.ive.tools.japt.instrument.Created_class_observer_13"),
		ENTERED = getString("com.ibm.ive.tools.japt.instrument.entered_14");
	
	final String DESCRIPTION = getString("com.ibm.ive.tools.japt.instrument.instrument_1");
	
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
