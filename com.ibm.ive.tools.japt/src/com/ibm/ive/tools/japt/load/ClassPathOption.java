/*
 * Created on Sep 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.OptionException;
import com.ibm.ive.tools.japt.ArchiveExtensionList;
import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.commandLine.OptionsFileReader;
import com.ibm.ive.tools.japt.commandLine.OptionsParser;
import com.ibm.jikesbt.BT_Repository;
import com.ibm.jikesbt.StringVector;
import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ClassPathOption extends Option {

	private LinkedList stringList = new LinkedList();
	private LinkedList entryList = new LinkedList();
	public static String GENERIC_EMBEDDED_OPTIONS = ".rules";
	private FlagOption noBuiltInRulesOption;
	private ArchiveExtensionList archiveExtensions;
	public static final int JRE = 1;
	public static final int RECURSIVE = 2;
	public static final int SIMPLE = 3;
	private int type;
	
	/**
	 * @param name
	 * @param description
	 * @param argCount
	 */
	public ClassPathOption(String name, 
			String description, 
			int argCount, 
			FlagOption noBuiltInRulesOption,
			ArchiveExtensionList archiveExtensions) {
		this(name, description, argCount, noBuiltInRulesOption, archiveExtensions, SIMPLE);
	}
	
	public ClassPathOption(String name, 
			int argCount, 
			FlagOption noBuiltInRulesOption,
			ArchiveExtensionList archiveExtensions) {
		super(name, argCount);
		this.noBuiltInRulesOption = noBuiltInRulesOption;
		this.archiveExtensions = archiveExtensions;
	}
	
	public ClassPathOption(String name, 
			String description, 
			int argCount, 
			FlagOption noBuiltInRulesOption,
			ArchiveExtensionList archiveExtensions,
			int type) {
		super(name, description, argCount);
		if(type != RECURSIVE && type != JRE && type != SIMPLE) {
			throw new IllegalArgumentException();
		}
		this.type = type;
		this.noBuiltInRulesOption = noBuiltInRulesOption;
		this.archiveExtensions = archiveExtensions;
	}
	
	private void searchClassPathEntryForOptions(String classPathName, OptionsFileReader optionsReader) throws CommandLineException {
		try {
			if(archiveExtensions.isArchive(classPathName)) { //TODO handle options files in directories, not just archives
				searchZipForOptions(classPathName, optionsReader);
			}
		}
		catch(IOException e) {
			throw new CommandLineException(e);
		}
	}
	
	private void searchZipForOptions(String classPathName, OptionsFileReader optionsReader) throws IOException, CommandLineException {
		class ZipFileEntry extends File {
			private ZipEntry ze;
			
			ZipFileEntry(File file, ZipEntry ze) throws IOException {
				super(file.getCanonicalPath());
				this.ze = ze;
			}
			
			public boolean equals(Object o) {
				if(super.equals(o) && ze != null && o instanceof ZipFileEntry) {
					return ze.equals(((ZipFileEntry) o).ze);
				}
				return false;
			}
			
			public String toString() {
				return super.toString() + ':' + ze;
			}
			
		};
		File containingFile = new File(classPathName);
		if(!containingFile.exists()) {
			return;
		}
		if(containingFile.isDirectory()) {
			return;
		}
		ZipFile zipFile = new ZipFile(classPathName);
		Enumeration e = zipFile.entries();
		while(e.hasMoreElements()) {
			ZipEntry ze = (ZipEntry) e.nextElement();
			if(ze.getName().toLowerCase().endsWith(GENERIC_EMBEDDED_OPTIONS)) {
				ZipFileEntry file = new ZipFileEntry(containingFile, ze);
				InputStream inputStream = zipFile.getInputStream(ze);
				Reader reader = new BufferedReader(new InputStreamReader(inputStream));
				String args[] = OptionsParser.parseTokens(reader);
				if(args != null && args.length > 0) {
					optionsReader.readOptionsFile(file, args, OptionsParser.emptyOptions);
				}
			}
		}
		zipFile.close();
	}

	/**
	 * Handles an appearance on the command line of this option.
	 */
	protected void handle(String args[], com.ibm.ive.tools.commandLine.CommandLineParser fromParser) throws CommandLineException {
		for(int i=0; i<argCount; i++) {
			if(args.length == i) {
				//we have fewer than the expected number of arguments
				throw new OptionException(this);
			}
			StringVector entries = BT_Repository.pathTokenizer(args[i], false);
			for(int j=0; j<entries.size(); j++) {
				OptionsFileReader reader = (OptionsFileReader) fromParser;
				add(entries.elementAt(j), reader);
			}
		}
	}
	
	public void add(String s, OptionsFileReader optionsReader) throws CommandLineException {
		stringList.add(s);
		if(type == RECURSIVE) {
			StringVector paths = JaptRepository.pathTokenizer(s, true);
			for(int j=0; j<paths.size(); j++) {
				String cps = paths.elementAt(j);
				handleDirectory(optionsReader, cps);
			}
		} else if(type == JRE) {
			handleDirectory(optionsReader, s);
		} else {
			StringVector paths = BT_Repository.pathTokenizer(s, true);
			for(int i=0; i<paths.size(); i++) {
				String path = paths.elementAt(i);
				entryList.add(path);
				if(!noBuiltInRulesOption.isFlagged() && archiveExtensions.isClassPathEntry(path)) {
					searchClassPathEntryForOptions(path, optionsReader);
				}
			}
		}
	}

	private void handleDirectory(OptionsFileReader optionsReader, String cps)
			throws CommandLineException, OptionException {
		File file = new File(cps);
		if (file.isDirectory()) {
			try {
				ClassPathEntry entry = new ClassPathEntry(file);
				Iterator iterator = entry.getLocations();
				while(iterator.hasNext()) {
					BT_ClassPathLocation location = (BT_ClassPathLocation) iterator.next();
					String name = location.getName();
					if(archiveExtensions.isArchive(name)) {
						name = cps + File.separatorChar + name;
						entryList.add(name);
						if(!noBuiltInRulesOption.isFlagged()) {
							try {
								searchZipForOptions(name, optionsReader);
							} catch(IOException e) {
								/* if the archive is not valid, ignore it for now, we will report it later */
								//data.messages.ERROR_READING_FILE.log(logger, toLoad + " (" + e.toString() + ")");
							}
						} 
					}
				}
			} catch(IOException e) {
				throw new OptionException(this, e.toString());
			}
		} else {
			throw new OptionException(this, "dir inexistent");//TODO internationalize this string
		}
	}
	
	public void add(String s) {
		stringList.add(s);
	}
	
	public String[] getEntries() {
		return (String[]) entryList.toArray(new String[entryList.size()]);
	}
	
	public String[] getStrings() {
		return (String[]) stringList.toArray(new String[stringList.size()]);
	}
	
	public boolean appears() {
		return stringList.size() > 0;
	}
}
