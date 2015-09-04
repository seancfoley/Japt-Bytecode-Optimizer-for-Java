package com.ibm.jikesbt;


/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 A base class for a {@link BT_Class}, {@link BT_Method}, or {@link BT_Field}.

 * @author IBM
**/
public abstract class BT_Item extends BT_Base implements BT_AttributeOwner, BT_Opcodes {

	/**
	 The flags bytes from the class-file and some additional flags, the model flags.
	
	 
	 The low 16 bits are the flags from the class file, while the high 16 bits
	 are used only by JikesBT for the model flags.
	**/
	int flags = INPROJECT;

	/**
	 The item's contained attributes.
	 This can be updated directly except for the Code attribute -- see {@link
	 BT_Method#setCode}.
	**/
	public BT_AttributeVector attributes = new BT_AttributeVector();

	/**
	 The name of the class, method, or field.
	 This field should be accessed via {@link BT_Item#getName}, {@link #resetName}, or possibly {@link #setName}.
	
	 <p> It is in fully-qualified external format for classes (e.g., "package.Class[]"),
	 but simple (no dots nor parens) for methods (e.g., "print") and fields
	 (eg., "length").
	**/
	String name;
	
	protected BT_Item() {}

	/**
	 Get the simple name of this item in external Java language format and not
	 qualified by any class it is in.
	 Unless either is overridden, same as {@link BT_Item#fullName() fullName()}.
	 (e.g., "java.lang.Class[][]", "city", "void", and "boolean").
	**/
	public String getName() {
		return name;
	}

	/**
	 Renames this class, method, or field.
	
	 @param  newName  The new name in Java language format and not
	   qualified by any class it is in.
	   (e.g., "java.lang.Class[][]", "City", "city", "void", and "boolean").
	 @see #resetName
	 @see #getName
	 @see BT_Class#setName
	 @see BT_Member#setName
	**/
	public abstract void setName(String newName);

	/**
	 Renames this class, method, or field and updates related information.
	 Assumes and preserves <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency</a>.
	
	 @param The name of the item in fully-qualified Java language format
	   (e.g., "java.lang.Class[][]", "City", "city", "void", and "boolean").
	 @see #setName
	 @see #getName
	 @see BT_Class#resetName
	 @see BT_Method#resetName
	 @see BT_Field#resetName
	**/
	public abstract void resetName(String newName);

	/**
	 @return  The fully-qualified name of this item in Java language format
	 (e.g., "java.lang.Class[][]", "p.C.foo", "boolean", "p.C.m").
	 Excludes any method returned value and argument types.
	**/
	public abstract String fullName();

	/**
	 @return  The fully-qualified name of this item in Java language format
	 (e.g., "java.lang.Class[][]", "p.C.foo", "boolean", "void p.C.m(int)")
	 as it would be used in an instruction.
	 Includes any argument types for methods.
	 This is the same as Java's definition of "signature" (but not JikesBT's).
	**/
	public abstract String useName();

	public abstract boolean isClassMember();
	
	/**
	 @return  The qualified name of this item in Java language format 
	 not including its containing class.
	 (e.g., "java.lang.Class[][]", "foo", "boolean", "m(int)")
	 Includes any argument types for methods.
	 This is the same as Java's definition of "signature" (but not JikesBT's).
	**/
	public String qualifiedName() {
		return getName();
	}
	
	/**
	 @return  The fully-qualified class part of the name of this item
	 (class, class.methodname w/o arguments, or class.fieldname).
	**/
	public abstract String className();

	/**
	 A low-level method that just updates {@link BT_Item#flags} to mark this item as declared
	 "public" (and no longer "protected" or "private"), but does not modify
	 any instructions that manipulate it.
	 E.g., if this is a {@link BT_Method} that formerly was private and
	 non-static, then the caller should also change invokers of it from {@link
	 BT_Opcodes#opc_invokespecial} to {@link BT_Opcodes#opc_invokevirtual}.
	**/
	public boolean becomePublic() {
		if(isPublic()) {
			return false;
		}
		enableFlags(PUBLIC);
		disableFlags((short) (PROTECTED | PRIVATE));
		return true;
	}

