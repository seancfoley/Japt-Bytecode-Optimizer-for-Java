package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Models the "LineNumberTable" attribute of a BT_CodeAttribute.

 <p> From the JVM documentation:
 If LineNumberTable attributes are present in the attributes table of a given Code
 attribute, then they may appear in any order. Furthermore, multiple LineNumberTable
 attributes may together represent a given line of a Java source file; that is,
 LineNumberTable attributes need not be one-to-one with source lines.
 <code><pre>
 *       LineNumberTable_attribute {
 *          u2 attribute_name_index;
 *          u4 attribute_length;
 *          u2 line_number_table_length;            // Number of entries
 *          {  u2 start_pc;
 *             u2 line_number;
 *          } line_number_table[line_number_table_length];
 *      }
 * </pre></code>

 * @author IBM
**/
public final class BT_LineNumberAttribute extends BT_Attribute {

        /**
	 The name of this attribute.
	**/
	public static final String ATTRIBUTE_NAME = "LineNumberTable";

	public String getName() {
		return ATTRIBUTE_NAME;
	}

	private PcRange[] pcRanges; // (PcRange is nested)
	
	/**
	 Constructs an attribute but defers filling in "pcRanges".
	 @param ne         The number of PcRanges to allocate initially.
	 @param container  The BT_CodeAttribute that contains this attribute.
	**/
	public BT_LineNumberAttribute(int ne, BT_CodeAttribute container) {
		this(new PcRange[ne], container);
	}
	
	public BT_LineNumberAttribute(PcRange ranges[], BT_CodeAttribute container) {
		super(container);
		pcRanges = ranges;
	}

