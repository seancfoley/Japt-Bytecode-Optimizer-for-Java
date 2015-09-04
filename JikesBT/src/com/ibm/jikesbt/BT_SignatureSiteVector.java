/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2000
 * All rights reserved
 */

package com.ibm.jikesbt;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 BT_SignatureSiteVector is a variable size contiguous indexable array of {@link BT_SignatureSite}s.
 The size of the BT_SignatureSiteVector is the number of BT_SignatureSites it contains.
 The capacity of the BT_SignatureSiteVector is the number of BT_SignatureSites it can hold.

 <p> BT_SignatureSites may be inserted at any position up to the size of the
 BT_SignatureSiteVector, increasing the size of the BT_SignatureSiteVector. BT_SignatureSites at any
 position in the BT_SignatureSiteVector may be removed, shrinking the size of
 the BT_SignatureSiteVector. BT_SignatureSites at any position in the BT_SignatureSiteVector may be replaced,
 which does not affect the BT_SignatureSiteVector size.

 <p> The capacity of a BT_SignatureSiteVector may be specified when the BT_SignatureSiteVector is
 created. If the capacity of the BT_SignatureSiteVector is exceeded, the capacity
 is increased, doubling by default.
**/

public final class BT_SignatureSiteVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_SignatureSite[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_SignatureSite[] emptyData = new BT_SignatureSite[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 * Constructs a new BT_SignatureSiteVector using the default capacity.
	 *
	 */
	public BT_SignatureSiteVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_SignatureSiteVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_SignatureSiteVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_SignatureSiteVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_SignatureSiteVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_SignatureSiteVector is full
	 */
	public BT_SignatureSiteVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData =
			(capacity == 0) ? emptyData : new BT_SignatureSite[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	/**
	 * Adds the specified object at the end of this BT_SignatureSiteVector.
	 *
	 *
	 * @param           object  the object to add to the BT_SignatureSiteVector
	 */
	public final void addElement(BT_SignatureSite object) {
		insertElementAt(object, elementCount);
	}
	/**
	 * Answers the number of elements this BT_SignatureSiteVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_SignatureSiteVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public final int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new BT_SignatureSiteVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_SignatureSiteVector.
	 *
	 *
	 * @return          a shallow copy of this BT_SignatureSiteVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_SignatureSiteVector vector =
				(BT_SignatureSiteVector) super.clone();
			int length = elementData.length;
			vector.elementData = new BT_SignatureSite[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_SignatureSiteVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_SignatureSiteVector
	 * @return          true if object is an element of this BT_SignatureSiteVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public final boolean contains(BT_SignatureSite object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this BT_SignatureSiteVector into the specified BT_SignatureSite array.
	 *
	 *
	 * @param           elements        the BT_SignatureSite array into which the elements
	 *                                                  of this BT_SignatureSiteVector are copied
	 *
	 * @see                     #clone
	 */
	public final void copyInto(BT_SignatureSite[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this BT_SignatureSiteVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_SignatureSiteVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public final BT_SignatureSite elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers an Enumeration on the elements of this BT_SignatureSiteVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_SignatureSiteVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_SignatureSiteVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public final Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_SignatureSiteVector can hold the specified number of elements
	 * without growing.
	 *
	 *
	 * @param           minimumCapacity  the minimum number of elements that this
	 *                                  vector will hold before growing
	 *
	 * @see                     #capacity
	 */
	public final void ensureCapacity(int minimumCapacity) {
		if (elementData.length < minimumCapacity)
			grow(minimumCapacity);
	}
	/**
	 * Answers the first element in this BT_SignatureSiteVector.
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
	public final BT_SignatureSite firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_SignatureSite newData[] = new BT_SignatureSite[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this BT_SignatureSiteVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_SignatureSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_SignatureSiteVector
	 * @return          the index in this BT_SignatureSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public final int indexOf(BT_SignatureSite object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this BT_SignatureSiteVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_SignatureSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_SignatureSiteVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_SignatureSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public final int indexOf(BT_SignatureSite object, int location) {
		BT_SignatureSite element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 * Inserts the specified object into this BT_SignatureSiteVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this BT_SignatureSiteVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this BT_SignatureSiteVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public final void insertElementAt(BT_SignatureSite object, int location) {
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
	 * Answers if this BT_SignatureSiteVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_SignatureSiteVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public final boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_SignatureSiteVector.
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
	public final BT_SignatureSite lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_SignatureSiteVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_SignatureSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_SignatureSiteVector
	 * @return          the index in this BT_SignatureSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public final int lastIndexOf(BT_SignatureSite object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_SignatureSiteVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_SignatureSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_SignatureSiteVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_SignatureSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public final int lastIndexOf(BT_SignatureSite object, int location) {
		BT_SignatureSite element;
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
	 * Removes all elements from this BT_SignatureSiteVector, leaving the size zero and
	 * the capacity unchanged.
	 *
	 *
	 * @see                     #isEmpty
	 * @see                     #size
	 */
	public final void removeAllElements() {
		for (int i = 0; i < elementCount; i++) {
			elementData[i] = null;
		}
		elementCount = 0;
	}
	/**
	 * Removes the first occurrence, starting at the beginning and
	 * moving towards the end, of the specified object from this
	 * BT_SignatureSiteVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_SignatureSiteVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public final boolean removeElement(BT_SignatureSite object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this BT_SignatureSiteVector.
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
	public final void removeElementAt(int location) {
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
	 * Replaces the element at the specified location in this BT_SignatureSiteVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this BT_SignatureSiteVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public final void setElementAt(BT_SignatureSite object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Sets the size of this BT_SignatureSiteVector to the specified size. If there
	 * are more than length elements in this BT_SignatureSiteVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_SignatureSiteVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_SignatureSiteVector
	 *
	 * @see                     #size
	 */
	public final void setSize(int length) {
		ensureCapacity(length);
		if (elementCount > length) {
			for (int i = length; i < elementCount; i++)
				elementData[i] = null;
		}
		elementCount = length;
	}
	/**
	 * Answers the number of elements in this BT_SignatureSiteVector.
	 *
	 *
	 * @return          the number of elements in this BT_SignatureSiteVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public final int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_SignatureSiteVector.
	 *
	 *
	 * @return          the string representation of this BT_SignatureSiteVector
	 *
	 * @see                     #elements
	 */
	public final String toString() {
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
	 * Sets the capacity of this BT_SignatureSiteVector to be the same as the size.
	 *
	 *
	 * @see                     #capacity
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public final void trimToSize() {
		if (elementData.length != elementCount)
			grow(elementCount);
	}
	// ---------- End of code common to all JikesBT Vectors ----------
}