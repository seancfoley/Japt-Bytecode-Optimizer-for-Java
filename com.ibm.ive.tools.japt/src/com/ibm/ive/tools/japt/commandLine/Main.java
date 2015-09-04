package com.ibm.ive.tools.japt.commandLine;

import java.io.FileNotFoundException;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.Japt;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Messages;
import com.ibm.ive.tools.japt.load.LoadExtension;


/**
 * @author sfoley
 * 
 * The command line version of Japt
 *
 */
public class Main implements Component {
	public String programName = "japt";
	public String version = "1.3"; //coincides with 2.5 builds
	//public String version = "1.2"; //coincides with 2.4 builds
	//public String version = "1.1"; //coincides with 2.3 builds
	public Messages messages = new Messages(this);
	
	public Main() {}
	
	/**
	 * Override this method to configure the opening logo message
	 */
	
	public static void main(String args[]) {
		Main main = new Main();
		main.execute(args, true);
	}
	
	public void execute(String args[], boolean enableExtensionShortcuts) {
		Logger logger  = new StandardLogger();
		CommandLineData data = new CommandLineData(enableExtensionShortcuts);
		try {
			data.printLogo(logger);
			logger.flush();
			if(readBasicOptions(data, args, logger)) {
				logger.close();
				return;
			}
			parseCommandLine(data, args, logger);
			logger.flush();
			if(data.log.appears()) {
				String log = data.log.getValue();
				if(log.indexOf('.') == -1) {
					log += ".log";
				}
				try {
					logger = data.noVerbose.appears() ? new QuietCommandLineLogger(log) : new CommandLineLogger(log);
					data.messages.LOGGING_TO.log(logger, log);
				}
				catch(FileNotFoundException e) {
					logger = data.noVerbose.appears() ? new QuietCommandLineLogger() : new CommandLineLogger();
					data.messages.COULD_NOT_OPEN_FILE.log(logger, log);
				}
			}
			else {
				logger = data.noVerbose.appears() ? new QuietCommandLineLogger() : new CommandLineLogger();
			}
			JaptFactory factory = new JaptFactory(messages, logger);
			JaptFactory.resolveRuntimeReferences = !data.noResolveRuntime.isFlagged();
			JaptRepository rep = new JaptRepository(factory);
			rep.resetClassLoading();
			run(rep, logger, data.extensions.getExtensions());
			//Note: I think that it's best that the classpath be one thing that persists from one extension to the next,
			//much like the classes themselves persist from one extension to the next
			rep.resetClassLoading(); //closes open zips and jars
			JaptMessage.logSummary(this, logger);
		}
		catch (CommandLineException e) {
			data.printUsage(logger, true, data.extensions.getExtensions());
		}
		logger.close();
	}
	
	
	/**
	 * collects the data required to start the Japt application.  This default
	 * implementation collects the data from the command line.  One the data
	 * is collected the runJapt method is called.
	 */
	public void run(JaptRepository rep, Logger logger, ExtensionDescriptor extensions[]) {
		long startTime = System.currentTimeMillis();
		Japt japt = new Japt(rep);
		japt.executeExtensions(extensions, logger);
		japt.logCompleted(logger, System.currentTimeMillis() - startTime);
	}
	
	/**
	 * parses the command line arguments, with the japt load extension as the default extension
	 * @param data will hold the data present on the command line
	 * @param args the command line
	 * @param logger for logging
	 * @return whether the command line is sufficient for execution
	 */
	public void parseCommandLine(CommandLineData data, String args[], Logger logger) throws CommandLineException {
		parseCommandLine(data, args, logger, data.incrementalLoad.isFlagged() ? null : LoadExtension.class.getName());
	}
	
	/**
	 * parses the command line arguments, if defaultExtensionName is specified, then
	 * such an extension will be created and will apply to the whole command line (it is equivalent to this
	 * extension appearing as the first item on the command line)
	 * @param data will hold the data present on the command line
	 * @param args the command line
	 * @param logger for logging
	 * @param defaultExtensionName
	 * @throws CommandLineException
	 */
	public void parseCommandLine(CommandLineData data, String args[], Logger logger, String loadExtensionName) throws CommandLineException {
		CommandLineParser parser = new CommandLineParser(data.messages, args, logger);
		parser.addInitialOptions(data.getBasicOptions());
		parser.addInitialOptions(data.getExtensionOptions());
		data.extensions.setCreateExtensions(true);
		if(loadExtensionName != null) {
			//we simulate the default extension appearing as the first item on the command line
			ExtensionDescriptor extDescriptor = data.extensions.handleExtension(loadExtensionName, parser);
			extDescriptor.setName(getName());
		}
		parser.filterArgs();
		//we need to add incrementalLoad just to avoid a warning if it is there 
		//(even though we have already checked for this option)
		parser.addOption(data.incrementalLoad);
		parser.parseCommandLine();
	}
	
	/**
	 * 
	 * @param data the command line data object
	 * @param args the command line arguments
	 * @param logger the logger used for logging info
	 * @return if the program should terminate immediately
	 * @throws CommandLineException
	 */
	public boolean readBasicOptions(CommandLineData data, String args[], Logger logger) throws CommandLineException {
		boolean result = false;
		com.ibm.ive.tools.commandLine.CommandLineParser parser = 
			new com.ibm.ive.tools.commandLine.CommandLineParser(args);
		parser.addOption(data.help);
		parser.addOption(data.version);
		parser.addOption(data.systemProperties);
		parser.addOption(data.incrementalLoad);
		parser.addOption(data.echo);
		parser.addOptions(data.getExtensionOptions());
		data.extensions.setCreateExtensions(false);
		parser.parseCommandLine();
		if(data.echo.isFlagged()) {
			data.printArgs(logger, programName, args);
			result = true;
		}
		if(data.version.isFlagged()) {
			data.printVersion(logger, programName, version);
			result = true;
		}
		if(data.systemProperties.isFlagged()) {
			data.printSystemProperties(logger);
			result = true;
		}
		if(args.length == 0 || data.help.isFlagged()) {
			data.printUsage(logger, !data.incrementalLoad.isFlagged(), data.extensions.getExtensions());
			result = true;
		}
		data.extensions.reset();
		return result;
	}
	
	public String getName() {
		return messages.DESCRIPTOR;
	}
	

}
