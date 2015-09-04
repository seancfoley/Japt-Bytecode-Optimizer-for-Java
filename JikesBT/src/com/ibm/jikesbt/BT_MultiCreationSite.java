/*
 * Created on May 31, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public class BT_MultiCreationSite extends BT_CreationSite {
	BT_Class target;
	
	BT_MultiCreationSite(BT_CodeAttribute creator, BT_MultiANewArrayIns in1, BT_Class targetClass) {
		//with the multianewarray instruction, while we are creating the main array object,
		//we are simultaneously creating objects for each lower dimension
		super(creator, in1);
		this.target = targetClass;
		//note for BT_MultiANewArrayIns, it is not necessarily true that targetClass == in1.getTarget() 
		//since a single instruction creates numerous new objects of different array class types,
		//so a single instruction is the creation site of instances of various class types
	}
	
	public BT_Class getTarget() {
		return target;
	}
}
