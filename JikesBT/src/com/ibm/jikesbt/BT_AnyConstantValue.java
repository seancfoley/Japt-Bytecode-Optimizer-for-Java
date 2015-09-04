package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 A class in which a constant of any type can be stored.

 * @author IBM
**/

public abstract class BT_AnyConstantValue {
	// Note:  It was tempting to use BT_ConstantPoolItem, but this seems 
	// more generally useful as well as more efficient.

	/**
	 Finds or creates the value in the constant pool.
	
	 @return  The index of the value in the constant pool.
	**/
	public abstract int findInPool(BT_ConstantPool pool);

	/**
	 Creates a BT_AnyConstantValue that contains the value of a constant-pool item.
	 @param index  The index of the constant-pool item.
	 @return  Never null.
	 @exception BT_ClassFileException used to terminate processing this class.
	**/
	static BT_AnyConstantValue create(
		BT_ConstantPool pool,
		int index,
		String typeName)
			throws BT_ConstantPoolException {
		switch (pool.getEntryTypeAt(index)) {
			case BT_ConstantPool.DOUBLE :
				verifyValueType(typeName.equals(pool.getRepository().getDouble().getName()), index);
				return new DoubleV(pool.getDoubleAt(index));
			case BT_ConstantPool.FLOAT :
				verifyValueType(typeName.equals(pool.getRepository().getFloat().getName()), index);
				return new FloatV(pool.getFloatAt(index));
			case BT_ConstantPool.INTEGER :
				verifyValueType(
						BT_Class.getOpcodeForStore(typeName) == BT_Class.opc_istore,
						index);
				return new IntV(pool.getIntegerAt(index));
			case BT_ConstantPool.LONG :
				verifyValueType(typeName.equals(pool.getRepository().getLong().getName()), index);
				return new LongV(pool.getLongAt(index));
			case BT_ConstantPool.STRING :
				verifyValueType(typeName.equals(pool.getRepository().findJavaLangString().getName()), index);
				return new StringV(pool.getStringAt(index));
			default :
				throw new BT_ConstantPoolException(
					Messages.getString("JikesBT.invalid_constant_type_at_cp_index__2") + index);
		}
	}
	
	private static void verifyValueType(boolean valid, int index)
		throws BT_ConstantPoolException {
		if (!valid)
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_constant_value_at_cp_index__3") + index);
	}

	public static class DoubleV extends BT_AnyConstantValue {
		public final double value;
		DoubleV(double v) {
			value = v;
		}
		public int findInPool(BT_ConstantPool pool) {
			return pool.indexOfDouble(value);
		}
		public String toString() {
			return String.valueOf(value);
		}
	}

	public static class FloatV extends BT_AnyConstantValue {
		public final float value;
		FloatV(float v) {
			value = v;
		}
		public int findInPool(BT_ConstantPool pool) {
			return pool.indexOfFloat(value);
		}
		public String toString() {
			return String.valueOf(value);
		}
	}
	public static class IntV extends BT_AnyConstantValue {
		public final int value;
		IntV(int v) {
			value = v;
		}
		public int findInPool(BT_ConstantPool pool) {
			return pool.indexOfInteger(value);
		}
		public String toString() {
			return String.valueOf(value);
		}
	}

	public static class LongV extends BT_AnyConstantValue {
		public final long value;
		LongV(long v) {
			value = v;
		}
		public int findInPool(BT_ConstantPool pool) {
			return pool.indexOfLong(value);
		}
		public String toString() {
			return String.valueOf(value);
		}
	}
	
	public static class StringV extends BT_AnyConstantValue {
		public final String value;
		StringV(String v) {
			value = v;
		}
		public int findInPool(BT_ConstantPool pool) {
			return pool.indexOfString(value);
		}
		public String toString() {
			return value;
		}
	}

	public abstract String toString();
}