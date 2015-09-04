package com.ibm.ive.tools.japt.reduction.xta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.ibm.ive.tools.japt.JaptClass;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.reduction.ClassProperties;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.ive.tools.japt.reduction.GenericReducer;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_StackPool;

/**
 * Creates the Field, Method and Class objects used in the reduction algorithm
 * 
 * @author sfoley
 *
 */
public class Repository {
	private HashMap methods = new HashMap();
	private HashMap fields = new HashMap();
	private HashMap classes = new HashMap();
	private ArrayList members = new ArrayList();
	private LinkedList newMembers = new LinkedList();
	public EntryPointLister entryPointLister;
	final public JaptRepository repository;
	final public GenericReducer reducer;
	final public RelatedMethodMap relatedMethodMap;
	final ClassProperties properties;
	final BT_StackPool pool = new BT_StackPool();
	
	protected Repository(GenericReducer reducer) {
		this.reducer = reducer;
		this.repository = reducer.repository;
		this.relatedMethodMap = repository.getRelatedMethodMap();
		properties = new ClassProperties(repository);
	}
	
	protected ArrayElement constructArrayElement(Clazz declaringClass, BT_Class elementClass) {
		return new ArrayElement(declaringClass, elementClass);
	}
	
	protected Clazz constructClass(BT_Class clz) {
		return new Clazz(this, clz);
	}
	
	protected Method constructMethod(BT_Method method) {
		return new Method(this, method);
	}
	
	protected Field constructField(BT_Field field) {
		return new Field(this, field);
	}
	
	private ArrayElement getArrayElement(JaptClass declaringArrayClass) {
		Clazz declaringClass = getClazz(declaringArrayClass);
		ArrayElement result = declaringClass.arrayElement;
		if(result == null) {
			BT_Class elementClass = declaringArrayClass.getElementClass();
			result = constructArrayElement(declaringClass, elementClass);
			newMembers.add(result);
			declaringClass.arrayElement = result;
			if(elementClass.isArray()) {
				result.subType = getArrayElement(elementClass);
			}	
		}
		return result;
	}
	
	ArrayElement getArrayElement(BT_Class declaringArrayClass) {
		return getArrayElement((JaptClass) declaringArrayClass);
	}
	
	public Method getMethod(BT_Method method) {						
		Method result = (Method) methods.get(method);
		if(result == null) {
			result = constructMethod(method);
			newMembers.add(result);
			methods.put(method, result);
		}
		return result;
	}
	
	public Field getField(BT_Field field) {
		Field result = (Field) fields.get(field);
		if(result == null) {
			result = constructField(field);
			fields.put(field, result);
			newMembers.add(result);
		}
		return result;
	}
	
	public Clazz getClazz(BT_Class clazz) {
		Clazz result = (Clazz) classes.get(clazz);
		if(result == null) {
			result = constructClass(clazz);
			classes.put(clazz, result);
		}
		return result;
	}
	
	boolean contains(BT_Class clazz) {
		return classes.containsKey(clazz);
	}
	
	boolean contains(BT_Method method) {
		return methods.containsKey(method);
	}
	
	boolean contains(BT_Field field) {
		return fields.containsKey(field);
	}
	
	int getMemberCount() {
		return members.size();
	}
	
	Iterator getMembers() {
		members.addAll(newMembers);
		members.trimToSize();
		newMembers.clear();
		return members.iterator();
	}
	
	static void addSubtypes(BT_ClassVector types, BT_Class type) {
		if(!types.contains(type)) {
			types.addElement(type);
			BT_ClassVector kids = type.getKids();
			for(int i=0; i<kids.size(); i++) {
				addSubtypes(types, kids.elementAt(i));	
			}	
		}
	}
	
	
	/**
	 * Determines if an object of type objectType can be passed to an variable of type declaredType.
	 * objectType cannot be an array type or a primitive type.
	 *  
	 * If the declared class is an interface, the objectType implements that interface.
	 * If the declared type is a class, the object type is that class or a subclass.
	 * 
	 * @param declaredType the declared type, non-array and non-primitive
	 * @param objectType the object type, non-array and non-primitive
	 */
	/*boolean isCompatibleType(BT_Class declaredType, BT_Class objectType) {
		try {
			return Class.forName(objectType.fullName()).isAssignableFrom(Class.forName(declaredType.fullName()));
		}
		catch(ClassNotFoundException e) {
			System.out.println(e);
			return isCompatibleTypeSlow(declaredType, objectType);
		}
	}*/
	boolean isCompatibleType(BT_Class declaredType, BT_Class objectType) {
		return ((JaptClass) declaredType).isInstance(objectType);
	}
	
	public BT_Class getElementClass(BT_Class objectType) {
		if(objectType.isArray()) {
			String declaringName = objectType.fullName();
			String elementClassName = declaringName.substring(0, declaringName.length() - 2);
			return objectType.getRepository().forName(elementClassName);
		}
		return null;
	}
	
	/**
	 * determines if objectType is an instance of a class in the vector types
	 * @param types a vector of non-array, non primitive classes
	 * @param objectType a type that is not an array or primitive type
	 * @return true if types contains objectType, a parent class of objectType, a parent interface of objectType,
	 * an interface which is implemented by objectType or objectType is null and typesPropagatable is non-zero size
	 */
	boolean isCompatibleType(BT_ClassVector types, BT_Class objectType) {
		int size = types.size();
		if(size == 0) {
			return false;
		}
		if(objectType == null) {
			return true;
		}
		for(int i=0; i<size; i++) {
			BT_Class clazz = types.elementAt(i);
			if(isCompatibleType(clazz, objectType)) {
				return true;
			}
		}
		return false;
		
	}
	
	boolean isCompatibleType(BT_Class types[], BT_Class objectType) {
		int size = types.length;
		if(size == 0) {
			return false;
		}
		if(objectType == null) {
			return true;
		}
		for(int i=0; i<size; i++) {
			BT_Class clazz = types[i];
			if(isCompatibleType(clazz, objectType)) {
				return true;
			}
		}
		return false;
		
	}

	
}
