/*
 * Created on Jun 13, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;


/**
 * Represents an element of a java method's local variable array, 
 * including both the method arguments and additional local variable required by the bytecode.
 * 
 * The simplest implementation of a local cell is the BT_StackType object occupying that cell.
 * But more advanced implementations can store additional information related to that cell.
 */
public interface BT_LocalCell extends BT_FrameCell {
}
