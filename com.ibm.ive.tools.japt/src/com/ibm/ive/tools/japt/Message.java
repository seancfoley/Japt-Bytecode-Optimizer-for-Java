package com.ibm.ive.tools.japt;


/**
 * Represents a message to be delivered from japt to the japt user.
 * @author sfoley
 */
public class Message {
	
	FormattedString formattedString;
	
	public Message(FormattedString string) {
		this.formattedString = string;
	}
	
	/**
	 * Constructs a message with a single component.  The message may have either zero or one argument, 
	 * with the sole argument appearing after the single component.
	 * @param message the message
	 */
	public Message(String message) {
		this(new String[] {message});
	}
	
	/**
	 * Constructs a message.  A message may take arguments that must appear in-between components
	 * or after the last component.
	 * @param components the message components
	 */
	public Message(String components[]) {
		this(new FormattedString(components));
	}
	
	/**
	 * @return the components of the message
	 */
	public String[] getComponents() {
		return formattedString.getComponents();
	}
	
	int getArgumentNumber(int index) {
		return formattedString.getArgumentNumber(index);
	}
	
	public String toString(Object argument) {
		return toString(new Object[] {argument});
	}
	
	/**
	 * a representation of the message as it would appear when being logged
	 */
	public String toString(Object arguments[]) {
		StringBuffer buffer = new StringBuffer();
		String[] components = getComponents();
		if(arguments != null) {
			for(int i=0; i<components.length; i++) {
				buffer.append(components[i]);
				int argNum = getArgumentNumber(i);
				if(argNum >= 0 && argNum < arguments.length) {
					buffer.append(arguments[argNum]);
				}
			}
		}
		else {
			for(int i=0; i<components.length; i++) {
				buffer.append(components[i]);
			}
		}
		return buffer.toString();
	}
	
	/**
	 * a representation of the message with no arguments
	 */
	public String toString() {
		return toString(null);
	}
	
	/**
	 * Capitalize the first letter in a string
	 */
	public static String capitalizeFirst(String s) {
		if(s.length() > 0) {
			return Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}
		return s;
	}
	
}
