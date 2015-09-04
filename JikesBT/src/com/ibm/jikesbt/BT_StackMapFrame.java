/*
 * Created on Aug 20, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_StackType.StackFrameClassType;
import com.ibm.jikesbt.BT_StackType.StackFrameUninitializedOffset;

/**
 * @author Sean Foley
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class BT_StackMapFrame implements Cloneable {

	static final int OFFSET_INDICATOR_OPCODE = -1;
	BT_BasicBlockMarkerIns instruction;
    
    BT_StackMapFrame(int offsetDelta) throws BT_AttributeException {
    	if(offsetDelta < 0) {
    		throw new BT_AttributeException(getAttributeName(), "invalid frame");
    	}
    	/* A fake instruction is used to indicate the offset to the real instruction when dereferencing occurs.
    	 * This saves us from having to keep an additional field in this class - we overload the instruction field.
    	 */
    	this.instruction = BT_Ins.make();
    	instruction.byteIndex = offsetDelta;
    	instruction.opcode = OFFSET_INDICATOR_OPCODE;
    }
    
    public BT_StackMapFrame(BT_BasicBlockMarkerIns instruction) {
    	this.instruction = instruction;
    }
    
    abstract String getAttributeName();
	
	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {
			return null;
		}
	}
	
	void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
		if(instruction == oldIns) {
			instruction = (BT_BasicBlockMarkerIns) newIns;
		}
	}
	
	BT_StackType readType(DataInputStream dis, BT_ConstantPool pool) 
		throws IOException, BT_ConstantPoolException, BT_AttributeException, BT_DescriptorException {
		int typeType = dis.readUnsignedByte();
		switch(typeType) {
			case BT_StackType.ITEM_TOP:
				return BT_StackType.TOP;
			case BT_StackType.ITEM_INTEGER:
				return pool.getRepository().getInt().classType;
			case BT_StackType.ITEM_FLOAT:
				return pool.getRepository().getFloat().classType;
			case BT_StackType.ITEM_LONG:
				return pool.getRepository().getLong().classType;
			case BT_StackType.ITEM_DOUBLE:
				return pool.getRepository().getDouble().classType;
			case BT_StackType.ITEM_NULL:
				return BT_StackType.NULL;
			case BT_StackType.ITEM_UNINITIALIZED_THIS:
				return BT_StackType.UNINITIALIZED_THIS;
			case BT_StackType.ITEM_OBJECT:
				return new StackFrameClassType(pool.getClassNameAt(dis.readUnsignedShort(), BT_ConstantPool.CLASS));
			case BT_StackType.ITEM_UNINITIALIZED: {
				int instructionOffset = dis.readUnsignedShort();
				return new StackFrameUninitializedOffset(instructionOffset);
			}
			default:
				throw new BT_AttributeException(getAttributeName(), "unknown stack map type");
		}
	}
	
	/* overridden for frames that have additional data past the offset */
	void read(DataInputStream dis, BT_ConstantPool pool, BT_CodeAttribute container) 
		throws IOException, BT_AttributeException, BT_ConstantPoolException, BT_DescriptorException {}
	
	abstract void dereference(BT_Repository rep, BT_StackMapAttribute owner) throws BT_InstructionReferenceException;
    
	void resolve(BT_ConstantPool pool) {}
	
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {}
	
	abstract int getLength();//the total length
	
	public abstract String toString();
	
	public abstract String getName();
	
	
}
