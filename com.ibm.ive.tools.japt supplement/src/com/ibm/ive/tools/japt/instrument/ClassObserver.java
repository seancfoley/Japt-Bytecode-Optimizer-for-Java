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
 * A specific type of ObjectObserver that observes the class object (the instance of java.lang.Class)
 * of any given class.  The class observer creates ObjectObserver instances for observing individual objects
 * instances of the observed class created at runtime.  The class observer also observes entries into
 * static methods.
 * <p>
 * When there is no 'instrumentation by object', the createObjectObserver method is never called,
 * and the class observer itself is used to observe both static and instance methods.
 * <p>
 * With 'instrumentation by object' enabled, the ClassObserver will observe static methods, while each
 * ObjectObserver created by the createObjectObserver method will observe instance method invocations.
 * 
 */
public interface ClassObserver extends ObjectObserver {
	ObjectObserver createObjectObserver(String className);
}
