package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_CodeAttribute;

/**
 * An object that is created within a method.  Includes the location of where the object was created.
 * It is interchangeable with PropagatedObject.  This means that the information regarding the
 * location of creation of the object does not figure into its identity.
 * 
 * This also means that a given PropagatedObject is equal to a wrapping ReceivedObject with a null location,
 * or a wrapping CreatedObject with the created location.  
 * 
 * 
 * @author sfoley
 *
 */
public class CreatedObject extends WrappedObject {
	public final MethodInvocation invocation;
	public final InstructionLocation location;
	
	public CreatedObject(PropagatedObject object, MethodInvocation inv, InstructionLocation location) {
		super(object);
		this.location = location;
		invocation = inv;
	}
	
	public InstructionLocation getInstructionLocation() {
		return location;
	}
	
	public String toString() {
		BT_CodeAttribute code = invocation.getMethod().getCode();
		int num = code.findLineNumber(location.instruction);
		if(num > 0) {
			return object + " created in " + invocation + " at line number " + num + " in " + code.getMethod().getDeclaringClass().getSourceFile();
		}
		return object + " created in " + invocation + " at " + location;
	}

}
