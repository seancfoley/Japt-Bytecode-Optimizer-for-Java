package com.ibm.ive.tools.japt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodInlaw;
import com.ibm.jikesbt.BT_MethodInlawVector;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Repository;


/**
 * Finds methods that are related to other methods through implementation and overriding.
 * <p>
 * @author sfoley
 * 
 */
public class RelatedMethodMap {

	private HashMap relatedMethodMap;
	private HashMap relatedClassMap;
	private HashMap additionalParentMap;
	private HashMap additionalChildMap;
	private BT_Repository rep;
	private BT_MethodVector allRelatedMethods[];
	private boolean isConstructed = false;
	
	public RelatedMethodMap(BT_Repository rep) {
		this.rep = rep;
	}

	public void reset() {
		isConstructed = false;
	}
	
	/*private BT_MethodVector doMapCheck() {
		Iterator iterator = relatedMethodMap.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry next = (Map.Entry) iterator.next();
			BT_Method method = (BT_Method) next.getKey();
			BT_MethodVector relatedMethods = (BT_MethodVector) next.getValue();
			if(relatedMethods.size() <= 1) {
				return relatedMethods;
			}
		}
		return null;
	}*/
	
	//note that if I remove a class C:
	//class A{
	// void run(){}
	//}
	//class C implements Runnable {}
	//then Runnable.run is no longer related to A.run, so should have a remove method for classes here to check whether such a relation has been removed...
	
	public void remove(BT_Method method) {
		if(!isConstructed) {
			return;
		}
		
		BT_MethodVector relatedMethods = (BT_MethodVector) relatedMethodMap.get(method);
		if(relatedMethods != null) {
			if(relatedMethods.size() > 2) {
				relatedMethods.removeElement(method);
				BT_ClassVector relatedClasses = (BT_ClassVector) relatedClassMap.get(method);
				relatedClasses.removeElement(method.getDeclaringClass());
			}
			else { //the map should not contain sets of size 1
				relatedMethods.removeElement(method);
				BT_Method otherMethod = relatedMethods.firstElement();
				relatedMethodMap.remove(otherMethod);
				relatedClassMap.remove(otherMethod);
				relatedMethods.removeAllElements(); //make garbage collection easier
				
				//we reset the collection of related methods.
				//They will be reconstructed by iterating through the related method map.
				//the alternative would be to iterate through the array, looking for the 
				//method vector relatedMethods and removing it.  Or we could simply stipulate
				//that it is ok to have 0 length vectors in that array. 
				allRelatedMethods = null;
			}
			relatedMethodMap.remove(method);
			relatedClassMap.remove(method);
		}
		//by the removal of the method
		BT_MethodVector c = getAdditionalChildren(method);
		if(c != null) {
			//for each child, we remove our identity as a parent
			for(int i=0; i<c.size(); i++) {
				BT_Method child = c.elementAt(i);
				BT_MethodVector parents = getAdditionalParents(child);
				parents.removeElement(method);
				//Note that removing a parent, which is an interface method, 
				//cannot generate a new parent, because there is no real interface hierarchy
				//any interface parents of the removed method are ALREADY indirect parents of the child
				if(parents.size() == 0) {
					additionalParentMap.remove(child);
				}
			}
		}
		
		BT_MethodVector p = getAdditionalParents(method);
		if(p != null) {
			//for each parent, we remove our identity as a child
			for(int i=0; i<p.size(); i++) {
				BT_Method parent = p.elementAt(i);
				BT_MethodVector children = getAdditionalChildren(parent);
				children.removeElement(method);
				//The parent may have a new child in a parent class of the removed method
				findIndirectLink(method.getDeclaringClass(), parent);
				if(children.size() == 0) {
					additionalChildMap.remove(parent);
				}
			}
		}
		additionalParentMap.remove(method);
		additionalChildMap.remove(method);
	}
	
	public void construct() {
		if(!isConstructed) {
			constructRelatedMethodMap();
			allRelatedMethods = null;
			isConstructed = true;
		}
	}
	
