package com.ibm.ive.tools.japt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.ive.tools.japt.MemberActor.ClassActor;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.PatternString.PatternStringPair;
import com.ibm.ive.tools.japt.PatternString.PatternStringTriple;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassComparator;
import com.ibm.jikesbt.BT_ClassPathEntry;
import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.BT_Factory;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_HashedFieldVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_JarResource;
import com.ibm.jikesbt.BT_JarResourceVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Repository;
import com.ibm.jikesbt.BT_ResourceComparator;
import com.ibm.jikesbt.StringVector;

/**
 * This subclass of BT_Repository keeps track of those classes which were loaded as internal to the application, 
 * as opposed to those classes simply required on the class path.  It also has other functionality specific to japt,
 * such as loading classes and resources based on wildcard-enabled identifiers.
 * <p>
 * There are two important concepts related to any Japt module.  
 * <p>
 * A class that is part of the interface to the application or library is one that was explicitly specified on the 
 * command line to be included in the output.  The same is true of a method or field.  Such an item might be an entry point in the 
 * application and therefore any of its identifying characteristics must be preserved, such as the name, the
 * access permissions, whether it is static, the containing class, and any other characteristic that might affect how it is
 * accessed from elsewhere.  All such classes, methods and fields are detailed in the InternalClassesInterface object that may
 * be obtained from the repository.  This object in turn contains conditional interface items, each of which
 * specifies elements to the interface if and only if the conditional classes, methods and fields will become and 
 * remain a part of the repository.
 * <p>
 * Japt has two class paths, the internal class path and the external class path.  An internal class is any class that was 
 * loaded from the internal class path.  Classes loaded from the internal class path are considered internal to the
 * application or library, part of the working set, and may be altered and manipulated.  
 * Classes loaded from the external class path are considered
 * supporting classes, and cannot be altered in any way, and will not be considered part of the Japt output.  Regardless,
 * it is advisable that all external classes be loaded for optimal results.
 * <p>
 * For example, a typical midp applet might be loaded on the internal class path while the midp library might be loaded
 * on the external class path.  Since the midp library will be pre-exisiting and unalterable on any given platform,
 * it is not put on the internal class path and will not be included in the Japt output.  The possible entry points into
 * the midp midlet might be marked as "specified", such as the javax.microedition.Midlet virtual methods that might 
 * be called from the platform.
 * <p>
 * The more general example is that of a standard java application, whose main method is the single entry point.  If any
 * of the java.lang.reflection API is used, then those classes, methods and fields accessed dynamically by name 
 * should also be specified as entry points.
 * <p>
 * A third example might be a library that one wishes to optimize for a specific application.  The library would be
 * placed on the internal class path, while the application would be on the external class path.
 * <p>
 * Those who wish to use this repository should be aware of the interface to the internal classes and the internal/external elements
 * in order to properly make use of the repository and its classes.
 * <p>
 * @author sfoley
 */
public class JaptRepository extends BT_Repository {

	public static final BT_Class emptyClasses[] = new BT_Class[0];
	private BT_Class mainClass;
	private ArrayList otherMainClasses = new ArrayList();
	private ArrayList internalClassPaths = new ArrayList(); //contains ClassPathEntry
	private ArrayList extendedClassPathEntries = new ArrayList(); //contains FileClassPathEntry and other non-conventional entries
	private BT_ClassVector internalClasses = new BT_HashedClassVector();
	private InternalClassesInterface internalClassesInterface;
	private HashMap relatedClassCollectors = new HashMap();
	private HashMap accessorMethodGenerators = new HashMap();
	RelatedMethodMap relatedMethodMap;
	public ErrorReporter errorReporter;
	private Lock japtLoadLock = new ReentrantLock();

	
	public JaptRepository(JaptFactory factory) {
		super(factory);
		internalClassesInterface = new InternalClassesInterface(this);
	}
	
	public void sortClasses(BT_ClassComparator comparator) {
		internalClasses.sort(comparator);
		classes.sort(comparator);
		for(int i=0; i<internalClassPaths.size(); i++) {
			ClassPathEntry entry = (ClassPathEntry) internalClassPaths.get(i);
			entry.sortLoadedClasses(comparator);
		}
		
	}
	
	public void sortResources(BT_ResourceComparator comparator) {
		resources.sort(comparator);
		for(int i=0; i<internalClassPaths.size(); i++) {
			ClassPathEntry entry = (ClassPathEntry) internalClassPaths.get(i);
			entry.sortLoadedResources(comparator);
		}
	}
	
	public JaptFactory getFactory() {
		return (JaptFactory) factory;
	}
	
	public void setMainClass(BT_Class clazz) {
		mainClass = clazz;
	}
	
	public BT_Class getMainClass() {
		return mainClass;
	}
	
	public void setOtherMainClass(BT_Class clazz) {
		otherMainClasses.add(clazz);
	}
	
	public BT_Class[] getOtherMainClasses() {
		if(otherMainClasses.isEmpty()) {
			return emptyClasses;
		}
		return (BT_Class[]) otherMainClasses.toArray(new BT_Class[otherMainClasses.size()]);
	}
	
	public BT_Class[] getAllMainClasses() {
		if(mainClass == null) {
			return getOtherMainClasses();
		}
		if(otherMainClasses.isEmpty()) {
			return new BT_Class[] {mainClass};
		}
		
		BT_Class res[] = new BT_Class[otherMainClasses.size() + 1];
		res[0] = mainClass;
		for(int i=1; i<res.length; i++) {
			res[i] = (BT_Class) otherMainClasses.get(i-1);
		}
		return res;
	}
	
	public InternalClassesInterface getInternalClassesInterface() {
		return internalClassesInterface;
	}
	
