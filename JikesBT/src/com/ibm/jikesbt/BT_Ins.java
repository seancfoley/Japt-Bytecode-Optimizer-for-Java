package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InvalidInstructionException;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Represents a JVM instruction.

 * @author IBM
 * 
 * 	
 The hierarchy:
              BT_Ins
                    BT_BasicBlockMarkerIns: a zero-length instruction that represents jump targets and exception handler blocks.
                    BT_ClassRefIns: instructions that refer to classes
                          BT_CheckCastIns
                          BT_InstanceofIns
                          BT_NewIns: allocates a new non-array object
                          	  BT_MultiANewArrayIns: allocates a multi-dimensional array
                              BT_ANewArrayIns: allocates an object array
                              BT_NewArrayIns: allocates a primitive array
                    BT_ConstantIns: load a constant
                          BT_ConstantFloatIns
                          BT_ConstantIntegerIns
                          BT_ConstantStringIns: load a java.lang.String
                          BT_ConstantClassIns: load a java.lang.Class object
                          BT_ConstantWideIns
                                BT_ConstantDoubleIns
                                BT_ConstantLongIns
                    BT_DoubleOperationIns
                    BT_Dup2Ins
                    BT_DupIns
                    BT_FieldRefIns: a field access instruction
                    BT_IIncIns
                    BT_IntegerOperationIns
                    BT_JumpIns: a jump instruction - either a jsr, goto or a conditional jump
                          BT_JumpOffsetIns
                    BT_LocalIns: a local variable access instruction
                          BT_LoadLocalIns
                          BT_StoreLocalIns
                    BT_LongOperationIns
                    BT_MethodRefIns: a method invocation instruction
                          BT_InvokeInterfaceIns
                          BT_InvokeSpecialIns
                          BT_InvokeStaticIns
                          BT_InvokeVirtualIns
                    BT_NopIns
                    BT_NullIns
                    BT_RetIns: a subroutine ret instruction
                    BT_SwitchIns: a switch instruction
                          BT_LookupSwitchIns
                          BT_TableSwitchIns
**/
public class BT_Ins extends BT_Base implements BT_Opcodes, Cloneable {

//TODO if we ever try to reuse instructions xxx only instructions whose link and unlink are empty can be reused
//xxx also, we cannot reuse the same instruction in the same method (because of byteIndex field) - so we should have a mechanism to handle that;
//xxx find all instruction comparisons, replace with a call to isSame;

	public boolean isSame(BT_Ins ins) {
		return this == ins;
	}
	
	/**
	 The opcode at the time this instruction was created or written.
	
	 <p> For some types of instruction, the opcode can be changed when the
	 instruction is written due to movement of data (changes in offsets).
	 E.g., when an opc_ldc is written, the opcode may be changed to opc_ldc_w,
	 or vice versa.
	
	 <p> For other types of instruction, the opcode can be changed due to the
	 size of the value of the operand.
	**/
	public int opcode;

	/**
	 Normally the byte offset of the instruction.
	**/
	public int byteIndex;

	static final boolean debugOptimization = false;

	/**
	 Typically called by one of the {@link BT_Ins#make} methods.
	**/
	public BT_Ins(int opcode) {
		this(opcode, -1);
	}

	/**
	 Typically called by one of the {@link BT_Ins#make} methods.
	 @param index  The byte offset of the instruction.
	   -1 mean unknown.
	**/
	public BT_Ins(int opcode, int index) {
		this.opcode = opcode;
		this.byteIndex = index;
	}

	public Object clone() {
		return new BT_Ins(opcode);
	}

	/**
	 Returns the size of the instruction.
	 Some instructions depend on the value of byteIndex being correct
	 in order to calculate the size, e.g. LookupSwitch and TableSwitch
	 that insert padding depending on the byteIndex value.
	 
	 BT_LocalIns instructions can change in size (wide or not) based upon their index into the local variables.
	 
	 BT_JumpIns instructions can change in size based upon their target location.
	 jsr and goto instructions can become wide or not wide, while conditional jumps
	 can be turned into a combination of the conditional jump, a goto and a goto wide if the
	 target offset is large enough.
	 
	 BT_ConstantIns instructions can change in size based upon their index into the constant pool
	 
	 This method does not change the opcode of the instruction and thus the size is calculated based upon
	 the current state of the instruction.  The size may change when the resolve method is called.
	 
	 It is appropriate to call this method just after instructions have been created or just after they have
	 been resolved.  At other times it is likely best to call maxSize(), since instruction sizes may vary,
	 and maxSize will give a better estimate of eventual instruction sizes.
	**/
	public int size() {
		return BT_Misc.opcodeLength[opcode & 0xff];
	}
	
	/**
	 * Calculates the maximum size of the instruction irrespective of the constant pool.  
	 * This will attempt to calculate the opcode of the instruction and thus the instruction state may change.  
	 * 
	 * A few instructions have sizes that are variable depending upon the size of the constant pool (namely ldc versus ldc_w, 
	 * BT_ConstantClassIns, BT_ConstantStringIns, BT_ConstantFloatIns and BTConstantIntegerIns).  
	 * Such instructions will return their maximum possible size.
	 * 
	 * Some instructions sizes vary depending upon the instructions they reference, particularly jump instructions,
	 * and these instructions will return a value based upon the currently stored byte index of their 
	 * target instructions.
	 * 
	 * Some instruction sizes vary depending upon the index of the local variable they target, those instructions will
	 * return a value based upon the local variable currently referenced.
	 */
	public int maxSize() {
		return size();
	}
	

