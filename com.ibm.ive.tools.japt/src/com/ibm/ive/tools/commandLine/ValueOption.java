package com.ibm.ive.tools.commandLine;


/**
 * @author sfoley
 * <p>
 * An option that is meant to appear on the command line just once with a single argument.
 * If it appears a second time then the second appearance takes precedence.
 */
public class ValueOption extends ArgumentOption {

	String value;
	ValueOption valueDual;
	FlagOption flagDual;
	
	public ValueOption(String name, String description) {
		super(name, description, 1);
	}
	
	public ValueOption(String name) {
		super(name, 1);
	}
		
	protected void handle(String args[], CommandLineParser fromParser) throws CommandLineException {
		if(args.length != 1) {
			throw new OptionException(this);
		}
		String value = args[0];
		if(value == null) {
			throw new OptionException(this);
		}
		setValue(value);
	}
	
	public void setValue(String s) {
		value = s;
		if(valueDual != null && s != null) {
			valueDual.value = null;
		}
		if(flagDual != null) {
			flagDual.flagged = (s == null);
		}
	}
	
	public void setDual(FlagOption dual) {
		flagDual = dual;
	}
	
	public void setDual(ValueOption dual) {
		valueDual = dual;
	}
	
	public boolean appears() {
		return value != null;
	}
	
	public String getValue() {
		return value;
	}

	public String[] getStrings() {
		return new String[] {value};
	}
}
