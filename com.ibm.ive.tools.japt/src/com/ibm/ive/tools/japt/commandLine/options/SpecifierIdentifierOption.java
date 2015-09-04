/*
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine.options;

import java.util.LinkedList;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.CommandLineParser;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.OptionException;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.SpecifierIdentifierPair;
import com.ibm.ive.tools.japt.commandLine.OptionsParser;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SpecifierIdentifierOption extends Option {

	private LinkedList list = new LinkedList();
	
	/**
	 * determines whether indicated specifiers are resolvable
	 */
	public boolean resolvableSpecifier = true;

	/**
	 * determines whether indicated identifiers are resolvable
	 */
	public boolean resolvableIdentifier = true;

	/**
	 * @param name
	 * @param description
	 * @param argCount
	 */
	public SpecifierIdentifierOption(String name, String description) {
		super(name, description, 2);
	}

	/**
	 * @param name
	 * @param argCount
	 */
	public SpecifierIdentifierOption(String name) {
		super(name, 2);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#appears()
	 */
	public boolean appears() {
		return list.size() > 0;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#handle(java.lang.String[], com.ibm.ive.tools.commandLine.CommandLineParser)
	 */
	protected void handle(String[] args, CommandLineParser fromParser)
			throws CommandLineException {
		if(args.length < 2) {
			throw new OptionException(this);
		}
		OptionsParser optionsParser = (OptionsParser) fromParser;
		add(args[0], args[1], optionsParser.getSource(), resolvableSpecifier, resolvableIdentifier);

	}

	public void add(String s1, String s2, String from, boolean resolvableSpecifier, boolean resolvableIdentifier) {
		Specifier specifier = new Specifier(s1, from, resolvableSpecifier);
		Identifier identifier = new Identifier(s2, from, resolvableIdentifier);
		add(specifier, identifier);
	}
	
	public void add(Specifier spec, Identifier ident) {
		list.add(new SpecifierIdentifierPair(spec, ident));
	}
	
	public SpecifierIdentifierPair[] get() {
		return (SpecifierIdentifierPair[]) list.toArray(new SpecifierIdentifierPair[list.size()]);
	}
}
