package com.ibm.ive.tools.japt.out;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.Resource;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassWriteException;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_FileConstants;
import com.ibm.jikesbt.BT_Misc;

/**
 * @author sfoley
 *
 * Writes various different entry types to a jar file
 */
public class JarGenerator extends Generator implements BT_FileConstants {
	private JarOutputStream jarStream;
	private Boolean defaultCompressed;
	
	public JarGenerator(OutputStream outputStream)  throws IOException {
		jarStream = new JarOutputStream(outputStream);
	}
	
	public JarGenerator(OutputStream outputStream, Manifest manifest) throws IOException {
		jarStream = new JarOutputStream(outputStream, manifest);
	}
	
	/**
	 * override the compression settings of individual jar entries
	 * @param compress whether to compress all entries
	 * @return
	 */
	public void setDefaultCompressed(boolean compress) {
		defaultCompressed = compress ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public void close() throws IOException {
		jarStream.closeEntry();
		jarStream.close();
	}
	
	public void write(String name, byte bytes[]) throws IOException {
		java.util.jar.JarEntry jarEntry = new java.util.jar.JarEntry(name);
		jarEntry.setTime(new Date().getTime());
		if(defaultCompressed != null && !defaultCompressed.booleanValue()) {
			jarEntry.setMethod(ZipEntry.STORED);
			writeEntry(bytes, jarEntry);
			return;
		}
		jarEntry.setMethod(ZipEntry.DEFLATED);
		jarStream.putNextEntry(jarEntry);
		jarStream.write(bytes);
	}
	
	/**
	 * @param bytes
	 * @param jarEntry
	 * @throws IOException
	 */
	private void writeEntry(byte[] bytes, java.util.jar.JarEntry jarEntry) throws IOException {
		CRC32 tempCrc = new CRC32();
		tempCrc.update(bytes);
		jarEntry.setCrc(tempCrc.getValue());
		jarEntry.setSize(bytes.length);
		jarStream.putNextEntry(jarEntry);
		jarStream.write(bytes);
	}

	public void write(Manifest manifest) throws IOException {
		java.util.jar.JarEntry jarEntry = new java.util.jar.JarEntry(JarFile.MANIFEST_NAME);
		jarEntry.setTime(new Date().getTime());
		if(defaultCompressed != null && !defaultCompressed.booleanValue()) {
			jarEntry.setMethod(ZipEntry.STORED);
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			manifest.write(byteStream);
			writeEntry(byteStream.toByteArray(), jarEntry);
			return;
		}
		jarEntry.setMethod(ZipEntry.DEFLATED);
		jarStream.putNextEntry(jarEntry);
		manifest.write(jarStream);
	}
	
	public void write(Resource resource) throws IOException {
		try {
			write(new ResourceJarEntry(resource));
		} catch(BT_ClassWriteException e) {}
	}
		
	public void write(BT_Class clazz) throws IOException, BT_ClassWriteException {
		String name = ClassPathEntry.fileNameForClassName(clazz.getName());
		java.util.jar.JarEntry jarEntry = new java.util.jar.JarEntry(name);
		jarEntry.setTime(new Date().getTime());
		if(defaultCompressed != null && !defaultCompressed.booleanValue()) {
			jarEntry.setMethod(ZipEntry.STORED);
			writeEntry(clazz.bytes(), jarEntry);
		} else {
			jarEntry.setMethod(ZipEntry.DEFLATED);
			jarStream.putNextEntry(jarEntry);
			DataOutputStream dos = new DataOutputStream(jarStream);
			clazz.write(dos);
		}
		if(loader != null) {
			loader.addWrittenClass(clazz);
		}
	}
	
	public void writeAssemblyClass(BT_Class clazz, String fileExtension) throws IOException, BT_CodeException {
		String name = clazz.getName().replace('.', ZIPFILE_SEPARATOR_SLASH) + fileExtension;
		java.util.jar.JarEntry jarEntry = new java.util.jar.JarEntry(name);
		jarEntry.setTime(new Date().getTime());
		if(defaultCompressed != null && !defaultCompressed.booleanValue()) {
			jarEntry.setMethod(ZipEntry.STORED);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bs);
			clazz.print(ps, BT_Misc.PRINT_IN_ASSEMBLER_MODE);
			writeEntry(bs.toByteArray(), jarEntry);
		} else {
			jarEntry.setMethod(ZipEntry.DEFLATED);
			jarStream.putNextEntry(jarEntry);
			PrintStream ps = new PrintStream(jarStream);
			clazz.print(ps, BT_Misc.PRINT_IN_ASSEMBLER_MODE);
		}
	}
	
	public void write(JarEntry entry) throws IOException, BT_ClassWriteException {
		String name = entry.getName();
		java.util.jar.JarEntry jarEntry = new java.util.jar.JarEntry(name);
		jarEntry.setTime(entry.getTime());
		InputStream inStream = entry.getInputStream();
		byte buffer[] = new byte[1024];
		if(defaultCompressed != null && !defaultCompressed.booleanValue()) {
			jarEntry.setMethod(ZipEntry.STORED);
			ByteArrayOutputStream oStream = new ByteArrayOutputStream();
			int numBytes = inStream.read(buffer);
			while(numBytes != -1) {
				oStream.write(buffer, 0, numBytes);
				numBytes = inStream.read(buffer);
			}
			writeEntry(oStream.toByteArray(), jarEntry);
			return;
		}
		jarEntry.setMethod(ZipEntry.DEFLATED);
		jarStream.putNextEntry(jarEntry);
		int numBytes = inStream.read(buffer);
		while(numBytes != -1) {
			jarStream.write(buffer, 0, numBytes);
			numBytes = inStream.read(buffer);
		}
		jarStream.flush();
	}	
}
