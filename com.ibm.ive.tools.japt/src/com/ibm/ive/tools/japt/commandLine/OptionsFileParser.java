/*
 * Created on Mar 8, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine;

import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.Logger;

/**
 * @author sfoley
 *
 * This parser generally overrides several methods to make them specific
 * to parsing an options file as opposed to parsing from the command line.
 * <p>
 * The macros current.file and current.file.dir are introduced, some error messages
 * are made more specific and the extension options are added to the parent parser (if it exists).
 * <p>
 * Otherwise this class parses in the same way as the CommandLineParser class.
 */
public class OptionsFileParser extends CommandLineParser {

	File file;
	CommandLineParser parent;
	
	public OptionsFileParser(
			File sourceFile, 
			CommandLineMessages messages,
			String args[], 
			LinkedList visitedOptionsFiles, 
			Logger logger) {
		this(sourceFile, messages, args, visitedOptionsFiles, logger, new TreeMap());
	}
	
	public OptionsFileParser(
			File sourceFile, 
			CommandLineMessages messages,
			String args[], 
			LinkedList visitedOptionsFiles, 
			Logger logger,
			Map macros) {
		super(messages, args, visitedOptionsFiles, logger, macros);
		this.file = sourceFile;
	}
	
	public OptionsFileParser(CommandLineParser parent, 
			File sourceFile, 
			CommandLineMessages messages,
			String args[], 
			LinkedList visitedOptionsFiles, 
			Logger logger,
			Map macros) {
		this(sourceFile, messages, args, visitedOptionsFiles, logger, macros);
		this.parent = parent;
	}
	
	protected void reportUnknownString(String string, int commandLineIndex) {
		logMessage(messages.UNKNOWN_STRING_IN_FILE, new Object[] {string, file});
	}

	protected String getReplacement(String keyString) {
		//for a rules file or any other zip-embedded options file, 
		//current.file refers to the zip file and not the options file inside
		if(keyString.toLowerCase().endsWith("current.file.dir")) {
			return new File(file.getAbsolutePath()).getParent();
		}
		else if(keyString.toLowerCase().endsWith("current.file")) {
			return file.getAbsolutePath();
		}
		return super.getReplacement(keyString);	
	}

	protected void reportErrorReadingOptionsFile(String name) {
		logMessage(messages.INACCESSIBLE_OPTIONS, name + " (" + file + ")");
	}
	
	protected void reportUnknownOption(char switchCharacter, final String name, int commandLineIndex) {
		logMessage(messages.UNKNOWN_OPTION_IN_FILE, new Object[] {switchCharacter + name, file});
	}
	
	/**
	 * the parent method is overriden so that both the parent parser and this
	 * parser have the new option added
	 * @param option
	 */
	public void addExtensionOption(Option option) {
		if(parent != null) {
			parent.addExtensionOption(option);
		}
		super.addExtensionOption(option);
	}
	
	public String getSource() {
		return file.toString();
	}

}
