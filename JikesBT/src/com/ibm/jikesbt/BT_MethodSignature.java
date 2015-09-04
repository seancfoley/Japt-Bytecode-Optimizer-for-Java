package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.jikesbt.BT_Method.MethodType;

/**
 The return type and the types of the arguments of some methods.
 The "this" argument (if any) is NOT included in the signature.
 Note that this is different than Java's definition of signature,
 that is a method's name and the types of each argument, but excludes its
 return type.

 <p> Method signatures are cached so that multiple methods with the same
 signature share a single method signature object.
 For this reason they are not constructed by the caller, but created via
 the "create" methods supplied here.
 Also, they must not be modified after they are created.
 * @author IBM
**/

public final class BT_MethodSignature implements BT_Opcodes, MethodType {

	/**
	 The return type.
	 This _must_ not be modified after the class is created because
	 BT_MethodSignature are cached and shared.
	**/
	public final BT_Class returnType;

	/**
	 The types of the parameters.
	 This and its contents _must_ not be modified after the class is created because
	 BT_MethodSignature are cached and shared.
	**/
	public final BT_ClassVector types;

	String stringRepresentation;
	
	private static ReentrantLock sigLock = new ReentrantLock();

	/**
	 Private.  See {@link BT_MethodSignature#create(String intSig)}.
	**/
	private BT_MethodSignature(String intSig, BT_Repository repo) throws BT_DescriptorException {
		returnType = repo.forName(BT_ConstantPool.getReturnType(intSig));
		BT_ClassVector types = BT_ConstantPool.getArgumentTypes(intSig, repo);
		if(types.size() == 0) {
			this.types = BT_ClassVector.emptyVector;
		} else {
			this.types = types;
			types.trimToSize();
		}
		stringRepresentation = intSig;
	}

	/**
	 Private.  See {@link BT_MethodSignature#create(String retExtType, String argExtTypes)}.
	**/
	private BT_MethodSignature(
		String retExtType,
		String argExtTypes,
		BT_Repository repo) {
		BT_ClassVector types = new BT_ClassVector();
		StringTokenizer t =
			new StringTokenizer(argExtTypes.substring(1), ",) ");
		while (t.hasMoreTokens()) {
			types.addElement(repo.forName(t.nextToken()));
		}
		if(types.size() == 0) {
			this.types = BT_ClassVector.emptyVector;
		} else {
			this.types = types;
			types.trimToSize();
		}
		returnType = repo.forName(retExtType);
	}

	/**
	 Private.  See {@link BT_MethodSignature#create(BT_Class returnType, BT_ClassVector args)}.
	**/
	private BT_MethodSignature(BT_Class ret, BT_ClassVector args) {
		returnType = ret;
		types = args;
	}

	/**
	 Private.  See {@link BT_MethodSignature#create(java.lang.Class returnType, java.lang.Class[] args)}.
	**/
	private BT_MethodSignature(
		java.lang.Class returnTypeClass,
		java.lang.Class[] args,
		BT_Repository repo) {
		BT_ClassVector types = new BT_ClassVector();
		for (int n = 0; n < args.length; n++)
			types.addElement(
				repo.forName(BT_ConstantPool.toJavaName(args[n].getName())));
		if(types.size() == 0) {
			this.types = BT_ClassVector.emptyVector;
		} else {
			this.types = types;
			types.trimToSize();
		}
		returnType =
			repo.forName(BT_ConstantPool.toJavaName(returnTypeClass.getName()));
	}

	public String getDescriptor() {
		return toString();
	}
	
	public BT_MethodSignature getSignature() {
		return this;
	}
	
	/**
	 Creates a method signature from an internal method type name.
	
	 @param intSig  In {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., "(Ljava/lang/String;IZ)I"
	**/
	public static BT_MethodSignature create(
		String intSig,
		BT_Repository repo) throws BT_DescriptorException {
		return find(intSig, repo, null);
	}
	
	

	/**
	 Creates a method signature from a Java language type string.
	
	 @param  returnTypeString  The return type in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	   E.g. "boolean" or "java.lang.String".
	 @param  argExtTypes  The argument types in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	   E.g. "(int,java.lang.Object)".
	**/
	public static BT_MethodSignature create(
		String retExtType,
		String argExtTypes, BT_Repository repo) throws BT_DescriptorException {
		BT_MethodSignature sig = new BT_MethodSignature(retExtType, argExtTypes, repo);
		return find(sig.toString(), repo, sig);
	}
	
