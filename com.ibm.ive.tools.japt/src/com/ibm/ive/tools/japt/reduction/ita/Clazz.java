package com.ibm.ive.tools.japt.reduction.ita;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.ibm.ive.tools.japt.reduction.ita.ObjectSet.ObjectSetEntry;
import com.ibm.ive.tools.japt.reduction.ita.Repository.ArrayIterator;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 * 
 * Represents a java class.
 * 
 * As a propagator, this propagator inititates propagations of its constituent static fields, 
 * object instances and methods.
 *
 */
public class Clazz implements Propagator {

	/**
	 * this class is required (ie it is acessed or referenced or needed in some way).
	 */
	private boolean isRequired;
	
	/**
	 * this class is initialized (hence it is also required) 
	 */
	private boolean isInitialized;
	
	/**
	 * this class is instantiated (hence it is also initialized)
	 */
	private boolean isInstantiated;
	
	/**
	 * for additional class data
	 */
	public int contextFlags;
	
	/**
	 * The JIKESBT representation of the class
	 */
	private BT_Class clazz;
	
	public final Repository repository;
	
	private Method methods[];
	private Field fields[];
	
	//Propagators
	private StaticFieldInstance staticFields[];
	private FieldInstance sharedFields[];
	private FieldInstance genericFields[];
	private ObjectSet createdObjects = new ObjectSet(); //contains PropagatedObject
	private ObjectSet createdGenericObjects = new ObjectSet(); //contains PropagatedObject
	
	private SpecificMethodInvocation specificMethodInvocations[];//each array entry is a linked list, not just a single entry
	private GenericMethodInvocation genericMethodInvocations[];//each array entry is a linked list, not just a single entry
	
	private int specificInvocationCount;
	private int genericInvocationCount;
	
	private Boolean isRuntimeThrowable;
	private Boolean isThrowable;
	
	boolean isSharedArrayElement;
	private ArrayElement sharedArrayElement;
	private ArrayElement genericArrayElement;
	public final Instantiator instantiator;
	
	final int staticFieldCount;
	final int instanceFieldCount;
	
	private Clazz parents[];
	
	private Clazz superClass;
	private Clazz elementClass;
	private Clazz arrayType;
	private Clazz arrayClass;
	
	private Boolean hasFinalizer;
	private Method finalizerMethod;
	
	Clazz(Repository r, BT_Class clazz, Instantiator instantiator) {
		this.clazz = clazz;
		this.repository = r;
		this.instantiator = instantiator;
		
		BT_FieldVector fields = clazz.getFields();
		int instances = 0;
		int statics = 0;
		for(int i=0; i<fields.size(); i++) {
			BT_Field f = fields.elementAt(i);
			if(f.isStatic()) {
				statics++;
			} else {
				instances++;
			}
		}
		staticFieldCount = statics;
		BT_Class sup = clazz.getSuperClass();
		if(sup == null) {
			superClass = null;
		} else {
			superClass = repository.getClazz(sup);
		}
		int superCount = sup == null ? 0 : superClass.instanceFieldCount;
		instanceFieldCount = instances + superCount;
	}
	
	public int getContextFlags() {
		return contextFlags;
	}
	
	public void setContextFlags(int flags) {
		this.contextFlags = flags;
	}
	public void doCount() {
		int methodCount = Repository.doCount(methods);
		int fieldCount = Repository.doCount(fields);
		int objectCount = Repository.doCount(createdObjects);
		int genericObjectCount = Repository.doCount(createdGenericObjects);
		int arrayElementCount = sharedArrayElement != null ? 1 : 0;
		int staticFieldCount = (staticFields == null) ? 0 : Repository.doCount(staticFields);
		int sharedFieldCount = (sharedFields == null) ? 0 : Repository.doCount(sharedFields);
		int genericFieldCount = (genericFields == null) ? 0 : Repository.doCount(genericFields);
		int methodInvCount = (specificMethodInvocations == null) ? 0 : Repository.doCount(specificMethodInvocations);
		int genericMethodInvCount = (genericMethodInvocations == null) ? 0 : Repository.doCount(genericMethodInvocations);
		repository.fieldCount += fieldCount;
		repository.methodCount += methodCount;
		repository.staticFieldCount += staticFieldCount;
		repository.methodInvCount += methodInvCount;
		repository.genericMethodInvCount += genericMethodInvCount;
		repository.objectCount += objectCount;
		repository.genericObjectCount += genericObjectCount;
		repository.instanceFieldCount += sharedFieldCount;
		repository.instanceFieldCount += genericFieldCount;
		repository.arrayElementCount += arrayElementCount;
	}
	
