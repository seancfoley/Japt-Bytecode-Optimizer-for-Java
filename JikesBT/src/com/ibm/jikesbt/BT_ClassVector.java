package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 BT_ClassVector is a variable size contiguous indexable array of {@link BT_Class}es.
 The size of the BT_ClassVector is the number of BT_Classs it contains.
 The capacity of the BT_ClassVector is the number of BT_Classs it can hold.

 <p> BT_Classs may be inserted at any position up to the size of the
 BT_ClassVector, increasing the size of the BT_ClassVector. BT_Classs at any
 position in the BT_ClassVector may be removed, shrinking the size of
 the BT_ClassVector. BT_Classs at any position in the BT_ClassVector may be replaced,
 which does not affect the BT_ClassVector size.

 <p> The capacity of a BT_ClassVector may be specified when the BT_ClassVector is
 created. If the capacity of the BT_ClassVector is exceeded, the capacity
 is increased, doubling by default.


 <p> The following public members are in addition to the usual Vector methods:
 <sl>
 <li> {@link BT_ClassVector#addUnique( BT_Class object)}
 <li> {@link BT_ClassVector#findClass(String name)}
 <li> {@link BT_ClassVector#sort()}
 <li> {@link BT_ClassVector#sort(boolean descending)}
 </sl>
 * @author IBM
**/
public class BT_ClassVector
	extends BT_Base
	implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_Class[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_Class[] emptyData = new BT_Class[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 *  a utility empty vector
	 */
	public static final BT_ClassVector emptyVector = new BT_ClassVector();
	
	/**
	 * Constructs a new BT_ClassVector using the default capacity.
	 *
	 */
	public BT_ClassVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_ClassVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_ClassVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_ClassVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_ClassVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_ClassVector is full
	 */
	public BT_ClassVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData = (capacity == 0) ? emptyData : new BT_Class[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	
	/**
	 * Constructs a new BT_ClassVector using the given array as its initial backing array.
	 *
	 * @param           classes	the initial classes
	 */
	public BT_ClassVector(BT_Class classes[]) {
		for(int i=0; ; i++) {
			if(i == classes.length || classes[i] == null) {
				elementCount = i;
				break;
			}
		}
		elementData = classes;
		this.capacityIncrement = 0;
	}
	
	/**
	 Adds the specified object at the end of this BT_ClassVector.
	
	 @param           object  the object to add to the BT_ClassVector
	**/
	// A null class or a null class name should not be added.
	public void addElement(BT_Class object) {
		insertElementAt(object, elementCount);
	}
	/**
	 * Answers the number of elements this BT_ClassVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_ClassVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new BT_ClassVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_ClassVector.
	 *
	 *
	 * @return          a shallow copy of this BT_ClassVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_ClassVector vector = (BT_ClassVector) super.clone();
			int length = elementData.length;
			vector.elementData = new BT_Class[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_ClassVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_ClassVector
	 * @return          true if object is an element of this BT_ClassVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(BT_Class object) {
		return indexOf(object, 0) != -1;
	}
	
	public boolean contains(BT_ClassVector objects) {
		for(int i = 0; i < objects.size(); i++) {
			if(!contains(objects.elementAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Copies the elements of this BT_ClassVector into the specified BT_Class array.
	 *
	 *
	 * @param           elements        the BT_Class array into which the elements
	 *                                                  of this BT_ClassVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(BT_Class[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this BT_ClassVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_ClassVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public BT_Class elementAt(int location) {
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers an Enumeration on the elements of this BT_ClassVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_ClassVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_ClassVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_ClassVector can hold the specified number of elements
	 * without growing.
	 *
	 *
	 * @param           minimumCapacity  the minimum number of elements that this
	 *                                  vector will hold before growing
	 *
	 * @see                     #capacity
	 */
	public void ensureCapacity(int minimumCapacity) {
		if (elementData.length < minimumCapacity) {
			grow(minimumCapacity);
		}
	}
	/**
	 * Answers the first element in this BT_ClassVector.
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
	public BT_Class firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_Class newData[] = new BT_Class[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	
	/**
	 * Searches in this BT_ClassVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_ClassVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassVector
	 * @return          the index in this BT_ClassVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Class object) {
		return indexOf(object, 0);
	}
	/**
	 * Searches in this BT_ClassVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_ClassVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_ClassVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Class object, int location) {
		BT_Class element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	/**
	 Inserts the specified object into this BT_ClassVector at the specified
	 location. This object is inserted before any previous element at
	 the specified location. If the location is equal to the size of
	 this BT_ClassVector, the object is added at the end.
	
	
	 @param           object  the object to insert in this BT_ClassVector
	 @param           location        the index at which to insert the element
	
	 @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 @see                     #addElement
	 @see                     #size
	**/
	// A null class or a null class name should not be added.
	public void insertElementAt(BT_Class object, int location) {
		if (CHECK_USER && object == null)
			expect(Messages.getString("JikesBT.Class_should_not_be_null_1"));
		if (CHECK_USER && object.name == null)
			expect(Messages.getString("JikesBT.name__null_2"));
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
	
	public void addAll(BT_ClassVector other) {
		int newElementCount = expandFor(other.elementCount);
		System.arraycopy(other.elementData, 0, elementData, elementCount, other.elementCount);
		elementCount = newElementCount;
	}
	
	public void addAllUnique(BT_ClassVector other) {
		expandFor(other.elementCount);
		for(int i=0; i<other.size(); i++) {
			addUnique(other.elementAt(i));
		}
	}
	
	public void addAllUnique(BT_Class other[]) {
		expandFor(other.length);
		for(int i=0; i<other.length; i++) {
			addUnique(other[i]);
		}
	}
	
	public void addAll(BT_Class other[]) {
		for(int i=0; i<other.length; i++) {
			BT_Class clazz = other[i];
			if(clazz == null) {
				continue;
			}
			addElement(clazz);
		}
	}
	
	/**
	 * Answers if this BT_ClassVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_ClassVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_ClassVector.
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
	public BT_Class lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_ClassVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_ClassVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassVector
	 * @return          the index in this BT_ClassVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Class object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_ClassVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_ClassVector.
	 *
	 *
	 * @param           object  the object to find in this BT_ClassVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_ClassVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Class object, int location) {
		BT_Class element;
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
	 * Removes all elements from this BT_ClassVector, leaving the size zero and
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
	 * BT_ClassVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_ClassVector
	 * @return          true if the specified object was found, false otherwise
	 *
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #size
	 */
	public boolean removeElement(BT_Class object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this BT_ClassVector.
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
	public BT_Class removeElementAt(int location) {
		if (location < elementCount) {
			elementCount--;
			BT_Class result = elementData[location];
			int size = elementCount - location;
			if (size > 0) {
				System.arraycopy(
					elementData,
					location + 1,
					elementData,
					location,
					size);
			}
			elementData[elementCount] = null;
			return result;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 Replaces the element at the specified location in this BT_ClassVector with
	 the specified object.
	
	
	 @param           object  the object to add to this BT_ClassVector
	 @param           location        the index at which to put the specified object
	 @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 @see                     #size
	**/
	// A null class or a null class name should not be added.
	public void setElementAt(BT_Class object, int location) {
		if (CHECK_USER && object == null)
			expect(Messages.getString("JikesBT.Class_should_not_be_null_1"));
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException(Messages.getString("JikesBT.Index___4") + location);
	}
	/**
	 * Sets the size of this BT_ClassVector to the specified size. If there
	 * are more than length elements in this BT_ClassVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_ClassVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_ClassVector
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
	 * Answers the number of elements in this BT_ClassVector.
	 *
	 *
	 * @return          the number of elements in this BT_ClassVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_ClassVector.
	 *
	 *
	 * @return          the string representation of this BT_ClassVector
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
	public BT_Class[] toArray() {
		if(elementCount == 0) {
			return emptyData;
		}
		BT_Class newData[] = new BT_Class[elementCount];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		return newData;
	}
	
	/**
	 * returns an array containing the elements in the vector.  It is the same
	 * array used to store vector elements, so changes can alter the class vector
	 * itself.
	 */
	public BT_Class[] getBackingArray() {
		return elementData;
	}
	
	
	/**
	 * Sets the capacity of this BT_ClassVector to be the same as the size.
	 *
	 *
	 * @see                     #capacity
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public void trimToSize() {
		if (elementData.length != elementCount) {
			elementData = toArray();
		}
	}
	// ---------- End of code common to all JikesBT Vectors ----------

	/**
	 * returns whether the two vectors contain the same instructions, not necessarily in the same order
	 */
	public boolean hasSameClasses(BT_ClassVector other) {
		int otherSize = other.size();
		if(otherSize != size()) {
			return false;
		}
		for(int i=0; i<otherSize; i++) {
			BT_Class otherClass = other.elementAt(i);
			if(!contains(otherClass)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 Adds the specified object at the end of this BT_ClassVector,
	 unless the vector already contains it.
	 @return  true if it was added.
	**/
	// A null class or a null class name should not be added.
	public boolean addUnique(BT_Class object) {
		if (contains(object))
			return false;
		addElement(object);
		return true;
	}
	/**
	 Searches in this BT_ClassVector for the element with the specified name.
	
	
	 @param           name    the name of the object to find in this BT_ClassVector
	 @return          the element in this BT_ClassVector with the specified name, null if the
	   element isn't found
	
	
	 @see                     #contains
	**/
	// A name should not be null.
	public BT_Class findClass(String name) {
		if (CHECK_USER && name == null)
			expect(Messages.getString("JikesBT.name__null_2"));
		for (int i = elementCount - 1; i >= 0; --i) {
			BT_Class element = elementData[i];
			if (element.name.equals(name))
				return element;
		}
		return null;
	}

	/**
	 Sorts the classes in this BT_ClassVector into collating sequence.
	
	
	 @return         the sorted BT_ClassVector
	
	 @see            #elements
	**/
	// <em>warning</em> -- no longer returns the result to
	//     make it clearer that this sorts the vector without cloning it.
	public void sort() {
		sort(false);
	}

	/**
	 Sorts the classes in this BT_ClassVector into ascending or
	 descending collating sequence.
	
	
	 @param           reverse whether or not the classes are to be
	   sorted in descending sequence.
	
	 @return          the sorted BT_ClassVector
	
	 @see                     #elements
	**/
	//  <em>warning</em> -- no longer returns the result to
	//     make it clearer that this sorts the vector without cloning it.
	public void sort(boolean descending) {
		if(descending) {
			sort(new BT_ClassComparator() {
				public int compare(BT_Class class1, BT_Class class2) {
					return class2.compareTo(class1);
				}
			});
		} else {
			Arrays.sort(elementData, 0, elementCount);
		}
	}
	
	public static Comparator convertComparator(final BT_ClassComparator comparator) {
		return new Comparator() {
			public int compare(Object o1, Object o2) {
				return comparator.compare((BT_Class) o1, (BT_Class) o2);
			}
		};
	}

	/**
	 Sorts the classes in this BT_ClassVector using a given BT_ClassComparator.
	**/
	public void sort(final BT_ClassComparator comparator) {
		Arrays.sort(elementData, 0, elementCount, convertComparator(comparator));
	}

}