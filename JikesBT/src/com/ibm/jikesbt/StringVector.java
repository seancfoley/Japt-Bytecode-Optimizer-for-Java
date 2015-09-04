package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 StringVector is a variable size contiguous indexable array of java.lang.Strings.
 The size of the StringVector is the number of Strings it contains.
 The capacity of the StringVector is the number of Strings it can hold.

 <p> Strings may be inserted at any position up to the size of the
 StringVector, increasing the size of the StringVector. Strings at any
 position in the StringVector may be removed, shrinking the size of
 the StringVector. Strings at any position in the StringVector may be replaced,
 that does not affect the StringVector size.

 <p> The capacity of a StringVector may be specified when the StringVector is
 created. If the capacity of the StringVector is exceeded, the capacity
 is increased, doubling by default.

 <p> The following methods are in addition to the usual Vector methods:
 <sl>
 <li> {@link StringVector#sort()}
 <li> {@link StringVector#sort(boolean descending)}
 <li> {@link StringVector#print( java.io.PrintStream ps)}
 <li> {@link StringVector#toArray()}
 </sl>
 * @author IBM
**/

public final class StringVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected String[] elementData;
	/**
	 * The amount by that the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static String[] emptyData = new String[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 * Constructs a new StringVector using the default capacity.
	 *
	 */
	public StringVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new StringVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public StringVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new StringVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new StringVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this StringVector is full
	 */
	public StringVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData = (capacity == 0) ? emptyData : new String[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	
	/**
	 * Adds the specified object to the end of this StringVector if not already contained
	 * within the vector.
	 *
	 *
	 * @param           object  the object to add to the StringVector
	 */
	public void addUnique(String object) {
		if(!contains(object)) {
			addElement(object);
		}
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
	
	public void addAllUnique(StringVector other) {
		expandFor(other.elementCount);
		for(int i=0; i<other.size(); i++) {
			addUnique(other.elementAt(i));
		}
	}
	
	public void addAllUnique(String other[]) {
		expandFor(other.length);
		for(int i=0; i<other.length; i++) {
			addUnique(other[i]);
		}
	}
	
	public void addAll(StringVector other) {
		int newElementCount = expandFor(other.elementCount);
		System.arraycopy(other.elementData, 0, elementData, elementCount, other.elementCount);
		elementCount = newElementCount;
	}
	
	public void addAll(String other[]) {
        int newElementCount = expandFor(other.length);
        System.arraycopy(other, 0, elementData, elementCount, other.length);
        elementCount = newElementCount;
    }
	
	/**
	 * Adds the specified object to the end of this StringVector.
	 *
	 *
	 * @param           object  the object to add to the StringVector
	 */
	public void addElement(String object) {
		insertElementAt(object, elementCount);
	}
	
	/**
	 * Adds the specified objects to the end of this StringVector.
	 *
	 *
	 * @param           objects  the objects to add to the StringVector
	 */
	public void addElements(String objects[]) {
		insertElementsAt(objects, elementCount);
	}
	
	/**
	 * Answers the number of elements this StringVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this StringVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new StringVector with the same elements, size, capacity
	 * and capacityIncrement as this StringVector.
	 *
	 *
	 * @return          a shallow copy of this StringVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			StringVector vector = (StringVector) super.clone();
			int length = elementData.length;
			vector.elementData = new String[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this StringVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this StringVector
	 * @return          true if object is an element of this StringVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(String object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this StringVector into the specified String array.
	 *
	 *
	 * @param           elements        the String array into that the elements
	 *                                                  of this StringVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(String[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this StringVector.
	 *
	 *
	 * @param           location        the index of the element to return in this StringVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public String elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers an Enumeration on the elements of this StringVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this StringVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this StringVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this StringVector can hold the specified number of elements
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
	 * Answers the first element in this StringVector.
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
	public String firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		String newData[] = new String[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this StringVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this StringVector.
	 *
	 *
	 * @param           object  the object to find in this StringVector
	 * @return          the index in this StringVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(String object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this StringVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this StringVector.
	 *
	 *
	 * @param           object  the object to find in this StringVector
	 * @param           location        the index at that to start searching
	 * @return          the index in this StringVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(String object, int location) {
		String element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 * Inserts the specified object into this StringVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this StringVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this StringVector
	 * @param           location        the index at that to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public void insertElementAt(String object, int location) {
		
		if (location >= 0 && location <= elementCount) {
			if (elementCount == elementData.length) {
				int newCapacity =
					(capacityIncrement == 0 ? elementCount : capacityIncrement)
						+ elementCount;
				if (newCapacity == 0)
					newCapacity++;
				grow(newCapacity);
			}
			int count = elementCount - location;
			if (count > 0) {
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
	 * Inserts the specified objects into this StringVector at the specified
	 * location. The objects are inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this StringVector, the objects are added at the end.
	 */
	public void insertElementsAt(String objects[], int location) {
		int numObjects = objects.length;
		if (location >= 0 && location <= elementCount) {
			if(elementCount + numObjects > elementData.length) {
				int actualIncrement = (capacityIncrement == 0 ? (elementCount == 0 ? 1 : elementCount) : capacityIncrement);
				int newCapacity = elementCount;
				do {
					newCapacity += actualIncrement;
				} while(elementCount + numObjects > newCapacity);
				grow(newCapacity);
			}
			int count = elementCount - location;
			if (count > 0) {
				System.arraycopy(
					elementData,
					location,
					elementData,
					location + numObjects,
					count);
			}
			System.arraycopy(objects, 0, elementData, location, numObjects);
			elementCount += numObjects;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	
	/**
	 * Answers if this StringVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this StringVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this StringVector.
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
	public String lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this StringVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this StringVector.
	 *
	 *
	 * @param           object  the object to find in this StringVector
	 * @return          the index in this StringVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(String object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this StringVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this StringVector.
	 *
	 *
	 * @param           object  the object to find in this StringVector
	 * @param           location        the index at that to start searching
	 * @return          the index in this StringVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(String object, int location) {
		String element;
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
	 * Removes all elements from this StringVector, leaving the size zero and
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
	 * StringVector.
	 *
	 *
	 * @param           object  the object to remove from this StringVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public boolean removeElement(String object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this StringVector.
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
	 * Replaces the element at the specified location in this StringVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this StringVector
	 * @param           location        the index at that to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void setElementAt(String object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Sets the size of this StringVector to the specified size. If there
	 * are more than length elements in this StringVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the StringVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this StringVector
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
	 * Answers the number of elements in this StringVector.
	 *
	 *
	 * @return          the number of elements in this StringVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this StringVector.
	 *
	 *
	 * @return          the string representation of this StringVector
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
	 * Sets the capacity of this StringVector to be the same as the size.
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
	 Sorts the classes in this StringVector into collating sequence.
	
	 @return          the sorted StringVector
	
	 @see                     #elements
	
	**/
	public void sort() {
		sort(false);
	}

	/**
	 Sorts the classes in this StringVector into ascending or
	 descending collating sequence.
	
	 @param           reverse whether or not the classes are to be
	   sorted in descending sequence.
	
	 @return          the sorted StringVector
	
	 @see                     #elements
	
	**/
	public void sort(boolean descending) {
		if(descending) {
			Arrays.sort(elementData, 0, elementCount, Collections.reverseOrder());
		}
		else {
			Arrays.sort(elementData, 0, elementCount);
		}
	}

	/**
	 Prints the elements one per line.
	**/
	public void print(java.io.PrintStream ps) {
		for (int i = 0; i < elementCount; ++i)
			ps.println(elementData[i]);
	}

	public String[] toArray() {
		String[] pats = new String[size()];
		copyInto(pats);
		return pats;
	}
}