	/**
	 A low-level method that just updates {@link BT_Item#flags} to mark this item as declared
	 "protected" (and no longer "public" or "private"), but does not modify
	 any instructions that manipulate it.
	 E.g., if this is a {@link BT_Method} that formerly was private and
	 non-static, then the caller should also change invokers of it from {@link
	 BT_Opcodes#opc_invokespecial} to {@link BT_Opcodes#opc_invokevirtual}.
	**/
	public boolean becomeProtected() {
		if(isProtected()) {
			return false;
		}
		disableFlags((short) (PUBLIC | PRIVATE));
		enableFlags(PROTECTED);
		return true;
	}

	/**
	 A low-level method that just updates {@link BT_Item#flags} to mark this item as declared
	 "private" (and no longer "public" or "protected"), but does not modify
	 any instructions that manipulate it.
	 E.g., if this is a {@link BT_Method} that formerly was public and
	 non-static, then the caller should also change invokers of it from {@link
	 BT_Opcodes#opc_invokevirtual} to {@link BT_Opcodes#opc_invokespecial}.
	**/
	public boolean becomePrivate() {
		if(isPrivate()) {
			return false;
		}
		disableFlags((short) (PUBLIC | PROTECTED));
		enableFlags(PRIVATE);
		return true;
	}


   	public boolean becomeDefaultAccess() {
   		if(isDefaultAccess()) {
   			return false;
   		}
   		disableFlags(PERMISSION_FLAGS);
   		return true;
   	}
   	
   	public BT_Item getEnclosingItem() {
   		return this;
   	}
   	
   	public abstract boolean isVisibleFrom(BT_Class clazz);

   	public abstract boolean isUnconditionallyVisibleFrom(BT_Class clazz);
   	
   	/**
	 Gets the class this item is declared in.
	 Named like java.lang.reflect.getDeclaringClass().
	**/
	public abstract BT_Class getDeclaringClass();
	
	public BT_AttributeVector getAttributes() {
		return attributes;
	}
	
	public interface ReferenceSelector {
		public boolean selectReference(BT_Item to, BT_Item from, BT_Attribute att);
		
		public void printReference(BT_Item to, BT_Item from, String extra);
	}
	
	public abstract void printReferences(ReferenceSelector selector);
	
	/**
	 A low-level method that just updates {@link BT_Item#flags}.
	 E.g., does not unlink from overridden or overriding methods.
	 E.g., does not remove the "this" argument.
	 @see BT_Method#convertFromInstanceMethodToStatic()
	**/
	public void becomeStatic() {
		//this is called from BT_MethodRefIns and BT_FieldRefIns;
		enableFlags(STATIC);
		disableFlags(FINAL);
	}

	/**
	 A low-level method that just updates {@link BT_Item#flags}.
	**/
	public void becomeAbstract() { // Could be moved to BT_Method and to BT_Class
		if(this instanceof BT_Field) {
			throw new IllegalStateException();
		}
		enableFlags(ABSTRACT);
		
		//Can't be native, strict or synchronized at the same time
		disableFlags((short) (NATIVE | STRICT | SYNCHRONIZED));
	}

	/**
	 * sets whether this item is final.
	 * @param isFinal whether the item is final.
	 */
	public void setFinal(boolean isFinal) {
		if (isFinal) {
			enableFlags(FINAL);
		} else {
			disableFlags(FINAL);
		}
	}
	

	/**
	 * 
	 * @return the access permission flags for this item:
	 * either BT_Item.PUBLIC, BT_Item.PRIVATE, BT_Item.PROTECTED or BT_Item.DEFAULT_ACCESS.
	 */
	public short getAccessPermission() {
		return (short) (getFlags() & PERMISSION_FLAGS);
	}
	
