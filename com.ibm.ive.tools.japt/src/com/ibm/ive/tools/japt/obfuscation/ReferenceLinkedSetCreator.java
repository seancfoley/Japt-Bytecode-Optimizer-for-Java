package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;

import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;

/**
 * @author sfoley
 * <p>
 * The following class produces sets of methods and fields which can all have the same name
 * in order to maximize compression.
 * 
 * Consider the following:
 * 
 * 		class A {
 *		 	int x;
 *		}
 *		 
 *		class B {
 *		 	int y = A.x;
 *		}
 * 
 *	In this example, we can rename x and y to have the same name, 
 *	which allows us to re-use the contant pool reference to x in B: 
 *  once to refer to the reference to B and once to refer to the field y.
 *  In addition, the constant pool of B will contain a name-type entry for
 *  A.A and for B.A.  Since they both have the same name and they are
 *  both the same type, this entry can be the same for both fields.
 * 
 * 
 * The following is the optimal solution:
 * 		class A {
 *		 	int A;
 *		}
 *		 
 *		class B  {
 *		 	int A = A.A;
 * 		}
 *
 * 
 * The sets constructed are as large as possible.  This class employs an algorithm that attempts
 * to duplicate method and field names, overload method names, and duplicate method and field name-types, thus
 * reducing the size of the constant pools of the classes containing those methods.  
 * 
 * Special care must be taken not to alter the semantics of the classes.  
 * For instance, if methods are overridden or are implementations of interface methods, then the names
 * must remain the same.  Two methods in the same class with the same parameter lists cannot be given the same name.  
 * Methods of child and parent classes with the same signatures but different
 * names cannot be given the same name in the event that one overrides the other.  Fields of child and parent
 * classes cannot be given the same name in the event that a field lookup on a child that delegates to a field
 * in the parent must find the parent field, and not a child field that has been given the same name.
 * 
 */
class ReferenceLinkedSetCreator {

	private final BT_ClassVector classes;
	private final NameHandler nameHandler;
	private final RelatedMethodMap relatedMethodMap;
	private final JaptRepository repository;
	private List linkedSetList = new LinkedList();
	private final Map linkedSetMap = new HashMap(); //maps any field, or method to its corresponding linked set.
	private Set ownedItems = new HashSet(); //all fields and methods that have been assigned to a set
	
	public ReferenceLinkedSetCreator(JaptRepository repository, NameHandler nameHandler) {
		this.classes = repository.getClasses();
		this.relatedMethodMap = repository.getRelatedMethodMap();
		this.nameHandler = nameHandler;
		this.repository = repository;
		constructLinkedSets();
		//allow for some garbage collection:
		ownedItems = null;
	}

	ReferenceLinkedSet getLinkedSet(BT_Item item) {
		return (ReferenceLinkedSet) linkedSetMap.get(item);
	}
	
	ReferenceLinkedSet[] getAllLinkedSets() {
		return (ReferenceLinkedSet[]) linkedSetList.toArray(new ReferenceLinkedSet[linkedSetList.size()]);
	}
	
