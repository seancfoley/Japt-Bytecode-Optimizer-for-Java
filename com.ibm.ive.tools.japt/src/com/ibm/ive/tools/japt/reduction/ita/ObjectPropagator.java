package com.ibm.ive.tools.japt.reduction.ita;

import java.util.Iterator;

/**
 * A propagator that propagates objects from one propagator to another.  In the ITA algorithm, objects
 * are propagated to fields, methods and array elements, who store these objects and propagate them to other
 * fields, methods and array elements.
 * @author sfoley
 *
 */
public abstract class ObjectPropagator implements Propagator {

	/**
	 * a propagated object is an object that has moved from one place to the next.
	 * <p>
	 * The possible propagations are:<br>
	 * - a method invocation (invokestatic, invokeinterface, invokevirtual, invokespecial) 
	 * moves objects from the stack of one method to the stack of another, and returns objects from one stack to another
	 * - a field write (putstatic, putfield) moves objects from a method stack to a field in an object instance
	 * - a field read (getstatic, getfield) moves objects from a field in an object instance to a method stack
	 * - an exception is thrown from one method to another
	 * - an array read/write from/to an object array element (aaload, aastore)
	 */
	private ObjectSet propagatedObjects;
	private ObjectSet allPropagatedObjects; //TODO combine into one, by putting a wrapper around the just added ones;
	
	/**
	 * used to store booleans as bits.  The lower byte is used to store booleans.  
	 * The top 3 bytes are used to store a 3-byte integerm the method invocation depth.
	 */
	protected int flags;
	
	
	private static final short propagationIsInitialized = 0x1;
	private static final short requiresRepropagation = 0x2;
	private static final short isAccessed = 0x4;
	
	boolean isAccessed() {
		return (flags & isAccessed) != 0;
	}
	
	void setAccessed() {
		flags |= isAccessed;
	}
	
	/**
	 * the member is known to hold an object of the indicated type, 
	 * so propagate this object to other members accessed by this member in the next iteration.
	 */
	public final boolean addPropagatedObject(ReceivedObject object, PropagationAction action, ObjectPropagator from) {
		boolean result = addObject(object);
		if(result) {
			logPropagation(object, action, from);
		}
		return result;
	}
	
	public final boolean addInstantiatedObject(ReceivedObject object) {
		return addPropagatedObject(object, INSTANTIATED, null);
	}
	
	private boolean addObject(ReceivedObject object) {
		if(propagatedObjects == null) {
			propagatedObjects = new ObjectSet();
		}
		boolean added = propagatedObjects.add(object);
		return added;
	}
	
	/**
	 * PropagationAction objects are used solely for logging.
	 * @author sfoley
	 *
	 */
	public static class PropagationAction {
		private final String action;
		private final String referral;
		
		private PropagationAction(String action, String ref) {
			this.action = action;
			this.referral = ref;
		}
		
		private PropagationAction(String action) {
			this(action, null);
		}
		
		public String toString(ReceivedObject object, Member to, ObjectPropagator propagator /* can be null */) {
			return toString(object, to, (java.lang.Object) propagator);
		}
		
		public String toString(ReceivedObject object, ObjectPropagator to, ObjectPropagator propagator /* can be null */) {
			return toString(object, to, (java.lang.Object) propagator);
		}
		
		public String toString(ReceivedObject object, ObjectPropagator to, Member member /* can be null */) {
			return toString(object, to, (java.lang.Object) member);
		}
		
		private String toString(ReceivedObject object, java.lang.Object to, java.lang.Object from /* can be null */) {
			PropagatedObject obj = object.getObject();
			MethodInvocationLocation location = object.getLocation();
			StringBuffer buffer = new StringBuffer(action.length() + 100);
			buffer.append(obj.getName());
			buffer.append(' ');
			buffer.append(action);
			buffer.append(' ');
			buffer.append(to);
			if(location != null) {
				buffer.append(' ');
				buffer.append(location);
			}
			if(from != null) {
				buffer.append(' ');
				buffer.append(referral);
				buffer.append(' ');
				buffer.append(from);
			}
			return buffer.toString();
		}
	}
	public static final PropagationAction THROWN = new PropagationAction("thrown to", "by");
	public static final PropagationAction CAUGHT = new PropagationAction("caught by", "thrown by");
	public static final PropagationAction INSTANTIATED = new PropagationAction("instantiated by");
	public static final PropagationAction INVOKED = new PropagationAction("is the receiver of an", "from");
	public static final PropagationAction WRITTEN = new PropagationAction("written to", "by");
	public static final PropagationAction INVOCATION_ARGUMENT = new PropagationAction("passed as argument to", "by");
	public static final PropagationAction READ = new PropagationAction("read by", "from");
	public static final PropagationAction RETURNED = new PropagationAction("returned to", "by");
		
