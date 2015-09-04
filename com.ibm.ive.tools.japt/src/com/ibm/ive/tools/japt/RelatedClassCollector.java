package com.ibm.ive.tools.japt;

import java.util.*;
import com.ibm.jikesbt.*;

/**
 * Finds all related classes of a given class.
 * <p>
 * All parents of the class itself, any child class, and the parents of any child class
 * are considered a related class.
 * <p>
 * Objects may iterate through the related classes by implementing the RelatedClassVisitor interface
 * and calling the visitClasses method.
 * <p>
 * @author sfoley
 */
public class RelatedClassCollector {

	
	private BT_Class clazz;
	private BT_Class relatedClasses[];
	private BT_Class relatedParents[];
	private BT_Class relatedChildren[];
	private BT_Class relatedParentsOfChildren[];
	private boolean isSet;
		
	public RelatedClassCollector(BT_Class clazz) {
		this.clazz = clazz;
	}
	
	public void reset() {
		isSet = false;
		relatedClasses = relatedParents = relatedChildren = relatedParentsOfChildren = null;
	}
	
	private void setUp() {
		if(!isSet) {
			final Set relatedParentsSet = new HashSet();
			final Set relatedParentsOfChildrenSet = new HashSet();
			final Set relatedChildrenSet = new HashSet();
			Set relatedClassesSet = new HashSet();
			examineClass(new RelatedClassVisitor() {
							public void visit(BT_Class relatedClass, int relation) {
								switch(relation) {
									case RelatedClassVisitor.PARENT:
										relatedParentsSet.add(relatedClass);
									case RelatedClassVisitor.PARENT_OF_CHILD:
										relatedParentsOfChildrenSet.add(relatedClass);
									case RelatedClassVisitor.CHILD:
										relatedChildrenSet.add(relatedClass);
								}
							}
						}, 
						relatedClassesSet, 
						clazz, 
						RelatedClassVisitor.SELF);
			
			relatedClasses = (BT_Class[]) relatedClassesSet.toArray(new BT_Class[relatedClassesSet.size()]);
			relatedParents = (BT_Class[]) relatedParentsSet.toArray(new BT_Class[relatedParentsSet.size()]);
			relatedChildren = (BT_Class[]) relatedChildrenSet.toArray(new BT_Class[relatedChildrenSet.size()]);
			relatedParentsOfChildren = (BT_Class[]) relatedChildrenSet.toArray(new BT_Class[relatedParentsOfChildrenSet.size()]);
			isSet = true;
		}
	}
	
	/**
	 * @return all related classes including the class itself
	 * Will cause a visit to all classes if none has been done already
	 */
	public BT_Class[] getAllRelatedClasses() {
		setUp();
		return relatedClasses;
	}
	
	private void examineClass(RelatedClassVisitor visitor, Set relatedClassesSet, BT_Class relatedClass, int relation) {
		if(!relatedClassesSet.contains(relatedClass)) {
			relatedClassesSet.add(relatedClass);
			visitor.visit(relatedClass, relation);
			BT_ClassVector parents = relatedClass.getParents();
			
			//the ordering here ensures that all parents are visited first, then all children, then all parents of children
			if(relation == RelatedClassVisitor.PARENT || relation == RelatedClassVisitor.SELF) {
				for(int k=0; k<parents.size(); k++) {
					examineClass(visitor, relatedClassesSet, parents.elementAt(k), RelatedClassVisitor.PARENT);
				}
			}
				
			if(relation == RelatedClassVisitor.SELF || relation == RelatedClassVisitor.CHILD) {
				BT_ClassVector kids = relatedClass.getKids();
				for(int i=0; i<kids.size(); i++) {
					examineClass(visitor, relatedClassesSet, kids.elementAt(i), RelatedClassVisitor.CHILD);
				}
			}
			
			if(relation == RelatedClassVisitor.PARENT_OF_CHILD || relation == RelatedClassVisitor.CHILD) {
				for(int k=0; k<parents.size(); k++) {
					examineClass(visitor, relatedClassesSet, parents.elementAt(k), RelatedClassVisitor.PARENT_OF_CHILD);
				}
			}
		}
	}
	
	/**
	 * Visit all related classes exactly once.  The order is undefined, and in addition the order
	 * may change from one call to the next.
	 * <p>
	 * A related class is any child class, any parent of the class itself or any parent of a child class.
	 * <p>
	 * The visit method will be called once for each related class on any registered listener.
	 */
	public void visitClasses(RelatedClassVisitor visitor) {
		setUp();
		for(int i=0; i<relatedParents.length; i++) {
			visitor.visit(relatedParents[i], RelatedClassVisitor.PARENT);
		}
		for(int i=0; i<relatedChildren.length; i++) {
			visitor.visit(relatedChildren[i], RelatedClassVisitor.CHILD);
		}
		for(int i=0; i<relatedParentsOfChildren.length; i++) {
			visitor.visit(relatedParentsOfChildren[i], RelatedClassVisitor.PARENT_OF_CHILD);
		}
		visitor.visit(clazz, RelatedClassVisitor.SELF);	
	}
	
	public interface RelatedClassVisitor {
		/**
		 * a parent
		 */
		int PARENT = 0;
		
		/**
		 * a parent of a child class
		 */
		int PARENT_OF_CHILD = 1;
		
		/**
		 * a child, which in the case of a class is a subclass, or in the case of an interface
		 * it is a child interface or an implementing class
		 */
		int CHILD = 2;
		
		/**
		 * the class itself
		 */
		int SELF = 3;
	
		/**
		 * @param relatedClass the class that has been reached
		 * @param relation one of PARENT, PARENT_OF_CHILD, CHILD, SELF
		 */
		void visit(BT_Class relatedClass, int relation);
	}
	
	
}
