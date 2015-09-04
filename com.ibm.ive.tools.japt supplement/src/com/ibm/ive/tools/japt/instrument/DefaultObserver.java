/*
 * Created on Oct 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.instrument;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DefaultObserver implements ClassObserver, RuntimeObserver {

	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.instrument.ClassObserver#createObject()
	 */
	public ObjectObserver createObjectObserver(String className) {
		if(System.out == null) {
			return this;
		}
		System.out.print(getObjectObserverString());
		System.out.print(' ');
		System.out.println(className);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.instrument.RuntimeObserver#createClass(java.lang.String)
	 */
	public ClassObserver createClassObserver(String className) {
		if(System.out == null) {
			return this;
		}
		System.out.print(getClassObserverString());
		System.out.print(' ');
		System.out.println(className);
		return this;
	}
	
	/*
	 * When this class is loaded as a BT_Class in Japt, the following method is changed
	 * to return an appropriate internationalized string message.
	 */
	public static String getObjectObserverString() {
		return null;
	}
	
	/*
	 * When this class is loaded as a BT_Class in Japt, the following method is changed
	 * to return an appropriate internationalized string message.
	 */
	public static String getClassObserverString() {
		return null;
	}

	
	/*
	 * When this class is loaded as a BT_Class in Japt, the following method is changed
	 * to return an appropriate internationalized string message.
	 */
	public static String getMethodEntryString() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.instrument.ObjectObserver#createMethod(java.lang.String, java.lang.String, short)
	 */
	public MethodObserver createMethodObserver(
			final String className, 
			final String methodName, 
			final String signature) {
		return new MethodObserver() {
			public void observeEntry(Object o) {
				if(System.out == null) {
					return;
				}
				if(o == null || o.getClass().getName().equals(className)) {
					System.out.print(getMethodEntryString());
					System.out.print(' ');
					System.out.print(className);
					System.out.print('.');
					System.out.print(methodName);
					System.out.println(signature);
				}
				else {
					System.out.print(getMethodEntryString());
					System.out.print(' ');
					System.out.print(className);
					System.out.print('.');
					System.out.print(methodName);
					System.out.print(signature);
					System.out.print(" (");
					System.out.print(o.getClass().getName());
					System.out.println(')');
				}
			}
		};
	}

}
