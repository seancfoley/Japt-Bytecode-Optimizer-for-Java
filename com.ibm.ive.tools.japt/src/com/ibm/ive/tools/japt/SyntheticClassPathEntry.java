/*
 * Created on Mar 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author sfoley
 *
 * A class path entry that exists solely for creating new classes and new resources.
 * We must create such an entry so that when the class path entries are searched for the classes
 * they have loaded, the classes loaded here are not missed.
 * 
 * All classes and resources pertain to a class path entry. 
 * Such class path entries may correspond to archives (jar or zip), directories, class files,
 * or synthetic ones.
 * 
 * The parent class ClassPathEntry keeps track of classes and resources pertaining to this class path entry.
 * 
 */
public class SyntheticClassPathEntry extends ClassPathEntry {

	private String name;
	private boolean isArchive;
	
	/**
	 * @param file
	 * @throws IOException
	 */
	public SyntheticClassPathEntry(String name) {
		this.name = name;
	}
	
	/**
	 * @param file
	 * @throws IOException
	 */
	public SyntheticClassPathEntry(String name, boolean isArchive) {
		this.name = name;
		this.isArchive = isArchive;
	}
	
	public Resource[] getResources(boolean includeZipped) {
		return super.getResources(false);
	}
	
	public Iterator getLocations(PatternString pattern) {
		return new Iterator() {
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			public boolean hasNext() {
				return false;
			}
			
			public Object next() {
				throw new NoSuchElementException();
			}
		};
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isDirectory() {
		return false;
	}
	
	public boolean isArchive() {
		return isArchive;
	}
	
	/*
	 * since this type of classpath entry cannot generate classes on its own,
	 * we do not generate class names either
	 *  (non-Javadoc)
	 * @see com.ibm.jikesbt.BT_ClassPathEntry#getClassName(java.lang.String)
	 */
	public String getClassName(String name) {
		return null;
	}
	
	/**
	 * Override the parent behaviour.  Do not search for files.
	 */
	public BT_ClassPathLocation findFile(String fileName) {
		return null;
	}
	
	/**
	 * Override the parent behaviour.  Do not search for files.
	 */
	public BT_ClassPathLocation[] findFileWithPattern(PatternString pattern) {
		return noLocations;
	}
	
	public boolean equals(Object o) {
		return o == this;
	}
	
	public String getEntryCanonicalName() {
		return getName();
	}
	
	public String toString() {
		//String s = Messages.getString("JikesBT.Class_Path_Entry___2");
		String s = "synthetic class path entry: ";
		return s + getName();
	}
}
