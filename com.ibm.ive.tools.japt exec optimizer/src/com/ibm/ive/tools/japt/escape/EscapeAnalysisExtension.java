package com.ibm.ive.tools.japt.escape;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.MemberActor;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.ive.tools.japt.reduction.ita.PropagationException;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

public class EscapeAnalysisExtension implements CommandLineExtension {
	protected FlagOption noUseIntraOption = new FlagOption("noIntraprocedural");
	//the report is produced after all methods have been analyzed and contains summarized data linking the individual analyses
	public ValueOption reportOption = new ValueOption("escapeSummary", "create named file summarizing object escape"); //Note: What used to be called escapeReport is now called escapeSummary
	
	public ValueOption depthLimitOption = new ValueOption("escapeDepth", "constrain escape analysis to given call depth");
	public SpecifierOption escapeMethods = new SpecifierOption("escapeMethod", "constrain escape analysis to named method(s)");
	
	
	//the log is produced while each method is analyzed, and the data is specific to each analysis
	public ValueOption escapeLogOption = new ValueOption("escapeReport", "create named file listing objects not escaping"); //Note: What used to be called escapeLog is now called escapeReport
	
	Messages messages = new Messages(this);
	protected FlagOption fullAnalysis = new FlagOption("fullAnalysis"); {
		fullAnalysis.setFlagged(true);
	}
	
	protected String name = "escape";//TODO intra add to messagesas DESCRIPTION
	
	
	public Option[] getOptions() {
		return new Option[] {reportOption, depthLimitOption, noUseIntraOption, escapeLogOption, fullAnalysis, escapeMethods};
	}

	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		int depthLimitValue;
		if(depthLimitOption.appears()) {
			try {
				depthLimitValue = Integer.parseInt(depthLimitOption.getValue());
			} catch(NumberFormatException e) {
				//TODO intra a message;messages.VERSION_ERROR.log(logger, classVersionOption.getValue());
				depthLimitValue = -1;
			}
		} else {
			depthLimitValue = -1;
		}
		RelatedMethodMap map = repository.getRelatedMethodMap();
		map.construct();
		PrintWriter logWriter = null;
		try {
			if(escapeLogOption.appears()) {
				logWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(escapeLogOption.getValue())));
			} 
			UnescapedObjectSummary total = fullAnalysis.isFlagged() ? (UnescapedObjectSummary) new FullSummary() : new BasicSummary();
			BT_ClassVector classes = repository.classes;
			boolean firstTimeThrough = true;
			int count = 0;
			total.exceedCount = 0;
			
//			System.out.println("using " + depthLimitValue + " for " + escapeLogOption.getValue());
//			System.out.flush();
//			try {Thread.sleep((long) 5000);} catch(InterruptedException e) {}
			
			Specifier methodsSpec[] = escapeMethods.getSpecifiers();
			BT_MethodVector methodsToAnalyze = new BT_HashedMethodVector();
			if(methodsSpec.length > 0) {
				MemberActor actor = new MemberCollectorActor(methodsToAnalyze, null);
				repository.findMethods(methodsSpec, actor);
			}
			
			while(true) {
				int counter = 0;
				for(int i=0; i<classes.size(); i++) {
					BT_Class clz = classes.elementAt(i);
					if(!repository.isInternalClass(clz)) {
						continue;
					}
					if(clz.isInterface()) {
						total.totalInterfaces++;
						continue;
					}
					total.totalClasses++;
					BT_MethodVector methods = clz.getMethods();
					for(int j=0; j<methods.size(); j++) {
						BT_Method meth = methods.elementAt(j);
						if(meth.isAbstract() || meth.isNative() || meth.isStub()) {
							continue;
						}
						
						if(escapeMethods.appears()) {
							if(!methodsToAnalyze.contains(meth)) {
								continue;
							}
						}
						if(firstTimeThrough) {/* the first time through we are just counting how many we will be doing the second time through */
							count++;
							continue;
						}
						EscapeAnalyzer analyzer = new EscapeAnalyzer(meth, repository, messages, logger, this, fullAnalysis.isFlagged());
						
						try {
							logger.logProgress(JaptMessage.formatStart(this).toString());
							logger.logProgress("Escape analysis " + ++counter + " of " + count + ": " + meth); //TODO intra a message to the logger
							logger.logProgress(Logger.endl);
							UnescapedObjectSet set = analyzer.analyze(!noUseIntraOption.isFlagged(), depthLimitValue);
							total.add(set);
							if(logWriter != null) {
								//TODO xxx;  here is where we update the Eclipse GUI xxx;
								set.write(logWriter);
								logWriter.println();
							}
						} catch(PropagationException e) {
							logger.logProgress(JaptMessage.formatStart(this).toString());
							logger.logProgress(e.toString()); //TODO intra a message
							logger.logProgress(Logger.endl);
							total.exceedCount++;
						} catch(OutOfMemoryError e) {
							analyzer = null;
							System.gc();
							logger.logProgress(JaptMessage.formatStart(this).toString());
							logger.logProgress(e.toString()); //TODO intra a message
							logger.logProgress(Logger.endl);
							total.exceedCount++;
						}
					}
				}
				if(firstTimeThrough) {
					firstTimeThrough = false;
				} else {
					break;
				}
			}
			if(total.exceedCount > 0) {
				logger.logProgress(JaptMessage.formatStart(this).toString());
				logger.logProgress(total.exceedCount + " method propagations exceeded propagation limits");
				logger.logProgress(Logger.endl);
			}
			if(reportOption.appears()) {
				PrintWriter reportWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(reportOption.getValue())));
				total.write(reportWriter);
				reportWriter.close();
			} else if(logWriter != null) {
				total.write(logWriter);
			} else {
				total.write(new PrintWriter(System.out));
			}
		} catch(IOException e) {
			//TODO intra add a message
			logger.logProgress(JaptMessage.formatStart(this).toString());
			logger.logProgress(e.toString());
			logger.logProgress(Logger.endl);
		} finally {
			if(logWriter != null) {
				logWriter.close();
			}
		}
		
	}
	
	public String getName() {
		return name;
	}
}
