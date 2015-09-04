package com.ibm.ive.tools.japt.escape;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.ive.tools.japt.reduction.ita.CreatedObject;
import com.ibm.ive.tools.japt.reduction.ita.InstructionLocation;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Factory;
import com.ibm.jikesbt.BT_Method;

public class FullSummary extends UnescapedObjectSummary {
	
	static final Long zero = new Long(0);
	
	HashMap statsMap = new HashMap();
	
	private long totalMaxDepth;
	private long overallMaxDepth;
	private long totalAnalysisTime;
	private int addCount;
	private int noTimeCount;
	
	ArrayList analyses = new ArrayList();
	TreeMap timesCount = new TreeMap();
	
	
	static class DataPoint implements Comparable {
		static int pointCounter;
		Long analysisMeasurement;
		float percentage;
		int counter = pointCounter++; /* this is used so that we can be stored in tree maps and tree sets,
		 			with no two points matching exactly.  hashCode() is not guaranteed to be distinct amongst distinct objects. */
		
		DataPoint(Long analysisMeasurement, float percentage) {
			this.analysisMeasurement = analysisMeasurement;
			this.percentage = percentage;
		}
		
		public int compareTo(Object object) {
			DataPoint other = (DataPoint) object;
			int result;
			result = analysisMeasurement.compareTo(other.analysisMeasurement);
			if(result == 0) {
				float diff = other.percentage - percentage;
				result = (diff > 0) ? -1 : ((diff < 0) ? 1 : 0);
				if(result == 0) {
					result = counter - other.counter;
					if(result == 0) { /* pointCounter can overflow giving two distinct points the same counter, so use hashCode as a last resort */
						result = hashCode() - other.hashCode();
						if(result == 0 && this != other) {
							/* if by some miracle both the hash codes and counters are the same for two distinct points, 
							 * then there is not much else we can do 
							 */
						}
					}
				}
			}
			return result;
		}
		
		public String toString() {
			return analysisMeasurement + "," + getPercentage(percentage);
		}
		
	}
	
	static class Counter implements Comparable {
		Comparable comparable;
		int count;
		boolean sortByCount;
		
		Counter(BT_Class clazz) {
			this.comparable = clazz;
			sortByCount = true;
		}
		
		Counter(Long time) {
			this.comparable = time;
		}
		
		void increment() {
			count++;
		}
		
		public int compareTo(Object object) {
			Counter other = (Counter) object;
			int result;
			if(sortByCount) {
				result = other.count - count;
				if(result == 0) {
					result = comparable.compareTo(other.comparable);
				}
			} else {
				result = comparable.compareTo(other.comparable);
				if(result == 0) {
					result = other.count - count;
				}
			}
			return result;
		}
		
		public String toString() {
			if(!sortByCount) {
				StringBuffer buffer = new StringBuffer(comparable.toString());
				for(int i=1; i<count; i++) {
					buffer.append(BT_Factory.endl());
					buffer.append(comparable);
				}
				return buffer.toString();
			}
			return comparable + "," + count;
		}
		
		public boolean equals(Object object) {
			if(object instanceof Counter) {
				Counter other = (Counter) object;
				return comparable.equals(other.comparable);
			}
			return false;
		}
	}
	
	static class SiteStats {
		final InstantiationSite site;
		
		private boolean noEscape;
		
		/**
		 *  The number of times the unescaped object might have made its way to a base method.
		 *  This might be an overestimate, because method invocations are shared.  For instance, an append()
		 *  call on StringBuffer will result in all StringBuffer objects being propagated from the append() method
		 *  back to the caller through the return value.
		 */
		private int receivedCount; 
		
		/** 
		 * The number of times the unescaped object does not make its way to a base method.  This can
		 * be an underestimate for the reason described above.
		 */
		private int unreceivedCount; 
		
		
		private int escapedBaseCount; /* the number of times the object escaped */
		private int escapedOtherCount; /* the number of times the object escaped */
		private int escapedReceivedCount; /* the number of times the escaped object makes its way to a base method */
		
		SiteStats(InstantiationSite site) {
			this.site = site;
		}
		
		boolean isInternalSite() {
			return site.isInternal();
		}
		
		public String toString() {
			return "stats for " + site;
		}
	}
	
	private static class EscapeStats {
		BT_Method method;
		long analysisTime;
		boolean exceededDepth;
		int maxDepth;
		int unescapedInstantiatedCount; 
		int unescapedReceivedCount;
		int unescapedUnreceivedCount;
		
		int escapedInstantiatedCount; 
		int escapedReceivedCount;
		int escapedUnreceivedCount;
		
