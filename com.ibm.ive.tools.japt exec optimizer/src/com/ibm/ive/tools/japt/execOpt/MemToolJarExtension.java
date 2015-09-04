package com.ibm.ive.tools.japt.execOpt;

import java.io.File;
import java.util.ArrayList;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.IntegratedExtension;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.memoryAreaCheck.ClassPathSplitter;
import com.ibm.ive.tools.japt.memoryAreaCheck.RTSJCallingContext;
import com.ibm.ive.tools.japt.out.JarGenerationExtension;

public class MemToolJarExtension extends JarGenerationExtension implements IntegratedExtension {
	final FlagOption split;
	//final SingleIdentifierOption mainClass;
	final public FlagOption removeDebugInfo = new FlagOption("removeDebugInfo", "remove debug attributes from classes");
	final public FlagOption removeInfoAttributes = new FlagOption("removeSourceInfo", "remove source file info from classes");
	final public FlagOption removeAnnotations = new FlagOption("removeAnnotations", "remove annotations from classes");
	
	public MemToolJarExtension(FlagOption split, String packageName) {
		this.split = split;
		this.loaderPackage = packageName;
		removeAttribute.setDescription("remove named attribute(s) from classes");
		target.setDescription("write to named jar file or directory");
		//excludeResource.setName("removeAttribute");
		excludeResource.setDescription("exclude the named resource(s) from archives");
		//this.mainClass = mainClass;
	}
	
	public String getDefaultName() {
		return "execOptOut.jar";
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Option[] getOptions() {
		return new Option[] {
				target,
				//noCompress,
				
				removeDebugInfo,
				removeInfoAttributes,
				removeAnnotations,
				removeAttribute,
				removeStackMaps,
				addStackMaps,
				preverifyForCLDC,//this will be a hidden option
				excludeResource, 
				excludeClass,
				createAutoLoaders,
				//includeZipped,
				newVersion
				};
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) {
		includeZipped.setFlagged(true);
		noStripDebugInfo.setFlagged(!removeDebugInfo.isFlagged());
		noStripInfoAttributes.setFlagged(!removeInfoAttributes.isFlagged());
		noStripAnnotations.setFlagged(!removeAnnotations.isFlagged());
		this.logger = logger;
		String targetJar = target.getValue();
		if(targetJar == null) {
			if(removeDebugInfo.appears() ||
					removeInfoAttributes.appears() ||
					removeAnnotations.appears() ||
					removeAttribute.appears() ||
					//noCompress.appears() || 
					removeStackMaps.appears() || 
					addStackMaps.appears() || 
					excludeResource.appears() ||
					excludeClass.appears() ||
					createAutoLoaders.appears() ||
					preverifyForCLDC.appears())
			messages.NO_TARGET.log(logger);
			return;
		}
		super.writeOutput(targetJar, repository);
	}
	
	private boolean writeSegment(
			String suffix, 
			int contextFlags,
			File outputTarget, 
			JaptRepository repository,
			ClassPathEntry cpes[],
			boolean written[],
			boolean writeEmpty) {
		ArrayList list = new ArrayList(cpes.length);
		for(int i=0; i<cpes.length; i++) {
			ClassPathEntry cpe = cpes[i];
			if(!written[i] && (cpe.getContextFlags() & contextFlags) == contextFlags) {
				list.add(cpe);
				written[i] = true;
			}
		}
		if(list.size() > 0) {
			outputTarget = modifyTarget(suffix, outputTarget);
			ClassPathEntry entries[] = (ClassPathEntry[]) list.toArray(new ClassPathEntry[list.size()]);
			return super.writeDefaultJar(outputTarget, repository, entries, writeEmpty);
		}
		return false;
	}

	private static File modifyTarget(String suffix, File outputTarget) {
		String parent = outputTarget.getParent();
		if(parent == null) {
			parent = "";
		}
		String names[] = ClassPathSplitter.splitName(outputTarget.getName());
		parent = parent.trim();
		String prefix = parent.equals("") ? "" : parent + File.separatorChar;
		outputTarget = new File(prefix + names[0] + suffix + names[1]);
		return outputTarget;
	}
	
	protected boolean writeDefaultJar(
			File outputTarget, 
			JaptRepository repository, 
			ClassPathEntry cpes[], 
			boolean writeEmpty) {
		if(split.isFlagged()) {
			/* all class path entries have already been split */
			/* each entry is either completely NO_HEAP_REAL_TIME_ACCESSED,
			 * REAL_TIME_ACCESSED, NOT_ACCESSED, JAVA_LANG_THREAD_ACCESSED,
			 * or resources.  So we must join multiple entries of the same
			 * type into the same default jar.
			 */
			boolean written[] = new boolean[cpes.length];
			boolean result1 = writeSegment(ClassPathSplitter.NHRT_SUFFIX, 
					RTSJCallingContext.NO_HEAP_REAL_TIME_ACCESSED,
					outputTarget, repository, cpes, written, writeEmpty);
			boolean result2 = writeSegment(ClassPathSplitter.RT_SUFFIX, 
					RTSJCallingContext.REAL_TIME_ACCESSED, 
					outputTarget, repository, cpes, written, writeEmpty);
			boolean result3 = writeSegment(ClassPathSplitter.UNREACHABLE_SUFFIX, 
					RTSJCallingContext.NOT_ACCESSED, 
					outputTarget, repository, cpes, written, writeEmpty);
			boolean result4 = writeSegment(ClassPathSplitter.JAVA_LANG_THREAD_SUFFIX, 
					RTSJCallingContext.JAVA_LANG_THREAD_ACCESSED, 
					outputTarget, repository, cpes, written, writeEmpty);
			ArrayList list = new ArrayList(cpes.length);
			for(int i=0; i<cpes.length; i++) {
				if(!written[i]) {
					ClassPathEntry cpe = cpes[i];
					list.add(cpe);
				}
			}
			if(list.size() > 0) {
				outputTarget = modifyTarget(ClassPathSplitter.RESOURCE_SUFFIX, outputTarget);
				ClassPathEntry entries[] = (ClassPathEntry[]) list.toArray(new ClassPathEntry[list.size()]);
				return super.writeDefaultJar(outputTarget, repository, entries, writeEmpty) 
					|| result1 || result2 || result3 || result4;
			}
			return result1 || result2 || result3 || result4;
		} else {
			return super.writeDefaultJar(outputTarget, repository, cpes, writeEmpty);
		}
	}
	
	private boolean doRun;
	
	public void noteExecuting(Logger logger) {
		Option opts[] = getOptions();
		for(int i=0; i<opts.length; i++) {
			if(opts[i].appears()) {
				doRun = true;
				break;
			}
		}
		if(doRun) {
			logger.logProgress(JaptMessage.formatStart(this).toString());
			logger.logProgress("Writing...");
			logger.logProgress(Logger.endl);
		}
	}
	
	public void noteExecuted(Logger logger, String timeString) {
		if(doRun) {
			logger.logProgress(JaptMessage.formatStart(this).toString());
			logger.logProgress("Completed writing in ");
			logger.logProgress(timeString);
			logger.logProgress(Logger.endl);
		}
	}
}
