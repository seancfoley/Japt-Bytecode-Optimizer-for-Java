package com.ibm.ive.tools.japt.commandLine;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.JaptMessage.ErrorMessage;
import com.ibm.ive.tools.japt.JaptMessage.ProgressMessage;
import com.ibm.ive.tools.japt.JaptMessage.StatusMessage;
import com.ibm.ive.tools.japt.JaptMessage.WarningMessage;
import com.ibm.jikesbt.BT_Factory;

/**
 * @author sfoley
 *
 */
public class CommandLineMessages {
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.commandLine.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	
	static final String usageIndent = "    ";
		
	public CommandLineMessages(Component component) {
		//the first few messages do not indicate their component
		String newLine = BT_Factory.endl();
		LOGO_MESSAGE = new StatusMessage(null,
			getString("com.ibm.ive.tools.japt.commandLine.copy1_103") + newLine + 
			getString("com.ibm.ive.tools.japt.commandLine.copy2_104") + newLine + 
			getString("com.ibm.ive.tools.japt.commandLine.copy3_105") + newLine);
		PROGRAM_MESSAGE = new StatusMessage(null, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.{0}_version_{1}_5")));
		PROPERTY_MESSAGE = new StatusMessage(null, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.___{0}__{1}_83")));
		USAGE_MESSAGE = new StatusMessage(null, getString("com.ibm.ive.tools.japt.commandLine.usage_optimizes_java_class_files_6") + newLine + newLine + 
			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.japt_optimizes_java_classes_7") + newLine + newLine + getString("com.ibm.ive.tools.japt.commandLine.basic_options_8"));
		OPTION_MESSAGE = new StatusMessage(null, usageIndent);
		EXTENSION_MESSAGE = new StatusMessage(null, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.program_extensions_13") + newLine + usageIndent + getString("com.ibm.ive.tools.japt.commandLine.{0}_14")));
//			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.An_extension_is_an_implementation_of__15") + newLine +
//			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.{4}__12") + newLine +
//			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.which_may_have_its_own_interpretation_of_the_above_command_line_options,_16") + newLine + 
//			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.as_well_as_its_own_command_line_options_appearing_after___{1}___17") + newLine + 
//			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.on_the_command_line._18") + newLine + 
			
		EXTENSION_HELP_MESSAGE = new StatusMessage(null, new FormattedString(
			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.Combining___{0}___with___{1}___or__for_example_{2}__with__{3}__will_print_out_the_available_options_19") + newLine + 
			usageIndent + getString("com.ibm.ive.tools.japt.commandLine.for_the_named_extension_following_this_message__20")));
		OPTIONS_MESSAGE = new StatusMessage(null, new FormattedString(getString("com.ibm.ive.tools.japt.commandLine.{0}_options__21")));
		NO_OPTIONS_MESSAGE = new StatusMessage(null, new FormattedString(getString("com.ibm.ive.tools.japt.commandLine.none_22")));
		SECTION_END = new StatusMessage(null, "");
		ABBREVIATION_MESSAGE = new StatusMessage(null, usageIndent + getString("com.ibm.ive.tools.japt.commandLine.The_following_extensions_are_available__97"));
		
		UNKNOWN_OPTION = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Unknown_option__{0}_24")));
		UNKNOWN_STRING = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Invalid_parameter__{0}_23")));
		UNKNOWN_OPTION_IN_FILE = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Unknown_option__{0}_({1})_25")));
		UNKNOWN_STRING_IN_FILE = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Invalid_parameter__{0}_({1})_26")));
		INACCESSIBLE_OPTIONS = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Could_not_read_options_from_{0}_27")));
		INVALID_OPTION = new WarningMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Invalid_appearance_of_option__{0}_28")));
		
		UNAVAILABLE_CONSTRUCTOR = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.The_constructor_of_{0}_is_not_visible._29")));
		UNINSTANTIABLE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Could_not_create_an_instance_of_{0}_30")));
		NO_PERMISSIONS = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.No_security_permissions_to_create_an_instance_of_{0}_31")));
		NO_EXTENSION = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Could_not_find_extension_{0}_32")));
		ERROR_INITIALIZING = new ErrorMessage(component, new FormattedString( 
			getString("com.ibm.ive.tools.japt.commandLine.Error_initializing_extension_{0}__{1}_33")));
		ERROR_LINKING = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Error_linking_extension_{0}_34")));
		INVALID_EXTENSION = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Extension_{0}_is_not_an_instance_of_{1}_35")));
		
		
		COULD_NOT_OPEN_FILE = new ErrorMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Could_not_open_file_{0}_89")));
		LOGGING_TO = new ProgressMessage(component, new FormattedString(
			getString("com.ibm.ive.tools.japt.commandLine.Logging_to_file_{0}_90"))); 
		CREATED_FILE = new ProgressMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.commandLine.Created_file_{0}_106"))); 
	}
	
	public final LogMessage LOGO_MESSAGE,
		PROGRAM_MESSAGE,
		PROPERTY_MESSAGE,
		UNKNOWN_OPTION,
		UNKNOWN_STRING,
		UNKNOWN_OPTION_IN_FILE,
		UNKNOWN_STRING_IN_FILE,
		INACCESSIBLE_OPTIONS,
		INVALID_OPTION,
		USAGE_MESSAGE,
		ABBREVIATION_MESSAGE,
		EXTENSION_MESSAGE,
		EXTENSION_HELP_MESSAGE,
		OPTION_MESSAGE,
		OPTIONS_MESSAGE,
		NO_OPTIONS_MESSAGE,
		SECTION_END,
		UNAVAILABLE_CONSTRUCTOR,
		UNINSTANTIABLE,
		NO_PERMISSIONS,
		NO_EXTENSION,
		ERROR_INITIALIZING,
		ERROR_LINKING,
		INVALID_EXTENSION,
		COULD_NOT_OPEN_FILE,
		LOGGING_TO,
		CREATED_FILE;

	public final String OPTIONS_FILE_LABEL = "@xxx";	
	
	final String 
		NO_VERBOSE_LABEL = "quiet",
		LOG_LABEL = "log",
		HELP_LABEL = "help",
		IC_LABEL = "incrementalLoad",
		EXTENSION_LABEL = "extension",
		VERSION_LABEL = "version",
		SYS_PROP_LABEL = "sysProps",
		MACRO_LABEL = "macro",
		NO_RESOLVE_LABEL = "noResolveRuntimeRefs",
		LOAD_LABEL = "load",
		INLINE_LABEL = "inline",
		REDUCE_LABEL = "reduce",
		REFACTORINNER_LABEL = "refactorInner",
		DEVIRTUALIZE_LABEL = "devirtualize",
		OBFUSCATE_LABEL = "obfuscate",
		JAR_OUTPUT_LABEL = "jarOutput",
		DIR_OUTPUT_LABEL = "dirOutput",
		STARTUP_OUTPUT_LABEL = "deferClassLoads",
		//COLD_LABEL = "migrateMethodBodies" xx "migrateMethodCode",
		COLD_LABEL = "migrateMethodBodies",
		ORDER_OUTPUT_LABEL = "orderClasses";
		
	
	public final String OPTIONS_FILE = getString("com.ibm.ive.tools.japt.commandLine.read_named_file_for_additional_options_72");	
	
	final String 
		NO_VERBOSE = getString("com.ibm.ive.tools.japt.commandLine.quiet_mode_99"),
		HELP = getString("com.ibm.ive.tools.japt.commandLine.print_out_this_message_55"),
		EXTENSION = getString("com.ibm.ive.tools.japt.commandLine.use_named_program_extension_56"),
		LOG = getString("com.ibm.ive.tools.japt.commandLine.log_to_the_named_file_75"),
		VERSION = getString("com.ibm.ive.tools.japt.commandLine.print_out_program_versions_79"),
		SYS_PROP = getString("com.ibm.ive.tools.japt.commandLine.print_out_the_system_properties_84"),
		MACRO = getString("com.ibm.ive.tools.japt.commandLine.define_a_macro_88"),
		NO_RESOLVE = getString("com.ibm.ive.tools.japt.commandLine.no_resolve_runtime_references_101"),
		IC = getString("com.ibm.ive.tools.japt.commandLine.no_automatic_load_102"),
		DESCRIPTION = getString("com.ibm.ive.tools.japt.commandLine.command_line_100");
		
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