		float getEscapeRatio() {
			int unescaped = unescapedInstantiatedCount + unescapedReceivedCount + unescapedUnreceivedCount;
			int escaped = escapedInstantiatedCount + escapedReceivedCount + escapedUnreceivedCount;
			int total = unescaped + escaped;
			return ((float) unescaped) / (float) total;
		}
		
		int getReachedCount() {
			return unescapedInstantiatedCount + unescapedReceivedCount + unescapedUnreceivedCount
			 + escapedInstantiatedCount + escapedReceivedCount + escapedUnreceivedCount;
		}
		
		int getUnescapedCount() {
			return unescapedInstantiatedCount + unescapedReceivedCount + unescapedUnreceivedCount;
		}
		
	}
	
	SiteStats getStats(CreatedObject object) {
		InstantiationSite site = convert(object);
		Object obj = statsMap.get(site);
		if(obj == null) {
			obj = new SiteStats(site);
			statsMap.put(site, obj);
		}
		return (SiteStats) obj;
	}
	
	Counter getCount(Long time) {
		Object obj = timesCount.get(time);
		if(obj == null) {
			obj = new Counter(time);
			timesCount.put(time, obj);
		}
		return (Counter) obj;
	}
	
	static Counter getTypeCount(TreeMap map, BT_Class type) {
		Counter counter = (Counter) map.get(type);
		if(counter == null) {
			counter = new Counter(type);
			map.put(type, counter);
		}
		return counter;
	}
	
	void add(UnescapedObjectSet o) {
		Iterator iterator = o.unescapedInstantiated.iterator();
		while(iterator.hasNext()) {
			CreatedObject next = (CreatedObject) iterator.next();
			SiteStats stats = getStats(next);
			stats.noEscape = true;
		}
		FullUnescapedObjectSet other = (FullUnescapedObjectSet) o;
		iterator = other.unescapedReceived.iterator();
		while(iterator.hasNext()) {
			CreatedObject next = (CreatedObject) iterator.next();
			SiteStats stats = getStats(next);
			stats.receivedCount++;
		}
		iterator = other.unescapedUnreceived.iterator();
		while(iterator.hasNext()) {
			CreatedObject next = (CreatedObject) iterator.next();
			SiteStats stats = getStats(next);
			stats.unreceivedCount++;

		}
		iterator = o.escapedInstantiated.iterator();
		while(iterator.hasNext()) {
			CreatedObject next = (CreatedObject) iterator.next();
			SiteStats stats = getStats(next);
			stats.escapedBaseCount++;
			//stats.escapedCount++;
			//TODO intra ensure ldc creation sites are ignored.
		}
		iterator = other.escapedReceived.iterator();
		while(iterator.hasNext()) {
			CreatedObject next = (CreatedObject) iterator.next();
			SiteStats stats = getStats(next);
			//stats.escapedCount++;
			stats.escapedReceivedCount++;
		}
		iterator = other.escapedUnreceived.iterator();
		while(iterator.hasNext()) {
			CreatedObject next = (CreatedObject) iterator.next();
			SiteStats stats = getStats(next);
			//stats.escapedCount++;
			stats.escapedOtherCount++;
		}
		
		EscapeStats stats = new EscapeStats();
		analyses.add(stats);
		stats.exceededDepth = other.exceededDepth;
		stats.analysisTime = other.analysisTime;
		stats.maxDepth = other.maxDepth;
		stats.method = other.method.getMethod();
		stats.escapedInstantiatedCount = other.escapedInstantiated.size();
		stats.escapedReceivedCount = other.escapedReceived.size();
		stats.escapedUnreceivedCount = other.escapedUnreceived.size();
		stats.unescapedInstantiatedCount = other.unescapedInstantiated.size();
		stats.unescapedReceivedCount = other.unescapedReceived.size();
		stats.unescapedUnreceivedCount = other.unescapedUnreceived.size();
			
		long analysisTime = other.analysisTime;
		if(analysisTime == 0) {
			noTimeCount++;
		} else {
			totalAnalysisTime += other.analysisTime;
		}
		totalMaxDepth += other.maxDepth;
		overallMaxDepth = Math.max(other.maxDepth, overallMaxDepth);
		Long analysisInteger = new Long(analysisTime);
		Counter count = getCount(analysisInteger);
		count.increment();
		addCount++;
	}
	
	
	