	/**
	 Creates a method signature from a BT_Class representation of the types.
	 This is useful for cloning or near-cloning signatures.
	 Usually, the corresponding "create" method should be used instead
	 so that the method signature can be properly registered.
	
	 @param  returnType  The return type
	 @param  args        The argument types as a class vector
	**/
	public static BT_MethodSignature create(
		BT_Class returnType,
		BT_ClassVector args, BT_Repository repo) {
		BT_MethodSignature sig = new BT_MethodSignature(returnType, args);
		return find(sig.toString(), repo, sig);
	}
	
	public static BT_MethodSignature create(
		BT_Class returnType,
		BT_Class arg, BT_Repository repo) {
		BT_ClassVector args = new BT_ClassVector(1);
		args.addElement(arg);
		return create(returnType, args, repo);
	}
	
	static BT_MethodSignature create(
		String intSig,
		BT_Class returnType,
		BT_ClassVector args, BT_Repository repo) {
		
		if(BT_Factory.multiThreadedLoading) {
			sigLock.lock();
		}
		
		Hashtable signatures = repo.signatures;
		BT_MethodSignature found = (BT_MethodSignature) signatures.get(intSig);
		if (found != null) {
			//class names can change, so we do a recheck to ensure that the one we found is accurate
			String actualSig = found.toString();
			if(actualSig.equals(intSig)) {
				if(BT_Factory.multiThreadedLoading) {
					sigLock.unlock();
				}
				return found;
			} else {
				signatures.remove(intSig);
				if(signatures.get(actualSig) == null) {
					signatures.put(actualSig, found);
				}
			}
		}
		found = new BT_MethodSignature(returnType, args);
		found.stringRepresentation = intSig;
		signatures.put(intSig, found);
		
		if(BT_Factory.multiThreadedLoading) {
			sigLock.unlock();
		}
		
		return found;
	}
	
	
	/**
	 Creates a method signature from a java.lang.Class representation of the
	 types (as obtained via reflection using java.lang.reflect).
	 @param  returnTypeClass  The return type as a java.lang.Class object.
	 @param  args             The arguments as an array of java.lang.Class objects.
	**/
	public static BT_MethodSignature create(
		java.lang.Class returnType,
		java.lang.Class[] args, BT_Repository repo) {
		BT_MethodSignature sig = new BT_MethodSignature(returnType, args, repo);
		return find(sig.toString(), repo, sig);
	}

	public static void cleanCache(BT_Repository repo) {
		Hashtable newSignatures = new Hashtable();
		Hashtable oldSignatures = repo.signatures;
		Iterator i = oldSignatures.values().iterator();
		while(i.hasNext()) {
			BT_MethodSignature next = (BT_MethodSignature) i.next();
			newSignatures.put(next.toString(), next);
		}
		oldSignatures.clear();
		repo.signatures = newSignatures;
	}
	
	/**
	 * call this method if the signature string has changed.
	 */
	void resetStringCache(BT_Repository repo) {
		stringRepresentation = null;
	}
	
	/**
	 Finds or creates and interns a signature.
	**/
	public static BT_MethodSignature find(String intSig, BT_Repository repo) throws BT_DescriptorException {
		return find(intSig, repo, null);
	}
	
	private static BT_MethodSignature find(String intSig, BT_Repository repo, BT_MethodSignature result) 
			throws BT_DescriptorException {
		Hashtable signatures = repo.signatures;
		
		if(BT_Factory.multiThreadedLoading) {
			sigLock.lock();
		}
		
		BT_MethodSignature found = (BT_MethodSignature) signatures.get(intSig);
		if (found != null) {
			//class names can change, so we do a recheck to ensure that the one we found is accurate
			String actualSig = found.toString();
			if(actualSig.equals(intSig)) {
				if(BT_Factory.multiThreadedLoading) {
					sigLock.unlock();
				}
				return found;
			} else {
				signatures.remove(intSig);
				if(signatures.get(actualSig) == null) {
					signatures.put(actualSig, found);
				}
			}
		}
		if(result == null) {
			result = new BT_MethodSignature(intSig, repo);
		}
		signatures.put(intSig, result);
		
		if(BT_Factory.multiThreadedLoading) {
			sigLock.unlock();
		}
		return result;
	}
	

