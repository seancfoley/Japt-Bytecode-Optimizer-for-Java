package com.ibm.ive.tools.japt.assembler;

/**
 * @author Chris Laffra
 *
 * Something unexpected was found in an assembly file
 */
public class UnexpectedTokenException extends Exception {

	int line;
	
	public UnexpectedTokenException(int line) {
		this.line = line;
	}
	
	public UnexpectedTokenException(int line, String detailMessage) {
		super(detailMessage);
		this.line = line;
	}

}
