package com.ibm.ive.tools.japt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;

import com.ibm.ive.tools.japt.PatternString.PatternStringPair;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassComparator;
import com.ibm.jikesbt.BT_ClassPathEntry;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_JarResource;
import com.ibm.jikesbt.BT_JarResourceVector;
import com.ibm.jikesbt.BT_ResourceComparator;

/**
 * A classpath entry with additional functionality:<br>
 * -it knows which classes it loaded<br>
 * -it is capable of finding classes in its path based on search strings with wildcards<br>
 * @author sfoley
 */
public class ClassPathEntry extends BT_ClassPathEntry {
	public static BT_ClassPathLocation[] noLocations = new BT_ClassPathLocation[0];
	private BT_ClassVector loadedClasses = new BT_HashedClassVector();
	private BT_JarResourceVector loadedResources = new BT_JarResourceVector();
	
	/**
	 * for additional class path entry data
	 */
	public int contextFlags;
	
	/**
	 * Constructor for JaptClassPathEntry.
	 * @param file a directory, zip file or jar file
	 * @throws IOException
	 */
	public ClassPathEntry(File file) throws IOException {
		super(file);
	}
	
	protected ClassPathEntry() {}
	
	public int getContextFlags() {
		return contextFlags;
	}
	
	public void setContextFlags(int flags) {
		this.contextFlags = flags;
	}
	public void addClass(BT_Class clazz) {
		loadedClasses.addElement(clazz);	
	}
	
	public void addResource(BT_JarResource resource) {
		loadedResources.addElement(resource);
	}
	
	public void removeClass(BT_Class clazz) {
		loadedClasses.removeElement(clazz);
	}
	
	public void removeResource(BT_JarResource resource) {
		loadedResources.removeElement(resource);
	}
	
	public boolean loadedResource(BT_JarResource resource) {
		return loadedResources.contains(resource);
	}
	
	public boolean loadedClass(BT_Class clazz) {
		return loadedClasses.contains(clazz);
	}
	
	public BT_ClassVector getLoadedClasses() {
		return loadedClasses;
	}
	
	public boolean hasLoaded() {
		return loadedClasses.size() > 0 || loadedResources.size() > 0;
	}
	
	public void sortLoadedClasses(BT_ClassComparator comparator) {
		loadedClasses.sort(comparator);
	}
	
	public void sortLoadedResources(BT_ResourceComparator comparator) {
		loadedResources.sort(comparator);
	}
	
	private class FileSystemIterator implements Iterator {
		private final PatternString pattern;
		private final ArrayList currentEntries = new ArrayList();
		private final ArrayList unvisitedDirectories = new ArrayList();
		
		FileSystemIterator(PatternString pattern) {
			this.pattern = pattern;
			EntryFile startFile = getInitialDirectory(pattern);
			if(startFile != null) {
				unvisitedDirectories.add(new EntryFile(startFile.fileName, startFile.file));
				populateCurrentEntries();
			}
		}
		
		void populateCurrentEntries() {
			do  {
				if(unvisitedDirectories.isEmpty()) {
					break;
				}
				EntryFile dir = (EntryFile) unvisitedDirectories.remove(unvisitedDirectories.size() - 1);
				String[] entries  = dir.file.list();
				for (int i=0; i<entries.length; i++) {
					String entry = entries[i];
					if (entry.equals(".") || entry.equals("..")) {
						continue;
					}
					String fileName = dir.fileName + entry;
					File file = new File(dir.file, entry);
					if(pattern.startsWith(fileName)) {
						if (file.isDirectory()) {
							String dirName = fileName + ZIPFILE_SEPARATOR_SLASH;
							if(pattern.startsWith(dirName)) {
								unvisitedDirectories.add(new EntryFile(dirName, file));
							}
						}
						else {
							if(pattern.isMatch(fileName)) {
								currentEntries.add(new EntryFile(fileName, file));
							}
						}
					}
				}
			} while (currentEntries.isEmpty());
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		public boolean hasNext() {
			return currentEntries.size() > 0;
		}
		
		public Object next() {
			EntryFile result;
			switch(currentEntries.size()) {
				case 0:
					throw new NoSuchElementException();
				case 1:
					result = (EntryFile) currentEntries.remove(0);
					populateCurrentEntries();
					return getLocation(result.fileName, result.file);
				default:
					result = (EntryFile) currentEntries.remove(0);
					return getLocation(result.fileName, result.file);
			}
		}
	}
	
