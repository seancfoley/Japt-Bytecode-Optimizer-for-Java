package com.ibm.ive.tools.japt.commandLine;

import java.util.Enumeration;
import java.util.Properties;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.MessageLogger;
import com.ibm.ive.tools.japt.coldMethod.ColdMethodExtension;
import com.ibm.ive.tools.japt.commandLine.options.ExtensionOption;
import com.ibm.ive.tools.japt.commandLine.options.MacroOption;
import com.ibm.ive.tools.japt.commandLine.options.SpecificExtensionOption;
import com.ibm.ive.tools.japt.devirtualization.DevirtualizerExtension;
import com.ibm.ive.tools.japt.inline.InlineExtension;
import com.ibm.ive.tools.japt.load.LoadExtension;
import com.ibm.ive.tools.japt.obfuscation.NameCompressionExtension;
import com.ibm.ive.tools.japt.out.ClassGenerationExtension;
import com.ibm.ive.tools.japt.out.JarGenerationExtension;
import com.ibm.ive.tools.japt.reduction.ReductionExtension;
import com.ibm.ive.tools.japt.refactorInner.RefactorExtension;
import com.ibm.ive.tools.japt.startupPerformance.StartupPerformanceExtension;

/**
 * @author sfoley
 *
 */
public class CommandLineData implements Component  {
	
	final public CommandLineMessages messages = new CommandLineMessages(this);
	
	
	final public MacroOption macro = new MacroOption(messages.MACRO_LABEL, messages.MACRO);
	final public ExtensionOption extensions = new ExtensionOption(messages.EXTENSION_LABEL, messages.EXTENSION, messages);
	final public ValueOption log = new ValueOption(messages.LOG_LABEL, messages.LOG);
	final public FlagOption noVerbose = new FlagOption(messages.NO_VERBOSE_LABEL, messages.NO_VERBOSE);
	final public FlagOption noResolveRuntime = new FlagOption(messages.NO_RESOLVE_LABEL, messages.NO_RESOLVE);
	
	//these four options are only used when specifically requested, since they are not typical options that can appear anywhere
	final public FlagOption systemProperties = new FlagOption(messages.SYS_PROP_LABEL, messages.SYS_PROP);
	final public FlagOption version = new FlagOption(messages.VERSION_LABEL, messages.VERSION);
	final public FlagOption help = new FlagOption(messages.HELP_LABEL, messages.HELP);
	final public FlagOption incrementalLoad = new FlagOption(messages.IC_LABEL, messages.IC);
	{incrementalLoad.setVisible(false);}
	
	final public FlagOption echo = new FlagOption("echo", "echoes the command line");
	{echo.setVisible(false);} //TODO echo should show what's in options files too
	
	final public SpecificExtensionOption load;
	final public SpecificExtensionOption inline;
	final public SpecificExtensionOption reduce;
	final public SpecificExtensionOption refactorInner;
	final public SpecificExtensionOption devirtualize;
	final public SpecificExtensionOption obfuscate;
	final public SpecificExtensionOption coldMethod;
	final public SpecificExtensionOption jarOutput;
	final public SpecificExtensionOption dirOutput;
	//final public SpecificExtensionOption classOrder;
	final public SpecificExtensionOption startup;
	
	final boolean enableExtensionShortcuts;
	
	public CommandLineData() {
		this(true);
	}
	
