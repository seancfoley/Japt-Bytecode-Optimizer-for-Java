package com.ibm.ive.tools.japt.escape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import com.ibm.ive.tools.japt.reduction.ita.FieldInstance;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator;
import com.ibm.ive.tools.japt.reduction.ita.ObjectSet;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.ReceivedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.WrappedObject;

public class Finder {
	final Repository rep;
	final MethodInvocation method;
	
	private ObjectSet objectRoots = new ObjectSet();
	private ArrayList propagatorRoots = new ArrayList();
	
	Finder(Repository rep, MethodInvocation method) {
		this.rep = rep;
		this.method = method;
	}
	
	//arguments, thrown objects, returned objects
	public void addRoot(PropagatedObject object) {
		objectRoots.add(object);
	}
	
	//static fields, generic method invocations, native method invocations
	public void addRoot(ObjectPropagator propagator) {
		propagatorRoots.add(propagator);
	}
	
	public ObjectSet reachAllObjects() {
		ObjectSet result = new ObjectSet();
		Stack unvisited = new Stack();
		for(int i=0; i<propagatorRoots.size(); i++) {
			ObjectPropagator root = (ObjectPropagator) propagatorRoots.get(i);
			ObjectSet contained = root.getContainedObjects();
			Iterator iterator = contained.iterator();
			while(iterator.hasNext()) {
				ReceivedObject object = (ReceivedObject) iterator.next();
				PropagatedObject obj = object.getObject();
				if(!result.contains(obj)) {
					result.add(new WrappedObject(obj, root));
					unvisited.push(obj);
				}
			}
		}
		Iterator iterator = objectRoots.iterator();
		while(iterator.hasNext()) {
			PropagatedObject object = (PropagatedObject) iterator.next();
			if(!result.contains(object)) {
				result.add(new WrappedObject(object, object));
				unvisited.push(object);
			}
		}
		
		while(!unvisited.isEmpty()) {
			PropagatedObject object = (PropagatedObject) unvisited.pop();
			if(!object.isArray()) {
				FieldInstance fields[] = object.getFields();
				if(fields == null) {
					continue;
				}
				for(int i=0; i<fields.length; i++) {
					FieldInstance field = fields[i];
					if(field == null) {
						continue;
					}
					ObjectSet contained = field.getContainedObjects();
					find(result, unvisited, field, contained);
				}
			} else {
				ObjectSet contained = object.getContainedObjects();
				find(result, unvisited, object, contained);
			}
		}
		
		return result;
	}

	private void find(ObjectSet result, Stack unvisited,
			Object object, ObjectSet contained) {
		Iterator iterator = contained.iterator();
		while(iterator.hasNext()) {
			PropagatedObject reached = (PropagatedObject) iterator.next();
			if(!result.contains(reached)) {
				result.add(new WrappedObject(reached, object));
				unvisited.push(reached);
			}
		}
	}
	
	
}
