package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Represents an instruction that refers to a method -- see its subclasses.
 Typically created by one of the {@link BT_Ins#make} methods.
 * @author IBM
**/
public abstract class BT_MethodRefIns extends BT_Ins {

	/**
	 See {@link BT_MethodRefIns#getTarget}, and {@link #resetTarget}.
	**/
	public BT_Method target;
	
	
	public BT_Class targetClass;

	
	BT_MethodRefIns(int opcode, int index, BT_Method target, BT_Class targetClass) {
		super(opcode, index);
		this.target = target;
		this.targetClass = targetClass;
	}
	
	BT_MethodRefIns(int opcode, int index, BT_Method target) {
		this(opcode, index, target, target.getDeclaringClass());
	}

	/**
	 @param index  The byte offset of the instruction.
	 @param poolIndex  The target methodref in the constant pool.
	 @param inM  The method that will contain this instruction.
	**/
	BT_MethodRefIns(int opcode, int index, int poolIndex, BT_Method inM, LoadLocation loadedFrom)
		throws BT_DescriptorException, BT_ConstantPoolException {
		super(opcode, index);
		if (CHECK_JIKESBT
			&& opcode != opc_invokevirtual
			&& opcode != opc_invokeinterface
			&& opcode != opc_invokespecial
			&& opcode != opc_invokestatic)
			assertFailure(Messages.getString("JikesBT.Invalid_opcode__{0}_1", opcode));
		BT_ConstantPool pool = inM.cls.pool;

		// See if this constructor is expected to find the target
		// method. This is indicated by passing a pool and pool index.
		// If pool is null, the subclass will be expected to set
		// the target directly.
		//
		int expectedType =
			opcode == opc_invokeinterface
				? BT_ConstantPool.INTERFACEMETHODREF
				: BT_ConstantPool.METHODREF;
		String className = pool.getClassNameAt(poolIndex, expectedType);
		String methodName = pool.getMethodNameAt(poolIndex);
		String signatureString = pool.getMethodTypeAt(poolIndex);
		
		BT_Repository repo = pool.getRepository();
		BT_Class returnType = repo.linkTo(BT_ConstantPool.getReturnType(signatureString)); 
		BT_ClassVector args = BT_ConstantPool.linkToArgumentTypes(signatureString, repo);
		
		BT_MethodSignature signature = 
			BT_MethodSignature.create(signatureString, returnType, args, repo);
		
		targetClass = pool.getRepository().forName(className);
		if(targetClass.isStub()) {
			if(isInvokeInterfaceIns()) {
				if(!targetClass.isInterface()) {
					targetClass.becomeInterface();
				}
			} else if(isInvokeSpecialIns() || isInvokeVirtualIns()){ 
				if(!targetClass.isClass()) {
					targetClass.becomeClass();
				}
			}
		}
		
		if(isInvokeSpecialIns() && BT_Method.isContructorName(methodName)) {
			//for constructor invocations, the method cannot be inherited, it must exist in the target class
			target = targetClass.findMethodOrNull(methodName, signature);
		} else {
			// Try to resolve to an inherited method. Use the inherited method if found.
			target = targetClass.findInheritedMethod(
					methodName,
					signature,
					true);
		}
		if (target == null) {
			createStubTarget(targetClass, inM, methodName, signature, loadedFrom);
		}
	}
	
	private void createStubTarget(
			BT_Class targetClass,
			BT_Method from,
			String methodName,
			BT_MethodSignature signature,
			LoadLocation loadedFrom) {
		target = targetClass.addStubMethod(methodName, signature);
		if(isInvokeStaticIns()) {
			target.becomeStatic();
		}
		targetClass.getRepository().factory.noteUndeclaredMethod(target, targetClass, from, this, loadedFrom);
	}

	public boolean optimize(BT_CodeAttribute code, int n, boolean strict) {
		// Prevent this instruction from being optimized away
		return false;
	}
	
	public BT_MethodCallSite findCallSite() {
		return target.findCallSite(this);
	}
	
	public void link(BT_CodeAttribute code) {
		BT_MethodCallSite callSite = target.addCallSite(this, code);
		if(callSite != null) {
			code.addCalledMethod(callSite);
		}
	}
	
	public void unlink(BT_CodeAttribute code) {
		target.removeCallSite(this);
		code.removeCalledMethod(this);
	}
	
	public void resolve(BT_CodeAttribute code, BT_ConstantPool pool) {
		getCPIndex(code, pool);
	}
	
	
	int getCPIndex(BT_CodeAttribute code, BT_ConstantPool pool) {
		return pool.indexOfMethodRef(getResolvedClassTarget(code), target);
	}
	
	// ----------------------------------
	// Target-related ...

	public BT_Method getMethodTarget() {
		return target;
	}
	
	public BT_Class getClassTarget() {
		return targetClass;
	}
	
	public BT_Class getResolvedClassTarget(BT_CodeAttribute code) {
		if(isInvokeSpecialIns()) {
			return targetClass;
		}
		if(BT_Factory.resolveRuntimeReferences) {
			/*
			 * resolve runtime references means for the instruction to point directly to the method
			 * that would be invoked at runtime.  JikesBT performs the steps necessary to determine which
			 * class/interface has defined the method to be invoked.  For example, a call to String.wait() will
			 * be resolved to the invoked method Object.wait().
			 */
			 /* 
			 * there are a couple of exceptional cases in which resolution MUST de done at runtime
			 */
			//TODO a test case for these and also for the same in BT_FieldRefIns
			if((isInvokeVirtualIns() && target.getDeclaringClass().isInterface()) /* "targetClass" is an abstract class which has no implementation of the method "target" from an implemented interface */
					|| (isInvokeInterfaceIns() && target.getDeclaringClass().isClass()) /* the target is java.lang.Object */
				
					//"target" is only accessible through "targetClass" which has greater access permissions than "target"'s declaring class,
					//so referencing "target" directly will break access permissions
					|| !target.getDeclaringClass().isVisibleFrom(code.getMethod().getDeclaringClass())
					|| target.isConstructor() /* we should never resolve a constructor method reference */
					/*|| target.isStub() the result of this method must not change, and since something can switch to/from a stub, we cannot alter resolution based on this quality
					|| target.getDeclaringClass().isStub()*/
			) {
				return targetClass;
			}
			return target.getDeclaringClass();
		}
		return targetClass; 
	}
	
	/**
	 Just gets field {@link BT_MethodRefIns#target}.
	 Same as {@link BT_MethodRefIns#getMethodTarget} but using a different naming convention.
	**/
	public BT_Method getTarget() {
		return target;
	}

	public void resetTarget(BT_Method m, BT_CodeAttribute owner) {
		if (m != target) {
			if(target != null) {
				unlink(owner);
			}
			target = m;
			targetClass = m.getDeclaringClass();
			link(owner);
		}
	}
	// ----------------------------------

	public int getPoppedStackDiff() {
		int diff = 0;
		for (int n = target.getSignature().types.size() - 1; n >= 0; n--) {
			diff -= target.getSignature().types.elementAt(n).getSizeForLocal();
		}
		if (opcode != opc_invokestatic)
			diff--; // for "this"
		return diff;
	}
	
	public int getPushedStackDiff() {
		if (!target.isVoidMethod()) {
			return target.getSignature().returnType.getSizeForLocal();
		}
		return 0;
	}

	/**
	 Returns the opcode for popping a reference or primitive of the specified argument's type from the stack.
	 Used primarily internally within JikesBT.
	 <br> Example: "int" --> opc_pop
	 <br> Example: "double" --> opc_pop2
	**/
	int getOpcodeForPop(BT_Method target, int argNumber) {
		return target.getSignature().types.elementAt(argNumber).getOpcodeForPop();
	}

	public void write(DataOutputStream dos, BT_CodeAttribute code, BT_ConstantPool pool)
		throws IOException {
		dos.writeByte(opcode);
		dos.writeShort(getCPIndex(code, pool));
		if (size() != 3)
			throw new BT_InvalidInstructionSizeException(Messages.getString("JikesBT.Write/size_error_{0}_3", this));
	}
	
	public String toString() {
		return getPrefix()
			+ BT_Misc.opcodeName[opcode & 0xff]
			+ " "
			+ target.getSignature().returnType
			+ " "
			+ getInstructionTarget()
			//+ target.useName()
			;
	}
	
	public String getInstructionTarget() {
		return targetClass.getName() + '.' + target.qualifiedName();
	}
	
	public String toAssemblerString(BT_CodeAttribute code) {
		return BT_Misc.opcodeName[opcode & 0xff]
			+ " "
			+ target.getSignature().returnType
			+ " "
			+ target.getDeclaringClass().getName()
			+ "." 
			+ target.getName()
			+ "(" 
			+ target.getSignature().toExternalArgumentString() 
			+ ")";
	}
	

}