	/**
	 Sets the byte offset (byteIndex) of this instruction within its instruction
	 stream.
	**/
	public void setByteIndex(int offset) {
		byteIndex = offset;
	}

	
	
	/**
	 * Optimize instructions by replacing them with an equivalent shorter or faster 
	 * set of instructions. As we only look at the current instruction and perhaps a few more
	 * following this one, this kind of optimization is referred to in the literature
	 * as "peep hole optimization".
	 * <p>
	 * Because JikesBT inserts pseude {@link BT_BasicBlockMarker} instructions when it 
	 * reads a method, it is save to replace any pattern by another, as there will be no
	 * instruction jumping into the middle of a recognized pattern.
	 * <p>
	 * Basic optimizations are performed here. The interesting ones are done by the
	 * subclasses of {BT_Ins}.
	 * @return true if an optimization has occurred
	 */
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		// If instruction performs a division, an ArithmeticException can be thrown as
		// a side effect, and therefore we can't remove the instruction.
		// If instruction performs an array access, an IndexOutOfBoundsException can be thrown as
		// a side effect, and therefore we can't remove the instruction.
		if ((BT_Misc.opcodeRuntimeExceptions[opcode]
			& (BT_Misc.ARRAYCHECK | BT_Misc.DIVZEROCHECK))
			!= 0)
			return false;

		//
		// remove any instruction that pushes one word on the stack and
		// that is followed by a pop
		//
		BT_InsVector instructions = code.getInstructions();
		if (instructions.size() > n + 1
			&& BT_Misc.opcodeStackHeight[opcode][1] == 1
			&& instructions.elementAt(n + 1).opcode == opc_pop) {
			switch (getStackDiff()) {
				case 1 :
					return code.removeInstructionsAt(2, n);
				case 0 :
					return code.removeInstructionAt(n);
				case -1 :
					return code.replaceInstructionsAtWith(2, n, make(opc_pop2));
				case -2 :
					return code.replaceInstructionsAtWith(
						2,
						n,
						make(opc_pop),
						make(opc_pop2));
				default :
					return false;
			}

		}

		//
		// remove any instruction that pushes two words on the stack and
		// that is followed by a pop2
		//
		if (instructions.size() > n + 1
			&& BT_Misc.opcodeStackHeight[opcode][1] == 2
			&& instructions.elementAt(n + 1).opcode == opc_pop2) {
			switch (getStackDiff()) {
				case 2 :
					return code.removeInstructionsAt(2, n);
				case 1 :
					return code.replaceInstructionsAtWith(2, n, make(opc_pop));
				case 0 :
					return code.removeInstructionAt(n);
				case -1 :
					return code.replaceInstructionsAtWith(1, n, make(opc_pop));
				case -2 :
					return code.replaceInstructionsAtWith(1, n, make(opc_pop2));
				default :
					return false;
			}
		}

