package com.ibm.ive.tools.japt.memoryAreaCheck;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.ReceivedObject;
import com.ibm.ive.tools.japt.reduction.ita.TargetedObject;

public class MemAreaObjectTargets {
	private final MemAreaInstantiator method;
	
	private ArrayList memAreaObjects; //contains PropagatedObjects representing type javax.Realtime.MemoryArea
	private ArrayList returnedObjects; //contains ReceivedObjects representing any type
	private ArrayList translatedObjects; //contains PropagatedObjects representing any type
	
	MemAreaObjectTargets(MemAreaInstantiator method) {
		this.method = method;
	}
	
	public String toString() {
		return "mem area object targets for " + method;
	}
	
	void addMemoryArea(PropagatedObject added) {
		if(memAreaObjects == null) {
			memAreaObjects = new ArrayList(1);
		}
		memAreaObjects.add(added);
		if(returnedObjects != null) {
			for(int i=0; i<returnedObjects.size(); i++) {
				ReceivedObject returned = (ReceivedObject) returnedObjects.get(i);
				propagateTranslatedObject(added, returned);
			}
		}
	}

	private void propagateTranslatedObject(PropagatedObject mem, ReceivedObject returned) {
		ReceivedObject received;
		AllocationContext context = method.typeProps.convert(mem);
		PropagatedObject ret = returned.getObject();
		PropagatedObject translated = ret.clone(context);
		if(method.useIntraProceduralAnalysis()) {
			MethodInvocationLocation loc = returned.getLocation();
			if(loc != null) {
				received = new TargetedObject(translated, loc);
			} else {
				received = translated;
			}
		} else {
			received = translated;
		}
		addTranslatedObject(translated);
		method.addInstantiatedObject(received);
	}
	
	void addTranslatedObject(PropagatedObject added) {
		if(translatedObjects == null) {
			translatedObjects = new ArrayList(1);
		}
		translatedObjects.add(added);
	}
	
	void addReturnedObject(ReceivedObject added) {
		if(returnedObjects == null) {
			returnedObjects = new ArrayList(1);
		}
		returnedObjects.add(added);
		if(memAreaObjects != null) {
			for(int i=0; i<memAreaObjects.size(); i++) {
				PropagatedObject mem = (PropagatedObject) memAreaObjects.get(i);
				propagateTranslatedObject(mem, added);
			}
		}
	}
	
	boolean isTranslatedObject(PropagatedObject obj) {
		return translatedObjects != null && translatedObjects.contains(obj);
	}
	
	boolean isEmpty() {
		return (memAreaObjects == null || memAreaObjects.size() == 0);
	}
	
}
