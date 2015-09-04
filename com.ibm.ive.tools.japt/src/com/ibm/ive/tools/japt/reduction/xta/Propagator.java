package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.ive.tools.japt.reduction.ClassSet;
import com.ibm.ive.tools.japt.reduction.SimpleTreeSet;
import com.ibm.ive.tools.japt.reduction.ClassSet.ClassIterator;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Member;

/**
 * @author sfoley
 *
 */
abstract class Propagator extends Member {

	/**
	 * a propagated object is an object that has moved from one place to the next.
	 * <p>
	 * The possible propagations are:<br>
	 * - a method invocation (invokestatic, invokeinterface, invokevirtual, invokespecial) 
	 * moves objects from the stack of one method to the stack of another
	 * - a field write (putstatic, putfield) moves objects from a method stack to a field in an object instance
	 * - a field read (getstatic, getfield) moves objects from a field in an object instance to a method stack
	 * - an exception object is thrown from one method to another
	 */
	protected BT_HashedClassVector propagatedObjects;
	private ClassSet allPropagatedObjects;
	
	private static final short propagationIsInitialized = 0x4;
	private static final short requiresRepropagation = 0x8;
	
	
	Propagator(Clazz declaringClass) {
		this(declaringClass, null, null);
	}
	
	Propagator(Clazz declaringClass,  BT_HashedClassVector propagatedObjects, ClassSet allPropagatedObjects) {
		super(declaringClass);
		this.allPropagatedObjects = allPropagatedObjects;
		this.propagatedObjects = propagatedObjects;
	}
	
	/**
	 * the member is known to hold an object of the indicated type, 
	 * so propagate this object to other members accessed by this member in the next iteration.
	 */
	protected void addPropagatedObject(BT_Class objectType) {
		if(objectType == null) {
			throw new NullPointerException("invalid added propagated object");
		}
		//trace("adding " + objectType);
		if(propagatedObjects == null) {
			propagatedObjects = new BT_HashedClassVector();
			propagatedObjects.addElement(objectType);
			return;
		}
		propagatedObjects.addUnique(objectType);
	}
	
//	protected void trace(String message) {
//		if(
//				
//				//getName().indexOf("java.text.DateFormat.parse(java.lang.String)") != -1	
//				getName().indexOf("CaffeineMarkEmbeddedBenchmark.run") != -1
//		//|| getName().indexOf("getDateTimeInstance") != -1		
//		//|| getName().indexOf("java.text.DateFormat.getInstance()") != -1		
//		) {
//			if(message != null && message.length() > 0)
//				System.out.println(message);
//			//String s1 = "New objects to propagate: " + ((propagatedObjects == null) ? 0 : propagatedObjects.size()) +  ((propagatedObjects == null) ? "\n" : ":\n" + getListMembers(propagatedObjects) + '\n');
//			//String s2 = "Objects previously propagated: " + ((allPropagatedObjects == null) ? 0 : allPropagatedObjects.size()) +  ((allPropagatedObjects == null) ? "\n" : ":\n" + getListMembers(allPropagatedObjects) + '\n');
//			//if(s1.indexOf("BenchmarkMonitor") != -1) {
//			//	System.out.println(s1);
//			//}
//			//if(s2.indexOf("BenchmarkMonitor") != -1) {
//			//	System.out.println(s2);
//			//}
//			System.out.println(getState());
//		}
//	}
	
	public String getState() {
		return getName() + '\n'  
			+ "Initialized " + isPropagationInitialized() + '\n'
			+ "Requires repropagation " + requiresRepropagation() + '\n'
			+ "Accessed " + isAccessed() + '\n'
			+ "Required " + isRequired() + '\n'
			+ "New objects to propagate: " + ((propagatedObjects == null) ? 0 : propagatedObjects.size()) +  ((propagatedObjects == null) ? "\n" : ":\n" + getListMembers(propagatedObjects) + '\n')
			//+ "Objects previously propagated: " + ((allPropagatedObjects == null) ? 0 : allPropagatedObjects.size()) + '\n'
			+ "Objects previously propagated: " + ((allPropagatedObjects == null) ? 0 : allPropagatedObjects.size()) +  ((allPropagatedObjects == null) ? "\n" : ":\n" + getListMembers(allPropagatedObjects) + '\n')
			;
	}
	
	static String getListMembers(ClassSet set) {
		StringBuffer res = new StringBuffer();
		ClassIterator it = set.iterator();
		while(it.hasNext()) {
			res.append(it.next());
			res.append('\n');
		}
		return res.toString();
	}
	
	static String getListMembers(BT_ClassVector list) {
		StringBuffer res = new StringBuffer();
		for(int i=0; i<list.size(); i++) {
			res.append(list.elementAt(i));
			res.append('\n');
		}
		return res.toString();
	}
	
