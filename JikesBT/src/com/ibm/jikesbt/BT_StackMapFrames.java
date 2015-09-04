/*
 * Created on Jul 23, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;


/**
 * @author Sean Foley
 *
 */
public abstract class BT_StackMapFrames implements Cloneable {
	protected BT_StackMapFrame frames[];/* will be null if hasJSR is true, otherwise will be an array of frames (possibly zero length) */
	int maxStack;
	int maxLocals;
	boolean hasJSR;
	protected static BT_StackType emptyArray[] = new BT_StackType[0];
	protected static BT_StackMapFrame emptyFrames[] = new BT_StackMapFrame[0];
	
	BT_StackMapFrames(boolean hasJSR, int maxStack, int maxLocals) {
		this.hasJSR = hasJSR;
		this.maxStack = maxStack;
		this.maxLocals = maxLocals;
	}
	
	BT_StackMapFrames() {
		this(false, -1, -1);
	}
    
	public int getFrameCount() {
		if(hasJSR) {
			return 0;
		}
		return frames.length;
	}
	
	abstract String getAttributeName();
	
	public void print(PrintStream stream, BT_CodeAttribute container) throws BT_AttributeException {
    	BT_StackShapes allShapes = getShapes(container);
    	allShapes.print(stream);
    	allShapes.returnStacks();
    }
    
	abstract public BT_StackShapes getShapes(BT_CodeAttribute container) throws BT_AttributeException;
    
    abstract void read(DataInputStream dis, BT_ConstantPool pool, BT_CodeAttribute container) 
		throws IOException, BT_AttributeException, BT_ConstantPoolException, BT_DescriptorException;

    private Object superClone() {
		try {
    		return super.clone();
		} catch(CloneNotSupportedException e) {
			return null;
		}
    }
    
    public Object clone() {
    	BT_StackMapFrames ret = (BT_StackMapFrames) superClone();
    	if(hasJSR) {
    		return ret;
    	}
		ret.frames = (BT_StackMapFrame []) frames.clone();
		for(int i=0; i<frames.length; i++) {
			ret.frames[i] = (BT_StackMapFrame) frames[i].clone();
		}
    	return ret;
    }
    
    public String toString() {
    	String none = "none\n";
    	if(hasJSR) {
    		return none;
    	}
    	StringBuffer result = new StringBuffer();
    	int i=0;
    	for(; i<frames.length; i++) {
    		result.append('[');
    		result.append(frames[i].toString());
        }
    	if(i == 0) {
    		result.append(none);
    	}
    	return result.toString();
    }
    
	int getLength() {
		if(hasJSR) {
    		return 0;
    	}
		int result = 2;
		for(int i=0; i<frames.length; i++) {
			result += frames[i].getLength();
		}
		return result;
	}
	
	void dereference(BT_Repository rep, BT_StackMapAttribute att) throws BT_InstructionReferenceException {
		if(hasJSR) {
    		return;
    	}
		for (int i = 0; i < frames.length; ++i) { // Per element
			BT_StackMapFrame current = frames[i];
			current.dereference(rep, att);
		}
	}
	
	public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
		if(hasJSR) {
    		return;
    	}
		for (int i = 0; i < frames.length; ++i) { // Per element
			BT_StackMapFrame current = frames[i];
			current.changeReferencesFromTo(oldIns, newIns);
		}
	}
	
	void write(DataOutputStream dos, BT_ConstantPool pool)
		throws IOException {
		if(hasJSR) {
    		return;
    	}
		dos.writeShort(frames.length); // number of frames
		for (int i = 0; i < frames.length; ++i) {
			frames[i].write(dos, pool);
		}
	}
	
	public void resolve(BT_ConstantPool pool) {
		if(hasJSR) {
    		return;
    	}
		for (int i = 0; i < frames.length; ++i) {
			frames[i].resolve(pool);
		}
	}
	
	static BT_LocalCell[] copyFrameArrayToCellArray(BT_LocalCell unchangedTypes[], BT_LocalCell source[], BT_StackPool pool) {
		if(unchangedTypes == null) {
			unchangedTypes = emptyArray;
		}
		int extra = 0;
		for(int i=0; i<source.length; i++) {
			if(source[i].getCellType().isTwoSlot()) {
				extra++;
			}
		}
		if(extra > 0) {
			BT_LocalCell result[] = pool.getLocals(unchangedTypes.length + source.length + extra);
			for(int i=0; i<unchangedTypes.length; i++) {
				BT_LocalCell localCell = unchangedTypes[i];
				result[i] = localCell;
			}
			
			extra = 0;
			for(int i=0; i<source.length; i++) {
				BT_LocalCell localCell = source[i];
				result[unchangedTypes.length + i + extra] = localCell;
				if(localCell.getCellType().isTwoSlot()) {
					extra++;
					result[unchangedTypes.length + i + 1] = BT_StackType.TOP;
				}
			}
			return result;
		}
		return pool.getDuplicate(unchangedTypes, source);
	}
	
	static BT_StackCell[] copyFrameArrayToCellArray(BT_StackCell source[], BT_StackPool pool) {
		int extra = 0;
		for(int i=0; i<source.length; i++) {
			if(source[i].getCellType().isTwoSlot()) {
				extra++;
			}
		}
		if(extra > 0) {
			BT_StackCell result[] = pool.getStack(source.length + extra);
			extra = 0;
			for(int i=0; i<source.length; i++) {
				BT_StackCell cell = source[i];
				result[i + extra] = cell;
				if(cell.getCellType().isTwoSlot()) {
					extra++;
					result[i + 1] = BT_StackType.TOP;
				}
			}
			return result;
		}
		return pool.getDuplicate(source);
	}
	
	static BT_StackType[] copyCellArrayToFrameArray(BT_LocalCell oldTypes[], int localsCount, int fromIndex) {
		if(localsCount == 0) {
			return emptyArray;
		}
		BT_StackType newTypes[] = new BT_StackType[localsCount];
		for(int i=0, k=fromIndex; i<newTypes.length; i++, k++) {
			BT_StackType type = oldTypes[k].getCellType();
			newTypes[i] = type;
			if(type.isTwoSlot()) {
				k++;
			}
		}
		return newTypes;
	}
	
	static BT_StackType[] copyCellArrayToFrameArray(BT_StackCell oldTypes[]) {
		if(oldTypes.length == 0) {
			return emptyArray;
		}
		int j=0;
		for(int i=0; i<oldTypes.length; i++) {
			if(oldTypes[i].getCellType().isTwoSlot()) {
				j++;
			}
		}
		BT_StackType newTypes[] = new BT_StackType[oldTypes.length - j];
		for(int i=0, k=0; i<newTypes.length; i++, k++) {
			BT_StackType type = oldTypes[k].getCellType();
			newTypes[i] = type;
			if(type.isTwoSlot()) {
				k++;
			}
		}
		return newTypes;
	}
}
