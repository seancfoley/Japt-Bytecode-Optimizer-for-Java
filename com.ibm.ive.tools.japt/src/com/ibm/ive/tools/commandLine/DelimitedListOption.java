/*
 * Created on Mar 2, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.commandLine;

import java.util.StringTokenizer;

public class DelimitedListOption extends ListOption {
	private String delimiters;
	
	public DelimitedListOption(String name, String description, int argCount, String delimiters) {
		super(name, description, argCount);
		this.delimiters = delimiters;
	}
	
	public DelimitedListOption(String name, int argCount, String delimiters) {
		super(name, argCount);
		this.delimiters = delimiters;
	}
	
	public DelimitedListOption(String name, String description, int argCount) {
		this(name, description, argCount, ",");
	}
	
	public DelimitedListOption(String name, int argCount) {
		this(name, argCount, ",");
	}
	
	/**
	 * Handles an appearance on the command line of this option.
	 */
	protected void handle(String args[], CommandLineParser fromParser) throws CommandLineException {
		int count = Math.min(argCount, args.length);
		for(int i=0; i<count; i++) {
			StringTokenizer tokenizer = new StringTokenizer(args[i], delimiters);
			while(tokenizer.hasMoreTokens()) {
				String s = tokenizer.nextToken().trim();
				add(s);
			}
		}
	}
}
