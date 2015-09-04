/*
 * Created on Feb 7, 2006
 *
 * The enclosing method attribute is found in the attributes table
 * of the class file structure.
 */
package com.ibm.jikesbt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

public class BT_EnclosingMethodAttribute extends BT_Attribute {
	private String className;
	private String methodName;
	private String methodTypeName;
	BT_Class enclosingClass;
	BT_Method enclosingMethod;
			
	/**
	 The name of this attribute.
	**/
	public static final String ATTRIBUTE_NAME = "EnclosingMethod";

	BT_EnclosingMethodAttribute(byte data[], BT_ConstantPool pool, BT_Class containingClass, LoadLocation loadedFrom)
		throws BT_AttributeException {
		super(containingClass, loadedFrom);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			try {
				className = pool.getClassNameAt(dis.readUnsignedShort(), BT_ConstantPool.CLASS);
				int methodIndex = dis.readUnsignedShort();
				if(methodIndex != 0) {
					methodName = pool.getNameAndTypeNameAt(methodIndex);
					methodTypeName = pool.getNameAndTypeTypeAt(methodIndex);
				}
			} catch(BT_ConstantPoolException e) {
				throw new BT_AttributeException(ATTRIBUTE_NAME, e);
			}
		} catch(IOException e) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, e);
		}
	}
	
	
	public String getName() {
		return ATTRIBUTE_NAME;
	}
	
	public String toString() {
		return Messages.getString("JikesBT.{0}_size_{1}_4", 
				new String[] {ATTRIBUTE_NAME, "4"}); 
	}
	
	void dereference(BT_Repository rep) throws BT_AttributeException {
		enclosingClass = rep.forName(className);
		if(methodName != null && methodTypeName != null) {
			try {
				BT_MethodSignature sig = BT_MethodSignature.create(methodTypeName, rep);
				enclosingMethod = enclosingClass.findMethodOrNull(methodName, sig);
				if(enclosingMethod == null) {
					enclosingMethod = enclosingClass.addStubMethod(methodName, sig);
				}
				enclosingMethod.addReferencingAttribute(this);
			} catch(BT_DescriptorException e) {
				throw new BT_AttributeException(ATTRIBUTE_NAME, e);
			}
		}
	}
	
	void remove() {
		if(enclosingMethod != null) {
			enclosingMethod.removeReferencingAttribute(this);
		}
		super.remove();
	}
	
	/**
	 * Undo any reference to the given item.
	 */
	void removeReference(BT_Item reference) {
		if(reference == enclosingClass || reference == enclosingMethod) {
			remove();
		}
	}

	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(ATTRIBUTE_NAME);
		pool.indexOfClassRef(enclosingClass);
		if(enclosingMethod != null) {
			pool.indexOfNameAndType(enclosingMethod.name, enclosingMethod.getDescriptor());
		}
	}

	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(pool.indexOfUtf8(ATTRIBUTE_NAME)); // attribute_name_index
		dos.writeInt(4); // attribute_length
		dos.writeShort(pool.indexOfClassRef(enclosingClass));
		if(enclosingMethod != null) {
			dos.writeShort(pool.indexOfNameAndType(enclosingMethod.name, enclosingMethod.getDescriptor()));
		} else {
			dos.writeShort(0);
		}
	}

	public void print(java.io.PrintStream ps, String prefix) {
		ps.println(Messages.getString("JikesBT.{0}EnclosingMethod__{1}__4", 
				new Object[] {prefix, (enclosingMethod != null) ? enclosingMethod.useName() : ((enclosingClass != null) ? enclosingClass.getName() : className)}));
	}
}
