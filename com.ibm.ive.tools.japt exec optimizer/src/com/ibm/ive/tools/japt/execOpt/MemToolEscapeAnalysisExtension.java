package com.ibm.ive.tools.japt.execOpt;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.IntegratedExtension;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.escape.EscapeAnalysisExtension;

public class MemToolEscapeAnalysisExtension extends EscapeAnalysisExtension implements IntegratedExtension {
	
	public void setName(String name) {
		this.name = name;
	}
		
	public Option[] getOptions() {
		return new Option[] {
			depthLimitOption,
			escapeLogOption,
			escapeMethods
		};
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) throws ExtensionException {
		if(escapeLogOption.appears()) {
			noUseIntraOption.setFlagged(false);
			fullAnalysis.setFlagged(false);
			super.execute(repository, logger);
		} else {
			if(depthLimitOption.appears() || escapeMethods.appears()) {
				logger.logWarning(JaptMessage.formatStart(this).toString());
				logger.logWarning("Must specify " + escapeLogOption.getAppearance() + " when specifying "
						+ depthLimitOption.getAppearance() + " or " + escapeMethods.getAppearance());
				logger.logWarning(Logger.endl);
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
			logger.logProgress("Analyzing Object Escape from Methods...");
			logger.logProgress(Logger.endl);
		}
	}
	
	public void noteExecuted(Logger logger, String timeString) {
		if(doRun) {
			logger.logProgress(JaptMessage.formatStart(this).toString());
			logger.logProgress("Completed in ");
			logger.logProgress(timeString);
			logger.logProgress(Logger.endl);
		}
	}
}
