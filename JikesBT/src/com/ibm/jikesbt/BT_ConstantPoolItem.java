package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents one element in the {@link BT_ConstantPool} of a class.
 An element can describe a String, int, class, method, field, ....
 * @author IBM
**/
public final class BT_ConstantPoolItem extends BT_Base {

	public int index1;
	public int index2;
	public int intValue;
	public long longValue;
	public float floatValue;
	public double doubleValue;
	public String strValue;
	public Object value1, value2;

	final byte type_;

	/**
	 A map from an item type's number (one of {@link
	 BT_ConstantPool#INTERFACEMETHODREF}, ...) to item type
	 name (e.g., 1 to "UTF8").
	**/
	public static final String BT_ItemName[] =
		{
			"??????",
			"UTF8",
			"??????",
			"INTEGER",
			"FLOAT",
			"LONG",
			"DOUBLE",
			"CLASS",
			"STRING",
			"FIELDREF",
			"METHODREF",
			"INTERFACEMETHODREF",
			"NAMEANDTYPE",
			"<dummy>",
			};

	BT_ConstantPoolItem(int type) {
		type_ = (byte) type;
	}

	public static BT_ConstantPoolItem getDummy() {
		return new BT_ConstantPoolItem(BT_ConstantPool.DUMMY);
	}

	/** Returns the item's type, that will be {@link
	 BT_ConstantPool#INTERFACEMETHODREF} or one of its relatives.
	 The item must be a {@link BT_ConstantPool#UTF8}.
	**/
	public String strValue() {
		if (CHECK_USER && type_ != BT_ConstantPool.UTF8)
			assertFailure(Messages.getString("JikesBT.Requesting_string_from_a_{0}_15", type_));
		return strValue;
	}

	/**
	 Returns the item's type, that will be {@link
	 BT_ConstantPool#INTERFACEMETHODREF} or one of its relatives.
	**/
	public byte type() {
		return type_;
	}
	
	void read(DataInputStream dis) throws BT_ConstantPoolException, IOException {
		switch (type_) {
			case BT_ConstantPool.FIELDREF :
			case BT_ConstantPool.METHODREF :
			case BT_ConstantPool.INTERFACEMETHODREF :
			case BT_ConstantPool.NAMEANDTYPE :
				index1 = dis.readUnsignedShort();
				index2 = dis.readUnsignedShort();
				break;
			case BT_ConstantPool.STRING :
			case BT_ConstantPool.CLASS :
				index1 = dis.readUnsignedShort();
				break;
			case BT_ConstantPool.INTEGER :
				intValue = dis.readInt();
				break;
			case BT_ConstantPool.FLOAT :
				floatValue = dis.readFloat();
				break;
			case BT_ConstantPool.LONG :
				longValue = dis.readLong();
				break;
			case BT_ConstantPool.DOUBLE :
				doubleValue = dis.readDouble();
				break;
			case BT_ConstantPool.UTF8 :
				strValue = readUTF(dis);
				break;
			default :
				throw new BT_ConstantPoolException(
					Messages.getString("JikesBT.unexpected_constant_pool_entry_type_{0}_16", type_));
		}
	}

	final String readUTF(DataInputStream dis) throws BT_ConstantPoolException, IOException {
		return dis.readUTF();
	}

	public void write(DataOutputStream dos) throws IOException, BT_ClassWriteException {
		dos.writeByte(type_);
		switch (type_) {
			case BT_ConstantPool.FIELDREF :
			case BT_ConstantPool.METHODREF :
			case BT_ConstantPool.INTERFACEMETHODREF :
			case BT_ConstantPool.NAMEANDTYPE :
				dos.writeShort(index1);
				dos.writeShort(index2);
				break;
			case BT_ConstantPool.STRING :
			case BT_ConstantPool.CLASS :
				dos.writeShort(index1);
				break;
			case BT_ConstantPool.INTEGER :
				dos.writeInt(intValue);
				break;
			case BT_ConstantPool.FLOAT :
				dos.writeFloat(floatValue);
				break;
			case BT_ConstantPool.LONG :
				dos.writeLong(longValue);
				break;
			case BT_ConstantPool.DOUBLE :
				dos.writeDouble(doubleValue);
				break;
			case BT_ConstantPool.UTF8 :
				int length = dos.size();
				dos.writeUTF(strValue);
				int writtenLength = dos.size() - length;
				if(BT_Misc.overflowsUnsignedShort(writtenLength)) {
					throw new BT_ClassWriteException(Messages.getString("JikesBT.UTF8_string_too_long_108"));
				}
				break;
		}
	}

