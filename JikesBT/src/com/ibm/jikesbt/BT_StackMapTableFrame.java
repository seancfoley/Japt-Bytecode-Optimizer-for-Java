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
public abstract class BT_StackMapTableFrame extends BT_StackMapFrame {

	BT_StackMapTableFrame previousFrame;
	
    BT_StackMapTableFrame(BT_StackMapTableFrame previousFrame, int offsetDelta) throws BT_AttributeException {
    	super(offsetDelta);
    	this.previousFrame = previousFrame;
    }
    
    public BT_StackMapTableFrame(BT_StackMapTableFrame previousFrame, BT_BasicBlockMarkerIns instruction) {
    	super(instruction);
        this.previousFrame = previousFrame;
    }
    
    String getAttributeName() {
    	return BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME;
    }
    
	int getOffsetDelta() {
		if(instruction.opcode == OFFSET_INDICATOR_OPCODE) {
			//not dereferenced yet
			return instruction.byteIndex;
		}
		if(previousFrame == null) {
			return instruction.byteIndex;
		}
		return (instruction.byteIndex - previousFrame.instruction.byteIndex) - 1; 
	}
	
	void dereference(BT_Repository rep, BT_StackMapAttribute owner) throws BT_InstructionReferenceException {
		if(instruction.opcode == OFFSET_INDICATOR_OPCODE) {
			BT_CodeAttribute code = owner.getCode();
			//not dereferenced yet
			if(previousFrame == null) {
				instruction = code.getInstructions().findBasicBlock(code, owner, instruction.byteIndex);
			} else {
				int byteCodeOffset = previousFrame.instruction.byteIndex + instruction.byteIndex + 1;
				instruction = code.getInstructions().findBasicBlock(code, owner, byteCodeOffset);
			}
		}
	}
	
	/**
	 * Populate the stack and locals in newShapes based on the previous stack and locals.
	 * All stacks are allocated from the pool.  The new stacks will be allocated from the pool
	 * even if they are the same as the previous stacks.
	 * @param previousShapes
	 * @param newShapes
	 * @param pool
	 */
	abstract void getStack(StackShapes previousShapes, StackShapes newShapes, BT_StackPool pool);
	
	static class SameFrame extends BT_StackMapTableFrame {
 		//same locals as previous frame and 0 stack items
 		
 		public SameFrame(BT_StackMapTableFrame previousFrame, BT_BasicBlockMarkerIns ins) {
 			super(previousFrame, ins);
 		}
		
 		SameFrame(BT_StackMapTableFrame previousFrame, int offsetDelta) throws BT_AttributeException {
 			super(previousFrame, offsetDelta);
 		}
 		
 		int getLength() {
 			if(getOffsetDelta() < 64) {
 				return 1;
 			}
 			return 3;
 		}
 		
 		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
 			int delta = getOffsetDelta();
			if(delta < 64) {
 				dos.writeByte(delta);
 			} else {
 				dos.writeByte(251);
 				dos.writeShort(delta);
 			}
 		}
 		
 		public String toString() {
			return "same frame at instruction " + instruction + "\n";
		}
 		
 		void getStack(StackShapes previousShapes, StackShapes newShapes, BT_StackPool pool) {
 			newShapes.newLocals = pool.getDuplicate(previousShapes.newLocals);
 			newShapes.newStack = BT_StackPool.emptyStack;
 		}
 		
