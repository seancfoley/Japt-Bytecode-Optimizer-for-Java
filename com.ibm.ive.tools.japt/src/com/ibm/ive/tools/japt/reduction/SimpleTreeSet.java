package com.ibm.ive.tools.japt.reduction;

import com.ibm.jikesbt.*;

/**
 * @author sfoley
 *
 * A simple set designed for purposes of reduction.
 * 
 * Once added to the set, objects are never removed.  The set has a binary lookup, and uses
 * a minimum amount of memory.
 * 
 * This class is not thread-safe, it is meant for single thread usage only.
 */
public class SimpleTreeSet implements ClassSet {

	TreeSetEntry root;
	private Iterator iterator;
	private int size;
	
	/**
	 * Constructor for SimpleTreeSet.
	 */
	public SimpleTreeSet() {}
	
	private class TreeSetEntry {
		TreeSetEntry(BT_Class value) {
			if(value == null) {
				throw new NullPointerException("Null class entry in SimpleTreeSet");
			}
			this.value = value;
		}
		
		TreeSetEntry left, right;
		BT_Class value;
	}

	public void add(BT_Class clazz) {
		if(root == null) {
			root = new TreeSetEntry(clazz);
			size = 1;
			return;
		}
		if(!check(root, clazz, false)) {
			size++;
		}
	}
	
	public void removeAll() {
		root = null;
		size = 0;
	}
	
	public int size() {
		return size;
	}
	
	/**
	 * adds the given BT_Class object to the set, if it is not already contained with the set,
	 * or checks whether the set contains the object.
	 * @param contains if true, the method checks whether the set contains the object.  If false, the object will be added.
	 * @param node the root node to search from
	 * @param clazz the object to add or search for
	 * @return whether the object already existed within the set
	 */
	private boolean check(TreeSetEntry node, BT_Class clazz, boolean contains) {
		while(true) {
			int comparison = node.value.hashCode() - clazz.hashCode();
			if(comparison == 0) {
				if(node.value.equals(clazz)) {
					//already in set
					return true;
				}
			}
			TreeSetEntry nextNode;
			if(comparison <= 0) {
				nextNode = node.left;
				if(nextNode == null) {
					if(!contains) {
						node.left = new TreeSetEntry(clazz);
					}
					return false;
				}
			}
			else {
				nextNode = node.right;
				if(nextNode == null) {
					if(!contains) {
						node.right = new TreeSetEntry(clazz);
					}
					return false;
				}
			}
			node = nextNode;
		}
	}
	
	
	public ClassIterator iterator() {
		if(iterator == null) {
			iterator = new Iterator();
		}
		else {
			iterator.reset();
		}
		return iterator;
	}
	
	class Iterator implements ClassIterator {
		
		java.util.Stack unVisited = new java.util.Stack();
		TreeSetEntry current = root;
		
		public boolean hasNext() {
			return current != null;
		}

		public BT_Class next() {
			if(current == null) {
				throw new java.util.NoSuchElementException();
			}
			BT_Class result = current.value;
			TreeSetEntry left = current.left;
			TreeSetEntry right = current.right;
			if(left != null) {
				current = left;
				if(right != null) {
					unVisited.push(right);
				}
			}
			else if(right != null) {
				current = right;
			}
			else if(unVisited.isEmpty()) {
				current = null;
			}
			else {
				current = (TreeSetEntry) unVisited.pop();
			}
			return result;
		}
		
		void reset() {
			current = root;
			unVisited.clear();
		}
	}
	
	public boolean isEmpty() {
		return root == null;
	}
	
	public boolean contains(BT_Class object) {
		if(object == null) {
			throw new NullPointerException("invalid containment check");
		}
		return (root != null) && check(root, object, true);
	}
		
}