	void write(PrintWriter writer) throws IOException {
		final boolean withTypeCounts = true;
		final boolean withTimes = true;
		final boolean withPercentagePoints = true;
		int siteCount = 0;
		int internalSiteCount = 0;
		int externalSiteCount = 0;
		int externalSiteCountReceivedInternally = 0;
		int internalNoEscapeCount = 0;
		int internalSometimesEscapeCount = 0;
		int internalNoArgEscapeCount = 0;
		int internalEscapeCount = 0;
		int externalNoArgEscapeCount = 0;
		int externalSometimesEscapeCount = 0;
		int externalAlwaysEscapeCount = 0;
		TreeMap typeCount = new TreeMap();
		TreeMap receivedTypeCount = new TreeMap();
		
		Iterator iterator = statsMap.values().iterator();
		while(iterator.hasNext()) {
			SiteStats stats = (SiteStats) iterator.next();
			if(stats.isInternalSite()) {
				internalSiteCount++;
				if(stats.noEscape) {
					internalNoEscapeCount++;
					
					/* add the unescaped type to the type count */
					BT_Class createdType = stats.site.instantiatedType;
					Counter counter = getTypeCount(typeCount, createdType);
					counter.increment();
					
				} else if(stats.receivedCount > 0 || stats.unreceivedCount > 0) {
					if(stats.escapedOtherCount > 0 || stats.escapedReceivedCount > 0) {//xx; escaped count will always be non-zero because of the time it escaped the base method
					//if(stats.escapedCount > 0) {//xx; escaped count will always be non-zero because of the time it escaped the base method
						internalSometimesEscapeCount++;
					} else {
						internalNoArgEscapeCount++;
					}
				} else {
					internalEscapeCount++;
				}
				
			} else {
				if(stats.noEscape) {
					/* only internal methods are being examined for escaped objects */
					throw new Error();
				}
				
				if(stats.receivedCount > 0) { /* the object is used by an internal method and does not escape */
					
					//if(stats.escapedCount > 0 || ||) { /* other times it does escape */
					if(stats.escapedOtherCount > 0 || stats.escapedReceivedCount > 0) { /* other times it does escape */
						externalSometimesEscapeCount++;
					} else {
						externalNoArgEscapeCount++;
					}
					externalSiteCountReceivedInternally++;
					
					
					/* add the unescaped type to the type count */
					BT_Class createdType = stats.site.instantiatedType;
					Counter counter = getTypeCount(receivedTypeCount, createdType);
					counter.increment();

				} else if(stats.escapedReceivedCount > 0) { /* the object is used by an internal method and does escape */
					externalAlwaysEscapeCount++;
					externalSiteCountReceivedInternally++;
				}
				externalSiteCount++;
			}
			siteCount++;
			
		}
		
		float percentage = getPercentage(internalNoEscapeCount, internalSiteCount);
		writer.println(internalNoEscapeCount + " of " + internalSiteCount 
				+ " internal object creations do not escape the method of creation," + percentage + "%");
		percentage = getPercentage(internalNoArgEscapeCount, internalSiteCount);
		writer.println(internalNoArgEscapeCount + " of " + internalSiteCount 
				+ " internal object creations never escape the call stack," + percentage + "%");
		percentage = getPercentage(internalSometimesEscapeCount, internalSiteCount);
		writer.println(internalSometimesEscapeCount + " of " + internalSiteCount 
				+ " internal object creations sometimes escape the call stack depending on the calling context," + percentage + "%");
		percentage = getPercentage(internalEscapeCount, internalSiteCount);
		writer.println(internalEscapeCount + " of " + internalSiteCount 
				+ " internal object creations escape," + percentage + "%");
		writer.println();
		
		if(withTypeCounts) {
			writer.println("Created unescaped counts:");
			toSortedString(typeCount, writer);
			writer.println();
		}
		
		
		percentage = getPercentage(externalNoArgEscapeCount, externalSiteCountReceivedInternally);
		writer.println(externalNoArgEscapeCount + " of " + externalSiteCountReceivedInternally 
				+ " external object creations used internally never escape the call stack," + percentage + "%");
		
		percentage = getPercentage(externalSometimesEscapeCount, externalSiteCountReceivedInternally);
		writer.println(externalSometimesEscapeCount + " of " + externalSiteCountReceivedInternally 
				+ " external object creations used internally sometimes escape the call stack depending on the calling context," + percentage + "%");
		
		percentage = getPercentage(externalAlwaysEscapeCount, externalSiteCountReceivedInternally);
		writer.println(externalAlwaysEscapeCount + " of " + externalSiteCountReceivedInternally 
				+ " external object creations used internally escape," + percentage + "%");
		writer.println();
		
		if(withTypeCounts) {
			writer.println("Counts of external objects created in external methods but used by internal methods that do not escape:");
			toSortedString(receivedTypeCount, writer);
			writer.println();
		}
		
		iterator = analyses.iterator();
		int statsCount = analyses.size();
		int reachedInstantiationCount = 0;
		double percentageTotal = 0;
		int totalUnescaped = 0;
		int total = 0;
		int totalExceededDepth = 0;
		TreeSet timeVsPercentage = new TreeSet();
		TreeSet timeVsPercentageAll = new TreeSet();
		TreeSet depthVsPercentage = new TreeSet();
		while(iterator.hasNext()) {
			EscapeStats stats = (EscapeStats) iterator.next();
			if(stats.exceededDepth) {
				totalExceededDepth++;
			}
			int reached = stats.getReachedCount();
			float escapeRatio = stats.getEscapeRatio();
			if(reached > 0) {
				reachedInstantiationCount++;
				percentageTotal += escapeRatio;
				totalUnescaped += stats.getUnescapedCount();
				total += reached;
				DataPoint point = new DataPoint(new Long(stats.analysisTime), escapeRatio);
				timeVsPercentage.add(point);
				timeVsPercentageAll.add(point);
			} else {
				DataPoint point = new DataPoint(zero, escapeRatio);
				timeVsPercentageAll.add(point);
			}
			DataPoint point = new DataPoint(new Long(stats.maxDepth), escapeRatio);
			depthVsPercentage.add(point);
		}
		writer.println("Percentage which reach instantiations," + getPercentage(reachedInstantiationCount, statsCount) + "%");
		writer.println("Average percentage unescaped," + getPercentage(percentageTotal, reachedInstantiationCount) + "%");
		writer.println("Weighted overall percent unescaped," + getPercentage(totalUnescaped, total) + "%");
		writer.println("Percentage which exceed depth," + getPercentage(totalExceededDepth, statsCount) + "%, and total, " + totalExceededDepth);
		percentage = getPercentage(noTimeCount, addCount);
		writer.println("The average analysis time is " + getAverage(totalAnalysisTime, addCount) + " micro seconds");
//		writer.println("The average analysis time is " + getAverage(totalAnalysisTime, addCount) + " time units with " 
//				+ noTimeCount + " of " + addCount + " taking less than a single time unit," + percentage + "%");
		writer.println("The average depth is " + getAverage(totalMaxDepth, addCount));
		writer.println("The maximum depth is " + overallMaxDepth);
		writer.println("The number of analyzed methods is " + addCount + " in " + totalClasses + " classes (non-interface)");
		writer.println("The number of interfaces is " + totalInterfaces);
		writer.println();
		if(withPercentagePoints) {
			writer.println("Time vs escape percentage points:");
			toSortedString(timeVsPercentage, writer);
			writer.println();
			writer.println();
			
			writer.println("Depth vs escape percentage points:");
			toSortedString(depthVsPercentage, writer);
			writer.println();
			writer.println();
		}
		if(withTimes) {
			writer.println("Analysis time counts:");
			toSortedString(timesCount, writer);
			writer.println();
			writer.println();
		}
		writer.println(exceedCount + " analyses exceeded propagation limits");
	}
	
