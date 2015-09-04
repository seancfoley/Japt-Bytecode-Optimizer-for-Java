/*
 * Created on Sep 17, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.commandLine;

import java.util.ArrayList;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ArgumentOption extends Option {

	ArrayList parametrizedDescriptions = new ArrayList();
	
	/**
	 * 
	 * @author sfoley
	 *
	 * represents a special format that this option may take.  
	 * For example, an option may take on a selection of three ParametrizedDescriptions:
	 * -debugLevel 0
	 * -debugLevel 1
	 * -debugLevel 2
	 * or the argument may make for a series of parametrized descriptions:
	 * -person name=xxx
	 * -person gender=xxx
	 *
	 * For each such argument format, a specialized description string is provided.
	 * 
	 * This is mainly for use in writing usage and/or help strings.
	 */
	public class ParametrizedDescription {
		String defaultArgs[];
		String description;
		
		private ParametrizedDescription(String defaultArgs[], String description) {
			this.defaultArgs = defaultArgs;
			this.description = description;
		}
		
		public String toString() {
			return ArgumentOption.this.toString(defaultArgs);
		}
		
		public String getDescription() {
			return description;
		}
	}
	
	public ArgumentOption(String name, String description, int argCount,
			char switchCharacters[]) {
		super(name, description, argCount, switchCharacters);
	}
	
	public ArgumentOption(String name, String description, int argCount,
			char switchCharacter) {
		super(name, description, argCount, switchCharacter);
	}

	public ArgumentOption(String name, String description, int argCount) {
		super(name, description, argCount);
	}
	
	public ArgumentOption(String name, int argCount) {
		super(name, argCount);
	}

	public ParametrizedDescription getDescription(int index) {
		return (ParametrizedDescription) parametrizedDescriptions.get(index);
	}
	
	public void addParametrizedDescription(String defaultArgs[], String description) {
		parametrizedDescriptions.add(new ParametrizedDescription(defaultArgs, description));
	}

}
