package com.ibm.ive.tools.japt.execOpt.commandLine;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ErrorReporter;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.CommandLineData;
import com.ibm.ive.tools.japt.commandLine.CommandLineLogger;
import com.ibm.ive.tools.japt.commandLine.CommandLineParser;
import com.ibm.ive.tools.japt.commandLine.ExtensionDescriptor;
import com.ibm.ive.tools.japt.commandLine.StandardLogger;
import com.ibm.ive.tools.japt.execOpt.ExecOptimizer;
import com.ibm.ive.tools.japt.execOpt.MemToolEscapeAnalysisExtension;
import com.ibm.ive.tools.japt.execOpt.MemToolJarExtension;
import com.ibm.ive.tools.japt.execOpt.MemToolLoadExtension;
import com.ibm.ive.tools.japt.memoryAreaCheck.MemAreaExtension;


/**
 * @author sfoley
 * 
 * Included with this program is a simplified and incomplete subset of proxy classes 
 * for the RTSJ version 1.
 * 
 * This subset will be used by default to enable the error-checking of a given application.
 * However, if the user wishes to verify the use of the RTSJ itself, then the user must override
 * the loading of the proxy classes by putting on the class path a proper implementation of the RTSJ provided
 * with a java virtual machine.
 * 
 * Additionally, the user must also put on the class path the JRE itself in order to verify the usage
 * of JRE classes.
 * 
 * Otherwise, only the application classes will be checked for errors.
 * 
 *
 */

/**
 * The tool is used to:
 * -identify potential locations of the RTSJ runtime errors MemoryAccessError, IllegalAssignmentError and IllegalThreadStateException
 * -identify class loading/resolution/verification errors
 * -identify the classes which are:
 * 		-NHRT thread accessible
 * 		-RT thread accessible
 * 		-regular java thread accessible
 * 		-inaccessible (an inaccessible class is one loaded because it is reference by other classes that are loaded, but is never otherwise accessed by a runtime thread)
 * 		
 * 		Thread-accessibility information can be used to identify those classes that need to be NHRT-safe, those classes that should be preloaded or precompiled 
 * 		for real-time, and to identify all classes needed by application deployment.
 * -identify the classes needed for deployment, and also identify all unresolved references. 
 * 	Unresolved references are references to classes, methods and fields which are currently missing from the classes on the class path.
 * 	Unresolved reference implementation can assist with porting applications from one profile or version of java to another: SE (java 5, 6, real-time), ME (CLDC, MIDP, CDC, Foundation) and EE.
 * -identify all the entry points into the application, such as from the RTSJ class callbacks or methods in RTSJ classes overridden in application subclasses
 * -remove unneeded class file attributes (such as attributes needed only for debugging), or to add stackmaps to classes
 * 
 * Docs:
 * 
 * This tool is RTSJ and java implementation independent.  It is also java version independent.  
 * It does not contain an implementation of Java SE or the RTSJ, other than a minimal set of proxy classes 
 * for RTSJ version 1 which are used for RTSJ error analysis.  
 * If there is an implementation of RTSJ on the class path, then the class path implementation takes precedence over the
 * proxy classes contained within the tool.  An RTSJ implementation on the class path
 * must be version 1 for the analysis options to work.
 * 
 * 
 * Classes loaded are defined as either interal or external.  This designation is determined by the class path entry
 * that loads the class, which are also designated as internal or external on the command line.  Internal classes
 * are considered to be a part of the application or library, while external classes are considered to be part of the
 * supporting classes, such as classes in supporting libraries or classes included with the virtual machine.
 * 
 * Specifying "split", "internalEntryFile", or "errorFile" will cause a path analysis to occur.  The path analysis will
 * instantiate objects and follow methods to approximate the flow of execution with the code at runtime.
 * 
 * When running the tool against a full JSE real time VM, it may take a considerable amount
 * of memory and time, as it follows all the possible paths of execution within the entire
 * standard edition class library.  Therefore, the analysis is limited to internal classes unless "followExternal" is specified.
 * 
 * The "split" option splits classes as accessible to NHRT, RT and regular threads.
 * This can assist with:
 * -determining which classes need to be NHRT-safe
 * -determining which classes should be pre-compiled and/or preloaded.
 * -determining the complete list of classes needed by the application or library for deployment
 * 
 * The classes will be divided into 4 groups when written to disk:
 * -NHRT accessible
 * -RT accessible
 * -thread accessible
 * -inaccessible
 * Resources will be separated into a fifth group.
 * 
 * When writing classes to disk, the user may specify either a directory or a jar archive file.  When specifying a file archive, the
 * classes will be written to the archive.  If the "split" option is specified, the archive will be split into as many as four separate archives,
 * whose names have been lengthened by applying a prefix to the file name: ("nhrt", "rt", "t", and "inaccessible", for each of the categories
 * listed above).
 * 
 * When specifying a directory, the classes will be written to individual archives with names corresponding to the archives from which the 
 * classes originated.  If the "split" option is specified, each of these archives will be split into as many as four separate archives, as specified above.
 * 
 * The "entryPointFile" option provides a list of the various means by which the external classes make use of the internal classes.
 * This can be used to identify the various entry points into the application.  If "checkExternal" is not specified, the list will be limited
 * to the RTSJ entry points, because other external paths will not be followed.  The "checkExternal" option will allow
 * all entry points to be shown.
 * 
 * The "errorFile" option will cause errors found by the analysis to be reported within the specified file.
 * MemoryAccessError, IllegalAssignmentError and IllegalThreadStateException errors reported are potential errors, along
 * with any error that can be thrown as the result of the class loading, verification and resolution process.
 * This includes AbstractMethodError, ClassCircularityError, ClassFormatError, IllegalAccessError, IncompatibleClassChangeError,
 * InstantiationError, LinkageError, NoClassDefFoundError, NoSuchFieldError, NoSuchMethodError, UnsupportedClassVersionError,
 * and VerifyError.  At this time, the tool supports all know versions of java class files, and all attributes within as specified
 * by the Virtual Machine Specification version corresponding to Java version 6 and to numerous Java ME specifications.
 * 
 * The static analysis tool is unable to evaulate boolean conditions that are evaluated at runtime, and so it will
 * follow paths within methods that might not be followed at runtime, resulting in errors reported that might
 * not be reachable in runtime conditions.  
 * 
 * Additionally, the usage of JNI and reflection (java.lang.Class and java.lang.reflection) cannot be followed
 * by the analysis and may result in unreported errors.
 * 
 * The analysis will not follow paths into methods of external classes.  Therefore the analysis will miss errors that are the result
 * of callbacks from external code back into the internal classes.  To avoid missing such errors, either specify the option
 * "checkExternal", or simply add the corresponding classes to the internal class path.  
 * Internal class path entries have a higher precedence on the class path and so these
 * classes will be loaded as internal classes even if they can also be found on the external class path.  
 * Following all paths with "checkExternal" requires a lot of memory for the analysis of large applications or libraries.
 * 
 */
