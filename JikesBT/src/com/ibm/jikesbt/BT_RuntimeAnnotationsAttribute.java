/*
 * Created on Mar 9, 2006
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

public class BT_RuntimeAnnotationsAttribute extends BT_Attribute {

	public static final String RUNTIME_VISIBLE_ANNOTATIONS_ATTRIBUTE_NAME = "RuntimeVisibleAnnotations";
	public static final String RUNTIME_INVISIBLE_ANNOTATIONS_ATTRIBUTE_NAME = "RuntimeInvisibleAnnotations";
	private String name;
	BT_AnnotationArray annotationArray;
	
	
	public BT_RuntimeAnnotationsAttribute(byte data[], BT_ConstantPool pool, String name, BT_Item owner,
			LoadLocation loadedFrom) throws BT_AnnotationAttributeException {
		super(owner, loadedFrom);
		this.name = name;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			annotationArray = new BT_AnnotationArray();
			try {
				annotationArray.read(name, dis, pool);
				if(data.length != getLength()) {
					throw new BT_AnnotationAttributeException(getName(), Messages.getString("JikesBT.{0}_attribute_length_2", name));
				}
			} catch(BT_ConstantPoolException e) {
				throw new BT_AnnotationAttributeException(getName(), e);
			} catch(BT_DescriptorException e) {
				throw new BT_AnnotationAttributeException(getName(), e);
			}
		} catch(IOException e) {
			throw new BT_AnnotationAttributeException(getName(), e);
		}
	}

	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(pool.indexOfUtf8(name));// attribute_name_index
		dos.writeInt(getLength()); // attribute_length
		annotationArray.write(dos, pool);
	}

	public String getName() {
		return name;
	}
	
	void dereference(BT_Repository rep) throws BT_AnnotationAttributeException {
		try {
			annotationArray.dereference(rep, this);
		} catch(BT_DescriptorException e) {
			throw new BT_AnnotationAttributeException(getName(), e);
		}
	}
	
	void remove() {
		annotationArray.remove(this);
		super.remove();
	}
	
	/**
	 * Undo any reference to the given item.
	 */
	void removeReference(BT_Attribute att, BT_Item reference) {
		annotationArray.removeReference(this, reference);
	}
	
	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(name);
		annotationArray.resolve(pool);
	}
	
	/**
	 * @return the byte length of this attribute excluding the initial 6 bytes
	 */
	int getLength() {
		return annotationArray.getLength();
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_<{1}_bytes>", new Object[] {name, Integer.toString(getLength())});
	}
}
