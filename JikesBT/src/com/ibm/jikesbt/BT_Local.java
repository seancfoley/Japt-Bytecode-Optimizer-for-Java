package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents a method's local variable or parameter.

 @see BT_LoadLocalIns
 @see BT_LocalIns
 @see com.ibm.samples.AsmSkel#main
 * @author IBM
**/
public final class BT_Local implements Cloneable {

	/**
	 The number of the local slot.
	**/
	public int localNr;

	public BT_Local(int localNr) {
		this.localNr = localNr;
	}

	public String toString() {
		return Messages.getString("JikesBT.local_{0}_1", localNr);
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {}
		return null;
	}
}
