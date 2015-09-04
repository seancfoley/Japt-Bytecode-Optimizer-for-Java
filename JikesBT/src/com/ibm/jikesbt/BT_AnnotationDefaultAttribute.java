/*
 * Created on Mar 10, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;


public class BT_AnnotationDefaultAttribute extends BT_Attribute {

	public static final String ATTRIBUTE_NAME = "AnnotationDefault";
	
	BT_AnnotationElementValue defaultValue;
	
	public BT_AnnotationDefaultAttribute(byte data[], BT_ConstantPool pool, BT_Method method, LoadLocation loadedFrom) 
			throws BT_AnnotationAttributeException {
		super(method, loadedFrom);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		defaultValue = new BT_AnnotationElementValue();
		try {
			defaultValue.read(ATTRIBUTE_NAME, dis, pool);
			if(data.length != getLength()) {
				throw new BT_AnnotationAttributeException(ATTRIBUTE_NAME, Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
			}
		} catch(BT_ConstantPoolException e) {
			throw new BT_AnnotationAttributeException(ATTRIBUTE_NAME, e);
		} catch(BT_DescriptorException e) {
			throw new BT_AnnotationAttributeException(ATTRIBUTE_NAME, e);
		} catch(IOException e) {
			throw new BT_AnnotationAttributeException(ATTRIBUTE_NAME, e);
		}
	}

	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(pool.indexOfUtf8(ATTRIBUTE_NAME));// attribute_name_index
		dos.writeInt(getLength()); // attribute_length
		defaultValue.write(dos, pool);
	}
	
	void dereference(BT_Repository rep) throws BT_AnnotationAttributeException {
		try {
			defaultValue.dereference(rep, this);
		} catch(BT_DescriptorException e) {
			throw new BT_AnnotationAttributeException(ATTRIBUTE_NAME, e);
		}
	}
	
	void remove() {
		defaultValue.remove(this);
		super.remove();
	}
	
	/**
	 * Undo any reference to the given item.
	 */
	void removeReference(BT_Item reference) {
		defaultValue.removeReference(this, reference);
	}
	
	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(getName());
		defaultValue.resolve(pool);
	}

	/**
	 * @return the byte length of this attribute excluding the initial 6 bytes
	 */
	int getLength() {
		return defaultValue.getLength();
	}
	
	public String getName() {
		return ATTRIBUTE_NAME;
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_<{1}_bytes>", new Object[] {getName(), Integer.toString(getLength())});
	}

}
