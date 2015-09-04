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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Represents a field of a class.

 <p> See the <a href=../jikesbt/doc-files/UserGuide.html#BT_FIELD>User Guide<a>.

 * @author IBM
**/
public class BT_Field extends BT_Member {

	/**
	 The declared type of the field.
	**/
	FieldType fieldType;
	
	static interface FieldType {
		BT_Class getType();
		
		String getName();
	}
	
	class UndereferencedFieldType implements FieldType {
		String name;
		
		UndereferencedFieldType(String name) {
			this.name = name;
		}
		
		public BT_Class getType() {
			return cls.repository.linkTo(name);
		}
		
		public String getName() {
			return name;
		}
	}
	
	/**
	 The instructions that access this field.
	 These are updated when an accessing method is loaded.
	 Manipulated by {@link BT_Field#addAccessor} and {@link #removeAccessor}.
	**/
	public BT_AccessorVector accessors = new BT_AccessorVector();

	private Lock fieldAccessorLock = new ReentrantLock();
	
	/**
	 Initializes the main fields of a BT_Field and updates the containing class to
	 reference the field.
	**/
	private final void initialize(
		short flags,
		BT_Class type,
		String simpleName) {

		setFlags(flags);
		this.name = simpleName;
		setFieldType(type);
		this.cls.fields.addElement(this);
	}

	/**
	 Construct classes from classpath using reflection API 
	 for applications that want to use the hosting VM for loading
	 classes, instead of letting JikesBT use its own classpath.
	**/
	public void initializeFrom(java.lang.reflect.Field f) {
		name = f.getName();
		setFlags((short) f.getModifiers());
		setFieldType(cls.getRepository().forName(f.getType().getName()));
	}

	/**
	 Constructs a field.
	 The containing class is <em>not</em> updated to reference to this field.
	
	 <p> This is never called by JikesBT except in {@link BT_Factory#createField}.
	**/
	protected BT_Field(BT_Class cls) {
		super(cls);
	}

	/**
	 Constructs a field and updates the containing class to refer to this field.
	 
	 <p> This is never called by JikesBT.
	**/
	protected BT_Field(
		BT_Class cls,
		short flags,
		BT_Class type,
		String simpleName) {
		super(cls);
		initialize(flags, type, simpleName);
	}

	/**
	 An abbreviation for <code>{@link BT_Field#BT_Field(BT_Class,short,BT_Class,String) this(cls,flags,BT_Class.forName(typeName),simpleName)}</code>.
	
	 <p> This is never called by JikesBT.
	**/
	protected BT_Field(
		BT_Class cls,
		short flags,
		String typeName,
		String simpleName) {
		this(cls, flags, cls.getRepository().forName(typeName), simpleName);
	}

	/**
	 An abbreviation for <code>{@link BT_Field#BT_Field(BT_Class,short,String,String) BT_Field(cls, BT_Field.PUBLIC, typeName, simpleName)}</code>.
	
	 <p> This is never called by JikesBT.
	**/
	protected BT_Field(BT_Class cls, String typeName, String simpleName) {
		this(cls, PUBLIC, typeName, simpleName);
	}

	/**
	 Creates a field and adds it to its class.
	 Similar to a constructor, but calls {@link BT_Factory#createField} to allocate the object.
	**/
	public static BT_Field createField(
		BT_Class inClass,
		short flags,
		BT_Class ftype,
		String simpleName) {

		BT_Field f = inClass.repository.createField(inClass);
		f.initialize(flags, ftype, simpleName);
		return f;
	}
		
	/**
	 * trims all vectors related to this field that grow as new classes are loaded.  Calling this method
	 * when all loading is complete will release unused memory.
	 */
	public void trimToSize() {
		accessors.trimToSize();
	}
	
	public void resetDeclaringClassAndName(BT_Class c, String nm) {
		this.name = nm;
		this.cls = c;
	}

	public void resetName(String newName) {
		setName(newName);
	}

	public void resetDeclaringClass(BT_Class cls) {
		this.cls = cls;
	}

	/**
	 True if this field is declared volatile as determined by {@link BT_Field#flags}.
	**/
	public boolean isVolatile() {
		return areAnyEnabled(VOLATILE);
	}

	/**
	 True if this field is declared transient as determined by {@link BT_Field#flags}.
	**/
	public boolean isTransient() {
		return areAnyEnabled(TRANSIENT);
	}

