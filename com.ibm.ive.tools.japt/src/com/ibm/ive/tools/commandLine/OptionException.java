package com.ibm.ive.tools.commandLine;

/**
 * @author sfoley
 *
 */
public class OptionException extends CommandLineException {
	Option option;
	
	public OptionException(Option option) {
		this.option = option;
	}

	public OptionException(Option option, String detailMessage) {
		super(detailMessage);
		this.option = option;
	}
	
	public Option getOption() {
		return option;
	}
	
	
}
