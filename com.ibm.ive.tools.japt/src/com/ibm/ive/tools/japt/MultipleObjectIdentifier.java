/*
 * Created on Oct 23, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.util.LinkedList;



/** 
 * Identifies multiple objects and their configurations.
 * 
 * The format is:
 * o1,o2,o3
 * where o1,o2 and o3 are object identifiers
 */
public class MultipleObjectIdentifier {
	public final ObjectIdentifier objectIdents[];
	
	MultipleObjectIdentifier(ObjectIdentifier ids[]) {
		this.objectIdents = ids;
	}
	
	/**
	 * Identifies an object or multiple objects and their configuration(s)
	 * @author sfoley
	 *
	 * The format is 
	 * i1{m1}f2{m2}f3{m3}
	 * where i1 is a full qualified field identifier, 
	 * f2 and f3 are field identifiers (name only, no class name) and m1, m2 and m3 are multiple object identifiers
	 * or 
	 * i2{m4}
	 * where i2 is a fully qualified field identifier and m4 is a multiple object identifier
	 * or 
	 * i3
	 * where i3 is a qualified class identifier.
	 * 
	 * A field identifier is fully qualified if it specifies the qualified class name.
	 * A class identifier is qualified it is specifies the package name.
	 */
	public static class ObjectIdentifier {
		public final Identifier identifier;
    	
    	public ObjectIdentifier(Identifier ident) {
    		this.identifier = ident;
    	}
    	
    	boolean hasFields() {
    		return false;
    	}
    }
    
    public static class ObjectAndFieldsIdentifier extends ObjectIdentifier {
    	private static FieldIdentifier emptyFields[] = new FieldIdentifier[0];
    	public final MultipleObjectIdentifier initialFieldContents;
    	public final FieldIdentifier[] fields;
    	
    	public ObjectAndFieldsIdentifier(
    			Identifier ident,
    			MultipleObjectIdentifier initial,
    			FieldIdentifier fields[]) {
    		super(ident);
    		this.initialFieldContents = initial;
    		this.fields = fields;
    	}
    	
    	public ObjectAndFieldsIdentifier(Identifier ident, MultipleObjectIdentifier initial) {
    		this(ident, initial, emptyFields);
    	}
    	
    	boolean hasFields() {
    		return true;
    	}
    	
    	boolean hasMultipleFields() {
    		return fields.length > 0;
    	}
    }
    
    public static class FieldIdentifier {
    	public final Identifier field;
    	public final MultipleObjectIdentifier fieldContents;
    	
    	public FieldIdentifier(Identifier field, MultipleObjectIdentifier fieldContents) {
    		this.field = field;
    		this.fieldContents = fieldContents;
    	}
    }
    
	static MultipleObjectIdentifier parse(String s) 
			throws InvalidSpecException {
    	//String objectSpec;
    	int startOfSpecIndex;
    	int endOfSpecIndex = -1;
    	LinkedList specs = new LinkedList();
    	do {
    		startOfSpecIndex = endOfSpecIndex + 1;
    		endOfSpecIndex = s.indexOf(',', startOfSpecIndex);
    		String specString;
    		if(endOfSpecIndex >= 0) {
    			specString = s.substring(startOfSpecIndex, endOfSpecIndex);
    		} else {
    			specString = s.substring(startOfSpecIndex);
    		}
    		ObjectIdentifier objectSpec = parseSpecString(specString);
    		specs.add(objectSpec);
    	} while(endOfSpecIndex >= 0);
    	ObjectIdentifier ospecs[] = (ObjectIdentifier[]) specs.toArray(new ObjectIdentifier[specs.size()]);
    	return new MultipleObjectIdentifier(ospecs);
    }
    
    //format is Object.f1{}f2{}f3{} or just Object
    //the places where we construct Identifiers can have wildcads, which means not the brackets but everything else
    private static ObjectIdentifier parseSpecString(String s) throws InvalidSpecException {
    	int bracketIndex = s.indexOf('{');
    	if(bracketIndex < 0) {
    		return new ObjectIdentifier(new Identifier(s));
    	} 
    	if(bracketIndex < 3) {
    		throw new InvalidSpecException();
    	}
    		
		int nextBracket = findClosingBracket(s, bracketIndex);
		MultipleObjectIdentifier firstFieldSpec = 
			parse(s.substring(bracketIndex + 1, nextBracket));
		String fieldString = s.substring(0, bracketIndex);
		
		//	Object.f1
		Identifier fieldIdentifier = new Identifier(fieldString);
		if(nextBracket + 1 == s.length()) {
			ObjectAndFieldsIdentifier nestedSpec = 
				new ObjectAndFieldsIdentifier(fieldIdentifier, firstFieldSpec);
			return nestedSpec;
		} else {
			LinkedList linkedList = new LinkedList();
			do {
				int startIndex = nextBracket + 1;
				bracketIndex = s.indexOf('{', startIndex);
		    	if(bracketIndex < (3 - startIndex)) {
		    		throw new InvalidSpecException();
		    	}
		    	nextBracket = findClosingBracket(s, bracketIndex);
		    	MultipleObjectIdentifier mSpec = 
		    		parse(s.substring(bracketIndex + 1, nextBracket));
		    	fieldString = s.substring(startIndex, bracketIndex);
		    	
		    	//now we have a field id and nested spec pair
		    	linkedList.add(new FieldIdentifier(new Identifier(fieldString), mSpec));
				if(nextBracket + 1 == s.length()) {
					break;
				} else if(nextBracket + 4 > s.length()) {
					throw new InvalidSpecException();
				}
			} while(true);
			
			//we are going to construct a list of ClassSpecWithFields objects
			FieldIdentifier sets[] = (FieldIdentifier[]) linkedList.toArray(new FieldIdentifier[linkedList.size()]);
			ObjectAndFieldsIdentifier ident = new ObjectAndFieldsIdentifier(
				fieldIdentifier,
				firstFieldSpec,
				sets);
			
			return ident;
		}
    }

    private static int findClosingBracket(String s, int openBracketIndex) throws InvalidSpecException {
    	int openCount = 0;
    	int index = openBracketIndex;
    	while(true) {
    		index++;
    		if(index == s.length()) {
    			throw new InvalidSpecException();
    		}
    		char c = s.charAt(index);
    		switch(c) {
    			case '{':
    				openCount++;
    				break;
    			case '}':
    				if(openCount == 0) {
    					if(index == openBracketIndex + 1) {
    						throw new InvalidSpecException();
    					}
    					return index;
    				}
    				openCount--;
    				break;
    			default:
    				break;
    		}
    	}
    }
    
    static class InvalidSpecException extends Exception {}
    
	
}
