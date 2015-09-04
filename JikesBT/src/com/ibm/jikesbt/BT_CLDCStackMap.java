package com.ibm.jikesbt;

import java.io.DataInputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_StackMapTable.LocalsCount;
import com.ibm.jikesbt.BT_StackShapeVisitor.StackShapes;



/**
 * @author Sean Foley
 *
 * Represents the CLDC stack map table
 */
public class BT_CLDCStackMap extends BT_StackMapFrames {

	BT_CLDCStackMap() {}
	
	public BT_CLDCStackMap(boolean hasJSR) {
		super(hasJSR, -1, -1);
		if(hasJSR) {
    		return;
    	}
		this.frames = emptyFrames;
	}
	
	public BT_CLDCStackMap(BT_StackShapes shapes, boolean isFrame[], int totalFrames, BT_CodeAttribute inCode) {
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
	
    private BT_CLDCStackMap(
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
    	int codeLen = stackShapes.length;
    	frames = new BT_CLDCStackMapFrame[totalFrames];
    	int frameNum = 0;
    	BT_InsVector instructions = container.getInstructions();
    	for(int i=0; i<codeLen; i++) {
    		if(isFrame[i]) {
    			BT_StackCell stack[] = stackShapes[i];
    			BT_LocalCell locals[] = localShapes[i];
    			BT_BasicBlockMarkerIns mergeIns = (BT_BasicBlockMarkerIns) instructions.elementAt(i);
    			frames[frameNum] = getNextFrame(mergeIns, locals, stack);
    			frameNum++;
    		}
    	}
    }
    
    private static BT_CLDCStackMapFrame getNextFrame(
			BT_BasicBlockMarkerIns mergeInstruction, 
			BT_LocalCell locals[], 
			BT_StackCell stack[]) {
    	LocalsCount localsCount = new LocalsCount(locals);
		BT_StackType stackLocals[] = copyCellArrayToFrameArray(locals, localsCount.count, 0);
		BT_StackType stackTypes[] = copyCellArrayToFrameArray(stack);
		return new BT_CLDCStackMapFrame(mergeInstruction, stackLocals, stackTypes);
	}
    
    String getAttributeName() {
    	return BT_StackMapAttribute.CLDC_STACKMAP_NAME;
    }
    
	public BT_StackShapes getShapes(BT_CodeAttribute container)
			throws BT_AttributeException {
		BT_InsVector ins = container.getInstructions();
    	BT_StackCellProvider provider = new BT_StackCellProvider(container.getMethod().cls.getRepository());
    	BT_StackPool pool = BT_StackPool.pool;
    	BT_StackShapes allShapes = new BT_StackShapes(container, pool, provider);
    	allShapes.createInitialStacks();
    	if(!hasJSR) {
	    	StackShapes newShapes = new StackShapes();
	    	//assign the stack maps to the appropriate instructions
	    	for (int i = 0, index = -1; i < frames.length; ++i) {
				BT_CLDCStackMapFrame current = (BT_CLDCStackMapFrame) frames[i];
				BT_BasicBlockMarkerIns instruction = current.instruction;
				if(instruction.opcode == BT_StackMapFrame.OFFSET_INDICATOR_OPCODE) {
					//this will happen if not dereferenced yet
					throw new BT_AttributeException(getAttributeName(), "stack map has not been dereferenced yet");
				}
				index = ins.indexOf(instruction);
				if(index < 0) {
					//should never reach here, because changing the instruction vector should automatically update the stack maps
					throw new BT_AttributeException(getAttributeName(), "stack map references an inexistent instruction");
				}
				current.getStack(newShapes, pool);
				allShapes.stackShapes[index] = newShapes.newStack;
				allShapes.localShapes[index] = newShapes.newLocals;
				if(index == 0) {
					allShapes.mergedInitial = true;
				}
			}
    	}
    	return allShapes;
	}

	void read(DataInputStream dis, BT_ConstantPool pool,
			BT_CodeAttribute container) throws IOException,
			BT_AttributeException, BT_ConstantPoolException,
			BT_DescriptorException {
		int numFrames = dis.readUnsignedShort();
		frames = new BT_CLDCStackMapFrame[numFrames];
		for(int i=0; i<numFrames; i++) {
			BT_CLDCStackMapFrame frame = new BT_CLDCStackMapFrame(dis.readUnsignedShort());
			frame.read(dis, pool, container);
			frames[i] = frame;
		}
	}
}
