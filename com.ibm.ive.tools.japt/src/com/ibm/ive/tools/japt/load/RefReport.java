/*
 * Created on Oct 7, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.load;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSite;
import com.ibm.jikesbt.BT_ClassReferenceSiteVector;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_ExceptionTableEntryVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_InnerClassesAttribute;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;
import com.ibm.jikesbt.BT_InnerClassesAttribute.Description;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RefReport {

	private PrintStream refTreeStream;
	private JaptFactory factory;
	private JaptFactory.ClassLocatedListener listener;
	private BT_ClassVector refTreeClasses;
	
	/**
	 * 
	 */
	public RefReport(String fileName, JaptFactory factory) throws FileNotFoundException {
		refTreeStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
		listener = new JaptFactory.ClassLocatedListener() {
			public void classLocated(BT_Class c, BT_ClassPathLocation location) {
				refTreeStream.print("\tClass: ");
				refTreeStream.println(c.getName());
				refTreeClasses.addUnique(c);
			}
		};
		factory.addClassLocatedListener(listener);
		this.factory = factory;
		refTreeClasses = new BT_HashedClassVector();
	}
	
	public RefReport() {}
	
	public void completeRefTree() {
		if(refTreeStream == null) {
			return;
		}
		
		/*
		 * note that the vector refTreeClasses will increase in size as more classes are added
		 */
		for(int classIndex = 0; classIndex < refTreeClasses.size(); classIndex++) {
			BT_Class currentClass = refTreeClasses.elementAt(classIndex);
			print(currentClass);		
		}
		refTreeStream.close();
		factory.removeClassLocatedListener(listener);
	}


	private void print(BT_Class clazz) {
		refTreeStream.print("Class: ");
		refTreeStream.println(clazz.getName());
		
		BT_ClassVector parents = clazz.getParents();
		for(int i=0; i<parents.size(); i++) {
			refTreeStream.print("\tClass: ");
			BT_Class parent = parents.elementAt(i);
			refTreeStream.println(parent.getName());
			refTreeClasses.addUnique(parent);
		}
		
		//we need to add inner classes because they will have constant pool
		//class entries and thus will have been loaded by JIKESBT
		BT_InnerClassesAttribute attr = clazz.getInnerClassAttr();
		if(attr != null) {
			Description[] inners = attr.inners;
			for(int i=0; i<inners.length; i++) {
				Description desc = inners[i];
				BT_Class ref = desc.innerClass;
				if(ref != null) {
					refTreeStream.print("\tClass: ");
					refTreeStream.println(ref.getName());
					refTreeClasses.addUnique(ref);
				}
				ref = desc.outerClass;
				if(ref != null) {
					refTreeStream.print("\tClass: ");
					refTreeStream.println(ref.getName());
					refTreeClasses.addUnique(ref);
				}
			}
		}
		
		BT_FieldVector fields = clazz.fields;
		for(int i=0; i<fields.size(); i++) {
			refTreeStream.print("\tField: ");
			refTreeStream.println(fields.elementAt(i).useName());
		}
		BT_MethodVector methods = clazz.methods;
		for(int i=0; i<methods.size(); i++) {
			refTreeStream.print("\tMethod: ");
			refTreeStream.println(methods.elementAt(i).useName());
		}
		
		fields = clazz.fields;
		for(int i=0; i<fields.size(); i++) {
			refTreeStream.print("Field: ");
			BT_Field field = fields.elementAt(i);
			refTreeStream.println(field.useName());
			BT_Class type = field.getFieldType();
			if(!type.isBasicTypeClass) {
				refTreeStream.print("\tClass: ");
				refTreeStream.println(type.getName());
				refTreeClasses.addUnique(type);
			}
		}
		methods = clazz.methods;
		for(int i=0; i<methods.size(); i++) {
			refTreeStream.print("Method: ");
			BT_Method method = methods.elementAt(i);
			refTreeStream.println(method.useName());
			BT_MethodSignature sig = method.getSignature();
			BT_ClassVector types = sig.types;
			for(int j=0; j<types.size(); j++) {
				BT_Class type = types.elementAt(j);
				if(!type.isBasicTypeClass) {
					refTreeStream.print("\tClass: ");
					refTreeStream.println(type.getName());
					refTreeClasses.addUnique(type);
				}
			}
			BT_Class type = sig.returnType;
			if(!type.isBasicTypeClass) {
				refTreeStream.print("\tClass: ");
				refTreeStream.println(type.getName());
				refTreeClasses.addUnique(type);
			}
			BT_Class exceptions[] = method.getDeclaredExceptions();
			for(int j=0; j<exceptions.length; j++) {
				BT_Class target = exceptions[j];
				refTreeStream.print("\tClass: ");
				refTreeStream.println(target.getName());
				refTreeClasses.addUnique(target);
			}
			
			BT_CodeAttribute code = method.getCode();
			if(code == null) {
				continue;
			}
			//note that instead of printing a reference to the methods
			//and fields I could print a reference to their owning classes
			//although this would be forfeiting information
			BT_MethodCallSiteVector calledMethods = code.calledMethods;
			for(int j=0; j<calledMethods.size(); j++) {
				BT_MethodCallSite site = calledMethods.elementAt(j);
				BT_Method target = site.getTarget();
				refTreeStream.print("\tMethod: ");
				refTreeStream.println(target.useName());
				refTreeClasses.addUnique(target.getDeclaringClass());
			}
			BT_AccessorVector accessedFields = code.accessedFields;
			for(int j=0; j<accessedFields.size(); j++) {
				BT_Accessor site = accessedFields.elementAt(j);
				BT_Field target = site.getTarget();
				refTreeStream.print("\tField: ");
				refTreeStream.println(target.useName());
				refTreeClasses.addUnique(target.getDeclaringClass());
			}
			BT_ClassReferenceSiteVector referencedClasses = code.referencedClasses;
			for(int j=0; j<referencedClasses.size(); j++) {
				BT_ClassReferenceSite site = referencedClasses.elementAt(j);
				BT_Class target = site.getTarget();
				refTreeStream.print("\tClass: ");
				refTreeStream.println(target.getName());
				refTreeClasses.addUnique(target);
			}
			BT_ExceptionTableEntryVector exceptionTable = code.getExceptionTableEntries();
			for (int t=0; t < exceptionTable.size(); t++) {
				BT_ExceptionTableEntry e = exceptionTable.elementAt(t);
				BT_Class target = e.catchType;
				if (target == null)
					continue;
				refTreeStream.print("\tClass: ");
				refTreeStream.println(target.getName());
				refTreeClasses.addUnique(target);
			}
		}
	}
	
	public void printRule(Option option, String value) {
		if(refTreeStream != null) {
			refTreeStream.print("Rule: ");
			refTreeStream.print(option.getAppearance());
			refTreeStream.print(' ');
			refTreeStream.println(value);
		}
	}
	
	public void printEntry(String first, String second) {
		if(refTreeStream != null) {
			refTreeStream.print(first);
			refTreeStream.println(second);
		}
	}
}
