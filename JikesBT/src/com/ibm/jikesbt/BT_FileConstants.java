/*
 * Created on Jun 20, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public interface BT_FileConstants {
	/**
	 The currently used factory.
	 Initialized to "new BT_Factory()".
	public static BT_Factory factory;
	**/

	/**
	 Used to represent qualified names in class files.
	 Similar to {@link BT_ConstantPool#JAVA_FILE_SEPARATOR_SLASH}.
	**/
	public static final char ZIPFILE_SEPARATOR_SLASH = '/';
	
	public static final char UNIX_SEPARATOR_SLASH = '/';
	
	public static final char DOSFILE_SEPARATOR_BACKSLASH = '\\';
}
