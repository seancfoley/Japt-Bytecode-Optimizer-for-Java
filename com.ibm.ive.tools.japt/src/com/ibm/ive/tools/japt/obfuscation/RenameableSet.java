package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;
import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;

/**
 * Renames all members of a reference linked set
 * @author sfoley
 */
class RenameableSet {

	private ReferenceLinkedSet referenceLinkedSet;
	private RelatedNameCollectorCreator nameCollectorCreator;
	private NameHandler nameHandler;
	private boolean reuseConstantPoolStrings;
	
	public RenameableSet(JaptRepository repository, 
						 NameHandler nameHandler, 
						 RelatedNameCollectorCreator nameCollectorCreator, 
						 ReferenceLinkedSet referenceLinkedSet,
						 boolean reuseConstantPoolStrings) {
		this.nameCollectorCreator = nameCollectorCreator;
		this.referenceLinkedSet = referenceLinkedSet;
		this.nameHandler = nameHandler;
		this.reuseConstantPoolStrings = reuseConstantPoolStrings;
	}
	
	boolean hasOutsideAccessors() {
		BT_MethodVector methods = referenceLinkedSet.getMethods();
		for(int j=0; j<methods.size(); j++) {
			if(RenameableClass.hasOutsideAccessors(methods.elementAt(j))) {
				return true;
			}
		}
		
		BT_FieldVector fields = referenceLinkedSet.getFields();
		for(int j=0; j<fields.size(); j++) {
			if(RenameableClass.hasOutsideAccessors(fields.elementAt(j))) {
				return true;
			}
		}
		return false;
	}
	
	void renameSet() {
		String name = findOptimalName();
		renameMembers(name);
	}
	
	
	private void renameMembers(String name) {
		BT_FieldVector fields = referenceLinkedSet.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			BT_Class fieldClass = field.getDeclaringClass();
			RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(fieldClass);
			nameHandler.rename(field, name);
			nameHandler.freezeName(field);
			collector.addFieldName(name);
		}
		
		BT_MethodVector methods = referenceLinkedSet.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			BT_Class methodClass = method.getDeclaringClass();
			RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(methodClass);
			nameHandler.rename(method, name);
			nameHandler.freezeName(method);
			collector.addMethodName(name, method.getSignature());
		}
	}
	
	private String findOptimalName() {
		BT_FieldVector fields = referenceLinkedSet.getFields();
		BT_MethodVector methods = referenceLinkedSet.getMethods();
		BT_ClassVector allClasses = new BT_HashedClassVector(fields.size() + methods.size());
		for(int i=0; i<fields.size(); i++) {
			allClasses.addUnique(fields.elementAt(i).getDeclaringClass());
		}
		for(int i=0; i<methods.size(); i++) {
			allClasses.addUnique(methods.elementAt(i).getDeclaringClass());
		}
		MultipleClassNameGenerator cpGenerator;
		try {
			cpGenerator = new MultipleClassNameGenerator(
					allClasses, 
					false, //the method names and field names will change later so we do not reuse them
					reuseConstantPoolStrings);
		}
		catch(BT_ClassFileException e) {
			cpGenerator = null;
		}
		NameGenerator nameGenerator = new NameGenerator();
		String name = null;
		String cpName = null;
		String genName = nameGenerator.getName();
		int lenCPName = 0, lenGenName = UTF8Converter.convertToUtf8(genName).length;
		do {
			if(name == genName) {
				genName = nameGenerator.getName();
				lenGenName = UTF8Converter.convertToUtf8(genName).length;
			}
			else if(cpGenerator != null) {
				cpName = cpGenerator.getName();
				if(cpName != null) {
					lenCPName = UTF8Converter.convertToUtf8(cpName).length;
				}
			}
			if(cpName == null || lenGenName < lenCPName) {
				name = genName;
			}
			else {
				name = cpName; 
			}
		} while(nameIsUnavailable(name));
		return name;
	}
	
	private boolean nameIsUnavailable(String name) {
		Set checkedClasses = new HashSet();
		BT_FieldVector fields = referenceLinkedSet.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			BT_Class fieldClass = field.getDeclaringClass();
			if(checkedClasses.contains(fieldClass)) {
				continue;
			}
			checkedClasses.add(fieldClass);
			RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(fieldClass);
			if(collector.fieldNameAlreadyExists(name)) {
				return true;
			}
		}
		
		checkedClasses.clear();
		BT_MethodVector methods = referenceLinkedSet.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			BT_Class methodClass = method.getDeclaringClass();
			if(checkedClasses.contains(methodClass)) {
				continue;
			}
			checkedClasses.add(methodClass);
			RelatedNameCollector collector = nameCollectorCreator.getRelatedNameCollector(methodClass);
			if(collector.methodNameAlreadyExists(name, method.getSignature())) {
				return true;
			}
		}
		return false;
	}

}
