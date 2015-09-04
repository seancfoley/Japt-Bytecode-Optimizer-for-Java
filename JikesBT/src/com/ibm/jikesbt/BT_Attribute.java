package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Any class file attribute.
 Specific attributes supported by JikesBT are subclasses of this class.
 Other attributes are represented by {@link BT_GenericAttribute}, which is
 also a subclass of this class.

 * @author IBM
**/
public abstract class BT_Attribute extends BT_Base implements Cloneable {

	public static final String DEPRECATED_ATTRIBUTE_NAME = "Deprecated";
	public static final String SYNTHETIC_ATTRIBUTE_NAME = "Synthetic";
	public static final String SOURCE_DEBUG_EXTENSION_ATTRIBUTE_NAME = "SourceDebugExtension";
	private static final byte emptyBytes[] = new byte[0];
	
	public static final String debugAttributes[] = new String[] {
		BT_SourceFileAttribute.ATTRIBUTE_NAME,
		BT_Attribute.SOURCE_DEBUG_EXTENSION_ATTRIBUTE_NAME,
		BT_LineNumberAttribute.ATTRIBUTE_NAME,
		BT_LocalVariableAttribute.LOCAL_VAR_ATTRIBUTE_NAME,
		BT_LocalVariableAttribute.LOCAL_VAR_TYPE_ATTRIBUTE_NAME,
	};

	//inner classes, synthetic, enclosing method, signature, and stackmaptable attributes are all required for jck/tck certification
	//inner classes and signature affect reflection
	//stackmaptable affects performance
	//deprecated is generally not needed
	public static final String infoAttributes[] = new String[] {
			BT_InnerClassesAttribute.ATTRIBUTE_NAME,//class
			BT_Attribute.SYNTHETIC_ATTRIBUTE_NAME, //class, field, method
			BT_EnclosingMethodAttribute.ATTRIBUTE_NAME, //class
			BT_Attribute.DEPRECATED_ATTRIBUTE_NAME, //method
			BT_SignatureAttribute.ATTRIBUTE_NAME,
		};
	
	public static final String annotationAttributes[] = new String[] {
		BT_RuntimeParamAnnotationsAttribute.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS_ATTRIBUTE_NAME,
		BT_RuntimeParamAnnotationsAttribute.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS_ATTRIBUTE_NAME,
		BT_AnnotationDefaultAttribute.ATTRIBUTE_NAME,
		BT_RuntimeAnnotationsAttribute.RUNTIME_VISIBLE_ANNOTATIONS_ATTRIBUTE_NAME,
		BT_RuntimeAnnotationsAttribute.RUNTIME_INVISIBLE_ANNOTATIONS_ATTRIBUTE_NAME
	};

	/**
	 The {@link BT_Class}, {@link BT_CodeAttribute}, {@link BT_Field}, or
	 {@link BT_Method} that contains this attribute.
	**/
	BT_AttributeOwner container;
	LoadLocation loadedFrom;

	public BT_Attribute(BT_AttributeOwner container, LoadLocation loadedFrom) {
		this.container = container;
		this.loadedFrom = loadedFrom;
	}
	
	public BT_Attribute(BT_AttributeOwner container) {
		this(container, null);
	}
	
