package com.ibm.ive.tools.japt.obfuscation;


import java.util.*;
import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;


/**
 * @author sfoley
 * <p>
 * Keeps track of names that are fixed or changed
 * 
 */
class NameHandler {
	private HashSet fixedNameItems = new HashSet(); //anything in this set cannot have its name changed
	private HashSet fixedPackageNameClasses = new HashSet(); //a class in this set must remain in the same package
	private HashMap changedMethodNames = new HashMap();
	private HashMap changedFieldNames = new HashMap();
	private HashMap changedClassNames = new HashMap();
	private Logger logger;
	private Messages messages;
	
	
  	NameHandler(Logger logger, Messages messages, boolean prepend) {
  		this.logger = logger;
  		this.messages = messages;
  	}
  	
  	
  	
  	/**
  	 * 
  	 * @return an iterator of java.util.Map.Entry items
  	 * In each Map.Entry the key is a BT_Method while the value is a string giving the old name
  	 */
  	Iterator getChangedMethodNames() {
  		return changedMethodNames.entrySet().iterator();
  	}
  	
	/**
	 * 
	 * @return an iterator of java.util.Map.Entry items
	 * In each Map.Entry the key is a BT_Class while the value is a string giving the old name
	 */
	Iterator getChangedClassNames() {
		return changedClassNames.entrySet().iterator();
	}
		
	/**
	 * 
	 * @return an iterator of java.util.Map.Entry items
	 * In each Map.Entry the key is a BT_Field while the value is a string giving the old name
	 */
	Iterator getChangedFieldNames() {
		return changedFieldNames.entrySet().iterator();
	}
  	
  	void fixName(BT_Class clazz, String reason) {
		if(clazz.isArray()) {
			fixName(clazz.arrayType, reason);
			return;
		}
		messages.FIXED_CLASS.log(logger, clazz.getName());
		fixedNameItems.add(clazz);
	}
	
	void fixName(BT_Method method, String reason) {
		messages.FIXED_METHOD.log(logger, new String[] {method.useName()});			
		fixedNameItems.add(method);
	}
	
	void fixName(BT_Field field, String reason) {
		messages.FIXED_FIELD.log(logger, new String[] {field.useName()});
		fixedNameItems.add(field);
	}
	
	void freezeName(BT_Item item) {
		fixedNameItems.add(item);
	}
	
	void freezeName(BT_Class item) {
		if(item.isArray()) {
			freezeName(item.arrayType);
			return;
		}
		fixedNameItems.add(item);
	}
	
	
	/**
	 * keep clazz in the same package
	 */
	void fixPackageName(BT_Class clazz) {
		if(clazz.isArray()) {
			fixPackageName(clazz.arrayType);
		}
		
		messages.FIXED_PACKAGE.log(logger, clazz.getName());			
		fixedPackageNameClasses.add(clazz);
	}
	
	void freezePackageName(BT_Class clazz) {
		if(clazz.isArray()) {
			freezePackageName(clazz.arrayType);
		}
		fixedPackageNameClasses.add(clazz);
	}
	
	
	/** a class might have its package name fixed while its full name is not, due to requirements of
	 * package access to and from classes with fixed names.  A class may have its name fixed it
	 * is accessed from outside the project.
	 */
	boolean packageNameIsFixed(BT_Class clazz) {
		return fixedPackageNameClasses.contains(clazz);
	}
	
	/** the item's name cannot be changed.  In the case of a class, this refers to the full
	 * name including the package.
	 */
	boolean nameIsFixed(BT_Item item) {
		return fixedNameItems.contains(item);
	}

	void rename(BT_Method item, String newName) {
		messages.METHOD_NAME_MAPPED.log(logger, new String[] {getPreviousClassName(item.getDeclaringClass()), item.getName(), newName});
		if(!changedMethodNames.containsKey(item)) {
			changedMethodNames.put(item, item.getName());
		}
		item.resetName(newName);
		//remove the signature attribute
		item.attributes.removeAttribute(BT_SignatureAttribute.ATTRIBUTE_NAME);
	}
	
	void rename(BT_Field item, String newName) {
		messages.FIELD_NAME_MAPPED.log(logger, new String[] {getPreviousClassName(item.getDeclaringClass()), item.getName(), newName});
		if(!changedFieldNames.containsKey(item)) {
			changedFieldNames.put(item, item.getName());
		}
		item.resetName(newName);
		//remove the signature attribute
		item.attributes.removeAttribute(BT_SignatureAttribute.ATTRIBUTE_NAME);
	}
	
	void rename(BT_Class clazz, String newName, boolean report) {
		if(report) {
			messages.CLASS_NAME_MAPPED.log(logger, new String[] {LogMessage.capitalizeFirst(clazz.kindName()), getPreviousClassName(clazz), newName});
		}
		if(!changedClassNames.containsKey(clazz)) {
			changedClassNames.put(clazz, clazz.getName());
		}
		clazz.setName(newName);
		//remove the signature attribute
		clazz.attributes.removeAttribute(BT_SignatureAttribute.ATTRIBUTE_NAME);
	}
	
	void renamePackage(BT_Class clazz, String newName) {
		if(!changedClassNames.containsKey(clazz)) {
			changedClassNames.put(clazz, clazz.getName());
		}
		clazz.setName(newName);
		//remove the signature attribute
		clazz.attributes.removeAttribute(BT_SignatureAttribute.ATTRIBUTE_NAME);
	}
	
	boolean isRenamed(BT_Class item) {
		return changedClassNames.containsKey(item);	
	}
	
	/** 
	 * returns the item's previous names or the current name if the name was never changed
	 */
	public String getPreviousClassName(BT_Class item) {
		return getPreviousName(changedClassNames, item);
	}
	
	/** 
	 * returns the item's previous names or the current name if the name was never changed
	 */
	public String getPreviousFieldName(BT_Field item) {
		return getPreviousName(changedFieldNames, item);
	}
		
	/** 
	 * returns the item's previous names or the current name if the name was never changed
	 */
	public String getPreviousMethodName(BT_Method item) {
		return getPreviousName(changedMethodNames, item);
	}
	
	private String getPreviousName(HashMap map, BT_Item item) {
		if(map.containsKey(item)) {
			return (String) map.get(item);
		}
		return item.getName();
	}	
	
//	String getPackageName(BT_Class clazz) {
//		return clazz.packageName();
//	}
}
