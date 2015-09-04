package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_Repository.LoadLocation;




/**
 Models the "LocalVariableTable" attribute of a {@link BT_CodeAttribute}.

 <p> From the JVM documentation:
 The LocalVariableTable attribute is an optional variable-length attribute of a
 Code (§4.7.4) attribute. It may be used by debuggers to determine the value of
 a given local variable during the execution of a method. If LocalVariableTable
 attributes are present in the attributes table of a given Code attribute, then
 they may appear in any order. There may be no more than one LocalVariableTable
 attribute per local variable in the Code attribute.

 <code><pre>
 *      LocalVariableTable_attribute {
 *
 *          u2 attribute_name_index;
 *          u4 attribute_length;
 *          u2 local_variable_table_length;                      // number of entries
 *          {   u2 start_pc;
 *              u2 length;
 *              u2 name_index;
 *              u2 descriptor_index;
 *              u2 index;
 *          } local_variable_table[
 *                          local_variable_table_length];
 *      }
 </pre></code>
 * @author IBM
**/
/*
   Details:  The items of the LocalVariableTable_attribute structure are as follows:

        start_pc, length

       The given local variable must have a value at indices into the code array
       in the interval [start_pc, start_pc+length], that is, between start_pc and
       start_pc+length inclusive. The value of start_pc must be a valid index
       into the code array of this Code attribute of the opcode of an
       instruction. The value of start_pc+length must be either a valid index
       into the code array of this Code attribute of the opcode of an
       instruction, or the first index beyond the end of that code array.

       Comment:  If it's really "inclusive", why can start_pc+length be
       "the first index beyond the end of that code array"?

        name_index, descriptor_index

       The value of the name_index item must be a valid index into the
       constant_pool table. The constant_pool entry at that index must contain a
       CONSTANT_Utf8_info (§4.4.7) structure representing a valid Java local
       variable name stored as a simple name (§2.7.1).

       The value of the descriptor_index item must be a valid index into the
       constant_pool table. The constant_pool entry at that index must contain a
       CONSTANT_Utf8_info (§4.4.7) structure representing a valid descriptor for
       a Java local variable. Java local variable descriptors have the same form
       as field descriptors (§4.3.2).

        index

       The given local variable must be at index in its method's local variables.
       If the local variable at index is a two-word type (double or long), it
       occupies both index and index+1.
*/
public final class BT_LocalVariableAttribute extends BT_Attribute {

	public static final String LOCAL_VAR_ATTRIBUTE_NAME = "LocalVariableTable";
	public static final String LOCAL_VAR_TYPE_ATTRIBUTE_NAME = "LocalVariableTypeTable";
	
	public LV[] localVariables; // (LV is nested)
	
	private String name;
	
	/**
	 Constructs an attribute but defer filling in "localVariables".
	 @param ne         The number of PcRanges to allocate initially.
	 @param container  The BT_CodeAttribute that contains this attribute.
	**/
	public BT_LocalVariableAttribute(
			String name, 
			int ne, 
			BT_CodeAttribute container) {
		super(container);
		localVariables = new LV[ne];
		this.name = name;
	}

	/**
	 This form of constructor is used when a class file is read.
	 @param name either "LocalVariableTable" or "LocalVariableTypeTable"
	 @param data The part of the attribute value following "attribute_length" from the class file.
	 @param container  The BT_CodeAttribute that contains this attribute.
	**/
	BT_LocalVariableAttribute(
		String name,
		byte data[],
		BT_ConstantPool pool,
		BT_CodeAttribute container,
		LoadLocation loadedFrom)
		throws BT_AttributeException {
		super(container, loadedFrom);
		this.name = name;
		try {
			DataInputStream dis =
				new DataInputStream(new ByteArrayInputStream(data));
			int ne = dis.readUnsignedShort(); // AKA local_variable_table_length
			if (data.length != 2 + 10 * ne)
				throw new BT_AttributeException(name,
					Messages.getString("JikesBT.{0}_attribute_length_2", name));
			localVariables = new LV[ne];
			try {
				for (int ie = 0; ie < localVariables.length; ++ie) { // Per description
					int startPC = dis.readUnsignedShort(); // AKA start_pc
					localVariables[ie] =
						new UndereferencedLocalVariable(startPC, startPC + dis.readUnsignedShort(),
							// start_pc+length
							pool.getUtf8At(dis.readUnsignedShort()), // AKA name_index
							pool.getUtf8At(dis.readUnsignedShort()), // AKA descriptor_index
							dis.readUnsignedShort()); // AKA index
				} // Per description
			} catch(BT_ConstantPoolException e) {
				throw new BT_AttributeException(getName(), e);
			}
		} catch(IOException e) {
			throw new BT_AttributeException(getName(), e);
		}
	}
	
