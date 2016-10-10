package com.ibm.ive.tools.japt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.ive.tools.japt.commandLine.StandardLogger;
import com.ibm.jikesbt.BT_Attribute;
import com.ibm.jikesbt.BT_AttributeException;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassPathEntry;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Exception;
import com.ibm.jikesbt.BT_Factory;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldRefIns;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_JarResource;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Repository;
import com.ibm.jikesbt.StringVector;
import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 * @author sfoley
 *
 */
public class JaptFactory extends BT_Factory {
	
	public Logger logger;
	protected int internalClassCount;
	protected int externalClassCount; 
	protected int resourceCount; 
	protected int notLoadedClassCount;
	protected int notLoadedResourceCount;
	protected int notFoundClassCount;
	private boolean ignoreClassesNotFound;
	
	public Messages messages;
	protected boolean verbose = true;
	private ArrayList locatedListeners = new ArrayList();
	
	public boolean saveUnresolved;
	private BT_MethodVector unresolvedMethods;
	private BT_FieldVector unresolvedFields;
	private BT_ClassVector unresolvedClasses;
	private StringVector ignoreClassNotFound = new StringVector();
	
	public JaptFactory() {
		this(new StandardLogger());
	}
	
	public JaptFactory(Logger logger) {
		this(new Messages(new Component() {
			public String getName() {
				return "japt";
			}
		}), logger);
	}
	
	public JaptFactory(Messages messages, Logger logger) {
		this(messages, logger, 
				true, //buildMethodRelationships
				true, //loadMethods
				false, //keepConstantPool
				true, //readDebugInfo
				false, //keepBytecodes
				true, //trackClassReferences
				true);//readStackMaps
	}
	
	private JaptFactory(Messages messages, Logger logger, 
			boolean buildMethodRelationships,
			boolean loadMethods,
			boolean keepConstantPool,
			boolean readDebugInfo,
			boolean keepBytecodes,
			boolean trackClassReferences, 
			boolean readStackMaps) {
		super(buildMethodRelationships,
				loadMethods,
				keepConstantPool,
				readDebugInfo,
				keepBytecodes,
				trackClassReferences, 
				readStackMaps
		);
		this.logger = logger;
		this.messages = messages;
	}
	
	public void clearUnresolved() {
		if(unresolvedMethods != null) {
			unresolvedMethods.removeAllElements();
		}
		if(unresolvedFields != null) {
			unresolvedFields.removeAllElements();
		}
		if(unresolvedClasses != null) {
			unresolvedClasses.removeAllElements();
		}
	}
	
