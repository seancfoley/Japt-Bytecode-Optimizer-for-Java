package com.ibm.ive.tools.japt.inline;

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
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.inline.ExternalMessages"; //$NON-NLS-1$

	private ResourceBundle bundle = com.ibm.ive.tools.japt.MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	
	Messages(Component component) {
		INLINED_SPEC_METHOD = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Inlined_specified_method_{0}_at_{1}_of_{2}_call_sites_2")));
		INLINED_METHOD = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Inlined_method_{0}_at_{1}_of_{2}_possible_call_sites_3")));
		INLINED_METHOD_NO_SITES = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Method_{0}_had_no_call_sites_to_inline_4")));
		INLINED_METHOD_ONE_SITE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Inlined_method_{0}_at_its_only_call_site_5")));
		INLINED_METHOD_ALL_SITES = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Inlined_method_{0}_at_all_{1}_call_sites_6")));
		SPEC_METHOD_NOT_INLINABLE = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Method_{0}_cannot_be_inlined_7")));
		String newLine = BT_Factory.endl();
		SUMMARY = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Inlining_summary_8") + newLine + 
				getString("com.ibm.ive.tools.japt.inline.Inlined_{0}_methods_at_{1}_call_sites_9")));
		JSR_SUMMARY = new InfoMessage(component, new FormattedString(
				getString("com.ibm.ive.tools.japt.inline.Inlined_jsrs_in_{0}_methods_27")));
	}
	
	final LogMessage 
		INLINED_METHOD,
		INLINED_METHOD_NO_SITES,
		INLINED_METHOD_ONE_SITE,
		INLINED_METHOD_ALL_SITES,
		INLINED_SPEC_METHOD,
		SPEC_METHOD_NOT_INLINABLE,
		SUMMARY,
		JSR_SUMMARY;
		
	final String 
		INLINE_METHOD_LABEL = "inlineMethod",
		NO_INLINE_METHOD_LABEL = "noInlineMethod",
		PERF_INLINE_LABEL = "performanceInline",
		COMP_INLINE_LABEL = "compressionInline",
		EXPAND_PERMISSIONS_LABEL = "expandPermissions",
		ANYWHERE_INLINE_LABEL = "inlineFromAnywhere",
		ASSUME_UNKNOWN_LABEL = "assumeUnknownVirtuals",
		INLINE_JSRS_METHOD_LABEL = "inlineMethodJSRs",
		NO_INLINE_JSRS_METHOD_LABEL = "noInlineMethodJSRs",
		INLINE_ALL_JSRS_LABEL = "inlineAllJSRs";
	
	final String 
		INLINE_JSRS_METHOD = getString("com.ibm.ive.tools.japt.inline.inline_jsrs_in_named_method_23"),
		NO_INLINE_JSRS_METHOD = getString("com.ibm.ive.tools.japt.inline.do_not_inline_jsrs_in_named_method_24"),
		INLINE_ALL_JSRS = getString("com.ibm.ive.tools.japt.inline.inline_all_jsrs_25"),
		INLINE_METHOD = getString("com.ibm.ive.tools.japt.inline.inline_the_named_method_16"),
		NO_INLINE_METHOD = getString("com.ibm.ive.tools.japt.inline.do_not_inline_the_named_method_17"),
		PERF_INLINE = getString("com.ibm.ive.tools.japt.inline.inline_for_performance_18"),
		COMP_INLINE = getString("com.ibm.ive.tools.japt.inline.inline_for_compression_19"),
		EXPAND_PERMISSIONS = getString("com.ibm.ive.tools.japt.inline.allow_changed_permissions_for_increased_access_20"),
		ANYWHERE_INLINE = getString("com.ibm.ive.tools.japt.inline.inline_from_anywhere_on_the_classpath_21"),
		ASSUME_UNKNOWN = getString("com.ibm.ive.tools.japt.inline.assume_virtual_method_calls_have_unknown_targets_22");

	String DESCRIPTION = getString("com.ibm.ive.tools.japt.inline.inline_29");
	
	
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
