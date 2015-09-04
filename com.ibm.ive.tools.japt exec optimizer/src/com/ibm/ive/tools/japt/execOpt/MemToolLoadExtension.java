package com.ibm.ive.tools.japt.execOpt;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ErrorReporter;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.IntegratedExtension;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.TransferredClassPathEntry;
import com.ibm.ive.tools.japt.execOpt.commandLine.RTFactory;
import com.ibm.ive.tools.japt.load.LoadExtension;
import com.ibm.ive.tools.japt.load.RefReport;
import com.ibm.ive.tools.japt.load.RepositoryLoader;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Factory;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Repository;
import com.ibm.jikesbt.BT_StackShapeVisitor;
import com.ibm.jikesbt.BT_StackShapes;

public class MemToolLoadExtension extends LoadExtension implements IntegratedExtension {
	
	final public FlagOption aggressiveLoading = new FlagOption("aggressiveLoading", "load all referenced classes");
	final public FlagOption checkExternal;
	
	public MemToolLoadExtension(FlagOption checkExternal) {
		this.checkExternal = checkExternal;
		options.unresolvedReferenceFile.setVisible(true);
		options.noBuiltInRules.setFlagged(true);
		
		options.internalClassPathList.setDescription("append named jar/dir to internal class path");
		options.internalClassPathAll.setDescription("append jars in named dir to internal cls path");
		options.externalClassPathList.setDescription("append named jar/dir to external class path");
		options.externalClassPathAll.setDescription("append jars in named dir to external cls path");
		options.jreClassPath.setDescription("append named jdk/jre to external class path");
		
		options.load.setName("load");
		options.load.setDescription("load named jar/dir/class/resource on internal cp");
		options.loadAll.setDescription("load jars in named dir on internal class path");
		
		options.loadClass.setDescription("load named class(es) from class path");
		options.loadResource.setDescription("load named resource(s) from class path");
		
		options.includeClass.setName("entryClass");
		options.includeClass.setDescription("class(es)");
		options.includeWholeClass.setName("entryWholeClass");
		options.includeWholeClass.setDescription("class(es) and members within");
		options.includeLibraryClass.setName("entryLibraryClass");
		options.includeLibraryClass.setDescription("class(es) and non-private members within");
		options.includeAccessibleClass.setName("entryAccessibleClass");
		options.includeAccessibleClass.setDescription("class(es) and public/protected members within");
		options.includeField.setName("entryField");
		options.includeField.setDescription("field(s) and their declaring class(es)");
		options.includeMethod.setName("entryMethod");
		options.includeMethod.setDescription("method(s) and their declaring class(es)");
		options.includeMainMethod.setName("mainMethod");
		options.includeMainMethod.setDescription("class(es) and main methods within");
		options.includeMethodEx.setName("entryMethodEx");
		options.includeMethodEx.setDescription("method(s) independent of declaring class(es)");
		options.includeExtendedLibraryClass.setName("entryExtLibraryClass");
		options.includeExtendedLibraryClass.setDescription("class(es), non-prvt members and overriding meths");
		options.includeExtendedAccessibleClass.setName("entryExtAccClass");
		options.includeExtendedAccessibleClass.setDescription("class(es), pub/prot members and overriding meths");
		
		options.unresolvedReferenceFile.setName("unresolvedRefFile");
		options.unresolvedReferenceFile.setDescription("create named file listing unresolved references");
		
		
	}
	