	public RelatedClassCollector removeRelatedClassCollector(BT_Class clazz) {
		return (RelatedClassCollector) relatedClassCollectors.remove(clazz);
	}
	
	public RelatedClassCollector getRelatedClassCollector(BT_Class clazz) {
		RelatedClassCollector result = (RelatedClassCollector) relatedClassCollectors.get(clazz);
		if(result == null) {
			result = new RelatedClassCollector(clazz);
			relatedClassCollectors.put(clazz, result);
		}
		return result;
	}
	
	public AccessorMethodGenerator removeAccessorMethodGenerator(BT_Class clazz) {
		return (AccessorMethodGenerator) accessorMethodGenerators.remove(clazz);
	}
	
	public AccessorMethodGenerator createAccessorMethodGenerator(BT_Class clazz) {
		AccessorMethodGenerator result = (AccessorMethodGenerator) accessorMethodGenerators.get(clazz);
		if(result == null) {
			if(!isInternalClass(clazz)) {
				return null;
			}
			result = new AccessorMethodGenerator(clazz);
			accessorMethodGenerators.put(clazz, result);
		}
		return result;
	}
	
	public AccessorMethodGenerator getAccessorMethodGenerator(BT_Class clazz) {
		AccessorMethodGenerator result = (AccessorMethodGenerator) accessorMethodGenerators.get(clazz);
		return result;
	}
	
	public RelatedMethodMap getRelatedMethodMap() {
		if(relatedMethodMap == null) {
			relatedMethodMap = new RelatedMethodMap(this);
		}
		return relatedMethodMap;
	}
	
	public void resetClassLoading() {
		super.resetClassLoading();
		extendedClassPathEntries.clear();
	}
	
	/**
	 * overrides parent @see com.ibm.jikesbt.loadFromClassPath(String, BT_ClassPathEntry.BT_ClassPathLocation)
	 * 
	 * Users are advised not to call this method but to call loadClass instead. 
	 * The loadClass method will return the existing class if it has already been loaded, while this method should only
	 * be called after it has been confirmed that the class has not been loaded.  This method also assumes
	 * that the given class path location represents a java class file and not some other form of class
	 * representation, while the loadClass method will defer to the class path entry to determine how a class
	 * will be loaded.
	 */
	protected BT_Class loadFromClassPath(String className, BT_ClassPathEntry.BT_ClassPathLocation location, BT_Class stub) {
		BT_Class clazz = super.loadFromClassPath(className, location, stub);
		// at this point we probably no longer hold the table lock;
		// but we do still hold the table lock if clazz is null
		if(clazz == null) {
			clazz = createStub(className);
			releaseTableLock(clazz);
		}
		registerClass(className, (ClassPathEntry) location.getClassPathEntry(), clazz);
		return clazz;
	}
	
	/**
	 * If a class has been loaded by an extension, then this method should be called to make the repository
	 * aware of the class.
	 * @param className
	 * @param location
	 * @param clazz
	 * @return
	 */
	private void registerClass(String className, ClassPathEntry classPathEntry, BT_Class clazz) {
		JaptFactory factory = getFactory();
		if(BT_Factory.multiThreadedLoading) {
			japtLoadLock.lock();
		}
		if(clazz.isStub()) {
			factory.noteClassNotLoaded(className, classPathEntry);
		} else if(internalClassPaths.contains(classPathEntry)) {
			classPathEntry.addClass(clazz);
			internalClasses.addElement(clazz);
			factory.noteInternalClassLoaded(clazz, classPathEntry);
		} else {
			factory.noteExternalClassLoaded(clazz, classPathEntry);
		}
		if(BT_Factory.multiThreadedLoading) {
			japtLoadLock.unlock();
		}
	}

	public BT_Class createInternalInterface(Identifier identifier, ClassPathEntry classPathEntry) throws InvalidIdentifierException {
		return createInternal(identifier, classPathEntry, true, new BT_ClassVersion());
	}
	
	public BT_Class createInternalInterface(
			Identifier identifier, 
			ClassPathEntry classPathEntry,
			BT_ClassVersion version) throws InvalidIdentifierException {
		return createInternal(identifier, classPathEntry, true, version);
	}
	
	private BT_Class createInternal(
			Identifier identifier, 
			ClassPathEntry classPathEntry, 
			boolean makeInterface,
			BT_ClassVersion version) throws InvalidIdentifierException {
		if(!identifier.isRegularString() || !identifier.isValidClassName()) {
			throw new InvalidIdentifierException(identifier);
		}
		if(!internalClassPaths.contains(classPathEntry)) {
			throw new IllegalArgumentException("class path entry must be internal");
		}
		String name = identifier.getPattern().getString();
		if(!canCreate(name)) {
			throw new IllegalArgumentException("class already exists, or is an array or a primitive");
		}
		BT_Class result = createStub(name);
		createInternalClass(
            classPathEntry,
            makeInterface,
            version,
            result);
		return result;
	}

	public void createInternalClass(
        	ClassPathEntry classPathEntry,
        	boolean makeInterface,
        	BT_Class stub) {
        createInternalClass(classPathEntry, makeInterface, new BT_ClassVersion(), stub);
    }
    
    public void createInternalClass(
        	ClassPathEntry classPathEntry,
        	boolean makeInterface,
        	BT_ClassVersion version,
        	BT_Class stub) {
        stub.setStub(false);
        stub.setInProject(factory.isProjectClass(stub.getName(), null));
        stub.version = version;
        if(makeInterface) {
        	stub.becomeInterface();
        } else {
        	stub.becomeClass();
        }
        classPathEntry.addClass(stub);
        internalClasses.addElement(stub);
        getFactory().noteInternalClassCreated(stub, classPathEntry);
    }
	
