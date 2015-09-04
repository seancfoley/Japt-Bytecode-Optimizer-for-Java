/*
 * Created on Oct 15, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.File;
import java.io.IOException;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ResourceFileClassPathEntry extends FileClassPathEntry {

	/**
	 * @param file
	 * @param repository
	 * @throws IOException
	 */
	public ResourceFileClassPathEntry(File file)
			throws IOException {
		super(file);
	}
	
	public BT_FileLocation initializeLocation() throws IOException {
		return new BT_FileLocation(file.getName(), file);
	}

}
