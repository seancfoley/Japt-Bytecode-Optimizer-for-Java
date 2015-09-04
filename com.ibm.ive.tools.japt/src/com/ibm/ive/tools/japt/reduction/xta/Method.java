package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSiteVector;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CreationSiteVector;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_ExceptionTableEntryVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;

/**
 * Represents a method declared in an interface or class.  A method propagates objects from one place
 * to another through the parameters (which includes the class itself in a non-static method call).  An
 * object may be propagated to the method through the parameters and then propagated elsehwere by a field write
 * or another method call or from a thrown exception.<p>
 * For example, when calling threadx.equals(objectx) in a method x(), 
 * the thread object threadx and the object objectx is propagated from the
 * stack of method x to the stack of java.lang.Object.equals().  From there, the two 
 * objects may be propagated elsewhere.  Note that the methods Object.equals() as listed as
 * a method call from the body of x() can only be called if an object of type thread has
 * previously been propagated to x() or has been created inside the body of x().  Once this
 * has been proven true, then it is possible that any other object propagated to x() or created
 * within the body of x() might be passed as the sole argument to the method call threadx.equals(objectx)
 * and propagated to Object.equals().
 * <p>
 * This class uses these principles, propagating objects from methods to other methods and fields
 * when it is known that such propagations are possible.  
 * @author sfoley
 *
 */
public class Method extends MethodPropagator {

	public BT_Method underlyingMethod;

	private static final short scanned = 0x20;
	private static final short storesIntoArrays = 0x40;
	private static final short loadsFromArrays = 0x80;
	private static final short canThrow = 0x100;
	private static final short verified = 0x200;
	

	/**
	 * The types that this method accepts as arguments, including the declaring class
	 * if this method is non-static
	 */
	private BT_Class typesPropagatable[];
	static final BT_Class noTypesPropagatable[] = new BT_Class[0];
	
	public Method(Repository repository, BT_Method method,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(repository.getClazz(method.getDeclaringClass()), propagatedObjects, allPropagatedObjects);
		this.underlyingMethod = method;
		initialize();
	}
	
	/**
	 * Constructor for Method.
	 * @param member
	 */
	public Method(Repository repository, BT_Method method) {
		super(repository.getClazz(method.getDeclaringClass()));
		this.underlyingMethod = method;
		initialize();
	}
	
	
//	public void tracex(String msg) {
//		if(	false
//				//|| toString().startsWith("java.util.Map.containsKey")
//				//|| toString().startsWith("java.lang.reflect.Proxy.")
//				|| toString().startsWith("java.lang.String.valueOf(java")
//				|| toString().startsWith("java.lang.Object.toString()")
//				//|| toString().startsWith("java.lang.reflect.String.valueOf")
////|| toString().startsWith("java.lang.Byte.int")
////				|| toString().startsWith("java.lang.Byte.short")
////				|| toString().startsWith("java.lang.Byte.byte")
////				|| toString().startsWith("java.lang.Number.byte")
////				|| toString().startsWith("java.lang.Byte(")
//				
//				//|| toString().startsWith("java.util.Vector(java.util.Collection)") //(Ljava/util/Collection;)
//				//|| underlyingMethod.fullName().equals("com.oti.bb.automatedtests.Test_java_io_BufferedOutputStream.suite")
//				//|| underlyingMethod.fullName().equals("junit.framework.TestSuite.addTest")
//				//|| msg.indexOf("Test_java_io_BufferedOutputStream") != -1
//				//|| msg.indexOf("test.framework.TestCase") != -1
//				//|| underlyingMethod.fullName().equals("test.framework.TestSuite.run")
//				//|| underlyingMethod.fullName().equals("test.textui.TestRunner.doRun")
//				//|| underlyingMethod.fullName().equals("test.textui.TestRunner.run")
//				//|| underlyingMethod.fullName().equals("java.util.Vector$1.nextElement")
//				//|| underlyingMethod.fullName().equals("java.util.Enumeration.nextElement")
//				//|| underlyingMethod.fullName().equals("junit.framework.TestSuite.tests")
//				//|| underlyingMethod.fullName().equals("test.framework.TestSuite")
//				//|| underlyingMethod.fullName().equals("com.oti.bb.automatedtests.Test_java_io_BufferedOutputStream.main")
//				) {
//			System.out.println(getState());
//		}
//				
//	}
	
