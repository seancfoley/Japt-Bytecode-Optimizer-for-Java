package com.ibm.ive.tools.japt.obfuscation;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.StringVector;
			
		
/**
 * @author sfoley
 * <p>
 * Changes names of fields, methods and classes to reduce class file size
 *
 * <p>
 * Here is the algorithm:
 * 
 * <p>
 * 1. Make note of classes, fields and methods whose names must be preserved as specified on command line and in options files
 * <p>
 * 2. Make note of various names requiring preservation: <init>s, <clinit>s, serialized names, externalized names, names used by Class.forName, native method names
 * <p>
 * 3. Propagate method names that should be preserved.  Some names may need to be preserved because they are related to other names that have been marked preserved:
 *  overridden methods, overriding methods, implemented methods, implementing methods
 * <p>
 * 4. Determine which classes must remain in the same package because of other class names that must be preserved and
 *  their mutual package access requirements.  If specified as a command line option, some access permissions are broadened to
 *  reduce package access requirements, thus permitting more classes to be moved into the default package.
 *  Note that this is permitted by the java language specification, see http://java.sun.com/docs/books/jls/second_edition/html/binaryComp.doc.html#47259
 * <p>
 * 5. Move everything into the default package except for those classes whose package names must be preserved as specified above
 * <p>
 * 6. Create sets of fields and methods which can all be named the same thing.
 *  These sets are created by a complex algorithm that
 *  attempts to collect as many fields and methods as possible for the purpose of duplicating names 
 *  while also attempting to duplicate method and field name-types.  If a set does not span more than one class, then it is discarded for more
 *  complex renaming schemes later.
 * <p>
 *  Aside: If a class C with member x has a reference to a member y in some
 *  other class, then if both the name and type (field type or method signature) are the same then the constant pool of C can have both a single name-type entry and a
 *  single UTF8 name entry for both x and y.  
 * <p>
 * 7. Renames the members of all classes using the above mentioned sets.  Names are usually generated as short names, 
 * but in the case where all methods and fields are accessed only from within 
 * a single class, constant pool ldc strings that can double as method/field names are sometimes used.
 * <p>
 * 8.  Not all class members are a member of a set (see note about discarding sets in step 6), such class members are renamed, starting with methods first and fields afterwards.  
 * The names are generated using constant pool strings, names already used for methods in the class, or new generated names.
 * <p>
 * 9. Rename all classes in the default package by duplicating existing method or field names, generating new short names or
 *  obtaining names from constant pool ldc String entries.  For each class an optimal names is chosen by determining where the class is
 *  referenced from and then computing the best solution from the available names.
 * <p>
 * 10. Rename all classes in the non-default packages using short generated names.
 * 
 * 
 */
class NameCompressor {

 	private boolean retainingPermissions = true;
 	//private boolean preserveSerialization = false;
 	//private boolean preserveExternalization = false;
 	private boolean caseSensitiveClasses = false;
 	private boolean reuseConstantPoolStrings = true;
 	private boolean renamePackages = true;
 	private String baseName = "";
	private String packageBaseName = "";
	private String logFileName;
 	private NameHandler nameHandler;
 	private Logger logger;
 	private JaptRepository repository;
 	private Messages messages;
 	private boolean prepend;
 	
	public NameCompressor(JaptRepository repository, Logger logger, Messages messages, boolean prepend) {
		this.logger = logger;
		this.repository = repository;
		this.messages = messages;
		this.prepend = prepend;
		nameHandler = new NameHandler(logger, messages, prepend);
	}
	
	public void setLogFile(String log) {
		logFileName = log;
	}
	
	public void setPackageBaseName(String s) {
		if(s != null) {
			packageBaseName = s;
		}
		else {
			packageBaseName = "";
		}
	}
	
	public void setBaseName(String s) {
		if(s != null) {
			baseName = s;
		}
		else {
			baseName = "";
		}
	}
	
	public void setReuseConstantPoolStrings(boolean reuse) {
		reuseConstantPoolStrings = reuse;
	}
	
	public void setRetainingPermissions(boolean retain) {
		retainingPermissions = retain;
	}
	
	public void setCaseSensitiveClassNames(boolean caseSensitive) {
		caseSensitiveClasses = caseSensitive;
	}
	
	public void setRenamePackages(boolean rename) {
		renamePackages = rename;
	}
	
//	public void setPreserveSerialization(boolean preserve) {
//		preserveSerialization = preserve;
//		if(!preserve) {
//			preserveExternalization = false;
//		}
//	}
//	
//	public void setPreserveExternalization(boolean preserve) {
//		preserveExternalization = preserve;
//		if(preserve) {
//			preserveSerialization = true;
//		}
//	}
	
	
	public NameHandler getNameHandler() {
		return nameHandler;
	}
	
