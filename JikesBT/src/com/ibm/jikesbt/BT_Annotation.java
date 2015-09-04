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



public class BT_Annotation {
	String typeName;
	BT_Class type;
	ElementValuePair elementValuePairs[];
	
	BT_Annotation() {}
	
	public BT_Annotation(String type, ElementValuePair elementValuePairs[]) {
		this.typeName = type;
		this.elementValuePairs = elementValuePairs;
	}
	
	void read(String attName, DataInputStream di, BT_ConstantPool pool) 
			throws IOException, BT_ConstantPoolException, BT_AnnotationAttributeException, BT_DescriptorException {
		typeName = BT_ConstantPool.toJavaName(pool.getUtf8At(di.readUnsignedShort()));
		int numPairs = di.readUnsignedShort();
		elementValuePairs = new ElementValuePair[numPairs];
		for(int i=0; i<elementValuePairs.length; i++) {
			ElementValuePair pair = new ElementValuePair();
			pair.read(attName, di, pool);
			elementValuePairs[i] = pair;
		}
	}
	
	String getTypeName() {
		if(type != null) {
			return type.getName();
		}
		return typeName;
	}
	
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(pool.indexOfUtf8(BT_ConstantPool.toInternalName(getTypeName())));
		dos.writeShort(elementValuePairs.length);
		for(int i = 0; i < elementValuePairs.length; ++i) {
			elementValuePairs[i].write(dos, pool);
		}
	}
	
	void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {
		//String javaName = BT_ConstantPool.toJavaName(typeName);
		//type = rep.forName(javaName);
		type = rep.forName(typeName);
		type.addReferencingAttribute(att);
		typeName = null;
		for(int i = 0; i < elementValuePairs.length; ++i) { // Per element
			elementValuePairs[i].dereference(rep, att);
		}
	}
	
	void remove(BT_Attribute att) {
		if(type != null) {
			type.removeReferencingAttribute(att);
		}
		for(int i = 0; i < elementValuePairs.length; ++i) { // Per element
			elementValuePairs[i].remove(att);
		}
	}
	
	void removeReference(BT_Attribute att, BT_Item ref) {
		if(type == ref) {
			att.remove();
		}
		for(int i = 0; i < elementValuePairs.length; ++i) { // Per element
			elementValuePairs[i].removeReference(att, ref);
		}
	}
	
	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(BT_ConstantPool.toInternalName(getTypeName()));
		for (int i = 0; i < elementValuePairs.length; ++i) { // Per element
			elementValuePairs[i].resolve(pool);
		}
	}
	
	int getLength() {
		int length = 4;
		for(int i=0; i<elementValuePairs.length; i++) {
			length += elementValuePairs[i].getLength();
		}
		return length;
	}
	
	public static class ElementValuePair {
		String name;
		BT_AnnotationElementValue elementValue;
		
		ElementValuePair() {}
		
		public ElementValuePair(String name, BT_AnnotationElementValue value) {
			this.name = name;
			this.elementValue = value;
		}
		
		
		void read(String attName, DataInputStream di, BT_ConstantPool pool) 
				throws IOException, BT_ConstantPoolException, BT_AnnotationAttributeException, BT_DescriptorException  {
			name = pool.getUtf8At(di.readUnsignedShort());
			elementValue = new BT_AnnotationElementValue();
			elementValue.read(attName, di, pool);
		}
		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			dos.writeShort(pool.indexOfUtf8(name));
			elementValue.write(dos, pool);
		}
		
		void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {
			elementValue.dereference(rep, att);
		}
		
		void remove(BT_Attribute att) {
			elementValue.remove(att);
		}
		
		void removeReference(BT_Attribute att, BT_Item ref) {
			elementValue.removeReference(att, ref);
		}
		
		public void resolve(BT_ConstantPool pool) {
			pool.indexOfUtf8(name);
			elementValue.resolve(pool);
		}
		
		int getLength() {
			return 2 + elementValue.getLength();
		}
	}
} 
