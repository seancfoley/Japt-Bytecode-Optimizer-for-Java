/*
 * Created on Oct 3, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public abstract class BT_NoSuchMemberException extends BT_RuntimeException {

	public BT_NoSuchMemberException(String explanation) {
		super(explanation);
	}
	
	public static class BT_NoSuchFieldException extends BT_NoSuchMemberException {
		public BT_NoSuchFieldException(String explanation) {
			super(explanation);
		}
	}
	
	public static class BT_NoSuchMethodException extends BT_NoSuchMemberException {
		public BT_NoSuchMethodException(String explanation) {
			super(explanation);
		}
	}

}
