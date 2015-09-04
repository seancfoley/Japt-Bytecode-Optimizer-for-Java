package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 BT_AttributeVector is a variable size contiguous indexable array of {@link BT_Attribute}s.
 The size of the BT_AttributeVector is the number of BT_Attributes it contains.
 The capacity of the BT_AttributeVector is the number of BT_Attributes it can hold.

 <p> BT_Attributes may be inserted at any position up to the size of the
 BT_AttributeVector, increasing the size of the BT_AttributeVector. BT_Attributes at any
 position in the BT_AttributeVector may be removed, shrinking the size of
 the BT_AttributeVector. BT_Attributes at any position in the BT_AttributeVector may be replaced,
 which does not affect the BT_AttributeVector size.

 <p> The capacity of a BT_AttributeVector may be specified when the BT_AttributeVector is
 created. If the capacity of the BT_AttributeVector is exceeded, the capacity
 is increased, doubling by default.


 <p> The following public members are in addition to the usual Vector methods:
 <sl>
 <li> {@link BT_AttributeVector#getAttribute(String name)}
 <li> {@link BT_AttributeVector#removeAttribute(String name)}
 <li> {@link BT_AttributeVector#print( java.io.PrintStream ps, String prefix)}
 </sl>
 * @author IBM
**/
public final class BT_AttributeVector implements Cloneable, Serializable {
	/**
	 * The number of elements or the size of the vector.
	 */
	protected int elementCount;
	/**
	 * The elements of the vector.
	 */
	protected BT_Attribute[] elementData;
	/**
	 * The amount by which the capacity of the vector is increased.
	 */
	protected int capacityIncrement;

	public final static BT_AttributeVector emptyVector = new BT_AttributeVector(0);
	
	/**
	 * Initial empty value for elementData.
	 */
	private final static BT_Attribute[] emptyData = new BT_Attribute[0];
	private static final int DEFAULT_SIZE = 0;

	/**
	 * Constructs a new BT_AttributeVector using the default capacity.
	 *
	 */
	public BT_AttributeVector() {
		this(DEFAULT_SIZE, 0);
	}
	/**
	 * Constructs a new BT_AttributeVector using the specified capacity.
	 *
	 *
	 * @param           capacity        the initial capacity of the new vector
	 */
	public BT_AttributeVector(int capacity) {
		this(capacity, 0);
	}
	/**
	 * Constructs a new BT_AttributeVector using the specified capacity and
	 * capacity increment.
	 *
	 *
	 * @param           capacity        the initial capacity of the new BT_AttributeVector
	 * @param           capacityIncrement       the amount to increase the capacity
	                                        when this BT_AttributeVector is full
	 */
	public BT_AttributeVector(int capacity, int capacityIncrement) {
		elementCount = 0;
		elementData = (capacity == 0) ? emptyData : new BT_Attribute[capacity];
		this.capacityIncrement = capacityIncrement;
	}
	/**
	 * Adds the specified object at the end of this BT_AttributeVector.
	 *
	 *
	 * @param           object  the object to add to the BT_AttributeVector
	 */
	public void addElement(BT_Attribute object) {
		insertElementAt(object, elementCount);
	}
	
	/**
	 * Adds the specified object at the end of this BT_AttributeVector,
	 * if it not already contained within this vector.
	 *
	 *
	 * @param           object  the object to add to the BT_AttributeVector
	 */
	public void addUnique(BT_Attribute object) {
		if(!contains(object)) {
			addElement(object);
		}
	}
	
	/**
	 * Replaces the element at the specified location in this BT_InsVector with
	 * a clone of itself.
	 *
	 *
	 * @param           location        the index at which to clone the instruction
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 * @return the clone
	 * @see                     #size
	 */
	public BT_Attribute cloneElementAt(int location) {
		if (location < elementCount) {
			BT_Attribute clone = (BT_Attribute) elementData[location].clone();
			elementData[location] = clone;
			return clone;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	/**
	 * Answers the number of elements this BT_AttributeVector can hold without
	 * growing.
	 *
	 *
	 * @return          the capacity of this BT_AttributeVector
	 *
	 * @see                     #ensureCapacity
	 * @see                     #size
	 */
	public int capacity() {
		return elementData.length;
	}
	/**
	 * Answers a new BT_AttributeVector with the same elements, size, capacity
	 * and capacityIncrement as this BT_AttributeVector.
	 *
	 *
	 * @return          a shallow copy of this BT_AttributeVector
	 *
	 * @see                     java.lang.Cloneable
	 */
	public Object clone() {
		try {
			BT_AttributeVector vector = (BT_AttributeVector) super.clone();
			int length = elementData.length;
			vector.elementData = new BT_Attribute[length];
			System.arraycopy(elementData, 0, vector.elementData, 0, length);
			return vector;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	/**
	 * Searches this BT_AttributeVector for the specified object.
	 *
	 *
	 * @param           object  the object to look for in this BT_AttributeVector
	 * @return          true if object is an element of this BT_AttributeVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(BT_Attribute object) {
		return indexOf(object, 0) != -1;
	}
	
	/**
	 * Searches this BT_AttributeVector for an attribute with the specified attribute name.
	 *
	 *
	 * @param           name  the object to look for in this BT_AttributeVector
	 * @return          true if object is an element of this BT_AttributeVector, false otherwise
	 *
	 * @see                     #indexOf
	 * @see                     java.lang.Object#equals
	 */
	public boolean contains(String name) {
		return indexOf(name, 0) != -1;
	}
	
	/**
	 * Copies the elements of this BT_AttributeVector into the specified BT_Attribute array.
	 *
	 *
	 * @param           elements        the BT_Attribute array into which the elements
	 *                                                  of this BT_AttributeVector are copied
	 *
	 * @see                     #clone
	 */
	public void copyInto(BT_Attribute[] elements) {
		System.arraycopy(elementData, 0, elements, 0, elementCount);
	}
	/**
	 * Answers the element at the specified location in this BT_AttributeVector.
	 *
	 *
	 * @param           location        the index of the element to return in this BT_AttributeVector
	 * @return          the element at the specified location
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public BT_Attribute elementAt(int location) {
		//BB fast range check, no need to check for < 0 as the array access will throw the exception
		if (location < elementCount) {
			return elementData[location];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Answers an Enumeration on the elements of this BT_AttributeVector. The
	 * results of the Enumeration may be affected if the contents
	 * of this BT_AttributeVector are modified.
	 *
	 *
	 * @return          an Enumeration of the elements of this BT_AttributeVector
	 *
	 * @see                     #elementAt
	 * @see                     Enumeration
	 */
	public Enumeration elements() {
		return new BT_ArrayEnumerator(elementData, elementCount);
	}
	/**
	 * Ensures that this BT_AttributeVector can hold the specified number of elements
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
	 * Answers the first element in this BT_AttributeVector.
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
	public BT_Attribute firstElement() {
		if (elementCount == 0)
			throw new NoSuchElementException();
		return elementData[0];
	}
	private void grow(int newCapacity) {
		BT_Attribute newData[] = new BT_Attribute[newCapacity];
		System.arraycopy(elementData, 0, newData, 0, elementCount);
		elementData = newData;
	}
	/**
	 * Searches in this BT_AttributeVector for the index of the specified object. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_AttributeVector.
	 *
	 *
	 * @param           object  the object to find in this BT_AttributeVector
	 * @return          the index in this BT_AttributeVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Attribute object) {
		return indexOf(object, 0);
	}
	
	/**
	 * Searches in this BT_AttributeVector for the index of an attribute with the specified name. The
	 * search for the object starts at the beginning and moves towards the
	 * end of this BT_AttributeVector.
	 *
	 *
	 * @param           object  the object to find in this BT_AttributeVector
	 * @return          the index in this BT_AttributeVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(String name) {
		return indexOf(name, 0);
	}
	
	/**
	 * Searches in this BT_AttributeVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_AttributeVector.
	 *
	 *
	 * @param           object  the object to find in this BT_AttributeVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_AttributeVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(BT_Attribute object, int location) {
		BT_Attribute element;
		for (int i = location; i < elementCount; i++) {
			if ((element = elementData[i]) == object)
				return i;
			if ((element != null) && (element.equals(object)))
				return i;
		}
		return -1;
	}
	
	/**
	 * Searches in this BT_AttributeVector for the index of an attribute with the given name. The
	 * search for the object starts at the specified location and moves
	 * towards the end of this BT_AttributeVector.
	 *
	 *
	 * @param           name  the name of the attribute to find in this BT_AttributeVector
	 * @param           location        the index at which to start searching
	 * @return          the index in this BT_AttributeVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0
	 *
	 * @see                     #contains
	 * @see                     #lastIndexOf
	 */
	public int indexOf(String name, int location) {
		for (int i = location; i < elementCount; i++) {
			BT_Attribute element = elementData[i];
			if(element != null && element.getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Inserts the specified object into this BT_AttributeVector at the specified
	 * location. This object is inserted before any previous element at
	 * the specified location. If the location is equal to the size of
	 * this BT_AttributeVector, the object is added at the end.
	 *
	 *
	 * @param           object  the object to insert in this BT_AttributeVector
	 * @param           location        the index at which to insert the element
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || > size()
	 *
	 * @see                     #addElement
	 * @see                     #size
	 */
	public void insertElementAt(BT_Attribute object, int location) {
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
	 * Answers if this BT_AttributeVector has no elements, a size of zero.
	 *
	 *
	 * @return          true if this BT_AttributeVector has no elements, false otherwise
	 *
	 * @see                     #size
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}
	/**
	 * Answers the last element in this BT_AttributeVector.
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
	public BT_Attribute lastElement() {
		try {
			return elementData[elementCount - 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * Searches in this BT_AttributeVector for the index of the specified object. The
	 * search for the object starts at the end and moves towards the start
	 * of this BT_AttributeVector.
	 *
	 *
	 * @param           object  the object to find in this BT_AttributeVector
	 * @return          the index in this BT_AttributeVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Attribute object) {
		return lastIndexOf(object, elementCount - 1);
	}
	/**
	 * Searches in this BT_AttributeVector for the index of the specified object. The
	 * search for the object starts at the specified location and moves
	 * towards the start of this BT_AttributeVector.
	 *
	 *
	 * @param           object  the object to find in this BT_AttributeVector
	 * @param           location      the index at which to start searching
	 * @return          the index in this BT_AttributeVector of the specified element, -1 if the
	 *                          element isn't found
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location >= size()
	 *
	 * @see                     #contains
	 * @see                     #indexOf
	 */
	public int lastIndexOf(BT_Attribute object, int location) {
		BT_Attribute element;
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
	 * Removes all elements from this BT_AttributeVector, leaving the size zero and
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
	 * BT_AttributeVector.
	 *
	 *
	 * @param           object  the object to remove from this BT_AttributeVector
	 * @return          true if the specified object was found, false otherwise
	 * @see                     #removeAllElements
	 * @see                     #removeElementAt
	 * @see                     #removeAttribute(String name)
	 * @see                     #size
	 */
	public boolean removeElement(BT_Attribute object) {
		int index;
		if ((index = indexOf(object, 0)) == -1)
			return false;
		removeElementAt(index);
		return true;
	}
	/**
	 * Removes the element at the specified location from this BT_AttributeVector.
	 *
	 *
	 * @param           location        the index of the element to remove
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 * @see                     #removeElement
	 * @see                     #removeAllElements
	 * @see                     #removeAttribute(String name)
	 * @see                     #size
	 */
	public void removeElementAt(int location) {
		// fast range check, no need to check < 0 as the arraycopy will throw the exception
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
	 * Replaces the element at the specified location in this BT_AttributeVector with
	 * the specified object.
	 *
	 *
	 * @param           object  the object to add to this BT_AttributeVector
	 * @param           location        the index at which to put the specified object
	 *
	 * @exception       ArrayIndexOutOfBoundsException when location < 0 || >= size()
	 *
	 * @see                     #size
	 */
	public void setElementAt(BT_Attribute object, int location) {
		//BB fast range check, no need to check < 0 as the array access will throw the exception
		if (location < elementCount) {
			elementData[location] = object;
			return;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/**
	 * Sets the size of this BT_AttributeVector to the specified size. If there
	 * are more than length elements in this BT_AttributeVector, the elements
	 * at end are lost. If there are less than length elements in
	 * the BT_AttributeVector, the additional elements contain null.
	 *
	 *
	 * @param           length  the new size of this BT_AttributeVector
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
	 * Answers the number of elements in this BT_AttributeVector.
	 *
	 *
	 * @return          the number of elements in this BT_AttributeVector
	 *
	 * @see                     #elementCount
	 * @see                     #lastElement
	 */
	public int size() {
		return elementCount;
	}
	/**
	 * Answers the string representation of this BT_AttributeVector.
	 *
	 *
	 * @return          the string representation of this BT_AttributeVector
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
	 * Sets the capacity of this BT_AttributeVector to be the same as the size.
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
	 Return the specified BT_Attribute, or null if none.
	 @param  name  The name of the attribute of interest.
	 @see #removeAttribute(String name)
	**/
	public BT_Attribute getAttribute(String name) {
		for (int n = 0; n < size(); n++)
			if (elementData[n].getName().equals(name))
				return elementData[n];
		return null;
	}

	/**
	 Delete and return the first BT_Attribute with a matching name, or null if none.
	 @param  name  The name of the attribute of interest.
	 @see     #getAttribute(String name)
	 @see     #removeElement
	 @see     #removeElementAt
	 @return  The removed BT_Attribute or null if none.
	**/
	public BT_Attribute removeAttribute(String name) {
		for (int n = 0; n < size(); n++) {
			if (elementData[n].getName().equals(name)) {

				BT_Attribute att = elementData[n];
				removeElementAt(n);
				return att;
			}
		}
		return null;
	}

	/**
	 Prints the elements one per line.
	 @param  prefix  A string to be printed before each attribute.
	**/
	public void print(java.io.PrintStream ps, String prefix) {
		for (int i = 0; i < elementCount; ++i)
			elementData[i].print(ps, prefix);
	}

	/**
	 Reads all the attributes from class-file format.
	 Attributes in system classes are ignored.
	 @param container  The BT_Class, BT_CodeAttribute, BT_Field, or BT_Method
	   that contains this attribute.
	**/
	// Class-file format:
	//       ...
	//       u2 attributes_count;
	//       attribute_info attributes[attributes_count];
	//       ...
	static BT_AttributeVector read(DataInputStream di, 
			BT_ConstantPool pool, 
			BT_AttributeOwner container, 
			BT_Item item, 
			BT_Repository rep,
			LoadLocation loadedFrom)
		throws BT_ClassFileException, IOException {
		int count = di.readUnsignedShort(); // attributes_count
		BT_AttributeVector v = new BT_AttributeVector(count);
		for (int i = 0; i < count; i++) {
			try {
				BT_Attribute at = BT_Attribute.read(di, pool, container, rep.factory, loadedFrom);
				if (at != null) {
					String atName = at.getName();
					if (at.singletonRequired() && v.getAttribute(atName) != null) {
						throw new BT_AttributeException(atName,
							Messages.getString("JikesBT.duplicate_{0}_attribute", atName));
					}
					v.addElement(at);
				}
			} catch(BT_AttributeException e) {
				rep.factory.noteAttributeLoadFailure(rep, item, e.getAttributeName(), null, e, loadedFrom);
			} 
		}
		return v;
	}
	
	/**
	 Convert the representation to directly reference related objects
	 (instead of using class-file artifacts such as indices and
	 offsets to identify them).
	 For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>.
	**/
	void dereference(BT_Item item, BT_Repository rep) throws BT_ClassFileException {
		for (int i = 0; i < size(); i++) {
			BT_Attribute att = elementAt(i);
			try {
				att.dereference(rep);
				//BT_AttributeException means the attribute is bogus but the class is not necessarily bad
				//BT_ClassFileException means the whole class is bogus
			} catch(BT_AttributeException e) {
				try {
					rep.factory.noteAttributeLoadFailure(rep, item, att.getName(), att, e, att.loadedFrom);
				} finally {
					//we remove the invalid attribute
					removeElementAt(i--);
				}
			} catch(BT_ClassFileException e) {
				removeElementAt(i--);
				throw e;
			}
		}
	}
	
	public void remove() {
		BT_AttributeVector clone = (BT_AttributeVector) clone();
		for (int i = 0; i < size(); i++) {
			BT_Attribute att = clone.elementAt(i);
			att.remove();
		}
	}
	
	/**
	 * Undo any reference to the given item.
	 */
	void removeReference(BT_Item reference) {
		BT_AttributeVector clone = (BT_AttributeVector) clone();
		for (int i = 0; i < size(); i++) {
			BT_Attribute att = clone.elementAt(i);
			att.removeReference(reference);
		}
	}

	/**
	 * @see BT_CodeAttribute#changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns)
	 @param switchingAttributes true if oldIns and newIns are not in the same code attribute
	 */
	void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching) {
		for (int i = 0; i < size(); i++)
			elementAt(i).changeReferencesFromTo(oldIns, newIns, switching);
	}

	public void resolve(BT_Item item, BT_ConstantPool pool) throws BT_ClassWriteException {
		resolve(item, pool, null);
	}
	
	/**
	 Touches everything in this area that is needed to build a
	 constant pool in preparation for writing the pool.
	 For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#resolve_method>resolve method</a>.
	 * @exception BT_ClassFileException when an important attribute in the vector could not be resolved
	 **/
	public void resolve(BT_Item item, BT_ConstantPool pool, BT_AttributeVector skip) throws BT_ClassWriteException {
		for (int i = 0; i < size(); i++) {
			BT_Attribute att = elementAt(i);
			if(skip != null && skip.contains(att)) {
				continue;
			}
			try {
				att.resolve(pool);
			} catch(BT_AttributeException e) {
				try {
					pool.getRepository().factory.noteAttributeWriteFailure(item, att, e);
				} finally {
					//we remove the invalid attribute
					removeElementAt(i--);
				}
			}
		}
	}

	static class WrittenAttributesLength {
		int count;
		int writtenLength;
	}
	
	private WrittenAttributesLength wal = new WrittenAttributesLength();
	
	/**
	 * Return the number of bytes that write(...) will write.
	 * Resolve must be called just prior to calling this method.
	 * @exception BT_ClassFileException when the length could not be calculated
	 */
	WrittenAttributesLength writtenLength() {
		int nb = 2; // For "attributes_count"
		int count = 0;
		for (int i = 0; i < size(); i++) {
			int len = elementAt(i).writtenLength();
			if(len > 0) {
				count++;
				nb += len;
			}
		}
		wal.count = count;
		wal.writtenLength = nb;
		return wal;
	}

	public void write(java.io.DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		write(size(), dos, pool);
	}
	
	/**
	 * Writes all the attributes in class-file format.
	 * Resolve must be called just prior to calling this method.
	 * @exception java.io.IOException when writing fails.
	 * @exception BT_ClassFileException when an important attributes could not be written
	 */
	public void write(int count, java.io.DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(count); 
		for (int i = 0; i < size(); i++) {
			BT_Attribute att = elementAt(i);
			//attributes of length 0 (ie they are not part of the count) will write nothing
			att.write(dos, pool);
		}
	}
}