	public int getSpecificMethodInvocationCount() {
		return specificInvocationCount;
//		int count = (specificMethodInvocations == null) ? 0 : Repository.doCount(specificMethodInvocations);
//		//count += Repository.doCount(genericMethodInvocations);
//		return count;
	}
	
	public int getMethodInvocationCount() {
		return specificInvocationCount + genericInvocationCount;
//		int count = (specificMethodInvocations == null) ? 0 : Repository.doCount(specificMethodInvocations);
//		if(genericMethodInvocations != null) {
//			count += Repository.doCount(genericMethodInvocations);
//		}
//		return count;
	}
	
	public boolean doPropagation() throws PropagationException {
		if(clazz.isPrimitive()) {
			return false;
		}
		int objectCount = Repository.doPropagation(createdObjects);
		int genericObjectCount = Repository.doPropagation(createdGenericObjects);
		int arrayElementCount = (sharedArrayElement != null && sharedArrayElement.doPropagation()) ? 1 : 0;
		int staticFieldCount = (staticFields == null) ? 0 : Repository.doPropagation(staticFields);
		int sharedFieldCount = (sharedFields == null) ? 0 : Repository.doPropagation(sharedFields);
		int genericFieldCount = (genericFields == null) ? 0 : Repository.doPropagation(genericFields);
		int methodInvCount = (specificMethodInvocations == null) ? 0 : Repository.doPropagation(specificMethodInvocations);
		int genericMethodInvCount = (genericMethodInvocations == null) ? 0 : Repository.doPropagation(genericMethodInvocations);
		repository.staticFieldCount += staticFieldCount;
		repository.methodInvCount += methodInvCount;
		repository.genericMethodInvCount += genericMethodInvCount;
		repository.objectCount += objectCount;
		repository.genericObjectCount += genericObjectCount;
		repository.instanceFieldCount += sharedFieldCount;
		repository.instanceFieldCount += genericFieldCount;
		repository.arrayElementCount += arrayElementCount;
//		System.out.println("looked at " 
//				+ ((methodInvocations == null) ? 0 : methodInvocations.size()) + " methods " 
//				+ ((genericMethodInvocations == null) ? 0 : genericMethodInvocations.size()) + " generic methods "
//				+ ((interfaceMethods == null) ? 0 : interfaceMethods.size()) + " interface methods " 
//				+ "in " + this);
		
		return (objectCount | genericObjectCount | arrayElementCount | staticFieldCount 
				| sharedFieldCount | genericFieldCount | methodInvCount | genericMethodInvCount) != 0;
		
	}
	
	public CreatedObject findCreator(PropagatedObject object) {
		SpecificMethodInvocation methods[] = specificMethodInvocations;
		if(methods != null) {
			for(int j=0; j<methods.length; j++) {
				SpecificMethodInvocation method = methods[j];
				while(method != null) {
					ObjectSet created = method.getCreatedObjects();
					if(created != null) {
						ObjectSetEntry entry = created.find(object);
						if(entry instanceof CreatedObject) {
							return (CreatedObject) entry;
						}
					}
					method = (SpecificMethodInvocation) method.next;
				}
			}
		}
		return null;
	}
	
	public boolean isInternal() {
		return repository.repository.isInternalClass(getUnderlyingType());
	}
	
	int getSizeForLocal() {
		return clazz.getSizeForLocal();
	}
	
	boolean isRequired() {
		return isRequired;
	}
	
	boolean isInitialized() {
		return isInitialized;
	}
	
	
//	public Iterator getObjectIterator() {
//		return createdObjects.iterator();
//	}
	
	public ObjectSet getCreatedObjects() {
		return createdObjects;
	}
	
	public ObjectSet getGenericObjects() {
		return createdGenericObjects;
	}
	
	boolean isInstantiated() {
		return isInstantiated;
	}
	
	public boolean mightBeInstanceOf(Clazz type) {
		return mightBeInstanceOf(type.getUnderlyingType());
	}
	
	public boolean isInstanceOf(Clazz type) {
		return isInstanceOf(type.getUnderlyingType());
	}
	
