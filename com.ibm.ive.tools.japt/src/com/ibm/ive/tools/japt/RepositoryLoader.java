package com.ibm.ive.tools.japt;


import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.ibm.ive.tools.japt.MemberActor.ClassActor;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassPathEntry;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Factory;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_StackShapeVisitor;
import com.ibm.jikesbt.BT_StackShapes;
import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;

/**
 * Creates the Japt repository
 * @author sfoley
 */
public class RepositoryLoader {
	public final JaptRepository repository;
	protected Messages messages;
	protected Logger logger;
	private ArchiveExtensionList archiveExtensions;
	
	public RepositoryLoader(
			JaptRepository repository) {
		this(repository, (JaptFactory) repository.factory);
	}
	
	public RepositoryLoader(
			JaptRepository repository,
			JaptFactory factory) {
		this(repository, factory.logger);
	}
			
	public RepositoryLoader(
			JaptRepository repository,
			Logger logger) {
			this.messages = repository.getFactory().messages;
			this.logger = logger;
			this.repository = repository;
			this.archiveExtensions = new ArchiveExtensionList();
	}
	
	public RepositoryLoader(
			JaptRepository repository,
			Logger logger,
			ArchiveExtensionList archiveExtensions) {
			this.messages = repository.getFactory().messages;
			this.logger = logger;
			this.repository = repository;
			this.archiveExtensions = archiveExtensions;
	}
	
	/**
	 * works the same as loadClasses, except that only stub classes are found
	 */
	public void findClassStubs(
			Identifier identifier, 
			ClassActor classActor) throws InvalidIdentifierException {
			
		BT_ClassVector stubs = repository.findClassStubs(identifier);
		if(classActor != null) {
			classActor.actOn(stubs);
		}
	}
	
	public void loadFile(String fileName) throws IOException {
		File file = new File(fileName).getCanonicalFile();
		if(!file.exists()) {
			throw new IOException();
		}
		
		//we create a class path entry that essentially contains only the one file we are attempting to load
		FileClassPathEntry classPathEntry;
		boolean isClass = file.getName().endsWith(".class");
		if(isClass) {
			classPathEntry = new ClassFileClassPathEntry(file, repository);
		} else {
			classPathEntry = new ResourceFileClassPathEntry(file);
		}
		BT_ClassPathEntry.BT_ClassPathLocation loc = classPathEntry.getLocation();
		repository.addExtendedClassPathEntry(classPathEntry);
		if(isClass) {
			repository.loadClass(loc.getClassName(), loc);
		} else {
			repository.loadResource(loc);
		}
	}
	
	/**
	 * loads all classes in the given classpath entries (separated by the patform specific path separator File.pathSeparator)
	 * as internal classes.
	 * @param classPathEntries
	 * @return a vector containing all classes loaded in the given classpath entries.
	 */
	public BT_ClassVector loadAll(String classPathEntries) {
		BT_ClassVector classes = null;
		ClassPathEntry cpes[] = repository.prependInternalClassPathEntry(classPathEntries, false);
		for(int i=0; i<cpes.length; i++) {
			BT_ClassVector clzs = loadAll(cpes[i]);
			if(i == 0) {
				classes = clzs;
			} else {
				classes.addAll(clzs);
			}
		}
		return classes;
	}
	
	public BT_ClassVector loadAll(ClassPathEntry toLoad) {
		BT_ClassVector classes = new BT_ClassVector();
		Iterator locations = toLoad.getLocations();
		while(locations.hasNext()) {
			BT_ClassPathLocation location = (BT_ClassPathLocation) locations.next();
			String name = location.getClassName();
			if(name != null) {
				BT_Class result = repository.loadClass(name, location);
				if(!result.isStub()) { //the result will be a stub if the file was not a valid class file
					classes.addElement(result);
				}
			} else {
				repository.loadResource(location);
			}
		}
		return classes;
	}
			
	public void optimize(boolean strict) {
		messages.OPTIMIZING.log(logger);
		
		BT_ClassVector classes = repository.getInternalClasses();
		for (int i = 0; i < classes.size(); i++) {
			BT_Class cls = classes.elementAt(i);
			BT_MethodVector methods = cls.methods;
			for (int j = 0; j < methods.size(); j++) {
				BT_Method method = methods.elementAt(j);
				
				BT_CodeAttribute code = method.getCode();
				if(code == null) {
					continue;
				}
				try {
					code.optimizeAndRemoveDeadCode(true);
				} catch(BT_CodeException e) {
					repository.getFactory().noteCodeException(e);
				} 
				
			}
		}
			
	}
	
	/**
	 * Check the internal classes in the repository for class and method verification errors
	 */
	public void verify() {
		verify(repository.getInternalClasses());
	}
	