	private BT_MethodVector addToMap(BT_Method methodOne, BT_Method methodTwo) {
		if(methodOne.equals(methodTwo)) {
			throw new Error();
		}
		BT_MethodVector one = (BT_MethodVector) relatedMethodMap.get(methodOne);
		BT_MethodVector two = (BT_MethodVector) relatedMethodMap.get(methodTwo);
		if(one != null) {
			if(two != null) {
				//must merge the two sets
				for(int j=0; j<two.size(); j++) {
					BT_Method twoMethod = two.elementAt(j);
					one.addUnique(twoMethod);
					relatedMethodMap.put(twoMethod, one);
				}
			} else {
				one.addUnique(methodTwo);
				relatedMethodMap.put(methodTwo, one);
			}
			return one;
		} else if(two != null) {
			two.addUnique(methodOne);
			relatedMethodMap.put(methodOne, two);
			return two;
		} else {
			BT_MethodVector set = new BT_HashedMethodVector();
			set.addElement(methodOne);
			set.addElement(methodTwo);
			relatedMethodMap.put(methodOne, set);
			relatedMethodMap.put(methodTwo, set);
			return set;
		}
	}
	
	private void addToMap(BT_MethodVector one, BT_Method methodOne, BT_Method methodTwo) {
		if(methodOne.equals(methodTwo)) {
			throw new Error();
		}
		BT_MethodVector two = (BT_MethodVector) relatedMethodMap.get(methodTwo);
		if(two != null) {
			//must merge the two sets
			for(int j=0; j<two.size(); j++) {
				BT_Method twoMethod = two.elementAt(j);
				one.addUnique(twoMethod);
				relatedMethodMap.put(twoMethod, one);
			}
		} else {
			one.addUnique(methodTwo);
			relatedMethodMap.put(methodTwo, one);
		}
	}
	
	private void findRelatedMethods(BT_Method method) {
		int i;
		BT_MethodVector methods = null;
		BT_MethodVector kids = method.getKids();
		BT_MethodInlawVector inlaws = method.getInlaws();
		BT_MethodVector parents = method.getParents();
		for (i=0; i<kids.size(); i++) {
			if(methods != null) {
				addToMap(methods, method, kids.elementAt(i));
			} else {
				methods = addToMap(method, kids.elementAt(i));
			}
		}
		for (i=0; i < inlaws.size(); i++) {
			BT_MethodInlaw inlaw = inlaws.elementAt(i);
			if(methods != null) {
				addToMap(methods, method, inlaw.getOtherMethod(method));
			} else {
				methods = addToMap(method, inlaw.getOtherMethod(method));
			}
		}
		
		//TODO gxxx;
		///xxx make sure we find same methods in parent interface of interface method
		
		for (i=0; i < parents.size(); i++) {
			if(methods != null) {
				addToMap(methods, method, parents.elementAt(i));
			} else {
				methods = addToMap(method, parents.elementAt(i));
			}
		}
	}
	
	/** 
	 * Unfortunately JIKES_BT does not always link a method implementing an interface
	 * to the method in the interface, when something like the following occurs:
	 * class A { public void x() {} }
	 * class B extends A implements I {}
	 * interface I { void x(); }
	 * 
	 * The relation between A.x and I.x (through B) is currently not being detected by JIKES_BT.
	 * This relation cannot be determined when looking at A or I.  The relationship is "consummated" :-) by B.
	 * 
	 * In fact, you can have a similar relationship when A is an interface and B is a class or interface.
	 * 
	 * At this time I have not the desire to undertake a refactorization of JIKES_BT to handle this,
	 * instead I simply fix it here, later this fix should probably be added to JIKES_BT
	 */
	private void findIndirectlyRelatedMethods(BT_Class clazz) {
		if(clazz.isInterface()) {
			// TODO also handle cases where clazz is interface and where parent is interface 
			//(but there is no parent/child relationship in such cases_
			return;
		}
		Set interfacesVisited = new HashSet();
		BT_ClassVector parents = clazz.getParents();
		//for each implemented interface we check if each interface method is implemented in a parent class
		//then we will repeat this for all the interfaces implemented by parent classes, 
		//because we may now have methods that now implement those interface methods as well and did not before
		
		for(int i=0; i<parents.size(); i++) {
			BT_Class parent = parents.elementAt(i);
			if(parent.isInterface() && !interfacesVisited.contains(parent)) {
				interfacesVisited.add(parent);
				findIndirectLink(clazz, parent, interfacesVisited);
			}
		}
	}

