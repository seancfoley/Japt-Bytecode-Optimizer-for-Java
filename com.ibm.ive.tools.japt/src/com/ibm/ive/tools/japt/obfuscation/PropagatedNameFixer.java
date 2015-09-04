package com.ibm.ive.tools.japt.obfuscation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptMethod;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassRefIns;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldRefIns;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_HashedFieldVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 * <p>
 * Fixes names that need to be fixed due to a relation with a previously fixed name.
 * </p><p>
 * In the case of methods, these relations include method names that have been overrided in child classes, 
 * method names that are overriding parent methods, and method names that implement interfaces.
 * </p><p>
 * In the case of classes, this includes classes (and contained field and method members) 
 * that need visibility to (or must be visible from) other classes in the same package with a fixed package name.
 * </p>
 */
class PropagatedNameFixer {

	private NameHandler nameHandler;
	boolean retainingPermissions;
	JaptRepository repository;
	String packageBaseName;
	
	public PropagatedNameFixer(JaptRepository repository, NameHandler nameHandler, boolean retainingPermissions,
			String packageBaseName) {
		this.retainingPermissions = retainingPermissions;
		this.nameHandler = nameHandler;
		this.repository = repository;
		this.packageBaseName = packageBaseName;
	}
	
	void propagateFixedMethodNames(RelatedMethodMap relatedMethodMap) {
		BT_MethodVector methodSets[] = relatedMethodMap.getAllRelatedMethods();
		for(int i=0; i<methodSets.length; i++) {
			BT_MethodVector methodSet = methodSets[i];
			//boolean namesAreFixed = false;
			for(int j=0; j<methodSet.size(); j++) {
				BT_Method method = methodSet.elementAt(j);
				if(nameHandler.nameIsFixed(method)) {
					for(int k=0; k<methodSet.size(); k++) {
						BT_Method methodx = methodSet.elementAt(k);
						if(!nameHandler.nameIsFixed(methodx)) {
							nameHandler.fixName(methodx, "related to other fixed method");
						}
					}
					break;
				}
			}
		}
	}
		
	private static boolean separateClassesSamePackage(BT_Class one, BT_Class two) {
		return !one.equals(two) && one.isInSamePackage(two);
	}
	
	/**
	 * @return true if default access is required to access target from source
	 */
	private static boolean requiresAccessibility(BT_Class source, BT_Class target) {
		return target.isDefaultAccess() && !target.isPrimitive() && separateClassesSamePackage(source, target);
	}
	
	private boolean isSeparateFixedAndDefaultAccess(BT_Class source, BT_Class target) {
		return requiresAccessibility(source, target) && isFixedToPackage(target);
	}
	
	private boolean isFixedToPackage(BT_Class clazz) {
		return nameHandler.packageNameIsFixed(clazz) || nameHandler.nameIsFixed(clazz);
	}
	
	public boolean canChangeClass(BT_Class clazz) {
		return repository.isInternalClass(clazz);
	}
	
