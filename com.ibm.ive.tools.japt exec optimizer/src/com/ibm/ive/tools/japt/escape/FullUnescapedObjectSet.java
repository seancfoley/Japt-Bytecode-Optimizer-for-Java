package com.ibm.ive.tools.japt.escape;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import com.ibm.ive.tools.japt.escape.Propagation.MethodPropagation;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.CreatedObject;
import com.ibm.ive.tools.japt.reduction.ita.ObjectSet;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.ReceivedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.SpecificMethodInvocation;

public class FullUnescapedObjectSet extends UnescapedObjectSet {
	ObjectSet unescapedReceived = new ObjectSet();//contains CreatedObject
	ObjectSet unescapedUnreceived = new ObjectSet();//contains CreatedObject
	
	ObjectSet escapedReceived = new ObjectSet();//contains CreatedObject
	ObjectSet escapedUnreceived = new ObjectSet();//contains CreatedObject
	
	FullUnescapedObjectSet(Repository rep, SpecificMethodInvocation methodInvocation) {
		super(rep, methodInvocation);
	}
	
	void findUnescaped(MethodPropagation prop, ObjectSet reachables) {
		super.findUnescaped(prop, reachables);
		SpecificMethodInvocation invocation = prop.methodInvocation;
		ObjectSet received = getReceived(invocation); //includes objects created in invocation and also objects received by invocation
		Iterator iterator = prop.rep.getClassesIterator();
		while(iterator.hasNext()) {
			Clazz clazz = (Clazz) iterator.next();
			Iterator methodIterator = clazz.getFollowedMethodInvocations();
			while(methodIterator.hasNext()) {
				SpecificMethodInvocation inv = (SpecificMethodInvocation) methodIterator.next();
				if(inv == invocation) {
					//TODO instead of identity comparison to the base method, check for a special "part of application interface" context
					continue;
				}
				ObjectSet createdObjectInstantiations = inv.getCreatedObjects();
				Iterator objectIterator = createdObjectInstantiations.iterator();
				while(objectIterator.hasNext()) {
					CreatedObject created = (CreatedObject) objectIterator.next();
					PropagatedObject propObject = created.getObject();
					if(reachables.contains(propObject)) {//the object escaped
						//escapedOther.add(created);
						if(received.contains(propObject)) {//the object ended up in the base method
							escapedReceived.add(created);
						} else {
							escapedUnreceived.add(created);
						}
					} else {
						if(received.contains(propObject)) {
							unescapedReceived.add(created);
						} else {
							unescapedUnreceived.add(created);
						}
					}
				}
			}
		}
	}
	
	/**
	 * gets the objects (PropagatedObject) that made their way to the base method but were not instantiated there.
	 * @return
	 */
	private static ObjectSet getReceived(SpecificMethodInvocation invocation) {
		ObjectSet set = new ObjectSet();
		ObjectSet containedObjects = invocation.getContainedObjects();
		Iterator iterator = containedObjects.iterator();
		while(iterator.hasNext()) {
			ReceivedObject received = (ReceivedObject) iterator.next();
			PropagatedObject receivedObject = received.getObject();
			set.add(receivedObject); 
		}
		return set;
	}
	
	public void write(PrintWriter writer) throws IOException {
		writer.print(method);
		writer.println(":");
		if(unescapedInstantiated.size() > 0) {
			writer.println("instantiated unescaped objects:");
			append(unescapedInstantiated.iterator(), writer);
		}
		if(unescapedReceived.size() > 0) {
			writer.println("received unescaped objects:");
			append(unescapedReceived.iterator(), writer);
		}
		if(unescapedUnreceived.size() > 0) {
			writer.println("nested unescaped objects:");
			append(unescapedUnreceived.iterator(), writer);
		}
		if(unescapedReceived.size() + unescapedUnreceived.size() +  unescapedInstantiated.size() == 0) {
			if(escapedReceived.size() + escapedUnreceived.size() + escapedInstantiated.size() == 0) {
				writer.println("\tNo objects instantiated");
			} else {
				writer.println("\tAll instantiated objects escape");
			}
		}
	}
	
	static void append(Iterator iterator, PrintWriter writer) throws IOException {
		while(iterator.hasNext()) {
			writer.print('\t');
			writer.println(iterator.next().toString());
		}
	}
}
