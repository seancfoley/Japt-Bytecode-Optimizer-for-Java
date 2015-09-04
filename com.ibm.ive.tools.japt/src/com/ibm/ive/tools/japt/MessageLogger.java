/*
 * Created on Mar 1, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

/**
 * @author sfoley
 *
 * An object that is capable of logging messages (other than a com.ibm.ive.tools.japt.Logger itself) implements this interface
 */
public interface MessageLogger {
	public void logMessage(LogMessage message, Object arg);
	
	public void logMessage(LogMessage message, Object args[]);
}
