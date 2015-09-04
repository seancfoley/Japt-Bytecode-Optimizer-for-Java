package com.ibm.ive.tools.japt.out;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.MsgHelp;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.ive.tools.japt.JaptMessage.StatusMessage;
import com.ibm.ive.tools.japt.JaptMessage.WarningMessage;

/**
 * @author sfoley
 *
 */
public class Messages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.out.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	public Messages(Component component) {
		NO_DATA_AVAILABLE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.no_data_available_for_jar_entry__{0}_1")));
		NO_TARGET = new ErrorMessage(component, getString("com.ibm.ive.tools.japt.out.no_target_specified_2"));
		ERROR_WRITING_RESOURCE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Error_writing_resource_{0}__{1}_3")));
		WROTE_CLASS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Class_file_generated_for__{0}_4")));
		WROTE_RESOURCE = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Resource_file_generated_for__{0}_5")));
		ERROR_WRITING_JAR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Error_writing_to_jar_{0}__{1}_6")));
		CREATED = new StatusMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Created_{0}_13")));
		NO_OUTPUT = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.No_output_14")));
		ERROR_WRITING_CLASS = new ErrorMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.out.Error_writing_class_{0}__{1}_31")));
		ERROR_WRITING = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Error_writing_{0}_15")));
		ERROR_WRITING_TO_DIR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Error_writing_to_directory_{0}__{1}_16")));
		COULD_NOT_CREATE_DIRECTORY = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.could_not_create_directory_{0}_21")));
		DIRECTORY_EXISTS = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.target_{0}_exists_and_is_not_a_directory_22")));
		WRITTEN_TO = new StatusMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Written_class_files_and_resources_to_{0}_23")));
		REMOVING_STACKMAPS_WARNING = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.removing_stackmaps_from_method_{0}_in_class_{1}_27")));
		ADDED_STACKMAPS = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Added_{1}_stackmaps_to_{0}_30")));
		COULD_NOT_CREATE_STACKMAPS = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Could_not_add_stackmaps_to_{0}_32")));
		ATTRIBUTE_NOT_WRITTEN = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Attribute_{0}_of_{1}_not_written_34")));
		ATTRIBUTE_REMOVED = new InfoMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Attribute_{0}_of_{1}_removed_41")));
		CREATED_LOADER = new StatusMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.out.Created_loader_method_{0}_in_class_{1}_36")));
		VERSION_UNSUPPORTED_CLDC = new ErrorMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.out.Version_{0}_of_class_{1}_not_supported_for_CLDC_42")));
		STACK_MAPS_LIKELY_IGNORED = new WarningMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.out.Stackmaps_likely_ignored_in_{0}_43")));
		CANNOT_HAVE_BOTH_STACKMAPS = new ErrorMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.out.Cannot_both_stackmaps_in_{0}_44")));
		
	}
	
	public final ErrorMessage
		NO_TARGET;
	
	final LogMessage NO_DATA_AVAILABLE,
		ERROR_WRITING_RESOURCE,
		WROTE_CLASS,
		WROTE_RESOURCE,
		ERROR_WRITING_JAR,
		CREATED,
		NO_OUTPUT,
		ERROR_WRITING_CLASS,
		ERROR_WRITING,
		ERROR_WRITING_TO_DIR,
		COULD_NOT_CREATE_DIRECTORY,
		DIRECTORY_EXISTS,
		WRITTEN_TO,
		REMOVING_STACKMAPS_WARNING,
		ADDED_STACKMAPS,
		COULD_NOT_CREATE_STACKMAPS,
		ATTRIBUTE_NOT_WRITTEN,
		ATTRIBUTE_REMOVED,
		CREATED_LOADER,
		VERSION_UNSUPPORTED_CLDC,
		STACK_MAPS_LIKELY_IGNORED,
		CANNOT_HAVE_BOTH_STACKMAPS;

	final String 
		NO_STRIP_META_LABEL = "noStripMetaInfo", 
		NO_STRIP_DEBUG_LABEL = "noStripDebugInfo", 
		NO_STRIP_ATTS_LABEL = "noStripSourceInfo",
		NO_STRIP_ANNOTATIONS_LABEL = "noStripAnnotations",
		REMOVE_ATTRIBUTE_LABEL = "removeAttribute",
		REMOVE_STACKMAPS_LABEL = "removeStackMaps",
		NO_COMPRESS_LABEL = "noCompress",
		INCLUDE_ZIPPED_LABEL = "includeZippedResources",
		OUTPUT_LABEL = "output",
		DIR_LABEL = "dir",
		EXCLUDE_RESOURCE_LABEL = "excludeResource",
		ADD_STACKMAPS_LABEL = "addStackMaps",
		PREVERIFY_LABEL = "preverifyCLDC",
		CLASS_VERSION_LABEL = "classVersion",
		CREATE_AUTO_LOADER_LABEL = "createAutoLoaders",
		EXCLUDE_CLASS_LABEL = "excludeClass",
		STRIP_DIGEST_LABEL = "removeSignatures";
	
	final String
		NO_STRIP_META = getString("com.ibm.ive.tools.japt.out.do_not_strip_jar_meta_info_40"),
		NO_STRIP_DEBUG = getString("com.ibm.ive.tools.japt.out.do_not_strip_debug_info_7"),
		NO_STRIP_ANNOTATIONS = getString("com.ibm.ive.tools.japt.out.do_not_strip_annotations_26"),
		OUTPUT = getString("com.ibm.ive.tools.japt.out.name_of_the_output_jar_file_or_directory_8"),
		DIR = getString("com.ibm.ive.tools.japt.out.name_of_the_target_directory_20"),
		DESCRIPTION = getString("com.ibm.ive.tools.japt.out.jar_generation_9"),
		DESCRIPTION_DIR = getString("com.ibm.ive.tools.japt.out.file_generation_17"),
		EXCLUDE_RESOURCE = getString("com.ibm.ive.tools.japt.out.exclude_the_named_resource_10"),
		NO_COMPRESS = getString("com.ibm.ive.tools.japt.out.do_not_compress_jar_entries_11"),
		INCLUDE_ZIPPED = getString("com.ibm.ive.tools.japt.out.include_zipped_resources_12"),
		NO_STRIP_ATTS = getString("com.ibm.ive.tools.japt.out.do_not_strip_unnecessary_attributes_24"),
		REMOVE_ATTRIBUTE = getString("com.ibm.ive.tools.japt.out.remove_the_named_attribute_25"),
		REMOVE_STACKMAPS = getString("com.ibm.ive.tools.japt.out.remove_stackmaps_28"),
		ADD_STACKMAPS = getString("com.ibm.ive.tools.japt.out.add_stack_maps_29"),
		PREVERIFY = getString("com.ibm.ive.tools.japt.out.preverify_for_CLDC_33"),
		CLASS_VERSION = getString("com.ibm.ive.tools.japt.out.update_class_version_to_named_newer_version_35"),
		CREATE_AUTO_LOADER  = getString("com.ibm.ive.tools.japt.out.create_auto_load_class_within_each_archive_37"),
		EXCLUDE_CLASS  = getString("com.ibm.ive.tools.japt.out.exclude_the_name_class_from_archives_38"),
		STRIP_DIGEST = getString("com.ibm.ive.tools.japt.out.strip_signatures_from_archives_39");
		


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
