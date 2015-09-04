package com.ibm.ive.tools.commandLine;

/**
 * @author sfoley
 * <p>
 * Represents a boolean command line option - of this option is present on the command line
 * then the option is considered "flagged" or "true".  The option may have a dual FlagOption that negates
 * the "flag" or boolean value.
 * <p>
 * For example, if the program were to be called "run", then it might have a flag option "quickly", and
 * a dual flag "slowly", allowing for the command lines:<br>
 * run -quickly
 * run -slowly
 * run -quickly -slowly
 * In the third example, the option appearing last on the command line takes precedence.
 */
public class FlagOption extends Option {

	public boolean flagged;
	private FlagOption flagDual; //the flag that negates this flag
	private ValueOption valueDual; 
	
	public FlagOption(String name, String description) {
		super(name, description, 0);
	}
	
	public FlagOption(String name) {
		super(name, 0);
	}
	
	/**
	 * The dual will always have a value opposite this flag
	 */
	public void setDual(FlagOption dual) {
		flagDual = dual;
	}
	
	public void setDual(ValueOption dual) {
		valueDual = dual;
	}
	
	/**
	 * Handle an occurence of this flag on the command line
	 */
	protected void handle(String args[], CommandLineParser fromParser) throws CommandLineException {
		setFlagged(true);
	}
	
	public void setFlagged(boolean flagged) {
		this.flagged = flagged;
		if(flagDual != null) {
			flagDual.flagged = !flagged;
		}
		if(valueDual != null && flagged) {
			valueDual.value = null;
		}
	}
	
	public boolean isFlagged( ){
		return flagged;
	}
	
	public boolean appears() {
		return flagged;
	}
	
}