	public boolean mightBeInstanceOf(BT_Class type) {
		return clazz.mightBeInstanceOf(type);
	}
	
	public boolean isInstanceOf(BT_Class type) {
		return clazz.isInstanceOf(type);
	}
	
	public boolean isDescendentOf(BT_Class c) {
		return clazz.isDescendentOf(c);
	}
	
	public boolean isDescendentOf(Clazz c) {
		return clazz.isDescendentOf(c.clazz);
	}
	
	public boolean isInstance(BT_Class c) {
		return clazz.isInstance(c);
	}
	
	public boolean isInstance(Clazz c) {
		return clazz.isInstance(c.clazz);
	}
	
	public boolean mightBeInstance(BT_Class c) {
		return clazz.mightBeInstance(c);
	}
	
	public boolean mightBeInstance(Clazz c) {
		return clazz.mightBeInstance(c.clazz);
	}
	
	boolean isThrowable() {
		if(isThrowable == null) {
			BT_Class javaLangThrowable = repository.getClassProperties().javaLangThrowable;
			boolean bresult = clazz.equals(javaLangThrowable) 
				|| clazz.isDescendentOf(javaLangThrowable);
			isThrowable = bresult ? Boolean.TRUE : Boolean.FALSE;
			if(!bresult) {
				isRuntimeThrowable = Boolean.FALSE;
			}
		}
		return isThrowable.booleanValue();
	}
	
	public boolean isRuntimeThrowable() {
		if(!isThrowable()) {
			return false;
		}
		if(isRuntimeThrowable == null) {
			BT_Class javaLangError = repository.getClassProperties().javaLangError;
			BT_Class javaLangRuntimeException = repository.getClassProperties().javaLangRuntimeException;
			boolean bresult = clazz.isDescendentOf(javaLangRuntimeException)
				|| clazz.isDescendentOf(javaLangError)
				|| clazz.equals(javaLangRuntimeException)
				|| clazz.equals(javaLangError);
			isRuntimeThrowable = bresult ? Boolean.TRUE : Boolean.FALSE;
		}
		return isRuntimeThrowable.booleanValue();
	}

	
	
	void setRequired() {
		if(isRequired) {
			return;
		}
		isRequired = true;
		Clazz arrayType = getArrayType();
		if(arrayType != null) {
			arrayType.setRequired();
		} 
		
		Clazz parents[] = getParents();
		for(int i=0; i<parents.length; i++) {
			Clazz parent = parents[i];
			parent.setRequired();
		}
	}
	
	
	//xxx mark verifier required xxx, do this by passing the context doing the initialization?;
	void setInitialized() {
		if(isInitialized) {
			return;
		}
		isRequired = isInitialized = true;
		if(!repository.getPropagationProperties().isEscapeAnalysis()) {
			Method initializer = getInitializer();
			if(initializer != null) {
				ContextProvider provider = repository.getPropagationProperties().provider;
				CallingContext callingContext = provider.getInitializingContext();
				MethodInvokeFromVM methodInvoke = new MethodInvokeFromVM(repository, 
						callingContext, 
						initializer,
						this);
				methodInvoke.invokeStaticMethod();
			}
		}
		Clazz arrayType = getArrayType();
		if(arrayType != null) {
			arrayType.setInitialized();
		} 
		
		
		//note that when a class is intialized, 
		//the interfaces are not necessarily initialized,
		//and there is no reason for them to be, initialization of 
		//interfaces is required only when a static field read is made
		
		/* initializing all interface parents is a little aggressive, but
		 * a JVM might be this aggressive as well so we need to be
		 */
		Clazz parents[] = getParents();
		for(int i=0; i<parents.length; i++) {
			Clazz parent = parents[i];
			parent.setInitialized();
		}
	}
	
	private void setInstantiated() {
		if(isInstantiated) {
			return;
		}
		setInitialized();
		isInstantiated = true;
		Clazz sup = getSuperClass();
		if(sup != null) {
			sup.setInstantiated();
		}
	}
	