	private void findIndirectLink(BT_Class clazz, BT_Class interfaceParent, Set interfacesVisited) {
		BT_MethodVector interfaceMethods = interfaceParent.getMethods();
		searchInterface:
		for(int k=0; k<interfaceMethods.size(); k++) {
			BT_Method interfaceMethod = interfaceMethods.elementAt(k);
			BT_MethodVector classMethods = clazz.getMethods();
			for(int j=0; j<classMethods.size(); j++) {
				BT_Method classMethod = classMethods.elementAt(j);
				
				if(classMethod.getName().equals(interfaceMethod.getName()) && parametersMatch(classMethod, interfaceMethod)) {
					//the class implements the interface directly, so there is no indirect implementation in a super class
					continue searchInterface;
				}
			}
			//if we have reached here, then either clazz is abstract and doesn't have an implementation of interfaceMethod,
			//or the implementation of interfaceMethod lies in a parent class of clazz
			findIndirectLink(clazz, interfaceMethod);
		}
		
		BT_ClassVector parents = interfaceParent.getParents();
		for(int i=0; i<parents.size(); i++) {
			BT_Class parent = parents.elementAt(i);
			if(parent.isInterface() && !interfacesVisited.contains(parent)) {
				interfacesVisited.add(parent);
				findIndirectLink(clazz, parent, interfacesVisited);
			}
		}
	}
	
	private void findIndirectLink(BT_Class clazz, BT_Method interfaceMethod) {
		BT_Class superClass = clazz;
		do {
			superClass = superClass.getSuperClass();
			//TODO see above about also linking interface methods with interface methods and not just class methods with interface methods
			//But in cases where you are not in a superclass, do not add to additionalParentMap
			if(superClass == null) {
				return;
			}
			BT_MethodVector superClassMethods = superClass.getMethods();
			for(int j=0; j<superClassMethods.size(); j++) {
				BT_Method superClassMethod = superClassMethods.elementAt(j);
				if(superClassMethod.getName().equals(interfaceMethod.getName()) && parametersMatch(superClassMethod, interfaceMethod)) {
					addToAdditionalParentMap(interfaceMethod, superClassMethod);
					addToMap(superClassMethod, interfaceMethod);
					return;
				}
			}
		} while(true);
	}
	
	public BT_MethodVector getAllParents(BT_Method method) {
		construct();
		BT_MethodVector additionalParents = getAdditionalParents(method);
		BT_MethodVector parents = method.getParents();
		if(additionalParents == null || additionalParents.size() == 0) {
			return parents;
		}
		else if(parents.size() == 0) {
			return additionalParents;
		}
		BT_MethodVector results = (BT_MethodVector) parents.clone();
		for(int i=0; i<additionalParents.size(); i++) {
			results.addUnique(additionalParents.elementAt(i));
		}
		return results;
	}
	
	
	
	/**
	 * Finds the set of parents that are liked indirectly through a common child class.
	 * The BT_Method.getParents() method finds methods in parent classes or parent interfaces
	 * that have been overridden or implemented by the given method.
	 * There are additional methods that have been implemented by a given method m in a class C,
	 * indirectly through the children of C.  If a child class K implements an interface with a method m2
	 * that is implemented in K by the inherited method m, then m2 is considered a parent of m.
	 *  
	 * @return a set of parents that are liked indirectly through a common child class, or null if no such methods exist.
	 */
	private BT_MethodVector getAdditionalParents(BT_Method method) {
		return (BT_MethodVector) additionalParentMap.get(method);
	}
	
	private BT_MethodVector getAdditionalChildren(BT_Method method) {
		return (BT_MethodVector) additionalChildMap.get(method);
	}
	
	private void addToAdditionalParentMap(BT_Method parent, BT_Method child) {
		BT_MethodVector p = getAdditionalParents(child);
		if(p == null) {
			p = new BT_MethodVector(2);
			additionalParentMap.put(child, p);
			p.addElement(parent);
		} else {
			p.addUnique(parent);
		}
		
		BT_MethodVector c = getAdditionalChildren(parent);
		if(c == null) {
			c = new BT_MethodVector(2);
			additionalChildMap.put(parent, c);
			c.addElement(child);
		} else {
			c.addUnique(child);
		}
	}
	