	public static boolean doLog;
	//public static boolean doLog = true;
	
	
	protected void logPropagation() {
		if(doLog) {
			String string = "propagated " + this;
			System.out.println(string);
		}
	}
	
	protected void logPropagation(ReceivedObject object, PropagationAction action, ObjectPropagator propagator) {
		if(doLog) {
			String string = action.toString(object, this, propagator);
			System.out.println(string);
		}
	}

	/**
	 * Propagate all objects requiring propagation to all possible targets
	 */
	public boolean doPropagation() throws PropagationException {
		boolean result = false;
		if((flags & propagationIsInitialized) == 0) {
			initializePropagation();
			flags |= propagationIsInitialized;
			allPropagatedObjects = new ObjectSet();
			result = true;
		} else if(requiresRepropagation()) {
			repropagateObjects();
			migrateTargets();
			result = true;
		}
		if(propagateObjects()) {
			result = true;
		}
		if(result) {
			logPropagation();
			incrementCounter();
		}
		return result;
	}
	
	private void incrementCounter() {
		getRepository().propagationCount++;
	}
	
	/**
	 * Propagate those objects which have not been propagated yet
	 * @return whether or not anything was propagated
	 */
	boolean propagateObjects() throws PropagationException {
		if(propagatedObjects != null && propagatedObjects.size() > 0) {
			ObjectSet nowPropagating = propagatedObjects;
			propagatedObjects = null;
			do {
				Iterator iterator = nowPropagating.iterator();
				while(iterator.hasNext()) {
					ReceivedObject obj = (ReceivedObject) iterator.next();
					propagateNewObject(obj);
					allPropagatedObjects.add(obj);
				}
				nowPropagating.clear();
				if(propagatedObjects == null || propagatedObjects.size() == 0) {
					propagatedObjects = nowPropagating;
					return true;
				}
				/* now check for objects propaged to myself (can occur only in unusual circumstances)*/
				iterator = propagatedObjects.iterator();
				while(iterator.hasNext()) {
					ReceivedObject obj = (ReceivedObject) iterator.next();
					if(!hasPropagated(obj)) {
						nowPropagating.add(obj);
					}
				}
				propagatedObjects.clear();
			} while(true);
		}
		return false;
	}
	
	/**
	 * Propagate those objects which have been previously propagated, but
	 * need to be propagated again to the targets which are new.
	 */
	boolean repropagateObjects() {
		boolean result = false;
		if(allPropagatedObjects != null && allPropagatedObjects.size() > 0) {
			Iterator it = allPropagatedObjects.iterator();
			//Iterator it = allPropagatedTypes.values().iterator();
			while(it.hasNext()) {
				propagateOldObject((ReceivedObject) it.next());	
			}
			result = true;
		}
		flags &= ~requiresRepropagation;
		return result;
	}
	
	boolean requiresRepropagation() {
		return (flags & requiresRepropagation) != 0;
	}
	
	/**
	 * @return whether this member has previously attempted to propagate objects
	 */
	boolean isInitialized() {
		return (flags & propagationIsInitialized) != 0;
	}
	
	/**
	 * @return whether an object has been propagated already
	 */
	public boolean hasPropagated(ReceivedObject obj) {
		boolean result = allPropagatedObjects != null && allPropagatedObjects.contains(obj);
		return result;
	}
	
	public ObjectSet getContainedObjects() {
		ObjectSet set = allPropagatedObjects;
		if(set == null || set.size() == 0) {
			return ObjectSet.EMPTY_SET;
		}
		return set;
	}
	
	/**
	 * @return whether an object has been previously propagated by this member
	 */
	protected boolean hasPropagated() {
		return allPropagatedObjects != null && allPropagatedObjects.size() > 0;
	}
	
	/**
	 * there is a new possible destination member for propagated objects, which means
	 * that anything previously propagated should be now propagated to this new member as well.
	 */
	protected void scheduleRepropagation() {
		flags |= requiresRepropagation;
	}
	
	/**
	 * This member is known to hold an object of type objectType.  
	 * Propagate this object to other members to whom this method may pass the object.
	 * @param objectType a non-array, non-primitive class or interface
	 */
	abstract void propagateNewObject(ReceivedObject object) throws GenericInvocationException;
	
	/**
	 * This member is known to hold an object of type objectType.  
	 * There are new locations where this member can propagate objects, so propagate
	 * the is object to only those new locations.
	 * @param objectType a non-array, non-primitive class or interface
	 */
	abstract void propagateOldObject(ReceivedObject object);
	
	/**
	 * migrate the set of new targets to the set of all targets
	 */
	abstract void migrateTargets();
	
	/**
	 * find those objects which are created within this propagator, 
	 * and also determine where such objects may be propagated.
	 */
	abstract void initializePropagation();
	
	abstract Repository getRepository();
	
	/**
	 * @return the owner this data member.
	 */
	public abstract Clazz getDefiningClass();
}
