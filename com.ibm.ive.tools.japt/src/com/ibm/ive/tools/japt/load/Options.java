/*
 * Created on Sep 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.load;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.ListOption;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ArchiveExtensionList;
import com.ibm.ive.tools.japt.commandLine.options.IdentifierOption;
import com.ibm.ive.tools.japt.commandLine.options.SingleIdentifierOption;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Options {
	
	Messages messages;
	
	final public ArchiveExtensionList archiveExtensions = new ArchiveExtensionList();
	
	final public SpecifierOption includeClass;
	final public SpecifierOption includeLibraryClass;
	final public SpecifierOption includeAccessibleClass;
	final public SpecifierOption includeWholeClass;
	final public SpecifierOption includeMainMethod;
	
	
	final public SpecifierOption includeMethod;
	final public SpecifierOption includeField;
	final public SpecifierOption includeMethodEx;
	
	final public SpecifierOption loadResource;
	final public SpecifierOption loadClass;
	
	final public SpecifierOption includeExtendedLibraryClass;
	final public SpecifierOption includeExtendedAccessibleClass;
	
	
	/*
	 * options added for support for jxelink but considered deprecated
	 */
	final public SpecifierOption includeSubclass;
	final public SpecifierOption includeResource;

	final public SingleIdentifierOption mainClass;
	
	final public ClassPathOption externalClassPathList;
	final public ClassPathOption internalClassPathList;
	final public ClassPathOption load;
	final public ClassPathOption loadAll;
	final public ClassPathOption externalClassPathAll;
	final public ClassPathOption internalClassPathAll;
	final public ClassPathOption jreClassPath;
	
	final public FlagOption verify;
	final public IdentifierOption printClass;
	final public IdentifierOption printMethod;
	final public FlagOption optimize;
	final public FlagOption includeSerialized;
	final public FlagOption includeSerializable;
	final public FlagOption includeExternalized;
	final public FlagOption includeDynamicClassLoad;
	final public FlagOption reflectionWarnings;
	final public FlagOption noBuiltInRules;
	final public FlagOption builtInRules;
	final public FlagOption resetClassPath;
	final public ValueOption refTree;
	final public FlagOption noFollow; //See the japt docs for details on the effects of -noFollow
	final public ValueOption unresolvedReferenceFile;
	final public ValueOption referenceFile;
	
	final public FlagOption noDebug;
	final public FlagOption noTrackClasses;
	final public FlagOption noReadStackMaps;
			
	
	final public ListOption fileExtension;
	
	/**
	 * 
	 */
	public Options(Messages messages) {
		this.messages = messages;
		
		includeClass = new SpecifierOption(messages.INCLUDE_CLASS_LABEL, messages.INCLUDE_CLASS);
		includeLibraryClass = new SpecifierOption(messages.INCLUDE_LIBRARY_CLASS_LABEL, messages.INCLUDE_LIBRARY_CLASS);
		includeAccessibleClass = new SpecifierOption(messages.INCLUDE_ACCESSIBLE_CLASS_LABEL, messages.INCLUDE_ACCESSIBLE_CLASS);
		includeWholeClass = new SpecifierOption(messages.INCLUDE_WHOLE_CLASS_LABEL, messages.INCLUDE_WHOLE_CLASS);
		includeMainMethod = new SpecifierOption(messages.INCLUDE_MAIN_METHOD_LABEL, messages.INCLUDE_MAIN_METHOD);
		
		includeMethod = new SpecifierOption(messages.INCLUDE_METHOD_LABEL, messages.INCLUDE_METHOD);
		includeField = new SpecifierOption(messages.INCLUDE_FIELD_LABEL, messages.INCLUDE_FIELD);
		includeMethodEx = new SpecifierOption(messages.INCLUDE_METHODEX_LABEL, messages.INCLUDE_METHODEX);
		
		includeExtendedLibraryClass = new SpecifierOption(messages.INCLUDE_EXT_LIBRARY_LABEL, messages.INCLUDE_EXT_LIBRARY);
		includeExtendedAccessibleClass = new SpecifierOption(messages.INCLUDE_EXT_ACCESSIBLE_LABEL, messages.INCLUDE_EXT_ACCESSIBLE);
		loadResource = new SpecifierOption(messages.LOAD_RESOURCE_LABEL, messages.LOAD_RESOURCE);
		loadResource.setDelimited(false);
		loadClass = new SpecifierOption(messages.LOAD_CLASS_LABEL, messages.LOAD_CLASS);
		includeSubclass = new SpecifierOption(messages.INCLUDE_SUBCLASS_LABEL, messages.INCLUDE_SUBCLASS);
		includeSubclass.setVisible(false);
		includeResource = new SpecifierOption(messages.INCLUDE_RESOURCE_LABEL, messages.LOAD_RESOURCE);
		includeResource.setVisible(false);
		includeResource.setDelimited(false);
		noBuiltInRules = new FlagOption(messages.NO_BUILT_IN_RULES_LABEL, messages.NO_BUILT_IN_RULES);
		externalClassPathList = new ClassPathOption(messages.CP_LABEL, messages.CP, 1, noBuiltInRules, archiveExtensions);
		load = new ClassPathOption(messages.LOAD_LABEL, messages.LOAD, 1, noBuiltInRules, archiveExtensions);
		internalClassPathList = new ClassPathOption(messages.INTERNAL_CLASS_PATH_LABEL, messages.INTERNAL_CLASS_PATH, 1, noBuiltInRules, archiveExtensions);
		
		externalClassPathAll = new ClassPathOption(messages.CP_ALL_LABEL, messages.CP_ALL, 1, noBuiltInRules, archiveExtensions, ClassPathOption.RECURSIVE);
		loadAll = new ClassPathOption(messages.LOAD_ALL_LABEL, messages.LOAD_ALL, 1, noBuiltInRules, archiveExtensions, ClassPathOption.RECURSIVE);
		internalClassPathAll = new ClassPathOption(messages.INTERNAL_CLASS_ALL_LABEL, messages.INTERNAL_CLASS_ALL, 1, noBuiltInRules, archiveExtensions, ClassPathOption.RECURSIVE);
		jreClassPath = new ClassPathOption(messages.JRE_LABEL, messages.JRE, 1, noBuiltInRules, archiveExtensions, ClassPathOption.JRE);
		
		printClass = new IdentifierOption(messages.PRINT_CLASS_LABEL, messages.PRINT_CLASS);
		printMethod = new IdentifierOption(messages.PRINT_METHOD_LABEL, messages.PRINT_METHOD);
		verify = new FlagOption(messages.VERIFY_LABEL, messages.VERIFY);
		optimize = new FlagOption(messages.OPTIMIZE_LABEL, messages.OPTIMIZE);
		noFollow = new FlagOption(messages.NO_FOLLOW_LABEL, messages.NO_FOLLOW);
		
		includeSerializable = new FlagOption(messages.INCLUDE_SERIALIZABLE_LABEL, messages.INCLUDE_SERIALIZABLE);
		includeSerialized = new FlagOption(messages.INCLUDE_SERIALIZED_LABEL, messages.INCLUDE_SERIALIZED);
		includeExternalized = new FlagOption(messages.INCLUDE_EXTERNALIZED_LABEL, messages.INCLUDE_EXTERNALIZED);
		includeDynamicClassLoad = new FlagOption(messages.INCLUDE_DYNAMIC_CLASS_LABEL, messages.INCLUDE_DYNAMIC_CLASS);
		//includeSerialized.setVisible(false);
		//includeExternalized.setVisible(false);
		//includeDynamicClassLoad.setVisible(false);
		
		reflectionWarnings = new FlagOption(messages.REFLECTION_WARNINGS_LABEL, messages.REFLECTION_WARNINGS);
		resetClassPath = new FlagOption(messages.RESET_CP_LABEL, messages.RESET_CP);
		builtInRules = new FlagOption(messages.BUILT_IN_RULES_LABEL, messages.BUILT_IN_RULES);
		noBuiltInRules.setDual(builtInRules);
		builtInRules.setDual(noBuiltInRules);
		refTree = new ValueOption(messages.REFTREE_LABEL, messages.REFTREE);
		mainClass = new SingleIdentifierOption(messages.MAIN_CLASS_LABEL, messages.MAIN_CLASS);
		fileExtension = new ListOption(messages.FILE_EXTENSION_LABEL, messages.FILE_EXTENSION, 1) {
			public void add(String s) {
				super.add(s);
				archiveExtensions.addArchiveExtension(s);
			}
		};
		unresolvedReferenceFile = new ValueOption(messages.UNRESOLVED_REFERENCE_FILE_LABEL, messages.UNRESOLVED_REFERENCE_FILE);
		//unresolvedReferenceFile.setVisible(false);
		
		referenceFile = new ValueOption(messages.REFERENCE_FILE_LABEL, messages.REFERENCE_FILE);
		
		noDebug = new FlagOption("noLoadDebug");
		noDebug.setVisible(false);
		noTrackClasses = new FlagOption("noTrackClasses");
		noTrackClasses.setVisible(false);
		noReadStackMaps = new FlagOption("noReadStackMaps");
		noReadStackMaps.setVisible(false);
	}

}
