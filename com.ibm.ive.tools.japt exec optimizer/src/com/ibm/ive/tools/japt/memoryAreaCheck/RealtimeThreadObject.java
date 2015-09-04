package com.ibm.ive.tools.japt.memoryAreaCheck;

import java.util.TreeSet;

import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.ThreadObject;


public class RealtimeThreadObject extends ThreadObject {

	TreeSet initialMemoryAreas = new TreeSet(); //contains AllocationContext
	TypeProperties typeProps;
	
	/**
	 * 
	 * @param type either javax.realtime.RealtimeThread or javax.realtime.NoHeapRealtimeThread
	 * @param context the area in which the object was allocated
	 */
	public RealtimeThreadObject(Clazz type, AllocationContext context, TypeProperties typeProps) {
		super(type, context);
		this.typeProps = typeProps;
	}
	
	public void addDefaultRunContext() {
		//overrides super class, since realtime threads run only in their IMAs so there is no default
	}
	
	boolean hasPropagated(AllocationContext context) {
		return initialMemoryAreas.contains(context);
	}
	
	void addAllocationContext(AllocationContext context) {
		initialMemoryAreas.add(context);
	}
	
	ThreadType convertToThreadType() {
		return typeProps.convertToThreadType(getType());
	}

}