	/**
	 * Returns an iterator of all locations found within this classpath entry.
	 * The iterator will provide instances of BT_ClassPathLocation.
	 */
	public Iterator getLocations() {
		return getLocations(PatternString.ALWAYS_MATCHES_PATTERN);
	}
	
	/**
	 * Returns an iterator of the locations found within this classpath entry that match the
	 * given pattern string.
	 * The iterator will provide instances of BT_ClassPathLocation.
	 */
	public Iterator getLocations(PatternString pattern) {
		if(isDirectory()) {
			return new FileSystemIterator(pattern);
		} else {
			return new ZipIterator(pattern);
		}
	}
	
	private class ZipIterator implements Iterator {
		private final int size;
		private final PatternString pattern;
		private int index;
		private ZipEntry currentEntry;
		
		ZipIterator(PatternString pattern) {
			size = zipFileDirectory_.size();
			this.pattern = pattern;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		public boolean hasNext() {
			if(currentEntry != null) {
				return true;
			}
			while(index < size) {
				ZipEntry ze = (ZipEntry) zipFileDirectory_.elementAt(index);
				index++;
				if(pattern.isMatch(ze.getName())) {
					currentEntry = ze;
					return true;
				}
			}
			return false;
		}
		
		public Object next() {
			if(!hasNext()) {
				throw new NoSuchElementException();
			}
			ZipEntry ze = currentEntry;
			currentEntry = null;
			return getLocation(ze);
		}
	}
	
	/**
	 * Equivalent to calling getResources(false)
	 * @return those resources that have been explicitly loaded by this classpath
	 * entry.
	 */
	public Resource[] getResources() {
		return getResources(false);
	}
	
	/**
	 * 
	 * @param includeZipped include those resource entries found within the
	 * zip file represented by this class path entry (has no effect if this
	 * class path entry represents a directory or something else)
	 * @return
	 */
	public Resource[] getResources(boolean includeZipped) {
		List resourceList = new ArrayList();
		if(!isDirectory() && includeZipped && zipFileDirectory_ != null) {
			HashSet alreadyLoaded = new HashSet();
			for(int i=0; i<loadedResources.size(); i++) {
				alreadyLoaded.add(loadedResources.elementAt(i).name);
			}
			for(int k=0; k<zipFileDirectory_.size(); k++) {
				
				final ZipEntry ze = (ZipEntry) zipFileDirectory_.elementAt(k);
				if(ze.isDirectory()) {
					continue;
				}
				String name = ze.getName();
				if(!name.endsWith(".class") && !alreadyLoaded.contains(name)) {
					Resource res = new Resource() {
					
						public long getTime() {
							return ze.getTime();
						}
						
						public String getName() {
							return ze.getName();
						}
											
						public InputStream getInputStream() throws IOException {
							return zipFile_.getInputStream(ze);
						}
					
						public String loadedFrom() {
							return zipFile_.getName();
						}
					};
					resourceList.add(res);			
				}
			}
		}
		for(int i=0; i<loadedResources.size(); i++) {
			final BT_JarResource resource = loadedResources.elementAt(i);
			Resource res = new Resource() {
					
				public long getTime() {
					return System.currentTimeMillis();
				}
	
				public String getName() {
					return resource.name;
				}
	
				public InputStream getInputStream() throws IOException {
					return new ByteArrayInputStream(resource.contents);
				}

				public String loadedFrom() {
					return getEntryCanonicalName();
				}
			};
			resourceList.add(res);
			
		}
		
		return (Resource[]) resourceList.toArray(new Resource[resourceList.size()]);
	}	
	
	/**
	 * Given a pattern string representing a class name, finds all matches in this classpath entry
	 * e.g. java.lang.reflect.*
	 * 
	 * Note that with traditional class path entries, the file name structure matches the class names,
	 * but this is not necessarily true with all Japt class path entries.
	 * 
	 * @param pattern
	 * @return
	 */
	public BT_ClassPathLocation[] findClassWithPattern(PatternString pattern) {
		PatternString newPattern = new PatternString(fileNameForClassName(pattern.getString()));
		return findFileWithPattern(newPattern);
		
	}
	
