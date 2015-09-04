package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.jikesbt.*;

/**
 * Represents a member of a class.  Used to keep track of objects propagating from one member
 * to another through various iterations.
 * 
 * @author sfoley
 *
 */
abstract class Member {

	/**
	 * this member is required.  This means that the member is required as a part of the class, but
	 * it does not mean that the member is ever accessed.  Virtual methods can be required but not
	 * accessed.  Methods which implement interfaces may be required but never accessed.  Such members may be
	 * made abstract if the class itself is never instantiated.  Otherwise the body might be converted to a simple
	 * return statement.
	 */
	private static final short isRequired = 0x1;
	
	/**
	 * this member is accessed.  Note that any accessed member is also required.
	 */
	private static final short isAccessed = 0x2;
	
	/**
	 * used to store booleans as bits
	 */
	protected short flags;
	
	
	
	/**
	 * the class that declares this member
	 */
	Clazz declaringClass;
	
	
	
	Member(Clazz declaringClass) {
		this.declaringClass = declaringClass;
	}
	
	public Clazz getDeclaringClass() {
		return declaringClass;
	}
	
	public BT_Class getBTClass() {
		return declaringClass.getBTClass();
	}
	
	public boolean isRequired() {
		return (flags & isRequired) != 0;
	}
	
	public boolean isAccessed() {
		return (flags & isAccessed) != 0;
	}
	
	public void setRequired() {
		if(isRequired()) {
			return;
		}
		flags |= isRequired;
		declaringClass.setRequired();
	}
	
	public void setAccessed() {
		if(isAccessed()) {
			return;
		}
		flags |= isAccessed;
		setRequired();
	}	
	
	boolean isDeclaringType(BT_Class type) {
		return getRepository().isCompatibleType(declaringClass.getBTClass(), type);
	}
	
	public Repository getRepository() {
		return declaringClass.repository;
	}	
	
	
}
