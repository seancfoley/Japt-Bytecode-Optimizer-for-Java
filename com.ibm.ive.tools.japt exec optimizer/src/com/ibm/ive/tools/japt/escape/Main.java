package com.ibm.ive.tools.japt.escape;



/**
 * @author sfoley
 * 
 *
 */
public class Main {
	public String programName = "escape tool";
	
	
	/**
	 * Override this method to configure the opening logo message
	 */
	
	public static void main(String args[]) {
		com.ibm.ive.tools.japt.commandLine.Main main = new com.ibm.ive.tools.japt.commandLine.Main();
		main.execute(args, false);
	}
	
	

}