 		public String getName() {
			return "Same";
		}
 	}
 	
 	static class ChopFrame extends BT_StackMapTableFrame {
 		//same locals as in previous frame except last absentLocals are absent, stack items is 0
 		/* these type counts can include a double or long, and if so, they do not include the TOP value that accompanies each double and long */
 		private int absentLocals;
 		
 		ChopFrame(BT_StackMapTableFrame previousFrame, int absentLocals, int offsetDelta) throws BT_AttributeException {
 			super(previousFrame, offsetDelta);
 			this.absentLocals = absentLocals;
 		}
 		
 		public ChopFrame(BT_StackMapTableFrame previousFrame, BT_BasicBlockMarkerIns ins, int absentLocals) {
 			super(previousFrame, ins);
 			this.absentLocals = absentLocals;
 		}
 		
 		int getLength() {
 			return 3;
 		}
 		
 		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
 			dos.writeByte(251 - absentLocals);
 			dos.writeShort(getOffsetDelta());
 		}
 		
 		public String toString() {
			return "chop frame with " + absentLocals + " locals absent at instruction " + instruction + '\n';
		}
 		
 		void getStack(StackShapes previousShapes, StackShapes newShapes, BT_StackPool pool) {
 			int absent = absentLocals;
 			BT_LocalCell prevLocals[] = previousShapes.newLocals;
 			for(int i=0, j = prevLocals.length - 1; i<absentLocals && j > 0; i++, j--) {
 				BT_StackType type = prevLocals[j - 1].getCellType();
 				if(type.isTwoSlot()) {
 					absent++;
 					j--;
 				}
 			}
 			newShapes.newLocals = pool.getDuplicate(previousShapes.newLocals, absent);
 			newShapes.newStack = BT_StackPool.emptyStack;
 		}
 		
 		public String getName() {
			return "Chop " + absentLocals;
		}
 	}
 	
 	static class SameLocalsOneStackItemFrame extends BT_StackMapTableFrame {
 		//same locals as previous frame and one stack item
 		/* this type can be a double or long, and if a double or long, does not include the TOP value that accompanies each double and long */
 		private BT_StackType stackItem;
 		
 		public SameLocalsOneStackItemFrame(BT_StackMapTableFrame previousFrame, BT_BasicBlockMarkerIns ins, BT_StackType stackItem) {
 			super(previousFrame, ins);
 			this.stackItem = stackItem;
 		}
 		
 		SameLocalsOneStackItemFrame(BT_StackMapTableFrame previousFrame, int offsetDelta) throws BT_AttributeException {
 			super(previousFrame, offsetDelta);
 		}
 		
 		public Object clone() {
 			SameLocalsOneStackItemFrame f = (SameLocalsOneStackItemFrame) super.clone();
 			stackItem = (BT_StackType) stackItem.clone();
 			return f;
 		}
 		
 		void read(DataInputStream dis, BT_ConstantPool pool, BT_CodeAttribute container) 
 				throws IOException, BT_AttributeException, BT_ConstantPoolException, BT_DescriptorException {
 			stackItem = readType(dis, pool);
		}
		
		void dereference(BT_Repository rep, BT_StackMapAttribute owner) throws BT_InstructionReferenceException {
			super.dereference(rep, owner);
	 		stackItem.dereference(rep, owner);
 		}
		
		void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
			super.changeReferencesFromTo(oldIns, newIns);
			stackItem.changeReferencesFromTo(oldIns, newIns);
		}
 		
		void resolve(BT_ConstantPool pool) {
			stackItem.resolve(pool);
		}
		
		int getLength() {
 			if(getOffsetDelta() < 64) { 
 				return 1 + stackItem.getWrittenLength();
 			} else {
 				return 3 + stackItem.getWrittenLength();
 			}
 		}
 		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			int delta = getOffsetDelta();
			if(delta < 64) { 
 				dos.writeByte(delta + 64);
			} else {
				dos.writeByte(247);
				dos.writeShort(delta);
			}
			stackItem.write(dos, pool);
 		}
		
		public String toString() {
			return "same locals with stack item " + stackItem + " at instruction " + instruction + '\n';
		}
 		
		void getStack(StackShapes previousShapes, StackShapes newShapes, BT_StackPool pool) {
 			newShapes.newLocals = pool.getDuplicate(previousShapes.newLocals);
 			boolean isTwo = stackItem.isTwoSlot();
 			int size = isTwo ? 2 : 1;
 			BT_StackCell stack[] = pool.getStack(size);
 			stack[0] = stackItem;
 			if(isTwo) {
 				stack[1] = BT_StackType.TOP;
 			}
 			newShapes.newStack = stack;
 		}
		
		public String getName() {
			return "Same Locals One Stack Item";
		}
 	}
 	
 	static class AppendFrame extends BT_StackMapTableFrame { 
		//same locals as in previous frame except newLocals new locals, stack items is 0
 		/* these types can include a double and long, and if so, they do not include the TOP value that accopmanies each double and long */
 		private BT_StackType newLocals[];
 		
 		AppendFrame(BT_StackMapTableFrame previousFrame, int offsetDelta, int numLocals) throws BT_AttributeException {
 			super(previousFrame, offsetDelta);
 			newLocals = new BT_StackType[numLocals];
 		}
 		
 		public AppendFrame(BT_StackMapTableFrame previousFrame, BT_BasicBlockMarkerIns ins, BT_StackType newLocals[]) {
 			super(previousFrame, ins);
 			this.newLocals = newLocals;
 		}
 		
 		public Object clone() {
 			AppendFrame f = (AppendFrame) super.clone();
 			newLocals = (BT_StackType[]) newLocals.clone();
 			for(int i=0; i<newLocals.length; i++) {
 				newLocals[i] = (BT_StackType) newLocals[i].clone();
 			}
 			return f;
 		}
 		
 		void read(DataInputStream dis, BT_ConstantPool pool, BT_CodeAttribute container) 
 				throws IOException, BT_AttributeException, BT_ConstantPoolException, BT_DescriptorException {
 			for(int i=0; i<newLocals.length; i++) {
 				newLocals[i] = readType(dis, pool);
 			} 
		}
 		
 		void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
			super.changeReferencesFromTo(oldIns, newIns);
			for(int i=0; i<newLocals.length; i++) {
 				newLocals[i].changeReferencesFromTo(oldIns, newIns);
 			}
		}
		
		void dereference(BT_Repository rep, BT_StackMapAttribute owner) throws BT_InstructionReferenceException {
			super.dereference(rep, owner);
	 		for(int i=0; i<newLocals.length; i++) {
 				newLocals[i].dereference(rep, owner);
 			}
 		}

		void resolve(BT_ConstantPool pool) {
			for(int i=0; i<newLocals.length; i++) {
 				newLocals[i].resolve(pool);
 			}
		}
		
		int getLength() {
			int length = 3;
			for(int i=0; i<newLocals.length; i++) {
				length += newLocals[i].getWrittenLength();
			}
			return length;
 		}
 		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			dos.writeByte(newLocals.length + 251);
			dos.writeShort(getOffsetDelta());
			for(int i=0; i<newLocals.length; i++) {
				newLocals[i].write(dos, pool);
			}
 		}
		
		public String toString() {
			return "append frame at instruction " + instruction + "\n" + BT_StackType.toString(newLocals, null);
		}
		
		void getStack(StackShapes previousShapes, StackShapes newShapes, BT_StackPool pool) {
			newShapes.newLocals = BT_StackMapFrames.copyFrameArrayToCellArray(previousShapes.newLocals, newLocals, pool);
 			newShapes.newStack = BT_StackPool.emptyStack;
 		}
		
		public String getName() {
			return "Append " + newLocals.length;
		}
 	}
 	
 	static class FullFrame extends BT_StackMapTableFrame {
 		/* these types do not include the TOP value that accopmanies each double and long */
 		private BT_StackType locals[];
 		private BT_StackType stack[];
 		
 		FullFrame(BT_StackMapTableFrame previousFrame, int offsetDelta) throws BT_AttributeException {
 			super(previousFrame, offsetDelta);
 		}
 		
 		public FullFrame(BT_StackMapTableFrame previousFrame, BT_BasicBlockMarkerIns ins, BT_StackType locals[], BT_StackType stack[]) {
 			super(previousFrame, ins);
 			this.locals = locals;
 			this.stack = stack;	
 		}
 		
 		public Object clone() {
 			FullFrame f = (FullFrame) super.clone();
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
			super.dereference(rep, owner);
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
			int length = 7;
			for(int i=0; i<locals.length; i++) {
				length += locals[i].getWrittenLength();
			}
			for(int i=0; i<stack.length; i++) {
				length += stack[i].getWrittenLength();
			}
			return length;
 		}
 		
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			dos.writeByte(255);
			dos.writeShort(getOffsetDelta());
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
			return "full frame at instruction " + instruction + "\n" + BT_StackType.toString(locals, stack);
		}
		
		
		void getStack(StackShapes previousShapes, StackShapes newShapes, BT_StackPool pool) {
			newShapes.newLocals = BT_StackMapFrames.copyFrameArrayToCellArray(null, locals, pool);
			newShapes.newStack = BT_StackMapFrames.copyFrameArrayToCellArray(stack, pool);
 		}
		
		public String getName() {
			return "Full";
		}
 	}
}
