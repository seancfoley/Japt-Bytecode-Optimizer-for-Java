package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;

/**
 * A name generator that simply redistributes names that it has been provided
 * 
 * @author sfoley
 *
 */
public class UsedNameGenerator {

	private final List names = new ArrayList();
	private int index;
	
	UsedNameGenerator() {}
	
	void add(String name) {
		names.add(name);
	}
	
	void reset() {
		index = 0;
	}
	
	void removeLast() {
		if(index > 0) {
			--index;
			names.remove(index);
		}
	}
	
	/**
	 * look at the next name
	 */
	public String peekName() {
		if(index < names.size()) {
			return (String) names.get(index);
		}
		return null;
	}

	/**
	 * dispense the next name
	 */
	public String getName() {
		if(index < names.size()) {
			String ret = (String) names.get(index);
			index++;
			return ret;
		}
		return null;
	}
}
