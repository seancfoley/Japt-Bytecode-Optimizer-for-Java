package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.reduction.ClassProperties;

public class PropagationProperties {
	protected boolean useIntraProceduralAnalysis;
	protected boolean shareGenericStrings;
	protected boolean useGenericObjects;
	//protected boolean enterExternalMethods = true;
	public boolean verboseIterations;
	public boolean storeCreatedInMethodInvocations;
	int depthLimit = -1;
	long timeLimit = 0;
	int propLimit = 0;
	public boolean exceededDepth;
	ArrayList closedPackages = new ArrayList();
	public final int analysisType;
	public final ContextProvider provider;
	public final InstantiatorProvider instantiatorProvider;
	public final ClassProperties classProps;
	
	
	public static final int ESCAPE_ANALYSIS = 0;
	public static final int REACHABILITY_ANALYSIS = 1;
	public static final int RTSJ_ANALYSIS = 2;
	
	public PropagationProperties(int analysisType, ContextProvider provider, InstantiatorProvider instantiatorProvider, ClassProperties classProps) {
		this.analysisType = analysisType;
		this.provider = provider;
		this.instantiatorProvider = instantiatorProvider;
		this.classProps = classProps;
	}
	
	/**
	 * returns whether all classes in the given package have been loaded.
	 * @param packageName
	 * @return
	 */
	boolean packageIsClosed(String packageName) {
		for(int i=0; i<closedPackages.size(); i++) {
			PatternString pattern = (PatternString) closedPackages.get(i);
			if(pattern.isMatch(packageName)) {
				return true;
			}
		}
		return false;
	}
	
	public void addClosedPackages(PatternString pattern) {
		closedPackages.add(pattern);
	}
	
	public boolean isRTSJAnalysis() {
		return analysisType == RTSJ_ANALYSIS;
	}
	
	public boolean isEscapeAnalysis() {
		return analysisType == ESCAPE_ANALYSIS;
	}
	
	public boolean isReachabilityAnalysis() {
		return analysisType == REACHABILITY_ANALYSIS;
	}
	
	public boolean useIntraProceduralAnalysis() {
		return useIntraProceduralAnalysis;
	}
	
	public void setIntraProceduralAnalysis(boolean on) {
		useIntraProceduralAnalysis = on;
	}
	
//	public boolean enterExternalMethods() {
//		return enterExternalMethods;
//	}
//	
//	public void setEnterExternalMethods(boolean on) {
//		enterExternalMethods = on;
//	}
	
	public void setDepthLimit(int limit) {
		depthLimit = limit;
	}
	
	public int getDepthLimit() {
		return depthLimit;
	}
	
	public void setPropagationLimit(int limit) {
		propLimit = limit;
	}
	
	public int getPropagationLimit() {
		return propLimit;
	}
	
	/**
	 * sets the time limit to a future time in milliseconds measured from the epoch.
	 * @param limit
	 */
	public void setTimeLimit(long limit) {
		timeLimit = limit;
	}
	
	public long getTimeLimit() {
		return timeLimit;
	}
	
	public boolean useGenericObjects() {
		return useGenericObjects;
	}
	
	public void setUseGenerics(boolean on) {
		useGenericObjects = on;
	}
	
	/* 
	 * classes that are final (hence no subclassing and thus generic splitting) 
	 * and cannot propagate other objects (hence ITA propagation is not compromised) can share their objects.
	 * 
	 * But this means that the shared objects themselves are no longer trackable individually, possibly detrimental
	 * if applying escape analysis to generic objects.
	 * 
	 */
	public boolean shareGenericStrings() {
		return shareGenericStrings;
	}
	
	public void setShareGenericStrings(boolean share) {
		shareGenericStrings = share;
	}
}
