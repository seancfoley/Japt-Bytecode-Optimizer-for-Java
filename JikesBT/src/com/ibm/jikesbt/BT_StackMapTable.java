/*
 * Created on Jul 23, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.io.DataInputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_StackMapTableFrame.AppendFrame;
import com.ibm.jikesbt.BT_StackMapTableFrame.ChopFrame;
import com.ibm.jikesbt.BT_StackMapTableFrame.FullFrame;
import com.ibm.jikesbt.BT_StackMapTableFrame.SameFrame;
import com.ibm.jikesbt.BT_StackMapTableFrame.SameLocalsOneStackItemFrame;
import com.ibm.jikesbt.BT_StackShapeVisitor.StackShapes;


/**
 * @author Sean Foley
 *
 * Represents the Java 5 stack map table
 */
public class BT_StackMapTable extends BT_StackMapFrames {
	
	BT_StackMapTable() {}
    
	public BT_StackMapTable(boolean hasJSR) {
		super(hasJSR, -1, -1);
		if(hasJSR) {
    		return;
    	}
    	this.frames = emptyFrames;
	}
	
	public BT_StackMapTable(BT_StackShapes shapes, boolean isFrame[], int totalFrames, BT_CodeAttribute inCode) {
		this(shapes.initialLocals,
				shapes.stackShapes,
				shapes.localShapes,
				isFrame,
				totalFrames,
				shapes.maxDepth,
				shapes.maxLocals,
				shapes.hasJSR(),
				inCode);
	}
	
    private BT_StackMapTable(
    	BT_LocalCell initialLocals[], 
    	BT_StackCell stackShapes[][], 
    	BT_LocalCell localShapes[][], 
    	boolean isFrame[],
    	int totalFrames,
    	int maxStack,
    	int maxLocals,
    	boolean hasJSR,
    	BT_CodeAttribute container) {
    	super(hasJSR, maxStack, maxLocals);
    	if(hasJSR) {
    		return;
    	}
    	if(totalFrames > 0) {
    		BT_InsVector instructions = container.getInstructions();
	    	BT_StackMapTableFrame frames[] = new BT_StackMapTableFrame[totalFrames];
	    	BT_LocalCell previousLocals[] = initialLocals;
	    	LocalsCount previousCount = new LocalsCount(previousLocals);
	    	LocalsCount localsCount = new LocalsCount();
	    	int frameNum = 0;
	    	int codeLen = stackShapes.length;
	    	for(int i=0; i<codeLen; i++) {
	    		if(isFrame[i]) {
	    			BT_StackCell stack[] = stackShapes[i];
	    			BT_LocalCell locals[] = localShapes[i];
	    			localsCount.init(locals);
	    			BT_BasicBlockMarkerIns mergeIns = (BT_BasicBlockMarkerIns) instructions.elementAt(i);
	    			frames[frameNum] = getNextFrame(mergeIns, locals, localsCount, stack, previousLocals, previousCount, (frameNum > 0) ? frames[frameNum - 1] : null);
	    			frameNum++;
	    			previousLocals = locals;
	    			LocalsCount tmp = previousCount;
	    			previousCount = localsCount;
	    			localsCount = tmp;
	    			
	    		}
	    	}
	    	this.frames = frames;
    	} else {
    		this.frames = emptyFrames;
    	}
    }
    
    public Object clone() {
		BT_StackMapTable ret = (BT_StackMapTable) super.clone();
		if(!hasJSR) {
			for(int i=1; i<frames.length; i++) {
				((BT_StackMapTableFrame) ret.frames[i]).previousFrame = ((BT_StackMapTableFrame)ret.frames[i - 1]);
			}
		}
    	return ret;
    }
    
    String getAttributeName() {
    	return BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME;
    }
    
    public BT_StackShapes getShapes(BT_CodeAttribute container) throws BT_AttributeException {
    	BT_InsVector ins = container.getInstructions();
    	BT_StackCellProvider provider = new BT_StackCellProvider(container.getMethod().cls.getRepository());
    	BT_StackPool pool = BT_StackPool.pool;
    	BT_StackShapes allShapes = new BT_StackShapes(container, pool, provider, true);
    	allShapes.createInitialStacks();
    	if(!hasJSR) {
	    	StackShapes newShapes = new StackShapes();
	    	StackShapes previousShapes = new StackShapes();
	    	previousShapes.newLocals = allShapes.localShapes[0];
	    	previousShapes.newStack = allShapes.stackShapes[0];
	    	//assign the stack maps to the appropriate instructions
	    	for (int i = 0, index = -1; i < frames.length; ++i) {
				BT_StackMapTableFrame current = (BT_StackMapTableFrame) frames[i];
				BT_BasicBlockMarkerIns instruction = current.instruction;
				if(instruction.opcode == BT_StackMapFrame.OFFSET_INDICATOR_OPCODE) {
					//this will happen if not dereferenced yet
					throw new BT_AttributeException(getAttributeName(), "stack map has not been dereferenced yet");
				}
				index = ins.indexOf(instruction, index + 1);
				if(index < 0) {
					//should never reach here, because changing the instruction vector should automatically update the stack maps
					throw new BT_AttributeException(getAttributeName(), "stack map references inexistent instruction");
				}
				current.getStack(previousShapes, newShapes, pool);
				allShapes.stackShapes[index] = newShapes.newStack;
				allShapes.localShapes[index] = newShapes.newLocals;
				if(index == 0) {
					allShapes.mergedInitial = true;
				}
				allShapes.frames[index] = current;
				StackShapes tmp = previousShapes;
				previousShapes = newShapes;
				newShapes = tmp;
			}
	    }
    	return allShapes;
    }
    
