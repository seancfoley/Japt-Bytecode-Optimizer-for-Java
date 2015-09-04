package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */



/**
 This can be used to visit (call a method on) each parent (superclass,
 superinterface, or implemented interface), its parents, their parents,
 ....
 E.g., do: <code><pre>
 *   BT_Class  initC = __;
 *   BT_ClassAncestorVisitor  cdv = new BT_ClassAncestorVisitor() {
 *       public void  visit(BT_Class visitingC) {
 *             System.out.println( "  t Visiting ... " + visitingC.useName());
 *             super.visit(visitingC);
 *             System.out.println( "  t  ... Visited " + visitingC.useName());
 *         } };
 *   cdv.visitAncestors(initC); // or cdv.visitAClassAndAncestors(initC);
 </pre></code>

 The "opposite of" {@link BT_ClassDescendentVisitor}.
 * @author IBM
**/
public abstract class BT_ClassAncestorVisitor extends BT_Base {

	/**
	 Visits this class and its descendents.
	 You can <em>override</em> this method with a method that does something
	 interesting and that then invokes this method via
	 <code>super.visit(visitingC)</code> to continue visiting its ancestors.
	
	 @param  visitingC  The class being visited.
	**/
	public void visit(BT_Class visitingC) {
		BT_ClassVector parents = visitingC.parents_;
		int size = parents.size();
		for (int i = 0; i < size; ++i)
			visit(parents.elementAt(i));
	}

	/**
	 Calls {@link BT_ClassAncestorVisitor#visit} for this class, so visits this class and its
	 ancestors.
	 Invoke this method if both the initial class (initC) and its ancestors
	 are to be visited.
	
	 <p> Override this method if you want to return something from an
	 anonymous class -- something like: <code><pre>
	 *   public Object  visitAClassAndAncestors(BT_Class initC) {
	 *       super.visitAClassAndAncestors( initC);
	 *       return this.someField;
	 *   }
	 </pre></code>
	
	 <p> {@link BT_ClassAncestorVisitor#visitAncestors} is the same except that it does not
	 visit initC.
	
	 @param  initC  The initial class to be visited.
	**/
	public Object visitAClassAndAncestors(BT_Class initC) {
		visit(initC);
		return null;
	}

	/**
	 Visits this class's ancestors, but is not intended to be
	 overridden so this class will <em>not</em> be visited.
	 Invoke this method if the initial class is <em>not</em> to be visited.
	
	 <p> Override this method if you want to return something from an
	 anonymous class.
	
	 <p> {@link BT_ClassAncestorVisitor#visitAClassAndAncestors} is the same except that it also
	 visits initC.
	
	 @param  initC  The descendent of the classes to be visited.
	**/
	public void visitAncestors(BT_Class initC) {
		BT_ClassVector parents = initC.parents_;
		int size = parents.size();
		for (int i = 0; i < size; ++i)
			visit(parents.elementAt(i));
	}
}