package com.ibm.ive.tools.japt.commandLine;

import com.ibm.ive.tools.japt.Extension;
import com.ibm.ive.tools.commandLine.*;

/**
 * @author sfoley
 *
 * <p>
 * Extensions may be added dynamically by using the -extension command line argument.
 * <p>
 * Certain extensions are included with the program, such as the output extension that generates jar files.
 * <p>
 */
public interface CommandLineExtension extends Extension {
	/**
	 * @return null or an array of options pertaining to the extension
	 */
	Option[] getOptions();
	
}
