package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.jikesbt.BT_CodeAttribute;

/**
 * @author sfoley
 *
 */
public interface MethodPotentialTargets {
	void findNewTargets(com.ibm.jikesbt.BT_Class objectType);
	
	void findTargets(BT_CodeAttribute code);
}
