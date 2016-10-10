/*
 * Created on Oct 8, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.assembler;

import java.io.File;
import java.io.IOException;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.JaptClass;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Repository;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AssemblerClassPathEntry extends ClassPathEntry {

	private ClassLoadingQueue queue;
	private String fileExtension;
	
	/**
	 * @param file
	 * @throws IOException
	 */
	public AssemblerClassPathEntry(
			File file, 
			ClassLoadingQueue queue,
			String fileExtension) throws IOException {
		super(file);
		this.queue = queue;
		this.fileExtension = fileExtension;
	}
	
	public BT_ClassPathLocation[] findClassWithPattern(PatternString pattern) {
		String newPattern = fileNameForClassName(pattern.getString(), fileExtension);
		PatternString newPatternString = new PatternString(newPattern);
		return findFileWithPattern(newPatternString);
	}

	public BT_ClassPathLocation findClass(String className) {
		String fileName = fileNameForClassName(className, fileExtension);
		BT_ClassPathLocation result = findFile(fileName);
		return result;
	}
	
	public String getClassName(String name) {
		if(name.endsWith(fileExtension)) {
			return classFileNameToClassName(name);
		}
		return null;
	}
	
	@Override
	protected BT_Class loadClass(String className, BT_Repository repository, BT_ClassPathLocation loc, BT_Class stub) {
		return queue.loadClass(className, loc, (JaptClass) stub);
	}
}
