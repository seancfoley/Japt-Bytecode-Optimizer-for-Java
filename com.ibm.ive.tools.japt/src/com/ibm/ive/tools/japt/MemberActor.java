/*
 * Created on Jun 6, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.util.TreeSet;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class MemberActor {

	protected void actOnMethod(BT_Class referencedClass, BT_Method method) {}
	
	protected void actOnField(BT_Class referencedClass, BT_Field field) {}
	
	protected void actOnInterfaceMethod(BT_Class referencedInterface, BT_Method method) {}
	
	
	/**
	 * 
	 * Acts on a class and its specified containing fields and methods in the repository.
	 * Will search parent classes and parent interfaces in order to act on all methods owned by the class.
	 * @param loadedClass the class to be analyzed
	 * @param methodSpec specifies methods to be acted on, can be null to specify no methods
	 * @param fieldSpec specifies fields to be acted on, can be null to specify no fields
	 */
	public void actOn(BT_Class loadedClass, MemberSelector spec) {
		if(spec == null) {
			return;
		}
		actOnMembers(loadedClass, spec);
		if(loadedClass.isInterface() && spec.selectsMethods() && spec.isResolvable()) {		
			actOnClassMethods(loadedClass, loadedClass.getRepository().findJavaLangObject(), spec);
		}
		
	}
	
	/**
	 * 
	 * Acts on a set of classes and their specified containing fields and methods.
	 * Will search parent classes and parent interfaces in order to act on all methods owned by each class.
	 * @param loadedClasses the class(es) to be analyzed
	 * @param methodSpec aspecifies methods to be acted on, can be null to specify no methods
	 * @param fieldSpec specifies fields to be acted on, can be null to specify no fields
	 */
	public void actOn(BT_ClassVector classes, MemberSelector spec) {
		TreeSet examinedClasses = new TreeSet();
		//we construct a set to ensure we do not act on the same class twice, since
		//there may be duplicates in the vector
		for(int j=0; j<classes.size(); j++) {
			BT_Class clazz = classes.elementAt(j);
			if(!examinedClasses.contains(clazz)) {
				examinedClasses.add(clazz);
				actOn(clazz, spec);
			}
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * Same as a MemberActor but also calls the method ActOnClass once
	 * for every class.
	 */
	public abstract static class ClassActor extends MemberActor {
		
		abstract public void actOnClass(BT_Class referencedClass);
		
		public void actOn(BT_Class loadedClass, MemberSelector spec) {
			actOnClass(loadedClass);
			super.actOn(loadedClass, spec);
		}
		
		public void actOn(BT_ClassVector classes) {
			TreeSet examinedClasses = new TreeSet();
			//we construct a set to ensure we do not act on the same class twice
			for(int j=0; j<classes.size(); j++) {
				BT_Class clazz = classes.elementAt(j);
				if(!examinedClasses.contains(clazz)) {
					examinedClasses.add(clazz);
					actOnClass(clazz);
				}
			}
		}
		
	}
	
	static public class MemberCollectorActor extends ClassActor {
		public BT_MethodVector methods;
		public BT_FieldVector fields;
		
		public void actOnClass(BT_Class referencedClass) {}
		
		public MemberCollectorActor(BT_MethodVector methods, BT_FieldVector fields) {
			this.methods = methods;
			this.fields = fields;
		}
		
		public MemberCollectorActor() {
			this(new BT_MethodVector(), new BT_FieldVector());
		}
		
		public void actOnMethod(BT_Class referencedClass, BT_Method method) {
			methods.addUnique(method);
		}

		public void actOnField(BT_Class referencedClass, BT_Field field) {
			fields.addUnique(field);
		}

		public void actOnInterfaceMethod(BT_Class referencedInterface, BT_Method method) {
			methods.addUnique(method);
		}
	}
	
	/**
	 * adds all methods specified by methodSpec in clazz that can be resolved through a 
	 * reference to loadedInterface
	 */
	private void actOnInterfaceMethods(BT_Class referencedInterface, BT_Class clazzOrParent, MemberSelector methodSpec) {
		BT_MethodVector methods = clazzOrParent.getMethods();
		for(int k=0; k<methods.size(); k++) {
			BT_Method method = methods.elementAt(k);
			if(methodSpec.isSelected(referencedInterface, method)) {
				actOnInterfaceMethod(referencedInterface, method);
			}
		}
		
	}
	
	/**
	 * adds all methods specified by methodSpec 
	 * @param loadedClass the method was specified relative to loadedClass ie loadedClass.m for some method m
	 * @param classOrParent either loadedClass or a parent of loadedClass
	 * @param MemberSelector specifies which members of classOrParent are to be included
	 */
	private void actOnClassMethods(BT_Class referencedClass, BT_Class classOrParent, MemberSelector methodSpec) {
		BT_MethodVector methods = classOrParent.getMethods();
		for(int k=0; k<methods.size(); k++) {
			BT_Method method = methods.elementAt(k);
			if(methodSpec.isSelected(referencedClass, method)) {
				actOnMethod(referencedClass, method);
			}
		}
	}
	
	/**
	 * adds all fields specified by fieldComparable in clazz that can be resolved through a 
	 * reference to loadedClass
	 */
	private void actOnClassFields(BT_Class referencedClass, BT_Class clazz, MemberSelector fieldSpec) {
		BT_FieldVector fields = clazz.getFields();
		for(int k=0; k<fields.size(); k++) {
			BT_Field field = fields.elementAt(k);
			if(fieldSpec.isSelected(referencedClass, field)) {
				actOnField(referencedClass, field);
			}
		}
	}
	
	private void actOnMembers(BT_Class referencedClass, MemberSelector spec) {
		if(spec.selectsMethods()) {
			actOnMethods(referencedClass, referencedClass, spec);
		}
		if(spec.selectsFields()) {
			actOnFields(referencedClass, referencedClass, spec);
		}
	}
	
	private void actOnMethods(BT_Class referencedClass, BT_Class referencedClassOrParent, MemberSelector spec) {
		if(spec.isSelected(referencedClassOrParent)) {
			if(referencedClassOrParent.isInterface()) {
				actOnInterfaceMethods(referencedClass, referencedClassOrParent, spec);
			}
			else {
				actOnClassMethods(referencedClass, referencedClassOrParent, spec);
			}
		}
		if(spec.isResolvable()) {
			//we search in the same order that the methods/fields are resolved,
			//so that any selector can rely on this order
			BT_ClassVector parents = referencedClassOrParent.getParents();
			
			//method resolution, VM spec 5.4.3.3
			//we check the class, all superclasses and all superinterfaces
			
			//interface method resolution, VM spec 5.4.3.4
			//we check the interface, all superinterfaces, and java.lang.Object
			//checking java.lang.Object is done above in the method "actOn(BT_Class, MemberSelector)"
			//since in JikesBT interfaces have a null superclass
			
			//superclasses first, then superinterfaces
			BT_Class superClass = referencedClassOrParent.getSuperClass();
			if(superClass != null) {
				actOnMethods(referencedClass, superClass, spec);
			}
			for(int i=0; i<parents.size(); i++) {
				BT_Class parent = parents.elementAt(i);
				if(parent.isInterface()) {
					actOnMethods(referencedClass, parent, spec);
				}
			}
		}
	}
	
	private void actOnFields(BT_Class referencedClass, BT_Class referencedClassOrParent, MemberSelector spec) {
		if(spec.isSelected(referencedClassOrParent)) {
			actOnClassFields(referencedClass, referencedClassOrParent, spec);
		}
			
		if(spec.isResolvable()) {
			//we search in the same order that the methods/fields are resolved,
			//so that any selector can rely on this order
			BT_ClassVector parents = referencedClassOrParent.getParents();
			
			//field resolution, VM spec 5.4.3.2
			//we check the class/interface, all superinterfaces and superclasses
			
			//direct superinterfaces first, then superclasses
			for(int i=0; i<parents.size(); i++) {
				BT_Class parent = parents.elementAt(i);
				if(parent.isInterface()) {
					actOnFields(referencedClass, parent, spec);
				}
			}
			BT_Class superClass = referencedClassOrParent.getSuperClass();
			if(superClass != null) {
				actOnFields(referencedClass, superClass, spec);
			}
		}
	}
}
