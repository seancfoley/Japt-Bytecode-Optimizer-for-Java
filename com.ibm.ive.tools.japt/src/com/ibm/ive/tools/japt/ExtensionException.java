package com.ibm.ive.tools.japt;

/**
 * 
 * Thrown when an extension failed to complete its class and resource manipulations
 *
 * @author sfoley
 */
public class ExtensionException extends Exception {
	public Extension extension;
	
	public ExtensionException(Extension extension) {
		this.extension = extension;
	}

	public ExtensionException(Extension extension, String detailMessage) {
		super(detailMessage);
		this.extension = extension;
	}

}
