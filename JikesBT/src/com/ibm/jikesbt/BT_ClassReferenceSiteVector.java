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
 BT_ClassReferenceSiteVector is a variable size contiguous indexable array of {@link BT_ClassReferenceSite}s.
 The size of the BT_ClassReferenceSiteVector is the number of BT_ClassReferenceSites it contains.
 The capacity of the BT_ClassReferenceSiteVector is the number of BT_ClassReferenceSites it can hold.

 <p> BT_ClassReferenceSites may be inserted at any position up to the size of the
 BT_ClassReferenceSiteVector, increasing the size of the BT_ClassReferenceSiteVector. BT_ClassReferenceSites at any
 position in the BT_ClassReferenceSiteVector may be removed, shrinking the size of
 the BT_ClassReferenceSiteVector. BT_ClassReferenceSites at any position in the BT_ClassReferenceSiteVector may be replaced,
 which does not affect the BT_ClassReferenceSiteVector size.

 <p> The capacity of a BT_ClassReferenceSiteVector may be specified when the BT_ClassReferenceSiteVector is
 created. If the capacity of the BT_ClassReferenceSiteVector is exceeded, the capacity
 is increased, doubling by default.
**/

public final class BT_ClassReferenceSiteVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_ClassReferenceSite[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_ClassReferenceSite[] emptyData = new BT_ClassReferenceSite[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 * Constructs a new BT_ClassReferenceSiteVector using the default capacity.
	 *
	 */
	public BT_ClassReferenceSiteVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_ClassReferenceSiteVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_ClassReferenceSiteVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_ClassReferenceSiteVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_ClassReferenceSiteVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_ClassReferenceSiteVector is full
	 */
	public BT_ClassReferenceSiteVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData =
			(capacity == 0) ? emptyData : new BT_ClassReferenceSite[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	/**
	 * Adds the specified object at the end of this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @param           object  the object to add to the BT_ClassReferenceSiteVector
	 */
	public final void addElement(BT_ClassReferenceSite object) {
		insertElementAt(object, elementCount);
	}
	
	/**
	 Adds the specified object at the end of this BT_ClassReferenceSiteVector,
	 unless the vector already contains it.
	 @return  true if it was added.
	**/
	public boolean addUnique(BT_ClassReferenceSite object) {
		if (contains(object))
			return false;
		addElement(object);
		return true;
	}
	
	/**
	 * Answers the number of elements this BT_ClassReferenceSiteVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_ClassReferenceSiteVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public final int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new BT_ClassReferenceSiteVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @return          a shallow copy of this BT_ClassReferenceSiteVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_ClassReferenceSiteVector vector =
				(BT_ClassReferenceSiteVector) super.clone();
			int length = elementData.length;
			vector.elementData = new BT_ClassReferenceSite[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_ClassReferenceSiteVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_ClassReferenceSiteVector
	 * @return          true if object is an element of this BT_ClassReferenceSiteVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public final boolean contains(BT_ClassReferenceSite object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this BT_ClassReferenceSiteVector into the specified BT_ClassReferenceSite array.
	 *
	 *
	 * @param           elements        the BT_ClassReferenceSite array into which the elements
	 *                                                  of this BT_ClassReferenceSiteVector are copied
	 *
	 * @see                     #clone
	 */
	public final void copyInto(BT_ClassReferenceSite[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_ClassReferenceSiteVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public final BT_ClassReferenceSite elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers an Enumeration on the elements of this BT_ClassReferenceSiteVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_ClassReferenceSiteVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_ClassReferenceSiteVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public final Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_ClassReferenceSiteVector can hold the specified number of elements
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
	 * Answers the first element in this BT_ClassReferenceSiteVector.
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
	public final BT_ClassReferenceSite firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_ClassReferenceSite newData[] = new BT_ClassReferenceSite[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this BT_ClassReferenceSiteVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassReferenceSiteVector
	 * @return          the index in this BT_ClassReferenceSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public final int indexOf(BT_ClassReferenceSite object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this BT_ClassReferenceSiteVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassReferenceSiteVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_ClassReferenceSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public final int indexOf(BT_ClassReferenceSite object, int location) {
		BT_ClassReferenceSite element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 * Inserts the specified object into this BT_ClassReferenceSiteVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this BT_ClassReferenceSiteVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this BT_ClassReferenceSiteVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public final void insertElementAt(BT_ClassReferenceSite object, int location) {
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
	 * Answers if this BT_ClassReferenceSiteVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_ClassReferenceSiteVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public final boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_ClassReferenceSiteVector.
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
	public final BT_ClassReferenceSite lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_ClassReferenceSiteVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassReferenceSiteVector
	 * @return          the index in this BT_ClassReferenceSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public final int lastIndexOf(BT_ClassReferenceSite object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_ClassReferenceSiteVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassReferenceSiteVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_ClassReferenceSiteVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public final int lastIndexOf(BT_ClassReferenceSite object, int location) {
		BT_ClassReferenceSite element;
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
	 * Removes all elements from this BT_ClassReferenceSiteVector, leaving the size zero and
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
	 * BT_ClassReferenceSiteVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_ClassReferenceSiteVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public final boolean removeElement(BT_ClassReferenceSite object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this BT_ClassReferenceSiteVector.
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
	 * Replaces the element at the specified location in this BT_ClassReferenceSiteVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this BT_ClassReferenceSiteVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public final void setElementAt(BT_ClassReferenceSite object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Sets the size of this BT_ClassReferenceSiteVector to the specified size. If there
	 * are more than length elements in this BT_ClassReferenceSiteVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_ClassReferenceSiteVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_ClassReferenceSiteVector
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
	 * Answers the number of elements in this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @return          the number of elements in this BT_ClassReferenceSiteVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public final int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_ClassReferenceSiteVector.
	 *
	 *
	 * @return          the string representation of this BT_ClassReferenceSiteVector
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
	 * Sets the capacity of this BT_ClassReferenceSiteVector to be the same as the size.
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