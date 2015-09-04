package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;
import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;

/**
 * @author sfoley
 * <p>
 * Obtains names from the constant pool of a class in reverse order of length
 */
class ConstantPoolNameGenerator {

	private static final int MAX_NAME_LENGTH = 50;
	private LinkedList stringList;
	private int lowIndex;
	private int highIndex;
	private int lastIndex = -1;
	
	static LinkedList getStringList(BT_Class clazz) {
		LinkedList stringList = new LinkedList();
		BT_MethodVector methods = clazz.methods;
		for(int j=0; j<methods.size(); j++) {
			BT_Method method = methods.elementAt(j);
			if(method.isStub() || method.isAbstract() || method.isNative()) {
				continue;
			}
			BT_CodeAttribute code = method.getCode();
			if(code == null) {
				continue;
			}
			BT_InsVector ins = code.getInstructions();
			topLoop:
			for(int l=0; l<ins.size(); l++) {
				BT_Ins instruction = ins.elementAt(l);
				if(instruction.isLoadConstantStringIns()) {
					BT_ConstantStringIns stringIns = (BT_ConstantStringIns) instruction;
					if(!stringIns.isImmutable()) { 
						continue;
					}
					/* note that the constant folding optimization
					can render this optimization invalid
				    see BT_ConstantStringIns for the constant folding optimization
				    */
					String string = stringIns.getValue();
					int len = string.length();
					
					/*
					 * It doesn't matter what the length of the string is in terms
					 * of the class file format, but we should not generate really long
					 * names because they will likely be inefficient at runtime. 
					 */
					if(len > MAX_NAME_LENGTH) {
						continue;
					}
					
					if(!Identifier.isValidJavaIdentifier(string)) {
						continue;
					}
					
					/* there is an inconsistency between whether certain characters
					 * are valid identifiers across platforms. For example, 0x2118
					 * is a valid character in Foundation 1.0 but not Foundation 1.1, at least
					 * in the IBM J9 J2ME implementation.
					 * 
					 * Therefore we restrict the re-use of strings to one-byte characters, for which
					 * there are absolutely no inconsistencies.
					 * 
					 * The first 256 Code Points (0-255) of Unicode are exactly same as ISO8859-1.
					 * 
					 * See bugzilla 116904
					 * 
					 * For the '-' character, this character is included in class names for classes
					 * produced by javadoc (e.g. package-info), so this character is allowed in loaded classes,
					 * but some VMs do not accept the use of this character, so we disallow the use here.
					 */
					for(int k=0; k<len; k++) {
						char c = string.charAt(k);
						if(c >= 256 || c == '-') {
							continue topLoop;
						}
					}
					
					stringList.add(string);
				}
			}
		}
		return stringList;
	}
	
	/**
	 * Constructor for ConstantPoolNameGenerator.
	 */
	public ConstantPoolNameGenerator(BT_Class clazz) throws BT_ClassFileException {
		stringList = getStringList(clazz);
		if(stringList.size() > 1) {
		
			/**
			 * put the long strings first, because the shortest strings are given out on demand.
			 * 
			 * Class files contain UTF8 strings so it is the UTF8 length we are worried about.
			 */
			Collections.sort(stringList, new Comparator() {
					public int compare(Object o1, Object o2) {
						String s1 = (String) o1;
						String s2 = (String) o2;
						int s1Length = getUTF8Length(s1);
						int s2Length = getUTF8Length(s2);
						if(s1Length > s2Length) {
							return -1;
						}
						else if(s1Length < s2Length) {
							return 1;
						}
						return s1.compareTo(s2);
					}
					
					public boolean equals(Object obj) {
						return obj.getClass().equals(getClass());
					}
				}
			);
		}
		reset();
	}

	
	private static int getUTF8Length(String string) {
		return UTF8Converter.convertToUtf8(string).length;
	}
	
	/**
	 * take a look at the next name without dispensing it
	 */
	public String peekLongestName() {
		if(lowIndex <= highIndex) {
			String string = (String) stringList.get(lowIndex);
			return string;
		}
		return null;
	}
	
	/**
	 * dispense the next name
	 */
	public String getLongestName() {
		if(lowIndex <= highIndex) {
			lastIndex = lowIndex;
			lowIndex++;
			String string = (String) stringList.get(lastIndex);
			return string;
		}
		return null;
	}
	
	/**
	 * remove the last name obtained from the generator by a get method from the generator
	 */
	public void removeLast() {
		if(lastIndex >= 0) {
			if(lastIndex <= highIndex) {
				highIndex--;
				if(lastIndex <= lowIndex) {
					lowIndex--;
				}
			}
			stringList.remove(lastIndex);
			lastIndex = -1;
		}
	}
	
	/**
	 * take a look at the next name without dispensing it
	 */
	public String peekShortestName() {
		if(highIndex >= lowIndex) {
			String string = (String) stringList.get(highIndex);
			return string;
		}
		return null;
	}
	
	/**
	 * dispense the next name
	 */
	public String getShortestName() {
		if(highIndex >= lowIndex) {
			lastIndex = highIndex;
			highIndex--;
			String string = (String) stringList.get(lastIndex);
			return string;
		}
		return null;
	}
	
	public void reset() {
		lowIndex = 0;
		highIndex = stringList.size() - 1;
	}
	

}
