package com.ibm.ive.tools.japt;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
/**
 * A string containing wildcards and other characters that enable it
 * to represent many strings at once.
 * 
 * Like Strings, PatternStrings are immutable.
 * @author sfoley
 */
public class PatternString implements Serializable {
	private final boolean isRegularString;
	private final boolean alwaysMatches;
	private final String pattern;
	private final String tokens[];
	
	public static final char wildCard = '*';
	public static final char alternateWildCard = '~';
	public static final char separatorExcludingWildCard = '^';
	private static final int nullArray[] = new int[0];
	private static final String ALWAYS_MATCHES_STRING = String.valueOf(wildCard);
	public static final PatternString ALWAYS_MATCHES_PATTERN = new PatternString(ALWAYS_MATCHES_STRING);
	
	public PatternString(String pattern) {
		this.pattern = pattern;
		if(pattern.equals(ALWAYS_MATCHES_STRING)) {
			isRegularString = false;
			alwaysMatches = true;
			tokens = null;
			return;
		}
		else {
			alwaysMatches = false;
		}
		
		StringTokenizer tokenizer = new StringTokenizer(pattern, 
					String.valueOf(wildCard) + alternateWildCard + separatorExcludingWildCard, true);
		
		List tokenList = new LinkedList();
		boolean isReg = true;
		while(tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if(isReg) {
				switch(token.charAt(0)) {
					case alternateWildCard:
					case wildCard:
					case separatorExcludingWildCard:
						isReg = false;
				}
			}
			tokenList.add(token);
		}
		isRegularString = isReg;
		tokens = (String[]) tokenList.toArray(new String[tokenList.size()]);
	}
	
	public boolean alwaysMatches() {
		return alwaysMatches;
	}
	
	int indexOfWildcard() {
		if(isRegularString) {
			return -1;
		}
		if(alwaysMatches) {
			return 0;
		}
		int index = 0;
		for(int i=0; i<tokens.length; i++) {
			String token = tokens[i];
			switch(token.charAt(0)) {
				case alternateWildCard:
				case wildCard:
				case separatorExcludingWildCard:
					return index;
				default:
					index += token.length();
			}
		}
		//should never reach here
		return -1;
	}

	public static class PatternStringPair{
		public final PatternString first;
		public final PatternString second;
		
		PatternStringPair(PatternString first, PatternString second) {
			this.first = first;
			this.second = second;
		}
		
		PatternStringPair(String first, String second) {
			this(new PatternString(first), new PatternString(second));
		}
	}
	
	public static class PatternStringTriple extends PatternStringPair {
		public final PatternString third;
		
		PatternStringTriple(PatternString first, PatternString second, PatternString third) {
			super(first, second);
			this.third = third;
		}
		
		PatternStringTriple(PatternStringPair first, PatternString second) {
			this(first.first, first.second, second);
		}
	}
	
	public static class PatternStringQuadruple extends PatternStringTriple {
		public final PatternString fourth;
		
		PatternStringQuadruple(PatternString first, PatternString second, PatternString third, PatternString fourth) {
			super(first, second, third);
			this.fourth = fourth;
		}
		
		PatternStringQuadruple(PatternStringPair first, PatternString second, PatternString third) {
			this(first.first, first.second, second, third);
		}
		
		PatternStringQuadruple(PatternStringPair first, PatternStringPair second) {
			this(first, second.first, second.second);
		}
		
		PatternStringQuadruple(PatternStringTriple first, PatternString second) {
			this(first, first.third, second);
		}
	}
	
	/**
	 * Splits the patterns into two separate patterns, the splitting being done by the
	 * character at the indicated index, so this character is not included in either string.  
	 * If such a character is a variable length wildcard then the 
	 * new patterns will begin and end with wildcards as needed.   
	 */
	public PatternStringPair split(int index) {
		int c = pattern.charAt(index);
		if(c == wildCard || c == separatorExcludingWildCard || c == alternateWildCard) {
			return new PatternStringPair(pattern.substring(0, index + 1),
				pattern.substring(index));
		}
		return new PatternStringPair(pattern.substring(0, index), pattern.substring(index + 1));
	}
	
	public int indexOfSeparator() {
		return indexOfSeparator(pattern, 0);
	}

	public int indexOfSeparator(int index) {
		return indexOfSeparator(pattern, index);
	}
	
