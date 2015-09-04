/*
 * Created on Sep 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.assembler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.ListOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ArchiveExtensionList;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.FileClassPathEntry;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassPathEntry;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.StringVector;
import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;


/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AssemblerExtension extends BaseAssemblerExtension {

	private AssemblerMessages messages = new AssemblerMessages(this);
	private String name = messages.DESCRIPTION;
	
	private FlagOption resetClassPath = new FlagOption(messages.RESET_LABEL, messages.RESET);
	private ListOption load = new ListOption(messages.LOAD_LABEL, messages.LOAD, 1);
	private ListOption internalClassPathList = new ListOption(messages.INTERNAL_CLASS_PATH_LABEL, messages.INTERNAL_CLASS_PATH, 1);
	private ArchiveExtensionList archiveExtensions = new ArchiveExtensionList();
	private ListOption fileExtension = new ListOption(messages.FILE_EXTENSION_LABEL, messages.FILE_EXTENSION, 1) {
		public void add(String s) {
			super.add(s);
			archiveExtensions.addArchiveExtension(s);
		}
	};
	private ValueOption classVersionOption = new ValueOption(messages.CLASS_VERSION_LABEL, messages.CLASS_VERSION);
	
	
	/* if we wish to add rules files to archives with assembly files in them, we need to follow running
	 * this extension with running the load extension with the same classpath entry listed, so that those
	 * rules are parsed.  We could offer the support of rules files here but such support could only
	 * extend to options that are pertinent to this extension.  Currently all rules files contain
	 * options pertinent to the load extension. 
	 */
	
	/**
	 * 
	 */
	public AssemblerExtension() {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		if(resetClassPath.isFlagged()) {
			repository.resetClassLoading();
		}
		BT_ClassVersion version;
		if(classVersionOption.appears()) {
			try {
				version = new BT_ClassVersion(classVersionOption.getValue());
			} catch(NumberFormatException e) {
				messages.VERSION_ERROR.log(logger, classVersionOption.getValue());
				version = new BT_ClassVersion();
			}
		} else {
			version = new BT_ClassVersion();
		}
		String fileExtension = getFileExtension();
		ClassLoadingQueue queue = new ClassLoadingQueue(repository, version, messages, logger);
		
		if(internalClassPathList.appears()) {
			String list[] = internalClassPathList.getStrings();
			for(int i=list.length - 1; i>=0; i--) {
				String entry = list[i];
				StringVector entries = JaptRepository.pathTokenizer(entry, true);
				for(int j=0; j<entries.size(); j++) {
					entry = entries.elementAt(j);
					File file = new File(entry);
					try {
						if(file.exists()) {
							file = file.getCanonicalFile();
							AssemblerClassPathEntry ae = new AssemblerClassPathEntry(file, queue, fileExtension);
							repository.prependInternalClassPathEntry(ae);
						}
						else {
							messages.ERROR_READING.log(logger, file);
						}
					}
					catch(IOException e) {
						messages.ERROR_READING.log(logger, file);
					}
				}
			}
		}
		
		String loadClassPathStrings[] = load.getStrings();
		ArrayList loadClassPathList = new ArrayList(loadClassPathStrings.length);
		for(int i=loadClassPathStrings.length - 1; i>=0; i--) {
			String cps = loadClassPathStrings[i];
			StringVector entries = JaptRepository.pathTokenizer(cps, true);
			for(int j=0; j<entries.size(); j++) {
				cps = entries.elementAt(j);
				if(archiveExtensions.isClassPathEntry(cps)) {
					File file = new File(cps);
					try {
						//TODO nice-to-have: the equivalent to ClassSource, which loads stuff regardless of path
						AssemblerClassPathEntry ae = new AssemblerClassPathEntry(file, queue, fileExtension);
						repository.prependInternalClassPathEntry(ae);
						loadClassPathList.add(ae);
					}
					catch(IOException e) {
						messages.ERROR_READING_FILE.log(logger, new Object[] {cps, e});
					}
				}
			}
		}
		AssemblerClassPathEntry loadClassPathEntries[] = (AssemblerClassPathEntry[]) loadClassPathList.toArray(new AssemblerClassPathEntry[loadClassPathList.size()]);
		
		//load assembly files specified with -load
		String[] othersToLoad = load.getStrings();
		for(int i=0; i<othersToLoad.length; i++) {
			String toLoad = othersToLoad[i];
			StringVector entries = JaptRepository.pathTokenizer(toLoad, false);
			for(int j=0; j<entries.size(); j++) {
				toLoad = entries.elementAt(j);
				if(!archiveExtensions.isClassPathEntry(toLoad)) {
					try {
						loadFile(toLoad, repository, logger, queue, version);
					}
					catch(IOException e) {
						messages.ERROR_READING_FILE.log(logger, new Object[] {toLoad, e});
					}
				}
			}
		}					

		//now load assembly files stored in archives specified with -load
		for(int i=0; i<loadClassPathEntries.length; i++) {
			AssemblerClassPathEntry toLoad = loadClassPathEntries[i];
			loadAll(toLoad, repository);
		}
		repository.resetClassLoading();
	}
	
	private BT_ClassVector loadAll(AssemblerClassPathEntry toLoad, JaptRepository repository) {
		BT_ClassVector classes = new BT_ClassVector();
		Iterator locations = toLoad.getLocations();
		while(locations.hasNext()) {
			BT_ClassPathLocation location = (BT_ClassPathLocation) locations.next();
			String name = location.getClassName();
			if(name != null) {
				BT_Class result = repository.loadClass(name, location);
				if(!result.isStub()) {
					classes.addElement(result);
				}
			}
			else {
				repository.loadResource(location);
			}
		}
		return classes;
	}
			
	private void loadFile(
			String fileName,
			JaptRepository repository,
			Logger logger,
			ClassLoadingQueue queue,
			BT_ClassVersion classVersion) throws IOException {
		File file = new File(fileName).getCanonicalFile();
		if(!file.exists()) {
			throw new IOException("file inexistent");
		}
		//we create a class path entry that essentially contains only the one file we are attempting to load
		FileClassPathEntry classPathEntry = new AssemblyFileClassPathEntry(
			messages, logger, file, repository, queue, classVersion);
		BT_ClassPathEntry.BT_ClassPathLocation loc = classPathEntry.getLocation();
		repository.addExtendedClassPathEntry(classPathEntry);
		String name = loc.getClassName();
		if(name != null) {
			repository.loadClass(name, loc);
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return null or an array of options pertaining to the extension
	 */
	public Option[] getOptions() {
		
		return combine(new Option[] { 
				resetClassPath, 
				internalClassPathList,
				load,
				classVersionOption,
				fileExtension
		}, super.getOptions());
	}
}
