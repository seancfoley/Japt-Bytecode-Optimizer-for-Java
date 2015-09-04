package com.ibm.ive.tools.japt.remapping;

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
 *
 */
public class Messages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.remapping.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	Messages(Component component) {
		NO_MATCH_FOUND = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.no_match_found_for_remapping_candidate(s)_{0}_1")));
		NO_TARGET_FOUND = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.no_match_found_for_remapping_target_{0}_2")));
		TARGET_NOT_UNIQUE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.identifier_{0}_matches_multiple_targets_3")));
		TARGET_NOT_INTERNAL = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.identifier_target_{0}_matches_external_class_12")));
		TARGET_AMBIGUOUS = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.identifier_{0}_ambiguous_10")));
		TARGET_INVALID = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.identifier_{0}_invalid_11")));
		NO_CALLSITES_TO_MAP = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.no_call_sites_to_map_for_method_{0}_4")));
		NO_INSTANTIATIONS_TO_MAP = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.no_instantiations_to_map_for_class_{0}_16")));
		NO_ACCESSORS_TO_MAP = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.no_accesses_to_map_for_field_{0}_7")));
		MAPPED_CALLSITE = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.mapped_call_site_of_{0}_in_{1}_to_{2}_5")));
		NOT_MAPPED_CALLSITE = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.unable_to_map_call_site_of_{0}_in_{1}_to_{2}__{3}_6")));
		MAPPED_INSTANTIATION = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.mapped_instantiation_of_{0}_in_{1}_to_{2}_14")));
		NOT_MAPPED_INSTANTIATION = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.unable_to_map_instantiation_of_{0}_in_{1}_to_{2}__{3}_15")));
		MAPPED_ACCESSOR = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.mapped_accessor_of_{0}_in_{1}_to_{2}_8")));
		NOT_MAPPED_ACCESSOR = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.unable_to_map_accessor_of_{0}_in_{1}_to_{2}__{3}_9")));
		INCOMPATIBLE_TYPES = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.incompatible_types_for_specifier_{0}_13")));
		INCOMPATIBLE_CLASSES = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.cannot_map_both_interface_and_class_methods_for_specifier_{0}_40")));
		TARGET_WRONG_TYPE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.remapping.target_{0}_is_not_correct_type__{1}_41")));
	}
	
	
	final LogMessage NO_MATCH_FOUND,
		NO_TARGET_FOUND,
		TARGET_NOT_UNIQUE,
		TARGET_WRONG_TYPE,
		TARGET_NOT_INTERNAL,
		NO_CALLSITES_TO_MAP,
		NO_ACCESSORS_TO_MAP,
		MAPPED_CALLSITE,
		MAPPED_INSTANTIATION,
		NOT_MAPPED_CALLSITE,
		NOT_MAPPED_INSTANTIATION,
		NO_INSTANTIATIONS_TO_MAP,
		MAPPED_ACCESSOR,
		NOT_MAPPED_ACCESSOR,
		TARGET_AMBIGUOUS,
		TARGET_INVALID,
		INCOMPATIBLE_TYPES,
		INCOMPATIBLE_CLASSES;
	
	final String DESCRIPTION = getString("com.ibm.ive.tools.japt.remapping.remapping_28");

	final String IDENT = getString("com.ibm.ive.tools.japt.remapping.source_and_target_identical_29"),
		TYPES_INCOMPATIBLE = getString("com.ibm.ive.tools.japt.remapping.types_incompatible_30"),
		NO_MATCHING_CONSTRUCTOR = getString("com.ibm.ive.tools.japt.remapping.no_matching_constructor_31"),
		AMBIGUOUS_INVOCATION = getString("com.ibm.ive.tools.japt.remapping.ambiguous_method_invocation_32"),
		NO_CONSTRUCTOR_INVOCATION_FOUND = getString("com.ibm.ive.tools.japt.remapping.no_constructor_invocation_found_33"),
		SPECIAL_METHOD = getString("com.ibm.ive.tools.japt.remapping.static_initializer_or_finalizer_34"),
		CONSTRUCTORS_INCOMPATIBLE = getString("com.ibm.ive.tools.japt.remapping.constructors_incompatible_35"),
		VISIBILITY_INCOMPATIBLE = getString("com.ibm.ive.tools.japt.remapping.visibilities_incompatible_36"),
		EXTERNAL_CLASS = getString("com.ibm.ive.tools.japt.remapping.external_class_37"),
		ACCESSES_STATIC_TO_NON_STATIC = getString("com.ibm.ive.tools.japt.remapping.cannot_map_static_field_access_to_non-static_field_access_38"),
		LOAD_CONSTANT = getString("com.ibm.ive.tools.japt.remapping.cannot_remap_instantiation_of_constant_strings_39");
	
	final String 
		REMAP_METHOD_SPECIAL_LABEL = "remapMethodCallsSp",
		REMAP_METHOD_LABEL = "remapMethodCalls",
		REMAP_FIELD_LABEL = "remapFieldAccesses",
		REMAP_FIELD_READ_LABEL = "remapFieldReads",
		REMAP_FIELD_WRITE_LABEL = "remapFieldWrites",
		REMAP_INSTANTIATION_LABEL = "remapInstantations",
		CREATE_LABEL = "create",
		NO_CHECK_TYPES_LABEL = "noCheckTypes",
		EXPAND_PERMISSIONS_LABEL = "expandPermissions",
		WITHIN_CLASS_LABEL = "remapWithinClass",
		WITHIN_METHOD_LABEL = "remapWithinMethod";
	
	final String 
		REMAP_METHOD_SPECIAL = getString("com.ibm.ive.tools.japt.remapping.remap_method_invocations_using_invokespecial_17"),
		REMAP_METHOD = getString("com.ibm.ive.tools.japt.remapping.remap_method_invocations_of_named_source_method_to_named_target_method_18"),
		REMAP_FIELD = getString("com.ibm.ive.tools.japt.remapping.remap_field_accesses_of_named_source_fields_to_named_target_field_19"),
		REMAP_FIELD_READ = getString("com.ibm.ive.tools.japt.remapping.remap_field_reads_of_named_source_fields_to_named_target_field_20"),
		REMAP_FIELD_WRITE = getString("com.ibm.ive.tools.japt.remapping.remap_field_writes_of_named_source_fields_to_named_target_field_21"),
		REMAP_INSTANTIATION = getString("com.ibm.ive.tools.japt.remapping.remap_instantiations_of_named_classes_to_instantiations_of_named_target_22"),
		EXPAND_PERMISSIONS = getString("com.ibm.ive.tools.japt.remapping.expand_permissions_to_allow_remapping_23"),
		CREATE = getString("com.ibm.ive.tools.japt.remapping.create_targets_if_none_found_24"),
		NO_CHECK_TYPES = getString("com.ibm.ive.tools.japt.remapping.do_not_check_type_compatibility_25"),
		WITHIN_CLASS = getString("com.ibm.ive.tools.japt.remapping.remap_only_within_named_class(es)_26"),
		WITHIN_METHOD = getString("com.ibm.ive.tools.japt.remapping.remap_only_within_named_method(s)_27");
	
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
