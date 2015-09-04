package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;
import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;

/**
 * Encapsulates functionality to rename the methods and fields of a single class.
 * <p>
 * Will also rename related methods of separate classes and interfaces, such as overriding or overridden methods.
 * @author sfoley
 */
class RenameableClass {

	private final BT_Class clazz;
	private final NameGenerator nameGenerator = new NameGenerator();
	private ConstantPoolNameGenerator constantPoolNameGenerator;
	private final UsedNameGenerator usedMethodNameGenerator = new UsedNameGenerator();
	private final UsedNameGenerator unusedGeneratedMethodNames = new UsedNameGenerator();
	private final NameHandler nameHandler;
	private final RelatedNameCollectorCreator nameCollectorCreator;
	private final RelatedMethodMap relatedMethodMap;
	//private final boolean caseSensitive;
	private boolean prepend;
	
	RenameableClass(BT_Class clazz, 
			NameHandler nameHandler, 
			RelatedMethodMap relatedMethodMap, 
			RelatedNameCollectorCreator rncc,
			boolean reuseConstantPoolNames,
			boolean prepend) {
		this.clazz = clazz;
		this.nameHandler = nameHandler;
		this.nameCollectorCreator = rncc;
		this.relatedMethodMap = relatedMethodMap;
		BT_MethodVector methods = clazz.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(nameHandler.nameIsFixed(method) && !StandardNameFixer.isStandardMethod(method)) {
				usedMethodNameGenerator.add(method.getName());
			}
		}
		if(reuseConstantPoolNames) {
			try {
				constantPoolNameGenerator = new ConstantPoolNameGenerator(clazz);
			}
			catch(BT_ClassFileException e) {}
		}
		this.prepend = prepend;
	}
	
	private void renameField(BT_Field field) {
		String newName;
		if(prepend) {
			newName = '_' + field.getName();
			while(fieldNameIsUnavailable(field, newName)) {
				newName = '_' + newName;
			}
		}
		else {
			newName = getNextFieldName(field);
		}
		renameField(field, newName);
	}
	
	private String getNextFieldName(BT_Field field) {
		while(true) {
			String newName = usedMethodNameGenerator.getName();
			if(newName == null) {
				break;
			}
			if(!fieldNameIsUnavailable(field, newName)) {
				return newName;
			}
		}
		
		if(constantPoolNameGenerator != null) { 
			constantPoolNameGenerator.reset();
			if(!hasOutsideAccessors(field)) {
				while(true) {
					String result = constantPoolNameGenerator.getLongestName();
					if(result == null) {
						break;
					}
					//if the field name is not available it should be removed because no other field can use it either
					//if it is available then we will use it so we should remove it in this case as well
					constantPoolNameGenerator.removeLast();
					if(!fieldNameIsUnavailable(field, result)) {
						return result;
					}
				}
			}
		}
		
		while(true) {
			String result = nameGenerator.peekName();
			if(!fieldNameIsUnavailable(field, result)) {
				String cpName = getShortestAvailableConstantPoolFieldName(field, result.length());
				if(cpName != null) {
					result = cpName;
				}
				else {
					nameGenerator.getName();
				}
				return result;
			}
			else {
				nameGenerator.getName();
			}
		}
	}
	
	private String getShortestAvailableConstantPoolFieldName(BT_Field field, int maximumLength) {
		if(constantPoolNameGenerator == null) {
			return null;
		}
		while(true) {
			String result = constantPoolNameGenerator.getShortestName();
			if(result == null || UTF8Converter.convertToUtf8(result).length > maximumLength) {
				return null;
			}
			//if the field name is not available it should be removed because no other field can use it either
			//if it is available then we will use it so we should remove it in this case as well
			constantPoolNameGenerator.removeLast();
			if(!fieldNameIsUnavailable(field, result)) {
				//in this particular case we will end up checking availability a second time in the main
				//loop, so we could create a hack here to speed things up
				return result;
			}
		}
	}
	
	private boolean fieldNameIsUnavailable(BT_Field field, String name) {
		RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(field.getDeclaringClass());
		if(collector.fieldNameAlreadyExists(name)) {
			return true;
		}
		return false;
	}
	
	private void renameField(BT_Field field, String name) {
		nameHandler.rename(field, name);
		nameHandler.freezeName(field);
		RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(field.getDeclaringClass());
		collector.addFieldName(name);
	}
	
	
	private void renameFields() {
		if(constantPoolNameGenerator != null) {
			constantPoolNameGenerator.reset();
		}
		usedMethodNameGenerator.reset();
		nameGenerator.reset();
		BT_FieldVector fields = clazz.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			if(!nameHandler.nameIsFixed(field)) {
				renameField(field);
			}
		}
	}

	
	private String getNextMethodName(BT_Method method) {
		//we try to get a method name from the constant pool if the method is private or not-accessed from elsewhere
		if(constantPoolNameGenerator != null && !hasOutsideAccessors(method)) {
			constantPoolNameGenerator.reset();
			while(true) {
				String result = constantPoolNameGenerator.getLongestName();
				if(result == null) {
					break;
				}
				if(!methodNameIsUnavailable(method, result)) {
					//we don't add the name to the used list because 
					//1) it is still available from the constant pool list, and 
					//2) the name is potentially long so it should not be used by methods accessed from elsewhere
					return result;
				}
			}	
		}
		
		//we try to overload an existing short name
		usedMethodNameGenerator.reset();
		while(true) {
			String result = usedMethodNameGenerator.getName();
			if(result == null) {
				break;
			}
			if(!methodNameIsUnavailable(method, result)) {
				return result;
			}
		}
		
		//now we try to use a previously generated name that was unusable, so that
		//we do not waste any short names
		unusedGeneratedMethodNames.reset();
		while(true) {
			String result = unusedGeneratedMethodNames.getName();
			if(result == null) {
				break;
			}
			if(!methodNameIsUnavailable(method, result)) {
				
				//is there something just as short in the constant pool?
				String cpName = getShortestAvailableConstantPoolMethodName(method, result.length());
				if(cpName != null) {
					result = cpName;
				}
				else {
					unusedGeneratedMethodNames.removeLast();
				}
				usedMethodNameGenerator.add(result);
				return result;
			}
		}
		
		//now we generate a new short name
		while(true) {				
			String result = nameGenerator.peekName();
			if(methodNameIsUnavailable(method, result)) {
				nameGenerator.getName();
				unusedGeneratedMethodNames.add(result);
			}
			else {
				//is there something just as short in the constant pool?
				String cpName = getShortestAvailableConstantPoolMethodName(method, result.length());
				if(cpName != null) {
					result = cpName;
				}
				else {
					nameGenerator.getName();
				}
				usedMethodNameGenerator.add(result);
				return result;
			}		
		}
	}
	
	private String getShortestAvailableConstantPoolMethodName(BT_Method method, int maximumLength) {
		if(constantPoolNameGenerator == null) {
			return null;
		}
		while(true) {
			String result = constantPoolNameGenerator.getShortestName();
			if(result == null || UTF8Converter.convertToUtf8(result).length > maximumLength) {
				return null;
			}
			if(!methodNameIsUnavailable(method, result)) {
				return result;
			}
		}
	}
	
	private void renameMethod(BT_Method method) {
		String newName;
		if(prepend) {
			newName = '_' + method.getName();
			while(methodNameIsUnavailable(method, newName)) {
				newName = '_' + newName;
			}
		}
		else {
			newName = getNextMethodName(method);
		}
		renameMethod(method, newName);
	}
	
	private void renameMethods() {
		nameGenerator.reset();
		BT_MethodVector methods = clazz.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(!nameHandler.nameIsFixed(method)) {
				renameMethod(method);
			}
		}
	}

	void renameMembers() {
		//the order here is important because fields attempt to have the same names as methods.
		renameMethods();
		renameFields();
	}
	
	private boolean methodNameIsUnavailable(BT_Method methodToRename, String name) {
		Set checkedClasses = new HashSet();
		BT_MethodVector relatedMethods = relatedMethodMap.getRelatedMethods(methodToRename);
		for(int j=0; j<relatedMethods.size(); j++) {
			BT_Method method = relatedMethods.elementAt(j);
			BT_Class mClass = method.getDeclaringClass();
			if(checkedClasses.contains(mClass)) {
				continue;
			}
			checkedClasses.add(mClass);
			RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(mClass);
			if(collector.methodNameAlreadyExists(name, method.getSignature())) {
				return true;
			}
		}
		return false;
	}
	
	private void renameMethod(BT_Method methodToRename, String name) {
		BT_MethodVector relatedMethods = relatedMethodMap.getRelatedMethods(methodToRename);
		for(int j=0; j<relatedMethods.size(); j++) {
			BT_Method method = relatedMethods.elementAt(j); 
			RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(method.getDeclaringClass());
			nameHandler.rename(method, name);
			nameHandler.freezeName(method);
			collector.addMethodName(name, method.getSignature());
		}
	}


	/**
	 * @return true if the method is accessed from outside the declaring class
	 */
	public static boolean hasOutsideAccessors(BT_Method memberMethod) {
		if(!memberMethod.isPrivate()) {
			BT_MethodCallSiteVector accessors = memberMethod.callSites;
			if(accessors != null) {	
				for (int k = 0; k < accessors.size(); k++) {
					BT_MethodCallSite accessor = accessors.elementAt(k);
					BT_Class accessorClass = accessor.getFrom().getDeclaringClass();
					if (accessorClass != memberMethod.getDeclaringClass()) {
						return true;
					}		
				}
			}	
		}
		return false;
	}
	
	/**
	 * @return true if the field is accessed from outside the declaring class
	 */
	public static boolean hasOutsideAccessors(BT_Field memberField) {
		if(!memberField.isPrivate()) {
			BT_AccessorVector accessors = memberField.accessors;
			if(accessors != null) {	
				for (int k = 0; k < accessors.size(); k++) {
					BT_Accessor accessor = accessors.elementAt(k);
					BT_Class accessorClass = accessor.getFrom().getDeclaringClass();
					if (accessorClass != memberField.getDeclaringClass()) {
						return true;	
					}		
				}
			}	
		}
		return false;
	}
	
	
	
}