	public Option[] getOptions() {
		return new Option[] {
				
			options.internalClassPathList,
			options.internalClassPathAll,
			options.externalClassPathList,
			options.externalClassPathAll,
			options.jreClassPath,
			options.fileExtension,
			options.loadClass,
			options.loadResource,
			options.load,
			options.loadAll,
			options.includeClass,
			options.includeWholeClass,
			options.includeLibraryClass,
			options.includeAccessibleClass,
			options.includeMainMethod,
			options.includeField,
			options.includeMethod,
			options.includeMethodEx,
			
			options.includeExtendedLibraryClass,
			options.includeExtendedAccessibleClass,
			//options.includeSubclass,
			//options.includeResource,
//			options.includeSerializable,
//			options.includeSerialized,
//			options.includeExternalized,
//			options.includeDynamicClassLoad,
			//options.mainClass,
			//options.addStackMaps, this is in jar generation now
//			options.verify,
//			options.optimize,
//			options.reflectionWarnings,
//			options.builtInRules, 
//			options.noBuiltInRules,
			//options.refTree,
			//options.noFollow,
			//options.resetClassPath,
			options.unresolvedReferenceFile,
//			options.noDebug,
//			options.noTrackClasses,
//			options.noReadStackMaps
			
			//agressiveLoading,
		};
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		RTFactory factory = (RTFactory) repository.getFactory();
		BT_Factory.strictVerification = true;
		if(options.unresolvedReferenceFile.appears()) {
			factory.saveUnresolved = true;
		}	
		
		int internalCount = factory.getInternalClassCount();
		int externalCount = factory.getExternalClassCount();
		int notLoadedCount = factory.getNotLoadedClassCount();
		int notFoundCount = factory.getNotFoundClassCount();
			
		//RefReport ref = createRefReport(logger, factory);
		RepositoryLoader loader = createRepositoryLoader(new RefReport(), repository, logger);
		if(options.resetClassPath.isFlagged()) {
			repository.resetClassLoading();
		}
		loader.setClassPath();
		
		//This class path entry allows us to load the proxy RTSJ classes
		try {
			TransferredClassPathEntry cp = new TransferredClassPathEntry(
				new PatternString[] {
				new PatternString(TransferredClassPathEntry.fileNameForClassName("javax.realtime." + PatternString.wildCard)),
				new PatternString(TransferredClassPathEntry.fileNameForClassName(BT_Repository.JAVA_LANG_RUNNABLE)),
				new PatternString(TransferredClassPathEntry.fileNameForClassName(BT_Repository.JAVA_LANG_THREAD)),
				new PatternString(TransferredClassPathEntry.fileNameForClassName(BT_Repository.JAVA_LANG + '.' + PatternString.separatorExcludingWildCard + "Error")),
				new PatternString(TransferredClassPathEntry.fileNameForClassName(BT_Repository.JAVA_LANG + '.' + PatternString.separatorExcludingWildCard + "Exception"))
			},
			"proxy");
			cp.setName("proxy classes");
			repository.appendExternalClassPathEntry(cp);
		} catch(IOException e) {
			throw new ExtensionException(this, e.toString());
		}
		aggressiveLoading.setFlagged(true);
		loader.load(true, aggressiveLoading.isFlagged());
		if(factory.errorReporter != null) {
			verify(loader, repository.getInternalClasses(), factory.errorReporter);
		}
		repository.trimToSize();
		messages.COMPLETED_LOADING.log(logger, new String[] {
				Integer.toString(factory.getInternalClassCount() - internalCount), 
				Integer.toString(factory.getExternalClassCount() - externalCount), 
				Integer.toString(factory.getNotLoadedClassCount() - notLoadedCount),
				Integer.toString(factory.getNotFoundClassCount() - notFoundCount)
		});
		repository.resetClassLoading();
		if(options.unresolvedReferenceFile.appears()) {
			String file = options.unresolvedReferenceFile.getValue();
			if(file.indexOf('.') == -1) {
				file += ".txt";
			}
			try {
				PrintStream stream = new PrintStream(
								new BufferedOutputStream(
										new FileOutputStream(file)));
				messages.CREATING_UNRESOLVED_REF.log(logger, file);
				writeUnresolved(repository, stream);
				stream.close();
			} catch(FileNotFoundException e) {
				messages.NO_UNRESOLVED_REF.log(logger, file);
			}
			factory.trimUnresolved();
		}
	}
	