	public CommandLineData(boolean enableExtensionShortcuts) {
		this.enableExtensionShortcuts = enableExtensionShortcuts;
		String classString = LoadExtension.class.getName();
		load = new SpecificExtensionOption(messages.LOAD_LABEL, "", classString, extensions);
		if(enableExtensionShortcuts) {
			classString = JarGenerationExtension.class.getName();
			jarOutput = new SpecificExtensionOption(messages.JAR_OUTPUT_LABEL, "", classString, extensions);
			classString = ClassGenerationExtension.class.getName();
			dirOutput = new SpecificExtensionOption(messages.DIR_OUTPUT_LABEL, "", classString, extensions);
			
			classString = InlineExtension.class.getName();
			inline = new SpecificExtensionOption(messages.INLINE_LABEL, "", classString, extensions);
			classString = ReductionExtension.class.getName();
			reduce = new SpecificExtensionOption(messages.REDUCE_LABEL, "", classString, extensions);
			classString = RefactorExtension.class.getName();
			refactorInner = new SpecificExtensionOption(messages.REFACTORINNER_LABEL, "", classString, extensions);
			classString = DevirtualizerExtension.class.getName();
			devirtualize = new SpecificExtensionOption(messages.DEVIRTUALIZE_LABEL, "", classString, extensions);
			classString = NameCompressionExtension.class.getName();
			obfuscate = new SpecificExtensionOption(messages.OBFUSCATE_LABEL, "", classString, extensions);
			classString = StartupPerformanceExtension.class.getName();
			startup = new SpecificExtensionOption(messages.STARTUP_OUTPUT_LABEL, "", classString, extensions);
			classString = ColdMethodExtension.class.getName();
			coldMethod = new SpecificExtensionOption(messages.COLD_LABEL, "", classString, extensions);
			//classString = ClassOrderExtension.class.getName();
			//classOrder = new SpecificExtensionOption(messages.ORDER_OUTPUT_LABEL, "", classString, extensions);
		} else {
			inline = reduce = refactorInner = devirtualize = obfuscate = startup = coldMethod = null;
			jarOutput = dirOutput = null;
		}
	}
	
	public String getName() {
		return messages.DESCRIPTION;
	}
	
	Option[] getBasicOptions() {
		return new Option[] {
			macro,
			log, 
			noVerbose,
			noResolveRuntime,
			//noLoadMethods
		};
	}
	
	Option[] getExtensionOptions() {
		if(enableExtensionShortcuts) {
			return new Option[] {
				extensions,
				load,
				inline, 
				reduce,
				refactorInner,
				devirtualize,
				obfuscate,
				startup,
				coldMethod,
				dirOutput,
				//classOrder,
				jarOutput
			};
		} else {
			return new Option[] {
				extensions,
				load,
			};
		}
	}
	
	public void printLogo(Logger logger) {
		messages.LOGO_MESSAGE.log(logger);
	}
	
	public void printVersion(Logger logger, String programName, String version) {
		messages.PROGRAM_MESSAGE.log(logger, new String[] {programName, version});
	}
	
	public void printArgs(Logger logger, String programName, String args[]) {
		StringBuffer buffer = new StringBuffer(programName);
		for(int i=0; i<args.length; i++) {
			buffer.append(' ');
			buffer.append(args[i]);
		}
		messages.OPTION_MESSAGE.log(logger, buffer.toString());
	}
	
	public void printSystemProperties(Logger logger) {
		Properties properties = System.getProperties();
		Enumeration keys = properties.keys();

		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = properties.getProperty(key);

			if (key.equals("line.separator")) {
				StringBuffer newValue = new StringBuffer();
				char[] chars = value.toCharArray();
				for (int i=0; i<chars.length; i++) {
					switch(chars[i]) {
						case '\n': newValue.append("\\n");    break;
						case '\r': newValue.append("\\r");    break;
						default:   newValue.append(chars[i]); break;
					}
				}
				value = newValue.toString();
			}
			messages.PROPERTY_MESSAGE.log(logger, new String[] {key, value});
		}
	}
	