		return false;
	}

	/**
	 Links instructions directly to objects with which they have a relationship, e.g. methods,
	 classes, other instructions.
	 E.g., sets the proper opcode, links to branch target instructions, records that this
	 instruction accesses a field, invokes a method, or allocates a class, records that this
	 instruction references a local, ....
	 
	 Also creates relationship links between code, methods, fields and classes that
	 have been initiated by this instruction.  For instance, a method invoke instruction
	 initiates a relationship between the code attribute doing the invoke and the method being
	 invoked.
	
	 <p> This is called while a method is being read.
	
	 <p> For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>.
	
	 @param  items  The instructions that will (or do?) contain this.
	**/
	public void link(BT_CodeAttribute code) {}

	/**
	 Unlinks the instruction from the given code attribute.  Undos links creaded
	 by {@link BT_Ins#dereference(BT_InsVector)}
	 
	 Unlinks relationships between code, methods, fields and classes that
	 have been initiated by this instruction.  For instance, a method invoke instruction
	 initiates a relationship between the code attribute doing the invoke and the method being
	 invoked.
	 
	 This is called when the instruction is being removed or replaced.
	 
	 @param code the code that possesses the instruction.
	**/
	public void unlink(BT_CodeAttribute code) {}
	// ----------------------------------
	// Target-related ...

	/**
	 Returns null or the method this instruction references.
	**/
	public BT_Method getMethodTarget() {
		return null;
	}
	/**
	 Returns null or the field this instruction references.
	**/
	public BT_Field getFieldTarget() {
		return null;
	}
	/**
	 Returns null or the class this instruction references.
	**/
	public BT_Class getClassTarget() {
		return null;
	}
	
	public String getInstructionTarget() {
		return null;
	}
	
	/**
	 Returns null or the class this instruction references once resolved.
	**/
	public BT_Class getResolvedClassTarget(BT_CodeAttribute code) {
		return null;
	}


	/**
	 Sets the non-primitive class referred to by this instruction, including
	 invoking {@link BT_Ins#remove} and {@link #dereference}.
	 
	 @see  #getClassTarget
	**/
	public void resetTarget(BT_Class m, BT_CodeAttribute owner) {
		throw new UnsupportedOperationException();
	}

	/**
	 Sets the method referred to by this instruction, including invoking
	 {@link BT_Ins#remove} and {@link #dereference}.
	 
	 @see  #getMethodTarget
	**/
	public void resetTarget(BT_Method m, BT_CodeAttribute owner) {
		throw new UnsupportedOperationException();
	}

	/**
	 Sets the field referred to by this instruction, including invoking {@link
	 #remove} and {@link BT_Ins#dereference}.
	 
	 @see  #getFieldTarget
	**/
	public void resetTarget(BT_Field m, BT_CodeAttribute owner) {
		throw new UnsupportedOperationException();
	}

	/**
	 Returns the integer associated with this {@link BT_ConstantIntegerIns}.
	 This is the same as doing "(BT_ConstantIntegerIns)ins.getIntValue()"
	 except a cast is avoided.
	**/
	//   One may consider making this version throw a
	//     RuntimeException to indicate the call was made for an instruction that
	//     doesn't support int values.  
	public int getIntValue() {
		throw new UnsupportedOperationException();
	}
	// ----------------------------------

	/**
	 Makes room for parameters.
	
	 @param  inc  The increment.
	 @param  start  The number of the first "local-variable local" (i.e., the
	 first local after any parameters).
	**/
	public void incrementLocalsAccessWith(
		int inc,
		int start,
		BT_LocalVector locals) {
		// default implementation, do nothing
	}
	
	/**
	 Build the constant pool, ... in preparation for writing the class file.
	 <p> For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#resolve_method>resolve method</a>.
	**/
	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {}

	/**
	 If this instruction refers to "oldIns", changes it to refer to "newIns".
	**/
	public void changeReferencesFromTo(BT_BasicBlockMarkerIns oldIns, BT_BasicBlockMarkerIns newIns) {}

	/**
	 Writes this instruction to a class file.
	**/
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		if (size() != 1)
			throw new BT_InvalidInstructionSizeException(
				Messages.getString("JikesBT.Writing_one_byte_instruction_{0}_size_{1}_class_{2}_12", 
					new Object[] {this, Integer.toString(size()), getClass().getName()}));
		dos.writeByte(opcode);
	}

	/**
	 Instantiates a new ...Ins object of the proper type depending on the opcode.
	**/
	public static BT_Ins make(int opcode) {
		switch (opcode) {
			case opc_aload_0 :
			case opc_aload_1 :
			case opc_aload_2 :
			case opc_aload_3 :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					(opcode - opc_aload_0));
			case opc_astore_0 :
			case opc_astore_1 :
			case opc_astore_2 :
			case opc_astore_3 :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					(opcode - opc_astore_0));
			case opc_iload_0 :
			case opc_iload_1 :
			case opc_iload_2 :
			case opc_iload_3 :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					(opcode - opc_iload_0));
			case opc_istore_0 :
			case opc_istore_1 :
			case opc_istore_2 :
			case opc_istore_3 :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					(opcode - opc_istore_0));
			case opc_lload_0 :
			case opc_lload_1 :
			case opc_lload_2 :
			case opc_lload_3 :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					(opcode - opc_lload_0));
			case opc_lstore_0 :
			case opc_lstore_1 :
			case opc_lstore_2 :
			case opc_lstore_3 :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					(opcode - opc_lstore_0));
			case opc_fload_0 :
			case opc_fload_1 :
			case opc_fload_2 :
			case opc_fload_3 :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					(opcode - opc_fload_0));
			case opc_fstore_0 :
			case opc_fstore_1 :
			case opc_fstore_2 :
			case opc_fstore_3 :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					(opcode - opc_fstore_0));
			case opc_dload_0 :
			case opc_dload_1 :
			case opc_dload_2 :
			case opc_dload_3 :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					(opcode - opc_dload_0));
			case opc_dstore_0 :
			case opc_dstore_1 :
			case opc_dstore_2 :
			case opc_dstore_3 :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					(opcode - opc_dstore_0));
			case opc_nop :
				return new BT_NopIns(-1);
			case opc_aconst_null :
				return new BT_NullIns(opcode, -1);
			case opc_iconst_m1 :
			case opc_iconst_0 :
			case opc_iconst_1 :
			case opc_iconst_2 :
			case opc_iconst_3 :
			case opc_iconst_4 :
			case opc_iconst_5 :
				return new BT_ConstantIntegerIns(opcode, opcode - opc_iconst_0);
			case opc_lconst_0 :
			case opc_lconst_1 :
				return new BT_ConstantLongIns(opcode, opcode - opc_lconst_0);
			case opc_fconst_0 :
			case opc_fconst_1 :
			case opc_fconst_2 :
				return new BT_ConstantFloatIns(opcode, opcode - opc_fconst_0);
			case opc_dconst_0 :
			case opc_dconst_1 :
				return new BT_ConstantDoubleIns(opcode, opcode - opc_dconst_0);
			case opc_dup :
				return new BT_DupIns(opcode, -1);
			case opc_dup2 :
				return new BT_Dup2Ins(opcode, -1);
		}
		return new BT_Ins(opcode, -1);
	}

	public static BT_Ins make(int opcode, BT_BasicBlockMarkerIns i) {
		switch (opcode) {
			case opc_jsr :
			case opc_jsr_w :
			case opc_goto :
			case opc_goto_w :
				return new BT_JumpOffsetIns(opcode, -1, i);
			case opc_ifeq :
			case opc_ifne :
			case opc_iflt :
			case opc_ifge :
			case opc_ifgt :
			case opc_ifle :
			case opc_if_icmpeq :
			case opc_if_icmpne :
			case opc_if_icmplt :
			case opc_if_icmpge :
			case opc_if_icmpgt :
			case opc_if_icmple :
			case opc_if_acmpeq :
			case opc_if_acmpne :
			case opc_ifnull :
			case opc_ifnonnull :
				return new BT_JumpOffsetIns(opcode, -1, i);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	public static BT_Ins make(int opcode, BT_Method m, BT_Class throughClass) {
		//if(!throughClass.equals(m.getDeclaringClass()) && !m.getDeclaringClass().isAncestorOf(throughClass)) {
		//	throw new IllegalArgumentException();
		//}
		switch (opcode) {
			case opc_invokestatic :
				return new BT_InvokeStaticIns(m, throughClass);
			case opc_invokevirtual :
				return new BT_InvokeVirtualIns(m, throughClass);
			case opc_invokespecial :
				return new BT_InvokeSpecialIns(m, throughClass);
			case opc_invokeinterface :
				return new BT_InvokeInterfaceIns(m, throughClass);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}
	
	public static BT_Ins make(int opcode, BT_Method m) {
		return make(opcode, m, m.getDeclaringClass());
	}

	public static BT_Ins make(int opcode, BT_Field f) {
		return make(opcode, f, f.getDeclaringClass());
	}
	
	public static BT_Ins make(int opcode, BT_Field f, BT_Class throughClass) {
//		if(!throughClass.equals(m.getDeclaringClass()) && !m.getDeclaringClass().isAncestorOf(throughClass)) {
		//	throw new IllegalArgumentException();
		//}
		switch (opcode) {
			case opc_getfield :
			case opc_putfield :
			case opc_getstatic :
			case opc_putstatic :
				return new BT_FieldRefIns(opcode, f, throughClass);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	/**
	 @param  c  The type to be allocated.
	   If opcode==opc_newarray, the type should be a primitive (e.g., {@link BT_Class#getInt()}).
	**/
	public static BT_Ins make(int opcode, BT_Class c) {
		switch (opcode) {
			case opc_new :
				return new BT_NewIns(c);
			case opc_newarray :
				return new BT_NewArrayIns(c);
			case opc_anewarray :
				return new BT_ANewArrayIns(c);
			case opc_instanceof :
				return new BT_InstanceofIns(c);
			case opc_checkcast :
				return new BT_CheckCastIns(c);
			case opc_ldc :
				return new BT_ConstantClassIns(opcode, -1, c);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	/**
	 @param  c  The type to be allocated.
	**/
	public static BT_Ins make(int opcode, BT_Class c, short dimensions) {
		switch (opcode) {
			case opc_multianewarray :
				return new BT_MultiANewArrayIns(opcode, c, dimensions);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	public static BT_Ins make(int opcode, int value, short constant) {
		switch (opcode) {
			case opc_iinc :
				return new BT_IIncIns(opcode, -1, value, constant);
		}
		throw new IllegalArgumentException(
				Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}
	
	public static BT_Ins make(int opcode, int value) {
		switch (opcode) {
			case opc_bipush :
			case opc_sipush :
			case opc_ldc :
				return new BT_ConstantIntegerIns(opcode, -1, value);
			case opc_astore :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					value);
			case opc_istore :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					value);
			case opc_lstore :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					value);
			case opc_fstore :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					value);
			case opc_dstore :
				return new BT_StoreLocalIns(
					opcode,
					-1,
					value);
			case opc_aload :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					value);
			case opc_iload :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					value);
			case opc_lload :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					value);
			case opc_fload :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					value);
			case opc_dload :
				return new BT_LoadLocalIns(
					opcode,
					-1,
					value);
			case opc_ret :
				return new BT_RetIns(opcode, -1, value);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	public static BT_Ins make(int opcode, float value) {
		switch (opcode) {
			case opc_ldc :
				return new BT_ConstantFloatIns(opcode, -1, value);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	public static BT_Ins make(int opcode, double value) {
		switch (opcode) {
			case opc_ldc2_w :
				return new BT_ConstantDoubleIns(opcode, -1, value);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	public static BT_Ins make(int opcode, long value) {
		switch (opcode) {
			case opc_ldc2_w :
				return new BT_ConstantLongIns(opcode, -1, value);
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}
	
	public static BT_Ins make(int opcode, String value, BT_Repository repository) {
		switch (opcode) {
			case opc_ldc :
				return new BT_ConstantStringIns(opcode, -1, value, repository.findJavaLangString());
		}
		throw new IllegalArgumentException(
			Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}

	public static BT_Ins make(int opcode, Object value, BT_Repository repository) {
		if(value instanceof BT_Ins) {
			throw new IllegalArgumentException("invalid value");
		}
		switch (opcode) {
			case opc_ldc :
					return new BT_ConstantStringIns(opcode, -1, value, repository.findJavaLangString());
		}
		throw new IllegalArgumentException(
				Messages.getString("JikesBT.Unsupported_opcode_{0}_14", BT_Misc.opcodeName[opcode]));
	}
	
	public static BT_BasicBlockMarkerIns make() {
		return new BT_BasicBlockMarkerIns();
	}
	
	public static BT_BasicBlockMarkerIns make(String label) {
		return new BT_BasicBlockMarkerIns(label);
	}

	/**
	 In JikesBT, called only by BT_CodeAttribute.dereference().
	 @param  data  Bytecodes from the class file (starting at __).
	 @param  offset  The offset of the instruction in the bytecodes.
	 @param  ins  Needed only for .locals
	 @param  item  The BT_Method that will contain this instruction.
	**/
	static BT_Ins make(byte[] data, int offset, BT_CodeAttribute code, BT_Method method, LoadLocation loadedFrom)
		 throws BT_ClassFileException, BT_ConstantPoolException, BT_DescriptorException {
		BT_ConstantPool pool = method.getPool();
		int opcode = data[offset] & 0xff;
		switch (opcode) {

			case opc_iload_0 :
			case opc_iload_1 :
			case opc_iload_2 :
			case opc_iload_3 :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_iload_0));
			case opc_lload_0 :
			case opc_lload_1 :
			case opc_lload_2 :
			case opc_lload_3 :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_lload_0));
			case opc_fload_0 :
			case opc_fload_1 :
			case opc_fload_2 :
			case opc_fload_3 :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_fload_0));
			case opc_dload_0 :
			case opc_dload_1 :
			case opc_dload_2 :
			case opc_dload_3 :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_dload_0));
			case opc_aload_0 :
			case opc_aload_1 :
			case opc_aload_2 :
			case opc_aload_3 :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_aload_0));
			case opc_istore_0 :
			case opc_istore_1 :
			case opc_istore_2 :
			case opc_istore_3 :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_istore_0));
			case opc_lstore_0 :
			case opc_lstore_1 :
			case opc_lstore_2 :
			case opc_lstore_3 :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_lstore_0));
			case opc_fstore_0 :
			case opc_fstore_1 :
			case opc_fstore_2 :
			case opc_fstore_3 :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_fstore_0));
			case opc_dstore_0 :
			case opc_dstore_1 :
			case opc_dstore_2 :
			case opc_dstore_3 :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_dstore_0));
			case opc_astore_0 :
			case opc_astore_1 :
			case opc_astore_2 :
			case opc_astore_3 :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					(opcode - opc_astore_0));
			case opc_iload :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_lload :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_fload :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_dload :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_aload :
				return new BT_LoadLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_istore :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_lstore :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_fstore :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_dstore :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_astore :
				return new BT_StoreLocalIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_newarray :
				return new BT_NewArrayIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1),
					pool.getRepository());
			case opc_ret :
				return new BT_RetIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1));
			case opc_bipush :
				return new BT_ConstantIntegerIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToSignedByte(data, offset + 1));
			case opc_sipush :
				return new BT_ConstantIntegerIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToShort(data, offset + 1));
			case opc_iinc :
				return new BT_IIncIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToByte(data, offset + 1),
					BT_Misc.bytesToSignedByte(data, offset + 2));
			case opc_ifeq :
			case opc_ifne :
			case opc_iflt :
			case opc_ifge :
			case opc_ifgt :
			case opc_ifle :
			case opc_if_icmpeq :
			case opc_if_icmpne :
			case opc_if_icmplt :
			case opc_if_icmpge :
			case opc_if_icmpgt :
			case opc_if_icmple :
			case opc_if_acmpeq :
			case opc_if_acmpne :
			case opc_goto :
			case opc_jsr :
			case opc_ifnull :
			case opc_ifnonnull :
				return new BT_JumpOffsetIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToShort(data, offset + 1));
			case opc_goto_w :
			case opc_jsr_w :
				return new BT_JumpOffsetIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToInt(data, offset + 1));
			case opc_getstatic :
			case opc_putstatic :
			case opc_getfield :
			case opc_putfield :
				return new BT_FieldRefIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToUnsignedShort(data, offset + 1),
					method,
					loadedFrom);
			case opc_invokevirtual :
				return new BT_InvokeVirtualIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToUnsignedShort(data, offset + 1),
					method,
					loadedFrom);
			case opc_invokespecial :
				return new BT_InvokeSpecialIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToUnsignedShort(data, offset + 1),
					method,
					loadedFrom);
			case opc_invokestatic :
				return new BT_InvokeStaticIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToUnsignedShort(data, offset + 1),
					method,
					loadedFrom);
			case opc_invokeinterface :
				return new BT_InvokeInterfaceIns(
					opcode,
					offset - 8,
					BT_Misc.bytesToUnsignedShort(data, offset + 1),
					BT_Misc.bytesToByte(data, offset + 3),
					BT_Misc.bytesToByte(data, offset + 4),
					method,
					loadedFrom);
			case opc_new :
				return new BT_NewIns(
					opcode,
					offset - 8,
					pool.getClassNameAt(
						BT_Misc.bytesToUnsignedShort(data, offset + 1),
						BT_ConstantPool.CLASS),
					pool.getRepository(), code);
			case opc_anewarray :
				return new BT_ANewArrayIns(
					opcode,
					offset - 8,
					pool.getClassNameAt(
						BT_Misc.bytesToUnsignedShort(data, offset + 1),
						BT_ConstantPool.CLASS),
					pool.getRepository(), code);
			case opc_checkcast :
				return new BT_CheckCastIns(
					opcode,
					offset - 8,
					pool.getClassNameAt(
						BT_Misc.bytesToUnsignedShort(data, offset + 1),
						BT_ConstantPool.CLASS),
					pool.getRepository());
			case opc_instanceof :
				return new BT_InstanceofIns(
					opcode,
					offset - 8,
					pool.getClassNameAt(
						BT_Misc.bytesToUnsignedShort(data, offset + 1),
						BT_ConstantPool.CLASS),
					pool.getRepository());
			case opc_multianewarray :
				return new BT_MultiANewArrayIns(
					opcode,
					offset - 8,
					pool.getClassNameAt(
						BT_Misc.bytesToUnsignedShort(data, offset + 1),
						BT_ConstantPool.CLASS),
					BT_Misc.bytesToByte(data, offset + 3),
					pool.getRepository(), code);
			case opc_ldc :
			case opc_ldc_w :
				int index =
					(opcode == opc_ldc)
						? BT_Misc.bytesToByte(data, offset + 1)
						: BT_Misc.bytesToUnsignedShort(data, offset + 1);
				if (pool.getEntryTypeAt(index) == BT_ConstantPool.CLASS)
					return new BT_ConstantClassIns(
						opcode,
						offset - 8,
						pool.getClassNameAt(index, BT_ConstantPool.CLASS),
						pool.getRepository());
				if (pool.getEntryTypeAt(index) == BT_ConstantPool.STRING)
					return new BT_ConstantStringIns(
						opcode,
						offset - 8,
						pool.getStringAt(index),
						method.cls.getRepository().findJavaLangString());
				if (pool.getEntryTypeAt(index) == BT_ConstantPool.INTEGER)
					return new BT_ConstantIntegerIns(
						opcode,
						offset - 8,
						pool.getIntegerAt(index));
				if (pool.getEntryTypeAt(index) == BT_ConstantPool.FLOAT)
					return new BT_ConstantFloatIns(
						opcode,
						offset - 8,
						pool.getFloatAt(index));
				throw new BT_ClassFileException(
					Messages.getString("JikesBT.Unknown_constant_pool_type_{0}_at_index_{1}_44", 
						new Object[] {Byte.toString(pool.getEntryTypeAt(index)), Integer.toString(index)}));
			case opc_ldc2_w :
				index = BT_Misc.bytesToUnsignedShort(data, offset + 1);
				if (pool.getEntryTypeAt(index) == BT_ConstantPool.DOUBLE)
					return new BT_ConstantDoubleIns(
						opcode,
						offset - 8,
						pool.getDoubleAt(index));
				if (pool.getEntryTypeAt(index) == BT_ConstantPool.LONG)
					return new BT_ConstantLongIns(
						opcode,
						offset - 8,
						pool.getLongAt(index));
				throw new BT_ClassFileException(
					Messages.getString("JikesBT.Unknown_constant_pool_type_{0}_at_index_{1}_44", 
						new Object[] {Byte.toString(pool.getEntryTypeAt(index)), Integer.toString(index)}));
			case opc_tableswitch :
				return BT_TableSwitchIns.make(code, opcode, offset, data, method);
			case opc_lookupswitch :
				return BT_LookupSwitchIns.make(code, opcode, offset, data, method);
			case opc_dup2 :
				return new BT_Dup2Ins(opcode, offset - 8);
			case opc_dup :
				return new BT_DupIns(opcode, offset - 8);
			case opc_aconst_null :
				return new BT_NullIns(opcode, offset - 8);
			case opc_iconst_m1 :
			case opc_iconst_0 :
			case opc_iconst_1 :
			case opc_iconst_2 :
			case opc_iconst_3 :
			case opc_iconst_4 :
			case opc_iconst_5 :
				return new BT_ConstantIntegerIns(
					opcode,
					offset - 8,
					opcode - opc_iconst_0);
			case opc_lconst_0 :
			case opc_lconst_1 :
				return new BT_ConstantLongIns(
					opcode,
					offset - 8,
					opcode - opc_lconst_0);
			case opc_fconst_0 :
			case opc_fconst_1 :
			case opc_fconst_2 :
				return new BT_ConstantFloatIns(
					opcode,
					offset - 8,
					opcode - opc_fconst_0);
			case opc_dconst_0 :
			case opc_dconst_1 :
				return new BT_ConstantDoubleIns(
					opcode,
					offset - 8,
					opcode - opc_dconst_0);
			case opc_iaload :
			case opc_laload :
			case opc_faload :
			case opc_daload :
			case opc_aaload :
			case opc_baload :
			case opc_caload :
			case opc_saload :
			case opc_iastore :
			case opc_lastore :
			case opc_fastore :
			case opc_dastore :
			case opc_aastore :
			case opc_bastore :
			case opc_castore :
			case opc_sastore :

			case opc_pop :
			case opc_pop2 :
			case opc_dup_x1 :
			case opc_dup_x2 :
			case opc_dup2_x1 :
			case opc_dup2_x2 :
			case opc_swap :

			case opc_fneg :
			case opc_frem :
			case opc_fdiv :
			case opc_fmul :
			case opc_fsub :
			case opc_fadd :

			case opc_i2l :
			case opc_i2f :
			case opc_i2d :
			case opc_l2i :
			case opc_l2f :
			case opc_l2d :
			case opc_f2i :
			case opc_f2l :
			case opc_f2d :
			case opc_d2i :
			case opc_d2l :
			case opc_d2f :
			case opc_int2byte :
			case opc_int2char :
			case opc_int2short :
			case opc_lcmp :
			case opc_fcmpl :
			case opc_fcmpg :
			case opc_dcmpl :
			case opc_dcmpg :
			case opc_ireturn :
			case opc_lreturn :
			case opc_freturn :
			case opc_dreturn :
			case opc_areturn :
			case opc_return :
			//case opc_xxxunusedxxx :
			case opc_arraylength :
			case opc_athrow :
			case opc_monitorenter :
			case opc_monitorexit :
				return new BT_Ins(opcode, offset - 8);
			case opc_dadd :
			case opc_dsub :
			case opc_dmul :
			case opc_ddiv :
			case opc_drem :
			case opc_dneg :
				return new BT_DoubleOperationIns(opcode, offset - 8);
			case opc_ladd :
			case opc_lsub :
			case opc_lmul :
			case opc_ldiv :
			case opc_lrem :
			case opc_lneg :
			case opc_lshl :
			case opc_lshr :
			case opc_lushr :
			case opc_land :
			case opc_lor :
			case opc_lxor :
				return new BT_LongOperationIns(opcode, offset - 8);
			case opc_iadd :
			case opc_isub :
			case opc_imul :
			case opc_idiv :
			case opc_irem :
			case opc_ineg :
			case opc_ishl :
			case opc_ishr :
			case opc_iushr :
			case opc_iand :
			case opc_ixor :
			case opc_ior :
				return new BT_IntegerOperationIns(opcode, offset - 8);
			case opc_nop :
				return new BT_NopIns(offset - 8);
			case opc_wide :
				if(offset + 1 >= data.length) {
					throw new BT_InvalidInstructionException(code, opc_wide, offset - 8);
				}
				int nextOpcode = data[offset + 1] & 0xff;
				switch (nextOpcode) {
					case opc_iload :
						return new BT_LoadLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							BT_LocalIns.iloadPair,
							true);
					case opc_lload :
						return new BT_LoadLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							BT_LocalIns.lloadPair,
							true);
					case opc_fload :
						return new BT_LoadLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							BT_LocalIns.floadPair,
							true);
					case opc_dload :
						return new BT_LoadLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							BT_LocalIns.dloadPair,
							true);
					case opc_aload :
						return new BT_LoadLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							BT_LocalIns.aloadPair,
							true);
					case opc_istore :
						return new BT_StoreLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							true);
					case opc_lstore :
						return new BT_StoreLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							true);
					case opc_fstore :
						return new BT_StoreLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							true);
					case opc_dstore :
						return new BT_StoreLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							true);
					case opc_astore :
						return new BT_StoreLocalIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							true);
					case opc_iinc :
						return new BT_IIncIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							BT_Misc.bytesToShort(data, offset + 1 + 3),
							true);
					case opc_ret :
						return new BT_RetIns(
							nextOpcode,
							offset + 1 - 8,
							BT_Misc.bytesToUnsignedShort(data, offset + 1 + 1),
							true);
					default :
						throw new BT_InvalidInstructionException(code, opcode, offset - 8,
							Messages.getString("JikesBT.Invalid_wide_opcode_{0}_at_{1}_of_method_{2}_48",
								new Object[] {Integer.toString(opcode), Integer.toString((offset - 8)), method.fullName()}));
				}
			default :
				throw new BT_InvalidInstructionException(code, opcode, offset - 8,
					Messages.getString("JikesBT.Unknown_opcode_{0}_at_{1}_of_method_{2}_51",
						new Object[] {Integer.toString(opcode), Integer.toString((offset - 8)), method.fullName()}));
		}
	}
	
	public boolean isReturnIns() {
		return opcode >= opc_ireturn && opcode <= opc_return;
	}
	public boolean isAThrowIns() {
		return opcode == opc_athrow;
	}
	public final boolean isObjectArrayLoadIns() {
		return opcode == opc_aaload;
	}
	public final boolean isObjectArrayStoreIns() {
		return opcode == opc_aastore;
	}
	public boolean isInvokeSpecialIns() {
		return opcode == opc_invokespecial;
	}
	public boolean isInvokeVirtualIns() {
		return opcode == opc_invokevirtual;
	}
	public boolean isInvokeStaticIns() {
		return opcode == opc_invokestatic;
	}
	public boolean isInvokeInterfaceIns() {
		return opcode == opc_invokeinterface;
	}
	public boolean isInvokeIns() {
		return opcode >= opc_invokevirtual && opcode <= opc_invokeinterface;
	}
	
	public boolean isFieldAccessIns() {
		return opcode >= opc_getstatic && opcode <= opc_putfield;
	}
	
	public boolean isStaticFieldAccessIns() {
		return opcode == opc_getstatic || opcode == opc_putstatic;
	}

	/**
	 * @return True if this instruction reads from a field
	 */
	public boolean isFieldReadIns() {
		return opcode == opc_getstatic || opcode == opc_getfield;
	}
	
	/**
	 * @return True if this instruction writes to a field
	 */
	public boolean isFieldWriteIns() {
		return opcode == opc_putstatic || opcode == opc_putfield;
	}

	/**
	 True for opc_new, opc_anewarray, opc_newarray, and opc_multianewarray.
	**/
	public boolean isNewIns() {
		return opcode == opc_new
			|| opcode == opc_anewarray
			|| opcode == opc_multianewarray
			|| opcode == opc_newarray;
	}
	
	public boolean isNewArrayIns() {
		return opcode == opc_anewarray
				|| opcode == opc_multianewarray
				|| opcode == opc_newarray;
	}
	
	public boolean isCheckCastIns() {
		return opcode == opc_checkcast;
	}
	public boolean isInstanceofIns() {
		return opcode == opc_instanceof;
	}
	/**
	 * 
	 * @return true if the instruction is a throw, return or ret instruction
	 */
	public boolean hasNoSuccessor() {
		switch(opcode) {
			case opc_ret:
			case opc_athrow:
			case opc_ireturn:
			case opc_lreturn:
			case opc_freturn:
			case opc_dreturn:
			case opc_areturn:
			case opc_return:
				return true;
			default:
				return false;
		}
	}
	
	/*
	 * The following methods are similar to the above methods but instead of
	 * looking at the opcode, they simply return false, and are overridden
	 * in subclasses as necessary. 
	 */
	
	public boolean isClassRefIns() {
		return false;
	}
	public boolean isLoadConstantStringIns() {
		return false;
	}
	public boolean isLoadConstantClassIns() {
		return false;
	}
	public boolean isSwitchIns() {
		return false;
	}
	public boolean isJumpIns() {
		return false;
	}
	public boolean isGoToIns() {
		return false;
	}
	public boolean isJSRIns() {
		return false;
	}
	public boolean isLocalLoadIns() {
		return false;
	}
	public boolean isLocalReadIns() {
		return false;
	}
	public boolean isLocalWriteIns() {
		return false;
	}
	public boolean isRetIns() {
		return false;
	}
	public boolean isMultiNewArrayIns() {
		return false;
	}
	
	
	
	/**
	 @return  true if this is an invokespecial of the specified method.
	**/
	public boolean isInvokeSpecial(String className, String methodName) {
		return false;
	}
	public boolean isInvokeVirtual(String className, String methodName) {
		return false;
	}
	public boolean isInvokeVirtual(String className, String methodName, String sig) {
		return false;
	}
	public boolean isConstantIns() {
		return false;
	}
	
	public boolean isBlockMarker() {
		return false;
	}
	
	public boolean isDoubleWideConstantIns() {
		return false;
	}

	public boolean isPushConstantIns() {
		return !(isLocalReadIns() || isLocalWriteIns())
			&& BT_Misc.opcodeStackHeight[opcode][1] == 1
			&& BT_Misc.opcodeStackHeight[opcode][0] == 0;
	}

	/**
	 Returns how much executing this instruction changes the stack size.
	**/
	public int getStackDiff() {
		return getPoppedStackDiff() + getPushedStackDiff();
	}

	/**
	 Returns the impact in slots on the stack from the 
	 instruction popping entries (a negative number).
	**/
	public int getPoppedStackDiff() {
		return -BT_Misc.opcodeStackHeight[opcode][0];
	}
	
	/**
	 Returns the impact in slots on the stack from the 
	 instruction pushing entries (a positive number).
	**/
	public int getPushedStackDiff() {
		return BT_Misc.opcodeStackHeight[opcode][1];
	}

	/**
	 Returns value of this instruction (if it is a constant) concatenated with "other".
	**/
	String appendValueTo(String other) {
		return other;
	}

	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode];
	}
	
	
	protected String getPrefix() {
		/* the maximum byte index is 65535 */
		StringBuffer buffer = new StringBuffer(6);
		buffer.append(byteIndex);
		//buffer.append("     ".substring(buffer.length() - 1));
		buffer.append(' ');
		return buffer.toString();
	}
	
	public String getOpcodeName() {
		return BT_Misc.getOpcodeName(opcode);
	}
	
	public String toString() {
		return getPrefix() + BT_Misc.getOpcodeName(opcode);
	}
	/**
	 * Method getLabel.
	 * @return String
	 */
	public String getLabel() {
		return null;
	}
	
	public BT_BasicBlockMarkerIns[] getAllReferences() {
		return null;
	}

}
