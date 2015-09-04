/*
 * Created on Aug 15, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public interface BT_AttributeOwner {
	BT_Item getEnclosingItem();
	
	BT_AttributeVector getAttributes();
	
	String useName();
}
