package com.ibm.ive.tools.japt.inline;

import com.ibm.ive.tools.japt.AccessChecker;
import com.ibm.ive.tools.japt.AccessPermissionsChanger;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_CodeVisitor;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_InvokeSpecialIns;
import com.ibm.jikesbt.BT_JumpIns;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_Misc;
import com.ibm.jikesbt.BT_SwitchIns;

/**
 * @author sfoley
 *
 * Represents a method call site that may or may not be inlined.
 */
public class MethodCallSite {

	Boolean isInlinable;
	private boolean hasBeenInlined;
	private boolean overridePermissions;
	private boolean inlineFromAnywhere;
	final BT_Method resolvedTarget;
	final InliningCodeAttribute inliningCode;
	final BT_MethodRefIns instruction;
	private AccessChecker enabler;
	
	/**
	 * Constructs a method call site at the given instruction which exists within
	 * the given code, and has the sole target method.  If the target method supplied is null,
	 * then this call site will not be inlinable or inlined.
	 * @param overridePermissions if inlining a method would cause a violation of access permissions,
	 * then inline the method anyway while broadening the permissions.  For example, this would allow
	 * the inlining of getter and setter methods which access private members.
	 */
	MethodCallSite(InliningCodeAttribute code, 
			BT_MethodRefIns instruction, 
			BT_Method resolvedTarget,
			boolean overridePermissions,
			boolean inlineFromAnywhere) {
		this.inliningCode = code;
		this.instruction = instruction;
		this.resolvedTarget = resolvedTarget;
		this.overridePermissions = overridePermissions;
		this.inlineFromAnywhere = inlineFromAnywhere;
	}
	
	BT_Method getContainingMethod() {
		return inliningCode.getCode().getMethod();
	}
	
	private void getEnabler() {
		BT_Class oldClass = resolvedTarget.cls;
		BT_Class newClass = getContainingMethod().cls;
		if(enabler == null) {
			enabler = new AccessChecker(
					resolvedTarget, 
					oldClass, 
					newClass, 
					new AccessPermissionsChanger(newClass));
		}
	}
	
	/**
	 * verifies whether or not acess permissions will permit the inlining of this call site.  The inlined
	 * code will contain method calls, field accesses and method calls which might not have the same
	 * visibility from the new location as they did from the old location.
	 */
	private boolean areAccessesPreserved() {
		getEnabler();
		return enabler.isLegal();
	}
	
	/**
	 * Checks whether visibility is preserved and tries to preserve
	 * visibility by changing access permissions if possible.
	 * returns whether visibility is successfully preserved.
	 */
	private boolean preserveAccesses() {
		getEnabler();
		return enabler.makeLegal();
	}
	
	private boolean canPreserveAccesses() {
		getEnabler();
		return enabler.canMakeLegal();
	}
	
	boolean uninitializedObjectsAtCallSite() throws BT_CodeException {
		//  Section 4.9.4 of bytecode verification in VM spec:
		//	"A valid instruction sequence must not have an uninitialized object on the operand stack 
		//	or in a local variable during a backwards branch, 
		//	or in a local variable in code protected by an exception handler or a finally clause"
		//	
		class CallSiteVisitor extends BT_CodeVisitor {
			int uninitializedObjects[]; //the number of uninitialized obects at each instruction index
			boolean isConstructor;
			boolean uninitializedObjectsAtCallSite;
			
			protected void setUp() {
				isConstructor = code.getMethod().isConstructor();
				uninitializedObjects = new int[code.getInstructionSize()];
			}
			
			protected boolean visit(BT_Ins instr, int iin, BT_Ins previousInstr, int prev_iin, BT_ExceptionTableEntry handler) {
				if (prev_iin == ENTRY_POINT) {
					uninitializedObjects[iin] = isConstructor ? 1 : 0;
				} else if(handler != null) {
					uninitializedObjects[iin] = 0;
				} else {
					if(previousInstr.isInvokeSpecialIns()) {
						BT_InvokeSpecialIns invokeSpecial = (BT_InvokeSpecialIns) previousInstr;
						if(invokeSpecial.target.isConstructor()) {
							uninitializedObjects[iin] = uninitializedObjects[prev_iin] - 1;
						}
						else {
							uninitializedObjects[iin] = uninitializedObjects[prev_iin];
						}
					}
					else if(previousInstr.isNewIns() && !previousInstr.isNewArrayIns()) {
						uninitializedObjects[iin] = uninitializedObjects[prev_iin] + 1;
					}
					else {
						uninitializedObjects[iin] = uninitializedObjects[prev_iin];
					}
				}
				if(instr.equals(instruction)) {
					uninitializedObjectsAtCallSite = uninitializedObjectsAtCallSite || (uninitializedObjects[iin] > 0);
				}
				return true;
			}
		};
		CallSiteVisitor visitor = new CallSiteVisitor();
		inliningCode.visitReachableCode(visitor);
		return visitor.uninitializedObjectsAtCallSite;
	}
	
