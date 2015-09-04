package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.CallingContext;
import com.ibm.ive.tools.japt.reduction.ita.ConstructedObject;
import com.ibm.ive.tools.japt.reduction.ita.ContextProvider;

/*
 * Note: Initialization poses a challenge for analysis.  This is because it is not possible to tell during static
 * analysis which thread triggers the initialization of a class.  Therefore, in RTSJ it is possible that initialization 
 * could be triggered by a regular thread, an RT, or an NHRT.  If class initializers try to make use of scoped memory then
 * problems can result if the initializing thread is a regular thread.  For instance, if initializers try to make use of the heap,
 * then problems can arise if the intializing thread is an NHRT.
 * 
 * For the purpose of this analysis, the initializing thread is assumed to be a regular thread running in immortal
 * memory. This includes classes that are loaded by the verifier, it is assumed that the initializing thread will
 * load these classes. 
 * In fact, virtual machines which provide aggressive loading will ensure that the initializing thread is a regular thread, 
 * by loading when the application starts, so that loading is never performed by user threads.
 */

/*
 * Note: Finalization poses another challenge for analysis.  This is because it is not possible to tell during static
 * analysis which thread triggers the finalization of an object.  Therefore, in RTSJ it is possible that finalization 
 * of a scoped object could be triggered by an RT, or an NHRT.  
 * 
 * There is no confusion in the case of heap objects, which are typically finalized by a regular thread.
 * 
 * Immortal objects are not finalized at all.
 * 
 */
public class RTSJContextProvider implements ContextProvider {
	final RTSJCallingContext initialContext;
	final RTSJCallingContext scopedFinalizerContext;
	final RTSJCallingContext javaLangThreadContext;
	final RTSJCallingContext initializingContext;
	final TypeProperties props;
	private RTSJCallingContext[][] contexts;
	final ErrorReporter reporter;
	final FlagOption followExternal;
	
	RTSJContextProvider(TypeProperties props, ErrorReporter reporter, FlagOption followExternal) {
		this.props = props;
		this.reporter = reporter;
		this.followExternal = followExternal;
		
		/** 
		 * TODO option for setting entry thread
		 * Note it might be possible that the initial thread is not a regular thread, so this could be a command line option
		 */
		initialContext = new RTSJCallingContext(props.contextProperties.HEAP, props.REGULAR_THREAD, this, props, reporter, followExternal);
		/**
		 * It is not possible to determine exactly in static analysis which thread finalizes a scope.  Here we assume RT instead of NHRT.
		 */
		scopedFinalizerContext = new RTSJCallingContext(props.SCOPED, props.REAL_TIME_THREAD, this, props, reporter, followExternal);
		
		javaLangThreadContext = initialContext;
		
		/** 
		 * TODO option for classes initialized by RT
		 * 
		 * Note it might be possible for a programmer to assume an RT initializes a class, so this could be a command line option
		 */
		initializingContext = new RTSJCallingContext(props.IMMORTAL, props.REGULAR_THREAD, this, props, reporter, followExternal);
		add(initialContext);
		add(scopedFinalizerContext);
		add(javaLangThreadContext);
		add(initializingContext);
	}
	
	void add(RTSJCallingContext context) {
		int index1 = context.getAllocationContext().count;
		if(contexts == null || index1 >= contexts.length) {
			RTSJCallingContext[][] newContexts = new RTSJCallingContext[props.contextProperties.allocationContextCount][];
			if(contexts != null) {
				System.arraycopy(contexts, 0, newContexts, 0, contexts.length);
			}
			contexts = newContexts;
		}
		
		RTSJCallingContext[] list = contexts[index1];
		int index2 = context.getThreadType().count;
		if(list == null || index2 >= list.length) {
			RTSJCallingContext[] newList = new RTSJCallingContext[ThreadType.counter];
			if(list != null) {
				System.arraycopy(list, 0, newList, 0, list.length);
			}
			contexts[index1] = list = newList;
		}
		
		list[index2] = context;
	}
	
	RTSJCallingContext get(AllocationContext allocationContext, ThreadType threadType) {
		RTSJCallingContext res;
		int index1 = allocationContext.count;
		int index2 = threadType.count;
		if(contexts == null || index1 >= contexts.length || contexts[index1] == null || index2 >= contexts[index1].length) {
			add(new RTSJCallingContext(allocationContext, threadType, this, props, reporter, followExternal));
			res = contexts[index1][index2];
		} else {
			res = contexts[index1][index2];
			if(res == null) {
				add(new RTSJCallingContext(allocationContext, threadType, this, props, reporter, followExternal));
				res = contexts[index1][index2];
			}
		}
		return res;
	}
	
	public AllocationContext getGenericContext() {
		return props.GENERIC;
	}
	
	public CallingContext getJavaLangThreadContext() {
		return javaLangThreadContext;
	}
	
	public CallingContext getFinalizingContext(ConstructedObject object) {
		AllocationContext ac = object.getAllocationContext();
		if(ac.isSame(props.SCOPED)) {
			return scopedFinalizerContext;
		}
		if(ac.isSame(props.contextProperties.HEAP)) {
			return initialContext;
		}
		//immortal memory: no finalization
		return null;
	}

	public CallingContext getInitialCallingContext() {
		return initialContext;
	}

	public CallingContext getInitializingContext() {
		return initializingContext;
	}
}
