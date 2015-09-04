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
 The "ConstantValue" attribute in a field in a class-file, that contains
 the initial value for a constant field.

 * @author IBM
**/
/* The format in the class file:
                ConstantValue_attribute {
                    u2 attribute_name_index;
                    u4 attribute_length;
                    u2 constantvalue_index;  -- Index of one of:
                           CONSTANT_Long
                           CONSTANT_Float
                           CONSTANT_Double
                           CONSTANT_Integer
                           CONSTANT_String
                }
*/
public final class BT_ConstantValueAttribute extends BT_Attribute {

	/**
	 The name of this attribute.
	**/
	public static final String ATTRIBUTE_NAME = "ConstantValue";

	public String getName() {
		return ATTRIBUTE_NAME;
	}

	private final int dataLength = 2;
	private BT_AnyConstantValue value;

	/**
	 This form of constructor is used when a class file is read.
	 @param data  The part of the attribute value following "attribute_length" from the class file.
	**/
	BT_ConstantValueAttribute(
		byte data[],
		BT_ConstantPool pool,
		String fieldType,
		BT_AttributeOwner owner,
		LoadLocation loadedFrom)
			throws BT_ClassFileException {
		super(owner, loadedFrom);
		if (data.length != dataLength) {
			throw new BT_ClassFileException(Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
		}
		int poolIndex = BT_Misc.bytesToUnsignedShort(data, 0);
		try {
			/* note that when we create the constant value attribute, we load the class
			 * right away, we do not use a dereference step to do so.  THis is because
			 * the class can be at most String, Class or a primitive.  So there would not be a possible recursion
			 * of class loading.
			 */
			value = BT_AnyConstantValue.create(pool, poolIndex, fieldType);
		} catch(BT_ConstantPoolException e) {
			throw new BT_ClassFileException(e);
		}
	}

	public int cpIndex(BT_ConstantPool pool) {
		return pool.indexOfItem(value);
	}

	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(ATTRIBUTE_NAME);
		cpIndex(pool);
	}

	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(pool.indexOfUtf8(ATTRIBUTE_NAME));
		dos.writeInt(2);
		dos.writeShort(cpIndex(pool));
	}

	/**
	 @return  The constant, but null if there was an error reading the value
	   and BT_Factory.factory.exception continued JikesBT's execution.
	**/
	public BT_AnyConstantValue getValue() {
		return value;
	}

	public String toString() {
		String val = value.toString();
		return getName() + ": " + val;
	}
}
