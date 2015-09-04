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

public class BT_RuntimeParamAnnotationsAttribute extends BT_Attribute {

	public static final String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS_ATTRIBUTE_NAME = "RuntimeVisibleParameterAnnotations";
	public static final String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS_ATTRIBUTE_NAME = "RuntimeInvisibleParameterAnnotations";
	BT_AnnotationArray parameterAnnotations[];
	private String name;
	
	public BT_RuntimeParamAnnotationsAttribute(byte data[], BT_ConstantPool pool, String name, BT_Method method, LoadLocation loadedFrom) 
			throws BT_AnnotationAttributeException {
		super(method, loadedFrom);
		this.name = name;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			int numParameters = dis.readUnsignedByte(); // AKA number_of_classes
			parameterAnnotations = new BT_AnnotationArray[numParameters];
			try {
				for(int i=0; i<parameterAnnotations.length; i++) {
					BT_AnnotationArray parameterAnnotation = new BT_AnnotationArray();
					parameterAnnotation.read(name, dis, pool);
					parameterAnnotations[i] = parameterAnnotation;
				}
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
		dos.writeShort(pool.indexOfUtf8(name));
		// attribute_name_index
		dos.writeInt(getLength()); // attribute_length
		dos.writeByte(parameterAnnotations.length); // number_of_classes
		for (int i = 0; i < parameterAnnotations.length; ++i) { // Per element
			parameterAnnotations[i].write(dos, pool);
		}
	}
	
	void dereference(BT_Repository rep) throws BT_AnnotationAttributeException {
		try {
			for(int i=0; i<parameterAnnotations.length; i++) {
				parameterAnnotations[i].dereference(rep, this);
			}
		} catch(BT_DescriptorException e) {
			throw new BT_AnnotationAttributeException(getName(), e);
		}
	}
	
	void remove() {
		for(int i=0; i<parameterAnnotations.length; i++) {
			parameterAnnotations[i].remove(this);
		}
		super.remove();
	}
	
	/**
	 * Undo any reference to the given item.
	 */
	void removeReference(BT_Item reference) {
		for(int i=0; i<parameterAnnotations.length; i++) {
			parameterAnnotations[i].removeReference(this, reference);
		}
	}
	
	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(getName());
		for(int i=0; i<parameterAnnotations.length; i++) {
			parameterAnnotations[i].resolve(pool);
		}
	}
	
	/**
	 * @return the byte length of this attribute excluding the initial 6 bytes
	 */
	int getLength() {
		int length = 1;
		for(int i=0; i<parameterAnnotations.length; i++) {
			length += parameterAnnotations[i].getLength();
		}
		return length;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_<{1}_bytes>", new Object[] {name, Integer.toString(getLength())});
	}
	
	
}