	void read(DataInputStream dis, BT_ConstantPool pool, BT_Repository repo, LoadLocation loadedFrom)
		throws BT_ClassFileException, IOException {
		try {
			short readFlags = dis.readShort();
			setFlags(readFlags);
			name = pool.getSimpleNameAt(dis.readUnsignedShort());
			int allFlags =
				readFlags
					& (PERMISSION_FLAGS
						| STATIC
						| FINAL
						| VOLATILE
						| TRANSIENT);
			int accessFlags = allFlags & PERMISSION_FLAGS;
			int otherFlags = allFlags & (FINAL | VOLATILE);
			if ((cls.isInterface() && (allFlags != (PUBLIC | STATIC | FINAL)))
				|| (cls.isClass()
					&& (!(accessFlags == 0
						|| accessFlags == PUBLIC
						|| accessFlags == PRIVATE
						|| accessFlags == PROTECTED)
						|| otherFlags == (FINAL | VOLATILE)))) {
				if (BT_Factory.strictVerification)
					throw new BT_ClassFileException(
						Messages.getString("JikesBT.invalid_access_flags_for_field_{0}_1", name));
			}
			fieldType = new UndereferencedFieldType(pool.getFieldDescriptorAt(dis.readUnsignedShort()));
			attributes = BT_AttributeVector.read(dis, pool, this, this, repo, new MemberLocation(loadedFrom));
		} catch(BT_ConstantPoolException e) {
			throw new BT_ClassFileException(e);
		}
	}
	
	
	public String getTypeName() {
		return fieldType.getName();
	}
	
	/**
	 * @deprecated
	 */
	public String getType() {
		return getTypeName();
	}
	
	public BT_Class getFieldType() {
		return fieldType.getType();
	}
	
	void resolve() throws BT_ClassWriteException {
		cls.pool.indexOfUtf8(name);
		cls.pool.indexOfUtf8(BT_ConstantPool.toInternalName(getTypeName()));
		resolveFlags();
		attributes.resolve(this, cls.pool);
	}

	/**
	 Returns opc_pop or opc_pop2.
	**/
	public int getOpcodeForPop() {
		return fieldType.getType().getOpcodeForPop();
	}

	/**
	 Returns opc_dup or opc_dup2.
	**/
	public int getOpcodeForDup() {
		return fieldType.getType().getOpcodeForDup();
	}

	/**
	 Returns opc_aconst_null, opc_iconst_0, opc_dconst_0, or opc_fconst_0.
	**/
	public int getOpcodeForDefaultValue() {
		BT_Class type = fieldType.getType();
		if (type.isArray() || !type.isBasicTypeClass)
			return opc_aconst_null;
		String name = getTypeName();
		if (name.equals("byte")
			|| name.equals("char")
			|| name.equals("int")
			|| name.equals("boolean")
			|| name.equals("short")) {
			return opc_iconst_0;
		}
		if (name.equals("double"))
			return opc_dconst_0;
		if (name.equals("float"))
			return opc_fconst_0;
		if (name.equals("long"))
			return opc_lconst_0;
		throw new IllegalStateException(Messages.getString("JikesBT.Unexpected_field_type_{0}_14", name));
	}
	
	public void becomeStatic() {
		BT_Class clazz = getDeclaringClass();
		if(BT_Factory.multiThreadedLoading) {
			clazz.classLock.lock();
		}
		super.becomeStatic();
		if(BT_Factory.multiThreadedLoading) {
			clazz.classLock.unlock();
		}
	}

	BT_Accessor findAccessor(BT_FieldRefIns ins) {
		for (int n = accessors.size() - 1; n >= 0; n--)
			if (accessors.elementAt(n).instruction == ins)
				return accessors.elementAt(n);
		return null;
	}
	
	/**
	 Adds to collection {@link BT_Field#accessors} if the instruction is not already
	 recorded there.
	**/
	public BT_Accessor addAccessor(BT_FieldRefIns ins, BT_CodeAttribute caller) {
		if (!cls.repository.factory.buildMethodRelationships) {
			return null;
		}
		if(BT_Factory.multiThreadedLoading) {
			fieldAccessorLock.lock();
		}
		BT_Accessor acc = findAccessor(ins);
		if(acc == null) {
			acc = new BT_Accessor(caller, ins);
			accessors.addElement(acc);
		}
		if(BT_Factory.multiThreadedLoading) {
			fieldAccessorLock.unlock();
		}
		return acc;
	}

