package com.ibm.ive.tools.japt;

/**
 * Thrown when a class, method or field identifier is invalid.
 * @author sfoley
 *
 */
public class InvalidIdentifierException extends Exception {

	Identifier identifier;
	
	public InvalidIdentifierException(Identifier identifier) {
		this.identifier = identifier;
	}

	public InvalidIdentifierException(Identifier identifier, String detailMessage) {
		super(detailMessage);
		this.identifier = identifier;
	}
	
	public Identifier getIdentifier() {
		return identifier;
	}

}
