package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Represents a field-manipulating instruction -- one of
 opc_getstatic,
 opc_putstatic,
 opc_getfield,
 or
 opc_putfield.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public class BT_FieldRefIns extends BT_Ins {

	/**
	 See {@link BT_FieldRefIns#getTarget}, and {@link #resetTarget}.
	**/
	public BT_Field target;
	public BT_Class targetClass;
	
	public BT_FieldRefIns(int opcode, BT_Field target, BT_Class throughClass) {
		super(opcode, -1);
		this.target = target;
		this.targetClass = throughClass;
	}
	
	public BT_FieldRefIns(int opcode, BT_Field target) {
		this(opcode, target, target.cls);
	}

	/**
	 Constructs a field-referencing instruction.
	 For use when reading the instruction from byte-codes.
	**/
	protected BT_FieldRefIns(
		int opcode,
		int index,
		int poolIndex,
		BT_Method inM,
		LoadLocation loadedFrom)
			throws BT_DescriptorException, BT_ConstantPoolException {
		super(opcode, index);
		BT_ConstantPool pool = inM.cls.pool;
		targetClass =
			pool.getRepository().forName(
				pool.getClassNameAt(poolIndex, BT_ConstantPool.FIELDREF));
		BT_Class targetType = pool.getRepository().linkTo(pool.getFieldTypeAt(poolIndex));
		String fieldName = pool.getFieldNameAt(poolIndex);
		target = targetClass.findInheritedField(
				fieldName,
				targetType,
				true);
		//TODO could check for stubs, and if none exist we would throw an exception here
		if (target == null) {
			createStubTarget(targetClass, inM, fieldName, targetType, loadedFrom);
			if(isStaticFieldAccessIns()) {
				target.becomeStatic();
			}
		} 
	}

	private void createStubTarget(BT_Class targetClass, BT_Method from, String fieldName, BT_Class targetType, LoadLocation loadedFrom) {
		target = targetClass.addStubField(fieldName, targetType);
		targetClass.getRepository().factory.noteUndeclaredField(target, targetClass, from, this, loadedFrom);
	}

	public BT_Accessor findAccessor() {
		return target.findAccessor(this);
	}
	
	public void link(BT_CodeAttribute code) {
		BT_Accessor accessor = target.addAccessor(this, code);
		if(accessor != null) {
			code.addAccessedField(accessor);
		}
	}

	public void unlink(BT_CodeAttribute code) {
		target.removeAccessor(this);
		code.removeAccessedField(this);
	}
	
	private int getConstantPoolIndex(BT_CodeAttribute code, BT_ConstantPool pool) {
		return pool.indexOfFieldRef(getResolvedClassTarget(code), target);
	}
	
	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		getConstantPoolIndex(code, pool);
	}

	// ----------------------------------
	// Target-related ...

	public BT_Field getFieldTarget() {
		return target;
	}
	public BT_Class getClassTarget() {
		return targetClass;
	}
	
	/**
	 Returns null or the class this instruction references once resolved.
	**/
	public BT_Class getResolvedClassTarget(BT_CodeAttribute code) {
		if(BT_Factory.resolveRuntimeReferences) {
			if(//"target" is only accessible through "targetClass" which has greater access permissions 
					//than "target"'s declaring class
					
					//TODO There may be other cases in which the declaring class cannot be used.
					//see BT_StackShapeVisitor.buildMergeCandidates : some classes might not be visible due to class loader design
					
				!target.getDeclaringClass().isVisibleFrom(code.getMethod().getDeclaringClass())
					/*|| target.isStub() the result of this method must not change, and since something can switch to/from a stub, we cannot alter resolution based on this quality
					|| target.getDeclaringClass().isStub()*/
			) {
				return targetClass;
			}
			return target.getDeclaringClass();
		}
		return targetClass;
	}
	
	/**
	 Just gets field {@link BT_FieldRefIns#target}.
	 Same as {@link BT_FieldRefIns#getFieldTarget} but using a different naming convention.
	**/
	public BT_Field getTarget() {
		return target;
	}

	public void resetTarget(BT_Field m, BT_CodeAttribute owner) {
		if (m != target) {
			if(target != null) {
				unlink(owner);
			}
			target = m;
			targetClass = m.cls;
			link(owner);
		}
	}
	// ----------------------------------

	public boolean isPushConstantIns() {
		return false;
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		dos.writeShort(getConstantPoolIndex(code, pool));
		if (size() != 3)
			throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.Write/size_error_{0}_3", this));
	}
	
	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		BT_InsVector ins = code.getInstructions();
		BT_Ins next = (ins.size() > n + 1) ? ins.elementAt(n + 1) : null;

		int popOpcode = target.getOpcodeForPop();

		//
		// replace "getfield, pop" by "ifnonnull X;aconstnull;athrow;X:" if the
		// method has no control flow before this instruction and
		// there are other references to the same class before
		// or after this point in this basic block.
		//
		if (next != null
			&& opcode == opc_getfield
			&& next.opcode == popOpcode) {
			next = ins.elementAt(n + 2);
			BT_BasicBlockMarkerIns nextBlock;
			if(next.isBlockMarker()) {
				nextBlock = (BT_BasicBlockMarkerIns) next;
			} else {
				nextBlock = BT_Ins.make();
				code.insertInstructionAt(nextBlock, n + 2);
			}
			code.insertInstructionAt(BT_Ins.make(BT_Ins.opc_athrow), n + 2);
			code.insertInstructionAt(
				BT_Ins.make(BT_Ins.opc_aconst_null),
				n + 2);
			return code.replaceInstructionsAtWith(
				2,
				n,
				new BT_JumpOffsetIns(BT_Ins.opc_ifnonnull, -1, nextBlock));
		}

		//
		// remove  "getstatic, pop" if the 
		// method has no control flow before this instruction and
		// there are other references to the same class before
		// or after this point in this basic block.
		//
		if (next != null
			&& opcode == opc_getstatic
			&& next.opcode == popOpcode
			&& hasEquivalentClassReferences(code, n)) {
			return code.removeInstructionsAt(2, n);
		}

		//
		// replace "getstatic, getstatic" with "getstatic, dup"
		//
		if (next != null
			&& opcode == opc_getstatic
			&& next.opcode == opc_getstatic
			&& target == ((BT_FieldRefIns) next).target
			&& targetClass == ((BT_FieldRefIns) next).targetClass) {
			return code.replaceInstructionsAtWith(
				1,
				n + 1,
				BT_Ins.make(target.getOpcodeForDup()));
		}

		//
		// replace two getfields with one getfield and a dup
		//
		if (n > 0
			&& ins.size() > n + 2
			&& opcode == opc_getfield
			&& ins.elementAt(n - 1).opcode == opc_aload_0
			&& ins.elementAt(n + 1).opcode == opc_aload_0
			&& ins.elementAt(n + 2).opcode == opc_getfield) {
			BT_FieldRefIns fri = (BT_FieldRefIns) ins.elementAt(n + 2);
			if (target == fri.target && targetClass == fri.targetClass) {
				return code.replaceInstructionsAtWith(
					2,
					n + 1,
					BT_Ins.make(target.getOpcodeForDup()));
			}
		}

		return false;
	}

	private boolean hasEquivalentClassReferences(
		BT_CodeAttribute code,
		int n) {
		BT_InsVector ins = code.getInstructions();
		BT_Class cls = target.cls;
		for (int i = n - 1; i >= 0; i--) {
			BT_Ins instr = ins.elementAt(i);
			if (instr.isBlockMarker()) {
				break;
			}
			BT_Class target = instr.getClassTarget();
			if (target != null && target == cls) {
				return true;
			}
		}
		for (int i = n + 1; i < ins.size(); i++) {
			BT_Ins instr = ins.elementAt(i);
			if (instr.isBlockMarker()) {
				return false;
			}
			BT_Class target = instr.getClassTarget();
			if (target != null && target == cls) {
				return true;
			} else if (BT_Misc.opcodeRuntimeExceptions[instr.opcode] != 0) {
				return false;
			}
		}
		return false;
	}

	public int getPoppedStackDiff() {
		String fieldType = target.getTypeName();
		
		switch (opcode) {
			case opc_getfield :
				return -1;
			case opc_getstatic :
				return 0;
			case opc_putfield :
				boolean wide = fieldType.equals("long") || fieldType.equals("double");
				return wide ? -3 : -2;
			case opc_putstatic :
				wide = fieldType.equals("long") || fieldType.equals("double");
				return wide ? -2 : -1;
		}
		throw new IllegalStateException(Messages.getString("JikesBT.Unsupported_opcode_{0}_14", opcode));
	}
	
	public int getPushedStackDiff() {
		String fieldType = target.getTypeName();
		
		switch (opcode) {
			case opc_getfield :
			case opc_getstatic :
				boolean wide = fieldType.equals("long") || fieldType.equals("double");
				return wide ? 2 : 1;
			case opc_putfield :
			case opc_putstatic :
				return 0;
		}
		throw new IllegalStateException(Messages.getString("JikesBT.Unsupported_opcode_{0}_14", opcode));
	}

	public Object clone() {
		return new BT_FieldRefIns(opcode, target, targetClass);
	}
	//   Also displays the target's type.
	public String toString() {
		String suf = true ? (Messages.getString("JikesBT.__type_{0}_15", target.getFieldType())) : "";

		return getPrefix()
			+ BT_Misc.opcodeName[opcode]
			+ " "
			+ getInstructionTarget()
			//+ target.useName()
			+ suf;
	}
	
	public String getInstructionTarget() {
		return targetClass.getName() + '.' + target.qualifiedName();
	}

	public String toAssemblerString(BT_CodeAttribute code) {
		String fieldName = target.useName();
		return BT_Misc.opcodeName[opcode] + " " + fieldName + " " + target.getFieldType();
	}
}
