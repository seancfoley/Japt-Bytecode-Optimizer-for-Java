package com.ibm.ive.tools.japt.reduction.ita;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;



public class ObjectSet implements Set, Cloneable {
	static final ObjectSet EMPTY_SET = new ObjectSet();
	private int size;
	private Entry root;
	
	public Object clone() {
		try {
			ObjectSet clone = (ObjectSet) super.clone();
			if (root != null) {
				clone.root = root.copy();
			}
			return clone;
		} catch (CloneNotSupportedException e) {return null;}
	}
		
	public boolean addAll(Collection set) {
		Iterator iterator = set.iterator();
		int origSize = size();
		while(iterator.hasNext()) {
			add(iterator.next());
		}
		return size() != origSize;
	}
	
	public boolean retainAll(Collection set) {
		return changeAll(set, false);
	}
	
	public boolean removeAll(Collection set) {
		return changeAll(set, true);
	}
	
	private boolean changeAll(Collection set, boolean remove) {
		int origSize = size();
		ObjectSet toRemove = new ObjectSet();
		Iterator iterator = iterator();
		while(iterator.hasNext()) {
			Object o = iterator.next();
			boolean contains = set.contains(o);
			if(remove ? contains : !contains) {
				toRemove.add(o);
			}
		}
		iterator = toRemove.iterator();
		while(iterator.hasNext()) {
			Object o = iterator.next();
			remove(o);
		}
		return size() != origSize;
	}
	
	public Object[] toArray() {
		return toArray(new Comparable[size()]);
	}
	
	public Object[] toArray(Object[] a) {
		if(a.length < size()) {
			a = (Object[]) Array.newInstance(a.getClass().getComponentType(), size);
		}
		Iterator iterator = iterator();
		int i = 0;
		while(iterator.hasNext()) {
			a[i++] = iterator.next();
		}
		return a;
	}
	
	public boolean add(ObjectSetEntry object) {
		return put(object, true, null);
	}
	
	public boolean add(Object object) {
		return add((ObjectSetEntry) object);
	}
	
	public void clear() {
		root = null;
		size = 0;
	}
	
	public boolean containsAll(Collection collection) {
		Iterator iterator = collection.iterator();
		
		while(iterator.hasNext()) {
			Object o = iterator.next();
			if(!contains(o)) {
				return false;
			}
		}
		return true;
	}
	
	public ObjectSetEntry find(ObjectSetEntry object) {
		Entry entry = doFind(object);
		if(entry != null) {
			return entry.getKey();
		}
		return null;
	}
	
	public boolean contains(Object object) {
		return contains((ObjectSetEntry) object);
	}
	
	public boolean contains(ObjectSetEntry object) {
		return doFind(object) != null;
	}
	
	public ObjectSetEntry get(ObjectSetEntry object) {
		Entry entry = doFind(object);
		if(entry != null) {
			return entry.getKey();
		}
		return null;
	}
	
	public boolean isEmpty() {
		return root == null;
	}
	
	static final Iterator emptyIterator = new Iterator() {
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		public boolean hasNext() {
			return false;
		}

		public Object next() {
			throw new java.util.NoSuchElementException();
		}
	};
	
	public Iterator iterator() {
		if(isEmpty()) {
			return emptyIterator;
		}
		
		return new Iterator() {
			ArrayList unVisited = new ArrayList();
			Entry current = root;
			
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			public boolean hasNext() {
				return current != null;
			}
	
			public Object next() {
				if(current == null) {
					throw new java.util.NoSuchElementException();
				}
				Object result = current.getKey();
				Entry left = current.getLeft();
				Entry right = current.getRight();
				if(left != null) {
					current = left;
					if(right != null) {
						unVisited.add(right);
					}
				} else if(right != null) {
					current = right;
				} else if(unVisited.isEmpty()) {
					current = null;
				} else {
					current = (Entry) unVisited.remove(unVisited.size() - 1);
				}
				return result;
			}
		};
	}
	
	public boolean remove(Object object) {
		return remove((ObjectSetEntry) object);
	}
	
	public boolean remove(ObjectSetEntry object) {
		return doRemove(object) != null;
	}
	
	public int size() {
		return size;
	}
	
	static interface Entry extends Comparable {
		ObjectSetEntry getKey();
		
		Entry getLeft();
		
		Entry getRight();
		
		Entry attachLeft(Entry attached);
		
		Entry attachRight(Entry attached);
		
		void setLeft(Entry attached);
		
		void setRight(Entry attached);
		
		Entry detachLeft();
		
		Entry detachRight();
		
		Entry copy();
		
		int getChildCount();
		
	}
	
