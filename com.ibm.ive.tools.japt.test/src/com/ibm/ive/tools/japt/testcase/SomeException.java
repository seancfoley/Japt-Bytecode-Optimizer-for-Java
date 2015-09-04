/*
 * Created on Nov 17, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.testcase;

public class SomeException extends RuntimeException implements SomeInterface {
	Exception e;
	
	static void aMethodArgOnStackTop(Exception e) {
		System.out.println(e);
	}
	
	static void aMethodArgJustBelowStackTop(Exception e, int i) {
		System.out.println(i + " " + e);
	}
	
	void aMethodArgFarBelowStackTop(Exception e, int i, long j, Object o) {
		System.out.println(i + j + " " + o + " " + e);
	}
	
	public void anInterfaceMethod() {
		System.out.println("inside an interface method");
	}
}
