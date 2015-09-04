/*
 * Created on Sep 7, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

/**
 * An extension is integrated if it is intended to be invisible to the user.
 * Such an extension adds to the functionality of Japt or an existing extension,
 * when it is being referenced as an integrated extension.  As an integrated
 * extension it is not expected to be run independently on its own.
 * 
 * @author sfoley
 *
 */
public interface IntegratedExtension extends Extension {
	void setName(String name);
	
	void noteExecuting(Logger logger);
	
	void noteExecuted(Logger logger, String timeString);
}
