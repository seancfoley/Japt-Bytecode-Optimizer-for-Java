package com.ibm.ive.tools.japt.escape;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class BasicSummary extends UnescapedObjectSummary {

	int methodCount;
	
	int methodInstantiatingCount;
	int methodWithUnescapedCount;
	int methodWithEscapedCount;
	double totalPercentEscaping;
	
	int initialMethodInstantiatingCount;
	int initialMethodWithUnescapedCount;
	int initialMethodWithEscapedCount;
	double initialTotalPercentEscaping;
	
	int exceededDepthCount;
	long totalDepth;
	int deepestDepth;
	long totalAnalysisTime;
	private static int INCREMENT = 10000;
	long analysisTimes[] = new long[INCREMENT];
	
	void add(UnescapedObjectSet other) {
		int unescapedSize = other.unescapedInstantiated.size() + other.unescaped.size();
		int escapedSize = other.escapedInstantiated.size() + other.escaped.size();
		if(unescapedSize > 0) {
			methodWithUnescapedCount++;
		}
		if(escapedSize > 0) {
			methodWithEscapedCount++;
		}
		if(escapedSize > 0 && unescapedSize > 0) {
			if(other.exceededDepth) {
				exceededDepthCount++;
			}
		}
		
		unescapedSize = other.unescapedInstantiated.size();
		escapedSize = other.escapedInstantiated.size();
		if(unescapedSize > 0) {
			initialMethodWithUnescapedCount++;
		}
		if(escapedSize > 0) {
			initialMethodWithEscapedCount++;
		}
		
		totalDepth += other.maxDepth;
		deepestDepth = Math.max(deepestDepth, other.maxDepth);
		totalAnalysisTime += other.analysisTime;
		if(analysisTimes.length == methodCount) {
			resizeTimes(analysisTimes.length + INCREMENT);
		}
		analysisTimes[methodCount] = other.analysisTime;
		methodCount++;
		if(other.totalObjects > 0) {
			methodInstantiatingCount++;
			totalPercentEscaping += other.percentageEscaping;
		}
		if(other.totalInstantiated > 0) {
			initialMethodInstantiatingCount++;
			initialTotalPercentEscaping += other.percentageEscapingInitialMethod;
		}
	}
	
	void resizeTimes(int newlen) {
		long tmp[] = analysisTimes;
		analysisTimes = new long[newlen];
		System.arraycopy(tmp, 0, analysisTimes, 0, Math.min(tmp.length, newlen));
	}

	void write(PrintWriter writer) throws IOException {
		writer.println("The number of analyzed methods is " + methodCount + " in " + totalClasses + " non-interface non-array classes");
		
		if(methodCount == 0) {
			return;
		}
		writer.println("Escaping objects:");
		
		float val;
		String prefix = "\t";
		writer.print(prefix);
		writer.println("Counting object instantiations within the full analyzed call depth:");
		
		prefix = "\t\t";
		val = ((int) (100 * (100 * methodInstantiatingCount / (float) methodCount))) / (float) 100;
		writer.print(prefix);
		//System.out.println(methodInstantiatingCount + " of " + methodCount);
		String perInvAtLeastOne = "Percentage of invocations instantiating at least one object: ";
		String str = perInvAtLeastOne + val + "%";
		writer.println(str);
		
		val = ((int) (100 * (methodWithEscapedCount * 100 / (float) methodCount))) / (float) 100;
		writer.print(prefix);
		//System.out.println(methodWithEscapedCount + " of " + methodCount);
		String perInvAtLeastOneEscaping = "Percentage of invocations with at least one object escaping: ";
		str = perInvAtLeastOneEscaping + val + "%";
		writer.println(str);
		
		val = ((int) (100 * (methodWithUnescapedCount * 100 / (float) methodCount))) / (float) 100;
		writer.print(prefix);
		//System.out.println(methodWithEscapedCount + " of " + methodCount);
		String perInvAtLeastOneBitEscaping = "Percentage of invocations with at least one object not escaping: ";
		str = perInvAtLeastOneBitEscaping + val + "%";
		writer.println(str);
		
		val = ((int) (100 * (methodWithEscapedCount * 100 / (float) methodInstantiatingCount))) / (float) 100;
		writer.print(prefix);
		//System.out.println(methodWithEscapedCount + " of " + methodInstantiatingCount);
		String perInvInstAtLeastOneEscaping = "Percentage of invocations instantiating at least one object with at least one object escaping: ";
		str = perInvInstAtLeastOneEscaping + val + "%";
		writer.println(str);
		
		val = ((int) (100 * (methodWithUnescapedCount * 100 / (float) methodInstantiatingCount))) / (float) 100;
		writer.print(prefix);
		//System.out.println(methodWithEscapedCount + " of " + methodInstantiatingCount);
		String perInvInstAtLeastOneUnescaping = "Percentage of invocations instantiating at least one object with at least one object not escaping: ";
		str = perInvInstAtLeastOneUnescaping + val + "%";
		writer.println(str);
		
		val = ((int) (100 * totalPercentEscaping / methodInstantiatingCount)) / (float) 100;
		writer.print(prefix);
		//System.out.println(totalPercentEscaping + " of " + methodInstantiatingCount);
		String avgPerEscaping = "Average percentage of instantiated objects escaping amongst invocations that instantiate objects: ";
		str = avgPerEscaping + val + "%";
		writer.println(str);
		
		prefix = "\t";
		writer.print(prefix);
		writer.println("Counting object instantiations within only the initial invocation:");
		
		prefix = "\t\t";
		val = ((int) (100 * (100 * initialMethodInstantiatingCount / (float) methodCount))) / (float) 100;
		writer.print(prefix);
		writer.println(perInvAtLeastOne + val + '%');
		
		val = ((int) (100 * (initialMethodWithEscapedCount * 100 / (float) methodCount))) / (float) 100;
		writer.print(prefix);
		writer.println(perInvAtLeastOneEscaping + val + '%');
		
		val = ((int) (100 * (initialMethodWithUnescapedCount * 100 / (float) methodCount))) / (float) 100;
		writer.print(prefix);
		str = perInvAtLeastOneBitEscaping + val + "%";
		writer.println(str);
		
		val = ((int) (100 * (initialMethodWithEscapedCount * 100 / (float) initialMethodInstantiatingCount))) / (float) 100;
		writer.print(prefix);
		writer.println(perInvInstAtLeastOneEscaping + val + '%');
		
		val = ((int) (100 * (initialMethodWithUnescapedCount * 100 / (float) initialMethodInstantiatingCount))) / (float) 100;
		writer.print(prefix);
		str = perInvInstAtLeastOneUnescaping + val + "%";
		writer.println(str);
		
		val = ((int) (100 * initialTotalPercentEscaping / initialMethodInstantiatingCount)) / (float) 100;
		writer.print(prefix);
		writer.println(avgPerEscaping + val + '%');
		
		writer.println("Depth measurements:");
		prefix = "\t";
		writer.print(prefix);
		writer.println("Average depth of followed subinvocations per invocation: " + (totalDepth / methodCount));
		writer.print(prefix);
		writer.println("\tProgam wide maximum depth of subinvocations: " + deepestDepth);
		//TODO number that exceeded depth limit
		writer.println("Analysis times:");
		if(analysisTimes.length != methodCount) {
			resizeTimes(methodCount);
		}
		Arrays.sort(analysisTimes);
		long median;
		if(methodCount % 2 == 0) {
			median = (analysisTimes[methodCount / 2] + analysisTimes[(methodCount / 2) - 1])  / 2;
		} else {
			median = analysisTimes[methodCount / 2];
		}
		long Percentile99 = analysisTimes[99 * methodCount / 100];
		long Percentile95 = analysisTimes[95 * methodCount / 100];
		writer.print(prefix);
		writer.println("Average analysis time per escape analysis: " + ((totalAnalysisTime / methodCount) / 1000) + " microseconds");
		writer.print(prefix);
		writer.println("Median analysis time per escape analysis: " + (median / 1000) + " microseconds");
		writer.print(prefix);
		writer.println("95th percentile analysis time per escape analysis: " + (Percentile95/1000)  + " microseconds");
		writer.print(prefix);
		writer.println("99th percentile analysis time per escape analysis: " + (Percentile99/1000) + " microseconds");
	}

}
