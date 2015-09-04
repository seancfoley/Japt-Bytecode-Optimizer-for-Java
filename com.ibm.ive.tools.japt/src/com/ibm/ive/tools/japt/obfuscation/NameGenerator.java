package com.ibm.ive.tools.japt.obfuscation;

import java.util.Arrays;

/**
 * @author sfoley
 *
 * <p>Performs name generation for java identifiers as part of reduction</p>
 *
 * <p>Each package will have a NameGenerator for classes and subpackages.
 * Each class will have a NameGenerator for fields and another one for methods</p>
 */
class NameGenerator {
	private static char caseInsensitiveCharsStart[] = {'a','b','c','d','e','f','g','h',
				'i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','_', 
				'$'
				};
				
	private static char caseInsensitiveCharsPart[] = {'a','b','c','d','e','f','g','h',
				'i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','_', 
				'$',
				'0','1','2','3','4','5','6','7','8','9'};
				
	private static char caseSensitiveCharsStart[] = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N',
				'O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h',
				'i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','_', 
				'$'
				};
				
	private static char caseSensitiveCharsPart[] = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N',
				'O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h',
				'i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','_', 
				'$',
				'0','1','2','3','4','5','6','7','8','9'};
	
	static {
		//needed for binary search
		Arrays.sort(caseSensitiveCharsStart);
		Arrays.sort(caseSensitiveCharsPart);
		Arrays.sort(caseInsensitiveCharsStart);
		Arrays.sort(caseInsensitiveCharsPart);
	}
	
	private char javaIdentifierStart[];
	private char javaIdentifierPart[]; 
	private String previousName;
	private String cachedName;
	
	/**
	 * equivalent to NameGenerator(null, false);
	 */
	NameGenerator() {
		this(null, false);
	}
	
	/**
	 * equivalent to NameGenerator(null, caseInsensitive);
	 */
	NameGenerator(boolean caseSensitive) {
		this(null, caseSensitive);			
	}
	
	/**
	 * equivalent to NameGenerator(baseName, false);
	 */
	NameGenerator(String baseName) {
		this(baseName, false);
	}
	
	/**
	 * @param baseName the lexicrographic base for all generated names, can be null for default behaviour of maximum compression
	 * @param caseInsensitive generate case insensitive names, e.g. not both 'A' and 'a'
	 */
	NameGenerator(String baseName, boolean caseSensitive) {
		previousName = baseName;
		if(caseSensitive) {
			javaIdentifierStart = caseSensitiveCharsStart;
			javaIdentifierPart = caseSensitiveCharsPart;
		}
		else {
			javaIdentifierStart = caseInsensitiveCharsStart;
			javaIdentifierPart = caseInsensitiveCharsPart;
		}
		
	}
	
	/** 
	 * Increments the character at the specified index in chars using characters from sourceChars.
	 * If the character can be incremented, true is returned.
	 * If the character can no longer be incremented, it is set to the first character and false is returned.
	 */
	private boolean incrementCharacter(char chars[], int index, char sourceChars[]) {
		int sourceCharIndex = (Arrays.binarySearch(sourceChars, chars[index]) + 1) % sourceChars.length;
		chars[index] = sourceChars[sourceCharIndex];
		if(sourceCharIndex == 0) {
			return false;
		}
		return true;
	}
	
	/**
	 * builds the next name based on the saved previousName, by incrementing the last character or extending the name if necessary
	 */
	private String getNextName() {
		int previousLength = previousName.length();
		char nextName[] = new char[previousLength + 1];
		int nameIndex = previousLength - 1;
		previousName.getChars(0, previousLength, nextName, 0);
		
		while(!incrementCharacter(nextName, nameIndex, (nameIndex == 0) ? javaIdentifierStart : javaIdentifierPart)) {
			if(nameIndex == 0) {
				//must extend the length of the name, since we've exhausted all possibilities at the current length
				nextName[previousLength] = javaIdentifierPart[0];
				return new String(nextName);
			}
			nameIndex--; //could not increment the current character, try to increment the previous character
		}
		return new String(nextName, 0, previousLength);
	}
	
	/** 
	 * dispenses names in ascending numerical order.
	 * For example, the following is a case-sensitive ascending numerical order: 
	 * "$", "A", "B", ..., "y", "z", 
	 * "$$", "$0", "$1", ..., "$y", "$z", 
	 * "A$", "A0", ..., "Az", ..., "z$", "z0", ...,  "zy", "zz",  
	 * "$$$", "$$0", ... ad infinitum
	 */
	public String getName() {
		if(cachedName != null) {
			String result = cachedName;
			cachedName = null;
			return result;
		}
		else {
			return generateNextName();
		}
	}	
	
	/**
	 * see what the next name will be before dispensing it
	 */
	public String peekName() {
		if(cachedName != null) {
			return cachedName;
		}
		else {
			return cachedName = generateNextName();
		}
	}
	
	private String generateNextName() {
		if(previousName == null || previousName.length() == 0) {
			return previousName = String.valueOf(javaIdentifierStart[0]);
		}
		return previousName = getNextName();
	}
	
	/**
	 * start from the first name again
	 */
	public void reset() {
		previousName = cachedName = null;
	}
}
