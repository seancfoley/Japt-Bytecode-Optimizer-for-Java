package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.ObjectSet.ObjectSetEntry;

/**
 * An object that is sent to a method.  Includes the location of where the object was sent.
 * 
 * TargetedObject is a ReceivedObject with a non-null location.
 * 
 * A ReceivedObject with a null location is considered equal to the contained PropagatedObject.
 * A ReceivedObject with a non-null location is not considered equal to the contained PropagatedObject.
 * 
 * The location contributes to the identity.
 * 
 * @author sfoley
 *
 */
public class TargetedObject extends ObjectSetEntry implements ReceivedObject {
	private final MethodInvocationLocation location;
	private final PropagatedObject object;
	
	public TargetedObject(PropagatedObject object, MethodInvocationLocation location) {
		if(location == null || object == null) {
			throw new NullPointerException();
		}
		this.object = object;
		this.location = location;
	}
	
	public MethodInvocationLocation getLocation() {
		return location;
	}

	public PropagatedObject getObject() {
		return object;
	}
	
	public int hashCode() {
		return object.hashCode() + location.hashCode();
	}
	
	public boolean equals(Object o) {
		if(o instanceof ReceivedObject) {
			ReceivedObject other = (ReceivedObject) o;
			return object.equals(other.getObject()) 
				&& location.equals(other.getLocation())
				&& object.type.equals(other.getObject().type);
		}
		return false;
	}
	
	/**
	 * compares to any other ReceivedObject
	 */
	public int compareTo(Object obj) {
		ReceivedObject other = (ReceivedObject) obj;
		MethodInvocationLocation otherLocation = other.getLocation();
		PropagatedObject otherObject = other.getObject();
		int result = compare(object, location, otherObject, otherLocation);
		return result;
	}
	
	static int compare(
			PropagatedObject object,
			MethodInvocationLocation location,
			PropagatedObject other,
			MethodInvocationLocation otherLocation) {
		
		/* In PropagatedObject locations are ignored, so if there is no location assigned to obj we do not make any contribution to the comparison */
		
		//Even if two objects have the same hash code, they must also have the same type and location,
		//to be considered the same, which is extremely unlikely
		
		//TODO this is unsafe, if two objects of the same type at the same location provide the same hash code
		//if we use the hashcode of individual items inside, that can help
		//combining with an id in each object that is just a byte might help
		int result = object.hashCode() - other.hashCode();
		if(result != 0) {
			return result;
		}
		
		//location
		if(location == null) {
			if(otherLocation != null) {
				return -1;
			}
		} else {
			if(otherLocation == null) {
				return 1;
			} else {
				result = location.hashCode() - otherLocation.hashCode();
				if(result != 0) {
					return result;
				}
			}
		}
		
		//type
		result = object.type.getUnderlyingType().compareTo(other.type.getUnderlyingType());
		if(result != 0) {
			return result;
		}
		
		//generic or not
		if(object.isGeneric()) {
			if(!other.isGeneric()) {
				return -1;
			} 
		} else {
			if(other.isGeneric()) {
				return 1;
			}
		}
		
		//allocation context
		AllocationContext allOne = object.getAllocationContext();
		AllocationContext allTwo = other.getAllocationContext();
		
		//TODO this is also unsafe for the same reasons as listed above
		result = allOne.hashCode() - allTwo.hashCode();
		if(result != 0) {
			return result;
		}
		
		return result;
	}
	
	public String toString() {
		return object + " at " + location;
	}

}
