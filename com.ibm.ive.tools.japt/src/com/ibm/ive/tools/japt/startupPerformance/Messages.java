package com.ibm.ive.tools.japt.startupPerformance;

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
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.startupPerformance.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	Messages(Component component) {
		OPTIMIZED_THROW_AT_LINE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_throw_instruction_in_method_{0}_at_line_number_{1}_and_bytecode_offset_{2}_4")));
		OPTIMIZED_THROW = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_throw_instruction_in_method_{0}_at_bytecode_offset_{1}_5")));
		OPTIMIZED_THROW_SIMPLE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.throw__{0}_6")));
		CLAZZ_SIMPLE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.class__{0}_7")));
		METHOD_SIMPLE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.method__{0}_8")));
		OPTIMIZED_CATCH_AT_LINE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Adding_an_exception_table_entry_for_catch_block_at_line_{0}_and_bytecode_offset_{1}_catching_{2}_9")));
		OPTIMIZED_CATCH_SIMPLE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.catch__{0}_10")));
		OPTIMIZED_CATCH = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Adding_an_exception_table_entry_for_catch_block_at_bytecode_offset_{0}_catching_{1}_11")));
		DONE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.done_12")));
		String newLine = BT_Factory.endl();
		String summaryString = getString("com.ibm.ive.tools.japt.startupPerformance.Summary_13");
		String line1 = getString("com.ibm.ive.tools.japt.startupPerformance.Optimized_{0}_methods__22");
		String line2 = getString("com.ibm.ive.tools.japt.startupPerformance.Optimized_{1}_catch_blocks_for_{2}_try_blocks__23");
		String line3 = getString("com.ibm.ive.tools.japt.startupPerformance.Optimized_{3}_throw_instructions__24");
		String line4 = getString("com.ibm.ive.tools.japt.startupPerformance.Optimized_{4}_return_instructions__25");
		String line5 = getString("com.ibm.ive.tools.japt.startupPerformance.Optimized_{5}_arguments_for_{6}_field_access_instructions__26");
		String line6 = getString("com.ibm.ive.tools.japt.startupPerformance.Optimized_{7}_arguments_for_{8}_method_invocation_instructions__27");
		SUMMARY = new InfoMessage(component, new FormattedString(summaryString + newLine + line1 + newLine + line2 + newLine + line3 + newLine + line4 + newLine + line5 + newLine + line6));
		OPTIMIZED_RETURN_AT_LINE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_upcast1_in_method_{0}_at_line_number_{1}_and_bytecode_offset_{2}_16")));
		OPTIMIZED_RETURN = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_upcast1_in_method_{0}_at_bytecode_offset_{1}_17")));
		OPTIMIZED_RETURN_SIMPLE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.upcast1__{0}_18")));
		OPTIMIZED_METHOD_AT_LINE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_method_invocation_of_method_{3}_in_method_{0}_at_line_number_{1}_and_bytecode_offset_{2}_19")));
		OPTIMIZED_METHOD = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_method_invocation_of_method_{2}_in_method_{0}_at_bytecode_offset_{1}_20")));
		OPTIMIZED_METHOD_SIMPLE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.upcast2__{0}_21")));
		OPTIMIZED_FIELD_AT_LINE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_access_to_field_{3}_in_method_{0}_at_line_number_{1}_and_bytecode_offset_{2}_28")));
		OPTIMIZED_FIELD = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.startupPerformance.Inserted_checkcast_for_access_to_field_{2}_in_method_{0}_at_bytecode_offset_{1}_29")));
		
	}
	
	
	
	final LogMessage 
		OPTIMIZED_THROW_AT_LINE,
		OPTIMIZED_THROW,
		OPTIMIZED_THROW_SIMPLE,
		CLAZZ_SIMPLE,
		METHOD_SIMPLE,
		OPTIMIZED_CATCH,
		OPTIMIZED_CATCH_AT_LINE,
		OPTIMIZED_CATCH_SIMPLE,
		DONE,
		SUMMARY,
		OPTIMIZED_RETURN_AT_LINE,
		OPTIMIZED_RETURN,
		OPTIMIZED_RETURN_SIMPLE,
		OPTIMIZED_METHOD_AT_LINE,
		OPTIMIZED_METHOD,
		OPTIMIZED_METHOD_SIMPLE,
		OPTIMIZED_FIELD_AT_LINE,
		OPTIMIZED_FIELD;
		
	final String 
		OPTIMIZE_CATCHES_LABEL = "optimizeCatches",
		OPTIMIZE_THROWS_LABEL = "optimizeThrows",
		OPTIMIZE_UPCAST_LABEL = "optimizeTypeChecks";
	
	final String 
		OPTIMIZE_CATCHES = getString("com.ibm.ive.tools.japt.startupPerformance.insert_checkcasts_for_catch_types_2"),
		OPTIMIZE_THROWS = getString("com.ibm.ive.tools.japt.startupPerformance.insert_checkcasts_for_throw_instructions_3"),
		OPTIMIZE_UPCASTS = getString("com.ibm.ive.tools.japt.startupPerformance.insert_checkcasts_for_upcasts_15");
	
	String DESCRIPTION = getString("com.ibm.ive.tools.japt.startupPerformance.startup_performance_optimizer_1");
	
	
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
