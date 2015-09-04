package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_MultiANewArrayIns;
import com.ibm.jikesbt.BT_NewIns;


/**
 * @author sfoley
 * 
 * Represents a method invocation.
 * 
 * As an object propagator, it propagates objects to other method invocations, fields and array elements.
 * It propagates to methods that have been "invoked", methods that have "invoked" this method, fields that it
 * writes to, and array elements that it writes to.
 *
 */
public class SpecificMethodInvocation extends MethodInvocation {

	private static final AccessedPropagator emptyCalled[] = new AccessedPropagator[0];
	
	static final MethodPotentialTargets nullPotentialTargets = new MethodPotentialTargets() {
		public void findNewTargets(ReceivedObject obj, SpecificMethodInvocation invocation) {}
		public void findStaticTargets(SpecificMethodInvocation invocation) {}
	};
	
	protected MethodPotentialTargets potentialTargets;
	protected MethodInvocationTargets targets;
	protected MethodInvocationTargets newTargets;
	
	public SpecificMethodInvocation(Method method, int depth, CallingContext context) {
		super(method, context);
		setDepth(depth);
	}
	
	public ObjectSet getCreatedObjects() {
		return createdObjects;
	}
	
	void setDepth(int depth) {
		flags &= 0xff;
		flags |= depth << 8;
	}
	
	int getDepth() {
		return flags >> 8;
	}
	
	/** 
	 * Only makes sense for instance methods because static methods are always
	 * called as soon as the calling method has been called.  Instance methods are
	 * only called when an owning object instance has been passed to this propagator.
	 */
	MethodInvocation getPreviousCall(
			Method calledMethod,
			CallingContext invokedContext,
			InstructionLocation callLocation,
			boolean generic) {
		AccessedPropagator result;
		if(targets != null) {
			result = targets.getCallTo(calledMethod, invokedContext, callLocation, generic, this);
			if(result == null) {
				if(newTargets != null) {
					result = newTargets.getCallTo(calledMethod, invokedContext, callLocation, generic, this);
					if(result != null) {
						return (MethodInvocation) result.propagator;
					}
				} 
			} else {
				return (MethodInvocation) result.propagator;	
			}
		} else if(newTargets != null) {
			result = newTargets.getCallTo(calledMethod, invokedContext, callLocation, generic, this);
			if(result != null) {
				return (MethodInvocation) result.propagator;
			}
		}
		return null;
	}
	
	public boolean isGeneric() {
		return false;
	}
	
	protected void addWrittenDataMember(AccessedPropagator memberWrittenTo) {
		//track("adding written member " + memberWrittenTo);
		if(hasPropagated()) {
			scheduleRepropagation();
			initializeNewTargets();
			newTargets.addWrittenDataMember(memberWrittenTo);
		} else {
			initializeTargets();
			targets.addWrittenDataMember(memberWrittenTo);
		}
	}
	