	/**
	 Reads an attribute of a BT_Class, BT_CodeAttribute, BT_Field, or BT_Method.
	 Attributes in system classes are ignored.
	
	 @param   container  The BT_Class, BT_CodeAttribute, BT_Field, or BT_Method that contains this attribute.
	 @return  Null means the caller should ignore the attribute.
	**/
	static BT_Attribute read(DataInputStream di, BT_ConstantPool pool, 
			BT_AttributeOwner container, BT_Factory factory, LoadLocation loadedFrom)
		throws BT_ClassFileException, BT_AttributeException, IOException {
			
		// Class-file format (according to VM spec):
		//     attribute_info {
		//             u2 attribute_name_index;
		//             u4 attribute_length;
		//             u1 info[attribute_length];
		//    
		String name;
		int attributeLength;
		try {
			name = pool.getUtf8At(di.readUnsignedShort());
			attributeLength = di.readInt();
		} catch(BT_ConstantPoolException e) {
			throw new BT_ClassFileException(e);
		}
		if(attributeLength < 0) {
			throw new BT_ClassFileException(Messages.getString("JikesBT.attribute_length_too_long_107"));
			//di.readInt & 0xffffffffL is the correct length (it's unsigned) 
			//there is no di.readUnsignedInt
			//but we cannot
			//index the array with a long, so this does not handle big arrays, we should probably pass on the very same stream
			//ie converting to an array not a great idea
		}
		
		byte data[] = new byte[attributeLength];
		di.readFully(data);
				
		// Check for known attributes
		//
		if(container instanceof BT_Item) {
			BT_Item item = (BT_Item) container;
			if (name.equals(SYNTHETIC_ATTRIBUTE_NAME)) {
				item.setSynthetic(true);
				/* if necessary, the attribute will be created lazily when the item is resolved */
				return null;
			}
			if(name.equals(DEPRECATED_ATTRIBUTE_NAME)) {
				item.setDeprecated(true);
				/* if necessary, the attribute will be created lazily when the item is resolved */
				return null;
			}
			if (name.equals(BT_SignatureAttribute.ATTRIBUTE_NAME)) {
				return new BT_SignatureAttribute(data, pool, item, loadedFrom);
			}
			if(name.equals(BT_RuntimeAnnotationsAttribute.RUNTIME_VISIBLE_ANNOTATIONS_ATTRIBUTE_NAME)
					|| name.equals(BT_RuntimeAnnotationsAttribute.RUNTIME_INVISIBLE_ANNOTATIONS_ATTRIBUTE_NAME)) {
				return new BT_RuntimeAnnotationsAttribute(data, pool, name, item, loadedFrom);
			}
		}
		
		if (container instanceof BT_Method) {
			BT_Method method = (BT_Method) container;
			if (!method.cls.inProject()) {
				return null; // Not of interest
			}
			if (name.equals(BT_CodeAttribute.ATTRIBUTE_NAME)) {
				return new BT_CodeAttribute(data, method, pool, loadedFrom);
			}
			if (name.equals(BT_ExceptionsAttribute.ATTRIBUTE_NAME)) {
				return new BT_ExceptionsAttribute(data, pool, method, loadedFrom);
			}
			if(name.equals(BT_RuntimeParamAnnotationsAttribute.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS_ATTRIBUTE_NAME)
					|| name.equals(BT_RuntimeParamAnnotationsAttribute.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS_ATTRIBUTE_NAME)) {
				return new BT_RuntimeParamAnnotationsAttribute(data, pool, name, method, loadedFrom);
			}
			if (name.equals(BT_AnnotationDefaultAttribute.ATTRIBUTE_NAME)) {
				return new BT_AnnotationDefaultAttribute(data, pool, method, loadedFrom);
			}
		} else if (container instanceof BT_Class) {
			BT_Class containerClass = (BT_Class) container;
			if (!containerClass.inProject()) {
				return null; // Not of interest
			}
			if (name.equals(BT_InnerClassesAttribute.ATTRIBUTE_NAME)) {
				return new BT_InnerClassesAttribute(data, pool, containerClass, loadedFrom);
			}
			if (name.equals(BT_SourceFileAttribute.ATTRIBUTE_NAME)) {
				if(pool.getRepository().factory.readDebugInfo) {
					return new BT_SourceFileAttribute(data, pool, containerClass, loadedFrom);
				} else {
					return null;
				}
			}
			if (name.equals(BT_EnclosingMethodAttribute.ATTRIBUTE_NAME)) {
				return new BT_EnclosingMethodAttribute(data, pool, containerClass, loadedFrom);
			}
			if (name.equals(SOURCE_DEBUG_EXTENSION_ATTRIBUTE_NAME)) {
				if(pool.getRepository().factory.readDebugInfo) {
					return pool.getRepository().createGenericAttribute(name, data, containerClass, pool, loadedFrom);
				} else {
					return null;
				}
			}
		} else if (container instanceof BT_Field) {
			BT_Field field = (BT_Field) container;
			if (!field.cls.inProject()) {
				return null; // Not of interest
			}
			if (name.equals(BT_ConstantValueAttribute.ATTRIBUTE_NAME)) {
				return new BT_ConstantValueAttribute(data, pool, field.getTypeName(), field, loadedFrom);
			}
		} else if (container instanceof BT_CodeAttribute) {
			BT_CodeAttribute containerCode = (BT_CodeAttribute) container;
			if (name.equals(BT_LineNumberAttribute.ATTRIBUTE_NAME)) {
				if (pool.getRepository().factory.readDebugInfo) {
					return new BT_LineNumberAttribute(data, pool, containerCode, loadedFrom);
				} else {
					return null;
				}
			}
			if (name.equals(BT_LocalVariableAttribute.LOCAL_VAR_ATTRIBUTE_NAME)
					|| name.equals(BT_LocalVariableAttribute.LOCAL_VAR_TYPE_ATTRIBUTE_NAME)) {
				if (pool.getRepository().factory.readDebugInfo) {
					return new BT_LocalVariableAttribute(name, data, pool, containerCode, loadedFrom);
				} else {
					return null;
				}
			}
			if(name.equals(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME) 
					|| name.equals(BT_StackMapAttribute.CLDC_STACKMAP_NAME)) {
				if (pool.getRepository().factory.readStackMaps) {
					try {
						return new BT_StackMapAttribute(data, pool, containerCode, name, loadedFrom);
					} catch(BT_AttributeException e) {
						/* If the format of the existing one is bad, we create another that we will populate ourselves */
						factory.noteAttributeLoadFailure(pool.getRepository(), container.getEnclosingItem(), name, null, e, loadedFrom);
					}
				}
				return new BT_StackMapAttribute(containerCode, name);
			}
		}
		return pool.getRepository().createGenericAttribute(name, data, container, pool, loadedFrom);
	}
	
