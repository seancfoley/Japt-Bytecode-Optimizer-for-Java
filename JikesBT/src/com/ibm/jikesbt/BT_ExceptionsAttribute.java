package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 The "Exceptions" attribute in a method in a class-file (not the exceptions_table).
 It contains the exceptions this method is declared to throw.

 <p> Another exception-related representation is {@link BT_ExceptionTableEntry}.

 * @author IBM
**/
/* From the VM spec:
                Exceptions_attribute {
                        u2 attribute_name_index;
                        u4 attribute_length;
                        u2 number_of_exceptions;
                        u2 exception_index_table[number_of_exceptions];
                    }
*/

public final class BT_ExceptionsAttribute extends BT_Attribute {

	/**
	 The name of this attribute, "Exceptions".
	**/
	public static final String ATTRIBUTE_NAME = "Exceptions";

	public String getName() {
		return ATTRIBUTE_NAME;
	}

	/**
	 The checked exceptions that the method may throw.
	 Always valid.
	**/
	public BT_ClassVector declaredExceptions;

	private StringVector exceptionNames;
	
	
	/**
	 Constructs from a vector of exceptions.
	 @param  de  The non-checked exceptions with the method may throw.
	**/
	public BT_ExceptionsAttribute(BT_ClassVector des, BT_Method owner) {
		super(owner);
		declaredExceptions = des;
		for(int i=0; i<des.size(); i++) {
			BT_Class exception = des.elementAt(i);
			exception.addReferencingAttribute(this);
		}
	}

	/**
	 This form of constructor is used when a class file is read.
	 @param data  The part of the attribute value following "attribute_length" from the class file.
	**/
	BT_ExceptionsAttribute(byte data[], BT_ConstantPool pool, BT_Method method, LoadLocation loadedFrom)
		throws BT_AttributeException, IOException {
		super(method, loadedFrom);
		if (data.length < 2)
			throw new BT_AttributeException(ATTRIBUTE_NAME,
				Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
		int ne = BT_Misc.bytesToUnsignedShort(data, 0);
		if (data.length != 2 + 2 * ne)
			throw new BT_AttributeException(ATTRIBUTE_NAME,
				Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
		
		try {
			exceptionNames = new StringVector(ne);
			
			for (int ie = 2; ie < data.length; ie += 2) {
				int index = BT_Misc.bytesToUnsignedShort(data, ie);
				if (index != 0) { // Don't ignore it 
					exceptionNames.addElement(pool.getClassNameAt(index, BT_ConstantPool.CLASS));
				}
			}
		} catch(BT_ConstantPoolException e) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, e);
		} 
	}
	
	void dereference(BT_Repository rep) {
		if(exceptionNames == null) {
			return;
		}
		int ne = exceptionNames.size();
		if(declaredExceptions == null) {
			declaredExceptions = new BT_ClassVector(ne);
		}
		for(int i=0; i<exceptionNames.size(); i++) {
			BT_Class exception = rep.linkTo(exceptionNames.elementAt(i));
			declaredExceptions.addElement(exception);
			exception.addReferencingAttribute(this);
		}
		exceptionNames = null;
	}
	
	public void remove() {
		if(declaredExceptions != null) {
			for(int i=0; i<declaredExceptions.size(); i++) {
				BT_Class clazz = declaredExceptions.elementAt(i);
				clazz.removeReferencingAttribute(this);
			}
		}
		super.remove();
	}
	
	/**
	 * Undo any reference to the given item.
	 */
	void removeReference(BT_Item reference) {
		if(declaredExceptions != null) {
			for(int i=declaredExceptions.size() - 1; i>=0; i--) {
				BT_Class clazz = declaredExceptions.elementAt(i);
				if(clazz == reference) {
					clazz.removeReferencingAttribute(this);
					declaredExceptions.removeElementAt(i);
					if(declaredExceptions.size() == 0) {
						remove();
						break;
					}
				}
			}
		}
	}
	

//	/**
//	 Constructs from reflection.
//	**/
//	public BT_ExceptionsAttribute(java.lang.reflect.Method m, BT_Repository repo, BT_Method owner) {
//		super(owner);
//		Class etypes[] = m.getExceptionTypes();
//		declaredExceptions = new BT_ClassVector(etypes.length);
//		for (int n = 0; n < etypes.length; ++n)
//			declaredExceptions.addElement(
//				repo.forName(etypes[n].getName()));
//	}

	/**
	 Builds the constant pool, ... in preparation for writing the class-file.
	 For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#resolve_method>resolve method</a>.
	
	 <p> Sets resolvedIndices_.
	 This field will be nulled again once the class-file has been written.
	**/
	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(ATTRIBUTE_NAME);
		for (int ie = 0; ie < declaredExceptions.size(); ++ie) // Per element
			pool.indexOfClassRef(declaredExceptions.elementAt(ie));
	}

	/**
	 Writes to a class-file.
	
	 <p> Refs resolvedIndices_, so {@link com.ibm.jikesbt#resolve} must be called before this is.
	 <p> Clears resolvedIndices_ once the attribute has been written.
	**/
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		//BT_Repository.debugRecentlyWrittenAttribute = this;
		dos.writeShort(pool.indexOfUtf8(ATTRIBUTE_NAME));
		// attribute_name_index
		dos.writeInt(2 + 2 * declaredExceptions.size()); // attribute_length
		dos.writeShort(declaredExceptions.size()); // number_of_exceptions
		for (int ie = 0; ie < declaredExceptions.size(); ++ie) { // Per element
			dos.writeShort(
				pool.indexOfClassRef(declaredExceptions.elementAt(ie)));
			// exception_index_table[number_of_exceptions];
		} // Per element
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_size_{1}_4", new Object[] {ATTRIBUTE_NAME, Integer.toString(declaredExceptions.size())});
	}
	
	public Object clone() {
		BT_ExceptionsAttribute attr = (BT_ExceptionsAttribute) super.clone();
		if(declaredExceptions != null) {
			attr.declaredExceptions = (BT_ClassVector) declaredExceptions.clone();
		}
		return attr;
	}
}