	private void constructRelatedMethodMap() {
		relatedMethodMap = new HashMap();
		relatedClassMap = new HashMap();
		additionalParentMap = new HashMap();
		additionalChildMap = new HashMap();
		BT_ClassVector classes = rep.getClasses();
		int size = classes.size();
		for(int j=0; j<size; j++) {
			BT_Class clazz = classes.elementAt(j);
			BT_MethodVector methods = clazz.getMethods();
			int methodsSize = methods.size();
			for(int k=0; k<methodsSize; k++) {
				BT_Method method = methods.elementAt(k);
				findRelatedMethods(method);
			}
			findIndirectlyRelatedMethods(clazz);
		}
		convertSets();
	}
	
	private void convertSets() {
		Iterator iterator = relatedMethodMap.values().iterator();
		HashSet alreadySeen = new HashSet();
		while(iterator.hasNext()) {
			BT_MethodVector set = (BT_MethodVector) iterator.next();
			if(alreadySeen.contains(set)) {
				continue;
			}
			alreadySeen.add(set);
			set.trimToSize();
			BT_ClassVector relatedClasses = new BT_HashedClassVector(set.size());
			for(int i=0; i<set.size(); i++) {
				BT_Method relatedMethod = set.elementAt(i);
				BT_Class clazz = relatedMethod.getDeclaringClass();
				relatedClassMap.put(relatedMethod, relatedClasses);
				try {
					relatedClasses.addElement(clazz);
				} catch(IllegalArgumentException e) {
					/* The class load process checks the fill descriptor when checking for duplicate methods, 
					 * which permits methods with the same
					 * name and signature but different return value, so we must handle that case here.
					 */
					rep.factory.noteClassLoadError(null, clazz, clazz.getName(), clazz.loadedFrom, 
							"duplicate method definition " + relatedMethod, BT_Repository.JAVA_LANG_CLASS_FORMAT_ERROR);
				}
			}
		}
	}
	
	/**
	 * @return an array of all methods to which the given method is related.
	 * 
	 * The set includes all direct relatives of the method, 
	 * which are methods which the given method overrides, is overrided by,
	 * implements or is implemented by.  In addition, the set also includes the direct relatives of
	 * any method in the set.  The set also includes the method itself, so all returned arrays are of non-zero size.
	 * 
	 * The relation of being related forms an equivalence relation on the set of all methods.
	 */
	public BT_MethodVector getRelatedMethods(BT_Method method) {
		construct();
		BT_MethodVector relatedMethods = (BT_MethodVector) relatedMethodMap.get(method);
		if(relatedMethods == null) {
			BT_MethodVector result = new BT_MethodVector(1);
			result.addElement(method);
			return result;
		}
		return relatedMethods;
	}
	
	/**
	 * behaves the same as getRelatedMethods except that null is returned if the method
	 * is related only to itself.  Note that if a non-null vector is returned then the
	 * vector will contain the given method.
	 * @param method
	 * @return the set of related methods if its size is 2 or more, or null otherwise
	 */
	public BT_MethodVector getOtherRelatedMethods(BT_Method method) {
		construct();
		return (BT_MethodVector) relatedMethodMap.get(method);
	}
	
	/**
	 * Given a non-private non-static non-constructor method call (ie a class method call that
	 * can be overridden or an interface method call that can be implemented), this method returns
	 * the method that will actually be called if different from the method in the method call.
	 * If no such differing method is found, then null is returned
	 * @param clazz the class inheriting the method or an overriding method, 
	 * 	or in other words, the object type of the virtual method call
	 * @param the method being overridden or implemented
	 * @return the overriding method or null if no such method
	 */
	public BT_Method getOverridingMethod(BT_Class clazz, BT_Method method) {
		BT_Class declaringClass = method.getDeclaringClass();
		if(clazz.isInterface() || declaringClass.isDescendentOf(clazz)) {
			return null;
		}
		construct();
		BT_ClassVector relatedClasses = (BT_ClassVector) relatedClassMap.get(method);
		if(relatedClasses == null) {
			return null;
		}
		BT_MethodVector relatedMethods = getRelatedMethods(method);
		while(clazz != null && !clazz.equals(declaringClass)) {
			if(relatedClasses.contains(clazz)) {
				for(int i=0; i<relatedMethods.size(); i++) {
					if(relatedMethods.elementAt(i).getDeclaringClass().equals(clazz)) {
						return relatedMethods.elementAt(i);
					}
				}
			}
			clazz = clazz.getSuperClass();
		}
		return null;
	}
	
