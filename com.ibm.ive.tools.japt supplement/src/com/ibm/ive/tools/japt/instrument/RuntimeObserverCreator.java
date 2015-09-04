/*
 * Created on Nov 1, 2004
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
public class RuntimeObserverCreator {

	/*
	 * When this class is loaded as a BT_Class in Japt, the following method is changed
	 * to return an appropriate internationalized string message.
	 */
	public static String getClassNotFoundErrorMessage() {
		return null;
		
	}
	
	/*
	 * When this class is loaded as a BT_Class in Japt, the following method is changed
	 * to return an appropriate internationalized string message.
	 */
	public static String getLoadingErrorMessage() {
		return null;
	}
	
	/*
	 * When this class is loaded as a BT_Class in Japt, the following method is changed
	 * to return an appropriate internationalized string message.
	 */
	public static String getInstantiationErrorMessage() {
		return null;
	}
	
	/*
	 * When this class is loaded as a BT_Class in Japt, the following method is changed
	 * to return an appropriate internationalized string message.
	 */
	public static String getInvalidTypeErrorMessage() {
		return null;
	}
	
	public static Class createClass(String className) throws ClassNotFoundException {
		try {
			return Class.forName(className);
		}
		catch(SecurityException e) {
			System.out.print(getLoadingErrorMessage());
			System.out.println(className);
			throw e;
		}
		catch(ClassNotFoundException e) {
			System.out.print(getClassNotFoundErrorMessage());
			System.out.println(className);
			throw e;
		}
		catch(ExceptionInInitializerError e) {
			System.out.print(getLoadingErrorMessage());
			System.out.println(className);
			throw e;
		}
		catch(LinkageError e) {
			System.out.print(getLoadingErrorMessage());
			System.out.println(className);
			throw e;
		}
	}
	
	/**
	 * @param className
	 */
	public static RuntimeObserver createInstance(String className) 
		throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Class clazz = createClass(className);
		try {
			return (RuntimeObserver) clazz.newInstance();
		}
		catch(IllegalAccessException e) {
			System.out.print(getInstantiationErrorMessage());
			System.out.println(className);
			throw e;
		}
		catch(InstantiationException e) {
			System.out.print(getInstantiationErrorMessage());
			System.out.println(className);
			throw e;
		}
		catch(ClassCastException e) {
			System.out.print(getInvalidTypeErrorMessage());
			System.out.println(className);
			throw e;
		}
	}

}
