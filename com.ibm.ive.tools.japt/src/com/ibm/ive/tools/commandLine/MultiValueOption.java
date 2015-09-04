package com.ibm.ive.tools.commandLine;


/**
 * @author sfoley
 * <p>
 * An option that is meant to appear on the command line just once with a multiple arguments.
 * If it appears a second time then the second appearance takes precedence.
 */
public class MultiValueOption extends ArgumentOption {

	String values[];
	FlagOption flagDual;
	
	public MultiValueOption(String name, String description, int count) {
		super(name, description, count);
	}
	
	public MultiValueOption(String name, int count) {
		super(name, count);
	}
	
	protected void handle(String args[], CommandLineParser fromParser) throws CommandLineException {
		if(args.length != argCount) {
			throw new OptionException(this);
		}
		for(int i=0; i<args.length; i++) {
			if(args[i] == null) {
				throw new OptionException(this);
			}
		}
		setValues(args);
	}
	
	public void setValues(String s[]) {
		values = s;
		if(flagDual != null) {
			flagDual.flagged = (s == null);
		}
	}
	
	public void setDual(FlagOption dual) {
		flagDual = dual;
	}
	
	public boolean appears() {
		return values != null;
	}
	
	public String[] getValues() {
		return values;
	}

	public String[] getStrings() {
		return values;
	}
}