	/**
	 * Propagate all objects requiring propagation to all possible targets
	 */
	void doPropagation() {
		//trace("");
		/* Note that in XTA it is not possible for there to be a repropagation if initialization has not been done,
		 * but in RTA this is possible */
		if((flags & propagationIsInitialized) == 0) {
			initializePropagation();
			flags |= propagationIsInitialized;
		}
		if(requiresRepropagation()) {
			repropagateObjects();
			migrateTargets();
		}
		propagateObjects();
	}
	
	/**
	 * Propagate those objects which have not been propagated yet
	 */
	protected void propagateObjects() {
		if(propagatedObjects == null || propagatedObjects.size() == 0) {
			return;
		}
		
		/*
		 * Note that it's possible that propagatedObjects might increase in size as we iterate
		 * through this loop
		 */
		for(int i=0; i<propagatedObjects.size(); i++) {
			BT_Class propagatedObject = propagatedObjects.elementAt(i);
			if(propagatedObject == null) {
				throw new NullPointerException("invalid stored prop object");
			}
			propagateNewObject(propagatedObject);
			if(allPropagatedObjects == null) {
				allPropagatedObjects = new SimpleTreeSet();
			}
			allPropagatedObjects.add(propagatedObject);
		}
		propagatedObjects.removeAllElements();
	}
	
	/**
	 * Propagate those objects which have been previously propagated, but
	 * need to be propagated again to the targets which are new.
	 */
	protected void repropagateObjects() {
		if(allPropagatedObjects != null) {
			//SimpleTreeSet.Iterator it = allPropagatedObjects.iterator();
			ClassIterator it = allPropagatedObjects.iterator();
			while(it.hasNext()) {
				BT_Class objectType = it.next();
				if(objectType == null) {
					throw new NullPointerException("invalid stored propagated object");
				}
				propagateOldObject(objectType);	
			}
		}
		flags &= ~requiresRepropagation;
	}
	
	protected boolean requiresRepropagation() {
		return (flags & requiresRepropagation) != 0;
	}
	
	/**
	 * @return whether this member has previously attempted to propagate objects
	 */
	boolean isPropagationInitialized() {
		return (flags & propagationIsInitialized) != 0;
	}
	
	/**
	 * @return whether an object of the indicated type has been propagated already
	 */
	boolean hasPropagated(BT_Class objectType) {
		if(objectType == null) {
			throw new NullPointerException("invalid hasPropagated check");
		}
		return allPropagatedObjects != null && allPropagatedObjects.contains(objectType);
	}
	
	/**
	 * An object has been previously propagated by this member.
	 */
	boolean hasPropagated() {
		return allPropagatedObjects != null && !allPropagatedObjects.isEmpty();
	}
	
	/**
	 * there are objects waiting to be propagated or the member needs to propagate objects that it creates.
	 */
	protected boolean isPropagationRequired() {
		return isAccessed() && (requiresRepropagation() || somethingNewToPropagate() || !isPropagationInitialized()); 
	}
	
	/**
	 * @return true if there is an object waiting to be propagated, false otherwise
	 */
	protected boolean somethingNewToPropagate() {
		return propagatedObjects != null && propagatedObjects.size() > 0; 
	}
	
	/**
	 * there is a new possible destination member for propagated objects, which means
	 * that anything previously propagated should be now propagated to this new member as well.
	 */
	public void scheduleRepropagation() {
		flags |= requiresRepropagation;
	}
	
	/**
	 * Indicate that the given object was created by this member.
	 */
	protected Clazz addCreatedObject(BT_Class creation) {
		//TODO reduction fix involving not targetting explicitly removed items and don't forget could be an array object
		Clazz clazz = getRepository().getClazz(creation);
		clazz.setInstantiated();
		addPropagatedObject(creation);
		return clazz;
	}
	
	protected void addConditionallyCreatedObjects(BT_Member member) {
		BT_ClassVector classes = getRepository().properties.getConditionallyCreatedObjects(member);
		for(int i=0; i<classes.size(); i++) {
			addCreatedObject(classes.elementAt(i));
		}
	}
	
	/**
	 * The member is included as part of an API so we must assume it can be called
	 */
	void propagateFromUnknownSource() {
		setAccessed();
	}
	
	/**
	 * This member is known to hold an object of type objectType.  
	 * Propagate this object to other members to whom this method may pass the object.
	 * @param objectType a non-array, non-primitive class or interface
	 */
	protected abstract void propagateNewObject(BT_Class objectType);
	
	/**
	 * This member is known to hold an object of type objectType.  
	 * There are new locations where this member can propagate objects, so propagate
	 * the is object to only those new locations.
	 * @param objectType a non-array, non-primitive class or interface
	 */
	protected abstract void propagateOldObject(BT_Class objectType);
	
	/**
	 * migrate the set of new targets to the set of all targets
	 */
	protected abstract void migrateTargets();
	
	/**
	 * find those objects which are created within this propagator, 
	 * and also determine where such objects may be propagated.
	 */
	abstract void initializePropagation();
	
	public abstract String getName();
}
