package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Various simple static things such as printing constants, primitive type
 numbers, arithmetic conversions, and opcode-related stuff.
 * @author IBM
**/
public final class BT_Misc implements BT_Opcodes {

	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

	// Flags grouped together so they occupy distinct bit positions so they can be combined.

	/**
	 A flag that can be passed to {@link BT_Method#print}, ... to suppress
	 printing of {@link BT_CodeAttribute} (instructions, local variables, and related
	 information).
	**/
	public static final int PRINT_NO_CODE = 1;

	/**
	 A flag that can be passed to {@link BT_Class#print}, ... to suppress
	 printing of {@link BT_Method}s.
	**/
	public static final int PRINT_NO_METHOD = 2 * PRINT_NO_CODE;

	/**
	 A flag that can be passed to {@link BT_Repository#print} to cause even
	 non-project classes to be printed.
	**/
	public static final int PRINT_SYSTEM_CLASSES = 2 * PRINT_NO_METHOD;

	/**
	 A flag that can be passed to {@link BT_CodeAttribute#print}, ... to suppress
	 printing instruction offset as value 0 in order to make comparing print-outs easier.
	**/
	public static final int PRINT_ZERO_OFFSETS = 2 * PRINT_SYSTEM_CLASSES;

	/**
	 A flag that can be passed to {@link BT_CodeAttribute#print}, ... to suppress
	 printing instruction offset in order to make comparing print-outs easier.
	**/
	public static final int PRINT_IN_ASSEMBLER_MODE = 2 * PRINT_ZERO_OFFSETS;

	/**
	 A flag that can be passed to {@link BT_CodeAttribute#print}, ... 
	 to print source file lines.
	**/
	public static final int PRINT_SOURCE_FILE = 2 * PRINT_IN_ASSEMBLER_MODE;


	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

	/**
	 * returns true if the given int cannot be stored in an unsigned byte value.
	 */
	public static boolean overflowsUnsignedByte(int i) {
		return i > 255;
	}
	
	/**
	 * returns true if the given int cannot be stored in an unsigned byte value.
	 */
	public static boolean overflowsSignedByte(int i) {
		return i > Byte.MAX_VALUE || i < Byte.MIN_VALUE; /* i > 127 || i < -128 */
	}
	
	/**
	 * returns true if the given int cannot be stored in an unsigned byte value.
	 */
	public static boolean overflowsUnsignedShort(int i) {
		return i > 65535;
	}
	
	/**
	 * returns true if the given int cannot be stored in an unsigned byte value.
	 */
	public static boolean overflowsSignedShort(int i) {
		return i > Short.MAX_VALUE || i < Short.MIN_VALUE; /* i > 32767 || i < -32768 */
	}
	
	/**
	 Should be named bytesToUnsignedByte.
	 Returns as short to ensure it is never negative.
	**/
	public static short bytesToByte(byte data[], int index) {
		return (short) (data[index] & 0xff);
	}

	public static short bytesToSignedByte(byte data[], int index) {
		return (short) data[index];
	}

	public static short bytesToShort(byte data[], int index) {
		return (short) (((data[index] & 0xff) << 8) | (data[index + 1] & 0xff));
	}

	public static int bytesToUnsignedShort(byte data[], int index) {
		return ((data[index] & 0xff) << 8) | (data[index + 1] & 0xff);
	}

