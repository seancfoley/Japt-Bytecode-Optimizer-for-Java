/*
 * Created on Jun 7, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ConditionalInterfaceItemCollection extends InterfaceItemCollection {

	private static final char divider = ':';
	public static final BT_Method[] emptyMethods = new BT_Method[0];
	public static final BT_Field[] emptyFields = new BT_Field[0];
	public static final BT_Class[] emptyClasses = new BT_Class[0];
	
	private BT_Method[] conditionalMethods;
	private BT_Field[] conditionalFields;
	private BT_Class[] conditionalClasses;
	
	
	/**
	 * 
	 */
	public ConditionalInterfaceItemCollection(
			JaptRepository repository, 
			String from, 
			boolean resolvable, 
			String condition)
				throws InvalidIdentifierException {
		super(repository.getFactory());
		
		if(condition == null) {
			throw new InvalidIdentifierException(new Identifier(condition));
		}
		int index = condition.indexOf(divider);
		
		String conditionQualifier;
		if(index >= 0) {
			conditionQualifier = condition.substring(0, index);
		}
		else {
			conditionQualifier = "class";
		}
		String conditionString = condition.substring(index + 1);
		if(!conditionQualifier.equalsIgnoreCase("class")) {
			MemberCollectorActor actor = new MemberCollectorActor();
			//the qualifier can be either method or field
			Identifier conditionIdentifier = new Identifier(conditionString, true, from, resolvable);
			if(conditionQualifier.equalsIgnoreCase("method")) {
				repository.findMethods(conditionIdentifier, actor);
				conditionalMethods = actor.methods.toArray();
				conditionalFields = emptyFields;
			}
			else { //conditionQualifier is "field"
				repository.findFields(conditionIdentifier, actor);
				conditionalFields = actor.fields.toArray();
				conditionalMethods = emptyMethods;
			}
			conditionalClasses = emptyClasses;
		}
		else {
			Identifier conditionIdentifier = new Identifier(conditionString, true, from, resolvable);
			conditionalClasses = repository.findClasses(conditionIdentifier, false, true).toArray();
			conditionalFields = emptyFields;
			conditionalMethods = emptyMethods;
		}
	}
	
	public ConditionalInterfaceItemCollection(
			JaptRepository repository, 
			BT_Method[] conditionalMethods, 
			BT_Field[] conditionalFields, 
			BT_Class[] conditionalClasses) {
		super(repository.getFactory());
		this.conditionalMethods = conditionalMethods;
		this.conditionalClasses = conditionalClasses;
		this.conditionalFields = conditionalFields;
	}
	
	boolean hasNoConditions() {
		return conditionalMethods.length == 0
			&& conditionalFields.length == 0
			&& conditionalClasses.length == 0;
	}
	
	public BT_Class[] getConditionalClasses() {
		return (BT_Class[]) conditionalClasses;
	}
	
	public BT_Method[] getConditionalMethods() {
		return (BT_Method[]) conditionalMethods;
	}
	
	public BT_Field[] getConditionalFields() {
		return (BT_Field[]) conditionalFields;
	}
	
	public boolean satisfiesCondition(BT_Member member) {
		if(member instanceof BT_Method) {
			return satisfiesCondition((BT_Method) member);
		}
		else if(member instanceof BT_Field) {
			return satisfiesCondition((BT_Field) member);
		}
		return false;
	}
	
	public boolean satisfiesCondition(BT_Method meth) {
		for(int i=0; i<conditionalMethods.length; i++) {
			if(conditionalMethods[i].equals(meth)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean satisfiesCondition(BT_Field field) {
		for(int i=0; i<conditionalFields.length; i++) {
			if(conditionalFields[i].equals(field)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean satisfiesCondition(BT_Class clazz) {
		for(int i=0; i<conditionalClasses.length; i++) {
			if(conditionalClasses[i].equals(clazz)) {
				return true;
			}
		}
		return false;
	}
		
	public boolean conditionIsTrue() {
		return conditionalMethods.length > 0 || conditionalFields.length > 0 || conditionalClasses.length > 0;
	}
	
	/**
	 * @param i the only array element not to copy
	 * @param orig the source
	 * @param temp the destination
	 */
	private static void copyArray(int i, Object[] orig, Object[] temp) {
		//first copy up to the ith element
		if(i > 0) {
			System.arraycopy(orig, 0, temp, 0, i);
		}
		//now copy after the ith element
		if(i < orig.length - 1) {
			System.arraycopy(orig, i+1, temp, i, orig.length - i - 1);
		}
	}

	
	public void removeFromInterface(BT_Class clazz, boolean checkConditionals) {
		super.removeFromInterface(clazz);
		if(checkConditionals) {
			for(int i=0; i<conditionalClasses.length; i++) {
				if(conditionalClasses[i].equals(clazz)) {
					if(conditionalClasses.length == 1) {
						conditionalClasses = emptyClasses;
					}
					else {
						BT_Class[] tempClasses = new BT_Class[conditionalClasses.length - 1];
						copyArray(i, conditionalClasses, tempClasses);
						conditionalClasses = tempClasses;
					}
				}
			}
		}
	}
	
	
	public void removeFromInterface(BT_Method meth, boolean checkConditionals) {
		super.removeFromInterface(meth);
		if(checkConditionals) {
			for(int i=0; i<conditionalMethods.length; i++) {
				if(conditionalMethods[i].equals(meth)) {
					if(conditionalMethods.length == 1) {
						conditionalMethods = emptyMethods;
					}
					else {
						BT_Method[] temp = new BT_Method[conditionalMethods.length - 1];
						copyArray(i, conditionalMethods, temp);
						conditionalMethods = temp;
					}
				}
			}
		}
	}
	
	public void removeFromInterface(BT_Field field, boolean checkConditionals) {
		super.removeFromInterface(field);
		if(checkConditionals) {
			for(int i=0; i<conditionalFields.length; i++) {
				if(conditionalFields[i].equals(field)) {
					if(conditionalFields.length == 1) {
						conditionalFields = emptyFields;
					}
					else {
						BT_Field[] temp = new BT_Field[conditionalFields.length - 1];
						copyArray(i, conditionalFields, temp);
						conditionalFields = temp;
					}
				}
			}
		}
	}
	
}