	/**
	 * Get the method in this class that overrides or implements the given method.  The result
	 * method might not be declared in this class, it might be a member of an intermediate class
	 * that exists between the declaring class or baseMethod and this class.
	 * Returns null if no such method exists.
	 */
	Method getOverridingMethod(BT_Method baseMethod) {
		BT_Method overridingMethod;
		if(baseMethod.getDeclaringClass().isInterface()) {
			overridingMethod = repository.relatedMethodMap.getImplementingMethod(clazz, baseMethod);
		}
		else {
			overridingMethod = repository.relatedMethodMap.getOverridingMethod(clazz, baseMethod);
		}
		if(overridingMethod == null) {
			return null;
		}
		
		//the overriding method might not be in this class, it might be in an itermediary clazz between clazz and declaringClass
		BT_Class overridingClassBT = overridingMethod.getDeclaringClass();
		Clazz overridingClass;
		if(clazz.equals(overridingClassBT)) {
			overridingClass = this;
		} else {
			overridingClass = repository.getClazz(overridingClassBT);
		}
		return overridingClass.getMethod(overridingMethod);
	}
	
	Method getInitializer() {
		BT_MethodVector methods = clazz.getMethods();
		for(int k=0; k<methods.size(); k++) {
			BT_Method method = methods.elementAt(k);
			if(method.isStaticInitializer()) {
				return getMethod(method);
			}
		}
		return null;	
	}
	
	Method getFinalizer() {
		if(hasFinalizer == null) {
			Clazz clazz = this;
			Clazz object = repository.getJavaLangObject();
			if(!isInterface() && !clazz.equals(object)) {
				do {
					BT_Method finalizer = clazz.getUnderlyingType().getFinalizerMethod();
					if(finalizer != null) {
						hasFinalizer = Boolean.TRUE;
						return finalizerMethod = clazz.getMethod(finalizer);
					}
					clazz = clazz.getSuperClass();
				} while(clazz != null && !clazz.equals(object));
			}
			hasFinalizer = Boolean.FALSE;
			return null;
		}
		if(hasFinalizer.booleanValue()) {
			return finalizerMethod;
		}
		return null;
	}
	
	Method[] getConstructors() {
		BT_MethodVector constructors = new BT_MethodVector();
		BT_MethodVector methods = clazz.getMethods();
		for(int k=0; k<methods.size(); k++) {
			BT_Method method = methods.elementAt(k);
			if(method.isConstructor()) {
				constructors.addElement(method);
			}
		}
		
		Method consts[] = new Method[constructors.size()];
		for(int i=0; i<consts.length; i++) {
			consts[i] = getMethod(constructors.elementAt(i));
		}
		return consts;
	}
	
	/*
	 * This method can be called even if the repository is changing, because it does a full linear scan.
	 */
	Field contains(BT_Field field) {
		if(!field.getDeclaringClass().equals(clazz)) {
			throw new IllegalArgumentException();
		}
		if(fields == null) {
			return null;
		}
		for(int i=0; i<fields.length; i++) {
			Field f = fields[i];
			if(f != null && f.getField().equals(field)) {
				return f;
			}
		}
		return null;
		//return fields[getFieldIndex(field)] != null;
	}
	
	/*
	 * This method can be called even if the repository is changing, because it does a full linear scan.
	 */
	Method contains(BT_Method method) {
		if(!method.getDeclaringClass().equals(clazz)) {
			throw new IllegalArgumentException();
		}
		if(methods == null) {
			return null;
		}
		for(int i=0; i<methods.length; i++) {
			Method m = methods[i];
			if(m != null && m.getMethod().equals(method)) {
				return m;
			}
		}
		return null;
		//int index = getMethodIndex(method);
		//Method meth = methods[index];
		//return meth != null;
	}
	
	public Method getMethod(BT_Method method, int index) {
		Method result;
		if(!method.getDeclaringClass().equals(clazz)) {
			throw new IllegalArgumentException();
		}
		if(methods == null) {
			methods = new Method[clazz.getMethods().size()];
			result = new Method(method, this, index);
			methods[index] = result;
		} else {
			result = methods[index];
			if(result == null) {
				result = new Method(method, this, index);
				methods[index] = result;
			}
		}
		return result;
	}
	
	public Method getMethod(BT_Method method) {
		int index = getMethodIndex(method);
		return getMethod(method, index);
	}
	
	private int getMethodIndex(BT_Method method) {
		BT_MethodVector ms = clazz.getMethods();
		for(int i=0; i<ms.size(); i++) {
			BT_Method m = ms.elementAt(i);
			if(m.equals(method)) {
				return i;
			}
		}
		throw new IllegalArgumentException();
	}
	
