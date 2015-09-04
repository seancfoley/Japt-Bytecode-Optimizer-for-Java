package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */



/**
 This can be used to visit (call a method on) each kid (subclass,
 subinterface, or implemented class), its kids, their kids, ....
 E.g., do: <code><pre>
 *   BT_ClassDescendentVisitor  cdv = new BT_ClassDescendentVisitor() {
 *         public void  visit(BT_Class visitingC) {
 *               System.out.println( "  t Visiting ... " + visitingC.useName());
 *               super.visit(visitingC);
 *               System.out.println( "  t  ... Visited " + visitingC.useName());
 *           } };
 *   BT_Class  initC = __;
 *   cdv.visitAncestors(initC); // or cdv.visitAClassAndDescendents(initC);
 </pre></code>

 The "opposite of" {@link BT_ClassAncestorVisitor}.
 * @author IBM
**/
public abstract class BT_ClassDescendentVisitor extends BT_Base {

	/**
	 Visits a class and its descendents.
	 This method can be overridden with a method that does something
	 interesting and that then invokes this method via
	 <code>super.visit(visitingC)</code> to continue visiting its descendents.
	
	 @param  visitingC  The class to be visited.
	**/
	public void visit(BT_Class visitingC) {
		BT_ClassVector kids = visitingC.kids_;
		int size = kids.size();
		for (int i = 0; i < size; ++i)
			visit(kids.elementAt(i));
	}

	/**
	 Calls {@link BT_ClassDescendentVisitor#visit} for this class, so visits this class and its
	 descendents.
	 Invoke this method both the initial class (initC) and its descendents are
	 to be visited.
	
	 <p> Override this method if you want to return something from an anonymous
	 class -- something like: <code><pre>
	 *   public Object  visitAClassAndDescendents(BT_Class initC) {
	 *       super.visitAClassAndDescendents( initC);
	 *       return this.someField;
	 *   }
	 </pre></code>
	
	 <p> {@link BT_ClassDescendentVisitor#visitDescendents} is the same except that it does not
	 visit initC.
	
	 @param  initC  The initial class to be visited.
	**/
	public Object visitAClassAndDescendents(BT_Class initC) {
		visit(initC);
		return null;
	}

	/**
	 Calls {@link BT_ClassDescendentVisitor#visit} for each kid of this class, so visits the
	 descendents of the initial class (initC) -- not initC itself.
	 Invoke this method if the initial class is <em>not</em> to be visited.
	
	 <p> Override this method if you want to return something from an
	 anonymous class.
	
	 <p> {@link BT_ClassDescendentVisitor#visitAClassAndDescendents} is the same except that it also
	 visits initC.
	
	 @param  initC  The parent of the classes to be visited.
	**/
	public Object visitDescendents(BT_Class initC) {
		BT_ClassVector kids = initC.kids_;
		int size = kids.size();
		for (int i = 0; i < size; ++i)
			visit(kids.elementAt(i));
		return null;
	}
}