	/**
	 * Check the given classes for class and method verification errors.
	 * This does not check for errors that will be found when each class is written to a class file.
	 */
	public void verify(BT_ClassVector classes) {
		messages.VERIFYING.log(logger);
		for (int i = 0; i < classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			
			if (clazz.throwsClassFormatError()) {
				clazz.makeThrowClassFormatError();
				messages.VERIFICATION_FAILURE.log(logger, clazz.fullName());
			} else if(clazz.throwsIncompatibleClassChangeError()) {
				clazz.makeThrowIncompatibleClassChangeError();
				messages.VERIFICATION_FAILURE.log(logger, clazz.fullName());
			} else if (clazz.throwsNoClassDefFoundError()) {
				clazz.makeThrowNoClassDefFoundError();
				messages.VERIFICATION_FAILURE.log(logger, clazz.fullName());
			} else if (clazz.throwsVerifyError()) {
				clazz.makeThrowVerifyError();
				messages.VERIFICATION_FAILURE.log(logger, clazz.fullName());
			} else if (clazz.throwsUnsupportedClassVersionError()) {
				clazz.makeThrowUnsupportedClassVersionError();
				messages.VERIFICATION_FAILURE.log(logger, clazz.fullName());
			} else {
				BT_MethodVector methods = clazz.getMethods();
				for (int j = 0; j < methods.size(); j++) {
					BT_Method method = methods.elementAt(j);
					if (!method.throwsVerifyError()) {
						String verifyError = verifyMethod(method);
						if (verifyError != null) {
							method.setThrowsVerifyErrorTrue();
							messages.VERIFICATION_FAILURE_MSG.log(logger, new Object[] {method.useName(), verifyError});
						}
					} else {
						messages.VERIFICATION_FAILURE.log(logger, method.useName());
					}
					if (method.throwsVerifyError()) {
						method.makeCodeThrowVerifyError();
						//a number of method verification errors are found at class
						//verification time (if not all) so we must make the class
						//throw a verify error when it is initialized
						clazz.setThrowsVerifyErrorTrue();
					}
				} 
			}
		}
	}
	
	/**
	 * @param calculatedValue
	 * @param initializedValue
	 * @param name
	 * @return true if the max values did verify
	 */
	public void checkExcessiveMax(int calculatedValue, int initializedValue, String name, BT_CodeAttribute code) {
		if(calculatedValue < initializedValue) {
			messages.EXCESSIVE_VALUE.log(logger, 
					new Object[] {
						Integer.toString(initializedValue),
						name,
						code.getMethod().useName(),
						code.getMethod().getDeclaringClass(),
						Integer.toString(calculatedValue)
					});
			code.resetCachedCodeInfo();
		}
	}
	
	//Similar to BT_CodeAttribute.verifyCode
	public void verifyCode(BT_CodeAttribute code) throws BT_CodeException {
		BT_StackShapeVisitor visitor = new BT_StackShapeVisitor(code);
		visitor.ignoreUpcasts(false);
		visitor.useMergeCandidates(false); //no need to use merge candidates for verification, in fact should not in order to be able to do more verification
		visitor.setAbsoluteMaxStacks(code.getMaxLocals(), code.getMaxStack());
		BT_StackShapes shapes = visitor.populate();
		if(shapes == null) {
			return;
		}
		
		/* 
		 * compare the true stack depth with the stack depth stored by code.stackInfo (and so the same for the locals)
		 * which may have been read from the class file.  
		 */
		checkExcessiveMax(shapes.maxDepth, code.getMaxStack(), "maxstack", code);
		checkExcessiveMax(shapes.maxLocals, code.getMaxLocals(), "maxlocals", code);
		
		shapes.verifyStacks();
		
		code.verifyRelationships(BT_Factory.strictVerification);
	}
	
	/**
	 * Performs verification that is not performed by the JikesBT loading process or the JikesBT write process
	 */
	public String verifyMethod(BT_Method method) {
		BT_CodeAttribute code = method.getCode();
		if (code == null) {
			return null;
		}
		
		try {
			verifyCode(code);
		} 
		/*
		 * the following BT_CodeException subclasses are caught here:
		 * 
		 * thrown by BT_CodeAttribute.visitReachableCode:
		 * BT_CodePathException
		 * BT_CircularJSRException
		 * 
		 * thrown by BT_StackShapeVisitor:
		 * BT_InconsistentStackDepthException
		 * BT_InconsistentStackTypeException
		 * BT_StackUnderflowException
		 * BT_InvalidStackTypeException
		 * BT_UninitializedLocalException
		 * BT_InvalidLoadException
		 * BT_InvalidStoreException
		 * BT_StackOverflowException,
		 * BT_LocalsOverflowException,
		 * BT_MissingConstructorException
		 * 
		 * thrown by BT_CodeAttribute.verifyRelationShips:
		 * BT_AbstractInstantiationException
		 * BT_IllegalClinitException
		 * BT_IllegalInitException
		 * BT_IncompatibleMethodException
		 * BT_IncompatibleFieldException
		 * BT_IncompatibleClassException
		 * BT_AbstractMethodException
		 * BT_InvalidReturnException
		 * BT_AccessException
		 * 
		 * thrown by verifyStacks:
		 * BT_StackUnderflowException
		 * BT_InvalidStackTypeException
		 * BT_AccessException for protected access in objects
		 */
		catch(BT_CodeException e) {
			return messages.ERROR.toString(e.getMessage());
		} 
		return null;
	}
	
	
	
	
	public boolean isClassPathEntry(String name) {
		return archiveExtensions.isClassPathEntry(name);
	}
	
	public boolean isArchive(String fileName) {
		return archiveExtensions.isArchive(fileName);
	}
	
	
}