	/**
	 * @return an array indicating the possible first locations of the indicated character.
	 * The results are returned in ascending order.
	 */
	public int[] indexOf(int c) {
		int results[] = new int[pattern.length()];
		int totalLocations;
		int index = pattern.indexOf(c);
		if(index == -1) {
			totalLocations = findAdditionalLocations(results, pattern, c, true);
		}
		else {
			totalLocations = findAdditionalLocations(results, pattern.substring(0, index), c, true);
			results[totalLocations++] = index;
		}
		if(totalLocations == 0) {
			return nullArray;
		}
		if(totalLocations == pattern.length()) {
			return results;
		}
		int newResults[] = new int[totalLocations];
		System.arraycopy(results, 0, newResults, 0, totalLocations);
		return newResults;
	}
	
	boolean isSeparator(int c) {
		return c == '.' || c == '/';
	}
	
	private int findAdditionalLocations(int results[], String sub, int c, boolean forward) {
		int resultIndex = 0;
		boolean isNotSeparator = !isSeparator(c);
		int len = sub.length();
		for(int i=0; i<len; i++) {
			int j = (forward ? i : (len - i - 1)); 
			char ch = sub.charAt(j);
			if(ch == wildCard || ch == alternateWildCard) {
				results[resultIndex++] = j;
			}
			else if(isNotSeparator && (ch == separatorExcludingWildCard)) {
				results[resultIndex++] = j;
			}
		}
		return resultIndex;
	}
	
	public int[] lastIndexOf(int c) {
		return lastIndexOf(c, pattern.length() - 1);
	}
	
	/**
	 * @return an array indicating the possible last locations of the indicated character,
	 * listed in descending order
	 */
	public int[] lastIndexOf(int c, int fromIndex) {
		int maxResults = pattern.length();
		int results[] = new int[maxResults];
		int totalLocations;
		int index = pattern.lastIndexOf(c, fromIndex);
		int offset = index + 1;
		
		if(offset < fromIndex + 1) {
			totalLocations = findAdditionalLocations(results, pattern.substring(offset, fromIndex + 1), c, false);
			if(index >= 0) {
				if(offset > 0) {
					for(int i=0; i<totalLocations; i++) {
						results[i] += offset;
					}
				}
				results[totalLocations++] = index;
			}
			if(totalLocations == 0) {
				return nullArray;
			}
		}
		else if(index >= 0) {
			totalLocations = 1;
			results[0] = index;
		}
		else {
			return nullArray;
		}
		
		if(totalLocations == maxResults) {
			return results;
		}
		int newResults[] = new int[totalLocations];
		System.arraycopy(results, 0, newResults, 0, totalLocations);
		return newResults;
	}
	
	
	/**
	 * @return true if the string contains no wildcards, false otherwise
	 */
	public boolean isRegularString() {
		return isRegularString;
	}
	
	public static boolean isWildcard(char c) {
		return c == wildCard || c == separatorExcludingWildCard || c == alternateWildCard;
	}
	
	/**
	 * Determine if there is a match to the pattern that ends with the given string
	 * @param string
	 * @return
	 */
	public boolean endsWith(String string) {
		String reversePattern = new StringBuffer(pattern).reverse().toString();
		String reverseString = new StringBuffer(string).reverse().toString();
		return new PatternString(reversePattern).startsWith(reverseString);
	}
	
	public PatternString toLowerCase() {
		String lower = pattern.toLowerCase();
		if(pattern.equals(lower)) {
			return this;
		}
		return new PatternString(lower);
	}
	
	public PatternString append(String str) {
		if(str.length() == 0) {
			return this;
		}
		return new PatternString(pattern + str);
	}
	
	public PatternString replace(char oldChar, char newChar) {
		String replaced = pattern.replace(oldChar, newChar);
		if(replaced.equals(pattern)) {
			return this;
		}
		return new PatternString(replaced);
	}
	
	/** 
	 * Determines if the pattern matches with a given string.
	 */
	public boolean isMatch(String string) {
		return alwaysMatches || (isRegularString ? pattern.equals(string) : isMatch(string, false));
	}
	
	/**
	 * Determine if there is a match to the pattern that starts with the given string
	 * @param string
	 * @return
	 */
	public boolean startsWith(String string) {
		return alwaysMatches || (isRegularString ? pattern.startsWith(string) : isMatch(string, true));
	}
	
	
	
	class MatchState implements Cloneable {
		boolean onWildCard; 
		boolean onDotExcludingWildCard; 
		
		void reset() {
			onWildCard = onDotExcludingWildCard = false;
		}
		
		boolean matchesRemainder(String string, int stringStartIndex) {
			return (string.length() == stringStartIndex) //we have reached the end of the string
				//the end of the string is covered by a wildcard
				|| onWildCard  
				|| (onDotExcludingWildCard && (indexOfSeparator(string, stringStartIndex) == -1));
			
		}
		
		
		
		void matchWildcard(char c) {
			switch(c) {
				case alternateWildCard:
				case wildCard:
					onWildCard = true;
					return;
				case separatorExcludingWildCard:
					onDotExcludingWildCard = true;
					return;
				default:
			}
		}
		