	public static int bytesToInt(byte data[], int index) {
		return (
			((data[index] & 0xff) << 24) | (data[index + 1] & 0xff)
				<< 16
					| ((data[index + 2] & 0xff) << 8)
					| (data[index + 3] & 0xff));
	}

	
	public static String getOpcodeName(int opcode) {
		return opcodeName[opcode];
	}
	
	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	/**
	 The name of each opcode.
	**/
	public static final String opcodeName[] =
		{
			"nop",
			"aconst_null",
			"iconst_m1",
			"iconst_0",
			"iconst_1",
			"iconst_2",
			"iconst_3",
			"iconst_4",
			"iconst_5",
			"lconst_0",
			"lconst_1",
			"fconst_0",
			"fconst_1",
			"fconst_2",
			"dconst_0",
			"dconst_1",
			"bipush",
			"sipush",
			"ldc",
			"ldc_w",
			"ldc2_w",
			"iload",
			"lload",
			"fload",
			"dload",
			"aload",
			"iload_0",
			"iload_1",
			"iload_2",
			"iload_3",
			"lload_0",
			"lload_1",
			"lload_2",
			"lload_3",
			"fload_0",
			"fload_1",
			"fload_2",
			"fload_3",
			"dload_0",
			"dload_1",
			"dload_2",
			"dload_3",
			"aload_0",
			"aload_1",
			"aload_2",
			"aload_3",
			"iaload",
			"laload",
			"faload",
			"daload",
			"aaload",
			"baload",
			"caload",
			"saload",
			"istore",
			"lstore",
			"fstore",
			"dstore",
			"astore",
			"istore_0",
			"istore_1",
			"istore_2",
			"istore_3",
			"lstore_0",
			"lstore_1",
			"lstore_2",
			"lstore_3",
			"fstore_0",
			"fstore_1",
			"fstore_2",
			"fstore_3",
			"dstore_0",
			"dstore_1",
			"dstore_2",
			"dstore_3",
			"astore_0",
			"astore_1",
			"astore_2",
			"astore_3",
			"iastore",
			"lastore",
			"fastore",
			"dastore",
			"aastore",
			"bastore",
			"castore",
			"sastore",
			"pop",
			"pop2",
			"dup",
			"dup_x1",
			"dup_x2",
			"dup2",
			"dup2_x1",
			"dup2_x2",
			"swap",
			"iadd",
			"ladd",
			"fadd",
			"dadd",
			"isub",
			"lsub",
			"fsub",
			"dsub",
			"imul",
			"lmul",
			"fmul",
			"dmul",
			"idiv",
			"ldiv",
			"fdiv",
			"ddiv",
			"irem",
			"lrem",
			"frem",
			"drem",
			"ineg",
			"lneg",
			"fneg",
			"dneg",
			"ishl",
			"lshl",
			"ishr",
			"lshr",
			"iushr",
			"lushr",
			"iand",
			"land",
			"ior",
			"lor",
			"ixor",
			"lxor",
			"iinc",
			"i2l",
			"i2f",
			"i2d",
			"l2i",
			"l2f",
			"l2d",
			"f2i",
			"f2l",
			"f2d",
			"d2i",
			"d2l",
			"d2f",
			"int2byte",
			"int2char",
			"int2short",
			"lcmp",
			"fcmpl",
			"fcmpg",
			"dcmpl",
			"dcmpg",
			"ifeq",
			"ifne",
			"iflt",
			"ifge",
			"ifgt",
			"ifle",
			"if_icmpeq",
			"if_icmpne",
			"if_icmplt",
			"if_icmpge",
			"if_icmpgt",
			"if_icmple",
			"if_acmpeq",
			"if_acmpne",
			"goto",
			"jsr",
			"ret",
			"tableswitch",
			"lookupswitch",
			"ireturn",
			"lreturn",
			"freturn",
			"dreturn",
			"areturn",
			"return",
			"getstatic",
			"putstatic",
			"getfield",
			"putfield",
			"invokevirtual",
			"invokespecial",
			"invokestatic",
			"invokeinterface",
			"xxxunusedxxx",
			"new",
			"newarray",
			"anewarray",
			"arraylength",
			"athrow",
			"checkcast",
			"instanceof",
			"monitorenter",
			"monitorexit",
			"wide",
			"multianewarray",
			"ifnull",
			"ifnonnull",
			"goto_w",
			"jsr_w",
			};

	/**
	 The opcode of a given name.
	**/
	public static int getOpcodeNumber(String name) {
		for (int n = 0; n < opcodeName.length; n++)
			if (opcodeName[n].equals(name))
				return n;
		return -1;
	}

