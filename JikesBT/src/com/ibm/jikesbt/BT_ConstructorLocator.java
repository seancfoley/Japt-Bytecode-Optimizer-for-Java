/*
 * Created on Oct 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_CodeException.BT_MissingConstructorException;

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
public abstract class BT_ConstructorLocator extends BT_StackShapeVisitor {
	
	/**
	 * will hold the call site when it is located
	 */
	public BT_MethodCallSite site;
	
	/**
	 * will indicate the instruction index of the call site when it is located
	 */
	public int siteInstructionIndex = -1;
	
	private BT_StackType type;
	
	
	/**
	 * @param code the code with the constructor call
	 */
	public BT_ConstructorLocator(BT_CodeAttribute code) {
		this(code, null);
	}
	
	/**
	 * @param code the code with the constructor call
	 */
	public BT_ConstructorLocator(BT_CodeAttribute code, BT_StackPool pool) {
		super(code, pool);
		super.useMergeCandidates(false); //no need to use merge candidates for constructor search
	}
	
	/**
	 * @param instruction
	 * @return whether the given instruction pushes the uninitialized object of interest on the stack,
	 * 	which is the new instruction or the aload_0 construction from within a constructor
	 */
	protected abstract boolean instructionPushesUnitializedObjectOnStack(BT_Ins instruction);
	
	/**
	 * @param clazz
	 * @return whether the uninitialized object of interest is to be initialized by the given class
	 */
	protected abstract boolean isInitializerTarget(BT_Class clazz);
	
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
		/* It is actually legal to create an uninitialized object that is never
		 * initialized, as long as:
		 * -it is never used (for example by a putstatic), 
		 * -it is not on the stack or in the locals during a backwards branch,
		 * -it is not in the locals within the confines of an exception handler.
		 * 
		 * So we do not throw an exception of the constructor is not found.
		 * Callers must check this locator object to determine if the constructor was found.
		 */
		return stackShapes;
		
	}
	
	protected boolean handleInstruction(BT_Ins currentIns, int iin, BT_Ins previousIns, int prev_iin, BT_ExceptionTableEntry handler) throws BT_CodeException {
		if(prev_iin == ENTRY_POINT) {
			return true;
		}
		if(handler != null) {
			return true;
		}
		if(type == null && instructionPushesUnitializedObjectOnStack(previousIns)) {
			BT_StackCell stackBeforeCurrent[] = stackShapes.stackShapes[iin];
			type = stackBeforeCurrent[stackBeforeCurrent.length - 1].getCellType();
		}
		
		if(type != null && currentIns.isInvokeSpecialIns()) {
			BT_InvokeSpecialIns invoke = (BT_InvokeSpecialIns) currentIns;
			BT_Method target = invoke.getTarget();
			if(target.isConstructor() && isInitializerTarget(target.getDeclaringClass())) {
				if(!target.getDeclaringClass().equals(invoke.getClassTarget())) {
					throw new BT_MissingConstructorException(code, invoke, iin, target);
				}
				//determine if we are invoking it on the uninitialized object on the stack
				BT_MethodSignature sig  = target.getSignature();
				int argsSize = sig.getArgsSize();
				BT_StackCell stackBeforeCurrent[] = stackShapes.stackShapes[iin];
				BT_StackType foundType = stackBeforeCurrent[stackBeforeCurrent.length - argsSize - 1].getCellType();
				if(foundType.equals(type)) {
					//we've done it
					exit();
					
					//save the information about invocation
					siteInstructionIndex = iin;

					//now find the corresponding call site
					for(int i=0; i<code.calledMethods.size(); i++) {
						BT_MethodCallSite aSite = code.calledMethods.elementAt(i);
						if(aSite.instruction.equals(currentIns)) {
							site = aSite;
							return false;
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
	
	/**
	 * 
	 * @author sfoley
	 *
	 * Finds the constructor invocation that corresponds to a "new" instruction.
	 */
	public static class NewInsConstructorLocator extends BT_ConstructorLocator {
		final BT_NewIns newIns;
		final int newInstructionIndex;
		
		public NewInsConstructorLocator(int newInsIndex, BT_NewIns newIns, BT_CodeAttribute code) {
			this(newInsIndex, newIns, code, null);
		}
		
		public NewInsConstructorLocator(int newInsIndex, BT_NewIns newIns, BT_CodeAttribute code, BT_StackPool pool) {
			super(code, pool);
			if(newIns.isNewArrayIns()) {
				/* array classes don't have constructors */
				throw new IllegalArgumentException();
			}
			this.newIns = newIns;
			this.newInstructionIndex = newInsIndex;
		}
		
		protected boolean isInitializerTarget(BT_Class target) {
			return target.equals(newIns.getTarget());
		}
		
		public boolean instructionPushesUnitializedObjectOnStack(BT_Ins instruction) {
			return instruction.equals(newIns);
		}
		
		public Instantiation findConstructor() throws BT_CodeException {
			//TODO can we start from the "new" instruction instead of starting from the method beginning?
			BT_StackShapes shapes = find();
			shapes.returnStacks();
			return new Instantiation(newInstructionIndex, newIns, site, siteInstructionIndex);
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * Finds a constructor call for a chained cosntructor, which is a call from a 
	 * constructor to either a constructor in the same class or a constructor in the
	 * super class.
	 */
	public static class ChainedConstructorLocator extends BT_ConstructorLocator {
		final BT_Class clazz;
		
		public ChainedConstructorLocator(BT_CodeAttribute code) {
			this(code, null);
		}
		
		public ChainedConstructorLocator(BT_CodeAttribute code, BT_StackPool pool) {
			super(code, pool);
			BT_Method owningMethod = code.getMethod();
			if(!owningMethod.isConstructor()) {
				throw new IllegalArgumentException();
			}
			this.clazz = owningMethod.cls;
		}
		
		protected boolean isInitializerTarget(BT_Class target) {
			return target.equals(clazz) //calls another constructor in same class 
			|| target.equals(clazz.getSuperClass()); //calls superclass constructor
		}
		
		public boolean instructionPushesUnitializedObjectOnStack(BT_Ins instruction) {
			if(instruction instanceof BT_LoadLocalIns) {
				BT_LoadLocalIns loadLocalIns = (BT_LoadLocalIns) instruction;
				return loadLocalIns.target.localNr == 0;
			}
			return false;
		}
		
//		public Initialization findConstructor() throws BT_CodeException {
//			BT_StackShapes shapes = find();
//			if(siteInstructionIndex < 0) {
//				return null;
//			}
//			return new Initialization(shapes, site, siteInstructionIndex);
//		}
	}
	
	public static class Initialization {
		/**
		 * will hold the call site when it is located
		 */
		public BT_MethodCallSite site;
		
		/**
		 * will indicate the instruction index of the call site when it is located
		 */
		public int siteInstructionIndex;
		
		public Initialization(BT_MethodCallSite site, int siteInstructionIndex) {
			this.site = site;
			this.siteInstructionIndex = siteInstructionIndex;
		}
		
		protected Initialization() {}
	}
	
	public static class Instantiation extends Initialization {
		public final int newInstructionIndex;
		public final BT_CreationSite creationSite;
		
		Instantiation(int newInsIndex, BT_NewIns newIns, BT_MethodCallSite site, int siteInstructionIndex) {
			super(site, siteInstructionIndex);
			this.newInstructionIndex = newInsIndex;
			this.creationSite = newIns.getTarget().findCreationSite(newIns);
		}
		
		Instantiation(int newInsIndex, BT_NewIns newIns) {
			this.newInstructionIndex = newInsIndex;
			this.creationSite = newIns.getTarget().findCreationSite(newIns);
		}
	}
	
}
