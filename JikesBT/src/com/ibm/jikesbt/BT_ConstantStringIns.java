package com.ibm.jikesbt;


/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents an opc_ldc or opc_ldc_w instruction.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_ConstantStringIns extends BT_ConstantIns {
	private Object value;
	final BT_Class stringClass;
	
	BT_ConstantStringIns(int opcode, Object value, BT_Class stringClass) {
		this(opcode, -1, value, stringClass);
	}
	
	BT_ConstantStringIns(int opcode, int index, Object value, BT_Class stringClass) {
		super(opcode, index);
		this.value = value;
		this.stringClass = stringClass;
	}

	public String getInstructionTarget() {
		return getJavaLangString().useName();
	}
	
	protected int constantIndex(BT_ConstantPool pool) {
		return pool.indexOfString(value.toString());
	}

	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		opcode = BT_Misc.overflowsUnsignedByte(constantIndex(pool)) ? opc_ldc_w : opc_ldc;
	}
	
	/**
	 * @return whether the value of this constant string might differ at class write time 
	 * from its current value
	 */
	public boolean isImmutable() {
		return value instanceof String;
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		boolean result = false;

		// optimize ldc "String"; ldc "String" to 
		// ldc "String"; dup
		//
		if (ins.size() > n + 1
			&& ins.elementAt(n).isLoadConstantStringIns()
			&& ins.elementAt(n + 1).isLoadConstantStringIns()) {
			String s1 = ((BT_ConstantStringIns) ins.elementAt(n)).getValue();
			String s2 = ((BT_ConstantStringIns) ins.elementAt(n + 1)).getValue();

			if (s1.equals(s2)) {
				return code.replaceInstructionsAtWith(1, n + 1, make(opc_dup));
			} else {
				// optimize ldc "One"; ldc "Two"; isInvokeVirtual("java.lang.String","equals")
				// 	with iconst_0
				// and 
				// optimize ldc "STRING"; ldc "string"; isInvokeVirtual("java.lang.String","equalsIgnoreCase")
				// 	with iconst_1
				if (ins.size() > n + 2) {
					if (ins
						.elementAt(n + 2)
						.isInvokeVirtual(BT_Repository.JAVA_LANG_STRING, "equals")) {
						return code.replaceInstructionsAtWith(
							3,
							n,
							make(opc_iconst_0));
					} else if (
						ins.elementAt(n + 2).isInvokeVirtual(
							BT_Repository.JAVA_LANG_STRING,
							"equalsIgnoreCase")) {
						if (s1.equalsIgnoreCase(s2)) {
							return code.replaceInstructionsAtWith(
								3,
								n,
								make(opc_iconst_1));
						} else {
							return code.replaceInstructionsAtWith(
								3,
								n,
								make(opc_iconst_0));
						}
					}
				}
			}
		}

		// optimize ldc "String"; dup; InvokeVirtual("java.lang.String","equals"|"equalsignoreCase")
		// to iconst 1
		if (ins.size() > n + 2
			&& ins.elementAt(n).isLoadConstantStringIns()
			&& ins.elementAt(n + 1).opcode == opc_dup
			&& (ins.elementAt(n + 2).isInvokeVirtual(BT_Repository.JAVA_LANG_STRING, "equals")
				|| ins.elementAt(n + 2).isInvokeVirtual(
					BT_Repository.JAVA_LANG_STRING,
					"equalsIgnoreCase"))) {

			return code.replaceInstructionsAtWith(3, n, make(opc_iconst_1));

		}

		//TODO the same optimizations with StringBuilder
		//
		// Constant folding for Strings.
		// replace  "string " + n    with   "string n"
		//
		while (isImmutable() && (ins.size() > n + 3)
			&& (ins
				.elementAt(n + 1)
				.isInvokeVirtual(BT_Repository.JAVA_LANG_STRING_BUFFER, "append")
				|| ins.elementAt(n + 1).isInvokeSpecial(
					BT_Repository.JAVA_LANG_STRING_BUFFER,
					BT_Method.INITIALIZER_NAME))
			&& ins.elementAt(n + 2).isPushConstantIns()
			&& (
					/* the other appends are not necessary,
					 * and in particular the one that appends
					 * characters must be handled differently 
					 * (TODO handle the char one)
					 * test for sig (C)Ljava/lang/StringBuffer;
					 * and instead of appending ins.elementAt(n + 2)
					 * using toString, which converts ints to decimal format,
					 * should use some other method which converts ints
					 * to chars (ie cast as char and then append)
					 */
				ins.elementAt(n + 3).isInvokeVirtual(
						BT_Repository.JAVA_LANG_STRING_BUFFER,
						"append",
						"(I)Ljava/lang/StringBuffer;")
				||
				ins.elementAt(n + 3).isInvokeVirtual(
						BT_Repository.JAVA_LANG_STRING_BUFFER,
						"append",
						"(J)Ljava/lang/StringBuffer;")
				||
				ins.elementAt(n + 3).isInvokeVirtual(
						BT_Repository.JAVA_LANG_STRING_BUFFER,
						"append",
						"(F)Ljava/lang/StringBuffer;")
				||
				ins.elementAt(n + 3).isInvokeVirtual(
						BT_Repository.JAVA_LANG_STRING_BUFFER,
						"append",
						"(D)Ljava/lang/StringBuffer;")
			)) {
			mergeWithConstant(ins.elementAt(n + 2));
			code.removeInstructionsAt(2, n + 2);
			result = true;
		}
		if (!result)
			result = super.optimize(code, n, strict);
		return result;
	}

	private void mergeWithConstant(BT_Ins other) {
		value = other.appendValueTo(value.toString());
	}

	String appendValueTo(String other) {
		return other + value;
	}

	public boolean isLoadConstantStringIns() {
		return true;
	}
	public int size() {
		return (opcode == opc_ldc_w) ? 3 : 2;
	}
	
	public int maxSize() {
		return 3;
	}
	
	public String toString() {
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " (" + BT_Repository.JAVA_LANG_STRING + ") \""
			+ parse(getValue())
			+ "\"";
	}
	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode]
			+ " (" + BT_Repository.JAVA_LANG_STRING + ") \""
			+ parse(getValue())
			+ "\"";
	}
	
	public String getValue() {
		return value.toString();
	}
	
	public void resetValue(Object value) {
		this.value = value;
	}
	
	//TODO instead or resetValue we could use resetInstructionTarget but would need to implement on each class
	//that overrides getInstructionTarget
	
	private static String parse(String s) {
		StringBuffer res = new StringBuffer(s.length());
		for(int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			switch(c) {
				case '\r':
					res.append("\\r");
					break;
				case '\n':
					res.append("\\n");
					break;
				case '\t':
					res.append("\\t");
					break;
				case '\b':
					res.append("\\b");
					break;
				case '\f':
					res.append("\\f");
					break;
				case '\\':
					res.append("\\\\");
					break;
				case '\'':
					res.append("\\\'");
					break;
				case '\"':
					res.append("\\\"");
					break;
				default:
					res.append(c);
			}
		}
		return res.toString();
	}
	
	public Object clone() {
		return new BT_ConstantStringIns(opcode, value, stringClass);
	}
	
	public BT_Class getJavaLangString() {
		return stringClass;
	}
	
	public void link(BT_CodeAttribute code) {
		//The ldc/ldc_w instruction is treated differently than the new/anewarray/multanewarray instructions
		//The interesting thing about this case is that the instruction represents the
		//creation of string objects (although in the case of a VM with a String pool the
		//site may resuse a previously created String instead in some circumstances), however
		//there is no explicit reference to the String class, there is no reference to java/lang/String
		//in the constant pool, the reference is implicit, while the class is known to be already loaded. 
		//In reality the class is referenced not by the instruction but by the constant pool when the class is loaded
		BT_ClassReferenceSite site = getJavaLangString().addReferenceSite(this, code);
		if(site != null) {
			code.addReferencedClass(site);
		}
	}
	
	public void unlink(BT_CodeAttribute code) {
		getJavaLangString().removeClassReferenceSite(this);
		code.removeReferencedClass(this);
	}
}
