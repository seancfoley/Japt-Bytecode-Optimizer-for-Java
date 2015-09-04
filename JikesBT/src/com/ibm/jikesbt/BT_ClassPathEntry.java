package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Represents an entry in the classpath (hopefully a jar file, a zip file,
 or a directory).

 * @author IBM
**/
public class BT_ClassPathEntry extends BT_Base implements BT_FileConstants {

	protected String entryCanonicalName_;
	protected File entryFile_; // Hopefully, a zip file or a directory
	protected ZipFile zipFile_;
	protected Vector zipFileDirectory_;
	private String setName;
	
	/**
	 @param  file  An existing file system member.
	   Hopefully, a directory or .jar/.zip file name.
	**/

	protected BT_ClassPathEntry() {}

	//   Resolves zip files during construction.
	protected BT_ClassPathEntry(File file) throws IOException {
		if(file == null) {
			throw new IOException();
		}
		entryFile_ = file;
		entryCanonicalName_ = entryFile_.getCanonicalPath();
		// Throws IOException
		if (entryFile_.isFile()) {
			zipFile_ = new ZipFile(entryFile_);
			// Throws ZipException, IOException
			zipFileDirectory_ = new Vector();
			Enumeration e = zipFile_.entries();
			// Why are these cached now?  Why not just use the directory in the .zip file?
			while (e.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) e.nextElement();
				if(!ze.isDirectory()) {
					zipFileDirectory_.addElement(ze);
				}
			}
		} else { // Not a file
			zipFile_ = null;
			zipFileDirectory_ = null;
			if (CHECK_JIKESBT && !entryFile_.isDirectory())
				expect(
					Messages.getString("JikesBT.Found_{0}_but_it_is_neither_a_file_nor_a_dictionary", entryCanonicalName_));
		}
	}
	
	public BT_ClassPathLocation findClass(String className) {
		return findFile(fileNameForClassName(className));
	}
	
	/**
	 Tries to find the specified file in this classpath entry.
	 
	 @param  fileName  Should use {@link BT_Factory#ZIPFILE_SEPARATOR_SLASH}
	   ('/') as delimiter because that's what is in .zip files.
	   @return null or the location of the file
	**/
	public BT_ClassPathLocation findFile(String fileName) {
		if (zipFileDirectory_ == null) {
			// Hopefully a file-system directory (not a .zip file)
			File f = new File(entryFile_, fileName);
			if (f.exists()) { // If the file exists in the directory
				return getLocation(fileName, f);
			}
		} else { // Is a .zip file
			// See if the file exists in the zipfile.
			for (int k = 0; k < zipFileDirectory_.size(); k++) {
				ZipEntry ze = (ZipEntry) zipFileDirectory_.elementAt(k);
				String entryName = ze.getName();
				if (entryName.equals(fileName)) {
					return getLocation(ze);
				}
			}
		}
		return null;
	}
	
	protected BT_ClassPathLocation getLocation(String fileName, File file) {
		return new BT_FileLocation(fileName, file);
	}
	
	protected BT_ClassPathLocation getLocation(ZipEntry ze) {
		return new BT_ZipLocation(ze);
	}

	public String getEntryCanonicalName() {
		return entryCanonicalName_;
	}

	protected ZipFile getZipFile() {
		return zipFile_;
	}
	
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if(o == null) {
			return false;
		}
		//ensure we have the same class/subclass type
		if(!getClass().equals(o.getClass())) {
			return false;
		}
		//if the class/subclass is the same type, check the location
		BT_ClassPathEntry other = (BT_ClassPathEntry) o;
		boolean result = entryCanonicalName_.equals(other.entryCanonicalName_);
		return result;
	}

	public String toString() {
		//String s = Messages.getString("JikesBT.Class_Path_Entry___2");
		String s = "";
		if (zipFile_ != null)
			s += Messages.getString("JikesBT.zip_file__3");
		else
			s += Messages.getString("JikesBT.directory__4");
		return s + entryCanonicalName_;
	}
	
	/**
	 * Whether this class path entry originates from a directory.
	 * 
	 * Note that a class path entry need not be either an archive or a directory, it may be neither.
	 * Alternative class path entries are sometimes neither.
	 * 
	 * @return
	 */
	public boolean isDirectory() {
		return zipFile_ == null;
	}
	
	/**
	 * Whether this class path entry originates from an archive.
	 * 
	 * Note that a class path entry need not be either an archive or a directory, it may be neither.
	 * Alternative class path entries are sometimes neither.
	 * 
	 * @return
	 */
	public boolean isArchive() {
		return zipFile_ != null;
	}
	
	public String getName() {
		if(setName != null) {
			return setName;
		}
		return entryFile_.getName();
	}
	
	public void setName(String name) {
		setName = name;
	}
	
	public void close() throws IOException {
		if(zipFile_ != null) {
			zipFile_.close();
		}
	}
	
	/**
	 * Overriding this method allows a classpath entry to load a given class any way it likes, in much the same
	 * way a class loader in java can load a class in many ways.  If this method is not overridden, the class path location
	 * is assumed to hold a class file in the standardized format in the given BT_ClassPathLocation.
	 * @param className the class name
	 * @param repository the recipient of the class
	 * @param loc the location at which the class has been found
	 * @return the loaded class
	 */
	protected BT_Class loadClass(String className, BT_Repository repository, BT_ClassPathLocation loc, BT_Class stub) {
		return repository.loadFromClassPath(className, loc, stub);
	}
	
	/**
	 * returns the class name associated with the given file name for this type of classpath entry,
	 * or null if the given name does not indicate that it is a class.
	 * 
	 * An alternative class path entry might override this method.  The method will return null if
	 * file names cannot be reliably mapped to class names.
	 * 
	 * @param name
	 * @return the class name or null
	 */
	public String getClassName(String name) {
		if(name.endsWith(".class")) {
			return classFileNameToClassName(name);
		}
		return null;
	}
	
	/**
	 * describes a file located within this class path entry.
	 * Note: when implementing this class, you should keep the
	 * getInputStream() implementation independent from the getName() and getClassPathEntry()
	 * implementations.  ie closing the input stream should not adversely affect the other methods.
	 */
	public abstract class BT_ClassPathLocation implements LoadLocation {
		public String className;
		
		/**
		 * @return the class path entry upon which this class path location resides
		 */
		public BT_ClassPathEntry getClassPathEntry() { 
			return BT_ClassPathEntry.this; 
		}
		
		public BT_ClassPathLocation getLocation() {
			return this;
		}
		
		/**
		 * returns an input stream providing the contents of the class path location
		 */
		public abstract InputStream getInputStream() throws IOException;
		
		/**
		 * @return the class file (java.io.File), zip or jar file (java.util.zip.ZipFile) 
		 * or some other object identifier from which the class bytes originate (possibly even java.lang.Class)
		 */
		public abstract Object getFileObject();
		
		/**
		 * @return the name of this location (file name or zip file entry name) relative to the 
		 * root of the classpath, with no starting separator (e.g. java/lang/Object.class)
		 */
		public abstract String getName();
		
		/**
		 * call this method to determine whether a given classpath entry represents a java class.
		 * @return the class name of the class (dot separated) if this entry represents a class or null otherwise
		 */
		public String getClassName() {
			/* note that className can be set by child classes */
			if(className == null) {
				className = BT_ClassPathEntry.this.getClassName(getName());
			}
			return className;
		}
		
		/**
		 * the time at which this entry was last modified.
		 */
		public abstract long getLastModifiedTime();
		
		public boolean equals(Object o) {
			if(o instanceof BT_ClassPathLocation) {
				BT_ClassPathLocation other = (BT_ClassPathLocation) o;
				return getClassPathEntry().equals(other.getClassPathEntry())
					&& getName().equals(other.getName());
			}
			return false;
		}
		
		public String toString() {
			return getClassPathEntry() + ": " + getName();
		}
		
		public BT_Class loadClass(String className, BT_Repository repository, BT_Class stub) {
			return BT_ClassPathEntry.this.loadClass(className, repository, this, stub);
		}
		
	}
	
	public class BT_FileLocation extends BT_ClassPathLocation {
		public final String fileName;
		public final File file;
		
		/**
		 * 
		 * @param fileName the name of the file as a path name relative to the classpath
		 * @param file the file object
		 */
		public BT_FileLocation(String fileName, File file) {
			this.fileName = fileName;
			this.file = file;
		}
	
		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getLastModifiedTime()
		 */
		public long getLastModifiedTime() {
			return file.lastModified();
		}
	
		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getInputStream()
		 */
		public InputStream getInputStream() throws IOException {
			return new FileInputStream(file);
		}

		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getFileObject()
		 */
		public Object getFileObject() {
			return file;
		}

		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getName()
		 */
		public String getName() {
			return fileName;
		}
		
	}
	
	public class BT_ZipLocation extends BT_ClassPathLocation {
		public final ZipEntry ze;
		
		public BT_ZipLocation(ZipEntry ze) {
			this.ze = ze;
		}
		
		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getInputStream()
		 */
		public InputStream getInputStream() throws IOException {
			return zipFile_.getInputStream(ze);
		}

		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getFileObject()
		 */
		public Object getFileObject() {
			return zipFile_;
		}

		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getLastModifiedTime()
		 */
		public long getLastModifiedTime() {
			return ze.getTime();
		}

		/*
		 *  (non-Javadoc)
		 * @see com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation#getName()
		 */
		public String getName() {
			return ze.getName();
		}
	}
	
	
	/**
	 @param   className  The name of a class.
	 @return  The file name in zip format.
	**/
	public static String fileNameForClassName(String className) {
		return fileNameForClassName(className, ".class");
	}
	
	/**
	 * Use this method instead of fileNameForClassName(String) if the class
	 * file format is something other than a ".class" file.
	 * @param className  The name of a class.
	 * @param extension ".class" or some other alternative known format for a class
	 * @return
	 */
	public static String fileNameForClassName(String className, String extension) {
		return className.replace('.', ZIPFILE_SEPARATOR_SLASH) + extension;
	}
	
	/**
	 @param   name  The name of a resource.
	 @return  The file name in zip format.
	**/
	public static String fileNameForResourceName(String name) {
		return name.replace(DOSFILE_SEPARATOR_BACKSLASH, ZIPFILE_SEPARATOR_SLASH);
	}
	
	/**
	 @param  f  The class name in file name format.
	   Must end with ".class", ".asm" or must have some other extension.
	   E.g., "p/c.class".
	 @return  E.g., "p.c".
	**/
	public static String classFileNameToClassName(String f) {
		int index = f.lastIndexOf('.');
		return f
			.substring(0, index)
			.replace(UNIX_SEPARATOR_SLASH, '.')
			.replace(DOSFILE_SEPARATOR_BACKSLASH, '.')
			.replace(File.separatorChar, '.');
	}
}
