/*
 * Created on Feb 20, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.out;

import java.io.IOException;
import java.util.jar.Manifest;

import com.ibm.ive.tools.japt.Resource;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassWriteException;
import com.ibm.jikesbt.BT_CodeException;

public abstract class Generator {
	AutoLoaderCreator loader;
	
	void setLoader(AutoLoaderCreator loader) {
		this.loader = loader;
	}
	
	public abstract void write(Manifest manifest) throws IOException;
	
	public abstract void write(BT_Class clazz) throws IOException, BT_ClassWriteException;
	
	public abstract void write(Resource resource) throws IOException;
	
	public abstract void writeAssemblyClass(BT_Class clazz, String fileExtension) throws IOException, BT_CodeException;
	
	public abstract void close() throws IOException;
}