		boolean startsWithRemainder(String string, int stringStartIndex, String token) {
			String remainderToMatch = string.substring(stringStartIndex);
			return token.startsWith(remainderToMatch);
		}
		
		
		int matchToken(String string, int stringStartIndex, int searchIndex, String token) {
			int index = string.indexOf(token, searchIndex);
			if(index < 0) {
				return -1;
			}
			switch(index - stringStartIndex) {
				default: //index > 1
					if(!onWildCard) {
						int lastDotIndex = lastIndexOfSeparator(string, index - 1);
						if(lastDotIndex >= stringStartIndex) {
							return -1;
						}
						if(!onDotExcludingWildCard) {
							return -1;
						}
					}
					//fall through
				case 0:
					break;
			}
			return index;
		}
		
		public Object clone() {
			try {
				return super.clone();
			} catch(CloneNotSupportedException e) {}
			return null;
		}
	}
	
	/* in a multi-threaded environment this field would need to go */
	private MatchState savedState = new MatchState();
	
	private boolean isMatch(String string, boolean startsWith) {
		savedState.reset();
		return isMatch(string, 0, startsWith, 0, savedState);
	}
	
	
	
	private boolean isMatch(String string, int stringStartIndex, boolean startsWith, int startTokenIndex, MatchState state) {
		for(int i=startTokenIndex; i<tokens.length; i++) {
			if(startsWith && state.matchesRemainder(string, stringStartIndex)) {
				return true;
			}
			String currentToken = tokens[i];
			char c = currentToken.charAt(0);
			if(isWildcard(c)) {
				state.matchWildcard(currentToken.charAt(0));
			}
			else {
				int stringTokenIndex = state.matchToken(string, stringStartIndex, stringStartIndex, currentToken);
				if(stringTokenIndex < 0) {
					if(startsWith) {
						return state.startsWithRemainder(string, stringStartIndex, currentToken);
					}
					return false;
				}
				else {
					int searchIndex = stringTokenIndex + 1;
					while(true) {
						//we have found the token in the string
						//now we check if we can find it again further along the string
						int nextIndex = state.matchToken(string, stringStartIndex, searchIndex, currentToken);
						if(nextIndex < 0) {
							break;
						}
						MatchState newState = (MatchState) state.clone();
						newState.reset();
						if(isMatch(string, nextIndex + currentToken.length(), startsWith, i+1, newState)) {
							return true;
						}
						searchIndex = nextIndex + 1;
					}
					state.reset();
					stringStartIndex = stringTokenIndex + currentToken.length();
				}
			}
		}
		return state.matchesRemainder(string, stringStartIndex);
	}

	/**
	 * 
	 * @param string
	 * @param beforeIndex
	 * @return the index of the last separator in the string, searching backward starting at the specified index.
	 */
	private static int lastIndexOfSeparator(String string, int fromIndex) {
		//considering that lastIndexOf is a native method in some java implementations (j9 specifically)
		//it might be best to do two separate searches instead of combining into one
		int lastDotIndex = string.lastIndexOf('.', fromIndex);
		int lastSlashIndex = string.lastIndexOf('/', fromIndex);
		return Math.max(lastDotIndex, lastSlashIndex);
	}
	
	private static int indexOfSeparator(String string, int index) {
		//considering that lastIndexOf is a native method in some java implementations (j9 specifically)
		//it might be best to do two separate searches instead of combining into one
		int dotIndex = string.indexOf('.', index);
		int slashIndex = string.indexOf('/', index);
		int min = Math.min(dotIndex, slashIndex);
		if(min < 0) {
			return Math.max(dotIndex, slashIndex);
		}
		return min;
	}
	
	/**
	 * The search for a separator does not include wildcard matches
	 * @param index the index ranging from 1 to the pattern length
	 * @return the index of the last separator in the pattern, searching backward starting at the specified index.
	 */
	public int lastIndexOfSeparator(int endIndex) {
		return lastIndexOfSeparator(pattern, endIndex);
	}
	
	/**
	 * The search for a separator does not include wildcard matches
	 * @return the index of the last separator in the pattern
	 */
	public int lastIndexOfSeparator() {
		return lastIndexOfSeparator(pattern, pattern.length() - 1);
	}

	public String getString() {
		return pattern;
	}

	public String toString() {
		return pattern;
	}
	
