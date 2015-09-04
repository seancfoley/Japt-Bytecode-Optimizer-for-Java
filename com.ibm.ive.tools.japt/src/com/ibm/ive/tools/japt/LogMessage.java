/*
 * Created on May 13, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

/**
 * Represents a message to be delivered to the user through a Japt logger.
 * 
 * @author sfoley
 *
 */
public abstract class LogMessage extends Message {

	/**
	 * @param string
	 */
	public LogMessage(FormattedString string) {
		super(string);
	}

	/**
	 * @param message
	 */
	public LogMessage(String message) {
		super(message);
	}

	/**
	 * @param components
	 */
	public LogMessage(String[] components) {
		super(components);
	}

	
	
	/**
	 * write the message using the given logger
	 */
	public void log(Logger logger) {
		log(logger, (Object[]) null);
	}
	
	/**
	 * write the message using the given logger with the given argument which will
	 * appear after the first message component
	 */
	public void log(Logger logger, Object argument) {
		log(logger, new Object[] {argument});
	}
	
	/**
	 * write the message using the given logger with the given arguments.
	 * Calls the final method logMessage.  Override this method for specialized
	 * message handling.  
	 */
	public void log(Logger logger, Object arguments[]) {
		logMessage(logger, arguments);
	}
	
	
	/**
	 * write the message using the given logger with the given arguments
	 */
	final void logMessage(Logger logger, Object arguments[]) {
		String[] components = getComponents();
		if(arguments != null) {
			for(int i=0; i<components.length && components[i] != null; i++) {
				outputObject(logger, components[i]);
				int argNum = getArgumentNumber(i);
				if(argNum >= 0 && argNum < arguments.length) {
					Object o = arguments[argNum];
					if(o == null) {
						outputObject(logger, "null");
					}
					else {
						outputObject(logger, arguments[argNum]);
					}
				}
			}
		}
		else {
			for(int i=0; i<components.length && components[i] != null; i++) {
				outputObject(logger, components[i]);
			}
		}
		
		outputObject(logger, Logger.endl);
		logger.flush();
	}
	
	/**
	 * output the message using the given logger.
	 */
	protected abstract void outputObject(Logger logger, Object object);

	
}
