package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;
import com.ibm.jikesbt.BT_Field.FieldType;
import com.ibm.jikesbt.BT_NoSuchMemberException.BT_NoSuchFieldException;
import com.ibm.jikesbt.BT_NoSuchMemberException.BT_NoSuchMethodException;
import com.ibm.jikesbt.BT_Repository.LoadLocation;
import com.ibm.jikesbt.BT_StackType.ClassType;

/*
 Implementation notes:
 - The words "attach" and "detach" are generally used for consistency-preserving public methods, whereas
   "link", "delink", and "relink" are generally not consistency-preserving and are private.
*/

/**
 A high-level representation of a given Java class (including interfaces,
 arrays, and primitive types).
 See {@link com.ibm.apps.AuxClass} for additional class-related methods.

 <p> See the <a href=../jikesbt/doc-files/UserGuide.html#BT_CLASS>User Guide<a>.

 <p> A class can be loaded from the classpath in two ways:
 <p> <center><table width="80%" cellpadding=8 bgcolor="#dddddd"><tr><td><code><pre>
     BT_Class sample  = BT_Class.forName("java.lang.String");
     BT_Class another = BT_Repository.findClass("java.lang.String");
 </pre></code></td></tr></table></center>

 <p> Both forms have the same effect.
 If the class has been loaded previously, it is returned.
 Otherwise, the classpath is searched, and each entry in the classpath is
 used to locate the class.

 <p> For each classpath entry, it is determined whether the entry is a
 directory name.
 In that case, a filename is created using the directory name, the package
 name of the target class, its class name, and the string ".class".
 If such a file exists, an effort is made to load it.
 If no error occurs during loading, the resulting class file is returned.
 If the entry is an archive, the archive is loaded to find the class
 inside it.
 If the current classpath entry does not contain the target class, we
 try the next entry in the classpath.
 If the class is never found, a stub class is generated.

 <p> The second form (using {@link BT_Repository}) is mostly used internally,
 and emphasizes the fact that the class is retrieved from the repository,
 and otherwise loaded.

 <p> The first form is the most often used form, and is very similar
 to the <tt>java.lang.Class.forName(String)</tt> method to dynamically
 load a class.

 <p> The following utility functions exist for locating methods and fields
 in the class:

 <p> <ul>
 <ul>
 <tt>
  <li>   BT_Field {@link BT_Class#findField findField}
  <li>   BT_Method {@link BT_Class#findMethod findMethod}
  <li>   BT_Method {@link BT_Class#findInheritedMethod findInheritedMethod}
 </tt>
 </ul>
 </ul>


 * @author IBM
**/
public class BT_Class extends BT_Item implements FieldType, Comparable {
	
	public BT_ClassVersion version = new BT_ClassVersion();
	
	public static final int MAGIC = 0xCAFEBABE;
	public static final int BAD_MAGIC = 0x0BADBABE;
		
	public static final String STUB_CLASS_NAME = "stub-class";
	public static final String CLASS_NAME = "class";
	public static final String EXTERNAL_CLASS_NAME = "external-class";
	public static final String EXTERNAL_INTERFACE_NAME = "stub-interface";
	public static final String INTERFACE_NAME = "interface";
	public static final String ENUM_NAME = "enum";
	public static final String STUB_INTERFACE_NAME = "stub-interface";
	public static final String ARRAY_NAME = "array";
	public static final String PRIMITIVE_NAME = "primitive";
	public static final String STUB_NAME = "stub";
	
	/**
	 Zero or the last modification time of the file/zip entry from which the class was read.
	**/
	public long lastModificationTime;

	/**
	 The constantpool for this class.
	 Removed after reading the class file unless {@link BT_Class#keepConstantPool} is true.
	**/
	public BT_ConstantPool pool;

	/**
	 Repository	that this class belongs to.
	**/
	protected BT_Repository repository;
	
	/**
	 The package name of the class.  Updated lazily with calls to packageName() and reset with calls to setName().
	 */
	String cachedPackageName;
	
//	class DebugLock extends ReentrantLock {
//		String name;
//		private StackTraceElement[] trace;
//		Thread lastOwner;
//		private StackTraceElement[] tracePrev;
//		
//		public void lock() {
//			super.lock();
//			if(name == null) {
//				name = BT_Class.this.getName();
//			}
//			if(lastOwner == Thread.currentThread()) {
//				tracePrev = trace;
//			} else tracePrev = null;
//			trace = new Throwable().getStackTrace();
//			lastOwner = Thread.currentThread();
//		}
//	}
	
	//DebugLock classLock = new DebugLock();
	ReentrantLock classLock = new ReentrantLock();
	private ReentrantLock creationSiteLock = new ReentrantLock();
	private ReentrantLock referenceSiteLock = new ReentrantLock();
	private ReentrantLock arrayTypeLock = new ReentrantLock();
	private ReentrantLock referencingAttributeLock = new ReentrantLock();
	private ReentrantLock fieldTypeLock = new ReentrantLock();
	private ReentrantLock signatureTypeLock = new ReentrantLock();
	private ReentrantLock kidsLock = new ReentrantLock();
	
	public BT_Repository getRepository() {
		return repository;
	}

	public BT_Class getType() {
		return this;
	}

	/**
	 Is this class created anywhere?
	 An abbreviation for <code>creationSites.size() != 0</code>.
	**/
	public boolean isCreated() {
		return creationSites.size() != 0;
	}

	public boolean isClassMember() {
		return false;
	}
	
	/**
	 True if this represents an array  (not a class, interface, primitive, nor stub).
	 An abbreviation for <code>c.arrayType != null</code>.
	**/
	public boolean isArray() {
		String nm = name;
		return nm.charAt(nm.length() - 1) == ']';
	}

	/**
	 Return the number of dimensions if an array type, or 0 otherwise.
	**/
	public int getArrayDimensions() {
		String nm = name;
		int dimensions = 0;
		for(int n = nm.length() - 1; nm.charAt(n) == ']'; n-=2, dimensions++);
		return dimensions;
	}
	
	/**
	 * 
	 * @return the array class with this class as the element class
	 */
	public BT_Class getArrayClass() {
		return repository.forName(fullName() + "[]");
	}
	
	/**
	 * If this class is an array class, this method returns the class representing an element of
	 * the array class.
	 * @return the element class or null if not an array class
	 */
	public BT_Class getElementClass() {
		return getElementClass(true);
	}
	
	/*
	 * currently we do not allow this method to be called with link == false, since
	 * this will cause unpredictable class loading.  Class loading must not occur 
	 * under the covers when doing class manipulations, because previous manipulations
	 * might not be compatible with classes loaded.
	 */
	private BT_Class getElementClass(boolean link) {
		//note: no extra locking required here
		BT_Class result = null;
		if(isArray()) {
			String declaringName = fullName();
			String elementClassName = declaringName.substring(0, declaringName.length() - 2);
			BT_Repository rep = getRepository();
			result = link ? rep.linkToStub(elementClassName, referencedFrom) : rep.forName(elementClassName, referencedFrom);
		}
		return result;
	}
	
	/**
	 Gets the class this item is declared in.
	 Named like java.lang.reflect.getDeclaringClass().
	**/
	public BT_Class getDeclaringClass() {
		return this;
	}
	
	/**
	 * Used by the data-flow analyzer
	 */
	public ClassType classType;
	
	/**
	 If this class represents an array, this is its "element type" (Java terminology);
	 otherwise, null.
	 An "element type" is the type of a component of the array, unless the
	 component has an array type, in that case the element type of the array
	 is the same as the element type of its component.
	 E.g., the element type of "A[][][]" is "A".
	
	 <p> Not quite like java.lang.Class.getComponentType() that returns the
	 array's "component type", that is the type of one element of the array,
	 that may itself be an array.
	**/
	public BT_Class arrayType;

	/**
	 True if this represents a class (not an array, interface, nor primitive).
	 May be false if this is a stub and not yet known to be a class.
	 @see #isArray
	 @see #isInterface
	 @see #isBasicTypeClass
	**/
	public boolean isClass;
	
	/**
	 Currently, same as field {@link BT_Class#isClass}.
	 This method allows calling a method instead of having to remember whether to use a field,
	 and using it may reduce changes needed if the field is ever changed.
	**/
	public boolean isClass() {
		return isClass;
	}

	public BT_InnerClassesAttribute getInnerClassAttr() {
		for (int i = 0; i < attributes.size(); i++) {
			BT_Attribute attr = attributes.elementAt(i);
			if (attr instanceof BT_InnerClassesAttribute) {
				return (BT_InnerClassesAttribute) attr;
			}
		}
		return null;
	}
	
	/**
	 * returns the version of this class.
	 */
	public BT_ClassVersion getVersion() {
		return version;
	}
	
	/**
	 Sets {@link BT_Class#isClass}, unsets other booleans, and nulls {@link #arrayType}.
	**/
	public void becomeClass() {
		if(isBasicTypeClass || isArray()) {
			throw new IllegalStateException();
		}
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		isClass = true;
		isInterface = false;
		disableFlags(INTERFACE);
		BT_Class object = repository.findJavaLangObject();
		if(object != this && superClass == null) {
			setSuperClass(object);
		}
		// isStub = false; -- No -- can still also be a stub?
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
	}

	/**
	 True if this represents an interface (not an array, class, nor primitive).
	 May be false if this is a stub and not yet known to be an interface.
	 @see #isArray
	 @see #isClass
	 @see #isBasicTypeClass
	**/
	boolean isInterface;

	/**
	 Currently, the same as field {@link BT_Class#isInterface}.
	 This method allows calling a method instead of having to remember whether to use a field,
	 and using it may reduce changes needed if the field is ever changed.
	**/
	public boolean isInterface() {
		return isInterface;
	}

	/**
	 True if this is declared enum as determined by {@link BT_Item#flags}.
	**/
	public boolean isEnum() {
		return areAnyEnabled(ENUM);
	}
	
	/**
	 Sets {@link BT_Class#isInterface}, unsets other booleans, and nulls {@link #arrayType}.
	**/
	public void becomeInterface() {
		if(isBasicTypeClass || isArray()) {
			throw new IllegalStateException();
		}
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		if (superClass != null) {
			setSuperClass(null);
		}
		isClass = false;
		isInterface = true;
		enableFlags(INTERFACE);
		
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
	}

	public boolean isVoid() {
		return name.equals("void");
	}
	
	/**
	 * denotes a stub that was created during linking that has not yet been 
	 * loaded from the classpath, and also the supplied LoadLocation when that loading was initiated (which can be null).
	 */
	boolean notLoaded;
	LoadLocation referencedFrom;
	
	/**
	 Null or the location (class/zip/jar file) where this class was loaded from.
	**/
	public String loadedFrom;
	
	/**
	 * This field is set to null after loading is complete.
	 */
	BT_ClassPathLocation loadedFromEntry; 
	
	
	/**
	 True means the JVM should use the new semantics for invokespecial.
	 Defaults to true and updated when a class file is read.
	**/
	public boolean usesInvokeSpecial = true; // New code should set this true
	
	
	/**
	 True if this represents "void" or a primitive type such as "int"
	 (not an array, class, interface, nor stub).
	 @see <a href=../jikesbt/doc-files/Glossary.html#primitive_class>primitive type</a>
	 @see #isArray
	 @see #isClass
	 @see #isInterface
	**/
	public boolean isBasicTypeClass;

	/**
	 Currently, same as field {@link BT_Class#isBasicTypeClass}.
	 This method allows calling a method instead of having to remember whether to use a field,
	 and using it may reduce changes needed if the field is ever changed.
	**/
	public boolean isPrimitive() {
		return isBasicTypeClass;
	}

	final protected BT_ClassVector parents_ = new BT_ClassVector();

	// added to be able to find superclass irregardless of type (could be interface by accident)
	private BT_Class superClass;

	// added to be able to regenerate circular interface and class definitions without endless recursion in building relationships.
	private BT_ClassVector circularParents_;
	private BT_Class invalidSuperClass;
	
	/**
	 Gets the classes and/or interfaces that this class or interface is
	 declared to extend or implement (i.e., "direct superclasses" and "direct
	 superinterfaces" in Java terminology).
	 Not indirect (not grandparents).
	 See {@link <a href=../jikesbt/doc-files/Glossary.html#parent_class>parent class</a>}.
	 Never null.
	
	 <p> If this is an interface or class "java.lang.Object", then there
	 will be no member of "parents" that is a class.
	 If this is a class other than "java.lang.Object", then there will
	 be one member of "parents" that is a class, and that will be the
	 super-class of this.
	
	 <p> In either case, there may be any number of members that are
	 interfaces that this class implements or that this interface
	 extends.
	 This collection will be empty if and only if this class is
	 "java.lang.Object" or this is an interface that has no
	 superinterfaces.
	
	 <p> The set returned is part of JikesBT's data model, so updating it will change the parents of this class.
	**/
	public BT_ClassVector getParents() {
		return parents_;
	}

	/**
	 Gets the interfaces that this class or interface is
	 declared to implement that were found to result in a circular definition.
	 Returns null if there are none.
	 */
	public BT_ClassVector getCircularParents() {
		return circularParents_;
	}

	/**
	 True if this class is visible (public or package private) from the given class.
	**/
	public boolean isVisibleFrom(BT_Class clazz) {
		if (isPublic() || isPrimitive() || isArray())
			return true;
		else
			return isInSamePackage(clazz);
	}

	public boolean isUnconditionallyVisibleFrom(BT_Class clazz) {
		return isVisibleFrom(clazz);
	}
	
	/**
	 Makes this class visible to the given class.
	 @returns true if the visibility was changed
	**/
	public boolean becomeVisibleFrom(BT_Class clazz) {
		if(!isPublic() && !isInSamePackage(clazz)) {
			becomePublic();
			return true;
		}
		return false;
	}
	
