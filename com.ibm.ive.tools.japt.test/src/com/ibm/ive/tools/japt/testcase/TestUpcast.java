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
public class TestUpcast {

	/**
	 * this method is here to test the data flow analyzer with 
	 * an invocation on a null type.  The local variable can only
	 * be seen as having type null by the analyzer, and therefore
	 * the analyzer sees the method call as an invocation on the
	 * null type.
	 */
	void invokeOnNull() {
		String s = null;
		
		if(s == null) {
			s = "";
		} else {
			s.trim();
		}
	}
	
	/**
	 * This method attempts an array load from the null type.  It will then
	 * perform an invocation on the array element.
	 * @param t
	 */
	void shapeVisitorTester(TestUpcast t) {
		TestUpcast array[] = null;
		
		//this array load occurs on the null type
		TestUpcast testUpcast = array[1];
		
		testUpcast.shapeVisitorTester(t);
	}
	
	/**
	 * A successful catch block optimization will prevent the verifier from loading SomeException
	 * @return
	 */
	void aMethodThatCatches() {
		/* 
		 * a verifier must ensure that the type of a caught object
		 * is a subclass of java.lang.Throwable
		 */
		try {
			System.out.println("in try block");
		} catch(SomeException e) {
			System.out.println("caught" + e);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TestUpcast2.main(args);
	}
	
	

}
