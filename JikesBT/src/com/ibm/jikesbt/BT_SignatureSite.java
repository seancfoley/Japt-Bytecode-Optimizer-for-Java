package com.ibm.jikesbt;


/**
 Represents the appearance of a class in a method signature.
 
 @author sfoley
**/
public class BT_SignatureSite extends BT_Base {

	/**
	 The referencing method.
	**/
	final public BT_Method from;

	/**
	  The index into the method signature of the class.  The parameter indices begin at 0.
	  The index of the return type is 1 more than the largest parameter index, 
	  much in the same way the signature is written from left to right.  Doubles and longs
	  do not occupy two indices, even though they occupy two slots on the stack.
	 **/
	final public short index;
	
	public BT_MethodSignature getSignature() {
		return from.getSignature();
	}
	
	public BT_Class getTarget() {
		BT_MethodSignature sig = getSignature();
		return isReturnType() ? sig.returnType : sig.types.elementAt(index);
	}

	
	BT_SignatureSite(BT_Method referencer, short index, BT_Class target) {
		if (CHECK_JIKESBT && (index < 0 || index >= 256))
		assertFailure(Messages.getString("JikesBT.Invalid_signature_index___1") + index);
		this.from = referencer;
		this.index = index;
		if(CHECK_JIKESBT && !getTarget().equals(target)) {
			assertFailure(Messages.getString("JikesBT.Invalid_class_in_signature_site___2") + target 
				+ Messages.getString("JikesBT._vs__117") + getTarget());
		}	
	}

	public boolean isReturnType() {
		BT_MethodSignature sig = getSignature();
		return index == sig.types.size();
	}
	
	public String toString() {
		return Messages.getString("JikesBT.{0}_referenced_at_{1}_at_index_{2}_of_signature_{3}_4",
			new Object[] {getTarget().name, from, Integer.toString(index), from.getSignature()});
	}
	
	public boolean equals(BT_Method method, int index) {
		return this.index == index && from.equals(method);
	}

}
