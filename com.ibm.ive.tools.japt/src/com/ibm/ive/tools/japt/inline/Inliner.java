package com.ibm.ive.tools.japt.inline;

import java.util.HashSet;

import com.ibm.ive.tools.japt.JaptMethod;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
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
public class Inliner {

	private boolean inlineFromAnywhere;
	private JaptRepository rep;
	private InlineRepository repository;
	private BT_MethodVector methodsToNotInline;
	Method.InlineReport totalReport;
	Messages messages;
	Logger logger;
	
	
	/**
	 * Constructor for Inliner.
	 */
	public Inliner(Logger logger, Messages messages,
				   BT_MethodVector methodsToNotInline,
				   boolean inlineFromAnywhere, 
				   InlineRepository repository, 
				   JaptRepository rep) {
		this.rep = rep;
		this.repository = repository;
		totalReport = new Method.InlineReport(repository);
		this.inlineFromAnywhere = inlineFromAnywhere;
		this.messages = messages;
		this.logger = logger;
		this.methodsToNotInline = methodsToNotInline;
	}
		
	/* 
	 * Strategy: we also attempt to inline methods with the fewest number of method calls first 
	 * This allows for a better estimate at inlining effectiveness: if we inline a method m
	 * into a method n based on the size of m, and later on we inline other methods into the bytecode of m, then
	 * we did not get a proper estimate of the amount of bytecode we were inlining into n.
	 * 
	 * This issue does not arise when inlining a method with no method calls.  In general, it is better
	 * to inline methods with fewer method calls first to get a better estimate on the effectiveness 
	 * of each inline.
	 */
	void inlineForCompression(boolean overridePermissions) {
		inlineEverything(overridePermissions, -1);
	}
	
	void inlineForPerformance(boolean overridePermissions) {
		inlineEverything(overridePermissions, 8);
	}
	
	void inlineEverything(boolean overridePermissions, int bytecodeSizeThreshold) {
		BT_ClassVector classes;
		if(inlineFromAnywhere) {
			classes = rep.getClasses();
		}
		else {
			classes = rep.getInternalClasses();
		}
		
		
		HashSet triedToInline = new HashSet();
		
		int totalMethodSize = 0;
		
		for(int i=0; i<classes.size(); i++) {
			BT_MethodVector methods = classes.elementAt(i).getMethods();
			totalMethodSize += methods.size();
		}
		
		//we maximize inline results by inlining those methods with the fewest number of callsites first.
		int currentCallSiteThreshold = 0;
		
		int triedSize = triedToInline.size();
		while(triedSize < totalMethodSize) {
			inline(classes, triedToInline, currentCallSiteThreshold, overridePermissions, bytecodeSizeThreshold);
			currentCallSiteThreshold++;
			triedSize = triedToInline.size();
		}
		
	}
	
	private int inline(BT_ClassVector classes, HashSet triedAlready, int callsiteThreshold, boolean overridePermissions, int bytecodeSizeThreshold) {
		int currentInlineAttemptCount = 0;
		for(int i=0; i<classes.size(); i++) {
			BT_MethodVector methods = classes.elementAt(i).getMethods();
			currentInlineAttemptCount += inline(methods, triedAlready, callsiteThreshold, overridePermissions, bytecodeSizeThreshold);
		}
		return currentInlineAttemptCount;
	}
	
	private int inline(BT_MethodVector methods, HashSet triedAlready, int callsiteThreshold, boolean overridePermissions, int bytecodeSizeThreshold) {
		int currentInlineAttemptCount = 0;
		for(int j=0; j<methods.size(); j++) {
			BT_Method method = methods.elementAt(j);
			if(triedAlready.contains(method)) {
				continue;
			}
			BT_CodeAttribute code = method.getCode();
			if(code == null || methodsToNotInline.contains(method)) {
				triedAlready.add(method);
				continue;
			}
			
			BT_MethodCallSiteVector callSitesVector = ((JaptMethod) method).getAllCallSites();
			BT_MethodCallSite callSites[] = callSitesVector.toArray();
			
			BT_MethodCallSiteVector containedCallSites = code.calledMethods;
			//we only try methods with call site counts below the threshold.
			//This allows us to target getters and setters and others like Math.max
			if(containedCallSites.size() > callsiteThreshold) {
				//we will try this one later
				continue;
			}
						
			Method methodToInline = repository.getMethod(method);
			Method.InlineReport report;
			if(bytecodeSizeThreshold < 0) {
				report = methodToInline.inlineForCompression(callSites, overridePermissions);
			}
			else {
				report = methodToInline.inlineForPerformance(callSites, overridePermissions, bytecodeSizeThreshold);
			}
			if(report != null) {
				totalReport.methodsInlined.addAllUnique(report.methodsInlined);
				totalReport.methodsInlinedInto.addAllUnique(report.methodsInlinedInto);
				totalReport.callSitesInlined.addAllUnique(report.callSitesInlined);
				totalReport.totalSites += report.totalSites;
				
				int sitesInlined = report.callSitesInlined.size();
				int totalSites = report.totalSites;
				if(totalSites == 0) {
					messages.INLINED_METHOD_NO_SITES.log(logger, method.useName());	
				}
				else if(sitesInlined == totalSites) {
					if(sitesInlined == 1) {
						messages.INLINED_METHOD_ONE_SITE.log(logger, method.useName());
					}
					else {
						messages.INLINED_METHOD_ALL_SITES.log(logger, new Object[] {method.useName(), Integer.toString(totalSites)});
					}
				}
				else {
					messages.INLINED_METHOD.log(logger, new Object[] {method.useName(), Integer.toString(sitesInlined), Integer.toString(totalSites)});
				}
				
				//we have just potentially reduced the number of callsites contained within those methods 
				//that we have inlined into, which might have brought the number of 
				//contained callsites below our current threshold, so we should try to inline
				//these right away since they potentially meet this or a lower threshold,
				//and we also do the lowest thresholds first
				
				for(int k=0; k<callsiteThreshold; k++) {
					currentInlineAttemptCount += inline(report.methodsInlinedInto, triedAlready, k, overridePermissions, bytecodeSizeThreshold);
				}
			}
			triedAlready.add(method);
			currentInlineAttemptCount++;
			
		}
		return currentInlineAttemptCount;
	}
	
}
