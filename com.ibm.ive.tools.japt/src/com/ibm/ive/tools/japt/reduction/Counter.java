package com.ibm.ive.tools.japt.reduction;

public class Counter {
	public int classCount;
	public int fieldCount;
	public int methodCount;
		
	public void reset() {
		classCount = fieldCount = methodCount = 0;
	}
	
}
