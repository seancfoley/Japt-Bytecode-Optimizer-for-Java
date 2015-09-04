/*
 * Created on Dec 19, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public interface BT_ClassComparator {
	/**
	 * Compare the two classes to determine
	 * the relative ordering. 
	 *
	 * @param class1 a class to compare
	 * @param class2 a class to compare
	 * @return an int < 0 if object1 is less than object2,
	 *	0 if they are equal, and > 0 if object1 is greater
	 */
	public int compare(BT_Class class1, BT_Class class2);
}
