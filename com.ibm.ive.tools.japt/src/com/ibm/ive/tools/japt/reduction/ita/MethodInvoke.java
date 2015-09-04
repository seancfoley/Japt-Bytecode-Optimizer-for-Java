package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.ParameterLocation;

public abstract class MethodInvoke {
	final Repository rep;
	final PropagationProperties props;
	final Method called;
	final Clazz targetClass;
	
	MethodInvoke(
			Repository rep,
			Method called,
			Clazz targetClass) {
		this.rep = rep;
		this.props = rep.getPropagationProperties();
		this.called = called;
		this.targetClass = targetClass;
	}
	
	public void invokeInstanceMethod(
			PropagatedObject obj,
			boolean isGeneric) {
		
		/* check if we are allowed to call the method */
		if(called.isAbstract() && !isGeneric) {
			called.setRequired();
			return;
		}
		CallingContext invokedContext = getInvokedContext(obj);
		if(invokedContext == null) {
			/* the invocation is disallowed due to an illegal context */
			called.setRequired();
			return;
		}
		invokeInstanceMethod(obj, isGeneric, invokedContext);
	}
	
	/*
	 * Check whether the method can be explicitly invoked. If not, a generic invocation will take place.
	 * 
	 * This includes native methods and stub methods (methods not loaded).
	 * This does not include abstract methods, which we have handled above.
	 */
	boolean cannotBeFollowed() {
		if(called.cannotBeFollowed() 
				&& !props.isReachabilityAnalysis()/* RTSJ and escape analysis both use generics */) {
			return true;
		}
		return false; 
	}
	
	public void invokeInstanceMethod(
			PropagatedObject obj,
			boolean isGeneric,
			CallingContext invokedContext) { 
		/* 
		 * An invocation is generic if it is a virtual invocation being invoked on a generic object.  
		 * This means it is not possible to determine the target, or even a set of possible targets.
		 * The invocation becomes a "sink", it will simply not identify a target.  For escape analysis
		 * it becomes a root, for other analysis it does not provide a benefit other than identifying
		 * locations where the analysis was not possible.
		 * 
		 * We don't need to know what actual code is invoked, we simple create generic objects
		 * for the throwables and return type, although if we can pass arguments to the method then the method itself
		 * will become a root
		 */
		
		/*
		 * Here we use generic methods in place of methods for which we have no code.
		 * This includes native methods and stub methods (methods not loaded).
		 * This does not include abstract methods, which we have handled above.
		 */
		isGeneric |= cannotBeFollowed();
		MethodInvocation invocation = invokeInstanceMethod(invokedContext, isGeneric);
		ReceivedObject sent;
		if(props.useIntraProceduralAnalysis() && !isGeneric) {
			ParameterLocation thisObjLocation = rep.locationPool.getParamLocation(0);
			sent = new TargetedObject(obj, thisObjLocation); 
		} else {
			sent = obj;
		}
		propagateToLocation(sent, invocation);
	}
	
	/**
	 * overriden in subclass
	 */
	void propagateToLocation(ReceivedObject object, MethodInvocation target) {
		if(!target.hasInvoked(object, null, null)) {
			target.addPropagatedObject(object, ObjectPropagator.INVOKED, null);
		}
	}
	
	/**
	 * overriden in subclass
	 */
	MethodInvocation invokeInstanceMethod(CallingContext invokedContext, boolean isGeneric) {
		return invokeMethod(invokedContext, isGeneric);
	}
	
	public void invokeStaticMethod() {
		CallingContext invokedContext = getInvokedContext(null);
		if(invokedContext == null) { /* the invocation is disallowed due to an illegal context */
			called.setRequired(); 
		} else {
			boolean generic = cannotBeFollowed(); /* we use generic methods if we cannot invoke */
			invokeMethod(invokedContext, generic);
		}
	}
	
	MethodInvocation invokeMethod(CallingContext invokedContext, boolean isGeneric) {
		targetClass.setRequired();
		Clazz calledDeclaringClass = called.getDeclaringClass();
		MethodInvocation methodInvocation; 
		if(isGeneric) {
			methodInvocation = calledDeclaringClass.getGenericMethodInvocation(called, invokedContext);
		} else {
			int depthLimit = props.getDepthLimit();
			int depth = getInvokedDepth();
			if(depthLimit >= 0 &&  depth > depthLimit) {
				props.exceededDepth = true;
				methodInvocation = calledDeclaringClass.getGenericMethodInvocation(called, invokedContext);
			} else {
				methodInvocation = calledDeclaringClass.getMethodInvocation(called, invokedContext, depth); 
			}
		}
		invokedContext.enter(targetClass, called);
		methodInvocation.setAccessed();
		return methodInvocation;
	}
	
	abstract CallingContext getInvokedContext(PropagatedObject obj);
		
	abstract int getInvokedDepth();
}
