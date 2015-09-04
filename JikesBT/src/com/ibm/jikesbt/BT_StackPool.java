/*
 * Created on Jul 19, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.util.ArrayList;


/**
 * @author Sean Foley
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BT_StackPool {
	
	private static final int DEFAULT_MAX_DEPTH = 10;

	static final BT_StackCell[] emptyStack = new BT_StackCell[0];
	private ArrayList stackSets[] = new ArrayList[DEFAULT_MAX_DEPTH]; //the set at index i is the set of stacks of size i
	static final BT_LocalCell[] emptyLocals = new BT_LocalCell[0];
	private ArrayList localsSets[] = new ArrayList[DEFAULT_MAX_DEPTH]; //the set at index i is the set of stacks of size i
	
	private static final ArrayList emptySets[] = new ArrayList[0];
	
	public static final BT_StackPool pool = new BT_StackPool();
	
    BT_LocalCell[] getDuplicate(BT_LocalCell cells[], BT_LocalCell additional[]) {
    	return getDuplicate(cells, additional.length, additional);
    }
    
    BT_LocalCell[] getDuplicate(BT_LocalCell cells[], int fewer) {
    	return getDuplicate(cells, -fewer, null);
    }
    
    BT_LocalCell[] getDuplicate(BT_LocalCell cells[]) {
   		return getDuplicate(cells, 0, null);
   	}
	
    private BT_LocalCell[] getDuplicate(BT_LocalCell cells[], int change, BT_LocalCell additional[]) {
    	BT_LocalCell duplicate[] = getLocals(cells.length + change);
    	if(change <= 0) {
    		System.arraycopy(cells, 0, duplicate, 0, duplicate.length);
    	} else {
    		System.arraycopy(cells, 0, duplicate, 0, cells.length);
    		System.arraycopy(additional, 0, duplicate, cells.length, additional.length);
    	} 
    	return duplicate;
    }
    
    BT_StackCell[] getDuplicate(BT_StackCell cells[]) {
   		BT_StackCell duplicate[] = getStack(cells.length);
		System.arraycopy(cells, 0, duplicate, 0, duplicate.length);
		return duplicate;
   	}
    
    public void empty() {
    	stackSets = emptySets;
    	localsSets = emptySets;
    }
    
    public BT_StackCell[] getStack(int depth) {
    	return (BT_StackCell[]) getArray(depth, stackSets, false);
    }
    
    public void returnStack(BT_StackCell stk[]) {
    	returnArray(stk, stackSets);
    }
    
    protected void returnStacks(BT_StackCell stacks[][]) {
    	//return the given stacks
		for(int i=0; i<stacks.length; i++) {
			returnStack(stacks[i]);
		}
    }
    
	public BT_LocalCell[] getLocals(int depth) {
		return (BT_LocalCell[]) getArray(depth, localsSets, true);
	}
	
	private static Object[] getArray(int depth, ArrayList stackSets[], boolean locals) {
		if(depth == 0) {
			return locals ? emptyLocals : (Object[]) emptyStack;
		} 
		Object ret[];
		synchronized(stackSets) {
			if(depth >= stackSets.length) {
				ArrayList newStackSets[] = new ArrayList[depth * 2];
				System.arraycopy(stackSets, 0, newStackSets, 0, stackSets.length);
				stackSets = newStackSets;
			}
			
			ArrayList set = stackSets[depth];
			if(set == null || set.size() == 0) {
				return locals ? new BT_LocalCell[depth] : (Object[]) new BT_StackCell[depth];
			}
			ret = (Object[]) set.remove(set.size() - 1);	
		}
		return ret;
	}
	
	public void returnLocals(BT_LocalCell stk[]) {
		returnArray(stk, localsSets);
	}
	
	protected void returnLocals(BT_LocalCell stacks[][]) {
		//return the given locals
		for(int i=0; i<stacks.length; i++) {
			returnLocals(stacks[i]);
		}
	}
	
	private static void returnArray(Object stk[], ArrayList localsSets[]) {
		if(stk == null) {
			return;
		}
		int depth = stk.length;
		if(depth == 0) {
			return;
		}
		for(int i=0; i<depth; i++) {
			stk[i] = null;//set everything to null for garbage collection
		}
		synchronized(localsSets) {
			ArrayList set;
			if(depth >= localsSets.length) {
				ArrayList newStackSets[] = new ArrayList[depth * 2];
				System.arraycopy(localsSets, 0, newStackSets, 0, localsSets.length);
				localsSets = newStackSets;
				set = new ArrayList(5);
				localsSets[depth] = set;
			} else {
				set = localsSets[depth];
				if(set == null) {
					set = new ArrayList(5);
					localsSets[depth] = set;
				}
			}
			set.add(stk);
		}
	}
	
}
