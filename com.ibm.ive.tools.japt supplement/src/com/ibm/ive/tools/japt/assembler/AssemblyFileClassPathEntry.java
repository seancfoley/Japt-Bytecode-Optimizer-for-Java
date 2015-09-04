/*
 * Created on Oct 15, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.assembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ibm.ive.tools.japt.FileClassPathEntry;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.BT_Repository;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AssemblyFileClassPathEntry extends FileClassPathEntry {

	private AssemblerMessages messages;
	private Logger logger;
	private JaptRepository repository;
	private ClassLoadingQueue queue;
	private BT_ClassVersion classVersion;
	
	/**
	 * @param file
	 * @param repository
	 * @throws IOException
	 */
	public AssemblyFileClassPathEntry(
			AssemblerMessages messages, 
			Logger logger, 
			File file, 
			JaptRepository repository,
			ClassLoadingQueue queue,
			BT_ClassVersion classVersion)
			throws IOException {
		super(file);
		this.repository = repository;
		this.messages = messages;
		this.logger = logger;
		this.queue = queue;
		this.classVersion = classVersion;
	}
	
	protected BT_FileLocation initializeLocation() throws IOException {
		String className = sniffClassName();
		BT_FileLocation location = new BT_FileLocation(file.getName(), file);
		location.className = className;
		return location;
	}
	
	String sniffClassName() {
		String result = null;
		Scanner scanner = null;
		InputStream is = null;
		try {
			is = new FileInputStream(file);
	        byte buffer[] = new byte[is.available()];
	        int bytes = 0, totalBytes = 0;
	        while(bytes >= 0 && totalBytes < buffer.length) {
	        	bytes = is.read(buffer, totalBytes, buffer.length - totalBytes);
	        	totalBytes += bytes;
	        }
	        scanner = new Scanner(buffer, file.getName());
	        Parser p = new Parser(repository, AssemblyFileClassPathEntry.this, scanner, classVersion);
	        result = p.parseName();
	    }
		catch(IOException e) {
			messages.ERROR_SOURCE.log(logger, file.getName());
		}
		catch(UnexpectedTokenException e) {
			messages.ERROR_SOURCE_LINE.log(logger, new Object[] {Integer.toString(e.line), file.getName()});
		} finally {
			if( is!= null) {
				try {
					is.close();
				} catch(IOException e) {
					repository.getFactory().noteFileCloseIOException(file.getName(), e);
				}
			}
		}
		return result;
	}

	protected BT_Class loadClass(String className, BT_Repository rep, BT_ClassPathLocation loc, BT_Class stub) {
		return queue.loadClass(className, loc, stub);
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
