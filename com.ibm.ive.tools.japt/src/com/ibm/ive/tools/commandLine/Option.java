package com.ibm.ive.tools.commandLine;

import java.util.Arrays;

/**
 * @author sfoley
 * <p>
 * A generic option that may appear on the command line
 */
public abstract class Option {
	protected int argCount;
	protected String name;
	private String description;
	private char switchCharacters[];
	private String defaultArgs[];
	private boolean isVisible = true;
	private static final String noArgs[] = new String[0];
	
	/**
	 * create an option with the given name, the given description, the expected
	 * number of arguments to follow the option's appearance, 
	 * and the given switch character.
	 */
	public Option(String name, String description, int argCount, char switchCharacters[]) {
		this.name = name;
		this.argCount = argCount;
		this.description = description;
		this.switchCharacters = switchCharacters;
		if(argCount == 0) {
			defaultArgs = noArgs;
		}
		else {
			defaultArgs = new String[argCount];
			Arrays.fill(defaultArgs, getDefaultArg());
		}
	}
	
	public static String getDefaultArg() {
		return "xxx";
	}
	
	public Option(String name, String description, int argCount, char switchCharacter) {
		this(name, description, argCount, new char[] {switchCharacter});
	}
	
	public Option(String name, String description, int argCount) {
		this(name, description, argCount, new char[] {45, 8211});
	}
	
	public Option(String name, int argCount) {
		this(name, "", argCount);
	}
	
	/**
	 * Do not change the name after it has been provided to a parser.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * @return what an appearance on the command line looks like (without arguments)
	 * which in particular is the switch character followed by the name
	 */
	public String getAppearance() {
		return switchCharacters[0] + name;
	}
	
	/**
	 * The character returned by this method will be recognized as
	 * a switch character that denotes an option.
	 */
	public char[] getSwitchCharacters() {
		return switchCharacters;
	}
	
	/**
	 * the given character is one of the switch characters for this option
	 * @param c
	 * @return
	 */
	public boolean isSwitchCharacter(char c) {
		for(int i=0; i<switchCharacters.length; i++) {
			if(switchCharacters[i] == c) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @return what an appearance on the command line looks like with arguments.
	 * which in particular is the switch character followed by the name and then the given arguments
	 */
	public String toString(String providedArgs[]) {
		StringBuffer result = new StringBuffer(1 + name.length() + argCount * 4);
		result.append(getAppearance());
		for(int i=0; i<argCount; i++) {
			result.append(' ');
			result.append(providedArgs[i]);
		}
		return result.toString();
	}
	
	/**
	 * 
	 * @return what an appearance on the command line looks like with arguments.
	 * which in particular is the switch character followed by the name and then the given arguments
	 */
	public String toString(String providedArgs) {
		StringBuffer result = new StringBuffer(2 + name.length() + providedArgs.length());
		result.append(getAppearance());
		if(providedArgs.length() > 0) {
			result.append(' ');
			result.append(providedArgs);
		}
		return result.toString();
	}
	
	/**
	 * calls toString(defualtArgs).  The default arguments are strings of the form "xxx".
	 * The default arguments can be customized by calling setDefaultArg.
	 * @return
	 */
	public String toString() {
		return toString(defaultArgs);
	}
	
	/**
	 * sets the default arguments that appear in a call to toString().
	 * @param argIndex the argument index
	 * @param defaultArg the argument
	 */
	public void setDefaultArg(int argIndex, String defaultArg) {
		defaultArgs[argIndex] = defaultArg;
	}
	
	public void setDescription(String desc) {
		this.description = desc;
	}
	
	public String getDescription() {
		return description;
	}
	
	/**
	 * invisible options are valid but do not appear in a usage string, this is intended
	 * for deprecated or obsolete options
	 * @return
	 */
	public boolean isVisible() {
		return isVisible;
	}
	
	public void setVisible(boolean visible) {
		isVisible = visible;
	}
	
	/**
	 * 
	 * @return whether or not the option appears on the command line
	 */
	public abstract boolean appears();
	
	/**
	 * Handles an appearance on the command line of this option.  
	 * @throws CommandLineException the arguments to this option are invalid
	 */
	protected abstract void handle(String args[], CommandLineParser fromParser) throws CommandLineException;
	
}
