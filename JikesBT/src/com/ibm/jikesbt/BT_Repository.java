package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;
import com.ibm.jikesbt.BT_NoSuchMemberException.BT_NoSuchFieldException;
import com.ibm.jikesbt.BT_NoSuchMemberException.BT_NoSuchMethodException;

/**
 Information related to all represented classes, and the {@link BT_Factory} and classpath being used.
 All fields and methods in this class are static.

 <p> See the <a href=../jikesbt/doc-files/UserGuide.html#BT_REPOSITORY>User Guide<a>.

 * @author IBM
**/
public class BT_Repository extends BT_Base implements BT_FileConstants {
	
	public static final String JAVA_LANG = "java.lang";
	public static final String JAVA_LANG_CLASS = "java.lang.Class";
	public static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";
	public static final String JAVA_LANG_THREAD = "java.lang.Thread";
	public static final String JAVA_LANG_RUNNABLE = "java.lang.Runnable";
	public static final String JAVA_LANG_STRING = "java.lang.String";
	public static final String JAVA_LANG_STRING_ARRAY = JAVA_LANG_STRING + "[]";
	public static final String JAVA_LANG_OBJECT = "java.lang.Object";
	public static final String JAVA_LANG_OBJECT_ARRAY = JAVA_LANG_OBJECT + "[]";
	public static final String JAVA_LANG_CLONEABLE = "java.lang.Cloneable";
	public static final String JAVA_IO_SERIALIZABLE = "java.io.Serializable";
	public static final String JAVA_IO_EXTERNALIZABLE = "java.io.Externalizable";
	public static final String JAVA_LANG_RUNTIME_EXCEPTION = "java.lang.RuntimeException";
	public static final String JAVA_LANG_ERROR = "java.lang.Error";
	public static final String JAVA_LANG_ILLEGAL_THREAD_STATE_EXCEPTION = "java.lang.IllegalThreadStateException";
	public static final String JAVA_LANG_REFLECT_CONSTRUCTOR = "java.lang.reflect.Constructor";
	public static final String JAVA_IO_OBJECT_STREAM_FIELD_ARRAY = "java.io.ObjectStreamField[]";
	public static final String JAVA_LANG_STRING_BUILDER = "java.lang.StringBuilder";
	public static final String JAVA_LANG_STRING_BUFFER = "java.lang.StringBuffer";
	
	public static final String JAVA_LANG_INCOMPATIBLE_CLASS_CHANGE_ERROR = "java.lang.IncompatibleClassChangeError";
	public static final String JAVA_LANG_VERIFY_ERROR = "java.lang.VerifyError";
	public static final String JAVA_LANG_ILLEGAL_ACCESS_ERROR = "java.lang.IllegalAccessError";
	public static final String JAVA_LANG_NO_SUCH_METHOD_ERROR = "java.lang.NoSuchMethodError";
	public static final String JAVA_LANG_INSTANTIATION_ERROR = "java.lang.InstantiationError";
	public static final String JAVA_LANG_ABSTRACT_METHOD_ERROR = "java.lang.AbstractMethodError";
	public static final String JAVA_LANG_NO_SUCH_FIELD_ERROR = "java.lang.NoSuchFieldError";
	public static final String JAVA_LANG_NO_CLASS_DEF_FOUND_ERROR = "java.lang.NoClassDefFoundError";
	public static final String JAVA_LANG_CLASS_FORMAT_ERROR = "java.lang.ClassFormatError";
	public static final String JAVA_LANG_UNSUPPORTED_CLASS_VERSION_ERROR = "java.lang.UnsupportedClassVersionError";
	public static final String JAVA_LANG_CLASS_CIRCULARITY_ERROR = "java.lang.ClassCircularityError";
	public static final String JAVA_LANG_LINKAGE_ERROR = "java.lang.LinkageError";
	
	protected static String sourcePath;
	
	public boolean isFinalizationEnabled = true;
	
	/**
	 Factory for the repository.
	 
	 @see BT_Repository(BT_Factory)
	**/

	public BT_Factory factory;
	
	public interface LoadLocation {
		BT_ClassPathLocation getLocation();
	}
	
	private static ExecutorService executorService;
	private final int numLoadingThreads;
	private LoaderRunnable loaderRunnable;
	private CyclicBarrier dereferencingBarrier;
	private Object loadLock, dereferenceLock;
	private boolean barrierTriggered, anotherRound, dereferencingDone;
	private Error dereferenceError;
	private boolean doingQueuedDereferences_;

