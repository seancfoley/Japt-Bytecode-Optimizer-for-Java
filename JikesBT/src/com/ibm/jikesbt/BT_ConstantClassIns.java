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
public final class BT_ConstantClassIns extends BT_ConstantIns {
	public BT_Class value;
	
	BT_ConstantClassIns(int opcode, int index, BT_Class value) {
		super(opcode, index);
		this.value = value;
	}
	
	BT_ConstantClassIns(int opcode, int index, String className, BT_Repository repo) {
		super(opcode, index);
		this.value = repo.forName(className);;
	}

	protected int constantIndex(BT_ConstantPool pool) {
		return pool.indexOfClassRef(value);
	}
	
	public String getInstructionTarget() {
		return getJavaLangClass().useName();
	}

	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		opcode = BT_Misc.overflowsUnsignedByte(constantIndex(pool)) ? opc_ldc_w : opc_ldc;
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		boolean result = false;

		// optimize ldc "Class"; ldc "Class" to 
		// ldc "String"; dup
		//
		if (ins.size() > n + 1
			&& ins.elementAt(n).isLoadConstantClassIns()
			&& ins.elementAt(n + 1).isLoadConstantClassIns()) {
			BT_Class s1 = ((BT_ConstantClassIns) ins.elementAt(n)).getValue();
			BT_Class s2 = ((BT_ConstantClassIns) ins.elementAt(n + 1)).getValue();

			if (s1.equals(s2)) {
				return code.replaceInstructionsAtWith(1, n + 1, make(opc_dup));
			} 
		}
		if (!result) {
			result = super.optimize(code, n, strict);
		}
		return result;
	}

	String appendValueTo(String other) {
		return other + value.getName();
	}
	
	public boolean isPushConstantIns() {
		return false;
	}

	public boolean isLoadConstantClassIns() {
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
			+ " (java.lang.Class) \""
			+ value.getName()
			+ "\"";
	}
	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode]
			+ " (java.lang.Class) \""
			+ value.getName()
			+ "\"";
	}
	
	public BT_Class getValue() {
		return value;
	}
	
	public Object clone() {
		return new BT_ConstantClassIns(opcode, -1, value);
	}
	
	public BT_Class getJavaLangClass() {
		return value.repository.findJavaLangClass();
	}
	
	public void link(BT_CodeAttribute code) {
		BT_ClassReferenceSite site = getJavaLangClass().addReferenceSite(this, code);
		if(site != null) {
			code.addReferencedClass(site);
		}
	}
	
	public void unlink(BT_CodeAttribute code) {
		getJavaLangClass().removeClassReferenceSite(this);
		code.removeReferencedClass(this);
	}
}
