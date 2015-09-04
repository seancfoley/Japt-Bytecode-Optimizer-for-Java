package com.ibm.ive.tools.japt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import com.ibm.jikesbt.BT_ClassFileException;
import com.ibm.jikesbt.BT_Repository;

/**
 * A ClassSource is a class path entry for which the relative path does not indicate the
 * packages names of classes within.  Instead, when the entry is created, all class files
 * within are analyzed for their class name contained within.  Afterwards, class searches
 * are based against the stored class names.
 * 
 * @author sfoley
 *
 */
public class ClassSource extends ClassPathEntry {

	final BT_Repository repository;
	final BT_ClassPathLocation classLocations[];
	
	public ClassSource(File file, BT_Repository repository) throws IOException {
		super(file);
		this.repository = repository;
		classLocations = super.findClassWithPattern(PatternString.ALWAYS_MATCHES_PATTERN);
	}
	
	/**
	 * For this class path entry, file names do not indicate class names.
	 */
	public String getClassName(String name) {
		return null;
	}
	
	protected BT_ClassPathLocation getLocation(String fileName, File file) {
		boolean isClass = fileName.endsWith(".class");
		if(isClass && classLocations != null) {
			for(int i=0; i<classLocations.length; i++) {
				BT_FileLocation location = (BT_FileLocation) classLocations[i];
				if(fileName.equals(location.fileName)) {
					return location;
				}
			}
		} 
		BT_ClassPathLocation location = super.getLocation(fileName, file);
		if(isClass) {
			location.className = sniffClassName(location, entryFile_);
		}
		return location;
	}
	
	protected BT_ClassPathLocation getLocation(ZipEntry ze) {
		boolean isClass = ze.getName().endsWith(".class");
		if(isClass && classLocations != null) {
			for(int i=0; i<classLocations.length; i++) {
				BT_ZipLocation location = (BT_ZipLocation) classLocations[i];
				if(ze.equals(location.ze)) {
					return location;
				}
			}
		} 
		BT_ClassPathLocation location = super.getLocation(ze);
		if(isClass) {
			location.className = sniffClassName(location, entryFile_);
		}
		return location;
	}
	
	private String sniffClassName(BT_ClassPathLocation location, File file) {
		try {
			String name = ClassFileClassPathEntry.sniffClassName(location.getInputStream(), file, repository);
			return name;
		} catch(IOException e) {
			repository.factory.noteClassReadIOException(null, entryFile_.toString(), e);
		} catch(BT_ClassFileException e) {
			String exc = e.getEquivalentRuntimeError();
			repository.factory.noteClassLoadFailure(repository, location.getClassPathEntry(), null, null, entryFile_.toString(), e, 
					exc == null ? BT_Repository.JAVA_LANG_CLASS_FORMAT_ERROR : exc);
		} catch(RuntimeException e) {
			repository.factory.noteClassLoadFailure(repository, location.getClassPathEntry(), null, null, entryFile_.toString(), e, 
					BT_Repository.JAVA_LANG_CLASS_FORMAT_ERROR);
		} 
		return null;
	}
	
	public BT_ClassPathLocation findClass(String className) {
		for(int i=0; i<classLocations.length; i++) {
			BT_ClassPathLocation location = classLocations[i];
			String name = location.getClassName();
			if(className.equals(name)) {
				return location;
			}
		}
		return null;
	}
	
	public BT_ClassPathLocation[] findClassWithPattern(PatternString pattern) {
		List locationList = new ArrayList();
		for(int i=0; i<classLocations.length; i++) {
			BT_ClassPathLocation location = classLocations[i];
			String name = location.getClassName();
			if(pattern.isMatch(name)) {
				locationList.add(location);
			}
		}
		return (BT_ClassPathLocation[]) locationList.toArray(new BT_ClassPathLocation[locationList.size()]);
	}

}