	/**
	 @param data  The part of the attribute value following "attribute_length" from the class file.
	 @param container  The BT_CodeAttribute that contains this attribute.
	**/
	BT_LineNumberAttribute(byte data[], BT_ConstantPool pool, BT_CodeAttribute container, LoadLocation loadedFrom) throws BT_AttributeException {
		super(container, loadedFrom);
		try {
			DataInputStream dis =
				new DataInputStream(new ByteArrayInputStream(data));
			int ne = dis.readUnsignedShort(); // AKA line_number_table_length
			if (data.length != 2 + 4 * ne)
				throw new BT_AttributeException(ATTRIBUTE_NAME, 
					Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
			pcRanges = new PcRange[ne];
			for (int ie = 0; ie < pcRanges.length; ++ie) { // Per element
				pcRanges[ie] = new UndereferencedPCRange(dis.readUnsignedShort(), // AKA start_pc
				dis.readUnsignedShort()); // AKA line_number
			} // Per element
		} catch(IOException e) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, e);
		}
	}
	
	public BT_CodeAttribute getCode() {
		return (BT_CodeAttribute) getOwner();
	}
	
	void setRanges(PcRange ranges[]) {
		pcRanges = ranges;
	}
	
	PcRange[] getRanges() {
		return pcRanges;
	}
	
	private void sort() {
		getCode().computeInstructionSizes();
		Arrays.sort(pcRanges, new java.util.Comparator() {
			public int compare(Object o1, Object o2) {
				return compareRanges((PcRange) o1, (PcRange) o2);
			}
			
			public boolean equals(Object obj) {
				return this == obj;
			}
		});
	}

	// Converts class-file artifacts (counters, offsets, ...) into references to related objects.
	void dereference(BT_Repository rep) throws BT_AttributeException {
		try {
			BT_CodeAttribute inCode = getCode();
			for (int ie = 0; ie < pcRanges.length; ++ie) { // Per element
				PcRange range = pcRanges[ie];
				if(range == null || range.isDereferenced()) {
					continue;
				}
				UndereferencedPCRange pcRange = (UndereferencedPCRange) range;
				BT_Ins startIns = inCode.getInstructions().findNonBlockInstruction(inCode, this, pcRange.startPC);
				pcRanges[ie] = new DereferencedPCRange(startIns, pcRange.lineNumber);
			}
		} catch(BT_InstructionReferenceException e) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, e);
		}
	}

	private void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
		for (int ie = pcRanges.length - 1; ie >= 0 ; --ie) {
			PcRange range = pcRanges[ie];
			range.changeReferencesFromTo(oldIns, newIns);
		}
	}
	
	/**
	 * @param switchingCodeAttributes true if oldIns and newIns are not in the same code attribute
	 */
	public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switchingCodeAttributes) {
		BT_CodeAttribute inCode = getCode();
		if(switchingCodeAttributes) {
			changeReferencesFromTo(oldIns, newIns);
		} else {
			BT_InsVector instructions = inCode.getInstructions();
			int newIndex = instructions.indexOf(newIns);
			int oldIndex = instructions.indexOf(oldIns);
			if(newIndex < 0 || oldIndex < 0) {
				changeReferencesFromTo(oldIns, newIns);
				return;
			}
			sort();
			boolean madeMatchAlready = false;
			boolean ascendingOrder = (oldIndex <= newIndex);
			boolean toRemove[] = null;
			int removeCount = 0;
			for (int ie = ascendingOrder ? pcRanges.length - 1 : 0; 
					ascendingOrder ? ie >= 0 : ie < pcRanges.length ; ie += (ascendingOrder ? -1 : 1)) {
				PcRange range = pcRanges[ie];
				if(range.changeReferencesFromTo(oldIns, newIns)) {
					madeMatchAlready = true;
					//if there already exists a range for this instruction, get rid of this range,
					//keep the one that already exists or the one that has been changed already
					
					//otherwise we rearrange the order of everything to maintain sort order
					for(int k = ascendingOrder ? ie + 1 : ie - 1; 
							ascendingOrder ? k <pcRanges.length : k >= 0; k += (ascendingOrder ? 1 : -1)) {
						PcRange otherRange = pcRanges[k];
						if(!otherRange.isDereferenced()) {
							continue;
						}
						DereferencedPCRange defRange = (DereferencedPCRange) otherRange;
						int comparison = newIns.byteIndex - defRange.startIns.byteIndex;
						if(comparison == 0) {
							//both ranges point to the same byte index, so we remove the one we're changing
							if(toRemove == null) {
								toRemove = new boolean[pcRanges.length];
							}
							toRemove[ie] = true;
							removeCount++;
							break;
						} else if(ascendingOrder ? comparison < 0 : comparison > 0) {
							//in order to maintain sort order we've found the new location for the changed range
							if(ascendingOrder) {
								//we must insert range just before defRange
								for(int n = ie + 1; n<k; n++) {
									pcRanges[n - 1] = pcRanges[n];
								}
								pcRanges[k - 1] = range;
							} else {
								//we must insert range just past defRange
								for(int n=ie - 1; n > k; n--) {
									pcRanges[n + 1] = pcRanges[n];
								}
								pcRanges[k + 1] = range;
							}
							break;
						}
					}
				} else if(madeMatchAlready) {
					break;
				}
			}
			if(removeCount > 0) {
				PcRange newRange[] = new PcRange[pcRanges.length - removeCount];
				for(int i=0, num = 0; i<pcRanges.length; i++) {
					if(!toRemove[i]) {
						newRange[num++] = pcRanges[i];
					}
				}
				pcRanges = newRange;
			}
		}
	}
	

	/**
	 Returns the number of bytes that write(...) will write.
	**/
	protected int writtenLength() {
		return 2 // attribute_name_index
		+4 // attribute_length
		+2 // number_of_classes
		+ eliminateOverflow().length * (2 + 2); // start_pc + line_number
	}

	// This must be kept in synch with {@link BT_LineNumberAttribute#writtenLength()}.
	void write(java.io.DataOutputStream dos, BT_ConstantPool pool)
		throws IOException {
		// eliminate pcranges that are out of range for short values
		PcRange newRanges[] = eliminateOverflow();
		dos.writeShort(pool.indexOfUtf8(ATTRIBUTE_NAME));
		// attribute_name_index
		dos.writeInt(2 + 4 * newRanges.length); // attribute_length
		dos.writeShort(newRanges.length); // number_of_classes
		for (int ie = 0; ie < newRanges.length; ++ie) { // Per element
			dos.writeShort(newRanges[ie].getStartIndex());
			dos.writeShort(newRanges[ie].lineNumber); // AKA line_number
		} // Per element
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_size_{1}_4", new Object[] {ATTRIBUTE_NAME, Integer.toString(pcRanges.length)});
	}

	public boolean singletonRequired() {
		return false;
	}

	public Object clone() {
		BT_LineNumberAttribute att = (BT_LineNumberAttribute) super.clone();
		att.pcRanges = (BT_LineNumberAttribute.PcRange[]) pcRanges.clone();
		for(int i=0; i<pcRanges.length; i++) {
			att.pcRanges[i] = (BT_LineNumberAttribute.PcRange) att.pcRanges[i].clone();
		}
		return att;
	}
	
	public void resolve(BT_ConstantPool pool) throws BT_AttributeException, BT_ClassWriteException {
		super.resolve(pool);
	}

	/**
	 * 
	 */
	private PcRange[] eliminateOverflow() {
		BT_CodeAttribute inCode = getCode();
		if(BT_Misc.overflowsUnsignedShort(inCode.getInstructions().size())) {
			for(int i=0; i<pcRanges.length; i++) {
				if(BT_Misc.overflowsUnsignedShort(pcRanges[i].getStartIndex())) {
					boolean isBadRange[] = new boolean[pcRanges.length];
					isBadRange[i] = true;
					int goodCount = i;
					for(int k = i + 1; k<pcRanges.length; k++) {
						if(BT_Misc.overflowsUnsignedShort(pcRanges[k].getStartIndex())) {
							isBadRange[k] = true;
						} else {
							goodCount++;
						}
					}
					PcRange[] newRanges = new PcRange[goodCount];
					int index = 0;
					for(int k=0; k<pcRanges.length; k++) {
						PcRange range = pcRanges[k];
						if(!isBadRange[k]) {
							newRanges[index++] = range;
						}
					}
					return newRanges;
				}
			}
		}
		return pcRanges;
	}
	
	/**
	 * returns the line number for the given instruction as indicated by this attribute.
	 * Note that a given code attribute might have several LineNumberTable attributes so
	 * all such attributes should be queried simultaneously.
	 * @param ins
	 * @return the line number in the source file for the given instruction.
	 */
	PcRange getEncompassingRange(BT_Ins ins) {
		sort();
		BT_CodeAttribute inCode = getCode();
		int instructionIndex = inCode.findInstruction(ins);
		if(instructionIndex == -1) {
			return null;
		}
		BT_LineNumberAttribute.PcRange[] ranges = pcRanges;
	    if (ranges.length == 0) {
	    	return null;
	    } 
    	/*
    	 * Each range defines a byte index at which a new line in the source file begins, and this
    	 * line is given by the line field in the range.
    	 * 
    	 *  So we go through the loop until: range1 index < given instruction index < range2 index
    	 *  at which point we know range1 is the correct range.
    	 */
    	int startInstructionIndex = 0;
    	int j;
    	for (j = 0; j < ranges.length; j++) {
    		if(ranges[j].isDereferenced()) {
	    		BT_Ins startIns = ((DereferencedPCRange) ranges[j]).startIns;
	    		startInstructionIndex = inCode.findInstruction(startIns, startInstructionIndex);
	    		if (instructionIndex < startInstructionIndex) {
	    			if (j == 0)
	    				return null;
	    			else
	    				return ranges[j - 1];
	    		}
    		} else {
    			int startPC = ((UndereferencedPCRange) ranges[j]).startPC;
    			if (ins.byteIndex < startPC) {
	    			if (j == 0)
	    				return null;
	    			else
	    				return ranges[j - 1];
	    		}
    		}
    	}
    	return ranges[j - 1];
	    
	}
	
	/**
	 * 
	 * @param range1
	 * @param range2
	 * @return -1, 0, or 1 depending upon whether range1 refers to an instruction that lies before, is the same, 
	 * or lies after the instruction referred to by range2.
	 */
	int compareRanges(PcRange range1, PcRange range2) {
		int result = range1.getStartIndex() - range2.getStartIndex();
		if(result == 0) {
			//the only way that two ranges could be equal is that
			//they both refer to the same instruction or that at least one
			//of the two refers to a 0-length block marker instruction.
			if(!range1.isDereferenced() || !range2.isDereferenced()) {
				return range1.lineNumber - range2.lineNumber;
			}
			DereferencedPCRange r1 = (DereferencedPCRange) range1;
			DereferencedPCRange r2 = (DereferencedPCRange) range2;
			
			
			BT_Ins start1 = r1.startIns;
			BT_Ins start2 = r2.startIns;
			if(start1.isBlockMarker()) {
				if(start2.isBlockMarker()) {
					BT_CodeAttribute code = getCode();
					return code.findInstruction(start1) - code.findInstruction(start2);
				}
				return -1;
			}
			if(start2.isBlockMarker()) {
				return 1;
			}
			//neither is a block marker, both must be the same instruction, go by line number
			return range1.lineNumber - range2.lineNumber;
		}
		return result;
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 Represents a range of program-counters that have the same line number.
	 The start of the range is given by "startPC" or "startIns".
	 The end of the range is inferred by the existence of another
	 "PcRange".
	**/
	public static abstract class PcRange implements Cloneable { // Nested

		
		/**
		 The line number in the source file.
		**/
		public final int lineNumber;

		public PcRange(int ln) {
			lineNumber = ln;
		}

		abstract int getStartIndex();
		
		abstract boolean changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns);
		
		public abstract String toString();

		
		public Object clone() {
			try {
				return super.clone();
			} catch(CloneNotSupportedException e) {
				return null;
			}
		}
		
		abstract boolean isDereferenced();
		
	} // PcRange
	
	
	
	public static class DereferencedPCRange extends PcRange {
		/**
		 The first instruction with this line number.
		**/
		public BT_Ins startIns;
		
		/**
		 Creates a PcRange using the starting instruction.
		 This public constructor is used when adding PcRanges
		 to a method that has already been "dereferenced".
		 @param si  The instruction.
		 @param ln  The lineNumber.
		**/
		public DereferencedPCRange(BT_Ins si, int ln) {
			super(ln);
			startIns = si;
			
		}
		
		int getStartIndex() {
			return startIns.byteIndex;
		}
		
		/**
		 @see BT_CodeAttribute#changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns)
		**/
		boolean changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
			if (startIns == oldIns) {
				startIns = newIns;
				return true;
			}
			return false;
		}

		public String toString() {
			return Messages.getString("JikesBT.PR(line__{0}_startI__{1})_4", new Object[] {Integer.toString(lineNumber), startIns});
		}
		
		boolean isDereferenced() {
			return true;
		}
	}
	
	public static class UndereferencedPCRange extends PcRange {
		public int startPC;
	
		/**
		 Creates a PcRange using the starting program-counter.
		 This non-public constructor is typically used only while
		 reading byte-codes (before "dereference" is run).
		 @param sp  The "startPC".
		 @param ln  The lineNumber.
		**/
		UndereferencedPCRange(int sp, int ln) {
			super(ln);
			startPC = sp;
		}
		
		int getStartIndex() {
			return startPC;
		}
		
		/**
		 @see BT_CodeAttribute#changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns)
		**/
		boolean changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
			return false;
		}

		public String toString() {
			return Messages.getString("JikesBT.PR(line__{0}_startI__{1})_4", new Object[] {Integer.toString(lineNumber), Integer.toString(startPC)});
		}
		
		boolean isDereferenced() {
			return false;
		}
	}
} 
