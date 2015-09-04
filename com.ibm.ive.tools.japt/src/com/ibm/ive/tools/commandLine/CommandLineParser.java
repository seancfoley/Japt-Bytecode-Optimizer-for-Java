package com.ibm.ive.tools.commandLine;

import java.util.*;

/**
 * @author sfoley
 *
 */
public class CommandLineParser {

	protected String args[];
	protected Set switchCharacters = new HashSet();
	private CommandLineOptions options = new CommandLineOptions(false);
	public static final int MATCH_FIRST = 0;
	public static final int MATCH_LAST = 1;
	public static final int MATCH_ALL = 2;
	int policy = MATCH_LAST;
	
	public CommandLineParser(String args[]) {
		this.args = args;
	}
	
	public void setMatchingPolicy(int policy) {
		switch(policy) {
			case MATCH_FIRST:
			case MATCH_LAST:
			case MATCH_ALL:
				this.policy = policy;
				break;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	public boolean isOption(String string, int commandLineIndex) {
		return string.length() > 1 && isSwitchCharacter(string.charAt(0));
	}
	
	protected boolean isSwitchCharacter(char c) {
		return switchCharacters.contains(new Character(c));
	}
	
	public void addOption(Option option) {
		options.add(option);
		char switches[] = option.getSwitchCharacters();
		for(int i=0; i<switches.length; i++) {
			switchCharacters.add(new Character(switches[i]));
		}
	}
	
	public void addOptions(Option options[]) {
		for(int i=0; i<options.length; i++) {
			addOption(options[i]);
		}
	}
	
	public void addOptions(List options) {
		for(int i=0; i<options.size(); i++) {
			addOption((Option) options.get(i));
		}
	}
	
	/**
	 * override this method to handle strings on the command line
	 */
	protected void handleString(String string, int commandLineIndex) throws CommandLineException {}
	
	/**
	 * override this method to handle non-configured options on the command line
	 */
	protected void handleUnknownOption(char switchCharacter, String name, int commandLineIndex) throws CommandLineException {}
	
	/**
	 * parses the command line
	 */
	public void parseCommandLine() throws CommandLineException {
		
		int numArgs = args.length;
		int optionIndex = 0;
		while (optionIndex < numArgs) {
			String optionArg = args[optionIndex];
			
			if(!isOption(optionArg, optionIndex)) {
				handleString(optionArg, optionIndex);
				optionIndex++;
				continue;
			} 
			String optionName = optionArg.substring(1);
			
			//find those interested in this option
			Option matchedOptions[];
			switch(policy) {
				case MATCH_FIRST:
					Option matchedOption = options.getFirstMatchingOption(optionArg.charAt(0), optionName);
					matchedOptions = (matchedOption == null) ? null : (new Option[] {matchedOption});
					break;
				case MATCH_LAST:
					matchedOption = options.getLastMatchingOption(optionArg.charAt(0), optionName);
					matchedOptions = (matchedOption == null) ? null : (new Option[] {matchedOption});
					break;
				case MATCH_ALL:
					matchedOptions = options.getOptions(optionArg.charAt(0), optionName);
					break;
				default:
					//should never reach here since there are only 3 possible values for policy
					throw new Error();
			}
			if(matchedOptions == null || matchedOptions.length == 0) {
				handleUnknownOption(optionArg.charAt(0), optionName, optionIndex);
				optionIndex++;
				continue;
			}
			
			//count the maximum number of arguments
			int maxArgs = matchedOptions[0].argCount;
			for(int i=1; i<matchedOptions.length; i++) {
				int nextCount = matchedOptions[i].argCount;
				if(nextCount > maxArgs) {
					maxArgs = nextCount;
				}
			}
			
			//get the arguments
			int argIndex = optionIndex + 1;
			LinkedList argList = new LinkedList();
			for(int i=0; i != maxArgs; argIndex++, i++) {
				if(argIndex == numArgs) {
					break;
				}
				String currentArg = args[argIndex];
				if(isOption(currentArg, argIndex)) {
					break;
				}
				argList.add(currentArg);	
			}
			String args[] = (String[]) argList.toArray(new String[argList.size()]);
			
			//handle the option
			for(int i=0; i<matchedOptions.length; i++) {
				try {
					matchedOptions[i].handle(args, this);
				}
				catch(OptionException e) {
					reportOptionException(e);
				}
			}
			optionIndex = argIndex;
		}
		
	}
	
	protected void reportOptionException(OptionException e) throws CommandLineException {}

}
