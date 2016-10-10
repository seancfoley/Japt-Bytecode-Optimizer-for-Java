package com.ibm.ive.tools.japt.test;
import com.ibm.ive.tools.commandLine.CommandLineException;
import com.ibm.ive.tools.japt.ArchiveExtensionList;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Messages;
import com.ibm.ive.tools.japt.commandLine.CommandLineData;
import com.ibm.ive.tools.japt.commandLine.Main;
import com.ibm.ive.tools.japt.load.LoadExtension;
import com.ibm.ive.tools.japt.out.ClassGenerationExtension;
import com.ibm.ive.tools.japt.out.GenerationExtension;
import com.ibm.ive.tools.japt.out.JarGenerationExtension;
import com.ibm.ive.tools.japt.test.ConfigDocumentHandler.JRE;
import com.ibm.jikesbt.StringVector;

/*
 * Created on Jun 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

/**
 * @author sfoley
 *
 * Each test run consists of a java application, a command line to run the java application, 
 * and a transformation for japt to use.  <p>
 * The XML file is read to generate an array of TestRun objects comprising these details, 
 * as well as a few other configurable items.
 */
public class TestRun {
	
	final AppRun appRun;
	final String japtApplicationArgs[]; 
	final String japtLogPath;
	final String japtedDir;
	final String japtedJar;
	final JRE jres[];
	final String japtTransformationArgs[]; 
	boolean verifyTransformation; /* unused */
	boolean optimizeTransformation; /* unused */
	
	TestRun(AppRun appRun,
			JRE jres[],
			String japtApplicationArgs[],
			String japtTransformationArgs[],
			String japtLogPath,
			String japtedJar,
			String japtedDir) {
		this.appRun = appRun; 
		this.japtApplicationArgs = japtApplicationArgs; 
		this.japtLogPath = japtLogPath;
		this.japtedJar = japtedJar;
		this.japtedDir = japtedDir;
		this.jres = jres; 
		this.japtTransformationArgs = japtTransformationArgs; 
	}
	
	JaptTrial getTrialArgs(JRE jre, String appClassPath, String target, String transformArgs[]) {
		boolean toArchive = ArchiveExtensionList.isStandardArchive(target);
		StringVector paths = JaptRepository.pathTokenizer(appClassPath, false);
		
		StringVector arguments = new StringVector(japtApplicationArgs.length + transformArgs.length + 10 + (2 * paths.size()));
		CommandLineData data = new CommandLineData();
		
		arguments.addElements(japtApplicationArgs);
		
		LoadExtension loadExtension = new LoadExtension();
		
		if(jre.classPath != null && jre.classPath.length() > 0) {
			arguments.addElement(loadExtension.options.externalClassPathList.getAppearance());
			arguments.addElement(jre.classPath);
		}
		if(jre.jre != null && jre.jre.length() > 0) {
			arguments.addElement(loadExtension.options.jreClassPath.getAppearance());
			arguments.addElement(jre.jre);
		}
		
		for(int i=0; i<paths.size(); i++) {
			arguments.addElement(loadExtension.options.internalClassPathList.getAppearance());
			arguments.addElement(paths.elementAt(i));
		}
		
		arguments.addElements(transformArgs);
		
		GenerationExtension ext;
		if(toArchive) {
			JarGenerationExtension jarExtension = new JarGenerationExtension();
			arguments.addElement(data.jarOutput.getAppearance());
			arguments.addElement(jarExtension.target.getAppearance());
			arguments.addElement(target);
			ext = jarExtension;
			
		} else {
			ClassGenerationExtension classExtension = new ClassGenerationExtension();
			arguments.addElement(data.dirOutput.getAppearance());
			arguments.addElement(classExtension.dirTarget.getAppearance());
			arguments.addElement(target);
			ext = classExtension;
		}
		arguments.addElement(ext.noStripDebugInfo.getAppearance());
		arguments.addElement(ext.noStripAnnotations.getAppearance());
		arguments.addElement(ext.noStripInfoAttributes.getAppearance());
		if(jre.classVersion != null) {
			arguments.addElement(ext.newVersion.getAppearance());//TODO try out later java 7 and 8 versions, currently using version 50
			arguments.addElement(jre.classVersion); 
		}
		
		JaptTrial trial = new JaptTrial();
		trial.japtArgs = arguments.toArray();
		trial.appArgs = jre.java + " -Xbootclasspath/p:" + jre.classPath + " -cp \"" + target + "\" " + appRun.commandLine;
		 
		return trial;
	}
	
	class JaptTrial {
		String japtArgs[]; //the args for japt to create a new app
		String appArgs; //the args for this new app to run
		
		String printJaptArgs() {
			if(japtArgs.length > 0) {
				StringBuffer buffer = new StringBuffer();
				for(int i=0; i<japtArgs.length; i++) {
					buffer.append(" \"");
					buffer.append(japtArgs[i]);
					buffer.append("\"");
				}
				return buffer.toString();
				
			} else {
				return "";
			}
		}
		
