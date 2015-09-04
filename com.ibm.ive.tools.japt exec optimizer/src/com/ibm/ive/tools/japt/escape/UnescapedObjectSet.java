package com.ibm.ive.tools.japt.escape;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import com.ibm.ive.tools.japt.escape.Propagation.MethodPropagation;
import com.ibm.ive.tools.japt.reduction.ita.AccessedPropagator;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.CreatedObject;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.ObjectSet;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.SpecificMethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.WrappedObject;

public class UnescapedObjectSet {
	final Method method;
	final SpecificMethodInvocation invocation;
	final Repository rep;
	
	int maxDepth;
	long analysisTime;
	public boolean exceededDepth;
	final ObjectSet unescapedInstantiated = new ObjectSet(); //contains CreatedObject
	final ObjectSet escapedInstantiated = new ObjectSet(); //contains CreatedObject
	final ObjectSet unescaped = new ObjectSet(); //contains CreatedObject
	final ObjectSet escaped = new ObjectSet(); //contains CreatedObject
	float percentageEscaping;
	float percentageEscapingInitialMethod;
	int totalObjects, totalInstantiated;
	
	UnescapedObjectSet(Repository rep, SpecificMethodInvocation methodInvocation) {
		this.invocation = methodInvocation;
		this.method = methodInvocation.getMethod();
		this.rep = rep;
	}
	
	void findUnescaped(MethodPropagation prop, ObjectSet reachables) {
		SpecificMethodInvocation invocation = prop.methodInvocation;
		ObjectSet createdObjectInstantiations = invocation.getCreatedObjects();
		Iterator iterator = createdObjectInstantiations.iterator();
		//TODO xxx; this extra stuff is unnecessary for the Eclipse GUI version xxx;
		while(iterator.hasNext()) {
			CreatedObject created = (CreatedObject) iterator.next();
			PropagatedObject createdObject = created.getObject();
			WrappedObject match = (WrappedObject) reachables.get(createdObject);
			if(match == null) {
				unescapedInstantiated.add(created);
			} else {
				//created.link = match.link;
				escapedInstantiated.add(created);
			}
		}
		iterator = prop.rep.getClassesIterator();
		while(iterator.hasNext()) {
			Clazz clazz = (Clazz) iterator.next();
			Iterator methodIterator = clazz.getFollowedMethodInvocations();
			while(methodIterator.hasNext()) {
				SpecificMethodInvocation inv = (SpecificMethodInvocation) methodIterator.next();
				if(inv == invocation) {
					//TODO instead of identity comparison to the base method, check for a special "part of application interface" context
					continue;
				}
				createdObjectInstantiations = inv.getCreatedObjects();
				Iterator objectIterator = createdObjectInstantiations.iterator();
				while(objectIterator.hasNext()) {
					CreatedObject created = (CreatedObject) objectIterator.next();
					PropagatedObject propObject = created.getObject();
					
					WrappedObject match = (WrappedObject) reachables.get(propObject);
					if(match == null) {
						unescaped.add(created);
					} else {
						//created.link = match.link;
						escaped.add(created);
					}
				}
			}
		}
		int escInstSize = escapedInstantiated.size();
		int unEscInstSize = unescapedInstantiated.size();
		int escapedCount = escaped.size() + escInstSize;
		int unescapedCount = unescaped.size() + unEscInstSize;
		int total = escapedCount + unescapedCount;
		if(total > 0) {
			percentageEscaping = (escapedCount * 100) / (float) total;
		} else {
			percentageEscaping = 0;
		}
		total = escInstSize + unEscInstSize;
		if(total > 0) {
			percentageEscapingInitialMethod = (escInstSize * 100) / (float) total;
		} else {
			percentageEscapingInitialMethod = 0;
		}
		totalInstantiated = unEscInstSize + escInstSize;
		totalObjects = unescaped.size() + escaped.size() + totalInstantiated;
		
	}
	
	
	public String toString() {
		StringWriter writer = new StringWriter();
		PrintWriter printer = new PrintWriter(writer);
		try {
			write(printer);
		} catch(IOException e) {}
		printer.flush();
		return writer.toString();
	}
	
	public void write(PrintWriter writer) throws IOException {
		writer.print("Starting from any invocation of ");
		writer.print(method);
		writer.println(": ");
		String prefix = "\t";
		if(totalObjects > 0) {
			writer.print(prefix);
			writer.print("Objects instantiated within ");
			writer.print(method);
			writer.println(": ");
			prefix = "\t\t";
			listObjects(prefix, writer, unescapedInstantiated, escapedInstantiated);
			writer.print(prefix);
			int unSize = unescaped.size();
			int size = escaped.size();
			writer.print("Subinvocations instantiate ");
			if(size == 0 && unSize == 0) {
				writer.println("no objects");
			} else {
				writer.print(unSize);
				writer.print(" objects that cannot escape and ");
				writer.print(size);
				writer.println(" objects that can potentially escape");
			}
			
			
			printSubinvocations(writer); 
			writer.println();
			
			
//			writer.print(prefix);
//			writer.print("Objects instantiated within subinvocations of ");
//			writer.print(method);
//			writer.println(": ");
//			prefix = "\t\t\t";
//			listObjects(prefix, writer, unescaped, escaped);
			float roundedPercent = ((int) (percentageEscaping * 100)) / 100;
			if(roundedPercent == 0 && percentageEscaping != 0) {
				roundedPercent += 0.01;
			}
			prefix = "\t";
			writer.print(prefix);
			writer.print("Percentage of instantiated objects escaping: " );
			writer.print(roundedPercent);
			writer.print('%');
		} else {
			prefix = "\t";
			writer.print(prefix);
			writer.println("No objects instantiated");
			printSubinvocations(writer); 
		}
	}

	private void printSubinvocations(PrintWriter writer) {
		String prefix = "\t\t";
		writer.print(prefix);
		writer.println("List of invoked direct subinvocations: ");
		prefix = "\t\t\t";
		AccessedPropagator called[] = invocation.getCalledMethods();
		if(called.length == 0) {
			writer.print(prefix);
			writer.print("No invocations");
		} else {
			for(int i=0; i<called.length; i++) {
				writer.print(prefix);
				writer.println(called[i].toString(method));
			}
			prefix = "\t\t";
			writer.print(prefix);
			int count = rep.getMethodInvocationCount() - (called.length + 1);
			if(count > 0) {
				writer.print("Another ");
				writer.print(count); 
			} else {
				writer.print("No");
			}
			writer.print(" subinvocations were invoked indirectly");
			
		}
	}

	private void listObjects(String prefix, PrintWriter writer, ObjectSet unescaped, ObjectSet escaped) {
		Iterator iterator = unescaped.iterator();
		while(iterator.hasNext()) {
			CreatedObject obj = (CreatedObject) iterator.next();
			writer.print(prefix);
			writer.print("Cannot escape: ");
			writer.println(obj);
		}
		iterator = escaped.iterator();
		while(iterator.hasNext()) {
			CreatedObject obj = (CreatedObject) iterator.next();
			writer.print(prefix);
			writer.print("Can potentially escape: ");
			writer.println(obj);
//			if(!obj.equals(obj.link)) {//TODO do I really want this?
//				writer.print(prefix);
//				//xxx change this to "potentially reachable from" but even then, it does not help to point to a generic object necessarily xxx;
//				writer.print("\tthrough: ");
//				writer.println(obj.link);
//			} else {
//				//writer.println();
//			}
		}
		if(unescaped.size() + escaped.size() == 0) {
			writer.print(prefix);
			writer.println("None");
		}
	}
	
	
}
