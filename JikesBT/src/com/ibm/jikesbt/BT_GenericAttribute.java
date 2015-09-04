package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Represents attribute other than the standard ones.
 I.e., not Code, ConstantValue, Exceptions, InnerClasses, LineNumberTable,
 nor LocalVariableTable,
 * @author IBM
**/
public final class BT_GenericAttribute extends BT_Attribute {

	/**
	 The name of the attribute.
	**/
	public final String name;

	public String getName() {
		return name;
	}

	/**
	 The part of the attribute value following "attribute_length" from the
	 class file.
	**/
	public byte data[];

	
	/**
	 @param nm         The name of the attribute.
	 @param data       The part of the attribute value following
	   "attribute_length" from the class file.
	 @param container  The BT_Class, BT_CodeAttribute, BT_Field, or BT_Method
	   that contains this attribute.
	**/
	public BT_GenericAttribute(String nm, byte[] data, BT_AttributeOwner container, LoadLocation loadedFrom) {
		super(container, loadedFrom);
		name = nm;
		this.data = data;
	}
	
	public BT_GenericAttribute(String nm, byte[] data, BT_AttributeOwner container) {
		this(nm, data, container, null);
	}

	public boolean singletonRequired() {
		return false;
	}
	
	/**
	 Converts the representation to directly reference related objects
	 (instead of using class-file artifacts such as indices and
	 offsets to identify them).
	 For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>.
	**/
	protected void dereference(BT_Repository rep) {
	}

	/**
	 Return the number of bytes that {@link BT_GenericAttribute#write} will write.
	 This must be kept in synch with write.
	**/
	protected int writtenLength() {
		return 2 // attribute_name_index
		+4 // attribute_length
		+data.length; // ...
	}

	/**
	 Writes this attribute to a class file.
	 This must be kept in synch with {@link BT_GenericAttribute#writtenLength()}.
	**/
	// This must be kept in synch with {@link BT_GenericAttribute#writtenLength()}.
	protected void write(DataOutputStream dos, BT_ConstantPool pool)
		throws IOException {
		dos.writeShort(pool.indexOfUtf8(name)); // attribute_name_index
		dos.writeInt(data.length); // attribute_length
		dos.write(data, 0, data.length); // ...
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_<{1}_bytes>", new Object[] {name, Integer.toString(data.length)});
	}
}