//Additional notes in RTSJContextProvider
/**
 * Classes loaded include all classes required at runtime:
 * -classes accessed by new, instanceof, checkcast, constant string, constant class, method invoke and field access instructions
 * -super classes and super interfaces
 * -classes loaded by enclosing methods
 * -classes loaded by annotations
 * -classes loaded by inner class and outer classes
 * -classes referenced by method signatures and field signatures 
 * -exception classes caught within methods
 * -exception classes in method declarations 
 * -the element class types of array classes
 * -classes referenced in the local variable tables of local variable types for debugging
 */
public class Main extends ExecOptimizer {
	/**
	 * Override this method to configure the opening logo message
	 */
	public static void main(String args[]) {
		Main main = new Main();
		main.execute(args);
	}
	
	public void execute(String args[]) {
		Logger logger  = new StandardLogger();
		CommandLineData data = new CommandLineData(false);
		ErrorReporter reporter = new ErrorReporter();
		MemAreaExtension mae = new MemAreaExtension(reporter);
		MemToolLoadExtension mtle = new MemToolLoadExtension(mae.checkExternal);
		MemToolEscapeAnalysisExtension mtae = new MemToolEscapeAnalysisExtension();
		MemToolJarExtension mtje = new MemToolJarExtension(mae.split, packageName);
		
		ExtensionDescriptor descs[] = new ExtensionDescriptor[] {
				new ExtensionDescriptor(mtle.getClass().getName(), mtle, data.messages),
				new ExtensionDescriptor(mae.getClass().getName(), mae, data.messages),
				new ExtensionDescriptor(mtae.getClass().getName(), mtae, data.messages),
				new ExtensionDescriptor(mtje.getClass().getName(), mtje, data.messages),
				
		};
		Logger fileLogger = null;
		
		try {
			//data.printLogo(logger);
			//logger.flush();
			readBasicOptions(descs, data, args, logger);
			if(data.version.isFlagged()) {
				data.printVersion(logger, fullProgramName, version + " " + build);
				logger.close();
				return;
			}			
			if(args.length == 0 || data.help.isFlagged()) {
				printUsage(data, logger, mtle, mae, mtje, mtae);
				logger.close();
				return;
			}
			
			parseCommandLine(descs, data, args, logger);
			logger.flush();
			if(data.log.appears()) {
				String log = data.log.getValue();
				if(log.indexOf('.') == -1) {
					log += ".log";
				}
				try {
					logger = new CommandLineLogger(log);
					data.messages.LOGGING_TO.log(logger, log);
				} catch(FileNotFoundException e) {
					logger = new CommandLineLogger();
					data.messages.COULD_NOT_OPEN_FILE.log(logger, log);
				}
			} else {
				logger = new CommandLineLogger();
			}
			RTFactory factory = new RTFactory(messages, logger, mae.checkExternal.isFlagged());
			factory.resolveRuntimeReferences = false;
			JaptRepository rep = new JaptRepository(factory);
			factory.errorReporter = reporter;
			if(mae.errorFile.appears()) {
				String fileName = mae.errorFile.getValue();
				try {
					fileLogger = new ErrorLogger(fileName);
					data.messages.CREATED_FILE.log(logger, fileName);
				} catch(IOException e) {
					data.messages.COULD_NOT_OPEN_FILE.log(logger, fileName);
					fileLogger = new StandardLogger();
				}
				reporter.setErrorLogger(fileLogger);
			}
			rep.resetClassLoading();
			run(rep, logger, descs);
			JaptMessage.logSummary(this, logger);
		} catch (CommandLineException e) {
			printUsage(data, logger, mtle, mae, mtje, mtae);
		} finally {
			if(fileLogger != null) {
				fileLogger.close();
			}
			logger.close();
		}
	}
	
	
	
	
	/**
	 * parses the command line arguments, if defaultExtensionName is specified, then
	 * such an extension will be created and will apply to the whole command line (it is equivalent to this
	 * extension appearing as the first item on the command line)
	 * @param data will hold the data present on the command line
	 * @param args the command line
	 * @param logger for logging
	 * @throws CommandLineException
	 */
	public void parseCommandLine(
			ExtensionDescriptor[] descs, CommandLineData data, String args[], Logger logger) throws CommandLineException {
		CommandLineParser parser = new CommandLineParser(data.messages, args, logger);
		parser.addInitialOptions(new Option[] {data.macro, data.log});
		for(int i=0; i<descs.length; i++) {
			descs[i].addExtensionOptions(parser);
		}
		parser.filterArgs();
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
	public void readBasicOptions(
			ExtensionDescriptor[] descs, CommandLineData data, String args[], Logger logger) throws CommandLineException {
		com.ibm.ive.tools.commandLine.CommandLineParser parser = 
			new com.ibm.ive.tools.commandLine.CommandLineParser(args);
		parser.addOption(data.help);
		parser.addOption(data.version);
		parser.parseCommandLine();
	}
	
	void printUsage(CommandLineData data, 
			Logger logger, 
			MemToolLoadExtension mtle, 
			MemAreaExtension mae, 
			MemToolJarExtension mtje, 
			MemToolEscapeAnalysisExtension mtae) {
		logger.logStatus(fullProgramName);
		logger.logStatus(Logger.endl);
		logger.logStatus("usage: java AppExecOptimizer [[-version|-help] | [basic options] [load options] [analysis options] [jar generation options]]");
		logger.logStatus(Logger.endl);
		//data.messages.SECTION_END.log(logger);
		
		printOption(data, logger, data.help);
		printOption(data, logger, data.version);
		data.messages.SECTION_END.log(logger);
		
		data.messages.OPTIONS_MESSAGE.log(logger, "basic");
//		data.messages.OPTION_MESSAGE.log(logger, data.help + data.getSpacer(data.help) + data.help.getDescription());
//		data.messages.OPTION_MESSAGE.log(logger, data.version + data.getSpacer(data.version) + data.version.getDescription());
//		data.messages.OPTION_MESSAGE.log(logger, data.log + data.getSpacer(data.log) + data.log.getDescription());
//		data.messages.OPTION_MESSAGE.log(logger, data.macro + data.getSpacer(data.macro) + data.macro.getDescription());
		printOption(data, logger, data.log);
		printOption(data, logger, data.macro);
		data.messages.OPTION_MESSAGE.log(logger, data.messages.OPTIONS_FILE_LABEL + data.getSpacer(data.messages.OPTIONS_FILE_LABEL) + data.messages.OPTIONS_FILE);
		data.messages.SECTION_END.log(logger);
		
	 	
		data.messages.OPTIONS_MESSAGE.log(logger, "load");
		printOption(data, logger, mtle.options.internalClassPathList);
		printOption(data, logger, mtle.options.internalClassPathAll);
		printOption(data, logger, mtle.options.externalClassPathList);
		printOption(data, logger, mtle.options.externalClassPathAll);
		printOption(data, logger, mtle.options.jreClassPath);
		printOption(data, logger, mtle.options.fileExtension);
		data.messages.SECTION_END.log(logger);
		printOption(data, logger, mtle.options.loadClass);
		printOption(data, logger, mtle.options.loadResource);
		data.messages.SECTION_END.log(logger);
		printOption(data, logger, mtle.options.load);
		printOption(data, logger, mtle.options.loadAll);
		data.messages.SECTION_END.log(logger);
		printOption(data, logger, mtle.options.unresolvedReferenceFile);
		data.messages.SECTION_END.log(logger);
		
//		printOption(data, logger, mtle.aggressiveLoading);
//		data.messages.SECTION_END.log(logger);
		logger.logStatus("options to load and mark points of entry:");
		logger.logStatus(Logger.endl);
		//data.messages.OPTIONS_MESSAGE.log(logger, "load and mark as points of entry");
		printOption(data, logger, mtle.options.includeClass);
		printOption(data, logger, mtle.options.includeWholeClass);
		printOption(data, logger, mtle.options.includeLibraryClass);
		printOption(data, logger, mtle.options.includeAccessibleClass);
		printOption(data, logger, mtle.options.includeField);
		printOption(data, logger, mtle.options.includeMethod);
		printOption(data, logger, mtle.options.includeMainMethod);
		printOption(data, logger, mtle.options.includeMethodEx);
		printOption(data, logger, mtle.options.includeExtendedLibraryClass);
		printOption(data, logger, mtle.options.includeExtendedAccessibleClass);
		
		data.messages.SECTION_END.log(logger);
		data.messages.OPTIONS_MESSAGE.log(logger, "path analysis");
		printOption(data, logger, mae.split);
		printOption(data, logger, mae.errorFile);
		printOption(data, logger, mae.checkExternal);
		printOption(data, logger, mae.followExternal);
		printOption(data, logger, mae.entryPointFile);
		data.messages.SECTION_END.log(logger);
		data.messages.OPTIONS_MESSAGE.log(logger, "escape analysis");
		//printOption(data, logger, mtae.reportOption);
		printOption(data, logger, mtae.escapeLogOption);
		printOption(data, logger, mtae.escapeMethods);
		printOption(data, logger, mtae.depthLimitOption);
		
		data.messages.SECTION_END.log(logger);
		
		
		data.messages.OPTIONS_MESSAGE.log(logger, "jar generation");
		printOption(data, logger, mtje.target);
		printOption(data, logger, mtje.removeDebugInfo);
		printOption(data, logger, mtje.removeInfoAttributes);
		printOption(data, logger, mtje.removeAnnotations);
		printOption(data, logger, mtje.removeAttribute);
		printOption(data, logger, mtje.removeStackMaps);
		printOption(data, logger, mtje.addStackMaps);
		printOption(data, logger, mtje.excludeResource);
		printOption(data, logger, mtje.excludeClass);
		printOption(data, logger, mtje.createAutoLoaders);
		//printOption(data, logger, mtje.preverifyForCLDC); TODO do we keep this?  yes, hidden
	}
	
	private static void printOption(CommandLineData data, Logger logger, Option option) {
		data.messages.OPTION_MESSAGE.log(logger, option + data.getSpacer(option) + option.getDescription());
	}
}
