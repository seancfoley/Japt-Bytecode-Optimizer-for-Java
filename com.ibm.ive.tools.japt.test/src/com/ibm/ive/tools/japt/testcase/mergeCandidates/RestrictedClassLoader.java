package com.ibm.ive.tools.japt.testcase.mergeCandidates;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *	A simple classloader which loads from one jar file.
 *
 *		Named instances of RestrictedClassLoader report their own finalization to 
 *	the FinalizationIndicator class, which may be querried to determine whether any
 *	given instance has been finalized.
 **/
public class RestrictedClassLoader extends ClassLoader {
	private JarFile jarFile;
	private File file;
	private List restrictedClasses;
	private String fileName;
	private boolean silent;
	StringBuffer buffer = new StringBuffer();
	
	public RestrictedClassLoader(String fileName, String loadableClasses[], boolean silent) throws IOException {
		this.fileName = fileName;
		this.restrictedClasses = Arrays.asList(loadableClasses);
		file = new File(fileName);
		if(!file.exists()) {
			throw new IOException("\"" + fileName + "\" inexistent");
		}
		if(!file.isDirectory()) {
			jarFile = new JarFile(fileName);
		}
		this.silent = silent;
	}
	
	public Class loadClass(String clsName) throws ClassNotFoundException {
		if(!silent) {
			synchronized(buffer) {
				buffer.append("loadClass() called to load class \"");
				buffer.append(removePackageName(clsName));
				buffer.append("\" in ");
				buffer.append(this);
				buffer.append('\n');
			}
		}
		if (restrictedClasses.contains(clsName)) {
			throw new ClassNotFoundException("Class \"" + clsName + "\" is restricted in this class loader");
		}
		return super.loadClass(clsName);
	}
	
	private static String remove(String name, char sep) {
		int index = name.lastIndexOf(sep);
		if(index < 0) {
			return name;
		}
		return name.substring(index + 1);
	}
	
	static String removePath(String name) {
		return remove(name, File.separatorChar);
	}
	
	static String removePackageName(String className) {
		return remove(className, '.');
	}
	
	private InputStream getInputStream(String fileName) throws IOException {
		if(jarFile != null) {
			JarEntry entry = jarFile.getJarEntry(fileName);
			if (entry != null) {
				return jarFile.getInputStream(entry);
			}
		} else {
			File fullFile = new File(file, fileName);
			if(fullFile.exists()) {
				return new FileInputStream(fullFile);
			}
		}
		return null;
	}
	
	public String toString() {
		return "restricted class loader for " + ((jarFile == null) ? "directory \"" : "archive \"") + removePath(fileName) + "\"";
	}
	
	public Class findClass(String clsName) throws ClassNotFoundException {
		if(!silent) {
			synchronized(buffer) {
				buffer.append("findClass() called to find class \"");
				buffer.append(removePackageName(clsName));
				buffer.append("\" in ");
				buffer.append(this);
				buffer.append('\n');
			}
		}
		try {
			InputStream is = getInputStream(clsName.replace('.', '/') + ".class");
			if (is != null) {
				byte[] clBuf = getBytes(is);
				is.close();
				return defineClass(clsName, clBuf, 0, clBuf.length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		throw new ClassNotFoundException(clsName);
	}
	
	private static byte[] getBytes(InputStream is) throws IOException {
		byte[] buf = new byte[4096];
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int count;
		while ((count = is.read(buf)) > 0)
			bos.write(buf, 0, count);
		return bos.toByteArray();
	}
}