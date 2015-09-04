/*
 * Created on Nov 24, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.commandLine;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class NullOption extends Option {

	public static final NullOption nullOption = new NullOption();
	
	public NullOption() {
		this("");
	}
	
	/**
	 * @param name
	 * @param argCount
	 */
	public NullOption(String name) {
		super(name, 0);
	}
	
	/**
	 * @param name
	 * @param description
	 */
	public NullOption(String name, String description) {
		super(name, description, 0);
	}
	
	public NullOption(String name, String description, char switchCharacter) {
		super(name, description, 0, switchCharacter);
	}
	
	public NullOption(String name, String description, char switchCharacters[]) {
		super(name, description, 0, switchCharacters);
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#appears()
	 */
	public boolean appears() {
		return false;
	}
	
	public String getAppearance() {
		return "";
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.commandLine.Option#handle(java.lang.String[], com.ibm.ive.tools.commandLine.CommandLineParser)
	 */
	protected void handle(String[] args, CommandLineParser fromParser)
			throws CommandLineException {}

}