	public Field getField(BT_Field field) {
		if(!field.getDeclaringClass().equals(clazz)) {
			throw new IllegalArgumentException();
		}
		Field result;
		if(fields == null) {
			fields = new Field[clazz.getFields().size()];
			result = new Field(field, this, repository.getClazz(field.getFieldType()));
			fields[getFieldIndex(field)] = result;
		} else {
			int index = getFieldIndex(field);
			result = fields[index];
			if(result == null) {
				result = new Field(field, this, repository.getClazz(field.getFieldType()));
				fields[index] = result;
			}
		}
		return result;
	}
	
	private int getFieldIndex(BT_Field field) {
		BT_FieldVector fs = clazz.getFields();
		for(int i=0; i<fs.size(); i++) {
			BT_Field f = fs.elementAt(i);
			if(f.equals(field)) {
				return i;
			}
		}
		throw new IllegalArgumentException();
	}
	
	ArrayElement getSharedArrayElement() {
		if(sharedArrayElement == null) {
			Clazz eClass = getElementClass();
			sharedArrayElement = new ArrayElement(eClass, null);
		}
		return sharedArrayElement;
	}
	
	void shareMembers() {
		if(isArray()) {
			isSharedArrayElement = true;
		} else {
			BT_FieldVector fields = clazz.fields;
			for(int i=0; i<fields.size(); i++) {
				BT_Field f = fields.elementAt(i);
				if(f.isStatic()) {
					continue;
				}
				Field field = getField(f);
				field.setShared();
			}
			Clazz superClass = getSuperClass();
			if(superClass != null) {
				superClass.shareMembers();
			}
		}
	}
	
	public BT_Class getUnderlyingType() {
		return clazz;
	}
	
	public Iterator getGenericMethodInvocations() {
		if(genericMethodInvocations == null) {
			return emptyList.iterator();
		}
		return new MethodInvocationIterator(genericMethodInvocations);
	}
	
	static final List emptyList = new AbstractList() {
		public boolean contains(Object object) {
			return false;
		}
		public int size() {
			return 0;
		}
		public Object get(int location) {
			throw new IndexOutOfBoundsException();
		}
		public Iterator iterator() {
			return ObjectSet.emptyIterator;
		}
	};
	
	/**
	 * get a method invocation for which there is no caller, such as a main method, a clinit or a finalizer
	 * @param m the method
	 * @return the invocation
	 */
	public GenericMethodInvocation getGenericMethodInvocation(Method method, CallingContext invokedContext) {
//		GenericMethodInvocation result;
//		ContextedMethodInvocation key = new ContextedMethodInvocation(invokedContext, calledMethod);
//		if(genericMethodInvocations == null) {
//			genericMethodInvocations = new HashMap();
//		} else {
//			result = (GenericMethodInvocation) genericMethodInvocations.get(key);
//			if(result != null) {
//				return result;
//			}
//		}
//		result = new GenericMethodInvocation(calledMethod, invokedContext);
//		genericMethodInvocations.put(key, result);
//		return result;
		
//		if(!method.getDeclaringClass().equals(this)) {
//			throw new IllegalArgumentException();
//		}
		GenericMethodInvocation result;
		int index = method.index;
		if(genericMethodInvocations == null) {
			genericMethodInvocations = new GenericMethodInvocation[methods.length];
		} else {
			result = genericMethodInvocations[index];
			while(result != null) {
				if(invokedContext.isSame(result.context)) {
					return result;
				}
				result = (GenericMethodInvocation) result.next;
			}
		}
		result = new GenericMethodInvocation(method, invokedContext);
		GenericMethodInvocation other = genericMethodInvocations[index];
		genericMethodInvocations[index] = result;
		result.next = other;
		genericInvocationCount++;
		return result;
	}
	
	public SpecificMethodInvocation getMethodInvocation(Method calledMethod, CallingContext invokedContext) {
		return getMethodInvocation(calledMethod, invokedContext, 0);
	}
	
