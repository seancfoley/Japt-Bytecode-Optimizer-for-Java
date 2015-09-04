package com.ibm.ive.tools.japt.testcase.escape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;


public class EscapeClass {

	public static void main() {
		Object array = new Object[] {new Object(), null}; /* arrays used to manage collections of objects often do not escape */
		ArrayList sorter = new ArrayList(); /* Collections used for data management sometimes do not escape */
		Collections.sort(sorter);
		Set set = new HashSet();
		Iterator iterator = set.iterator(); /* iterators frequently do not escape the method in which they are used */
		while(iterator.hasNext()) {
			System.out.println("Some object" + iterator.next()); /* StringBuffer or StringBuilder objects are frequently for temporary use and thus do not escape */
		}
		Vector vector = new Vector(); /* enumerations frequently do not escape the method in which they are used */
		Enumeration elements = vector.elements();
		while(elements.hasMoreElements()) {
			System.out.println(elements.nextElement());
		}
	}
	
	public static final short escapeClassInitializer[][] = { 
		{0, 0}, 
		{0, 1},
	};
	
	Object object;
	
	void escapeToThisArgument() {
		object = new Object();
	}
	
	void escapeToParameterArgument(EscapeClass arg) {
		arg.object = new Object();
	}
	
	static void escapeByThrow() {
		throw new RuntimeException();
	}
	
	static void escapeByPropagatedThrow() {
		escapeByThrow();
	}
	
	static Object escapeByReturn() {
		return new Object();
	}
	
	static Object escapeByDoubleReturn() {
		return escapeByReturn();
	}
	
	static Object staticObject;
	
	static void escapeByStaticField() {
		staticObject = new Object();
	}
	
	/*
	 * All of these escape by native examples are equivalent to escape by any method
	 * for which the code is unknown at the time of analysis, which incudes methods
	 * whose classes have not been loaded at runtime.
	 */
	
	static void escapeByNative() {
		new EscapeClass().aNative();
	}
	
	native void aNative();

	static void escapeByAnotherNative() {
		aNative2(new EscapeClass());
	}
	
	static native void aNative2(Object arg);
	
	static void escapeByNativeReturned() {
		aNative3().object = new Object();
	}
	
	static native EscapeClass aNative3();
	
	
	static void escapeByNativeThrown() {
		try {
			aNative4();
		} catch(RuntimeExceptionSubclass e) {
			e.object = new Object();
		}
	}
	
	static class RuntimeExceptionSubclass extends RuntimeException {
		Object object;
	}
	
	static native void aNative4();
	
	static void escapeByThread() {
		final Object object = new Object();
		class ThreadSubClass extends Thread {
			public void run() {
				Object local = object;
			}
		}
		Thread thread = new ThreadSubClass();
		thread.start();
	}
	
	class CustomThread extends Thread {
		Object object;
	}

	/*
	 * If currentThread() is a native call or if it accesses a static field, then that will cause the
	 * object to escape as well.
	 */
	static void escapeByCurrentThread() {
		((CustomThread) Thread.currentThread()).object = new Object();
	}
	
	static void escapeNotLoaded() {
		OtherClass object = new OtherClass();  
		/* 
		 * If we have not loaded the class for an object, analysis will determine that the object escapes,
		 * because there is always a constructor invocation with the object, and inside 
		 * that invocation the object can escape.
		 */
	}
	
	static class OtherClass {
		OtherClass() {
			staticObject = this;
		}
		
		static void method(Object arg) {
			staticObject = arg;
		}
		
		static EscapeClass method2() {
			staticObject = new EscapeClass();
			return (EscapeClass) staticObject;
		}
		
		static void method3() {
			staticObject = new RuntimeExceptionSubclass();
			throw (RuntimeExceptionSubclass) staticObject;
		}
	}
	
	static void escapeNotLoadedArg() {
		OtherClass.method(new EscapeClass());
	}
	
	static void escapeNotLoadedReturned() {
		OtherClass.method2().object = new Object();
	}
	
	static void escapeNotLoadedThrown() {
		try {
			OtherClass.method3();
		} catch(RuntimeExceptionSubclass e) {
			e.object = new Object();
		}
	}
	
	void escapeToArray() {
		/* There are 3 object instantiated here, the base array of type int[][] assigned to object the two objects of type int[] contained within the base array */
		object = new int[2][3]; 
	}
}
