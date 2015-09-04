package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

/**
 Represents the constant-pool for a class.
 <p>
 Note that the default setting of {@link BT_Class#keepConstantPool}
 causes constant-pools to be removed from a class right after the class
 is loaded and all symbolic references to other classes, methods, and fields
 have been replaced by explicit pointers to JikesBT artifacts. This is done
 to ease the life of a JikesBT user by removing the complexity of constant pools.
 <p>
 Only when the class is written out again, the constant pool is recreated in 
 two phases. First, all methods and fields are asked the exact constant pool 
 entries they need. Then, the class is written out (the constant pool comes
 first). The methods and fields that follow in the byte stream then refer to
 the entries in the pool just written.
 <p>
 As part of the writing process, the resulting constant pool only contains
 the entries that are really necessary and has no duplicate items either.
 Therefore, simply loading a class and writing it out again using JikesBT
 may result in a smaller class file even when nothing is explicitly removed
 from it.

  * @author IBM
**/

public class BT_ConstantPool extends BT_Base {

	/**
	 A compilation constant used in JikesBT to verify calls to the constant pool. 
	 Some systems based on JikesBT may not be worried about the input
	 files and assume their constant pools contain correct items.
	
	 The default value is true.
	**/
	public static boolean CHECK_INPUT = true;
	
	private BT_ConstantPoolItemVector items;
	private HashMap itemMap = new HashMap(32);
	
	boolean locked_ = false;

	/**
	 The character used in the class file to separate parts of a
	 fully-qualified class name.
	**/
	private static final char JAVA_FILE_SEPARATOR_SLASH = '/';

	private BT_Class clazz;	
	private BT_Repository repository;
	
	/**
	 Returns repository associated with this constant pool object
	**/
	public BT_Repository getRepository() {
		return repository;
	}

	/**
	 Creates a constant pool for some class.
	
	 See {@link BT_Factory#createConstantPool}.
	**/
	public BT_ConstantPool(BT_Repository repository) {
		this.repository = repository;
		items = new BT_ConstantPoolItemVector(1);
		items.addElement(new BT_ConstantPoolItem(99));
		// Represents the non-existent zeroth element
	}

	/**
	 constant pools actually exist independently of classes.  Setting the class
	 simply aids for debugging purposes.
	 */
	public void setClass(BT_Class clazz) {
		this.clazz = clazz;
	}
	
	/**
	 Causes an exception if the constant-pool is updated.
	**/
	public void lock() {
		locked_ = true;
	}

	public void unLock() {
		locked_ = false;
	}

	/**
	 Causes all classes named in the constant-pool to be loaded.
	 Called within JikesBT while the class is being read.
	**/
	public void closure() {
		for (int n = 1; n < items.size(); n++) {
			BT_ConstantPoolItem item = items.elementAt(n);
			if (item.type_ != CLASS)
				continue;
			try {
				String name = getClassNameAt(n, CLASS);
				repository.forName(name);
			} catch(BT_ConstantPoolException e) {
				//ignore, if the constant pool entry is never used there is no harm
			}
		}
	}
	
	/**
	  Adds an item to the end of the contant pool.  Will not check if there
	  already exists an equivalent entry.  If you wish to check for an
	  existing equivalent entry first, then use the appropriate indexOf method instead.
	  
	  @return the index of the item in the constant pool
	 **/
	public int addItem(BT_ConstantPoolItem item) {
		int size = items.size();
		items.addElement(item);
		itemMap.put(item, new Integer(size));
		return size;
	}
	
	public int addDoubleItem(BT_ConstantPoolItem item) {
		int ret = addItem(item);
		items.addElement(BT_ConstantPoolItem.getDummy());
		return ret;
	}
	

