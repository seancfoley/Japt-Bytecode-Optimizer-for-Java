/*
 * Created on Feb 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

/**
 * @author sfoley
 *
 * Describes a string that contains indicators for arguments.
 * 
 * For example:
 * 
 * "the {1} was walking down the street with {0} friends"
 * Possible arguments include: {"two", "girl"}, {"many", "shopper"}
 */
public class FormattedString {

	private static final int staticArgumentNumber[] = new int[0];
	private String components[];
	private int argumentNumber[];
	
	/**
	 * Construct a formatted string object from a string that contains
	 * markers of the form {x} where x = 0, 1, 2, ... and the marker
	 * {x} indicates the location of argument x.
	 * @param string
	 */
	public FormattedString(String string) {
		if(string == null) {
			throw new NullPointerException();
		}
		format(string);
	}
	
	/**
	 * Constructs a formatted string in which the string has already been
	 * parsed into components and the arguments occur in order, i.e. the ith
	 * argument occurs after the ith component.
	 * 
	 * @param components
	 */
	public FormattedString(String components[]) {
		for(int i=0; i<components.length; i++) {
			if(components[i] == null) {
				throw new NullPointerException();
			}
		}
		this.components = components;
	}
	
	static int findLargestArgument(String string) {
		int largest = -1;
		int bracketIndex = 0;
		do {
			bracketIndex = string.indexOf('{', bracketIndex);
			if(bracketIndex >= 0) {
				bracketIndex++;
				int nextIndex = string.indexOf('}', bracketIndex + 1);
				if(nextIndex > 0) {
					String numString = string.substring(bracketIndex, nextIndex);
					try {
						int num = Integer.parseInt(numString);
						largest = Math.max(num, largest);
					} catch(NumberFormatException e) {}
				} else {
					break;
				}
			} else {
				break;
			}
		} while(true);
		return largest;
	}
	
	private void format(String string) {
		components = new String[]{string};
		argumentNumber = staticArgumentNumber;
		int maxInt = findLargestArgument(string);
		if(maxInt < 0) {
			return;
		}
		top:
		for(int j = 0; j<=maxInt; j++) {
			StringBuffer buf = new StringBuffer(5);
			buf.append('{');
			buf.append(j);
			buf.append('}');
			String marker = buf.toString();
			for(int i=0; i<components.length; i++) {
				String componentString = components[i];
				int index = componentString.indexOf(marker);
				if(index >= 0) {
					String newComponents[] = new String[components.length + 1];
					if(i > 0) {
						System.arraycopy(components, 0, newComponents, 0, i);
					}
					newComponents[i] = componentString.substring(0, index);
					newComponents[i+1] = componentString.substring(index + 3);
					if(i + 1 < components.length) {
						System.arraycopy(components, i+1, newComponents, i+2, components.length - i - 1);
					}
					components = newComponents;
					int[] newArgumentNumber = new int[argumentNumber.length + 1];
					if(i > 0) {
						System.arraycopy(argumentNumber, 0, newArgumentNumber, 0, i);
					}
					newArgumentNumber[i] = j;
					if(i < argumentNumber.length) {
						System.arraycopy(argumentNumber, i, newArgumentNumber, i+1, argumentNumber.length - i);
					}
					argumentNumber = newArgumentNumber;
					continue top;
				} 
			}
		}
	}
	
	public String[] getComponents() {
		return components;
	}

	/**
	 * Determines the order by which arguments occur in the string.
	 * Consider the example string above, "the {1} was walking down the street with {0} friends",
	 * The first argument is {0}, the second is {1}.  So the argument at index 0 is 1 and the
	 * argument at index 1 is 0.
	 * 
	 * @return the argument at the specified index.  If there are not as many arguments as the
	 * specified index, then -1 is returned.
	 */
	public int getArgumentNumber(int argumentIndex) {
		if(argumentNumber == null) {
			return argumentIndex;
		}
		if(argumentIndex < argumentNumber.length) {
			return argumentNumber[argumentIndex];
		}
		return -1;
	}
}
