/*
 * Created on Sep 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.assembler;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;


/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class BaseAssemblerExtension implements CommandLineExtension {

	public static String DEFAULT_FILE_EXTENSION = ".jasm";
	
	private BaseMessages messages = new BaseMessages(this);
	
	private ValueOption assemblyFileExtension = new ValueOption(messages.USE_EXTENSION_LABEL, messages.USE_EXTENSION);
	
	static Option[] combine(Option one[], Option two[]) {
		Option res[] = new Option[one.length + two.length];
		System.arraycopy(one, 0, res, 0, one.length);
		System.arraycopy(two, 0, res, one.length, two.length);
		return res;
	}
	
	/**
	 * 
	 */
	public BaseAssemblerExtension() {}

	/**
	 * returns the file extension for assmembly files, which starts with a '.' character
	 */
	public String getFileExtension() {
		if(assemblyFileExtension.appears()) {
			String value = assemblyFileExtension.getValue();
			if(!value.startsWith(".")) {
				value = '.' + value;
			}
			return value;
		}
		return DEFAULT_FILE_EXTENSION;
	}
	
	/**
	 * @return null or an array of options pertaining to the extension
	 */
	public Option[] getOptions() {
		return new Option[] { 
				assemblyFileExtension
		};
	}
}
