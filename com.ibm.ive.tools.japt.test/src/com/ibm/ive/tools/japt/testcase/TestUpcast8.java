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
public class TestUpcast8 {
	
	/**
	 * these are examples that do not generate verifier class loads.  They are here in the event that
	 * in the future if they DO generate class loads, then the upcast code can be modified to account for this.
	 */
	void falseAlarms(SomeException x) {
		/* the verifier should not load SomeException to verify that it implements SomeInterface, which is the 
		 * target interface of the following instruction.
		 */
		x.anInterfaceMethod();
		
		/*
		 * the verifier should not load SomeOtherException to verify that it is a subclass of SomeException.  This is done
		 * when the aastore instruction is executed at runtime.  
		 */
		SomeException exc[] = new SomeException[1];
		exc[0] = new SomeOtherException();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * How this will work is the reduction extension and the upcast extension will combine:
		 * Reduction will remove the SomeException class from the japt output. 
		 * This can be done by explicitly removing the class with the reduction extension.
		 * The upcast will prevent the class from being required for the verifier to do its work.
		 */
		System.out.println("ran upcast test");
	}
}
