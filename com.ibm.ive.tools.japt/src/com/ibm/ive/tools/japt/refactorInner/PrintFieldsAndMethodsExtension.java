/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Japt Refactor Extension
 *
 * Copyright IBM Corp. 2008
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U. S. Copyright Office.
 */
package com.ibm.ive.tools.japt.refactorInner;

import java.io.PrintStream;
import java.util.Enumeration;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.jikesbt.BT_AttributeVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSite;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;

/**
 * Extension for Japt, which analyzes classes and
 * output internal information such as attributes
 * and method signatures.
 */
public class PrintFieldsAndMethodsExtension implements CommandLineExtension {
	private static PrintStream out = System.out; // where to print output
	
	/**
	 * Internal method for embedding copyright
	 */
	static String copyright() {
		return Copyright.IBM_COPYRIGHT;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return "PrintFieldsAndMethods"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.commandLine.CommandLineExtension#getOptions()
	 */
	public Option[] getOptions() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) {
		report(repository);
	}
	
	/**
	 * Sets PrintStream to print output to.
	 * @param output PrintStream object to specify where to print output
	 */
	public static void setPrintStream(PrintStream output) {
		out = output;
	}
	
	/**
	 * Analyzes and prints information of internal classes in given repository.
	 * @param repository JaptRepository containing internal classes to analyze.
	 */
	public static void report(JaptRepository repository) {
		report(repository.getInternalClasses());
	}
	
	/**
	 * Analyzes and prints information of classes.
	 * @param cv BT_ClassVector object containing classes to analyze.
	 */
	public static void report(BT_ClassVector cv) {
		Enumeration enumClasses = cv.elements();
		while (enumClasses.hasMoreElements()) {
			BT_Class clz = (BT_Class)enumClasses.nextElement();
			if (!clz.isStub() && !clz.isArray() && !clz.isPrimitive()) {
				report(clz);
			}
		}
	}
	
	/**
	 * Analyzes and prints information of given class.
	 * @param clz BT_Class object to analyze.
	 */
	public static void report(BT_Class clz) {
		out.println(clz.fullName());

		BT_ClassVector parents = clz.getParents();
		Enumeration enumParents = parents.elements();
		while (enumParents.hasMoreElements()) {
			BT_Class parent = (BT_Class)enumParents.nextElement();
			out.println("  " + (parent.isInterface() ? "implements " : "extends ") + parent.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		Enumeration enumFields = clz.getFields().elements();
		while (enumFields.hasMoreElements()) {
			BT_Field f = (BT_Field)enumFields.nextElement();
			out.println("  " + f.getName() + ": " + f.getTypeName() + " " + f.getName() + ": SYNTHETIC " + f.isSynthetic()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		Enumeration enumMethods = clz.getMethods().elements();
		while (enumMethods.hasMoreElements()) {
			BT_Method m = (BT_Method)enumMethods.nextElement();
			out.println("  " + m.getName() + m.getSignature()); //$NON-NLS-1$

			BT_ClassVector cv = m.getDeclaredExceptionsVector();
			if (cv != null && cv.size() >= 0) {
				out.println("     throws:"+cv); //$NON-NLS-1$
			}
			BT_AttributeVector ba = m.getAttributes();
			if (ba != null && ba.size() >= 0) {
				out.println("     attributes:"+ba); //$NON-NLS-1$
			}
		}

		Enumeration enumCreateSites = clz.creationSites.elements();
		while (enumCreateSites.hasMoreElements()) {
			BT_ClassReferenceSite r = (BT_ClassReferenceSite)enumCreateSites.nextElement();
			out.println("  " + r); //$NON-NLS-1$
		}

		Enumeration enumRefSites = clz.referenceSites.elements();
		while (enumRefSites.hasMoreElements()) {
			BT_ClassReferenceSite r = (BT_ClassReferenceSite)enumRefSites.nextElement();
			BT_Ins ins = r.getInstruction();
			out.println("  " + ins); //$NON-NLS-1$
		}

		out.println();
	}
}
