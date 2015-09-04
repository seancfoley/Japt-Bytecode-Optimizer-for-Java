package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.reduction.ClassProperties;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_ObjectPool;
import com.ibm.jikesbt.BT_StackPool;

/**
 * Creates the Field, Method and Class objects used in the reduction algorithm.
 * It is a propagator and delegates propagation to its classes.
 * 
 * @author sfoley
 *
 */
public class Repository implements Propagator {
	
	private final Clazz classes[];
	int classCount;
	
	final RelatedMethodMap relatedMethodMap;
	private final PropagationProperties propagationProperties;
	private final ContextProperties contextProperties;
	public final JaptRepository repository;
	final Messages messages;
	final Logger logger;
	public EntryPointLister entryPointLister;
	
	
	public final BT_StackPool pool = new BT_StackPool();
	final BT_ObjectPool objectPool = new BT_ObjectPool();
	private Clazz javaLangObject;
	private Clazz javaLangThread;
	private Clazz javaLangRuntimeException;
	private Clazz javaLangError;
	private Clazz javaLangThrowable;
	private Clazz objectArray;
	public final LocationPool locationPool = new LocationPool();
	final boolean isFinalizationEnabled;
	
	HashSet interfaceMethods = new HashSet();
	
	HashMap classMap = new HashMap();
	
	ConstructedObject stringObject; //we cache a couple of objects for multiple instantations
	ConstructedObject stringBufferObject;
	ConstructedObject stringBuilderObject;
	GenericObject genericStringObject;
	
	public int maxDepth;
	
	public final ObjectSet uncaughtObjects = new ObjectSet();
	public final ObjectSet unreceivedReturnedObjects = new ObjectSet();
	
	public Repository(
		PropagationProperties props,
		ContextProperties contextProps,
		JaptRepository repository,
		Messages messages,
		Logger logger,
		Component component) {
			this.propagationProperties = props;
			this.contextProperties = contextProps;
			this.classes = new Clazz[repository.classes.size()];
			ClassProperties properties = props.classProps;
			this.repository = repository;
			this.relatedMethodMap = repository.getRelatedMethodMap();
			this.messages = messages;
			this.logger = logger;
			BT_Class jlo = properties.javaLangObject;
			this.javaLangObject = getClazz(jlo);
			BT_Class jlt = properties.javaLangThread;
			this.javaLangThread = getClazz(jlt);
			isFinalizationEnabled = repository.isFinalizationEnabled;
			if(isFinalizationEnabled) { 
				/* 
				 * we mark the java.lang.Object.finalizer() method,
				 * because it should never be explictly called from the JVM
				 * or elsewhere, but it is needed regardless.
				 */
				BT_Method finalizer = jlo.getFinalizerMethod(true);
				Method fin = javaLangObject.getMethod(finalizer);
				fin.setAccessed();
			}
	}
	
	public AllocationContext getHeap() {
		return contextProperties.HEAP;
	}
	
	public int getAllocationContextCount() {
		return contextProperties.allocationContextCount;
	}
	
	public PropagationProperties getPropagationProperties() {
		return propagationProperties;
	}
	
	public BT_ClassVector getConditionallyCreatedObjects(BT_Member member) {
		return getClassProperties().getConditionallyCreatedObjects(member);
	}
	
	public CreatedObject findCreator(PropagatedObject object) {
		for(int i=0; i<classCount; i++) {
			Clazz clazz = classes[i];
			if(clazz == null) {
				continue;
			}
			CreatedObject creator = clazz.findCreator(object);
			if(creator != null) {
				return creator;
			}
		}
		return null;
	}
	
	public ClassProperties getClassProperties() {
		return propagationProperties.classProps;
	}
	
	public void addInterfaceMethod(MethodInvocation invocation) {
		interfaceMethods.add(invocation);
		//invocation.getMethod().getDeclaringClass().addInterfaceMethod(invocation);
	}
	
	boolean isInterfaceMethod(MethodInvocation invocation) {
		return interfaceMethods.contains(invocation);
	}
	