	/**
	 * 
	 * @return all overridding methods, including methods which override overridding methods
	 */
	public BT_MethodVector getOverridingMethods(BT_Method m) {
		if(m.isStatic() || m.isPrivate() || m.isConstructor()) {
			return BT_MethodVector.emptyMethodVector;
		}
		if(m.cls.isInterface()) {
			return BT_MethodVector.emptyMethodVector;
		}
		BT_MethodVector kids = (BT_MethodVector) m.getKids();
		BT_MethodVector result = null;
		for(int i=0; i<kids.size(); i++) {
			JaptMethod kid = (JaptMethod) kids.elementAt(i);
			BT_MethodVector kidKids = getOverridingMethods(kid);
			for(int j=0; j<kidKids.size(); j++) {
				if(result == null) {
					result = (BT_MethodVector) kids.clone();
				}
				result.addElement(kidKids.elementAt(j));
			}
		}
		if(result == null) {
			result = kids;
		}
		return result;
	}
	
	/**
	 * Determine if the given method is implemented in an implementing class
	 */
	/*
	 * there is a notable difference between the algorithm here and the algorithm
	 * for getOverridingMethod.  This is because there are instances in which
	 * an implementing method has no visible relationship to the implemented method,
	 * as in the following example:
	 * class A {
	 * 	public void run() {}
	 * }
	 * class B extends A implements Runnable {}
	 * There is no direct link from Runnable.run() to A.run(), the link
	 * occurs through B which is a child of both classes.
	 *  
	 */
	public BT_MethodVector getImplementingMethods(BT_Method method) {
		construct();
		if(!method.cls.isInterface()) {
			return BT_MethodVector.emptyMethodVector;
		}
		
		BT_ClassVector relatedClasses = getRelatedClasses(method);
		if(relatedClasses.size() == 1) {
			return BT_MethodVector.emptyMethodVector;
		}
		BT_MethodVector result = new BT_HashedMethodVector();
		JaptClass methodClass = (JaptClass) method.getDeclaringClass();
		getImplementingMethods(new HashSet(), methodClass, method, result);
		return result;
	}
	
	
	/**
	 * @param method
	 * @param result
	 * @param interfaceKids
	 */
	private void getImplementingMethods(Set visited, BT_Class child, BT_Method method, BT_MethodVector result) {
		BT_ClassVector interfaceKids = child.getKids();
		for(int i=0; i<interfaceKids.size(); i++) {
			BT_Class interfaceKid = interfaceKids.elementAt(i);
			if(visited.contains(interfaceKid)) {
				continue;
			}
			visited.add(interfaceKid);
			getImplementingMethods(visited, interfaceKid, method, result);
			if(interfaceKid.isClass) {
				BT_Method implementingMethod = getImplementingMethod(interfaceKid, method);
				if(implementingMethod != null) {
					result.addUnique(implementingMethod);
				}
			}
		}
	}

	/**
	 * Determine if the given method is overridden in a subclass
	 */
	public boolean hasOverridingMethod(BT_Method method) {
		construct();
		
		BT_ClassVector relatedClasses = (BT_ClassVector) relatedClassMap.get(method);
		if(relatedClasses == null) {
			return false;
		}
		
		BT_Class methodClass = method.getDeclaringClass();
		for(int i=0; i<relatedClasses.size(); i++) {
			BT_Class relatedClass = relatedClasses.elementAt(i);
			if(methodClass.isClassAncestorOf(relatedClass)) {
				return true;
			}
		}
		return false;
	}
	
	
	
	public BT_MethodVector getImplementedAndOverridenMethods(BT_Method m) {
		BT_MethodVector related = getOtherRelatedMethods(m);
		if(related == null || related.size() == 0) {
			return BT_MethodVector.emptyMethodVector;
		}
		BT_MethodVector res = new BT_MethodVector();
		for(int i=0; i<related.size(); i++) {
			BT_Method relatedMethod = related.elementAt(i);
			BT_Class relatedClass = relatedMethod.getDeclaringClass();
			if(!relatedClass.isInterface()) {
				//check if we override the related method
				if(relatedClass.isClassAncestorOf(m.getDeclaringClass())) {
					res.addElement(relatedMethod);
				}
			} else {
				BT_Class implementingClass = 
					implementsInterface(m.getDeclaringClass(), (JaptClass) relatedClass);
				if(implementingClass != null) {
					if(implementingClass.equals(m.getDeclaringClass())) {
						res.addElement(relatedMethod);
					}
					else {
						//a subclass implements the given related class, 
						//but we also check whether the method implements the given related method
						BT_Method impl = getImplementingMethod(implementingClass, relatedMethod);
						if(impl.equals(m)) {
							res.addElement(relatedMethod);
						}
					}
				}
			}
		}
		return res;
	}
	
