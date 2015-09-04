package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 BT_FieldVector is a variable size contiguous indexable array of {@link BT_Field}s.
 The size of the BT_FieldVector is the number of BT_Fields it contains.
 The capacity of the BT_FieldVector is the number of BT_Fields it can hold.

 <p> BT_Fields may be inserted at any position up to the size of the
 BT_FieldVector, increasing the size of the BT_FieldVector. BT_Fields at any
 position in the BT_FieldVector may be removed, shrinking the size of
 the BT_FieldVector. BT_Fields at any position in the BT_FieldVector may be replaced,
 that does not affect the BT_FieldVector size.

 <p> The capacity of a BT_FieldVector may be specified when the BT_FieldVector is
 created. If the capacity of the BT_FieldVector is exceeded, the capacity
 is increased, doubling by default.


 <p> The following public members are in addition to the usual Vector methods:
 <sl>
 <li> {@link BT_FieldVector#findField(String name, BT_Class type)}
 <li> {@link BT_FieldVector#findField(String name)}
 </sl>
 * @author IBM
**/
public class BT_FieldVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_Field[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_Field[] emptyData = new BT_Field[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 * Constructs a new BT_FieldVector using the default capacity.
	 *
	 */
	public BT_FieldVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_FieldVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_FieldVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_FieldVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_FieldVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_FieldVector is full
	 */
	public BT_FieldVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData = (capacity == 0) ? emptyData : new BT_Field[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	/**
	 * Adds the specified object at the end of this BT_FieldVector.
	 *
	 *
	 * @param           object  the object to add to the BT_FieldVector
	 */
	public void addElement(BT_Field object) {
		insertElementAt(object, elementCount);
	}
	
	public void addAll(BT_FieldVector other) {
		int newElementCount = elementCount + other.elementCount;
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
		System.arraycopy(other.elementData, 0, elementData, elementCount, other.elementCount);
		elementCount = newElementCount;
	}
	
	/**
	 Adds the specified object at the end of this BT_FieldVector,
	 unless the vector already contains it.
	 @return  true if it was added.
	**/
	public boolean addUnique(BT_Field object) {
		if (contains(object))
			return false;
		addElement(object);
		return true;
	}
	
	/**
	 * Answers the number of elements this BT_FieldVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_FieldVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new BT_FieldVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_FieldVector.
	 *
	 *
	 * @return          a shallow copy of this BT_FieldVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_FieldVector vector = (BT_FieldVector) super.clone();
			int length = elementData.length;
			vector.elementData = new BT_Field[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_FieldVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_FieldVector
	 * @return          true if object is an element of this BT_FieldVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(BT_Field object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this BT_FieldVector into the specified BT_Field array.
	 *
	 *
	 * @param           elements        the BT_Field array into which the elements
	 *                                                  of this BT_FieldVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(BT_Field[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this BT_FieldVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_FieldVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public BT_Field elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers an Enumeration on the elements of this BT_FieldVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_FieldVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_FieldVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_FieldVector can hold the specified number of elements
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
	 * Answers the first element in this BT_FieldVector.
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
	public BT_Field firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_Field newData[] = new BT_Field[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this BT_FieldVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_FieldVector.
	 *
	 *
	 * @param           object  the object to find in this BT_FieldVector
	 * @return          the index in this BT_FieldVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Field object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this BT_FieldVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_FieldVector.
	 *
	 *
	 * @param           object  the object to find in this BT_FieldVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_FieldVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Field object, int location) {
		BT_Field element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 * Inserts the specified object into this BT_FieldVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this BT_FieldVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this BT_FieldVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public void insertElementAt(BT_Field object, int location) {
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
	 * Answers if this BT_FieldVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_FieldVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_FieldVector.
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
	public BT_Field lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_FieldVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_FieldVector.
	 *
	 *
	 * @param           object  the object to find in this BT_FieldVector
	 * @return          the index in this BT_FieldVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Field object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_FieldVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_FieldVector.
	 *
	 *
	 * @param           object  the object to find in this BT_FieldVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_FieldVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Field object, int location) {
		BT_Field element;
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
	 * Removes all elements from this BT_FieldVector, leaving the size zero and
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
	 * BT_FieldVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_FieldVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public boolean removeElement(BT_Field object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this BT_FieldVector.
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
	 * Replaces the element at the specified location in this BT_FieldVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this BT_FieldVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void setElementAt(BT_Field object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Sets the size of this BT_FieldVector to the specified size. If there
	 * are more than length elements in this BT_FieldVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_FieldVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_FieldVector
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
	 * Answers the number of elements in this BT_FieldVector.
	 *
	 *
	 * @return          the number of elements in this BT_FieldVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_FieldVector.
	 *
	 *
	 * @return          the string representation of this BT_FieldVector
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
	public BT_Field[] toArray() {
		if(elementCount == 0) {
			return emptyData;
		}
		BT_Field newData[] = new BT_Field[elementCount];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		return newData;
	}
	
	/**
	 * Sets the capacity of this BT_FieldVector to be the same as the size.
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
	 * Find the BT_Field with the specified name and type.
	 * @return null If BT_Field not found.
	 */
	public BT_Field findField(String name, BT_Class type) {
		BT_Field field = findField(name, type.name);
		if(field != null && !(field.fieldType instanceof BT_Class)) {
			/* if the field is not dereferenced yet we can save time here */
			field.setFieldType(type);
		}
		return field;
	}
	
	public BT_Field findField(String name, String typeName) {
		if (elementCount == 0)
			return null;
		for (int n = 0; n < elementCount; n++) {
			BT_Field f = elementData[n];
			if (f.name.equals(name) && f.getTypeName().equals(typeName)) {
				return f;
			}
		}
		return null;
	}

	/**
	 * Find the BT_Field with the specified name.
	 * @return null If BT_Field not found.
	 */
	public BT_Field findField(String name) {
		if (elementCount == 0)
			return null;
		for (int n = 0; n < elementCount; n++)
			if (elementData[n].name.equals(name))
				return elementData[n];
		return null;
	}
	
	/**
	 Sorts the methods in this BT_MethodVector into collating sequence.
	**/
	public void sort() {
		sort(false);
	}

	/**
	 Sorts the methods in this BT_MethodVector into ascending or
	 descending collating sequence depending on their class and method
	 name.
	 @param  descending  True means to sort in descending order.
	**/
	public void sort(boolean descending) {
		if(descending) {
			sort(Collections.reverseOrder());
		} else {
			Arrays.sort(elementData, 0, elementCount);
		}
	}


	/**
	 Sorts the methods in this BT_MethodVector using a given Comparator.
	**/
	public void sort(Comparator comparator) {
		Arrays.sort(elementData, 0, elementCount, comparator);
	}


	/**
	 Sorts the methods in an arbitrary order useful only for comparing vectors.
	 The order is by their hashcode after they are cast to java.lang.Object.
	**/
	public void sortByAddress() {
		sort(new Comparator() {
				public int compare(Object p1, Object p2) {
					return p1.hashCode() - p2.hashCode();
				}
			}
		);
	}

}