	/**
	 This constructor creates a repository, to manage the life-cycle of classes.
	 There may be many repositories existing the same time, although only one
	 factory.
	**/
	public BT_Repository(BT_Factory factory) {
		this.factory = factory;
		basicSignature = BT_MethodSignature.create(getVoid(), BT_ClassVector.emptyVector, this);
		if(BT_Factory.multiThreadedLoading) {
			loadLock = new Object();
			dereferenceLock = new Object();
			classesToBeDereferenced = Collections.synchronizedList(classesToBeDereferenced);
			//numLoadingThreads = Runtime.getRuntime().availableProcessors() + 1;
			numLoadingThreads = Runtime.getRuntime().availableProcessors() * 2;
			if(executorService == null) {
				synchronized(BT_Repository.class) {
					if(executorService == null) {
						executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                300L, TimeUnit.SECONDS,
                                new SynchronousQueue());
					}
				}
			}
			dereferencingBarrier = new CyclicBarrier(numLoadingThreads, new DereferenceBarrierAction());
			//numLoadingThreads = Runtime.getRuntime().availableProcessors() - 1;
			//numLoadingThreads = Runtime.getRuntime().availableProcessors() - 4;
			//numLoadingThreads = 3;
		} else {
			numLoadingThreads = 1;
		}
	}
	
	/**
	 * A cache of method signatures
	 */
	java.util.Hashtable signatures = new java.util.Hashtable();
	
	public final BT_MethodSignature basicSignature; 

	/**
	 Adds a class to the repository.
	 This may be called before the class is complete (e.g., during reading).
	
	**/
	//   <em>warning</em> -- It should be
	//     illegal to try to add the same class more than once.
	public void addClass(BT_Class c) {
		if (CHECK_USER && c == null)
			assertFailure(Messages.getString("JikesBT.Deprecated__Should_not_pass_null_to_BT_Repository.addClass_1"));
		if (!contains(c))
			classes.addElement(c);
		else if (CHECK_USER)
			expect(
				Messages.getString("JikesBT.Should_not_try_to_add_a_class_to_the_repository_more_than_once___3")
					+ c);
	}
	
	/**
	 Removes a class from the repository.  Does not call BT_Class.remove(), so the
	 relationships of the class to others are preserved when this method is called.
	 */
	public void removeClass(BT_Class c) {
		classes.removeElement(c);
	}
	
	/**
	 Removes a field from the repository.  
	 */
	public void removeField(BT_Field f) {}
	
	/**
	 Removes a method from the repository.  
	 */
	public void removeMethod(BT_Method m) {}
	

	/**
	 Same as (just calls) {@link BT_Class#forName}.
	 @param  name  (fully qualified, of course)
	 @return  Never null.
	   Will be a stub if there is an error loading it.
	 @deprecated use forName(String)
	**/
	public BT_Class findClass(String name) {
		return forName(name);
	}

	/**
	 * Returns the class in the repository matching the given name, or null if no such class exists
	 * @param name
	 * @return
	 */
	public BT_Class getClass(String name) {
		return classes.findClass(name);
	}
	
	public void renameClass(BT_Class clazz, String newName) {
		classes.renameClass(clazz, newName);
	}
	
	/**
	 Finds a given field in the repository.
	 The search includes the field's type.
	 An abbreviation for <code>findClass(cName).findField(fName, findClass(type))</code>.
	
	 @param  cName   The fully-qualified class name
	 @param  type    The fully-qualified type name
	 @return         Never null.
	 @exception BT_NoSuchFieldException when field not found.
	**/
	public BT_Field findField(String cName, String fName, String type) throws BT_NoSuchFieldException {
		return findClass(cName).findField(fName, findClass(type));
	}

	/**
	 Find a method given the external form of its argument types.
	
	 @param cName      The fully-qualified class name.
	 @param mName      The method name.
	 @param extArgs    The method arguments, e.g. "(java.lang.String, int, boolean)".
	 @return           Never null.
	 @exception BT_NoSuchMethodException  If the method is not found.
	**/
	public  BT_Method findMethodWithArgs(
		String cName,
		String mName,
		String extArgs) throws BT_NoSuchMethodException, BT_DescriptorException {
		return findClass(cName).findMethod(mName, extArgs);
	}

	/**
	 Finds a given method in the repository using an _external_ type description.
	 The search includes the method's return type.
	 To improve performance, use {@link BT_Class#findField} directly when it is available.
	
	 @param  cName   The fully-qualified class name.
	 @param  returnType  E.g. "int"
	 @param  mName   The method name.
	 @param  args    The parameter types.
	                 E.g. "(java.lang.String, int, boolean)".
	 @return         Never null.
	 @exception      BT_NoSuchMethodException when the method is not
	                 found in the specified class.
	**/
	public  BT_Method findMethod(
		String cName,
		String returnType,
		String mName,
		String args) throws BT_DescriptorException, BT_NoSuchMethodException {
		return findClass(cName).findMethod(
			mName,
			BT_MethodSignature.create(returnType, args, this));
	}

	/**
	 Returns whether the given class is recorded in the repository.
	**/
	public boolean contains(BT_Class c) {
		return contains(c.name);
	}
	
	/**
	 Returns whether there exists a class with the given name in the repository.
	**/
	public  boolean contains(String name) {
		return getClass(name) != null;
	}
	
	/**
	 * @param name
	 */
	public boolean canCreate(String name) {
		if(getClass(name) != null || name.endsWith("]") || findClassIfPrimitive(name) != null) {
			return false;
		}
		return true;
	}
	

	/**
	 Returns a count of how many classes have the <em>inProject</em> set to false.
	 See {@link <a href=../jikesbt/doc-files/glossary.html#system_class>system class</a>}.
	**/
	public  int numberOfSystemClasses() {
		return classes.size() - numberOfProjectClasses();
	}

	/**
	 Returns a count of how many classes have the <em>inProject</em> set to true.
	 See {@link <a href=../jikesbt/doc-files/glossary.html#project_class>project class</a>}.
	**/
	public  int numberOfProjectClasses() {
		int n = 0;
		for (int i = 0; i < classes.size(); ++i) {
			if (classes.elementAt(i).inProject())
				++n;
		}
		return n;
	}
	
	public void empty() {
		resetClassLoading();
	
		classes.removeAllElements();
		resources.removeAllElements();

		classesToBeDereferenced.clear(); // Just in case
		java_lang_Object_Cache_ = null;
		java_lang_String_Cache_ = null;
		java_lang_Class_Cache_ = null;
		java_lang_Thread_Cache_ = null;
		java_lang_Throwable_Cache_ = null;
		signatures.clear();
	}
	
	
	/**
	 Verifies all classes in the repository.
	 Invoked by the more extensive {@link com.ibm.apps.RepositoryChecker#checkRepository()}.
	**/
	//   Doesn't skip verifying static initializers of interfaces.
	public void verify() 
		throws BT_ClassFileException, BT_CodeException {
				
		for (int i = 0; i < classes.size(); ++i) {
			BT_Class c = classes.elementAt(i);
			if (!c.inProject())
				continue;
			for (int n = c.methods.size() - 1; n >= 0; n--) {
				BT_Method m = c.methods.elementAt(n);
				BT_CodeAttribute code = m.getCode();
				if (!m.isAbstract()
					&& !m.isNative()
					&& !m.cls.isStub()
					&& code == null)
					throw new BT_ClassFileException(
						Messages.getString("JikesBT.Method_{0}_is_not_abstract_nor_native_nor_in_a_stub,_yet_has_no_code_body_6", m));
				if (code != null) {
					code.verify();
					code.verifyRelationships(BT_Factory.strictVerification);
				}
			}
			if (c.isClass)
				for (int n = 0; n < c.parents_.size(); ++n)
					if (c.parents_.elementAt(n).isInterface)
						// Not the superclass
						verifyImplements(c, c.parents_.elementAt(n));
		}
	}

	/**
	 Verify interface implementations.
	**/
	protected static void verifyImplements(BT_Class c, BT_Class itf)
		throws BT_ClassFileException {

		for (int n = 0; n < itf.methods.size(); ++n) {
			BT_Method m = itf.methods.elementAt(n);
			if (m.isStaticInitializer())
				continue;
			c.findInheritedMethod(m.name, m.getSignature(), false);

			if (c == null) {
				throw new BT_ClassFileException(
					Messages.getString("JikesBT.Class_{0}_7", c.name)
						+ endl()
						+ Messages.getString("JikesBT.____implements_interface_{0}_8", itf.name)
						+ endl()
						+ Messages.getString("JikesBT.____but_does_not_implement_method_{0}_9", m));
			}
		}
		for (int k = 0; k < itf.parents_.size(); ++k)
			if (itf.parents_.elementAt(k).isInterface)
				verifyImplements(c, itf.parents_.elementAt(k));
	}

	/**
	 Prints all classes in the repository in the specified PrintStream.
	
	 <p> Hint:  You may want to do BT_Repository.classes.sort() before calling this.
	
	 @param  printFlags  The sum of some of:
	   {@link BT_Misc#PRINT_NO_CODE},
	   {@link BT_Misc#PRINT_NO_METHOD},
	   {@link BT_Misc#PRINT_SYSTEM_CLASSES}, and
	   {@link BT_Misc#PRINT_ZERO_OFFSETS}.
	   Other bits are ignored.
	**/
	public  void print(PrintStream ps, int printFlags) {
		boolean printEvenSystemClasses =
			(printFlags & BT_Misc.PRINT_SYSTEM_CLASSES) != 0;
		for (int i = 0; i < classes.size(); ++i) {
			BT_Class c = classes.elementAt(i);
			if (printEvenSystemClasses || c.inProject())
				c.print(ps, printFlags);
		}
	}

	/**
	 An abbreviation of {@link BT_Repository#print(PrintStream,int) print(ps,0)}.
	**/
	public void print(PrintStream ps) throws BT_CodeException {
		print(ps, 0);
	}

	/**
	 The BT_ClassVector containing the classes.
	**/
	public final BT_ClassTable classes = new BT_ClassTable();

	/**
	 The Vector containing the resources to be added in case the repository is written
	 to an output jar
	 @see save
	**/
	public final BT_JarResourceVector resources = new BT_JarResourceVector();

	/**
	 * Add a resource to this repository so it will be written in a jar when save is called
	 * @see save
	 */
	public void addResource(String name, byte contents[]) {
		if (contents == null) {
			throw new NullPointerException(Messages.getString("JikesBT.Resource_{0}_contents___null_26", name));
		}
		resources.addElement(new BT_JarResource(name, contents));
	}
	
	/**
	 Just returns {@link BT_Repository#classes}.
	**/
	// Allows not remembering when to use methods and when to use fields, and may provide improved upward compatibility.
	public final BT_ClassVector getClasses() {
		return classes;
	}

	/**
	 * trims all vectors related to classes in this repository that grow as new classes are loaded.  
	 * Calling this method when all loading is complete will release unused memory.
	 */
	public void trimToSize() {
		for(int i=0; i<classes.size(); i++) {
			classes.elementAt(i).trimToSize();
		}
		classes.trimToSize();
		resources.trimToSize();
		//signatures hashtable?
	}
	
	/**
	 Creates a class that represents a primitive type or "void", and adds it to the repository.
	 Note:  This should only be called by {@link BT_Repository#getBoolean} and similar methods.
	 See the similar {@link BT_Repository#createArray}.
	 * @param name the class name
	 * @param stackMapType the stack map type for the StackMapTable attribute
	 * @param convertToStackType converts this class to the given class in the StackMapTable attribute
	 * @return
	 */
	private BT_Class createPrimitive(String name, int stackMapType, BT_Class convertToStackType) {
		//in some cases we do not have the class table lock, when we get here from BT_Repository.getXXX()
		//in other cases we do, like when we get here from BT_Repository.forName
		//so we must get the lock here
		//since it is a rentrant lock, that's fine
		acquireClassTableLock();
		try {
			if(BT_Factory.multiThreadedLoading) {
				BT_Class c = classes.findClass(name);
				if(c != null) {
					return c;
				}
			}	
			BT_Class c = createClass(name);
			factory.noteClassLoaded(c, null);
			c.initAsPrimitive(name, this, stackMapType, convertToStackType);
			return c;
		} finally {
			releaseClassTableLock();
		}
	}
	
	private BT_Class createPrimitive(String name, BT_Class convertToStackType) {
		return createPrimitive(name, BT_StackType.ITEM_UNDEFINED, convertToStackType);
	}
	
	private BT_Class createPrimitive(String name, int stackMapType) {
		return createPrimitive(name, stackMapType, null);
	}
	
	/**
	 *  Creates a class that represents an array class and adds it to the repository.
	 * @param cnm
	 * @param link whether or not to load the element class immediately
	 * @return
	 */
	protected BT_Class createArray(String cnm, boolean link) throws BT_ClassFileException {
		BT_Class c = createClass(cnm);
		c.initAsArray(cnm, link);
		factory.noteClassLoaded(c, null);
		c.arrayType.addReferencingArrayClass(c);
		return c;
	}

	/**
	 The similarly-named primitive.
	 Note:  These should be accessed only via "getter" methods such as {@link BT_Repository#getBoolean()}.
	**/
	protected  BT_Class t_boolean;
	protected  BT_Class t_byte;
	protected  BT_Class t_char;
	protected  BT_Class t_double;
	protected  BT_Class t_float;
	protected  BT_Class t_int;
	protected  BT_Class t_long;
	protected  BT_Class t_short;
	protected  BT_Class t_void;

	public BT_Class getBoolean() {
		if (t_boolean == null) {
			t_boolean = createPrimitive("boolean", getInt());
		}
		return t_boolean;
	}
	public BT_Class getByte() {
		if (t_byte == null) {
			t_byte = createPrimitive("byte", getInt());
		}
		return t_byte;
	}
	public BT_Class getChar() {
		if (t_char == null) {
			t_char = createPrimitive("char", getInt());
		}
		return t_char;
	}
	public BT_Class getDouble() {
		if (t_double == null) {
			t_double = createPrimitive("double", BT_StackType.ITEM_DOUBLE);
		}
		return t_double;
	}
	public BT_Class getFloat() {
		if (t_float == null) {
			t_float = createPrimitive("float", BT_StackType.ITEM_FLOAT);
		}
		return t_float;
	}
	public BT_Class getInt() {
		if (t_int == null) {
			t_int = createPrimitive("int", BT_StackType.ITEM_INTEGER);
		}
		return t_int;
	}
	public BT_Class getLong() {
		if (t_long == null) {
			t_long = createPrimitive("long", BT_StackType.ITEM_LONG);
		}
		return t_long;
	}
	public  BT_Class getShort() {
		if (t_short == null) {
			t_short = createPrimitive("short", getInt());
		}
		return t_short;
	}
	public  BT_Class getVoid() {
		if (t_void == null)
			t_void = createPrimitive("void", BT_StackType.ITEM_UNDEFINED);
		return t_void;
	}

	/**
	 Returns null if "name" is not a primitive.
	 @param  name  E.g., "boolean".
	 @return  E.g., the result of "getBoolean()".
	**/
	BT_Class findClassIfPrimitive(String name) {
		if (name.equals("boolean"))
			return getBoolean();
		if (name.equals("byte"))
			return getByte();
		if (name.equals("char"))
			return getChar();
		if (name.equals("double"))
			return getDouble();
		if (name.equals("float"))
			return getFloat();
		if (name.equals("int"))
			return getInt();
		if (name.equals("long"))
			return getLong();
		if (name.equals("short"))
			return getShort();
		if (name.equals("void"))
			return getVoid();
		return null;
	}

	/**
	 Returns true if "name" is a primitive or "void".
	**/
	public static boolean isPrimitiveName(String name) {
		return BT_Class.isPrimitiveName(name);
	}

	boolean isLoading;
	protected List classesToBeDereferenced = new LinkedList();
	
	/**
	 Loads a class, all its parents, and all the types mentioned in the field and
	 method declarations.
	 Adds the class to the {@link BT_Repository#.
	
	 @param  inputstream from which the class is read.
	 @param  file  A java.io.File or a java.util.zip.ZipFile.
	 @param  className  Null or the class name in class file-name format.
	   Must end with ".class".
	   E.g., "p/c.class".
	   If it is non-null, this parameter is simply used to ensure the
	   internal name of the class it what is expected.
	 @param  canonFid  The canonical name of the file being read.
	   Does not include any .zip-file member name.
	 @return  Never null.
	**/
	// Stub classes can be replaced by classes that are read.
	// Invalid classes result in a class in the repository marked as such.
	private BT_Class readClass(
		InputStream is,
		Object file,
		BT_ClassPathEntry.BT_ClassPathLocation entry,
		String className,
		String canonFid,
		BT_Class gotC)
		//throws BT_ClassFileException, BT_DuplicateClassException, IOException {
		throws BT_ClassFileException, BT_DuplicateClassException, IOException {

		boolean isTopLevelLoad = !isLoading;
		if(isTopLevelLoad) {
			isLoading = true;
		}
		
		try {
			// Need to have a BufferedInputStream to be able to mark and reset.
			DataInputStream dis =
				new DataInputStream(new BufferedInputStream(is));
			
			BT_ClassInfoUntilName classInfo = null;

			/*
			 * If the className isn't known (when reading from an arbitrary file or stream), then 
			 * the classInfo must be read before a class can be found or created in the repository.
			 * If the className is known, postpone reading the classInfo until after finding or creating
			 * the class so that we will have a class in the repository that will throw a ClassFormatError
			 * in case the magic is bad.
			 */
			if (className == null) {
				classInfo = new BT_ClassInfoUntilName();
				classInfo.readUntilName(dis, file, this);
				className = classInfo.className;
			}
			
			// Check if a class with the classname already exists.
			if(gotC == null) {
				gotC = classes.findClass(className);
			}
			if(gotC == null) {
				gotC = createStub(className);
			} else if (!gotC.isStub()) {
				//this can happen when className supplied is null and we were provided with a class path location to load
				throw new BT_DuplicateClassException(
					Messages.getString("JikesBT.Tried_to_read_an_already_existing_class___78")+gotC.getName(),
					gotC);
			}
			

			gotC.acquireClassLock();
			
			releaseTableLock(gotC);

			boolean didRead = false;
			try {
				gotC.setStub(false); // Not a stub after this point
				gotC.setInProject(factory.isProjectClass(className, file));
				gotC.loadedFrom = canonFid;
				gotC.loadedFromEntry = entry;
				
				if (classInfo == null) {
					classInfo = new BT_ClassInfoUntilName();
					classInfo.readUntilName(dis, file, this);
					
					if (!classInfo.className.equals(className)) {
						// Mark that this case requires a NoClassDefFound at run time
						gotC.setThrowsNoClassDefFoundError();
						BT_ClassFileException exception = new BT_ClassFileException(
								Messages.getString("JikesBT.The_class_file_of_{0}_has_internal_name_{1}_82", 
										new Object[] {className, classInfo.className}));
						exception.setEquivalentRuntimeError(JAVA_LANG_NO_CLASS_DEF_FOUND_ERROR);
						throw exception;
					}
				}
				
				// check the class version data
				if (!isValidClassVersion(classInfo.majorVersion, classInfo.minorVersion)) {
					gotC.setThrowsUnsupportedClassVersionError();
					BT_ClassFileException exception = new BT_ClassFileException(
						Messages.getString("JikesBT.The_class_file_of_{0}_has_wrong_internal_version_number__{1}.{2}_79",
							new Object[] {className, Integer.toString(classInfo.majorVersion), Integer.toString(classInfo.minorVersion)}));
					exception.setEquivalentRuntimeError(JAVA_LANG_UNSUPPORTED_CLASS_VERSION_ERROR);
					throw exception;
				}

				gotC.readAfterName(dis, classInfo, file);
				didRead = true;
				// Read remaining class
			} finally {
				factory.noteClassLoaded(gotC, canonFid);
				if(!didRead && !gotC.throwsAnyError()) {
					gotC.setThrowsClassFormatError();
				}
				gotC.releaseClassLock();
				classesToBeDereferenced.add(gotC);
				if (isTopLevelLoad) {
					doAsyncQueuedDereferences();
				}
			}
			return gotC;
		} finally {
			if(gotC != null) {
				//TODO I think this is redundant, but is it?  Thre is the one case where we throw.  
				releaseTableLockIfNotReleased(gotC);//make sure we release the table lock before dereferencing
			}
			is.close();
			if (isTopLevelLoad) {// This is not a nested load
				doSyncQueuedDereferences();
				isLoading = false;
			}
			
		}
	}

	/**
	 * Method validClassVersion returns true if class file version is supported.
	 * 
	 * Current implementation returns <code>True</code>. Override this method to
	 * change the default behaviour.
	 * @param major
	 * @param minor
	 * @return <code>True</code>
	 */
	protected boolean isValidClassVersion(int major, int minor) {
		return true;
	}

	void createStub(String name, BT_Class c) {
		if (CHECK_JIKESBT && name.endsWith("]"))
			assertFailure(Messages.getString("JikesBT.createStub_does_not_support_arrays___86") + name);
		if (CHECK_JIKESBT && isPrimitiveName(name))
			assertFailure(name);
		c.initClass(name, this);
		c.setInProjectFalse();
	}
	

	/**
	 Creates a {@link <a href=../jikesbt/doc-files/Glossary.html#stub_class>stub class or
	 interface</a>}.
	 This is typically called only by JikesBT when a class cannot be loaded.
	 The stub is non-differentiated, but may become a class or an interface
	 later as usage warrants.
	 The stub is added to the {@link BT_Repository}.
	
	 @param  name  The name of the class in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	   E.g., "package.Class$Nested[][]" or "void" or "boolean".
	 @return  Never null.
	**/
	public BT_Class createStub(String name) {
		BT_Class c = createClass(name);
		createStub(name, c);
		return c;
	}

	public BT_Class createStub(String name, boolean notLoaded) {
		BT_Class stub = createStub(name);
		stub.notLoaded = notLoaded;
		return stub;
	}
	
	protected void doAsyncQueuedDereferences() {
		if(BT_Factory.multiThreadedLoading) {
			if(loaderRunnable == null) {
				createLoaderThreads();
			}
			synchronized(dereferenceLock) {
				doingQueuedDereferences_ = true;
				dereferenceLock.notifyAll();
			}
		}
	}

	class LoaderRunnable implements Runnable {
		boolean done;
		
		public void run() {
			while(true) {
				synchronized(dereferenceLock) {
					while(!BT_Repository.this.doingQueuedDereferences_ && !done) {
						try {
							dereferenceLock.wait();
						} catch(InterruptedException e) {}
					}
				}
				if(done) {
					break;
				}
				dereferenceAll();
			}
		}
	}
	
	private void createLoaderThreads() {
		loaderRunnable = new LoaderRunnable();
		for(int i=0; i<numLoadingThreads; i++) {
			executorService.submit(loaderRunnable);
		}
	}
	
	private void waitForAsyncDereferences() {
		if(BT_Factory.multiThreadedLoading) {
			try {
				synchronized(loadLock) {
					while(doingQueuedDereferences_) {
						loadLock.wait();
					}
				}
			} catch(InterruptedException e) {}
		}
	}
	
	/**
	 Handles stuff to be done when this batch of classes has been loaded,
	 including their ancestors and the types mentioned in the signatures of their
	 methods and fields.
	
	 <p> Whether inter-method relationships are built and maintained is
	 affected by {@link BT_Factory#buildMethodRelationships}.
	**/
	private void doSyncQueuedDereferences() {
		if(BT_Factory.multiThreadedLoading) {
			waitForAsyncDereferences();
			if(dereferenceError != null) {
				Error e = dereferenceError;
				dereferenceError = null;
				throw e;
			}
		} else {
			if (doingQueuedDereferences_) {
				return;
			}
			doingQueuedDereferences_ = true;
			dereferenceAll();
		}
	}

	class DereferenceBarrierAction implements Runnable {
		public void run() {
			dereferencingDone |= !anotherRound;
			anotherRound = barrierTriggered = false;
			if(dereferencingDone) {
				synchronized(loadLock) {
					doingQueuedDereferences_ = false;
					loadLock.notify();
				}
			}
		}
	}
	
	private void dereferenceAll() {
		try {
			dereferencingDone = false;
			while(true) {
				try {
					BT_Class clazz = (BT_Class) classesToBeDereferenced.remove(0);
					anotherRound = true;
					clazz.handleDereference();
					if (factory.buildMethodRelationships) {
						BT_MethodRelationships.linkInlawsCausedByThisClass(clazz);
					}
				} catch (IndexOutOfBoundsException e) {
					if(!BT_Factory.multiThreadedLoading) {
						doingQueuedDereferences_ = false;
						break;
					}
					barrierTriggered = true;
				} catch (Error e) {
					if(!BT_Factory.multiThreadedLoading) {
						doingQueuedDereferences_ = false;
						throw e;
					}
					if(dereferenceError == null) {
						dereferenceError = e;
					}
					barrierTriggered = dereferencingDone = true;
				}
				if(barrierTriggered) {
					dereferencingBarrier.await();
					if(dereferencingDone) {
						break;
					}
				}
			}
		} catch (InterruptedException e) {
			//TODO call factory
		} catch (BrokenBarrierException e) {
			//TODO call factory
		}
	}

	/**
	 @param  cnm  The class name in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	   E.g., "package.Class$Nested[][]" or "void" or "boolean".
	 @return  Null if failed.
	**/
	protected BT_Class loadFromClassPath(String cnm, BT_ClassPathEntry.BT_ClassPathLocation location, BT_Class stub) {
		BT_Class gotC = null;
		BT_ClassPathEntry entry = location.getClassPathEntry();
		String fnm = entry.getEntryCanonicalName();
		InputStream stream = null;
		try {
			stream = location.getInputStream();
			gotC = readClass(stream, location.getFileObject(), location, cnm, fnm, stub);
			gotC.lastModificationTime = location.getLastModifiedTime();
		} catch(IOException e) {
			factory.noteClassReadIOException(cnm, fnm, e);
		} catch (BT_ClassFileException e) {
			String error = e.getEquivalentRuntimeError();
			gotC = classes.findClass(cnm);
			factory.noteClassLoadFailure(this, entry, gotC, cnm, fnm, e, 
					error != null ? error : "java.lang.ClassFormatError");
		} catch (RuntimeException e) {
			gotC = classes.findClass(cnm);
			factory.noteClassLoadFailure(this, entry, gotC, cnm, fnm, e, JAVA_LANG_CLASS_FORMAT_ERROR);
			
		} catch (BT_DuplicateClassException e) { 
			gotC = classes.findClass(cnm);
			factory.noteClassLoadFailure(this, entry, gotC, cnm, fnm, e, JAVA_LANG_LINKAGE_ERROR);
		} finally {
			if(stream != null) {
				try {
					stream.close();
				} catch(IOException e) {
					factory.noteFileCloseIOException(location.getName(), e);
				}
			}
		}
		return gotC;
	}

	/**
	 * Applications which do not wish to load superinterfaces during class loading and
	 * load stubs instead may wish to override this method.  Many VMs do not load super interfaces
	 * during class loading, but they need to be loaded before any field or method resolutions and
	 * so the default behaviour of JikesBT is a call to forName() to load them right away.
	 * Applications which wish to delay loading should replace with a call to linkTo().
	 * Because they are needed for proper method and field resolution, all interfaces should
	 * be loaded before instructions are dereferenced.
	 * 
	 * If this method does not load interfaces, then parent methods, kid methods, and inlaw methods
	 * might not be fully complete.
	 * 
	 * @param cnm
	 * @return
	 */
	//Do not remove this method, it is overridden by javah
	protected BT_Class linkToSuperInterface(String cnm, LoadLocation referencedFrom) {
		return forName(cnm, referencedFrom);
	}
	
	/**
	 * Link to a class.
	 * Stubs are created if factory.preloadRelatedClasses is false and the class must be
	 * loaded from the classpath.
	 * @param cnm
	 * @return
	 */
	public BT_Class linkTo(String cnm) {	
		return linkTo(cnm, null);
	}
	
	BT_Class linkTo(String cnm, LoadLocation referencedFrom) {	
		if(factory.preloadRelatedClasses) {
			return forName(cnm, referencedFrom);
		}
		return linkToStub(cnm, referencedFrom);
	}
	
	/**
	 * @param cnm
	 * @return
	 */
	public BT_Class linkToStub(String cnm) {
		return linkToStub(cnm, null);
	}
	
	BT_Class linkToStub(String cnm, LoadLocation referencedFrom) {
		boolean loadingReleasedClassTableLock = false;
		acquireClassTableLock();
		try {
			BT_Class c = classes.findClass(cnm);
			if (c == null) {
				loadingReleasedClassTableLock = true;
				try {
					c = loadInternalClass(cnm, true); // Is an array or primitive
					if (c == null) {
						c = createStub(cnm); //load it from the class path later
						c.notLoaded = true;
						c.referencedFrom = referencedFrom;
					}
				} finally {
					releaseTableLockIfNotReleased(c);
				}
			}
			return c;
		} finally {
			if(!loadingReleasedClassTableLock) {
				releaseClassTableLock();
			}
		}
	}

	//ReentrantLock classTableLock = new DebugLock();
	ReentrantLock classTableLock = new ReentrantLock();
	
	protected static void acquireLock(ReentrantLock lock) {
		BT_Class.acquireLock(lock);
	}
	
	protected static void releaseLock(ReentrantLock lock) {
		BT_Class.releaseLock(lock);
	}
	
	protected void acquireClassTableLock() {
		acquireLock(classTableLock);
	}
	
	protected void releaseClassTableLock() {
		releaseLock(classTableLock);
	}
	
	/**
	 * Unlocks the table lock used to load the given class.
	 * This can be called more than once, and it will only decrement the lock count the first time.
	 * 
	 * This can only be called only after this class has been added to the class table.
	 * 
	 * If this class is not yet fully loaded, do not call this method before gaining ownership of the loading lock for this class,
	 * so that other threads which obtain this class from the table will not access parts not yet loaded.
	 */
	protected void releaseTableLockIfNotReleased(BT_Class clazz) {
		if(!clazz.tableLockReleased) {
			releaseTableLock(clazz);
		}
	}
	
	protected void releaseTableLock(BT_Class clazz) {
		if(BT_Factory.multiThreadedLoading) {
			clazz.tableLockReleased = true;
			classTableLock.unlock();
		}
	}
	
