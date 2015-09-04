/*
 * Created on Jul 26, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_StackType.ClassType;

/**
 * 
 * @author sfoley
 *
 * Represents the contents of either a local variable or an element on the operand stack for a given java stack frame.
 */
public interface BT_FrameCell {
	/**
	 * @return the type that may be contained in the cell.  
	 */
	BT_StackType getCellType();
	
	/**
	 * @return the same as getCellType but throws ClassCastException if the cell type is not a ClassType.
	 */
	ClassType getClassType();
}
