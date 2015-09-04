/*
 * Created on Mar 9, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.CommandLineParser;
import com.ibm.ive.tools.commandLine.NullOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.OptionException;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.LogMessage;
import com.ibm.ive.tools.japt.MessageLogger;

/**
 * @author sfoley
 *
 * A japt command line parser.
 * <p>
 * Has support for macros, parsing options files (reading these options is delegated to subclasses),
 * and reporting messages for error and warning conditions.
 */
public abstract class OptionsParser extends CommandLineParser implements OptionsFileReader, MessageLogger {

	public static char OPTIONS_FILE_SWITCH = '@';
	protected Map macros;
	public CommandLineMessages messages;
	protected Logger logger;
	protected Option initialOptions[] = emptyOptions;
	protected int vmOptionIndex = -1;
	private NullOption vmOption = new NullOption("vmOption", "");
	
	public OptionsParser(String args[], CommandLineMessages messages, Logger logger) {
		this(args, new HashMap(10), messages, logger);
	}

	public OptionsParser(
			String args[], 
			Map predefinedMacros, 
			CommandLineMessages messages,
			Logger logger) {
		super(args);
		this.macros = predefinedMacros;
		this.messages = messages;
		this.logger = logger;
	}
	
	public void addInitialOptions(Option options[]) {
		Option[] newInitialOptions = new Option[initialOptions.length + options.length];
		System.arraycopy(initialOptions, 0, newInitialOptions, 0, initialOptions.length);
		System.arraycopy(options, 0, newInitialOptions, initialOptions.length, options.length);
		initialOptions = newInitialOptions;
		addOptions(options);
	}
	
	public boolean isOption(String string, int commandLineIndex) {
		//we must have this special handling for -vmOption because the argument will begin
		//with a '-' and therefore the parser will believe it is a command line option
		//as opposed to an argument to -vmOption
		return super.isOption(string, commandLineIndex) && commandLineIndex != vmOptionIndex;
	}
	
	
	public void filterArgs() {
		for(int i=0; i<args.length; i++) {
			String line = args[i];
			
			if(super.isSwitchCharacter(args[i].charAt(0))) {
				continue;
			}
			
			int currentIndex = 0;
			while(true) {
				int firstIndex = line.indexOf("{{", currentIndex);
				if(firstIndex == -1) {
					break;
				}
				int secondIndex = line.indexOf("}}", firstIndex + 3);
				if(secondIndex == -1) {
					break;
				}
				String replacement = getReplacement(line.substring(firstIndex + 2, secondIndex));
				if(replacement != null) {
					args[i] = line = line.substring(0, firstIndex) + replacement + line.substring(secondIndex + 2);
					currentIndex = firstIndex + replacement.length();
				}
				else {
					currentIndex = firstIndex + 2;
				}
			}
		}
	}
	
	protected void handleUnknownOption(char switchCharacter, String name, int commandLineIndex) throws CommandLineException {
		if(switchCharacter == OPTIONS_FILE_SWITCH) {
			readOptionsFile(name);
			return;
		}
		else if(vmOption.isSwitchCharacter(switchCharacter)
				&& vmOption.getName().equalsIgnoreCase(name)) {
			vmOptionIndex = commandLineIndex + 1;
			return;
		}
		reportUnknownOption(switchCharacter, name, commandLineIndex);
	}
	
	/**
	 * @param name
	 * @throws CommandLineException
	 */
	void readOptionsFile(String name) throws CommandLineException {
		try {
			Reader reader = new BufferedReader(new FileReader(name));
			String args[] = parseTokens(reader);
			if(args != null && args.length > 0) {
				readOptionsFile(new File(name), args, initialOptions);
			}
		}
		catch(IOException e) {
			reportErrorReadingOptionsFile(name);
		}
	}

	public static String[] parseTokens(Reader reader) throws IOException {
		
		StreamTokenizer tokenizer = new StreamTokenizer(reader);
		//tokenizer.resetSyntax();
		tokenizer.ordinaryChars(0x0, 0xffff);
		tokenizer.whitespaceChars(0x0, 0x20);
		//tokenizer.ordinaryChar('\\');
		tokenizer.wordChars(0x21, 0xffff);
		tokenizer.commentChar('#');
		tokenizer.eolIsSignificant(false);
		tokenizer.lowerCaseMode(false);
		tokenizer.quoteChar('"');
		tokenizer.nextToken();
		ArrayList stringList = new ArrayList();
		while(tokenizer.ttype != StreamTokenizer.TT_EOF) {
			stringList.add(tokenizer.sval);
			tokenizer.nextToken();
		}
		
		String result[] = (String []) stringList.toArray(new String[stringList.size()]);
		return result;
	}
	
	public void addMacro(String keyString, String value) {
		macros.put(keyString, value);
	}
		
	protected String getReplacement(String keyString) {
		String result = null;
		if(keyString.equals("/")) {
			result = File.separator;
		}
		else if("current.character.encoding".equals(keyString)) {
			result = new OutputStreamWriter(System.out).getEncoding();
			result = result.toUpperCase();
		}
		else {
			result = System.getProperty(keyString);
			if(result == null) {
				result = (String) macros.get(keyString);
			}
			else if(keyString.equals("file.encoding")) {
				result = result.toUpperCase();
			}
		}
		return result;
	}
	
	protected boolean isSwitchCharacter(char c) {
		return c == OPTIONS_FILE_SWITCH || super.isSwitchCharacter(c);
	}
	
	/**
	 * @return a string describing the source of the options being parsed by this parser, such as a file name, or null
	 */
	public String getSource() {
		return null;
	}
	
	/*
	 * Several methods report various error conditions.  We make them overridable so that subparsers
	 * can change their behaviours.
	 */
	
	public void reportOptionException(OptionException e) {
		messages.INVALID_OPTION.log(logger, e.getOption());
	}
	
	protected void handleString(String string, int commandLineIndex) throws CommandLineException {
		if(commandLineIndex == vmOptionIndex) {
			handleVMOption(string);
			return;
		}
		reportUnknownString(string, commandLineIndex);
	}
	
	/**
	 * to handle vm options, override this method, default behaviour is to do nothing
	 * @param option
	 */
	protected void handleVMOption(String option) {}
	
	protected void reportUnknownString(String string, int commandLineIndex) throws CommandLineException {
		messages.UNKNOWN_STRING.log(logger, string);
	}
	
	protected void reportUnknownOption(char switchCharacter, String name, int commandLineIndex) {
		messages.UNKNOWN_OPTION.log(logger, switchCharacter + name);
	}

	protected void reportErrorReadingOptionsFile(String name) {
		messages.INACCESSIBLE_OPTIONS.log(logger, name);
	}
	
	public void logMessage(LogMessage message, Object arg) {
		message.log(logger, arg);
	}
	
	public void logMessage(LogMessage message, Object args[]) {
		message.log(logger, args);
	}

}
