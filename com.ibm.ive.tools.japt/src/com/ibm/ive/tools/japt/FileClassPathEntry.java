/*
 * Created on Mar 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author sfoley
 *
 * A class path entry that exists solely to load a class or resource
 * directly from a file object.  For classes loaded by such an entry, the name of the
 * class need not match the directory structure relative to the loading classpath.
 * 
 */
public abstract class FileClassPathEntry extends ClassPathEntry {

	protected File file;
	private BT_FileLocation location;
	private boolean initialized;
	protected static final BT_FileLocation[] noLocations = new BT_FileLocation[0];
	
	/**
	 * @param file
	 * @throws IOException
	 */
	public FileClassPathEntry(File file) throws IOException {
		super(file.getParentFile());
		this.file = file;
	}
	
	public Iterator getLocations(final PatternString pattern) {
		return new Iterator() {
			BT_FileLocation iteratorLocation; {
				try {
					iteratorLocation = getLocation();
					if(!pattern.isMatch(iteratorLocation.getName())) {
						iteratorLocation = null;
					}
				} catch(IOException e) {}
			}
			
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			public boolean hasNext() {
				return iteratorLocation != null;
			}
			
			public Object next() {
				if(iteratorLocation == null) {
					throw new NoSuchElementException();
				}
				BT_FileLocation result = iteratorLocation;
				iteratorLocation = null;
				return result;
			}
		};
	}
	
	public BT_FileLocation getLocation() throws IOException {
		if(location == null) {
			if(initialized) {
				throw new IOException();
			}
			initialized = true;
			location = initializeLocation();
		}
		return location;
	}
	
	/*
	 * the given file name does not hold enough information to provide the full class name
	 *  (non-Javadoc)
	 * @see com.ibm.jikesbt.BT_ClassPathEntry#getClassName(java.lang.String)
	 */
	public String getClassName(String name) {
		//TODO use the BT_ClassInfoUntilName to peek at the class name, 
		//although I'm not sure I really want to go that far rather than return null
		//Also note that this does not necessarily make sense for all subclasses, possible just the subclass for class files
		//and not resource locations (which refer to no class at all) or assembly file locations
		return null;
	}
	
	/**
	 * Override the parent behaviour.  Do not search the subdirectory tree for files.
	 * Just check if the single file that can be loaded by this classpath entry is a match.
	 */
	public BT_ClassPathLocation[] findFileWithPattern(PatternString pattern) {
		try {
			BT_FileLocation location = getLocation();
			String entryName = location.getName();
			return pattern.isMatch(entryName) ? new BT_ClassPathLocation[] {location} : noLocations;
		}
		catch(IOException e) {}
		return noLocations;
	}
	
	/**
	 * Override the parent behaviour.  Do not search the subdirectory tree for files.
	 * Just check if the single file that can be loaded by this classpath entry is a match.
	 */
	public BT_ClassPathLocation findFile(String fileName) {
		try {
			BT_FileLocation location = getLocation();
			if(location != null) {
				String entryName = location.getName();
				return fileName.equals(entryName) ? location : null;
			}
		}
		catch(IOException e) {}
		return null;
	}
	
	public boolean isDirectory() {
		return false;
	}
	
	public boolean isArchive() {
		return false;
	}
	
	protected abstract BT_FileLocation initializeLocation() throws IOException;
	
	public String toString() {
		//String s = Messages.getString("JikesBT.Class_Path_Entry___2");
		String s = "file: ";
		return s + entryCanonicalName_;
	}
}
