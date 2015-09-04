/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Japt Refactor Extension
 *
 * Copyright IBM Corp. 2008
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U. S. Copyright Office.
 */
package com.ibm.ive.tools.japt.refactorInner;

import java.util.Enumeration;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.*;

/**
 * RefactorExtension feature class which provides refactoring
 * of anonymous inner classes.
 */
public class AnonymousInnerClassRefactor extends GenericRefactor {
	/**
	 * Internal method for embedding copyright
	 */
	static String copyright() {
		return Copyright.IBM_COPYRIGHT;
	}
	
	/**
	 * Constructs an AnonymousInnerClassRefactor.
	 * Candidate classes are sorted by default upon refactoring.
	 * @param repository JaptRepository object, usually given through Japt extension.
	 * @param logger Japt Logger object, usually given through Japt extension.
	 */
	public AnonymousInnerClassRefactor(JaptRepository repository, Logger logger) {	
		super(repository, logger);
		setSort(true);
	}

	private static final boolean debug = false; //true;

	private void debugPrint(String msg) {
		if (debug) {
			System.err.println(msg);
		}
	}

	private int numInternalClasses;
	private int numAnonInnerClasses;
	private int numRefactoredClasses;

	/**
	 * Overrides GenericRefactor#refactor()
	 * @see com.ibm.ive.tools.japt.refactor.GenericRefactor#refactor()
	 */
	public void refactor() {
		numAnonInnerClasses = 0;
		numRefactoredClasses = 0;

		BT_ClassVector candidates = candidateClasses(getRepository());
		reduce(candidates);
		refactor(candidates);

		logStatus("# of internal classes = " + numInternalClasses); //$NON-NLS-1$
		logStatus("# of anonymous inner classes = " + numAnonInnerClasses); //$NON-NLS-1$
		logStatus("# of refactored classes = " + numRefactoredClasses); //$NON-NLS-1$
	}

	/**
	 * Overrides GenericRefactor#isCandidateClass()
	 * @param clz BT_Class object to check.
	 * @return true if given class should be refactored.
	 * @see com.ibm.ive.tools.japt.refactor.GenericRefactor#isCandidateClass()
	public boolean isCandidateClass(BT_Class clz) {
	}
	 */

