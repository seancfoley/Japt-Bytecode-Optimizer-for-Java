package com.ibm.ive.tools.japt;

import java.io.*;

/**
 * Represents a resource found on the classpath, or a resource yet to be created
 */
public abstract class Resource {
	
	protected Resource() {}
	
	/**
	 * @return the time of the last modification to this resource in milliseconds since the epoch
	 */
	public abstract long getTime();
	
	/**
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * @return the byte contents as an input stream
	 */
	public abstract InputStream getInputStream() throws IOException;
	
	/**
	 * @return null or the location (class/zip/jar file) from where this resource originates
	 */
	public abstract String loadedFrom();
	
}