	/**
	 * Check the given classes for class and method verification errors.
	 * This does not check for errors that will be found when each class is written to a class file.
	 */
	public void verify(RepositoryLoader loader, BT_ClassVector classes, ErrorReporter listener) {
		for (int i = 0; i < classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if (clazz.throwsAnyError()) {
				continue;
			} 
			BT_MethodVector methods = clazz.getMethods();
			for (int j = 0; j < methods.size(); j++) {
				BT_Method method = methods.elementAt(j);
				if (method.throwsVerifyError()) {
					continue;
				}
				BT_CodeAttribute code = method.getCode();
				if (code == null) {
					continue;
				}
				verifyCode(loader, code, listener);
			} 
		}
	}
	
	
	/**
	 * Performs verification that is not performed by the JikesBT loading process or the JikesBT write process
	 */
	public void verifyCode(RepositoryLoader loader , BT_CodeAttribute code, ErrorReporter listener) {
		try {
			BT_StackShapeVisitor visitor = new BT_StackShapeVisitor(code);
			visitor.ignoreUpcasts(false);
			visitor.useMergeCandidates(false); //no need to use merge candidates for verification, in fact it should not be used
			visitor.setAbsoluteMaxStacks(code.getMaxLocals(), code.getMaxStack());
			BT_StackShapes shapes = visitor.populate();
			
			/* 
			 * compare the true stack depth with the stack depth stored by code.stackInfo (and so the same for the locals)
			 * which may have been read from the class file.  
			 */
			loader.checkExcessiveMax(shapes.maxDepth, code.getMaxStack(), "maxstack", code);
			loader.checkExcessiveMax(shapes.maxLocals, code.getMaxLocals(), "maxlocals", code);
			
			shapes.verifyStacks();
			
			code.verifyRelationships(true);
		} 
		
		/*
		 * the following BT_CodeException subclasses are caught here:
		 * 
		 * thrown by BT_CodeAttribute.visitReachableCode:
		 * BT_CodePathException
		 * BT_CircularJSRException
		 * 
		 * thrown by verifyStacks:
		 * BT_StackUnderflowException
		 * BT_InvalidStackTypeException
		 * BT_AccessException for protected access in objects
		 * 
		 * thrown by BT_StackShapeVisitor:
		 * BT_InconsistentStackDepthException
		 * BT_InconsistentStackTypeException
		 * BT_StackUnderflowException
		 * BT_InvalidStackTypeException
		 * BT_UninitializedLocalException
		 * BT_InvalidLoadException
		 * BT_InvalidStoreException
		 * BT_StackOverflowException,
		 * BT_LocalsOverflowException
		 * BT_MissingConstructorException
		 * 
		 * thrown by BT_CodeAttribute.verifyRelationShips:
		 * BT_InvalidReturnException
		 * BT_IllegalClinitException
		 * BT_IllegalInitException
		 * strict:
		 * BT_AbstractInstantiationException
		 * BT_IncompatibleMethodException
		 * BT_AbstractMethodException
		 * BT_IncompatibleFieldException
		 * BT_IncompatibleClassException
		 * BT_AccessException
		 * 
		 */
		catch(BT_CodeException e) {
			listener.noteError(loader.repository, checkExternal.isFlagged(), null, code.getMethod(), code.getMethod().useName(), e.getMessage(), e.getCorrespondingRuntimeError());
		} 
	}
	
	public void noteExecuting(Logger logger) {
		logger.logProgress(JaptMessage.formatStart(this).toString());
		logger.logProgress("Loading...");
		logger.logProgress(Logger.endl);
	}
	
	public void noteExecuted(Logger logger, String timeString) {
		logger.logProgress(JaptMessage.formatStart(this).toString());
		logger.logProgress("Completed loading in ");
		logger.logProgress(timeString);
		logger.logProgress(Logger.endl);
	}
}
