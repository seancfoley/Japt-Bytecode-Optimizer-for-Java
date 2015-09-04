package com.ibm.ive.tools.japt.reduction;

import java.util.HashMap;

import com.ibm.ive.tools.japt.ConditionalInterfaceItemCollection;
import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_Repository;

/**
 * @author sfoley
 *
 */
public class ClassProperties {

	final public BT_Class javaIoSerializable;
	final public BT_Class javaLangCloneable;
	final public BT_Class javaLangObject;
	final public BT_Class javaLangThrowable;
	final public BT_Class javaLangString; 
	final public BT_Class javaLangRuntimeException;
	final public BT_Class javaLangError;
	final public BT_Class objectArray;
	final public BT_Class javaLangThread;
	final public BT_Method threadStart;
	final public BT_Method threadRun;
	final private HashMap runtimeThrowables = new HashMap();
	private InternalClassesInterface internalClassesInterface;
	
	public ClassProperties(JaptRepository repository) {
		this.javaLangObject = repository.findJavaLangObject();
		this.javaLangThrowable = repository.findJavaLangThrowable();
		this.javaLangThread = repository.findJavaLangThread();
		this.javaLangString = repository.findJavaLangString();
		this.javaLangRuntimeException = repository.linkTo(BT_Repository.JAVA_LANG_RUNTIME_EXCEPTION);
		this.javaLangError = repository.linkTo(BT_Repository.JAVA_LANG_ERROR);
		this.javaIoSerializable = repository.linkToStub(BT_Repository.JAVA_IO_SERIALIZABLE);
		this.javaLangCloneable = repository.linkToStub(BT_Repository.JAVA_LANG_CLONEABLE);
		this.objectArray = repository.linkTo(BT_Repository.JAVA_LANG_OBJECT_ARRAY);
		
		
		BT_MethodSignature signature = repository.basicSignature;
		String start = "start";
		BT_Method threadStart = javaLangThread.findMethodOrNull(start, signature);
		if (threadStart == null) {
			threadStart = javaLangThread.addStubMethod(start, signature);
		}
		this.threadStart = threadStart;
		
		String run = "run";
		BT_Method threadRun = javaLangThread.findMethodOrNull(run, signature);
		if (threadRun == null) {
			threadRun = javaLangThread.addStubMethod(run, signature);
		}
		this.threadRun = threadRun;
		
		internalClassesInterface = repository.getInternalClassesInterface();
	}
	
	public boolean isThrowableObject(BT_Class objectType) {
		return objectType.equals(javaLangThrowable) || 
				objectType.isDescendentOf(javaLangThrowable);
	}
	
	public boolean isRuntimeThrowableObject(BT_Class objectType) {
		Object result = runtimeThrowables.get(objectType);
		if(result != null) {
			return ((Boolean) result).booleanValue();
		}
		boolean bresult = objectType.isDescendentOf(javaLangRuntimeException)
			|| objectType.isDescendentOf(javaLangError)
			|| objectType.equals(javaLangRuntimeException)
			|| objectType.equals(javaLangError);
		runtimeThrowables.put(objectType, bresult ? Boolean.TRUE : Boolean.FALSE);
		return bresult;
	}
	
	/**
	 * A given field or method may be considered to have created an object if a condition were satisifed, 
	 * the condition being that the given method or field were accessed.  In real terms, for a method, this might occur by something thrown
	 * or returned from a native method, or something created by reflection.  For fields, this would be
	 * the same that is created by the field's initializer, which is added to all constructors.
	 * Reducers can use this information to assume that a given class type has been instantiated and made
	 * available to the given member, so that reduction will become more accurate.
	 * 
	 * Note that adding a conditionally created object O to a propagator P does not account
	 * for other objects and fields that could potentially also have been created and held by
	 * field of O.  So for XTA, BTA, RTA reduction that is partially accounted for by
	 * marking methods and fields of O's class as reduction entry points later and continuing the
	 * reduction.  The associated methods and fields will then possess the requisite objects.  
	 * For ITA, the objects held by O will not become available to P regardless, since the instance
	 * fields of O will be untouchable even when marking reduction entry points, and so
	 * an alternative remedy is required. 
	 * @param member
	 * @return
	 */
	public BT_ClassVector getConditionallyCreatedObjects(BT_Member member) {
		if(internalClassesInterface.satisfiesAConditional(member) 
				|| internalClassesInterface.satisfiesAConditional(member.getDeclaringClass())) {
			BT_ClassVector classes = new BT_ClassVector();
			ConditionalInterfaceItemCollection conds[] = internalClassesInterface.getConditionals();
			for(int i=0; i<conds.length; i++) {
				ConditionalInterfaceItemCollection cond = conds[i];
				if(cond.satisfiesCondition(member) 
						|| cond.satisfiesCondition(member.getDeclaringClass())) {
					
					BT_ClassVector targetedClasses = cond.getTargetedClasses();
					for(int j=0; j<targetedClasses.size(); j++) {
						classes.addUnique(targetedClasses.elementAt(j));
					}
				}
			}
			return classes;
		}
		return new BT_ClassVector();
	}
}
