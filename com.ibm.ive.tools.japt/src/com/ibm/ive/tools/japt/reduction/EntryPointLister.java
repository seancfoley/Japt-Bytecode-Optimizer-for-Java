/*
 * Created on Dec 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reduction;

import java.io.PrintStream;

import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class EntryPointLister {

	private PrintStream out;
	boolean markEntryPoints;
	InternalClassesInterface internalClassesInterface;
	String arrow = " --> ";
	
	/**
	 * An entry point is any element of the internal classes that is accessed from external classes.  The interface
	 * to the internal classes generally consists of all entry points.
	 * This listener object acts upon all entry points found during reduction.
	 * @param out the print stream for listing entry points, may be null for no such listing
	 * @param markEntryPoints whether to mark the found entry points as part of the interface to the internal classes
	 * @param japtRepository
	 */
	public EntryPointLister(PrintStream out, boolean markEntryPoints, JaptRepository japtRepository) {
		this.out = out;
		this.markEntryPoints = markEntryPoints;
		this.internalClassesInterface = japtRepository.getInternalClassesInterface();
	}
	
	/**
	 * indicate that the given internal class is referenced externally
	 * @param clazz
	 * @param from
	 */
	public void foundEntryTo(BT_Class clazz, BT_Item from) {
		if(markEntryPoints) {
			internalClassesInterface.addToInterface(clazz);
			internalClassesInterface.addTargetedClassToInterface(clazz);
		}
		if(out != null) {
			out.print(from.useName());
			out.print(arrow);
			out.println(clazz.useName());
		}
	}
	
	public void foundEntryTo(BT_Class clazz) {
		if(out != null) {
			out.println(clazz.useName());
		}
	}
	
	/**
	 * indicate that the given internal field is referenced from the given external method
	 * @param field
	 * @param from
	 */
	public void foundEntryTo(BT_Field field, BT_Method from) {
		if(markEntryPoints) {
			internalClassesInterface.addToInterface(field);
			internalClassesInterface.addToInterface(field.getDeclaringClass());
			internalClassesInterface.addTargetedClassToInterface(field.getDeclaringClass());
		}
		if(out != null) {
			out.print(from.useName());
			out.print(arrow);
			out.println(field.useName());
		}
	}
	
	public void foundEntryTo(BT_Field field) {
		if(out != null) {
			out.println(field.useName());
		}
	}
	
	/**
	 * indicate that the given internal method is referenced from the given external method, but is not 
	 * called directly but instead overrides or implements a third method.
	 * @param method
	 * @param from
	 */
	public void foundOverridingOrImplementingEntryTo(BT_Method method, BT_Method from) {
		if(markEntryPoints) {
			internalClassesInterface.addToInterface(method);
		}
		if(out != null) {
			out.print(from.useName());
			out.print(arrow);
			out.print(method.qualifiedName());
			out.print(' ');
			out.print('(');
			out.print(method.getDeclaringClass().useName());
			out.println(')');
		}
	}
	
	/**
	 * indicate that the given internal method is referenced from the given external method
	 * @param method
	 * @param from
	 */
	public void foundEntryTo(BT_Method method, BT_Method from) {
		if(markEntryPoints) {
			internalClassesInterface.addToInterface(method);
			internalClassesInterface.addToInterface(method.getDeclaringClass());
			internalClassesInterface.addTargetedClassToInterface(method.getDeclaringClass());
		}
		if(out != null) {
			out.print(from.useName());
			out.print(arrow);
			out.println(method.useName());
		}
	}
	
	public void foundEntryTo(BT_Method method) {
		if(out != null) {
			out.println(method.useName());
		}
	}
	
}