	/**
	 * determines if methodClass or any subclass implements interfaceClass
	 * @param methodClass
	 * @param interfaceClass
	 * @return the class which implements interfaceClass
	 */
	private BT_Class implementsInterface(BT_Class methodClass, JaptClass interfaceClass) {
		if(interfaceClass.isInterfaceAncestorOf(methodClass)) {
			return methodClass;
		}
		BT_ClassVector kids = methodClass.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Class kid = kids.elementAt(i);
			BT_Class implementingClass = implementsInterface(kid, interfaceClass);
			if(implementingClass != null) {
				return implementingClass;
			}
		}
		return null;
	}
	
	/**
	 * This method does not check if the given class implements the given interface method either in
	 * itself or in a superclass.  It returns the method that would indeed be the implementing
	 * method if this is the case.
	 * @param clazz the implementing class
	 * @param the interface method being implemented
	 * @return the implementing method or null if no such method
	 */
	public BT_Method getImplementingMethod(BT_Class clazz, BT_Method method) {
		construct();
		
		//the obvious deficiency in this algorithm is that we never even bother to check
		//if the given class "clazz" implements the method "method", we just check to see
		//which method that would be if indeed it does
		BT_ClassVector relatedClasses = getRelatedClasses(method);
		if(relatedClasses.size() == 1) {
			return null;
		}
		BT_MethodVector relatedMethods = getRelatedMethods(method);
		do {
			if(relatedClasses.contains(clazz)) {
				for(int i=0; i<relatedMethods.size(); i++) {
					if(relatedMethods.elementAt(i).getDeclaringClass().equals(clazz)) {
						return relatedMethods.elementAt(i);
					}
				}
			}
			clazz = clazz.getSuperClass();
		} while(clazz != null);
		return null;
	}
	
	
	/**
	 * @return an array of all classes containing a method related to givenMethod.
	 * 
	 */
	public BT_ClassVector getRelatedClasses(BT_Method givenMethod) {
		construct();
		BT_ClassVector relatedClasses = (BT_ClassVector) relatedClassMap.get(givenMethod);
		if(relatedClasses == null) {
			relatedClasses = new BT_ClassVector(1);
			relatedClasses.addElement(givenMethod.getDeclaringClass());
		}
		return relatedClasses;
	}
	
	private void collectAllRelatedMethods() {
		Iterator iterator = relatedMethodMap.values().iterator();
		List methodSetList = new ArrayList();
		while(iterator.hasNext()) {
			BT_MethodVector relatedMethods = (BT_MethodVector) iterator.next();
			methodSetList.add(relatedMethods);	
		}
		allRelatedMethods = (BT_MethodVector[]) methodSetList.toArray(new BT_MethodVector[methodSetList.size()]);
	}
	
	/**
	 * @return an array of all arrays of related methods of size greater than 1.  
	 * Each element of the array is an array of methods as described by the method getRelatedMethods.  
	 * Each element will also be of length 2 or more, so if a method is related only to itself it will not appear at all.
	 */
	public BT_MethodVector[] getAllRelatedMethods() {
		construct();
		if(allRelatedMethods == null) {
			collectAllRelatedMethods();
		}
		return allRelatedMethods;
	}
	
	public static boolean parametersMatch(BT_MethodSignature a, BT_MethodSignature b) {
		BT_ClassVector typesA = a.types;
		BT_ClassVector typesB = b.types;
		if(typesA.size() != typesB.size()) {
			return false;
		}
		for(int i=0, size = typesA.size(); i<size; i++) {
			if(!typesA.elementAt(i).equals(typesB.elementAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean parametersMatch(BT_Method a, BT_Method b) {
		return parametersMatch(a.getSignature(), b.getSignature());
	}
	
	public static boolean signaturesMatch(BT_MethodSignature a, BT_MethodSignature b) {
		return a.equals(b);
	}
}