	public boolean isPublic() {
		return areAnyEnabled(PUBLIC);
	}
	
	public boolean isPrivate() {
		return areAnyEnabled(PRIVATE);
	}
	
	public boolean isProtected() {
		return areAnyEnabled(PROTECTED);
	}
	
	/**
	 True if this is not declared public, protected, nor private as determined by {@link BT_Item#flags}.
	 Accessible only to members of the same package.
	**/
	public boolean isDefaultAccess() {
		return !areAnyEnabled(PERMISSION_FLAGS);
	}

	/**
	 True if this is declared static as determined by {@link BT_Item#flags}.
	**/
	public boolean isStatic() {
		return areAnyEnabled(STATIC);
	}
	
	/**
	 True if this is declared enum as determined by {@link BT_Item#flags}.
	**/
	public boolean isAnnotation() {
		return areAnyEnabled(ANNOTATION);
	}
	
	/**
	 True if this is declared final as determined by {@link BT_Item#flags}.
	**/
	public boolean isFinal() {
		return areAnyEnabled(FINAL);
	}
	
	/**
	 Returns a string such as "public " as determined by {@link BT_Item#flags}.
	 Only flags which are permission modifiers (public, protected, private) of this BT_Item are displayed.
	 @return  Either is "" or ends with a blank.
	**/
	public String accessString() {
		return accessString(getFlags(), false);
	}

	/**
	 Returns a string such as "public " as determined by {@link BT_Item#flags}.
	 Only flags which are permission modifiers (public, protected, private) of this BT_Item are displayed.
	 If withDefault is true, then DEFAULT_ACCESS_NAME is returned for package access permission.
	 @return  Either is "" or ends with a blank.
	**/
	public String accessString(short flags, boolean withDefault) {
		short permissionFlags = (short) (flags & PERMISSION_FLAGS);
		if(withDefault) {
			if(permissionFlags == BT_Item.DEFAULT_ACCESS) {
				return BT_Item.DEFAULT_ACCESS_NAME + " ";
			} 
		}
		return flagString(permissionFlags, true, true);
	}
	
	/**
	 Returns a string such as "public static final " as determined by {@link BT_Item#flags}.
	 Only flags which are modifiers of this BT_Item are displayed.
	 @return  Either is "" or ends with a blank.
	**/
	public String modifierString() {
		return flagString(getFlags(), false, true);
	}
	
	/**
	 Returns a string such as "public static final " as determined by {@link BT_Item#flags}.
	 Only flags that have a corresponding language keyword are displayed.
	 @return  Either is "" or ends with a blank.
	**/
	public String keywordModifierString() {
		return keywordModifierString(getFlags());
	}
	
	/**
	 Returns a string such as "public static final " as determined by {@link BT_Item#flags}.
	 Only flags that have a corresponding language keyword and which are modifiers are displayed.
	 @return  Either is "" or ends with a blank.
	**/
	public String keywordModifierString(short flags) {
		return flagString(flags, true, true);
	}
	
	/**
	 Returns a string such as "public static final " for all bits in {@link BT_Item#flags} applicable to this item.
	 @return  Either is "" or ends with a blank.
	**/
	public String flagString() {
		return flagString(getFlags());
	}
	
	/**
	 Returns a string such as "public static final " for all bits in {@link BT_Item#flags} applicable to this item.
	 @return  Either is "" or ends with a blank.
	**/
	public String flagString(short flags) {
		return flagString(flags, false, false);
	}
	
