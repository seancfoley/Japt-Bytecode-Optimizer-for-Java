package com.ibm.ive.tools.japt.commandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.Logger;


/**
 *
 * A command line parser for japt.
 * <p>
 * Stores options added by extensions and has the functionality for parsing options files.
 * <p>
 * @author sfoley
 */
public class CommandLineParser extends OptionsParser {
	ArrayList extensionOptions = new ArrayList(); //contains Option
	private LinkedList visitedOptionsFiles;
	
	public CommandLineParser(CommandLineMessages messages, String args[], Logger logger) {
		super(args, messages, logger);
		visitedOptionsFiles = new LinkedList();
	}
	
	CommandLineParser(CommandLineMessages messages, String args[], LinkedList visitedOptionsFiles, Logger logger, Map macros) {
		super(args, macros, messages, logger);
		this.visitedOptionsFiles = visitedOptionsFiles;
	}
	
	public void addExtensionOption(Option option) {
		addOption(option);
		extensionOptions.add(option);
	}
	
	public void readOptionsFile(File file, String args[], Option initialOptions[]) throws CommandLineException {
		if(visitedOptionsFiles.contains(file)) {
			return;
		}
		visitedOptionsFiles.add(file);
		OptionsFileParser parser = new OptionsFileParser(this, file, messages, args, visitedOptionsFiles, logger, macros);
		parser.addInitialOptions(initialOptions);
		parser.addOptions(extensionOptions);
		parser.extensionOptions.addAll(extensionOptions);
		parser.filterArgs();
		parser.parseCommandLine();
	}
}