	public void compressNames() throws InvalidIdentifierException {
		BT_ClassVector classes = repository.getClasses();
		CommandLineNameFixer commandLineFixer = new CommandLineNameFixer(repository, nameHandler);
		StandardNameFixer standardFixer = new StandardNameFixer(repository,
			classes, nameHandler);
		PropagatedNameFixer propagatedFixer = new PropagatedNameFixer(repository, nameHandler, retainingPermissions, packageBaseName);
		
		//fix names specified on the command line
		commandLineFixer.fixNamesFromCommandLine();
		
		//fix names that are always fixed such as static initializer method names
		standardFixer.fixNames();
		
		//create a map which determines which methods are related through implementation and overriding
		RelatedMethodMap relatedMethodMap = repository.getRelatedMethodMap();
		
		//fix method names related to names already fixed
		propagatedFixer.propagateFixedMethodNames(relatedMethodMap);
	
		//partition the classes into packages 
		RenameablePackage packages[] = partitionPackages(classes, nameHandler);
		RenameablePackage newDefaultPackage = null;
		
		if(renamePackages) { 
			
			//determine the package to which we will attempt to move all classes
			String newPackageName = packageBaseName;
			RenameablePackage currentPackageWithThatName = null;
			boolean foundBaseNamePackage = false;
			for(int j=0; j<packages.length; j++) {
				if(packages[j].getPackageName().equals(newPackageName)) {
					currentPackageWithThatName = packages[j];
					foundBaseNamePackage = true;
					break;
				}
			}
			if(!foundBaseNamePackage) {
				newDefaultPackage = currentPackageWithThatName = new RenameablePackage(nameHandler, 
						caseSensitiveClasses, 
						reuseConstantPoolStrings, 
						prepend, 
						baseName);
				currentPackageWithThatName.setPackageName(newPackageName);
			}
			
			//determine which classes cannot be moved to a new package due to package access restrictions
			for(int i=0; i<packages.length; i++) {
				propagatedFixer.propagateFixedClassNames(packages[i], relatedMethodMap);
			}
			
			//move whatever we can into a single package, prefereably the default package
			for(int i=0; i<packages.length; i++) {
				//this will move some members of package[i] to currentPackageWithThatName
				packages[i].moveTo(currentPackageWithThatName);
			}
		}
		
		RelatedNameCollectorCreator rncc = new RelatedNameCollectorCreator(repository, nameHandler);
		
		if(!prepend) {
			ReferenceLinkedSetCreator rlsc = new ReferenceLinkedSetCreator(repository, nameHandler);
		
			//most fields and methods will be in a linked set
			renameClassMembersBySet(repository, nameHandler, rncc, rlsc);
			rlsc = null;
		}
		
		//some private and non-accessed non-private members are yet to be renamed
		renameClassMembersByClass(classes, nameHandler, relatedMethodMap, rncc);
		
		//rename classes - this should be done after methods and fields are renamed because
		//optimal class names are sometimes taken from the finalized method and field names
		//If the method and field names are not finalized, then the optimal class name calculations will be incorrect
		for(int i=0; i<packages.length; i++) {
//			if(packages[i].getPackageName().equals(packageBaseName)) {
//				System.out.println("hi");
//				theFlag = true;
//			} else {
//				theFlag = false;
//			}
			packages[i].renameClasses();
		}
		if(newDefaultPackage != null) {
			//if(packages[i].getPackageName().equals(packageBaseName)) {
				//System.out.println("hi");
				//theFlag = true;
			//} else {
			//	theFlag = false;
			//}
			
			newDefaultPackage.renameClasses();
		}
		
		if(logFileName != null) {
			outputLogFile();
		}
	}
	
