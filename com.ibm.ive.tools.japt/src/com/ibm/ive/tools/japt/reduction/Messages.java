package com.ibm.ive.tools.japt.reduction;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.MsgHelp;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.ive.tools.japt.JaptMessage.ProgressMessage;
import com.ibm.ive.tools.japt.JaptMessage.WarningMessage;
import com.ibm.jikesbt.BT_Factory;

/**
 * @author sfoley
 *
 */
public class Messages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.reduction.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	public Messages(Component component) {
		MADE_CLASS_ABSTRACT = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.reduction.Made_class_{0}_abstract_39")));
		REMOVED_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Removed_{0}__{1}_2")));
		REMOVED_FIELD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Removed_field_{0}_3")));
		REMOVED_METHOD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Removed_method_{0}_4")));
		REMOVED_UNUSED_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Removed_unused_{0}_{1}_5")));
		REMOVED_UNUSED_FIELD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Removed_unused_field_{0}_6")));
		REMOVED_UNUSED_METHOD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Removed_unused_method_{0}_7")));
		REMOVED_CODE_FROM_METHOD = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Removed_code_from_method_{0}_8")));
		MADE_METHOD_ABSTRACT = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Made_abstract_method_{0}_9")));
		MISSING_CLASSES = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Unavailable_classes_({0})_may_cause_reduction_errors_10")));
		XTA_ITERATION_INFO = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.XTA_Iteration_{0}__Object_types_were_propagated_by_{1}_methods,_fields_and_arrays_11")));
		RTA_ITERATION_INFO = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.RTA_Iteration_{0}__{1}_methods_and_fields_actively_propagated_12")));
		ITA_ITERATION_INFO = new ProgressMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.reduction.ITA_Iteration_{0}__propagated_{1}_classes_comprising_{2}_static_fields,_{3}_method_invocations,_and_{4}_objects_comprising_{5}_instance_fields_and_{7}_array_elements_41")));
		ITA_EXTENDED_ITERATION_INFO = new ProgressMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.reduction.ITA_Iteration_{0}__propagated_{1}_classes_comprising_{2}_static_fields,_{3}_method_invocations,_{6}_generic_method_invocations__{8}_generic_objects_and_{4}_objects_comprising_{5}_instance_fields_and_{7}_array_elements_42")));
		SEEDING = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Identifying_{0}_methods__{1}_fields__and_{2}_classes_as_reduction_entry_points_13")));
		STARTING_PROPAGATION = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.reduction.Starting_propagation_14"));
		REMOVING_ITEMS = new ProgressMessage(component, getString("com.ibm.ive.tools.japt.reduction.Removing_unused_items_38"));
		String newLine = BT_Factory.endl();
		SUMMARY = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Reduction_summary_15") + newLine +
			getString("com.ibm.ive.tools.japt.reduction.Removed_{0}_classes_comprising_{1}_fields_and_{2}_methods_18") + newLine +
			getString("com.ibm.ive.tools.japt.reduction.In_remaining_classes_16") + newLine +
			getString("com.ibm.ive.tools.japt.reduction.Made_{3}_uninstantiated_classes_abstract_19") + newLine +
			getString("com.ibm.ive.tools.japt.reduction.Removed_{4}_of_{5}_fields_and_{6}_of_{7}_methods_17") + newLine + 
			getString("com.ibm.ive.tools.japt.reduction.Changed_code_to_simply_return_in_{8}_methods_20") + newLine + 
			getString("com.ibm.ive.tools.japt.reduction.Made_{9}_methods_abstract_21")));
		BASIC_ITERATION_INFO = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Basic_Iteration_{0}__{1}_methods_and_fields_actively_propagated_30")));
		COULD_NOT_OPEN_FILE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Could_not_open_file_{0}_36")));
		CREATING_ENTRY_POINT_FILE = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Creating_entry_point_file_{0}_37")));
		FLOW_ERROR = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.reduction.Could_not_analyze_intraprocedural_object_flow_in_code_for_{0}__{1}_43")));
	}
	
	
	final String DESCRIPTION = "reduction";

	public final LogMessage 
		FLOW_ERROR,
		REMOVED_CLASS,
		REMOVED_FIELD,
		REMOVED_METHOD,
		REMOVED_UNUSED_CLASS,
		REMOVED_UNUSED_FIELD,
		REMOVED_UNUSED_METHOD,
		MADE_CLASS_ABSTRACT,
		REMOVED_CODE_FROM_METHOD,
		MADE_METHOD_ABSTRACT,
		MISSING_CLASSES,
		XTA_ITERATION_INFO,
		RTA_ITERATION_INFO,
		ITA_ITERATION_INFO,
		ITA_EXTENDED_ITERATION_INFO,
		SEEDING,
		STARTING_PROPAGATION,
		REMOVING_ITEMS,
		SUMMARY,
		BASIC_ITERATION_INFO,
		COULD_NOT_OPEN_FILE,
		CREATING_ENTRY_POINT_FILE;
		
	public final String 
		REMOVE_SUBCLASS_LABEL = "removeSubclasses",
		REMOVE_CLASS_LABEL = "removeClass",
		REMOVE_METHOD_LABEL = "removeMethod",
		REMOVE_FIELD_LABEL = "removeField",
		RTA_LABEL = "rta",
		NO_MAKE_CLASSES_ABSTRACT_LABEL = "noMakeClassesAbstract",
		NO_REMOVE_UNUSED_LABEL = "noRemoveUnused",
		BASIC_LABEL = "bta",
		XTA_LABEL = "xta",
		ITA_LABEL = "ita",
		NO_MARK_ENTRY_LABEL = "noMarkEntryPoints",
		NO_ALTER_LABEL = "removeClassesOnly",
		ENTRY_POINT_LABEL = "entryPointFile",
		REMOVES_NOT_RESOLVABLE_LABEL = "noResolveRemoveSpecs";
	
	public final String 
		REMOVE_SUBCLASS = getString("com.ibm.ive.tools.japt.reduction.remove_subclasses_or_subinterfaces_of_the_named_class_28"),
		REMOVE_CLASS = getString("com.ibm.ive.tools.japt.reduction.remove_the_named_class_23"),
		REMOVE_METHOD = getString("com.ibm.ive.tools.japt.reduction.remove_the_name_method_24"),
		REMOVE_FIELD = getString("com.ibm.ive.tools.japt.reduction.remove_the_named_field_25"),
		RTA = getString("com.ibm.ive.tools.japt.reduction.use_rapid_type_analysis_reduction_algorithm_26"),
		NO_MAKE_CLASSES_ABSTRACT = getString("com.ibm.ive.tools.japt.reduction.do_not_make_classes_abstract_27"),
		NO_REMOVE_UNUSED = getString("com.ibm.ive.tools.japt.reduction.do_not_remove_unused_items_29"),
		BASIC = getString("com.ibm.ive.tools.japt.reduction.use_basic_type_analysis_reduction_algorithm_31"),
		XTA = getString("com.ibm.ive.tools.japt.reduction.use_type_analysis_reduction_algorithm__default__32"),
		ITA = getString("com.ibm.ive.tools.japt.reduction.use_instantiated_type_analysis_reduction_algorithm__40"),
		NO_MARK_ENTRY = getString("com.ibm.ive.tools.japt.reduction.no_mark_entry_points_33"),
		NO_ALTER = getString("com.ibm.ive.tools.japt.reduction.do_not_reduce_class_contents_34"),
		ENTRY_POINT = getString("com.ibm.ive.tools.japt.reduction.create_named_entry_point_listing_file_35"),
		REMOVES_NOT_RESOLVABLE = getString("com.ibm.ive.tools.japt.reduction.do_not_resolve_method_field_remove_specifiers_39");
	
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
