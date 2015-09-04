/*
 * Created on Oct 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import java.util.HashMap;

import com.ibm.jikesbt.BT_CodeException.BT_MissingConstructorException;
import com.ibm.jikesbt.BT_CodeException.BT_InvalidStackTypeException.BT_ExpectedUninitializedTypeException;
import com.ibm.jikesbt.BT_ConstructorLocator.Instantiation;

/**
 * @author sfoley
 *
 * This class locates the occurence of a constructor call for a particular object on
 * the stack.  It is not enough to simply locate the call to a particular constructor to know
 * where an object is initialized, you must also determine whether that call is being performed
 * on the uninitialized object of interest.
 * <p>
 * This class works equally well when finding an invocation of a constructor after a new instruction
 * has put an object on the stack, or finding the invocation of a superclass or class constructor from within
 * a given contructor.
 * <p>
 * This class could easily be generalized to locate a specific method call on any given object of interest.
 * <p>
 */
public class BT_InstantiationLocator extends BT_StackShapeVisitor {
	
	private static Instantiation emptyInstantiations[] = new Instantiation[0];
	
	private HashMap instantiationMap;
	
	/**
	 * @param code the code with the constructor call
	 */
	public BT_InstantiationLocator(BT_CodeAttribute code) {
		this(code, null);
	}
	
	/**
	 * @param code the code with the constructor call
	 */
	public BT_InstantiationLocator(BT_CodeAttribute code, BT_StackPool pool) {
		super(code, pool);
		super.useMergeCandidates(false); //no need to use merge candidates for constructor search
	}
	
	public Instantiation[] getInstantiations() {
		if(instantiationMap == null) {
			return emptyInstantiations;
		}
		/* It is actually legal to create an uninitialized object that is never
		 * initialized, as long as:
		 * -it is never used (for example by a putstatic), 
		 * -it is not on the stack or in the locals during a backwards branch,
		 * -it is not in the locals within the confines of an exception handler.
		 * 
		 * So we do not throw an exception if a constructor is not found.
		 * Callers must check the instantiation objects to determine if the constructor was found.
		 */
		return (Instantiation[]) instantiationMap.values().toArray(new Instantiation[instantiationMap.size()]);
	}
	
	public static Instantiation[] findInstantiations(BT_Method method, BT_StackPool stackPool) throws BT_CodeException {
		BT_CodeAttribute code = method.getCode();
		if(code == null) {
			return emptyInstantiations;
		}
		BT_InstantiationLocator locator = new BT_InstantiationLocator(code, stackPool);
		BT_StackShapes shapes = locator.find();
		shapes.returnStacks();
		return locator.getInstantiations();
	}
	
	protected boolean visit(
			BT_Ins currentIns,
			int iin,
			BT_Ins previousInstruction,
			int prev_iin,
			BT_ExceptionTableEntry handler)  
				throws BT_CodeException {
		return super.visit(currentIns, iin, previousInstruction, prev_iin, handler)
			&& handleInstruction(currentIns, iin, previousInstruction, prev_iin, handler);
	}
	
	public BT_StackShapes find() throws BT_CodeException {
		stackShapes.createInitialStacks();
		try {
			code.visitReachableCode(this);
		} catch(BT_CodeException e) {
			stackShapes.returnStacks();
			throw e;
		} finally {
			returnStacks();
		}
		return stackShapes;
	}
	
	protected boolean handleInstruction(BT_Ins currentIns, int iin, BT_Ins previousIns, int prev_iin, BT_ExceptionTableEntry handler) throws BT_CodeException {
		if(prev_iin == ENTRY_POINT) {
			return true;
		}
		if(handler != null) {
			return true;
		}
		if(previousIns.isNewIns() && !previousIns.isNewArrayIns()) {
			BT_NewIns newIns = (BT_NewIns) previousIns;
			BT_StackCell stackBeforeCurrent[] = stackShapes.stackShapes[iin];
			BT_StackType type = stackBeforeCurrent[stackBeforeCurrent.length - 1].getCellType();
			if(!type.isUninitializedObject()) {
				throw new BT_ExpectedUninitializedTypeException(code, currentIns, iin, newIns.getTarget(), type, 0);
			}
			if(instantiationMap == null) {
				instantiationMap = new HashMap();
			}
			Instantiation instantiation = new Instantiation(prev_iin, (BT_NewIns) previousIns);
			instantiationMap.put(type, instantiation);
		}
		
		if(instantiationMap != null && instantiationMap.size() > 0 && currentIns.isInvokeSpecialIns()) {
			BT_InvokeSpecialIns invoke = (BT_InvokeSpecialIns) currentIns;
			BT_Method target = invoke.getTarget();
			if(target.isConstructor()) {
				if(!target.getDeclaringClass().equals(invoke.getClassTarget())) {
					throw new BT_MissingConstructorException(code, invoke, iin, target);
				}
				//determine if we are invoking it on the uninitialized object on the stack
				BT_MethodSignature sig  = target.getSignature();
				int argsSize = sig.getArgsSize();
				BT_StackCell stackBeforeCurrent[] = stackShapes.stackShapes[iin];
				BT_StackType foundType = stackBeforeCurrent[stackBeforeCurrent.length - argsSize - 1].getCellType();
				Instantiation instantiation = (Instantiation) instantiationMap.get(foundType);
				if(instantiation != null /* foundType.equals(type)*/) {
					
					//TODO what is a single instantiation is constructed in two places (ie one new instruction has two or more corresponding invokespecials)?
					//I think this is probably legal...  for this we probably need 
					//to alter the Instantiation class
					
					
					//now find the corresponding call site
					for(int i=0; i<code.calledMethods.size(); i++) {
						BT_MethodCallSite aSite = code.calledMethods.elementAt(i);
						if(aSite.instruction.equals(currentIns)) {
							instantiation.site = aSite;
							instantiation.siteInstructionIndex = iin;
							return true;
						}
					}
					//this should never happen, 
					//we should always have a callsite for each invocation instruction
					throw new RuntimeException();
				}
			}
		}
		return true;
	}
}
