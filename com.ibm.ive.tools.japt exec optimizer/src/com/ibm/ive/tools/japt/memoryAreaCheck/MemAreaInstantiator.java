package com.ibm.ive.tools.japt.memoryAreaCheck;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.CallingContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.GenericInvocationException;
import com.ibm.ive.tools.japt.reduction.ita.InstructionLocation;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.PropagationProperties;
import com.ibm.ive.tools.japt.reduction.ita.ReceivedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.SpecificMethodInvocation;

/**
 * Represents MemoryArea.newArray or MemoryArea.newInstance methods
 * @author sfoley
 *
 */
public class MemAreaInstantiator extends SpecificMethodInvocation {
	final TypeProperties typeProps;
	protected MemAreaObjectTargets targets;
	final RTSJContextProvider contextProvider;
	
	public MemAreaInstantiator(
			Method method, 
			int depth, 
			CallingContext context, 
			RTSJContextProvider contextProvider, 
			TypeProperties typeProps) {
		super(method, depth, context);
		this.typeProps = typeProps;
		this.contextProvider = contextProvider;
	}

	private void findNewMemAreaTargets(ReceivedObject object) {
		if(!isInvokedMemArea(object)) {
			return;
		}
		RTSJCallingContext next = nextContext(object);
		if(!next.isValid()) {
			return;
		}
		initializeTargets();
		targets.addMemoryArea(object.getObject());
	}
	
	private RTSJCallingContext nextContext(ReceivedObject object) {
		PropagatedObject obj = object.getObject();
		AllocationContext newContext = typeProps.convert(obj);
		ThreadType currentType = ((RTSJCallingContext) context).getThreadType();
		RTSJCallingContext next = contextProvider.get(newContext, currentType);
		return next;
	}
	
	private boolean isInvokedMemArea(ReceivedObject object) {
		Repository rep = getRepository();
		PropagationProperties props = rep.getPropagationProperties();
		if(props.useIntraProceduralAnalysis()) {
			MethodInvocationLocation location = object.getLocation();
			MethodInvocationLocation paramLocation = rep.locationPool.getParamLocation(0);
			if(!paramLocation.equals(location)) {
				return false;
			}
		} 
		PropagatedObject obj = object.getObject();
		Clazz type = obj.getType();
		if(!typeProps.classProperties.memClass.isInstance(type.getUnderlyingType())) {
			return false;
		}
		return true;
	}
	
	public boolean hasInvoked(ReceivedObject obj, SpecificMethodInvocation from, InstructionLocation callerLocation) {
		if(isInvokedMemArea(obj)) {
			RTSJCallingContext next = nextContext(obj);
			if(!next.isValid()) {/* if not valid, allow the invocation barrier to record this */
				next.invocationBarrier(obj.getObject(), getMethod(), from, callerLocation);
			}
		}
		return hasPropagated(obj);
	}

	private void initializeTargets() {
		if(targets == null) {
			targets = new MemAreaObjectTargets(this);
		}
	}
	
	protected void returnNewObject(ArrayList callingMethods, ReceivedObject obj, boolean isInstance) {
		initializeTargets();
		if(targets.isTranslatedObject(obj.getObject())) {
			super.returnNewObject(callingMethods, obj, isInstance);
		} else {
			targets.addReturnedObject(obj);
		}
	}
	
	protected void propagateNewObject(ReceivedObject obj) throws GenericInvocationException {
		findNewMemAreaTargets(obj);
		super.propagateNewObject(obj);
	}
}
