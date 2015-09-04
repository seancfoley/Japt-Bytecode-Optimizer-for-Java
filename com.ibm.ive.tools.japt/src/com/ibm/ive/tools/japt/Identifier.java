/*
 * Created on May 19, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import com.ibm.ive.tools.japt.PatternString.PatternStringPair;
import com.ibm.ive.tools.japt.PatternString.PatternStringQuadruple;
import com.ibm.ive.tools.japt.PatternString.PatternStringTriple;
import com.ibm.ive.tools.japt.load.ClassPathOption;
import com.ibm.jikesbt.BT_ConstantPool;
import com.ibm.jikesbt.BT_DescriptorException;
import com.ibm.jikesbt.BT_Method;


/**
 * @author sfoley
 *
 * This class represents identifiers of java classes, methods, fields and resources.
 * 
 * Such identifiers take the standard java forms of the java language and runtime, 
 * of which all of the following are examples:<br>
 * java.lang.String, java.lang.String.substring, java.lang.String.substring(),
 * java.lang.String.substring(int,int), java.lang.String.CASE_INSENSITIVE_ORDER,
 * java.lang.String[], java.lang.String.lastIndexOf(Ljava/lang/String;I)<br>
 * Item identifiers may also optionally contain wildcards that are supported by the class PatternString.
 * <p>
 * Some of the above forms are ambiguous and may represent different items.<br> 
 * eg java.lang.String.substring can potentially represent a class, several different methods, or a field.
 * However, most, if not all ambiguities can be resolved by choosing a more detailed
 * specifier, such as java.lang.String.substring(), which can represent only a single item.
 * <p>
 * There is nothing stored in an identifier object to stipulate that it must match a particular type of item.
 * The item itself may be a class, interface, enum, field, method, or any other item that can have the identifier as a name.
 * <p>
 * Constructors and static intializers can be represented by the method names <init> and <clinit> respectively.
 * <p>
 * Identifiers are distinguished as resolvable or not, affecting the set of fields and
 * methods that are identified by the identifier.
 * An identifier is resolvable if it can match methods or fields that are inherited by classes matched by the identifier.
 * Example:
 * class X {}
 * identifier I: X.wait()
 * if I is resolvable, then it will match java.lang.Object.wait(), otherwise there is no match.
 * identifier J: X.*
 * if J is resolvable, it will match all methods and fields in X and all methods and fields in java.lang.Object, otherwise
 * it will match only the single default constructor of class X.
 */
public class Identifier implements Serializable, Cloneable {

	private boolean isSilent;
	private PatternString pattern;
	private String from;
	private boolean resolvable;
	private static SeparatorChecker slashSeparatorChecker = new SeparatorChecker() {
		public boolean isSeparator(char c) {
			return c == '/';
		}
	};
	private static SeparatorChecker dotSlashSeparatorChecker = new SeparatorChecker() {
		public boolean isSeparator(char c) {
			return c == '.' || c == '/';
		}
	};
	public static SeparatorChecker noSeparatorChecker = new SeparatorChecker() {
		public boolean isSeparator(char c) {
			return false;
		}
	};
	
	/**
	 * used to answer whether a given character can be considered a separator
	 * in a class, method, field or package name.  Names usually have the '.' or '/'
	 * chars as separators, if they have separators at all.
	 * @author sfoley
	 *
	 * To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Generation - Code and Comments
	 */
	private interface SeparatorChecker {
		boolean isSeparator(char c);
	}
	
	
	public Identifier(String identifierString) {
		this(identifierString, false, null, true);
	}
	
	public Identifier(String identifierString, String from) {
		this(identifierString, false, from, true);
	}
	
	public Identifier(String identifierString, boolean resolvable) {
		this(identifierString, false, null, resolvable);
	}
	
	public Identifier(String identifierString, String from, boolean resolvable) {
		this(identifierString, false, from, resolvable);
	}
	