	/**
	True if this signature is equivalent to ([Ljava/lang/String)V
	**/
	public boolean isMainSignature() {
		if(returnType.isBasicTypeClass && returnType.getName().equals("void")
			&& types.size() == 1) {
			BT_Class arg = types.firstElement();
			return arg.isArray() && arg.getName().equals(BT_Repository.JAVA_LANG_STRING_ARRAY);
		}
 		return false;
	}
	
	/**
	 True if the return type and the arguments are "==".
	**/
	public boolean equals(Object otherObject) {
		if (otherObject == null
			|| !otherObject.getClass().equals(this.getClass()))
			return false;

		return otherObject.toString().equals(toString());
	}

	private int hash = 0;

	public int hashCode() {
		if (hash == 0) {
			hash = toString().hashCode();
		}
		return hash;
	}

	/**
	 @return  The sum of all the parameters sizes in terms of 32-bit words (doubles and longs are 2 words).
	**/
	public int getArgsSize() {
		return getArgsSize(0, true);
	}
	
	/**
	 @return  The sum of all the parameters sizes in terms of 32-bit words (doubles and longs are 2 words),
	 whose parameter index is greater than or equal to the given index.
	 In other words, this is the number of stack items including and above the given parameter argument.
	**/
	public int getArgsSize(int paramIndex, boolean fromTop) {
		int index = 0;
		if(fromTop) {
			for(int j = types.size() - 1;  j >= paramIndex; j--) {
				index += types.elementAt(j).getSizeForLocal();
			}
		} else {
			for(int j = 0;  j < paramIndex; j++) {
				index += types.elementAt(j).getSizeForLocal();
			}
		}
		return index;
	}

	/**
	 Creates or finds new signature that is this one plus a last argument of
	 the specified type.
	**/
	public BT_MethodSignature addArgument(BT_Class type) {
		BT_MethodSignature newSig = new BT_MethodSignature(returnType, (BT_ClassVector) types.clone());
		newSig.types.addElement(type);
		find(newSig.toString(), type.repository, newSig);
		return newSig;
	}

	/**
	 Returns the signature in Java language format.
	 @see BT_ConstantPool#getArgumentTypes
	 @return The arguments types in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	   E.g. "int,java.lang.Object".
	**/
	public String toExternalArgumentString() {
		StringBuffer s = new StringBuffer("");
		for (int n = 0; n < types.size(); n++) {
			if (n != 0)
				s.append(",");
			s.append(types.elementAt(n).name);
		}
		return s.toString();
	}

	/**
	 @return The signature string in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., "(Ljava/lang/String;IZ)I"
	**/
	public String toString() {
		if (stringRepresentation != null)
			return stringRepresentation;
		if(BT_Factory.multiThreadedLoading) {
			synchronized(this) {
				return stringRepresentation = toString(this);
			}
		}
		return stringRepresentation = toString(this);
	}
	
	public String toParameterString() {
		StringBuffer s = new StringBuffer();
		BT_ClassVector types = this.types;
		for (int n = 0; n < types.size(); n++)
			s.append(BT_ConstantPool.toInternalName(types.elementAt(n)));
		return s.toString();
	}
	
	public static String toString(BT_MethodSignature sig) {
		StringBuffer s = new StringBuffer("(");
		BT_ClassVector types = sig.types;
		for (int n = 0; n < types.size(); n++)
			s.append(BT_ConstantPool.toInternalName(types.elementAt(n).name));
		s.append(')');
		s.append(BT_ConstantPool.toInternalName(sig.returnType.name));
		return s.toString();
	}
	
	public static String toString(StringVector types, String returnType) {
		StringBuffer s = new StringBuffer("(");
		for (int n = 0; n < types.size(); n++)
			s.append(BT_ConstantPool.toInternalName(types.elementAt(n)));
		s.append(")");
		s.append(BT_ConstantPool.toInternalName(returnType));
		return s.toString();
	}
}