	public void trimUnresolved() {
		if(unresolvedMethods != null) {
			unresolvedMethods.trimToSize();
		}
		if(unresolvedFields != null) {
			unresolvedFields.trimToSize();
		}
		if(unresolvedClasses != null) {
			unresolvedClasses.trimToSize();
		}
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	public Messages getMessages() {
		return messages;
	}
	
	public int getResourceCount() {
		return resourceCount;
	}
	
	public int getInternalClassCount() {
		return internalClassCount;
	}
	
	public int getExternalClassCount() {
		return externalClassCount;
	}
	
	public int getNotLoadedClassCount() {
		return notLoadedClassCount;
	}
	
	public int getNotLoadedResourceCount() {
		return notLoadedResourceCount;
	}
	
	public int getNotFoundClassCount() {
		return notFoundClassCount;
	}
	
	public void noteInvalidIdentifier(Identifier itemIdentifier) {
		String from = itemIdentifier.getFrom();
		if(from == null) {
			messages.INVALID_IDENTIFIER.log(logger, itemIdentifier);
		}
		else {
			messages.INVALID_IDENTIFIER_FROM.log(logger, new Object[] {itemIdentifier, from});
		}
	}
	
	public interface ClassLocatedListener {
		void classLocated(BT_Class c, BT_ClassPathLocation location);
	}
	
	public void addClassLocatedListener(ClassLocatedListener listener) {
		locatedListeners.add(listener);
	}
	
	public void removeClassLocatedListener(ClassLocatedListener listener) {
		locatedListeners.remove(listener);
	}
	
	public void noteClassLocated(BT_Class c, BT_ClassPathLocation location) {
		Iterator iterator = locatedListeners.iterator();
		while(iterator.hasNext()) {
			ClassLocatedListener listener = (ClassLocatedListener) iterator.next();
			listener.classLocated(c, location);
		}
	}
	
	public void noteClassLoaded(BT_Class c, String fromFileName) {}
	
	public void noteResourceLoaded(BT_JarResource resource, BT_ClassPathEntry cpe) {
		resourceCount++;
		if(verbose) {
			messages.LOADED_RESOURCE.log(logger, 
				new Object[] {resource.name, cpe.getEntryCanonicalName()}
			);
		}
	}
	
	public void noteInternalClassCreated(BT_Class clazz, BT_ClassPathEntry cpe) {
		internalClassCount++;
		if(verbose) {
			acquireFactoryLock();
			try {
				messages.CREATED_INTERNAL_CLASS.log(logger, 
					new Object[] {clazz.fullKindName(), clazz, cpe.getEntryCanonicalName()}
				);
			} finally {
				releaseFactoryLock();
			}
		}
		
	}
	
	public void noteInternalClassLoaded(BT_Class clazz, BT_ClassPathEntry cpe) {
		internalClassCount++;
		if(verbose) {
			acquireFactoryLock();
			try {
				messages.LOADED_INTERNAL_CLASS.log(logger, 
					new Object[] {clazz.fullKindName(), clazz, cpe.getEntryCanonicalName()}
				);
			} finally {
				releaseFactoryLock();
			}
		}
	}
	
	public void noteClassDereferenced(BT_Class clazz) {
		if(verbose && !clazz.isArray() && !clazz.isPrimitive()) {
			acquireFactoryLock();
			try {
				messages.DEREFERENCED_CLASS.log(logger, new Object[] {clazz.fullKindName(), clazz});
			} finally {
				releaseFactoryLock();
			}
		}
	}
	
	public void noteExternalClassLoaded(BT_Class clazz, BT_ClassPathEntry cpe) {
		externalClassCount++;
		if(verbose) {
			acquireFactoryLock();
			try {
				messages.LOADED_EXTERNAL_CLASS.log(logger,
					new Object[] {clazz.fullKindName(), clazz, cpe.getEntryCanonicalName()}
				);
			} finally {
				releaseFactoryLock();
			}
		}
	}
	
	public void noteClassNotLoaded(String className, BT_ClassPathEntry cpe) {
		acquireFactoryLock();
		try {
			messages.COULD_NOT_LOAD_CLASS.log(logger, 
				new String[] {className, cpe.getEntryCanonicalName()}
			);
		} finally {
			releaseFactoryLock();
		}
		notLoadedClassCount++;
	}
	
	public void noteResourceNotLoaded(String name, BT_ClassPathEntry cpe) {
		messages.COULD_NOT_LOAD_RESOURCE.log(logger, 
			new String[] {name, cpe.getEntryCanonicalName()}
		);
		notLoadedResourceCount++;
	}
	
	public void setIgnoreClassesNotFound(boolean ignore) {
		ignoreClassesNotFound = ignore;
	}
	
	public void noteClassSpecified(BT_Class clazz) {
		messages.INCLUDED_CLASS.log(logger, new Object[] {clazz.fullKindName(), clazz});
	}
	
	public void noteMethodSpecified(BT_Method method) {
		messages.INCLUDED_METHOD.log(logger, method.useName());
	}
	
	public void noteFieldSpecified(BT_Field field) {
		messages.INCLUDED_FIELD.log(logger, field.useName());
	}
	
	public void noteNoMatch(Identifier identifier) {
		String from = identifier.getFrom();
		if(from == null) {
			messages.NO_MATCH.log(logger, identifier);
		} else {
			messages.NO_MATCH_FROM.log(logger, new Object[] {identifier, from});
		}
	}
	
	public void noteUndeclaredMethod(BT_Method m, BT_Class targetClass, BT_Method fromMethod, BT_MethodRefIns fromIns, LoadLocation loadedFrom) {
		acquireFactoryLock();
		try {
			if(saveUnresolved && (unresolvedClasses == null || !unresolvedClasses.contains(m.getDeclaringClass()))) {
				if(unresolvedMethods == null) {
					unresolvedMethods = new BT_MethodVector();
				}
				unresolvedMethods.addUnique(m);
			}
			if(!ignoreClassNotFound.contains(m.getDeclaringClass().getName())) {
				messages.UNRESOLVED_METHOD.log(logger, new Object[] {m.useName(), fromMethod.useName()});
			}
		} finally {
			releaseFactoryLock();
		}
	}

	public void noteUndeclaredField(BT_Field f, BT_Class targetClass, BT_Method fromMethod, BT_FieldRefIns fromIns, LoadLocation loadedFrom) {
		acquireFactoryLock();
		try {
			if(saveUnresolved && (unresolvedClasses == null || !unresolvedClasses.contains(f.getDeclaringClass()))) {
				if(unresolvedFields == null) {
					unresolvedFields = new BT_FieldVector();
				}
				unresolvedFields.addUnique(f);
			}
			if(!ignoreClassNotFound.contains(f.getDeclaringClass().getName())) {
				messages.UNRESOLVED_FIELD.log(logger, new Object[] {f.useName(), fromMethod.useName()});
			}
		} finally {
			releaseFactoryLock();
		}
	}
		
	public BT_ClassVector getUnresolvedClasses() {
		if(unresolvedClasses == null) {
			unresolvedClasses = new BT_ClassVector();
		}
		return unresolvedClasses;
	}
	
	public BT_FieldVector getUnresolvedFields() {
		if(unresolvedFields == null) {
			unresolvedFields = new BT_FieldVector();
		}
		return unresolvedFields;
	}
	
	public BT_MethodVector getUnresolvedMethods() {
		if(unresolvedMethods == null) {
			unresolvedMethods = new BT_MethodVector();
		}
		return unresolvedMethods;
	}
	
	public void ignoreClassNotFound(String className) {
		ignoreClassNotFound.addUnique(className);
	}
	
	public BT_Class noteClassNotFound(String className, BT_Repository repository, BT_Class stub, LoadLocation referencedFrom) {
		acquireFactoryLock();
		try {
			if(!ignoreClassNotFound.contains(className)) {
				messages.COULD_NOT_FIND_CLASS.log(logger, className);
				if(!ignoreClassesNotFound) {
					notFoundClassCount++;
				}
			}
			if(stub == null) {
				//no need for locking here as we hold the table lock when this method is called
				stub = repository.createStub(className);
			}
			if(saveUnresolved) {
				if(unresolvedClasses == null) {
					unresolvedClasses = new BT_ClassVector();
				}
				unresolvedClasses.addUnique(stub);
			}
		} finally {
			releaseFactoryLock();
		}
		return stub;
	}
		
	public void noteFileCloseIOException(String resource, IOException e) {
		acquireFactoryLock();
		try {
			messages.EXCEPTION_CLOSING.log(logger, resource);
		} finally {
			releaseFactoryLock();
		}
	}
	
	public void noteAttributeLoadFailure(BT_Repository rep, BT_Item item, String name, BT_Attribute attribute, BT_AttributeException e, LoadLocation loadLocation) {
		acquireFactoryLock();
		try {
			messages.COULD_NOT_LOAD_ATTRIBUTE.log(logger, new Object[] {attribute == null ? name : attribute.getName(), item.useName(), e.getInitialCause()});
		} finally {
			releaseFactoryLock();
		}
	}
	
	public void noteAttributeWriteFailure(BT_Item item, BT_Attribute attribute, BT_AttributeException e) {
		messages.COULD_NOT_WRITE_ATTRIBUTE.log(logger, new Object[] {attribute == null ? item.getName() : attribute.getName(), item.useName(), e.getInitialCause()});
	}
	
	public void noteClassLoadFailure(
		BT_Repository rep,
		BT_ClassPathEntry entry,
		BT_Class clazz,
		String className,
		String fileName,
		Throwable ex,
		String equivalentRuntimeError) {
		if(ex instanceof BT_Exception) {
			ex = ((BT_Exception) ex).getInitialCause();
		}
		if(className == null && clazz != null) {
			className = clazz.getName();
		}
		acquireFactoryLock();
		try {
			ex.printStackTrace();//TODO remove later
			messages.VERIFY_FAILURE.log(logger, new Object[] {className, fileName, ex, equivalentRuntimeError});
		} finally {
			releaseFactoryLock();
		}
	}
	
	public void noteClassLoadError(
			BT_ClassPathEntry entry,
			BT_Class clazz,
			String className,
			String fileName,
			String problem,
			String equivalentRuntimeError) {
		if(className == null && clazz != null) {
			className = clazz.getName();
		}
		acquireFactoryLock();
		try {
			messages.VERIFY_FAILURE.log(logger, new Object[] {className, fileName, problem, equivalentRuntimeError});	
		} finally {
			releaseFactoryLock();
		}
	}
	
	public void noteClassReadIOException(
			String className,
			String fileName,
			IOException ex) {
		acquireFactoryLock();
		try {
			messages.READ_FAILURE.log(logger, new Object[] {className, fileName, ex});
		} finally {
			releaseFactoryLock();
		}
	}
	
	/**
	 * overrides parent.
	 */
	public void noteClassPathProblem(String entry, String str) {
		messages.CLASS_PATH_PROBLEM.log(logger, new String[] {entry, str});
	}
	
	public void noteClassPathEntryInexistent(String entry) {
		messages.CLASS_PATH_PROBLEM.log(logger, new String[] {entry, messages.NO_CLASS_PATH_ENTRY});
	}
	
	public void noteCodeException(BT_CodeException e) {
		messages.ERROR.log(logger, e.getMessage());
	}
	
	public void noteCodeIrregularity(BT_CodeException e) {
		messages.CODE_IRREGULARITY.log(logger, e.getMessage());
	}
	
	public void noteJSRsInlined(BT_Method method) {
		messages.INLINED_METHOD_JSRS.log(logger, method.useName());
	}
	
	private static String accessString(BT_Item item, short permission) {
		return item.accessString(permission, true);
	}
	
	public void noteAccessPermissionsChanged(BT_Item item, short oldPermission, BT_Item forAccessFrom) {
		String accessString = accessString(item, item.getAccessPermission());
		String oldAccessString = accessString(item, oldPermission);
		messages.CHANGED_ACCESS_PERMISSION.log(logger, 
				new String[] {
					item.useName(), 
					oldAccessString.trim(), 
					accessString.trim(), 
					forAccessFrom.useName()});
	}

	public void noteFinalAccessChanged(BT_Item item, BT_Item forAccessFrom) {
		messages.CHANGED_FINAL_ACCESS.log(logger, new Object[] {item.useName(), forAccessFrom.useName()});
	}

	public void noteAccessorUsed(AccessorMethod accessor, BT_Item forAccessFrom) {
		String reference = accessor.throughClass.getName() + '.' + accessor.target.qualifiedName();		
		messages.USED_ACCESSOR.log(logger, new Object[] {accessor.method.useName(), reference, forAccessFrom.useName()});
	}
	
	/**
	 * @see com.ibm.jikesbt.BT_Factory#isProjectClass(String className, Object file)
	 * 
	 * If BT_Repository.loadSystemClassesUsingReflection is true, 
	 * then non-project classes will be loaded using reflection if possible.  
	 * Non-project classes will not be manipulated or altered in any way by JIKESBT or Japt.
	 */
	public boolean isProjectClass(String className, Object file) {
		return true;
	}

}
