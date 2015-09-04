/*
 * Created on Oct 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.Resource;

/**
 * @author sfoley
 *
 * Transfers the classpath that was used to run japt into a class path entry that japt
 * can use to load classes and resources
 */
public class TransferredClassPathEntry extends ClassPathEntry {
	
	public final PatternString matchers[];
	private String classPathEntry;
	String name = "Instrument";
	String alternateExtension;
	
	public TransferredClassPathEntry() throws IOException {
		 this(PatternString.ALWAYS_MATCHES_PATTERN);
	}
	
	/**
	 * Only files who name matches the given pattern string will be loaded
	 * @param fileMatcher
	 */
	public TransferredClassPathEntry(PatternString fileMatcher) throws IOException {
		this(new PatternString[] {fileMatcher}, null);
	}
	
	/**
	 * Only files who name matches one of the given pattern strings will be loaded
	 * @param fileMatcher
	 */
	public TransferredClassPathEntry(PatternString fileMatchers[]) throws IOException {
		this(fileMatchers, null);
	}
	
	/**
	 * Only files who name matches one of the given pattern string will be loaded
	 * @param fileMatchers
	 */
	public TransferredClassPathEntry(PatternString[] fileMatchers, String alternateExtension) throws IOException {
		this.matchers = fileMatchers;
		this.alternateExtension = alternateExtension;
		Class thisClass = getClass();
		String fileName = fileNameForClassName(thisClass.getName());
		Enumeration enumeration = thisClass.getClassLoader().getResources(fileName);
		int count = 0;
		while(enumeration.hasMoreElements()) {
			count++;
			URL url = (URL) enumeration.nextElement();
			classPathEntry = url.toExternalForm();
		}
		if(count != 1) {
			throw new IllegalStateException();
		}
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
	
	public void setName(String name) {
		if(name == null) {
			throw new NullPointerException();
		}
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isDirectory() {
		return false;
	}
	
	public boolean isArchive() {
		return false;
	}
	
	protected boolean isAccessiblePackage(String fileName) {
		for(int i=0; i<matchers.length; i++) {
			if(matchers[i].isMatch(fileName)) {
				return true;
			}
		}
		return false;
		//return matcher.isMatch(fileName);
		//String pkg = getClass().getPackage().getName().replace('.', '/');
		//return fileName.startsWith(pkg);
	}
	/**
	 * Override the parent behaviour.  Files will be searched for by
	 * the classloader of this TransferredClassPathEntry class.  Essentially what this means
	 * is this: the classes being used by the VM to run Japt can now be read and used by Japt.
	 * <p>
	 * This method restricts look-up to files that exist within the same package as this
	 * TransferredClassPathEntry class.
	 * <p>
	 * NOTE: this method will fail when running Japt from a jxe, since there are no class files
	 * in the class path.  In such cases, the user must ensure that the relevant classes are available
	 * elsewhere on the class path.
	 */
	public BT_ClassPathLocation findFile(String theFileName) {
		if(!isAccessiblePackage(theFileName)) {
			return null;
		}
		int extensionIndex;
		final String classFileName = theFileName;
		if(alternateExtension != null && (extensionIndex = theFileName.indexOf('.')) > 0) {
			theFileName = theFileName.substring(0, extensionIndex + 1) + alternateExtension;
		}
		final String fileName = theFileName;
		ArrayList urls = new ArrayList();
		int i = 0;
		final InputStream stream;
		URL url;
		try {
			Enumeration en = getClass().getClassLoader().getResources(fileName);
			int match = 0;
			int index = 0;
			
			/* 
			 * the same class loader might have copies of class files
			 * that match the file names we are searching for now!
			 * 
			 * To make sure we get our stubs,
			 * we convert each URL to a string, then we compare 
			 * with the same string for this TransferredClassPathEntry class,
			 * the resource that matches the most characters wins
			 */
			
			while(en.hasMoreElements()) {
				url = (URL) en.nextElement();
				urls.add(url);
				String urlString = url.toExternalForm();
				int j = 0;
				while(urlString.length() > j && classPathEntry.length() > j 
						&& urlString.charAt(j) == classPathEntry.charAt(j)) {
					j++;
				}
				//System.out.println(classPathEntry + " " + url.toExternalForm() + " " + j);
				if(j > match) {
					match = j;
					index = i;
				}
				i++;
			}
			if(i == 0) {
				return null;
			}
			url = (URL) urls.get(index);
			stream = url.openStream();
		} catch(IOException e) {
			return null;
		}
		final URL foundUrl = url;
		if(stream == null) {
			return null;
		}
		return new BT_ClassPathLocation() {
			{
				if(alternateExtension != null) {
					className = classFileName;
				}
			}
			public InputStream getInputStream() throws IOException {
				return stream;
			}
			
			public Object getFileObject() {
				return foundUrl;
			}
			
			public String getName() {
				return fileName;
			}
			
			public long getLastModifiedTime() {
				return System.currentTimeMillis();
			}
		};
	}
	
	/**
	 * Override the parent behaviour - do not search by pattern.
	 */
	public BT_ClassPathLocation[] findFileWithPattern(PatternString pattern) {
		return noLocations;
	}
	
	public boolean equals(Object o) {
		return o.getClass().equals(getClass());
	}
	
	public String toString() {
		//String s = Messages.getString("JikesBT.Class_Path_Entry___2");
		String s = "reflective class path entry: ";
		return s + getName();
	}
	
	public String getEntryCanonicalName() {
		return getName();
	}
}
