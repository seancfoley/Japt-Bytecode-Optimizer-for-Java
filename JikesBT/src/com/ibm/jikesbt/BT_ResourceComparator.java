/*
 * Created on Dec 19, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public interface BT_ResourceComparator {
	/**
	 * Compare the two resources to determine
	 * the relative ordering. 
	 *
	 * @param resource1 a resource to compare
	 * @param resource2 a resource to compare
	 * @return an int < 0 if resource1 is less than resource2,
	 *	0 if they are equal, and > 0 if resource1 is greater
	 */
	public int compare(BT_JarResource resource1, BT_JarResource resource2);
}
