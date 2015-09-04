/*
 * Created on Mar 9, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BT_AnnotationArray {
	BT_Annotation annotations[];
	
	BT_AnnotationArray() {}
	
	public BT_AnnotationArray(BT_Annotation annotations[]) {
		this.annotations = annotations;
	}
	
	void read(String attName, DataInputStream dis, BT_ConstantPool pool) 
			throws IOException, BT_ConstantPoolException, BT_AnnotationAttributeException, BT_DescriptorException {
		int numAnnotations = dis.readUnsignedShort(); // AKA number_of_classes
		annotations = new BT_Annotation[numAnnotations];
		for(int i=0; i<annotations.length; i++) {
			BT_Annotation annotation = new BT_Annotation();
			annotation.read(attName, dis, pool);
			annotations[i] = annotation;
		}
	}
	
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(annotations.length); // number_of_classes
		for (int i = 0; i < annotations.length; ++i) { // Per element
			annotations[i].write(dos, pool);
		}
	}
	
	void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {
		for (int i = 0; i < annotations.length; ++i) { // Per element
			annotations[i].dereference(rep, att);
		}
	}
	
	void remove(BT_Attribute att) {
		for (int i = 0; i < annotations.length; ++i) { // Per element
			annotations[i].remove(att);
		}
	}
	
	void removeReference(BT_Attribute att, BT_Item ref) {
		for (int i = 0; i < annotations.length; ++i) { // Per element
			annotations[i].removeReference(att, ref);
		}
	}
	
	public void resolve(BT_ConstantPool pool) {
		for (int i = 0; i < annotations.length; ++i) { // Per element
			annotations[i].resolve(pool);
		}
	}
	
	int getLength() {
		int length = 2;
		for(int i=0; i<annotations.length; i++) {
			length += annotations[i].getLength();
		}
		return length;
	}
}