	/**
	 Instruction width by opcode.
	**/
	public static final short opcodeLength[] = { 1, /* nop */
		1, /* aconst_null */
		1, /* iconst_m1 */
		1, /* iconst_0 */
		1, /* iconst_1 */
		1, /* iconst_2 */
		1, /* iconst_3 */
		1, /* iconst_4 */
		1, /* iconst_5 */
		1, /* lconst_0 */
		1, /* lconst_1 10 */
		1, /* fconst_0 */
		1, /* fconst_1 */
		1, /* fconst_2 */
		1, /* dconst_0 */
		1, /* dconst_1 */
		2, /* bipush */
		3, /* sipush */
		2, /* ldc */
		3, /* ldc_w */
		3, /* ldc2_w   20 */
		2, /* iload */
		2, /* lload */
		2, /* fload */
		2, /* dload */
		2, /* aload */
		1, /* iload_0 */
		1, /* iload_1 */
		1, /* iload_2 */
		1, /* iload_3 */
		1, /* lload_0   30 */
		1, /* lload_1 */
		1, /* lload_2 */
		1, /* lload_3 */
		1, /* fload_0 */
		1, /* fload_1 */
		1, /* fload_2 */
		1, /* fload_3 */
		1, /* dload_0 */
		1, /* dload_1 */
		1, /* dload_2   40 */
		1, /* dload_3 */
		1, /* aload_0   42 */
		1, /* aload_1 */
		1, /* aload_2 */
		1, /* aload_3 */
		1, /* iaload */
		1, /* laload */
		1, /* faload */
		1, /* daload */
		1, /* aaload */
		1, /* baload */
		1, /* caload */
		1, /* saload */
		2, /* istore */
		2, /* lstore */
		2, /* fstore */
		2, /* dstore */
		2, /* astore */
		1, /* istore_0 */
		1, /* istore_1 */
		1, /* istore_2 */
		1, /* istore_3 */
		1, /* lstore_0 */
		1, /* lstore_1 */
		1, /* lstore_2 */
		1, /* lstore_3 */
		1, /* fstore_0 */
		1, /* fstore_1 */
		1, /* fstore_2 */
		1, /* fstore_3 */
		1, /* dstore_0 */
		1, /* dstore_1 */
		1, /* dstore_2 */
		1, /* dstore_3 */
		1, /* astore_0 */
		1, /* astore_1 */
		1, /* astore_2 */
		1, /* astore_3 */
		1, /* iastore */
		1, /* lastore */
		1, /* fastore */
		1, /* dastore */
		1, /* aastore */
		1, /* bastore */
		1, /* castore */
		1, /* sastore */
		1, /* pop */
		1, /* pop2 */
		1, /* dup */
		1, /* dup_x1 */
		1, /* dup_x2 */
		1, /* dup2 */
		1, /* dup2_x1 */
		1, /* dup2_x2 */
		1, /* swap */
		1, /* iadd */
		1, /* ladd */
		1, /* fadd */
		1, /* dadd */
		1, /* isub */
		1, /* lsub */
		1, /* fsub */
		1, /* dsub */
		1, /* imul */
		1, /* lmul */
		1, /* fmul */
		1, /* dmul */
		1, /* idiv */
		1, /* ldiv */
		1, /* fdiv */
		1, /* ddiv */
		1, /* irem */
		1, /* lrem */
		1, /* frem */
		1, /* drem */
		1, /* ineg */
		1, /* lneg */
		1, /* fneg */
		1, /* dneg */
		1, /* ishl */
		1, /* lshl */
		1, /* ishr */
		1, /* lshr */
		1, /* iushr */
		1, /* lushr */
		1, /* iand */
		1, /* land */
		1, /* ior */
		1, /* lor */
		1, /* ixor */
		1, /* lxor */
		9999, /* iinc */
		1, /* i2l */
		1, /* i2f */
		1, /* i2d */
		1, /* l2i */
		1, /* l2f */
		1, /* l2d */
		1, /* f2i */
		1, /* f2l */
		1, /* f2d */
		1, /* d2i */
		1, /* d2l */
		1, /* d2f */
		1, /* int2byte */
		1, /* int2char */
		1, /* int2short */
		1, /* lcmp */
		1, /* fcmpl */
		1, /* fcmpg */
		1, /* dcmpl */
		1, /* dcmpg */
		3, /* ifeq */
		3, /* ifne */
		3, /* iflt */
		3, /* ifge */
		3, /* ifgt */
		3, /* ifle */
		3, /* if_icmpeq */
		3, /* if_icmpne */
		3, /* if_icmplt */
		3, /* if_icmpge */
		3, /* if_icmpgt */
		3, /* if_icmple */
		3, /* if_acmpeq */
		3, /* if_acmpne */
		3, /* goto */
		3, /* jsr */
		2, /* ret */
		99, /* tableswitch */
		99, /* lookupswitch */
		1, /* ireturn      -84  */
		1, /* lreturn      -83  */
		1, /* freturn      -82  */
		1, /* dreturn      -81  */
		1, /* areturn      -80  */
		1, /* return       -79  */
		3, /* getstatic */
		3, /* putstatic */
		3, /* getfield */
		3, /* putfield */
		3, /* invokevirtual        -74 */
		3, /* invokespecial     -73 */
		3, /* invokestatic         -72 */
		5, /* invokeinterface      -71 */
		1, /* xxxunusedxxx */
		3, /* new */
		2, /* newarray */
		3, /* anewarray */
		1, /* arraylength */
		1, /* athrow */
		3, /* checkcast */
		3, /* instanceof */
		1, /* monitorenter */
		1, /* monitorexit */
		1, /* wide */
		4, /* multianewarray */
		3, /* ifnull */
		3, /* ifnonnull */
		5, /* goto_w */
		5, /* jsr_w */
	};