	public Identifier(String identifierString, boolean isSilent, String from, boolean resolvable) {
		this(new PatternString(identifierString), isSilent, from, resolvable);
	}
	
	public Identifier(PatternString identifier, String from) {
		this(identifier, false, from, true);
	}
	
	public Identifier(PatternString identifier, boolean isSilent, String from) {
		this(identifier, isSilent, from, true);
	}
	
	public Identifier(PatternString identifier, boolean isSilent, String from, boolean resolvable) {
		this.from = from;
		this.isSilent = isSilent;
		this.pattern = identifier;
		this.resolvable = resolvable;
		
	}
	
	public void setResolvable(boolean resolvable) {
		this.resolvable = resolvable;
	}
	
	public boolean isResolvable() {
		return resolvable;
	}
	
	public PatternString getPattern() {
		return pattern;
	}
	
	public String toString() {
		return pattern.getString();
	}
	
	public boolean isRule() {
		return from != null && from.toLowerCase().endsWith(ClassPathOption.GENERIC_EMBEDDED_OPTIONS);
	}
	
	/**
	 * if an identifier is silent then failure of the identifier to match an existing 
	 * class, field, method, resource or other item will not generate a warning or
	 * error message
	 */
	public boolean isSilent() {
		return isSilent;
	}
	
	/**
	 * @param silent set whether the identifier is silent
	 */
	public void setSilent(boolean silent) {
		isSilent = silent;
	}
	
	/**
	 * A descriptor of where this identifier originated from, for informative purposes only
	 * @return
	 */
	public String getFrom() {
		return from;
	}
	
	public boolean isRegularString() {
		return pattern.isRegularString();
	}
	
	public boolean isValidClassName() {
		return isValidClassName(pattern);
	}
	
	public boolean isValidPackageName() {
		return isValidPackageName(pattern);
	}
	
	public boolean isValidClassMemberName() {
		return isValidClassMemberName(pattern);
	}
	
	/**
	 * 
	 * @param name
	 * @return whether the name represents a fully qualified class name
	 */
	public static boolean isValidClassName(String name) {
		name = removeArrayBrackets(name);
		if(name == null) {
			return false;
		}
		return isValidJavaIdentifier(name, false, dotSlashSeparatorChecker);
	}
	
	/**
	 * 
	 * @param name
	 * @return whether the name represents a fully qualified package name
	 */
	public static boolean isValidPackageName(String name) {
		return isValidJavaIdentifier(name, false, dotSlashSeparatorChecker);
	}
	
	/**
	 * 
	 * @param pattern
	 * @return whether the pattern can represent a fully qualified class name
	 */
	public static boolean isValidClassName(PatternString pattern) {
		String patternString = pattern.getString();
		patternString = removeArrayBrackets(patternString);
		if(patternString == null) {
			return false;
		}
		return isValidJavaIdentifier(patternString, true, dotSlashSeparatorChecker);
	}
	
	private static boolean checkArrayBrackets(int index, String className) {
		boolean lastIsLeft = false;
		for(int i=index; i<className.length(); i++) {
			char c = className.charAt(i);
			switch(c) {
				case '[':
					if(lastIsLeft) {
						return false;
					}
					lastIsLeft = true;
					continue;
				case ']':
					if(!lastIsLeft) {
						return false;
					}
					lastIsLeft = false;
					continue;
				default:
					return false;
			}
		}
		return !lastIsLeft;
	}
	
	public static int getArrayBracketsIndex(String className) {
		int index = className.indexOf('[');
		if(index < 0) {
			index = className.indexOf(']');
			if(index < 0) {
				return -1;
			}
			else {
				return index;
			}
		}
		else {
			return index;
		}
	}
	
