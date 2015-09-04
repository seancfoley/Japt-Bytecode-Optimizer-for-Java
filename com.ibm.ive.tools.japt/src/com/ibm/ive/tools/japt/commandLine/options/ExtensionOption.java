package com.ibm.ive.tools.japt.commandLine.options;

import java.util.LinkedList;
import java.util.List;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.commandLine.CommandLineMessages;
import com.ibm.ive.tools.japt.commandLine.CommandLineParser;
import com.ibm.ive.tools.japt.commandLine.ExtensionDescriptor;

/**
 * @author sfoley
 *
 */
public class ExtensionOption extends Option {

	private List extensionList = new LinkedList();
	private CommandLineMessages messages;
	private boolean create;
	
	public ExtensionOption(String label, String description, CommandLineMessages messages) {
		super(label, description, 1);
		this.messages = messages;
	}
	
	public ExtensionDescriptor[] getExtensions() {
		return (ExtensionDescriptor[]) extensionList.toArray(new ExtensionDescriptor[extensionList.size()]);
	}
	
	public boolean appears() {
		return extensionList.size() > 0;
	}
	
	public void setCreateExtensions(boolean create) {
		this.create = create;
	}
	
	public void reset() {
		extensionList.clear();
	}
	
	public void handle(String args[], com.ibm.ive.tools.commandLine.CommandLineParser fromParser) throws CommandLineException {
		if(args.length < 1) {
			return;
		}
		handleExtension(args[0], fromParser);
	}

	/**
	 * @param fromParser
	 * @param className
	 */
	public ExtensionDescriptor handleExtension(String className, com.ibm.ive.tools.commandLine.CommandLineParser fromParser) {
		ExtensionDescriptor descriptor = new ExtensionDescriptor(className, messages);
		if(create) {
			descriptor.createParserExtension((CommandLineParser) fromParser);
		}
		extensionList.add(descriptor);
		return descriptor;
	}

}