	static int failures = 0;
	public static void main(String args[]) {
		
		checkMatch("", "ind", "abc", false);
		checkMatch("", "x", "abc", false);
		checkMatch("", "", "abc", true);
		checkMatch("", "ind", "indind", false);
		checkMatch("", "indind", "indind", false);
		checkMatch("", "ind", "ind", false);
		checkMatch("", "x", "ind", false);
		checkMatch("", "", "ind", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "ind", "abc", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "x", "abc", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "", "abc", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "ind", "indind", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "indind", "indind", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "ind", "ind", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "x", "ind", true);
		checkMatch("" + wildCard + separatorExcludingWildCard, "", "ind", true);
		checkMatch("" + wildCard, "ind", "abc", true);
		checkMatch("" + wildCard, "x", "abc", true);
		checkMatch("" + wildCard, "", "abc", true);
		checkMatch("" + wildCard, "ind", "indind", true);
		checkMatch("" + wildCard, "indind", "indind", true);
		checkMatch("" + wildCard, "ind", "ind", true);
		checkMatch("" + wildCard, "x", "ind", true);
		checkMatch("" + wildCard, "", "ind", true);
		checkMatch("" + separatorExcludingWildCard, "ind", "abc", true);
		checkMatch("" + separatorExcludingWildCard, "x", "abc", true);
		checkMatch("" + separatorExcludingWildCard, "", "abc", true);
		checkMatch("" + separatorExcludingWildCard, "ind", "indind", true);
		checkMatch("" + separatorExcludingWildCard, "indind", "indind", true);
		checkMatch("" + separatorExcludingWildCard, "ind", "ind", true);
		checkMatch("" + separatorExcludingWildCard, "x", "ind", true);
		checkMatch("" + separatorExcludingWildCard, "", "ind", true);
		checkMatch("" + separatorExcludingWildCard, "i.nd", "abc", false);
		checkMatch("" + separatorExcludingWildCard, ".", "abc", false);
		checkMatch("" + separatorExcludingWildCard, ".ind", "ind", false);
		checkMatch("" + separatorExcludingWildCard, "ind.", "ind", false);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "ind", "abc", true);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "x", "abc", true);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "", "abc", false);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "ind", "indind", true);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "indind", "indind", true);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "ind", "ind", true);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "x", "ind", true);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "", "ind", false);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "i.nd", "abc", false);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, ".", "abc", false);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, ".ind", "ind", false);
//		checkMatch("" + separatorExcludingWildCard + separatorExcludingSingleCharWildCard, "ind.", "ind", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard, "ind", "abc", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard, "x", "abc", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard, "x", "x", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard, "", "abc", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard, ".", "ind", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, "ind", "abc", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, "x", "abc", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, "x", "x", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, "xx", "abc", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, "xx", "x", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, "", "abc", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, ".", "ind", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, ".x", "ind", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + separatorExcludingSingleCharWildCard, "x.", "ind", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "ind", "abc", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "x", "abc", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "", "abc", false);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "ind", "indind", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "indind", "indind", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "ind", "ind", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "x", "ind", true);
//		checkMatch("" + separatorExcludingSingleCharWildCard + wildCard, "", "ind", false);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "ind", "abc", true);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "x", "abc", true);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "", "abc", false);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "ind", "indind", true);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "indind", "indind", true);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "ind", "ind", true);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "x", "ind", true);
//		checkMatch("" + wildCard + separatorExcludingSingleCharWildCard, "", "ind", false);
		
		
		System.out.println("failures: " + failures);
	}
	
	static void checkMatch(String wildcard, String match, String pattern, boolean wildcardMatchesMatch) {
		checkMatch(wildcard + pattern, match + pattern, wildcardMatchesMatch);
		checkMatch(pattern + wildcard, pattern + match, wildcardMatchesMatch);
		checkMatch(pattern + wildcard + pattern, pattern + match + pattern, wildcardMatchesMatch);
	}
	
	static void checkMatch(String pattern, String string, boolean expectedResult) {
		PatternString p;
		boolean result;
		
		p = new PatternString(pattern);
		result = p.isMatch(string);
		if(result != expectedResult) {
			System.out.print("FAIL: ");
			failures++;
		}
		else {
			System.out.print("PASS: ");
		}
		System.out.println(pattern + ", " + string + ", matches: " + result);
		
		if(expectedResult) {
			String newPattern = pattern + "end";
			p = new PatternString(newPattern);
			//String newString = string + "end";
			result = p.startsWith(string);
			if(result) {
				System.out.print("PASS: ");
			}
			else {
				System.out.print("FAIL: ");
				failures++;
			}
			System.out.println(newPattern + ", " + string + ", starts with: " + result);
			
//			newString = "begin" + string;
//			result = p.startsWith(newString);
//			if(!result) {
//				System.out.print("PASS: ");
//			}
//			else if(result) {
//				System.out.print("FAIL: ");
//				failures++;
//			}
//			System.out.println(pattern + ", " + newString + ", starts with: " + result);
		}
	
	}
}