	public static String removeArrayBrackets(String className) {
		int index = getArrayBracketsIndex(className);
		if(index < 0) {
			return className;
		}
		if(!checkArrayBrackets(index, className)) {
			return null;
		}
		if(PatternString.isWildcard(className.charAt(index))) {
			//Note a check for a single char wildcard would be appropriate here
			return className.substring(0, index + 1);
		}
		return className.substring(0, index);
	}
	
	/**
	 * 
	 * @param pattern
	 * @return whether the pattern can represent a fully qualified package name
	 */
	public static boolean isValidPackageName(PatternString pattern) {
		String patternString = pattern.getString();
		return isValidJavaIdentifier(patternString, true, dotSlashSeparatorChecker);
	}
	
	
	
	/**
	 * 
	 * @param pattern
	 * @return whether the pattern represents a non-qualified (no class reference) 
	 * method or field name
	 */
	public static boolean isValidClassMemberName(PatternString pattern) {
		return isValidJavaIdentifier(pattern.getString(), true, noSeparatorChecker) 
			|| pattern.isMatch(BT_Method.INITIALIZER_NAME) || pattern.isMatch(BT_Method.STATIC_INITIALIZER_NAME);
	}
	
	public static boolean isValidJavaIdentifier(String name) {
		return isValidJavaIdentifier(name, false, noSeparatorChecker);
	}
	
	/**
	 * @param allowWildcards allow the use of wildcards as identified by the class PatternString
	 * @return whether the given name is a valid identifier
	 */
	public static boolean isValidJavaIdentifier(String name, boolean allowWildcards, SeparatorChecker separatorChecker) {
		int lastStartOfIdentifier = 0;
		boolean isStartOfIdentifier = true;
		int len = name.length();
		for(int i=0; i<len; i++) {
			char c = name.charAt(i);
			if(isStartOfIdentifier) {
				if(!(Character.isJavaIdentifierStart(c) || (allowWildcards && PatternString.isWildcard(c)))) {
					return false;
				}
				isStartOfIdentifier = false;
			}
			else {
				if(separatorChecker.isSeparator(c)) {
					if(isExcludedIdentifier(name.substring(lastStartOfIdentifier, i))) {
						return false;
					}
					isStartOfIdentifier = true;
					lastStartOfIdentifier = i + 1;
				}
				else if(!BT_ConstantPool.isJavaIdentifierPart(c) && !(allowWildcards && PatternString.isWildcard(c))) {
					return false;
				}
			}
		}
		return !isStartOfIdentifier && !isExcludedIdentifier(name.substring(lastStartOfIdentifier));
	}

	private static final String invalidIdentifiers[] = {
		//invalid literals:
		"null",
		"true",
		"false",
		
		//keywords
		"abstract",
		"default",
		"if",
		"private",
		"this",
		"boolean",
		"do",
		"implements",
		"protected",
		"throw",
		"break",
		"double",
		"import",
		"public",
		"throws",
		"byte",
		"else",
		"instanceof",
		"return",
		"transient",
		"case",
		"extends",
		"int",
		"short",
		"try",
		"catch",
		"final",
		"interface",
		"static",
		"void",
		"char",
		"finally",
		"long",
		"strictfp",
		"volatile",
		"class",
		"float",
		"native",
		"super",
		"while",
		"const",
		"for",
		"new",
		"switch",
		"continue",
		"goto",
		"package",
		"synchronized",
	};
	
	static private Set invalidIdentifierSet = new HashSet();
	
	static {
		for(int i=0; i<invalidIdentifiers.length; i++) {
			invalidIdentifierSet.add(invalidIdentifiers[i]);
		}
	}
	
	private static boolean isExcludedIdentifier(String name) {
		//section 2.2 of VM Spec dictates that an identifier cannot be a boolean literal, null literal or a keyword in the Java language
		return invalidIdentifierSet.contains(name);
	}
		