	protected void addCalledMethod(AccessedPropagator calledMethod) {
		//track("adding called method " + calledMethod);
		if(hasPropagated()) {
			scheduleRepropagation();
			initializeNewTargets();
			newTargets.addCalledMethod(calledMethod);
		} else {
			initializeTargets();
			targets.addCalledMethod(calledMethod);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public AccessedPropagator[] getCalledMethods() {
		int calledSize;
		if(targets != null && targets.calledMethods != null) {
			calledSize = targets.calledMethods.size();
		} else {
			calledSize = 0;
		}
		int newCalledSize;
		if(newTargets != null && newTargets.calledMethods != null) {
			newCalledSize = newTargets.calledMethods.size();
		} else {
			newCalledSize = 0;
		}
		if(calledSize == 0) {
			if(newCalledSize == 0) {
				return emptyCalled;
			} else {
				ArrayList called = newTargets.calledMethods;
				return (AccessedPropagator[]) called.toArray(new AccessedPropagator[called.size()]);
			}
		} else {
			if(newCalledSize == 0) {
				ArrayList called = targets.calledMethods;
				return (AccessedPropagator[]) called.toArray(new AccessedPropagator[called.size()]);
			} else {
				ArrayList called = targets.calledMethods;
				ArrayList newCalled = newTargets.calledMethods;
				AccessedPropagator result[] = new AccessedPropagator[newCalledSize + calledSize];
				int i=0;
				for(; i<called.size(); i++) {
					result[i] = (AccessedPropagator) called.get(i);
				}
				for(int j=i; j<result.length; j++) {
					result[j] = (AccessedPropagator) newCalled.get(j - i);
				}
				return result;
			}
		}
	}
	
	protected void addCallingMethod(AccessedPropagator callingMethod) {
		//track("adding calling method: " + propagator);
		
		//if(hasPropagated() && method.returnsObjects()) {
		if(hasPropagated()) {
			initializeNewTargets();
			newTargets.addCallingMethod(callingMethod);
			scheduleRepropagation();
			if(hasThrownPropagated()) {
				scheduleThrowingRepropagation();
			}
		} else if(hasThrownPropagated()) {
			initializeNewTargets();
			newTargets.addCallingMethod(callingMethod);
			scheduleThrowingRepropagation();
		} else {
			//note that this method has already been marked as accessed
			initializeTargets();
			targets.addCallingMethod(callingMethod);
		}
	}
	
	protected void migrateTargets() {
		if(newTargets == null) {
			return;
		} else if(targets == null) {
			targets = newTargets;
			newTargets = null;
		} else {
			newTargets.migrate(targets);
		}
	}
	

	protected void propagateNewObject(ReceivedObject obj) throws GenericInvocationException {
		potentialTargets.findNewTargets(obj, this);
		if(targets != null) {
			targets.propagateObject(obj, this);
		}
	}
	
	protected void propagateOldObject(ReceivedObject obj) {
		if(newTargets != null) {
			newTargets.propagateObject(obj, this);
		}
	}

	protected void propagateNewThrownObject(FieldObject obj) {
		if(targets != null) {
			targets.propagateThrownObject(obj, this);
		}
	}
	
	protected void propagateOldThrownObject(FieldObject obj) {
		if(newTargets != null) {
			newTargets.propagateThrownObject(obj, this);
		}
	}
	
	private void initializeTargets() {
		if(targets == null) {
			targets = new MethodInvocationTargets();
		}
	}
	
	private void initializeNewTargets() {
		if(newTargets == null) {
			newTargets = new MethodInvocationTargets();
		}
	}
	
	private void assignPotentialTargetsFromCode() {
		BT_CodeAttribute code = method.getCode();
		int accessedFields = code.accessedFields.size();
		int calledMethods = code.calledMethods.size();
		boolean hasArraySources = method.storesIntoArrays() || method.loadsFromArrays();
		if(accessedFields == 0 && calledMethods == 0) {
			if(hasArraySources) {
				potentialTargets = new MethodPotentialArrayTargets();
			} else {
				potentialTargets = nullPotentialTargets;
			}
		} else {
			final MethodPotentialCodeTargets potentialCodeTargets = new MethodPotentialCodeTargets();
			if(hasArraySources) {
				potentialTargets = new MethodPotentialTargets() {
					MethodPotentialArrayTargets potentialArrayTargets = 
						new MethodPotentialArrayTargets();
					
					public void findNewTargets(ReceivedObject obj, SpecificMethodInvocation invocation) throws GenericInvocationException {
						potentialArrayTargets.findNewTargets(obj, invocation);
						potentialCodeTargets.findNewTargets(obj, invocation);
						if(potentialCodeTargets.hasNoMoreSources()) {
							SpecificMethodInvocation.this.potentialTargets = potentialArrayTargets;
						}
					}
					
					public void findStaticTargets(SpecificMethodInvocation invocation) {
						potentialCodeTargets.findStaticTargets(invocation);
					}
				};
			} else {
				potentialTargets = new MethodPotentialTargets() {
					public void findNewTargets(ReceivedObject obj, SpecificMethodInvocation invocation) throws GenericInvocationException {
						potentialCodeTargets.findNewTargets(obj, invocation);
						if(potentialCodeTargets.hasNoMoreSources()) {
							SpecificMethodInvocation.this.potentialTargets = nullPotentialTargets;
						}
					}
					
					public void findStaticTargets(SpecificMethodInvocation invocation) {
						potentialCodeTargets.findStaticTargets(invocation);
					}
				};
			}
		}
	}
	
	private void assignPotentialTargets() {
		if(method.isNative()) {
			potentialTargets = new MethodPotentialArrayTargets();
		} else if(method.isAbstract() || method.getCode() == null) {
			potentialTargets = nullPotentialTargets;
		} else {
			assignPotentialTargetsFromCode();
		}
	}
	
	/**
	 * The current method has been accessed, so it can now propagate objects elsewhere.
	 * Find all possible targets and determine what objects are created within the body 
	 * of this method.
	 */
	void initializePropagation() {
		ObjectSet createdObjs = null;
		boolean storeCreated = getRepository().getPropagationProperties().storeCreatedInMethodInvocations;
		if(storeCreated) {
			createdObjs = new ObjectSet();
		}
		assignPotentialTargets();
		if(!method.cannotBeFollowed()) {
			potentialTargets.findStaticTargets(this);
			PotentialClassReference instantiated[] = method.getInstantations();
			for(int i=0; i<instantiated.length; i++) {
				PotentialClassReference instantiation = instantiated[i];
				Clazz target = instantiation.instructionTarget;
				if(target.isIgnoredInstantiation()) {
					continue;
				}
				InstructionLocation location = instantiation.location;
				BT_NewIns instruction = (BT_NewIns) location.instruction;
				PropagatedObject created = target.instantiate(context.getAllocationContext());
				ReceivedObject sent;
				if(method.useIntraProceduralAnalysis()) {
					int index = location.instructionIndex;
					sent = new TargetedObject(created, getRepository().locationPool.getStackLocation(index + 1, StackLocation.getTopCellIndex()));
				} else {
					sent = created;
				}
				/* how could a newly instantiated object have been propagated already?  
				 * That might happen in select cases where only a single object 
				 * of a given type is instantiated (ie we share instantiations), as might be the case with java.lang.String.
				 */
				if(!hasPropagated(sent)) {
					addInstantiatedObject(sent); 
					if(storeCreated) {
						createdObjs.add(new CreatedObject(created, this, location));
					}
				}
				if(instruction.isMultiNewArrayIns()) {
					BT_MultiANewArrayIns multiNew = (BT_MultiANewArrayIns) instruction;
					int dimensions = multiNew.dimensions;
					instantiateLowerDimensions((ArrayObject) sent.getObject(), dimensions, createdObjs, location, context.getAllocationContext());
				}
			}
		}
		if(!getRepository().getPropagationProperties().isRTSJAnalysis()) {
			//TODO when not using generics for RTSJ analysis, might want to enable this here, which both creates objects based
			//on rules, but also creates objects for methods not followed: returned and thrown
			Clazz instantiations[] = method.getConditionalInstantiations();
			for(int i=0; i<instantiations.length; i++) {
				Clazz clazz = instantiations[i];
				AllocationContext heap = getRepository().getHeap();
				PropagatedObject object = clazz.instantiate(heap);
				if(!hasPropagated(object)) {
					addInstantiatedObject(object);
				}
				if(object.isArray()) {
					instantiateLowerDimensions((ArrayObject) object, clazz.getUnderlyingType().getArrayDimensions(), null, null, heap);
				}
			}
		}
		if(storeCreated) {
			if(createdObjs.size() > 0) {
				createdObjects = createdObjs;
			} else {
				createdObjects = emptyObjectSet;
			}
		}
	}

	private void instantiateLowerDimensions(ArrayObject current, 
			int dimensions, ObjectSet createdSet, InstructionLocation location, AllocationContext context) {
		while(dimensions > 1) {
			ArrayElement element = current.getArrayElement();
			Clazz elementType = element.getDataType();
			PropagatedObject elementObject = elementType.instantiate(context);
			if(!element.hasPropagated(elementObject)) {
				if(createdSet != null) {
					createdSet.add(new CreatedObject(elementObject, this, location));
				}
				element.addInstantiatedObject(elementObject);
			}
			current = (ArrayObject) elementObject;
			dimensions--;
		}
	}
}