	/**
	 Updates {@link BT_Field#accessors}.
	**/
	public void removeAccessor(BT_Ins ins) {
		for (int n = accessors.size() - 1; n >= 0; n--)
			if (accessors.elementAt(n).instruction == ins)
				accessors.removeElementAt(n);
	}

	public void printReferences(ReferenceSelector selector) {
		BT_AccessorVector accessors = this.accessors;
		for(int j=0; j<accessors.size(); j++) {
			BT_Accessor accessor = accessors.elementAt(j);
			BT_Method from = accessor.getFrom();
			if(selector.selectReference(this, from, accessor.from)) {
				selector.printReference(this, from, accessor.getInstruction().getOpcodeName());
			}
		}
	}
	
	public void print(PrintStream ps, int printFlags) {
		boolean isAssemblerMode = (printFlags & BT_Misc.PRINT_IN_ASSEMBLER_MODE) != 0;
		String keywordString = isAssemblerMode ? modifierString() : keywordModifierString();
		if(keywordString.length() > 0) {
			keywordString += " ";
		}
		ps.println("\t" + keywordString
				+ getTypeName()
				+ " "
				+ getName() + ';');
		
		if (!isAssemblerMode) {/* assembler does not support reading attributes */
			for (int i = 0; i < attributes.size(); i++) {
				BT_Attribute attr = attributes.elementAt(i);
				attr.print(ps, "\t\t");
			}
		}
	}
	
	/**
	 * Writes out the field to a stream. Called when a class is written.
	 * This will only work well when {@link resolve} was called earlier,
	 * so that the constant pool contains the Utf8 values this field needs.
	 */
	public void write(DataOutputStream dos, BT_ConstantPool pool)
		throws IOException {
		if (isStub())
			return;
		dos.writeShort(getFlags());
		dos.writeShort(pool.indexOfUtf8(name));
		dos.writeShort(pool.indexOfUtf8(BT_ConstantPool.toInternalName(getTypeName())));
		attributes.write(dos, pool); // attributes
	}

	/**
	 Returns the fully qualified name of this item.
	 @return  The name of the item in external Java language format
	 (e.g., "java.lang.Class[][]", "C.foo" and "boolean").
	**/
	public String fullName() {
		return cls.name + "." + name;
	}

	public String useName() {
		return fullName();
	}
	
	StringBuffer flagString(StringBuffer s, short flags, boolean keywordsOnly, boolean modifiersOnly) {
		if ((flags & TRANSIENT) != 0) {
			s.append(TRANSIENT_NAME);
			s.append(' ');
		}
		if ((flags & VOLATILE) != 0) {
			s.append(VOLATILE_NAME);
			s.append(' ');
		}
		return s;
	}
	
	/**
	 A short description of this object for use in debugging.
	**/
	public String toString() {
		return fullName();
	}

	/**
	 True if has the same name and type.
	**/
	public boolean sigEquals(BT_Field other) {
		return name.equals(other.name) && getTypeName().equals(other.getTypeName());
	}

	/**
	 Replaces the contents of this field by the given field.
	**/
	protected void replaceContents(BT_Field other) {
		super.replaceContents(other);
		setFieldType(other.fieldType.getType());
	}

	/**
	 Updates this BT_Field object to directly reference related objects
	 (instead of using class file artifacts such as indices and
	 offsets to identify them).
	 This is used internally while JikesBT reads class files.
	 **/
	protected void dereference() throws BT_ClassFileException {
		if (CHECK_USER && !cls.inProject())
			expect(Messages.getString("JikesBT.Not_in_the_project__{0}_22", this));
		BT_Class type = fieldType.getType();
		setFieldType(type);
		attributes.dereference(this, cls.repository);
	}
	
	void setFieldType(BT_Class type) {
		if(fieldType != null && fieldType instanceof BT_Class) {
			((BT_Class) fieldType).removeReferencingField(this);
		}
		fieldType = type;
		type.addReferencingField(this);
	}
	
	/**
	 Removes any relationships established when dereferenced.
	 **/
	public void remove() {
		fieldType.getType().removeReferencingField(this);
		for(int i=accessors.size() - 1; i>=0; i--) {
			BT_Accessor site = accessors.elementAt(i);
			//this instruction removal does not remove the instruction from the code, 
			//instead it backs out all relationships created during dereferencing,
			//such as the storage of the call site in the code attribute
			site.instruction.unlink(site.from);
			//the instruction target remains the same and will throw an error if executed
		}
		attributes.remove();
		cls.fields.removeElement(this);
		cls.repository.removeField(this);
		setStub(true);
	}
}
