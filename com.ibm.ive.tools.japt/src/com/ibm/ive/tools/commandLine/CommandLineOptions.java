package com.ibm.ive.tools.commandLine;

import java.util.*;

/**
 * @author sfoley
 * <p>
 * Encapsulates the options available on the command line
 */
class CommandLineOptions {

	private HashMap options = new HashMap(); //each entry will be an Option[]
	private List optionList = new LinkedList();
	private boolean caseSensitive;
	
	CommandLineOptions(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
	boolean contains(char switcher, String name) {
		String key = switcher + (caseSensitive ? name : name.toLowerCase());
		return options.containsKey(key);
	}
	
	void add(Option option) {
		String key = caseSensitive ? option.name : option.name.toLowerCase();
		Option values[] = (Option[]) options.get(key);
		
		if(values != null) {
			Option oldValues[] = values;
			int oldLength = oldValues.length;
			values = new Option[oldLength + 1];
			System.arraycopy(oldValues, 0, values, 0, oldLength);
			values[oldLength] = option;
		}
		else {
			values = new Option[] {option};
		}
		options.put(key, values);
		optionList.add(option);
		
	}
	
	Option[] getOptions(char switchCharacter, String name) {
		String key = switchCharacter + (caseSensitive ? name : name.toLowerCase());
		return (Option[]) options.get(key);
	}
	
	Option getLastMatchingOption(char switchCharacter, String name) {
		for(int i=optionList.size()-1; i>=0; i--) {
			Option option = (Option) optionList.get(i);
			if(isMatch(switchCharacter, name, option)) {
				return option;
			}
		}
		return null;
	}
	
	Option getFirstMatchingOption(char switchCharacter, String name) {
		for(int i=0; i<optionList.size(); i++) {
			Option option = (Option) optionList.get(i);
			if(isMatch(switchCharacter, name, option)) {
				return option;
			}
		}
		return null;
	}
	
	private boolean isMatch(char switchCharacter, String name, Option option) {
		if(option.isSwitchCharacter(switchCharacter)) {
			if(caseSensitive ? name.equals(option.name) : name.equalsIgnoreCase(option.name)) {
				return true;
			}
		}
		return false;
	}
}
