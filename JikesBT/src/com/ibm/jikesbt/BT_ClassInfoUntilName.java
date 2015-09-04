package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


import java.io.DataInputStream;
import java.io.IOException;

/**
 The info of a class that is read until its name is known.
 * @author IBM
**/
// Added class to allow replacement of stub classes by read classes.
public class BT_ClassInfoUntilName extends BT_Base {
	public int minorVersion;
	public int majorVersion;
	public BT_ConstantPool pool;
	public short flags;
	public String className;

	/**
	 Reads start of class from a class file until its name is known.
	 @param  dis   An input stream from which class is read.
	 @param  file  A java.io.File or a java.util.zip.ZipFile.
	**/
	public void readUntilName(DataInputStream dis, Object file, BT_Repository repo)
		throws BT_ClassFileException, IOException {
		int magicRead = dis.readInt();
		if (magicRead != BT_Class.MAGIC)
			throw new BT_ClassFileException(Messages.getString("JikesBT.bad_magic____not_a_Java_class_file_1"));
		minorVersion = dis.readUnsignedShort();
		majorVersion = dis.readUnsignedShort();

		pool = repo.createConstantPool();
		if (CHECK_USER && 1 != pool.size())
			assertFailure(Messages.getString("JikesBT.The_constant_pool_should_be_empty_2"));
		try {
			pool.read(dis);
			flags = dis.readShort();
			className =
				pool.getClassNameAt(dis.readUnsignedShort(), BT_ConstantPool.CLASS);
		} catch(BT_ConstantPoolException e) {
			throw new BT_ClassFileException(e);
		} catch(BT_DescriptorException e) {
			throw new BT_ClassFileException(e);
		}
	}

}