	/**
	 The classes or interfaces that are declared to extend or
	 implement this class or interface.
	 Not indirect (not grandkids).
	 See {@link <a href=../jikesbt/doc-files/Glossary.html#kid_class>kid class</a>}.
	 Never null.
	
	 <p> If this is a class, then all members of "getKids()" will be classes
	 that "extend" this.
	 If this is an interface, then members of "kids_" that are
	 classes implement this, and members that are interfaces
	 extend this.
	
	 <p> The set returned is part of JikesBT's data model, so updating it will change the kids of this class.
	**/
	public BT_ClassVector getKids() {
		return kids_;
	}

	final BT_ClassVector kids_ = new BT_ClassVector();

	/**
	 True if this is a subclass, subinterface, or implementing class of parentC.
	 An abbreviation of  <code>getParents().contains(parentC)</code>.
	**/
	boolean isKidOf(BT_Class parentC) {
		return parents_.contains(parentC);
	}

	
	/**
	 Returns the index of the superclass in the {@link @getParents} collection.
	 -1 if none if this is "java.lang.Object", an interface, or perhaps if is a stub.
	**/
	final int getSuperClassIndex() {
		// Find the one (if any) non-interface ...
		for (int i = 0; i < parents_.size(); ++i) {
			BT_Class parent = parents_.elementAt(i);
			if (parent == superClass) // Has a superclass
				return i;
		}
		return -1;
	}

	/**
	 Returns the superclass, or null if this is "java.lang.Object" or an interface.
	 A stub class has java.lang.Object as the superclass.
	 If the superclass was found to be a circular dependent of this class, null is returned.
	**/
	public BT_Class getSuperClass() {
		if (!throwsClassCircularityError())
			return superClass;
		if (CHECK_JIKESBT
			&& isClass
			&& !isStub()
			&& !BT_Repository.JAVA_LANG_OBJECT.equals(name))
			assertFailure(Messages.getString("JikesBT.Missing_superclass_of___4") + fullName());
		// Should be true unless the user has added without deleting
		return null;
	}

	/**
	 Returns the superclass name, or null if this is "java.lang.Object", an interface, or a perhaps if is a stub.
	 The name is returned even if the superclass was found to be a circular dependent of this class.
	**/
	public String getSuperClassName() {
		if (superClass != null)
			return superClass.getName();
		return null;
	}

	/**
	 Makes this a class if it is not already a class.
	**/
	// warning -- adds superclass java.lang.Object and a
	// default, native constructor if this is initially an undifferentiated
	// stub.
	private void shouldBeClass() {
		if (!isClass && !isInterface) {
			becomeClass();
		}
		checkSuper();
	}

	private void checkSuper() {
		if (isClass) {
			if (superClass == null && this != repository.findJavaLangObject())
				setSuperClass(repository.findJavaLangObject());
		} else {
			setSuperClass(null);
		}
	}

	/**
	 Sets this class's superclass, including detaching from any old parent.
	 If this or the superclass is a stub, marks it as being a class (vs an interface).
	
	 <p> Is a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>non-consistency-preserving method</a>,
	 so does _not_ properly update information returned by the
	 following methods for the class represented by this object and
	 related classes and methods:
	
	 {@link BT_Class#resetSuperClass},
	 {@link BT_Class#getKids()},
	 {@link BT_Class#getParents()},
	 {@link BT_Method#getInlaws()},
	 {@link BT_Method#getKids()},
	 {@link BT_Method#getParents()},
	 etc.
	
	 @param c  The new superclass (not null).
	 @return  Null or the former superclass.
	**/
	public BT_Class setSuperClass(BT_Class newSuper) {
		if (CHECK_USER && !newSuper.name.equals(BT_Repository.JAVA_LANG_OBJECT))
			expect(Messages.getString("JikesBT.The_superclass_of_an_array_should_be_java.lang.Object____not__6") + newSuper);
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}

		if (superClass != null) {
			parents_.removeElement(superClass);
			if(BT_Factory.multiThreadedLoading) {
				superClass.kidsLock.lock();
			}
			superClass.kids_.removeElement(this);
			if(BT_Factory.multiThreadedLoading) {
				superClass.kidsLock.unlock();
			}
		}

		if (newSuper != null) {
			parents_.addUnique(newSuper);
			if(BT_Factory.multiThreadedLoading) {
				newSuper.kidsLock.lock();
			}
			newSuper.kids_.addUnique(this);
			if(BT_Factory.multiThreadedLoading) {
				newSuper.kidsLock.unlock();
			}
		}

