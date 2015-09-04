/*
 * Created on Feb 20, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.out;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Resource;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassWriteException;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Misc;

public class DirGenerator extends Generator {

	File baseDir;
	Messages messages;
	Logger logger;
	
	public DirGenerator(File baseDir, Messages messages, Logger logger) {
		this.baseDir = baseDir;
		this.messages = messages;
		this.logger = logger;
	}
	
	public void write(BT_Class clazz) throws IOException, BT_ClassWriteException {
		OutputStream outputStream = getOutputStream(clazz, ".class");
		DataOutputStream dos = new DataOutputStream(outputStream);
		clazz.write(dos);
		dos.close();
		if(loader != null) {
			loader.addWrittenClass(clazz);
		}
	}
	
	private OutputStream getOutputStream(BT_Class clazz, String fileExtension) throws IOException {
		String name = clazz.getName().replace('.', '/') + fileExtension;
		int last = name.lastIndexOf('/');
		File subdir = (last < 0) ? baseDir : new File(baseDir, name.substring(0, last));
		if(!subdir.exists() && !subdir.mkdirs()) {
			throw new IOException(messages.ERROR_WRITING.toString(subdir.getName()));
		}
		File subFile = new File(baseDir, name);
		FileOutputStream fos = new FileOutputStream(subFile);
		return fos;
	}
	
	public void write(Resource resource) throws IOException {
		String name = resource.getName();
		int last = name.lastIndexOf('/');
		File subdir = (last < 0) ? baseDir : new File(baseDir, name.substring(0, last));
		if(!subdir.exists() && !subdir.mkdirs()) {
			throw new IOException(messages.ERROR_WRITING.toString(subdir.getName()));
		}
		File subFile = new File(baseDir, name);
		FileOutputStream fos = new FileOutputStream(subFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		byte buffer[] = new byte[1024];
		InputStream inStream = resource.getInputStream();
		int numBytes = inStream.read(buffer);
		while(numBytes != -1) {
			bos.write(buffer, 0, numBytes);
			numBytes = inStream.read(buffer);
		}
		bos.close();
	}

	public void writeAssemblyClass(BT_Class clazz, String fileExtension) throws IOException, BT_CodeException {
		OutputStream outputStream = getOutputStream(clazz, fileExtension);
		PrintStream ps = new PrintStream(outputStream);
		clazz.print(ps, BT_Misc.PRINT_IN_ASSEMBLER_MODE);
		ps.close();
	}
	
	public void write(Manifest manifest) throws IOException {
		String name = JarFile.MANIFEST_NAME;
		int last = name.lastIndexOf('/');
		File subdir = (last < 0) ? baseDir : new File(baseDir, name.substring(0, last));
		if(!subdir.exists() && !subdir.mkdirs()) {
			throw new IOException(messages.ERROR_WRITING.toString(subdir.getName()));
		}
		File subFile = new File(baseDir, name);
		FileOutputStream fos = new FileOutputStream(subFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		manifest.write(bos);
		bos.close();
	}

	public void close() throws IOException {}
}