	void initialize() {
		boolean isNotStatic = !underlyingMethod.isStatic();
		BT_ClassVector paramTypes = underlyingMethod.getSignature().types;
		BT_ClassVector alreadyFound = new BT_ClassVector(paramTypes.size() + (isNotStatic ? 1 : 0));
		for (int x = 0; x < paramTypes.size(); x++) {
			BT_Class param = paramTypes.elementAt(x);
			if(param.isBasicTypeClass) {
				continue;
			}
			if(alreadyFound.contains(param)) {
				continue;
			}
			alreadyFound.addElement(param);
		}
		if (isNotStatic && !alreadyFound.contains(underlyingMethod.getDeclaringClass())) {
			alreadyFound.addElement(underlyingMethod.getDeclaringClass());
		}
		if(alreadyFound.size() == 0) {
			typesPropagatable = noTypesPropagatable;
		}
		else {
			alreadyFound.trimToSize();
			typesPropagatable = alreadyFound.getBackingArray();
		}
	}
	
	BT_Class getReturnType() {
		return underlyingMethod.getSignature().returnType;
	}
	
	BT_Class[] getDeclaredExceptions() {
		return underlyingMethod.getDeclaredExceptions();
	}
	
	public void setRequired() {
		if(isRequired()) {
			return;
		}
		super.setRequired();
		
		//tracex("set required method");
		
		
		Repository rep = getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(getBTClass());
		if(checkEntryPoints) {
			BT_MethodSignature sig = underlyingMethod.getSignature();
			BT_Class returnType = sig.returnType;
			BT_ClassVector types = sig.types;
			for(int i=0; i<types.size(); i++) {
				BT_Class type = types.elementAt(i);
				if(!type.isBasicTypeClass && japtRepository.isInternalClass(type)) {
					lister.foundEntryTo(type, underlyingMethod);
				}
			}
			if(!returnType.isBasicTypeClass && japtRepository.isInternalClass(returnType)) {
				lister.foundEntryTo(returnType, underlyingMethod);
			}
		}
		
		//The following stuff is actually not mandatory, it is possible to have
		//elements in the method signature that are not needed whatsoever
		
		BT_Class declaredExceptions []= getDeclaredExceptions();
		//if a method declares that that it can throw something, it must be included
		for(int i=0; i<declaredExceptions.length; i++) {
			getRepository().getClazz(declaredExceptions[i]).setRequired();
		}
		if(returnsObjects()) {
			getRepository().getClazz(getReturnType()).setRequired();
		}
		BT_MethodSignature sig = underlyingMethod.getSignature();
		BT_ClassVector types = sig.types;
		for(int i=0; i<types.size(); i++) {
			BT_Class type = types.elementAt(i);
			if(type.isBasicTypeClass)  {
				continue;
			}
			getRepository().getClazz(type).setRequired();
		}
	}
	
	public void setAccessed() {
		if(isAccessed()) {
			return;
		}
		super.setAccessed();
		if(underlyingMethod.isStatic()) {
			declaringClass.setInitialized();
		}
	}
	
	boolean isThrowableObject(BT_Class objectType) {
		return getRepository().properties.isThrowableObject(objectType);
	}
	
	boolean hasObjectParameters() {
		return typesPropagatable.length > 0;
	}
	
