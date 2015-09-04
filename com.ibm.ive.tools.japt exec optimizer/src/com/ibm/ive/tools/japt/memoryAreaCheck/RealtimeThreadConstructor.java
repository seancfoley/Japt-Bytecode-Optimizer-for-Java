package com.ibm.ive.tools.japt.memoryAreaCheck;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.memoryAreaCheck.RealtimeThreadConstructorTargets.ObjectSource;
import com.ibm.ive.tools.japt.reduction.ita.AccessedPropagator;
import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.CallingContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.InstructionLocation;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.PropagationProperties;
import com.ibm.ive.tools.japt.reduction.ita.ReceivedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.SpecificMethodInvocation;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_PopulatedNullProvider;
import com.ibm.jikesbt.BT_StackCell;
import com.ibm.jikesbt.BT_StackCellProvider;
import com.ibm.jikesbt.BT_StackPool;
import com.ibm.jikesbt.BT_StackShapeVisitor;
import com.ibm.jikesbt.BT_StackShapes;

public class RealtimeThreadConstructor extends SpecificMethodInvocation {
	final TypeProperties typeProps;
	private ArrayList imaObjects = new ArrayList(); //contains ObjectSource objects to be propagated to the targets
	protected RealtimeThreadConstructorTargets targets; //consists of thread object targets
	protected RealtimeThreadConstructorTargets newTargets;//consists of thread object targets
	int imaParam = -1;
	AllocationContext defaultContext;
	final RTSJContextProvider contextProvider;
	
	public RealtimeThreadConstructor(
			Method method, 
			int depth, 
			CallingContext context, 
			RTSJContextProvider contextProvider, 
			TypeProperties typeProps) {
		super(method, depth, context);
		this.contextProvider = contextProvider;
		this.typeProps = typeProps;
		BT_Class paramClass = typeProps.classProperties.memClass;
		BT_MethodSignature sig = method.getMethod().getSignature();
		for(int i=0; i<sig.types.size(); i++) {
			BT_Class cls = sig.types.elementAt(i);
			if(paramClass.equals(cls)) {
				imaParam = sig.getArgsSize(i, false) + 1 /* 1 for non-static */;
			}
		}
		if(imaParam < 0) {
			//the IMA is taken from the calling context of the constructor
			defaultContext = context.getAllocationContext();
		} else {
			defaultContext = null;
		}
	}
	
	protected void addCallingMethod(AccessedPropagator callingMethod) {
		MethodInvocation from = (MethodInvocation) callingMethod.propagator;
		InstructionLocation callerLocation = callingMethod.location;
		if(imaParam >= 0) {
			/* we do a code analysis to determine if null can be passed to this constructor by the calling method */
			int instructionIndex = callerLocation.instructionIndex;
			Repository repo = getRepository();
			JaptRepository rep = repo.repository;
			try {
				BT_StackPool pool = repo.pool;
				BT_StackCellProvider provider = new BT_PopulatedNullProvider(rep);
				BT_StackShapeVisitor visitor = new BT_StackShapeVisitor(from.getMethod().getCode(), pool, provider);
				visitor.ignoreUpcasts(true);
				visitor.useMergeCandidates(false);
				BT_StackShapes shapes = visitor.populate();
				try {
					//do not call verifyStacks: if some classes were not loaded, this can cause unwanted exceptions
					BT_StackCell stackShapes[][] = shapes.stackShapes;
					BT_StackCell stack[] = stackShapes[instructionIndex];
					BT_StackCell imaCell = stack[imaParam];
					if(imaCell.getCellType().isNull()) {
						/* this method call can cause a null value to be passed as the IMA */
						/* this means the deault IMA might be used */
						AllocationContext con = context.getAllocationContext();
						if(con != null) {
							this.defaultContext = con;
							ObjectSource objectSource = new ObjectSource(null, new AccessedPropagator(from, callerLocation));
							if(!imaObjects.contains(objectSource)) {
								imaObjects.add(objectSource);
								//initializeTargets();
								//targets.propagateObject(objectSource, con);
								
								if(targets != null) {
									targets.propagateObject(objectSource, con);
								}
								if(newTargets != null) {
									newTargets.propagateObject(objectSource, con);
								}
							}
						}
					}
					
				} finally {
					shapes.returnStacks();
				}
			} catch(BT_CodeException e) {
				/* We could not analyze the code for some reason. */
				getRepository().repository.factory.noteCodeException(e);
			}
		} else {
			/* the deault IMA might be used from this caller */
			AllocationContext con = context.getAllocationContext();
			if(con != null) {
				this.defaultContext = con;
				ObjectSource objectSource = new ObjectSource(null, new AccessedPropagator(from, callerLocation));
				if(!imaObjects.contains(objectSource)) {
					imaObjects.add(objectSource);
					initializeTargets();
					targets.propagateObject(objectSource, con);
				}
			}
		}
		
		super.addCallingMethod(callingMethod);
	}
	
