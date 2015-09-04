/*
 * Created on Nov 4, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ive.tools.japt.startupPerformance;

import java.util.Enumeration;

import com.ibm.jikesbt.BT_BasicBlockMarkerIns;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_ExceptionTableEntryVector;

/**
 * @author Sean Foley
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Span {

    public final BT_BasicBlockMarkerIns start;
    public final BT_BasicBlockMarkerIns stop;
    private BT_ExceptionTableEntryVector exceptionEntries;
    private BT_ExceptionTableEntry finallyEntry;
	
    private static final Enumeration emptyEnumeration = new BT_ExceptionTableEntryVector().elements();
	
	public Span(BT_BasicBlockMarkerIns start, BT_BasicBlockMarkerIns stop) {
		this.start = start;
		this.stop = stop;
	}
	
	BT_ExceptionTableEntry getFinallyEntry() {
		return finallyEntry;
	}
	
	Enumeration getExceptionEntries() {
		if(exceptionEntries == null) {
			return emptyEnumeration;
		}
		return exceptionEntries.elements();
	}
	
	public void addEntriesTo(BT_ExceptionTableEntryVector table) {
		if(exceptionEntries != null) {
			for (int n = 0; n < exceptionEntries.size(); n++) {
				table.addElement(exceptionEntries.elementAt(n));
			}
		}
		if(finallyEntry != null) {
			table.addElement(finallyEntry);
		}
	}
	
	public void addExceptionEntry(BT_ExceptionTableEntry newEntry) {
		if(newEntry.catchType == null) {
			throw new IllegalArgumentException();
		}
		if(exceptionEntries == null) {
			exceptionEntries = new BT_ExceptionTableEntryVector();
		}
		exceptionEntries.addElement(newEntry);
	}
	
	public void setFinallyEntry(BT_ExceptionTableEntry newEntry) {
		if(newEntry.catchType != null) {
			throw new IllegalArgumentException();
		}
		finallyEntry = newEntry;
	}
	
	public String toString() {
		String one = Integer.toString(start.byteIndex);
		String two = Integer.toString(stop.byteIndex);
		StringBuffer buf = new StringBuffer(one.length() + two.length() + 50);
		buf.append("from ");
		buf.append(one);
		buf.append(" to ");
		buf.append(two);
		if(exceptionEntries != null) {
			for(int i=0; i<exceptionEntries.size(); i++) {
				buf.append("\n\tcatch ");
				BT_ExceptionTableEntry entry = exceptionEntries.elementAt(i);
				buf.append(entry.catchType);
				buf.append(" at ");
				buf.append(Integer.toString(entry.handlerTarget.byteIndex));
			}
		}
		if(finallyEntry != null) {
			buf.append("\n\tfinally at ");
			buf.append(Integer.toString(finallyEntry.handlerTarget.byteIndex));
		}
		return buf.toString();
	}
	
	boolean catchesThrowableSubclasses(BT_Class javaLangThrowable) {
		if(exceptionEntries != null) {
			for(int i=0; i<exceptionEntries.size(); i++) {
				BT_ExceptionTableEntry entry = exceptionEntries.elementAt(i);
				BT_Class catchType = entry.catchType;
				if(catchType != null && !catchType.equals(javaLangThrowable)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean equals(Object o) {
		if(o instanceof Span) {
			Span other = (Span) o;
			return start.byteIndex == other.start.byteIndex 
			&& stop.byteIndex == other.stop.byteIndex; 
		}
		return false;
	}
	
	public boolean intersects(Span span) {
		int thisStart = start.byteIndex;
		int otherStart = span.start.byteIndex;
		if(thisStart >= otherStart) {
			int otherStop = span.stop.byteIndex;
			boolean startsWithinOther = thisStart < otherStop;
			return startsWithinOther;
		}
		return false;
	}
}