		superClass = newSuper;
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		return newSuper;
	}

	/**
	 The {@link BT_Field}s declared by this class.
	 Similar to java.lang.Class.getDeclaredFields().
	**/
	public BT_FieldTable fields = new BT_FieldTable();

	/**
	 Returns field {@link BT_Class#fields}.
	 This method allows calling a method instead of having to remember whether to use a field,
	 and using it may reduce changes needed if the field is ever changed.
	**/
	public BT_FieldVector getFields() {
		return fields;
	}

	/**
	 The {@link BT_Method}s declared by this class.
	 Similar to java.lang.Class.getDeclaredMethods() and java.lang.Class.getConstructors().
	**/
	public BT_MethodTable methods = new BT_MethodTable();

	/**
	 Returns field {@link BT_Class#methods}.
	 This method allows calling a method instead of having to remember whether to use a field,
	 and using it may reduce changes needed if the field is ever changed.
	**/
	public BT_MethodVector getMethods() {
		return methods;
	}
	
	public BT_MethodVector getConstructors() {
		BT_MethodVector cons = new BT_MethodVector(1);
		for(int i=0; i<methods.size(); i++) {
			BT_Method meth = methods.elementAt(i);
			if(meth.isConstructor()) {
				cons.addElement(meth);
			}
		}
		cons.trimToSize();
		return cons;
		
	}
	
	public boolean contains(BT_Member member) {
		if(member instanceof BT_Field) {
			return fields.contains((BT_Field) member);
		} else {
			return methods.contains((BT_Method) member);
		}
	}


	
	/* 
 	The 7 different dereferenced item links in JIKESBT:
	
	1. callSites (dereferenced by method invocation instructions, added to BT_Method.callSites and BT_CodeAttribute.calledMethods), 
	2. accessors(dereferenced by field access (get/put) instructions, added to BT_Field.accessors and BT_CodeAttribute.accessedFields), 
	3. array types (dereferenced by BT_Class, added to BT_Clazz.asArrayTypes), 
	4. field types (deferenced by BT_Field, added to BT_Clazz.asFieldTypes),
	5. method signature types (dereferenced by BT_Method, added to BT_Clazz.asSignatureTypes), 
	6. class reference sites (dereferenced by BT_ClassRef instructions, added to BT_Clazz.referenceSites and BT_CodeAttribute.referencedClasses),
	7. creation sites (dereferenced by ldc, ldc_w, ldc2_w and new instructions, added to BT_Clazz.creationSites and BT_CodeAttribute.createdClasses) 
	
	Any and all references to a class can be found from these 7 links and from the 
	hierarchical relationships: parents, kids and inlaws.
	
	The following is a list of known vectors
	BT_CodeAttribute.accessedFields
	BT_CodeAttribute.calledMethods
	BT_CodeAttribute.referencedClasses
	BT_CodeAttribute.createdClasses
	BT_CodeAttribute.exceptionTableEntries

	attribute vectors in BT_Class, BT_Field, BT_Method, BT_CodeAttribute

	BT_ConstantPool.items

	BT_Field.accessors
	
	//TODO get rid of BT_Method.inlaws (but perhaps transfer inlaws to parents), BT_Method.kids, BT_Class.kids
	//doing this will allow us to get rid of most of BT_MethodRelationships and all its ugliness
	//also keep in mind that if we move inlaws to parents, that deleting methods can create inlaws,
	//and that deleting classes can delete inlaws
	
	BT_Method.callSites
	BT_Method.inlaws
	BT_Method.kids
	BT_Method.parents
	BT_Method.referencingAttributes

	BT_Class.asArrayTypes
	BT_Class.asFieldTypes //primitives excluded
	BT_Class.asSignatureTypes //primitives excluded
	BT_Class.creationSites
	BT_Class.referenceSites
	BT_Class.referencingAttributes
	BT_Class.kids
	BT_Class.fields
	BT_Class.methods
	BT_Class.parents

	BT_MethodSignature.types

	BT_Repository.classesToBeDereferenced
	BT_Repository.signatures
	BT_Repository.classes
	BT_Repository.resources

	BT_ExceptionsAttribute.exceptionNames
	BT_ExceptionsAttribute.declaredExceptions
	
	BT_InsVector.locals
	*/
	
	/**
	 Attributes which reference this class, excluding the code attribute (BT_CodeAttribute)
	 and attributes which are contained in the code attribute (BT_LocalVariableAttribute, 
	 BT_LineNumberAttribute, BT_LocalVariableTypeTableAttribute)
	**/
	public BT_AttributeVector referencingAttributes;
	
	
	/**
	 The exact locations where this class is created by a given "new" instruction
	 in a method.
	**/
	public BT_CreationSiteVector creationSites = new BT_CreationSiteVector();
	
	/**
	 The exact locations where this class is referenced by a given checkcast, instanceof or new instruction
	 in a method.  Includes BT_ClassReferenceSite objects that are in fact BT_CreationSite objects.
	**/
	public BT_ClassReferenceSiteVector referenceSites = new BT_ClassReferenceSiteVector();
	
	/**
	 The array classes that use this class as an element class
	**/
	public BT_ClassVector asArrayTypes;
	
	/**
	 The fields that use this class as their type
	**/
	public BT_FieldVector asFieldTypes;
	
	/**
	 The methods that use this class in their signature
	**/
	public BT_SignatureSiteVector asSignatureTypes;
	
	/**
	 Creates an empty class.
	 This _should_ be invoked by overrides of {@link BT_Factory#createClass}.
	
	 <p> This is never called by JikesBT except in {@link BT_Factory#createClass}.
	 
	 You should usually follow the call to this constructor with a call to initClass, initAsPrimitive or initAsArray
	**/
	protected BT_Class(BT_Repository repository) {
		this.repository = repository;
	}
	
	boolean tableLockReleased;
	
	/**
	 * make this class a primitive
	 * @param name
	 */
	void initAsPrimitive(String name, BT_Repository rep, int stackMapType, BT_Class convertToStackType) {
		this.name = name;
		rep.addClass(this);
		isBasicTypeClass = true;
		setInProjectFalse();
		if(convertToStackType != null) {
			/* in stack maps this class is represented by another class (eg byte is represented by int) */
			classType = new ClassType(this, convertToStackType.classType);
		} else if(stackMapType != BT_StackType.ITEM_UNDEFINED) {
			classType = new ClassType(this, stackMapType);
		} else { /* this class will never appear in stack maps */ }
		
	}

	
	/**
	 <p> Makes this an array, including adding the same members as in the following class:<code><pre>
	 *   class A implements Cloneable, Serializable {
	 *     public final int length = __;
	 *     public Object clone() {
	 *           try { return super.clone(); }
	 *           catch( CloneNotSupportedException e) { throw new Error(e.getMessage()); }
	 *     }
	 *   }</pre></code>
	
	 But it also implements java.io.Serializable in Java 2.
	
	 <p>
	**/
	void initAsArray(String nm, boolean link) throws BT_ClassFileException {
		name = nm;
		
		if(BT_Misc.overflowsUnsignedByte(getArrayDimensions())) {
			throw new BT_ClassFileException(Messages.getString("JikesBT.{0}_count_too_large_109", "dimensions"));
		}
		
		setInProjectFalse();
		
		classType = new ClassType(this, BT_StackType.ITEM_OBJECT);
		
		repository.addClass(this);
		
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		repository.releaseTableLock(this);
		
		try {
			int bracketIndex = nm.indexOf('[');
			
			// Do "BT_Repository.addClass" before "BT_Class.forName"
			arrayType = link ? repository.linkTo(nm.substring(0, bracketIndex))
							 : repository.forName(nm.substring(0, bracketIndex));

			// All arrays implement java.lang.Cloneable except in J2ME
			//for this reason we link to a stub and not the loaded classes
			BT_Class cloneable = repository.linkToStub(BT_Repository.JAVA_LANG_CLONEABLE);
			this.parents_.addUnique(cloneable); // this -> parent
			
			
			if(BT_Factory.multiThreadedLoading) {
				cloneable.kidsLock.lock();
			}
			cloneable.kids_.addUnique(this); // parent -> this
			if(BT_Factory.multiThreadedLoading) {
				cloneable.kidsLock.unlock();
			}
			
			// All arrays implement java.io.Serializable except in J2ME
			BT_Class serializable = repository.linkToStub(BT_Repository.JAVA_IO_SERIALIZABLE);
			this.parents_.addUnique(serializable); // this -> parent
			
			if(BT_Factory.multiThreadedLoading) {
				serializable.kidsLock.lock();
			}
			serializable.kids_.addUnique(this); // parent -> this
			if(BT_Factory.multiThreadedLoading) {
				serializable.kidsLock.unlock();
			}
			
			// All arrays' immediate superclasses are java.lang.Object
			setSuperClass(repository.findJavaLangObject());

			// All arrays have field "length"
			BT_Field.createField(
				this,
				(short) (BT_Field.PUBLIC | BT_Field.FINAL),
				repository.getInt(),
				"length");
					
			// All arrays have a public method "clone".  Make it native so code is not needed
			BT_Method.createMethod(
				this,
				(short) (BT_Item.PUBLIC | BT_Item.NATIVE),
				BT_MethodSignature.create("()Ljava/lang/Object;", repository),
				"clone");
			
			fields.trimToSize();
			methods.trimToSize();
			parents_.trimToSize();
		} finally {
			if(BT_Factory.multiThreadedLoading) {
				classLock.unlock();
			}
		}
	}
	
	void initClass(String name, BT_Repository repository) {
		initClass(name, PUBLIC, repository);
	}
	
	void initClass(String name, short flags, BT_Repository repository) {
		setFlags(flags);
		this.name = name;
		classType = new ClassType(this, BT_StackType.ITEM_OBJECT);
		setStub(true);
		isClass = !areAnyEnabled(INTERFACE);
		isInterface = !isClass;
		if (!isInterface && !name.equals(BT_Repository.JAVA_LANG_OBJECT)) {
			setSuperClass(repository.findJavaLangObject());
		}
		repository.addClass(this);
	}
	
	public BT_Method getFinalizerMethod() {
		BT_MethodVector methods = getMethods();
		for(int k=0; k<methods.size(); k++) {
			BT_Method method = methods.elementAt(k);
			if(method.isFinalizer()) {
				return method;
			}
		}
		return null;
	}
	
	public BT_Method getFinalizerMethod(boolean insert) {
		BT_Method finalizer = getFinalizerMethod();
		if(finalizer == null && insert) {
			finalizer = BT_Method.createMethod(
				this,
				(short) (BT_Item.PROTECTED),
				repository.basicSignature,
				"finalize");
			finalizer.makeCodeSimplyReturn();
		}
		return finalizer;
	}

	/**
	 Can this class be inherited by another class?
	 I.e., is it not final?
	 Doesn't check intermediate subclasses.
	 There is no canInherit() method since all classes can inherit.
	**/
	public boolean canBeInherited() {
		return !isFinal();
	}

	/**
	 Returns the .class file representation of this class.
	**/
	public byte[] bytes() throws BT_ClassWriteException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			write(dos);
		} catch (IOException e) {
			super.fatal(Messages.getString("JikesBT.I/O_error_somehow_while_writing_to_a_byte_array_20"));
		}
		return baos.toByteArray();
	}

	/**
	 Saves the class to the specified stream.
	 If a BT_ClassFileException is thrown, then no data has been written.
	
	 @throws  IOException  
	 @throws  BT_ClassFileException  The class cannot be written.
	**/
	public void write(DataOutputStream dos) throws IOException, BT_ClassWriteException {
		if (!inProject())
			throw new IllegalStateException(Messages.getString("JikesBT.attempt_to_write_an_array,_primitive,_or_system_class__21") + name
					+ " " + repository.factory.isProjectClass(getName(), null));
		
		// Build the constant pool
		resolve();
		
		if(BT_Misc.overflowsUnsignedShort(pool.size())) {
			throw new BT_ClassWriteException(Messages.getString("JikesBT.{0}_count_too_large_109", "constant pool entry"));
		}
		
		if (throwsClassFormatError()) {
			dos.writeInt(BAD_MAGIC);
		} else {
			dos.writeInt(MAGIC);
		}
		dos.writeShort(version.minorVersion);
		dos.writeShort(version.majorVersion);

		int ownPoolIndex = pool.indexOfClassRef(this);

		// Cause internal name of the class to be different to force correct exception
		if (throwsNoClassDefFoundError()) {
			pool.elementAt(pool.elementAt(ownPoolIndex).index1).strValue += "INVALID";
		}

		pool.write(dos);

		short f = getFlags();
		if (usesInvokeSpecial) {
			f |= SUPER;
		}
		else {
			f &= ~SUPER;
		}
		if (isInterface) {
			f |= INTERFACE;
		}
		else {
			f &= ~INTERFACE;
		}
		dos.writeShort(f);
		dos.writeShort(ownPoolIndex);

		// In the class file (but not in JikesBT) interfaces have superclass java.lang.Object.
		BT_Class superC = isInterface ? repository.findJavaLangObject() : superClass;
		if (superC == null) {
			if(invalidSuperClass != null) {
				dos.writeShort(pool.indexOfClassRef(invalidSuperClass));
			} else {
				dos.writeShort(0);
			}
		} else {
			dos.writeShort(pool.indexOfClassRef(superC));
		}
		
		// Implemented or super-interfaces ...
		int sz = parents_.size();
		int cni = (circularParents_ == null ? 0 : circularParents_.size());
		int ni = sz + cni - (getSuperClass() != null ? 1 : 0); // # interfaces
		
		if(BT_Misc.overflowsUnsignedShort(ni)) {
			throw new BT_ClassWriteException(Messages.getString("JikesBT.{0}_count_too_large_109", "super interfaces"));
		}
		
		dos.writeShort(ni);
		for (int i = 0; i < sz; ++i) { // Per parent
			if (parents_.elementAt(i) != getSuperClass()) {
				// Is an interface or undifferentiated stub
				dos.writeShort(pool.indexOfClassRef(parents_.elementAt(i)));
			}
		}
		for (int i = 0; i < cni; ++i) { // Per parent
			dos.writeShort(pool.indexOfClassRef(circularParents_.elementAt(i)));
		}
		// Note:  This cannot reorder fields since their initializations can be order-dependent.  Or is that only a Java language rule?
		// Note that stubs may not be counted
		int fieldcount = 0;
		for (int i = 0; i < fields.size(); ++i) {
			if (!fields.elementAt(i).isStub())
				fieldcount++;
		}
		if(BT_Misc.overflowsUnsignedShort(fieldcount)) {
			throw new BT_ClassWriteException(Messages.getString("JikesBT.{0}_count_too_large_109", "fields"));
		}
		dos.writeShort(fieldcount);
		for (int i = 0; i < fields.size(); ++i) {
			BT_Field fld = fields.elementAt(i);
			if (!fld.isStub())
				fld.write(dos, pool);
		}

		int methodcount = 0;
		for (int i = 0; i < methods.size(); ++i) {
			if (!methods.elementAt(i).isStub())
				methodcount++;
		}
		if(BT_Misc.overflowsUnsignedShort(methodcount)) {
			throw new BT_ClassWriteException(Messages.getString("JikesBT.{0}_count_too_large_109", "methods"));
		}
		dos.writeShort(methodcount);
		for (int i = 0; i < methods.size(); ++i) {
			BT_Method mtd = methods.elementAt(i);
			if (!mtd.isStub())
				mtd.write(dos, pool);
		}

		attributes.write(dos, pool); // attributes

		if (!repository.factory.keepConstantPool) {
			pool = null; // remove constant pool
		}
	}

	/**
	 Writes the class into the named file.
	
	 <p> See {@link BT_Repository#save}.
	**/
	public void write(String fileName) {
		try {
			DataOutputStream dos =
				new DataOutputStream(new FileOutputStream(fileName));
			write(dos);
			dos.close();
		} catch(BT_ClassWriteException e) {
			repository.factory.noteClassWriteException(name, fileName, e);
		} catch (IOException e) {
			repository.factory.noteClassWriteIOException(name, fileName, e);
		}
	}

	/**
	 Writes the class into a .class file.
	 If the class is in the anonymous package, it will be written to the
	 current directory.
	 If the class is in a named package, it will be written to the
	 subdirectory of the current directory as determined by the package name.
	
	 <p> See {@link BT_Repository#save}.
	**/
	public void write() {
		String dirAndName = name.replace('.', File.separatorChar);
		int pic = dirAndName.lastIndexOf(File.separatorChar);
		if (pic != -1) {
			String dir = dirAndName.substring(0, pic);
			(new File(dir)).mkdirs();
		}
		String fileName = dirAndName + ".class";
		write(fileName);
	}

	/**
	 Returns the opcode to convert between two primitive types.
	 @return 0 when this class or the other class are not primitive,
	   or when a conversion operation is not available.
	**/
	public int getOpcodeForConversionTo(BT_Class other) {
		if (this == other)
			return 0;
		if (isBasicTypeClass && other.isBasicTypeClass) {
			if (other.name.equals("long")) {
				if (name.equals("int"))
					return opc_i2l;
				if (name.equals("short"))
					return opc_i2l;
				if (name.equals("byte"))
					return opc_i2l;
				if (name.equals("char"))
					return opc_i2l;
				if (name.equals("float"))
					return opc_f2l;
				if (name.equals("double"))
					return opc_d2l;
				return 0;
			}
			if (other.name.equals("int")) {
				if (name.equals("short"))
					return 0;
				if (name.equals("byte"))
					return 0;
				if (name.equals("char"))
					return 0;
				if (name.equals("long"))
					return opc_l2i;
				if (name.equals("float"))
					return opc_f2i;
				if (name.equals("double"))
					return opc_d2i;
				return 0;
			}
			if (other.name.equals("short")) {
				if (name.equals("byte"))
					return 0;
				if (name.equals("char"))
					return 0;
				if (name.equals("int"))
					return 0;
				if (name.equals("long"))
					return opc_l2i;
				if (name.equals("float"))
					return opc_f2i;
				if (name.equals("double"))
					return opc_d2i;
				return 0;
			}
			if (other.name.equals("byte")) {
				if (name.equals("short"))
					return 0;
				if (name.equals("char"))
					return 0;
				if (name.equals("int"))
					return 0;
				if (name.equals("long"))
					return opc_l2i;
				if (name.equals("float"))
					return opc_f2i;
				if (name.equals("double"))
					return opc_d2i;
				return 0;
			}
			if (other.name.equals("char")) {
				if (name.equals("byte"))
					return 0;
				if (name.equals("short"))
					return 0;
				if (name.equals("int"))
					return 0;
				if (name.equals("long"))
					return opc_l2i;
				if (name.equals("float"))
					return opc_f2i;
				if (name.equals("double"))
					return opc_d2i;
				return 0;
			}
			if (other.name.equals("float")) {
				if (name.equals("int"))
					return opc_i2f;
				if (name.equals("short"))
					return opc_i2f;
				if (name.equals("byte"))
					return opc_i2f;
				if (name.equals("char"))
					return opc_i2f;
				if (name.equals("long"))
					return opc_l2f;
				if (name.equals("double"))
					return opc_d2f;
				return 0;
			}
			if (other.name.equals("double")) {
				if (name.equals("float"))
					return opc_f2d;
				if (name.equals("long"))
					return opc_l2d;
				if (name.equals("int"))
					return opc_i2d;
				if (name.equals("short"))
					return opc_i2d;
				if (name.equals("byte"))
					return opc_i2d;
				if (name.equals("char"))
					return opc_i2d;
				return 0;
			}
		}
		return 0;
	}

	/**
	 Returns whether this class or interface directly or indirectly is derived from or
	 implements the given class or interface.
	 I.e., whether it is an ancestor.
	 Identity results in _false_.
	 Assumes there are no cycles in the inheritence lattice.
	 @param  origC  Non-null conditional ancestor.
	**/
	public boolean isDescendentOf(BT_Class origC) {
		for (int i = 0; i < this.parents_.size(); i++) {
			BT_Class par = this.parents_.elementAt(i);
			if (par == origC || par.isDescendentOf(origC))
				return true;
		}
		return false;
	}

	/**
	 * determines whether the given class is a subclass of the current class
	 */
	public boolean isClassAncestorOf(BT_Class origC) {
		BT_Class superClass = origC.getSuperClass();
		while(superClass != null) {
			if(superClass == this) {
				return true;
			}
			superClass = superClass.getSuperClass();
		}
		return false;
	}
	
	/**
	 Same as {@link BT_Class#isDescendentOf} but in the other direction.
	**/
	public boolean isAncestorOf(BT_Class ck) {
		return ck.isDescendentOf(this);
	}
	
	public boolean isStubOrHasParentStub() {
		if(isStub()) {
			return true;
		}
		for(int i=0; i<parents_.size(); i++) {
			if(parents_.elementAt(i).isStubOrHasParentStub()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isStubOrHasSuperClassStub() {
		if(isStub()) {
			return true;
		}
		BT_Class clazz = getSuperClass();
		if(clazz != null) {
			return clazz.isStubOrHasParentStub();
		}
		return false;
	}

	/**
	 Returns whether this class or interface is derived from or
	 implements the given class or interface.
	 There is special handling for predefined types.
	 Identity results in _true_ for historical purposes.
	 {@link BT_Class#isDescendentOf} is faster and doesn't have special handling.
	 @param  origC  Conditional ancestor.  May be null.
	**/
	public boolean isDerivedFrom(BT_Class origC) {
		if (origC == null)
			return false;
		if (origC == this)
			return true;
		if (isBasicTypeClass) {
			if(!origC.isBasicTypeClass) {
				return false;
			}
			if (origC.name.equals("long")) {
				return name.equals("int") || name.equals("short") || name.equals("byte") || name.equals("char");
			}
			if (origC.name.equals("int")) {
				return name.equals("short") || name.equals("byte") || name.equals("char");
			}
			if (origC.name.equals("short")) {
				return name.equals("byte") || name.equals("char");
			}
			//if (origC.name.equals("byte")) {
			//	return name.equals("char");
			//}
			if (origC.name.equals("char")) {
				return name.equals("byte");
			}
			if (origC.name.equals("float")) {
				return name.equals("int") || name.equals("short") || name.equals("byte") || name.equals("char");
			}
			if (origC.name.equals("double")) {
				return name.equals("float") || name.equals("long") || name.equals("int") || name.equals("short")
					|| name.equals("byte") || name.equals("char");
			}
		}
		return isDescendentOf(origC);
	}

	BT_CreationSite findCreationSite(BT_Ins ins) {
		for (int n = creationSites.size() - 1; n >= 0; n--)
			if (creationSites.elementAt(n).instruction == ins)
				return creationSites.elementAt(n);
		return null;
	}
	
	/**
	 Adds a creation site for the given JVM "newarray" instruction.
	**/
	BT_CreationSite addCreationSite(BT_NewArrayIns ins, BT_CodeAttribute code) {
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.lock();
		}
		BT_CreationSite site = findCreationSite(ins);
		if(site == null) {
			site = new BT_CreationSite(code, ins);
			creationSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.unlock();
		}
		return site;
	}
	
	/**
	 Adds a creation site for the given JVM "new" instruction.
	**/
	BT_CreationSite addCreationSite(BT_NewIns ins, BT_CodeAttribute code) {
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.lock();
		}
		BT_CreationSite site = findCreationSite(ins);
		if(site == null) {
			site = new BT_CreationSite(code, ins);
			creationSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.unlock();
		}
		return site;
	}
	
	/**
	 Adds a creation site for the given JVM "anewarray" instruction.
	**/
	BT_CreationSite addCreationSite(BT_ANewArrayIns ins, BT_CodeAttribute code) {
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.lock();
		}
		BT_CreationSite site = findCreationSite(ins);
		if(site == null) {
			site = new BT_CreationSite(code, ins);
			creationSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.unlock();
		}
		return site;
	}
	
	/**
	 Adds a creation site for the given JVM "multianewarray" instruction.
	**/
	BT_CreationSite addCreationSite(BT_MultiANewArrayIns ins, BT_CodeAttribute code) {
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.lock();
		}
		BT_CreationSite site = findCreationSite(ins);
		if(site == null) {
			site = new BT_MultiCreationSite(code, ins, this);
			creationSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			creationSiteLock.unlock();
		}
		return site;
	}

	/**
	 Finds the creation sites for this class that contains the
	 provided instruction, and removes them.
	**/
	void removeCreationSite(BT_Ins ins) {
		for (int n = creationSites.size() - 1; n >= 0; n--)
			if (creationSites.elementAt(n).instruction == ins)
				creationSites.removeElementAt(n);
	}
	
	private BT_ClassReferenceSite findReferenceSite(BT_Ins ins) {
		for (int n = referenceSites.size() - 1; n >= 0; n--)
			if (referenceSites.elementAt(n).instruction == ins)
				return referenceSites.elementAt(n);
		return null;
	}
	
	/**
	 Adds a class reference site for the given JVM checkcast, instanceof, 
	 new, anewarray, or multianewarray instruction.
	**/
	BT_ClassReferenceSite addReferenceSite(BT_ClassRefIns ins, BT_CodeAttribute code) {
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.lock();
		}
		BT_ClassReferenceSite site = findReferenceSite(ins);
		if(site == null) {
			site = new BT_ClassReferenceSite(code, ins);
			referenceSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.unlock();
		}
		return site;
	}
	
	/**
	 Adds a class reference site for the given ldc, ldc_w instruction.
	**/
	BT_ClassReferenceSite addReferenceSite(BT_ConstantStringIns ins, BT_CodeAttribute code) {
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.lock();
		}
		BT_ClassReferenceSite site = findReferenceSite(ins);
		if(site == null) {
			site = new BT_ClassReferenceSite(code, ins, repository.findJavaLangClass());
			referenceSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.unlock();
		}
		return site;
	}
	
	/**
	 Adds a class reference site for the given ldc, ldc_w instruction.
	**/
	BT_ClassReferenceSite addReferenceSite(BT_ConstantClassIns ins, BT_CodeAttribute code) {
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.lock();
		}
		BT_ClassReferenceSite site = findReferenceSite(ins);
		if(site == null) {
			site = new BT_ClassReferenceSite(code, ins, repository.findJavaLangClass());
			referenceSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.unlock();
		}
		return site;
	}
	
	/**
	 Adds the given class reference site for the given JVM checkcast, instanceof, ldc, ldc_w, 
	 new, anewarray, or multianewarray instruction.
	**/
	BT_ClassReferenceSite addReferenceSite(BT_ClassReferenceSite site) {
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.lock();
		}
		BT_ClassReferenceSite existingSite = findReferenceSite(site.instruction);
		if(existingSite == null) {
			existingSite = site;
			referenceSites.addElement(site);
		}
		if(BT_Factory.multiThreadedLoading) {
			referenceSiteLock.unlock();
		}
		return existingSite;
	}

	/**
	 Finds the class reference sites for this class that contains the
	 provided instruction, and removes them.
	**/
	void removeClassReferenceSite(BT_Ins ins) {
		for (int n = referenceSites.size() - 1; n >= 0; n--) {
			if (referenceSites.elementAt(n).instruction == ins) {
				referenceSites.removeElementAt(n);
			}
		}
	}
	
	final void addReferencingArrayClass(BT_Class clazz) {
		if(!repository.factory.trackClassReferences) {
			return;
		}
		if(BT_Factory.multiThreadedLoading) {
			arrayTypeLock.lock();
		}
		boolean found = false;
		if(asArrayTypes == null) {
			asArrayTypes = new BT_ClassVector();
		} else for (int n = asArrayTypes.size() - 1; n >= 0; n--)
			if (asArrayTypes.elementAt(n).equals(clazz)) {
				found = true;
				break;
			}
		if(!found) {
			asArrayTypes.addElement(clazz);
		}
		if(BT_Factory.multiThreadedLoading) {
			arrayTypeLock.unlock();
		}
	}
	
	final void removeReferencingArrayClass(BT_Class clazz) {
		if(asArrayTypes != null) {
			for (int n = asArrayTypes.size() - 1; n >= 0; n--) {
				if (asArrayTypes.elementAt(n).equals(clazz)) {
					asArrayTypes.removeElementAt(n);
				}
			}
		}
	}
	
	public final void addReferencingAttribute(BT_Attribute att) {
		if(isPrimitive() || !repository.factory.trackClassReferences) {
			return;
		}
		if(BT_Factory.multiThreadedLoading) {
			referencingAttributeLock.lock();
		}
		boolean found = false;
		if(referencingAttributes == null) {
			referencingAttributes = new BT_AttributeVector();
		} else for (int n = referencingAttributes.size() - 1; n >= 0; n--)
			if (referencingAttributes.elementAt(n).equals(att)) {
				found = true;
				break;
			}
		if(!found) {
			referencingAttributes.addElement(att);
		}
		if(BT_Factory.multiThreadedLoading) {
			referencingAttributeLock.unlock();
		}
	}
	
	public final void removeReferencingAttribute(BT_Attribute att) {
		if(referencingAttributes != null) {
			for (int n = referencingAttributes.size() - 1; n >= 0; n--) {
				if (referencingAttributes.elementAt(n).equals(att)) {
					referencingAttributes.removeElementAt(n);
				}
			}
		}
	}
	
	final void addReferencingField(BT_Field field) {
		if(isPrimitive() || !repository.factory.trackClassReferences) {
			return;
		}
		boolean found = false;
		if(BT_Factory.multiThreadedLoading) {
			fieldTypeLock.lock();
		}
		if(asFieldTypes == null) {
			asFieldTypes = new BT_HashedFieldVector();
		} else for (int n = asFieldTypes.size() - 1; n >= 0; n--) {
			if (asFieldTypes.elementAt(n).equals(field)) {
				found = true;
				break;
			}
		}
		if(!found) {
			asFieldTypes.addElement(field);
		}
		if(BT_Factory.multiThreadedLoading) {
			fieldTypeLock.unlock();
		}
	}
	
	final void removeReferencingField(BT_Field field) {
		if(isPrimitive()) {
			return;
		}
		if(BT_Factory.multiThreadedLoading) {
			fieldTypeLock.lock();
		}
		if(asFieldTypes != null) {
			for (int n = asFieldTypes.size() - 1; n >= 0; n--) {
				if (asFieldTypes.elementAt(n).equals(field)) {
					asFieldTypes.removeElementAt(n);
				}
			}
		}
		if(BT_Factory.multiThreadedLoading) {
			fieldTypeLock.unlock();
		}
	}
	
	final void addReferencingSignature(BT_Method method, short index) {
		if(isPrimitive()) {
			return;
		}
		if(BT_Factory.multiThreadedLoading) {
			signatureTypeLock.lock();
		}
		boolean found = false;
		if(asSignatureTypes == null) {
			asSignatureTypes = new BT_SignatureSiteVector();
		} else for (int n = asSignatureTypes.size() - 1; n >= 0; n--) {
			if (asSignatureTypes.elementAt(n).equals(method, index)) {
				found = true;
				break;
			}
		}
		if(!found) {
			asSignatureTypes.addElement(new BT_SignatureSite(method, index, this));
		}
		if(BT_Factory.multiThreadedLoading) {
			signatureTypeLock.unlock();
		}
	}
	
	final void removeReferencingSignature(BT_Method method, short index) {
		if(isPrimitive()) {
			return;
		}
		if(asSignatureTypes != null) {
			for (int n = asSignatureTypes.size() - 1; n >= 0; n--) {
				if (asSignatureTypes.elementAt(n).equals(method, index)) {
					asSignatureTypes.removeElementAt(n);
				}
			}
		}
	}

	void handleDereference() {
		try {
			dereference();
		} catch (RuntimeException e) {
			setThrowsVerifyErrorTrue();
			BT_ClassPathLocation location = loadedFromEntry;
			BT_ClassPathEntry entry = location == null ? null : location.getClassPathEntry();
			repository.factory.noteClassLoadFailure(repository, entry, this, getName(), loadedFrom, e, BT_Repository.JAVA_LANG_VERIFY_ERROR);
		} catch (BT_ClassFileException e) {
			setThrowsVerifyErrorTrue();
			String error = e.getEquivalentRuntimeError();
			BT_ClassPathLocation location = loadedFromEntry;
			BT_ClassPathEntry entry = location == null ? null : location.getClassPathEntry();
			repository.factory.noteClassLoadFailure(repository, entry, this, getName(), loadedFrom, e, 
					error != null ? error : BT_Repository.JAVA_LANG_VERIFY_ERROR);
		} finally {
			loadedFromEntry = null;
		}
	}

	/**
	 Links the class and its members to other objects with which it has a
	 relationship.
	 Note that some linkage is done before this is called, primarily
	 from classes, methods, and fields to the classes named in their
	 signatures.
	
	 <p> Nothing is done for classes that are not in the project.
	
	 <p> Whether inter-method relationships are built and maintained is
	 affected by {@link BT_Factory#buildMethodRelationships}.
	
	 <p> This is normally called internally while JikesBT reads a class file.
	
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>
	**/
	protected void dereference() throws BT_ClassFileException {
		BT_ClassFileException exception = null;
		if (inProject()) {
			for (int n = 0; n < methods.size(); n++) {
				BT_Member member = methods.elementAt(n);
				try {
					member.dereference();
				} catch (BT_ClassFormatRuntimeException e) {
					if (exception == null) {
						exception = new BT_ClassFileException(e);
					}
				} catch(BT_ClassFileException e) {
					if (exception == null) {
						exception = e;
					}
				}
			}
			
			for (int n = 0; n < fields.size(); n++) {
				BT_Member member = fields.elementAt(n);
				try {
					member.dereference();
				} catch (BT_ClassFormatRuntimeException e) {
					if (exception == null) {
						exception = new BT_ClassFileException(e);
					}
				} catch(BT_ClassFileException e) {
					if (exception == null) {
						exception = e;
					}
				}
			}
		}
		// Want to compute parents even for system classes
		if (repository.factory.buildMethodRelationships) {
			buildMethodRelationships();
		}
		try {
			attributes.dereference(this, repository);
		} catch (BT_ClassFormatRuntimeException e) {
			if (exception == null) {
				exception = new BT_ClassFileException(e);
			}
		} catch(BT_ClassFileException e) {
			if (exception == null) {
				exception = e;
			}
		}

		if (!repository.factory.keepConstantPool) {
			pool = null; // Allow gc
		}
		
		//we can save memory by trimming private methods and fields
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			if(field.isPrivate()) {
				field.trimToSize();
			}
		}
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(method.isPrivate()) {
				method.trimToSize();
			}
		}
		if (exception != null) {
			throw exception;
		}
	}
	
	/**
	 Removes any relationships established when dereferenced.  Also removes certain 
	 relationships established when created, namely the visibility of the class as a child
	 of each one of its parents.
	 **/
	public void remove() {
		if(isBasicTypeClass) {
			return;
		}
		
		if(arrayType == null) {
			for (int n = 0; n < methods.size(); n++) {
				methods.elementAt(n).remove();
			}
				
			for (int n = 0; n < fields.size(); n++) {
				fields.elementAt(n).remove();
			}
		} else {
			arrayType.removeReferencingArrayClass(this);
		}
		
		if(asArrayTypes != null) {
			for(int i=0; i<asArrayTypes.size(); i++) {
				BT_Class arrayClass = asArrayTypes.elementAt(i);
				arrayClass.remove();
			}
		}
		for(int i=0; i<kids_.size(); i++) {
			BT_Class kidClass = kids_.elementAt(i);
			if(!isInterface || kidClass.isInterface()) {
				kidClass.remove();
			}
			else {
				kidClass.parents_.removeElement(this);
			}
		}
		 
		//do nothing for fields of this class type
//		BT_Class javaLangObject = cls.repository.findJavaLangObject();
//		for(int i=0; i<asFieldTypes.size(); i++) {
//			BT_Field fieldType = asFieldTypes.elementAt(i);
//			fieldType.type = javaLangObject; //also change javaLangObject.asFieldType
//		}
		
		//remove any cached signatures, but do not alter any signatures that
		//contain this class
		java.util.Hashtable signatures = repository.signatures;
		if(asSignatureTypes != null) {
			for(int i=0; i<asSignatureTypes.size(); i++) {
				BT_SignatureSite site = asSignatureTypes.elementAt(i);
				BT_MethodSignature sig = site.getSignature();
				String sigString = sig.toString();
				signatures.remove(sigString);
			}
		}
		
		for(int i=referenceSites.size() - 1; i>= 0; i--) {
			BT_ClassReferenceSite site = referenceSites.elementAt(i);
			site.instruction.unlink(site.from);
			//the instruction target remains the same, it will throw an error if executed...
		}
		
		for(int i=creationSites.size() - 1; i>= 0; i--) {
			BT_CreationSite site = creationSites.elementAt(i);
			site.instruction.unlink(site.from);
			//the instruction target remains the same, it will throw an error if executed...
		}
		
		for(int j=0; j<parents_.size(); j++) {
			BT_Class parent = parents_.elementAt(j);
			parent.getKids().removeElement(this);
		}
			
		if(referencingAttributes != null) {
			BT_AttributeVector referencingAttributes = (BT_AttributeVector) this.referencingAttributes.clone();
			for(int i=0; i<referencingAttributes.size(); i++) {
				BT_Attribute att = referencingAttributes.elementAt(i);
				att.removeReference(this);
			}
		}
		
		attributes.remove();
		
		repository.removeClass(this);
		
		setStub(true);
	}

	/**
	 Set the name of this class.
	 This also sets the name of any classes that represent arrays of this
	 class.
	 Names of classes representing arrays, primitive types, or
	 java.lang.Object cannot be set.
	 The name cannot be set to the name of a class that already exists
	 in the repository.
	 @param newName The name of the item in Java language format
	   (e.g. "mypackage.myclass")
	 @see BT_Item#setName
	**/
	public void setName(String newName) {
		
		// If this class is in the repository, update the class table
		if (repository.getClass(name) != null) {
			repository.renameClass(this, newName);
		}
		if(asArrayTypes != null) {
			for(int n=0; n<asArrayTypes.size(); n++) {
				BT_Class c = asArrayTypes.elementAt(n);
				String newArrayName = newName + c.name.substring(c.name.indexOf('['));
				//no need to reset dimensionCount in c since it does not change here
				c.setName(newArrayName);
			}
		}
		name = newName;
		cachedPackageName = null;
		
		//remove caching of signature names
		if(asSignatureTypes != null) {
			for(int n=0; n<asSignatureTypes.size(); n++) {
				BT_SignatureSite site = asSignatureTypes.elementAt(n);
				site.getSignature().resetStringCache(repository);
			}
		}
	}

	
	/**
	 Does a {@link BT_Class#setName} and updates related information (now there is none).
	 Is a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>
	 (except it won't do special handling for special names such as java.lang.Object).
	 This exists only to be a safer version of setName in case of future changes.
	
	 <p> Cannot rename java.lang.Object since that would result in an
	 inconsistent model, since its superclass would have to be added
	 or deleted.
	**/
	public void resetName(String newName) {
		setName(newName);
	}

	/**
	 * read the constant pool
	 */
	public void resolve() throws BT_ClassWriteException {
		if (!inProject() || isStub())
			return;
		pool = repository.createConstantPool();
		pool.setClass(this);

		/* 
		 * optimization
		 * 
		 * Here we resolve instructions of type BT_ConstantStringIns, BT_ConstantClassIns, BT_ConstantFloatIns and BT_ConstantIntegerIns first,
		 * which vary depending upon their constant pool indices (if they end up as opcodes ldc_w or ldc_w),
		 *  so doing them first reduces class file size
		 * 
		 */
		for (int i = 0; i < methods.size(); ++i) {
			BT_CodeAttribute code = methods.elementAt(i).getCode();
			if(code == null) {
				continue;
			}
			BT_InsVector ins = code.getInstructions();
			for (int n = 0; n < ins.size(); n++) {
				BT_Ins in1 = ins.elementAt(n);
				if(in1.isConstantIns() && !(in1.isDoubleWideConstantIns())) {
					in1.resolve(code, pool);
				}
			}
		}
		/* end optimization */
		
		
		
		pool.indexOfClassRef(this);
		if (superClass != null) {
			pool.indexOfClassRef(superClass);
		} else if(invalidSuperClass != null) {
			pool.indexOfClassRef(invalidSuperClass);
		}
		for (int i = 0; i < parents_.size(); ++i) {
			pool.indexOfClassRef(parents_.elementAt(i));
		}
		if (circularParents_ != null) {
			for (int i = 0; i < circularParents_.size(); ++i) {
				pool.indexOfClassRef(circularParents_.elementAt(i));
			}
		}
		// In the class file (but not in JikesBT), interfaces have superClass "java.lang.Object"
		if (isInterface) {
			pool.indexOfClassRef(repository.findJavaLangObject());
		}
		for (int i = 0; i < fields.size(); ++i) {
			fields.elementAt(i).resolve();
		}
		for (int i = 0; i < methods.size(); ++i) {
			methods.elementAt(i).resolve();
		}
		resolveFlags();
		attributes.resolve(this, pool);
		pool.lock();
	}
	
	//TODO should relink the method with its parents and children
	public BT_Method addStubMethod(String methodName, BT_MethodSignature signature) {
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		short codeType = isInterface ? BT_Method.ABSTRACT : 0;
		BT_Method result =
			BT_Method.createMethod(
				this,
				(short) (BT_Method.PUBLIC | codeType),
				signature,
				methodName);
		result.setStub(true);
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		return result;
	}

	public BT_Field addStubField(String fieldName, BT_Class type) {
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		BT_Field result =
			BT_Field.createField(this, BT_Item.PUBLIC, type, fieldName);
		result.setStub(true);
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		return result;
	}

	/**
	 Finds a given method in this class.
	
	 @param mName      The method name.
	 @param extArgs    The method arguments, e.g. "(java.lang.String, int, boolean)".
	 @return           Never null.
	 @throws  BT_NoSuchMethodException when method not found
	**/
	public BT_Method findMethod(String mName, String extArgs) 
			throws BT_NoSuchMethodException, BT_DescriptorException {
		BT_MethodSignature sig = null;
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		BT_Method result = null;
		for (int n = 0; n < methods.size(); n++) {
			BT_Method m = methods.elementAt(n);
			if (m.name.equals(mName)) {
				if(sig == null) {
					sig = BT_MethodSignature.create(
							m.getSignature().returnType.name,
							extArgs, repository);
				}
				if(m.getSignature().equals(sig)) {
					result = m;
					break;
				}
			}
		}
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		if(result != null) {
			return result;
		}
		throw new BT_NoSuchMethodException(
			Messages.getString("JikesBT.{0}{1}_in_{2}_131", new Object[] {mName, extArgs, name}));		
	}

	/**
	 Finds the given methods in this class (not inherited from
	 superclasses) matching the given name.
	
	 @param mName      The method name.
	 @return           Never null.
	**/
	public BT_MethodVector findMethods(String mName) {
		return methods.findMethods(mName);
	}
	
	/**
	 Finds a given method in this class (not inherited from superclasses) or
	 returns null.
	
	 @param mName      The method name
	 @param sig        The method signature
	 @return           Null if not found.
	
	**/
	public BT_Method findMethodOrNull(
		String mName,
		BT_MethodSignature sig) {
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		BT_Method m = methods.findMethod(mName, sig);
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		return m;
	}

	/**
	 Finds a given method in this class (not inherited from
	 superclasses) or throws.
	
	 @param mName      The method name
	 @param sig        The method signature
	 @return           Never null.
	 @throws  BT_NoSuchMethodException when method not found
	
	 @see #findMethodOrNull(String mName, BT_MethodSignature sig)
	**/
	public BT_Method findMethod(String mName, BT_MethodSignature sig) throws BT_NoSuchMethodException {
		BT_Method m = findMethodOrNull(mName, sig);
		if (m == null) {
			throw new BT_NoSuchMethodException(
				Messages.getString("JikesBT.{0}_{1}_in_{2}_132", new Object[] {mName, sig, name}));
		}
		return m;
	}

	/**
	 Finds a field with the given name in this class.
	
	 @param  fieldName  The simple field name (no dots).
	 @param  fieldType  The type of the field.
	 @return  Null if not found.
	
	 @see  #findField
	 @see  BT_Repository#findField
	**/
	public BT_Field findFieldOrNull(
		String fieldName,
		BT_Class fieldType) {
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		BT_Field f = fields.findField(fieldName, fieldType);
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		return f;
	}

	/**
	 Searches for a given method in this class or its ancestor classes (not in
	 its ancestor interfaces), or in this interface and its ancestor interfaces.
	 Does not support arrays or primitives.
	
	 @param mName    The method name
	 @param sig      The method signature
	 @return         Null or the method.
	 @see BT_Repository#findInheritedMethod
	**/
	public BT_Method findInheritedMethod(String mName, BT_MethodSignature sig) {
		return findInheritedMethod(mName, sig, false);
	}
	
	public BT_Method findInheritedMethod(String mName, BT_MethodSignature sig, boolean allowStub) {
		String sigString = sig.toString();
		String key = methods.getKey(mName, sigString);
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		BT_Method m = findInheritedMethod(mName, sigString, key, allowStub);
		if(m == null && isInterface()) {
			BT_Class javaLangObject = repository.findJavaLangObject();
			m = javaLangObject.methods.findMethod(mName, sig);
		}
		if(m != null && !(m.methodType instanceof BT_MethodSignature)) {
			//if the method is not dereferenced yet we can save a lot of time by setting the signature now
			m.methodType = sig;
		}
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		return m;
	}
	
	private BT_Method findInheritedMethod(
		String mName,
		String sig,
		String key,
		boolean allowStub) {
		
		BT_Method m = methods.findMethod(mName, sig.toString(), key);
		if (m != null && (!m.isStub() || allowStub)) {
			return m; // Defined in this class
		}
		
		//method resolution, VM spec 5.4.3.3
		//we check the class, all superclasses and all superinterfaces
		
		//interface method resolution, VM spec 5.4.3.4
		//we check the interface, all superinterfaces, and java.lang.Object
		//checking java.lang.Object is done above in the calling method findInheritedMethod
		//since in JikesBT interfaces have a null superclass
		
		BT_Class supercls = getSuperClass();
		if (supercls != null) {
			m = supercls.findInheritedMethod(mName, sig, key, allowStub);
			if (m != null && (!m.isStub() || allowStub)) {
				return m;
			}
		}
		
		// Look up the method in its interfaces.
		BT_ClassVector parents = this.getParents();
		for (int i = 0; i < parents.size(); i++) {
			BT_Class pc = parents.elementAt(i);
			if (pc.isInterface()) {
				/* super interfaces might not have been loaded yet */
				if(pc.notLoaded) {
					repository.forName(pc.getName());
				}
				m = pc.findInheritedMethod(mName, sig, key, allowStub);
				if (m != null && (!m.isStub() || allowStub)) {
					return m;
				}
			}
		}
		
		return null;
	}

	/**
	 Finds a field with the given name and type in this class.
	
	 @param  fieldName  The simple field name (no dots).
	 @return  Never null.
	 @throws  BT_NoSuchFieldException when field not found.
	
	 @see BT_Repository#findField
	**/
	public BT_Field findField(String fieldName, BT_Class type) throws BT_NoSuchFieldException {
		BT_Field field = findFieldOrNull(fieldName, type);
		if (field == null)
			throw new BT_NoSuchFieldException(
				Messages.getString("JikesBT.{0}_in_{1}_134", new Object[] {fieldName, name}));
		return field;
	}

	/**
	 Searches for a given field in this class or interface, or in its
	 ancestors (including in ancestor interfaces).
	
	 
	 @param fName    The field name
	 @param fieldType     The field type
	 @return         Null or the field.
	**/
	public BT_Field findInheritedField(String fName, BT_Class fieldType) {
		return findInheritedField(fName, fieldType, false);
	}
	
	/**
	 * 
	 * @param throwStubException if true, will abort by throwing a BT_StubLookupException
	 * 	whenever lookup requires searching a stub's superclass or superinterface 
	 */
	protected BT_Field findInheritedField(
			String fName,
			BT_Class fieldType,
			boolean allowStub) {
		String key = fields.getKey(fName, fieldType.name);
		if(BT_Factory.multiThreadedLoading) {
			classLock.lock();
		}
		BT_Field f = findInheritedField(fName, fieldType.name, key, allowStub);
		if(f != null && !(f.fieldType instanceof BT_Class)) {
			//the type might not be dereferenced yet, but we can save time since we know the type already
			f.setFieldType(fieldType);
		}
		if(BT_Factory.multiThreadedLoading) {
			classLock.unlock();
		}
		return f;
	}
	
	private BT_Field findInheritedField(
		String fName,
		String fieldTypeName,
		String key,
		boolean allowStub) {
		
		BT_Field f = fields.findField(fName, fieldTypeName, key);
		if(f != null && (!f.isStub() || allowStub)) {
			return f;
		}
		
		/*
		 * field resolution, VM spec 5.4.3.2
		 * we check the class/interface, all superinterfaces and superclasses
		 * direct superinterfaces first, then superclasses
		 */
		for(int i=0; i<parents_.size(); i++) {
			BT_Class parent = parents_.elementAt(i);
			if(parent.isInterface()) {
				/* super interfaces might not have been loaded yet */
				if(parent.isStub() && parent.notLoaded) {
					repository.forName(parent.getName());
				}
				f = parent.findInheritedField(fName, fieldTypeName, key, allowStub);
				if(f != null && (!f.isStub() || allowStub)) {
					return f;
				}
			}
		}
		BT_Class superClass = getSuperClass();
		if(superClass != null) {
			f = superClass.findInheritedField(fName, fieldTypeName, key, allowStub);
			if(f != null && (!f.isStub() || allowStub)) {
				return f;
			}
		}
		return null;
	}
	
	/**
	 Reads remaining class from a class file (after we read the header before).
	 Part of {@link BT_Class#forName}.
	
	 @param  dis   An input stream from which class is read.
	 @param  file  A java.io.File or a java.util.zip.ZipFile.
	 @param  classInfo  Info in the part of the class that has already been read.
	**/
	final void readAfterName(
		DataInputStream dis,
		BT_ClassInfoUntilName classInfo,
		Object file)
			throws BT_ClassFileException, IOException {
		version.minorVersion = classInfo.minorVersion;
		version.majorVersion = classInfo.majorVersion;
		pool = classInfo.pool;
		pool.setClass(this);
		setFlags(classInfo.flags);
		isClass = !areAnyEnabled(INTERFACE);
		isInterface = !isClass;
		usesInvokeSpecial = areAnyEnabled(SUPER) || version.invokeSpecialSemanticsMandatory();
		BT_ClassPathEntry loadedFromCPEntry = loadedFromEntry == null ? null : loadedFromEntry.getLocation().getClassPathEntry();
		if (BT_Factory.strictVerification) {
			if ((isInterface() && !isAbstract() && version.interfacesAbstract()) ||
				(isInterface() && isEnum()) ||
				(isInterface() && areAnyEnabled(SUPER) && version.invokeSpecialSemantics()) || 
				(isAbstract() && isFinal()) ||
				(isAnnotation() && !isInterface()) ||
				(isPackageAnnotation() && version.packageAnnotationIsSynthetic() && !isSynthetic()) 
				) {
					throw new BT_ClassFileException(Messages.getString("JikesBT.invalid_combination_of_class_modifiers_135") + ": " + flagString() + ": " + version);
			}
		}
		try {
			int superClassIndex = dis.readUnsignedShort();
			if (superClassIndex != 0) {
				if (name.equals(BT_Repository.JAVA_LANG_OBJECT)) {
					throw new BT_ClassFileException(
						Messages.getString("JikesBT.Class_{0}_has_superindex_of_0_but_is_not_java.lang.Object_136", name));
				}
				String scn = pool.getClassNameAt(superClassIndex, BT_ConstantPool.CLASS);
				BT_Class superClass = repository.forName(scn);
				
				if(BT_Factory.multiThreadedLoading) {
					superClass.classLock.lock();
				}
				
				if (superClass.isStub()) {
					superClass.becomeClass();
				}
				
				// For backward-compatability
				if (isInterface) { // Suppress the super-class from the class file
					//in JIKESBT interfaces have a null super-class
					setSuperClass(null);
					if (!superClass.name.equals(BT_Repository.JAVA_LANG_OBJECT))
						throw new BT_ClassFileException(
							Messages.getString("JikesBT.Interface__140")
								+ name
								+ Messages.getString("JikesBT._does_not_have_superclass_java.lang.Object_in_the_class_file_141"));
				} // Suppress the super-class from the class file
				else { // Can have a real super-class
					if (superClass == this || superClass.isDescendentOf(this)) {
						setThrowsClassCircularityError();
						invalidSuperClass = superClass;
						repository.factory.noteClassLoadError(
								loadedFromCPEntry,
								this,
								name,
								file.toString(),
								Messages.getString("JikesBT.Class_circularity__0_149", superClass.fullName()),
								BT_Repository.JAVA_LANG_CLASS_CIRCULARITY_ERROR);
					} else if(superClass.isInterface) {
						setThrowsIncompatibleClassChangeError();
						invalidSuperClass = superClass;
						repository.factory.noteClassLoadError( 
								loadedFromCPEntry,
								this,
								name,
								file.toString(),
								Messages.getString("JikesBT.Extending_interface__0_148", superClass.fullName()),
								BT_Repository.JAVA_LANG_INCOMPATIBLE_CLASS_CHANGE_ERROR);
					} else {
						setSuperClass(superClass);
					}
				} // Can have a real super-class
				
				if(BT_Factory.multiThreadedLoading) {
					superClass.classLock.unlock();
				}
				
			} else {
				if (!name.equals(BT_Repository.JAVA_LANG_OBJECT)) {
					throw new BT_ClassFileException(
						Messages.getString("JikesBT.Class__143")
							+ name
							+ Messages.getString("JikesBT._has_superindex_of_non_0_but_is_java.lang.Object_144"));
				}
			}
	
			int count = dis.readUnsignedShort();
			parents_.ensureCapacity(count); // prevent unnecessary array copying
			for (int i = 0; i < count; ++i) { // Per implemented or super-interface
				String itfName = pool.getClassNameAt(dis.readUnsignedShort(), BT_ConstantPool.CLASS);
				BT_Class itf = repository.linkToSuperInterface(itfName, loadedFromEntry);
				
				if(BT_Factory.multiThreadedLoading) {
					itf.classLock.lock();
				}
				
				if (itf.isStub()) { // && !itf.isClass) // A stub not known to be a class
					itf.becomeInterface();
				}
				if (!itf.isInterface) {
						setThrowsIncompatibleClassChangeError();
						repository.factory.noteClassLoadError( 
							loadedFromCPEntry,
							this,
							name,
							file.toString(),
							Messages.getString("JikesBT.Implementing_class__0_145", itf.fullName()),
							BT_Repository.JAVA_LANG_INCOMPATIBLE_CLASS_CHANGE_ERROR);
				} else { // Is an interface
					if (itf == this || itf.isDescendentOf(this)) {   
						// Need to mark to avoid certain processing like preverification.
						setThrowsClassCircularityError();
						repository.factory.noteClassLoadError( 
								loadedFromCPEntry,
								this,
								name,
								file.toString(),
								Messages.getString("JikesBT.Class_circularity__0_149", itf.fullName()),
								BT_Repository.JAVA_LANG_CLASS_CIRCULARITY_ERROR);
						if (circularParents_ == null) {
							circularParents_ = new BT_ClassVector();
						}
						circularParents_.addElement(itf);
					} else {
						parents_.addElement(itf);
						
						if(BT_Factory.multiThreadedLoading) {
							itf.kidsLock.lock();
						}
						itf.kids_.addElement(this);
						if(BT_Factory.multiThreadedLoading) {
							itf.kidsLock.unlock();
						}
					}
				} // Is an interface
				
				if(BT_Factory.multiThreadedLoading) {
					itf.classLock.unlock();
				}
				
			} // Per implemented or super-interface
		} catch(BT_ConstantPoolException e) {
			throw new BT_ClassFileException(e);
		}
		// Fields and methods remain in the same order as in the classfile.
		readFields(dis);
		readMethods(dis);
		attributes = BT_AttributeVector.read(dis, pool, this, this, repository, loadedFromEntry);
		if (BT_Factory.strictVerification && dis.read() != -1) {
			throw new BT_ClassFileException(Messages.getString("JikesBT.extra_bytes_after_end_of_class_file_147"));
		}
	}

	/**
	 * trims all vectors related to this class that grow as new classes are loaded.  Calling this method
	 * when all loading is complete will release unused memory.
	 */
	public void trimToSize() {
		if(asArrayTypes != null) {
			asArrayTypes.trimToSize();
		}
		if(asFieldTypes != null) {
			asFieldTypes.trimToSize();
		}
		if(asSignatureTypes != null) {
			asSignatureTypes.trimToSize();
		}
		if(referencingAttributes != null) {
			referencingAttributes.trimToSize();
		}
		creationSites.trimToSize();
		referenceSites.trimToSize();
		kids_.trimToSize();
		//the fields, methods and parents vectors need not be trimmed
		//because they are adjusted to the right size
		//when the class is loaded
		for(int i=0; i<fields.size(); i++) {
			fields.elementAt(i).trimToSize();
		}
		for(int i=0; i<methods.size(); i++) {
			methods.elementAt(i).trimToSize();
		}
	}
	
	private void readFields(DataInputStream dis)
		throws IOException, BT_ClassFileException {
		BT_FieldTable newFields = new BT_FieldTable();

		int count = dis.readUnsignedShort();
		newFields.ensureCapacity(count);
		for (int i = 0; i < count; i++) {
			BT_Field newField = repository.createField(this);
			newField.read(dis, pool, repository, loadedFromEntry);

			// If the field already existed, we replace its contents.
			BT_Field oldField = null;
			if ((oldField = fields.findField(newField.name, newField.getTypeName()))
				!= null) {
				oldField.replaceContents(newField);
				newField = oldField;
			}
			if (newFields.findField(newField.name, newField.getTypeName()) != null) {
				throw new BT_ClassFileException(
					"duplicate field definition " + newField.name);
			}
			newFields.addElement(newField);
		}

		// See if there are any stubs left, and add them at the end of the vector.
		for (int i = 0; i < fields.size(); i++) {
			BT_Field oldField = fields.elementAt(i);
			if (oldField.isStub())
				newFields.addElement(oldField);
		}
		// Finally, swap old for new
		fields = newFields;
	}

	public boolean isJavaLangObject() {
		return equals(repository.findJavaLangObject());
	}
	
	public boolean isPackageAnnotation() {
		return classNameWithoutPackage().equals("package-info");
	}
	
	private void readMethods(DataInputStream dis)
		throws IOException, BT_ClassFileException {
		BT_MethodTable newMethods = new BT_MethodTable();

		boolean isJavaLangObject = isJavaLangObject();
		boolean foundJavaLangObjectFinalizer = false;
		int count = dis.readUnsignedShort();
		newMethods.ensureCapacity(count);
		for (int i = 0; i < count; i++) {
			BT_Method newMethod = repository.createMethod(this);
			newMethod.read(dis, pool, repository, loadedFromEntry);
			if(isJavaLangObject && newMethod.isFinalizer()) {
				foundJavaLangObjectFinalizer = true; 
			}

			// If the method already existed, we replace its contents.
			BT_Method oldMethod = null;
			if ((oldMethod = methods.findMethod(newMethod.name, newMethod.getDescriptor())) != null) {
				oldMethod.replaceContents(newMethod);
				newMethod = oldMethod;
			}
			if (newMethods.findMethod(newMethod.name, newMethod.getDescriptor())
				!= null) {
				throw new BT_ClassFileException(
					"duplicate method definition " + newMethod.name);
			}
			newMethods.addElement(newMethod);
		}

		if(isJavaLangObject && !foundJavaLangObjectFinalizer) {
			repository.isFinalizationEnabled = false;
		}
		 
		// See if there are any stubs left, and add them at the end of the vector.
		for (int i = 0; i < methods.size(); i++) {
			BT_Method oldMethod = methods.elementAt(i);
			if (oldMethod.isStub())
				newMethods.addElement(oldMethod);
		}
		// Finally, swap old for new
		methods = newMethods;
	}

	/**
	 Builds relationships between methods.
	**/
	void buildMethodRelationships() {
		for (int n = 0; n < methods.size(); n++) {
			BT_Method method = methods.elementAt(n);
			// At this point, know all the parents, but not all the kids.
			// This may be rerun, so don't re-accumulate.
			BT_MethodRelationships.delinkParents(method);
			// To prevent re-adding this time
			if (method.canInherit())
				BT_MethodRelationships.linkParents(method);
		}
	}

	/**
	 True if this class is declared abstract as determined by {@link BT_Item#flags}.
	**/
	public boolean isAbstract() {
		return areAnyEnabled(ABSTRACT);
	}

	/**
	 Returns the bytecode value for storing a value of this class into a local.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> opc_istore
	 <br> Example: "float" --> opc_fstore
	**/
	public int getOpcodeForStore() {
		return getOpcodeForStore(name);
	}
	
	static int getOpcodeForStore(String name) {
		if (name.equals("short"))
			return opc_istore;
		if (name.equals("byte"))
			return opc_istore;
		if (name.equals("char"))
			return opc_istore;
		if (name.equals("int"))
			return opc_istore;
		if (name.equals("boolean"))
			return opc_istore;
		if (name.equals("long"))
			return opc_lstore;
		if (name.equals("double"))
			return opc_dstore;
		if (name.equals("float"))
			return opc_fstore;
		return opc_astore;
	}

	/**
	 Returns the bytecode value for this class's default value.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> opc_iconst_0
	 <br> Example: "float" --> opc_fconst_0
	**/
	public int getOpcodeForReturnValue() {
		if (name.equals("short"))
			return opc_iconst_0;
		if (name.equals("byte"))
			return opc_iconst_0;
		if (name.equals("char"))
			return opc_iconst_0;
		if (name.equals("int"))
			return opc_iconst_0;
		if (name.equals("boolean"))
			return opc_iconst_0;
		if (name.equals("long"))
			return opc_lconst_0;
		if (name.equals("double"))
			return opc_dconst_0;
		if (name.equals("float"))
			return opc_fconst_0;
		if (name.equals("void"))
			return opc_nop;
		return opc_aconst_null;
	}

	/**
	 Returns the appropriate return bytecode for this class.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> opc_ireturn
	 <br> Example: "float" --> opc_freturn
	**/
	public int getOpcodeForReturn() {
		if (name.equals("short"))
			return opc_ireturn;
		if (name.equals("byte"))
			return opc_ireturn;
		if (name.equals("char"))
			return opc_ireturn;
		if (name.equals("int"))
			return opc_ireturn;
		if (name.equals("boolean"))
			return opc_ireturn;
		if (name.equals("long"))
			return opc_lreturn;
		if (name.equals("double"))
			return opc_dreturn;
		if (name.equals("float"))
			return opc_freturn;
		if (name.equals("void"))
			return opc_return;
		return opc_areturn;
	}

	/**
	 Returns the appropriate load bytecode for this class.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> opc_iload
	 <br> Example: "float" --> opc_fload
	**/
	public int getOpcodeForLoadLocal() {
		if (name.equals("short"))
			return opc_iload;
		if (name.equals("byte"))
			return opc_iload;
		if (name.equals("char"))
			return opc_iload;
		if (name.equals("int"))
			return opc_iload;
		if (name.equals("boolean"))
			return opc_iload;
		if (name.equals("long"))
			return opc_lload;
		if (name.equals("double"))
			return opc_dload;
		if (name.equals("float"))
			return opc_fload;
		return opc_aload;
	}

	/**
	 Returns the appropriate store bytecode for this class.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> opc_istore
	 <br> Example: "float" --> opc_fstore
	**/
	public int getOpcodeForStoreLocal() {
		if (name.equals("short"))
			return opc_istore;
		if (name.equals("byte"))
			return opc_istore;
		if (name.equals("char"))
			return opc_istore;
		if (name.equals("int"))
			return opc_istore;
		if (name.equals("boolean"))
			return opc_istore;
		if (name.equals("long"))
			return opc_lstore;
		if (name.equals("double"))
			return opc_dstore;
		if (name.equals("float"))
			return opc_fstore;
		return opc_astore;
	}

	/**
	 Returns the size of a local of this class's type.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> 1
	 <br> Example: "double" --> 2
	**/
	public int getSizeForLocal() {
		if (name.equals("long") || name.equals("double"))
			return 2;
		if (name.equals("void"))
			return 0;
		return 1;
	}

	/**
	 Returns the opcode for popping a reference or primitive of this type from the stack.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> opc_pop
	 <br> Example: "double" --> opc_pop2
	**/
	public int getOpcodeForPop() {
		if (name.equals("long") || name.equals("double"))
			return opc_pop2;
		return opc_pop;
	}

	/**
	 Returns the opcode for duplicating an object of this class's type.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> dup
	 <br> Example: "double" --> dup2
	**/
	public int getOpcodeForDup() {
		if (name.equals("long") || name.equals("double"))
			return opc_dup2;
		return opc_dup;
	}

	/**
	 Sets the value for the "SourceFile" attribute.
	 Shows up in stack dumps and when "javap" is used.
	 Useful insert advertizing or legal messages.
	**/
	public void setSourceFile(String name) {
		BT_SourceFileAttribute s =
			(BT_SourceFileAttribute) attributes.getAttribute(BT_SourceFileAttribute.ATTRIBUTE_NAME);
		if (s == null)
			attributes.addElement(new BT_SourceFileAttribute(name, this));
		else
			s.fileName = name;
	}

	/**
	 Returns the value for the "SourceFile" attribute (or null if none).
	**/
	public String getSourceFile() {
		BT_SourceFileAttribute s =
			(BT_SourceFileAttribute) attributes.getAttribute(BT_SourceFileAttribute.ATTRIBUTE_NAME);
		return (s == null) ? null : s.fileName;
	}

	/**
	 Compares only names.
	 For use by {@link BT_ClassVector#sort}.
	**/
	public int compareTo(Object that) {
		if(this == that) {
			return 0;
		}
		return this.name.compareTo(((BT_Class) that).name);
	}

	/**
	 Prints this class in the indicated PrintStream.
	 Somewhat similar to the output of "javap".
	 Uses {@link BT_Class#fullKindName} to distinguish external, stub, and {@link <a
	 href=../jikesbt/doc-files/Glossary.html#project_class>project classes</a>}.
	
	 @param  printFlags  The sum of some of:
	   {@link BT_Misc#PRINT_NO_CODE},
	   {@link BT_Misc#PRINT_NO_METHOD}, and
	   {@link BT_Misc#PRINT_ZERO_OFFSETS}.
	   Other bits are ignored.
	**/
	public void print(PrintStream ps, int printFlags, BT_SourceFile sourceFile) {
		boolean isAssemblerMode = (printFlags & BT_Misc.PRINT_IN_ASSEMBLER_MODE) != 0;
		if (getSourceFile() != null) {
			ps.println("/*");
			ps.println(Messages.getString("JikesBT._*_This_class_file_was_compiled_from___205") + '"' + getSourceFile() +'"');
			ps.println(" */");
			ps.println("");
		}
		String keywordString = isAssemblerMode ? modifierString() : keywordModifierString();
		if(keywordString.length() > 0) {
			keywordString += " ";
		}
		ps.print(keywordString + fullKindName() + " " + fullName());
		if (getSuperClass() != null) {
			String superClass = getSuperClass().fullName();
			if (!superClass.equals(BT_Repository.JAVA_LANG_OBJECT))
				ps.print(" extends " + superClass);
		}

		// Sort them to make file comparisons clearer ...
		StringVector sort = new StringVector();

		// To avoid having side effects, this does not use BT_Class.sort.
		for (int i = 0; i < parents_.size(); ++i) {
			if (parents_.elementAt(i).isInterface) // Not the superclass
				sort.addElement(parents_.elementAt(i).fullName());
		}
		String kw = isInterface ? " extends " : " implements ";
		String delim = kw;
		for (int i = 0; i < sort.size(); ++i) {
			ps.print(delim + sort.elementAt(i));
			delim = ", ";
		}
		sort.removeAllElements();

		if (!isAssemblerMode) {
			for (int i = 0; i < kids_.size(); ++i)
				sort.addElement("\t/"
					+ "/ "
					+ (kids_.elementAt(i).isInterface
						? Messages.getString("JikesBT.Has_subinterface__217")
						: isInterface
						? Messages.getString("JikesBT.Has_implementor__218")
						: Messages.getString("JikesBT.Has_subclass__219"))
					+ kids_.elementAt(i).fullName());
			sort.print(ps);
			sort.removeAllElements();
			if (!isCreated())
				ps.println("\t/" + Messages.getString("JikesBT./_Not_instantiated_221"));
			else {
				for (int k = 0; k < creationSites.size(); k++)
					sort.addElement(
						"\t/"
							+ Messages.getString("JikesBT./_Created_in__223")
							+ creationSites.elementAt(k).getFrom().useName());
				//sort.sort();
				sort.print(ps);
				sort.removeAllElements();
			}
		}

		ps.println("{");
		if(fields.size() > 0) {
			for (int n = 0; n < fields.size(); n++) {
				BT_Field field = fields.elementAt(n);
				field.print(ps, printFlags);
			}
			ps.println();
		}
		if ((printFlags & BT_Misc.PRINT_NO_METHOD) == 0) { // Not suppressed
			// Could be sorted ...
			for (int n = 0; n < methods.size(); n++) {
				methods.elementAt(n).print(ps, printFlags, sourceFile);
				ps.println();
			}
		}

		if (!isAssemblerMode) { /* assembler does not support reading attributes */
			attributes.print(ps, "\t");
		}

		ps.println("}");
	}
	
	public void printReferences(ReferenceSelector selector) {
		BT_ClassVector kids = getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Class from = kids.elementAt(i);
			if(selector.selectReference(this, from, null)) {
				String relationship;
				if(isInterface()) {
					if(from.isInterface()) {
						relationship = "subinterface";
					} else {
						relationship = "implementing class";
					}
				} else {
					if(from.isInterface()) {
						relationship = null;
					} else {
						relationship = "subclass";
					}
				}
				selector.printReference(this, from, relationship);
			}
		}
		BT_ClassReferenceSiteVector sites = referenceSites;
		for(int j=0; j<sites.size(); j++) {
			BT_ClassReferenceSite site = sites.elementAt(j);
			BT_Method from = site.getFrom();
			if(selector.selectReference(this, from, site.from)) {
				selector.printReference(this, from, site.getInstruction().getOpcodeName());
			}
		}
		BT_AttributeVector atts = referencingAttributes;
		if(atts != null) {
			for(int j=0; j<atts.size(); j++) {
				BT_Attribute att = atts.elementAt(j);
				BT_AttributeOwner owner = att.getOwner();
				BT_Item owningItem = owner.getEnclosingItem();
				if(selector.selectReference(this, owningItem, att)) {
					selector.printReference(this, owningItem, att.getName());
				}
			}
		}
		BT_FieldVector fieldTypes = asFieldTypes;
		if(fieldTypes != null) {
			for(int i=0; i<fieldTypes.size(); i++) {
				BT_Field from = fieldTypes.elementAt(i);
				if(selector.selectReference(this, from, null)) {
					selector.printReference(this, from, "field type");
				}
			}
		}
		BT_SignatureSiteVector sigTypes = asSignatureTypes;
		if(sigTypes != null) {
			for(int i=0; i<sigTypes.size(); i++) {
				BT_SignatureSite site = sigTypes.elementAt(i);
				BT_Method from = site.from;
				if(selector.selectReference(this, from, null)) {
					selector.printReference(this, from, site.isReturnType() ? "return type" : "parameter type");
				}
			}
		}
		printReferencesToInheritedMembers(selector, this, new BT_HashedClassVector());
		BT_MethodVector methods = getMethods();
		for(int k=0; k<methods.size(); k++) {
			BT_Method method = methods.elementAt(k);
			method.printReferences(selector);
		}
		BT_FieldVector fields = getFields();
		for(int k=0; k<fields.size(); k++) {
			BT_Field field = fields.elementAt(k);
			field.printReferences(selector);
		}
		
		BT_ClassVector arrayTypes = asArrayTypes;
		if(arrayTypes != null) {
			for(int i=0; i<arrayTypes.size(); i++) {
				BT_Class from = arrayTypes.elementAt(i);
				from.printReferences(selector);
			}
		}
	}
	
	private void printReferencesToInheritedMembers(
			ReferenceSelector selector,
			BT_Class clazz, 
			BT_ClassVector excluded) {
		BT_ClassVector parents = clazz.getParents();
		for(int i=0; i<parents.size(); i++) {
			BT_Class parent = parents.elementAt(i);
			if(excluded.contains(parent)) {
				continue;
			}
			excluded.addElement(parent);
			
			BT_MethodVector methods = parent.getMethods();
			for(int k=0; k<methods.size(); k++) {
				BT_Method method = methods.elementAt(k);
				BT_MethodCallSiteVector accessors = method.callSites;
				for(int j=0; j<accessors.size(); j++) {
					printReferencesToInheritedMembers(selector, accessors.elementAt(j));
				}
			}
			BT_FieldVector fields = parent.getFields();
			for(int k=0; k<fields.size(); k++) {
				BT_Field field = fields.elementAt(k);
				BT_AccessorVector accessors = field.accessors;
				for(int j=0; j<accessors.size(); j++) {
					printReferencesToInheritedMembers(selector, accessors.elementAt(j));
				}
			}
			
			printReferencesToInheritedMembers(selector, parent, excluded);
		}
	}
	
	private void printReferencesToInheritedMembers(ReferenceSelector selector, BT_ItemReference accessor) {
		BT_Class through = accessor.getClassTarget();
		if(!equals(through)) {
			return;
		}
		BT_Method from = accessor.getFrom();
		if(selector.selectReference(this, from, accessor.from)) {
			selector.printReference(this, from, accessor.getInstruction().getOpcodeName());
		}
	}

	public void print(PrintStream ps, BT_SourceFile sourceFile) {
		print(ps, 0, sourceFile);
	}
	
	public void print(PrintStream ps) {
		print(ps, 0, null);
	}
	
	public void print(PrintStream ps, int printFlags) {
		if((printFlags & BT_Misc.PRINT_SOURCE_FILE) == 0) {
			print(ps, printFlags, null);
			return;
		}
		String sourceName = getSourceFile();
		if(sourceName == null) {
			print(ps, printFlags, null);
			return;
		}
		//BT_SourceFile sourceFile = repository.getSourceFile(sourceName); 
		//TODO hashmap or vector of sourcefiles in repository
		BT_SourceFile sourceFile = new BT_SourceFile(this, sourceName);
		print(ps, printFlags, sourceFile);
	}

	/**
	 Returns the type of object this BT_Class represents, including
	 distinguishing whether it is an
	 {@link <a href=../jikesbt/doc-files/Glossary.html#external_class>external classes</a>},
	 that can be useful since they have different properties from
	 {@link <a href=../jikesbt/doc-files/Glossary.html#system_class>system classes</a>}.
	 {@link BT_Class#kindName} returns a less complete description.
	
	 <p> {@link BT_Class#kindName} returns a simpler string.
	
	 @return One of
	   "class", "stub-class", "external class",
	   "interface", "stub-interface", "external interface",
	   "array", "primitive", or "stub".
	   It will be "stub" only if the stub has not yet been differentiated
	   (determined to be a class or an interface).
	   Note that all stub classes are also external classes.
	**/
	public String fullKindName() {
		if (isClass) {
			return isStub()
				? STUB_CLASS_NAME  
				: (inProject()
				? CLASS_NAME 
				: EXTERNAL_CLASS_NAME);
		}
		if (isInterface) {
			return isStub()
				? STUB_INTERFACE_NAME 
				: (inProject()
				? INTERFACE_NAME
				: EXTERNAL_INTERFACE_NAME);
		}
		if (arrayType != null) {
			return ARRAY_NAME;
		}
		if (isBasicTypeClass) {
			return PRIMITIVE_NAME;
		}
		if (isStub()) {
			return STUB_NAME;
		}
		return "?kindName?"; // Try to recover
	}
	
	//TODO clean up places where assembler printer uses external messages
	//eg toAssemblerString locations such as BT_ExceptionTableEntry.toAssemblerString
	//BT_Class.fullKindName and BT_Class.kindName
	
	/**
	 Returns the type of object this BT_Class represents.
	 {@link BT_Class#fullKindName} returns a more complete description.
	
	 <p> {@link BT_Class#fullKindName} returns a more complete string.
	
	 @return One of "class", "interface", "array", "primitive", or "stub".
	   It will be "stub" only if the stub has not yet been differentiated
	   (determined to be a class or an interface).
	**/
	public String kindName() {
		if (isClass)
			return CLASS_NAME;
		if (isInterface)
			return INTERFACE_NAME;
		if (arrayType != null)
			return ARRAY_NAME;
		if (isBasicTypeClass)
			return PRIMITIVE_NAME;
		if (isStub())
			return STUB_NAME;
		return "?kindName?"; // Try to recover
	}

	public String className() {
		return name;
	}

	public String useName() {
		return fullName();
	}

	public String fullName() {
		return name;
	}

	public boolean isInSamePackage(BT_Class other) {
		if(equals(other)) {
			return true;
		}
		String pkg1 = packageName();
		String pkg2 = other.packageName();
		return pkg1 == pkg2 || pkg1.equals(pkg2);
	}
	
	/**
	 Returns the package name of the class.
	**/
	public String packageName() {
		if(cachedPackageName != null) {
			return cachedPackageName;
		}
		String result;
		int packageIndex = name.lastIndexOf('.');
		if (packageIndex < 0) {
			result = "";
		} else {
			result = name.substring(0, packageIndex);
		}
		/* intern the result so that isInSamePackage(BT_Class) is faster */
		result = result.intern();
		cachedPackageName = result;
		return result;
	}
	
	/**
	 Returns the name of the class without the package prefix.
	**/
	public String classNameWithoutPackage() {
		return classNameWithoutPackage(name);
	}
	
	/**
	 Returns the name of the class without the package prefix.
	**/
	public static String classNameWithoutPackage(String name) {
		int packageIndex = name.lastIndexOf('.');
		if(packageIndex < 0) {
			return name;
		}
		return name.substring(packageIndex + 1);
	}
	
	StringBuffer flagString(StringBuffer s, short flags, boolean keywordsOnly, boolean modifiersOnly) {
		if (!keywordsOnly) {
			if ((flags & SUPER) != 0) {
				s.append(SUPER_NAME);
				s.append(' ');
			}
		}
		if(!modifiersOnly) {
			if ((flags & ENUM) != 0) {
				s.append(ENUM_NAME);
				s.append(' ');
			}
			if ((flags & INTERFACE) != 0) {
				s.append(INTERFACE_NAME);
				s.append(' ');
			}
		}
		return s;
	}
	
	/**
	 Returns a short description of this object for use in debugging.
	**/
	public String toString() {
		return fullName();
	}

	// ----------------------------------------------------------------------------
	// Class/method relationships

	
	//TODO get rid of these two weird hacks tempMark and curMark
	/**
	 For general use for marking classes.
	 See {@link BT_Class#curMark_}.
	**/
	int tempMark_ = 0;

	/**
	 The value that {@link BT_Class#tempMark_} is currently being assigned and compared to.
	**/
	static int curMark_ = 0;

	/**
	 Doesn't check that the parent is not a class.
	
	 <p> Whether inter-method relationships are built and maintained is
	 affected by {@link BT_Factory#buildMethodRelationships}.
	
	 @see #detachParent
	**/
	private final void uncheckedDetachParent(BT_Class pc) {
		boolean removedP = this.parents_.removeElement(pc); // Remove par<-me
		if (CHECK_USER && !removedP)
			expect(Messages.getString("JikesBT.Expected_back_pointer_255"));
		boolean removedK = pc.kids_.removeElement(this); // Remove par->me
		if (CHECK_USER && !removedK)
			expect(Messages.getString("JikesBT.Expected_back_pointer_255"));

		if (repository.factory.buildMethodRelationships) {
			// Redo ancestors' methods' kids & inlaws
			boolean someBranchesNow =
				BT_MethodRelationships.relinkParentsOfMyAndDescendentMethods(
					this);
			if (someBranchesNow || parents_.size() != 0) {
				// There may have been and/or not be a inlaw from an ancestor thru me
				BT_MethodRelationships
					.delinkInlawsOfAllMethodsOfClassAndAncestors(
					pc);
				BT_MethodRelationships
					.linkInlawsOfAllMethodsOfClassAndAncestors(
					pc);
			}
		}
	}
	
	/**
	 Makes this subinterface or implementing class no longer extend or implement delP.
	 The specified parent must be known to be a direct parent (e.g., not just an ancestor).
	 Cannot be used to remove a superclass, that must instead be reset.
	
	 <p> Whether inter-method relationships are built and maintained is
	 affected by {@link BT_Factory#buildMethodRelationships}.
	
	 @see  #attachKid
	 @see  #attachParent
	 @see  #detachKid
	**/
	// Design observations:
	//       - Class relationships between my descendents and my other old parents are unchanged
	//       - Method relationships between my descendents and my other old parents are unchanged
	//         -- Except that perhaps some inlaws of them are added since they are no longer suppressed by inheritence
	//       - Parents+kids:
	//         - No parent nor kid relationships will be added
	//           -- but not all thru/to me will be deleted.
	//         - My ancestors' methods' kids to and thru me via the deleted edge will be deleted
	//           -- not just to my kids
	//           -- except if there is additional inheritence not thru the deleted edge.
	//           - Alg
	//             - delete & recompute kids of all ancestors!
	//             - keep a list of ancestors processed to avoid reprocessing them
	//         - My+descendents' parent relationships thru me will be deleted
	//           -- except if there is additional inheritence not thru the deleted edge.
	//           - Handle as side-effect of doing ancestors
	//         - The branching==1 special case optimization:
	//           - If I and _all_ descendents down a single path inherit from ==1 class
	//             - Then each's parents not at or directly below me are simply deleted
	//               - But how efficiently keep track of all methods already seen?
	//                 - Mark each class while going down and check if the zero or one parent of the method in question is in a marked class
	//       - Inlaws
	//         - Inlaws that used to be suppressed by the "except if inherited" rule will be added
	//           - Will be one per common descendent of my class and the detached parent's class
	//         - The detached parent's and its ancestors' methods' inlaws thru the deleted edge will be deleted
	//           (i.e., my ancestors' methods' inlaws thru the deleted edge)
	//           -- except if there is additional inheritence not thru the deleted edge.
	//           - Not alg:  Could go back to ancestors, then start search from me
	//             -- but won't know if is an alternate route
	//           - Alg:  Recompute for the detached parent and all its ancestors
	//         - My+descendents' methods' inlaws won't be directly affected
	//           -- except that there may be new inlaws between me or my descendents and my former ancestors as noted above
	//         - The branching==1 special case optimization #1
	//           - If none of my ancestors' classes along a path up thru class AC have >1 _kid_ (before this detach),
	//             -- Then any inlaw of a method in AC must be via me (at me+descendents)
	//             - This case is too rare to be of interest yet
	//         - The branching==1 special case optimization #2
	//           - If knew none of the descendents of a class had >1 parent, would know none of the methods had or have any inlaws.
	//           - Implemented.
	//
	public void detachParent(BT_Class delP) {
		if (CHECK_USER && delP == null)
			assertFailure("" + this);
		if (CHECK_USER && delP.isClass)
			assertFailure("" + this);
		uncheckedDetachParent(delP);
	}

	final BT_ConstantPool getPool() {
		return pool;
	}
	
	/**
	 Returns true if any of the 'throws error' flags is true.
	**/
	public boolean throwsAnyError() {
		return areAnyModelEnabled((short) 
					(THROWSVERIFYERROR
					| THROWSCLASSFORMATERROR
					| THROWSNOCLASSDEFFOUNDERROR
					| THROWSUNSUPPORTEDCLASSVERSIONERROR
					| THROWSCLASSCIRCULARITYERROR
					| THROWSINCOMPATIBLECLASSCHANGEERROR));
	}

	/**
	 Returns flag {@link BT_Class#THROWSINCOMPATIBLECLASSCHANGEERROR}.
	**/
	public boolean throwsIncompatibleClassChangeError() {
		return areAnyModelEnabled(THROWSINCOMPATIBLECLASSCHANGEERROR);
	}
	 
	/**
	 Just returns flag {@link BT_Class#THROWSCLASSCIRCULARITYERROR}.
	**/
	public boolean throwsClassCircularityError() {
		return areAnyModelEnabled(THROWSCLASSCIRCULARITYERROR);
	}

	/**
	 Just returns flag {@link BT_Class#THROWSCLASSFORMATERROR}.
	**/
	public boolean throwsClassFormatError() {
		return areAnyModelEnabled(THROWSCLASSFORMATERROR);
	}

	/**
	 Just returns flag {@link BT_Class#THROWSNOCLASSDEFFOUNDERROR}.
	**/
	public boolean throwsNoClassDefFoundError() {
		return areAnyModelEnabled(THROWSNOCLASSDEFFOUNDERROR);
	}

	/**
	 Just returns flag {@link BT_Class#THROWSUNSUPPORTEDCLASSVERSIONERROR}.
	**/
	public boolean throwsUnsupportedClassVersionError() {
		return areAnyModelEnabled(THROWSUNSUPPORTEDCLASSVERSIONERROR);
	}

	/**
	 Sets flag {@link BT_Class#THROWSINCOMPATIBLECLASSCHANGEERROR}.
	**/
	public void setThrowsIncompatibleClassChangeError() {
		enableModelFlags(THROWSINCOMPATIBLECLASSCHANGEERROR);
	}
	
	
	/**
	 Sets flag {@link BT_Class#THROWSCLASSCIRCULARITYERROR}.
	**/
	public void setThrowsClassCircularityError() {
		shouldBeClass(); // Make sure it is at least a class if still a stub
		enableModelFlags(THROWSCLASSCIRCULARITYERROR);
	}

	/**
	 Sets flag {@link BT_Class#THROWSCLASSFORMATERROR}.
	**/
	public void setThrowsClassFormatError() {
		shouldBeClass(); // Make sure it is at least a class if still a stub
		enableModelFlags(THROWSCLASSFORMATERROR);
	}

	/**
	 Sets flag {@link BT_Class#THROWSNOCLASSDEFFOUNDERROR}.
	**/
	public void setThrowsNoClassDefFoundError() {
		shouldBeClass(); // Make sure it is at least a class if still a stub
		enableModelFlags(THROWSNOCLASSDEFFOUNDERROR);
	}

	/**
	 Sets flag {@link BT_Class#THROWSVERIFYERROR}.
	**/
	public void setThrowsVerifyError() {
		shouldBeClass(); // Make sure it is at least a class if still a stub
		super.setThrowsVerifyErrorTrue();
	}

	/**
	 Sets flag {@link BT_Class#THROWSUNSUPPORTEDCLASSVERSIONERROR}.
	**/
	public void setThrowsUnsupportedClassVersionError() {
		shouldBeClass(); // Make sure it is at least a class if still a stub
		enableModelFlags(THROWSUNSUPPORTEDCLASSVERSIONERROR);
	}

	/**
	 Replaces any code in the class initializer method to throw java.lang.ClassFormatError.
	 A class initializer will be created if it does not exist.
	**/
	public void makeThrowClassFormatError() {
		makeClassInitializerThrowError(BT_Repository.JAVA_LANG_CLASS_FORMAT_ERROR);
	}

	/**
	 Replaces any code in the class initializer method to throw java.lang.NoClassDefFoundError.
	 A class initializer will be created if it does not exist.
	**/
	public void makeThrowNoClassDefFoundError() {
		makeClassInitializerThrowError(BT_Repository.JAVA_LANG_NO_CLASS_DEF_FOUND_ERROR);
	}
	
	/**
	 Replaces any code in the class initializer method to throw java.lang.IncompatibleClassChangeError.
	 A class initializer will be created if it does not exist.
	**/
	public void makeThrowIncompatibleClassChangeError() {
		makeClassInitializerThrowError(BT_Repository.JAVA_LANG_INCOMPATIBLE_CLASS_CHANGE_ERROR);
	}

	/**
	 Replaces any code in the class initializer method to throw java.lang.VerifyError.
	 A class initializer will be created if it does not exist.
	**/
	public void makeThrowVerifyError() {
		makeClassInitializerThrowError(BT_Repository.JAVA_LANG_VERIFY_ERROR);
	}

	/**
	 Replaces any code in the class initializer method to throw java.lang.UnsupportedClassVersionError.
	 A class initializer will be created if it does not exist.
	**/
	public void makeThrowUnsupportedClassVersionError() {
		makeClassInitializerThrowError(BT_Repository.JAVA_LANG_UNSUPPORTED_CLASS_VERSION_ERROR);
	}

	private BT_Method classInitializer;
	
	/**
	 * Returns the class initializer or null if no such method exists.
	 */
	public BT_Method getClassInitializer() {
		if(classInitializer == null) {
			classInitializer = findMethodOrNull(
					BT_Method.STATIC_INITIALIZER_NAME, repository.basicSignature);
		}
		return classInitializer;
	}
	
	
	/**
	 * same as getClassInitializer() except that it creates the initializer if none exists already
	 */
	public BT_Method createClassInitializer() {
		BT_Method clinit = getClassInitializer();
		if (clinit == null) {
			classInitializer = clinit =
				BT_Method.createMethod(
					this,
					(short) (BT_Method.PUBLIC | BT_Method.STATIC),
					repository.basicSignature,
					BT_Method.STATIC_INITIALIZER_NAME);
		}
		return clinit;
	}
	
	
	public BT_Method createDefaultConstructor() {
		BT_Method init = findMethodOrNull(BT_Method.INITIALIZER_NAME, repository.basicSignature);
		if (init == null) {
			init =
				BT_Method.createMethod(
					this,
					(short) BT_Method.PUBLIC,
					repository.basicSignature,
					BT_Method.INITIALIZER_NAME);
		}
		return init;
	}
	
	/**
	 Replaces any code in the class initializer method to throw the given Error.
	 A class initializer will be created if it does not exist.
	**/
	public void makeClassInitializerThrowError(String errorClassName) {
		createClassInitializer().makeCodeThrowError(errorClassName);
	}

       /**
         Returns true if "name" is a primitive or "void".
        **/
    //   @since     In JikesBT 7.2 renamed from "isBasicTypeName".
   public static boolean  isPrimitiveName(String name) {
         return name.equals("boolean")
             || name.equals("byte")
             || name.equals("char")
             || name.equals("double")
             || name.equals("float")
             || name.equals("int")
             || name.equals("long")
             || name.equals("short")
             || name.equals("void");
   }
   
   public boolean mightBeInstance(BT_Class objectType) {
	   return isInstance(objectType) 
	   	|| (!objectType.isInstance(this)
	   			&& isPotentialInstance(objectType));
   }
   
   private boolean isPotentialInstance(BT_Class objectType) {
	   return objectType.isStubOrHasParentStub()  
		|| (objectType.isArray() && objectType.getElementClass().isStubOrHasParentStub());
   }
   
   public boolean isInstance(BT_Class objectType) {
	    if(equals(repository.findJavaLangObject())) {
	    	return !objectType.isPrimitive();
	    }
		if(equals(objectType)) {
			return true;
		}
		if(objectType.isArray()) {
			return /* equals(repository.findJavaLangObject()) || */
				(isInterface() && isArrayInterface()) ||
				(isArray() && getElementClass().isInstance(objectType.getElementClass()));
		}
		if(objectType.isInterface()) {
			return /* equals(repository.findJavaLangObject()) || */
				(isInterface() && isInterfaceAncestorOf(objectType));
		}
		return isInterface() ? isInterfaceAncestorOf(objectType) : isClassAncestorOf(objectType);
	}
	
   public boolean mightBeInstanceOf(BT_Class objectType) {
	   return objectType.mightBeInstance(this);
   }
   
	public boolean isInstanceOf(BT_Class objectType) {
		return objectType.isInstance(this);
	}
	
	/**
	 * determines whether the given class implements the current interface or is a child
	 * interface of the current interface 
	 */
	public boolean isInterfaceAncestorOf(BT_Class origC) {
		BT_ClassVector parents = origC.getParents();
		for (int i = 0; i < parents.size(); i++) {
			BT_Class par = parents.elementAt(i);
			if (par == this || isInterfaceAncestorOf(par))
				return true;
		}
		return false;
	}
	
	/**
	 * @return true if this class represents the interface java.io.Serializable or java.lang.Cloneable,
	 * the two interfaces implemented by every array
	 */
	public boolean isArrayInterface() {
		String name = fullName();
		return name.equals(BT_Repository.JAVA_IO_SERIALIZABLE) || 
			name.equals(BT_Repository.JAVA_LANG_CLONEABLE);
	}

}