	/**
	 Stack size affects by opcode.
	**/
	public static final short opcodeStackHeight[][] = { { 0, 0 }, // nop
		{
			0, 1 }, // aconst_null
		{
			0, 1 }, // iconst_m1
		{
			0, 1 }, // iconst_0
		{
			0, 1 }, // iconst_1
		{
			0, 1 }, // iconst_2
		{
			0, 1 }, // iconst_3
		{
			0, 1 }, // iconst_4
		{
			0, 1 }, // iconst_5
		{
			0, 2 }, // lconst_0
		{
			0, 2 }, // lconst_1
		{
			0, 1 }, // fconst_0
		{
			0, 1 }, // fconst_1
		{
			0, 1 }, // fconst_2
		{
			0, 2 }, // dconst_0
		{
			0, 2 }, // dconst_1
		{
			0, 1 }, // bipush
		{
			0, 1 }, // sipush
		{
			0, 1 }, // ldc
		{
			0, 1 }, // ldc_w
		{
			0, 2 }, // ldc2_w
		{
			0, 1 }, // iload
		{
			0, 2 }, // lload
		{
			0, 1 }, // fload
		{
			0, 2 }, // dload
		{
			0, 1 }, // aload
		{
			0, 1 }, // iload_0
		{
			0, 1 }, // iload_1
		{
			0, 1 }, // iload_2
		{
			0, 1 }, // iload_3
		{
			0, 2 }, // lload_0
		{
			0, 2 }, // lload_1
		{
			0, 2 }, // lload_2
		{
			0, 2 }, // lload_3
		{
			0, 1 }, // fload_0
		{
			0, 1 }, // fload_1
		{
			0, 1 }, // fload_2
		{
			0, 1 }, // fload_3
		{
			0, 2 }, // dload_0
		{
			0, 2 }, // dload_1
		{
			0, 2 }, // dload_2
		{
			0, 2 }, // dload_3
		{
			0, 1 }, // aload_0
		{
			0, 1 }, // aload_1
		{
			0, 1 }, // aload_2
		{
			0, 1 }, // aload_3
		{
			2, 1 }, // iaload
		{
			2, 2 }, // laload
		{
			2, 1 }, // faload
		{
			2, 2 }, // daload
		{
			2, 1 }, // aaload
		{
			2, 1 }, // baload
		{
			2, 1 }, // caload
		{
			2, 1 }, // saload
		{
			1, 0 }, // istore
		{
			2, 0 }, // lstore
		{
			1, 0 }, // fstore
		{
			2, 0 }, // dstore
		{
			1, 0 }, // astore
		{
			1, 0 }, // istore_0
		{
			1, 0 }, // istore_1
		{
			1, 0 }, // istore_2
		{
			1, 0 }, // istore_3
		{
			2, 0 }, // lstore_0
		{
			2, 0 }, // lstore_1
		{
			2, 0 }, // lstore_2
		{
			2, 0 }, // lstore_3
		{
			1, 0 }, // fstore_0
		{
			1, 0 }, // fstore_1
		{
			1, 0 }, // fstore_2
		{
			1, 0 }, // fstore_3
		{
			2, 0 }, // dstore_0
		{
			2, 0 }, // dstore_1
		{
			2, 0 }, // dstore_2
		{
			2, 0 }, // dstore_3
		{
			1, 0 }, // astore_0
		{
			1, 0 }, // astore_1
		{
			1, 0 }, // astore_2
		{
			1, 0 }, // astore_3
		{
			3, 0 }, // iastore
		{
			4, 0 }, // lastore
		{
			3, 0 }, // fastore
		{
			4, 0 }, // dastore
		{
			3, 0 }, // aastore
		{
			3, 0 }, // bastore
		{
			3, 0 }, // castore
		{
			3, 0 }, // sastore
		{
			1, 0 }, // pop
		{
			2, 0 }, // pop2
		{
			1, 2 }, // dup
		{
			2, 3 }, // dup_x1
		{
			3, 4 }, // dup_x2
		{
			2, 4 }, // dup2
		{
			3, 5 }, // dup2_x1
		{
			4, 6 }, // dup2_x2
		{
			2, 2 }, // swap
		{
			2, 1 }, // iadd
		{
			4, 2 }, // ladd
		{
			2, 1 }, // fadd
		{
			4, 2 }, // dadd
		{
			2, 1 }, // isub
		{
			4, 2 }, // lsub
		{
			2, 1 }, // fsub
		{
			4, 2 }, // dsub
		{
			2, 1 }, // imul
		{
			4, 2 }, // lmul
		{
			2, 1 }, // fmul
		{
			4, 2 }, // dmul
		{
			2, 1 }, // idiv
		{
			4, 2 }, // ldiv
		{
			2, 1 }, // fdiv
		{
			4, 2 }, // ddiv
		{
			2, 1 }, // irem
		{
			4, 2 }, // lrem
		{
			2, 1 }, // frem
		{
			4, 2 }, // drem
		{
			1, 1 }, // ineg
		{
			2, 2 }, // lneg
		{
			1, 1 }, // fneg
		{
			2, 2 }, // dneg
		{
			2, 1 }, // ishl
		{
			3, 2 }, // lshl
		{
			2, 1 }, // ishr
		{
			3, 2 }, // lshr
		{
			2, 1 }, // iushr
		{
			3, 2 }, // lushr
		{
			2, 1 }, // iand
		{
			4, 2 }, // land
		{
			2, 1 }, // ior
		{
			4, 2 }, // lor
		{
			2, 1 }, // ixor
		{
			4, 2 }, // lxor
		{
			0, 0 }, // iinc
		{
			1, 2 }, // i2l
		{
			1, 1 }, // i2f
		{
			1, 2 }, // i2d
		{
			2, 1 }, // l2i
		{
			2, 1 }, // l2f
		{
			2, 2 }, // l2d
		{
			1, 1 }, // f2i
		{
			1, 2 }, // f2l
		{
			1, 2 }, // f2d
		{
			2, 1 }, // d2i
		{
			2, 2 }, // d2l
		{
			2, 1 }, // d2f
		{
			1, 1 }, // i2b
		{
			1, 1 }, // i2c
		{
			1, 1 }, // i2s
		{
			4, 1 }, // lcmp
		{
			2, 1 }, // fcmpl
		{
			2, 1 }, // fcmpg
		{
			4, 1 }, // dcmpl
		{
			4, 1 }, // dcmpg
		{
			1, 0 }, // ifeq
		{
			1, 0 }, // ifne
		{
			1, 0 }, // iflt
		{
			1, 0 }, // ifge
		{
			1, 0 }, // ifgt
		{
			1, 0 }, // ifle
		{
			2, 0 }, // if_icmpeq
		{
			2, 0 }, // if_icmpne
		{
			2, 0 }, // if_icmplt
		{
			2, 0 }, // if_icmpge
		{
			2, 0 }, // if_icmpgt
		{
			2, 0 }, // if_icmple
		{
			2, 0 }, // if_acmpeq
		{
			2, 0 }, // if_acmpne
		{
			0, 0 }, // goto
		{
			0, 1 }, // jsr
		{
			0, 0 }, // ret
		{
			1, 0 }, // tableswitch
		{
			1, 0 }, // lookupswitch
		{
			1, 0 }, // ireturn
		{
			2, 0 }, // lreturn
		{
			1, 0 }, // freturn
		{
			2, 0 }, // dreturn
		{
			1, 0 }, // areturn
		{
			0, 0 }, // return
		{
			0, 1 }, // getstatic
		{
			1, 0 }, // putstatic
		{
			1, 1 }, // getfield
		{
			2, 0 }, // putfield
		{
			1, 1 }, // invokevirtual
		{
			1, 1 }, // invokespecial
		{
			1, 1 }, // invokestatic
		{
			1, 1 }, // invokeinterface
		{
			1, 1 }, // xxxunusedxxx
		{
			0, 1 }, // new
		{
			1, 1 }, // newarray
		{
			1, 1 }, // anewarray
		{
			1, 1 }, // arraylength
		{
			1, 0 }, // athrow
		{
			1, 1 }, // checkcast
		{
			1, 1 }, // instanceof
		{
			1, 0 }, // monitorenter
		{
			1, 0 }, // monitorexit
		{
			0, 0 }, // wide
		{
			2, 1 }, // multianewarray
		{
			1, 0 }, // ifnull
		{
			1, 0 }, // ifnonnull
		{
			0, 0 }, // goto_w
		{
			0, 1 }, // jsr_w
	};

