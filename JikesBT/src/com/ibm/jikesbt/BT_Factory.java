package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

//import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 <p> Allows clients to customize aspects of JikesBT, such as specializing
 certain classes to add or change behavior or properties.

 This can be done by defining a subclass of BT_Factory, creating an
 instance of the subclass, and setting static field {@link BT_Factory#factory} to
 reference the instance.  E.g., <code><pre>
 *   class MyFactory extends BT_Factory { ... }
 *    ...
 *   BT_Factory.factory = new MyFactory();
 </pre></code>

 This should be done before JikesBT is used, if it is done at all.

 <p> A method that should probably be overridden is
 {@link BT_Factory#isProjectClass(String,Object)}.

 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#global_settings>global settings</a>

 * @author IBM
**/
public class BT_Factory {

	/**
	 * Java compilers have changed over the years.  Consider the following example.
	 * class A {
	 * 	A() {
	 * 		new B().c();
	 * 	}
	 * }
	 * class B extends C {}
	 * 
	 * class C {
	 * 	public void c() {}
	 * }
	 * 
	 * With older compilers, the method invocation would be stored in the class file as C.c(), while with newer
	 * compilers the reference is stored as B.c(). The difference is that with newer compilers
	 * the resolution is done at run-time as opposed to compile time.  In most cases there will be
	 * no difference in the end.  The difference arises with class compatibility changes, in which 
	 * class B and or C is changed and compiled later.  This is true particularly with invokespecial and
	 * invokestatic instructions.  
	 * 
	 * JikesBT by default conforms to the older method.  To change this, set the following boolean resolveRuntimeReferences
	 * to false.
	 * 
	 * In one instance, resolving a reference cannot be done regardless, as in the following example:
	 * abstract class A implements Runnable {
	 * 	A(A ref) {
	 * 		ref.run();
	 * 	}
	 * }
	 * Since A does not contain its own run method, the resolved target is Runnable.run().  
	 * But an invokevirtual instruction cannot make a reference to an interface (see VM spec 5.4.3.3 step #1).
	 * The reference cannot be compiled as an invokeinterface since the variable is of type A.  
	 * So the reference must be stored as A.run() and not Runnable.run().  
	 * 
	 * In this particular instance there is actually no valid BT_Method object that can be referenced
	 * as the target, since there is an "implied" but no real abstract method run() in class A.
	 * 
	 * The boolean resolveRuntimeReferences applies to field references as well.
	 */
	public static boolean resolveRuntimeReferences = true;
	
	//public static boolean multiThreadedLoading = false;
	public static boolean multiThreadedLoading = true;
	
	/**
	 Determines whether JikesBT builds method relationships.
	 This is public to allow applications to test it.
	 @see BT_Method#getInlaws()
	 @see BT_Method#getParents()
	 @see BT_Method#getKids()
	 @see #BT_Factory( boolean)
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#global_settings>global settings</a>
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#setting_defaults>setting defaults</a>
	**/
	public boolean buildMethodRelationships;

	/**
	 A parameter set by the application that specifies whether or not the
	 constant pool will be kept after reading or writing is complete.
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#global_settings>global settings</a>
	 @see BT_CodeAttribute#keepBytecodes
	 @see BT_Factory#loadMethods
	**/
	public boolean keepConstantPool;

	/**
	 * A parameter set by the application that specifies whether or not related classes such as
	 * inner classes, field types and method signature classes 
	 * are loaded immediately during class loading or are initially created as stubs instead and 
	 * explicitly loaded later.  
	 * Such classes are not needed for type checks, method resolution or field resolution and are typically
	 * not loaded by a virtual machine during class loading.
	 */
	public boolean preloadRelatedClasses = true;
	
	/**
	 A parameter set by the application that specifies whether or not method bodies should be loaded.
	 Some applications only want to load a class to discover its structure and do not intend
	 to write out the class at all. Loading all of the method body would be wasteful in that
	 case.  <p>
	 If a class was loaded with {@link BT_Class#loadMethods} turned off, its bytecode instructions
	 were not loaded, and saving the class will not be possible, unless {@link BT_CodeAttribute#keepBytecodes}
	 was also turned on.
	 <p>
	 Useful settings:
	 <table border=1 cellpadding=2>
	 <tr><td> loadMethods </td><td>  Intent   </td></tr>
	 <tr><td>   false     </td><td>  Fastest load, smallest memory, recommended for signature analysis tools, cannot save class   </td></tr>
	 <tr><td>   true      </td><td>  Slowest load, more memory, recommended for manipulation tools, can save class   </td></tr>
	 </table>
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#global_settings>global settings</a>
	 @see BT_Factory#keepConstantPool
	**/
	public boolean loadMethods;
		
	/**
	 Determines whether or not {@link BT_CodeAttribute#bytecodes} is kept after reading is complete.
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#global_settings>global settings</a>
	**/
	public boolean keepBytecodes;


	/**
	 Determines whether JikesBT should read line number tables and variable name tables.
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#global_settings>global settings</a>
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#setting_defaults>setting defaults</a>
	**/
	public boolean readDebugInfo;

	/**
	 Determines whether a number of class relationships are monitored.
	 BT_Method.referencingAttributes
	 BT_Class.referencingAttributes
	 BT_Class.asArrayTypes
	 BT_Class.asSignatureTypes
	 BT_Class.asFieldTypes
	 */
	public boolean trackClassReferences;
	
	/**
	 Whether stack maps present in a class file will be read.  If stack maps are found in a class file,
	 and this flag is false, then they will not be read from the class file, but instead the stack maps
	 will be recreated internally only when they are first needed.
	 */
	public boolean readStackMaps;
	
	/**
	 Controls whether the verification while reading in classes is strict.
	 If not strict, the following errors are ignored:
	 - bytes after end of file
	 - invalid modifiers
	 - invalid access
	**/
	public static boolean strictVerification = true;
	
	protected Lock factoryLock = new ReentrantLock();

	/**
	 Creates a JikesBT "factory"
	 @param  buildMethodRelationships  Determines whether JikesBT builds method relationships
	 @param keepConstantPool  Determines whether JikesBT keeps a class' constantpool after reading
	 @param readDebugInfo  Determines whether JikesBT reads linenumber and variable name attributes
	 @param loadMethods  Determines whether JikesBT loads methods bodies or skips them
	 @param keepBytecodes  Determines whether JikesBT saves the original method bytecodes after dereferencing a method
	**/
	public BT_Factory(
			boolean buildMethodRelationships, 
			boolean loadMethods, 
			boolean keepConstantPool, 
			boolean readDebugInfo, 
			boolean keepBytecodes) {
		this(buildMethodRelationships,
				loadMethods,
				keepConstantPool,
				readDebugInfo,
				keepBytecodes,
				true,
				true);
	}
	
	public BT_Factory(
			boolean buildMethodRelationships, 
			boolean loadMethods, 
			boolean keepConstantPool, 
			boolean readDebugInfo, 
			boolean keepBytecodes,
			boolean trackClassReferences, 
			boolean readStackMaps) {
		this.buildMethodRelationships = buildMethodRelationships;
		this.loadMethods = loadMethods;
		this.keepConstantPool = keepConstantPool;
		this.readDebugInfo = readDebugInfo;
		this.keepBytecodes = keepBytecodes;
		this.trackClassReferences = trackClassReferences;
		this.readStackMaps = readStackMaps;
	}

	/**
	 An abbreviation of <code>{@link BT_Factory#BT_Factory(boolean, boolean) BT_Factory(true, true)}</code>.
	**/
	public BT_Factory() {
		this(true, true, false, true, false);
	}

	/**
	 Determines whether the named class is of interest.
	 This is typically called by JikesBT while the class is being read.
	
	 This method is often overridden, but if it is not it will cause classes whose names
	 begin with
	 "java.", "sunw.", "sun.", "com.ibm.jikesbt.", "com.ibm.javaSpy.", or "com.ms."
	 to be
	 {@link <a href=../jikesbt/doc-files/glossary.html#external_class>external classes</a>}.
	
	 <p> A sample implementation:<code><pre>
	 * if( file instanceof java.io.File
	 *     && ((java.io.File)file).getPath().startsWith( "abc"))
	 *   return true;
	 * return ! className.startsWith( "java.");
	 </pre></code>
	
	 @param  className  Will not be the name of an array, a primitive type, or a stub class.
	 @param  file  Null or the java.io.File or java.util.zip.ZipFile from
	   which the class is being read.
	   When this is called by {@link BT_Class#loadFromFile}, this argument will
	   be the exact one that was passed to loadFromFile, so "==" can be used.
	
	 @return  True means it is JikesBT should model the class as a
	   {@link <a href=../jikesbt/doc-files/glossary.html#project_class>project class</a>}.
	   False means it is an
	   {@link <a href=../jikesbt/doc-files/glossary.html#external_class>external class</a>}.
	**/
	public boolean isProjectClass(String className, Object file) {
		if (className.startsWith("java.")
			|| className.startsWith("sunw.")
			|| className.startsWith("sun.")
			|| className.startsWith("com.ibm.jikesbt.")
			|| className.startsWith("com.ibm.javaSpy.")
			|| className.startsWith("com.ms."))
			return false;
		return true;
	}

	
	/**
	 Returns the end-of-line character(s).
	 More portable than "\n" (at least on NT 4 where "\n" != cr-lf).
	 An abbreviation for <code>System.getProperties().getProperty("line.separator")</code>.
	**/
	public static final String endl() {
		return BT_Item.endl();
	}	
	

	//-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	// Handle notifications from JikesBT ...

	/**
	 Called while a class is being loaded in case the client cares.
	 This is called after the first pass of loading is complete, but
	 before the class file artifacts have been converted to
	 higher-level constructs (by dereferencing), so the contents
	 of the class should not be referenced by this method.
	
	 <p> This default implementation simply prints the name of the class if it
	 is in the project.
	
	 <p> A sample implementation:<code><pre>
	 * if( c.inProject())
	 *   log( "Loaded "+c.fullKindName()+" "+c+" from "+fromFileName);
	 </pre></code>
	
	 @param  fromFileName  Null if not from a file (i.e., if is an array or a primitive).
	 @see <a href=#noteMethods>notification-methods</a>
	**/
	public void noteClassLoaded(
		BT_Class c,
		String fromFileName) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		if (c.inProject())
			System.out.println(
				Messages.getString("JikesBT.__*_Loaded_{0}_{1}_from_{2}_17", new Object[] {c.fullKindName(), c, fromFileName}));
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
	}

	/**
	 Called when JikesBT cannot find a needed class.
	 If this returns instead of aborting, JikesBT will create a stub class,
	 which may be appropriate perhaps depending on whether the class should be
	 in the project.
	
	 This default (non-overridden) method will call {@link BT_Factory#fatal} if the
	 class is in the project, and will call {@link BT_Factory#warning} otherwise.
	
	 <p> A sample implementation:<code><pre>
	 * if(isProjectClass( className, null))
	 *   fatal( "Could not find class "+className);
	 * warning( "Could not find class "+className);
	 * return BT_Class.createStub( className);
	 </pre></code>
	
	 @param  className  The name of the class.
	 @return  A non-null, initialized {@link BT_Class}.
	   Typically this is generated by calling {@link BT_Class#createStub}.
	**/
	//   Warning: This will call fatal if a project class is not found.
	public BT_Class noteClassNotFound(String className, BT_Repository repo, BT_Class stub, LoadLocation referencedFrom) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		if (isProjectClass(className, null)) // An "important" class
			fatal(Messages.getString("JikesBT.Could_not_find_class_{0}_20", className)); // End execution now
		warning(Messages.getString("JikesBT.Could_not_find_class_{0}_20", className) + Messages.getString("JikesBT.____will_create_a_stub_22"));
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
		//we are holding the class table lock when this  method is called, so no locking required before calling createStub
		if(stub == null) {
			stub = repo.createStub(className);
		}
		return stub;
	}
	
	/**
	 Called when JikesBT finds a entry in the classpath is neither a directory nor
	 a .jar/.zip file.
	 Proceeding (returning) should be safe.
	
	 <p> A sample implementation:<code><pre>
	 * printAndLog( message);
	 </pre></code>
	
	 @param  entry  The text in error in the class path.
	**/
	public void noteClassPathProblem(String entry, String message) {
		warning(message);
	}
	
	
	public void noteFileCloseIOException(String resource, IOException e) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		warning(Messages.getString("JikesBT.Exception_closing__{0}_104",
				resource));
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
	}

	/**
	 Handles an I/O error that occurred while a class file is being read.
	 Note that it is possible that the I/O error occurred on a file other than the class file (e.g., a log file).
	
	 This may return to the caller to continue execution, may throw a
	 RuntimeException to end this call to JikesBT, or may even simply invoke System.exit.
	 If this returns, JikesBT will create a stub class in place of this one.
	
	 @param className  A {@link <a
	   href=../jikesbt/doc-files/glossary.html#project_class>project class</a>} name, a
	   {@link <a href=../jikesbt/doc-files/glossary.html#external_class>external class</a>}
	   name, or "" if loading by file name instead of class name.
	 @param ex  Not null.
	 @see <a href=#noteMethods>notification-methods</a>
	**/
	public void noteClassReadIOException(
		String className,
		String fileName,
		IOException ex) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		boolean pc = isProjectClass(className, null);
		String m =
			Messages.getString("JikesBT.I/O_error_while_reading_{0}_class_{1}{2}_--_From_file_{3}_24", 
				new Object[] {(pc ? "project" : "external"), className, endl(), fileName});
		if (pc) // Is an important class
			fatal(m, ex);
		else { // Can create a stub to represent the (external) class
			warning(m + endl() + Messages.getString("JikesBT.____Will_create_a_stub_class_and_continue_29"));
		}
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
	}
	
	/**
	 Called when an error in a class file attribute is detected (usually
	 while it is being read).
	 
	 This method will not be called due to a failure to read the CodeAttribute, but it will
	 be called for all other attributes.
	
	 This may return to the caller to continue execution, may throw a
	 RuntimeException to end this call to JikesBT, or may even simply invoke
	 System.exit(int).
	 JikesBT will remove the attribute from the class.
	 *
	 */
	public void noteAttributeLoadFailure(BT_Repository rep, BT_Item item, String attName, BT_Attribute attribute, BT_AttributeException e, LoadLocation loadLocation) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		warning(Messages.getString("JikesBT.Could_not_load_attribute_{0}_in_{1}__{2}_105", 
				new Object[] {attribute == null ? attName : attribute.getName(), item.useName(), e}));
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
	}
	
	/**
	 Handles an error that occurred while a class file attribute is being written.
	 
	 This may return to the caller to continue execution, may throw a
	 RuntimeException to end this call to JikesBT, or may even simply invoke
	 System.exit.
	 
	 If the method throws BT_ClassWriteException then failure to write this attribute will cause the class to not be written at all.
	
	 JikesBT will remove the attribute from the class.
	 
	**/
	public void noteAttributeWriteFailure(BT_Item item, BT_Attribute attribute, BT_AttributeException e) throws BT_ClassWriteException {
		warning(Messages.getString("JikesBT.Could_not_write_attribute_{0}_in_{1}__{2}_106", 
				new Object[] {attribute.getName(), item.useName(), e}));
	}
	

	/**
	 Called when an error in a class file is detected (usually
	 while it is being read).
	
	 This may return to the caller to continue execution, may throw a
	 RuntimeException to end this call to JikesBT, or may even simply invoke
	 System.exit(int).
	 If this returns, JikesBT will create a stub to represent the class, but
	 note that other classes may continue to be linked to the
	 partially-created class.
	
	
	 @param  className  A {@link <a href=../jikesbt/doc-files/glossary.html#project_class>project class</a>} name,
	   a {@link <a href=../jikesbt/doc-files/glossary.html#external_class>external class</a>} name,
	   or "" if loading by file name instead of class name.
	 @param  filename  Null or the name of the class file being read.
	 @param  ex  Either null or an exception that is not specifically handled by JikesBT.
	**/
	private void noteClassLoadFailure(
		String className,
		String fileName,
		Throwable ex,
		String equivalentRuntimeError) {
		if (ex instanceof BT_FatalRuntimeException)
			// Assumed to be thrown by "fatal"
			throw (BT_FatalRuntimeException) ex; // Don't recover from these

		boolean pc = isProjectClass(className, null);
		String text =
			Messages.getString("JikesBT.Error_verifying_{0}_class_{1}_31",
				new Object[] {(pc ? "project" : "external"), className});
		if (fileName != null)
			text = text + endl() + Messages.getString("JikesBT.____From_file__35") + fileName;
		text = text + endl();

		if (pc) // Is an important class
			fatal(text, ex);
		else { // Is an unimportant class
			// Can create a stub to represent the (external) class
			warning(
				text + endl() + Messages.getString("JikesBT.____Will_create_a_stub_class_and_continue_29"));
		}
	}
	
	public void noteClassLoadFailure(
			BT_Repository rep,
			BT_ClassPathEntry entry,
			BT_Class clazz,
			String name, 
			String fileName,
			Throwable ex,
			String equivalentRuntimeError) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		noteClassLoadFailure(name, fileName, ex, equivalentRuntimeError);
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
	}
	
	/** 
	 This method records errors in class files that are not caught by the runtime verifiers, or errors that
	 do not prevent class loading from completing.
	 
	 For instance, the IBM JRE contains auto-generated classes that have methods with the same name
	 and arguments, but different return types.  This is illegal.  In these classes, however,
	 one such method calls the other.  So the error does not affect runtime behaviour, and the
	 run-time verifiers do not catch the error.
	 Run-time verifiers will catch methods with duplicate names and signatures, with the signatures
	 including the return type.
	 */
	public void noteClassLoadError(
			BT_ClassPathEntry entry,
			BT_Class clazz,
			String className,
			String fileName,
			String problem,
			String equivalentRuntimeError) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		warning(className + endl() + problem);
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
		return;
	}
	
	/**
	 Handles an I/O error that occurred while a class file is being written.
	 Note that it is possible that the I/O error occurred on a file other than
	 the class file (e.g., a log file).
	
	 This may return to the caller to continue execution, may throw a
	 RuntimeException to end this call to JikesBT, or may even simply invoke
	 System.exit.
	
	 <p> A sample implementation:<code><pre>
	 * String  m = "I/O error while writing class "+className
	 *     +"\n -- to file "+fileName;
	 * fatal( m, ex);
	 </pre></code>
	
	 @param  ex  Not null.
	 @see <a href=#noteMethods>notification-methods</a>
	**/
	public void noteClassWriteIOException(
		String className,
		String fileName,
		IOException ex) {
		String m =
			Messages.getString("JikesBT.I/O_error_while_writing_class_{0}{1}____to_file_{2}_40",
				new Object[] {className, endl(), fileName});
		fatal(m, ex); // Terminate the active call to JikesBT
	}
	
	/**
	 Handles an error that occurred while a class file is being written.
	 
	 This may return to the caller to continue execution, may throw a
	 RuntimeException to end this call to JikesBT, or may even simply invoke
	 System.exit.
	
	 @param  ex  Not null.
	 @see <a href=#noteMethods>notification-methods</a>
	**/
	public void noteClassWriteException(
		String className,
		String fileName,
		BT_ClassWriteException ex) {
		fatal("", ex); // Terminate the active call to JikesBT
	}
	
	public void noteCodeException(BT_CodeException ex) {
		warning(ex.toString());
	}
	
	public void noteCodeIrregularity(BT_CodeException ex) {
		warning(ex.toString());
	}

	/**
	 Called when JikesBT first finds a reference to an undeclared field.
	 Before this is called, JikesBT has added the missing field.
	 If this returns, JikesBT will continue with the newly added field.
	
	 <p> A sample implementation:<code><pre>
	 * String msg = referrer+"\n -- refers to an undeclared field";
	 * if( f.cls.inProject())
	 *   fatal( msg);
	 * else
	 *   warning( msg);
	 </pre></code>
	
	 @param m  The missing referenced field in a
	   {@link <a href=../jikesbt/doc-files/glossary.html#project_class>project class</a>} or
	   {@link <a href=../jikesbt/doc-files/glossary.html#external_class>external class</a>}.
	 @param referrer  What is doing the referencing.
	 @see <a href=#noteMethods>notification-methods</a>
	**/
	//   Uses {@link BT_Factory#warning} instead of print.
	public void noteUndeclaredField(BT_Field f, BT_Class targetClass, BT_Method fromMethod, BT_FieldRefIns fromIns, LoadLocation loadedFrom) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		String msg =
				Messages.getString("JikesBT.Instruction_{0}_1", fromIns)
				+ endl() 
				+ Messages.getString("JikesBT.____in_method_{0}_2", fromMethod)
				+ endl()
				+ Messages.getString("JikesBT.____refers_to_an_undeclared_field_in_a_project_class_68");
		if (f.cls.inProject())
			fatal(msg);
		else
			warning(msg + endl() + Messages.getString("JikesBT.____a___public___field_will_be_added_69"));
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
	}

	/**
	 Called when JikesBT first finds a reference to an undeclared method.
	 Before this is called, JikesBT has added the missing method.
	 If this returns, JikesBT will continue with the newly added method.
	
	 <p> A sample implementation:<code><pre>
	 * String msg = referrer+"\n -- refers to an undeclared method";
	 * if(m.cls.inProject())
	 *   fatal( msg);
	 * else
	 *   warning( msg);
	 </pre></code>
	
	 @param m  The missing referenced method in a
	   {@link <a href=../jikesbt/doc-files/glossary.html#project_class>project class</a>} or
	   {@link <a href=../jikesbt/doc-files/glossary.html#external_class>external class</a>}.
	 @param referrer  What is doing the referencing.
	**/
	//   Uses {@link BT_Factory#warning} instead of print.
	public void noteUndeclaredMethod(BT_Method m, BT_Class targetClass, BT_Method fromMethod, BT_MethodRefIns fromIns, LoadLocation loadedFrom) {
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.lock();
		}
		String msg =
				Messages.getString("JikesBT.Instruction_{0}_1", fromIns)
				+ endl() 
				+ Messages.getString("JikesBT.____in_method_{0}_2", fromMethod)
				+ endl()
				+ Messages.getString("JikesBT.____refers_to_an_undeclared_method_in_a_{0}_class_70", (m.cls.inProject() ? "project" : "system"));
		if (m.cls.inProject())
			// May not want to add missing methods to project classes
			fatal(msg);
		else
			warning(
				msg + endl() + Messages.getString("JikesBT.____a___public_native___method_will_be_added_74"));
		if(BT_Factory.multiThreadedLoading) {
			factoryLock.unlock();
		}
	}

	//TODO make warning, error and fatal package level, so that they are not called directly from elsewhere.
	//This requires altering the jikesbt samples.
	
	/**
	 Issues a warning message.
	 This is never invoked by JikesBT except perhaps in BT_Factory methods.
	**/
	public void warning(String s) {
		System.out.println(Messages.getString("JikesBT.Warning__{0}_75", s));
	}

	/**
	 Issues an error message.
	 This is never invoked by JikesBT except perhaps in BT_Factory methods.
	**/
	public void error(String s) {
		System.out.println(Messages.getString("JikesBT.Error__{0}_{0}_76", s));
	}
	
	/**
	 Issues an error message and possibly information about an exception, and
	 terminates via System.exit or by throwing a {@link
	 BT_FatalRuntimeException}.
	 If this method returns normally, results are unpredictable.
	
	 <p> A sample implementation:<code><pre>
	 * throw new BT_FatalRuntimeException( "Fatal error: "+message);
	 </pre></code>
	
	 @param  ex  May be null.
	**/
	void fatal(String message, Throwable ex) {
		System.out.println("");
		System.out.println("***** " + Messages.getString("JikesBT.Fatal_error__{0}_10", message));
		(new Throwable()).printStackTrace( System.err );
		throw new BT_FatalRuntimeException(Messages.getString("JikesBT.Fatal_error__{0}_10", message));
	}

	/**
	 Issues an error message and terminates via System.exit or by throwing
	 a RuntimeException (or a java.lang.Error).
	 If this method returns normally, results are unpredictable.
	**/
	public void fatal(String message) {
		fatal(message, null);
	}

}
