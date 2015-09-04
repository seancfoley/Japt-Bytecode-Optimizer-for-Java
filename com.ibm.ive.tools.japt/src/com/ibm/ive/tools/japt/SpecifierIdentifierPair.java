/*
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

/**
 * @author sfoley
 *
 * Maps a given identifier to a specifier or vice versa
 */
public class SpecifierIdentifierPair {

	public Specifier specifier;
	public Identifier identifier;
	
	/**
	 * 
	 */
	public SpecifierIdentifierPair(Specifier specifier, Identifier identifier) {
		this.specifier = specifier;
		this.identifier = identifier;
	}
	
	public String toString() {
		return specifier + ": " + identifier;
	}

}
