/*
 * Created on May 19, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.io.Serializable;

/**
 * @author sfoley
 *
 * A specifier specifies a class, method, field or resource item.  For it to be conditional
 * means that it only specifies such an item if a condition is met, which is the existence in the
 * repository of some other item.
 * @param specifierString the form of this string is the following:<br>
 * [?][conditionIdentifier:]itemIdentifier<br>
 * where  conditionalIdentifier has the form [[method|field]:]itemIdentifier<br>
 * and the optional '?' means silent, which means failure of the specifier to match an existing 
 * class, field, method, resource or other item will not generate a warning or error message<br>
 * For a conditional identifier, the absence of [method|field]: means that itemIdentifier
 * identifies a class or interface.
 * <p>
 * Item identifiers take the standard java forms of identifying classes, fields and methods.
 * See the ItemIdentifier class for details.
 * <p>
 * A specifier is resolvable if all its contained identifiers are resolvable.
 */
public class Specifier implements Serializable {

	private static final char silentFail = '?';
	private static final char divider = ':';
	
	private String condition;
	private Identifier identifier;
	private String fullString;
	
	public Specifier(String specifierString) {
		this(specifierString, null);
	}
	
	public Specifier(String specifierString, String from) {
		this(specifierString, from, true);
	}
	
	public Specifier(String specifierString, boolean resolvable) {
		this(specifierString, null, resolvable);
	}
	
	public Specifier(String specifierString, String from, boolean resolvable) {
		fullString = specifierString;
		boolean isSilent = false;
		if(specifierString.length() > 0 && specifierString.charAt(0) == silentFail) {
			specifierString = specifierString.substring(1);
			isSilent = true;
		}
		int index = specifierString.lastIndexOf(divider);
		if(index > 0) {
			condition = specifierString.substring(0, index);
			specifierString = specifierString.substring(index + 1);
		}
		identifier = new Identifier(specifierString, isSilent, from, resolvable);
	}
	
	public Specifier(Identifier ident) {
		fullString = ident.toString();
		identifier = ident;
	}
	
	public Specifier append(String string) {
		return new Specifier(fullString + string, identifier.getFrom());
	}
	
	public String getFullString() {
		return fullString;
	}
	
	public String getCondition() {
		return condition;
	}
	
	public boolean isConditional() {
		return condition != null;
	}
	
	public Identifier getIdentifier() {
		return identifier;
	}
	
	public boolean isRule() {
		return identifier.isRule();
	}
	
	public String toString() {
		return fullString;
	}
	
	public boolean equals(Object o) {
		if(!(o instanceof Specifier)) {
			return false;
		}
		Specifier other = (Specifier) o;
		return (condition == null ? (other.condition == null): condition.equals(other.condition)) 
			&& identifier.equals(other.identifier);
	}
	
	public void setResolvable(boolean resolvable) {
		identifier.setResolvable(resolvable);
	}
	
	public boolean conditionIsTrue(JaptRepository repository) throws InvalidIdentifierException {
		if(!isConditional()) {
			return true;
		}
		return new ConditionalInterfaceItemCollection(repository, identifier.getFrom(), identifier.isResolvable(), condition).conditionIsTrue();
	}
}
