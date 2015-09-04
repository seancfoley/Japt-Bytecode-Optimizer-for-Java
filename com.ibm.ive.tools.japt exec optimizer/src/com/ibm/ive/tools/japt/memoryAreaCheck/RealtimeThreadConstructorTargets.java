package com.ibm.ive.tools.japt.memoryAreaCheck;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.reduction.ita.AccessedPropagator;
import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;

/**
 * Thread objects are considered the "targets" and IMA objects are propagated to these targets.
 * So when a new thread object is created, repropagation occurs if there is the possibility that previously propagated
 * IMA objects can be paired with these thread objects.  When IMA objects are added, then they are paired with
 * any thread targets that apply.
 * @author sfoley
 *
 */
public class RealtimeThreadConstructorTargets {
	private ArrayList threadObjects; //contains ObjectSource
	private RealtimeThreadConstructor constructor;
	
	RealtimeThreadConstructorTargets(RealtimeThreadConstructor constructor) {
		this.constructor = constructor;
	}
	
	void migrate(RealtimeThreadConstructorTargets to) {
		if(to.threadObjects == null) {
			to.threadObjects = threadObjects;
			threadObjects = null;
		} else if(threadObjects != null) {
			to.threadObjects.addAll(threadObjects);
			threadObjects = null;
		} 
	}
	
	boolean hasThreadTarget(ObjectSource source) {
		return threadObjects != null && threadObjects.contains(source);
	}
	
	public String toString() {
		return "thread object targets for " + constructor;
	}
	
	static class ObjectSource {
		PropagatedObject object;
		
		/**
		 * The location from which the constructor was called (method invocation and instruction location)
		 */
		AccessedPropagator from;
		
		ObjectSource(PropagatedObject object, AccessedPropagator from) {
			this.object = object;
			this.from = from;
		}
		
		public boolean equals(Object o) {
			if(o instanceof ObjectSource) {
				ObjectSource t = (ObjectSource) o;
				if(object == null) {
					return t.object == null && from.equals(t.from);
				}
				return object.equals(t.object) && from.equals(t.from);
			}
			return false;
		}
		
		public int hashCode() {
			if(object == null) {
				return from.hashCode();
			}
			return object.hashCode() ^ from.hashCode();
		}
		
		public String toString() {
			return object + " from " + from;
		}
	}
	
	void addThreadTarget(ObjectSource source) {
		if(threadObjects == null) {
			threadObjects = new ArrayList(1);
		}
		threadObjects.add(source);
	}
	
	boolean isEmpty() {
		return (threadObjects == null || threadObjects.size() == 0);
	}
	
	/**
	 * we have determined the obj is an IMA and here we pass to each corresponding thread object 
	 */
	void propagateObject(ObjectSource imaSource, AllocationContext context) {
		if(threadObjects == null) {
			return;
		}
		int size = threadObjects.size();
		for(int i=0; i<size; i++) {
			ObjectSource source = (ObjectSource) threadObjects.get(i);
			AccessedPropagator threadFrom = source.from;
			AccessedPropagator imaFrom = imaSource.from;
			if(threadFrom.equals(imaFrom)) {
				RealtimeThreadObject threadObject = (RealtimeThreadObject) source.object;
				tryStartThread(context, threadObject, threadFrom);
			}
		}
	}
	
	/**
	 * 
	 * @param context
	 * @param threadObject
	 * @param imaObject the MemoryArea object indicating the IMA, can be null
	 */
	void tryStartThread(AllocationContext context, 
			RealtimeThreadObject threadObject, 
			AccessedPropagator threadFrom) {
		/* Now we check if we can propagate the object to the field itself */
		if(threadObject.hasPropagated(context)) {
			return;
		}
		threadObject.addAllocationContext(context);
		ThreadType threadType = threadObject.convertToThreadType();
		RTSJCallingContext runContext = constructor.contextProvider.get(context, threadType);


		if(runContext.isValid()) {
			threadObject.addRunContext(runContext);
		} else {/* if not valid, allow the invocation barrier to record this */
			//TODO use a slightly different message, because we are not actually attempting to run in heap memory here,
			//we are constructing a thread that would, so the error message should say "nhrt constructed with heap initial memory area"
			//instread of "nhrt running in heap memory"
			runContext.invocationBarrier(
					threadObject, 
					threadObject.runMethod, 
					(MethodInvocation) threadFrom.propagator,
					threadFrom.location);
		}
	}
}
