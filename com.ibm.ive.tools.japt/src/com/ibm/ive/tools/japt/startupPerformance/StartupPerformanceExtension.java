package com.ibm.ive.tools.japt.startupPerformance;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_StackHistoryProvider;
import com.ibm.jikesbt.BT_StackPool;

/**
 * Design #484
 * A Japt extension to optimize classes for startup time by rewriting throws
 * and catches to prevent the Virtual Machine verifier from loading many
 * subclasses of java.lang.Throwable (Errors and Exceptions).
 * 
 * Design #895
 * In cases of type return and method arguments if the type on the stack
 * is actually a subclass of the type that is expected then we can insert
 * a checkcast to prevent the VM verfier from loading additional classes
 * to verify the chain of classes between the two types at startup.
 * 
 * @author Nicholas Doyle, Alex Kennberg, Sean Foley.
 */
public class StartupPerformanceExtension implements CommandLineExtension {
	final private Messages messages = new Messages(this);
	
	private FlagOption parseableOption = new FlagOption("parseable", "make output easily parseable"); {
		parseableOption.setVisible(false);
	}
	private FlagOption doCatchOptimizationOpt = new FlagOption(messages.OPTIMIZE_CATCHES_LABEL, messages.OPTIMIZE_CATCHES);
	private FlagOption doThrowOptimizationOpt = new FlagOption(messages.OPTIMIZE_THROWS_LABEL, messages.OPTIMIZE_THROWS);
	private FlagOption doUpcastOptimizationOpt = new FlagOption(messages.OPTIMIZE_UPCAST_LABEL, messages.OPTIMIZE_UPCASTS);
	
	public Option[] getOptions() {
		return new Option[] {
			doThrowOptimizationOpt,
			doCatchOptimizationOpt,
			doUpcastOptimizationOpt,
			parseableOption};
	}

	
	
	public void execute(JaptRepository repository, Logger logger) throws ExtensionException {
		if(!doThrowOptimizationOpt.isFlagged()
			&& !doCatchOptimizationOpt.isFlagged()
			&& !doUpcastOptimizationOpt.isFlagged()) {
				//TODO a warning
				return;
		}
		
		BT_ClassVector classes = repository.getInternalClasses();
		BT_StackPool pool = new BT_StackPool();
		BT_StackHistoryProvider provider = new BT_StackHistoryProvider(repository);
		
		int totalOptimizedThrows = 0;
		int totalOptimizedCatches = 0;
		int totalOptimizedSpans = 0;
		int totalOptimizedMethods = 0;
		int totalOptimizedReturns = 0;
		int totalOptimizedFieldAccesses = 0;
		int totalOptimizedMethodArgs = 0;
		int totalOptimizedMethodInvokes = 0;
		int totalOptimizedFieldArg = 0;
	
		// Process all internal classes
		for (int i = 0; i < classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if (!clazz.isClass()) {
				continue;
			}
			
			if (parseableOption.flagged) {
				messages.CLAZZ_SIMPLE.log(logger, clazz);
			}
			
			BT_MethodVector methods = clazz.getMethods();
			// Process all appropriate methods in the class
			for (int j = 0; j < methods.size(); j++) {
				BT_Method method = methods.elementAt(j);
				//TODO here we can match the method name to a set of methods from a command line option
				if (method.isAbstract() || method.isNative() || method.isStub()) {
					continue;
				}
				if (parseableOption.flagged) {
					messages.METHOD_SIMPLE.log(logger, method.internalName());
				}
				Method meth = new Method(
					method,
					provider,
					logger,
					messages,
					parseableOption.isFlagged());
				
				meth.doCatchOptimization = doCatchOptimizationOpt.isFlagged();
				meth.doThrowOptimization = doThrowOptimizationOpt.isFlagged();
				meth.doUpcastOptimization = doUpcastOptimizationOpt.isFlagged();
	
				if(meth.optimize(pool)) {
					totalOptimizedMethods++;
					totalOptimizedCatches += meth.optimizedCatches;
					totalOptimizedSpans += meth.optimizedSpans;
					totalOptimizedThrows += meth.optimizedThrows;
					totalOptimizedReturns += meth.optimizedReturns;
					totalOptimizedFieldAccesses += meth.optimizedFieldAccesses;
					totalOptimizedMethodArgs += meth.optimizedMethodArgs;
					totalOptimizedMethodInvokes += meth.optimizedMethodInvokes;
					totalOptimizedFieldArg += meth.optimizedFieldArg;
				}
			}
		}
		if (parseableOption.isFlagged()) {
			messages.DONE.log(logger);
		} else {
			messages.SUMMARY.log(logger, new Object[] {
					Integer.toString(totalOptimizedMethods),
					Integer.toString(totalOptimizedCatches),
					Integer.toString(totalOptimizedSpans),
					Integer.toString(totalOptimizedThrows),
					Integer.toString(totalOptimizedReturns),
					Integer.toString(totalOptimizedFieldArg),
					Integer.toString(totalOptimizedFieldAccesses),
					Integer.toString(totalOptimizedMethodArgs),
					Integer.toString(totalOptimizedMethodInvokes),
			});
		}
	}
	
	public String getName() {
		return messages.DESCRIPTION;
	}

}