	BT_ClassVector candidateClasses(JaptRepository repository) {
		BT_ClassVector candidateClasses = new BT_ClassVector();

		BT_ClassVector cv = repository.getInternalClasses();
		numInternalClasses = cv.size();

		Enumeration enumClasses = cv.elements();
		while (enumClasses.hasMoreElements()) {
			BT_Class clz = (BT_Class)enumClasses.nextElement();

			if (clz.isStub() || clz.isArray() || clz.isPrimitive()) {
				continue;
			}

			/*
			 * Check inner class attribute
			 */
			BT_InnerClassesAttribute attr = clz.getInnerClassAttr();
			if (attr == null) {
				continue;
			}
			/* TODO: check the types in InnerClassAttr */

			/*
			 * Check the class name
			 */
			if (isAnonInnerClassName(clz.getName())) {
				numAnonInnerClasses++;
			} else {
				continue;
			}

			/*
			 * Check parents
			 * Allow only one "implements" and no "extends" in this implementation
			 */
			BT_ClassVector parents = clz.getParents();
			Enumeration enumParents = parents.elements();
			int intfCount = 0;
			while (enumParents.hasMoreElements()) {
				BT_Class parent = (BT_Class)enumParents.nextElement();
				if (parent.isInterface()) {
					intfCount++;
				} else if (!parent.fullName().equals("java.lang.Object")) { //$NON-NLS-1$
					intfCount = -1;
					break;
				}
			}
			if (intfCount != 1) {
				if (intfCount < 0) {
					logStatusIfVerbose("@@ extended class " + clz); //$NON-NLS-1$
				} else {
					logStatusIfVerbose("@@ too many interfaces " + clz); //$NON-NLS-1$
				}
				continue;
			}

			/*
			 * Check fields
			 * Allow synthetic fields only
			 */
			BT_FieldVector fieldsVec = clz.getFields();
			if (fieldsVec.size() == 0) {
				logStatusIfVerbose("@@ no fields " + clz); //$NON-NLS-1$
				continue;
			}
			Enumeration enumFields = fieldsVec.elements();
			boolean syntheticOnly = true;
			while (enumFields.hasMoreElements()) {
				BT_Field f = (BT_Field)enumFields.nextElement();
				if (!f.isSynthetic()) {
					syntheticOnly = false;
				}
			}
			if (!syntheticOnly) {
				logStatusIfVerbose("@@ non-synthetic fields " + clz); //$NON-NLS-1$
				continue;
			}

			/*
			 * Check creattion sites
			 * Assume anonymous inner class has only one creation site -- No
			 */
			if (clz.creationSites.size() != 1) {
				logStatusIfVerbose("@@ too many creation sites " + clz); //$NON-NLS-1$
				continue;
			}

			/*
			 * Check native methods
			 */
			Enumeration enumMethods = clz.getMethods().elements();
			boolean hasNativeMethod = false;
			while (enumMethods.hasMoreElements()) {
				BT_Method mtd = (BT_Method)enumMethods.nextElement();
				if (mtd.isNative()) {
					hasNativeMethod = true;
					break;
				}
			}
			if (hasNativeMethod) {
				logStatusIfVerbose("@@ native method " + clz); //$NON-NLS-1$
				continue;
			}
			
			/*
			 * Check constructor type
			 * Allow <init>(outer class) only in this implementation
			 */
			BT_MethodVector ctors = clz.getConstructors();
			if (ctors.size() != 1) {
				logStatusIfVerbose("@@ too many constructors " + clz); //$NON-NLS-1$
				continue;
			}
			BT_Method ctor = ctors.firstElement();
			if (ctor.getArgsSize() < 2) { /* inner + outer at least */
				logStatusIfVerbose("@@ too few arguments to the constructor " + clz); //$NON-NLS-1$
				continue;
			}

			/*
			 * Need more checks?
			 */

			candidateClasses.addElement(clz);
		}

		debugPrint("# of candidate classes = " + candidateClasses.size()); //$NON-NLS-1$
		return candidateClasses;
	}

	/*
	 * Check if the class name ends with "$[0-9]+"
	 */
	boolean isAnonInnerClassName(String name) {
		int idx = name.lastIndexOf('$');
		int len = name.length();
		if (idx >= 0 && idx < len-1) {
			int i;
			for (i = idx + 1; i < len; i++) {
				if (!Character.isDigit(name.charAt(i))) {
					return false; /* non-digit char found after '$' */
				}
			}
			return true;
		}
		return false; /* no '$' found in the class name or '$' is the last char */		
	}

	/*
	 * Remove anonymous inner classes from the candidate list
	 * if they are referenced from other classes in the repository.
	 * This takes care of "inner class of inner class" case.
	 */
	void reduce(BT_ClassVector candidateClasses) {
		/*
		 * TODO: This check may be incomplete and slow
		 * Memo: BT_Class.referenceSites is not suitable for this check
		 */
		BT_Class clz;
		Enumeration enumClasses;
		BT_ClassVector refs = new BT_ClassVector();
		enumClasses = getRepository().getInternalClasses().elements();
		while (enumClasses.hasMoreElements()) {
			clz = (BT_Class)enumClasses.nextElement();

			if (clz.isStub() || clz.isArray() || clz.isPrimitive()) {
				continue;
			}

			/*
			 * Check the types of all the fields
			 */
			Enumeration enumFields = clz.getFields().elements();
			while (enumFields.hasMoreElements()) {
				BT_Field fld = (BT_Field)enumFields.nextElement();
				BT_Class fldType = fld.getFieldType();

				if (fldType.isStub() || fldType.isPrimitive()) {
					continue;
				} else if (fldType.isArray()) {
					BT_Class elemType = fldType.getElementClass();
					if (!elemType.isStub() && !elemType.isPrimitive()) {
						refs.addUnique(elemType);
					}
				} else {
					refs.addUnique(fldType);
				}
			}
		}

		BT_ClassVector clzToRemove = new BT_ClassVector();
		enumClasses = candidateClasses.elements();
		while (enumClasses.hasMoreElements()) {
			clz = (BT_Class)enumClasses.nextElement();
			if (refs.contains(clz)) {
				clzToRemove.addUnique(clz);
			}
		}

		enumClasses = clzToRemove.elements();
		while (enumClasses.hasMoreElements()) {
			clz = (BT_Class)enumClasses.nextElement();
			candidateClasses.removeElement(clz);
			logStatusIfVerbose("@@ inner class of inner class " + clz); //$NON-NLS-1$
		}
	}

