package com.ibm.ive.tools.japt.escape;

import java.util.Iterator;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.escape.Propagation.MethodPropagation;
import com.ibm.ive.tools.japt.reduction.ClassProperties;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.ive.tools.japt.reduction.ita.CallingContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.ContextProperties;
import com.ibm.ive.tools.japt.reduction.ita.ContextProvider;
import com.ibm.ive.tools.japt.reduction.ita.DefaultInstantiatorProvider;
import com.ibm.ive.tools.japt.reduction.ita.InstructionLocation;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.ObjectSet;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.PropagationException;
import com.ibm.ive.tools.japt.reduction.ita.PropagationProperties;
import com.ibm.ive.tools.japt.reduction.ita.ReceivedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.SpecificMethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.StaticFieldInstance;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Method;

public class EscapeAnalyzer {
	final BT_Method method;
	final Messages messages;
	final Logger logger;
	final JaptRepository japtRepository;
	final Component component;
	final boolean fullAnalysis;
	
	EscapeAnalyzer(
			BT_Method method, 
			JaptRepository rep, 
			Messages messages, 
			Logger logger, 
			Component component, 
			boolean fullAnalysis) {
		this.method = method;
		this.japtRepository = rep;
		this.logger = logger;
		this.messages = messages;
		this.component = component;
		this.fullAnalysis = fullAnalysis;
	}
	
	static boolean hasNanoTime = true;
	
	static long getTime() {
		//nano time is expected, however, it is not available in Foundation 1.0,
		if(hasNanoTime) {
			try {
				return System.nanoTime();
			} catch(NoSuchMethodError e) {
				hasNanoTime = false;
			}
		}
		return System.currentTimeMillis() * 1000000;
	}
	
	UnescapedObjectSet analyze(boolean useIntra, int depthLimit) throws PropagationException {
		long startTime = getTime();
		ClassProperties properties = new ClassProperties(japtRepository);
		final ContextProperties contextProps = new ContextProperties();
		final CallingContext BASE_CONTEXT = contextProps.new SimpleCallingContext() {
			public CallingContext getInvokedContext(
					PropagatedObject invokedObject, 
					Clazz targetClass,
					Method invoked, 
					MethodInvocation from, 
					InstructionLocation fromLocation) {
				return contextProps.SIMPLE_CONTEXT;
			}
			
			public String toString() {
				return "Base context for escape analysis in heap";
			}
		};
		ContextProvider escapeContextProvider = contextProps.new SimpleContextProvider() {
			/* Escape analysis starts with a base context that switches into the HEAP context as the base method is called */
			public CallingContext getInitialCallingContext() {
				return BASE_CONTEXT;
			}
		};
		PropagationProperties props = new PropagationProperties(
				PropagationProperties.ESCAPE_ANALYSIS, 
				escapeContextProvider, 
				new DefaultInstantiatorProvider(properties),
				properties);
		props.storeCreatedInMethodInvocations = true;
		props.verboseIterations = false;
		MethodPropagation prop = propagateMethod(props, contextProps, useIntra, depthLimit);
		Repository rep = prop.rep;
		MethodInvocation invocation = prop.methodInvocation;
		Finder finder = new Finder(rep, invocation);
		/* add the escape roots */
		addRoots(prop, finder, properties);
		
		/* find all objects reachable from the roots */
		ObjectSet reachables = finder.reachAllObjects();
		
		/* find the unescaped objects */
		SpecificMethodInvocation inv = prop.methodInvocation;
		UnescapedObjectSet set = fullAnalysis ? new FullUnescapedObjectSet(rep, inv) : new UnescapedObjectSet(rep, inv);
		set.findUnescaped(prop, reachables);
		set.analysisTime = getTime() - startTime;
		set.maxDepth = rep.maxDepth;
		set.exceededDepth = props.exceededDepth;
		return set;
	}

	private MethodPropagation propagateMethod(
			PropagationProperties props, 
			ContextProperties contextProps, 
			boolean useIntra, 
			int depthLimit) throws PropagationException {
		props.setShareGenericStrings(false);
		props.setUseGenerics(true);
		props.setIntraProceduralAnalysis(useIntra);
		props.setDepthLimit(depthLimit);
		
		//TODO intra configure these properties through command line options
		props.setPropagationLimit(0);
		props.addClosedPackages(new PatternString("java.*"));
		props.addClosedPackages(new PatternString("javax.*"));
		props.addClosedPackages(new PatternString("com.ibm.*"));
		props.addClosedPackages(new PatternString("com.sun.*"));
		props.setTimeLimit(0);
		
		Propagation propagation = new Propagation(method, japtRepository, messages, logger, component, props, contextProps);		
		MethodPropagation prop = propagation.propagate();
		return prop;
	}

