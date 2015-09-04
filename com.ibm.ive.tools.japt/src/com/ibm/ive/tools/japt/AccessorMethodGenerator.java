/*
 * Created on May 7, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.ibm.ive.tools.japt.out.JarGenerator;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Exception;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

public class AccessorMethodGenerator {
	private BT_Class clazz;
	AccessorMethodVector accessorMethods;	
	
	/**
	 * To get an accessor method generator, use the methods in JaptRepository, since they are cached.
	 * They need not be cached: there is no reason why one could not be constructed at any time.  They are
	 * cached for performance since each time one is created it scans all methods in the given class looking
	 * for existing accessors.
	 * @param clazz
	 */
	AccessorMethodGenerator(BT_Class clazz) {
		this.clazz = clazz;
		BT_MethodVector methods = clazz.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			//TODO this is not working properly: to test, go to AccessorMethod and change makeUniqueName
			//and that will cause verify errors when we end up having two accessors with the same name because we could
			//not detect the existing one
			AccessorMethod accessorMethod = AccessorMethod.getAccessor(method);
			if(accessorMethod != null) {
				if(accessorMethods == null) {
					accessorMethods = new AccessorMethodVector();
				}
				accessorMethods.addElement(accessorMethod);
			}
		}
	}
	
	
	public static AccessorMethod isMethodAccessor(BT_Method method) {
		if(!method.isSynthetic()) {
			return null;
		}
		BT_Class declaringClass = method.getDeclaringClass();
		JaptRepository repo = (JaptRepository) declaringClass.getRepository();
		AccessorMethodGenerator generator = repo.getAccessorMethodGenerator(declaringClass);
		if(generator == null) {
			return null;
		}
		return generator.hasMethodAccessor(method);
	}
	
	public AccessorMethod hasMethodAccessor(BT_Method method) {
		if(accessorMethods == null) {
			return null;
		}
		for(int i=0; i<accessorMethods.size(); i++) {
			AccessorMethod accessorMethod = accessorMethods.elementAt(i);
			if(accessorMethod.method.equals(method)) {
				return accessorMethod;
			}
		}
		return null;
	}
	
	/**
	 * Get an accessor declared in this class to access the method in this class or a parent class.
	 * The method invocation instruction will be resolved through this class.  The instruction used will  
	 * be either invokestatic, invokevirtual or invokeinterface.
	 * @param toMethod
	 * @return
	 */
	public AccessorMethod getMethodAccessor(BT_Method toMethod, BT_Class throughClass) {
		return getMethodAccessor(toMethod, throughClass, false);
	}
	
	/**
	 * Get an accessor declared in this class to access the method in a parent class.
	 * The accessor will use an invokespecial invocation to access the method. 
	 * @param toMethod
	 * @return
	 */
	public AccessorMethod getSpecialMethodAccessor(BT_Method toMethod, BT_Class throughClass) {
		/* the target should either be a method in the current class or a method inherited from the parent class */
		return getMethodAccessor(toMethod, throughClass, true);
	}
	
	private AccessorMethod getMethodAccessor(BT_Method toMethod, BT_Class throughClass, boolean isSpecial) {
		if(accessorMethods == null) {
			accessorMethods = new AccessorMethodVector();
		} else {
			for(int i=0; i<accessorMethods.size(); i++) {
				AccessorMethod method = accessorMethods.elementAt(i);
				if(isSpecial ? 
						method.invokesSpecial(toMethod, throughClass) : 
						method.invokes(toMethod, throughClass)) {
					return method;
				}
			}
		}
		AccessorMethod accessor = new AccessorMethod(clazz, toMethod, throughClass, isSpecial);
		accessorMethods.addElement(accessor);
		return accessor;
	}
	
	private AccessorMethod getFieldAccessor(BT_Field toField, BT_Class throughClass, boolean isGetter) {
		if(accessorMethods == null) {
			accessorMethods = new AccessorMethodVector();
		} else {
			for(int i=0; i<accessorMethods.size(); i++) {
				AccessorMethod method = accessorMethods.elementAt(i);
				if(isGetter ? 
						method.gets(toField, throughClass) : 
						method.sets(toField, throughClass)) {
					return method;
				}
			}
		}
		AccessorMethod accessor = new AccessorMethod(clazz, toField, throughClass, isGetter);
		accessorMethods.addElement(accessor);
		return accessor;
	}
	
	/**
	 * Get an accessor declared in this class to get the field value in this class or a parent class.
	 * The field access instruction will be resolved through this class.
	 * @param toField
	 * @return
	 */
	public AccessorMethod getFieldGetter(BT_Field toField, BT_Class throughClass) {
		return getFieldAccessor(toField, throughClass, true);
	}
	
	/**
	 * Get an accessor declared in this class to set the field value in this class or a parent class.
	 * The field access instruction will be resolved through this class.
	 * @param toField
	 * @return
	 */
	public AccessorMethod getFieldSetter(BT_Field toField, BT_Class throughClass) {
		return getFieldAccessor(toField, throughClass, false);
	}
	
	public boolean removeAccessor(AccessorMethod method) {
		return accessorMethods.removeElement(method);
	}
	
	public void removeUnusedAccessors() {
		for(int i=accessorMethods.size() - 1; i>=0; i--) {
			AccessorMethod method = accessorMethods.elementAt(i);
			if(method.isUnused()) {
				method.remove();
				accessorMethods.removeElementAt(i);
			}
		}
	}
	
	public static void main(String args[]) throws InvalidIdentifierException, IOException, BT_Exception {
		String accessingClassName = "XXXAccessingXXX";
		String testJarName = "XXXAccessorTestJarXXX";
		int accessorCount = createJar(accessingClassName, testJarName);
		loadJar(accessingClassName, testJarName, accessorCount);
	}
	
	/**
	 * @param accessingClassName
	 * @param testJarName
	 * @throws Error
	 */
	private static void loadJar(String accessingClassName, String testJarName, int accessorCount) throws Error {
		JaptRepository repository2 = new JaptRepository(new JaptFactory());
		RepositoryLoader loader = new RepositoryLoader(repository2);
		loader.loadAll(testJarName);
		BT_Class accessingClass = repository2.forName(accessingClassName);
		AccessorMethodGenerator gen2 = repository2.createAccessorMethodGenerator(accessingClass);
		if(gen2 == null) {
			throw new Error("could not create accessor method generator for " + accessingClass);
		}
		AccessorMethodVector methods = gen2.accessorMethods;
		if(methods == null || methods.size() != accessorCount) {
			throw new Error("accessor methods were not detected");
		}
		for(int i=0; i<methods.size(); i++) {
			System.out.println("loaded " + methods.elementAt(i));
		}
	}


	/**
	 * @param accessingClassName
	 * @param testJarName
	 * @throws InvalidIdentifierException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws BT_Exception
	 */
	private static int createJar(String accessingClassName, String testJarName) throws InvalidIdentifierException, IOException, FileNotFoundException, BT_Exception {
		int accessorCount = 0;
		JaptRepository repository = new JaptRepository(new JaptFactory());
		SyntheticClassPathEntry cpe = new SyntheticClassPathEntry("accessor tester");
		repository.appendInternalClassPathEntry(cpe);
		BT_Class accessedClass = repository.createInternalClass(new Identifier("XXXAccessedXXX"), cpe);
		BT_Field field = BT_Field.createField(accessedClass, BT_Item.PRIVATE, repository.getByte(), "afield");
		BT_Method method = BT_Method.createMethod(accessedClass, "void", "amethod", "()");
		method.makeCodeSimplyReturn();
		BT_Class accessingClass = repository.createInternalClass(new Identifier(accessingClassName), cpe);
		accessingClass.setSuperClass(accessedClass);/* to have a special accessor to a non-private method we need to have the accessed method in a parent class */
		AccessorMethodGenerator gen = repository.createAccessorMethodGenerator(accessingClass);
		AccessorMethod fieldGetter = gen.getFieldGetter(field, accessingClass);
		System.out.println("created " + fieldGetter);
		accessorCount++;
		AccessorMethod fieldSetter = gen.getFieldSetter(field, accessingClass);
		System.out.println("created " + fieldSetter);
		accessorCount++;
		AccessorMethod specialMethodAccessor = gen.getSpecialMethodAccessor(method, accessingClass);
		System.out.println("created " + specialMethodAccessor);
		accessorCount++;
		AccessorMethod methodAccessor = gen.getMethodAccessor(method, accessingClass);
		System.out.println("created " + methodAccessor);
		accessorCount++;
		JarGenerator jarGenerator = new JarGenerator(new FileOutputStream(testJarName));
		jarGenerator.write(accessedClass);
		jarGenerator.write(accessingClass);
		jarGenerator.close();
		return accessorCount;
	}
	
	
}
