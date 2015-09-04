/*
 * Created on Aug 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.remapping;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptClass;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.SyntheticClassPathEntry;
import com.ibm.ive.tools.japt.PatternString.PatternStringPair;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Opcodes;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Creator {

	JaptRepository repository;
	Logger logger;
	Messages messages;
	String name;
	
	/**
	 * 
	 */
	public Creator(String name, JaptRepository repository, Logger logger, Messages messages) {
		this.repository = repository;
		this.logger = logger;
		this.messages = messages;
		this.name = name;
	}
	
	private ClassPathEntry classPathEntry;
	
	ClassPathEntry createClassPathEntry() {
		if(classPathEntry == null) {
			classPathEntry = new SyntheticClassPathEntry(name);
			repository.addExtendedClassPathEntry(classPathEntry);
		}
		return classPathEntry;
	}
	
	BT_Method createMethodTarget(PatternStringPair identifier, String from, BT_MethodSignature signature, boolean inInterface) throws InvalidIdentifierException {
		//Note that wildcards are not present and the identifiers are all valid names at this point
		BT_Class target = createClassTarget(identifier.first, from, inInterface);
		if(target == null) {
			return null;
		}
		String methodName = identifier.second.getString();
		BT_Method targetMethod = BT_Method.createMethod(target, (short) (BT_Item.PUBLIC | BT_Item.STATIC), signature, methodName);
		targetMethod.makeCodeSimplyReturn();
		return targetMethod;
	}
	
	BT_Field createFieldTarget(PatternStringPair identifier, String from, BT_Class type) throws InvalidIdentifierException {
		//Note that wildcards are not present and the identifiers are all valid names at this point
		BT_Class target = createClassTarget(identifier.first, from, false);
		if(target == null) {
			return null;
		}
		String fieldName = identifier.second.getString();
		BT_Field targetField = BT_Field.createField(target, (short) (BT_Item.PUBLIC | BT_Item.STATIC), type, fieldName);
		return targetField;
	}
	
	BT_Class createClassTarget(PatternString pattern, String from, boolean isInterface) throws InvalidIdentifierException {
		Identifier classIdentifier = new Identifier(pattern, from);
		return createClassTarget(classIdentifier, isInterface);
	}
	
	BT_Class createClassTarget(Identifier classIdentifier, boolean isInterface) throws InvalidIdentifierException {
		BT_Class target = null;
		BT_ClassVector classes = repository.findClasses(classIdentifier, false);
		if(classes.size() > 1) {
			messages.TARGET_NOT_UNIQUE.log(logger, classIdentifier);
			return null;
		}
		else if(classes.size() == 1) {
			target = classes.firstElement();
			if(!repository.isInternalClass(target)) {
				//cannot create a new field or method in an external class
				messages.TARGET_NOT_INTERNAL.log(logger, classIdentifier);
				return null;
			}
			if(isInterface) {
				if(target.isClass()) {
					messages.TARGET_WRONG_TYPE.log(logger, new Object[] {target, target.kindName()});
					return null;
				}
			} else {
				if(target.isInterface()) {
					messages.TARGET_WRONG_TYPE.log(logger, new Object[] {target, target.kindName()});
					return null;
				}
			}
		}
		else {//must create the target class
			ClassPathEntry cpe = createClassPathEntry();
			if(isInterface) {
				target = repository.createInternalInterface(classIdentifier, cpe);
			} else {
				target = repository.createInternalClass(classIdentifier, cpe);
			}
		}
		
		return target;
	}
	
	/**
	 * returns a method signature for a static method that is compatible
	 * for all given methods.  If any methods are non-static, then the type of
	 * the declaring class will be compatible with the first type in the returned signature,
	 * the first type in the method's signature will be compatible with the seconf type in the
	 * returned signature, and so on.  For static methods the mapping is one-to-one.
	 * @param methods
	 * @return a signature that is compatible for all given methods.  In other words, any call to a
	 * method in the list could be redirected to a call to a static method with the returned signature.
	 */
	BT_MethodSignature getCommonSignature(BT_MethodVector methods) {
		BT_Class returnType = null;
		BT_ClassVector argTypes = null;
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			BT_MethodSignature signature = method.getSignature();
			if(returnType == null) {
				returnType = signature.returnType;
				argTypes = signature.types;
				continue;
			}
			
			returnType = getCommonType(returnType, signature.returnType);
			if(returnType == null) {
				return null;
			}
			BT_ClassVector types = signature.types;
			
			if(!method.isStatic()) {
				types = (BT_ClassVector) types.clone();
				types.insertElementAt(method.getDeclaringClass(), 0);
			}
			if(types.size() != argTypes.size()) {
				return null;
			}
			for(int j=0; j<types.size(); j++) {
				BT_Class argType = argTypes.elementAt(j);
				BT_Class type = types.elementAt(j);
				BT_Class commonType = getCommonType(argType, type);
				if(commonType == null) {
					return null;
				}
				argTypes.setElementAt(commonType, j);
			}
		}
		return BT_MethodSignature.create(returnType, argTypes, repository);
	}
	
	BT_Class getCommonType(BT_ClassVector classes) {
		BT_Class commonType = null;
		for(int i=0; i<classes.size(); i++) {
			BT_Class type = classes.elementAt(i);
			commonType = getCommonType(commonType, type);
			if(commonType == null) {
				return null;
			}
		}
		return commonType;
	}
	
	
	BT_Class getCommonType(BT_FieldVector fields) {
		BT_Class commonType = null;
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			BT_Class type = field.getFieldType();
			commonType = getCommonType(commonType, type);
			if(commonType == null) {
				return null;
			}
		}
		return commonType;
	}
	
	BT_Class getCommonType(BT_Class type1, BT_Class type2) {

		if(type1 == null) {
			return type2;
		}
		if(type1.equals(type2) || type1.isAncestorOf(type2)) {
			return type1;
		}
		//we need to find a common type
		if(type1.isPrimitive()) {
			if(!type2.isPrimitive()) {
				return null;
			}
			
			int store = type1.getOpcodeForStore();
			if(store != type2.getOpcodeForStore()) {
				return null;
			}
			//here we consider short, byte, char, int and boolean to be equivalent to the int type
			if(store == BT_Opcodes.opc_istore) {
				return repository.forName("int");
			}
			//else we must have matching long, float or double types
			return type1;
		}
		if(type2.isPrimitive()) {
			return null;
		}
		//we must find a common ancestor type
		if(type1.isInterface() || type2.isInterface()) {
			return repository.findJavaLangObject();
		}
		else if(((JaptClass) type2).isClassAncestorOf(type1)) {
			return type2;
		}
		return repository.findJavaLangObject();
	}
}
