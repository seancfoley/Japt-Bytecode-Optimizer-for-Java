package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.jikesbt.BT_Class;

public class ErrorWrapper {
	BT_Class error;
	Object descriptor;
	
	public ErrorWrapper(BT_Class error, Object descriptor) {
		this.error = error;
		this.descriptor = descriptor;
	}
	
	public String toString() {
		return error.fullName() + ": " + descriptor;
	}
	
}