	public static abstract class ObjectSetEntry implements Entry {
		final public Entry copy() {
			return this;
		}
		
		final public ObjectSetEntry getKey() {
			return this;
		}
		
		final public int getChildCount() {
			return 0;
		}
		
		final public Entry getLeft() {
			return null;
		}
		
		final public Entry getRight() {
			return null;
		}
		
		final public Entry attachLeft(Entry attached) {
			return new EntryWithLeft(this, attached);
		}
		
		final public Entry attachRight(Entry attached) {
			return new EntryWithRight(this, attached);
		}
		
		final public void setLeft(Entry attached) {
			throw new UnsupportedOperationException();
		}
		
		final public void setRight(Entry attached) {
			throw new UnsupportedOperationException();
		}
		
		final public Entry detachLeft() {
			throw new UnsupportedOperationException();
		}
		
		final public Entry detachRight() {
			throw new UnsupportedOperationException();
		}
	}
	
	static class EntryWithRight implements Entry {
		private final ObjectSetEntry key;
		private Entry right;
		
		EntryWithRight(ObjectSetEntry key) {
			this.key = key;
		}
		
		EntryWithRight(ObjectSetEntry key, Entry attached) {
			this.key = key;
			this.right = attached;
		}
		
		public Entry copy() {
			EntryWithRight entry = new EntryWithRight(key);
			entry.right = right.copy();
			return entry;
		}
		
		public Entry getLeft() {
			return null;
		}
		
		public Entry detachLeft() {
			throw new UnsupportedOperationException();
		}
		
		public Entry attachRight(Entry attached) {
			throw new UnsupportedOperationException();
		}
		
		public int getChildCount() {
			return 1;
		}
		
		public ObjectSetEntry getKey() {
			return key;
		}
		
		public Entry getRight() {
			return right;
		}
		
		public void setLeft(Entry attached) {
			throw new UnsupportedOperationException();
		}
		
		public void setRight(Entry attached) {
			if(attached == null) {
				throw new NullPointerException();
			}
			right = attached;
		}
		
		public Entry attachLeft(Entry attached) {
			return new FullEntry(key, attached, right);
		}
		
		public Entry detachRight() {
			return key;
		}
		
		public int compareTo(Object obj) {
			return key.compareTo(obj);
		}
	}
	
	static class EntryWithLeft implements Entry {
		private final ObjectSetEntry key;
		private Entry left;
		
		EntryWithLeft(ObjectSetEntry key) {
			this.key = key;
		}
		
		EntryWithLeft(ObjectSetEntry key, Entry attached) {
			this.key = key;
			this.left = attached;
		}
		
		public Entry copy() {
			EntryWithLeft entry = new EntryWithLeft(key);
			entry.left = left.copy();
			return entry;
		}
		
		public int getChildCount() {
			return 1;
		}
		
		public ObjectSetEntry getKey() {
			return key;
		}
		
		public Entry getLeft() {
			return left;
		}
		
		public Entry getRight() {
			return null;
		}
		
		public Entry detachRight() {
			throw new UnsupportedOperationException();
		}
		
		public Entry attachLeft(Entry attached) {
			throw new UnsupportedOperationException();
		}
		
		public Entry attachRight(Entry attached) {
			return new FullEntry(key, left, attached);
		}
		
		public Entry detachLeft() {
			return key;
		}
		
		public int compareTo(Object obj) {
			return key.compareTo(obj);
		}

		public void setLeft(Entry attached) {
			if(attached == null) {
				throw new NullPointerException();
			}
			left = attached;
		}
		
		public void setRight(Entry attached) {
			throw new UnsupportedOperationException();
		}
	}
	
	static class FullEntry implements Entry {
		private final ObjectSetEntry key;
		private Entry left, right;
		
		FullEntry(ObjectSetEntry key) {
			this.key = key;
		}
		
		FullEntry(ObjectSetEntry key, Entry left, Entry right) {
			this.key = key;
			this.left = left;
			this.right = right;
		}
		
		public Entry copy() {
			FullEntry entry = new FullEntry(key);
			entry.left = left.copy();
			entry.right = right.copy();
			return entry;
		}
		
		public Entry attachLeft(Entry attached) {
			throw new UnsupportedOperationException();
		}
		
		public Entry attachRight(Entry attached) {
			throw new UnsupportedOperationException();
		}
		
		public int getChildCount() {
			return 2;
		}
		
		public ObjectSetEntry getKey() {
			return key;
		}
		
		public Entry getLeft() {
			return left;
		}
		
		public Entry getRight() {
			return right;
		}
		
		public Entry detachLeft() {
			return new EntryWithRight(key, right);
		}
		
		public Entry detachRight() {
			return new EntryWithLeft(key, left);
		}
		
