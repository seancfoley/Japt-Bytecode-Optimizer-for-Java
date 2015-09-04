package com.ibm.ive.tools.japt.out;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Resource;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;

/**
 *
 * This extension is responsible for writing the classes and resources to jar files
 * 
 * @author sfoley
 */
public class JarGenerationExtension extends GenerationExtension {

	protected String name = messages.DESCRIPTION;
	public ValueOption target = new ValueOption(messages.OUTPUT_LABEL, messages.OUTPUT); 
	public FlagOption noCompress = new FlagOption(messages.NO_COMPRESS_LABEL, messages.NO_COMPRESS);
	public SpecifierOption excludeClass = new SpecifierOption(messages.EXCLUDE_CLASS_LABEL, messages.EXCLUDE_CLASS);
	
	public JarGenerationExtension() {
		noStripMetaFiles.setFlagged(true);
	}

	/**
	 * @see com.ibm.ive.tools.japt.Extension#getOptions()
	 */
	public Option[] getOptions() {
		return combine(new Option[] {target, noCompress, excludeClass, stripDigest}, super.getOptions());
	}
	
	private static String getNextName(String name, int index) {
		String lowerCaseName = name.toLowerCase();
		int dotIndex = lowerCaseName.indexOf('.');
		if(dotIndex >= 0) {
			return name.substring(0, dotIndex) + index + name.substring(dotIndex);
		}
		return name + index;
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) {
		this.logger = logger;
		String targetJar = target.getValue();
		if(targetJar == null) {
			messages.NO_TARGET.log(logger);
		} else {
			writeOutput(targetJar, repository);
		}
	}

