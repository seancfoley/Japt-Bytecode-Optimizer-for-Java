/*
 * Created on Oct 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.instrument;

/**
 * @author sfoley
 *
 * A method observer observes an individual method.
 */
public interface MethodObserver {
	
	/**
	 * called whenever the method represent by this
	 * object is invoked.  For non-static 
	 * non-contructor methods the o argument will be 
	 * the corresponding object instance.
	 * For static and constructor methods o is null.
	 * @param o the corresponding object instance
	 */
	void observeEntry(Object o);
}
