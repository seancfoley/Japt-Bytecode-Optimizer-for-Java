/*
 * Created on Nov 4, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ive.tools.japt.startupPerformance;

import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Ins;

/**
 * @author Sean Foley
 *
 * Represents an injection into a code attribute. 
 * An injection replaces a series of instructions with another series of instructions.
 */
public class CodeInjection implements Comparable {
	final BT_CodeAttribute code;
	final int instructionIndex;
	final BT_Ins newInstructions[];
	final BT_Ins replacedInstruction;

	public CodeInjection(BT_CodeAttribute code, int index, BT_Ins newIns, BT_Ins replacedInstruction) {
		this.code = code;
		this.instructionIndex = index;
		this.newInstructions = new BT_Ins[] {newIns};
		this.replacedInstruction = replacedInstruction;
	}
	
	public CodeInjection(BT_CodeAttribute code, int index, BT_Ins newIns[], BT_Ins replacedInstruction) {
		this.code = code;
		this.instructionIndex = index;
		this.newInstructions = newIns;
		this.replacedInstruction = replacedInstruction;
	}
	
	public String toString() {
		return "injection at instruction index " + instructionIndex + " of " + code.getMethod().useName();
	}
	
	public int compareTo(Object o) {
		CodeInjection other = (CodeInjection) o;
		return instructionIndex - other.instructionIndex;
	}
	
	public void inject() {
		if(replacedInstruction != null) {
			if(code.getInstructions().elementAt(instructionIndex) != replacedInstruction) {
				throw new IllegalArgumentException("instruction index inaccurate");
			}
			code.replaceInstructionsAtWith(1, instructionIndex, newInstructions);
		} else {
			code.insertInstructionsAt(newInstructions, instructionIndex);
		}
	}
	
}