	/**
	 * Overrides GenericRefactor#refactor()
	 * @param inner BT_Class object to refactor.
	 * @see com.ibm.ive.tools.japt.refactor.GenericRefactor#refactor()
	 */
	public void refactor(BT_Class inner) {
		String innerName = inner.fullName();
		BT_CreationSiteVector siteVec = inner.creationSites;
		if (siteVec.size() != 1) {
			logStatusIfVerbose("@@ too many creation sites " + inner); //$NON-NLS-1$
			return;
		}
		BT_ClassReferenceSite refSite = siteVec.firstElement();
		BT_Ins insNew = refSite.getInstruction();
		if (!insNew.isNewIns()) {
			logStatusIfVerbose("@@ not a new instruction " + inner);  //$NON-NLS-1$
			return;
		}
		BT_Method mtd = refSite.getFrom();
		if (mtd.isStatic()) {
			logStatusIfVerbose("@@ creation site is static " + inner); //$NON-NLS-1$
			return;
		}
		BT_Class outer = mtd.getDeclaringClass();
		if (outer.isStub()) {
			logStatusIfVerbose("@@ outer class is a stub " + inner); //$NON-NLS-1$
			return;
		}

		/*
		 * Check conflicts of methods and fields
		 */
		if (checkConflictsRecursive(inner, outer)) { return; }

		/*
		 * Check the constructor arguments
		 */
		BT_MethodVector innerCtors = inner.getConstructors();
		if (innerCtors.size() != 1) {
			logStatusIfVerbose("@@ too many constructors " + inner); //$NON-NLS-1$
			return;
		}
		BT_Method innerCtor = innerCtors.firstElement();
		int argsSize = innerCtor.getArgsSize();
		if (argsSize < 2) { /* inner + outer at least */
			logStatusIfVerbose("@@ too few arguments to the constructor " + inner); //$NON-NLS-1$
			return;
		}
		/* the first argument must be an instance of the outer class */
		BT_MethodSignature sig = innerCtor.getSignature();
		BT_Class argType = sig.types.elementAt(0);
		if (argType != outer) {
			logStatusIfVerbose("@@ unexpected type with the constructor argument " + inner); //$NON-NLS-1$
			return;
		}
		BT_Field[] fields = analyzeFieldMapping(innerCtor);
		if (fields == null) {
			logStatusIfVerbose("@@ unexpected instruction sequence in the constructor " + inner); //$NON-NLS-1$
			return;
		}

		/*
		 * get the creation site
		 */
		BT_CodeAttribute code = mtd.getCode();
		BT_Ins insDup, insInvoke, ins;
		BT_LoadLocalIns[] insLoad = new BT_LoadLocalIns[argsSize-1];
		int argCount = 0;
		insDup = code.getNextInstruction(insNew);
		if (insDup == null) { return; }
		if (insDup.opcode != BT_Opcodes.opc_dup) {
			logStatusIfVerbose("@@ not a dup instruction " + inner); //$NON-NLS-1$
			return;
		}
		ins = insDup;
		while (true) {
			BT_Ins insNext;
			insNext = code.getNextInstruction(ins);
			ins = insNext;
			if (ins == null) {
				logStatusIfVerbose("@@ instruction missing " + inner); //$NON-NLS-1$
				return;
			}
			if (argCount == 0) {
				if (ins.opcode != BT_Opcodes.opc_aload_0) {
					logStatusIfVerbose("@@ not a aload0 instruction " + inner); //$NON-NLS-1$
					return;
				}
			} else {
				if (!ins.isLocalLoadIns()) {
					break;
				}
			}
			insLoad[argCount++] = (BT_LoadLocalIns)ins;
		}
		insInvoke = ins; /* never null here */
		if (!insInvoke.isInvokeSpecialIns()) {
			logStatusIfVerbose("@@ not a invokespecial instruction " + inner); //$NON-NLS-1$
			return;
		}
		if (insInvoke.getMethodTarget() != innerCtor) {
			logStatusIfVerbose("@@ not calling the constructor " + inner); //$NON-NLS-1$
			return;
		}

		debugPrint("inner=" + inner + ", outer=" + outer); //$NON-NLS-1$ //$NON-NLS-2$

		if (debug) {
			System.err.println("<< before refactoring >>"); //$NON-NLS-1$
			outer.print(System.err);
			inner.print(System.err);
		}

		/* -=-=-=- actual refactoring starts here -=-=-=- */

		/*
		 * modify the creation site in the outer class
		 */
		BT_CodeAttribute origCode = (BT_CodeAttribute)code.clone();
		if (!modifyCreationSite(code, insNew, insDup, insInvoke, argCount, insLoad, fields, outer)) {
			/* failure -- restore the outer class method */
			code.removeAllInstructions();
			mtd.setCode(origCode);
			return;
		}

		/*
		 * copy non-constructor methods from the inner class to the outer
		 */
		if (!copyInnerMethodsToOuter(inner, outer)) {
			/* failure -- restore the outer class method */
			code.removeAllInstructions();
			mtd.setCode(origCode);
			return;
		}

		origCode.remove();

		/*
		 * copy interface declarations from the inner class to the outer
		 */
		BT_ClassVector parents = inner.getParents();
		Enumeration enumParents = parents.elements();
		while (enumParents.hasMoreElements()) {
			BT_Class innerParent = (BT_Class)enumParents.nextElement();
			if (innerParent.isInterface()) {
				outer.getParents().addElement(innerParent);
			}
		}

		/*
		 * remove the refactored inner class from the repository
		 */
		inner.remove();

		if (debug) {
			System.err.println("<< after refactoring >>"); //$NON-NLS-1$
			outer.print(System.err);
		}
		logStatus("Refactored " + innerName); //$NON-NLS-1$
		numRefactoredClasses++;
	}