	/**
	 * get a method invocation for which there is no caller, such as a main method, a clinit or a finalizer
	 * @param m the method
	 * @return the invocation
	 */
	public SpecificMethodInvocation getMethodInvocation(Method method, CallingContext invokedContext, int depth) {
//		if(!method.getDeclaringClass().equals(this)) {
//			throw new IllegalArgumentException();
//		}
		SpecificMethodInvocation result;
		int index = method.index;
		if(specificMethodInvocations == null) {
			specificMethodInvocations = new SpecificMethodInvocation[methods.length];
		} else {
			result = specificMethodInvocations[index];
			while(result != null) {
				if(invokedContext.isSame(result.context)) {
					return result;
				}
				result = (SpecificMethodInvocation) result.next;
			}
		}
		result = instantiator.create(method, depth, invokedContext);
		SpecificMethodInvocation other = specificMethodInvocations[index];
		specificMethodInvocations[index] = result;
		result.next = other;
		int newDepth = result.getDepth();
		if(newDepth > repository.maxDepth) {
			repository.maxDepth = newDepth;
		}
		specificInvocationCount++;
		return result;
	}
	
	
	
	boolean isString() {
		return isSame(repository.getClassProperties().javaLangString);
	}
	
	ArrayElement getGenericArrayElement(GenericArrayObject owner) {
		if(genericArrayElement == null) {
			Clazz elementType = isArray() ? getElementClass() 
					: repository.getJavaLangObject();
			genericArrayElement = new ArrayElement(elementType, owner);
			GenericObject contents = elementType.instantiateGeneric(
					repository.getPropagationProperties().provider.getGenericContext());
			elementType.addCreated(contents);
			genericArrayElement.addInstantiatedObject(contents);
		}
		return genericArrayElement;
	}
	
	FieldInstance getGenericFieldInstance(Field instanceField) {
		FieldInstance instance;
		if(genericFields == null) {
			genericFields = new FieldInstance[instanceFieldCount];
			instance = null;
		} else {
			instance = genericFields[instanceField.index];
		}
		if(instance == null) {
			instance = new FieldInstance(instanceField);
			if(instanceField.isStatic()) {
				throw new IllegalArgumentException();
			}
			genericFields[instanceField.index] = instance;
			Clazz type = instanceField.getType();
			GenericObject contents = type.instantiateGeneric(repository.getPropagationProperties().provider.getGenericContext());
			instanceField.getType().addCreated(contents);
			instance.addInstantiatedObject(contents);
		}
		return instance;
	}
	
	FieldInstance getSharedFieldInstance(Field instanceField) {
		FieldInstance instance;
		if(sharedFields == null) {
			sharedFields = new FieldInstance[instanceFieldCount];
			instance = null;
		} else {
			instance = sharedFields[instanceField.index];
		}
		if(instance == null) {
			if(instanceField.isStatic()) {
				throw new IllegalArgumentException();
			}
			instance = new FieldInstance(instanceField);
			sharedFields[instanceField.index] = instance;
		}
		return instance;
	}
	
	StaticFieldInstance getStaticFieldInstance(BT_Field staticField) {
		StaticFieldInstance result;
		Field field = getField(staticField);
		if(staticFields == null) {
			staticFields = new StaticFieldInstance[staticFieldCount];
			result = null;
		} else {
			result = staticFields[field.index];
		}
		if(result == null) {
			if(!staticField.isStatic()) {
				throw new IllegalArgumentException();
			}
			if(field.getType().isString() && field.getAttributes().getAttribute("ConstantValue") != null) {
				result = new ConstantStaticFieldInstance(field);
			} else {
				result = new StaticFieldInstance(field);
			}
			PropagationProperties props = repository.getPropagationProperties();
			if(props.isEscapeAnalysis() && props.useGenericObjects()) {
				Clazz type = field.getType();
				GenericObject contents = type.instantiateGeneric(repository.getPropagationProperties().provider.getGenericContext());
				field.getType().addCreated(contents);
				result.addInstantiatedObject(contents);
			}
			staticFields[field.index] = result;
		}
		return result;
	}
	
	public Iterator getStaticFieldInstances() {
		if(staticFields == null) {
			return emptyList.iterator();
		}
		return new ArrayIterator(staticFields);
	}

