package com.ibm.ive.tools.japt.inline;


import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.MemberActor;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_HashedMethodCallSiteVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodVector;



/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class InlineExtension implements com.ibm.ive.tools.japt.commandLine.CommandLineExtension {
	final private Messages messages = new Messages(this);

	final private String name = messages.DESCRIPTION;
	
	final public SpecifierOption inlineMethod = new SpecifierOption(messages.INLINE_METHOD_LABEL, messages.INLINE_METHOD);
	
	//methods that are not inlined, even if they were specified by "inline" ie "noinline" takes precedence
	final public SpecifierOption noInlineMethod = new SpecifierOption(messages.NO_INLINE_METHOD_LABEL, messages.NO_INLINE_METHOD);
		
	//inlining based on compression
	final public FlagOption compInline = new FlagOption(messages.COMP_INLINE_LABEL, messages.COMP_INLINE); 
	
	//inlining based on performance
	final public FlagOption perfInline = new FlagOption(messages.PERF_INLINE_LABEL, messages.PERF_INLINE); 
	
	//inline from anywhere on the classpath, including even methods in java.lang.Object
	final public FlagOption inlineFromAnywhere = new FlagOption(messages.ANYWHERE_INLINE_LABEL, messages.ANYWHERE_INLINE);
	
	//there are more inlining opportunities if permissions are expanded to allow increased access
	final public FlagOption overridePermissions = new FlagOption(messages.EXPAND_PERMISSIONS_LABEL, messages.EXPAND_PERMISSIONS); 
	
	//assume that virtual method calls can be overridden, so that even if there is no overriding method
	//currently found, that at run-time more will be found
	final public FlagOption assumeUnknownVirtualTargets = new FlagOption(messages.ASSUME_UNKNOWN_LABEL, messages.ASSUME_UNKNOWN);
	
	final public SpecifierOption inlineMethodJSRs = new SpecifierOption(messages.INLINE_JSRS_METHOD_LABEL, messages.INLINE_JSRS_METHOD);
	
	final public SpecifierOption noInlineMethodJSRs = new SpecifierOption(messages.NO_INLINE_JSRS_METHOD_LABEL, messages.NO_INLINE_JSRS_METHOD);
	
	final public FlagOption inlineAllJSRs = new FlagOption(messages.INLINE_ALL_JSRS_LABEL, messages.INLINE_ALL_JSRS);
		
	
			
	public InlineExtension() {}

	/**
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {inlineMethod, noInlineMethod, perfInline, compInline, inlineFromAnywhere,
			overridePermissions, assumeUnknownVirtualTargets, inlineMethodJSRs, noInlineMethodJSRs, inlineAllJSRs};
	}
	
	//static PrintStream errorStream;//TODO remove
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository rep, Logger logger)
		throws ExtensionException {
//		try {
//			MethodCallSite.errorStream = errorStream = new PrintStream(new FileOutputStream("d:\\temp\\err.txt"));
//		} catch(IOException e) {
//			throw new ExtensionException(this, e.toString());
//		}
		InlineRepository inlineRepository = new InlineRepository(
			rep, 
			assumeUnknownVirtualTargets.isFlagged(),
			inlineFromAnywhere.isFlagged());		
			
		Specifier methodsSpec[];
		
		methodsSpec = noInlineMethod.getSpecifiers();
		BT_MethodVector methodsToNotInline = new BT_HashedMethodVector();
		if(methodsSpec.length > 0) {
			MemberActor noinlineActor = 
				new MemberCollectorActor(methodsToNotInline, null);
			rep.findMethods(methodsSpec, noinlineActor);
		}
		
		methodsSpec = inlineMethod.getSpecifiers();
			
		BT_MethodVector methodsToInline = new BT_HashedMethodVector();
		if(methodsSpec.length > 0) {
			MemberActor actor = 
				new MemberCollectorActor(methodsToInline, null);
			rep.findMethods(methodsSpec, actor);
		}
		inline(rep, logger, inlineRepository, methodsToNotInline, methodsToInline);
	
		
		methodsSpec = noInlineMethodJSRs.getSpecifiers();
		BT_MethodVector methodsToNotJSRInline = new BT_HashedMethodVector();
		if(methodsSpec.length > 0) {
			MemberActor noinlineActor = 
				new MemberCollectorActor(methodsToNotJSRInline, null);
			rep.findMethods(methodsSpec, noinlineActor);
		}

		methodsSpec = inlineMethodJSRs.getSpecifiers();
	
		BT_MethodVector methodsToJSRInline = new BT_HashedMethodVector();
		if(methodsSpec.length > 0) {
			MemberActor actor = 
				new MemberCollectorActor(methodsToJSRInline, null);
			rep.findMethods(methodsSpec, actor);
		}
				
		inlineJSRs(rep, logger, inlineRepository, methodsToNotJSRInline, methodsToJSRInline);
		
		//errorStream.close();
	}
	
	protected void inlineJSRs(JaptRepository rep, 
							Logger logger, 
							InlineRepository inlineRepository, 
							BT_MethodVector methodsToNotJSRInline, 
							BT_MethodVector methodsToJSRInline) {
		JaptFactory factory = rep.getFactory();
		int inlinedMethodCount = 0;
		for(int j=0; j<methodsToJSRInline.size(); j++) {
			BT_Method method = methodsToJSRInline.elementAt(j);
			if(methodsToNotJSRInline.contains(method)) {
				continue;
			}

			Method m = inlineRepository.getMethod(method);
			try {
				if(!m.hasAttemptedInlineJSRs() && m.inlineJSRs()) {
					factory.noteJSRsInlined(method);
					inlinedMethodCount++;
				}
			} catch(BT_CodeException e) {
				rep.getFactory().noteCodeException(e);
			}
		}
		
		if(!inlineAllJSRs.isFlagged()) {
			return;
		}
		BT_ClassVector classes = rep.getInternalClasses();
		for(int i=0; i<classes.size(); i++) {
			BT_MethodVector methods = classes.elementAt(i).getMethods();
			for(int j=0; j<methods.size(); j++) {
				BT_Method m = methods.elementAt(j);
				Method method = inlineRepository.getMethod(m);
				try {
					if(!method.hasAttemptedInlineJSRs() && method.inlineJSRs()) {
						factory.noteJSRsInlined(m);
						inlinedMethodCount++;
					}
				} catch(BT_CodeException e) {
					rep.getFactory().noteCodeException(e);
				}
			}
		}
		messages.JSR_SUMMARY.log(
			logger, 
			new Object[] {
				Integer.toString(inlinedMethodCount)}
		);
	}
	
	//TODO: special inlining case involving permissions
	//consider the case where method A calls B calls C
	//Suppose B cannot be inlined into A because of access
	//permissions of C (MethodCallSite.isVisibilityPreserved), 
	//and later C is inlined into B, then
	//afterwards we actually can inline B into A, so if the order were
	//reversed B would be inlined into A.
	//THink of a way to handle this properly.
	
	//One consequence of this is that running the inline extension twice with the same options can actually produce more inlines!
	protected void inline(JaptRepository rep, 
						Logger logger, 
						InlineRepository inlineRepository, 
						BT_MethodVector methodsToNotInline, 
						BT_MethodVector methodsToInline) {
		int inlinedMethodCount = 0;
		int inlinedCallSiteCount = 0;
		
		int callSiteThreshold = 0;
		
		BT_HashedMethodCallSiteVector uninlinableCallSites = new BT_HashedMethodCallSiteVector();
		topLoop:
		while(methodsToInline.size() > 0) {
			for(int j=methodsToInline.size() - 1; j>=0; j--) {
				BT_Method method = methodsToInline.elementAt(j);
				if(methodsToNotInline.contains(method)) {
					methodsToInline.removeElementAt(j);
					continue;
				}
		
				if(method.isAbstract() || method.isStub() || method.isNative()) {
					methodsToInline.removeElementAt(j);
					continue;
				}
				BT_CodeAttribute code = method.getCode();
				BT_MethodCallSiteVector methodsCalled = code.calledMethods;
				if(methodsCalled.size() > callSiteThreshold) {
					continue;
				}
				Method m = inlineRepository.getMethod(method);
				
				try {
					Method.InlineReport report = m.inline(overridePermissions.isFlagged(), rep.isInternalClass(method.getDeclaringClass()), uninlinableCallSites);
					if(report == null) {
						messages.SPEC_METHOD_NOT_INLINABLE.log(logger, method);
						methodsToInline.removeElementAt(j);
						continue;
					}
					int callSitesInlined = report.callSitesInlined.size();
					messages.INLINED_SPEC_METHOD.log(
						logger, 
						new Object[] {
							method.useName(), 
							Integer.toString(callSitesInlined), 
							Integer.toString(report.totalSites)});
					inlinedMethodCount++;
					inlinedCallSiteCount += callSitesInlined;
					report.optimizeMethodsInlinedInto();
					methodsToInline.removeElementAt(j);
					callSiteThreshold = 0; /* we reset the threshold because the act of inlining reduces the call site count in other methods */
					continue topLoop;
				} catch(BT_CodeException e) {
					rep.getFactory().noteCodeException(e);
					methodsToInline.removeElementAt(j);
					continue;
				}
			}
			callSiteThreshold++;
		}
		uninlinableCallSites = null;
		if(perfInline.isFlagged() || compInline.isFlagged()) {
			Inliner inliner = 
				new Inliner(
					logger, 
					messages,
					methodsToNotInline,
					inlineFromAnywhere.isFlagged(),
					inlineRepository,
					rep);
			if(perfInline.isFlagged()) {
				inliner.inlineForPerformance(overridePermissions.isFlagged());
			}
			
			if(compInline.isFlagged()) {
				inliner.inlineForCompression(overridePermissions.isFlagged());
			}
			inlinedMethodCount += inliner.totalReport.methodsInlined.size();
			inlinedCallSiteCount += inliner.totalReport.callSitesInlined.size();
			inliner.totalReport.optimizeMethodsInlinedInto();
		}
		messages.SUMMARY.log(
			logger, 
			new Object[] {
				Integer.toString(inlinedMethodCount),
				Integer.toString(inlinedCallSiteCount)}
		);
	}

	
	
}
