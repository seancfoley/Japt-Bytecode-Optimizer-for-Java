package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;

/**
 BT_InsVector is a variable size contiguous indexable array of {@link BT_Ins}s.
 The size of the BT_InsVector is the number of BT_Inss it contains.
 The capacity of the BT_InsVector is the number of BT_Inss it can hold.

 <p> BT_Inss may be inserted at any position up to the size of the
 BT_InsVector, increasing the size of the BT_InsVector. BT_Inss at any
 position in the BT_InsVector may be removed, shrinking the size of
 the BT_InsVector. BT_Inss at any position in the BT_InsVector may be replaced,
 which does not affect the BT_InsVector size.

 <p> The capacity of a BT_InsVector may be specified when the BT_InsVector is
 created. If the capacity of the BT_InsVector is exceeded, the capacity
 is increased, doubling by default.

 <p> The following public members are in addition to the usual Vector methods:
 <sl>
 <li> {@link BT_InsVector#findBasicBlock}
 <li> {@link BT_InsVector#findInstruction}
 <li> {@link BT_InsVector#setAllByteIndexes}
 </sl>
 * @author IBM
**/

public final class BT_InsVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_Ins[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_Ins[] emptyData = new BT_Ins[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 * Constructs a new BT_InsVector using the default capacity.
	 *
	 */
	public BT_InsVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_InsVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_InsVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_InsVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_InsVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_InsVector is full
	 */
	public BT_InsVector(int capacity, int capacityIncrement) {
		elementData = (capacity == 0) ? emptyData : new BT_Ins[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	
	
	/**
	 * Constructs an instruction vector with the specified number of instructions
	 * from the array instrs.
	 */
	public BT_InsVector(BT_Ins instrs[]) {
		this.elementData = (BT_Ins[]) instrs.clone();
		this.elementCount = instrs.length;
	}
	
	
	/**
	 * Adds the specified object at the end of this BT_InsVector.
	 *
	 *
	 * @param           object  the object to add to the BT_InsVector
	 */
	public void addElement(BT_Ins object) {
		insertElementAt(object, elementCount);
	}
	
	/**
	 Adds the specified object at the end of this AccessorMethodVector,
	 unless the vector already contains it.
	 @return  true if it was added.
	**/
	public boolean addUnique(BT_Ins object) {
		if (contains(object)) {
			return false;
		}
		addElement(object);
		return true;
	}
	
	private int expandFor(int count) {
		int newElementCount = elementCount + count;
		if(newElementCount >= elementData.length) {
			int newCapacity = elementCount;
			int increment = capacityIncrement == 0 ? elementCount : capacityIncrement;
			if(increment == 0) {
				increment = 1;
			}
			do {
				newCapacity += increment;
			} while(newCapacity < newElementCount);
			grow(newCapacity);
		}
		return newElementCount;
	}
	
	public void addAllUnique(BT_InsVector other) {
		expandFor(other.elementCount);
		for(int i=0; i<other.size(); i++) {
			addUnique(other.elementAt(i));
		}
	}
	
	public void addAllUnique(BT_Ins other[]) {
		expandFor(other.length);
		for(int i=0; i<other.length; i++) {
			addUnique(other[i]);
		}
	}
	
	public void addAll(BT_InsVector other) {
		int newElementCount = expandFor(other.elementCount);
		System.arraycopy(other.elementData, 0, elementData, elementCount, other.elementCount);
		elementCount = newElementCount;
	}
	
	/**
	 * Answers the number of elements this BT_InsVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_InsVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	
	/**
	 * Answers a new BT_InsVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_InsVector.
	 *
	 *
	 * @return          a shallow copy of this BT_InsVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_InsVector vector = (BT_InsVector) super.clone();
			vector.elementData = (BT_Ins[]) elementData.clone();
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_InsVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_InsVector
	 * @return          true if object is an element of this BT_InsVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(BT_Ins object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this BT_InsVector into the specified BT_Ins array.
	 *
	 *
	 * @param           elements        the BT_Ins array into which the elements
	 *                                                  of this BT_InsVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(BT_Ins[] elements) {
		copyInto(elements, 0, 0, elementCount);
	}
	
	/**
	 * Copies the elements of this BT_InsVector into the specified BT_Ins array.
	 *
	 *
	 * @param           elements        the BT_Ins array into which the elements
	 *                                                  of this BT_InsVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(BT_Ins[] elements, int copyIndex, int index, int howMany) {
		System.arraycopy(elementData, index, elements, copyIndex, howMany);
	}
	
	/**
	 * Answers the element at the specified location in this BT_InsVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_InsVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public BT_Ins elementAt(int location) {
			return elementData[location];
	}
	
	
	/**
	 * Answers an Enumeration on the elements of this BT_InsVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_InsVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_InsVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_InsVector can hold the specified number of elements
	 * without growing.
	 *
	 *
	 * @param           minimumCapacity  the minimum number of elements that this
	 *                                  vector will hold before growing
	 *
	 * @see                     #capacity
	 */
	public void ensureCapacity(int minimumCapacity) {
		if (elementData.length < minimumCapacity)
			grow(minimumCapacity);
	}
	/**
	 * Answers the first element in this BT_InsVector.
	 *
	 *
	 * @return          the element at the first position
	 *
	 * @exception       NoSuchElementException  when this vector is empty
	 *
	 * @see                     #elementAt
	 * @see                     #lastElement
	 * @see                     #size
	 */
	public BT_Ins firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_Ins newData[] = new BT_Ins[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this BT_InsVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_InsVector.
	 *
	 *
	 * @param           object  the object to find in this BT_InsVector
	 * @return          the index in this BT_InsVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Ins object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this BT_InsVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_InsVector.
	 *
	 *
	 * @param           object  the object to find in this BT_InsVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_InsVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Ins object, int location) {
		for (int i = location; i < elementCount; i++) {
			if (elementData[i] == object)
				return i;
		}
		return -1;
	}
	
	/**
	 * for performance, finds the indices of all listed instructions.
	 * @param ins1
	 * @return
	 */
	public int[] indexOf(BT_Ins instructions[], int location) {
		int total = instructions.length;
		int result[] = new int[total];
		if(0 == total) {
			return result;
		}
		Arrays.fill(result, -1);
		int found = 0;
		for (int i = location; i < elementCount; i++) {
			BT_Ins element = elementData[i];
			for(int k=0; k<total; k++) {
				if(result[k] < 0) {
					BT_Ins object = instructions[k];
					if (element == object || (element != null && element.equals(object))) {
						result[k] = i;
						found++;
						if(found == total) {
							return result;
						}
					}
				}
			}
		}
		return result;
	}
	
	
	/**
	 * Inserts the specified object into this BT_InsVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this BT_InsVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this BT_InsVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public void insertElementAt(BT_Ins object, int location) {
		int count;

		if (location >= 0 && location <= elementCount) {
			if (elementCount == elementData.length) {
				int newCapacity =
					(capacityIncrement == 0 ? elementCount : capacityIncrement)
						+ elementCount;
				if (newCapacity == 0) {
					newCapacity = 1;
				}
				grow(newCapacity);
			}
			if ((count = elementCount - location) > 0) {
				System.arraycopy(
					elementData,
					location,
					elementData,
					location + 1,
					count);
			}
			elementData[location] = object;
			elementCount++;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers if this BT_InsVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_InsVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_InsVector.
	 *
	 *
	 * @return          the element at the last position
	 *
	 * @exception       NoSuchElementException  when this vector is empty
	 *
	 * @see                     #elementAt
	 * @see                     #firstElement
	 * @see                     #size
	 */
	public BT_Ins lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_InsVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_InsVector.
	 *
	 *
	 * @param           object  the object to find in this BT_InsVector
	 * @return          the index in this BT_InsVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Ins object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_InsVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_InsVector.
	 *
	 *
	 * @param           object  the object to find in this BT_InsVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_InsVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Ins object, int location) {
		if (location < elementCount) {
			for (int i = location; i >= 0; i--) {
				if (elementData[i] == object)
					return i;
			}
			return -1;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Removes all elements from this BT_InsVector, leaving the size zero and
	 * the capacity unchanged.
	 *
	 *
	 * @see                     #isEmpty
	 * @see                     #size
	 */
	public void removeAllElements() {
		for (int i = 0; i < elementCount; i++) {
			elementData[i] = null;
		}
		elementCount = 0;
	}
	/**
	 * Removes the first occurrence, starting at the beginning and
	 * moving towards the end, of the specified object from this
	 * BT_InsVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_InsVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public boolean removeElement(BT_Ins object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this BT_InsVector.
	 *
	 *
	 * @param           location        the index of the element to remove
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #removeElement
	 * @see                     #removeAllElements
	 * @see                     #size
	 */
	public void removeElementAt(int location) {
		if (location < elementCount) {
			int size;
			elementCount--;
			if ((size = elementCount - location) > 0) {
				System.arraycopy(
					elementData,
					location + 1,
					elementData,
					location,
					size);
			}
			elementData[elementCount] = null;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Replaces the element at the specified location in this BT_InsVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this BT_InsVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void setElementAt(BT_Ins object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	/**
	 * Replaces the element at the specified location in this BT_InsVector with
	 * a clone of itself.
	 *
	 *
	 * @param           location        the index at which to clone the instruction
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void cloneElementAt(int location) {
		if (location < elementCount) {
			elementData[location] = (BT_Ins) elementData[location].clone();
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	
	/**
	 * Sets the size of this BT_InsVector to the specified size. If there
	 * are more than length elements in this BT_InsVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_InsVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_InsVector
	 *
	 * @see                     #size
	 */
	public void setSize(int length) {
		ensureCapacity(length);
		if (elementCount > length) {
			for (int i = length; i < elementCount; i++)
				elementData[i] = null;
		}
		elementCount = length;
	}
	/**
	 * Answers the number of elements in this BT_InsVector.
	 *
	 *
	 * @return          the number of elements in this BT_InsVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_InsVector.
	 *
	 *
	 * @return          the string representation of this BT_InsVector
	 *
	 * @see                     #elements
	 */
	public String toString() {
		if (elementCount == 0)
			return "[]";
		int length = elementCount - 1;
		StringBuffer buffer = new StringBuffer();
		buffer.append('[');
		for (int i = 0; i < length; i++) {
			buffer.append(elementData[i]);
			buffer.append(',');
		}
		buffer.append(elementData[length]);
		buffer.append(']');
		return buffer.toString();
	}
	/**
	 * Sets the capacity of this BT_InsVector to be the same as the size.
	 *
	 *
	 * @see                     #capacity
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public void trimToSize() {
		if (elementData.length != elementCount)
			grow(elementCount);
	}
	
	public BT_Ins[] toArray() {
		BT_Ins[] ins = new BT_Ins[this.size()];
		for (int i = 0; i < this.size(); i++)
			ins[i] = this.elementAt(i);
		return ins;
	}
	
	
	
	// ---------- End of code common to all JikesBT Vectors ----------

	/**
	 * returns whether the two vectors contain the same instructions, not necessarily in the same order
	 */
	public boolean hasSameInstructions(BT_InsVector other) {
		int otherSize = other.size();
		if(otherSize != size()) {
			return false;
		}
		for(int i=0; i<otherSize; i++) {
			BT_Ins otherIns = other.elementAt(i);
			if(!contains(otherIns)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * returns whether the two vectors contain the same instructions in the same order
	 */
	public boolean isSame(BT_InsVector other) {
		int otherSize = other.size();
		if(otherSize != size()) {
			return false;
		}
		for(int i=0; i<otherSize; i++) {
			BT_Ins otherIns = other.elementAt(i);
			BT_Ins thisIns = elementAt(i);
			boolean isSame = otherIns == null ? thisIns == null : 
				(otherIns == thisIns || otherIns.equals(thisIns));
			if(!isSame) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 Find the basic block marker with the specified byteIndex, and
	 create one if none already exists.
	
	@param code the code containing the instruction
	@param from the attribute making the reference (which may be the same as code)
	@param the byte index into the bytecodes
	 @return  The BT_BasicBlockMarkerIns found or created at the specified
	   byteIndex.
	 @exception BT_InstructionReferenceException If no such element can be created.
	**/
	public BT_BasicBlockMarkerIns findBasicBlock(BT_CodeAttribute code, BT_Attribute from, int bytecodeIndex) 
			throws BT_InstructionReferenceException {
		try {
			return findBasicBlock(bytecodeIndex);
		} catch(IllegalArgumentException e) {
			throw new BT_InstructionReferenceException(code, from, bytecodeIndex, e.getMessage());
		}
	}
	
	public BT_BasicBlockMarkerIns findBasicBlock(BT_CodeAttribute code, BT_ExceptionTableEntry from, 
			int bytecodeIndex, boolean end) 
		throws BT_InstructionReferenceException {
		try {
			if(end) {
				BT_Ins lastIns = lastElement();
				if(bytecodeIndex == lastIns.byteIndex + lastIns.size()) {
					if(lastIns.isBlockMarker()) {
						return (BT_BasicBlockMarkerIns) lastIns;
					}
					//we are pointing to the end of the code array
					BT_BasicBlockMarkerIns blockIns = BT_Ins.make();
					blockIns.byteIndex = bytecodeIndex;
					addElement(blockIns);
					return blockIns;
				}
			}
			return findBasicBlock(bytecodeIndex);
		} catch(IllegalArgumentException e) {
			throw new BT_InstructionReferenceException(code, from, bytecodeIndex, e.getMessage());
		}
	}

	public BT_BasicBlockMarkerIns findBasicBlock(BT_CodeAttribute code, BT_Ins from, int bytecodeIndex) 
		throws BT_InstructionReferenceException {
		try {
			return findBasicBlock(bytecodeIndex);
		} catch(IllegalArgumentException e) {
			throw new BT_InstructionReferenceException(code, from, bytecodeIndex, e.getMessage());
		}
	}
	
	private BT_BasicBlockMarkerIns findBasicBlock(int bytecodeIndex) {
		return (BT_BasicBlockMarkerIns) findBlock(bytecodeIndex, true, false);
	}
	
	/**
	 * @param bytecodeIndex
	 * @param asBlockMarker whether to return a BT_BasicBlockMarkerIns instead of the matching instruction
	 *  if the matching instruction is not a BT_BasicBlockMarkerIns
	 * @throws IllegalArgumentException if the bytecodeIndex is out of range
	 * @return the matching instruction, which will be a BT_BasicBlockMarkerIns if asBlockMarker is true
	 */
	private BT_Ins findBlock(int bytecodeIndex, boolean asBlockMarker, boolean notBlockMarker) throws IllegalArgumentException {
		if (bytecodeIndex < 0) {
			throw new IllegalArgumentException(Messages.getString("JikesBT.instruction_reference_out_of_range_3"));
		}
		/* 
		 Experimentation has shown that typically the instruction index to be found is 
		 57% the value of byte index.
		 Other things the experimentation showed:
		 in general, the average error in such a case is higher when underestimating
		 the correct instruction index, but in general this average is skewed by
		 a larger number of large errors when underestimating.
		 The total sum of the errors is about the same when missing above or below,
		 so 57% is the most efficient guess.
		*/
		/*
		 * We jump directly to the most likely location of the targeted instruction.
		 * If not a match, then we move upwards or downwards as required.
		 */
		
		int x = (int) (0.57 * (float) bytecodeIndex);
		
		if(x > elementCount - 1) {
			if(elementCount == 0) {
				throw new IllegalArgumentException(Messages.getString("JikesBT.empty_vector_2"));
			}
			x = elementCount - 1;
		}
		
		int byteIndex = elementData[x].byteIndex;  //both elementCount == 0 and index < 0 throws ArrayIndexOutOfBoundsException here
		if(byteIndex == bytecodeIndex) {
			return findBlock(x, bytecodeIndex, true, asBlockMarker, notBlockMarker);
		} else if(byteIndex < bytecodeIndex) { /* must go upwards */
			do {
				++x;
				if(x == elementCount) {
					throw new IllegalArgumentException(Messages.getString("JikesBT.invalid_instruction_reference_4"));
				}
				byteIndex = elementData[x].byteIndex;
				if (byteIndex == bytecodeIndex) {
					return findBlock(x, bytecodeIndex, false, asBlockMarker, notBlockMarker);
				}
			} while(byteIndex < bytecodeIndex);
		} else { /* must go downwards */
			do {
				--x;
				if(x < 0) {
					throw new IllegalArgumentException(Messages.getString("JikesBT.invalid_instruction_reference_4"));
				}
				byteIndex = elementData[x].byteIndex;
				if (byteIndex == bytecodeIndex) {
					return findBlock(x, bytecodeIndex, true, asBlockMarker, notBlockMarker);
				}
			} while(byteIndex > bytecodeIndex);
		}
		throw new IllegalArgumentException(Messages.getString("JikesBT.invalid_instruction_reference_4"));
	}
	
	private BT_Ins findBlock(
			int instructionIndex,
			int byteIndex,
			boolean checkBelow,
			boolean asBlockMarker,
			boolean notBlockMarker) {
		if(asBlockMarker) {
			if(notBlockMarker) {
				throw new IllegalArgumentException();//should never reach here
			}
			return markBlock(instructionIndex, byteIndex, checkBelow);
		}
		BT_Ins ins = elementData[instructionIndex];
		if(notBlockMarker) {
			while(ins.isBlockMarker()) {
				++instructionIndex;
				if(instructionIndex == elementCount) {
					//this should never happen, the last instruction should not be a block marker
					throw new IllegalArgumentException(Messages.getString("JikesBT.invalid_instruction_reference_4"));
				}
				ins = elementData[instructionIndex];
			}
		}
		return ins;
	}
	
	private BT_BasicBlockMarkerIns markBlock(int instructionIndex, int byteIndex, boolean checkBelow) {
		BT_BasicBlockMarkerIns result = markBlock(instructionIndex, checkBelow);
		result.byteIndex = byteIndex;
		return result;
	}
	
	/**
	 * places a block marker instruction at the specified instructionIndex corresponding
	 * to the specified blockIndex.
	 * @param checkBelow check if a block marker instruction exists one index
	 * less than instruction index.  Such a block marker will suffice.  This will
	 * prevent duplicates from being created.  Set this parameter to false
	 * if you already know the instruction just before is not the required block marker.
	 */
	public BT_BasicBlockMarkerIns markBlock(int instructionIndex, boolean checkBelow) {
		BT_Ins result1 = elementData[instructionIndex];
		if(result1.isBlockMarker()) {
			return (BT_BasicBlockMarkerIns) result1;
		}
		
		if(checkBelow && instructionIndex > 0) {
			result1 = elementData[instructionIndex - 1];
			if(result1.isBlockMarker()) {
				return (BT_BasicBlockMarkerIns) result1;
			}
		}
		
		BT_BasicBlockMarkerIns result =  BT_Ins.make();
		insertElementAt(result, instructionIndex);
		return result;			
	}

	/**
	 * Find the BT_Ins with the specified byteIndex.
	 *
	 * @return the BT_Ins found at the specified byteIndex.
	 * @see #findBasicBlock
	 * @exception BT_InstructionReferenceException if no such element exists.
	 */
	public BT_Ins findInstruction(BT_CodeAttribute code, BT_Attribute from, int bytecodeIndex) throws BT_InstructionReferenceException {
		try {
			return findBlock(bytecodeIndex, false, false);
		} catch(IllegalArgumentException e) {
			throw new BT_InstructionReferenceException(code, from, bytecodeIndex, e.getMessage());
		}
	}
	
	public BT_Ins findNonBlockInstruction(BT_CodeAttribute code, BT_Attribute from, int bytecodeIndex) throws BT_InstructionReferenceException {
		try {
			return findBlock(bytecodeIndex, false, true);
		} catch(IllegalArgumentException e) {
			throw new BT_InstructionReferenceException(code, from, bytecodeIndex, e.getMessage());
		}
	}

	/**
	 Reset the byteIndex for each instruction to its proper value.
	**/
	public int setAllByteIndexes() {
		return setAllByteIndexes(false);
	}
	
	/**
	 Reset the byteIndex for each instruction to its maximum proper value.
	**/
	public int setAllByteIndexesMax() {
		return setAllByteIndexes(true);
	}
	
	/**
	 Reset the byteIndex for each instruction to its proper value.
	 If max is true, then it assumes the instruction will take on its maximum size.
	 The ambiguity arises because there are a few instructions that very in size depending
	 upon the index of their references to the constant pool.
	 There are also some instructions that very in size depending upon the index of the local
	 variable that is utilized, however that should not produce ambiguity here because the code
	 attribute maintains a local vector so we are aware at all times of the index.
	**/
	public int setAllByteIndexes(boolean max) {
		//we continue to recalculate the byte indices because the length of jump
		//instructions is dependent upon the byte indices of their targets
		
		//the recalculation continues until no more jump instructions are changed to/from
		//wide instructions
		int offset;
		check:
		do {
			offset = 0;
			for (int k = 0; k < size(); k++) {
				BT_Ins instr = elementAt(k);
				if(instr.byteIndex != offset) {
					while(true) {
						instr.setByteIndex(offset);
						if(++k >= size()) {
							continue check;
						}
						offset += max ? instr.maxSize() : instr.size();
						instr = elementAt(k);
					}
				}
				offset += max ? instr.maxSize() : instr.size();
			}
			break;
		} while(true);
		return offset;	
	}

	public void initializeLabels() {
		for (int n = 0, nLabels = 0; n < size(); n++) {
			BT_Ins in1 = elementAt(n);
			if(in1.isBlockMarker()) {
				((BT_BasicBlockMarkerIns) in1).setLabel("label_" + (nLabels++));
			}	
		}
	}
	
}