	public BT_Class createInternalClass(Identifier identifier, ClassPathEntry classPathEntry) throws InvalidIdentifierException {
		return createInternal(identifier, classPathEntry, false, new BT_ClassVersion());
	}
	
	public BT_Class createInternalClass(
			Identifier identifier, 
			ClassPathEntry classPathEntry,
			BT_ClassVersion version) throws InvalidIdentifierException {
		return createInternal(identifier, classPathEntry, false, version);
	}
	/**
	 * determines whether a given class was loaded from one of the internal class path locations
	 */
	public boolean isInternalClass(BT_Class clazz) {
		return internalClasses.contains(clazz);
	}
	
	/**
	 * make an internal class an external one, while leaving the class in the repository.
	 * @param clazz
	 */
	public void changeToExternal(BT_Class clazz) {
		Iterator iterator = internalClassPaths.iterator();
		while(iterator.hasNext()) {
			ClassPathEntry entry = (ClassPathEntry) iterator.next();
			if(entry.loadedClass(clazz)) {
				entry.removeClass(clazz);
				break;
			}
		}
		internalClasses.removeElement(clazz);
	}
	
	/**
	 * make an internal resource an external one, while leaving the resource in the repository.
	 * @param resource
	 */
	public void changeToExternal(BT_JarResource resource) {
		Iterator iterator = internalClassPaths.iterator();
		while(iterator.hasNext()) {
			ClassPathEntry entry = (ClassPathEntry) iterator.next();
			if(entry.loadedResource(resource)) {
				entry.removeResource(resource);
				break;
			}
		}
	}
	
	public boolean isInternalClassPathEntry(BT_ClassPathEntry cpe) {
		return internalClassPaths.contains(cpe);
	}
	
	/**
	 * Adds a class path entry to be used to load external classes.
	 * The entry is prepended to the list of existing entries and will
	 * be used first to attempt to locate a class.
	 * @param cpe
	 */
	public void prependExternalClassPathEntry(ClassPathEntry cpe) {
		getClassPathVector().insertElementAt(cpe, 0);
	}
	
	/**
	 * adds a class path entry to be used to load external classes.
	 * The entry is appended to the list of existing entries and will be
	 * used last to attempt to locate a class.
	 * @param cpe
	 */
	public void appendExternalClassPathEntry(ClassPathEntry cpe) {
		getClassPathVector().addElement(cpe);
	}
	
	/**
	 * Adds a class path entry to be used to load internal classes.
	 * The entry is prepended to the list of existing entries and will
	 * be used first to attempt to locate a class.
	 * @see addExtendedClassPathEntry(ClassPathEntry)
	 * @param cpe
	 */
	public void prependInternalClassPathEntry(ClassPathEntry cpe) {
		getClassPathVector().insertElementAt(cpe, 0);
		internalClassPaths.add(cpe);
	}
	
	/**
	 * adds a class path entry to be used to load internal classes.
	 * The entry is appended to the list of existing entries and will be
	 * used last to attempt to locate a class.
	 * @see addExtendedClassPathEntry(ClassPathEntry)
	 * @param cpe
	 */
	public void appendInternalClassPathEntry(ClassPathEntry cpe) {
		getClassPathVector().addElement(cpe);
		internalClassPaths.add(cpe);
	}
	
	/**
	 * Adds a class path entry to be used to load internal classes.
	 * The entry is prepended to the list of existing entries and will
	 * be used first to attempt to locate a class.
	 * @see addExtendedClassPathEntry(ClassPathEntry)
	 * @param path
	 */
	public ClassPathEntry[] prependInternalClassPathEntry(String path, boolean relative) {
		return prependorAppendInternalClassPathEntry(path, false, relative);
	}
	
	/**
	 * Adds a class path entry to be used to load internal classes.
	 * The entry is appended to the list of existing entries and will be
	 * used last to attempt to locate a class.
	 * @see addExtendedClassPathEntry(ClassPathEntry)
	 * @param path
	 */
	public ClassPathEntry[] appendInternalClassPathEntry(String path) {
		return prependorAppendInternalClassPathEntry(path, true, true);
	}
	
	/**
	 * prepends a single classpath entry on the internal classpath
	 */
	private ClassPathEntry[] prependorAppendInternalClassPathEntry(String path, boolean append, boolean relative) {
		ArrayList entries = new ArrayList();
		StringVector paths = pathTokenizer(path, !append);
		for(int i=0; i<paths.size(); i++) {
			path = paths.elementAt(i);
			ClassPathEntry cpe = (ClassPathEntry) createClassPathEntry(path, relative);
			if(cpe != null) {
				if(append) {
					appendInternalClassPathEntry(cpe);
				}
				else {
					prependInternalClassPathEntry(cpe);
				}
				entries.add(cpe);
			}
		}
		return (ClassPathEntry[]) entries.toArray(new ClassPathEntry[entries.size()]);
	}
	
