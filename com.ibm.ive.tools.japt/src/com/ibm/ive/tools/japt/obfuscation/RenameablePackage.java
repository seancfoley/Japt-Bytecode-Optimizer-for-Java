package com.ibm.ive.tools.japt.obfuscation;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.UTF8Converter;
import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassFileException;
import com.ibm.jikesbt.BT_ClassReferenceSite;
import com.ibm.jikesbt.BT_ClassReferenceSiteVector;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_SignatureSite;
import com.ibm.jikesbt.BT_SignatureSiteVector;

		
/**
 * Represents a java package.  Has methods to be used for renaming members of java packages (classes)
 * and renaming members of classes (methods and fields)
 * @author sfoley
 */
class RenameablePackage  {

	BT_ClassVector classes = new BT_HashedClassVector();
	private String packageName;
	private NameHandler nameHandler;
	private NameGenerator classNameGenerator;
	private String baseName;
	private boolean reuseConstantPoolStrings;
	private boolean prepend;
	private boolean caseSensitive;
	
	RenameablePackage(NameHandler nameHandler, 
			boolean caseSensitive, 
			boolean reuseConstantPoolStrings, 
			boolean prepend,
			String baseName) {
		this.nameHandler = nameHandler;
		this.baseName = baseName;
		classNameGenerator = new NameGenerator(baseName, caseSensitive);
		this.reuseConstantPoolStrings = reuseConstantPoolStrings;
		this.prepend = prepend;
		this.caseSensitive = caseSensitive;
		if(!caseSensitive) {
			lowercaseClassNames = new HashMap();
		}
	}
	
	BT_ClassVector getClasses() {
		return classes;
	}
	
	/** 
	 * Renames classes in this package.
	 * Classes that haved fixed names will not be renamed.
	 */
	void renameClasses() {
		for(int index = 0; index < classes.size(); index++) {
			BT_Class clazz = classes.elementAt(index);
			if(clazz.isArray() || clazz.isBasicTypeClass) { //arrays are renamed according to their element classes
				continue;
			}
			 
			if(!nameHandler.nameIsFixed(clazz)) {
				renameClass(clazz);
			}
		}
	}
	
	/** 
	 * Moves classes in this package to newPackage.
	 * Classes that haved fixed names of fixed package names will not be moved.
	 */
	void moveTo(RenameablePackage newPackage) {
		if(newPackage == this) {
			return;
		}
		
		//we make a copy of the vector because items will be removed from the vector as classes are renamed
		BT_ClassVector classesCopy = (BT_ClassVector) classes.clone();
		
		for(int index = classesCopy.size() - 1; index >= 0; index--) {
			BT_Class clazz = classesCopy.elementAt(index);
			//array classes are renamed when their element classes are renamed
			if(!clazz.isArray() && !nameHandler.nameIsFixed(clazz) && !nameHandler.packageNameIsFixed(clazz)) {
				renamePackageNameOfClass(newPackage, clazz);
			}
		}
	}
	
	private String getNonQualifiedClassName(BT_Class clazz) {
		String className = clazz.getName();
		if(packageName.length() == 0) {
			return className;
		}
		return className.substring(packageName.length() + 1);
	}
	
	private void renamePackageNameOfClass(RenameablePackage newPackage, BT_Class clazz) {
		String newName;
		int suffix = 0;
		
		boolean nameClashes;
		do {
			newName = newPackage.generateNewName(getNonQualifiedClassName(clazz) + ((suffix == 0) ? "" : Integer.toString(suffix)));
			nameClashes = false;
			for(int j = 0; j < newPackage.classes.size(); j++) {
				BT_Class clazz2 = newPackage.classes.elementAt(j);
				if(clazz2.getName().equals(newName)) {
					nameClashes = true;
					suffix++;
					break;
				}
			}
		} while(nameClashes);
		removeContainedClassName(clazz, clazz.getName());
		
		nameHandler.renamePackage(clazz, newName);
		classes.removeElement(clazz);
		newPackage.addClassToPackage(clazz);
		nameHandler.freezePackageName(clazz);
		BT_ClassVector arrayTypes = clazz.asArrayTypes;

		//the array classes are automatically renamed by JikesBT when the class is renamed,
		//but we must also move them over here
		if(arrayTypes != null) {
			for(int i=0; i<arrayTypes.size(); i++) {
				BT_Class arrayClass = arrayTypes.elementAt(i);
				classes.removeElement(arrayClass);
				newPackage.addClassToPackage(arrayClass);
				nameHandler.freezePackageName(arrayClass);
			}
		}
		
	}
	