//	TODO make the help a little nicer - check to see if the executable
	//can run an outside extension (which it probably cannot), so
	//that means you should remove the -extension message
	
	
	void printUsage(final Logger logger, boolean loadOptions, ExtensionDescriptor exts[]) {
		messages.USAGE_MESSAGE.log(logger);
		messages.OPTION_MESSAGE.log(logger, help + getSpacer(help) + help.getDescription());
		messages.OPTION_MESSAGE.log(logger, version + getSpacer(version) + version.getDescription());
		messages.OPTION_MESSAGE.log(logger, systemProperties + getSpacer(systemProperties) + systemProperties.getDescription());
		messages.OPTION_MESSAGE.log(logger, log + getSpacer(log) + log.getDescription());
		messages.OPTION_MESSAGE.log(logger, noVerbose + getSpacer(noVerbose) + noVerbose.getDescription());
		messages.OPTION_MESSAGE.log(logger, macro + getSpacer(macro) + macro.getDescription());
		messages.OPTION_MESSAGE.log(logger, noResolveRuntime + getSpacer(noResolveRuntime) + noResolveRuntime.getDescription());
		messages.OPTION_MESSAGE.log(logger, incrementalLoad + getSpacer(incrementalLoad) + incrementalLoad.getDescription());
		messages.OPTION_MESSAGE.log(logger, messages.OPTIONS_FILE_LABEL + getSpacer(messages.OPTIONS_FILE_LABEL) + messages.OPTIONS_FILE);
		messages.SECTION_END.log(logger);
		
		if(loadOptions) {
			ExtensionDescriptor loadDescriptor = new ExtensionDescriptor(load.getExtensionName(), messages);
			printExtensionUsage(logger, loadDescriptor);
		}
		
		messages.EXTENSION_MESSAGE.log(logger, new Object[] {extensions + getSpacer(extensions) + extensions.getDescription(),
			extensions, CommandLineExtension.class.getName()});
		messages.SECTION_END.log(logger);
		
		if(this.enableExtensionShortcuts) {
			messages.ABBREVIATION_MESSAGE.log(logger);
			messages.OPTION_MESSAGE.log(logger, inline + getSpacer(inline) + inline.getDescription());
			messages.OPTION_MESSAGE.log(logger, reduce + getSpacer(reduce) + reduce.getDescription());
			messages.OPTION_MESSAGE.log(logger, refactorInner + getSpacer(refactorInner) + refactorInner.getDescription());
			messages.OPTION_MESSAGE.log(logger, devirtualize + getSpacer(devirtualize) + devirtualize.getDescription());
			messages.OPTION_MESSAGE.log(logger, obfuscate + getSpacer(obfuscate) + obfuscate.getDescription());
			messages.OPTION_MESSAGE.log(logger, startup + getSpacer(startup) + startup.getDescription());
			messages.OPTION_MESSAGE.log(logger, coldMethod + getSpacer(coldMethod) + coldMethod.getDescription());
			messages.OPTION_MESSAGE.log(logger, dirOutput + getSpacer(dirOutput) + dirOutput.getDescription());
			//messages.OPTION_MESSAGE.log(logger, classOrder + getSpacer(classOrder) + classOrder.getDescription());
			messages.OPTION_MESSAGE.log(logger, jarOutput + getSpacer(jarOutput) + jarOutput.getDescription());
			messages.SECTION_END.log(logger);
		
			messages.EXTENSION_HELP_MESSAGE.log(logger, new Object[] {extensions, help, inline, help});
			messages.SECTION_END.log(logger);
			
		}
		
		
		
		
		
		for(int i=0; i<exts.length; i++) {
			ExtensionDescriptor desc = exts[i];
			printExtensionUsage(logger, desc);
		}
	}
	
	
	
	/**
	 * @param logger
	 * @param desc
	 * @param ext
	 */
	public void printExtensionUsage(final Logger logger, ExtensionDescriptor desc) {
		CommandLineExtension ext = desc.instance;
		
		if(ext == null) {
			MessageLogger mLogger = new MessageLogger() {
				public void logMessage(LogMessage message, Object arg) {
					message.log(logger, arg);
				}
				
				public void logMessage(LogMessage message, Object args[]) {
					message.log(logger, args);
				}
			};
			ext = desc.createExtension(mLogger);
		}
		
		if(ext != null) {
			Option options[] = ext.getOptions();
			boolean hasVisibleOptions = false;
			for(int j=0; options != null && j<options.length; j++) {
				if(options[j].isVisible()) {
					hasVisibleOptions = true;
					break;
				}
			}
		 	if(!hasVisibleOptions) {
		 		messages.OPTIONS_MESSAGE.log(logger, ext.getName());
				messages.NO_OPTIONS_MESSAGE.log(logger);
			}
			else {
				messages.OPTIONS_MESSAGE.log(logger, ext.getName());
				for(int j=0; j<options.length; j++) {
					Option option = options[j];
					if(option.isVisible()) {
						messages.OPTION_MESSAGE.log(logger, option + getSpacer(option) + option.getDescription());
					}
				}
			}
		 	messages.SECTION_END.log(logger);
		}
	}

	public String getSpacer(Object object) {
		String first = object.toString();
		String s28spaces = "                            ";
		int len = first.length();
		if(len <= 27) {
			String res = s28spaces.substring(len);
			return res;
		}
		return "\n" + CommandLineMessages.usageIndent + s28spaces;
	}
	
	
	
}