	/**
	 Instruction runtime exception flags.
	**/
	public static final short NULLCHECK = 0x1;
	/**
	 Instruction runtime exception flags.
	**/
	public static final short ARRAYCHECK = 0x2;
	/**
	 Instruction runtime exception flags.
	**/
	public static final short DIVZEROCHECK = 0x4;
	/**
	 Instruction runtime exception flags.
	**/
	public static final short OTHERCHECK = 0x80;

	public static final short opcodeRuntimeExceptions[] = { 0, /* nop */
		0, /* aconst_null */
		0, /* iconst_m1 */
		0, /* iconst_0 */
		0, /* iconst_1 */
		0, /* iconst_2 */
		0, /* iconst_3 */
		0, /* iconst_4 */
		0, /* iconst_5 */
		0, /* lconst_0 */
		0, /* lconst_1 10 */
		0, /* fconst_0 */
		0, /* fconst_1 */
		0, /* fconst_2 */
		0, /* dconst_0 */
		0, /* dconst_1 */
		0, /* bipush */
		0, /* sipush */
		0, /* ldc */
		0, /* ldc_w */
		0, /* ldc2_w   20 */
		0, /* iload */
		0, /* lload */
		0, /* fload */
		0, /* dload */
		0, /* aload */
		0, /* iload_0 */
		0, /* iload_1 */
		0, /* iload_2 */
		0, /* iload_3 */
		0, /* lload_0   30 */
		0, /* lload_1 */
		0, /* lload_2 */
		0, /* lload_3 */
		0, /* fload_0 */
		0, /* fload_1 */
		0, /* fload_2 */
		0, /* fload_3 */
		0, /* dload_0 */
		0, /* dload_1 */
		0, /* dload_2   40 */
		0, /* dload_3 */
		0, /* aload_0   42 */
		0, /* aload_1 */
		0, /* aload_2 */
		0, /* aload_3 */
		0 | ARRAYCHECK | NULLCHECK, /* iaload */
		0 | ARRAYCHECK | NULLCHECK, /* laload */
		0 | ARRAYCHECK | NULLCHECK, /* faload */
		0 | ARRAYCHECK | NULLCHECK, /* daload */
		0 | ARRAYCHECK | NULLCHECK, /* aaload */
		0 | ARRAYCHECK | NULLCHECK, /* baload */
		0 | ARRAYCHECK | NULLCHECK, /* caload */
		0 | ARRAYCHECK | NULLCHECK, /* saload */
		0, /* istore */
		0, /* lstore */
		0, /* fstore */
		0, /* dstore */
		0, /* astore */
		0, /* istore_0 */
		0, /* istore_1 */
		0, /* istore_2 */
		0, /* istore_3 */
		0, /* lstore_0 */
		0, /* lstore_1 */
		0, /* lstore_2 */
		0, /* lstore_3 */
		0, /* fstore_0 */
		0, /* fstore_1 */
		0, /* fstore_2 */
		0, /* fstore_3 */
		0, /* dstore_0 */
		0, /* dstore_1 */
		0, /* dstore_2 */
		0, /* dstore_3 */
		0, /* astore_0 */
		0, /* astore_1 */
		0, /* astore_2 */
		0, /* astore_3 */
		0 | ARRAYCHECK | NULLCHECK, /* iastore */
		0 | ARRAYCHECK | NULLCHECK, /* lastore */
		0 | ARRAYCHECK | NULLCHECK, /* fastore */
		0 | ARRAYCHECK | NULLCHECK, /* dastore */
		0 | ARRAYCHECK | NULLCHECK, /* aastore */
		0 | ARRAYCHECK | NULLCHECK, /* bastore */
		0 | ARRAYCHECK | NULLCHECK, /* castore */
		0 | ARRAYCHECK | NULLCHECK, /* sastore */
		0, /* pop */
		0, /* pop2 */
		0, /* dup */
		0, /* dup_x1 */
		0, /* dup_x2 */
		0, /* dup2 */
		0, /* dup2_x1 */
		0, /* dup2_x2 */
		0, /* swap */
		0, /* iadd */
		0, /* ladd */
		0, /* fadd */
		0, /* dadd */
		0, /* isub */
		0, /* lsub */
		0, /* fsub */
		0, /* dsub */
		0, /* imul */
		0, /* lmul */
		0, /* fmul */
		0, /* dmul */
		0 | DIVZEROCHECK, /* idiv */
		0 | DIVZEROCHECK, /* ldiv */
		0, /* fdiv */
		0, /* ddiv */
		0 | DIVZEROCHECK, /* irem */
		0 | DIVZEROCHECK, /* lrem */
		0, /* frem */
		0, /* drem */
		0, /* ineg */
		0, /* lneg */
		0, /* fneg */
		0, /* dneg */
		0, /* ishl */
		0, /* lshl */
		0, /* ishr */
		0, /* lshr */
		0, /* iushr */
		0, /* lushr */
		0, /* iand */
		0, /* land */
		0, /* ior */
		0, /* lor */
		0, /* ixor */
		0, /* lxor */
		9990, /* iinc */
		0, /* i2l */
		0, /* i2f */
		0, /* i2d */
		0, /* l2i */
		0, /* l2f */
		0, /* l2d */
		0, /* f2i */
		0, /* f2l */
		0, /* f2d */
		0, /* d2i */
		0, /* d2l */
		0, /* d2f */
		0, /* int2byte */
		0, /* int2char */
		0, /* int2short */
		0, /* lcmp */
		0, /* fcmpl */
		0, /* fcmpg */
		0, /* dcmpl */
		0, /* dcmpg */
		0, /* ifeq */
		0, /* ifne */
		0, /* iflt */
		0, /* ifge */
		0, /* ifgt */
		0, /* ifle */
		0, /* if_icmpeq */
		0, /* if_icmpne */
		0, /* if_icmplt */
		0, /* if_icmpge */
		0, /* if_icmpgt */
		0, /* if_icmple */
		0, /* if_acmpeq */
		0, /* if_acmpne */
		0, /* goto */
		0, /* jsr */
		0, /* ret */
		90, /* tableswitch */
		90, /* lookupswitch */
		0 | OTHERCHECK, /* ireturn      -84  */
		0 | OTHERCHECK, /* lreturn      -83  */
		0 | OTHERCHECK, /* freturn      -82  */
		0 | OTHERCHECK, /* dreturn      -81  */
		0 | OTHERCHECK, /* areturn      -80  */
		0 | OTHERCHECK, /* return       -79  */
		0, /* getstatic */
		0, /* putstatic */
		0 | NULLCHECK, /* getfield */
		0 | NULLCHECK, /* putfield */
		0 | NULLCHECK, /* invokevirtual        -74 */
		0 | NULLCHECK, /* invokespecial     -73 */
		0, /* invokestatic         -72 */
		0 | NULLCHECK, /* invokeinterface      -71 */
		0, /* xxxunusedxxx */
		0 | OTHERCHECK, /* new */
		0 | ARRAYCHECK, /* newarray */
		0 | ARRAYCHECK, /* anewarray */
		0 | ARRAYCHECK | NULLCHECK, /* arraylength */
		0 | NULLCHECK, /* athrow */
		0 | OTHERCHECK, /* checkcast */
		0, /* instanceof */
		0 | NULLCHECK, /* monitorenter */
		0 | NULLCHECK, /* monitorexit */
		0, /* wide */
		0 | ARRAYCHECK, /* multianewarray */
		0, /* ifnull */
		0, /* ifnonnull */
		0, /* goto_w */
		0, /* jsr_w */
	};

	
}