	static BT_Attribute getNewDeprecatedAttribute(BT_AttributeOwner container) {
		return new BT_GenericAttribute(DEPRECATED_ATTRIBUTE_NAME, emptyBytes, container);
	}
	
	static BT_Attribute getNewSyntheticAttribute(BT_AttributeOwner container) {
		return new BT_GenericAttribute(SYNTHETIC_ATTRIBUTE_NAME, emptyBytes, container);
	}
	
	/**
	 Converts the representation to directly reference related objects
	 (instead of using class-file artifacts such as indices and
	 offsets to identify them).
	 For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>.
	 @throws BT_AttributeException the attribute is invalid, although the class file integrity remains valid.
	 	BT_Factory.noteAttributeLoadFailure will handle this occurrence.
	 @throws BT_ClassFileException the attribute is invalid and this means the class file is also invalid.
	**/
	// Not protected since users only override BT_GenericAttribute
	void dereference(BT_Repository rep) 
		throws BT_ClassFileException, BT_AttributeException {}

	/**
	 * Undo any relationships initiated by the attribute.
	 */
	void remove() {
		getOwner().getAttributes().removeElement(this);
	}
	
	/**
	 * Undo any reference to the given item.
	 */
	void removeReference(BT_Item reference) {}
	
	/**
	 Updates all references that are "contained in" this object and that
	 refer to an "old" instruction so they refer to a "new" one.  The 
	 new one may be null.
	 @param switching true if oldIns and newIns are not in the same code attribute
	**/
	public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching) {}

	/**
	 Build the constant pool, ... in preparation for writing the class-file.
	 Overriding methods should call this version.
	 For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#resolve_method>resolve method</a>.
	 
	 @throws BT_AttributeException if the attribute could not be resolved
	 @throws BT_ClassFileException if the attribute could not be resolved, and this renders the associated class invalid
	**/
	public void resolve(BT_ConstantPool pool) throws BT_AttributeException, BT_ClassWriteException {
		pool.indexOfUtf8(getName());
	}

	/**
	 Return the number of bytes that write(...) will write.
	
	 <p> It is expected that this base class version will never be called.
	 Attributes that are attached to a BT_CodeAttribute must override this method,
	 other attributes need not.
	**/
	int writtenLength() {
		throw new UnsupportedOperationException(Messages.getString("JikesBT.method___writtenLength()___must_be_overridden_or_not_called_8"));
	}
	
	/**
	 Writes this attribute to a class file.
	 
	 @throws BT_AttributeException if the attribute could not be written
	**/
	abstract void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException;

	/**
	 Returns the name of the attribute (e.g., "Code").
	**/
	public abstract String getName();

	/**
	 Returns true if only one instance of this attribute type is allowed in a container.
	**/
	public boolean singletonRequired() {
		return true;
	}
	
	/**
	 Returns the class, method, field or attribute that owns this attribute.
	**/
	public BT_AttributeOwner getOwner() {
		return container;
	}

	public void print(java.io.PrintStream ps, String prefix) {
		ps.println(prefix + this);
	}

	public abstract String toString();
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}