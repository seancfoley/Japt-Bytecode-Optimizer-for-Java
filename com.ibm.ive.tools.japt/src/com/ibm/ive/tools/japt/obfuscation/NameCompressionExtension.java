package com.ibm.ive.tools.japt.obfuscation;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;

/**
 * @author sfoley
 *
 * <p>
 */
public class NameCompressionExtension implements com.ibm.ive.tools.japt.commandLine.CommandLineExtension {
	final Messages messages = new Messages(this);
	final private String name = messages.DESCRIPTION;
	final public FlagOption expandPermissions = new FlagOption(messages.EXPAND_PERMISSIONS_LABEL, messages.EXPAND_PERMISSIONS); 
	final public FlagOption caseSensitiveClassNames = new FlagOption(messages.CASE_SENSITIVE_LABEL, messages.CASE_SENSITIVE);
	final public FlagOption reuseConstantPoolStrings = new FlagOption(messages.REUSE_CONSTANT_POOL_LABEL, messages.REUSE_CONSTANT_POOL);
	final public FlagOption noRenamePackages = new FlagOption(messages.NO_RENAME_PACKAGE_LABEL, messages.NO_RENAME_PACKAGE);
	final public ValueOption baseName = new ValueOption(messages.BASE_NAME_LABEL, messages.BASE_NAME);
	final public ValueOption packageBaseName = new ValueOption(messages.PACKAGE_BASE_NAME_LABEL, messages.PACKAGE_BASE_NAME);
	final public ValueOption logFile = new ValueOption(messages.OBFUSCATED_LOG_LABEL, messages.OBFUSCATED_LOG);
	final public FlagOption prepend = new FlagOption(messages.PREPEND_LABEL, messages.PREPEND);
	{prepend.setVisible(false);}
	
	//TODO obfuscate annotation classes?
	//TODO reduction on annotation classes?
	
	public NameCompressionExtension() {}

	/**
	 * @see com.ibm.ive.tools.japt.Extension#getName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {
				expandPermissions, 
				caseSensitiveClassNames, 
			//preserveSerialization, preserveExternalization, 
				reuseConstantPoolStrings,
				baseName, 
				packageBaseName, 
				noRenamePackages, 
				logFile,
				prepend};
	}

	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) throws ExtensionException {
		JaptFactory factory = repository.getFactory();
		if(factory.getNotLoadedClassCount() > 0 || factory.getNotFoundClassCount() > 0) {
			messages.MISSING_CLASSES.log(logger);
		}
		NameCompressor nameCompressor = new NameCompressor(repository, logger, messages, prepend.isFlagged());
		nameCompressor.setCaseSensitiveClassNames(caseSensitiveClassNames.isFlagged());
		nameCompressor.setRetainingPermissions(!expandPermissions.isFlagged());
		//nameCompressor.setPreserveSerialization(preserveSerialization.isFlagged());
		//nameCompressor.setPreserveExternalization(preserveExternalization.isFlagged());
		nameCompressor.setReuseConstantPoolStrings(reuseConstantPoolStrings.isFlagged());
		nameCompressor.setRenamePackages(!noRenamePackages.isFlagged());
		nameCompressor.setBaseName(baseName.getValue());
		nameCompressor.setPackageBaseName(packageBaseName.getValue());
		if(logFile.appears()) {
			nameCompressor.setLogFile(logFile.getValue());
		}
		//name compression preserves method relationships so we disable
		//the recalculation done by JIKESBT in BT_Method.resetName
		boolean wasBuilding = factory.buildMethodRelationships;
		factory.buildMethodRelationships = false;
		try {
			nameCompressor.compressNames();
		} catch(InvalidIdentifierException e) {
			factory.noteInvalidIdentifier(e.getIdentifier());
			throw new ExtensionException(this);
		} finally {
			factory.buildMethodRelationships = wasBuilding;
		}
	}


}
