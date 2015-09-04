/*
 * Created on Oct 17, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.File;

import com.ibm.jikesbt.StringVector;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ArchiveExtensionList {

	private StringVector archiveExtensions = new StringVector(2, 2);
	
	public void addArchiveExtension(String ext) {
		if(!archiveExtensions.contains(ext)) {
			archiveExtensions.addElement(ext.toLowerCase());
		}
	}
	
	public boolean removeArchiveExtension(String ext) {
		return archiveExtensions.removeElement(ext);
	}
	
	public String getArchiveExtension(String fileName) {
		int extensionIndex = fileName.lastIndexOf('.');
		if(extensionIndex < 0) {
			return null;
		}
		String fileExtension = fileName.substring(extensionIndex + 1).toLowerCase();
		if(fileExtension.equals("jar") || fileExtension.equals("zip")) {
			return fileExtension;
		}
		int ind = archiveExtensions.indexOf(fileExtension);
		if(ind < 0) {
			return null;
		}
		return archiveExtensions.elementAt(ind);
	}
	
	public static boolean isStandardArchive(String fileName) {
		int extensionIndex = fileName.lastIndexOf('.');
		if(extensionIndex < 0) {
			return false;
		}
		String fileExtension = fileName.substring(extensionIndex + 1).toLowerCase();
		return fileExtension.equals("jar") || fileExtension.equals("zip");
	}
	
	public boolean isArchive(String fileName) {
		if(isStandardArchive(fileName)) {
			return true;
		}
		int extensionIndex = fileName.lastIndexOf('.');
		if(extensionIndex < 0) {
			return false;
		}
		String fileExtension = fileName.substring(extensionIndex + 1).toLowerCase();
		return archiveExtensions.contains(fileExtension);
	}
	
	public boolean isClassPathEntry(String name) {
		File file = new File(name);
		return file.exists() && (isArchive(name) || file.isDirectory());
	}
	
}