	/**
	 * finds the stubs of classes that could not be loaded from the class path.
	 * @param identifier
	 * @return
	 * @throws InvalidIdentifierException
	 */
	public BT_ClassVector findClassStubs(Identifier identifier) throws InvalidIdentifierException {
		if(!identifier.isValidClassName()) {
			throw new InvalidIdentifierException(identifier);
		}
		BT_ClassVector results = new BT_HashedClassVector();
		PatternString classSpecification = 
			new PatternString(internalToStandardClassName(identifier.getPattern().getString()));
		boolean isRegularString = classSpecification.isRegularString();
		if(isRegularString) {
			BT_Class element = getClass(classSpecification.toString());
			if(element != null) {
				/* make sure an attempt at loading has been made, which is not the case with some stubs */
				forName(element.getName());
				if(element.isStub()) {
					results.addElement(element);
				}
			}
		} else {
			BT_ClassVector searchClasses = classes;
			for(int i=0; i<searchClasses.size(); i++) {
				BT_Class element = searchClasses.elementAt(i);
				String name = element.getName();
				if(classSpecification.isMatch(name)) {
					/* make sure an attempt at loading has been made, which is not the case with some stubs */
					forName(name);
					if(element.isStub()) {
						results.addElement(element);
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * finds classes in the repository that match the given identifier.
	 * @param internalOnly search only those locations on the internal class path
	 */
	public BT_ClassVector findClasses(Identifier identifier, boolean internalOnly) throws InvalidIdentifierException {
		return findClasses(identifier, internalOnly, false);
	}
	
	/**
	 * finds all classes whose name matches the given identifier in the repository.
	 * @param identifier the class name identifier
	 * @param internalOnly look only at internal classes
	 * @param single stop after finding the first match
	 * @return a vector containing all classes found
	 * @throws InvalidIdentifierException the identifier is invalid
	 */
	public BT_ClassVector findClasses(Identifier identifier, boolean internalOnly, boolean single) throws InvalidIdentifierException {
		if(!identifier.isValidClassName()) {
			throw new InvalidIdentifierException(identifier);
		}
		BT_ClassVector results = new BT_HashedClassVector();
		PatternString classSpecification = 
			new PatternString(internalToStandardClassName(identifier.getPattern().getString()));
		boolean isRegularString = classSpecification.isRegularString();
		if(isRegularString) {
			BT_Class candidate;
			if(internalOnly) {
				candidate = internalClasses.findClass(classSpecification.toString());
			} else {
				candidate = getClass(classSpecification.toString());
			}
			if(candidate != null) {
				results.addElement(candidate);
			}
		} else {
			BT_ClassVector searchClasses = internalOnly ? internalClasses : classes;
			for(int i=0; i<searchClasses.size(); i++) {
				BT_Class element = searchClasses.elementAt(i);
				if(classSpecification.isMatch(element.getName())) {
					results.addElement(element);
					if(single) {
						break;
					}
				}
			}
		}
		if(!identifier.isSilent() && results.size() == 0) {
			getFactory().noteNoMatch(identifier);
		}
		return results;
	}
	
	/**
	* adds a class path entry to the overall class path vector.  The difference between this method and
	* the append/prependInternalClassPathEntry methods is that an extended class path entry will not be used 
	* by the system to load new classes.  So it is up to the user to create or load a class
	* using such a class path entry.  Examples of ClassPathEntry types that are appropriate
	* to be used with this method include com.ibm.ive.tools.japt.FileClassPathEntry and 
	* com.ibm.ive.tools.japt.SyntheticClassPathEntry.
	* 
	* Note that all classes in the system must be associated with a class path entry.  Users who wish to
	* create new classes using createInternalClass can create a SyntheticClassPathEntry that is passed to 
	* both this method and the createInternalClass method.
	*/
	public void addExtendedClassPathEntry(ClassPathEntry entry) {
		extendedClassPathEntries.add(entry);
		internalClassPaths.add(entry);
	}
	
	/**
	 * @return the JIKESBT classpath extended by classpath entries created by 
	 * RepositoryGenerator.loadFile(String fileName) or japt extensions
	 */
	Vector getExtendedClassPathVector() {
		Vector v = getClassPathVector();
		Vector result = new Vector(v.size() + extendedClassPathEntries.size());
		result.addAll(extendedClassPathEntries);
		result.addAll(v);
		return result;
	}
	
	/**
	 * loads classes into the repository that match the given class name
	 * or class name specification. Will return all class files loaded that match the specification.
	 * @param internalOnly search only those locations on the internal class path
	 */
	public BT_ClassVector loadClasses(Identifier identifier, boolean internalOnly) throws InvalidIdentifierException {
		if(!identifier.isValidClassName()) {
			throw new InvalidIdentifierException(identifier);
		}
		BT_ClassVector results = new BT_HashedClassVector();
		PatternString classSpecification = identifier.getPattern();
		String spec = internalToStandardClassName(classSpecification.toString());
		int index = Identifier.getArrayBracketsIndex(spec);
		if(index >= 0) {
			BT_Class arrayClass = getClass(spec);
			if(arrayClass == null) {
				arrayClass = loadInternalClass(spec, false);
			}
			if(arrayClass != null) {
				results.addElement(arrayClass);
			}
		} else {
			//Vector vector = getExtendedClassPathVector();
			Vector vector = getClassPathVector();
			boolean isRegularString = classSpecification.isRegularString();
			for(int j=0; j<vector.size(); j++) {
				ClassPathEntry entry = (ClassPathEntry) vector.elementAt(j);
				if(internalOnly && !internalClassPaths.contains(entry)) {
					continue;
				}
				if(isRegularString) {
					BT_ClassPathLocation location = entry.findClass(spec);
					if(location != null) {
						//note that classes that failed to load and are represented by stubs are included in the results 
						results.addElement(loadClass(location.getClassName(), location));
						return results;
					}
				} else {
					BT_ClassPathLocation locations[] = entry.findClassWithPattern(classSpecification);
					for(int k=0; k<locations.length; k++) {
						BT_ClassPathLocation currentLoc = locations[k];
						String className = currentLoc.getClassName();
						//note that classes that failed to load and are represented by stubs are included in the results 
						results.addUnique(loadClass(className, locations[k]));
					}
				}
			}
			
			//find any additional classes that were loaded from somewhere other than the file system,
			//such as a synthetic classpath entry or possibly a jxelink map
			Identifier silentIdentifier = (Identifier) identifier.clone();
			silentIdentifier.setSilent(true);
			BT_ClassVector additional = findClasses(silentIdentifier, internalOnly);
			for(int i=0; i<additional.size(); i++) {
				results.addUnique(additional.elementAt(i));
			}
		}
		if(!identifier.isSilent() && results.size() == 0) {
			getFactory().noteNoMatch(identifier);
		}
		return results;
	}
	
	/**
	 * loads resources into the repository that match the given resource name
	 * or resource name specification. Will return all resources loaded as well
	 * as all resources already loaded that match the specification.
	 * Will load only a single resource for each given name, so if the identifier
	 * contains no wildcards a maximum of one resource will be loaded.
	 * @param internalOnly search only those locations on the internal class path
	 */
	public BT_JarResourceVector loadResources(Identifier identifier, boolean internalOnly) {
		BT_JarResourceVector results = new BT_JarResourceVector();
		Identifier silentIdentifier = (Identifier) identifier.clone();
		silentIdentifier.setSilent(true);
		BT_ClassPathLocation[] locations = findResourceLocations(silentIdentifier, internalOnly);
		for(int i=0; i<locations.length; i++) {
			results.addUnique(loadResource(locations[i]));
		}
		
		
		//find any additional classes that were loaded from somewhere other than the file system,
		//such as some other type of classpath entry or possibly a jxelink map
		if(!internalOnly) { /* unlike with classes, internal resources MUST exist somewhere on the internal classpath, 
			 				 * since there is no vector containing all internal resources together.  
			 				 * Therefore, if we are looking for internal resources, we would have found them already.
			 				 */
			BT_JarResourceVector additional = findResources(silentIdentifier);
			for(int i=0; i<additional.size(); i++) {
				results.addUnique(additional.elementAt(i));
			}
		}
		
		if(!identifier.isSilent() && results.size() == 0) {
			getFactory().noteNoMatch(identifier);
		}
		return results;
	}
	
	/**
	 * finds resources in the repository that match the given identifier.
	 * @param identifier
	 * @param internalOnly
	 */
	public BT_JarResourceVector findResources(Identifier identifier) {
		BT_JarResourceVector results = new BT_JarResourceVector();
		if(identifier.isRegularString()) {
			BT_JarResource resource = new NamedResource(identifier.toString(), null);
			int index = resources.indexOf(resource);
			if(index >= 0) {
				results.addElement(resources.elementAt(index));
			}
		}
		else {
			PatternString resSpecification = identifier.getPattern();
			for(int i=0; i<resources.size(); i++) {
				BT_JarResource res = resources.elementAt(i);
				if(resSpecification.isMatch(res.name)) {
					results.addElement(res);
				}
			}
		}
		if(!identifier.isSilent() && results.size() == 0) {
			getFactory().noteNoMatch(identifier);
		}
		return results;
	}
	
	/**
	 * Finds the locations on the classpath of resources matching the given identifier.
	 * @param identifier
	 * @param internalOnly
	 * @return
	 */
	public BT_ClassPathLocation[] findResourceLocations(Identifier identifier, boolean internalOnly) {
		ArrayList results = new ArrayList();
		/* replace platform specific separators with generalized separators */
		String s = identifier.getPattern().getString();
		PatternString resourceSpecification = new PatternString(ClassPathEntry.fileNameForResourceName(s));
		
		boolean isRegularString = resourceSpecification.isRegularString();
		//Vector vector = getExtendedClassPathVector();
		Vector vector = getClassPathVector();
		for(int j=0; j<vector.size(); j++) {
			ClassPathEntry entry = (ClassPathEntry) vector.elementAt(j);
			if(internalOnly && !internalClassPaths.contains(entry)) {
				continue;
			}
			if(isRegularString) {
				BT_ClassPathLocation location = entry.findFile(resourceSpecification.toString());
				if(location != null) {
					results.add(location);
					break;
				}
			}
			else {
				BT_ClassPathLocation locations[] = entry.findFileWithPattern(resourceSpecification);
				outerLoop:
				for(int k=0; k<locations.length; k++) {
					BT_ClassPathLocation location = locations[k];
					//check if this is a duplicate
					for(int i=0; i<results.size(); i++) {
						BT_ClassPathLocation other = (BT_ClassPathLocation) results.get(i);
						if(other.getName().equals(location.getName())) {
							continue outerLoop;
						}
					}
					results.add(location);
				}
			}
		}
		if(!identifier.isSilent() && results.size() == 0) {
			getFactory().noteNoMatch(identifier);
		}
		return (BT_ClassPathLocation[]) results.toArray(new BT_ClassPathLocation[results.size()]);
	}
	
	public BT_Class loadClass(String name, BT_ClassPathLocation location) {
		BT_Class clazz = super.loadClass(name, location);
		getFactory().noteClassLocated(clazz, location);
		return clazz;
	}
	
	/**
	 * Load the given classpath location and return the resource object, or
	 * simply return the resource object if it was already loaded from this location
	 * or elsewhere.
	 * @param location
	 * @return
	 */
	public BT_JarResource loadResource(ClassPathEntry.BT_ClassPathLocation location) {
		String name = location.getName();
		//we ensure that resources are identified by name by overriding the equals method
		BT_JarResource resource = new NamedResource(name, null);
		int index = resources.indexOf(resource);
		if(index >= 0) {
			return resources.elementAt(index);
		}
		InputStream in = null;
		try {
			in = location.getInputStream();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte buffer[] = new byte[1024];
			int bytesRead = in.read(buffer);
			while(bytesRead >= 0) {
				out.write(buffer, 0, bytesRead);
				bytesRead = in.read(buffer);
			}
			in.close();
			in = null;
			out.flush();
			resource.contents = out.toByteArray();
			out.close();
			resources.addElement(resource);
			ClassPathEntry classPathEntry = (ClassPathEntry) location.getClassPathEntry();
			classPathEntry.addResource(resource);
			getFactory().noteResourceLoaded(resource, classPathEntry);
			return resource;
		}
		catch(IOException e) {
			getFactory().noteResourceNotLoaded(location.getName(), location.getClassPathEntry());
			return null;
		}
		finally {
			if(in != null) {
				try {
					in.close();
				} catch(IOException e) {}
			}
		}
	}
		
	
	/**
	 * overrides parent method
	 */
	public BT_ClassPathEntry createClassPathEntry(String path) {
		return createClassPathEntry(path, true);
	}
	
	public BT_ClassPathEntry createClassPathEntry(String path, boolean relative) {
		try {
			path = path.replace('\\', '/');
			File file = new File(path);
			if (file.exists()) { // Hopefully, a zip file or a directory
				if(relative) {
					return new ClassPathEntry(file);
				} else {
					return new ClassSource(file, this);
				}
			} else {
				getFactory().noteClassPathEntryInexistent(path);
			}
		}
		catch(IOException e) {
			getFactory().noteClassPathProblem(path, e.toString());
		}
		return null;
	}
	
	/**
	 * Returns an array of all classpath entries
	 */
	public ClassPathEntry[] getAllClassPaths() {
		Vector v = getExtendedClassPathVector();
		return (ClassPathEntry[]) v.toArray(new ClassPathEntry[v.size()]);
	}
	
	/**
	 * All internal class paths to the japt program specified on the command line.
	 */
	public ClassPathEntry[] getInternalClassPaths() {
		return (ClassPathEntry[]) internalClassPaths.toArray(new ClassPathEntry[internalClassPaths.size()]);
	}
	
	/**
	 * @return all classes that were loaded from the internal class path
	 */
	public BT_ClassVector getInternalClasses() {
		return internalClasses;
	}
	
	/**
	 * removes a class from the repository.
	 */
	public void removeClass(BT_Class c) {
		super.removeClass(c);
		if(internalClasses.contains(c)) {
			internalClasses.removeElement(c);
			for(int i=0; i<internalClassPaths.size(); i++) {
				ClassPathEntry cpe = (ClassPathEntry) internalClassPaths.get(i);
				if(cpe.loadedClass(c)) {
					cpe.removeClass(c);
				}
			}
		}
		internalClassesInterface.removeFromInterface(c);
	}
	
	public void removeField(BT_Field f) {
		super.removeField(f);
		internalClassesInterface.removeFromInterface(f);
	}
	
	public void removeMethod(BT_Method m) {
		super.removeMethod(m);
		internalClassesInterface.removeFromInterface(m);
	}
				
	/**
	 * overrides parent method
	 */
	public BT_Class createClass(String nameComment) {
		BT_Class ret = new JaptClass(this);
		return ret;
	}
	
	/**
	 * overrides parent method
	 */
	public BT_Method createMethod(BT_Class c) {
		return new JaptMethod(c);
	}
	
	/**
	 * overrides parent method
	 */
	public BT_Field createField(BT_Class c) {
		return new JaptField(c);
	}
				
	
	
	
	
	/**
	 * 
	 * Finds fields and methods in the repository.
	 * @param actor will act on the specified members
	 * @param internalOnly use only the internal class path
	 * @param identifier the member identifier
	*/
	public void findMembers(
			Identifier identifier,
			MemberActor actor) throws InvalidIdentifierException {
		findMembers(identifier, false, actor);
	}
	
	public void findFields(
			Identifier identifier,
			MemberActor actor) throws InvalidIdentifierException {
		findFields(identifier, false, actor);
	}
	
	public void findMethods(
			Identifier identifier,
			MemberActor actor) throws InvalidIdentifierException {
		findMethods(identifier, false, actor);
	}
	
	public void findMethods(
			Identifier identifiers[],
			MemberActor actor){
		for(int i=0; i<identifiers.length; i++) {
			try {
				findMethods(identifiers[i], false, actor);
			} catch(InvalidIdentifierException e) {
				getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
	}
	
	public void findMembers(
			Identifier identifiers[],
			boolean internalOnly,
			MemberActor actor) {
		for(int i=0; i<identifiers.length; i++) {
			try {
				findMembers(identifiers[i], internalOnly, actor);
			} catch(InvalidIdentifierException e) {
				getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
	}
	
	public void findMembers(
			Identifier identifier,
			boolean internalOnly,
			MemberActor actor) throws InvalidIdentifierException {
		getMembers(identifier, false, true, false, internalOnly, actor);
	}
	
	public void findFields(
			Identifier identifiers[],
			boolean internalOnly,
			MemberActor actor) {
		for(int i=0; i<identifiers.length; i++) {
			try {
				findFields(identifiers[i], internalOnly, actor);
			} catch(InvalidIdentifierException e) {
				getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
	}
	
	public void findFields(
			Identifier identifier,
			boolean internalOnly,
			MemberActor actor) throws InvalidIdentifierException {
		getMembers(identifier, false, false, false, internalOnly, actor);
	}
	
	public BT_FieldVector findFields(
			Identifier identifier,
			boolean internalOnly) throws InvalidIdentifierException {
		BT_FieldVector result = new BT_HashedFieldVector();
		MemberCollectorActor actor = new MemberCollectorActor(null, result);
		findFields(identifier, internalOnly, actor);
		return result;
	}
	
	public void findMethods(
			Identifier identifiers[],
			boolean internalOnly,
			MemberActor actor) {
		for(int i=0; i<identifiers.length; i++) {
			try {
				findMethods(identifiers[i], internalOnly, actor);
			}
			catch(InvalidIdentifierException e) {
				getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
	}
	
	public void findMethods(
			Identifier identifier,
			boolean internalOnly,
			MemberActor actor) throws InvalidIdentifierException {
		getMembers(identifier, false, false, true, internalOnly, actor);
	}
	
	public BT_MethodVector findMethods(
			Identifier identifier,
			boolean internalOnly) throws InvalidIdentifierException {
		BT_MethodVector result = new BT_HashedMethodVector();
		MemberCollectorActor actor = new MemberCollectorActor(result, null);
		findMethods(identifier, internalOnly, actor);
		return result;
	}
	
	/**
	 * 
	 * Loads and finds in the repository all specified fields and methods.
	 * @param actor will act on the specified members
	 * @param identifier identifies the methods or fields (or both)
	 */
	public void loadMembers(
			Identifier identifier,
			MemberActor actor) throws InvalidIdentifierException {
		loadMembers(identifier, false, actor);
	}
	
	public void loadFields(
			Identifier identifier,
			MemberActor actor) throws InvalidIdentifierException {
		loadFields(identifier, false, actor);
	}
	
	public void loadMethods(
			Identifier identifier,
			MemberActor actor) throws InvalidIdentifierException {
		loadMethods(identifier, false, actor);
	}	
	
	public void loadMembers(
			Identifier identifier,
			boolean internalOnly,
			MemberActor actor) throws InvalidIdentifierException {
		getMembers(identifier, true, true, false, internalOnly, actor);
	}
	
	public void loadFields(
			Identifier identifier,
			boolean internalOnly,
			MemberActor actor) throws InvalidIdentifierException {
		getMembers(identifier, true, false, false, internalOnly, actor);
	}
	
	public void loadMethods(
			Identifier identifier,
			boolean internalOnly,
			MemberActor actor) throws InvalidIdentifierException {
		getMembers(identifier, true, false, true, internalOnly, actor);
	}	
	
	private void getMembers(
				Identifier identifier,
				boolean load, 
				boolean isBoth, 
				boolean isMethod,
				boolean internalOnly,
				MemberActor actor) throws InvalidIdentifierException {
		PatternStringPair membersIds[] = 
			isMethod ? identifier.splitAsMethodIdentifier() : identifier.splitAsMemberIdentifier();
		boolean wasFound = false;
		for(int j=0; j<membersIds.length; j++) {
			PatternStringPair member = membersIds[j];
			Identifier classIdentifier = new Identifier(member.first, true, identifier.getFrom());
			BT_ClassVector loadedClasses = 
				load ? loadClasses(classIdentifier, internalOnly) : findClasses(classIdentifier, internalOnly);
			if(loadedClasses.size() > 0) {
				//note the identifiers are checked for validity when split above
				if(isMethod) {
					PatternStringTriple method = (PatternStringTriple) member;
					//the selector will match a given method name/signature and a given field name just once, 
					//so for each class we use a new selector object
					for(int n=0; n<loadedClasses.size(); n++) {
						MemberSelector.NameTypePatternSelector spec = new MemberSelector.NameTypePatternSelector(method.second, method.third, identifier.isResolvable(), true, false);
						actor.actOn(loadedClasses.elementAt(n), spec);
						wasFound |= spec.wasFound;
					}
				}
				else {
					//the selector will match a given method name/signature and a given field name just once, 
					//so for each class we use a new selector object
					for(int n=0; n<loadedClasses.size(); n++) {
						MemberSelector.PatternSelector spec = new MemberSelector.PatternSelector(member.second, identifier.isResolvable(), isBoth, true);
						actor.actOn(loadedClasses.elementAt(n), spec);
						wasFound |= spec.wasFound;
					}
				}
			}
		}
		if(!identifier.isSilent() && !wasFound) {
			getFactory().noteNoMatch(identifier);
		}
	}
	
	public BT_ClassVector findClasses(Specifier specifiers[], boolean internalOnly) {
		BT_ClassVector res = new BT_HashedClassVector();
		for(int i=0; i<specifiers.length; i++) {
			Specifier id = specifiers[i];
			BT_ClassVector vec = findClasses(id, internalOnly);
			res.addAll(vec);
		}
		return res;
	}
	
	public BT_ClassVector findClasses(Specifier specifier, boolean internalOnly) {
		try {
			if(!specifier.isConditional() || specifier.conditionIsTrue(this)) {
				return findClasses(specifier.getIdentifier(), internalOnly);
			}
		}
		catch(InvalidIdentifierException e) {
			getFactory().noteInvalidIdentifier(e.getIdentifier());
		}
		return BT_ClassVector.emptyVector;
	}
	
	public void findMethods(Specifier methodsSpec[], MemberActor actor) {
		for(int i=0; i<methodsSpec.length; i++) {
			Specifier methodId1 = methodsSpec[i];
			findMethods(methodId1, actor);
		}
	}

	public void findMethods(Specifier specifier, MemberActor actor) {
		try {
			if(!specifier.isConditional() || specifier.conditionIsTrue(this)) {
				findMethods(specifier.getIdentifier(), actor);
			}
		}
		catch(InvalidIdentifierException e) {
			getFactory().noteInvalidIdentifier(e.getIdentifier());
		}
	}
	
	public void findFields(Specifier spec[], MemberActor actor) {
		for(int i=0; i<spec.length; i++) {
			Specifier id = spec[i];
			findFields(id, actor);
		}
	}
	
	public void findFields(Specifier specifier, MemberActor actor) {
		try {
			if(!specifier.isConditional() || specifier.conditionIsTrue(this)) {
				findFields(specifier.getIdentifier(), actor);
			}
		}
		catch(InvalidIdentifierException e) {
			getFactory().noteInvalidIdentifier(e.getIdentifier());
		}
	}
	
	public void findClasses(Specifier spec[], ClassActor classActor,
			MemberSelector memberSpecifier, boolean internalOnly) {
		for(int i=0; i<spec.length; i++) {
			Specifier id = spec[i];
			findClasses(id, classActor, memberSpecifier, internalOnly);
		}
	}
	
	public void findClasses(
			Specifier specifier, 
			ClassActor classActor,
			MemberSelector memberSpecifier,
			boolean internalOnly) {
		try {
			if(!specifier.isConditional() || specifier.conditionIsTrue(this)) {
				findClasses(specifier.getIdentifier(), classActor, memberSpecifier, internalOnly);
			}
		}
		catch(InvalidIdentifierException e) {
			getFactory().noteInvalidIdentifier(e.getIdentifier());
		}
	}
	
	/**
	 * Finds classes in the repository and acts on class members as specified.  The specified class names can contain wild cards as applicable
	 * to the class PatternString.  Once a class is found, those members specified by selector will be
	 * acted on by the given actor.
	 * 
	 * @param selector specifies which members to act on
	 * @param actor an actor which will act on the class members specified
	 */
	public void findClasses(
			Identifier identifier, 
			ClassActor classActor,
			MemberSelector selector,
			boolean internalOnly) throws InvalidIdentifierException {
		BT_ClassVector classes = findClasses(identifier, internalOnly);
		if(classActor != null) {
			if(selector == null) {
				classActor.actOn(classes);
			}
			else {
				classActor.actOn(classes, selector);
			}
		}
	}
	
	public BT_ClassVector findClasses(Identifier identifiers[], boolean internalOnly) {
		BT_ClassVector res = new BT_HashedClassVector();
		for(int i=0; i<identifiers.length; i++) {
			try {
				Identifier id = identifiers[i];
				BT_ClassVector vec = findClasses(id, internalOnly);
				if(i==0) {
					res.addAll(vec);
				} else {
					res.addAllUnique(vec);
				}
			}
			catch(InvalidIdentifierException e) {
				getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
		return res;
	}
	
	/**
	 * Loads classes into the repository and acts on class members as specified .  The specified class names can contain wild cards as applicable
	 * to the class PatternString.  Once a class is loaded (or a previously loaded class is found), those members specified by memberSpecifier will be
	 * acted on by the given actor.
	 * 
	 * @param selector specifies which members to act on
	 * @param actor an actor which will act on the class members specified
	 */
	public void loadClasses(
			Identifier identifier, 
			ClassActor classActor,
			MemberSelector selector) throws InvalidIdentifierException {
		BT_ClassVector loadedClasses = loadClasses(identifier, false);
		if(classActor != null) {
			if(selector == null) {
				classActor.actOn(loadedClasses);
			}
			else {
				classActor.actOn(loadedClasses, selector);
			}
		}
	}
	
	/**
	 * the basic idea of this method is to determine whether a particular method, although perhaps never called,
	 * may in fact prevent an instantiated class (either the method'd declaring class or a child class) 
	 * from becoming abstract.  So even though it is never called, it is required.
	 * 
	 * The logic is rather convoluted, but such a method must be a non-abstract method
	 * that overrides an abstract superclass method or implements a superinterface method and which
	 * is not overridden itself in an instantiated class.
	 * 
	 * @param removeAbstractions allows for making an abstract parent method (it will add a method body to the parent method) 
	 * in a superclass non-abstract.  This would then
	 * possibly prevent the given method from being required to fulfill class requirements, and in general will not change
	 * the behaviour of a bug-free program.
	 */
	public boolean methodFulfillsClassRequirements(BT_Method method, boolean removeAbstractions) {
		if(method.isAbstract() || method.isStatic() || method.isPrivate() || method.isConstructor()) {
			//there is no harm in removing an unused abstract method (in an interface or not) 
			//there is no harm in removing an unused static method, constructor, or private method
			return false;
		}
		BT_Class declaringClass = method.getDeclaringClass();
		boolean declaringClassIsNotAbstract = !declaringClass.isAbstract();
		BT_MethodVector allParents = getRelatedMethodMap().getAllParents(method);
		BT_Method offendingParentMethod = null;
		for(int i=0; i<allParents.size(); i++) {
			BT_Method parent = allParents.elementAt(i);
			if(parent.isAbstract()) {
				//this abstract parent might pose problems
				if(declaringClassIsNotAbstract || rendersKidAbstract(declaringClass, method)) {
					//removing the method will render one of the kid classes abstract		
					if(removeAbstractions && !parent.getDeclaringClass().isInterface()) {
						//we enter here just once when the parent is the superclass
						
						//since the parent is in a superclass, 
						//we can later make the parent method non-abstract,
						//but we'll only do so if the other parents all check out
						offendingParentMethod = parent;
					}
					else return true;
				}
			}
		}
		
		if(offendingParentMethod != null) {
			
			if(internalClassesInterface.isInEntireInterface(offendingParentMethod) || !isInternalClass(offendingParentMethod.getDeclaringClass())) {
				//do not make the abstract method non-abstract if the method is part of the interface or is not internal
				return true;
			} else {
				offendingParentMethod.makeCodeSimplyReturn();
			}
		}
		return false;
	}
	
	/**
	 * @return true if removing the method, which exists in clazz or a parent class of clazz,
	 * and has a parent method m that is abstract, will render the child class abstract. 
	 */
	private boolean rendersKidAbstract(BT_Class clazz, BT_Method method) {
		BT_ClassVector kids = clazz.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Class kid = kids.elementAt(i);
			BT_Method overridingMethod = getRelatedMethodMap().getOverridingMethod(kid, method);
			if(overridingMethod == null 
				&& (!kid.isAbstract() || rendersKidAbstract(kid, method))) {
				return true;
			}
		}
		return false;
	}
	
	
	
}