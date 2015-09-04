/*
 * Created on Nov 6, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.testcase;

/**
 * 
 * @author sfoley
 *
 * This class is primarily designed to test the casting optimizations for startup performance.
 * 
 * It contains various methods which normally cause the verifier to load the class SomeException but
 * with all the upcast optimizations this class will not be needed.
 * 
 * It also contains various other methods to test the dataflow analyzer used for the casting optimizations and 
 * for stackmap creation.
 */
public class TestUpcast4 {
	
	
	
	boolean aCondition;
	SomeException e;
	
	/**
	 * There are 4 method argument upcasts, with 3 of them requiring two separate checkcasts.
	 * The upcasts will prevent SomeException from being loaded.
	 * @return
	 */
	void aMethodInvoke() {
		
		/*
		 * Tests the upcast of the only parameter: from SomeException to Exception.
		 * 
		 * The conditional assignment means a merge of the will take place on the stack
		 */
		SomeException.aMethodArgOnStackTop(aCondition ? new SomeException() : this.e);
		
		
		/*
		 * Tests the upcast of the first parameter: from SomeException to Exception.
		 * 
		 * The conditional assignment means a merge of the will take place on the stack
		 */
		SomeException.aMethodArgJustBelowStackTop(aCondition ? new SomeException() : this.e, 0);
		
		/* 
		 * Tests that an object buried in the stack can be successfully upcasted, 
		 * the parameter 1 upcast from SomeException to Exception.
		 * 
		 * There is also the additional upcast od parameter 4 from SomeException to Object.
		 */
		new SomeException().aMethodArgFarBelowStackTop(aCondition ? new SomeException() : this.e, 0, 1, this.e);
	
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TestUpcast5.main(args);
	}
}