	/**
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	 @return  One of {@link BT_ConstantPool#INTEGER}, {@link BT_ConstantPool#STRING}, ....
	**/
	public byte getEntryTypeAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_cp_index_{0}_2", index));
		return items.elementAt(index).type_;
	}

	/**
	Returns a verified simple identifier name.
	@param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getSimpleNameAt(int index) throws BT_ConstantPoolException {
		String name = getUtf8At(index);
		
		// this line is neccessary for tests
		// do not comment it since it make tests fail
		if (CHECK_INPUT && !isValidSimpleName(name))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_simple_name_at_cp_index_{0}_3", index));
		return name;
	}

	/**
	 Returns a verified field descriptor.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getFieldDescriptorAt(int index) throws BT_ConstantPoolException {
		return toFieldDescriptor(getUtf8At(index));
	}

	/**
	 Returns a verified method descriptor.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getMethodDescriptorAt(int index) throws BT_ConstantPoolException {
		return toMethodDescriptor(getUtf8At(index));
	}

	public static String internalToStandardClassName(String className) {
		return className.replace(JAVA_FILE_SEPARATOR_SLASH, '.');
	}
	
	/**
	 Returns the name of the class referenced by this item.
	 The constant pool item must be a CLASS, METHODREF, INTERFACEMETHODREF, or a FIELDREF.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getClassNameAt(int index, int expectedType)
		throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_cp_index_{0}_2", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != expectedType)) {
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.wrong_constant_pool_entry_for_class_{0}._At_index_{1},_expected_{2}_found_{3}_5", new Object[] {clazz != null ? clazz.getName() : "unknown", Integer.toString(index), Integer.toString(expectedType), Byte.toString(item.type_)}));
		}
		if (item.type_ == CLASS) {
			String className = getUtf8At(item.index1);

			if (className.charAt(0) == '[') {
				try {
					return toJavaName(className);
				} catch(BT_DescriptorException e) {
					throw new BT_ConstantPoolException(e);
				}
			}
			if (CHECK_INPUT && !isValidIdentifier(className))
				throw new BT_ConstantPoolException(
					Messages.getString("JikesBT.invalid_class_name_at_cp_index_{0}_6", index));
			return internalToStandardClassName(className);
		} else {
			if (item.index1 == index)
				throw new BT_ConstantPoolException(Messages.getString("JikesBT.cannot_find_class_name_at_index_{0}_7", index));
			return getClassNameAt(item.index1, CLASS);
		}
	}
	
	public static boolean isJavaIdentifierPart(char c) {
		/* javadoc now produces the class package-info which contains the hyphen character, so we must allow that character now */
		return Character.isJavaIdentifierPart(c) || c == '-';
	}

	private static boolean isValidIdentifier(String name) {
		int len = name.length();
		int p = 0;
		boolean valid = false;
		while (p < len) {
			char c = name.charAt(p);
			if (!Character.isJavaIdentifierStart(c))
				return false;
			valid = true;
			while (++p < len) {
				c = name.charAt(p);
				if (c == '/') {
					valid = false;
					p++;
					break;
				} else {
					if (!isJavaIdentifierPart(c))
						return false;
				}
			}
		}
		return valid;
	}

	private static boolean isValidSimpleName(String name) {
		int len = name.length();
		if (len == 0)
			return false;
		char c = name.charAt(0);
		if (!(Character.isJavaIdentifierStart(c) || c == '<'))
			return false;
		int p = 0;
		while (++p < len) {
			c = name.charAt(p);
			if (!(isJavaIdentifierPart(c) || c == '>'))
				return false;
		}
		return true;
	}

	private static String toFieldDescriptor(String name) throws BT_ConstantPoolException {
		try {
			if (CHECK_INPUT) {
				int l = name.length();
				if (l > 0 && "BCDFIJLVSZ[".indexOf(name.charAt(0)) != -1) {
					return toJavaName(name);
				} else {
					throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_field_descriptor_in_constant_pool_9"));
				}
			}
			else {
				return toJavaName(name);
			}
		} catch(BT_DescriptorException e) {
			throw new BT_ConstantPoolException(e);
		}
	}

	private static String toMethodDescriptor(String name)
		throws BT_ConstantPoolException {
		if (CHECK_INPUT) {
			int l = name.length();
			int p = name.indexOf(')');
			if (l > 2 && name.charAt(0) == '(' && p > 0 && p < l - 1) {
				return name;
			} else {
				throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_method_descriptor_in_constant_pool_10"));
			}
		}
		else {
			return name;
		}
	}

	/**
	Returns the name of a NAMEANDTYPE item.
	@param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getNameAndTypeNameAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_NAMEANDTYPE_cp_index_{0}_11", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != NAMEANDTYPE))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_NAMEANDTYPE_at_cp_index_{0}_12", index));
		return getSimpleNameAt(item.index1);
	}

	/**
	 Returns the type of a NAMEANDTYPE item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getNameAndTypeTypeAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_NAMEANDTYPE_cp_index_{0}_11", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != NAMEANDTYPE))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_NAMEANDTYPE_at_cp_index_{0}_12", index));
		return getUtf8At(item.index2);
	}

	/**
	 Returns a method name from a METHODREF or INTERFACEMETHODREF item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getMethodNameAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_METHODREF_cp_index_{0}_13", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != METHODREF && item.type_ != INTERFACEMETHODREF))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_METHODREF_or_INTERFACEMETHODREF_at_cp_index_{0}_14", index));
		return getNameAndTypeNameAt(item.index2);
	}

	/**
	 Returns a method's return type from a METHODREF or INTERFACEMETHODREF item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getMethodTypeAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_METHODREF_cp_index_{0}_13", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != METHODREF && item.type_ != INTERFACEMETHODREF))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_METHODREF_or_INTERFACEMETHODREF_at_cp_index_{0}_14", index));
		String nameAndType = getNameAndTypeTypeAt(item.index2);
		return toMethodDescriptor(nameAndType);
	}

	/**
	 Returns a field's name from a FIELDREF item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getFieldNameAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_FIELDREF_cp_index_{0}_15", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != FIELDREF))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_FIELDREF_at_cp_index_{0}_16", index));
		return getNameAndTypeNameAt(item.index2);
	}

	/**
	 Returns a field's type from a FIELDREF item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getFieldTypeAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_FIELDREF_cp_index_{0}_15", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != FIELDREF))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_FIELDREF_at_cp_index_{0}_16", index));
		String nameAndType = getNameAndTypeTypeAt(item.index2);
		return toFieldDescriptor(nameAndType);
	}

	/**
	 Returns a String from a STRING item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getStringAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_STRING_cp_index_{0}_23", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != STRING))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_STRING_at_cp_index_{0}_24", index));
		return getUtf8At(item.index1);
	}

	/**
	 Returns a float from a FLOAT item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public float getFloatAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_FLOAT_cp_index_{0}_25", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != FLOAT))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_FLOAT_at_cp_index_{0}_26", index));
		return item.floatValue;
	}

	/**
	 Returns an int from an INTEGER item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public int getIntegerAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.invalid_INTEGER_cp_index_{0}_27", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != INTEGER))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_INTEGER_at_cp_index_{0}_28", index));
		return item.intValue;
	}

	/**
	 Returns a long from a LONG item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public long getLongAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_LONG_cp_index_{0}_29", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != LONG))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_LONG_at_cp_index_{0}_30", index));
		return item.longValue;
	}

	/**
	 Returns a double from a DOUBLE item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public double getDoubleAt(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT &&  (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_DOUBLE_cp_index_{0}_31", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != DOUBLE))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_DOUBLE_at_cp_index_{0}_32", index));
		return item.doubleValue;
	}

	/**
	 Returns a String from a UTF8 item.
	 @param  index  The index of a {@link BT_ConstantPool#tem} in the constant pool.
	**/
	public String getUtf8At(int index) throws BT_ConstantPoolException {
		if (CHECK_INPUT && (index < 1 || index >= items.size()))
			throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_UTF8_cp_index_{0}_33", index));
		BT_ConstantPoolItem item = items.elementAt(index);
		if (CHECK_INPUT && (item.type_ != UTF8))
			throw new BT_ConstantPoolException(
				Messages.getString("JikesBT.expected_UTF8_at_cp_index_{0}_34", index));
		return item.strValue;
	}

	/**
		Returns the index of the given item in the pool.  Adds the item to the pool
		if necessary.
	 **/
	private int indexOfItem(BT_ConstantPoolItem searchItem, boolean isDouble) {
		Integer index = (Integer)itemMap.get(searchItem);
		if (index != null) 
			return index.intValue();	
		if(locked_) {
			throw new BT_LockedConstantPoolException(
					Messages.getString("JikesBT.Modifying_a_locked_constant_pool_35"));
		}
		return isDouble ? addDoubleItem(searchItem) : addItem(searchItem);
	}
	
	/**
	 Returns the position of the constant pool entry that represents the utf8 argument.
	 If none is found then if the pool is unlocked, a matching entry is created.
	**/
	public int indexOfUtf8(String utf8) {
		if (utf8 == null) {
			// for example inner class name can be null, and CP index 0 must be written.
			return 0;
		}
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(UTF8);
		searchItem.strValue = utf8;
		return indexOfItem(searchItem, false);
	}

	public int indexOfInteger(int value) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(INTEGER);
		searchItem.intValue = value;
		return indexOfItem(searchItem, false);
	}

	public int indexOfFloat(float value) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(FLOAT);
		searchItem.floatValue = value;
		return indexOfItem(searchItem, false);
	}

	public int indexOfDouble(double value) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(DOUBLE);
		searchItem.doubleValue = value;
		return indexOfItem(searchItem, true);
	}

	public int indexOfLong(long value) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(LONG);
		searchItem.longValue = value;
		return indexOfItem(searchItem, true);
	}
	
	/*
	 * The following indexOf methods cannot call indexOfItem like the above
	 * indexOf methods because the following insert constant pool items that refer 
	 * to additional constant pool items.  
	 * So when an item is not found in the pool, only then do we wish to create 
	 * the additional entries.  To improve this we can change the indexOfItem
	 * to set a flag that indicates whether an item was not found, so that the additional
	 * items are created when the flag is set.  The one minor difference is that
	 * the ordering of the constant pool will be different.
	 */
	
	public int indexOfString(String value) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(STRING);
		searchItem.value1 = value;
		Integer index = (Integer)itemMap.get(searchItem);
		if (index != null) 
			return index.intValue();	
		if(locked_) {
			throw new BT_LockedConstantPoolException(
					Messages.getString("JikesBT.Modifying_a_locked_constant_pool_35"));
		}
		searchItem.index1 = indexOfUtf8(value);
		return addItem(searchItem);
	}

	
	public int indexOfClassRef(BT_Class cls) {
		if (cls == null) {
			// for example outer class can be null, and CP index 0 must be written.
			return 0;
		}
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(CLASS);
		searchItem.value1 = cls;
		Integer index = (Integer)itemMap.get(searchItem);
		if (index != null) 
			return index.intValue();
		
		String internalName;
		if (cls.isArray())
			internalName = toInternalName(cls.name);
		else
			internalName = toInternalClassName(cls.name);

		if(locked_) {
			throw new BT_LockedConstantPoolException(
					Messages.getString("JikesBT.Modifying_a_locked_constant_pool____Can__t_find_ClassRef_{0}_37", internalName));
		}
		searchItem.index1 = indexOfUtf8(internalName);
		return addItem(searchItem);
	}

	public int indexOfNameAndType(String name, String type) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(NAMEANDTYPE);
		searchItem.value1 = name;
		searchItem.value2 = type;
		Integer index = (Integer)itemMap.get(searchItem);
		if (index != null) 
			return index.intValue();
		if(locked_) {
			throw new BT_LockedConstantPoolException(
					Messages.getString("JikesBT.Modifying_a_locked_constant_pool_35"));
		}
		searchItem.index1 = indexOfUtf8(name);
		searchItem.index2 = indexOfUtf8(type);
		return addItem(searchItem);
	}

	private int indexOfMethodRef(BT_Class throughClass, BT_Method m, int type) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(type);
		searchItem.value1 = m;
		searchItem.value2 = throughClass;
		Integer index = (Integer)itemMap.get(searchItem);
		if (index != null) 
			return index.intValue();
		if(locked_) {
			throw new BT_LockedConstantPoolException(
					Messages.getString("JikesBT.Modifying_a_locked_constant_pool__{0}_39", m));
		}
		searchItem.index1 = indexOfClassRef(throughClass);
		searchItem.index2 = indexOfNameAndType(m.name, m.getDescriptor());
		return addItem(searchItem);
	}
	
	public int indexOfMethodRef(BT_Class throughClass, BT_Method m) {
		return indexOfMethodRef(throughClass, m, METHODREF);
	}
	
	public int indexOfMethodRef(BT_Method m) {
		return indexOfMethodRef(m.cls, m, METHODREF);
	}
	
	public int indexOfInterfaceMethodRef(BT_Class throughInterface, BT_Method m) {
		return indexOfMethodRef(throughInterface, m, INTERFACEMETHODREF);
	}

	public int indexOfInterfaceMethodRef(BT_Method m) {
		return indexOfMethodRef(m.cls, m, INTERFACEMETHODREF);
	}

	public int indexOfFieldRef(BT_Field f) {
		return indexOfFieldRef(f.cls, f);
	}
	
	public int indexOfFieldRef(BT_Class throughClass, BT_Field f) {
		BT_ConstantPoolItem searchItem = new BT_ConstantPoolItem(FIELDREF);
		searchItem.value1 = f;
		searchItem.value2 = throughClass;
		Integer index = (Integer)itemMap.get(searchItem);
		if (index != null) 
			return index.intValue();
		if(locked_) {
			throw new BT_LockedConstantPoolException(
					Messages.getString("JikesBT.Modifying_a_locked_constant_pool_35"));
		}
		String signature = toInternalName(f.getFieldType().name);
		searchItem.index1 = indexOfClassRef(throughClass);
		searchItem.index2 = indexOfNameAndType(f.name, signature);
		return addItem(searchItem);
	}

	/**
	 * Find or create an item that _equals_ (not "==") the argument.
	 * This method is here for consistency with the other pool.find
	 * methods, but it simply calls
	 * {@link com.ibm.jikesbt.BT_AnyConstantValue#findInPool(BT_ConstantPool)}.
	 */
	public int indexOfItem(BT_AnyConstantValue acv) {
		return acv.findInPool(this);
	}

	void read(DataInputStream dis) throws BT_ConstantPoolException, BT_DescriptorException, IOException {
		int count = dis.readUnsignedShort();
		items.ensureCapacity(count + 1);
		for (int n = 1; n < count; n++) {
			BT_ConstantPoolItem item = new BT_ConstantPoolItem(dis.readUnsignedByte());
			item.read(dis);
			items.addElement(item);
			if (item.type_ == LONG || item.type_ == DOUBLE) {
				items.addElement(BT_ConstantPoolItem.getDummy());
				// Dummy for double-width item
				++n;
				if (CHECK_INPUT && (n >= count))
					throw new BT_ConstantPoolException(Messages.getString("JikesBT.invalid_constant_pool_count_42"));
			}
		}
		verifyItems();
	}

	void verifyItems() throws BT_ConstantPoolException, BT_DescriptorException {
		for (int n = 1; n < items.size(); n++) {
			BT_ConstantPoolItem item = items.elementAt(n);
			switch (item.type_) {
				case BT_ConstantPool.FIELDREF :
					getClassNameAt(n, item.type_);
					getFieldNameAt(n);
					getFieldTypeAt(n);
					break;
				case BT_ConstantPool.METHODREF :
					getClassNameAt(n, item.type_);
					getMethodNameAt(n);
					getMethodTypeAt(n);
					break;
				case BT_ConstantPool.INTERFACEMETHODREF :
					getClassNameAt(n, item.type_);
					getMethodNameAt(n);
					getMethodTypeAt(n);
					break;
				case BT_ConstantPool.STRING :
					getStringAt(n);
					break;
				case BT_ConstantPool.CLASS :
					getClassNameAt(n, item.type_);
					break;
				case BT_ConstantPool.INTEGER :
					getIntegerAt(n);
					break;
				case BT_ConstantPool.FLOAT :
					getFloatAt(n);
					break;
				case BT_ConstantPool.LONG :
					getLongAt(n);
					break;
				case BT_ConstantPool.DOUBLE :
					getDoubleAt(n);
					break;
				case BT_ConstantPool.UTF8 :				
					getUtf8At(n);
					break;
			}
		}
	}

	void write(DataOutputStream dos) throws IOException, BT_ClassWriteException {
		dos.writeShort(size());
		for (int n = 1; n < items.size(); n++) {
			BT_ConstantPoolItem item = items.elementAt(n);
			item.write(dos);
			if (item.type_ == LONG || item.type_ == DOUBLE)
				n++;
		}
	}

	/**
	 * Return true if the constant pool contains ClassInfos for inner classes.
	 */
	public boolean containsInnerClassNames() {
		for (int n=1; n<items.size(); n++) {
			BT_ConstantPoolItem item = items.elementAt(n);
			try {
				if (item.type_ == CLASS && getClassNameAt(n, CLASS).indexOf(Character.CURRENCY_SYMBOL) != -1)
					return true;
			} 
			catch(BT_ConstantPoolException e) {/* ignore unusable class entries */}
		}
		return false;
	}

	   /**
		Converts the trailing part of an internal Java type name (e.g., a field
		descriptor) to an external Java language type name.
		E.g., <code>"Name[][]" == toJavaName( "[[[[[LName;", 3)</code>.
		Method signatures are not supported.

		<p> Also see {@link BT_ConstantPool#toInternalClassName} and {@link #toInternalName}.

		@param  shortName    A class name in the
		  {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
		  E.g., "[[LName;".
		@param  index        The index of the trailing substring to be converted.
		@return              The name converted to the
		  {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
		  E.g., "Name[][]".
	   **/
	public static String toJavaName(String shortName, int index) throws BT_DescriptorException {
		int arrayDimension = 0;
		StringBuffer x = new StringBuffer();
		while (shortName.charAt(index) == '[') {
		  arrayDimension++;
		  index++;
		}
		if (BT_Misc.overflowsUnsignedByte(arrayDimension)) { /* section 4.10 of vmspec */
			throw new BT_DescriptorException(Messages.getString("JikesBT.array_descriptor_exceeds_255_dimensions_43"));
		}
		char c = shortName.charAt(index);
		String name = toJavaName(c);
		if(name != null) {
			x.append(name);
		} else {
			switch (c) {
			  case 'L':
				int  end = shortName.indexOf(';', index);
				// the following test is required by test suites
				// if disabled it will break the tests
				if (CHECK_INPUT && !isValidIdentifier(shortName.substring(index, end))) {
					throw new BT_DescriptorException(Messages.getString("JikesBT.invalid_type_name_50"));
				}
				for (int i=index+1; i<end; i++)                         // Use .replace(,) instead of this!!
					x.append( (shortName.charAt(i) != JAVA_FILE_SEPARATOR_SLASH) ? shortName.charAt(i):  '.');
				break;
			  default:
				throw new BT_DescriptorException(Messages.getString("JikesBT.invalid_type_name_50"));
			}
		}
		while (arrayDimension-- > 0) x.append("[]");
		return x.toString();
	}
	
	public static String toJavaName(char c) {
		switch (c) {
		  case 'B':  return "byte";
		  case 'C':  return "char";
		  case 'D':  return "double";
		  case 'F':  return "float";
		  case 'I':  return "int";
		  case 'J':  return "long";
		  case 'V':  return "void";
		  case 'S':  return "short";
		  case 'Z':  return "boolean";
		  default:
			return null;
		}
	}

	   /**
		Converts an internal Java type name to a Java language type name.
		E.g., "Name[][]" == toJavaName( "[[LName;")

		@param  shortName    A class name in
		  {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
		  E.g., "[[LName;".
		@return  The name converted to
		  {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
		  E.g., "Name[][]".
	   **/
	public static String  toJavaName(String shortName) throws BT_DescriptorException {
		return toJavaName(shortName, 0);
	}

	private static final java.util.Hashtable internalClassNames_ =
		new java.util.Hashtable();
	private static final java.util.Hashtable internalNames_ =
		new java.util.Hashtable();

	/**
	 Converts a class name from external Java format to internal bytecode format.
	 Also see {@link BT_ConstantPool#toJavaName} and {@link #toInternalName}.
	
	 @param  javaClassName  A class name in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	 @return  An interned class name in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., not surrounded by "L" and ";".
	**/
	public static String toInternalClassName(String javaClassName) {
		String result = (String) internalClassNames_.get(javaClassName);
		if (result != null) // Already interned
			return result;
		result = javaClassName.replace('.', JAVA_FILE_SEPARATOR_SLASH);
		internalClassNames_.put(javaClassName, result); // Intern it
		return result;
	}

	/**
	 Converts a type descriptor from external Java format to internal bytecode format.
	 Also see {@link BT_ConstantPool#toJavaName} and {@link #toInternalClassName}.
	
	 @param  javaName  A type descriptor in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	 @return  An interned type descriptor in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., "Ljava.lang.Object;" or "I".
	**/
	public static String toInternalName(String javaName) {
		String result = (String) internalNames_.get(javaName);
		if (result != null)
			return result;
		int index = javaName.indexOf('[');
		result = index > -1 ? javaName.substring(0, index) : javaName;
		if (result.equals("byte"))
			result = "B";
		else if (result.equals("char"))
			result = "C";
		else if (result.equals("double"))
			result = "D";
		else if (result.equals("float"))
			result = "F";
		else if (result.equals("int"))
			result = "I";
		else if (result.equals("long"))
			result = "J";
		else if (result.equals("void"))
			result = "V";
		else if (result.equals("boolean"))
			result = "Z";
		else if (result.equals("short"))
			result = "S";
		else
			result = "L" + result.replace('.', JAVA_FILE_SEPARATOR_SLASH) + ";";

		if (index > -1) {
			StringBuffer s = new StringBuffer();
			for (int n = (javaName.length() - index) / 2; n > 0; n--)
				s.append("[");
			s.append(result);
			result = s.toString();
		}
		internalNames_.put(javaName, result);
		return result;
	}

	/**
	 An abbreviation of {@link BT_ConstantPool#toInternalName(String) toInternalName(c.name)}.
	 Also see {@link BT_ConstantPool#toJavaName}.
	**/
	public static String toInternalName(BT_Class c) {
		return toInternalName(c.name);
	}

	/**
	 Skips to the next type in a list of method argument types.
	 @param  sig  E.g., "LClassName1;LClassName2;[[I".
	**/
	public static int nextSig(String sig, int index) {
		while (sig.charAt(index) == '[')
			index++;
		if (sig.charAt(index) == 'L')
			while (sig.charAt(index) != ';')
				index++;
		index++;
		return index;
	}

	/**
	 Returns the classes that represent the types of the given signature.
	
	 @param  intSig  A method signature in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., "(LClassName1;LClassName2;[[I)V".
	 @see #getReturnType
	 @see BT_MethodSignature#toExternalArgumentString().
	**/
	public static BT_ClassVector getArgumentTypes(String intSig, BT_Repository repo) throws BT_DescriptorException {
		BT_ClassVector v = new BT_ClassVector();
		int i = 1;
		while (intSig.charAt(i) != ')') {
			v.addElement(repo.forName(toJavaName(intSig, i)));
			i = nextSig(intSig, i);
		}
		return v;
	}

	static BT_ClassVector linkToArgumentTypes(String intSig, BT_Repository repo) throws BT_DescriptorException {
		BT_ClassVector v = new BT_ClassVector();
		int i = 1;
		while (intSig.charAt(i) != ')') {
			v.addElement(repo.linkTo(toJavaName(intSig, i)));
			i = nextSig(intSig, i);
		}
		return v;
	}
	
	/**
	 * Counts the size of the arguments in a signature string.
	 @param  intSig  A method signature in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., "(LClassName1;LClassName2;[[I)V".
	 */
	static int getArgsSize(String intSig) {
		int count = 0;
		int i = 1;
		while (intSig.charAt(i) != ')') {
			count++;
			char c = intSig.charAt(i);
			switch (c) {
			  case 'D':
			  case 'J':
			  	count++;
			  	break;
			  default:
				break;
			}
			i = BT_ConstantPool.nextSig(intSig, i);
		}
		return count;
	}
	
	/**
	 * Count the number of arguments in a signature string.
	 @param  intSig  A method signature in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., "(LClassName1;LClassName2;[[I)V".
	 */
	static int getArgsCount(String intSig) {
		int count = 0;
		int i = 1;
		while (intSig.charAt(i) != ')') {
			count++;
			i = BT_ConstantPool.nextSig(intSig, i);
		}
		return count;
	}
	
	/**
	 Returns the class that represent the return type in the given signature.
	
	 @param  shortName  A method signature in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#internal_format>internal format</a>}.
	   E.g., "(LClassName1;LClassName2;[[I)V".
	 @return  The return type in
	   {@link <a href=../jikesbt/doc-files/Glossary.html#external_format>external format</a>}.
	 @see #getArgumentTypes
	**/
	public static String getReturnType(String shortName) throws BT_DescriptorException {
		return toJavaName(shortName, shortName.indexOf(')') + 1);
	}

	/**
	 Returns the number of items in the constant pool plus one for the
	 existing representative of the non-existant zeroth item.
	**/
	public int size() {
		return items.size();
	}

	/**
	 @return  The requested element in the constant pool.
	**/
	public BT_ConstantPoolItem elementAt(int n) {
		return items.elementAt(n);
	}

	/**
	 Prints this constant pool.
	**/
	public void print(PrintStream ps) {
		ps.print("Pool:");
		for (int n = 1; n < items.size(); n++)
			ps.print("    " + n + " " + items.elementAt(n));
		ps.print("");
	}

	public String toString() {
		String result = "Pool: ";
		for (int n = 1; n < items.size(); n++) {
			result += endl() + "   " + n + " " + items.elementAt(n);		
		}
		return result + endl();
	}

	/**
	 A value that can be returned by {@link BT_ConstantPool#getEntryTypeAt}.
	**/
	public static final byte UTF8 = 1;
	public static final byte INTEGER = 3;
	public static final byte FLOAT = 4;
	public static final byte LONG = 5;
	public static final byte DOUBLE = 6;
	public static final byte CLASS = 7;
	public static final byte STRING = 8;
	public static final byte FIELDREF = 9;
	public static final byte METHODREF = 10;
	public static final byte INTERFACEMETHODREF = 11;
	public static final byte NAMEANDTYPE = 12;
	public static final byte DUMMY = 13;
}
