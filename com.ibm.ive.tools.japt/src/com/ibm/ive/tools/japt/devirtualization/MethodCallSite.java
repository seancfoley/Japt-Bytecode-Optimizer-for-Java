/*
 * Created on Feb 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.devirtualization;

import com.ibm.ive.tools.japt.JaptClass;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Opcodes;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class MethodCallSite {
	BT_MethodCallSite callSite;
	JaptRepository repository;
	BT_CodeAttribute code;
	boolean assumeUnknownVirtuals;
	
	/**
	 * 
	 */
	public MethodCallSite(BT_CodeAttribute code, 
						  BT_MethodCallSite callSite, 
						  JaptRepository repository,
						  boolean assumeUnknownVirtuals) {
		this.callSite = callSite;
		this.repository = repository;
		this.code = code;
		this.assumeUnknownVirtuals = assumeUnknownVirtuals;
	}
	
	void convertInvokeToStatic(BT_Method target, BT_Method calledMethod) {
		BT_Ins newIns = BT_Ins.make(BT_Opcodes.opc_invokestatic, calledMethod);
		BT_Ins oldIns = callSite.instruction;
		int index = code.findInstruction(oldIns);
		
		code.insertInstructionAt(newIns, index);
		code.removeInstructionAt(index + 1);
		code.changeReferencesFromTo(oldIns, newIns, false);
		
		//If it is an invokeinterface, a checkcast must be inserted
		 //(can only be done if target has no parameters).
		if (callSite.instruction.isInvokeInterfaceIns()) {
			newIns = BT_Ins.make(BT_Opcodes.opc_checkcast, target.getDeclaringClass());
			code.insertInstructionAt(newIns, index);
		}
		
		//a null check instruction sequence is
		//currently not needed because the null check is placed inside
		//the new static method
		
		code.computeMaxInstructionSizes();
	}
	
	void convertInvokeToSpecial(BT_Method target) {
		// Replace invokevirtual instruction by invokespecial.
		BT_Ins oldIns = callSite.instruction;
		BT_Ins newIns = BT_Ins.make(BT_Opcodes.opc_invokespecial, target);
		int index = code.findInstruction(oldIns);
		code.insertInstructionAt(newIns, index);
		code.removeInstructionAt(index+1);
		
		// If it is an invokeinterface, a checkcast must be inserted first
		// to satisfy the verifier (can only be done if target has no parameters).
		if (callSite.instruction.isInvokeInterfaceIns()) {
			newIns = BT_Ins.make(BT_Opcodes.opc_checkcast, target.getDeclaringClass());
			code.insertInstructionAt(newIns, index);
		}
		
		code.changeReferencesFromTo(oldIns, newIns, false);
		code.computeMaxInstructionSizes();
		//code.resetMaxStack();
	}
	

	private boolean canDevirtualize() {
		return !callSite.instruction.isInvokeStaticIns() 
			&& !repository.getInternalClassesInterface().isInEntireInterface(callSite.getTarget())
			&& !(callSite.instruction.isInvokeInterfaceIns() 
				&& callSite.getTarget().getSignature().getArgsSize() > 0);
	}
	
	boolean canDevirtualizeToStatic() {
		return canDevirtualize();
	}
	
	boolean canDevirtualizeToSpecial() {
		return !callSite.instruction.isInvokeSpecialIns() 
		/*
		 * The Sun 1.3.1 VM verifier rejects an invokespecial to a final method in
		 * a superclass where the J9 VM verifier accepts it.  Here we are conservative.
		 */
		/* there used to be some special hook that if the called methods was final and in 
		 * java.lang.Object and we were making a jxe, then it's fine... 
		 */
			&& callSite.getFrom().getDeclaringClass().equals(callSite.getTarget().getDeclaringClass())
			&& canDevirtualize();
		
		//TODO insert better invokespecial handling...
		
//				//		a method with an invokespecial to a private method should never be inlined
//					  //to a callsite outside of the class it is in (ie by increasing access permissions)
//					  //this will cause verify errors with Sun verifiers, unless it is a constructor
//					  
//					if(to.isPrivate() && !newFrom.equals(oldFrom) && !to.isConstructor()) {
//						  return false;
//					  }
//				
//					  //a method with an invokespecial to a superclass should never be inlined to
//					  //a class outside of the class it is in, unless that class is also a subclass
//					  //of the same parent class...  this will cause errors with the Sun verifier,
//					  //unless it is a constructor
//				
//					  //for example, if class C attempts to inline B.m, and B.m has a call to superclass
//					  //method A.n, then that will be chnaged to an invokespecial of A.n from C.  But this
//					  //now means that the call becomes overridable (virtual) and should then be an 
//					  //invokevirtual, but then this can change program behaviour.  If C is also a subclass
//					  //of A, then unless is has the exact same parent as B we cannot assure there
//					  //will be an intervening overriding method (invokespecials must check for this occurrence)
//				
//					  JaptClass toClass = (JaptClass) to.getDeclaringClass();
//					  if(toClass.isClassAncestorOf(oldFrom) && !newFrom.equals(oldFrom) && !to.isConstructor()) {
//						  BT_Class newSuper = newFrom.getSuperClass();
//						  BT_Class oldSuper = oldFrom.getSuperClass();
//						  if(newSuper == null) {
//							  if(oldSuper != null) {
//								  return false;
//							  }
//						  }
//						  else if(!newSuper.equals(oldSuper)) {	
//							  return false;
//						  }
//					  }
//					  
			
	}
	
	
	BT_Method getVirtualCallSiteTarget() {
		BT_Method referencedMethod = callSite.getTarget();
		if (callSite.instruction.isInvokeSpecialIns()) {
			return resolveSpecial();
		}
		
		if (referencedMethod.isPrivate()
				|| referencedMethod.isFinal()
				|| referencedMethod.getDeclaringClass().isFinal()) {
			
			return referencedMethod;
		}
		if(!assumeUnknownVirtuals) {
			RelatedMethodMap map = repository.getRelatedMethodMap();
			BT_MethodVector relatedMethods = map.getRelatedMethods(referencedMethod);
			if(relatedMethods.size() == 1) {
				//the only related method is itself
				return referencedMethod;
			}
			for(int i=0; i<relatedMethods.size(); i++) {
				BT_Method relatedMethod = relatedMethods.elementAt(i);
				JaptClass targetClass = (JaptClass) referencedMethod.getDeclaringClass();
				BT_Class relatedClass = relatedMethod.getDeclaringClass();
				if(!targetClass.equals(relatedClass) && targetClass.isClassAncestorOf(relatedClass)) {
					return null;
				}
			}
			return referencedMethod;
		}
		return null;
	}
	
	BT_Method resolveSpecial() {
		BT_Method targettedMethod = callSite.getTarget();
		BT_Class targettedClass = targettedMethod.getDeclaringClass();
		BT_Class fromClass = targettedMethod.getDeclaringClass();
		if(!targettedClass.equals(fromClass)) {
			if(((JaptClass) targettedClass).isClassAncestorOf(fromClass)) {
				RelatedMethodMap map = repository.getRelatedMethodMap();
				BT_Method overridingMethod = map.getOverridingMethod(fromClass.getSuperClass(), targettedMethod);
				if(overridingMethod != null) {
					return overridingMethod;
				}
			}
		}
		return targettedMethod;
		
	}

}
