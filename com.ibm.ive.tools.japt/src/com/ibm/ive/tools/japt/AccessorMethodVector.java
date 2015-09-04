package com.ibm.ive.tools.japt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.Serializable;
import java.util.NoSuchElementException;

import com.ibm.jikesbt.BT_Method;

/**
 AccessorMethodVector is a variable size contiguous indexable array of {@link BT_Method}s.
 The size of the AccessorMethodVector is the number of BT_Methods it contains.
 The capacity of the AccessorMethodVector is the number of BT_Methods it can hold.

 <p> BT_Methods may be inserted at any position up to the size of the
 AccessorMethodVector, increasing the size of the AccessorMethodVector. BT_Methods at any
 position in the AccessorMethodVector may be removed, shrinking the size of
 the AccessorMethodVector. BT_Methods at any position in the AccessorMethodVector may be replaced,
 which does not affect the AccessorMethodVector size.

 <p> The capacity of a AccessorMethodVector may be specified when the AccessorMethodVector is
 created. If the capacity of the AccessorMethodVector is exceeded, the capacity
 is increased, doubling by default.

 @see                 com.ibm.apps.AuxMethodVector
 * @author IBM
**/
public class AccessorMethodVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected AccessorMethod[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static AccessorMethod[] emptyData = new AccessorMethod[0];
	private static final int DEFAULT_SIZE = 0;
	public static final AccessorMethodVector emptyMethodVector = new AccessorMethodVector();
	
	/**
	 * Constructs a new AccessorMethodVector using the default capacity.
	 *
	 */
	public AccessorMethodVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new AccessorMethodVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public AccessorMethodVector(int capacity) {
		this(capacity, 0);
	}
	
	/**
	 * Constructs a new AccessorMethodVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new AccessorMethodVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this AccessorMethodVector is full
	 */
	public AccessorMethodVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData = (capacity == 0) ? emptyData : new AccessorMethod[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	/**
	 * Adds the specified object at the end of this AccessorMethodVector.
	 *
	 *
	 * @param           object  the object to add to the AccessorMethodVector
	 */
	public void addElement(AccessorMethod object) {
		insertElementAt(object, elementCount);
	}
	
	/**
	 Adds the specified object at the end of this AccessorMethodVector,
	 unless the vector already contains it.
	 @return  true if it was added.
	**/
	public boolean addUnique(AccessorMethod object) {
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
	
	public void addAllUnique(AccessorMethodVector other) {
		expandFor(other.elementCount);
		for(int i=0; i<other.size(); i++) {
			addUnique(other.elementAt(i));
		}
	}
	
	public void addAllUnique(AccessorMethod other[]) {
		expandFor(other.length);
		for(int i=0; i<other.length; i++) {
			addUnique(other[i]);
		}
	}
	
	public void addAll(AccessorMethodVector other) {
		int newElementCount = expandFor(other.elementCount);
		System.arraycopy(other.elementData, 0, elementData, elementCount, other.elementCount);
		elementCount = newElementCount;
	}
	
	/**
	 * Adds the specified objects to the end of this AccessorMethodVector.
	 *
	 *
	 * @param           other  the objects to add to the AccessorMethodVector
	 */
	public void addElements(AccessorMethodVector other) {
		addAll(other);
	}
	
	
	/**
	 * Answers the number of elements this AccessorMethodVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this AccessorMethodVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new AccessorMethodVector with the same elements, size, capacity
	 * and capacityIncrement as this AccessorMethodVector.
	 *
	 *
	 * @return          a shallow copy of this AccessorMethodVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			AccessorMethodVector vector = (AccessorMethodVector) super.clone();
			int length = elementData.length;
			vector.elementData = new AccessorMethod[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this AccessorMethodVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this AccessorMethodVector
	 * @return          true if object is an element of this AccessorMethodVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(AccessorMethod object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this AccessorMethodVector into the specified BT_Method array.
	 *
	 *
	 * @param           elements        the BT_Method array into which the elements
	 *                                                  of this AccessorMethodVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(AccessorMethod[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this AccessorMethodVector.
	 *
	 *
	 * @param           location        the index of the element to return in this AccessorMethodVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public AccessorMethod elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException("" + location);
	}
	
	/**
	 * Ensures that this AccessorMethodVector can hold the specified number of elements
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
	 * Answers the first element in this AccessorMethodVector.
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
	public AccessorMethod firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		AccessorMethod newData[] = new AccessorMethod[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this AccessorMethodVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this AccessorMethodVector.
	 *
	 *
	 * @param           object  the object to find in this AccessorMethodVector
	 * @return          the index in this AccessorMethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(AccessorMethod object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this AccessorMethodVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this AccessorMethodVector.
	 *
	 *
	 * @param           object  the object to find in this AccessorMethodVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this AccessorMethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(AccessorMethod object, int location) {
		AccessorMethod element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	
	public int indexOf(BT_Method object, int location) {
		AccessorMethod element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]).method == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 * Inserts the specified object into this AccessorMethodVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this AccessorMethodVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this AccessorMethodVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public void insertElementAt(AccessorMethod object, int location) {
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
	 * Answers if this AccessorMethodVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this AccessorMethodVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this AccessorMethodVector.
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
	public AccessorMethod lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this AccessorMethodVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this AccessorMethodVector.
	 *
	 *
	 * @param           object  the object to find in this AccessorMethodVector
	 * @return          the index in this AccessorMethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(AccessorMethod object) {
		return lastIndexOf(object, elementCount - 1);
	}
	
	/**
	 * Searches in this AccessorMethodVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this AccessorMethodVector.
	 *
	 *
	 * @param           object  the object to find in this AccessorMethodVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this AccessorMethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(AccessorMethod object, int location) {
		AccessorMethod element;
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
	 * Removes all elements from this AccessorMethodVector, leaving the size zero and
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
	 * AccessorMethodVector.
	 *
	 *
	 * @param           object  the object to remove from this AccessorMethodVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public boolean removeElement(AccessorMethod object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	
	/**
	 * Removes the first occurrence, starting at the beginning and
	 * moving towards the end, of the specified object from this
	 * AccessorMethodVector.
	 *
	 *
	 * @param           object  the object to remove from this AccessorMethodVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public boolean removeElement(BT_Method object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	
	/**
	 * Removes the element at the specified location from this AccessorMethodVector.
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
	 * Replaces the element at the specified location in this AccessorMethodVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this AccessorMethodVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void setElementAt(AccessorMethod object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Sets the size of this AccessorMethodVector to the specified size. If there
	 * are more than length elements in this AccessorMethodVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the AccessorMethodVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this AccessorMethodVector
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
	 * Answers the number of elements in this AccessorMethodVector.
	 *
	 *
	 * @return          the number of elements in this AccessorMethodVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this AccessorMethodVector.
	 *
	 *
	 * @return          the string representation of this AccessorMethodVector
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
	 * returns an array view of the elements in the vector.  Changes to the array
	 * will have no impact on the vector itself.
	 */
	public AccessorMethod[] toArray() {
		if(elementCount == 0) {
			return emptyData;
		}
		AccessorMethod newData[] = new AccessorMethod[elementCount];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		return newData;
	}
	
	/**
	 * Sets the capacity of this AccessorMethodVector to be the same as the size.
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
	
	/**
	 Determines whether the collections contain exactly the same elements in the same order.
	 @param  mv  A AccessorMethodVector
	**/
	public boolean equals(Object mv) {

		AccessorMethodVector that = (AccessorMethodVector) mv;
		if (this.elementCount != that.elementCount)
			return false;
		for (int i = 0; i < elementCount; ++i)
			if (this.elementData[i] != that.elementData[i])
				return false;
		return true;
	}
	
}
