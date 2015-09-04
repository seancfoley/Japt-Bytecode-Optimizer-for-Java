package com.ibm.ive.tools.japt.out;

import java.io.*;

import com.ibm.ive.tools.japt.*;


/**
 * @author sfoley
 */
class ResourceJarEntry extends JarEntry {
	private Resource resource;
	
	public ResourceJarEntry(Resource resource) {
		this.resource = resource;
	}
	
	public long getTime() {
		return resource.getTime();
	}
	
	public InputStream getInputStream() throws IOException {
		return resource.getInputStream();
	}
	
	public String getName() {
		return resource.getName();
	}
	
	public String toString() {
		return resource.getName();
	}
	
}
