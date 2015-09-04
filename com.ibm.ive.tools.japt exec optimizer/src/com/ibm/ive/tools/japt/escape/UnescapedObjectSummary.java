package com.ibm.ive.tools.japt.escape;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class UnescapedObjectSummary {
	int totalClasses;
	int totalInterfaces;
	int exceedCount;
	
	abstract void add(UnescapedObjectSet other);
	
	abstract void write(PrintWriter writer) throws IOException;
	
	public String toString() {
		StringWriter writer = new StringWriter();
		PrintWriter printer = new PrintWriter(writer);
		try {
			write(printer);
		} catch(IOException e) {}
		printer.flush();
		return writer.toString();
	}
}
