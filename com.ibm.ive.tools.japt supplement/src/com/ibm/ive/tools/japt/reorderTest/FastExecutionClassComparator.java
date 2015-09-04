/*
 * Created on Dec 19, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reorderTest;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassComparator;

public class FastExecutionClassComparator implements BT_ClassComparator {
	public int compare(BT_Class one, BT_Class two) {
		return SlowExecutionClassComparator.getCallIndex(one) - SlowExecutionClassComparator.getCallIndex(two);
	}
	
	
}
