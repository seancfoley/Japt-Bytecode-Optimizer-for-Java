/*
 * Created on May 20, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine.options;

import java.util.LinkedList;
import java.util.StringTokenizer;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.commandLine.OptionsParser;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SpecifierOption extends Option {

	private LinkedList specifierList = new LinkedList();
	private SpecifierOption dual;
	
	/**
	 * determines whether the indicated specifiers are resolvable
	 */
	public boolean resolvable = true;
	
	private boolean delimited = true;
	
	/**
	 * @param name
	 * @param description
	 * @param argCount
	 */
	public SpecifierOption(String name, String description) {
		super(name, description, 1);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#appears()
	 */
	public boolean appears() {
		return specifierList.size() > 0;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#handle(java.lang.String[])
	 */
	protected void handle(String[] args, com.ibm.ive.tools.commandLine.CommandLineParser fromParser) throws CommandLineException {
		if(args.length < 1) {
			return;
		}
		OptionsParser optionsParser = (OptionsParser) fromParser;
		String from = optionsParser.getSource();
		if(delimited) {
			StringTokenizer tokenizer = new StringTokenizer(args[0], ",");
			while(tokenizer.hasMoreTokens()) {
				String s = tokenizer.nextToken().trim();
				add(s, from, resolvable);
			}
		}
		else {
			add(args[0], from, resolvable);
		}
	}
	
	public void add(Specifier spec) {
		specifierList.add(spec);
		if(dual != null) {
			if(dual.specifierList.contains(spec)) {
				dual.specifierList.remove(spec);
			}
		}
	}
	
	public void add(Identifier ident) {
		add(new Specifier(ident));
	}
	
	public void add(Identifier idents[]) {
		for(int i=0; i<idents.length; i++) {
			add(idents[i]);
		}
	}
	
	public void add(String s, String from, boolean resolvable) {
		add(new Specifier(s, from, resolvable));
	}
	
	
	public Specifier[] getSpecifiers() {
		return (Specifier[]) specifierList.toArray(new Specifier[specifierList.size()]);
	}
	
	/**
	 * determines whether values can be delimited by a comma, eliminating the 
	 * need for multiple appearances of this option.
	 * The default is true.
	 * @param delimited
	 */
	public void setDelimited(boolean delimited) {
		this.delimited = delimited;
	}
	
	/**
	 * An element in this list will never appear (will be removed if necessary)
	 * from the dual list
	 */
	public void setDual(SpecifierOption dual) {
		this.dual = dual;
	}
}
