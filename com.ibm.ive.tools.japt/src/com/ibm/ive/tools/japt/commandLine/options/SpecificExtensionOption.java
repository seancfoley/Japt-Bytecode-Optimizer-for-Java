/*
 * Created on Apr 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine.options;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.commandLine.ExtensionDescriptor;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SpecificExtensionOption extends Option {

	private boolean appears;
	private ExtensionOption extensionOption;
	private String extensionName;
	
	/**
	 * @param name
	 * @param description
	 * @param argCount
	 */
	public SpecificExtensionOption(
		String name,
		String description,
		String extensionName,
		ExtensionOption extensionOption) {
		super(name, description, 0);
		this.extensionOption = extensionOption;
		this.extensionName = extensionName;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#handle(java.lang.String[])
	 */
	public void handle(String[] args, com.ibm.ive.tools.commandLine.CommandLineParser fromParser) throws CommandLineException {
		handle(fromParser);
	}
	
	/**
	 * @param fromParser
	 */
	public ExtensionDescriptor handle(com.ibm.ive.tools.commandLine.CommandLineParser fromParser) {
		appears = true;
		return extensionOption.handleExtension(extensionName, fromParser);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#appears()
	 */
	public boolean appears() {
		return appears;
	}
	
	public String getExtensionName() {
		return this.extensionName;
	}

}
