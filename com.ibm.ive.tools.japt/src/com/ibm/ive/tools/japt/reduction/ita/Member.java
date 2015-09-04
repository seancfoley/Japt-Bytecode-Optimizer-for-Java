package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_Member;

/**
 * Represents a member of a class.  Used to keep track of objects propagating from one member
 * to another through various iterations.
 * 
 * @author sfoley
 *
 */
public abstract class Member implements Comparable {

	/**
	 * this member is required.  This means that the member is required as a part of the class, but
	 * it does not mean that the member is ever accessed.  
	 * 
	 * Virtual methods can be required but not
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
	protected short flags;//the first byte is used internally, the higher byte is for external use
	
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
	
	public byte getContextFlags() {
		return (byte) (flags >> 8);
	}
	
	public void setContextFlags(int flags) {
		this.flags &= 0x00ff;
		this.flags |= flags << 8;
	}
	
	boolean isRequired() {
		return (flags & isRequired) != 0;
	}
	
	boolean isCalled() {
		return (flags & isAccessed) != 0;
	}
	
	void setRequired() {
		if(isRequired()) {
			return;
		}
		flags |= isRequired;
		declaringClass.setRequired();
	}
	
	void setAccessed() {
		if(isCalled()) {
			return;
		}
		flags |= isAccessed;
		setRequired();
	}
	
	public boolean isStatic() {
		return getMember().isStatic();
	}
	
	boolean isFinal() {
		return getMember().isFinal();
	}
	
	
	public String toString() {
		return getMember().toString();
	}
	
	Repository getRepository() {
		return declaringClass.repository;
	}
	
	abstract BT_Member getMember();
	
	public int compareTo(Object other) {
		Member otherMember = (Member) other;
		return getMember().compareTo(otherMember.getMember());
	}
	
	public boolean isDefaultAccess() {
		return getMember().isDefaultAccess();
	}
	
	public boolean useIntraProceduralAnalysis() {
		return getRepository().getPropagationProperties().useIntraProceduralAnalysis();
	}
}