	private void addRoots(MethodPropagation prop, Finder finder, ClassProperties properties) {
		Repository rep = prop.rep;
		ObjectSet arguments[] = prop.arguments;
		for(int i=0; i<arguments.length; i++) {
			ObjectSet arg = arguments[i];
			if(arg == null) {
				continue;
			}
			addRoots(finder, arg);
		}
		ObjectSet thisArg = prop.thisArgument;
		if(thisArg != null) {
			addRoots(finder, thisArg);
		}
		
		ObjectSet uncaughtRoots = rep.uncaughtObjects;
		addRoots(finder, uncaughtRoots);
		ObjectSet unreceivedRoots = rep.unreceivedReturnedObjects;
		addRoots(finder, unreceivedRoots);
		ObjectSet threadObjects = rep.getThreadObjects();
		addRoots(finder, threadObjects);
		
		//TODO intra ensure: a thread object representation for the main thread? Nope, not necessary since thread objects are roots.
		//TODO intra ensure: thread objects should not be generic: since thread objects are roots, they might as well be generic objects, which will save lots of processing time (but this creates more generic method invocations)
		
		Iterator classIterator = rep.getClassesIterator();
		 while(classIterator.hasNext()) {
			 Clazz clazz = (Clazz) classIterator.next();
			 if(clazz.isPrimitive()) {
				 continue;
			 }
			 ObjectSet genericObjects = clazz.getGenericObjects();
			 addRoots(finder, genericObjects);
			 Iterator staticFieldIterator = clazz.getStaticFieldInstances();
			 while(staticFieldIterator.hasNext()) {
				 StaticFieldInstance staticFieldInstance = (StaticFieldInstance) staticFieldIterator.next();
				 finder.addRoot(staticFieldInstance);
			 }
			 Iterator nativeIterator = clazz.getNativeMethodInvocations();
			 addMethodInvocationRoots(finder, nativeIterator, properties);
			 Iterator genericMethodIterator = clazz.getGenericMethodInvocations();
			 addMethodInvocationRoots(finder, genericMethodIterator, properties);
		 }
	}
	
	private static void addMethodInvocationRoots(Finder finder, Iterator iterator, ClassProperties properties) {
		 while(iterator.hasNext()) {
			 MethodInvocation invocation = (MethodInvocation) iterator.next();
			 if(excludeInvocationRoot(invocation, properties)) {
				 continue;
			 }
			 finder.addRoot(invocation);
		 }
	}

	private static void addRoots(Finder finder, ObjectSet uncaughtRoots) {
		Iterator iterator = uncaughtRoots.iterator();
		while(iterator.hasNext()) {
			PropagatedObject object = ((ReceivedObject) iterator.next()).getObject();
			finder.addRoot(object);
		}
	}
	
	/**
	 * Here we add common native methods which we know are not points of escape.
	 * @param invocation
	 * @return
	 */
	private static boolean excludeInvocationRoot(MethodInvocation invocation, ClassProperties properties) {
		Method method = invocation.getMethod();
		BT_Class javaLangObject = properties.javaLangObject;
		BT_Class javaLangThrowable = properties.javaLangThrowable;
		BT_Class methClass = method.getDeclaringClass().getUnderlyingType();
		if(methClass.equals(javaLangObject) || methClass.equals(javaLangThrowable)) {
			String name = method.toString();
			if(name.equals("java.lang.Object.getClass()")
					 || name.equals("java.lang.Object.hashCode()")
					 || name.equals("java.lang.Object.wait()")
					 || name.equals("java.lang.Object.wait(J)")
					 || name.equals("java.lang.Object.wait(JI)")
					 || name.equals("java.lang.Object.clone()")
					 || name.equals("java.lang.Object.notify()")
					 || name.equals("java.lang.Object.notifyAll()")
					 || name.equals("java.lang.Throwable.fillInStackTrace()")
					 ) {
				 return true;
			 }
		}
		return false;
	}
}