	private void outputLogFile() {
		try {
			if(logFileName.indexOf('.') == -1) { //give it an extension
				logFileName += ".obfuscationLog";
			}
			PrintStream out = new PrintStream(new FileOutputStream(logFileName));
			Iterator changedItems;
			changedItems = nameHandler.getChangedClassNames();
			while(changedItems.hasNext()) {
				Map.Entry next = (Map.Entry) changedItems.next();
				BT_Class item = (BT_Class) next.getKey();
				String value = (String) next.getValue();
				print(out, value, item.getName());
			}
			
			changedItems = nameHandler.getChangedFieldNames();
			while(changedItems.hasNext()) {
				Map.Entry next = (Map.Entry) changedItems.next();
				BT_Field field = (BT_Field) next.getKey();
				String oldClassName = nameHandler.getPreviousClassName(field.getDeclaringClass());
				String value = (String) next.getValue();
				print(out, oldClassName + '.' + value, field.fullName());
			}
						
			changedItems = nameHandler.getChangedMethodNames();
			while(changedItems.hasNext()) {
				Map.Entry next = (Map.Entry) changedItems.next();
				BT_Method item = (BT_Method) next.getKey();
				String value = (String) next.getValue();
				BT_MethodSignature sig = item.getSignature();
				BT_ClassVector types = sig.types;
				BT_Class returnType = sig.returnType;
				StringVector vector = new StringVector(types.size());
				for(int i=0; i<types.size(); i++) {
					vector.addElement(nameHandler.getPreviousClassName(types.elementAt(i)));
				}
				String oldClassName = nameHandler.getPreviousClassName(item.getDeclaringClass());
				String oldSignature = BT_MethodSignature.toString(vector, nameHandler.getPreviousClassName(returnType));
				print(out, 
					oldClassName + '.' + value + oldSignature,
					item.fullName() + item.getSignature());
			}
			out.close();
		}
		catch(IOException e) {
			messages.COULD_NOT_OPEN_LOG.log(logger, logFileName);
		}
	}

	private void print(PrintStream stream, String oldName, String newName) {
		stream.print(newName);
		stream.print('=');
		stream.println(oldName);
	}
	
	/**
	 * Partitions a set of classes into distinct packages.
	 */
	 RenameablePackage[] partitionPackages(BT_ClassVector allClasses, NameHandler nameHandler) {
		BT_ClassVector classes = (BT_ClassVector) allClasses.clone();
		HashSet packagesSet = new HashSet(); //will contain elements of type RenameablePackage
		
		while(!classes.isEmpty()) {
			BT_Class clazz = classes.firstElement();
			classes.removeElementAt(0);
			RenameablePackage renameablePackage = new RenameablePackage(nameHandler, 
					caseSensitiveClasses, 
					reuseConstantPoolStrings, 
					prepend,
					baseName);
			renameablePackage.addClassToPackage(clazz);
			for(int j=classes.size() - 1; j>=0; j--) {
				BT_Class clazz2 = classes.elementAt(j);
				if(clazz.isInSamePackage(clazz2)) {
					classes.removeElementAt(j);
					renameablePackage.addClassToPackage(clazz2);	
				}
			}
			packagesSet.add(renameablePackage);
		}
		return (RenameablePackage[]) packagesSet.toArray(new RenameablePackage[packagesSet.size()]);
	}
	
	
	/** 
	 * Renames the fields and methods of all classes.
	 * Methods and fields that have fixed names will not be renamed.
	 */
	private void renameClassMembersByClass(BT_ClassVector classes, NameHandler nameHandler, RelatedMethodMap relatedMethodMap, RelatedNameCollectorCreator rncc) {
		for(int index = 0; index < classes.size(); index++) {
			BT_Class clazz = classes.elementAt(index);
			if(clazz.isArray()) {
				continue;
			}
			renameClassMembers(new HashSet(), clazz, nameHandler, relatedMethodMap, rncc);
		}
	}
	
	private void renameClassMembers(Set alreadyRenamed, BT_Class clazz, NameHandler nameHandler, RelatedMethodMap relatedMethodMap, RelatedNameCollectorCreator rncc) {
		BT_ClassVector kids = clazz.getKids();
		//we rename kids first - that ensures that the shortest names can be used most often.
		//Once a name is used in a parent class it cannot be used in any kid (with the exception of overloaded method names)
		for(int i=0; i<kids.size(); i++) {
			BT_Class kid = kids.elementAt(i);
			if(kid.isArray()) {
				continue;
			}
			renameClassMembers(alreadyRenamed, kid, nameHandler, relatedMethodMap, rncc);
		}
			
		if(!alreadyRenamed.contains(clazz)) {
			alreadyRenamed.add(clazz);
			RenameableClass renameableClass = new RenameableClass(clazz, nameHandler, relatedMethodMap, rncc, reuseConstantPoolStrings, prepend);
			renameableClass.renameMembers();
		}
	}
	
	void renameClassMembersBySet(JaptRepository repository, NameHandler nameHandler, RelatedNameCollectorCreator nameCollectorCreator, ReferenceLinkedSetCreator setCreator) {
		ReferenceLinkedSet sets[] = setCreator.getAllLinkedSets();
		for(int j=0; j<sets.length; j++) {
			ReferenceLinkedSet referenceLinkedSet = sets[j];
			RenameableSet set = new RenameableSet(repository, nameHandler, nameCollectorCreator, referenceLinkedSet, reuseConstantPoolStrings);
			if(referenceLinkedSet.classCount() == 1 || !set.hasOutsideAccessors()) {
				continue;
			}
			set.renameSet();
			referenceLinkedSet.clear();
		}
	}
	
}
