/*
 * Created on Oct 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.instrument;

/**
 * @author sfoley
 *
 * Serves as a holder for the single instance of RuntimeObserver that will
 * be instantiated and will be accessible from every class.
 */
public class RuntimeObserverHolder extends RuntimeObserverCreator {

	private static RuntimeObserver runtimeObserver;
	
	/**
	 * This method will be called by the <clinit> method of every instrumented class.
	 * @param className
	 * @return
	 */
	public static RuntimeObserver getRuntimeObserver(String className) 
		throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		if(runtimeObserver == null) {
			runtimeObserver = createInstance(className);
		}
		return runtimeObserver;
	}
	
	

}