	/**
	 * Finds the optimal name for the class clazz to provide the most compression.
	 * 
	 * If this is not the default package, then a unique randomly generated name is used.
	 * If JIKES_BT is not configured to retain constant pools, then a randomly generated name is used.
	 * 
	 * Otherwise, the following algorithm is used:
	 * 
	 * First finds all classes that reference clazz, let the set of such classes be S.
	 * Let the referenced class clazz be represented by C.  
	 * This method searches the constant pools of all classes in S for possible names
	 * and determines the cost of using each name amongst clazz and the set S.  
	 * Also determines the cost of a newly generated random name.
	 * 
	 * Tries to use the name with the smallest cost.  If the name with the smallest cost is a duplicate
	 * of an existing class name for a class D, then that class is temporarily renamed if its name is not
	 * fixed and it has not been renamed already itself.  Otherwise the next best name is chosen for C.
	 * If necessary new names are generated until a new unique name is found for C.
	 * 
	 */
	private String findOptimalClassName(BT_Class clazz) {
		if(prepend) {
			String optimalName = '_' + clazz.getName();
			while(nameIsDuplicate(optimalName)) {
				optimalName = '_' + optimalName;
			}
			return optimalName;
		}
		if(!packageName.equals("")) {
			String optimalName = null;
			do {
				optimalName = generateNewName();
			} while(nameIsDuplicate(optimalName));
			return optimalName;
		}
		return calculateOptimalClassName(clazz);
	}
	

	private BT_ClassVector getClassesReferencing(BT_Class clazz) {
		
		BT_ClassVector referencers = new BT_HashedClassVector();
		getClassesReferencingThroughAccessors(referencers, clazz, clazz);
		
		BT_ClassReferenceSiteVector referenceSites = clazz.referenceSites;
		for(int i=0; i<referenceSites.size(); i++) {
			BT_ClassReferenceSite site = referenceSites.elementAt(i);
			referencers.addUnique(site.getFrom().getDeclaringClass());
		}
		
		return referencers;			
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	private BT_ClassVector getClassesReferencingThroughAccessors(BT_ClassVector referencers, BT_Class referencedClass, BT_Class clazz) {
		BT_MethodVector methods = clazz.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			BT_MethodCallSiteVector callSites = method.callSites;
			for(int j=0; j<callSites.size(); j++) {
				BT_MethodCallSite site = callSites.elementAt(j);
				if(site.instruction.getResolvedClassTarget(site.from).equals(referencedClass)) {
					referencers.addUnique(site.getFrom().getDeclaringClass());
				}
			}
		}
		
		BT_FieldVector fields = clazz.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			BT_AccessorVector accs = field.accessors;
			for(int j=0; j<accs.size(); j++) {
				BT_Accessor acc = accs.elementAt(j);
				if(acc.instruction.getResolvedClassTarget(acc.from).equals(referencedClass)) {
					referencers.addUnique(acc.getFrom().getDeclaringClass());
				}
			}
		}
		