    void read(DataInputStream dis, BT_ConstantPool pool, BT_CodeAttribute container) 
    		throws IOException, BT_AttributeException, BT_ConstantPoolException, BT_DescriptorException {
		int numFrames = dis.readUnsignedShort();
		frames = new BT_StackMapTableFrame[numFrames];
		BT_StackMapTableFrame frame = null;
		for(int i=0; i<frames.length; i++) {
			int frameType = dis.readUnsignedByte();
			switch(frameType) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
				case 8:
				case 9:
				case 10:
				case 11:
				case 12:
				case 13:
				case 14:
				case 15:
				case 16:
				case 17:
				case 18:
				case 19:
				case 20:
				case 21:
				case 22:
				case 23:
				case 24:
				case 25:
				case 26:
				case 27:
				case 28:
				case 29:
				case 30:
				case 31:
				case 32:
				case 33:
				case 34:
				case 35:
				case 36:
				case 37:
				case 38:
				case 39:
				case 40:
				case 41:
				case 42:
				case 43:
				case 44:
				case 45:
				case 46:
				case 47:
				case 48:
				case 49:
				case 50:
				case 51:
				case 52:
				case 53:
				case 54:
				case 55:
				case 56:
				case 57:
				case 58:
				case 59:
				case 60:
				case 61:
				case 62:
				case 63:
					frame = new SameFrame(frame, frameType);
					break;
				case 64:
				case 65:
				case 66:
				case 67:
				case 68:
				case 69:
				case 70:
				case 71:
				case 72:
				case 73:
				case 74:
				case 75:
				case 76:
				case 77:
				case 78:
				case 79:
				case 80:
				case 81:
				case 82:
				case 83:
				case 84:
				case 85:
				case 86:
				case 87:
				case 88:
				case 89:
				case 90:
				case 91:
				case 92:
				case 93:
				case 94:
				case 95:
				case 96:
				case 97:
				case 98:
				case 99:
				case 100:
				case 101:
				case 102:
				case 103:
				case 104:
				case 105:
				case 106:
				case 107:
				case 108:
				case 109:
				case 110:
				case 111:
				case 112:
				case 113:
				case 114:
				case 115:
				case 116:
				case 117:
				case 118:
				case 119:
				case 120:
				case 121:
				case 122:
				case 123:
				case 124:
				case 125:
				case 126:
				case 127:
					frame = new SameLocalsOneStackItemFrame(frame, frameType - 64);
					frame.read(dis, pool, container);
					break;
				case 247:
					frame = new SameLocalsOneStackItemFrame(frame, dis.readUnsignedShort());
					frame.read(dis, pool, container);
					break;
				case 248:
				case 249:
				case 250:
					frame = new ChopFrame(frame, 251 - frameType, dis.readUnsignedShort());
					break;
				case 251:
					frame = new SameFrame(frame, dis.readUnsignedShort());
					break;
				case 252:
				case 253:
				case 254:
					frame = new AppendFrame(frame, dis.readUnsignedShort(), frameType - 251);
					frame.read(dis, pool, container);
					break;
				case 255:
					frame = new FullFrame(frame, dis.readUnsignedShort());
					frame.read(dis, pool, container);
					break;
				default:
					throw new BT_AttributeException(getAttributeName(), "invalid frame type");
			}
			frames[i] = frame;
		}
	}
	
    static class LocalsCount {
    	int slots; /* doubles and longs count as 2 slots */
    	int count; /* doubles and longs count as 1 */
    	
    	LocalsCount() {}
    	
    	LocalsCount(BT_LocalCell types[]) {
    		init(types);
    	}
    	
    	void init(BT_LocalCell types[]) {
    		int j = 0;
    		int length = types.length;
     		while(length > 0 && types[length - 1] == null || 
     				(length > 1 && types[length - 1].getCellType().isTop() && !types[length - 2].getCellType().isTwoSlot())) {
     			length--;
     		}
    		for(int i=0; i<length; i++) {
    			BT_LocalCell type = types[i];
    			BT_StackType one = type.getCellType();
    			if(one.isTwoSlot()) {
    				j++;
    			}
    		}
    		slots = length;
    		count = length - j;
    	}
    }
    
	private static void getNumEqual(BT_LocalCell types1[], int types1Length, BT_LocalCell types2[], int types2Length, LocalsCount counter) {
		int length = Math.min(types1Length, types2Length);
		int slot = 0;
		int count = 0;
		for(; slot<length; slot++) {
			BT_LocalCell type1 = types1[slot];
			BT_LocalCell type2 = types2[slot];
			if(type1 == null) {
				if(type2 == null) {
					continue;
				}
				counter.slots = slot;
				counter.count = count;
				return;
			}
			BT_StackType one = type1.getCellType();
			if(!one.equals(type2.getCellType())) {
				counter.slots = slot;
				counter.count = count;
				return;
			}
			if(!one.isTwoSlot()) {
				count++;
			}
		}
		counter.slots = slot;
		counter.count = count;
	}

	private static BT_StackMapTableFrame getNextFrame(
			BT_BasicBlockMarkerIns mergeInstruction,
			BT_LocalCell locals[],
			LocalsCount localsCount,
			BT_StackCell stack[],
			BT_LocalCell previousLocals[],
			LocalsCount previousLocalsCount,
			BT_StackMapTableFrame previousFrame) {
		int stackLength = stack.length;
		if(stackLength <= 1 || (stackLength == 2 && stack[0].getCellType().isTwoSlot())) {
			int previousLocalsLength = previousLocalsCount.slots;
			int localsLength = localsCount.slots;
			if(localsLength == previousLocalsLength) {
				LocalsCount equalCount = new LocalsCount();
				getNumEqual(locals, localsLength, previousLocals, previousLocalsLength, equalCount);
				int numEqualLocalSlots = equalCount.slots;
				if(localsLength == numEqualLocalSlots) { /* the locals are the same */
					if(stackLength == 0) {
						return new SameFrame(previousFrame, mergeInstruction);
					} else {
						return new SameLocalsOneStackItemFrame(previousFrame, mergeInstruction, stack[0].getCellType());
					}
				}
			} else if(stackLength == 0) {
				LocalsCount equalCount = new LocalsCount();
				getNumEqual(locals, localsLength, previousLocals, previousLocalsLength, equalCount);
				int numEqualLocalSlots = equalCount.slots;
				int numEqualLocals = equalCount.count;
				return getAlteredLocalsFrame(
						mergeInstruction,
						locals,
						localsCount,
						stack,
						previousLocals,
						previousLocalsCount,
						previousFrame, 
						numEqualLocalSlots,
						numEqualLocals);
			} 
		}
		BT_StackType stackLocals[] = copyCellArrayToFrameArray(locals, localsCount.count, 0);
		BT_StackType stackTypes[] = copyCellArrayToFrameArray(stack);
		return new FullFrame(previousFrame, mergeInstruction, stackLocals, stackTypes);
	}
	
	private static BT_StackMapTableFrame getAlteredLocalsFrame(
			BT_BasicBlockMarkerIns mergeInstruction, 
			BT_LocalCell locals[], 
			LocalsCount localsCount,
			BT_StackCell stack[],
			BT_LocalCell previousLocals[],
			LocalsCount previousLocalsCount,
			BT_StackMapTableFrame previousFrame,
			int numEqualLocalSlots,
			int numEqualLocals) {
		if(previousLocalsCount.slots > numEqualLocalSlots) {
			if(localsCount.slots == numEqualLocalSlots) {
				int numChoppedLocals = previousLocalsCount.count - numEqualLocals;
				if(numChoppedLocals <= 3) {
					return new ChopFrame(previousFrame, mergeInstruction, numChoppedLocals);
				}
			}
		} else if(localsCount.slots > numEqualLocalSlots) {
			if(previousLocalsCount.slots == numEqualLocalSlots) {
				int numAppendedLocals = localsCount.count - numEqualLocals;
				if(numAppendedLocals <= 3) {
					BT_StackType types[] = copyCellArrayToFrameArray(locals, numAppendedLocals, numEqualLocalSlots);
					return new AppendFrame(previousFrame, mergeInstruction, types);
				}
			}
		}
		BT_StackType localTypes[] = copyCellArrayToFrameArray(locals, localsCount.count, 0);
		BT_StackType stackTypes[] = copyCellArrayToFrameArray(stack);
		return new FullFrame(previousFrame, mergeInstruction, localTypes, stackTypes);
	}
}