	/**
	 * @return whether the method can accept an object of the given type during a method invocation
	 */
	boolean acceptsArgument(BT_Class type) {
		return getRepository().isCompatibleType(typesPropagatable, type);
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Member#propagateFromUnknownSource()
	 */
//	void propagateFromUnknownSource() {
//		if(underlyingMethod.isAbstract()) {
//			setRequired();
//			return;
//		}
//		super.propagateFromUnknownSource();
//		BT_ClassVector unknowns = new BT_HashedClassVector();
//		for(int i=0; i<typesPropagatable.length; i++) {
//			unknowns.addUnique(typesPropagatable[i]); 
//		}
//		for(int i=0; i<unknowns.size(); i++) {
//			BT_Class creation = unknowns.elementAt(i);
//			Clazz clazz = addCreatedObject(creation);
//			clazz.includeAllConstructors();
//		}
//		Clazz clazz = getDeclaringClass();
//		if(underlyingMethod.isStatic()) {
//			clazz.setInitialized();
//		}
//	}
	
	
	void propagateFromUnknownSource() {
		if(underlyingMethod.isAbstract()) {
			setRequired();
			return;
		}
		super.propagateFromUnknownSource();
		BT_ClassVector unknowns = new BT_HashedClassVector();
		for(int i=0; i<typesPropagatable.length; i++) {
			unknowns.addUnique(typesPropagatable[i]); 
		}
		for(int i=0; i<unknowns.size(); i++) {
			BT_Class creation = unknowns.elementAt(i);
			addCreatedObject(creation);
		}
		Clazz clazz = getDeclaringClass();
		if(!underlyingMethod.isStatic()) {
			addCreatedObject(clazz.getBTClass());
			clazz.setInstantiated();
			clazz.includeConstructors();
		}
	}
	
	
	
	/**
	 * The current method has been accessed, so it can now propagate objects elsewhere.
	 * Find all possible targets and determine what objects are created within the body 
	 * of this method.
	 * <p>
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#initializePropagation()
	 */
	void initializePropagation() {
		
		if(underlyingMethod.isAbstract()) {
			potentialTargets = noPotentialTargets;
			return;
		}
		if(underlyingMethod.isNative()) {
			addCreatedObjectsUnknown();
			potentialTargets = getPotentialNativeTargets();
			return;
		}
		BT_CodeAttribute code = underlyingMethod.getCode();
		
		if(code != null) {
			addCreatedObjects(code);
			findReferences(code);
			potentialTargets = getPotentialTargets(code);
			potentialTargets.findTargets(code);
		}
		else { //really we should never arrive here, presumably this method can be
			//executed but for some reason there is no code, so we treat it like a native method
			addCreatedObjectsUnknown();
			potentialTargets = noPotentialTargets;
		}
	}
	
	protected MethodPotentialTargets getPotentialNativeTargets() {
		return new MethodPotentialArrayTargets(this);
	}
	
	protected MethodPotentialTargets getPotentialTargets(BT_CodeAttribute code) {
		int accessedFields = code.accessedFields.size();
		int calledMethods = code.calledMethods.size();
		boolean hasArraySources = storesIntoArrays() || loadsFromArrays();
		if(accessedFields == 0 && calledMethods == 0) {
			if(hasArraySources) {
				return new MethodPotentialArrayTargets(this);
			}
			return noPotentialTargets;
		}
		final MethodPotentialCodeTargets codeTargets = 
			new MethodPotentialCodeTargets(this);
		if(hasArraySources) {
			return new MethodPotentialTargets() {
				MethodPotentialArrayTargets arrayTargets = 
					new MethodPotentialArrayTargets(Method.this);
				
				public void findTargets(BT_CodeAttribute code) {
					codeTargets.findTargets(code);
					arrayTargets.findTargets(code);
				}
				
				public void findNewTargets(BT_Class objectType) {
					arrayTargets.findNewTargets(objectType);
					codeTargets.findNewTargets(objectType);
					if(codeTargets.hasNoPotentialTargets()) {
						potentialTargets = arrayTargets;
					}
				}
			};
		}
		
		return new MethodPotentialTargets() {
			
			public void findTargets(BT_CodeAttribute code) {
				codeTargets.findTargets(code);
			}
			
			public void findNewTargets(BT_Class objectType) {
				codeTargets.findNewTargets(objectType);
				if(codeTargets.hasNoPotentialTargets()) {
					potentialTargets = noPotentialTargets;
				}
			}
		};
		
		
	}
	
	void findVerifierRequiredClasses(BT_CodeAttribute code) {
		if((flags & verified) != 0) {
			return;
		}
		flags |= verified;
		Repository rep = getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(getBTClass());
		
		
		BT_ClassVector referencedClses = code.getVerifierRequiredClasses(getRepository().pool);
		for(int i=0; i<referencedClses.size(); i++) {
			BT_Class referencedClass = referencedClses.elementAt(i);
			if(checkEntryPoints && !referencedClass.isBasicTypeClass && japtRepository.isInternalClass(referencedClass)) {
				lister.foundEntryTo(referencedClass, underlyingMethod);
			}
			Clazz clazz = rep.getClazz(referencedClass);
			clazz.setRequired();
		}
	}
	
	/**
	 * This method finds classes that are required by the method body and marks them as being
	 * required. This is done when the method is accessed.  This could be changed so that it is
	 * done when the method is marked required for more stringent VM's 
	 */
	private void findReferences(BT_CodeAttribute code) {
		//TODO reduction fix involving not targetting explicitly removed items
		Repository rep = getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(getBTClass());
		
		//determine which classes are required by the method body
		BT_ClassReferenceSiteVector referencedClasses = code.referencedClasses;
		for(int i=0; i<referencedClasses.size(); i++) {
			BT_Class referencedClass = referencedClasses.elementAt(i).getTarget();
			if(checkEntryPoints && !referencedClass.isBasicTypeClass && japtRepository.isInternalClass(referencedClass)) {
				lister.foundEntryTo(referencedClass, underlyingMethod);
			}
			Clazz clazz = rep.getClazz(referencedClass);
			clazz.setRequired();
		}
		
		findVerifierRequiredClasses(code);
		
		//if a method is included, then anything that it explicitly declares that it can catch is required
		BT_ExceptionTableEntryVector exceptions = code.getExceptionTableEntries();
		for (int t=0; t < exceptions.size(); t++) {
			BT_ExceptionTableEntry e = exceptions.elementAt(t);
			BT_Class catchType = e.catchType;
			if (catchType == null)
				continue;
			if(checkEntryPoints && !catchType.isBasicTypeClass && japtRepository.isInternalClass(catchType)) {
				lister.foundEntryTo(catchType, underlyingMethod);
			}
			rep.getClazz(catchType).setRequired();
		}
	}

	
	protected void addCreatedObjects(BT_CodeAttribute code) {
		/*
		 * Determine which classes have instances created
		 * Note that this includes all new, anewarray, multianewarray instructions as well
		 * as ldc and ldc_w instructions that reference strings
		 */
		BT_CreationSiteVector createdClasses = code.createdClasses;
		for(int i=0; i<createdClasses.size(); i++) {
			BT_Class creation = createdClasses.elementAt(i).getCreation();
			//TODO this does not populate arrays created by multianewarray - we need to get the array element of each creation and add the element Clazz to it,
			//recursively through the array dimensions
			//trace("Creating " + creation);
			addCreatedObject(creation);
		}
		addConditionallyCreatedObjects(underlyingMethod);
	}
	
	protected void addCreatedObjectsUnknown() {
		/*
		 * We must assume that the return type and any subclass or implementing type
		 * is being created within the method body and can be propagated.
		 * In other words, since there is no method body to scan we must assume the worst, 
		 * that anything that can be created by this method will be created.
		 */
		BT_Class returnType = getReturnType();
		if(!returnType.isBasicTypeClass) {
			addCreatedObject(returnType);
			//addCreatedObjectAndAllSubtypes(returnType);
		}
		
		/*
		 * We can say the same as abovefor thrown exceptions, so we must assume
		 * that all subtypes of declared exceptions as well as all RuntimeExceptions are propagated
		 */
		//addCreatedObjectAndAllSubtypes(getRepository().properties.javaLangRuntimeException);
		addCreatedObject(getRepository().properties.javaLangRuntimeException);
		BT_Class declaredExceptions[] = getDeclaredExceptions();
		for(int i=0; i<declaredExceptions.length; i++) {
			addCreatedObject(declaredExceptions[i]);
		}
		addConditionallyCreatedObjects(underlyingMethod);
	}
	
	boolean returnsObjects() {
		return !getReturnType().isBasicTypeClass;
	}
	
	boolean returns(BT_Class objectType) {
		BT_Class returnType = getReturnType();
		return objectType.equals(returnType) || objectType.isDescendentOf(returnType);
	}
	
	/**
	 * returns true if the method declares the throwable type in its list of checked exceptions
	 */
	boolean canThrow(BT_Class objectType) {
		return canThrow() && canPassThrown(objectType);
	}
	
	boolean canPassThrown(BT_Class objectType) {
		if(getRepository().properties.isRuntimeThrowableObject(objectType)) {
			return true;
		}
		BT_Class declaredExceptions[] = getDeclaredExceptions();
		for(int i=0; i<declaredExceptions.length; i++) {
			BT_Class throwable = declaredExceptions[i];
			if(objectType.equals(throwable) || objectType.isDescendentOf(throwable)) {
				return true;
			}
		}
		return false;
	}

	/**
	 Returns true if there is no athrow instruction.
	**/
	private void scanCode() {
		if((flags & scanned) != 0) {
			return;
		}
		flags |= scanned;
		BT_CodeAttribute code = underlyingMethod.getCode();
		if(underlyingMethod.isNative() || code == null) {
			//for natives we assume the worst to try to be safe, although there is no guarantee
			//this will help
			flags |= canThrow;
			flags |= storesIntoArrays;
			flags |= loadsFromArrays;
			return;
		}
		else if(underlyingMethod.isAbstract()) {
			return;
		}
		boolean foundCanThrow = false;
		boolean foundStoresIntoArrays = false;
		boolean foundLoadsFromArrays = false;
		BT_InsVector ins = code.getInstructions();
		if(ins != null) {
			for (int k = 0; k < ins.size(); k++) {
				BT_Ins instruction = ins.elementAt(k);
				if(foundCanThrow && foundStoresIntoArrays && foundLoadsFromArrays) {
					return;
				}
				else {
					switch(instruction.opcode) {
						case BT_Ins.opc_athrow:
							flags |= canThrow;
							foundCanThrow = true;
							continue;
						case BT_Ins.opc_aastore:
							flags |= storesIntoArrays;
							foundStoresIntoArrays = true;
							continue;
						case BT_Ins.opc_aaload:
							flags |= loadsFromArrays;
							foundLoadsFromArrays = true;
						default:
							continue;
					}
				}
				
			}
		}
		
	}
	
	/**
	 * returns true if the method contains an aastore instruction
	 */
	public boolean storesIntoArrays() {
		scanCode();
		return (flags & storesIntoArrays) != 0;
	}
	
	/**
	 * returns true if the method contains an aaload instruction
	 */
	public boolean loadsFromArrays() {
		scanCode();
		return (flags & loadsFromArrays) != 0;
	}
	
	/**
	 * returns true if the method contains an athrow instruction
	 */
	boolean canThrow() {
		scanCode();
		return (flags & canThrow) != 0;
	}
	
	
	/**
	 * returns true if the method has an exception handler for the given type,
	 * not including finally clauses (ie even if there is a finally clause, this method
	 * will return false if there is not explicit handler)
	 */
	boolean catches(BT_Class throwableType) {
		BT_ExceptionTableEntryVector exceptions = underlyingMethod.getCode().getExceptionTableEntries();
		for(int i=0; i<exceptions.size(); i++) {
			BT_ExceptionTableEntry entry = exceptions.elementAt(i);
			BT_Class catchable = entry.catchType;
			if(catchable == null) {
				continue;
				//technically, the exception object is placed on the operand
				//stack, so it is propagated, but the finally clause does not allow 
				//the programmer to access any thrown exception objects, so we can
				//assume that the object is not propagated to the method
			}
			if(throwableType.equals(catchable) || throwableType.isDescendentOf(catchable)) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return getName();
	}
	
	public String getName() {
		return underlyingMethod.toString();
	}
	
}