	private RealtimeThreadObject isInvokedThreadObject(ReceivedObject object) {
		Repository rep = getRepository();
		PropagationProperties props = rep.getPropagationProperties();
		if(props.useIntraProceduralAnalysis()) {
			MethodInvocationLocation location = object.getLocation();
			MethodInvocationLocation paramLocation = rep.locationPool.getParamLocation(0);
			if(!paramLocation.equals(location)) {
				return null;
			}
		} 
		PropagatedObject obj = object.getObject();
		Clazz type = obj.getType();
		//TODO currently there is no generic object handling, which means that java.lang.Object or Runnable is not considered here as possible thread objects
		if(!typeProps.classProperties.realtimeThread.isInstance(type.getUnderlyingType())) {
			return null;
		}
		return (RealtimeThreadObject) obj;
	}
	
	public boolean hasReceivedArg(ReceivedObject obj, MethodInvocation from, InstructionLocation callerLocation) {
		if(!(from instanceof RealtimeThreadConstructor)) {
			AllocationContext context = isIMAObject(obj);
			if(context != null) {
				ObjectSource objectSource = new ObjectSource(obj.getObject(), new AccessedPropagator(from, callerLocation));
				if(!imaObjects.contains(objectSource)) {
					imaObjects.add(objectSource);
					//initializeTargets();
					//targets.propagateObject(objectSource, context);
					if(targets != null) {
						targets.propagateObject(objectSource, context);
					}
					if(newTargets != null) {
						newTargets.propagateObject(objectSource, context);
					}
				}
			}
		}
		return super.hasPropagated(obj);
	}
	
	public boolean hasInvoked(ReceivedObject obj, SpecificMethodInvocation from, InstructionLocation callerLocation) {
		if(!(from instanceof RealtimeThreadConstructor)) {
			RealtimeThreadObject threadObject = isInvokedThreadObject(obj);
			if(threadObject != null) {
				ObjectSource objectSource = new ObjectSource(obj.getObject(), new AccessedPropagator(from, callerLocation));
				if(targets == null || !targets.hasThreadTarget(objectSource)) {//new target
					if(hasPropagated() || defaultContext != null) {
						initializeNewTargets();
						newTargets.addThreadTarget(objectSource);
						scheduleRepropagation();
					} else {
						initializeTargets();
						targets.addThreadTarget(objectSource);
					}
//					if(defaultContext != null) {
//						initializeTargets();
//						targets.tryStartThread(defaultContext, threadObject, objectSource.from);
//					}
				}
			}
		}
		return super.hasPropagated(obj);
	}
	
	/**
	 * propagate the default IMA to new Thread object targets
	 */
	protected boolean repropagateObjects() {
		boolean res = false;
		if(defaultContext != null && newTargets != null) {
			/* first we find the corresponding ObjectSource object for this IMA */
			for(int i=0; i<imaObjects.size(); i++) {
				ObjectSource source = (ObjectSource) imaObjects.get(i);
				if(source.object == null) {
					newTargets.propagateObject(source, defaultContext);
					res = true;
				}
			}
		}
		return super.repropagateObjects() || res;
	}
	
	/**
	 * propagate old IMA objects to new Thread object targets
	 */
	protected void propagateOldObject(ReceivedObject obj) {
		if(imaParam >= 0) { /* maybe it is an IMA */
			AllocationContext context = isIMAObject(obj);
			/* propagate this old IMA object to the new thread targets */
			if(context != null && newTargets != null && imaObjects != null) {
				/* first we find the corresponding ObjectSource object for this IMA */
				for(int i=0; i<imaObjects.size(); i++) {
					ObjectSource source = (ObjectSource) imaObjects.get(i);
					if(source.object != null && source.object.equals(obj.getObject())) {
						newTargets.propagateObject(source, context);
					}
				}
			}
		}
		
		super.propagateOldObject(obj);
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.reduction.xta.Propagator#migrate()
	 */
	protected void migrateTargets() {
		if(newTargets == null) {
			return;
		} else if(targets == null) {
			targets = newTargets;
			newTargets = null;
		} else {
			newTargets.migrate(targets);
		}
		super.migrateTargets();
	}
	
	private void initializeTargets() {
		if(targets == null) {
			targets = new RealtimeThreadConstructorTargets(this);
		}
	}
	
	private void initializeNewTargets() {
		if(newTargets == null) {
			newTargets = new RealtimeThreadConstructorTargets(this);
		}
	}
	
	AllocationContext isIMAObject(ReceivedObject obj) {
		if(imaParam < 0) {
			return null;
		}
		Repository rep = getRepository();
		PropagationProperties props = rep.getPropagationProperties();
		if(props.useIntraProceduralAnalysis()) {
			MethodInvocationLocation origin = obj.getLocation();
			MethodInvocationLocation paramLocation = rep.locationPool.getParamLocation(imaParam);
			if(!paramLocation.equals(origin)) {
				return null;
			}
		}
		
		PropagatedObject imaObject = obj.getObject();
		AllocationContext context = typeProps.convert(imaObject);
		return context;
	}
	
}