	/*
	 * Strings to match the modifiers above in access strings for fields, methods and classes
	 * These strings should be the same as they would appear in the source code.
	 */
	public static final String SYNTHETIC_NAME = "synthetic";
	public static final String PUBLIC_NAME = "public";
	public static final String PROTECTED_NAME = "protected";
	public static final String PRIVATE_NAME = "private";
	public static final String ABSTRACT_NAME = "abstract";
	public static final String STATIC_NAME = "static";
	public static final String FINAL_NAME = "final";
	public static final String STRICT_NAME = "strict";
	public static final String NATIVE_NAME = "native";
	public static final String SUPER_NAME = "super";
	public static final String TRANSIENT_NAME = "transient";
	public static final String VOLATILE_NAME = "volatile";
	public static final String SYNCHRONIZED_NAME = "synchronized";
	public static final String VARARGS_NAME = "variable-argument";
	public static final String BRIDGE_NAME = "bridge";
	public static final String ANNOTATION_NAME = "annotation";
	
	public static final String DEFAULT_ACCESS_NAME = "default";
	
	/**
	 Returns a string such as "public static final " as determined by the flags argument.
	 @return  Either is "" or ends with a blank.
	**/
	//   Reordered results to better conform to Java standards
	//     (although that cannot be done completely without knowing the type of the object
	//     because the ordering of "static" and "final" differs for methods versus fields).
	private String flagString(short flags, boolean keywordsOnly, boolean modifiersOnly) {
		// Recommended order for classes: public                   abstract static? final  static??
		// Recommended order for methods: public/protected/private abstract static  final          synchronized native
		// Recommended order for fields:  public/protected/private                  final  static  transient volatile
		// This could be modified to test for the type of the object.
		
		//does not include super, synchronized, bridge, varargs, volatile or transient which conflict
		StringBuffer s = new StringBuffer();
		
		if ((flags & PUBLIC) != 0) {
			s.append(PUBLIC_NAME);
			s.append(' ');
		} 
		if ((flags & PROTECTED) != 0) {
			s.append(PROTECTED_NAME);
			s.append(' ');
		}
		if ((flags & PRIVATE) != 0) {
			s.append(PRIVATE_NAME);
			s.append(' ');
		}
		if ((flags & ABSTRACT) != 0) {
			s.append(ABSTRACT_NAME);
			s.append(' ');
		}
		if ((flags & STATIC) != 0) {
			s.append(STATIC_NAME);
			s.append(' ');
		}
		if ((flags & FINAL) != 0) {
			s.append(FINAL_NAME);
			s.append(' ');
		}
		if ((flags & STRICT) != 0) {
			s.append(STRICT_NAME);
			s.append(' ');
		}
		if ((flags & NATIVE) != 0) {
			s.append(NATIVE_NAME);
			s.append(' ');
		}
		if (!keywordsOnly) {
			if ((flags & SYNTHETIC) != 0) {
				s.append(SYNTHETIC_NAME);
				s.append(' ');
			}
			if ((flags & ANNOTATION) != 0) {
				s.append(ANNOTATION_NAME);
				s.append(' ');
			}
		}
		//handle conflicting flags in subclasses (ie some access bits mean different things in different items)
		flagString(s, flags, keywordsOnly, modifiersOnly);
		//no "enum" (ENUM), no "interface" (INTERFACE), no "annotation" (ANNOTATION)
		//Note: we do not include the flags INTERFACE or ENUM as they are not modifiers (ie part of access string)
		//They are in fact identifiers
		return s.toString().trim();
	}
	
	
	/*
	 * provide item-specific descriptors for flag elements
	 * that differ between items such as super and synchronized
	 * which are both value 0x20 
	 */
	abstract StringBuffer flagString(StringBuffer s, short flags, boolean keywordsOnly, boolean modifiersOnly);
	
	/**
	 A mask bit useful for testing {@link BT_Item#flags}.
	**/
	public static final short DEFAULT_ACCESS = 0x0;
	public static final short PUBLIC = 0x1;
	public static final short PRIVATE = 0x2; //field or method only
	public static final short PROTECTED = 0x4; //field or method only
	public static final short STATIC = 0x8; //field or method only
	public static final short FINAL = 0x10;
	
	public static final short SUPER = 0x20; //class only
	public static final short INTERFACE = 0x200; //class only
	
