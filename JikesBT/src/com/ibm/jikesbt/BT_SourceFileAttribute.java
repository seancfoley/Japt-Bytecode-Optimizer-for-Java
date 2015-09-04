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
 The "SourceFile" attribute of a class-file, that stores the name of the
 class file's source file.
 See {@link BT_Class#getSourceFile()} and {@link BT_Class#setSourceFile}.
 * @author IBM
**/
public final class BT_SourceFileAttribute extends BT_Attribute {

	/**
	 The name of this attribute.
	**/
	public static final String ATTRIBUTE_NAME = "SourceFile";

	public String getName() {
		return ATTRIBUTE_NAME;
	}

	/**
	 The source file name.
	**/
	public String fileName;

	
	public BT_SourceFileAttribute(String fileName, BT_Class clazz) {
		super(clazz);
		this.fileName = fileName;
	}

	public BT_SourceFileAttribute(byte data[], BT_ConstantPool pool, BT_Class clazz, LoadLocation loadedFrom)
			throws BT_AttributeException {
		super(clazz, loadedFrom);
		try {
			int index = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
			fileName = pool.getUtf8At(index);
		} catch(BT_ConstantPoolException e) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, e);
		}
	}

	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(getName());
		pool.indexOfUtf8(fileName);
	}

	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		//BT_Repository.debugRecentlyWrittenAttribute = this;
		dos.writeShort(pool.indexOfUtf8(getName()));
		dos.writeInt(2);
		dos.writeShort(pool.indexOfUtf8(fileName));
	}

	public String toString() {
		return ATTRIBUTE_NAME + "=" + fileName;
	}
}
