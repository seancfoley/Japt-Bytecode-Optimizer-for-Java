package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_ClassPathEntry.BT_ClassPathLocation;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 A base class for something that can be a member of a class -- a {@link
 BT_Method} or {@link BT_Field}.

 <p> In JikesBT, nested classes are not treated as being members of another class.
 They are normal classes except that they have "$" in their names.

 * @author IBM
**/
public abstract class BT_Member extends BT_Item implements Comparable {

	/**
	 The containing class.
	 Should be accessed via {@link BT_Member#getDeclaringClass} or {@link BT_Member#resetDeclaringClass}.
	**/
	public BT_Class cls;

	/**
	 Constructs and sets {@link BT_Member#cls} to refer to the containing class.
	 The reverse pointer is not set.
	**/
	public BT_Member(BT_Class cls) {
		super();
		this.cls = cls;
	}

	public boolean isClassMember() {
		return true;
	}
	
	/**
	 Compares only names.
	 For use by {@link BT_ClassVector#sort}.
	**/
	public int compareTo(Object that) {
		BT_Member other = (BT_Member) that;
		if(this == that) {
			return 0;
		}
		int comparison = cls.compareTo(other.cls);
		if(comparison == 0) {
			comparison = getName().compareTo(other.getName());
			if(comparison == 0) {
				comparison = qualifiedName().compareTo(other.qualifiedName());
			}
		}
		return comparison;
	}
	
	public abstract void remove();
	
	/**
	 Moves and renames this method or field and updates related information.
	 Assumes and preserves <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency</a>
	 (except it won't cause special methods such as "<clinit>",
	 "<init>", "final" to become static or non-static).
	 Changes class and name simultaneously for efficiency.
	**/
	public abstract void resetDeclaringClassAndName(BT_Class c, String nm);

	/**
	 Moves this method or field to a new class and updates related information.
	 Assumes and preserves <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency</a>.
	 Must not be used to move a method from/to an internal nonstatic
	 class to/from another type of class.
	**/
	public abstract void resetDeclaringClass(BT_Class cls);

	/**
	 Gets the class this field or method is in.
	 Named like java.lang.reflect.getDeclaringClass().
	**/
	public BT_Class getDeclaringClass() {
		return cls;
	}
	
	/**
	 * returns the version of the class owning this class member.
	 */
	public BT_ClassVersion getVersion() {
		return cls.getVersion();
	}

	/**
	 Returns true if this Member is a constructor.  This base methods always returns false.  Overriden in BT_Method.
	**/
	public boolean isConstructor() {
		return false;
	}
	
	/**
	 True if this is declared protected as determined by {@link BT_Item#flags}.
	**/
	public boolean isProtected() {
		return areAnyEnabled(PROTECTED);
	}

	/**
	 True if this is declared private as determined by {@link BT_Item#flags}.
	**/
	public boolean isPrivate() {
		return areAnyEnabled(PRIVATE);
	}
	
	/**
	 * A protected non-static member can be visible in one object while the same
	 * member is not visible in another object of the same class.  For example,
	 * package x;
	 * class A {
	 * 	protected int x;
	 * }
	 * package y;
	 * class B extends A {
	 * 	B() {
	 * 		x = 3; //permissible
	 * 		new B().x = 4; //not allowed, will cause verify error
	 * 	}
	 * }
	 * Will return true is a member is visible at all times
	 * @param clazz
	 * @return
	 */
	public boolean isUnconditionallyVisibleFrom(BT_Class clazz) {
		if (isPrivate())
			return cls.equals(clazz);
		else if(isProtected() && isStatic()) {
			return cls.isInSamePackage(clazz)
				|| clazz.isDescendentOf(cls);
		}
		else
			return isPublic() || cls.equals(clazz) || cls.isInSamePackage(clazz);
		//TODO we can handle the protected case by analysing whether protected non-static access is access to "this"
		//using a code visitor
	}
	
	/**
	 True if this member is visible from the given class.
	**/
	public boolean isVisibleFrom(BT_Class clazz) {
		if (isPrivate())
			return cls.equals(clazz);
		else if (isProtected())
			return cls.isInSamePackage(clazz)
				|| clazz.isDescendentOf(cls)
				
				/* this last case only really applies when this method overrides a method in clazz 
				 */
				//TODO check this, it might be unnecessary  
				/*
				 package p1;
				 class C1 {
				 	protected m() {}
				 
				 	static void main(String args[]) {
				 		new C1().m();//this resolves to C2.m(), which means we can see C2.m() from here
				 	}
				 }
				 
				 package p2;
				 class C2 extends C1 {
				 	protected m() {
				 		super.m(); //we can see C1.m() from here
				 	}
				 }
				*/
				|| cls.isDescendentOf(clazz);
		else
			return isPublic() || cls.equals(clazz) || cls.isInSamePackage(clazz);
	}
	
	/**
	 Makes this member visible to the given class, unconditionally.
	 @returns true if the visibility was changed
	**/
	public boolean becomeVisibleFrom(BT_Class clazz) {
		if(isPublic() || cls.equals(clazz)) {
			return false;
		}
		if(isPrivate()) {
			if(clazz.isInSamePackage(cls)) {
				becomeDefaultAccess();
			} else {
				becomePublic();
			}
			return true;
		}
		if(clazz.isInSamePackage(cls) || (isProtected() && isStatic() && clazz.isDescendentOf(cls))) {
			return false;
		}
		becomePublic();
		return true;
	}

	abstract void read(DataInputStream d, BT_ConstantPool p, BT_Repository repo, LoadLocation loadedFrom)
		throws BT_ClassFileException, IOException;

	public void setName(String newName) {
		name = newName;
	}

	public String className() {
		return cls.name;
	}

	/**
	 Build the constant pool, ... in preparation for writing the class-file.
	 {@link BT_Class#resolve resolve} locks the constant-pool when it finishes.
	 <p> For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#resolve_method>resolve method</a>.
	**/
	abstract void resolve() throws BT_ClassWriteException;
	
	abstract void write(DataOutputStream dos, BT_ConstantPool pool)
		throws IOException, BT_ClassWriteException;

	final BT_ConstantPool getPool() {
		return cls.getPool();
	}
	
	abstract void dereference() throws BT_ClassFileException;

	/**
	 Replaces the contents of this member by the given member.
	**/
	void replaceContents(BT_Member other) {
		flags = other.flags;
		attributes = other.attributes;
		name = other.name;
		cls = other.cls;
		setStub(other.isStub());
	}
	
	class MemberLocation implements LoadLocation {
		private LoadLocation location;
		
		MemberLocation(LoadLocation location) {
			this.location = location;
		}
		
		public BT_ClassPathLocation getLocation() {
			return location.getLocation();
		}
		
		public String toString() {
			return BT_Member.this.fullName() + " in " + location;
		}
	}
}
