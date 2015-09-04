package com.ibm.ive.tools.commandLine;

import java.util.LinkedList;

/**
 * @author sfoley
 * <p>
 * Handles options which might occur multiple times, each time with argument(s).
 */
public class ListOption extends ArgumentOption {

	private static String nullStrings[] = new String[0];
	private LinkedList stringList;
	private ListOption dual;
	
	/**
	 * Constructor for ListOption.
	 * @param name
	 * @param argCount
	 */
	public ListOption(String name, String description, int argCount) {
		super(name, description, argCount);
	}
	
	public ListOption(String name, int argCount) {
		super(name, argCount);
	}
	
	/**
	 * Handles an appearance on the command line of this option.
	 */
	protected void handle(String args[], CommandLineParser fromParser) throws CommandLineException {
		int count = Math.min(argCount, args.length);
		for(int i=0; i<count; i++) {
			add(args[i]);
		}
	}
	
	public void add(String s) {
		if(stringList == null) {
			stringList = new LinkedList();
		}
		stringList.add(s);
		if(dual != null) {
			if(dual.stringList != null && dual.stringList.contains(s)) {
				dual.stringList.remove(s);
			}
		}
	}
	
	public void add(String s[]) {
		for(int i=0; i<s.length; i++) {
			add(s[i]);
		}
	}
	
	/**
	 * An element in this list will never appear (will be removed if necessary)
	 * from the dual list
	 */
	public void setDual(ListOption dual) {
		this.dual = dual;
	}
	
	public String getString(int index) {
		if(stringList == null) {
			throw new IndexOutOfBoundsException();
		}
		return (String) stringList.get(index);
	}
	
	public boolean contains(String s) {
		return stringList != null && stringList.contains(s);
	}
	
	public String[] getStrings() {
		return stringList == null ? nullStrings : (String[]) stringList.toArray(new String[stringList.size()]);
	}
	
	public boolean appears() {
		return stringList != null && stringList.size() > 0;
	}

}
