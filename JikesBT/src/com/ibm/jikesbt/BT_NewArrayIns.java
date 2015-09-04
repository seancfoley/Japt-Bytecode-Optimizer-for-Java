package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an opc_newarray instruction that allocates an array of primitives.
 Also see {@link BT_ANewArrayIns}, {@link BT_MultiANewArrayIns}, and {@link
 BT_NewIns}.
 Typically created by one of the {@link BT_Ins#make} methods.

 * @author IBM
**/
public final class BT_NewArrayIns extends BT_NewIns {

	/**
	 Types as represented in newarray instruction operands.
	 See {@link BT_Class#getBoolean()}, ..., {@link BT_Class#getLong()} for
	 types represented as classes.
	**/
	public static final short T_BOOLEAN = 4;
	public static final short T_CHAR = 5;
	public static final short T_FLOAT = 6;
	public static final short T_DOUBLE = 7;
	public static final short T_BYTE = 8;
	public static final short T_SHORT = 9;
	public static final short T_INT = 10;
	public static final short T_LONG = 11;
	
	/**
	 * The bytecode for a 'newarray' instruction actually refers to the element class
	 * and not the array class itself.  This is in contrast to the 'multianewarray' 
	 * instruction and the 'new' instruction, but similar to 'anewarray'.
	 * 
	 * The target field of a new instruction always refers to the created class.
	 * So both constructors here translate from the element class referred to in the 
	 * bytecode and initialize target as the array class that is actually created.
	 */
	
	/**
	 For application use via {@link BT_Ins#make}.
	**/
	BT_NewArrayIns(BT_Class primitive) {
		super(opc_newarray);
		if (primitive == null)
			assertFailure(Messages.getString("JikesBT.The_class_must_be_a_primitive____not_null_1"));
		if(!primitive.isBasicTypeClass) {
			assertFailure(Messages.getString("JikesBT.The_class_must_be_a_primitive____not_{0}_2", primitive));
		}
		target = primitive.getArrayClass();
	}
	
	/**
	 One of {@link BT_Misc#T_BOOLEAN} thru {@link BT_Misc#T_LONG}.
	**/
	int getTypeNumber() {
		BT_Class primitive = target.arrayType;
		BT_Repository repository = primitive.getRepository();
		if (primitive == repository.getBoolean())
			return T_BOOLEAN;
		else if (primitive == repository.getByte())
			return T_BYTE;
		else if (primitive == repository.getChar())
			return T_CHAR;
		else if (primitive == repository.getDouble())
			return T_DOUBLE;
		else if (primitive == repository.getFloat())
			return T_FLOAT;
		else if (primitive == repository.getInt())
			return T_INT;
		else if (primitive == repository.getLong())
			return T_LONG;
		else if (primitive == repository.getShort())
			return T_SHORT;
		else 
			throw new IllegalStateException(Messages.getString("JikesBT.The_class_must_be_a_primitive____not_{0}_2", primitive));
	}

	/**
	 For use when reading a class-file.
	
	 @param index  The byte offset of the instruction.
	   -1 mean unknown?
	**/
	BT_NewArrayIns(int opcode, int index, short typeNumber, BT_Repository repository)
		throws BT_ClassFileException {
		super(opcode, index);
		if (CHECK_USER && opcode != opc_newarray)
			expect(Messages.getString("JikesBT.Invalid_opcode_for_this_constructor___4") + opcode);
		BT_Class primitive;
		switch (typeNumber) {
			case T_BOOLEAN :
				primitive = repository.getBoolean();
				break;
			case T_BYTE :
				primitive = repository.getByte();
				break;
			case T_CHAR :
				primitive = repository.getChar();
				break;
			case T_DOUBLE :
				primitive = repository.getDouble();
				break;
			case T_FLOAT :
				primitive = repository.getFloat();
				break;
			case T_INT :
				primitive = repository.getInt();
				break;
			case T_LONG :
				primitive = repository.getLong();
				break;
			case T_SHORT :
				primitive = repository.getShort();
				break; // Ok
			default :
				throw new BT_ClassFileException(Messages.getString("JikesBT.invalid_primitive_type_in_newarray_instruction_4"));
		}
		target = primitive.getArrayClass();
	}

	public void link(BT_CodeAttribute code) {
		BT_CreationSite site = target.addCreationSite(this, code);
		if(site != null) {
			code.addCreatedClass(site);
			super.linkClassReference(site, target);
		}
	}
	
	public void unlink(BT_CodeAttribute code) {
		super.unlinkReference(code);
		target.removeCreationSite(this);
		code.removeCreatedClass(this);
	}
	
	public boolean isNewArrayIns() {
		return true;
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		return false;
	}
	
	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {}
	
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		dos.writeByte(getTypeNumber());
		if (CHECK_JIKESBT && size() != 2)
			assertFailure(
				Messages.getString("JikesBT.Write/size_error_{0}_3", this)
					+ Messages.getString("JikesBT._expected_{0}_got_{1}_7", 
						new Object[] {Integer.toString(2), Integer.toString(size())}));
	}
	
	public Object clone() {
		return new BT_NewArrayIns(target.arrayType);
	}
	
	public String toString() {
		return getPrefix() + BT_Misc.opcodeName[opcode] + " " + target.arrayType.name;
	}
	
	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode] + " " + target.arrayType.name;
	}
	
	
	
}