	/**
	 * Gets all thread objects, except those that are generic, or may be split from generic objects.
	 * @return
	 */
	public ObjectSet getThreadObjects() {
		BT_Class threadClass = javaLangThread.getUnderlyingType();
		BT_ClassVector kids = threadClass.getKids();
		BT_ClassVector allThreadClasses = new BT_ClassVector(1 + kids.size());
		allThreadClasses.addElement(threadClass);
		allThreadClasses.addAll(kids);
		
		ObjectSet result = new ObjectSet();
		for(int i=0; i<allThreadClasses.size(); i++) {
			BT_Class clazz = allThreadClasses.elementAt(i);
			Clazz threadClazz = contains(clazz);
			if(threadClazz != null) {
				ObjectSet createdObjects = threadClazz.getCreatedObjects();
				result.addAll(createdObjects);
			}
		}
		return result;
	}
	
	void addUncaughtException(FieldObject object) {
		uncaughtObjects.add(object);
	}
	
	void addUnreceivedReturned(PropagatedObject object) {
		unreceivedReturnedObjects.add(object);
	}
	
	Clazz getJavaLangThrowable() {
		Clazz res = javaLangThrowable;
		if(res == null) {
			res = getClazz(getClassProperties().javaLangThrowable);
			javaLangThrowable = res;
		}
		return res;
	}
	
	Clazz getJavaLangObject() {
		return javaLangObject;
	}
	
	Clazz getJavaLangThread() {
		return javaLangThread;
	}
	
	Clazz getJavaLangRuntimeException() {
		Clazz res = javaLangRuntimeException;
		if(res == null) {
			res = getClazz(getClassProperties().javaLangRuntimeException);
			javaLangRuntimeException = res;
		}
		return res;
	}
	
	Clazz getJavaLangError() {
		Clazz res = javaLangError;
		if(res == null) {
			res = getClazz(getClassProperties().javaLangError);
			javaLangError = res;
		}
		return res;
	}
	
	
	Clazz getObjectArray() {
		Clazz res = objectArray;
		if(res == null) {
			res = getClazz(getClassProperties().objectArray);
			objectArray = res;
		}
		return res;
	}
	
//	/*
//	 * This method can be called even if the repository is changing, 
//	 * because it does a full linear scan.  This is a safe alternative to contains() or getClazz
//	 * after classes have been removed from the BT_Repository.
//	 */
	Clazz containsFullScan(BT_Class clazz) {
		return contains(clazz);
	}
	
	public Clazz contains(BT_Class clazz) {
		Clazz result = (Clazz) classMap.get(clazz);
		return result;
	}
	
	/**
	 * This method cannot be called if classes have been removed from the BT_Repository after this
	 * repository was created.
	 * This is because this repository stores classes in an array that is identical to the one in the
	 * BT_Repository.  Once a class is removed, they become unsynchronized and this method will start throwing
	 * IllegalStateException.
	 * @param clazz
	 * @return
	 */
	public Clazz getClazz(BT_Class clazz) {
		Clazz result = (Clazz) classMap.get(clazz);
		if(result == null) {
			Instantiator ins = propagationProperties.instantiatorProvider.getInstantiator(clazz);
			result = new Clazz(this, clazz, ins);
			classes[classCount++] = result;
			classMap.put(clazz, result);
		}
		return result;
	}
	
	public Iterator getClassesIterator() {
		if(classes == null) {
			return Clazz.emptyList.iterator();
		}
		return new ArrayIterator(classes, classCount);
	}
	
	static class ArrayIterator implements Iterator {
		private final Object array[];
		int nextIndex = -1;
		int len;
		
		ArrayIterator(Object array[]) {
			this(array, array.length);
		}
		
		ArrayIterator(Object array[], int len) {
			this.array = array;
			this.len = len;
			findNext();
		}
		
		private void findNext() {
			int i=nextIndex + 1;
			for(; i<len; i++) {
				if(array[i] != null) {
					break;
				}
			}
			nextIndex = i;
		}
		
		public Object next() {
			if(nextIndex < len) {
				Object result = array[nextIndex];
				findNext();
				return result;
			}
			throw new NoSuchElementException();
		}
		