	public Iterator getMethodInvocations() {
		if(specificMethodInvocations == null && genericMethodInvocations == null) {
			return emptyList.iterator();
		}
		class MethodIterator implements Iterator {
			MethodInvocation next;
			Iterator methodsIterator;
			Iterator genericMethodsIterator;
			
			MethodIterator() {
				if(specificMethodInvocations != null) {
					methodsIterator = new MethodInvocationIterator(specificMethodInvocations);
				}
				if(genericMethodInvocations != null) {
					genericMethodsIterator = new MethodInvocationIterator(genericMethodInvocations);
				}
				getNext();
			}
			
			private void getNext() {
				next = null;
				if(methodsIterator != null) {
					if(methodsIterator.hasNext()) {
						MethodInvocation inv = (MethodInvocation) methodsIterator.next();
						next = inv;
						return;
					} else {
						methodsIterator = null;
					}
				}
				if(genericMethodsIterator != null) {
					if(genericMethodsIterator.hasNext()) {
						MethodInvocation inv = (MethodInvocation) genericMethodsIterator.next();
						next = inv;
						return;
					} else {
						genericMethodsIterator = null;
					}
				}
			}
			
			public boolean hasNext() {
				return next != null;
			}
			  
			public Object next() {
				Object result = next;
				if(result == null) {
					throw new NoSuchElementException();
				}
				getNext();
				return result;
			}
			  
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
		return new MethodIterator();
	}
	
	static class MethodInvocationIterator implements Iterator {
		int i;
		MethodInvocation current;
		MethodInvocation meths[];
		
		MethodInvocationIterator(MethodInvocation meths[]) {
			this.meths = meths;
			getNext();
		}
		
		public boolean hasNext() {
			return current != null;
		}
		
		private void getNext() {
			if(meths != null) {
				if(current != null) {
					current = current.next;
				}
				while(current == null && i < meths.length) {
					current = meths[i++];
				}
			}
		}
		  
		public Object next() {
			Object result = current;
			if(result == null) {
				throw new NoSuchElementException();
			}
			getNext();
			return result;
		}
		  
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public Iterator getNativeMethodInvocations() {
		if(specificMethodInvocations == null) {
			return emptyList.iterator();
		}
		class NativeIterator implements Iterator {
			MethodInvocation next;
			Iterator methodsIterator;
			
			NativeIterator() {
				methodsIterator = new MethodInvocationIterator(specificMethodInvocations);
				//methodsIterator = new MethodInvocationIterator(false);
				getNext();
			}
			
			private void getNext() {
				next = null;
				while(methodsIterator.hasNext()) {
					MethodInvocation inv = (MethodInvocation) methodsIterator.next();
					if(inv.getMethod().isNative()) {
						next = inv;
						break;
					}
				}
			}
			
			public boolean hasNext() {
				return next != null;
			}
			  
			public Object next() {
				Object result = next;
				if(result == null) {
					throw new NoSuchElementException();
				}
				getNext();
				return result;
			}
			  
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
		return new NativeIterator();
	}
	
	/**
	 * gets all method invocations except for those that are interface methods (i.e. the base
	 * method of an escape analysis), those that are generic,
	 * and those that cannot be followed because their code is not available.
	 * @return
	 */
	public Iterator getFollowedMethodInvocations() {
		if(specificMethodInvocations == null) {
			return emptyList.iterator();
		}
		class RegularIterator implements Iterator {
			MethodInvocation next;
			Iterator methodsIterator;
			
			RegularIterator() {
				methodsIterator = new MethodInvocationIterator(specificMethodInvocations);
				getNext();
			}
			
			private void getNext() {
				next = null;
				while(methodsIterator.hasNext()) {
					MethodInvocation inv = (MethodInvocation) methodsIterator.next();
					if(!inv.isGeneric() && !inv.getMethod().cannotBeFollowed()) {
						next = inv;
						break;
					}
				}
			}
			
			public boolean hasNext() {
				return next != null;
			}
			  
			public Object next() {
				Object result = next;
				if(result == null) {
					throw new NoSuchElementException();
				}
				getNext();
				return result;
			}
			  
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
		return new RegularIterator();
	}

	//TODO might want to combine this with instantiateGeneric later
	public void addCreated(GenericObject splitObject) {
		createdGenericObjects.add(splitObject);
	}
	
	public GenericObject instantiateGeneric(AllocationContext allocationContext) {
		if(isString() && repository.getPropagationProperties().shareGenericStrings()) {
			if(repository.genericStringObject == null) {
				return repository.genericStringObject = instantiator.instantiateGeneric(this, allocationContext);
			}
			return repository.genericStringObject;
		}
		return instantiator.instantiateGeneric(this, allocationContext);
	}
	
	boolean isIgnoredInstantiation() {
		return false;
//		String name = clazz.getName();
//		return name.startsWith("java.lang.String") || name.startsWith("java.lang.Object");
	}
	
	public PropagatedObject instantiate(AllocationContext context) {
		setInstantiated();
		PropagatedObject result;
		if(isArray()) {
			result = new ArrayObject(this, context);
		} else {
			if(repository.getPropagationProperties().isReachabilityAnalysis()) {
				if(clazz.getName().startsWith("java.lang.String")) {
					if(clazz.equals(repository.getClassProperties().javaLangString)) {
						if(repository.stringObject == null) {
							repository.stringObject = instantiator.instantiate(this, context);
							createdObjects.add(repository.stringObject);
						}
						return repository.stringObject;
					}
					//Note that if StringBuffer had an obfuscated class name this would fail,
					//but that would not break anything anyway
					else if(clazz.getName().equals("java.lang.StringBuffer")) {
						if(repository.stringBufferObject == null) {
							repository.stringBufferObject = instantiator.instantiate(this, context);
							createdObjects.add(repository.stringBufferObject);
						}
						return repository.stringBufferObject;
					} else if(clazz.getName().equals("java.lang.StringBuilder")) {
						if(repository.stringBuilderObject == null) {
							repository.stringBuilderObject = instantiator.instantiate(this, context);
							createdObjects.add(repository.stringBuilderObject);
						}
						return repository.stringBuilderObject;
					}
				}
			}
			result = instantiator.instantiate(this, context);
		}
		createdObjects.add(result);
		return result;
	}
	
	public boolean isArray() {
		return clazz.isArray();
	}
	
	public boolean isFinal() {
		return clazz.isFinal();
	}
	
	public boolean isAbstract() {
		return clazz.isAbstract();
	}
	
	public Clazz getArrayClass() {
		if(arrayClass == null) {
			arrayClass = repository.getClazz(clazz.getArrayClass());
		}
		return arrayClass;
	}
	
	/*
	 The array type is the type of a component of the array, unless the
	 component has an array type, in which case the array type of the array
	 is the array type of its component.
	 E.g., the element type of "A[][][]" is "A".
	 
	 For a non-array, the array type is null.
	 */
	Clazz getArrayType() {
		if(isArray()) {
			if(arrayType == null) {
				arrayType = repository.getClazz(clazz.arrayType);
			}
			return arrayType;
		}
		return null;
	}
	
	/*
	 The element type is the type of a component of the array.
	 E.g., the element type of "A[][][]" is "A[][]".
	 
	 For a non-array, the element type is null.
	 */
	Clazz getElementClass() {
		if(isArray()) {
			if(elementClass == null) {
				elementClass = repository.getClazz(clazz.getElementClass());
			}
			return elementClass;
		}
		return null;
	}
	
	public Clazz[] getParents() {
		if(parents == null) {
			BT_ClassVector pars = clazz.getParents();
			parents = new Clazz[pars.size()];
			for(int i=0; i<pars.size(); i++) {
				parents[i] = repository.getClazz(pars.elementAt(i));
			}
		}
		return parents;
	}
	
	public Clazz getSuperClass() {
		return superClass;
	}
	
	public boolean isPrimitive() {
		return clazz.isPrimitive();
	}
	
	public boolean isInterface() {
		return clazz.isInterface();
	}
	
	public String getName() {
		return clazz.getName();
	}
	
	boolean packageIsClosed() {
		return repository.getPropagationProperties().packageIsClosed(clazz.packageName());
	}
	
	public String toString() {
		return clazz.toString();
	}
	
	/**
	 * same as equals but type-safe so we don't end up comparing apples with oranges
	 */
	boolean isSame(BT_Class c) {
		return clazz.equals(c);
	}
	
	/**
	 * same as equals but type-safe so we don't end up comparing apples with oranges
	 */
	boolean isSame(Clazz c) {
		return isSame(c.clazz);
	}
	
	void enterVerifierRequiredClasses(CallingContext context) {
		if(repository.getPropagationProperties().isRTSJAnalysis()) {
			ContextProvider provider = repository.getPropagationProperties().provider;
			CallingContext callingContext = provider.getInitializingContext();
			BT_MethodVector methods = clazz.getMethods();
			for(int i=0; i<methods.size(); i++) {
				BT_Method method = methods.elementAt(i);
				Method meth = getMethod(method, i);
				meth.enterVerifierRequiredClasses(callingContext);
			}
		}
	}
}
