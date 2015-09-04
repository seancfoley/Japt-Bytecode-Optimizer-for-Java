package com.ibm.ive.tools.japt.out;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassWriteException;

/**
 * @author sfoley
 *
 */
class ClassJarEntry extends JarEntry {
	private final BT_Class clazz;
	private byte bytes[];
	private final long time;
	
	public ClassJarEntry(BT_Class clazz) {
		this.clazz = clazz;
		this.time = new Date().getTime();
	}
	
	private byte[] getBytes() throws BT_ClassWriteException {
		if(bytes == null) {
			bytes = clazz.bytes();
			if (0 == bytes.length) {
				bytes = new byte[] {0};
			}
		}
		return bytes;
	}
	
	public long getTime() {
		return time;
	}
		
	public InputStream getInputStream() throws IOException, BT_ClassWriteException {
		return new ByteArrayInputStream(getBytes());
	}
	
	public String getName() {
		return ClassPathEntry.fileNameForClassName(clazz.getName());
	}
	
	public String toString() {
		return clazz.toString();
	}
	
}