//	class DebugLock extends ReentrantLock {
//		String name;
//		private StackTraceElement[] trace;
//		Thread lastOwner;
//		private StackTraceElement[] tracePrev;
//		
//		public void lock() {
//			super.lock();
//			if(lastOwner == Thread.currentThread()) {
//				tracePrev = trace;
//			} else tracePrev = null;
//			trace = new Throwable().getStackTrace();
//			lastOwner = Thread.currentThread();
//		}
//	}
	
	/**
	 Finds, loads, or creates the named class.
	 If the class is already in the repository, that is found.
	 If such a class is a stub that has not been loaded from the classpath, then it 
	 is loaded.
	 Otherwise, if the class is a primitive or an array, a representative is created.
	 Otherwise the class is loaded from the current classpath.
	 If loading fails, a {@link <a href=../jikesbt/doc-files/Glossary.html#stub_class>stub class</a>}
	 is created.
	
	 <p> Very similar to <tt>java.lang.Class.forName(String)</tt>.
	 Same as (called by) {@link BT_Repository#findClass}.
	
	 @param  cnm  The fully-qualified name of the class in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	   E.g., "package.Class$Nested[][]" or "void" or "boolean".
	 @return Never null.
	**/
	public BT_Class forName(String cnm) {
		return forName(cnm, null);
	}
	
	BT_Class forName(String cnm, LoadLocation referencedFrom) {
		//Note: this and loadClass are really the two loading entry points
		boolean loadingReleasedClassTableLock = false;
		acquireClassTableLock();
		try {
			BT_Class c = classes.findClass(cnm);
			if (c != null) {
				if(c.isStub() && c.notLoaded) {
					loadingReleasedClassTableLock = true;
					try {
						c.tableLockReleased = false;
						LoadLocation referenced = referencedFrom == null ? c.referencedFrom : referencedFrom;
						c.notLoaded = false;
						loadFromClassPath(cnm, c, referenced);
					} finally {
						releaseTableLockIfNotReleased(c);
					}
				} else {
					return c;
				}
			} else {
				loadingReleasedClassTableLock = true;
				try {
					if ((c = loadInternalClass(cnm, false)) == null) { // Is an array or primitive
						c = loadFromClassPath(cnm, null, referencedFrom);
					}
				} finally {
					releaseTableLockIfNotReleased(c);
				}
			}
			return c;
		} finally {
			if(!loadingReleasedClassTableLock) {
				releaseClassTableLock();
			}
		}
	}
	
	protected BT_Class loadInternalClass(String cnm, boolean link) {
		if (cnm.endsWith("]")) { // Is an array
			try {
				return createArray(cnm, link);
			} catch (BT_ClassFileException e) {
				//one reason for reaching here is creating an array with too many dimensions
				String error = e.getEquivalentRuntimeError();
				BT_Class result = classes.findClass(cnm);
				factory.noteClassLoadFailure(this, null, result, cnm, "", e, 
						error != null ? error : JAVA_LANG_LINKAGE_ERROR);
				return result;
			}
		} else {
			return findClassIfPrimitive(cnm);
		}
	}

	private BT_Class loadFromClassPath(String cnm, BT_Class stub, LoadLocation referencedFrom) {
		BT_Class c;
		BT_ClassPathLocation location = findInClassPath(cnm);
		if (location == null) {// Didn't find the file
			c = factory.noteClassNotFound(cnm, this, stub, referencedFrom);
		} else {
			c = location.loadClass(cnm, this, stub);
			
			if (c == null) {
				c = classes.findClass(cnm);
				if (c == null) {
					c = createStub(cnm);
				}
			}
		}
		return c;
	}
	
	/**
	 * Returns the class object for a given class name.
	 * If the class was already loaded, return the class object.
	 * Otherwise load the class with the given name from the given classpath location.
	 * @param name the class name (with dots as delimiters)
	 * @param location a location storing the class file representation
	 * @return the class object (may be a stub if loading failed)
	 */
	public BT_Class loadClass(String name, BT_ClassPathLocation location) {
		boolean loadingReleasedClassTableLock = false;
		acquireClassTableLock();
		try {
			//Note: this and forName are really the two loading entry points
			BT_Class clazz = classes.findClass(name);
			if(clazz == null || (clazz.isStub() && clazz.notLoaded)) {
				loadingReleasedClassTableLock = true;
				if(clazz != null) {
					clazz.tableLockReleased = false;
				}
				try {
					clazz = location.loadClass(name, this, clazz);
				} finally {
					releaseTableLockIfNotReleased(clazz);
				}
			}
			return clazz;
		} finally {
			if(!loadingReleasedClassTableLock) {
				releaseClassTableLock();
			}
		}
	}
	
	/**
	 A cache of java.lang.Object.
	**/
	protected BT_Class java_lang_Object_Cache_;

	//TODO add a link parameter to the findJavaLangWhatever methods
	
	/**
	 Returns the BT_Class that represents java.lang.Object.
	**/
	public BT_Class findJavaLangObject() {
		if (java_lang_Object_Cache_ == null)
			java_lang_Object_Cache_ =
				forName(JAVA_LANG_OBJECT);
		return java_lang_Object_Cache_;
	}
	
	/**
	 A cache of java.lang.String.
	**/
	protected BT_Class java_lang_String_Cache_;

	/**
	 Returns the BT_Class that represents java.lang.String.
	**/
	public BT_Class findJavaLangString() {
		if (java_lang_String_Cache_ == null)
			java_lang_String_Cache_ =
			forName(JAVA_LANG_STRING);
		return java_lang_String_Cache_;
	}
	
	/**
	 A cache of java.lang.Thread.
	**/
	protected BT_Class java_lang_Thread_Cache_;
	
	/**
	 Returns the BT_Class that represents java.lang.Throwable.
	**/
	public BT_Class findJavaLangThread() {
		if (java_lang_Thread_Cache_ == null)
			java_lang_Thread_Cache_ =
			forName(JAVA_LANG_THREAD);
		return java_lang_Thread_Cache_;
	}
	
	/**
	 A cache of java.lang.Throwable.
	**/
	protected BT_Class java_lang_Throwable_Cache_;

	
	/**
	 Returns the BT_Class that represents java.lang.Throwable.
	**/
	public BT_Class findJavaLangThrowable() {
		if (java_lang_Throwable_Cache_ == null)
			java_lang_Throwable_Cache_ =
			forName(JAVA_LANG_THROWABLE);
		return java_lang_Throwable_Cache_;
	}
	
	/**
	 A cache of java.lang.Class.
	**/
	protected BT_Class java_lang_Class_Cache_;

	/**
	 Returns the BT_Class that represents java.lang.Class.
	**/
	public BT_Class findJavaLangClass() {
		if (java_lang_Class_Cache_ == null)
			java_lang_Class_Cache_ =
			forName(JAVA_LANG_CLASS);
		return java_lang_Class_Cache_;
	}

	public static String internalToStandardClassName(String className) {
		return BT_ConstantPool.internalToStandardClassName(className);
	}

	/**
	 Returns where source files can be found. This is set by users of JikesBT
	 and depends completely on their operating environment. 
	 @return sourcePath, can be a directory or a jar file
	**/
	public static final String getSourcePath() {
		return sourcePath;
	}

	/**
	 Sets where source files can be found. This is set by users of JikesBT
	 and depends completely on their operating environment. 
	 @param path, can be a directory or a jar file
	**/
	public static final void setSourcePath(String path) {
		sourcePath = path;
	}

	/**
	 @param  cnm  The class name.
	 @return  Null if failed.
	**/
	public BT_ClassPathEntry.BT_ClassPathLocation findInClassPath(String cnm) {
		Vector cpv = getClassPathVector();
		for (int n = 0; n < cpv.size(); n++) { // Per BT_ClassPathEntry
			BT_ClassPathEntry cpe = (BT_ClassPathEntry) cpv.elementAt(n);
			BT_ClassPathEntry.BT_ClassPathLocation location = cpe.findClass(cnm);
			if (location != null) {
				return location;
			}
		}
		return null;
	}

	private Vector classPath_;

	/**
	 * prepends the class path listed in the system property java.class.path to the JikesBT classpath
	 * @throws IOException
	 */
	public void prependPropertiesClassPath() {
		prependClassPath(System.getProperty("java.class.path", "."));
	}
	
	protected final Vector getClassPathVector() {
		if (classPath_ == null) {
			classPath_ = new Vector();
		}
		return classPath_;
	}

	/**
	 Prepends entries (directories and/or .jar or .zip file names) to JikesBT's
	 current classpath setting.
	 @throws IOException if the string does not denote an accessible directory or jar file
	**/
	public void prependClassPath(String path) {
		appendOrPrependClassPath(path, false);
	}

	/**
	 Appends entries (directories and/or .jar or .zip file names) to JikesBT's
	 current classpath setting.
	 @throws IOException if the string does not denote an accessible directory or jar file
	**/
	public void appendClassPath(String path) {
		appendOrPrependClassPath(path, true);
	}

	public static StringVector pathTokenizer(String path, boolean reverse) {
		String c =
			path.replace(DOSFILE_SEPARATOR_BACKSLASH, ZIPFILE_SEPARATOR_SLASH);
		// To standard zip-file-compatible format
		StringTokenizer tokenizer =
			new StringTokenizer(c, File.pathSeparator, false);
		StringVector strings = new StringVector();
		for (; tokenizer.hasMoreTokens(); ) {
			String tok = tokenizer.nextToken();
			if (tok.length() > 0) {
				if(reverse) {
					strings.insertElementAt(tok, 0);
				}
				else {
					strings.addElement(tok);
				}
			}
		}
		return strings;
	}
	
	/**
	 * @throws IOException if the string does not denote an accessible directory or jar file
	 * @param path
	 * @param append
	 */
	protected void appendOrPrependClassPath(String path, boolean append) {
		Vector cpv = getClassPathVector();
		StringVector paths = pathTokenizer(path, !append);
		int j = 0;
		for(int i=0; i<paths.size(); i++) {
			String tok = paths.elementAt(i);
			try {
				BT_ClassPathEntry cpe = createClassPathEntry(tok);
				// (It handles messages if there's an error)
				if (cpe != null) {
					if (append)
						cpv.addElement(cpe);
					else
						cpv.insertElementAt(cpe, j);
					++j;
				}
			} catch(IOException e) {
				factory.noteClassPathProblem(tok, e.getMessage());
			}
		}
	}

	/**
	 Sets the path JikesBT searches to find classes to null.
	**/
	public synchronized void resetClassLoading() {
		if(BT_Factory.multiThreadedLoading) {
			if(loaderRunnable != null) {
				loaderRunnable.done = true;
				
				//this line not entirely necessary but useful if we intend to start altering the repository,
				//it ensures the reference threads are not doing anything anymore
				waitForAsyncDereferences();
				
				//make sure the dereference threads terminate if not terminated already
				synchronized(dereferenceLock) {
					dereferenceLock.notifyAll();
				}
				loaderRunnable = null;
			}
		}
		Vector cp = getClassPathVector();
		while(!cp.isEmpty()) {
			BT_ClassPathEntry cpe = (BT_ClassPathEntry) cp.remove(0); 
			try {
				cpe.close();
			} catch(IOException e) {
				factory.noteFileCloseIOException(cpe.getEntryCanonicalName(), e);
			}
		}
	}

	/**
	 Adds one entry (directory or archive file name) to the end of the
	 current classpath setting.
	 @throws IOException if the string does not denote an accessible directory or jar file
	**/
	//   Will call {@link BT_Factory#noteClassPathProblem} if an
	//     entry is not a directory nor a zip file in case you want to warn the user.
	public void addClassPath(String path) {
		try {
			BT_ClassPathEntry cpe = createClassPathEntry(path);
			if (cpe != null)
				getClassPathVector().addElement(cpe);
		} catch(IOException e) {
			factory.noteClassPathProblem(path, e.getMessage());
		}
	}
	
	/**
	 Creates a new BT_ClassPathEntry.
	 For efficiency, the meaning of this entry is bound when it is created.
	 If the user creates or destroys the named directory or zip file later, the
	 change won't be noticed (at best).
	 <p>
	 Override this method if you wish the JIKESBT classpath to be composed
	 of subclasses of BT_ClassPathEntry.
	
	 @param  entryString  A directory or .jar/.zip file name.
	 @return  Null or the new BT_ClassPathEntry.
	**/
	protected BT_ClassPathEntry createClassPathEntry(String entryString) throws IOException {
		File file = new File(entryString);
		if(!file.exists()) {
			throw new IOException(
				Messages.getString(
						"JikesBT.Classpath_entry_{0}_is_not_an_existing_directory_nor_.jar/.zip_file_102",
						entryString));
		}
		// Hopefully, a zip file or a directory
		return new BT_ClassPathEntry(file);
	}

	/**
	 Returns the classpath currently being used by JikesBT in external form.
	**/
	public String classPathToString() {
		String sep = File.pathSeparator;
		StringBuffer buf = new StringBuffer();
		Vector cpv = getClassPathVector();
		for (int i = 0; i < cpv.size(); ++i)
			buf.append(
				((BT_ClassPathEntry) cpv.elementAt(i)).getEntryCanonicalName()
					+ sep);
		return buf.toString();
	}

	/**
	 Creates a subclass of {@link BT_ConstantPool}.
	**/
	public BT_ConstantPool createConstantPool() {
		return new BT_ConstantPool(this);
	}


	/**
	 Creates a subclass of {@link BT_Class}.
	 This should cause {@link BT_Class#BT_Class()} to be invoked -- not {@link
	 BT_Class#BT_Class(java.lang.String)} nor {@link BT_Class#BT_Class(java.lang.String,short)}.
	
	 <p> A sample implementation:<code><pre>
	 * return new MyClass();
	 </pre></code>
	
	 @param  nameComment  Null or the fully-qualified name of the class as a comment.
	   It can be ignored by this method.
	**/
	//   NameComment may be null (e.g., if {@link BT_Class#loadFromFile} is used).
	protected BT_Class createClass(String nameComment) {
		return new BT_Class(this);
	}

	/**
	 Creates a subclass of {@link BT_Method}.
	
	 <p> A sample implementation:<code><pre>
	 * return new MyMethod(c);
	 </pre></code>
	
	 Its constructor should do <code>super(c)</code> to invoke
	 {@link BT_Method#BT_Method(BT_Class)}.
	**/
	public BT_Method createMethod(BT_Class c) {
		return new BT_Method(c);
	}

	/**
	 Creates a subclass of {@link BT_Field}.
	
	 <p> A sample implementation:<code><pre>
	 * return new MyField(c);
	 </pre></code>
	
	 Its constructor should do <code>super(c)</code> to invoke
	 {@link BT_Field#BT_Field(BT_Class)}.
	**/
	public BT_Field createField(BT_Class c) {
		return new BT_Field(c);
	}

	/**
	 Creates a subclass of {@link BT_Attribute}.
	
	 <p> A sample implementation:<code><pre>
	 * return new MyAttribute(name, data, container);
	 </pre></code>
	
	 Its constructor should do <code>super(name, data, container)</code> to invoke
	 {@link BT_GenericAttribute#BT_GenericAttribute(String nm, byte[] data, Object container)}.
	
	 @exception BT_ClassFileException is declared because it may be
	   thrown by overridding methods.
	**/
	//   Overriding methods are allowed to throw
	//     BT_ClassFileException.
	public BT_Attribute createGenericAttribute(
		String name,
		byte[] data,
		BT_AttributeOwner container,
		BT_ConstantPool pool,
		LoadLocation loadedFrom)
		throws BT_ClassFileException, java.io.IOException {
		return new BT_GenericAttribute(name, data, container, loadedFrom);
	}
}