	public static final short SYNCHRONIZED = 0x20; //method only
	public static final short BRIDGE = 0x40; //method only
	public static final short VARARGS = 0x80; //method only
	public static final short NATIVE = 0x100; //method only
	
	public static final short VOLATILE = 0x40; //field only
	public static final short TRANSIENT = 0x80; //field only
	
	public static final short ABSTRACT = 0x400; //class and method only
	public static final short STRICT = 0x800; //method only

	public static final short SYNTHETIC = 0x1000;
	public static final short ANNOTATION = 0x2000;
	public static final short ENUM = 0x4000; //class only
	
	public static final short PERMISSION_FLAGS = PUBLIC | PRIVATE | PROTECTED;
	
	abstract BT_ConstantPool getPool();

	/**
	 * @return whether any of the non-zero bits in flags are set
	 */
	boolean areAnyEnabled(short flags) {
		return (getFlags() & flags) != 0;
	}
	
	/**
	 * @return whether all of the non-zero bits in flags are set
	 */
	boolean areAllEnabled(short flags) {
		return (getFlags() & flags) == flags;
	}
	
	/**
	 * disables the non-zero bits in flags
	 */
	public void disableFlags(short flags) {
		this.flags &= 0xffff0000 | ~flags;
	}
	
	/**
	 * enables the non-zero bits in flags
	 */
	public void enableFlags(short flags) {
		this.flags |= flags;
	}
	
	/**
	 * gets the class file class, method or field flags
	 */
	public short getFlags() {
		return (short) flags;
	}
	
	/**
	 * 
	 * @return the version of the class which owns this item.
	 */
	public abstract BT_ClassVersion getVersion();
	
	/**
	 * sets the class file class, method or field flags to the given flags
	 */
	public void setFlags(short flags) {
		disableFlags((short) ~0);
		enableFlags(flags);
	}
	
	/**
	 * disables the non-zero bits in model flags
	 */
	void disableModelFlags(short flags) {
		this.flags &= ~(flags << 16);
	}
	
	
	/**
	 * enables the non-zero bits in the model flags
	 */
	void enableModelFlags(short flags) {
		this.flags |= (flags << 16);
	}
	
	/**
	 * 
	 * @param flags
	 * @return whether all of the non-zero bits in the model flags are set
	 */
	boolean areAnyModelEnabled(short flags) {
		return (this.flags & (flags << 16)) != 0;
	}
	
	/**
	 Model flags that indicate whether this item is in the project and is a stub.
	 */
	private final static short INPROJECT = 0x1;
	private final static short ISSTUB = 0x2;
	private final static short ISDEPRECATED = 0x8;
	
	/**
	 Model flags that indicate if this item would throw the indicated error at load time.
	**/
	protected final static short THROWSVERIFYERROR = 0x4;
	protected final static short THROWSCLASSCIRCULARITYERROR = 0x10;
	protected final static short THROWSCLASSFORMATERROR = 0x20;
	protected final static short THROWSNOCLASSDEFFOUNDERROR = 0x40;
	protected final static short THROWSUNSUPPORTEDCLASSVERSIONERROR = 0x80;
	protected final static short THROWSINCOMPATIBLECLASSCHANGEERROR = 0x100;
	
	
	/**
	 Indicates this is a class of interest.
	 True if this represents a {@link <a href=../jikesbt/doc-files/Glossary.html#project_class>project class</a>}.
	 False if is a {@link <a href=../jikesbt/doc-files/Glossary.html#system_class>system class</a>}.
	
	 <p> This should not be changed from false to true after the
	 BT_Class is constructed because system classes are modelled more
	 simply than project classes.
	 @see  #setInProjectFalse()
	**/
	public boolean inProject() {
		return areAnyModelEnabled(INPROJECT);
	}

