/*
 * Created on Jun 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reduction;

import com.ibm.jikesbt.BT_Class;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface ClassSet {
	void add(BT_Class clazz);
	
	boolean contains(BT_Class clazz);
	
	boolean isEmpty();
	
	ClassIterator iterator();
	
	void removeAll();
	
	int size();
	
	interface ClassIterator {
		boolean hasNext();

		BT_Class next();
	}
}
