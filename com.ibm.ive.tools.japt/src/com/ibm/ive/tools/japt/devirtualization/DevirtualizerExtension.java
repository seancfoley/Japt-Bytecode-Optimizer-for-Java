/*
 * Created on Feb 24, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.devirtualization;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DevirtualizerExtension implements CommandLineExtension {

	Messages messages = new Messages(this);
	private String name = messages.DESCRIPTION;
	public FlagOption devirtualizeSpecial = new FlagOption(messages.DEVIRTUALIZE_SPECIAL_LABEL, messages.DEVIRTUALIZE_SPECIAL);
	public FlagOption devirtualizeStatic = new FlagOption(messages.DEVIRTUALIZE_STATIC_LABEL, messages.DEVIRTUALIZE_STATIC);
	public FlagOption makeFinal = new FlagOption(messages.MAKE_FINAL_LABEL, messages.MAKE_FINAL);
	
	//	assume that virtual method calls can be overridden, so that even if there is no overriding method
	//currently found, that at run-time more will be found
	//this flag mirrors a flag in the inliner extensions
	public FlagOption assumeUnknownVirtualTargets = new FlagOption(messages.ASSUME_UNKNOWN_LABEL, messages.ASSUME_UNKNOWN);
	
	/**
	 * 
	 */
	public DevirtualizerExtension() {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.commandLine.CommandLineExtension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {devirtualizeStatic, devirtualizeSpecial, makeFinal, assumeUnknownVirtualTargets};
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger)
		throws ExtensionException {
		Devirtualizer devirtualizer = new Devirtualizer(repository, logger, messages, assumeUnknownVirtualTargets.isFlagged());
		if(devirtualizeStatic.isFlagged() || devirtualizeSpecial.isFlagged()) {
			if(devirtualizeStatic.isFlagged()) {
				devirtualizer.devirtualizeCallsToStatic();
				devirtualizer.removeUnreferencedMethods();
			}
			if(devirtualizeSpecial.isFlagged()) {
				devirtualizer.devirtualizeCallsToSpecial();
			}
			if(makeFinal.isFlagged()) {
				devirtualizer.makeFinal();
				
			}
			messages.DEVIRTUALIZE_SUMMARY.log(logger, 
					new String[] {Integer.toString(devirtualizer.staticCount), 
					Integer.toString(devirtualizer.specialCount), 
					Integer.toString(devirtualizer.callSiteCount)
			});
		}
		if(makeFinal.isFlagged()) {
			devirtualizer.makeFinal();
			messages.MAKE_FINAL_SUMMARY.log(logger, 
					new String[] {
					Integer.toString(devirtualizer.classesFinalCount),
					Integer.toString(devirtualizer.candidateClassesCount),
					Integer.toString(devirtualizer.methodsFinalCount),
					Integer.toString(devirtualizer.methodsFinalCandidateCount)
			});
		}
					
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}

}
