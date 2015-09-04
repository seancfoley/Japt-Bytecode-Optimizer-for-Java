/*
 * Created on Dec 4, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class NullLogger implements Logger {

	public NullLogger() {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Logger#logInfo(java.lang.String)
	 */
	public void logInfo(String info) {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Logger#logStatus(java.lang.String)
	 */
	public void logStatus(String status) {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Logger#logWarning(java.lang.String)
	 */
	public void logWarning(String warning) {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Logger#logError(java.lang.String)
	 */
	public void logError(String error) {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Logger#logProgress(java.lang.String)
	 */
	public void logProgress(String progress) {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Logger#flush()
	 */
	public void flush() {}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Logger#close()
	 */
	public void close() {}
	
	
}
