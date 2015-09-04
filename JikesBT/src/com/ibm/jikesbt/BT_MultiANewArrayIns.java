package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InvalidInstructionException;

/**
 Represents an opc_multianewarray instruction that allocates an array of
 arrays of ... of null references.
 {@link BT_ANewArrayIns} is similar.
 Also see {@link BT_NewIns} and {@link BT_NewArrayIns}.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public final class BT_MultiANewArrayIns extends BT_NewIns {
	
	public final short dimensions;
	
	BT_MultiANewArrayIns(int opcode, int index, String name, short dimensions, BT_Repository repo, BT_CodeAttribute code)
			throws BT_InvalidInstructionException {
		super(opcode, index, name, repo, code);
		this.dimensions = dimensions;
		if (target.isPrimitive()) {
			throw new BT_InvalidInstructionException(code, opcode, index);
		}
		if (dimensions < 1 || target.getArrayDimensions() < dimensions) {
			throw new BT_InvalidInstructionException(code, opcode, index, Messages.getString("JikesBT.invalid_multianewarray_instruction_1"));
		}
	}
	
	public BT_MultiANewArrayIns(
		int opcode,
		BT_Class target,
		short dimensions) {
		super(opcode, target);
		this.dimensions = dimensions;
	}

	public Object clone() {
		return new BT_MultiANewArrayIns(opcode, target, dimensions);
	}
	
	public int getPushedStackDiff() {
		return 1;
	}

	public int getPoppedStackDiff() {
		return -dimensions;
	}
	
	public BT_Class getElementClass(BT_Class arrayClass) {
		String declaringName = arrayClass.fullName();
		String elementClassName = declaringName.substring(0, declaringName.length() - 2);
		return arrayClass.repository.forName(elementClassName);
	}

	public void link(BT_CodeAttribute code) {
		super.link(code);
		BT_Class targetClass = this.target;
		short dimensions = this.dimensions;
		//we are also creating objects for each smaller dimension
		while(dimensions > 1) {
			targetClass = getElementClass(targetClass);
			BT_CreationSite site = targetClass.addCreationSite(this, code);
			if(site != null) {
				code.addCreatedClass(site);
				super.linkClassReference(site, targetClass);
				
			}
			dimensions--;
		}
	}
	
	public void unlink(BT_CodeAttribute code) {
		super.unlink(code);
		BT_Class targetClass = this.target;
		short dimensions = this.dimensions;
		//we also created objects for each smaller dimension
		while(dimensions > 1) {
			targetClass = getElementClass(targetClass);
			targetClass.removeClassReferenceSite(this);
			targetClass.removeCreationSite(this);
			
			//no need for the following 2 calls.  This is because calling
			//the following methods just once for this instruction 
			//removes the multiple sites for all dimensions
			//because the lookup matches the instruction and not the created class
			//code.removeReferencedClass(this);
			//code.removeCreatedClass(this);
			dimensions--;
		}
	}
	
	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		dos.writeShort(pool.indexOfClassRef(target));
		dos.writeByte(dimensions);
		if (size() != 4)
			throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.Write/size_error_{0}_3", this));
	}
	
	public String toString() {
		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " "
			+ target.useName()
			+ " "
			+ dimensions;
	}
	
	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode]
		  + " "
		  + target.useName()
		  + " "
		  + dimensions;
	}
	
	public boolean isNewArrayIns() {
		return true;
	}
	
	public boolean isMultiNewArrayIns() {
		return true;
	}
}
