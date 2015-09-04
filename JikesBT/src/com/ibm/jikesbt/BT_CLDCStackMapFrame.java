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
import com.ibm.jikesbt.BT_StackShapeVisitor.StackShapes;

/**
 * @author Sean Foley
 *
 * Represents a Java 5 stack map table stack frame
 */
public class BT_CLDCStackMapFrame extends BT_StackMapFrame {
	/* these types do not include the TOP value that accopmanies each double and long */
	private BT_StackType locals[];
	private BT_StackType stack[];
	
	BT_CLDCStackMapFrame(int offset) throws BT_AttributeException {
    	super(offset);
    }
    
	public BT_CLDCStackMapFrame(BT_BasicBlockMarkerIns instruction, BT_StackType locals[], BT_StackType stack[]) {
    	super(instruction);
    	this.locals = locals;
		this.stack = stack;	
    }
    
	public Object clone() {
		BT_CLDCStackMapFrame f = (BT_CLDCStackMapFrame) super.clone();
		locals = (BT_StackType[]) locals.clone();
		for(int i=0; i<locals.length; i++) {
			locals[i] = (BT_StackType) locals[i].clone();
		}
		stack = (BT_StackType[]) stack.clone();
		for(int i=0; i<stack.length; i++) {
			stack[i] = (BT_StackType) stack[i].clone();
		}
		return f;
	}
	
    String getAttributeName() {
    	return BT_StackMapAttribute.CLDC_STACKMAP_NAME;
    }
    
    void getStack(StackShapes newShapes, BT_StackPool pool) {
    	newShapes.newLocals = BT_StackMapFrames.copyFrameArrayToCellArray(null, locals, pool);
		newShapes.newStack = BT_StackMapFrames.copyFrameArrayToCellArray(stack, pool);
	}
		 		
	void read(DataInputStream dis, BT_ConstantPool pool, BT_CodeAttribute container) 
				throws IOException, BT_AttributeException, BT_ConstantPoolException, BT_DescriptorException {
		int num = dis.readUnsignedShort();
		locals = new BT_StackType[num];
		for(int i=0; i<locals.length; i++) {
			locals[i] = readType(dis, pool);
		}
		num = dis.readUnsignedShort();
		stack = new BT_StackType[num];
		for(int i=0; i<stack.length; i++) {
			stack[i] = readType(dis, pool);
		} 
	}
		
	void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
		super.changeReferencesFromTo(oldIns, newIns);
		for(int i=0; i<locals.length; i++) {
				locals[i].changeReferencesFromTo(oldIns, newIns);
			}
			for(int i=0; i<stack.length; i++) {
				stack[i].changeReferencesFromTo(oldIns, newIns);
			}
	}
	
	void dereference(BT_Repository rep, BT_StackMapAttribute owner) throws BT_InstructionReferenceException {
		if(instruction.opcode == OFFSET_INDICATOR_OPCODE) {
			BT_CodeAttribute code = owner.getCode();
			instruction = code.getInstructions().findBasicBlock(code, owner, instruction.byteIndex);
		}
		for(int i=0; i<locals.length; i++) {
			locals[i].dereference(rep, owner);
		}
		for(int i=0; i<stack.length; i++) {
			stack[i].dereference(rep, owner);
		}
	}
		
	void resolve(BT_ConstantPool pool) {
		for(int i=0; i<locals.length; i++) {
			locals[i].resolve(pool);
		}
		for(int i=0; i<stack.length; i++) {
			stack[i].resolve(pool);
		}
	}
	
	int getLength() {
		int length = 6;
		for(int i=0; i<locals.length; i++) {
			length += locals[i].getWrittenLength();
		}
		for(int i=0; i<stack.length; i++) {
			length += stack[i].getWrittenLength();
		}
		return length;
	}
		
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(instruction.byteIndex);
		dos.writeShort(locals.length);
		for(int i=0; i<locals.length; i++) {
			locals[i].write(dos, pool);
		}
		dos.writeShort(stack.length);
		for(int i=0; i<stack.length; i++) {
			stack[i].write(dos, pool);
		}
	}
	
	public String toString() {
		return "frame at instruction " + instruction + "\n" + BT_StackType.toString(locals, stack);
	}
	
	public String getName() {
		return "CLDC";
	}
}
