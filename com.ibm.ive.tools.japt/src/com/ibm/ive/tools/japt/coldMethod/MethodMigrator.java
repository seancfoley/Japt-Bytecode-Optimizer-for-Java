/*
 * Created on Apr 9, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.coldMethod;

import com.ibm.ive.tools.japt.AccessChecker;
import com.ibm.ive.tools.japt.AccessorCallSite;
import com.ibm.ive.tools.japt.AccessorCallSiteVector;
import com.ibm.ive.tools.japt.AccessorMethod;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_ItemReference;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_StackCell;
import com.ibm.jikesbt.BT_StackShapes;
import com.ibm.jikesbt.BT_ConstructorLocator.ChainedConstructorLocator;
import com.ibm.jikesbt.BT_ConstructorLocator.Initialization;

public class MethodMigrator {
	private final BT_Class toClass;
	private final BT_Class origClass;
	private final ExtensionRepository coldRep;
	private final BT_Method originalMethod;
	
	/**
	 * enables the migration of code by checking access permisions
	 */
	private AccessChecker enabler;
	
	/**
	 * does the work required by the AccessChecker to enable access required by the code in its new location
	 */
	private MigratedMethodAccessChanger changer;
	
	/** 
	 * used if the method being migrated is a constructor, in which case the first few instructions cannot be migrated 
	 */
	private Initialization initialization; 
	
	private Boolean canMigrate;
	private BT_Method migratedMethod;
	
	MethodMigrator(
			ExtensionRepository coldRep,
			BT_Method originalMethod,
			BT_Class origClass,
			BT_Class toClass) {
		this.toClass = toClass;
		this.coldRep = coldRep;
		this.origClass = origClass;
		this.originalMethod = originalMethod;
	}
	
	private Initialization getInitialization() throws BT_CodeException {
		BT_CodeAttribute code = originalMethod.getCode();
		ChainedConstructorLocator locator = new ChainedConstructorLocator(code, coldRep.stackPool);
		
		BT_StackShapes shapes = locator.find();
		try {
			if(locator.siteInstructionIndex < 0) {
				coldRep.messages.CANNOT_MIGRATE_METHOD.log(coldRep.logger, 
						new String[] {originalMethod.useName(), coldRep.messages.CANNOT_LOCATE_CONSTRUCTOR});
				return null;
			}
			Initialization initialization = new Initialization(locator.site, locator.siteInstructionIndex);
			
			int codeSize = code.computeInstructionSizes();
			codeSize -= initialization.site.getInstruction().byteIndex;
			if (codeSize < ColdMethodExtension.MIN_METHOD_SIZE) {
				coldRep.messages.CANNOT_MIGRATE_METHOD.log(coldRep.logger, 
						new String[] {originalMethod.useName(), coldRep.messages.METHOD_TOO_SMALL});
				return null;
			}
			
			BT_StackCell stack[] = shapes.stackShapes[initialization.siteInstructionIndex];
			if(stack.length + initialization.site.instruction.getStackDiff() != 0) {
				//TODO we can handle the stack not empty condition by altering the signature of the migrated method,
				//and loading the stack inside the migrated method with the correct load instructions
				//However, it is unlikely the stack will be empty often so this might be a waste of time
				coldRep.messages.CANNOT_MIGRATE_METHOD.log(coldRep.logger, 
						new String[] {originalMethod.useName(), coldRep.messages.STACK_NOT_EMPTY});
				//stack is not empty
				return null;
			}
			return initialization;
		} finally {
			shapes.returnStacks();
		}
	}
	
	public boolean canMigrate() throws BT_CodeException {
		if(canMigrate != null) {
			return canMigrate.booleanValue();
		}
		changer = new MigratedMethodAccessChanger(
				coldRep,
				originalMethod, 
				toClass, 
				coldRep.allowChangedPermissions, 
				coldRep.allowAccessors);
		BT_InsVector ignoreInstructions;
		if(originalMethod.isConstructor()) {
			Initialization initialization = getInitialization();
			if(initialization == null) {
				canMigrate = Boolean.FALSE;
				return false;
			} else {
				this.initialization = initialization;
			}
			ignoreInstructions = new BT_InsVector(initialization.siteInstructionIndex + 1);
			BT_InsVector inst = originalMethod.getCode().getInstructions();
			for(int i=0; i<=initialization.siteInstructionIndex; i++) {
				ignoreInstructions.addElement(inst.elementAt(i));
			}
		} else {
			ignoreInstructions = new BT_InsVector(0);
		}
		enabler = new AccessChecker(originalMethod, origClass, toClass, changer, ignoreInstructions);
		if(!enabler.canMakeLegal()) {
			BT_ItemReference restricted = changer.getRestrictedSite();
			if(restricted == null) {
				coldRep.messages.CANNOT_MIGRATE.log(coldRep.logger, originalMethod.useName());
			} else {
				coldRep.messages.RESTRICTED_ACCESS.log(coldRep.logger, 
					new String[] {originalMethod.useName(), restricted.getInstructionTarget()});
			}
			canMigrate = Boolean.FALSE;
			return false;
		}
		canMigrate = Boolean.TRUE;
		return true;
	}
	
	/**
	 * migrate the given method to a new class.
	 */
	public BT_Method migrate() throws BT_CodeException {
		if(!canMigrate()) {
			return null;
		}
		if(migratedMethod != null) {
			return migratedMethod;
		}
		
		
		
		//TODO if the super constructor call is at the end, do not bother to migrate,
		//and measure the method body size against the minimum
		
		short flags = (short) (originalMethod.getFlags() | BT_Method.SYNTHETIC | BT_Method.STATIC);
		BT_MethodSignature signature = originalMethod.getSignature();
		if(!originalMethod.isStatic()) {
			BT_ClassVector newMethodArgs = (BT_ClassVector) signature.types.clone();
			newMethodArgs.insertElementAt(origClass, 0);
			signature = BT_MethodSignature.create(signature.returnType, newMethodArgs, toClass.getRepository());
		}
		String name = originalMethod.isConstructor() ? "$init$" : originalMethod.getName();
		/*  
		 Note: migrating a static and a non-static can produce a name clash, as shown:
		class A {
			
			static void m(A a) {}
			
			void m() {}
		}
		*/
		name = AccessorMethod.makeUniqueName(toClass, name, signature);
		BT_Method newMethod = BT_Method.createMethod(toClass, flags, signature, name);
		newMethod.becomePublic();
		newMethod.setSynchronized(false);
		
		
		BT_CodeAttribute code = originalMethod.getCode();
		originalMethod.setCode(null);
		newMethod.setCode(code);
		originalMethod.replaceBodyWithMethodCall(newMethod);
		enabler.makeLegal();
		
		//Keep the super constructor call in the original method
		if(originalMethod.isConstructor()) {
			BT_CodeAttribute warmCode = originalMethod.getCode();
			
			int numCopied = initialization.siteInstructionIndex + 1;
			BT_Ins movingInstructions[] = new BT_Ins[numCopied];
			code.getInstructions().copyInto(movingInstructions, 0, 0, numCopied);
			code.removeInstructionsAt(initialization.siteInstructionIndex + 1, 0);
			warmCode.insertInstructionsAt(movingInstructions, 0);
		}
		
		return migratedMethod = newMethod;
	}
	
	
	

	/**
	 It is possible that we use accessor methods unnecssarily, as shown in the following example.
	 Before cold method migration:
	 
	 class C {
	 	void cold1() {
	 		...
	 		cold2();
	 		...
	 	}
	 	
	 	private void cold2() {
	 		...
	 	}
	 }
	 
	 After cold method migration:
	 
	 class C {
	 	void cold1() {
	 		ColdC.cold1(this);
	 	}
	 	
	 	private void cold2() {
	 		ColdC.cold2(this);
	 	}
	 	
	 	public void cold2Accessor() {
	 		cold2();
	 	}
	 }
	 
	 class ColdC {
	 	public void cold1(C c) {
	 		...
	 		c.cold2Accessor();
	 		...
	 	}
	 	
	 	public void cold2(C c) {
	 		...
	 	}
	 }
	 
	 As you can see, we have created a call graph with: 
	 ColdC.cold1 -> 
	 C.cold2Accessor ->
	 C.cold2 ->
	 ColdC.cold2
	 
	 which should be replaced by:
	  
	 ColdC.cold1 -> 
	 ColdC.cold2
	 
	 So we wish to check for the condition in which a first migrated cold method calls an accessor which calls a second 
	 migrated cold method, and replace with a simple call to the second migrated cold method.
	 
	 final:
	 
	 class C {
	 	void cold1() {
	 		ColdC.cold1(this);
	 	}
	 	
	 	private void cold2() {
	 		ColdC.cold2(this);
	 	}
	 }
	 
	 class ColdC {
	 	public void cold1(C c) {
	 		...
	 		cold2(C);
	 		...
	 	}
	 	
	 	public void cold2(C c) {
	 		...
	 	}
	 }
	 */
	public AccessorCallSiteVector bypassAccessors() {
		if(migratedMethod == null || changer == null || changer.accessorCreator == null) {
			return AccessorCallSiteVector.emptySiteVector;
		}
		AccessorCallSiteVector accessedAccessors = changer.accessorCreator.accessedAccessors;
		AccessorCallSiteVector removedAccessors = new AccessorCallSiteVector(accessedAccessors.size());
		for(int i=accessedAccessors.size() - 1; i>=0; i--) {
			AccessorCallSite site = accessedAccessors.elementAt(i);
			AccessorMethod accessorMethod = site.getTarget();
			BT_Member accessorTarget = accessorMethod.getTarget();
			if(accessorTarget == null) {
				continue;
			}
			if(!(accessorTarget instanceof BT_Method)) {
				continue;
			}
			BT_Method accessorTargetMethod = (BT_Method) accessorTarget;
			Method targetMethod = coldRep.getClazz(accessorTarget.getDeclaringClass()).getMethod(accessorTargetMethod);
			if(!targetMethod.isCold()) {
				continue;
			}
			
			/* 
			 * only handle private or static targets (and not constructors which are migrated differently) 
			 * so that method overriding is not an issue and there is no null check required
			 */
			BT_Method underlyingTarget = targetMethod.method;
			if(!(underlyingTarget.isStatic() || underlyingTarget.isPrivate()) || underlyingTarget.isConstructor()) {
				continue;
			}
			
			BT_Method migratedTarget = targetMethod.getMigratedMethod();
			if(migratedTarget == null) {
				continue;
			}
			accessedAccessors.removeElementAt(i);
			removedAccessors.addUnique(site);
			changer.accessorCreator.changeTarget(site.site, migratedTarget);
		}
		return removedAccessors;
	}
}
