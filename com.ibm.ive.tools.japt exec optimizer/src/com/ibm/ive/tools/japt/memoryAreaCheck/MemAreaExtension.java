package com.ibm.ive.tools.japt.memoryAreaCheck;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.IntegratedExtension;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.ive.tools.japt.reduction.ita.ContextProperties;
import com.ibm.ive.tools.japt.reduction.ita.PropagationProperties;
import com.ibm.ive.tools.japt.reduction.ita.Repository;

/**
 * @author sfoley
 *
 */
public class MemAreaExtension implements com.ibm.ive.tools.japt.commandLine.CommandLineExtension, IntegratedExtension {

	protected Messages messages = new Messages(this);
	private String name = "path";
	
	public FlagOption noMarkEntryPoints = new FlagOption(messages.NO_MARK_ENTRY_LABEL, messages.NO_MARK_ENTRY);
	public ValueOption entryPointFile = new ValueOption("entryPointFile", "create named file listing points of entry");
	public ValueOption errorFile = new ValueOption("errorFile", "create named file listing errors");
	public FlagOption checkExternal = new FlagOption("checkExternal", "report errors in external classes");
	public FlagOption followExternal = new FlagOption("followExternal", "follow path analysis into external classes");
	
	public ValueOption depthLimitOption = new ValueOption("contextAnalysisDepth", "constrain RTSJ context analysis to given call stack depth");
	public FlagOption split = new FlagOption("split", "split jars according to thread type");
	final com.ibm.ive.tools.japt.ErrorReporter reporter;
	
	public MemAreaExtension() {
		this(null);
	}
	
	public MemAreaExtension(com.ibm.ive.tools.japt.ErrorReporter rep) {
		this.reporter = rep;
	}

	/**
	 * @see com.ibm.ive.tools.japt.Extension#getName()
	 */
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {
			//noMarkEntryPoints, 
			entryPointFile, 
			//depthLimitOption, 
			checkExternal,
			followExternal,
			errorFile,
			split};
	}

	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository repository, final Logger logger) throws ExtensionException {
		if(!split.isFlagged() && !entryPointFile.appears() && !errorFile.appears()) {
			if(checkExternal.appears() || followExternal.appears()) {
				logger.logWarning(JaptMessage.formatStart(this).toString());
				logger.logWarning("Must specify " + split.getAppearance() + " or " 
					+ entryPointFile.getAppearance() + " or " +  errorFile.getAppearance() + " when specifying "
						+ checkExternal.getAppearance() + " or " + followExternal.getAppearance());
				logger.logWarning(Logger.endl);
			}
			return;
		}
		com.ibm.ive.tools.japt.reduction.ClassProperties classProps = new com.ibm.ive.tools.japt.reduction.ClassProperties(repository);
		ClassProperties rtsjProps = new ClassProperties(repository);
		ContextProperties contextProperties = new ContextProperties();
		TypeProperties typeProperties = new TypeProperties(rtsjProps, contextProperties);
		RTSJContextProvider cp = new RTSJContextProvider(typeProperties, 
				new ErrorReporter(reporter, checkExternal.isFlagged()), 
				followExternal);
		PropagationProperties props = 
			new PropagationProperties(
					PropagationProperties.RTSJ_ANALYSIS,
					cp,
					new RTSJInstantiatorProvider(typeProperties, contextProperties, classProps, cp),
					classProps);
//		props.setDepthLimit(depthLimitValue);
//		props.setEnterExternalMethods(false);
		props.verboseIterations = true;
		props.storeCreatedInMethodInvocations = true;
		props.setIntraProceduralAnalysis(true);
		Analyzer analyzer = new Analyzer(repository, logger, messages, this, props, contextProperties);
		PrintStream entryPointStream = null;
		if(entryPointFile.appears()) {
			String fileName = entryPointFile.getValue();
			try {
				entryPointStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
				messages.CREATING_ENTRY_POINT_FILE.log(logger, fileName);
			} catch(IOException e) {
				messages.COULD_NOT_OPEN_FILE.log(logger, fileName);
			}
		}
		if(entryPointStream != null) {
			analyzer.entryPointLister = new EntryPointLister(entryPointStream, false, repository);
		}
		Repository rep = analyzer.analyze();
		if(entryPointStream != null) {
			entryPointStream.close();
		}
		if(split.isFlagged()) {
			ClassPathEntry cpes[] = repository.getInternalClassPaths();
			ClassPathSplitter splitter = new ClassPathSplitter(repository, rep);
			for(int i=0; i<cpes.length; i++) {
				ClassPathEntry cpe = cpes[i];
				ClassPathEntry newEntries[] = splitter.split(cpe);
				for(int j=0; j<newEntries.length; j++) {
					repository.appendInternalClassPathEntry(newEntries[j]);
				}
			}
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
			logger.logProgress("Performing context control flow analysis...");
			logger.logProgress(Logger.endl);
		}
	}
	
	public void noteExecuted(Logger logger, String timeString) {
		if(doRun) {
			logger.logProgress(JaptMessage.formatStart(this).toString());
			logger.logProgress("Completed context analysis in ");
			logger.logProgress(timeString);
			logger.logProgress(Logger.endl);
		}
	}
}