		public boolean hasNext() {
			return nextIndex < len;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public int getSpecificMethodInvocationCount() {
		int count = 0;
		for(int i=0; i<classCount; i++) {
			Clazz clazz = classes[i];
			count += clazz.getSpecificMethodInvocationCount();
		}
		return count;
	}
	
	public int getMethodInvocationCount() {
		int count = 0;
		for(int i=0; i<classCount; i++) {
			Clazz clazz = classes[i];
			//if(clazz != null) {
				count += clazz.getMethodInvocationCount();
			//}
		}
		return count;
	}
	
	public void doCount() {
		//int numClasses = 0;
		for(int i=0; i<classCount; i++) {
			Clazz clazz = classes[i];
			//if(clazz != null) {
				//numClasses++;
				clazz.doCount();
			//}
		}
		if(classCount > 0) {
//			System.out.println("classes: " + numClasses);
//			System.out.println("fields: " + fieldCount);	
//			System.out.println("methods: " + methodCount);
//			System.out.println("static fields: " + staticFieldCount);	
//			System.out.println("method invocations: " + methodInvCount);
//			System.out.println("objects: " + objectCount);
//			System.out.println("instance fields: " + instanceFieldCount);
//			System.out.println("generic method invocations: " + genericMethodInvCount);
//			System.out.println("array elements: " + arrayElementCount);
//			System.out.println("generic objects: " + genericObjectCount);
			
			
			//TODO a message
//					messages.ITA_EXTENDED_ITERATION_INFO.log(logger, new String[] {
//							Integer.toString(numClasses),
//							Integer.toString(staticFieldCount),	
//							Integer.toString(methodCount),
//							Integer.toString(objectCount),
//							Integer.toString(instanceFieldCount),
//							Integer.toString(genericMethodCount),
//							Integer.toString(arrayElementCount),
//							Integer.toString(genericObjectCount)
//							});
				
			methodCount = fieldCount = staticFieldCount = genericMethodInvCount = methodInvCount = objectCount = genericObjectCount = instanceFieldCount = arrayElementCount = 0;
		}
	}
	
	int iterationCounter;
	int staticFieldCount;
	int methodInvCount;
	int fieldCount;
	int methodCount;
	int genericMethodInvCount;
	int objectCount;
	int genericObjectCount;
	int instanceFieldCount;
	int arrayElementCount;
	
	int propagationCount; //total propagations by object propagators (fields, methods, array elements)
	
	public boolean doPropagation() throws PropagationException {
		//int numClasses = doPropagation(classes);
		//TODO make this multi-threaded by dividing up the classes into groups of work units.  each time a thread finishes it
		//grabs the next unit.  this method is always called within a while loop - move it inside here so that we can
		//assign threads to the next propagation.  Threads that finish off a propagation (work unit contains the last class) output iteration info.
		//to synchronize:  do not synchronize the propagation of stuff to avoid deadlocks.  synchronize class and method and field creations (instance and static).
		//since a class is propagated by only one thread, no synch required while propagating an object or class or method, such as initialization or whatever.
		//but setting the properties of a propagator (required, initialized, etc) requires sync.  adding propagators and objects to methods/fields need synch.
		int numClasses = 0;
		for(int i=0; i<classCount; i++) {
			Clazz propagator = classes[i];
			if(propagator.doPropagation()) {
				numClasses++;
			}
		}
		
		if(numClasses > 0) {
			if(getPropagationProperties().verboseIterations) {
				if(getPropagationProperties().isEscapeAnalysis()) {
					messages.ITA_EXTENDED_ITERATION_INFO.log(logger, new String[] {
							Integer.toString(++iterationCounter), 
							Integer.toString(numClasses),
							Integer.toString(staticFieldCount),	
							Integer.toString(methodInvCount),
							Integer.toString(objectCount),
							Integer.toString(instanceFieldCount),
							Integer.toString(genericMethodInvCount),
							Integer.toString(arrayElementCount),
							Integer.toString(genericObjectCount)
							});
				} else if(getPropagationProperties().isRTSJAnalysis()) {
					logger.logProgress(JaptMessage.formatStart(((JaptMessage) messages.ITA_EXTENDED_ITERATION_INFO).getComponent()).toString());
					String progress = "Iteration " + ++iterationCounter
						+ " propagated " + numClasses + " classes: " 
						+ staticFieldCount + " static fields, "
						+ methodInvCount + " method invocations followed, "
						+ genericMethodInvCount + " method invocations not followed, and "
						+ (genericObjectCount + objectCount) + " objects comprising "
						+ instanceFieldCount + " object fields and "
						+ arrayElementCount + " array elements" + Logger.endl;
					logger.logProgress(progress);
				} else {
					messages.ITA_ITERATION_INFO.log(logger, new String[] {
							Integer.toString(++iterationCounter), 
							Integer.toString(numClasses),
							Integer.toString(staticFieldCount),	
							Integer.toString(methodInvCount),
							Integer.toString(objectCount),
							Integer.toString(instanceFieldCount),
							Integer.toString(arrayElementCount)
							});
				}
			}
			staticFieldCount = genericMethodInvCount = methodInvCount = objectCount = genericObjectCount = instanceFieldCount = arrayElementCount = 0;
			int depthLimit = propagationProperties.getDepthLimit();
			if(depthLimit >= 0 && maxDepth > depthLimit) {
				throw new PropagationException("exceeded depth limit of " + propagationProperties.depthLimit);
			}
			long timeLimit = propagationProperties.getTimeLimit();
			if(timeLimit > 0 && System.currentTimeMillis() > timeLimit) {
				throw new PropagationException("terminating propagation: time exceeded limit of " + timeLimit + " milliseconds");
			}
			int propagationLimit = propagationProperties.getPropagationLimit();
			if(propagationLimit > 0 && propagationCount > propagationLimit) {
				throw new PropagationException("terminating propagation: propagation count of " + propagationCount + 
						" exceeded limit of " + propagationLimit + 
						" propagations of object propagators (field instances, array elements and method invocations)");
			}
			return true;
		}
		return false;
	}
	
	static int doCount(Member mems[]) {
		int count = 0;
		if(mems != null) {
			for(int i=0; i<mems.length; i++) {
				Member mem = mems[i];
				if(mem != null) {
					count++;
				}
			}
		}
		return count;
	}
	
	static int doCount(MethodInvocation propagators[]) {
		int count = 0;
		for(int i=0; i<propagators.length; i++) {
			MethodInvocation propagator = propagators[i];
			while(propagator != null) {
				count++;
				propagator = propagator.next;
			}
			
		}
		return count;
	}
	
	static int doPropagation(MethodInvocation propagators[]) throws PropagationException {
		int count = 0;
		for(int i=0; i<propagators.length; i++) {
			MethodInvocation propagator = propagators[i];
			while(propagator != null) {
				if(propagator.doPropagation()) {
					count++;
				}
				propagator = propagator.next;
			}
			
		}
		return count;
	}
	
	static int doCount(FieldInstance propagators[]) {
		int count = 0;
		for(int i=0; i<propagators.length; i++) {
			Propagator propagator = propagators[i];
			if(propagator != null) {
				count++;
			}
		}
		return count;
	}
	
	static int doPropagation(Propagator propagators[]) throws PropagationException {
		int count = 0;
		for(int i=0; i<propagators.length; i++) {
			Propagator propagator = propagators[i];
			if(propagator != null && propagator.doPropagation()) {
				count++;
			}
		}
		return count;
	}
	
	static int doCount(ObjectSet set) {
		int count = 0;
		Iterator iterator = set.iterator();
		while(iterator.hasNext()) {
			PropagatedObject obj = (PropagatedObject) iterator.next();
			obj.doCount();
			count++;
		}
		return count;
	}
	
	static int doPropagation(ArrayList list) throws PropagationException {
		return doPropagation(((ArrayList) list.clone()).iterator());
	}
	
	static int doPropagation(ObjectSet propagatorSet) throws PropagationException {
		return doPropagation(((Set) propagatorSet.clone()).iterator());
	}	
	
	static int doPropagation(HashSet propagatorSet) throws PropagationException {
		return doPropagation(((Set) propagatorSet.clone()).iterator());
	}		
		
	static int doPropagation(HashMap propagatorMap) throws PropagationException {
		return doPropagation(((Map) propagatorMap.clone()).values().iterator());
	}
	
	static int doPropagation(TreeMap propagatorMap) throws PropagationException {
		return doPropagation(((Map) propagatorMap.clone()).values().iterator());
	}
	
	static private int doPropagation(Iterator iterator) throws PropagationException {
		int count = 0;
		while(iterator.hasNext()) {
			Propagator propagator = (Propagator) iterator.next();
			if(propagator.doPropagation()) {
				count++;
			}
		}
		return count;
	}
	
	void removeItems(Repository rep, boolean alterClasses, boolean doNotMakeClassesAbstract) {
		
		messages.REMOVING_ITEMS.log(logger);
		
		//by cloning the vector, we know that classes will not be removed from it
		//when they are removed from the repository
		BT_ClassVector classes = (BT_ClassVector) repository.getInternalClasses().clone();
		ReductionStats stats = new ReductionStats();
		stats.originalClassCount = classes.size();
		InternalClassesInterface internalClassesInterface = repository.getInternalClassesInterface();
		
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			/* need a special contains method as we are shrinking the classes in the BT_Repository */
			Clazz clz = rep.containsFullScan(clazz);
			if(clz == null) {
				continue;
			}
			if(alterClasses
					&& !doNotMakeClassesAbstract 
					&& clz.isRequired() 
					&& !clz.isInstantiated() 
					&& !clazz.isInterface() 
					&& !clazz.isAbstract() 
					&& !clazz.isFinal() 
					//being in the interface or in a conditional interface means that there
					//might be some alternative fashion or location in which the class is instantiated,
					//so we cannot make it abstract.
					&& !internalClassesInterface.isInEntireInterface(clazz)
				) {
					messages.MADE_CLASS_ABSTRACT.log(logger, clazz);
					clazz.becomeAbstract();
					stats.classesMadeAbstract++;
			}
		}
		
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			Clazz clz = rep.containsFullScan(clazz);
			if(clz == null || !clz.isRequired()) {
				/*
				 * Note that in some cases we will be removing a class here that has already
				 * been removed because its superclass was removed, or in the case of an interface
				 * one of its superinterfaces was removed.  This poses no problems though, since
				 * calling the remove method a second time will not have an effect and we would 
				 * like to have the log message and the stats anwyay
				 */
				clazz.remove();
				messages.REMOVED_UNUSED_CLASS.log(logger, new java.lang.Object[] {clazz.kindName(), clazz});
				stats.classesRemoved++;
				stats.methodsImplicitlyRemoved += clazz.getMethods().size();
				stats.fieldsImplicitlyRemoved += clazz.getFields().size();
				continue;
			}
			
			if(alterClasses) {
				BT_MethodVector methods = clazz.getMethods();
				stats.originalMethodCount += methods.size();
				for(int j=0; j<methods.size(); j++) {
					BT_Method method = methods.elementAt(j);
					if(method.isStaticInitializer()) {
						if(!clz.isInitialized()) {
							method.remove();
							messages.REMOVED_UNUSED_METHOD.log(logger, method.useName());
							stats.methodsRemoved++;
							j--;
						}
						continue;
					}
					
					if(removeMethod(stats, rep, method, clazz)) {
						j--;
					}
					
				}
	
				BT_FieldVector fields = clazz.getFields();
				stats.originalFieldCount += fields.size();
				for(int j=0; j<fields.size(); j++) {
					BT_Field field = fields.elementAt(j);
					BT_Class declaringClass = field.getDeclaringClass();
					Clazz declaringClazz = rep.containsFullScan(declaringClass);
					if(declaringClazz != null) {
						Field f = declaringClazz.contains(field);
						if(f != null && f.isRequired()) {
							continue;
						}	
					}
					field.remove();
					messages.REMOVED_UNUSED_FIELD.log(logger, field.useName());
					stats.fieldsRemoved++;
					j--;
				}	
			}
		}
		