	public String toString() {
		String s;
		try {
			s = BT_ItemName[type_] + " ";
		}
		catch (Exception e) {
			s = "<unknown type> ";
		}
		switch (type_) {
			case BT_ConstantPool.FIELDREF :
			case BT_ConstantPool.METHODREF :
			case BT_ConstantPool.INTERFACEMETHODREF :
			case BT_ConstantPool.NAMEANDTYPE :
				s += index1 + " " + index2;
				break;
			case BT_ConstantPool.STRING :
			case BT_ConstantPool.CLASS :
				s += index1;
				break;
			case BT_ConstantPool.INTEGER :
				s += intValue;
				break;
			case BT_ConstantPool.FLOAT :
				s += floatValue;
				break;
			case BT_ConstantPool.LONG :
				s += longValue;
				break;
			case BT_ConstantPool.DOUBLE :
				s += doubleValue;
				break;
			case BT_ConstantPool.UTF8 :
				s += strValue;
				break;
			case BT_ConstantPool.DUMMY :
				s += "<spacer for previous long/double constant>";
				break;
		}
		return s;
	}
	
	
	public boolean equals(Object o) {
		if (!(o instanceof BT_ConstantPoolItem))
			return false;
		BT_ConstantPoolItem other = (BT_ConstantPoolItem)o;
		if (type_ != other.type_)
			return false;
		switch (type_) {
			case BT_ConstantPool.STRING :
			case BT_ConstantPool.CLASS :
				return value1.equals(other.value1);
			case BT_ConstantPool.FIELDREF :
			case BT_ConstantPool.METHODREF :
			case BT_ConstantPool.INTERFACEMETHODREF :
			case BT_ConstantPool.NAMEANDTYPE :
				return value1.equals(other.value1) && value2.equals(other.value2);
			case BT_ConstantPool.INTEGER :
				return intValue == other.intValue;
			case BT_ConstantPool.FLOAT :
				return floatValue == other.floatValue
					|| (Float.isNaN(floatValue) && Float.isNaN(other.floatValue));
			case BT_ConstantPool.LONG :
				return longValue == other.longValue;
			case BT_ConstantPool.DOUBLE :
				return doubleValue == other.doubleValue
					|| (Double.isNaN(doubleValue) && Double.isNaN(other.doubleValue));
			case BT_ConstantPool.UTF8 :
				return strValue == other.strValue || strValue.equals(other.strValue);
		}
		return super.equals(o);
	}
	
	public int hashCode() {
		switch (type_) {
			case BT_ConstantPool.STRING :
			case BT_ConstantPool.CLASS :
				return value1.hashCode();
			case BT_ConstantPool.FIELDREF :
			case BT_ConstantPool.METHODREF :
			case BT_ConstantPool.INTERFACEMETHODREF :
			case BT_ConstantPool.NAMEANDTYPE :
				return value1.hashCode() + value2.hashCode();
			case BT_ConstantPool.INTEGER :
				return intValue;
			case BT_ConstantPool.FLOAT :
				return (int)floatValue;
			case BT_ConstantPool.LONG :
				return (int)longValue;
			case BT_ConstantPool.DOUBLE :
				return (int)doubleValue;
			case BT_ConstantPool.UTF8 :
				return strValue.hashCode();
		}
		return super.hashCode();
	}

}
