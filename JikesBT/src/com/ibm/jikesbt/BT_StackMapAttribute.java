/*
 * Created on Jul 22, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_CodeAttribute.CodeInfo;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 * @author Sean Foley
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BT_StackMapAttribute extends BT_Attribute implements CodeInfo {

	public static final String STACK_MAP_TABLE_ATTRIBUTE_NAME = "StackMapTable";
	public static final String CLDC_STACKMAP_NAME = "StackMap";
	
	public BT_StackMapFrames stackMaps;
	int tempMaxLocals = -1;
	int tempMaxStack = -1;
	public final String name;
	
	BT_StackMapAttribute(BT_CodeAttribute code, String name) {
		super(code);
		this.name = name;
		if(!isStackMapTable() && !name.equals(CLDC_STACKMAP_NAME)) {
			throw new IllegalArgumentException();
		}
	}
	
	BT_StackMapAttribute(
			byte data[], 
			BT_ConstantPool pool, 
			BT_CodeAttribute container,
			String name,
			LoadLocation loadedFrom) throws BT_AttributeException {
		super(container, loadedFrom);
		this.name = name;
		try {
			DataInputStream dis =
				new DataInputStream(new ByteArrayInputStream(data));
			if(isStackMapTable()) {
				stackMaps = new BT_StackMapTable();
			} else if(name.equals(CLDC_STACKMAP_NAME)) {
				stackMaps = new BT_CLDCStackMap();
			} else {
				throw new IllegalArgumentException();
			}
			
			try {
				//note that we must ensure that after creating the stack maps we initialize the
				//fields maxLocals and maxStack
				stackMaps.read(dis, pool, container);
			} catch(BT_DescriptorException e) {
				throw new BT_AttributeException(name, e);
			} catch(BT_ConstantPoolException e) {
				throw new BT_AttributeException(name, e);
			}
		} catch(IOException e) {
			throw new BT_AttributeException(name, e);
		}
	} 
	
	boolean isStackMapTable() {
		return name.equals(STACK_MAP_TABLE_ATTRIBUTE_NAME);
	}
	
	public BT_CodeAttribute getCode() {
		return (BT_CodeAttribute) getOwner();
	}
	
	void setMaxes(int maxLocals, int maxStack) {
		//BT_StackMapFrames stackMaps = att.stackMaps;
		if(stackMaps != null) {
			stackMaps.maxStack = maxStack;
			stackMaps.maxLocals = maxLocals;
		} else {
			tempMaxLocals = maxLocals;
			tempMaxStack = maxStack;
		}
	}
	
	void dereference(BT_Repository rep) throws BT_AttributeException {
		if(stackMaps == null) {
//			BT_CodeAttribute owner = getCode();
//			tempMaxLocals = owner.preDeref.maxStack;
//			tempMaxStack = owner.preDeref.maxLocals;
			return;
		}
		try {
			stackMaps.dereference(rep, this);
		} catch(BT_InstructionReferenceException e) {
			throw new BT_AttributeException(name, e);
		}
	}
	
	public void printFrames(PrintStream stream) throws BT_AttributeException {
		if(stackMaps == null) {
			return;
		}
		stackMaps.print(stream, getCode());
	}
	
	/*
	 * you might think that this method should do nothing since changing
	 * instructions could very well invalidate the maps.  However, this method
	 * is indeed needed for cloning the attribute, in which case the replacement
	 * is safe. 
	 * 
	 * In the case where instructions are added and removed from a single code
	 * vector, then the reset() method in this class will be called to invalidate
	 * the current stack maps.
	 */
	/**
	 * @param switchingCodeAttributes true if oldIns and newIns are not in the same code attribute
	 */
	public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switchingCodeAttributes) {
		if(stackMaps == null) {
			return;
		}
		stackMaps.changeReferencesFromTo(oldIns, newIns);
	}
	
	public void reset() {
		tempMaxLocals = -1;
		tempMaxStack = -1;
		stackMaps = null;
	}
	
	public void resetMaxLocals() {
		tempMaxLocals = -1;
		if(stackMaps != null) {
			stackMaps.maxLocals = -1;
		}
	}
	
	private static void translateLocals(BT_LocalCell locals[]) {
		if(locals == null) {
			return;
		}
		int i = locals.length;
		while(i > 0 && locals[i - 1] == null) {
			i--;
		}
		
		for(int j=0; j<i-1; j++) {
			if(locals[j] == null) {
				locals[j] = BT_StackType.TOP;
			}
		}
	}
	
	public boolean isPopulated() {
		return stackMaps != null;
	}
	
	public void populate() throws BT_CodeException {
		if(stackMaps == null) {
			
			BT_CodeAttribute inCode = getCode();
			BT_InsVector ins = inCode.getInstructions();
			if(ins.size() == 0) {
				return;
			}
			boolean hasMerges = false;
			
			//ensure that all goto and others are followed by basic blocks
			//we do this before we construct the shapes so that we need not add any instructions later
			//At the same time, we determine if we have any frames in the stack maps at all
			int i=0;
			
	    	int size;
			while(i < (size = ins.size())) {
				BT_Ins instruction = ins.elementAt(i);
				i++;
				boolean isSwitch = instruction.isSwitchIns();
				if((instruction.isGoToIns() || isSwitch || instruction.hasNoSuccessor()) && (i < size)) {
					BT_Ins nextInstruction = ins.elementAt(i);
					if(!nextInstruction.isBlockMarker()) {
						ins.insertElementAt(nextInstruction = BT_Ins.make(), i);
					}
					i++;
					hasMerges = true;
				} else if(instruction.isJumpIns()) {
					if(instruction.isJSRIns()) {
						constructEmptyMaps(true);
						return;
					}
					hasMerges = true;
				} else if(isSwitch) {
					hasMerges = true;
				}
			}
	    	if(!hasMerges && inCode.getExceptionTableEntryCount() == 0) {
	    		constructEmptyMaps(false);
	    		return;
	    	}
	    	
	    	//now create the stack and local shapes
			BT_StackShapeVisitor shapeVisitor = new BT_StackShapeVisitor(inCode, BT_StackPool.pool);
			shapeVisitor.ignoreUpcasts(false);
			shapeVisitor.useMergeCandidates(true);
			BT_StackShapes shapes = shapeVisitor.populate();
			if(shapes == null) {
				return;
			}
			
			//ensure there are shapes for every instruction by removing dead code
			shapes.removeDeadCode();
			
			//don't create invalid stack maps
			//theoretically we could create stack maps for a method that would not verify
			//however, in particular, here we want to notify users that if classes failed to load then
			//stack maps might be faulty, which verifyStacks will tell us
			shapes.tolerateStubs(false);
			shapes.verifyStacks();
			
			//now find those additional block markers whose stack and local shapes will become stack frames
			int codeLength = ins.size();
	    	boolean startsBlock[] = new boolean[codeLength];
	    	i=0;
	    	while(i<codeLength) {
				BT_Ins instruction = ins.elementAt(i);
				i++;
				boolean doesNotReachNextInstruction = 
					instruction.isGoToIns() || 
					instruction.isSwitchIns() || 
					instruction.hasNoSuccessor();
				if(doesNotReachNextInstruction) {
					if(i < codeLength) {
						/* 
						 * we know there is a BasicBlockMarkerIns at index i since 
						 * we have inserted one earlier in this method, if necessary
						 */
						startsBlock[i] = true;
						i++;
					}
					
					/*
					 * 
					 * Note:
					 * We know that any jump target j will be a 
					 * merge instruction or a start block instruction.  
					 * If j is the first instruction, 
					 * then it is a merge instruction automatically.  
					 * Otherwise, consider the instruction p that precedes j.
					 * 
					 * For this instruction p, if doesNotReachNextInstruction is false,
					 * then it merges with j (because we also know p is reachable).
					 * If doesNotReachNextInstruction is true, 
					 * then j will be marked as a start block instruction.  
					 */
				}
			}
			
	    	int codeLen = shapes.stackShapes.length;
	    	
			//In the stack shape visitor, unknown types are represented as null, and in the stack maps they are top
			translateLocals(shapes.initialLocals);
			for(i=0; i<codeLen; i++) {
				translateLocals(shapes.localShapes[i]);
			}
			
			//It is illegal to have code after an unconditional branch without a stack frame map being provided for it
    		//this includes anything that follows a throw or return (see BT_Ins.hasNoSuccessor) and goto or switch.
    		//Also, any exception handler starts with a frame, even if just a single instruction targets the handler
    		
			boolean isFrame[] = new boolean[codeLen];
			int totalFrames = getFrameIndices(shapes.isMergeInstruction, startsBlock, codeLen, inCode, isFrame);	
			if(isStackMapTable()) {
				stackMaps = new BT_StackMapTable(shapes, isFrame, totalFrames, inCode);
			} else {
				stackMaps = new BT_CLDCStackMap(shapes, isFrame, totalFrames, inCode);
			}
			shapes.returnStacks();
		}
	}

	private void constructEmptyMaps(boolean hasJSR) {
		if(isStackMapTable()) {
			stackMaps = new BT_StackMapTable(hasJSR);
		} else {
			stackMaps = new BT_CLDCStackMap(hasJSR);
		}
	}
	
	private static int getFrameIndices(boolean isMergeInstruction[], boolean startsBlock[], int codeLen, BT_CodeAttribute container, boolean isFrame[]) {
    	int totalFrames = 0;
    	int lastFrameIndex = -1;
    	BT_InsVector inst = container.getInstructions();
    	findFrames:
    	for(int i=0; i<codeLen; i++) {
    		if(isMergeInstruction[i] || startsBlock[i]) {
    			
    			if(lastFrameIndex < 0) {
    				totalFrames++;
    				lastFrameIndex = i;
    				isFrame[i] = true;
	    			continue;
    			}
    			
				//check for consecutive frames with no intervening instructions
				for(int j = lastFrameIndex + 1; j<i; j++) {
					BT_Ins follower = inst.elementAt(j);
					if(!follower.isBlockMarker()) {
						totalFrames++;
						lastFrameIndex = i;
						isFrame[i] = true;
		    			continue findFrames;
					}
				}
    		}
    	}
    	return totalFrames;
    }
	
	public int getMaxLocalsQuickly() throws BT_CodeException {
		if(stackMaps == null) {
			if(tempMaxLocals >= 0) {
				return tempMaxLocals;
			}
			BT_CodeAttribute inCode = getCode();
			return tempMaxLocals = inCode.computeMaxLocals();
		}
		return getMaxLocalsFromStackMaps();
	}
	
	public int getMaxLocals() throws BT_CodeException {
		populate();
		if(stackMaps == null) {
			//to implement the CodeInfo interface, at this point we simply need to return the maxLocals,
			//the fact that the stack maps cannot be created will be handled later
			return getMaxLocalsQuickly();
		}
		return getMaxLocalsFromStackMaps();
	}
	
	private int getMaxLocalsFromStackMaps() throws BT_CodeException {
		if(stackMaps.maxLocals < 0) {
			BT_CodeAttribute inCode = getCode();
			stackMaps.maxLocals = inCode.computeMaxLocals();
		}
		return stackMaps.maxLocals;
	}
	
	
	public int getMaxStackQuickly() throws BT_CodeException {
		if(stackMaps == null) {
			if(tempMaxStack >= 0) {
				return tempMaxStack;
			}
			BT_CodeAttribute inCode = getCode();
			return tempMaxStack = inCode.computeMaxStackDepth();
		}
		return getMaxStackFromStackMaps();
	}
	
	public int getMaxStack() throws BT_CodeException {
		populate();
		if(stackMaps == null) {
			//to implement the CodeInfo interface, at this point we simply need to return the maxStack,
			//the fact that the stack maps cannot be created will be handled later
			return getMaxStackQuickly();
		}
		if(stackMaps.maxStack < 0) {
			BT_CodeAttribute inCode = getCode();
			stackMaps.maxStack = inCode.computeMaxStackDepth();
		}
		return stackMaps.maxStack;
	}
	
	private int getMaxStackFromStackMaps() throws BT_CodeException {
		if(stackMaps.maxStack < 0) {
			BT_CodeAttribute inCode = getCode();
			stackMaps.maxStack = inCode.computeMaxStackDepth();
		}
		return stackMaps.maxStack;
	}
	
	
	public void resolve(BT_ConstantPool pool) throws BT_AttributeException {
		if(stackMaps == null) {
			populate();
			if(stackMaps == null) {
				throw new BT_AttributeException(name, "cannot create stack maps for method " 
						+ getCode().getMethod().useName());
			}
		}
		if(stackMaps.hasJSR) {
			BT_CodeAttribute inCode = getCode();
			if(inCode.getMethod() != null) {
				throw new BT_AttributeException(name, "cannot create stack maps for method " 
						+ inCode.getMethod().useName() + " with a subroutine");
			} else {
				throw new BT_AttributeException(name, "cannot create stack maps for a method with a subroutine");
			}
		}
		if(isEmpty()) {
			return;
		}
		pool.indexOfUtf8(getName());
		stackMaps.resolve(pool);
	}
	
	/* (non-Javadoc)
     * @see com.ibm.jikesbt.BT_Attribute#write(java.io.DataOutputStream, com.ibm.jikesbt.BT_ConstantPool)
     */
    void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
    	if(stackMaps == null || stackMaps.hasJSR) {
    		throw new IllegalStateException("must call resolve first");
    	}
    	if(isEmpty()) {
			return;
		}
    	dos.writeShort(pool.indexOfUtf8(getName()));// attribute_name_index
		dos.writeInt(stackMaps.getLength()); // attribute_length
		stackMaps.write(dos, pool);
    }

	int writtenLength() {//the total length
		if(stackMaps == null) {
			throw new IllegalStateException("must call resolve first");
		}
		if(isEmpty()) {
			return 0;
		}
		return stackMaps.getLength() + 6;
	}
	
	private boolean isEmpty() {
		return stackMaps.getFrameCount() == 0;
	}
	
	public Object clone() {
		/* don't bother populating first, since you might assume a clone might
		 * later become different from the original
		 */
		BT_StackMapAttribute ret = (BT_StackMapAttribute) super.clone();
    	if(stackMaps != null) {
    		ret.stackMaps = (BT_StackMapFrames) stackMaps.clone();
    	}
		return ret;
    }
	
    /* (non-Javadoc)
     * @see com.ibm.jikesbt.BT_Attribute#getName()
     */
    public String getName() {
        return name;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	int length = 0;
    	if(stackMaps != null) {
			length = writtenLength();
		}
        return Messages.getString("JikesBT.{0}_<{1}_bytes>", 
        	new Object[] {getName(), Integer.toString(length)});
    }
    
    public void print(java.io.PrintStream ps, String prefix) {
    	super.print(ps, prefix);
		if(stackMaps == null) {
    		return;
		}
		try {
			printFrames(ps);
		} catch(BT_AttributeException e) {
			//TODO
//			BT_Method m = getCode().getMethod();
//			if(m != null) {
//				BT_Class clazz = m.getDeclaringClass();
//				if(clazz != null) {
//					clazz.getRepository().factory.noteCodeException(m, this, e);
//				}
//			}
		}
	}
    
}
