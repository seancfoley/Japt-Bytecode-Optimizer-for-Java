package com.ibm.ive.tools.japt.load;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.ive.tools.japt.JaptMessage.StatusMessage;

/**
 * @author sfoley
 *
 */
public class Messages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.load.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	static final String usageIndent = "    ";
		
	Messages(Component component) {
		ERROR_READING_FILE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.load.Error_reading_file_{0}_94")));
		NO_INTERNAL_SOURCES_SPECIFIED = new ErrorMessage(component, getString("com.ibm.ive.tools.japt.load.no_internal_class_paths_specified_100"));
		COMPLETED_LOADING = new StatusMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.load.Completed_loading_{0}_internal_classes_and_{1}_external_classes_with_{2}_classes_unreadable_and_{3}_classes_not_found_99")));	
		CREATING_REF = new StatusMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.load.Creating_reference_file_{0}_96")));
		NO_REF = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.load.Could_not_create_reference_file_{0}_74")));
		CREATING_INTERNAL_REF = new StatusMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.load.Creating_internal_class_reference_file_{0}_125")));
		CREATING_UNRESOLVED_REF = new StatusMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.load.Creating_unresolved_reference_file_{0}_113")));
		NO_UNRESOLVED_REF = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.load.Could_not_create_unresolved_reference_file_{0}_114")));
		NO_INTERNAL_REF = new InfoMessage(component, new FormattedString(getString("com.ibm.ive.tools.japt.load.Could_not_create_internal_class_reference_file_{0}_126")));
		DIR_INEXISTENT = new ErrorMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.load.Directory_{0}_does_not_exist_or_is_not_a_directory_117")));
	}
	
	public final LogMessage 
		NO_REF,
		CREATING_REF,
		NO_INTERNAL_REF,
		NO_UNRESOLVED_REF,
		CREATING_INTERNAL_REF,
		CREATING_UNRESOLVED_REF,
		COMPLETED_LOADING,
		DIR_INEXISTENT;
	
	final LogMessage 
		ERROR_READING_FILE,
		NO_INTERNAL_SOURCES_SPECIFIED;
	
	final String DESCRIPTION = getString("com.ibm.ive.tools.japt.load.load_98");
	
	final String 
		INCLUDE_CLASS_LABEL = "includeClass",
		INCLUDE_SUBCLASS_LABEL = "includeSubclasses",
		INCLUDE_RESOURCE_LABEL = "includeResource",
		INCLUDE_LIBRARY_CLASS_LABEL ="includeLibraryClass",
		INCLUDE_ACCESSIBLE_CLASS_LABEL ="includeAccessibleClass",
		INCLUDE_WHOLE_CLASS_LABEL = "includeWholeClass",
		INCLUDE_METHOD_LABEL = "includeMethod",
		INCLUDE_FIELD_LABEL = "includeField",
		INCLUDE_METHODEX_LABEL = "includeMethodEx",
		INCLUDE_MAIN_METHOD_LABEL = "includeMainMethod",
		INCLUDE_IMPLEMENTORS_LABEL = "includeImplementors",
		INCLUDE_OVERRIDERS_LABEL = "includeOverriders",
		INCLUDE_EXT_LIBRARY_LABEL = "includeExtLibraryClass",
		INCLUDE_EXT_ACCESSIBLE_LABEL = "includeExtAccClass",
		INCLUDE_DYNAMIC_CLASS_LABEL = "includeDynamicClass",
		INCLUDE_SERIALIZABLE_LABEL = "includeSerializable",
		INCLUDE_SERIALIZED_LABEL = "includeSerialized",
		INCLUDE_EXTERNALIZED_LABEL = "includeExternalized",
		REFLECTION_WARNINGS_LABEL = "reflectionWarnings",
		RESET_CP_LABEL = "resetClassPath",
		BUILT_IN_RULES_LABEL = "builtInRules",
		CP_LABEL = "cp",
		INTERNAL_CLASS_PATH_LABEL = "icp",
		CP_ALL_LABEL = "cpAll",
		INTERNAL_CLASS_ALL_LABEL = "icpAll",
		JRE_LABEL = "jre",
		VERIFY_LABEL = "verify",
		OPTIMIZE_LABEL = "optimize",
		PRINT_CLASS_LABEL = "printClass",
		PRINT_METHOD_LABEL = "printMethod",
		NO_FOLLOW_LABEL = "noFollow",
		NO_BUILT_IN_RULES_LABEL = "noBuiltInRules",
		LOAD_LABEL = "loadFile",
		LOAD_ALL_LABEL = "loadAll",
		LOAD_RESOURCE_LABEL = "loadResource",
		LOAD_CLASS_LABEL = "loadClass",
		MAIN_CLASS_LABEL = "startupClass",
		FILE_EXTENSION_LABEL = "jarExtension",
		REFTREE_LABEL = "ref",
		UNRESOLVED_REFERENCE_FILE_LABEL = "unresolvedReferenceFile",
		REFERENCE_FILE_LABEL = "referenceFile";

	
	
	final String 
		INCLUDE_CLASS = getString("com.ibm.ive.tools.japt.load.include_named_class(es)_57"),
		INCLUDE_ACCESSIBLE_CLASS = getString("com.ibm.ive.tools.japt.load.include_public/protected_members_of_named_class(es)_58"),
		INCLUDE_LIBRARY_CLASS = getString("com.ibm.ive.tools.japt.load.include_non_private_members_of_named_class(es)_107"),
		INCLUDE_WHOLE_CLASS = getString("com.ibm.ive.tools.japt.load.include_all_members_of_the_named_class(es)_59"),
		INCLUDE_METHOD = getString("com.ibm.ive.tools.japt.load.include_named_method(s)_63"),
		INCLUDE_FIELD = getString("com.ibm.ive.tools.japt.load.include_named_field(s)_64"),
		INCLUDE_METHODEX = getString("com.ibm.ive.tools.japt.load.include_named_method(s)_independent_of_containing_class(es)_65"),
		INCLUDE_IMPLEMENTORS = getString("com.ibm.ive.tools.japt.load.include_named_interface(s)_and_known_implementing_methods_104"),
		INCLUDE_OVERRIDERS = getString("com.ibm.ive.tools.japt.load.include_named_class(es)_and_known_overriding_methods_105"),
		INCLUDE_EXT_ACCESSIBLE = getString("com.ibm.ive.tools.japt.load.include_public_protected_members_of_named_class(es)_and_known_overriding_implementing_methods_106"),
		INCLUDE_EXT_LIBRARY = getString("com.ibm.ive.tools.japt.load.include_non_private_members_of_named_class(es)_and_known_overriding_implementing_methods_108"),
		CP = getString("com.ibm.ive.tools.japt.load.append_named_jar/zip/directory_to_the_classpath_71"),
		INTERNAL_CLASS_PATH = getString("com.ibm.ive.tools.japt.load.append_named_jar/zip/dir_to_internal_class_path_97"),
		LOAD_RESOURCE = getString("com.ibm.ive.tools.japt.load.load_named_resource(s)_76"),
		VERIFY = getString("com.ibm.ive.tools.japt.load.Check_internal_classes_for_class_and_method_verification_errors_77"),
		NO_BUILT_IN_RULES = getString("com.ibm.ive.tools.japt.load.Do_not_use_rules_reduction_files_78"),
		LOAD = getString("com.ibm.ive.tools.japt.load.load_named_class_file_or_archive_80"),
		LOAD_CLASS = getString("com.ibm.ive.tools.japt.load.load_named_class_81"),
		INCLUDE_MAIN_METHOD = getString("com.ibm.ive.tools.japt.load.include_main(String[])_method_resolved_from_named_class(es)_85"),
		INCLUDE_SUBCLASS = getString("com.ibm.ive.tools.japt.load.include_subclasses_of_named_class(es)_86"),
		OPTIMIZE = getString("com.ibm.ive.tools.japt.load.perform_bytecode_optimization_87"),
		INCLUDE_DYNAMIC_CLASS = getString("com.ibm.ive.tools.japt.load.include_dynamically_accessed_classes_91"),
		INCLUDE_SERIALIZABLE = getString("com.ibm.ive.tools.japt.load.include_serializable_items_111"),
		INCLUDE_SERIALIZED = getString("com.ibm.ive.tools.japt.load.include_serialized_items_92"),
		INCLUDE_EXTERNALIZED = getString("com.ibm.ive.tools.japt.load.include_externalized_items_93"),
		REFLECTION_WARNINGS = getString("com.ibm.ive.tools.japt.load.print_warnings_concerning_access_to_java_lang_Class_95"),
		RESET_CP = getString("com.ibm.ive.tools.japt.load.reset_class_path_101"),
		BUILT_IN_RULES = getString("com.ibm.ive.tools.japt.load.use_rules_reduction__default__102"),
		MAIN_CLASS = getString("com.ibm.ive.tools.japt.load.manifest_main_class_103"),
		NO_FOLLOW = getString("com.ibm.ive.tools.japt.load.do_not_load_all_referenced_classes_109"),
		FILE_EXTENSION = getString("com.ibm.ive.tools.japt.load.handle_files_with_named_extension_as_jar_files_110"),
		UNRESOLVED_REFERENCE_FILE = getString("com.ibm.ive.tools.japt.load.use_named_unresolved_reference_file_112"),
		REFERENCE_FILE = getString("com.ibm.ive.tools.japt.load.list_class_references_in_named_file_124"),
		REFTREE = getString("com.ibm.ive.tools.japt.load.generate_named_reference_file_73"),
		CP_ALL = getString("com.ibm.ive.tools.japt.load.append_archives_found_within_named_dir_to_class_path_119"),
		INTERNAL_CLASS_ALL = getString("com.ibm.ive.tools.japt.load.append_archives_found_within_named_dir_to_internal_class_path_120"),
		LOAD_ALL = getString("com.ibm.ive.tools.japt.load.load_archives_found_within_named_dir_118"),
		JRE = getString("com.ibm.ive.tools.japt.load.append_jre_classes_found_within_named_dir_121"),
		PRINT_CLASS = getString("com.ibm.ive.tools.japt.load.print_named_class(es)_122"),
		PRINT_METHOD = getString("com.ibm.ive.tools.japt.load.print_named_method(s)_123");
	
	
		//UNRESOLVED_ITEMS = getString("com.ibm.ive.tools.japt.load.Unresolved_Items_116"),
		//INACCESSIBLE_ITEMS = getString("com.ibm.ive.tools.japt.load.Inaccessible_Items_115");
	
	
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
