/*
 * Created on Oct 15, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ibm.jikesbt.BT_ClassFileException;
import com.ibm.jikesbt.BT_ClassInfoUntilName;
import com.ibm.jikesbt.BT_Repository;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ClassFileClassPathEntry extends FileClassPathEntry {

	private BT_Repository repository;
	
	/**
	 * @param file
	 * @param repository
	 * @throws IOException
	 */
	public ClassFileClassPathEntry(File file, BT_Repository repository)
			throws IOException {
		super(file);
		this.repository = repository;
	}
	
	protected BT_FileLocation initializeLocation() throws IOException {
		try {
			String className = sniffClassName(new FileInputStream(file), file, repository);
			BT_FileLocation location = new BT_FileLocation(file.getName(), file);
			location.className = className;
			return location;
		} catch(BT_ClassFileException e) {
			throw new IOException();
		}
	}
	
	static String sniffClassName(InputStream inputStream, File file, BT_Repository repository) throws IOException, BT_ClassFileException {
		BT_ClassInfoUntilName ciun = new BT_ClassInfoUntilName();
		DataInputStream dis = new DataInputStream(inputStream);
		ciun.readUntilName(dis, file, repository);
		dis.close();
		return ciun.className;
	}
	
	public BT_ClassPathLocation[] findClassWithPattern(PatternString pattern) {
		try {
			BT_FileLocation location = getLocation();
			String className = location.getClassName();
			return pattern.isMatch(className) ? new BT_ClassPathLocation[] {location} : noLocations;
		}
		catch(IOException e) {}
		return noLocations;
	}
		
	public BT_ClassPathLocation findClass(String className) {
		try {
			BT_FileLocation location = getLocation();
			String locationClassName = location.getClassName();
			return className.equals(locationClassName) ? location : null;
		}
		catch(IOException e) {}
		return null;
	}

}
