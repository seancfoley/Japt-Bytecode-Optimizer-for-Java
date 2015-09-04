package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 Implements java.util.Enumeration over an array.
 * @author IBM
**/
public final class BT_ArrayEnumerator implements Enumeration {
	Object array[];
	int start, end;
	public BT_ArrayEnumerator(Object array[], int length) {
		this.start = 0;
		this.end = length;
		this.array = array;
	}
	public boolean hasMoreElements() {
		return start < end;
	}
	public Object nextElement() {
		if (start < end)
			return array[start++];
		throw new NoSuchElementException();
	}
}