		BT_ClassVector parents = clazz.getParents();
		for(int i=0; i<parents.size(); i++) {
			BT_Class parent = parents.elementAt(i);
			getClassesReferencingThroughAccessors(referencers, referencedClass, parent);
		}
		return referencers;
	}

	private int getTotalIndirectReferenceCount(BT_Class clazz) {
		int indirectReferenceCount = getIndirectReferenceCount(clazz);
		
		//for each array type (each array class with the class as its element class)
		//we need to count the same as above (calls to array methods and reference sites, accessors can be ignored since no fields)
		BT_ClassVector asArrayTypes = clazz.asArrayTypes;
		if(asArrayTypes != null) {
			for(int i=0; i<asArrayTypes.size(); i++) {
				BT_Class arrayClass = asArrayTypes.elementAt(i);
				indirectReferenceCount += getIndirectReferenceCount(arrayClass);
				indirectReferenceCount += getClassesReferencing(arrayClass).size();
			}
		}
		return indirectReferenceCount;
	}
	
	private int getIndirectReferenceCount(BT_Class clazz) {
		//the count we need are the locations where the class name appears that
		//will never be duplicates of existing String or method names in the constant pool
		//the cost is the sum of the indirect counts, plus those direct counts which cannot
		//be duplicates of existing strings or method/field names
		
		
		//additionally we count the appearances of the class in field types -
		//we count each one just once per class because there will be only one constant pool entry
		BT_FieldVector asFieldTypes = clazz.asFieldTypes;
		BT_ClassVector classes = new BT_HashedClassVector();
		if(asFieldTypes != null) {
			for(int i=0; i<asFieldTypes.size(); i++) {
				classes.addUnique(asFieldTypes.elementAt(i).getDeclaringClass());
			}
		}
		int indirectReferenceCount = classes.size();
		
		//additionally we count the appearance of the class in signatures -
		//we count once per unique signature in each class, since the same signature
		//appearing twice in the same class will share the same constant pool entry
		HashSet sigByClassSet = new HashSet();
		BT_SignatureSiteVector sites = clazz.asSignatureTypes;
		if(sites != null) {
			for(int i=0; i<sites.size(); i++) {
				BT_SignatureSite sitex = sites.elementAt(i);
				
				//we can maintain a set in which the entries are equal if
				//they have the same signature and index in the same class
				//ie they would have the same position in the same constant pool entry
				
				class SigByClass {
					BT_Class clazz;
					int index;
					BT_MethodSignature sig;
				
					SigByClass(BT_SignatureSite site) {
						BT_Method from = site.from;
						clazz = from.getDeclaringClass();
						sig = from.getSignature();
						index = site.index;		
					}
					
					public boolean equals(Object o) {
						if(o.getClass() != getClass()) {
							return false;
						}
						SigByClass other = (SigByClass) o; 
						return sig == other.sig
							&& index == other.index
							&& clazz.equals(other.clazz);
					}
				}
				SigByClass entry = new SigByClass(sitex);
				sigByClassSet.add(entry);
			}
		}
		
		indirectReferenceCount += sigByClassSet.size();
		return indirectReferenceCount;
	}


	private int calculateCost(int classCount, int frequencyCount, int indirectCount, String name, int utf8Length) {
		//value will hold the cost in size for the use of the selected name
		return
			indirectCount * utf8Length +  //we count the length of the name for each occurence inside a signature
				
				//and we count the size of the constant pool entry (3 + utf8Length) for each class that does not already contain a constant pool
				//entry for that particular name (as a method name, a field name or a constant pool ldc string)
			((3 + utf8Length) * (classCount - frequencyCount));
			
			//in the case of a jxe the cost here is actually:
			//indirectCount * utf8Length + 3 + utf8Length 
			//or more simply 
			//(indirectCount + 1) * utf8Length + 3
						
	}
	
	/**
	 * calculates the best choice for a class name given the following choices:
	 * - names of fields and methods in the class and related classes
	 * - strings in the constant pool that can double as class names amongst related classes
	 * - short generated names
	 * 
	 * The one caveat about this calculation is that it only considers the size of the
	 * resultant constant pools and their containing class files.  However, the class name
	 * will be stored in a jar file, file system or elsewhere and that will have an additional
	 * influence on the overall size of a java application on the platform involved.
	 */
	private String calculateOptimalClassName(BT_Class clazz) {
		String optimalName;
		String generatedName = null;
		SortedMap nameRankings = new TreeMap();
		BT_ClassVector referencingClassesVector = getClassesReferencing(clazz);
		int referencingClassesCount = referencingClassesVector.size();
		int indirectReferenceCount = getTotalIndirectReferenceCount(clazz);
		try {
			MultipleClassNameGenerator nameGen = new MultipleClassNameGenerator(referencingClassesVector, baseName.equals(""), reuseConstantPoolStrings);
			while(true) {
				MultipleClassNameGenerator.ListEntry nextEntry = nameGen.getEntry();
				if(nextEntry == null) {
					break;
				}
				String nextName = nextEntry.entry;
				int utf8Length = getUTF8Length(nextName);
				addRanking(nameRankings, nextName, 
					calculateCost(referencingClassesCount, nextEntry.frequency,
						indirectReferenceCount, nextEntry.entry, utf8Length));
			}
		}
		catch(BT_ClassFileException e) {}
			
		do {
			optimalName = null;
			if(generatedName == null) {
				//we need a randomly generated name in the set
				generatedName = classNameGenerator.peekName();
				addRanking(nameRankings, generatedName, 
					calculateCost(referencingClassesCount, 0,
						indirectReferenceCount, generatedName, generatedName.length()));
			}
			
			
			//get all names that provide the optimal compression
			Integer optimalValue = (Integer) nameRankings.firstKey();
			Set optimalSet = (Set) nameRankings.get(optimalValue);
			
			if(optimalSet.size() > 1) {
				//use the generated name if there is a tie, so that contained names might be reused later
				if(optimalSet.contains(generatedName)) {
					optimalSet.remove(generatedName);
					optimalName = generatedName;
					classNameGenerator.getName(); //dispense the generated name
					generatedName = null;
				}
				else {
					optimalName = (String) optimalSet.iterator().next();
					optimalSet.remove(optimalName);
				}
			}
			else {
				optimalName = (String) optimalSet.iterator().next();
				nameRankings.remove(optimalValue);
				if(optimalName.equals(generatedName)) {
					classNameGenerator.getName(); //dispense the generated name
					generatedName = null;
				}
			}
			
		} while(nameIsDuplicate(optimalName));
		return optimalName;
	}


	
	private void addRanking(SortedMap nameRankings, String string, int value) {
		Integer key = new Integer(value);
		Set result = (Set) nameRankings.get(key);
		if(result == null) {
			HashSet val = new HashSet();
			val.add(string);
			nameRankings.put(key, val);
		}
		else {
			result.add(string);
		}
	}
	
	/**
	 * Will check if a name is a duplicate of a fixed class name.
	 * 
	 * Temporarily renames classes that have not yet been renamed and are not fixed-name classes
	 * if necessary in order to avoid duplicates.
	 */
	private boolean nameIsDuplicate(String newName) {
		BT_ClassVector classesToChange = getContainedClassNames(newName);
		for(int i=0; i<classesToChange.size(); i++) {
			if(nameHandler.nameIsFixed(classesToChange.elementAt(i))) {
				return true;
			}
		}
		String secondName = newName;
		while(classesToChange.size() > 0) {
			BT_Class clazz2 = classesToChange.lastElement();
			classesToChange.removeElementAt(classesToChange.size() - 1);
			do {
 				secondName += 'X';
 			} while(nameIsDuplicate(secondName));
 			//this name change is only temporary, we know the name will be renamed later because
			//it is not fixed (once something is renamed it is set to fixed, so at the end everything will be fixed)
			changeContainedClassName(clazz2, secondName, clazz2.getName());
			nameHandler.rename(clazz2, secondName, false);
		}
		return false;
	}
	
	private static int getUTF8Length(String string) {
		return UTF8Converter.convertToUtf8(string).length;
	}
	
	private void renameClass(BT_Class clazz) {
		String newName = findOptimalClassName(clazz);
		String oldName = clazz.getName();
		if(!oldName.equals(newName)) {
			changeContainedClassName(clazz, newName, oldName);
		}
		nameHandler.rename(clazz, newName, true);
		nameHandler.freezeName(clazz);
	}
	
	
	private String generateNewName() {
		return generateNewName(classNameGenerator.getName());
	}
	
	private String generateNewName(String className) {
		if(packageName.equals("")) {
			return className;
		}
		return packageName + '.' + className;
	}
	
	/**
	 * returns the package name or null if the package has no members and setPackageName has not been called
	 */
	String getPackageName() {
		return packageName;
	}
	
	/**
	 * sets the name of this package if it does not have any members yet
	 * call renameMembers if you wish to generate a package with a new name
	 */
	void setPackageName(String newName) throws InvalidIdentifierException {
		if(classes.size() > 0 && !packageName.equals(newName)) {
			throw new IllegalArgumentException("Cannot set package name to " + newName + 
				", package name is already set to " + packageName);
		}
		Identifier identifier = new Identifier(newName);
		if(!identifier.isValidPackageName()) {
			throw new InvalidIdentifierException(identifier);
		}
		packageName = newName;
	}
	
	void addClassToPackage(BT_Class clazz) {
		String pkg = clazz.packageName();
		if(classes.size() == 0) {
			packageName = pkg;
		}
		else if(!packageName.equals(pkg)) {
			throw new IllegalArgumentException("class does not belong to correct package");
		}
		classes.addElement(clazz);
		addContainedClassName(clazz, clazz.getName());
	}
	
	private Map containedClassNames = new HashMap(); //maps names to the class in this package that has that name
	private Map lowercaseClassNames; //maps names in lowercase to the class in this package that has that name
													//two classes might have the same name in lowercase, so these names are mapped to BT_ClassVectors
	
	private void removeContainedClassName(BT_Class clazz, String name) {
		containedClassNames.remove(name);
		if(!caseSensitive) {
			String lowerCaseName = name.toLowerCase();
			BT_ClassVector lowerCased = (BT_ClassVector) lowercaseClassNames.get(lowerCaseName);
			lowerCased.removeElement(clazz);
		}
	}
	
	private void addContainedClassName(BT_Class clazz, String name) {
		containedClassNames.put(name, clazz);
		if(!caseSensitive) {
			String lowerCaseName = name.toLowerCase();
			BT_ClassVector lowerCased = (BT_ClassVector) lowercaseClassNames.get(lowerCaseName);
			if(lowerCased == null) {
				lowerCased = new BT_ClassVector(1);
				lowercaseClassNames.put(lowerCaseName, lowerCased);
			}
			lowerCased.addElement(clazz);
		}
	}
		
	private void changeContainedClassName(BT_Class clazz, String newName, String oldName) {
		removeContainedClassName(clazz, oldName);
		addContainedClassName(clazz, newName);
	}
	
	private BT_ClassVector getContainedClassNames(String name) {
		BT_ClassVector classesToChange = new BT_ClassVector(1);
		BT_Class clazz2 = (BT_Class) containedClassNames.get(name);
		if(clazz2 != null) {
			classesToChange.addElement(clazz2);
		}
		if(!caseSensitive) {
			BT_ClassVector moreClasses = (BT_ClassVector) lowercaseClassNames.get(name.toLowerCase());
			if(moreClasses != null) {
				classesToChange.addAllUnique(moreClasses);
			}
		}
		return classesToChange;
	}
	
}
