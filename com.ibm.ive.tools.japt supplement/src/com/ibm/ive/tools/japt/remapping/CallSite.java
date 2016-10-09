/*
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.remapping;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Opcodes;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CallSite {

	BT_MethodCallSite site;
	BT_CodeAttribute owner;
	boolean makeSpecial;
	boolean overridePermissions;
	boolean checkTypes = true;
	boolean allowConstructorMapping;
	Messages messages;
	//known as the types of all arguments passed to the method call - initially this is set to the signature parameters
	BT_ClassVector knownArguments;
	//known as the type of all returned objects - initially set to the return type in the signature
	BT_Class knownReturnType;
	
	
	/**
	 * 
	 */
	public CallSite(BT_MethodCallSite site, Messages messages, boolean makeSpecial, boolean overridePermissions, boolean checkTypes) {
		this.site = site;
		this.checkTypes = checkTypes;
		this.owner = site.from;
		if(owner == null) { //should never happen, having a call site from non-existent code
			throw new IllegalArgumentException();
		}
		this.overridePermissions = overridePermissions;
		this.makeSpecial = makeSpecial;
		this.messages = messages;
		
		
		BT_Method oldTarget = site.instruction.getTarget();
		BT_MethodSignature oldSig = oldTarget.getSignature();
		knownArguments = (BT_ClassVector) oldSig.types.clone();
		if(!oldTarget.isStatic()) {
			knownArguments.insertElementAt(oldTarget.cls, 0);
		}
		knownReturnType = oldSig.returnType;
		
	}
	
	/**
	 * 
	 * @param allowConstructorMapping allow the mapping of the constructor of one object to the
	 * constructor of some other object
	 */
	void allowConstructorMapping(boolean allowConstructorMapping) {
		this.allowConstructorMapping = allowConstructorMapping;
	}
	
	/**
	 * It is known that the given class is the type of all arguments (of that index) passed to the invocation.
	 * @param index
	 * @param argument may be null indicating that only null is passed as an argument
	 */
	void setKnownArgument(int index, BT_Class argument) {
		knownArguments.setElementAt(argument, index);
	}
	
	void setKnownReturnType(BT_Class returnType) {
		knownReturnType = returnType;
	}
	
	String isValidTarget(BT_Method newTarget, BT_Class newClassTarget) {
		BT_Method oldTarget = site.instruction.getTarget();
		BT_Class oldClassTarget = site.getClassTarget();
		if(newTarget.equals(oldTarget)) {
			return messages.IDENT;
		}
		
		//special methods cannot be targetted
		if(newTarget.isStaticInitializer() || newTarget.isFinalizer()) {
			return messages.SPECIAL_METHOD;
		}
		
		//can only exchange a constructor for another constructor of the same object
		//since there is an uninitialized object on the stack
		if(newTarget.isConstructor() && 
				!(oldTarget.isConstructor() && 
						(allowConstructorMapping || newTarget.cls.equals(oldTarget.cls)))) {
			return messages.CONSTRUCTORS_INCOMPATIBLE;
		}

		//check signatures
		if(!signaturesCompatible(newTarget)) {
			return messages.TYPES_INCOMPATIBLE;
		}
		
		//check visibilities
		BT_Class owningClass = owner.getMethod().cls;
			
		//was the original target visible?
		if(!oldTarget.isVisibleFrom(owningClass) || !oldClassTarget.isVisibleFrom(owningClass)) {
			return messages.VISIBILITY_INCOMPATIBLE;
		}
		
		
		
		if(!newClassTarget.isVisibleFrom(owningClass)) {
			if(!newTarget.isUnconditionallyVisibleFrom(owningClass)) {
				if(canChangeClass(newClassTarget) 
						&& canChangeTarget(newTarget)) {
					setVisible(newTarget, owningClass);
					//newTarget.becomeVisibleFrom(owningClass);
					newClassTarget.becomeVisibleFrom(owningClass);
				} else {
					return messages.VISIBILITY_INCOMPATIBLE;
				}
			} else {
				if(canChangeClass(newClassTarget)) {
					newClassTarget.becomeVisibleFrom(owningClass);
				} else {
					return messages.VISIBILITY_INCOMPATIBLE;
				}
			}
		} else {
			if(!newTarget.isUnconditionallyVisibleFrom(owningClass)) {
				if(canChangeTarget(newTarget)) {
					setVisible(newTarget, owningClass);
					//newTarget.becomeVisibleFrom(owningClass);
				} else {
					return messages.VISIBILITY_INCOMPATIBLE;
				}
			}
		}
		
		return null;
	}
	
	//here we want to know if we can make the target public
	public static boolean canChangeTarget(BT_Method method) {
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
			if(!kid.isPublic() && !canChangeTarget(kid)) {
				return false;
			}
		}
		return true;
	}
	
	private void setVisible(BT_Method method, BT_Class newFrom) {
		method.becomeVisibleFrom(newFrom);
		BT_MethodVector kids = method.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Method kid = kids.elementAt(i);
			if(!kid.isPublic()) {
				setVisible(kid, newFrom);
			}
		}
	}
	
	
	private static boolean canChangeClass(BT_Class clazz) {
		return Accessor.canChangeClass(clazz);
	}
	
	boolean signaturesCompatible(BT_Method newTarget) {
		BT_MethodSignature newSig = newTarget.getSignature();
		boolean newIsStatic = newTarget.isStatic();
		int newArgsCount = newSig.types.size();
		if(!newIsStatic) {
			newArgsCount++;
		}
		if(knownArguments.size() != newArgsCount) {
			return false;
		}
		
		//check parameter and return types
		BT_ClassVector newParameters = (BT_ClassVector) newSig.types.clone();
		if(!newIsStatic) {
			newParameters.insertElementAt(newTarget.cls, 0);
		}
		
		if(checkTypes) {
			//all the signature parameters of the new type must be more general than the old
			for(int i=0; i<newParameters.size(); i++) {
				BT_Class newType = newParameters.elementAt(i);
				BT_Class knownArgument = knownArguments.elementAt(i);
 				//if(!newType.isInstance(oldParameters.elementAt(i))) {
 				if(knownArgument != null && !newType.isInstance(knownArgument)) {
 					return false;
				}
			}
			//the return type of the new type must be more specific than the old
			if(knownReturnType != null && !knownReturnType.isInstance(newSig.returnType)) {
				return false;
			}
		}
		return true;
	}
	
	String remap(BT_Method newTarget) {
		JaptRepository rep = (JaptRepository) owner.getMethod().cls.getRepository();
		if(!rep.isInternalClass(owner.getMethod().cls)) {
			return messages.EXTERNAL_CLASS;
		}
		String reason = isValidTarget(newTarget, newTarget.getDeclaringClass());
		if(reason != null) {
			return reason;
		}
		if(newTarget.isStatic()) {
			//note: we cannot just change the target of the instruction, that will not update callsites vectors
			changeInstruction(newTarget, BT_Opcodes.opc_invokestatic);
		} else if(newTarget.cls.isInterface()){
			//note: we cannot just change the target of the instruction, that will not update callsites vectors
			changeInstruction(newTarget, BT_Opcodes.opc_invokeinterface);
		} else {
			//Note: when changing from static to virtual, there is a primary
			//difference in that the first argument to the static method cannot be null, but cases where
			//this might occur are not detected at this time
			if(newTarget.isPrivate() || newTarget.isConstructor() || isSuperClassSpecialCall(newTarget)) {
				//note: we cannot just change the target of the instruction, that will not update callsites vectors
				changeInstruction(newTarget, BT_Opcodes.opc_invokespecial);
			} else {
				//note: we cannot just change the target of the instruction, that will not update callsites vectors
				changeInstruction(newTarget, BT_Opcodes.opc_invokevirtual);
			}
		}
		return null;
	}
	
	private boolean isSuperClassSpecialCall(BT_Method newTarget) {
		BT_Class owningClass = owner.getMethod().cls;
		BT_Class targetClass = newTarget.cls;
		return makeSpecial && targetClass.isClassAncestorOf(owningClass);
	}

	/**
	 * @param newTarget
	 * @param opcode
	 */
	private void changeInstruction(BT_Method newTarget, int opcode) {
		BT_MethodRefIns oldIns = site.instruction;
		BT_InsVector instructionVector = owner.getInstructions();
		int index = instructionVector.indexOf(oldIns);
		oldIns.unlink(owner);
		instructionVector.removeElementAt(index);
		BT_Ins newIns = BT_Ins.make(opcode, newTarget);
		owner.changeReferencesFromTo(oldIns, newIns, false);
		instructionVector.insertElementAt(newIns, index);
		newIns.link(owner);
		
		//change the callsite in the callsite list to reflect the new instruction
		BT_MethodCallSiteVector calledMethods = owner.calledMethods;
		int n = calledMethods.size() - 1;
		for (; n >= 0; n--) {
			BT_MethodCallSite s = calledMethods.elementAt(n);
			if (s.instruction == newIns) {
				site = s;
				break;
			}
		}
		if(n < 0) { //we could not find the changed callsite
			throw new RuntimeException();
		}
	
	}

}