	public BT_CodeAttribute getCode() {
		return (BT_CodeAttribute) getOwner();
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * The LocalVariableTable is dereferenced, while the LocalVariableTypeTable is not.
	 * 
	 * In this context, to be deferenced means that the type of
	 * each local variable will reference a class object in the jikesbt
	 * repository.  So if these classes change, then the attribute
	 * will be aware of these changes.
	 * The non-dereferenced table simple stores a string with the class
	 * name, and this string is never changed.
	 * The LocalVariableTypeTable is not dereferenced because the type string
	 * is a generic class name, and therefore does not actually exist in the 
	 * jikesbt repository.
	 * @return whether this table is dereferenced.
	 */
	boolean isLocalTable() {
		return name.equals(LOCAL_VAR_ATTRIBUTE_NAME);
	}
	
	
	public void removeEmptyRanges(int maxLocals) {
		int count = 0;
		for (int j = 0; j < localVariables.length; j++) {
			BT_LocalVariableAttribute.LV lv = localVariables[j];
			if (lv.getLocalIndex() < maxLocals && !lv.rangeIsEmpty())
				count++;
			else localVariables[j] = null;
		}
		if (count < localVariables.length) {
			BT_LocalVariableAttribute.LV[] newLocalVariables =
				new BT_LocalVariableAttribute.LV[count];
			count = 0;
			for (int j = 0; j < localVariables.length; j++) {
				if (localVariables[j] != null)
					newLocalVariables[count++] = localVariables[j];
			}
			localVariables = newLocalVariables;
		}
	}
	
	/**
	 @param  inc  The increment.
	 @param start ignore locals access for start and below
	**/
	public void incrementLocalsAccessWith(
		int inc,
		int start,
		BT_LocalVector locals) {
		for (int ie = 0; ie < localVariables.length; ++ie) {
			LV lv = localVariables[ie];
			lv.incrementLocalsAccessWith(inc, start, locals);
		}
	}
	

	// Converts class file artifacts (counters, offsets, ...) into references to related objects.
	void dereference(BT_Repository rep) throws BT_AttributeException {
		try {
			for (int ie = 0; ie < localVariables.length; ++ie) {
				LV lv = localVariables[ie];
				if(lv.isDereferenced()) {
					continue;
				}
				//change to a dereference local
				UndereferencedLocalVariable original = (UndereferencedLocalVariable) lv;
				BT_CodeAttribute inCode = getCode();
				BT_InsVector inst = inCode.getInstructions();
				BT_Ins startIns = inst.findInstruction(inCode, this, original.startPC);
				BT_Ins beyondIns = null;
				if (inst.size() > 0) { // Are instructions
					BT_Ins lastIns = inst.lastElement();
					int beyondPC = original.beyondPC;
					if (beyondPC <= lastIns.byteIndex) { // Not after the last ins
						beyondIns = inst.findInstruction(inCode, this, beyondPC);
					} else if(beyondPC == lastIns.byteIndex + lastIns.size()) {
						beyondIns = null;
					} else if(beyondPC > lastIns.byteIndex + lastIns.size()) {
						throw new BT_InstructionReferenceException(inCode, this, beyondPC,
							Messages.getString("JikesBT.instruction_reference_out_of_range_3"));
					} else {
						throw new BT_InstructionReferenceException(inCode, this, beyondPC,
							Messages.getString("JikesBT.invalid_instruction_reference_4"));
					}
				} // Are instructions
				BT_Local local = inCode.getLocals().elementAt(original.localIndex);
				String name = original.nameS;
				if(isLocalTable()) {
					BT_Class descriptorClass = 
						inCode.getMethod().cls.repository.linkTo(BT_ConstantPool.toJavaName(original.descriptorName));
					lv = new LocalVariable(startIns, beyondIns, name, local, descriptorClass);
				} else {
					String descriptorName = original.descriptorName;
					lv = new GenericLocalVariable(startIns, beyondIns, name, local, descriptorName);
				}
				localVariables[ie] = lv;
			}
		} catch(BT_DescriptorException e) {
			throw new BT_AttributeException(getName(), e);
		} catch(BT_InstructionReferenceException e) {
			throw new BT_AttributeException(getName(), e);
		}
	}

	/**
	 * @param switchingCodeAttributes true if oldIns and newIns are not in the same code attribute
	 */
	public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switchingAttributes) {
		for (int ie = 0; ie < localVariables.length; ++ie) // Per description
			localVariables[ie].changeReferencesFromTo(oldIns, newIns);
	}

	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(name);
		for (int ie = 0; ie < localVariables.length; ++ie) // Per description
			localVariables[ie].resolve(pool);
	}

	
	private LV[] eliminateOverflow(BT_InsVector ins) {
		BT_CodeAttribute inCode = getCode();
		if(BT_Misc.overflowsUnsignedShort(inCode.getInstructionSize())) {
			for(int i=0; i<localVariables.length; i++) {
				int startPC = localVariables[i].getStartBytecodeIndex();
				int length = localVariables[i].getBeyondBytecodeIndex(ins) - startPC;
				if(BT_Misc.overflowsUnsignedShort(startPC) || BT_Misc.overflowsUnsignedShort(length)) {
					boolean isBadRange[] = new boolean[localVariables.length];
					isBadRange[i] = true;
					int goodCount = i;
					for(int k = i + 1; k<localVariables.length; k++) {
						startPC = localVariables[k].getStartBytecodeIndex();
						length = localVariables[k].getBeyondBytecodeIndex(ins) - startPC;
						if(BT_Misc.overflowsUnsignedShort(startPC) || BT_Misc.overflowsUnsignedShort(length)) {
							isBadRange[k] = true;
						} else {
							goodCount++;
						}
					}
					LV[] newRanges = new LV[goodCount];
					int index = 0;
					for(int k=0; k<localVariables.length; k++) {
						LV range = localVariables[k];
						if(!isBadRange[k]) {
							newRanges[index++] = range;
						}
					}
					return newRanges;
				}
			}
		}
		return localVariables;
	}
	
	protected int writtenLength() {
		BT_CodeAttribute inCode = getCode();
		return // 2 for attribute_name_index
			// 4 for attribute_length
			// 2 for local_variable_table_length
			8 + eliminateOverflow(inCode.getInstructions()).length * 10;
	}

	// This must be kept in synch with {@link BT_LocalVariableAttribute#writtenLength()}.
	void write(java.io.DataOutputStream dos, BT_ConstantPool pool)
		throws java.io.IOException {
		
		BT_CodeAttribute inCode = getCode();
		LV locals[] = eliminateOverflow(inCode.getInstructions());
		dos.writeShort(pool.indexOfUtf8(name));
		// AKA attribute_name_index
		dos.writeInt(2 + 10 * locals.length); // AKA attribute_length
		dos.writeShort(locals.length);
		
		// AKA local_variable_table_length
		for (int ie = 0; ie < locals.length; ++ie) { // Per description
			locals[ie].write(dos, pool, inCode);
		} // Per description
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_size_{1}_4", 
				new Object[] {name, Integer.toString(localVariables.length)});
	}

	public void print(java.io.PrintStream ps, String prefix) {
		ps.println(prefix + Messages.getString("JikesBT.LocalVariables__4"));
		for (int i = 0; i < localVariables.length; ++i) // Per element
			ps.println(prefix + "  " + localVariables[i]);
	}

	public boolean singletonRequired() {
		return false;
	}
	
	public Object clone() {
		BT_LocalVariableAttribute att = (BT_LocalVariableAttribute) super.clone();
		att.localVariables = (BT_LocalVariableAttribute.LV[]) localVariables.clone();
		for(int i=0; i<att.localVariables.length; i++) {
			att.localVariables[i] = (BT_LocalVariableAttribute.LV) att.localVariables[i].clone();
		}
		return att;
	}
	
	public static abstract class LV implements Cloneable {
		
		/**
		 The name of this local variable.
		**/
		public final String nameS;

		LV(String name) {
			this.nameS = name;
		}
		
		abstract void write(java.io.DataOutputStream dos, BT_ConstantPool pool, BT_CodeAttribute inCode)
			throws IOException;
		
		abstract void resolve(BT_ConstantPool pool);
		
		public abstract void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns);
		
		public abstract boolean isDereferenced();
		
		public abstract String toString();
		
		public abstract int getLocalIndex();
		
		public abstract boolean rangeIsEmpty();
		
		public abstract String getTypeName();
		
		public abstract int getStartBytecodeIndex();
		
		public abstract int getBeyondBytecodeIndex(BT_InsVector ins);
		
		/**
		 * ensure that BT_Ins.byteIndex is correct before calling this method.
		 * This can be done be calling BT_CodeAttribute.computeInstrucionSizes()
		 * or BT_InsVector.setAllByteIndexes()
		 * @param ins
		 * @return
		 */
		public abstract boolean isWithinRange(BT_Ins ins);
		
		public abstract void incrementLocalsAccessWith(int increment, int start, BT_LocalVector locals);
		
		public Object clone() {
			try {
				return super.clone();
			} catch(CloneNotSupportedException e) {
				return null;
			}
		}
	}
	
	
	public static class GenericLocalVariable extends DereferencedLocal {
		/**
		 The type name of this local variable.
		**/
		public String descriptorName;
		
		public GenericLocalVariable(BT_Ins si, BT_Ins bi, String n, BT_Local local, String descriptorName) {
			super(si, bi, n, local);
			this.descriptorName = descriptorName;
		}
		
		int descriptorCPIx(BT_ConstantPool pool) {
			return pool.indexOfUtf8(descriptorName);
		}
		
		public String getTypeName() {
			return descriptorName;
		}
	}
	
	public static class LocalVariable extends DereferencedLocal {
		/**
		 The type of this local variable.
		**/
		public BT_Class descriptorC;
		
		public LocalVariable(BT_Ins si, BT_Ins bi, String n, BT_Local local, BT_Class c) {
			super(si, bi, n, local);
			descriptorC = c;
		}
		
		int descriptorCPIx(BT_ConstantPool pool) {
			return pool.indexOfUtf8(BT_ConstantPool.toInternalName(descriptorC.getName()));
		}
		
		public String getTypeName() {
			return descriptorC.getName();
		}
	}
	
	public abstract static class DereferencedLocal extends LV {
		/**
		 The first instruction for which this local variable is valid.
		**/
		public BT_Ins startIns;

		/**
		 One beyond the last instruction for which this local variable is valid.
		 Null means ends after the last instruction of the method.
		**/
		public BT_Ins beyondIns;

		
		/**
		 The slot number of this local variable.
		**/
		BT_Local localVariable;
		
		/**
		 @param si  The start instruction.
		 @param bi  Either the first instruction _beyond_ the range,
		   or null to signify that the range extends to the
		   last instruction of the method.
		 @param n   The name of the variable.
		**/
		public DereferencedLocal(BT_Ins si, BT_Ins bi, String n, BT_Local local) {
			super(n);
			startIns = si;
			beyondIns = bi;
			localVariable = local;
		}
		
		public int getStartBytecodeIndex() {
			return startIns.byteIndex;
		}
		
		public int getBeyondBytecodeIndex(BT_InsVector ins) {
			return beyondIns == null ? (ins.lastElement().byteIndex + ins.lastElement().size()) : beyondIns.byteIndex;
		}
		
		
		public boolean isWithinRange(BT_Ins ins) {
			return ins.byteIndex >= startIns.byteIndex && (beyondIns == null || ins.byteIndex < beyondIns.byteIndex);
		}
		
		public boolean rangeIsEmpty() {
			return startIns == beyondIns;
		}
		
		public boolean isDereferenced() {
			return true;
		}
		
		int nameCPIx(BT_ConstantPool pool) {
			return pool.indexOfUtf8(nameS);
		}
		
		abstract int descriptorCPIx(BT_ConstantPool pool);
		
		public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
			if (startIns == oldIns)
				startIns = newIns;
			if (beyondIns == oldIns)
				beyondIns = newIns;
		}
		
		void resolve(BT_ConstantPool pool) {
			nameCPIx(pool);
			descriptorCPIx(pool);
		}
		
		void write(java.io.DataOutputStream dos, BT_ConstantPool pool, BT_CodeAttribute inCode)
			throws IOException {
			dos.writeShort(startIns.byteIndex);
			
			if (beyondIns != null) {
				int index = beyondIns.byteIndex - startIns.byteIndex;
				dos.writeShort(index);
			// From start_pc+length
			} else {
				BT_Ins lastIns = inCode.getLastInstruction();
				int index = lastIns.byteIndex
					+ lastIns.size()
					- startIns.byteIndex;
				dos.writeShort(index);
			}
			
			dos.writeShort(nameCPIx(pool));
			dos.writeShort(descriptorCPIx(pool));
			dos.writeShort(localVariable.localNr);
		}
		
		public String toString() {
			String typeName = getTypeName();
			return Messages.getString("JikesBT.ix__{0}_{1}_{2}_startI__{3}_beyondI__{4}_7",
				new Object[] {Integer.toString(localVariable.localNr), nameS, typeName, startIns, beyondIns});
		}
		
		public int getLocalIndex() {
			return localVariable.localNr;
		}
		
		/**
		 @param  inc  The increment.
		 @param start ignore locals access for start and below
		**/
		public void incrementLocalsAccessWith(
			int inc,
			int start,
			BT_LocalVector locals) {
			if (localVariable.localNr < start)
				return;
			localVariable = locals.elementAt(localVariable.localNr + inc);
		}
	}
	
	public static class UndereferencedLocalVariable extends LV {
		private final int startPC;
		private final int beyondPC;
		
		
		/**
		 The type name of this local variable.
		**/
		public final String descriptorName;
		
		/**
		 The slot number of this local variable.
		**/
		public int localIndex;
		
		
		/**
		 Creates a LocalVariable using the starting program-counter.
		 This class is typically used only while
		 reading byte-codes (before "dereference" is run).
		 @param sp  The "startPC".
		**/
		UndereferencedLocalVariable(
			int sp,
			int bp,
			String n,
			String descriptorS,
			int i) {
			super(n);
			startPC = sp;
			beyondPC = bp;
			descriptorName = descriptorS;
			localIndex = i;
		}
		
		public int getStartBytecodeIndex() {
			return startPC;
		}
		
		public int getBeyondBytecodeIndex(BT_InsVector ins) {
			return beyondPC;
		}
		
		public boolean isWithinRange(BT_Ins ins) {
			return ins.byteIndex >= startPC && ins.byteIndex < beyondPC;
		}
		
		public boolean isDereferenced() {
			return false;
		}
		
		public String getTypeName() {
			return descriptorName;
		}
		
		/**
		 @param  inc  The increment.
		 @param start ignore locals access for start and below
		**/
		public void incrementLocalsAccessWith(
			int inc,
			int start,
			BT_LocalVector locals) {
			if (localIndex < start)
				return;
			localIndex += inc;
		}
		
		
		public boolean rangeIsEmpty() {
			return startPC >= beyondPC;
		}
		
		public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {}
		
		int nameCPIx(BT_ConstantPool pool) {
			return pool.indexOfUtf8(nameS);
		}
		
		int descriptorCPIx(BT_ConstantPool pool) {
			return pool.indexOfUtf8(descriptorName);
		}
		
		void resolve(BT_ConstantPool pool) {
			nameCPIx(pool);
			descriptorCPIx(pool);
		}
		
		void write(java.io.DataOutputStream dos, BT_ConstantPool pool, BT_CodeAttribute inCode)
			throws IOException {
			dos.writeShort(startPC);
			dos.writeShort(beyondPC);
			dos.writeShort(nameCPIx(pool));
			dos.writeShort(descriptorCPIx(pool));
			dos.writeShort(localIndex); // From index
		}
		
		public String toString() {
			return Messages.getString("JikesBT.ix__{0}_{1}_{2}_startI__{3}_beyondI__{4}_7",
				new Object[] {
					Integer.toString(localIndex), 
					nameS, 
					descriptorName, 
					Integer.toString(startPC), 
					Integer.toString(beyondPC)});
		}
		
		public int getLocalIndex() {
			return localIndex;
		}
	}
	
}
