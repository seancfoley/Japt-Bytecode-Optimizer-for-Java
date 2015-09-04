package com.ibm.ive.tools.japt;


/**
 *
 * Represents an extension to the application.  The base of the application consists of JIKESBT 
 * and the package com.ibm.ive.tools.japt.  An extension may access the public API of these packages.
  * @author sfoley
 */
public interface Extension extends Component {
	
	/**
	 * execute the extension on the classes in the given repository
	 * @param repository the repository with the loaded classes
	 * @param logger the output logger for message output
	 * @throws ExtensionException the extension failed to complete its class and resource manipulations
	 */
	void execute(JaptRepository repository, Logger logger) throws ExtensionException;
	
}
