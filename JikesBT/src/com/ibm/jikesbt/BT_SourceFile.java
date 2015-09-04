/*
 * Created on Oct 5, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * 
 * @author sfoley
 *
 * Used for reading lines of source from a source file 
 */
public class BT_SourceFile {
	private static final String[] noLines = new String[0];
	private String lines[];
	public final String name;
	private BT_Class clazz;
	
	BT_SourceFile(BT_Class clazz, String name) {
		this.name = name;
		this.clazz = clazz;
	}
	
	public boolean equals(Object o) {
		if(!(o instanceof BT_SourceFile)) {
			return false;
		}
		BT_SourceFile other = (BT_SourceFile) o;
		return name.equals(other.name);
	}
	
	private void readLines() {
		if(lines != null) {
			return;
		}
		String dir = BT_Repository.getSourcePath() + File.separatorChar;
		dir += clazz.packageName().replace('.', File.separatorChar) + File.separatorChar;
		BufferedReader sourceLineReader = null;
		String resource = dir + name;
		try {
			sourceLineReader = new BufferedReader(new FileReader(resource));
			StringVector lines = new StringVector();
			try {
				String line = sourceLineReader.readLine();
				while (line != null) {
					lines.addElement(line);
					line = sourceLineReader.readLine();
				}
			} catch(IOException e) {} finally {
				if(sourceLineReader != null) {
					try {
						sourceLineReader.close();
					} catch(IOException e) {
						clazz.getRepository().factory.noteFileCloseIOException(resource, e);
					}
				}
			}
			this.lines = lines.toArray(); 
		} catch(FileNotFoundException e) {
			lines = noLines;
		}
		return;
	}
	
	String getLine(int number) {
		readLines();
		if(number <= lines.length) {
			return lines[number - 1];
		}
		return null;
	}
	
}