	private static void toSortedString(TreeMap map, PrintWriter writer) throws IOException {
		TreeSet sortedSet = new TreeSet();
		Iterator iterator = map.values().iterator();
		while(iterator.hasNext()) {
			sortedSet.add(iterator.next());
		}
		toSortedString(sortedSet, writer);
	}
	
	private static void toSortedString(TreeSet sortedSet, PrintWriter writer) throws IOException {
		Iterator iterator = sortedSet.iterator();
		while(iterator.hasNext()) {
			Object object = iterator.next();
			writer.println(object.toString());
		}
	}
	
	private static InstantiationSite convert(CreatedObject object) {
		InstructionLocation location = object.getInstructionLocation();
		BT_Method containingMethod = object.invocation.getMethod().getMethod();
		BT_Class instantiatedType = object.getObject().getType().getUnderlyingType();
		return new InstantiationSite(containingMethod, location, instantiatedType);
	}
	
	private static float getAverage(double num, double denom) {
		double decimal = num / denom;
		return getAverage(decimal);
	}
	
	private static float getAverage(double decimal) {
		double val = decimal * 100;
		int res = (int) val;
		return ((float) res) / 100;
	}
	
	private static float getPercentage(double num, double denom) {
		double decimal = num / denom;
		return getPercentage(decimal);
	}
	
	private static float getPercentage(double decimal) {
		double val = decimal * 10000;
		int res = (int) val;
		return ((float) res) / 100;
	}
}