	private static class EntryFile {
		EntryFile(String s, File f) {
			fileName = s;
			file = f;
		}
		String fileName; //directory names do not end with separators
		File file;
	}
	
	private EntryFile getInitialDirectory(PatternString pattern) {
		//first we grab the initial part of the pattern up to the last separator before any wildcards appear
		//to get the starting subdirectory
		int wildcardIndex = pattern.indexOfWildcard();
		int separatorIndices[];
		if(wildcardIndex < 0) {
			//should not reach here, because if there are no wildcards then findFile should be used in place of this method
			separatorIndices = pattern.lastIndexOf(ZIPFILE_SEPARATOR_SLASH);
		}
		else {
			separatorIndices = pattern.lastIndexOf(ZIPFILE_SEPARATOR_SLASH, wildcardIndex - 1);
		}
		if(separatorIndices.length > 0) {
			if(separatorIndices.length > 1) {
				//should never reach here, indicates a bug
				throw new RuntimeException();
			}
			//we determine if there exists such a subdirectory
			PatternStringPair pair = pattern.split(separatorIndices[0]);
			String initialPath = pair.first.toString();
			File potentialFile = new File(entryFile_, initialPath);
			if(potentialFile.exists()) {
				if(potentialFile.isDirectory()) {
					//the subdirectory exists
					return new EntryFile(initialPath + ZIPFILE_SEPARATOR_SLASH, potentialFile);
				}
				//else the file exists, but it's not a subdirectory...
				//but we know that there is at least a trailing separator
				//unaccounted for in the file name, so there is no match
			}
			return null;
		}
		//there is a wildcard before the first separator appears, so we must start looking in the root directory
		else {
			return new EntryFile("", entryFile_);
		}
	}
	
	/**
	 * @deprecated use findFileWithPattern
	 */
	public BT_ClassPathLocation[] findWithPattern(PatternString pattern) {
		return findFileWithPattern(pattern);
	}
	
	/**
	 * Given a pattern string representing a file name, finds all matches in this classpath entry.
	 * e.g. java/lang/reflect/*.class
	 * 
	 * Note that with traditional class path entries, the file name structure matches the class names,
	 * but this is not necessarily true with all Japt class path entries.
	 * 
	 * @param pattern
	 * @return
	 */
	public BT_ClassPathLocation[] findFileWithPattern(PatternString pattern) {
		List locationList = new ArrayList();
		if (isDirectory()) {
			EntryFile startFile = getInitialDirectory(pattern);
			if(startFile != null) {
				findFileMatches(pattern, locationList, startFile.fileName, startFile.file);
			}
		} else {
			findZipMatches(pattern, locationList);
		}
		return (BT_ClassPathLocation[]) locationList.toArray(new BT_ClassPathLocation[locationList.size()]);
	}
	
	private void findFileMatches(PatternString pattern, List locationList, String nameSoFar, File currentDir) {
		File files[] = currentDir.listFiles();
		for(int i=0; i<files.length; i++) {
			final File file = files[i];
			String fileName = nameSoFar + file.getName();
			if(pattern.startsWith(fileName)) {
				if(file.isFile()) {
					if(pattern.isMatch(fileName)) {
						BT_ClassPathLocation location = getLocation(fileName, file);
						locationList.add(location);
					}
				}
				else {
					String path = fileName + ZIPFILE_SEPARATOR_SLASH;
					//for each potential path we travel, we check if the pattern can start with that path name...
					if(pattern.startsWith(path)) {
						findFileMatches(pattern, locationList, path, file);
					}
				}
			}
		}	
	}
	
	private void findZipMatches(PatternString pattern, List locationList) {
		for (int k = 0; k < zipFileDirectory_.size(); k++) {
			ZipEntry ze = (ZipEntry) zipFileDirectory_.elementAt(k);
			String entryName = ze.getName();
			if (pattern.isMatch(entryName)) {
				BT_ClassPathLocation location = getLocation(ze);
				locationList.add(location);
			}
		}
	}
	
	
}
