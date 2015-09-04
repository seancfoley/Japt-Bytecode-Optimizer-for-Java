/*
 * Created on Apr 11, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public abstract class BT_ItemReference extends BT_Base {
	/**
	 The referencing code.
	**/
	public final BT_CodeAttribute from;
	
	public BT_ItemReference(BT_CodeAttribute from) {
		if(from == null) {
			throw new NullPointerException();
		}
		
		this.from = from;
	}
	
	public abstract BT_Ins getInstruction();
	
	public abstract BT_Class getClassTarget();
	
	public BT_Method getFrom() {
		return from.getMethod();
	}
	
	public String getInstructionTarget() {
		return getInstruction().getInstructionTarget();
	}
	
	/**
	 * 
	 * @return whether this reference refers to a class.
	 */
	public boolean isClassReference() {
		return false;
	}
	
	/**
	 * 
	 * @return whether this reference refers to a field.
	 */
	public boolean isFieldReference() {
		return false;
	}
	
	/**
	 * 
	 * @return whether this reference refers to a method.
	 */
	public boolean isMethodReference() {
		return false;
	}
	
	public String toString() {
		String fromString = (getFrom() == null) ? from.toString() : getFrom().useName();
		return Messages.getString("JikesBT.{0}_referenced_at_{1}_2", new Object[] {getInstructionTarget(), fromString});
	}
}
