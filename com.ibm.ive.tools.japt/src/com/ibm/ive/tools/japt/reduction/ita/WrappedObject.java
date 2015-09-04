package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.ObjectSet.ObjectSetEntry;

/**
 * This class wraps a PropagatedObject to store more information associated with the object.
 * 
 * PropagatedObject is aware of this class, so that when this class is compared to a propagated object, or vice versa,
 * the wrapped objects are unwrapped before the comparison.
 * 
 * @author sfoley
 *
 */
public class WrappedObject extends ObjectSetEntry {
	public final PropagatedObject object;
	//public Object link;
		
	public WrappedObject(PropagatedObject obj) {
		this.object = obj;
	}
	
	public WrappedObject(PropagatedObject obj, Object link) {
		this(obj);
		//this.link = link;
	}
	
	public PropagatedObject getObject() {
		return object;
	}
	
	public int hashCode() {
		return object.hashCode();
	}
	
	public boolean equals(Object o) {
		return object.equals(o);
	}
	
	public int compareTo(Object obj) {
		return object.compareTo(obj);
	}
}
