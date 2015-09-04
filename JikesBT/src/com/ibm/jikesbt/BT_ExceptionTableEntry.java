package com.ibm.jikesbt;


/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;

/**
 Represents an exception table entry that describes a try/catch/finally
 block.

 <p> Another exception-related representation is {@link BT_ExceptionsAttribute}.

 * @author IBM
**/
public final class BT_ExceptionTableEntry extends BT_Base implements Cloneable {

	public static final String ANY = "any";
	public static final String EXCEPTION = "exception";
	public static final String FROM = "from";
	public static final String TO = "to";
	public static final String CATCH = "catch";
	public static final String HANDLER = "handler";
	
	public static class Indices {
		/**
		 See {@link BT_ExceptionTableEntry#BT_ExceptionTableEntry(int startPC, int endPC, int handlerPC, String type) BT_ExceptionTableEntry}.
		 If a class file is not read, JikesBT does not set this since using {@link BT_ExceptionTableEntry#startPCTarget} is preferred.
		**/
		public int start;
	
		/**
		 See {@link BT_ExceptionTableEntry#BT_ExceptionTableEntry(int startPC, int endPC, int handlerPC, String type) BT_ExceptionTableEntry}.
		 If a class file is not read, JikesBT does not set this since using {@link BT_ExceptionTableEntry#endPCTarget} is preferred.
		**/
		public int end;
	
		/**
		 See {@link BT_ExceptionTableEntry#BT_ExceptionTableEntry(int startPC, int endPC, int handlerPC, String type) BT_ExceptionTableEntry}.
		 If a class file is not read, JikesBT does not set this since using {@link BT_ExceptionTableEntry#handlerTarget} is preferred.
		**/
		public int handler;
		
		Indices(int start, int end, int handler) {
			this.start = start;
			this.end = end;
			this.handler = handler;
		}
	}
	
	Indices indices = null;

	/**
	 See {@link BT_ExceptionTableEntry#BT_ExceptionTableEntry(BT_Ins startPCTarget, BT_Ins endPCTarget, BT_Ins handlerTarget, BT_Class catchType)}.
	**/
	public BT_BasicBlockMarkerIns startPCTarget;

	/**
	 See {@link BT_ExceptionTableEntry#BT_ExceptionTableEntry(BT_Ins startPCTarget, BT_Ins endPCTarget, BT_Ins handlerTarget, BT_Class catchType)}.
	**/
	public BT_BasicBlockMarkerIns endPCTarget;

	/**
	 See {@link BT_ExceptionTableEntry#BT_ExceptionTableEntry(BT_Ins startPCTarget, BT_Ins endPCTarget, BT_Ins handlerTarget, BT_Class catchType)}.
	**/
	public BT_BasicBlockMarkerIns handlerTarget;

	/**
	 The caught exception type.
	 Null represents "<finally>".
	**/
	public BT_Class catchType;
	
	/**
	 * the code attribute which holds the above instructions
	 */
	public BT_Code code;

	static final BT_ExceptionTableEntry emptyHandlers[] = new BT_ExceptionTableEntry[0];
	
	/**
	 Constructs using bytecode offsets, typically for use while reading a
	 class file.
	
	 @param startPC  The values of the two items startPC and endPC indicate
	   the ranges in the code array at which the exception handler is active.
	   The value of startPC must be a valid index into the code array of the
	   opcode of an instruction. The value of endPC either must be a valid
	   index into the code array of the opcode of an instruction, or must be
	   equal to code_length, the length of the code array. The value of
	   startPC must be less than the value of endPC.
	
	   The startPC is inclusive and endPC is exclusive; that is,
	   the exception handler must be active while the program
	   counter is within the interval [startPC, endPC).2
	
	 @param endPC    See "startPC".
	
	 @param handlerPC  The value of the handlerPC item indicates the start of the
	   exception handler. The value of the item must be a valid
	   index into the code array, must be the index of the opcode
	   of an instruction, and must be less than the value of the
	   code_length item.
	   
	 @param code the code to which this table entry pertains
	
	 @param type     Either "<finally>" or the name of the class being caught
	   in Java language format (e.g., "package.Class[][]" or
	   "void" or "boolean").
	**/
	public BT_ExceptionTableEntry(
			int startPC,
			int endPC,
			int handlerPC,
			String type, 
			BT_Repository repo,
			BT_ObjectCode code) {
		indices = new Indices(startPC, endPC, handlerPC);
		
		if (type != null) {
			catchType = repo.linkTo(type);
		}
		this.code = code;
	}
	
	/**
	 Constructs using JikesBT objects, typically for use while generating or
	 modifying a class.
	
	 The parameters are higher-level equivalents of the parameters of
	 {@link BT_ExceptionTableEntry#BT_ExceptionTableEntry(int startPC, int endPC, int handlerPC, String type)}.
	
	 @param catchType  Either null (for "finally") or the class being caught.
	**/
	public BT_ExceptionTableEntry(
			BT_BasicBlockMarkerIns startPCTarget,
			BT_BasicBlockMarkerIns endPCTarget,
			BT_BasicBlockMarkerIns handlerTarget,
			BT_Class catchType,
			BT_ObjectCode code) {
		this.startPCTarget = startPCTarget;
		this.endPCTarget = endPCTarget;
		this.handlerTarget = handlerTarget;
		this.catchType = catchType;
		this.code = code;
	}
	