	/*
	 * Return true if any methods or fields conflict
	 */
	boolean checkConflictsRecursive(BT_Class inner, BT_Class outer) {
		boolean rc;
		rc = checkConflicts(inner, outer);
		if (rc) {
			return true;
		}

		Enumeration enumClasses = outer.getKids().elements();
		while (enumClasses.hasMoreElements()) {
			rc = checkConflictsRecursive(inner, (BT_Class)enumClasses.nextElement());
			if (rc) {
				return true;
			}	
		}
		
		return false;
	}

	boolean checkConflicts(BT_Class inner, BT_Class clz) {
		Enumeration enumMethods = inner.getMethods().elements();
		while (enumMethods.hasMoreElements()) {
			BT_Method innerMtd = (BT_Method)enumMethods.nextElement();
			String mtdName = innerMtd.getName();
			BT_MethodSignature sig = innerMtd.getSignature();
			if (clz.findInheritedMethod(mtdName, sig) != null) { /* @@ */
				logStatusIfVerbose("@@ method conflict: " + innerMtd + " / " + clz); //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
		}

		Enumeration enumFields = inner.getFields().elements();
		while (enumFields.hasMoreElements()) {
			BT_Field innerFld = (BT_Field)enumFields.nextElement();
			String fldName = innerFld.getName();
			BT_Class fldType = innerFld.getFieldType();
			if (clz.findInheritedField(fldName, fldType) != null) { /* @@ */
				logStatusIfVerbose("@@ field conflict: " + innerFld + " / " + clz); //$NON-NLS-1$ //$NON-NLS-2$
				return true;				
			}
		}

		return false;
	}

	/*
	 * Analyze the mappings from constructor arguments to inner class's fields
	 */
	BT_Field[] analyzeFieldMapping(BT_Method ctor) {
		BT_CodeAttribute code = ctor.getCode();
		BT_InsVector vecIns = code.getInstructions();
		int codeSize = vecIns.size();

		BT_Field[] fields = new BT_Field[ctor.getArgsSize()];

		for (int i = 0; i < codeSize; i++) {
			BT_Ins ins0 = vecIns.elementAt(i);
			BT_Ins ins1, ins2;

			/* simple constructor only */

			if (ins0.opcode == BT_Opcodes.opc_aload_0) {
				ins1 = code.getNextInstruction(ins0);
				if (ins1 == null) {
					/* this shouldn't happen */
					fields = null;
					break;
				}
				if (ins1.isInvokeSpecialIns()) {
					/* constructor calls Object.<init> unless the inner class is extended */
					BT_MethodRefIns insMR = (BT_MethodRefIns)ins1;
					if (insMR.getClassTarget().className().equals("java.lang.Object") //$NON-NLS-1$
						&& insMR.getMethodTarget().isConstructor()) {
						i++;
						continue;
					}
					/* this shouldn't happen */
					fields = null;
					break;
				}
				if (ins1.isLocalLoadIns()) {
					/* this should be an argument to the constructor */
					ins2 = code.getNextInstruction(ins1);
					if (ins2 == null) {
						/* this shouldn't happen */
						fields = null;
						break;
					}
					if (ins2.isFieldWriteIns()) {
						BT_LoadLocalIns loadIns = (BT_LoadLocalIns)ins1;
						int localNr = loadIns.target.localNr;
						BT_FieldRefIns putfIns = (BT_FieldRefIns)ins2;
						BT_Field fieldTarget = putfIns.getFieldTarget();
						fields[localNr] = fieldTarget;
						i += 2;
						continue;
					}
					/* unexpected instruction */
					fields = null;
					break;
				}
				/* unexpected instruction */
				fields = null;
				break;
			}
			if (ins0.isReturnIns()) {
				/* do nothing */
				continue;
			}
			/* any other acceptable cases ? */

			/* unexpected instruction */
			fields = null;
			break;
		}

		return fields;
	}

	/*
	 * modify the creation site in the outer class
	 */
	boolean modifyCreationSite(BT_CodeAttribute code, BT_Ins insNew, BT_Ins insDup, BT_Ins insInvoke, int argCount, BT_LoadLocalIns[] insLoad, BT_Field[] fields, BT_Class outer) {
		code.removeInstruction(insNew);
		code.removeInstruction(insDup);

		/*
		 * leave aload_0 (= insLoad[0]) as is
		 * insert aload_0 and putfield after other insLoad[]
		 */
		BT_InsVector insVec = code.getInstructions();
		int localNr = 2; /* skip local variables 0 and 1 in the constructor */
		for (int i = 1; i < argCount; i++) {
			BT_Field fieldOrig = fields[localNr];
			BT_Field fieldNew = BT_Field.createField(outer, fieldOrig.getFlags(), fieldOrig.getFieldType(), fieldOrig.getName());
			BT_Ins insAload0 = new BT_Ins(BT_Opcodes.opc_aload_0);
			BT_FieldRefIns insPutf = new BT_FieldRefIns(BT_Opcodes.opc_putfield, fieldNew);
			int pos = insVec.indexOf(insLoad[i]);
			code.insertInstructionAt(insPutf, pos+1);
			code.insertInstructionAt(insAload0, pos);
			if (insLoad[i].isWide()) {
				localNr += 2;
			} else {
				localNr++;
			}
		}

		code.removeInstruction(insInvoke);

		return true; /* success */
	}

	/*
	 * copy non-constructor methods from the inner class to the outer
	 */
	boolean copyInnerMethodsToOuter(BT_Class inner, BT_Class outer) {
		BT_MethodVector copiedMethods = new BT_MethodVector();

		Enumeration enumMethods = inner.getMethods().elements();
		while (enumMethods.hasMoreElements()) {
			BT_Method innerMtd = (BT_Method)enumMethods.nextElement();
			if (!innerMtd.isConstructor()) {
				BT_Method outerMtd = innerMtd.copyMethodTo(outer, false);
				if (outerMtd == null) {
					logStatusIfVerbose("@@ Failed to copy " + innerMtd); //$NON-NLS-1$
					removeCopiedMethods(copiedMethods);
					return false;
				}
				/* copyMethodTo() changes the method name */
				outerMtd.setName(innerMtd.getName());
				copiedMethods.addElement(outerMtd);

				if (!modifyCopiedMethod(outerMtd, inner, outer)) {
					removeCopiedMethods(copiedMethods);
					return false;
				}
			}
		}

		/*
		 * if calling other method in the inner class, modify the target class
		 */
		enumMethods = inner.getMethods().elements();
		while (enumMethods.hasMoreElements()) {
			BT_Method innerMtd = (BT_Method)enumMethods.nextElement();
			if (!innerMtd.isConstructor()) {
				Enumeration enumCallSites = innerMtd.callSites.elements();
				while (enumCallSites.hasMoreElements()) {
					BT_MethodCallSite callSite = (BT_MethodCallSite)enumCallSites.nextElement();
					BT_Method callerMtd = callSite.getFrom();
					BT_Class callerCls = callerMtd.getDeclaringClass();
					if (callerCls == outer) {
						BT_MethodRefIns callIns = (BT_MethodRefIns)callSite.getInstruction();
						BT_Method outerMtd = outer.findMethod(innerMtd.getName(), innerMtd.getSignature());
						BT_CodeAttribute callerCode = callerMtd.getCode();
						callIns.resetTarget(outerMtd, callerCode);
					}
				}
			}
		}

		return true; /* success */
	}

	/*
	 * Return true on success
	 */
	boolean modifyCopiedMethod(BT_Method mtd, BT_Class inner, BT_Class outer) {
		BT_Ins ins, insNext;
		BT_CodeAttribute code = mtd.getCode();
		Enumeration enumIns = code.getInstructions().elements();
		BT_InsVector insToRemove = new BT_InsVector();
		while (enumIns.hasMoreElements()) {
			ins = (BT_Ins)enumIns.nextElement();
			if (ins.opcode == BT_Opcodes.opc_aload_0) {
				insNext = code.getNextInstruction(ins);
				if (insNext != null) {
					if (insNext.isFieldReadIns()) {
						/* remove access to fields in the inner class */
						BT_Field f = insNext.getFieldTarget();
						if (f.getFieldType() == outer) {
							insToRemove.addElement(insNext);
						} else if (f.getDeclaringClass() == inner) {
							/* if accessing a field in the inner class, modify the declaring class */
							f.resetDeclaringClass(outer);
							// TODO: how to restore fields upon errors?
						}
					}
					/*
					 * if calling a method in the inner class, modify the target class
					 * -- do it later in copyInnerMethodsToOuter()
					else if (insNext.isInvokeVirtualIns() || insNext.isInvokeSpecialIns() || insNext.isInvokeInterfaceIns()) {
						BT_MethodRefIns insMR = (BT_MethodRefIns)insNext;
						BT_Class targetClass = insMR.getClassTarget();
						if (targetClass == inner) {
							// this does not do the work
							// insMR.targetClass = outer;
						}
					}
					 */
				}
			}
		}

		Enumeration enumInsToRemove = insToRemove.elements();
		while (enumInsToRemove.hasMoreElements()) {
			ins = (BT_Ins)enumInsToRemove.nextElement();
			code.removeInstruction(ins);
		}

		return true;
	}

	void removeCopiedMethods(BT_MethodVector methodsToRemove) {
		Enumeration enumMtdToRemove = methodsToRemove.elements();
		while (enumMtdToRemove.hasMoreElements()) {
			BT_Method mtd = (BT_Method)enumMtdToRemove.nextElement();
			mtd.remove();
		}
	}
}
