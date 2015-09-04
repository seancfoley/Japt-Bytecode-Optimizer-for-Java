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
 BT_MethodVector is a variable size contiguous indexable array of {@link BT_Method}s.
 The size of the BT_MethodVector is the number of BT_Methods it contains.
 The capacity of the BT_MethodVector is the number of BT_Methods it can hold.

 <p> BT_Methods may be inserted at any position up to the size of the
 BT_MethodVector, increasing the size of the BT_MethodVector. BT_Methods at any
 position in the BT_MethodVector may be removed, shrinking the size of
 the BT_MethodVector. BT_Methods at any position in the BT_MethodVector may be replaced,
 which does not affect the BT_MethodVector size.

 <p> The capacity of a BT_MethodVector may be specified when the BT_MethodVector is
 created. If the capacity of the BT_MethodVector is exceeded, the capacity
 is increased, doubling by default.

 @see                 com.ibm.apps.AuxMethodVector
 * @author IBM
**/
public class BT_MethodVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_Method[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_Method[] emptyData = new BT_Method[0];
	private static final int DEFAULT_SIZE = 0;
	public static final BT_MethodVector emptyMethodVector = new BT_MethodVector();
	
	/**
	 * Constructs a new BT_MethodVector using the default capacity.
	 *
	 */
	public BT_MethodVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_MethodVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_MethodVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_MethodVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_MethodVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_MethodVector is full
	 */
	public BT_MethodVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData = (capacity == 0) ? emptyData : new BT_Method[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	/**
	 * Adds the specified object at the end of this BT_MethodVector.
	 *
	 *
	 * @param           object  the object to add to the BT_MethodVector
	 */
	public void addElement(BT_Method object) {
		insertElementAt(object, elementCount);
	}
	
	public void addAllUnique(BT_MethodVector other) {
		for(int i=0; i<other.size(); i++) {
			addUnique(other.elementAt(i));
		}
	}
	
	public void addAll(BT_MethodVector other) {
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
	 * Adds the specified objects to the end of this BT_MethodVector.
	 *
	 *
	 * @param           other  the objects to add to the BT_MethodVector
	 */
	public void addElements(BT_MethodVector other) {
		addAll(other);
	}
	
	
	/**
	 * Answers the number of elements this BT_MethodVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_MethodVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new BT_MethodVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_MethodVector.
	 *
	 *
	 * @return          a shallow copy of this BT_MethodVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_MethodVector vector = (BT_MethodVector) super.clone();
			int length = elementData.length;
			vector.elementData = new BT_Method[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_MethodVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_MethodVector
	 * @return          true if object is an element of this BT_MethodVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(BT_Method object) {
		return indexOf(object, 0) != -1;
	}
	/**
	 * Copies the elements of this BT_MethodVector into the specified BT_Method array.
	 *
	 *
	 * @param           elements        the BT_Method array into which the elements
	 *                                                  of this BT_MethodVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(BT_Method[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this BT_MethodVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_MethodVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public BT_Method elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException(Messages.getString("JikesBT.Index___4") + location);
	}
	/**
	 * Answers an Enumeration on the elements of this BT_MethodVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_MethodVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_MethodVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_MethodVector can hold the specified number of elements
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
	 * Answers the first element in this BT_MethodVector.
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
	public BT_Method firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_Method newData[] = new BT_Method[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this BT_MethodVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_MethodVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodVector
	 * @return          the index in this BT_MethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Method object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this BT_MethodVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_MethodVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_MethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Method object, int location) {
		BT_Method element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 * Inserts the specified object into this BT_MethodVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this BT_MethodVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this BT_MethodVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public void insertElementAt(BT_Method object, int location) {
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
	 * Answers if this BT_MethodVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_MethodVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_MethodVector.
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
	public BT_Method lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_MethodVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_MethodVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodVector
	 * @return          the index in this BT_MethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Method object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_MethodVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_MethodVector.
	 *
	 *
	 * @param           object  the object to find in this BT_MethodVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_MethodVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Method object, int location) {
		BT_Method element;
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
	 * Removes all elements from this BT_MethodVector, leaving the size zero and
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
	 * BT_MethodVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_MethodVector
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
	 * Removes the element at the specified location from this BT_MethodVector.
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
	 * Replaces the element at the specified location in this BT_MethodVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this BT_MethodVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void setElementAt(BT_Method object, int location) {
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Sets the size of this BT_MethodVector to the specified size. If there
	 * are more than length elements in this BT_MethodVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_MethodVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_MethodVector
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
	 * Answers the number of elements in this BT_MethodVector.
	 *
	 *
	 * @return          the number of elements in this BT_MethodVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_MethodVector.
	 *
	 *
	 * @return          the string representation of this BT_MethodVector
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
	public BT_Method[] toArray() {
		if(elementCount == 0) {
			return emptyData;
		}
		BT_Method newData[] = new BT_Method[elementCount];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		return newData;
	}
	
	/**
	 * Sets the capacity of this BT_MethodVector to be the same as the size.
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
	 Finds the BT_Method with the specified name and signature.
	
	 @return  null if BT_Method not found.
	**/
	public BT_Method findMethod(String name, BT_MethodSignature sig) {
		BT_Method result = findMethod(name, sig.toString());
		if(result != null && !(result.methodType instanceof BT_MethodSignature)) {
			//if the method is not dereferenced yet we can save a lot of time by setting the signature now
			result.methodType = sig;
		}
		return result;
	}
	
	public BT_Method findMethod(String name, String sig) {
		if (elementCount == 0)
			return null;
		for (int n = 0; n < elementCount; n++) {
			if (elementData[n].name.equals(name)
				&& elementData[n].getDescriptor().equals(sig)) {
				return elementData[n];
			}
		}
		return null;
	}
	
	/**
	 Finds all methods matching the given name.
	
	 @param mName      The method name.
	 @return           Never null.
	**/
	public BT_MethodVector findMethods(String mName) {
		BT_MethodVector result = null;
		for (int n = 0; n < elementCount; n++) {
			BT_Method m = elementData[n];
			if (m.name.equals(mName)) {
				if(result == null) {
					result = new BT_MethodVector();
				}
				result.addElement(m);
			}
		}
		if(result == null) {
			return emptyMethodVector;
		}
		return result;
	}

	/**
	 Adds the specified object at the end of this BT_MethodVector,
	 unless the vector already contains it.
	 @return  true if it was added.
	**/
	public boolean addUnique(BT_Method object) {
		if (contains(object))
			return false;
		addElement(object);
		return true;
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

	/**
	 Determines whether the collections contain exactly the same elements in the same order.
	 @param  mv  A BT_MethodVector
	**/
	public boolean equals(Object mv) {

		BT_MethodVector that = (BT_MethodVector) mv;
		if (this.elementCount != that.elementCount)
			return false;
		for (int i = 0; i < elementCount; ++i)
			if (this.elementData[i] != that.elementData[i])
				return false;
		return true;
	}
	

}
