package com.ibm.ive.tools.japt.inline;


import java.util.HashMap;

import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_CodeVisitor;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Method;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class InliningCodeAttribute {

	private BT_CodeAttribute code;
	private HashMap instructionMap = new HashMap();
	
	InliningCodeAttribute(BT_CodeAttribute code) {
		if(code == null) {
			throw new NullPointerException();
		}
		this.code = code;
	}
	
	InliningCodeAttribute(BT_CodeAttribute code, BT_CodeAttribute originalCode) {
		this(code);
		BT_InsVector insVector = originalCode.getInstructions();
		BT_InsVector inlinedInstructionVector = code.getInstructions();
		
		for (int k = 0; k < insVector.size(); k++) {
			BT_Ins from = insVector.elementAt(k);
			BT_Ins to = inlinedInstructionVector.elementAt(k);
			instructionMap.put(from, to);
		}
	}
	
	BT_Ins getEquivalentInstruction(BT_Ins original) {
		return (BT_Ins) instructionMap.get(original);
	}
	
	BT_Method getContainingMethod() {
		return code.getMethod();
	}
	
	BT_CodeAttribute getCode() {
		return code;
	}
	
	BT_InsVector getInstructions() {
		return code.getInstructions();
	}
	
	void visitReachableCode(BT_CodeVisitor cv) throws BT_CodeException {
		code.visitReachableCode(cv);
	}
	
	int getLocalVarCount() throws BT_CodeException {
		//note: the fact that longs and doubles count as two locals each is accounted for in the locals vector
		return code.getMaxLocalsQuickly();
	}
    
	int getBytecodeSize() {
		return code.computeMaxInstructionSizes();
	}
}
