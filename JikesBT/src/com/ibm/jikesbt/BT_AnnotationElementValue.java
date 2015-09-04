/*
 * Created on Mar 10, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_AnyConstantValue.StringV;



public class BT_AnnotationElementValue {

	char tag; //one of 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 's', 'e', 'c', '@', '[' 
	Value value;
	
	BT_AnnotationElementValue() {}
	
	public BT_AnnotationElementValue(char tag, Value value) {
		this.tag = tag;
		this.value = value;
	}
	
	int getLength() {
		return 1 + value.getLength();
	}
	
	void read(String attName, DataInputStream di, BT_ConstantPool pool) 
			throws IOException, BT_AnnotationAttributeException, BT_ConstantPoolException, BT_DescriptorException  {
		tag = (char) di.readByte();
		switch(tag) {
			case 'B':
			case 'C':
			case 'D':
			case 'F':
			case 'I':
			case 'J':
			case 'S':
			case 'Z':
			case 's': //java.lang.String
				value = new ConstValue();
				break;
			case 'e': //enum constant
				value = new EnumConstValue();
				break;
			case 'c': //class
				value = new ClassInfoIndex();
				break;
			case '@': //annotation type
				value = new AnnotationValue();
				break;
			case '[': //array
				value = new ArrayValue();
				break;
			default:
				throw new BT_AnnotationAttributeException(attName, Messages.getString("JikesBT.invalid_type_name_50"));
		}
		value.read(attName, di, pool, tag);
		
	}
	
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeByte(tag);
		value.write(dos, pool);
	}
	
	void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {
		value.dereference(rep, att);
	}
	
	void remove(BT_Attribute att) {
		value.remove(att);
	}
	
	void removeReference(BT_Attribute att, BT_Item ref) {
		value.removeReference(att, ref);
	}
	
	public void resolve(BT_ConstantPool pool) {
		value.resolve(pool);
	}
	
	
	public static abstract class Value {
		abstract void read(String attName, DataInputStream di, BT_ConstantPool pool, char tag) 
			throws IOException, BT_AnnotationAttributeException, BT_ConstantPoolException, BT_DescriptorException;
		
		abstract void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException;
		
		abstract int getLength();
		
		void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {}
		
		void remove(BT_Attribute att) {}
		
		void removeReference(BT_Attribute att, BT_Item ref) {}
		
		public void resolve(BT_ConstantPool pool) {}
	}
	
	public static class ConstValue extends Value {
		BT_AnyConstantValue constantValue;
		
		ConstValue() {}
		
		public ConstValue(BT_AnyConstantValue constantValue) {
			this.constantValue = constantValue;
		}
		
		int getLength() {
			return 2;
		}
		
		void read(String attName, DataInputStream di, BT_ConstantPool pool, char tag) 
				throws IOException, BT_ConstantPoolException {
			int poolIndex = di.readUnsignedShort();
			String typeName;
			if(tag == 's') {
				typeName = BT_Repository.JAVA_LANG_STRING;
			} else {
				typeName = BT_ConstantPool.toJavaName(tag);
			}
			if(tag == 's' && pool.getEntryTypeAt(poolIndex) == BT_ConstantPool.UTF8) {
				constantValue = new StringV(pool.getUtf8At(poolIndex));
			} else {
				constantValue = BT_AnyConstantValue.create(pool, poolIndex, typeName);
			}
		}
		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			dos.writeShort(pool.indexOfItem(constantValue));
		}
		
		public void resolve(BT_ConstantPool pool) {
			pool.indexOfItem(constantValue);
		}
	}
	
	public static class EnumConstValue extends Value { 
		String binaryTypeName;
		String constantName;
		BT_Class clazz;
						
		EnumConstValue() {}
		
		public EnumConstValue(BT_Class clazz, String constantName) {
			this.clazz = clazz;
			this.constantName = constantName;
		}
		
		public EnumConstValue(String binaryName, String constantName) {
			this.binaryTypeName = binaryName;
			this.constantName = constantName;
		}
		
		int getLength() {
			return 4;
		}
		
		void read(String attName, DataInputStream di, BT_ConstantPool pool, char tag) throws IOException, BT_ConstantPoolException {
			int typeNameIndex = di.readUnsignedShort();
			//binaryTypeName = pool.getUtf8At(typeNameIndex);
			binaryTypeName = BT_ConstantPool.toJavaName(pool.getUtf8At(typeNameIndex));
			int constantNameIndex = di.readUnsignedShort();
			constantName = pool.getUtf8At(constantNameIndex);
		}
		
		private String getBinaryTypeName() {
			if(clazz != null) {
				return clazz.getName();
			}
			return binaryTypeName;
		}
		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			//dos.writeShort(pool.indexOfUtf8(getBinaryTypeName()));
			dos.writeShort(pool.indexOfUtf8(BT_ConstantPool.toInternalName(getBinaryTypeName())));
			dos.writeShort(pool.indexOfUtf8(constantName));
		}
		
		void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {
			//String javaName = BT_ConstantPool.toJavaName(binaryTypeName);
			//clazz = rep.forName(javaName);
			clazz = rep.forName(binaryTypeName);
			clazz.addReferencingAttribute(att);
			binaryTypeName = null;
		}
		
		void remove(BT_Attribute att) {
			if(clazz != null) {
				clazz.removeReferencingAttribute(att);
			}
		}
		
		void removeReference(BT_Attribute att, BT_Item ref) {
			if(clazz == ref) {
				att.remove();
			}
		}
		
		public void resolve(BT_ConstantPool pool) {
			//pool.indexOfUtf8(getBinaryTypeName());
			pool.indexOfUtf8(BT_ConstantPool.toInternalName(getBinaryTypeName()));
			pool.indexOfUtf8(constantName);
		}
	}

	public static class ClassInfoIndex extends Value {
		String returnClassName;
		BT_Class clazz;
		
		ClassInfoIndex() {}
		
		public ClassInfoIndex(BT_Class returnClass) {
			this.clazz = returnClass;
		}
		
		int getLength() {
			return 2;
		}
		
		void read(String attName, DataInputStream di, BT_ConstantPool pool, char tag) 
				throws IOException, BT_ConstantPoolException, BT_DescriptorException {
			int poolIndex = di.readUnsignedShort();
			returnClassName = BT_ConstantPool.toJavaName(pool.getUtf8At(poolIndex));
		}
		
		private String getReturnClassName() {
			if(clazz != null) {
				return clazz.getName();
			}
			return returnClassName;
		}
		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			dos.writeShort(pool.indexOfUtf8(BT_ConstantPool.toInternalName(getReturnClassName())));
		}
		
		void dereference(BT_Repository rep, BT_Attribute att) {
			clazz = rep.forName(returnClassName);
			clazz.addReferencingAttribute(att);
			returnClassName = null;
		}
		
		void remove(BT_Attribute att) {
			if(clazz != null) {
				clazz.removeReferencingAttribute(att);
			}
		}
		
		void removeReference(BT_Attribute att, BT_Item ref) {
			if(clazz == ref) {
				att.remove();
			}
		}
		
		public void resolve(BT_ConstantPool pool) {
			pool.indexOfUtf8(BT_ConstantPool.toInternalName(getReturnClassName()));
		}
	}

	public class AnnotationValue extends Value {
		BT_Annotation annotation;
		
		AnnotationValue() {}
		
		public AnnotationValue(BT_Annotation annotation) {
			this.annotation = annotation;
		}
		
		int getLength() {
			return annotation.getLength();
		}
		
		void read(String attName, DataInputStream di, BT_ConstantPool pool, char tag) 
				throws IOException, BT_ConstantPoolException, BT_AnnotationAttributeException, BT_DescriptorException {
			annotation = new BT_Annotation();
			annotation.read(attName, di, pool);
		}
		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			annotation.write(dos, pool);
		}
		
		void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {
			annotation.dereference(rep, att);
		}
		
		void remove(BT_Attribute att) {
			annotation.remove(att);
		}
		
		void removeReference(BT_Attribute att, BT_Item ref) {
			annotation.removeReference(att, ref);
		}
		
		public void resolve(BT_ConstantPool pool) {
			annotation.resolve(pool);
		}
	}

	public static class ArrayValue extends Value {
		BT_AnnotationElementValue elementValues[];
		
		ArrayValue() {}
		
		public ArrayValue(BT_AnnotationElementValue elementValues[]) {
			this.elementValues = elementValues;
		}
		
		int getLength() {
			int length = 2;
			for(int i=0; i<elementValues.length; i++) {
				length += elementValues[i].getLength();
			}
			return length;
		}
		
		void read(String attName, DataInputStream di, BT_ConstantPool pool, char tag) 
				throws IOException, BT_ConstantPoolException, BT_AnnotationAttributeException, BT_DescriptorException {
			int numValues = di.readUnsignedShort();
			elementValues = new BT_AnnotationElementValue[numValues];
			for(int i=0; i<numValues; i++) {
				BT_AnnotationElementValue elementValue = new BT_AnnotationElementValue();
				elementValue.read(attName, di, pool);
				elementValues[i] = elementValue;
				
			}
		}
		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			dos.writeShort(elementValues.length);
			for(int i=0; i<elementValues.length; i++) {
				elementValues[i].write(dos, pool);
			}
		}
		
		void dereference(BT_Repository rep, BT_Attribute att) throws BT_DescriptorException {
			for(int i=0; i<elementValues.length; i++) {
				elementValues[i].dereference(rep, att);
			}
		}
		
		void remove(BT_Attribute att) {
			for(int i=0; i<elementValues.length; i++) {
				elementValues[i].remove(att);
			}
		}
		
		void removeReference(BT_Attribute att, BT_Item ref) {
			for(int i=0; i<elementValues.length; i++) {
				elementValues[i].removeReference(att, ref);
			}
		}
		
		public void resolve(BT_ConstantPool pool) {
			for(int i=0; i<elementValues.length; i++) {
				elementValues[i].resolve(pool);
			}
		}
	}
}