	private void constructLinkedSets() {
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if(!clazz.isArray() && !clazz.isPrimitive()) {
				constructLinkedSets(clazz);
			}
		}
		//System.out.println("Total members: " + counter);
		//System.out.println("Total sets: " + setCounter);
	}
	
	//static int counter = 0;
	//static int setCounter;
	
	private void constructLinkedSets(BT_Class clazz) {
		
		BT_MethodVector methods = clazz.getMethods();
		for(int i=0; i<methods.size(); i++) {
			//counter++;
			BT_Method method = methods.elementAt(i);
			if(!memberIsNotAvailable(method)) {
				
				ReferenceLinkedSet set = createReferenceLinkedSet(clazz, method);
				if(set != null) {
					linkedSetList.add(set);
					//setCounter++;
					//System.out.println("New set with size " + set.size());
				}
			}
			
		}
		
		BT_FieldVector fields = clazz.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			//counter++;
			if(!memberIsNotAvailable(field)) {
				ReferenceLinkedSet set = createReferenceLinkedSet(clazz, field);
				if(set != null) {
					linkedSetList.add(set);
					//setCounter++;
					//System.out.println("New set with size " + set.size());
				}
			}
		}
	}
	
	/**
	 * The ordering of this list is important.  Name-types (not just names) are duplicated
	 * as much as possible by choosing targets in a specific order.
	 */
	private void serviceList(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list) {
		topLoop:
		do {
			/*
			 * these targets are used to attempt to duplicate method signatures.  This allows
			 * for the duplication of name-type and type constant pool entries, in addition to the UTF8
			 * string entries.
			 */
			do {
				ReferenceLinkedSetList.ClassMethodTarget target = list.getNextClassMethodTarget();
				if(target == null) {
					break;
				}
				boolean foundLink = false;
				BT_Class clazz = target.clazz;
				BT_MethodVector targetClazzMethods = clazz.getMethods();
				for(int k=0; k<targetClazzMethods.size(); k++) {
					BT_Method targetClazzMethod = targetClazzMethods.elementAt(k);
					if(RelatedMethodMap.signaturesMatch(targetClazzMethod.getSignature(), target.signature)) {
						if(tryLinkToClass(constructedSet, list, clazz, targetClazzMethod)) {
							foundLink = true;
							break;
						}		
					}	
				}
				if(!foundLink) {
					list.addClassTarget(clazz);
				}
			} while(true);
			
			/*
			 * these targets are used to attempt to duplicate field type signatures.  This allows
			 * for the suplication of name-type and type constant pool entries, in addition to the UTF8
			 * string entries.
			 */
			do {
				ReferenceLinkedSetList.ClassFieldTarget target = list.getNextClassFieldTarget();
				if(target == null) {
					break;
				}
				boolean foundLink = false;
				BT_Class accessorClass = target.clazz;
				BT_FieldVector accessorFields = accessorClass.getFields();
				for(int i=0; i<accessorFields.size(); i++) {
					BT_Field accessorField = accessorFields.elementAt(i);
					if(accessorField.getFieldType().equals(target.fieldType)) {
						if(tryLinkToClass(constructedSet, list, accessorClass, accessorField)) {
							foundLink = true;
							break;
						}		
					}
				}
				
				if(!foundLink) {
					list.addClassTarget(accessorClass);
				}
				
				if(list.hasClassMethodTargets()) {
					continue topLoop;
				}
			} while(true);
				
			/*
			 * a class has a reference to a method in some other class, so we attempt to duplicate
			 * names in both classes so as to double up on the UTF8 name in the constant pool of 
			 * the referring class, by adding the method to the set
			 */
			do {
				BT_Method method = list.getNextMethodTarget();
				if(method == null) {
					break;
				}
				tryLinkToClass(constructedSet, list, method.getDeclaringClass(), method);
				
				if(list.hasClassFieldTargets() || list.hasClassMethodTargets()) {
					continue topLoop;
				}
			} while(true);
			
			/*
			 * a class has a reference to a field in some other class, so we attempt to duplicate
			 * names in both classes so as to double up on the UTF8 name in the constant pool of 
			 * the referring class, by adding the field to the set.
			 */
			do {
				BT_Field field = list.getNextFieldTarget();
				if(field == null) {
					break;
				}
				tryLinkToClass(constructedSet, list, field.getDeclaringClass(), field);
				
				
				if(list.hasClassFieldTargets() || list.hasClassMethodTargets()) {
					continue topLoop;
				}
			} while(true);
			
			/*
			 * a method in one class is called from another or a field in one class
			 * is accessed by another, so we attempt to duplicate names in both classes, by 
			 * adding any member of the calling/referring class to the set.
			 */
			do {
				BT_Class clazz = list.getNextClassTarget();
				if(clazz == null) {
					break;
				}
				tryLinkToClass(constructedSet, list, clazz);
				
				if(list.hasClassFieldTargets() || list.hasClassMethodTargets()) {
					continue topLoop;
				}
			} while(true);
			
			/*
			 * these classes have a member in the set so we attempt to add more members from 
			 * the same class, doubling up on the UTF8 names.
			 */
			do {
				BT_Class clazz = list.getNextClassMemberTarget();
				if(clazz == null) {
					break;
				}
				collectNewMembers(list, clazz, getRelatedClasses(clazz), constructedSet);
			} while(true);
			
		} while(!list.isEmpty());
		
	}
	
	private ReferenceLinkedSet createReferenceLinkedSet(BT_Class clazz, BT_Field linkingField) {
		ReferenceLinkedSetList list = new ReferenceLinkedSetList();
		list.addFieldTarget(linkingField);
		return createSet(list);
	}
	
	private ReferenceLinkedSet createReferenceLinkedSet(BT_Class clazz, BT_Method linkingMethod) {
		ReferenceLinkedSetList list = new ReferenceLinkedSetList();
		list.addMethodTarget(linkingMethod);
		return createSet(list);
	}
	
	private ReferenceLinkedSet createSet(ReferenceLinkedSetList list) {
		ReferenceLinkedSet constructedSet = new ReferenceLinkedSet();
		serviceList(constructedSet, list);
		if(constructedSet.classCount() <= 1) {
			//we dismantle this set, because RenameableClass has
			//more complex techniques for renaming members that are not accessed from
			//outside the declaring class
			
			Iterator iterator = constructedSet.iterator();
			while(iterator.hasNext()) {
				Object next = iterator.next();
				unrecordMember((BT_Member) next);
			}
			return null;
		}
		return constructedSet;
	}
	
	
	/**
	 * Consider the following scenario:
	 * class A {
	 * 	int x() {
	 * 		B.h();
	 *  	return B.g();
	 *  }
	 * }
	 * class B {
	 * 	int g() {
	 * 		return 0;
	 * 	}
	 *  void h() {}
	 * }
	 * 
	 * If A.x() is in out set, then it is also beneficial to include members of B in our set,
	 * because the constant pool of A has method references to B.g() and B.h() and thus we can duplicate 
	 * the UTF8 name of these reference if the members of B have the same name.  In addition, it
	 * is best to first choose members of B that have the same signature, so it is best to choose
	 * B.g for the set instead of B.h.  That way the name-type of B.g and A.x can be duplicated
	 * in the constant pool of A.
	 * 
	 * The same holds true for fields, here is an example:
	 * class A {
	 * 	public int x;
	 * }
	 * class B extends A {
	 * 	public short y;
	 * }
	 * class C {
	 * 	int z;
	 *  void y() {
	 * 		int g = A.x + B.y;
	 * 	}	
	 * }
	 * If C.y() is in the set, it is best to include A.x in the set as well, duplicating
	 * both the UTF8 name and the identical name-types of A.x and C.z in the constant pool of C.
	 * But we cannot add A.y as well because a reference to B.x (accessing A.x through B) would
	 * look up the wrong field if we attempted to give A.x and B.y the same name.	 
	 */
	private void collectNewMembersFromCode(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz) {
		BT_MethodVector methods = clazz.getMethods();
		BT_FieldVector fields = clazz.getFields();
		for (int i = 0; i < methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			BT_CodeAttribute code = method.getCode();
			if (code == null) {
				continue;
			}
			List fieldTargetList = new ArrayList();
			List methodTargetList = new ArrayList();
			BT_InsVector inst = code.getInstructions();
			for (int j = 0; j < inst.size(); j++) {
				BT_Ins instruction = inst.elementAt(j);
				if(instruction instanceof BT_MethodRefIns) {
					BT_MethodRefIns methodRefInstruction = (BT_MethodRefIns) instruction;
					BT_Method target = methodRefInstruction.getTarget();
					BT_Class targetClass = target.getDeclaringClass();
					if(!targetClass.equals(clazz) && !memberIsNotAvailable(target)) {
						//TODO we should have different lists instead of simply ordering the list
						//ie we call list.addMethodTarget(target) on favourable ones first and then
						//less favourable ones later
						//same goes for fields
						//TODO also, we are checking for methods -- in the class -- with the same signature first
						//instead of methods -- in the class and ALSO in the set -- because we have not yet added class members to the set, which we should do
						
						
						//check if we have a method in the current class with the same signature
						if(containsMethodWithSignature(methods, target.getSignature())) {
							list.addMethodTarget(target);
						}
						else {
							methodTargetList.add(target);	
						}
					}
				}
				else if(instruction instanceof BT_FieldRefIns) {
					BT_FieldRefIns fieldRefInstruction = (BT_FieldRefIns) instruction;
					BT_Field target = fieldRefInstruction.getTarget();
					BT_Class targetClass = target.getDeclaringClass();
					if(!targetClass.equals(clazz) && !memberIsNotAvailable(target)) {
						//check if we have a field in the current class with the same field type
						if(containsFieldWithType(fields, target.getFieldType())) {
							list.addFieldTarget(target);
						}	
						else {
							fieldTargetList.add(target);		
						}
					}
				}
			}
			for(int j=0; j<methodTargetList.size(); j++) {
				list.addMethodTarget((BT_Method) methodTargetList.get(j));
			}
			for(int j=0; j<fieldTargetList.size(); j++) {
				list.addFieldTarget((BT_Field) fieldTargetList.get(j));
			}
		}
	}
	
	private static boolean containsMethodWithSignature(BT_MethodVector methods, BT_MethodSignature sig) {
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(RelatedMethodMap.signaturesMatch(method.getSignature(), sig)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean containsFieldWithType(BT_FieldVector fields, BT_Class type) {
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			if(field.getFieldType().equals(type)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Consider the following scenario:
	 * class A {
	 * 	int x() {}
	 * }
	 * class B {
	 * 	int y = A.x();
	 * 	int g() {
	 * 		return A.x();
	 * 	}
	 *  void h() {}
	 * }
	 * 
	 * If A.x() is in our set, then it is also beneficial to include members of B in our set,
	 * because the constant pool of B has a method reference to A.x() and thus we can duplicate 
	 * the UTF8 name of this reference if the members of B have the same name.  In addition, it
	 * is best to first choose members of B that have the same signature, so it is best to choose
	 * B.g for the set instead of B.h.  That way the name-type of B.g and A.x can be duplicated
	 * in the constant pool of B. 
	 */
	private void collectNewMembersFromCallSites(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz) {
		BT_MethodVector methods = constructedSet.getNonPrivateMethodsFromClass(clazz);
		if(methods != null) {
			for (int i=0; i < methods.size(); i++) {
				BT_Method method = methods.elementAt(i);
				BT_MethodCallSiteVector callers = method.callSites;
				
				/* callers to each method must have access */
				if(callers != null && callers.size() > 0) {
					for (int j = 0; j < callers.size(); j++) {
						BT_MethodCallSite callSite = callers.elementAt(j);
						BT_Class caller = callSite.getFrom().getDeclaringClass();
						list.addClassTarget(caller, method.getSignature());
					}
				}
			}
		}
	}
	
	/**
	 * Consider the following scenario:
	 * class A {
	 * 	int x;
	 * }
	 * class B {
	 *  short b;
	 * 	int y = A.x;
	 * 	int g() {
	 * 		return A.x;
	 * 	}
	 *
	 * }
	 * 
	 * If A.x is in out set, then it is also beneficial to include members of B in our set,
	 * because the constant pool of B has a field reference to A.x and thus we can duplicate 
	 * the UTF8 name of this reference if the members of B have the same name.  In addition, it
	 * is best to first choose members of B that have the same field type, so it is best to choose
	 * B.y for the set instead of B.b.  That way the name-type of B.y and A.x can be duplicated
	 * in the constant pool of B. 
	 */
	private void collectNewMembersFromAccessors(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz) {
		BT_Field field = constructedSet.getNonPrivateFieldFromClass(clazz);
		if(field != null) {
			BT_AccessorVector accessors = field.accessors;
			if(accessors != null && accessors.size() > 0) {		
				for (int j = 0; j < accessors.size(); j++) {
					BT_Accessor accessor = accessors.elementAt(j);
					BT_Class accessorClass = accessor.getFrom().getDeclaringClass();
					list.addClassTarget(accessorClass, field.getFieldType());
				}
			}
		}
	}
	
	
	private void collectNewMembers(ReferenceLinkedSetList list, BT_Class clazz, BT_Class relatedClasses[], ReferenceLinkedSet constructedSet) {
		collectNewMembersFromCode(constructedSet, list, clazz);
		collectMembersFromClass(list, clazz, relatedClasses, constructedSet);
		if(constructedSet.containsFieldFromClass(clazz)) {
			collectNewMembersFromAccessors(constructedSet, list, clazz);
		}
		if(constructedSet.containsMethodFromClass(clazz)) {
			collectNewMembersFromCallSites(constructedSet, list, clazz);
		}
	}
	
	private void addMethod(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz, BT_MethodVector relatedMethods) {
		for(int i=0; i<relatedMethods.size(); i++) {
			BT_Method relatedMethod = relatedMethods.elementAt(i);
			addMemberToConstructedSet(constructedSet, relatedMethod.getDeclaringClass(), relatedMethod);
		}
		for(int i=0; i<relatedMethods.size(); i++) {
			BT_Method relatedMethod = relatedMethods.elementAt(i);
			collectNewMembers(list, relatedMethod.getDeclaringClass(), getRelatedClasses(relatedMethod.getDeclaringClass()), constructedSet);
		}
	}
	
	private void addField(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz, BT_Field linkingField, BT_Class relatedClasses[]) {
		addMemberToConstructedSet(constructedSet, clazz, linkingField);
		collectNewMembers(list, clazz, relatedClasses, constructedSet);
	}
	

	private void addMemberToConstructedSet(ReferenceLinkedSet constructedSet, BT_Class clazz, BT_Member member) {
		recordMemberSet(constructedSet, member);
		constructedSet.addMemberFromClass(clazz, member);
	}
	
	private void recordMemberSet(ReferenceLinkedSet constructedSet, BT_Member member) {
		ownedItems.add(member);
		linkedSetMap.put(member, constructedSet);
	}
	
	private void unrecordMember(BT_Member member) {
		linkedSetMap.remove(member);
		ownedItems.remove(member);
	}
	
	/**
	 * find a member in clazz whose name can be duplicated
	 */
	private boolean tryLinkToClass(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz) {
		BT_FieldVector accessorFields = clazz.getFields();
		for(int i=0; i<accessorFields.size(); i++) {
			BT_Field accessorField = accessorFields.elementAt(i);
			if(tryLinkToClass(constructedSet, list, clazz, accessorField)) {
				return true;
			}		
		}
		
		BT_MethodVector accessorMethods = clazz.getMethods();
		for(int i=0; i<accessorMethods.size(); i++) {
			BT_Method accessorMethod = accessorMethods.elementAt(i);
			if(tryLinkToClass(constructedSet, list, clazz, accessorMethod)) {
				return true;
			}		
		}
		
		return false;
	}
	
	private boolean isSuperClass(BT_Class testClass, BT_Class child) {
		return ((JaptClass) testClass).isClassAncestorOf(child);
	}
//	private boolean isSuperClass(BT_Class testClass, BT_Class child) {
//		BT_Class superClass = child;
//		while(true) {
//			superClass = superClass.getSuperClass();
//			if(superClass == null) {
//				return false;
//			}
//			if(testClass.equals(superClass)) {
//				return true;
//			}
//		}
//	}
	 
	private boolean tryLinkToClass(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz, BT_Method linkingMethod) {
		if(memberIsNotAvailable(linkingMethod)) {
			return false;
		}
		BT_MethodVector relatedMethods = relatedMethodMap.getRelatedMethods(linkingMethod);
		if(methodIsAllowed(constructedSet, relatedMethods)) {
			addMethod(constructedSet, list, clazz, relatedMethods);
			return true;
		}
		return false;
		
	}
	
	private boolean fieldIsAllowed(ReferenceLinkedSet constructedSet, BT_Class clazz, BT_Class relatedClasses[]) {
		 for(int i=0; i<relatedClasses.length; i++) {
			if(isSuperClass(relatedClasses[i], clazz)) {
				if(constructedSet.containsNonPrivateFieldFromClass(relatedClasses[i])) {
					return false;
				}
			}
			else { //is same or is child of relatedMethodClass
				if(constructedSet.containsFieldFromClass(relatedClasses[i])) {
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean methodIsAllowed(ReferenceLinkedSet constructedSet, BT_MethodVector relatedMethods) {
		BT_MethodSignature sig = relatedMethods.firstElement().getSignature();
		for(int k=0; k<relatedMethods.size(); k++) {
			BT_Method relatedMethod = relatedMethods.elementAt(k);
			BT_Class relatedMethodClass = relatedMethod.getDeclaringClass();
			BT_Class relatedMethodClasses[] = getRelatedClasses(relatedMethod.getDeclaringClass());
			for(int i=0; i<relatedMethodClasses.length; i++) {
				if(isSuperClass(relatedMethodClasses[i], relatedMethodClass)) {
					if(constructedSet.containsNonPrivateMethodFromClassWithParameters(relatedMethodClasses[i], sig)) {
						return false;
					}
				}
				else { //is same or is child of relatedMethodClass
					if(constructedSet.containsMethodFromClassWithParameters(relatedMethodClasses[i], sig)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private boolean tryLinkToClass(ReferenceLinkedSet constructedSet, ReferenceLinkedSetList list, BT_Class clazz, BT_Field linkingField) {
		if(memberIsNotAvailable(linkingField)) {
			return false;
		}
		BT_Class relatedClasses[] = getRelatedClasses(clazz);
		if(fieldIsAllowed(constructedSet, clazz, relatedClasses)) {
			addField(constructedSet, list, clazz, linkingField, relatedClasses);
			return true;
		}
		return false;
	}
	

	/**
	 * Consider the following scenario:
	 * class A {
	 * 	int x;
	 *  A a;
	 *  void g() {}
	 *  void h() {}
	 *  int i(int h) {return 0;}
	 * }
	 * 
	 * If we have a member of A in the set, it is best to include as many other members of A
	 * as possible, thus duplicating names in the constant pool of A.  Note that we cannot
	 * include both A.g() and A.h() because of their identical parameter lists (so we cannot
	 * overload the same name) and we cannot include both A.x and A.a because we cannot
	 * have two fields of the same name.  An optimal solution is the following:
	 * 
	 * class A {
	 * 	int x;
	 *  A A;
	 *  void A() {}
	 *  void h() {}
	 *  int A(int h) {return 0;}
	 * }
	 * 
	 */
	private void collectMembersFromClass(ReferenceLinkedSetList list, BT_Class clazz, BT_Class relatedClasses[], ReferenceLinkedSet constructedSet) {
		if(!constructedSet.containsFieldFromClass(clazz)) {
			//try to select a field for our collection, preferably private
			BT_FieldVector fields = clazz.getFields();
			
			//try private fields
			BT_Field selectedField = null;
			for(int i=0; i<fields.size(); i++) {
				BT_Field field = fields.elementAt(i);
				if(memberIsNotAvailable(field)) {
					continue;
				}
				if(field.isPrivate()) {
					selectedField = field;
					break;
				}
			}
			
			//try any field
			if(selectedField == null) {
				for(int i=0; i<fields.size(); i++) {
					BT_Field field = fields.elementAt(i);
					if(memberIsNotAvailable(field)) {
						continue;
					}
					selectedField = field;
					break;
				}
			}
			
			if(selectedField != null && fieldIsAllowed(constructedSet, clazz, relatedClasses)) {
				addMemberToConstructedSet(constructedSet, clazz, selectedField);
			}
		}
	
	
		
		BT_MethodVector methods = clazz.getMethods();
		for(int j=0; j<methods.size(); j++) {
			BT_Method method = methods.elementAt(j);
			if(memberIsNotAvailable(method)) {
				continue;
			}
			
			BT_MethodVector relatedMethods = relatedMethodMap.getRelatedMethods(method);
			if(methodIsAllowed(constructedSet, relatedMethods)) {
				for(int k=0; k<relatedMethods.size(); k++) {
					BT_Method relatedMethod = relatedMethods.elementAt(k);
					addMemberToConstructedSet(constructedSet, relatedMethod.getDeclaringClass(), relatedMethod);
				}
				for(int k=0; k<relatedMethods.size(); k++) {
					BT_Method relatedMethod = relatedMethods.elementAt(k);
					if(!method.equals(relatedMethod)) {
						list.addClassMemberTarget(relatedMethod.getDeclaringClass());
					}
				}
			}
		}
	}
	
	private boolean memberIsNotAvailable(BT_Item item) {
		return nameHandler.nameIsFixed(item)  //this member has a fixed name
			|| ownedItems.contains(item); //member is already owned by some other set
	}
	
	private BT_Class[] getRelatedClasses(BT_Class clazz) {
		return repository.getRelatedClassCollector(clazz).getAllRelatedClasses();
	}
	

}



	
	