	public BT_ExceptionTableEntry(
			BT_BasicBlockMarkerIns startPCTarget,
			BT_BasicBlockMarkerIns endPCTarget,
			BT_BasicBlockMarkerIns handlerTarget,
			BT_Class catchType,
			BT_CodeAttribute code) {
		this(startPCTarget, endPCTarget, handlerTarget, catchType, (BT_ObjectCode) code.code);
	}
	
	/**
	 * returns a shallow copy of this exception table entry
	 */
	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {}
		return null;
	}
	

	/**
	 Links the exception to other objects with which it has a relationship.
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>
	**/
	void dereference(BT_CodeAttribute code) throws BT_InstructionReferenceException {
		if (startPCTarget != null) {
			return;
		}
		BT_InsVector inst = code.getInstructions();
		startPCTarget = inst.findBasicBlock(code, this, indices.start, false);
		endPCTarget = inst.findBasicBlock(code, this, indices.end, true);
		handlerTarget = inst.findBasicBlock(code, this, indices.handler, false);
		indices = null;
	}

	/**
	 Builds the constant pool, ... in preparation for writing the class-file.
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#resolve_method>resolve method</a>
	**/
	public void resolve(BT_InsVector ins, BT_ConstantPool pool) {
		if (catchType != null) {
			pool.indexOfClassRef(catchType);
		}
	}

	/**
	 Updates all references that are "contained in" this object and that
	 refer to an "old" instruction so they refer to a "new" one.
	**/
	public void changeReferencesFromTo(BT_BasicBlockMarkerIns oldIns, BT_BasicBlockMarkerIns newIns) {
		if (startPCTarget == oldIns) {
			startPCTarget = newIns;
		}
		if (endPCTarget == oldIns) {
			endPCTarget = newIns;
		}
		if (handlerTarget == oldIns) {
			handlerTarget = newIns;
		}
	}

	/**
	 Writes this exception table entry to a class file.
	**/
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(startPCTarget.byteIndex);
		dos.writeShort(endPCTarget.byteIndex);
		dos.writeShort(handlerTarget.byteIndex);
		if (catchType != null) {
			dos.writeShort(pool.indexOfClassRef(catchType));
		} else {
			dos.writeByte(0);
			dos.writeByte(0);
		}
	}

	/**
	 * The method relies on references to instructions 
	 * (startPCTarget and endPCTarget)
	 * @returns True if exception range is empty
	 */
	public boolean isEmpty() {
		return startPCTarget == endPCTarget;
	}

	public String toString() {
		if (startPCTarget == null || code == null) {
			Indices indices = this.indices;
			if(indices == null) {
				indices = new Indices(-1, -1, -1);
			}
			if (catchType != null) {
				return Messages.getString("JikesBT.[from_{0}_to_before_{1}_catch({2})_at_{3}]_2", 
						new Object[] {"bytecode index " + indices.start, "bytecode index " + indices.end, catchType, "bytecode index " + indices.handler});
			}
			return Messages.getString("JikesBT.exception_[from_{0}_to_before_{1}_finally_at_{2}]_3",
					new Object[] {"bytecode " + indices.start, "bytecode " + indices.end, "bytecode " + indices.handler});
		}
		Indices indices = calculateIndices();
		if (catchType != null)
			return Messages.getString("JikesBT.[from_{0}_to_before_{1}_catch({2})_at_{3}]_2", 
				new Object[] {Integer.toString(indices.start), Integer.toString(indices.end), catchType.name, Integer.toString(indices.handler)});  
		
		return Messages.getString("JikesBT.exception_[from_{0}_to_before_{1}_finally_at_{2}]_3",
			new Object[] {Integer.toString(indices.start), Integer.toString(indices.end), Integer.toString(indices.handler)});
	}
	
	/**
	 * Calculates the instruction indices which indicate the index into the instruction
	 * array for the handler parameters.  Note that this is different from the bytcode
	 * indices for the same, since there are several bytecodes per instruction.
	 */
	public Indices calculateIndices() {
		BT_InsVector ins = code.getInstructions(); 
		int found = 0;
		int start, end, handler;
		start = end = handler = -1;
		for(int i=0; i<ins.size() && found < 3; i++) {
			BT_Ins element = ins.elementAt(i);
			if(element == startPCTarget) {
				start = i;
				found++;
			}
			if(element == endPCTarget) {
				end = i;
				found++;
			}
			if(element == handlerTarget) {
				handler = i;
				found++;
			}
		}
		Indices indices = new Indices(start, end, handler);
		return indices;
	}
	
	public String toAssemblerString() {
		String catchTypeName;
		if (catchType != null) {
			catchTypeName = catchType.name;
		} else {
			catchTypeName = '(' + ANY + ')';
		}
		return  EXCEPTION + ' ' +  FROM + ' ' + startPCTarget.getLabel() + ' ' +  TO + ' ' + endPCTarget.getLabel() + 
			+ ' ' +  CATCH + ' ' + catchTypeName + ' ' +  HANDLER + ' ' + handlerTarget.getLabel();
	}
}