	protected void writeOutput(String target, JaptRepository repository) {
		Specifier classesToExclude[] = excludeClass.getSpecifiers();
		for(int i=0; i<classesToExclude.length; i++) {
			Specifier specifier = classesToExclude[i];
			try {
				if(!specifier.isConditional() || specifier.conditionIsTrue(repository)) {
					excludeCandidates = repository.findClasses(specifier.getIdentifier(), true);
				}
			} catch(InvalidIdentifierException e) {
				repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		File outputTarget = new File(target);
		ClassPathEntry cpes[] = repository.getInternalClassPaths();
		if(!outputTarget.isDirectory()) {
			
			if(outputTarget.getName().indexOf('.') == -1) {
				String parent = outputTarget.getParent();
				if(parent == null) {
					parent = "";
				}
				parent = parent.trim();
				String prefix = parent.equals("") ? "" : parent + File.separatorChar;
				outputTarget = new File(prefix + outputTarget.getName() + ".jar");
			}
			
			writeDefaultJar(outputTarget, repository, cpes, true);
			return;
		}
		ArrayList list = new ArrayList(cpes.length);
		for(int i=0; i<cpes.length; i++) {
			ClassPathEntry cpe = cpes[i];
			if(!cpe.isArchive()) {
				list.add(cpe);
			}
		}
		boolean wroteJar;
		int size = list.size();
		if(size > 0) {
			ClassPathEntry cpes2[] = (ClassPathEntry[]) list.toArray(new ClassPathEntry[size]);
			File defaultJarFile = new File(outputTarget, getDefaultName());
			wroteJar = writeDefaultJar(defaultJarFile, repository, cpes2, false);
		} else {
			wroteJar = false;
		}
		Set outputNames = new HashSet();
		for(int i=0; i<cpes.length; i++) {
			ClassPathEntry cpe = cpes[i];
			if(cpe.hasLoaded() && cpe.isArchive()) {
				BT_ClassVector cv = cpe.getLoadedClasses();
				Resource resources[] = cpe.getResources(includeZipped.isFlagged());
				String name = cpe.getName();
				int j = 1;
				while(outputNames.contains(name)) {
					name = getNextName(name, ++j);
				}
				outputNames.add(name);
				File output = new File(outputTarget, name);
				try {
					writeToJar(repository, output, cv, resources);
					wroteJar = true;
					messages.CREATED.log(logger, output.getAbsolutePath());
				} catch(IOException e) {
					messages.ERROR_WRITING_JAR.log(logger, new Object[] {outputTarget, e});
				} catch(InternalError e) {
					messages.ERROR_WRITING_JAR.log(logger, new Object[] {outputTarget, e});
				}
			}
		}
		if(!wroteJar) {
			messages.NO_OUTPUT.log(logger);
		}
	}
	
	public String getDefaultName() {
		return "japtOut.jar";
	}
	
	protected boolean writeDefaultJar(
			File outputTarget, 
			JaptRepository repository, 
			ClassPathEntry cpes[], 
			boolean writeEmpty) {
		JarGenerator defaultJar = null;
		try {
			BT_Class mainClasses[] = repository.getAllMainClasses();
			BT_Class foundMainClasses[] = (mainClasses.length > 0) ? new BT_Class[mainClasses.length] : null;
			boolean haveMainClass = false;
			boolean haveManifest = false;
			for(int i=0; i<cpes.length; i++) {
				ClassPathEntry cpe = cpes[i];
				Resource resources[] = cpe.getResources(includeZipped.isFlagged());
				BT_ClassVector clzs = cpe.getLoadedClasses();
				if(resources.length > 0 || clzs.size() > 0) {
					if(defaultJar == null) {
						defaultJar = createJarGenerator(repository, outputTarget);
					}
					
					ResourceController controller = new ResourceController(resources);
					resources = controller.getResources();
					writeToGenerator(repository, defaultJar, clzs, resources);
					/* 
					 * note that only the first manifest will make it in, the second time
					 * the following call tried to write a manifest it will output an error message 
					 */
					controller.writeManifest(defaultJar);
					haveMainClass |= foundMainClass(mainClasses, foundMainClasses, clzs);
					haveManifest = haveManifest || controller.hasManifest();
				}
			}
			if(defaultJar != null) {
				if(haveMainClass && !haveManifest) {
					writeNewManifest(getFirstFound(foundMainClasses), defaultJar);
				}
				writeLoader(defaultJar);
				messages.CREATED.log(logger, outputTarget);
			} else if (writeEmpty) {
				defaultJar = createJarGenerator(repository, outputTarget);
				//Note that a ZipException is thrown if we try to create an empty jar
				writeNewManifest(null, defaultJar);
				messages.NO_OUTPUT.log(logger);
			}
		} catch(IOException e) {
			messages.ERROR_WRITING_JAR.log(logger, new Object[] {outputTarget, e});
		} finally {
			try {
				if(defaultJar != null) {
					defaultJar.close();
				}
			} catch(IOException e) {
				messages.ERROR_WRITING_JAR.log(logger, new Object[] {outputTarget, e});
			}
		}
		return defaultJar != null;
	}
	
	private void writeToJar(JaptRepository repository, File file, 
			BT_ClassVector classes, Resource resources[]) throws IOException {
		JarGenerator jarGen = null;
		try {
			jarGen = createJarGenerator(repository, file);
			
			ResourceController controller = new ResourceController(resources);
			resources = controller.getResources();
			writeToGenerator(repository, jarGen, classes, resources);
			if(controller.hasManifest()) {
				controller.writeManifest(jarGen);
			} else {
				BT_Class mainClasses[] = repository.getAllMainClasses();
				if(mainClasses.length > 0) {
					BT_Class foundClasses[] = new BT_Class[mainClasses.length];
					if(foundMainClass(mainClasses, foundClasses, classes)) {
						writeNewManifest(getFirstFound(foundClasses), jarGen);
					}
				}
			}
			writeLoader(jarGen);
		} finally {
			try {
				if(jarGen != null) {
					jarGen.close();
				}
			} catch(IOException e) {}
		}
	}

	public String getName() {
		return name;
	}
	
	/**
	 * @param outputTarget
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private JarGenerator createJarGenerator(JaptRepository repository, File outputTarget) throws IOException, FileNotFoundException {
		JarGenerator defaultJar = new JarGenerator(new FileOutputStream(outputTarget));
		if(createAutoLoaders.isFlagged()) {
			defaultJar.setLoader(new AutoLoaderCreator(repository));
		}
		if(noCompress.isFlagged()) {
			defaultJar.setDefaultCompressed(false);
		}
		return defaultJar;
	}

	static BT_Class getFirstFound(BT_Class foundClasses[]) {
		for(int i=0; i<foundClasses.length; i++) {
			BT_Class res = foundClasses[i];
			if(res != null) {
				return res;
			}
		}
		return null;
	}
	private static boolean foundMainClass(BT_Class mainClasses[], BT_Class foundClasses[], BT_ClassVector inClasses) {
		if(mainClasses.length == 0) {
			return false;
		}
		boolean res = false;
		for(int i=0; i<mainClasses.length; i++) {
			BT_Class clz = mainClasses[i];
			if(inClasses.contains(clz)) {
				res = true;
				foundClasses[i] = clz;
			}
		}
		return res;
	}

	/**
	 * @param jarGen
	 */
	private void writeNewManifest(BT_Class mainClass, JarGenerator jarGen) {
		try {
			Manifest manifest = new Manifest();
			Attributes attr = manifest.getMainAttributes();
			attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			if(mainClass != null) {
				attr.put(Attributes.Name.MAIN_CLASS, mainClass.getName());
			}
			jarGen.write(manifest);
			messages.WROTE_RESOURCE.log(logger, JarFile.MANIFEST_NAME);
		}
		catch(IOException e) {
			messages.ERROR_WRITING_RESOURCE.log(logger, new Object[] {JarFile.MANIFEST_NAME, e});
		}
	}
}
