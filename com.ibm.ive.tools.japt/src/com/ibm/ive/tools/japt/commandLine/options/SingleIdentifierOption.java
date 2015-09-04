/*
 * Created on Nov 2, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine.options;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.CommandLineParser;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.commandLine.OptionsParser;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SingleIdentifierOption extends Option {

	private Identifier value;
	

	/**
	 * @param name
	 * @param description
	 * @param argCount
	 */
	public SingleIdentifierOption(String name, String description) {
		super(name, description, 1);
	}

	/**
	 * @param name
	 * @param argCount
	 */
	public SingleIdentifierOption(String name) {
		super(name, 1);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#appears()
	 */
	public boolean appears() {
		return value != null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#handle(java.lang.String[], com.ibm.ive.tools.commandLine.CommandLineParser)
	 */
	protected void handle(String[] args, CommandLineParser fromParser)
			throws CommandLineException {
		if(args.length < 1) {
			return;
		}
		OptionsParser optionsParser = (OptionsParser) fromParser;
		value = new Identifier(args[0], optionsParser.getSource());
	}
	
	public void set(Identifier identifier) {
		value = identifier;
	}
	
	public Identifier getIdentifier() {
		return value;
	}
	
}