		public String toString() {
			return printJaptArgs();
		}
	}
	
	AppInvocation getFirstApp() {
		return appRun.getAppInvocation(jres[0]);
	}
	
	AppInvocation getSecondApp() {
		return appRun.getAppInvocation(jres[1]);
	}
	
	String runFirstJapt(TestLogger logger) throws CommandLineException {
		JaptTrial trial = getTrialArgs(jres[0], appRun.appClassPath, japtedJar, japtTransformationArgs);
		Main japtMain = new Main();
		JaptFactory factory = new JaptFactory(japtMain.messages, logger);
		Messages messages = japtMain.messages;
		
		//this is disabled because some japt tests have code irregularities, code that is illegal according to the spec,
		//but not caught by the verifier and the code runs successfully despite the illegal bytecodes
		messages.CODE_IRREGULARITY.setEnabled(false);
		
		//this is disabled because some japt test apps have some excessive max stack or max locals values in their classes
		messages.EXCESSIVE_VALUE.setEnabled(false);
		
		//If classes were compiled for java6 but the VM is not, then we can ignore the missing StringBuilder class
		//For the second run, we ignore any missing classes.
		factory.ignoreClassNotFound("java.lang.StringBuilder");
		
		//the JRE I'm using has references to the realtime classes but those classes are nowhere to be found!
		factory.ignoreClassNotFound("javax.realtime.ImmortalMemory");
		factory.ignoreClassNotFound("javax.realtime.MemoryArea");
		factory.ignoreClassNotFound("javax.realtime.RealtimeThread");
		factory.ignoreClassNotFound("javax.realtime.HeapMemory");
		factory.ignoreClassNotFound("javax.realtime.ScopedMemory");
		
		String result = runJapt(factory, logger, trial, japtMain);
		return result;
	}
	
	String runSecondJapt(TestLogger logger) throws CommandLineException {
		logger.onReload = true;
		//String secondJar = japtedDir + "\\" + new File(japtedJar).getName();
		//JaptTrial trial = getJaptArgs(japtedJar, secondJar, japtTransformationArgs);
		JaptTrial trial = getTrialArgs(jres[1], japtedJar, japtedDir, japtTransformationArgs);
		Main japtMain = new Main();
		Messages messages = japtMain.messages;
		
		//disabled for the same reasons as in the first run
		messages.EXCESSIVE_VALUE.setEnabled(false);
		messages.CODE_IRREGULARITY.setEnabled(false);
		
		//these are disabled because reduction will remove items that are not used,
		//although there may remain references to these items in the code, references that are 
		//unreachable when the application runs
		messages.UNRESOLVED_METHOD.setEnabled(false);
		messages.UNRESOLVED_FIELD.setEnabled(false);
		messages.COULD_NOT_FIND_CLASS.setEnabled(false);
		messages.NO_MATCH_FROM.setEnabled(false);
		messages.NO_MATCH.setEnabled(false);
		
		JaptFactory factory = new JaptFactory(japtMain.messages, logger);
		factory.setIgnoreClassesNotFound(true);
		return runJapt(factory, logger, trial, japtMain);
	}
	

	/**
	 * @param logger
	 * @param trial
	 * @return
	 * @throws CommandLineException
	 */
	private String runJapt(JaptFactory factory, Logger logger, JaptTrial trial, Main japtMain) throws CommandLineException {
		CommandLineData data = new CommandLineData();
		japtMain.parseCommandLine(data, trial.japtArgs, logger);
		factory.resolveRuntimeReferences = !data.noResolveRuntime.isFlagged();
		JaptRepository rep = new JaptRepository(factory);
		rep.resetClassLoading();
		japtMain.run(rep, logger, data.extensions.getExtensions());
		rep.resetClassLoading(); //closes open zips and jars and kills loading threads
		return trial.appArgs;
	}
	
	boolean cleanUp() {
		//File logFile = new File(japtLogPath);
		//File japtJar = new File(japtedJar);
		//boolean logDeleted = logFile.delete();
		//boolean jarDeleted = japtJar.delete();
		//TODO clean up second jar file/classes
		//return logDeleted && jarDeleted;
		return false;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer("Testing \"");
		String cmd;
		int cmdLength = 40;
		if(appRun.commandLine.length() > cmdLength) {
			cmd = appRun.commandLine.substring(0, cmdLength - 3) + "...";
		}
		else {
			cmd = appRun.commandLine;
		}
		buffer.append(cmd);
		buffer.append("\" on \"");
		buffer.append(appRun.appClassPath);
		buffer.append("\" with \"");
		//buffer.append("\" with transformations \"");
		for(int i=0; i<japtTransformationArgs.length; i++) {
			if(i > 0) {
				buffer.append(' ');
			}
			buffer.append(japtTransformationArgs[i]);
		}
		buffer.append("\"");
		return buffer.toString();
	}
}