	/**
	 * @return each element of the returned array is four pattern strings given in order: the class name, method name, signature string and return type
	 */
	public PatternStringQuadruple[] splitAsMethodIdentifier() throws InvalidIdentifierException {
		PatternString identifier = this.pattern;
		String stringPattern = identifier.getString();
		int index = stringPattern.indexOf('(');
		PatternString nameIdentifier, sigIdentifier, retIdentifier;
		if(index == -1) {
			nameIdentifier = identifier;
			sigIdentifier = retIdentifier = PatternString.ALWAYS_MATCHES_PATTERN;
		}
		else {
			nameIdentifier = new PatternString(stringPattern.substring(0, index));
			PatternStringPair sigPair = splitSignature(stringPattern.substring(index));
			sigIdentifier = sigPair.first;
			retIdentifier = sigPair.second;
		}
		
		
		//I do not consider Class.method() an invocation of Class.method.<init>().
		
		//To change this, I would alter the stuff below to add another PatternStringPair.
		//The basic idea is that we currently search for all possible locations of '.' and split up the identifier,
		//but we could also assume there is a hidden '.' at the end and it is followed by a hidden <init>
		//to generate another possibility.  Note that this removes the possibility of the invalidIdentifierException
		//resulting from finding no occurrence of '.'
		
		//current behaviour is no, Class.method() is not an invocation of Class.method.<init>()
		PatternStringPair results[] = splitAsMemberIdentifier(nameIdentifier);
		int len = results.length;
		PatternStringQuadruple fullResults[] = new PatternStringQuadruple[len];
		for(int i=0; i<len; i++) {
			fullResults[i] = new PatternStringQuadruple(results[i], sigIdentifier, retIdentifier);
		}
		return fullResults;
	}

	private PatternStringPair[] splitAsMemberIdentifier(PatternString identifier) throws InvalidIdentifierException {
		int indices[] = identifier.lastIndexOf('.');
		int len = indices.length;
		ArrayList possibilities = new ArrayList(1);
		for(int i=0; i<len; i++) {
			PatternStringPair result = identifier.split(indices[i]);
			if(isValidClassName(result.first) && isValidClassMemberName(result.second)) {
				possibilities.add(result);
			}
		}
		if(possibilities.size() == 0) {
			throw new InvalidIdentifierException(this);
		}
		return (PatternStringPair[]) possibilities.toArray(new PatternStringPair[possibilities.size()]);
	}
	
	/**
	 * same as splitAsMemberIdentifier, but no class name involved
	 * @return three pattern strings given in order: the method name, signature string and return type
	 * @throws InvalidIdentifierException
	 */
	public PatternStringTriple splitAsMethodNameIdentifier() throws InvalidIdentifierException {
		PatternString identifier = this.pattern;
		String stringPattern = identifier.getString();
		int index = stringPattern.indexOf('(');
		PatternString nameIdentifier, sigIdentifier, retIdentifier;
		if(index == -1) {
			nameIdentifier = identifier;
			sigIdentifier = retIdentifier = PatternString.ALWAYS_MATCHES_PATTERN;
		}
		else {
			nameIdentifier = new PatternString(stringPattern.substring(0, index));
			PatternStringPair sigPair = splitSignature(stringPattern.substring(index));
			sigIdentifier = sigPair.first;
			retIdentifier = sigPair.second;
		}
		if(!isValidClassMemberName(nameIdentifier)) {
			throw new InvalidIdentifierException(this);
		}
		return new PatternStringTriple(nameIdentifier, sigIdentifier, retIdentifier);
		
	}
	
	/**
	 * @return a two dimensional array providing member and type names given a
	 * member name identifier
	 */
	public PatternStringPair[] splitAsMemberIdentifier() throws InvalidIdentifierException {
		return splitAsMemberIdentifier(pattern);
	}
	