	public boolean canChangeMethodAndKids(BT_Method method) {
		/* 
		 * Note that we know the method is not a private non-static non-constructor method 
		 * because we have ruled that out in canPreserveAccess.
		 * 
		 * The becomeVisibleFrom method will therefore change the base method to public.  So we must ensure the method
		 * and its kids can all become public.
		 * 
		 */
		if(!canChangeClass(method.getDeclaringClass())) {
			return false;
		}
		BT_MethodVector kids = method.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Method kid = kids.elementAt(i);
			if(!kid.isPublic() && !canChangeMethodAndKids(kid)) {
				return false;
			}
		}
		return true;
	}
	
	

	
	/** 
	 * The given clazz is fixed to its current package.  Because of this, other classes and class members accessed
	 * from this class must be checked to see if they too must remain in the same package as the given class.
	 * 
	 * This method finds classes for which clazz requires package visibility (ie must remain in the same package)
	 * by checking the following types of access.
	 * 
	 * the following types of access are monitored:
	 * - to parent classes
	 * - from methods of other classes to methods of this class
	 * - from methods of other classes to fields of this class
	 * - from methods of this class to methods/fields of other classes 
	 * - the types of fields of this class (the types must be accessible)
	 * - the signatures and return types of methods of this class (classes in the signature must be accessible)
	 * - class reference instructions in methods of this class such as new, checkcast, instanceof
	 * 
	 * @param unpropagatedList any instance of BT_Class which this method fixes to the current package is added to this list
	 * @param clazz the class fixed to its current package
	 */
	private void propagateFixedClass(List unpropagatedList, BT_Class clazz, RelatedMethodMap map) {
		
		 /* package visible parents */	
		 
		BT_ClassVector parents = clazz.getParents();
		for (int i = 0; i < parents.size(); i++) {
			BT_Class parent = parents.elementAt(i);
			if (requiresAccessibility(clazz, parent) && !isFixedToPackage(parent)) {
				//we fix the name of the parent class or we make it public
				if(retainingPermissions || !canChangeClass(parent)) {
					setFixed(unpropagatedList, parent);
				} else {
					markConditionallyPublic(parent, clazz);
				}
			}
		}
		
		/* methods */
		
		BT_MethodVector methods = clazz.getMethods();
		for (int i = 0; i < methods.size(); i++) {
			
			
			JaptMethod method = (JaptMethod) methods.elementAt(i);
			
			/* certain instructions in the method code might require visibility of other classes */
			
			BT_CodeAttribute code = method.getCode();
			BT_InsVector inst;
			if (code != null && (inst = code.getInstructions()) != null) {  
				for (int j = 0; j < inst.size(); j++) {
					BT_Ins instruction = inst.elementAt(j);
					if (instruction instanceof BT_ClassRefIns) {
						BT_Class target = removeDimensions(instruction.getClassTarget());
						if (requiresAccessibility(clazz, target) && !isFixedToPackage(target)) {
							//we fix the target class or make it public
							if(retainingPermissions || !canChangeClass(target)) {
								setFixed(unpropagatedList, target);
							} else {
								markConditionallyPublic(target, clazz);
							}
						}
					} else if(instruction instanceof BT_MethodRefIns) { 
						BT_MethodRefIns methodRefInstruction = (BT_MethodRefIns) instruction;
						
						BT_Method referencedTarget = methodRefInstruction.getTarget();
						BT_Class referencedClass = methodRefInstruction.getClassTarget();
						
						if(referencedClass.isPublic() && referencedTarget.isPublic()) {
							continue;
						}
						
						BT_MethodVector possibleTargets = (BT_MethodVector) map.getOverridingMethods(referencedTarget).clone();
						possibleTargets.addElement(referencedTarget);
						
						if(requiresAccessibility(clazz, referencedClass) && !isFixedToPackage(referencedClass)) {
							//access to another class in the same package	
							//I am not accessible from everywhere
							if(retainingPermissions || !canChangeClass(referencedClass)) {
								setFixed(unpropagatedList, referencedClass);
							} else {
								markConditionallyPublic(referencedClass, clazz);
							}
						}
						
						
						
						for(int k=0; k<possibleTargets.size(); k++) {
							BT_Method target = possibleTargets.elementAt(k);
							BT_Class targetDeclaringClass = target.getDeclaringClass(); //it's not the declaring class that matters, it's the target class
							
							
							//we check the possible invoked methods
							if(separateClassesSamePackage(clazz, targetDeclaringClass) && !isFixedToPackage(targetDeclaringClass)) {
								
								//the method is in the same package
								if(target.isDefaultAccess() 
										|| (target.isProtected() 
												&& !(clazz.isDescendentOf(targetDeclaringClass) && target.isStatic())  
											)
									) {
									//we fix the caller class or we make the accessed method public
									if(retainingPermissions || !canChangeMethodAndKids(target)) {
										setFixed(unpropagatedList, targetDeclaringClass);
									} else {
										markConditionallyPublic(target, clazz);
									}
								}
							
							}
						}
					} else if(instruction instanceof BT_FieldRefIns) {
						BT_FieldRefIns fieldRefInstruction = (BT_FieldRefIns) instruction;
						BT_Field target = fieldRefInstruction.getTarget();
						BT_Class targetClass = fieldRefInstruction.getClassTarget();
						
						if(requiresAccessibility(clazz, targetClass) && !isFixedToPackage(targetClass)) {
							if(retainingPermissions || !canChangeClass(targetClass)) {
								setFixed(unpropagatedList, targetClass);
							} else {
								markConditionallyPublic(targetClass, clazz);
							}
						}
						
						
						BT_Class targetDeclaringClass = target.getDeclaringClass();
						if(separateClassesSamePackage(clazz, targetDeclaringClass) && !isFixedToPackage(targetDeclaringClass)) {
							//access to another class in the same package	
							
							if(target.isDefaultAccess() 
									|| (target.isProtected() 
											&& !(clazz.isDescendentOf(targetDeclaringClass) && target.isStatic())  
										)
								) {
								if(retainingPermissions || !canChangeClass(targetDeclaringClass)) {
									setFixed(unpropagatedList, targetDeclaringClass);
								} else {
									markConditionallyPublic(target, clazz);
								}
							}	
						
						}
					}
				}
			}
				
			/* the method signature requires visibility of other classes */
			
			BT_MethodSignature signature = method.getSignature();
			BT_Class returnType = removeDimensions(signature.returnType);
			if (requiresAccessibility(clazz, returnType) && !isFixedToPackage(returnType)) {
				//we fix the return type or make it public
				if(retainingPermissions || !canChangeClass(returnType)) {
					setFixed(unpropagatedList, returnType);
				} else {
					markConditionallyPublic(returnType, clazz);
				}
			}
			for (int j = 0; j < signature.types.size(); j++) {
				BT_Class type = removeDimensions(signature.types.elementAt(j));
				if (requiresAccessibility(clazz, type) && !isFixedToPackage(type)) {
					
					//fix the signature type or make it public
					if(retainingPermissions || !canChangeClass(type)) {
						setFixed(unpropagatedList, type);
					} else {
						markConditionallyPublic(type, clazz);
					}
				}
			}
			
			
			/* any method we override must remain visible 
			 * 
			 * we need not check methods implemented since they are always public
			 * we have also already checked whether all parents are visible
			 */
			BT_MethodVector methodParents = method.getParents();
			for (int j = 0; j < methodParents.size(); j++) {
				BT_Method parentMethod = methodParents.elementAt(j);
				if(parentMethod.isPublic() || parentMethod.isPrivate() || parentMethod.isProtected()) {
					continue;
				}
				
				BT_Class type = parentMethod.getDeclaringClass();
				if(retainingPermissions || !canChangeMethodAndKids(parentMethod)) {
					setFixed(unpropagatedList, type);
				} else {
					markConditionallyPublic(parentMethod, clazz);
				}
			}
			
			
		}
			
		
		/* any classes accessing a field must have access */
		BT_FieldVector fields = clazz.getFields();
		for (int i = 0; i < fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
//			BT_AccessorVector accessors = field.accessors;		
//			if(accessors != null && accessors.size() > 0 
//				&& !field.isPrivate() && !field.isPublic()) {
//					
//				for (int j = 0; j < accessors.size(); j++) {
//					BT_Accessor accessor = accessors.elementAt(j);
//					BT_Class accessorClass = accessor.getFrom().getDeclaringClass();
//					if (separateClassesSamePackage(clazz, accessorClass) && !isFixedToPackage(accessorClass)) {
//						//either we fix the accessor class or we make the field public
//						if(field.isDefaultAccess() || (field.isProtected() && !accessorClass.isDescendentOf(clazz))) {
//							if(retainingPermissions || !canChangeClass(clazz)) {
//								unpropagatedList.add(accessorClass);
//								nameHandler.fixPackageName(accessorClass);
//								continue;
//							} else {
//								makePublic(field, accessorClass);
//								xxx can jump out of loop when this happens
//								xxx this invalidates previous accessorClasses
//							}
//						}			
//					}
//				}
//			}
		
			
			/* the field type must be accessible */
			BT_Class fieldClass = removeDimensions(field.getFieldType());
			if (requiresAccessibility(clazz, fieldClass) && !isFixedToPackage(fieldClass)) {
				//make fieldClass public or make it fixed
				if(retainingPermissions || !canChangeClass(fieldClass)) {
					setFixed(unpropagatedList, fieldClass);
				} else {
					markConditionallyPublic(fieldClass, clazz);
				}
			}
		}
	}
	
	
	
	/* TODO
	 * NOTE: could use the newer JIKESBT referencing links instead of checking for certain instruction
	 * types above and below, this would likely speed things up a little, see the comments on the
	 * "dereferenced item links" in BT_Class.
	 * 
	 * Use code.referenceSites, code.accessedFields and code.calledMethods 
	 * 
	 */
	
	
	/**
	 * The given clazz is not fixed to its current package.  We must check if it should be, by checking if it should remain
	 * in the same package as other classes that have already been fixed to their package.
	 * 
	 * This method finds classes for which clazz requires package visibility (ie must remain in the same package)
	 * by checking the following types of access.
	 * 
	 * This method covers types of access which is not covered by propagateFixedClass(List unpropagatedList, BT_Class clazz),
	 * which checks parents but does not check children, does not check class reference instructions in both directions
	 * (in both directions means we must check for a class referencing a fixed class and we must also check for 
	 * a fixed class referencing a class), field types in both directions and method signatures in both directions.
	 * 
	 * This method covers only class access permissions and not method/field access permissions.
	 * 
	 * 
	 * @param unpropagatedList if a class is fixed to its current package then it is added to this list
	 */
	private void checkReferencesToFixedClasses(List unpropagatedList, BT_Class clazz) {
		UnfixedClass unfixed = null;
		
	 	// package visible parents
	 
		BT_ClassVector parents = clazz.getParents();
		for (int i = 0; i < parents.size(); i++) {
			BT_Class parent = parents.elementAt(i);
			if(isSeparateFixedAndDefaultAccess(clazz, parent)) {
				if(retainingPermissions || !canChangeClass(parent)) {
					setFixed(unpropagatedList, clazz);
					return;
				} else {
					if(unfixed == null) {
						unfixed = getUnfixed(clazz);
					}
					unfixed.classesToChange.addUnique(parent);
				}
			}
		}
		
		
		// methods
		
		BT_MethodVector methods = clazz.getMethods();
		for (int i = 0; i < methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
				
			// certain instructions in the method code might require visibility of other classes
			
			BT_CodeAttribute code = method.getCode();
			if (code != null) {
				BT_InsVector inst = code.getInstructions();
				for (int j = 0; j < inst.size(); j++) { 
					BT_Ins instruction = inst.elementAt(j);
					if (!instruction.isInvokeIns() && !instruction.isFieldAccessIns() && !instruction.isClassRefIns()) {
						continue;
					}
					
					//Note: We could instead use the BT_Class.referenceSites of each fixed class
					//instead of iterating through all BT_ClassRefIns instructions to see if they access
					//a fixed class.
					//However, for the BT_MethodRefIns and BT_FieldRefIns instructions there is no alternative
					//because they are not included in the BT_Class.referenceSites.
					//We would need to look at the callsites and accessors of the methods and fields in the class
					//and all its parents to determine which ones reference the class
					BT_Class target = removeDimensions(instruction.getClassTarget());
					if(isSeparateFixedAndDefaultAccess(clazz, target)) {
						if(retainingPermissions || !canChangeClass(target)) {
							setFixed(unpropagatedList, clazz);
							return;
						} else {
							if(unfixed == null) {
								unfixed = getUnfixed(clazz);
							}
							unfixed.classesToChange.addUnique(target);
						}
					}
					
					if (instruction.isInvokeIns()) {
						BT_MethodRefIns methodRefIns = (BT_MethodRefIns) instruction;
						BT_Method targetMethod = methodRefIns.getTarget();
						BT_Class targetDeclaringClass = targetMethod.getDeclaringClass();
						if (separateClassesSamePackage(clazz, targetDeclaringClass)) {
							if(targetMethod.isDefaultAccess() || (targetMethod.isProtected() && !clazz.isDescendentOf(targetDeclaringClass))) {
								if(isFixedToPackage(targetDeclaringClass)) {
									//we fix the caller class or we make the accessed method public
									if(retainingPermissions || !canChangeMethodAndKids(targetMethod)) {
										setFixed(unpropagatedList, clazz);
										return;
									} else {
										if(unfixed == null) {
											unfixed = getUnfixed(clazz);
										}
										unfixed.methodsToChange.addUnique(targetMethod);
									}
								}
							}
						}
						
					} else if (instruction.isFieldAccessIns()) {
						BT_FieldRefIns fieldRefIns = (BT_FieldRefIns) instruction;
						BT_Field targetField = fieldRefIns.getTarget();
						BT_Class targetDeclaringClass = targetField.getDeclaringClass();
						if (separateClassesSamePackage(clazz, targetDeclaringClass)) {
							if(targetField.isDefaultAccess() 
									|| (targetField.isProtected() && !clazz.isDescendentOf(targetDeclaringClass))) {
								if(isFixedToPackage(targetDeclaringClass)) {
									//we fix the caller class or we make the accessed method public
									if(retainingPermissions || !canChangeClass(targetDeclaringClass)) {
										setFixed(unpropagatedList, clazz);
										return;
									} else {
										if(unfixed == null) {
											unfixed = getUnfixed(clazz);
										}
										unfixed.fieldsToChange.addUnique(targetField);
									}
								}
							}
						}
					}
				}
			}
			
			// the method signature requires visibility of other classes
			BT_MethodSignature signature = method.getSignature();
			BT_Class returnType = removeDimensions(signature.returnType);
			if (isSeparateFixedAndDefaultAccess(clazz, returnType)) {
				if(retainingPermissions || !canChangeClass(returnType)) {
					setFixed(unpropagatedList, clazz);
					return;
				} else {
					if(unfixed == null) {
						unfixed = getUnfixed(clazz);
					}
					unfixed.classesToChange.addUnique(returnType);
				}
			}
		
			for (int j = 0; j < signature.types.size(); j++) {
				BT_Class type = removeDimensions(signature.types.elementAt(j));
				if (isSeparateFixedAndDefaultAccess(clazz, type)) {
					//fix the signature type or make it public
					if(retainingPermissions || !canChangeClass(type)) {
						setFixed(unpropagatedList, clazz);
						return;
					} else {
						if(unfixed == null) {
							unfixed = getUnfixed(clazz);
						}
						unfixed.classesToChange.addUnique(type);
					}
				}
			}
			
			//if there is a method with the same name and signature in the parent class but it is package access,
			//and not visible, but we both move to the default package, so they go from being unrelated to being
			//related?!!  we must fix the public child method to its package!
			if(!method.isConstructor() && !method.isStaticInitializer() && !method.isStatic()) {
				BT_Class superClass = clazz.getSuperClass();
				if(superClass != null) {
					BT_Method meth = superClass.findInheritedMethod(method.getName(), method.getSignature());
					if(meth != null && meth.isDefaultAccess() && !meth.isStatic() 
							&& (!isFixedToPackage(meth.getDeclaringClass()) || meth.getDeclaringClass().packageName().equals(packageBaseName))) {
						setFixed(unpropagatedList, clazz);
						return;
					}
				}
			}
			
			//must retain visibility to methods being overridden
			BT_MethodVector methodParents = method.getParents();
			for(int j=0; j<methodParents.size(); j++) {
				BT_Method parentMethod = methodParents.elementAt(j);
				if(parentMethod.isDefaultAccess()) {
					BT_Class declaringClass = parentMethod.getDeclaringClass();
					if (separateClassesSamePackage(clazz, declaringClass) && isFixedToPackage(declaringClass)) {
						//we fix the parent class or we make the parent method public
						if(retainingPermissions || !canChangeMethodAndKids(parentMethod)) {
							setFixed(unpropagatedList, clazz);
							return;
						} else {
							if(unfixed == null) {
								unfixed = getUnfixed(clazz);
							}
							unfixed.methodsToChange.addUnique(parentMethod);
						}
					}
				}
			}	
		}
					
		// must have access to field types
		BT_FieldVector fields = clazz.getFields();
		for (int i = 0; i < fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			// the field type must be accessible
			BT_Class fieldClass = removeDimensions(field.getFieldType());
			if (isSeparateFixedAndDefaultAccess(clazz, fieldClass)) {
				if(retainingPermissions || !canChangeClass(fieldClass)) {
					setFixed(unpropagatedList, clazz);
					return;
				} else {
					if(unfixed == null) {
						unfixed = getUnfixed(clazz);
					}
					unfixed.classesToChange.addUnique(fieldClass);
				}
			}
		}
	}
	
	void propagateFixedClassNames(RenameablePackage renameablePackage, RelatedMethodMap map) {
		BT_ClassVector classes = renameablePackage.getClasses();
		List unpropagatedList = new ArrayList();
		for(int index = 0; index < classes.size(); index++) {
			BT_Class clazz = classes.elementAt(index);
			if(isFixedToPackage(clazz) && !clazz.isPrimitive() && !clazz.isArray()) {
				unpropagatedList.add(clazz);
			}
		}
		while(!unpropagatedList.isEmpty()) {
			/* first we propagate the property of being "fixed" to all classes, methods and fields that are accessed from fixed classes */
			while(!unpropagatedList.isEmpty()) {
				BT_Class clazz = (BT_Class) unpropagatedList.remove(0);
				propagateFixedClass(unpropagatedList, clazz, map);
			}
			/* 
			 * Now we check all classes to see if they must fix themselves because they access fixed classes.
			 * In order to avoid fixing themselves, each class will try to store a persistent list of items that must become public later.
			 * If and when they do fix themselves, their persistent list of items is discarded. 
			 */
			for(int j=0; j<classes.size(); j++) {
				BT_Class otherClass = classes.elementAt(j);
				if(otherClass.isArray() || otherClass.isPrimitive()) {
					continue;
				}
				if(!isFixedToPackage(otherClass)) {
					checkReferencesToFixedClasses(unpropagatedList, otherClass);
				}	
			}
		}
		
		/*
		 * Go through the persistent lists of items that must become public and make them all public.
		 */
		Iterator iterator = unfixedClassMap.values().iterator();
		while(iterator.hasNext()) {
			UnfixedClass unfixed = (UnfixedClass) iterator.next();
			unfixed.makePublic();
		}
	}
	
	private void setFixed(List unpropagatedList, BT_Class clazz) {
		nameHandler.fixPackageName(clazz);
		unpropagatedList.add(clazz);
		unfixedClassMap.remove(clazz);
	}
	
	private void markConditionallyPublic(BT_Class publicClass, BT_Class unfixedClass) {
		UnfixedClass unfixed = getUnfixed(unfixedClass);
		unfixed.classesToChange.addUnique(publicClass);
	}
	
	private void markConditionallyPublic(BT_Method publicMethod, BT_Class unfixedClass) {
		UnfixedClass unfixed = getUnfixed(unfixedClass);
		unfixed.methodsToChange.addUnique(publicMethod);
	}
	
	private void markConditionallyPublic(BT_Field publicField, BT_Class unfixedClass) {
		UnfixedClass unfixed = getUnfixed(unfixedClass);
		unfixed.fieldsToChange.addUnique(publicField);
	}
	
	private HashMap unfixedClassMap = new HashMap();
	
	private UnfixedClass getUnfixed(BT_Class clazz) {
		UnfixedClass unfixed = (UnfixedClass) unfixedClassMap.get(clazz);
		if(unfixed == null) {
			unfixed = new UnfixedClass(clazz);
			unfixedClassMap.put(clazz, unfixed);
		}
		return unfixed;
	}
	
	/**
	 * an unfixed class stores the classes, methods and fields that must become public
	 * for the class to remain unfixed.  To be "unfixed" means that it is allowed to migrate
	 * to another package; but first the listed classes, methods and fields must be made public.
	 */
	static class UnfixedClass {
		BT_HashedClassVector classesToChange = new BT_HashedClassVector();
		BT_HashedMethodVector methodsToChange = new BT_HashedMethodVector();
		BT_HashedFieldVector fieldsToChange = new BT_HashedFieldVector();
		final BT_Class clazz;
		
		UnfixedClass(BT_Class clazz) {
			this.clazz = clazz;
		}
		
		private void makePublic() {
			JaptFactory factory = ((JaptRepository) clazz.getRepository()).getFactory();
			for(int i=0; i<classesToChange.size(); i++) {
				BT_Class toChange = classesToChange.elementAt(i);
				if(toChange.isPublic()) {
					continue;
				}
				makePublic(toChange, clazz, factory);
			}
			for(int i=0; i<methodsToChange.size(); i++) {
				BT_Method toChange = methodsToChange.elementAt(i);
				if(toChange.isPublic()) {
					continue;
				}
				makePublic(toChange, clazz, factory);
			}
			for(int i=0; i<fieldsToChange.size(); i++) {
				BT_Field toChange = fieldsToChange.elementAt(i);
				if(toChange.isPublic()) {
					continue;
				}
				makePublic(toChange, clazz, factory);
			}
		}
		
		private void makePublic(BT_Class clazz, BT_Class accessor, JaptFactory factory) {
			clazz.becomePublic();
			factory.noteAccessPermissionsChanged(clazz, BT_Item.DEFAULT_ACCESS, accessor);
		}
		
		private void makePublic(BT_Field field, BT_Class accessor, JaptFactory factory) {
			short oldPermission = field.getAccessPermission();
			field.becomePublic();
			factory.noteAccessPermissionsChanged(field, oldPermission, accessor);
		}
		
		private void makePublic(BT_Method method, BT_Class caller, JaptFactory factory) {
			short oldPermission = method.getAccessPermission();
			method.becomePublic();
			factory.noteAccessPermissionsChanged(method, oldPermission, caller);
			BT_MethodVector kids = method.getKids();
			for(int i=0; i<kids.size(); i++) {
				BT_Method kid = kids.elementAt(i);
				if(!kid.isPublic()) {
					makePublic(kid, caller, factory);
				}
			}
		}
	}
	
	/**
	 * @return If the parameter is array type returns its component type
	 */
	private static BT_Class removeDimensions(BT_Class clazz) {
		return clazz.isArray() ? clazz.arrayType : clazz;
	}
}