	/**
	 A safer way to set field {@link BT_Item#inProject} since it should never be
	 changed from false to true because {@link <a
	 href=../jikesbt/doc-files/Glossary.html#external_class>external
	 classes</a>}.
	 read from class files are modelled more simply than
	 {@link <a href=../jikesbt/doc-files/Glossary.html#project_class>project classes</a>}.
	**/
	public void setInProjectFalse() {
		disableModelFlags(INPROJECT);
	}

	public final void setInProject(boolean inProject) {
		if (inProject)
			enableModelFlags(INPROJECT);
		else
			setInProjectFalse();
	}
	
	/**
	 True if this is synthetic as determined by {@link BT_Item#flags}.
	**/
	public boolean isSynthetic() {
		return areAnyEnabled(SYNTHETIC);
	}
	
	/**
	 * sets whether this class member is deprecated.
	 * @param deprecated whether the member is deprecated.
	 */
	public void setSynthetic(boolean synth) {
		if (synth) {
			/* if necessary, the attribute will be added when the class is written */
			enableFlags(SYNTHETIC);
		} else {
			attributes.removeAttribute(BT_GenericAttribute.SYNTHETIC_ATTRIBUTE_NAME);
			disableFlags(SYNTHETIC);
		}
	}
	
	/**
	 * @return whether this class member is deprecated.
	 */
	public boolean isDeprecated() {
		return areAnyModelEnabled(ISDEPRECATED);
	}
	
	/**
	 * sets whether this class member is deprecated.
	 * @param deprecated whether the member is deprecated.
	 */
	public void setDeprecated(boolean deprecated) {
		if (deprecated) {
			/* if necessary, the attribute will be added when the class is written */
			enableModelFlags(ISDEPRECATED);
		} else {
			attributes.removeAttribute(BT_GenericAttribute.DEPRECATED_ATTRIBUTE_NAME);
			disableModelFlags(ISDEPRECATED);
		}
	}
	
	/**
	 * This method is called just before the attributes are resolved in order to adjust the attributes vector as necessary.
	 */
	void resolveFlags() {
		if(isDeprecated() && !attributes.contains(BT_Attribute.DEPRECATED_ATTRIBUTE_NAME)) {
			attributes.addElement(BT_Attribute.getNewDeprecatedAttribute(this));
		}
		if(isSynthetic()) {
			if(getVersion().canUseSyntheticFlag()) {
				attributes.removeAttribute(BT_Attribute.SYNTHETIC_ATTRIBUTE_NAME);
			} else {
				if(!attributes.contains(BT_Attribute.SYNTHETIC_ATTRIBUTE_NAME)) {
					attributes.addElement(BT_Attribute.getNewSyntheticAttribute(this));
				}
			}
		}
	}
	
	/**
	 Indicates if this is a stub class or a stub interface, or method or field.
	 If true, one of {@link BT_Item#isClass} or {@link #isInterface} may also be true.
	 See {@link <a href=../jikesbt/doc-files/Glossary.html#stub_class>stub class</a>}.
	**/
	public boolean isStub() {
		return areAnyModelEnabled(ISSTUB);
	}

	/**
	 Set if this is a stub class or a stub interface, method or field.
	 If true, one of {@link BT_Item#isClass} or {@link #isInterface} may also be true.
	 See {@link <a href=../jikesbt/doc-files/Glossary.html#stub_class>stub class</a>}.
	**/
	public void setStub(boolean isStub) {
		if (isStub) {
			enableModelFlags(ISSTUB);
		}
		else
			disableModelFlags(ISSTUB);
	}

	/**
	 Just returns flag {@link BT_Item#THROWSVERIFYERROR}.
	**/
	public boolean throwsVerifyError() {
		return areAnyModelEnabled(THROWSVERIFYERROR);
	}

	/**
	 Sets flag {@link BT_Item#THROWSVERIFYERROR}.
	**/
	public void setThrowsVerifyErrorTrue() {
		enableModelFlags(THROWSVERIFYERROR);
	}
}