	private PatternStringPair splitSignature(String sig) throws InvalidIdentifierException {
		if(sig == null) {
			throw new IllegalArgumentException();
		}
		if(sig.length() == 0) {
			return new PatternStringPair(PatternString.ALWAYS_MATCHES_PATTERN, PatternString.ALWAYS_MATCHES_PATTERN);
		}
		
		//we're generously forgiving if the initial '(' is missing
		int firstBracketIndex = 0;
		if(sig.charAt(0) == '(') {
			firstBracketIndex = 1;
		}
		
		//we're also forgiving if the last bracket is missing
		int lastBracketIndex = sig.lastIndexOf(')');
		if(lastBracketIndex == -1) {
			String sigString = sig.substring(firstBracketIndex);
			if(!isValidParameterString(sigString)) {
				throw new InvalidIdentifierException(this);
			}
			return new PatternStringPair(new PatternString(sigString), PatternString.ALWAYS_MATCHES_PATTERN);
		}
		String sigString = sig.substring(firstBracketIndex, lastBracketIndex);
		String retString = sig.substring(lastBracketIndex + 1);
		if(!isValidParameterString(sigString)) {
			throw new InvalidIdentifierException(this);
		}
		if(retString.length() == 0) {
			return new PatternStringPair(new PatternString(sigString), PatternString.ALWAYS_MATCHES_PATTERN);
		}
		if(!isValidReturnType(retString)) {
			throw new InvalidIdentifierException(this);
		}
		return new PatternStringPair(sigString, retString);
	}
	
	static boolean isValidParameterString(String string) {
		return isExternalArgumentString(string) || isParameterString(string) >= 0;
	}
	
	/**
	 * Determines whether a given pattern string (with wildcards) may represent a 
	 * parameter string in the sense of VM spec sections 4.3.2 and 4.3.3.  
	 * A parameter string is a sequence of types as specified by section 4.3.2.
	 * @param string
	 * @return the minimum number of elements in the parameter string, or -1 if invalid
	 */
	static int isParameterString(String string) {
		int len = string.length();
		int count = 0;
		try {
			int i=0;
			top:
			while(i < len) {
				count++;
				char c = string.charAt(i);
				if(PatternString.isWildcard(c)) {
					// must find the beginning of the next identifier
					//since the wildcard can obscure 'L' characters
					next:
					for(int j=i + 1; j<string.length(); j++) {
						char d = string.charAt(j);
						switch(d) {
							case '[':
								i++;
								continue top;
							case ';':
								if(!isValidJavaIdentifier(string.substring(i, j), true, slashSeparatorChecker)) {
									return -1;
								}
								i = j+1;
								continue top;
							default:
								if(PatternString.isWildcard(d)) {
									if(!isValidJavaIdentifier(string.substring(i, j), true, slashSeparatorChecker)) {
										return -1;
									}
									i = j;
									continue next;
								}
						}
					}
					i++;
					continue top;
				}
				BT_ConstantPool.toJavaName(string, i);
				i = BT_ConstantPool.nextSig(string, i);
			}
		} catch(BT_DescriptorException e) {
			return -1;
		} catch(IndexOutOfBoundsException e) {
			return -1;
		}
		return count;
	}
	
	static boolean isExternalArgumentString(String string) {
		StringTokenizer t = new StringTokenizer(string, ",) ");
		while (t.hasMoreTokens()) {
			String token = t.nextToken();
			//remove the array identifiers
			while(token.endsWith("[]")) {
				token = token.substring(0, token.length() - 2);
			}
			if(!isValidJavaIdentifier(token, true, dotSlashSeparatorChecker)) {
				return false;
			}
		}
		return true;
	}
	
	static boolean isValidReturnType(String s) {
		return isValidJavaIdentifier(s, true, dotSlashSeparatorChecker) || isParameterString(s) == 1;
	}
	
	public boolean equals(Object o) {
		if(!(o instanceof Identifier)) {
			return false;
		}
		Identifier other = (Identifier) o;
		return pattern.equals(other.pattern) 
			&& (resolvable == other.resolvable);
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {}
		return new Identifier(pattern, isSilent, from, resolvable);
	}
	
}