		public int compareTo(Object obj) {
			return key.compareTo(obj);
		}

		public void setLeft(Entry attached) {
			if(attached == null) {
				throw new NullPointerException();
			}
			left = attached;
		}
		
		public void setRight(Entry attached) {
			if(attached == null) {
				throw new NullPointerException();
			}
			right = attached;
		}
	}
	
	private void putBack(Entry entry) {
		if(entry == null) {
			return;
		}
		ObjectSetEntryHolder holder = new ObjectSetEntryHolder();
		put(entry, false, holder);
		ObjectSetEntry toAdd = holder.toAdd;
		if(toAdd != null) {
			put(toAdd, false, null);
		}
	}
	
	static class ObjectSetEntryHolder {
		ObjectSetEntry toAdd;
	}
	
	/**
	 * 
	 * @param key
	 * @param isNew
	 * @param holder can be null if leaf has no children.  
	 * Otherwise, it will store an ObjectSetEntry that needs to be re-added.
	 * @return
	 */
	private boolean put(Entry key, boolean isNew, ObjectSetEntryHolder holder) {
		Entry x = root;
		if(x == null) {
			size = 1;
			root = key;
			return true;
		}
		int result = 0, yResult = 0, parentResult;
		Entry y = null, parent = null, topParent;
		do {
			topParent = parent;
			parent = y;
			y = x;
			parentResult = yResult;
			yResult = result;
			result = key.compareTo(x.getKey());
			if (result == 0) {
				return false;
			}
			x = result < 0 ? x.getLeft() : x.getRight();
		} while (x != null);
		if(isNew) {
			size++;
		}
		Entry newY = (result < 0) ? y.attachLeft(key) : y.attachRight(key);
		if(parent == null) {
			root = newY;
		} else {
			if(yResult < 0) {
				parent.setLeft(newY);
			} else {
				parent.setRight(newY);
			}
		}
		if(newY.getChildCount() == 1 && parent != null && parent.getChildCount() == 1) {
			balance(key, result, newY, yResult, parent, parentResult, topParent, holder);
		}
		return true;
	}
	
	
	private void balance(Entry leaf, 
			int leafDirection, 
			Entry node, 
			int nodeDirection, 
			Entry parent, 
			int parentDirection, 
			Entry parentParent,
			ObjectSetEntryHolder holder) {
		Entry newParent;
		if(nodeDirection < 0) {
			if(leafDirection < 0) {
				//node will be newParent
				newParent = new FullEntry(node.getKey(), leaf.getKey(), parent.getKey());
			} else {
				if(leaf.getChildCount() > 0) {
					newParent = leaf;
					holder.toAdd = node.getKey();
				} else {
					//leaf will be new Parent
					newParent = new FullEntry(leaf.getKey(), node.getKey(), parent.getKey());
				}
			}
		} else {
			if(leafDirection > 0) {
				//node will be newParent
				newParent = new FullEntry(node.getKey(), parent.getKey(), leaf.getKey());
			} else {
				if(leaf.getChildCount() > 0) {
					newParent = leaf;
					holder.toAdd = node.getKey();
				} else {
					//leaf will be new Parent
					newParent = new FullEntry(leaf.getKey(), parent.getKey(), node.getKey());
				}
			}
		}
		if(parent == root) {
			root = newParent;
		} else {
			if(parentDirection < 0) {
				parentParent.setLeft(newParent);
			} else {
				parentParent.setRight(newParent);
			}
		}
	}
	
	private Object doRemove(ObjectSetEntry key) {
		int result = 0;
		int lastResult = 0;
		Entry x = root;
		Entry y = null;
		Entry z = null;
		int prev;
		while (x != null) {
			prev = lastResult;
			lastResult = result;
			result = key.compareTo(x.getKey());
			if (result == 0) {
				size--;
				if(y == null) {
					root = null;
				} else {
					Entry newY;
					if(lastResult < 0) {
						newY = y.detachLeft();
					} else {
						newY = y.detachRight();
					}
					
					if(z == null) {
						root = newY;
					} else {
						if(prev < 0) {
							z.setLeft(newY);
						} else {
							z.setRight(newY);
						}
					}
						
				}
				putBack(x.getLeft());
				putBack(x.getRight());
				return x;
			}
			z = y;
			y = x;
			x = result < 0 ? x.getLeft() : x.getRight();
		}
		return null;
	}
	
	private Entry doFind(Comparable key) {
		int result;
		Entry x = root;
		while (x != null) {
			result = key.compareTo(x.getKey());
			if (result == 0) {
				return x;
			}
			x = result < 0 ? x.getLeft() : x.getRight();
		}
		return null;
	}
}
