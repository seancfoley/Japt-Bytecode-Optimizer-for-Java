package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 BT_MethodInlawVector is a variable size contiguous indexable array of BT_MethodInlaws.
 The size of the BT_MethodInlawVector is the number of BT_MethodInlaws it contains.
 The capacity of the BT_MethodInlawVector is the number of BT_MethodInlaws it can hold.

 <p> BT_MethodInlaws may be inserted at any position up to the size of the
 BT_MethodInlawVector, increasing the size of the BT_MethodInlawVector. BT_MethodInlaws at any
 position in the BT_MethodInlawVector may be removed, shrinking the size of
 the BT_MethodInlawVector. BT_MethodInlaws at any position in the BT_MethodInlawVector may be replaced,
 which does not affect the BT_MethodInlawVector size.

 <p> The capacity of a BT_MethodInlawVector may be specified when the BT_MethodInlawVector is
 created. If the capacity of the BT_MethodInlawVector is exceeded, the capacity
 is increased, doubling by default.

 <p> The following public methods are in addition to the usual Vector methods:
 <sl>
 <li> {@link BT_MethodInlawVector#findMethod(BT_Method m)}
 <li> {@link BT_MethodInlawVector#sortArbitrarily()}
 </sl>
 * @author IBM
**/
public final class BT_MethodInlawVector
	extends BT_Base
	implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_MethodInlaw[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_MethodInlaw[] emptyData = new BT_MethodInlaw[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 * Constructs a new BT_MethodInlawVector using the default capacity.
	 *
	 */
	public BT_MethodInlawVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_MethodInlawVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_MethodInlawVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_MethodInlawVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_MethodInlawVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_MethodInlawVector is full
	 */
	public BT_MethodInlawVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData =
			(capacity == 0) ? emptyData : new BT_MethodInlaw[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	/**
	 * Adds the specified object at the end of this BT_MethodInlawVector.
	 *
	 *
	 * @param           object  the object to add to the BT_MethodInlawVector
	 */
	public void addElement(BT_MethodInlaw object) {
		insertElementAt(object, elementCount);
	}
	/**
	 * Answers the number of elements this BT_MethodInlawVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_MethodInlawVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new BT_MethodInlawVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_MethodInlawVector.
	 *
	 *
	 * @return          a shallow copy of this BT_MethodInlawVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_MethodInlawVector vector = (BT_MethodInlawVector) super.clone();
			int length = elementData.length;
			vector.elementData = new BT_MethodInlaw[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_MethodInlawVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_MethodInlawVector
	 * @return          true if object is an element of this BT_MethodInlawVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(BT_MethodInlaw object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this BT_MethodInlawVector into the specified BT_MethodInlaw array.
	 *
	 *
	 * @param           elements        the BT_MethodInlaw array into which the elements
	 *                                                  of this BT_MethodInlawVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(BT_MethodInlaw[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this BT_MethodInlawVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_MethodInlawVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public BT_MethodInlaw elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers an Enumeration on the elements of this BT_MethodInlawVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_MethodInlawVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_MethodInlawVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_MethodInlawVector can hold the specified number of elements
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
	 * Answers the first element in this BT_MethodInlawVector.
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
	public BT_MethodInlaw firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_MethodInlaw newData[] = new BT_MethodInlaw[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this BT_MethodInlawVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_MethodInlawVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodInlawVector
	 * @return          the index in this BT_MethodInlawVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_MethodInlaw object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this BT_MethodInlawVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_MethodInlawVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodInlawVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_MethodInlawVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_MethodInlaw object, int location) {
		BT_MethodInlaw element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 * Inserts the specified object into this BT_MethodInlawVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this BT_MethodInlawVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this BT_MethodInlawVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public void insertElementAt(BT_MethodInlaw object, int location) {
		int count;

		if (location >= 0 && location <= elementCount) {
			if (elementCount == elementData.length) {
				int newCapacity =
					(capacityIncrement == 0 ? elementCount : capacityIncrement)
						+ elementCount;
				if (newCapacity == 0)
					newCapacity++;
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
	 * Answers if this BT_MethodInlawVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_MethodInlawVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_MethodInlawVector.
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
	public BT_MethodInlaw lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_MethodInlawVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_MethodInlawVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodInlawVector
	 * @return          the index in this BT_MethodInlawVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_MethodInlaw object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_MethodInlawVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_MethodInlawVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodInlawVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_MethodInlawVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_MethodInlaw object, int location) {
		BT_MethodInlaw element;
		if (location < elementCount) {
			for (int i = location; i >= 0; i--) {
				if ((element = elementData[i]) == object)
					return i;
				if ((element != null) && (element.equals(object)))
					return i;
			}
			return -1;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Removes all elements from this BT_MethodInlawVector, leaving the size zero and
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
	 * BT_MethodInlawVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_MethodInlawVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public boolean removeElement(BT_MethodInlaw object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this BT_MethodInlawVector.
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
	 * Replaces the element at the specified location in this BT_MethodInlawVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this BT_MethodInlawVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void setElementAt(BT_MethodInlaw object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException("Index: " + location);
	}
	/**
	 * Sets the size of this BT_MethodInlawVector to the specified size. If there
	 * are more than length elements in this BT_MethodInlawVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_MethodInlawVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_MethodInlawVector
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
	 * Answers the number of elements in this BT_MethodInlawVector.
	 *
	 *
	 * @return          the number of elements in this BT_MethodInlawVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_MethodInlawVector.
	 *
	 *
	 * @return          the string representation of this BT_MethodInlawVector
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
	 * Sets the capacity of this BT_MethodInlawVector to be the same as the size.
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
	// ---------- End of code common to all JikesBT Vectors ----------

	/**
	 Finds an existing BT_MethodInlaw.
	 @return  Null if not found.
	**/
	BT_MethodInlaw findInlawRecord(BT_Method m, BT_Class c) {
		for (int i = 0; i < this.size(); ++i) {
			BT_MethodInlaw p = this.elementAt(i);
			if ((m == p.method1_ || m == p.method2_) && c == p.cls_)
				return p;
		}
		return null;
	}

	/**
	 Removes the pair that points one inlaw method to its inlaw method.
	 "Point" implies one way.
	 Assumes there are no duplicates.
	 Does not disturb the part of the vector preceeding the part being operated on.
	 The pair must be found.
	 This should be part of BT_InlawVector.
	 @param  toM  The reference in "this" method to toM will be removed.
	**/
	void depointInlaw(BT_Class causeC, BT_Method toM) {
		for (int i = 0; i < this.size(); ++i) {
			BT_MethodInlaw p = this.elementAt(i);
			if ((toM == p.method1_ || toM == p.method2_) && causeC == p.cls_) {
				this.removeElementAt(i);
				return;
			}
		}
		if (CHECK_USER)
			expect("Did not find pair " + toM + " " + causeC.useName());
	}
	/**
	 Searches in this BT_MethodInlawVector for the element with the specified method.
	 @return  Null.
	 @see                     #contains
	**/
	public BT_MethodInlaw findMethod(BT_Method m) {
		for (int i = elementCount - 1; i != -1; --i) {
			BT_MethodInlaw element = elementData[i];
			if (element.method1_ == m || element.method2_ == m)
				return element;
		}
		return null;
	}

	/**
	 Sorts in a fixed but arbitrary order (using Object.hashCode).
	**/
	public void sortArbitrarily() {
		for (boolean done = false; !done;) {
			done = true;
			for (int i = elementCount - 1; i > 0; --i) {
				int j = elementData[i - 1].compareTo(elementData[i]);
				if (j > 0) {
					BT_MethodInlaw c = elementData[i];
					elementData[i] = elementData[i - 1];
					elementData[i - 1] = c;
					done = false;
				}
			}
		}
	}

}
