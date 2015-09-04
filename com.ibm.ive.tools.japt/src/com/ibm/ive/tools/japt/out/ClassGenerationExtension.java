/*
 * Created on Feb 20, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.out;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Resource;
import com.ibm.jikesbt.BT_ClassVector;

public class ClassGenerationExtension extends GenerationExtension {
	private String name = messages.DESCRIPTION_DIR;
	public ValueOption dirTarget = new ValueOption(messages.DIR_LABEL, messages.DIR); 
	
	public ClassGenerationExtension() {}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#getOptions()
	 */
	public Option[] getOptions() {
		return combine(new Option[] {dirTarget, noStripMetaFiles}, super.getOptions());
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) throws ExtensionException {
		this.logger = logger;
		String targetDir = dirTarget.getValue();
		if(targetDir == null) {
			messages.NO_TARGET.log(logger);
			throw new ExtensionException(this);
		}
		try {
			writeOutput(targetDir, repository);
		} catch(IOException e) {
			messages.ERROR_WRITING_TO_DIR.log(logger, new Object[] {targetDir, e});
		}
	}
	
	void writeOutput(String target, JaptRepository repository) throws IOException, FileNotFoundException {
		File outputTarget = new File(target);
		if(!outputTarget.exists()) {
			//create a new directory
			boolean result = outputTarget.mkdirs();
			if(!result) {
				messages.COULD_NOT_CREATE_DIRECTORY.log(logger, target);
				return;
			}
		} else if(!outputTarget.isDirectory()) {
			messages.DIRECTORY_EXISTS.log(logger, target);
			return;
		}
		
		ClassPathEntry cpes[] = repository.getInternalClassPaths();
		DirGenerator dirGenerator = null;
		try {
			for(int i=0; i<cpes.length; i++) {
				ClassPathEntry cpe = cpes[i];
				Resource resources[] = cpe.getResources(includeZipped.isFlagged());
				BT_ClassVector clzs = cpe.getLoadedClasses();
				ResourceController controller = new ResourceController(resources);
				
				/* filters out unwanted resources, and separates the manifest from the rest */
				resources = controller.getResources();
				
				if(resources.length > 0 || clzs.size() > 0 || (noStripMetaFiles.isFlagged() && controller.hasManifest())) {
					if(dirGenerator == null) {
						dirGenerator = new DirGenerator(outputTarget, messages, logger);
						if(createAutoLoaders.isFlagged()) {
							/* auto-load class will only be created if classes are written */
							dirGenerator.setLoader(new AutoLoaderCreator(repository));
						}
					}
					if(resources.length > 0 || clzs.size() > 0) {
						writeToGenerator(repository, dirGenerator, clzs, resources);
					}
					if(noStripMetaFiles.isFlagged()) {
						controller.writeManifest(dirGenerator);
					}
				}
			}
			if(dirGenerator != null) {
				writeLoader(dirGenerator);
			}
		} finally {
			if(dirGenerator != null) {
				try {
					dirGenerator.close();
				} catch(IOException e) {}
				messages.WRITTEN_TO.log(logger, outputTarget);
			} else {
				messages.NO_OUTPUT.log(logger);
			}
		}
	}
	
	public String getName() {
		return name;
	}

}
