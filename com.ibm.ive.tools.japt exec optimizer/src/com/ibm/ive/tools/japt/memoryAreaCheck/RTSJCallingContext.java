package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.CallingContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.DataMember;
import com.ibm.ive.tools.japt.reduction.ita.Field;
import com.ibm.ive.tools.japt.reduction.ita.InstructionLocation;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator.PropagationAction;
import com.ibm.jikesbt.BT_Class;

public class RTSJCallingContext implements CallingContext, Comparable {
	final AllocationContext allocationContext;
	final ThreadType threadType;
	final RTSJContextProvider provider;
	final TypeProperties typeProperties;
	final ErrorReporter reporter;
	final FlagOption followExternal;
	
	public static final int REAL_TIME_ACCESSED = 0x100;
	public static final int NO_HEAP_REAL_TIME_ACCESSED = 0x200 | REAL_TIME_ACCESSED;
	public static final int JAVA_LANG_THREAD_ACCESSED = 0x800;
	public static final int NOT_ACCESSED = 0x400;
	
	RTSJCallingContext(
			AllocationContext allocationContext, 
			ThreadType threadType, 
			RTSJContextProvider provider, 
			TypeProperties props,
			ErrorReporter reporter,
			FlagOption followExternal) {
		this.allocationContext = allocationContext;
		this.threadType = threadType;
		this.provider = provider;
		this.typeProperties = props;
		this.reporter = reporter;
		this.followExternal = followExternal;
	}
	
	public String toString() {
		return threadType + " running in " + allocationContext;
	}
	
	public boolean equals(Object o) {
		if(o instanceof RTSJCallingContext) {
			return isSame((RTSJCallingContext) o);
		}
		return false;
	}
	
	public int compareTo(Object obj) {
		RTSJCallingContext other = (RTSJCallingContext) obj;
		int result = allocationContext.compareTo(other.allocationContext);
		if(result == 0) {
			result = threadType.compareTo(other.threadType);
		}
		return result;
	}
	
	public int hashCode() {
		return allocationContext.hashCode() * threadType.hashCode();
	}
	
	public boolean isSame(CallingContext other) {
		if(other instanceof RTSJCallingContext) {
			RTSJCallingContext otherContext = (RTSJCallingContext) other;
			return allocationContext.isSame(otherContext.allocationContext) 
				&& threadType.isSame(otherContext.threadType);
		}
		return false;
	}
	
	public void enter(Clazz targetClass) {
		markAccess(targetClass);
	}
	
	public void enter(Clazz targetClass, Method method) {
		markAccess(targetClass, method);
	}
	public void enter(Clazz targetClass, Field method) {
		markAccess(targetClass, method);
	}
	
	public boolean cannotBeFollowed(Clazz targetClass, Method called) {
		Clazz declaring = called.getDeclaringClass();
		if(!declaring.isInternal() && !followExternal.isFlagged()) {
			BT_Class clazz = declaring.getUnderlyingType();
			if(!clazz.packageName().equals("javax.realtime") && !clazz.packageName().startsWith("java.lang")) {
						/* we always enter these two packages for RTSJ analysis */
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the context to use for the upcoming invocation
	 */
	public CallingContext getInvokedContext(
			PropagatedObject invokedObject,
			Clazz targetClass,
			Method invoked,
			MethodInvocation from, 
			InstructionLocation fromLocation) {
		AllocationContext context = typeProperties.switchesMemoryArea(invokedObject, invoked.getMethod());
		RTSJCallingContext next;
		if(context != null) {
			next = provider.get(context, threadType);
			if(!next.invocationBarrier(invokedObject, invoked, from, fromLocation)) {
				next = null;
			}
		} else {
			next = this;
		}
		return next;
	}
	
	public AllocationContext getAllocationContext() {
		return allocationContext;
	}
	
	public ThreadType getThreadType() {
		return threadType;
	}
	
	public boolean isValid() {
		return threadType.canReference(allocationContext) == null;
	}
	
	public boolean invocationBarrier(
			PropagatedObject invokedObject, 
			Method invoked, 
			MethodInvocation from,
			InstructionLocation fromLocation) {
		BT_Class error = threadType.canReference(allocationContext);
		if(error != null) {
			reporter.noteInvocationError(new ErrorWrapper(error, this), invokedObject, invoked, from, fromLocation);
			return false;
		}
		return true;
	}
	
	public boolean readBarrier(
			PropagatedObject object, 
			DataMember from, 
			MethodInvocation reader,
			InstructionLocation readerLocation, 
			PropagationAction action) {
		AllocationContext context = object.getAllocationContext();
		BT_Class error = threadType.canReference(context);
		if(error != null) {
			reporter.noteReadAccessError(new ErrorWrapper(error, threadType + " --> " + context), object, reader, readerLocation, from, action);
			return false;
		} 
		return true;
	}
	
	public boolean writeBarrier(
			PropagatedObject object, 
			PropagatedObject toObject, 
			DataMember to, 
			MethodInvocation writer,
			InstructionLocation writerLocation, 
			PropagationAction action) {
		AllocationContext toContext;
		if(toObject == null) {
			toContext = provider.getInitializingContext().getAllocationContext();
		} else {
			toContext = toObject.getAllocationContext();
		}
		AllocationContext context = object.getAllocationContext();
		BT_Class error = toContext.canReference(context);
		if(error != null) {
			reporter.noteWriteAccessError(new ErrorWrapper(error, toContext + " <-- " + context), object, to, writer, writerLocation, action, toObject);
			return false;
		}
		return true;
	}

	private void markAccess(Clazz targetClass, Field field) {
		if(threadType.isRealTime()) {
			int flags = threadType.isNoHeapRealTime() ? NO_HEAP_REAL_TIME_ACCESSED : REAL_TIME_ACCESSED;
			field.setContextFlags(flags);
			markAccess(targetClass);
		}
	}
	
	private void markAccess(Clazz definingClass) {
		if(threadType.isRealTime()) {
			int flags = threadType.isNoHeapRealTime() ? NO_HEAP_REAL_TIME_ACCESSED : REAL_TIME_ACCESSED;
			definingClass.setContextFlags(flags);
		}
	}
	
	public void markAccess(Clazz targetClass, Method method) {
		if(threadType.isRealTime()) {
			int flags = threadType.isNoHeapRealTime() ? NO_HEAP_REAL_TIME_ACCESSED : REAL_TIME_ACCESSED;
			method.setContextFlags(flags);
			markAccess(targetClass);
		}
	}
}