	boolean targetHasBackwardBranches() {
		if(resolvedTarget == null) {
			return false;
		}
		BT_CodeAttribute code = resolvedTarget.getCode();
		if(code == null) {
			return false;
		}
		
		BT_InsVector instructions = code.getInstructions();
		
		
		for(int i=0; i<instructions.size(); i++) {
			BT_Ins ins = instructions.elementAt(i);
			if (ins.isSwitchIns()) {
				BT_SwitchIns s = (BT_SwitchIns) ins;
				BT_Ins targets[] = s.getAllTargets();
				for (int k = 0; k < targets.length; k++) {
					BT_Ins target = targets[k];
					if(target.byteIndex < ins.byteIndex) {
						return true;
					}
				}
			}
			else if (ins.isJumpIns()) {
				BT_JumpIns jumpIns = (BT_JumpIns) ins;
				BT_Ins target = jumpIns.getTarget();
				if(target.byteIndex < ins.byteIndex) {
					return true;
				}
			}			
		}
		
		return false;
	}
	
	boolean inline(boolean alterTargetClass, boolean provokeInitialization) throws BT_CodeException {
		if(!hasBeenInlined && isInlinable()) {
			BT_CodeAttribute inliningCodeAtt = inliningCode.getCode();
			//MethodCallSite.errorStream.println("inlining " + resolvedTarget.useName() + " into " + inliningCodeAtt.getMethod().useName());
			if(inliningCodeAtt.inline(resolvedTarget.getCode(), instruction, alterTargetClass, provokeInitialization)
					&& verifyInline()) {
				if(overridePermissions && !preserveAccesses()) {
					//should never reach here
					throw new RuntimeException("not properly checking whether "
							+ "we can change method visibilities before inlining");
				}
				hasBeenInlined = true;
			}
		}
		return hasBeenInlined;
	}

	boolean isRecursive() {
		return getContainingMethod().equals(resolvedTarget);
	}
	
	//static PrintStream errorStream = InlineExtension.errorStream;
	
	/**
	 * checks whether a callsite is inlinable.  Note that this does not check whether the method
	 * being inlined is inlinable, which must be done by a separate call to Method.isInlinable()
	 * @return
	 */
	boolean isInlinable() throws BT_CodeException {
		//errorStream.println("checking to inline " + (resolvedTarget != null ? resolvedTarget.useName() : "null") + " into " + inliningCode.getCode().getMethod().useName());
		if(isInlinable == null) {
			if(resolvedTarget == null) {
				isInlinable = Boolean.FALSE;
			} else {
			JaptRepository rep = (JaptRepository) resolvedTarget.getDeclaringClass().getRepository();
				boolean canInline = 
					!BT_Misc.overflowsUnsignedShort(inliningCode.getLocalVarCount() + resolvedTarget.getCode().getMaxLocals())
					/* The following limitiation comes from VM spec 2nd ed, paragraph 4.10 */
					&& !BT_Misc.overflowsUnsignedShort(inliningCode.getBytecodeSize() + resolvedTarget.getCode().computeMaxInstructionSizes() - 1)
					&& !resolvedTarget.isSynchronized() //TODO remove this condition when inliner can handle synchronized methods, which is no small matter
					&& !isRecursive() /*it is quite possible that a callsite is recursive while the targetted or containing method is not: 
										*	class A implements Runnable {
										*		Runnable r;
										*		public void run() {
										*			r.run(); //this callsite is recursive if r holds an instance of A which in turn has the same instance in the field r, 
										*					//otherwise it is not recursive
										*		}
										*	}
										*/
					&& isAccessible()
					&& rep.isInternalClass(getContainingMethod().cls)
					&& (inlineFromAnywhere || rep.isInternalClass(resolvedTarget.cls))	
					&& (overridePermissions ? canPreserveAccesses() : areAccessesPreserved())
				
					//the following check should be last since it is expensive - see uninitializedObjectsAtCallSite() for description of this check
					&& !(targetHasBackwardBranches() && uninitializedObjectsAtCallSite());
				
				//TODO: a case not yet handled: if there are uninitialized objects in local variables in the inlined method, and we have inlined
				//into a section of code protected by an exception handler, then we cannot do the inline
				//This case is not created by any known compiler - there is no need to put uninit objects into local variables.
				
				isInlinable = canInline ? Boolean.TRUE : Boolean.FALSE;
			}
		}
		return isInlinable.booleanValue();
	}
	
	boolean isAccessible() {
		BT_Class from = getContainingMethod().getDeclaringClass();
		return (resolvedTarget != null) && resolvedTarget.getDeclaringClass().isVisibleFrom(from) 
			&& resolvedTarget.isVisibleFrom(from);
	}
	
	/**
	 * after inlining we must ensure that the local variable count and the bytecode size 
	 * are not too large
	 */
	boolean verifyInline() {
		//MethodCallSite.errorStream.println("verifying "  + inliningCode.getCode().getMethod().useName());
		try {
			return !BT_Misc.overflowsUnsignedShort(inliningCode.getLocalVarCount()) 
				/* The following limitiation comes from VM spec 2nd ed, paragraph 4.10 */
				&& !BT_Misc.overflowsUnsignedShort(inliningCode.getBytecodeSize() - 1);
		} catch(BT_CodeException e) {}
		return false;
	}
	
}
