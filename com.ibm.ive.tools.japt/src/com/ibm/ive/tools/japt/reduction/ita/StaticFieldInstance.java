package com.ibm.ive.tools.japt.reduction.ita;



/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class StaticFieldInstance extends FieldInstance {
	
	StaticFieldInstance(Field field) {
		super(field);
		if(!field.isStatic()) {
			throw new IllegalArgumentException();
		}
	}
	
	void setAccessed() {
		if(isAccessed()) {
			return;
		}
		super.setAccessed();
		field.getDeclaringClass().setInitialized();
	}
	
	public String toString() {
		return "static " + super.toString();
	}
}
