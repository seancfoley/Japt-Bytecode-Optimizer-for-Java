package com.ibm.ive.tools.japt.out;


import java.io.IOException;
import java.io.InputStream;

import com.ibm.jikesbt.BT_ClassWriteException;


/**
 *
 * <pre>
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corp. 1999 All Rights Reserved
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 * </pre>
 */
public abstract class JarEntry {
	private boolean toBeCompressed = true;
	
	protected JarEntry() {}
	
	public void setCompressed(boolean compress) {
		toBeCompressed = compress;
	}
	
	public boolean toBeCompressed() {
		return toBeCompressed;
	}
	
	abstract public long getTime();
	
	abstract public InputStream getInputStream() throws IOException, BT_ClassWriteException;
	
	abstract public String getName();
	
}