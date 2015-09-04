package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

/**
 Represents an constant-related instruction -- see its subclasses.
 <ul>
	 <li> {@link BT_ConstantIns}
	 <ul>
		 <li> {@link BT_ConstantFloatIns}
		 <li> {@link BT_ConstantIntegerIns}
		 <li> {@link BT_ConstantStringIns}
		 <li> {@link BT_ConstantClassIns}
		 <li> {@link BT_ConstantWideIns}
		 <ul>
			 <li> {@link BT_ConstantDoubleIns}
			 <li> {@link BT_ConstantLongIns}
		</ul>
	 </ul>
 </ul>
 * @author IBM
**/
public abstract class BT_ConstantIns extends BT_Ins {

	BT_ConstantIns(int opcode, int index) {
		super(opcode, index);
	}

	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		//
		// remove a constant load followed by a pop
		//
		if (ins.size() > n + 1 && ins.elementAt(n + 1).opcode == opc_pop) {
			return code.removeInstructionsAt(2, n);
		}

		// Tbd: optimize pushing two constants onto the stack
		
		//TODO multiple ldc's accessing the same constant can be optimized by inserting a local
		//so what we could do is initialize the local at the beginning of the method, then make all ldc's
		//access the local, or insert a getlocal, null check, ldc, assign local series:
		//const x = local;
		//if(x != null) {
		//use constant
		//} else {
		//x = ldc
		//use constant
		//}
		return false;
	}

	protected abstract int constantIndex(BT_ConstantPool pool);
  
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		if (opcode == opc_ldc_w) {
			dos.writeShort(constantIndex(pool));
		}
		else {
			dos.writeByte(constantIndex(pool));
		}
	}
	
	public boolean isConstantIns() {
		return true;
	}
}