		messages.SUMMARY.log(logger, new String[] {
			Integer.toString(stats.classesRemoved),
			Integer.toString(stats.fieldsImplicitlyRemoved),
			Integer.toString(stats.methodsImplicitlyRemoved),
			Integer.toString(stats.classesMadeAbstract),
			Integer.toString(stats.fieldsRemoved),
			Integer.toString(stats.originalFieldCount),
			Integer.toString(stats.methodsRemoved),
			Integer.toString(stats.originalMethodCount),
			Integer.toString(stats.simplifiedMethods),
			Integer.toString(stats.abstractedMethods)});
	}
	
	boolean removeMethod(
			ReductionStats stats, 
			Repository rep, 
			BT_Method method, 
			BT_Class declaringClass) {
		Method mtd;
		
		/* need a special contains method as we are shrinking the classes in the BT_Repository */
		Clazz declaringClazz = rep.containsFullScan(declaringClass);
				
		//if the method has been marked required in the repository, or it must be kept for other reasons
		if(declaringClazz != null && (mtd = declaringClazz.contains(method)) != null && mtd.isRequired()) {
			if(mtd.isCalled() || method.isAbstract() || method.isNative() || method.isStub()) {
				return false;	
			}
			if(!declaringClass.isAbstract()
				|| declaringClass.isFinal() //a final class cannot become abstract
				|| method.isFinal() //a final method cannot become abstract
				|| method.isStatic() //static methods cannot become abstract
				|| repository.methodFulfillsClassRequirements(method, false) //the method must remain because it overrides an abstract method
														//or implements an interface method, in which case it cannot become abstract
				) {
					if(!method.simplyReturns()) {
						stats.simplifiedMethods++;
						method.makeCodeSimplyReturn();
						messages.REMOVED_CODE_FROM_METHOD.log(logger, method.useName());
					}
			} else {
				stats.abstractedMethods++;
				method.removeCode(); //TODO how is this OK if it is not overridden in a non-abstract child class?  
									//Is this covered by methodFulfillsClassRequirements?  I don't think so.  But this probably does not
									//cause any runtime exceptions cuz we know method is not called, and therefore there is no AbstractMethodError
									//when method resolution takes place, and when the child classes are loaded there is no check that
									//they contain abstract methods!
									
				messages.MADE_METHOD_ABSTRACT.log(logger, method.useName());
			}
			
		}
		/* 
		 * We have some rather convoluted code to determine whether a method must remain
		 * because it prevents an instantiated class from becoming abstract (ie implements an
		 * interface method or overrides an abstract superclass method)
		 */
		/*
		 * Note that a method may fulfill class requirements now, but later the abstract method that is implements or
		 * overrides may be removed, so running reduction once more would cause it to be removed.  In fact, we might wish to
		 * re-run this method a second time.
		 */
		/*
		 * There are other cases: if a method is not abstract in an abstract class, but is not overridden in a subclass, 
		 * the method can be made abstract if the subclass is removed first, or the subclass is made abstract. 
		 */
		else if(repository.methodFulfillsClassRequirements(method, true)) {//TODO methodFulfillsClassRequirements can make an abstract method non-abstract, so we should log that here if it happens
			if(!method.isNative() && !method.isStub() && !method.simplyReturns() && !method.isAbstract()) {//no sense in altering the code attribute if there is no code attribute
				stats.simplifiedMethods++;
				method.makeCodeSimplyReturn();
				messages.REMOVED_CODE_FROM_METHOD.log(logger, method.useName());
			}
		} else {
			method.remove();
			messages.REMOVED_UNUSED_METHOD.log(logger, method.useName());
			stats.methodsRemoved++;
			return true;
		}
		return false;
	}
	
	private static class ReductionStats {
		int classesMadeAbstract;
		int classesRemoved;
		int methodsRemoved;
		int methodsImplicitlyRemoved;
		int fieldsImplicitlyRemoved;
		int fieldsRemoved;
		int simplifiedMethods;
		int abstractedMethods;
		int originalClassCount;
		int originalMethodCount;
		int originalFieldCount;
	}
}
