/*
 * Created on Mar 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine.options;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.CommandLineParser;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.OptionException;
import com.ibm.ive.tools.japt.commandLine.OptionsParser;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class MacroOption extends Option {

	private boolean appears;
	
	/**
	 * @param name
	 * @param description
	 */
	public MacroOption(String name, String description) {
		super(name, description, 2);
	}

	
	
	protected void handle(String args[], CommandLineParser fromParser) throws CommandLineException {
		if(args.length == 0 || args.length > argCount) {
			throw new OptionException(this);
		} else if(args.length == 1) {
			//we allow for the alternate format of macro xxx=yyy instead of macro xxx yyy
			String arg = args[0];
			if(arg == null) {
				throw new OptionException(this);
			}
			int index = arg.indexOf('=');
			if(index <= 0 || index == arg.length() - 1) {
				throw new OptionException(this);
			}
			args = new String[] {arg.substring(0, index), arg.substring(index + 1)};
		} else {
			for(int i=0; i<args.length; i++) {
				if(args[i] == null) {
					throw new OptionException(this);
				}
			}
		}
		setValues(args, fromParser);
	}
	
	public void setValues(String s[], com.ibm.ive.tools.commandLine.CommandLineParser fromParser) {
		appears = true;
		if(s != null) {
			OptionsParser parser = (OptionsParser) fromParser;
			parser.addMacro(s[0], s[1]);
		}
	}
	
	public boolean appears() {
		return appears;
	}

}
