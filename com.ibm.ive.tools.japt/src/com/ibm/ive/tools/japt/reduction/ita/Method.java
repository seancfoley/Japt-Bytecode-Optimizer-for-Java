package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.reduction.ClassProperties;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.ParameterLocation;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSiteVector;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_ExceptionTableEntryVector;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_NewIns;
import com.ibm.jikesbt.BT_Opcodes;
import com.ibm.jikesbt.BT_PopulatedObjectCell;
import com.ibm.jikesbt.BT_PopulatedObjectProvider;
import com.ibm.jikesbt.BT_StackCell;
import com.ibm.jikesbt.BT_StackPool;
import com.ibm.jikesbt.BT_StackShapeVisitor;
import com.ibm.jikesbt.BT_StackShapes;
import com.ibm.jikesbt.BT_ExceptionTableEntry.Indices;
import com.ibm.jikesbt.BT_PopulatedObjectCell.AcquiredObject;

/**
 * Represents a method declared in an interface or class.  A method propagates objects from one place
 * to another through the parameters (which includes the class itself in a non-static method call).  An
 * object may be propagated to the method through the parameters and then propagated elsehwere by a field write
 * or another method call or from a thrown exception.<p>
 * For example, when calling threadx.equals(objectx) in a method x(), 
 * the thread object threadx and the object objectx is propagated from the
 * stack of method x to the stack of java.lang.Object.equals().  From there, the two 
 * objects may be propagated elsewhere.  Note that the methods Object.equals() as listed as
 * a method call from the body of x() can only be called if an object of type thread has
 * previously been propagated to x() or has been created inside the body of x().  Once this
 * has been proven true, then it is possible that any other object propagated to x() or created
 * within the body of x() might be passed as the sole argument to the method call threadx.equals(objectx)
 * and propagated to Object.equals().
 * <p>
 * This class uses these principles, propagating objects from methods to other methods and fields
 * when it is known that such propagations are possible.  
 * @author sfoley
 *
 */
public class Method extends Member  {

	final int index;
	
	private BT_Method underlyingMethod;
	
	private static final short scanned = 0x4;
	private static final short storesIntoArrays = 0x8;
	private static final short loadsFromArrays = 0x10;
	private static final short canThrow = 0x20;
	private static final short verified = 0x40;
	
	private static final InstructionLocation noLocations[] = new InstructionLocation[0];
	private InstructionLocation returns[];
	private InstructionLocation arrayReads[];
	private InstructionLocation arrayWrites[];
	private InstructionLocation throwInstructions[];
	private InstructionLocation instantiations[];
	private InstructionLocation fieldReads[];
	private InstructionLocation fieldWrites[];
	private InstructionLocation invocations[];
	
	/**
	 * Japt allows for the user to indicate the creation of objects based on the invocation of
	 * native methods or other methods which access unavailable code.
	 */
	private Clazz conditionalInstantiations[];
	
	private static final Clazz emptyTypes[] = new Clazz[0];
	private static final ExceptionTableEntry emptyEntries[] = new ExceptionTableEntry[0];
	
	/**
	 * The types that this method accepts as arguments, indexed by the signature index.
	 * typesPropagatable[i] is the ith parameter, or null if the parameter is a primitive type
	 */
	public final Clazz typesPropagatable[];
	
	private ExceptionTableEntry[] handlers;
	private Clazz declaredExceptions[];
	private final Clazz returnType;
	private Method overridingMethods[];
	private BT_CodeAttribute substitutedCode;
	
	/**
	 * Used to analyze object flow in the code.
	 */
	private BT_StackCell stackShapes[][];
	private BT_PopulatedObjectProvider provider;
	private Boolean isCodeValid;
	
	private BT_ClassVector verifierRequiredClasses;
	
	
	/**
	 * Constructor for Method.
	 * @param member
	 */
	Method(BT_Method method, Clazz declaringClass, int index) {
		super(declaringClass);
		this.underlyingMethod = method;
		this.index = index;
		BT_ClassVector paramTypes = method.getSignature().types;
		BT_Class returnType = method.getSignature().returnType;
		this.returnType = declaringClass.repository.getClazz(returnType);
		int pTytpes = paramTypes.size();
		if(paramTypes.size() > 0) {
			this.typesPropagatable = new Clazz[pTytpes];
			for (int x = 0; x < paramTypes.size(); x++) {
				BT_Class param = paramTypes.elementAt(x);
				if(param.isBasicTypeClass) {
					this.typesPropagatable[x] = null;
				} else {
					this.typesPropagatable[x] = declaringClass.repository.getClazz(param);
				}
			}
		} else {
			this.typesPropagatable = emptyTypes;
		}
		if(method.isNative()) {
			if(method.fullName().equals("java.lang.System.arraycopy")
				&& method.getSignature().toString().equals("(Ljava/lang/Object;ILjava/lang/Object;II)V")) {
				this.substitutedCode = new BT_CodeAttribute(new BT_Ins[] {
					BT_Ins.make(BT_Opcodes.opc_aload_2),
					BT_Ins.make(BT_Opcodes.opc_checkcast, 
							method.getDeclaringClass().getRepository().forName("java.lang.Object[]")),
					BT_Ins.make(BT_Opcodes.opc_iload_3),
					BT_Ins.make(BT_Opcodes.opc_aload_0),
					BT_Ins.make(BT_Opcodes.opc_checkcast, 
							method.getDeclaringClass().getRepository().forName("java.lang.Object[]")),
					BT_Ins.make(BT_Opcodes.opc_iload_1),
					BT_Ins.make(BT_Opcodes.opc_aaload),
					BT_Ins.make(BT_Opcodes.opc_aastore),
					BT_Ins.make(BT_Opcodes.opc_return)
				}, method.getVersion());
				this.substitutedCode.setMethod(underlyingMethod);
			} else 
			if(!getRepository().getPropagationProperties().isRTSJAnalysis()
				&& method.fullName().equals("java.lang.Object.clone")
				&& method.getSignature().toString().equals("()Ljava/lang/Object;")) {
				this.substitutedCode = new BT_CodeAttribute(new BT_Ins[] {
					BT_Ins.make(BT_Opcodes.opc_aload_0),
					BT_Ins.make(BT_Opcodes.opc_areturn)
				}, method.getVersion());
				this.substitutedCode.setMethod(underlyingMethod);
			}
		}
	}
	
	Clazz[] getDeclaredExceptions() {
		Clazz declared[] = this.declaredExceptions;
		if(declared == null) {
			BT_ClassVector declaredExceptions = underlyingMethod.getDeclaredExceptionsVector();
			if(declaredExceptions != null && declaredExceptions.size() > 0) {
				ClassProperties props = getRepository().getClassProperties();
				BT_Class runtimeException = props.javaLangRuntimeException;
				BT_Class error = props.javaLangError;
				BT_Class throwable = props.javaLangThrowable;
				BT_ClassVector excs = null;
				top:
				for(int i=0; i<declaredExceptions.size(); i++) {
					BT_Class exc = declaredExceptions.elementAt(i);
					if(exc.isInstanceOf(runtimeException) 
							|| exc.isInstanceOf(error)
							|| !exc.isInstanceOf(throwable)) {
						continue;
					}
					if(excs == null) {
						excs = new BT_ClassVector();
					} else {
						for(int j=0; j<excs.size(); j++) {
							BT_Class previous = excs.elementAt(j);
							if(exc.isInstanceOf(previous)) {
								continue top;
							} else if(previous.isInstanceOf(exc)) {
								excs.setElementAt(exc, j);
								continue top;
							}
						}
					}
					excs.addElement(exc);
				}
				if(excs != null && excs.size() > 0) {
					declared = new Clazz[excs.size()];
					for(int i=0; i<excs.size(); i++) {
						declared[i] = declaringClass.repository.getClazz(excs.elementAt(i));
					}
				} else {
					declared = emptyTypes;
				}
			} else {
				declared = emptyTypes;
			}
			this.declaredExceptions = declared;
		}
		return declared;
	}
	
	static class ExceptionTableEntry {
		final BT_ExceptionTableEntry entry;
		final Clazz catchType;
		final Indices indices;
		private InstructionLocation catchLocation;
		
		ExceptionTableEntry(BT_ExceptionTableEntry entry, Repository rep) {
			this.entry = entry;
			this.indices = entry.calculateIndices();
			BT_Class catchType = entry.catchType;
			this.catchType = (catchType == null) ? null : rep.getClazz(catchType);
		}
		
		InstructionLocation getCatchLocation() {
			if(catchLocation == null) {
				catchLocation = new InstructionLocation(entry.handlerTarget, indices.handler);
			}
			return catchLocation;
		}
		
	}
	
	ExceptionTableEntry[] getExceptionTable() {
		if(handlers == null) {
			BT_CodeAttribute code = getCode();
			if(code != null) {
				BT_ExceptionTableEntryVector exceptions = code.getExceptionTableEntries();
				handlers = new ExceptionTableEntry[exceptions.size()];
				for(int i=0; i<handlers.length; i++) {
					handlers[i] = new ExceptionTableEntry(exceptions.elementAt(i), getRepository());
				}
			} else {
				handlers = emptyEntries;
			}
		}
		return handlers;
	}
	
	BT_Member getMember() {
		return underlyingMethod;
	}
	
	public BT_Method getMethod() {
		return underlyingMethod;
	}
	
	public boolean isAbstract() {
		return underlyingMethod.isAbstract();
	}
	
	public boolean isNative() {
		if(underlyingMethod.isNative()) {
			return substitutedCode == null;
		}
		return false;
	}
	
	public boolean isPrivate() {
		return underlyingMethod.isPrivate();
	}
	
	public boolean isConstructor() {
		return underlyingMethod.isConstructor();
	}
	
	/**
	 * 
	 * @return whether this method can be overridden by a method that has not been loaded.
	 */
	boolean isGenerallyOverridable() {
		return isOverridable() && !(getDeclaringClass().packageIsClosed() && isDefaultAccess());
	}
	
	/**
	 * 
	 * @return whether this method can be overridden
	 */
	boolean isOverridable() {
		return !isFinal()
				&& !getDeclaringClass().isFinal()
				&& !isPrivate()
				&& !isStatic()
				&& !isConstructor();
	}
	
	
	public BT_CodeAttribute getCode() {
		if(substitutedCode != null) {
			return substitutedCode;
		}
		return underlyingMethod.getCode();
	}
	
	/**
	 * @return the method in clazz that overrides this method
	 * This method is either a method which overrides or implements this method.  
	 * In either case, an invokeInterface or invokeVirtual call
	 * will call the overriding method and not the base method.
	 */
	Method getOverridingMethod(Clazz clazz) {
		return clazz.getOverridingMethod(underlyingMethod);
	}
	
	Method[] getOverridingMethods() {
		if(overridingMethods == null) {
			JaptRepository rep = (JaptRepository) underlyingMethod.getDeclaringClass().getRepository();
			RelatedMethodMap map = rep.getRelatedMethodMap();
			BT_MethodVector overriders;
			if(underlyingMethod.getDeclaringClass().isInterface()) {
				overriders = map.getImplementingMethods(underlyingMethod);
			}
			else {
				overriders = map.getOverridingMethods(underlyingMethod);
			}
			overridingMethods = new Method[overriders.size()];
			for(int i=0; i<overridingMethods.length; i++) {
				BT_Method overrider = overriders.elementAt(i);
				Clazz overridingClass = declaringClass.repository.getClazz(overrider.getDeclaringClass());
				overridingMethods[i] = overridingClass.getMethod(overrider);
			}
		}
		return overridingMethods;
	}
	
	public void setRequired() {
		if(isRequired()) {
			return;
		}
		super.setRequired();
		Repository rep = getRepository();
		JaptRepository japtRepository = rep.repository;
		EntryPointLister lister = rep.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(getDeclaringClass().getUnderlyingType());
		if(checkEntryPoints) {
			BT_MethodSignature sig = underlyingMethod.getSignature();
			BT_Class returnType = sig.returnType;
			BT_ClassVector types = sig.types;
			for(int i=0; i<types.size(); i++) {
				BT_Class type = types.elementAt(i);
				if(!type.isBasicTypeClass && japtRepository.isInternalClass(type)) {
					lister.foundEntryTo(type, underlyingMethod);
				}
			}
			if(!returnType.isBasicTypeClass && japtRepository.isInternalClass(returnType)) {
				lister.foundEntryTo(returnType, underlyingMethod);
			}
		}
		
		//The following stuff is actually not mandatory, it is possible to have
		//elements in the method signature that are not needed whatsoever
		
		Clazz declaredExceptions[] = getDeclaredExceptions();
		for(int i=0; i<declaredExceptions.length; i++) {
			declaredExceptions[i].setRequired();
		}
		if(returnsObjects()) {
			returnType.setRequired();
		}
		for(int i=0; i<typesPropagatable.length; i++) {
			Clazz type = typesPropagatable[i];
			if(type == null)  {
				continue;
			}
			type.setRequired();
		}
	}
	
	public boolean returnsObjects() {
		return !returnType.isPrimitive();
	}
	
	public Clazz getReturnType() {
		return returnType;
	}
	
	/**
	 * returns true if the method declares the throwable type in its list of checked exceptions
	 */
	boolean canThrow(FieldObject obj) {
		return canThrow() && canPassThrown(obj);
	}
	
	private boolean canPassThrown(FieldObject obj) {
		if(obj.isRuntimeThrowable()) {
			return true;
		}
		getDeclaredExceptions();
		for(int i=0; i<declaredExceptions.length; i++) {
			Clazz throwable = declaredExceptions[i];
			if(obj.mightBeGenericInstanceOf(throwable) 
			//if(obj.isGenericInstanceOf(throwable) 
					|| obj.mightBeInstanceOf(throwable)) {
					//|| obj.isInstanceOf(throwable)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * When intraprocedural analysis is enabled, this method determines if an object in one location can be propagated
	 * to another location.
	 * Stack indexes correspong to those specified in MethodInvocationLocation.
	 */
	boolean maps(MethodInvocationLocation origin, InstructionLocation target, int targetStackCellIndex) {
		return !useIntraProceduralAnalysis() || origin == null || checkMapping(origin, target, targetStackCellIndex);
	}
	
	private void clearUnusedShapes(BT_StackPool pool) {
		if(!useIntraProceduralAnalysis()) {
			provider = null;
			for(int i=0; i<stackShapes.length; i++) {
				pool.returnStack(stackShapes[i]);
			}
			stackShapes = null;
			return;
		}
		scanCode();
		BT_CodeAttribute code = getCode();
		boolean keepStack[]= new boolean[code.getInstructionSize()];
		if(returnsObjects()) {
			for(int i=0; i<returns.length; i++) {
				InstructionLocation loc = returns[i];
				keepStack[loc.instructionIndex] = true;
			}
		}
		if(loadsFromArrays()) {
			for(int i=0; i<arrayReads.length; i++) {
				InstructionLocation loc = arrayReads[i];
				keepStack[loc.instructionIndex] = true;
			}
		}
		if(storesIntoArrays()) {
			for(int i=0; i<arrayWrites.length; i++) {
				InstructionLocation loc = arrayWrites[i];
				keepStack[loc.instructionIndex] = true;
			}
		}
		for(int i=0; i<fieldReads.length; i++) {
			InstructionLocation loc = fieldReads[i];
			keepStack[loc.instructionIndex] = true;
		}
		for(int i=0; i<fieldWrites.length; i++) {
			InstructionLocation loc = fieldWrites[i];
			keepStack[loc.instructionIndex] = true;
		}
		if(canThrow()) {
			for(int i=0; i<throwInstructions.length; i++) {
				InstructionLocation loc = throwInstructions[i];
				keepStack[loc.instructionIndex] = true;
			}
		}
		for(int i=0; i<invocations.length; i++) {
			InstructionLocation loc = invocations[i];
			keepStack[loc.instructionIndex] = true;
		}
		for(int i=0; i<stackShapes.length; i++) {
			if(!keepStack[i]) {
				pool.returnStack(stackShapes[i]);
				stackShapes[i] = null;
			}
		}
	}
	
	private boolean populateShapes() {
		if(stackShapes != null || isCodeValid != null) {
			/* this method should be called only once */
			throw new Error();
		}
		Repository rep = getRepository();
		try {
			BT_StackPool pool = rep.pool;
			provider = new BT_PopulatedObjectProvider(underlyingMethod, getCode(), rep.objectPool);
			BT_StackShapeVisitor visitor = new BT_StackShapeVisitor(getCode(), pool, provider);
			visitor.ignoreUpcasts(false);
			visitor.useMergeCandidates(false);//xx;
			BT_StackShapes shapes = visitor.populate();
			try {
				//shapes.verifyStacks(); if some classes were not loaded, this can cause unwanted exceptions
				stackShapes = shapes.stackShapes;
				shapes.stackShapes = null;
			} finally {
				shapes.returnStacks();
			}
			return true;
		} catch(BT_CodeException e) {
			/* 
			 * We could not analyze the code for some reason.
			 * We try to verify the code: if it verifies, then
			 * we assume everything can be passed from now on, and we output a warning
			 * with the original exception.
			 * 
			 * If it does not verify, then we conclude the code
			 * not structured properly and cannot be executed, so
			 * we pass nothing from now on, and we log the code exception. 
			 */
			try {
				getCode().verify();
				isCodeValid = Boolean.TRUE;
				rep.messages.FLOW_ERROR.log(rep.logger, new Object[] {this, e.getMessage()});
			} catch(BT_CodeException e2) {
				isCodeValid = Boolean.FALSE;
				getRepository().repository.factory.noteCodeException(e2);
			}
			return false;
		}
	}
	
	private boolean checkMapping(MethodInvocationLocation start, InstructionLocation target, int targetStackCellIndex) {
		if(stackShapes == null) {
			if(isCodeValid != null || !populateShapes()) {
				return isCodeValid.booleanValue();
			}
			/* the shapes will be reused, but not all are required. */
			clearUnusedShapes(getRepository().pool);
		} 
		AcquiredObject introduced;
		if(start.isParameter()) {
			ParameterLocation paramLocation = (ParameterLocation) start;
			introduced = provider.parameterObjects[paramLocation.paramIndex];
		} else {
			StackLocation stackLocation = (StackLocation) start;
			introduced = provider.stackObjects[stackLocation.instructionIndex];
		}
		BT_StackCell targetStack[] = stackShapes[target.instructionIndex];
		BT_StackCell targetCell = targetStack[targetStack.length - targetStackCellIndex];
		if(targetCell instanceof BT_PopulatedObjectCell) {
			BT_PopulatedObjectCell popTargetCell = (BT_PopulatedObjectCell) targetCell;
			return popTargetCell.contains(introduced);
		}
		return false;
	}

	private boolean isVerified() {
		return (flags & verified) != 0;
	}
	
	private void setVerified() {
		flags |= verified;
	}
	
	private boolean isScanned() {
		return (flags & scanned) != 0;
	}
	
	private void setScanned() {
		flags |= scanned;
	}
	
	private void setStoresIntoArrays() {
		flags |= storesIntoArrays;
	}
	
	private void setLoadsFromArrays() {
		flags |= loadsFromArrays;
	}
	
	private void setCanThrow() {
		flags |= canThrow;
	}
	
	boolean cannotBeFollowed() {
		return isNative() || getCode() == null;
	}
	
	private void scanSimple() {
		if(cannotBeFollowed()) {
			/* for natives we assume the worst to try to be safe */
			setCanThrow();
			setStoresIntoArrays();
			setLoadsFromArrays();
			arrayReads = arrayWrites = returns = throwInstructions = noLocations;
		} else if(underlyingMethod.isAbstract()) {
			arrayReads = arrayWrites = returns = throwInstructions = noLocations;
		}
	}
	
	/**
	 Record information about the code that is not available from the code attribute.
	**/
	private void scanCode() {
		setScanned();
		BT_CodeAttribute code = getCode();
		if(code == null) {
			scanSimple();
			return;
		}
		ArrayList fieldReads = null;
		ArrayList fieldWrites = null;
		ArrayList instantiations = null;
		ArrayList invocations = null;
		InstructionLocation firstFieldRead = null;
		InstructionLocation firstFieldWrite = null;
		InstructionLocation firstInvocation = null;
		InstructionLocation firstInstantiation = null;
		
		BT_InsVector ins = code.getInstructions();
		int size = ins.size();
		if(useIntraProceduralAnalysis()) {
			InstructionLocation firstArrayRead = null;
			InstructionLocation firstArrayWrite = null;
			InstructionLocation firstThrow = null;
			InstructionLocation firstReturn = null;
			ArrayList arrayReads = null;
			ArrayList arrayWrites = null;
			ArrayList throwList = null;
			ArrayList returnList = null;
			
			for (int k = 0; k < size; k++) {
				BT_Ins instruction = ins.elementAt(k);
				switch(instruction.opcode) {
					case BT_Ins.opc_areturn:
						InstructionLocation location  = new InstructionLocation(instruction, k);
						if(firstReturn == null) {
							firstReturn = location;
						} else {
							if(returnList == null) {
								returnList = new ArrayList(ins.size() / 4);
								returnList.add(firstReturn);
							}
							returnList.add(location);
						}
						continue;
					case BT_Ins.opc_athrow:
						setCanThrow();
						location  = new InstructionLocation(instruction, k);
						if(firstThrow == null) {
							firstThrow = location;
						} else {
							if(throwList == null) {
								throwList = new ArrayList(ins.size() / 4);
								throwList.add(firstThrow);
							}
							throwList.add(location);
						}
						continue;
					case BT_Ins.opc_aastore:
						setStoresIntoArrays();
						location  = new InstructionLocation(instruction, k);
						if(firstArrayWrite == null) {
							firstArrayWrite = location;
						} else {
							if(arrayWrites == null) {
								arrayWrites = new ArrayList(ins.size() / 4);
								arrayWrites.add(firstArrayWrite);
							}
							arrayWrites.add(location);
						}
						continue;
					case BT_Ins.opc_aaload:
						setLoadsFromArrays();
						location = new InstructionLocation(instruction, k);
						if(firstArrayRead == null) {
							firstArrayRead = location;
						} else {
							if(arrayReads == null) {
								arrayReads = new ArrayList(ins.size() / 4);
								arrayReads.add(firstArrayRead);
							}
							arrayReads.add(location);
						}
						continue;
					default:
						if(instruction.isNewIns()) {
							location = new InstructionLocation(instruction, k);
							if(firstInstantiation == null) {
								firstInstantiation = location;
							} else {
								if(instantiations == null) {
									instantiations = new ArrayList(ins.size() / 4);
									instantiations.add(firstInstantiation);
								}
								instantiations.add(location);
							}
						} else if(instruction.isFieldAccessIns()) {
							location = new InstructionLocation(instruction, k);
							if(instruction.isFieldReadIns()) {
								if(firstFieldRead == null) {
									firstFieldRead = location;
								} else {
									if(fieldReads == null) {
										fieldReads = new ArrayList(ins.size() / 4);
										fieldReads.add(firstFieldRead);
									}
									fieldReads.add(location);
								}
							} else {
								if(firstFieldWrite == null) {
									firstFieldWrite = location;
								} else {
									if(fieldWrites == null) {
										fieldWrites = new ArrayList(ins.size() / 4);
										fieldWrites.add(firstFieldWrite);
									}
									fieldWrites.add(location);
								}
							}
						} else if(instruction.isInvokeIns()) {
							location = new InstructionLocation(instruction, k);
							if(firstInvocation == null) {
								firstInvocation = location;
							} else {
								if(invocations == null) {
									invocations = new ArrayList(ins.size() / 4);
									invocations.add(firstInvocation);
								}
								invocations.add(location);
							}
						}
						continue;
				}
			}
			this.throwInstructions = getArray(throwList, firstThrow);
			this.arrayReads = getArray(arrayReads, firstArrayRead);
			this.arrayWrites = getArray(arrayWrites, firstArrayWrite);
			this.returns = getArray(returnList, firstReturn);
		} else {
			for (int k = 0; k < size; k++) {
				BT_Ins instruction = ins.elementAt(k);
				switch(instruction.opcode) {
					case BT_Ins.opc_athrow:
						setCanThrow();
						continue;
					case BT_Ins.opc_aastore:
						setStoresIntoArrays();
						continue;
					case BT_Ins.opc_aaload:
						setLoadsFromArrays();
						continue;
					default:
						if(instruction.isNewIns()) {
							InstructionLocation location = new InstructionLocation(instruction, k);
							if(firstInstantiation == null) {
								firstInstantiation = location;
							} else {
								if(instantiations == null) {
									instantiations = new ArrayList(ins.size() / 4);
									instantiations.add(firstInstantiation);
								}
								instantiations.add(location);
							}
						} else if(instruction.isFieldAccessIns()) {
							InstructionLocation location = new InstructionLocation(instruction, k);
							if(instruction.isFieldReadIns()) {
								if(firstFieldRead == null) {
									firstFieldRead = location;
								} else {
									if(fieldReads == null) {
										fieldReads = new ArrayList(ins.size() / 4);
										fieldReads.add(firstFieldRead);
									}
									fieldReads.add(location);
								}
							} else {
								if(firstFieldWrite == null) {
									firstFieldWrite = location;
								} else {
									if(fieldWrites == null) {
										fieldWrites = new ArrayList(ins.size() / 4);
										fieldWrites.add(firstFieldWrite);
									}
									fieldWrites.add(location);
								}
							}
						} else if(instruction.isInvokeIns()) {
							InstructionLocation location = new InstructionLocation(instruction, k);
							if(firstInvocation == null) {
								firstInvocation = location;
							} else {
								if(invocations == null) {
									invocations = new ArrayList(ins.size() / 4);
									invocations.add(firstInvocation);
								}
								invocations.add(location);
							}
						}
						continue;
				}
				
			}
		}
		this.fieldReads = getArray(fieldReads, firstFieldRead);
		this.fieldWrites = getArray(fieldWrites, firstFieldWrite);
		this.invocations = getArray(invocations, firstInvocation);
		this.instantiations = getArray(instantiations, firstInstantiation);
	}
	
	private InstructionLocation[] getArray(ArrayList list, InstructionLocation first) {
		if(list != null) {
			return (InstructionLocation[]) list.toArray(new InstructionLocation[list.size()]);
		} else if(first != null) {
			return new InstructionLocation[] {first};
		} 
		return noLocations;
	}
	
	InstructionLocation[] getThrowInstructions() {
		if(!useIntraProceduralAnalysis()) {
			throw new IllegalStateException();
		}
		if(!isScanned()) {
			scanCode();
		}
		return throwInstructions;
	}
	
	InstructionLocation[] getObjectReturns() {
		if(!useIntraProceduralAnalysis()) {
			throw new IllegalStateException();
		}
		if(!isScanned()) {
			scanCode();
		}
		return returns;
	}
	
	InstructionLocation[] getArrayReads() {
		if(!useIntraProceduralAnalysis()) {
			throw new IllegalStateException();
		}
		if(!isScanned()) {
			scanCode();
		}
		return arrayReads;
	}
	
	InstructionLocation[] getArrayWrites() {
		if(!useIntraProceduralAnalysis()) {
			throw new IllegalStateException();
		}
		if(!isScanned()) {
			scanCode();
		}
		return arrayWrites;
	}
	/**
	 * returns true if the method contains an aastore instruction
	 */
	boolean storesIntoArrays() {
		if(!isScanned()) {
			scanCode();
		}
		return (flags & storesIntoArrays) != 0;
	}
	
	/**
	 * returns true if the method contains an aaload instruction
	 */
	boolean loadsFromArrays() {
		if(!isScanned()) {
			scanCode();
		}
		return (flags & loadsFromArrays) != 0;
	}
	
	/**
	 * returns true if the method contains an athrow instruction
	 */
	public boolean canThrow() {
		if(!isScanned()) {
			scanCode();
		}
		return (flags & canThrow) != 0;
	}
	
	private PotentialClassReference instantiationReferences[];
	
	public PotentialClassReference[] getInstantations() {
		if(instantiationReferences == null) {
			if(!isScanned()) {
				scanCode();
			}
			InstructionLocation instantiations[] = this.instantiations;
			PotentialClassReference instantiationReferences[] = new PotentialClassReference[instantiations.length];
			Repository rep = getRepository();
			for(int i=0; i<instantiationReferences.length; i++) {
				InstructionLocation location = instantiations[i];
				BT_NewIns instruction = (BT_NewIns) location.instruction;
				Clazz target = rep.getClazz(instruction.getClassTarget());
				instantiationReferences[i] = new PotentialClassReference(target, location);
			}
			this.instantiationReferences = instantiationReferences;
		}
		return instantiationReferences;
	}
	
	public InstructionLocation[] getFieldReads() {
		if(!isScanned()) {
			scanCode();
		}
		return fieldReads;
	}
	
	public InstructionLocation[] getFieldWrites() {
		if(!isScanned()) {
			scanCode();
		}
		return fieldWrites;
	}
	
	public InstructionLocation[] getInvocations() {
		if(!isScanned()) {
			scanCode();
		}
		return invocations;
	}
	
	BT_MethodSignature getSignature() {
		return underlyingMethod.getSignature();
	}
	
	void enterVerifierRequiredClasses(CallingContext context) {
		Repository repository = getRepository();
		PropagationProperties props = repository.getPropagationProperties();
		if(props.isEscapeAnalysis()) {
			return;
		}
		if(getCode() == null) {
			return;
		}
		findVerifierRequiredClasses();
		
		JaptRepository japtRepository = repository.repository;
		EntryPointLister lister = repository.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(getDeclaringClass().getUnderlyingType());
		for(int i=0; i<verifierRequiredClasses.size(); i++) {
			BT_Class referencedClass = verifierRequiredClasses.elementAt(i);
			if(checkEntryPoints && !referencedClass.isPrimitive() && japtRepository.isInternalClass(referencedClass)) {
				lister.foundEntryTo(referencedClass, underlyingMethod);
			}
			Clazz clazz = repository.getClazz(referencedClass);
			clazz.setRequired();
			context.enter(clazz);
		}
	}
	
	
	private void findVerifierRequiredClasses() {
		if(isVerified()) {
			return;
		}
		setVerified();
		BT_StackPool pool = getRepository().pool;
		BT_CodeAttribute code = getCode();
		
		boolean hasShapes = stackShapes != null;
		if(!hasShapes) {
			if(hasShapes = isCodeValid == null && populateShapes()) {
				clearUnusedShapes(pool);
			}
		}
		if(hasShapes) {
			verifierRequiredClasses = code.getVerifierRequiredClasses(stackShapes);
		} else {
			if(isCodeValid.booleanValue()) {
				verifierRequiredClasses = code.getVerifierRequiredClasses(pool);
			} else {
				verifierRequiredClasses = BT_ClassVector.emptyVector;
			}
		}
	}
	
	/**
	 * This method finds classes that are required by the method body and marks them as being
	 * required. This is done when the method is accessed.  This could be changed so that it is
	 * done when the method is marked required for more stringent VM's 
	 */
	void findReferences(CallingContext context) {
		PropagationProperties props = getRepository().getPropagationProperties();
		if(props.isEscapeAnalysis()) {
			return;
		}
		BT_CodeAttribute code = getCode();
		if(code == null) {
			return;
		}
		Repository repository = getRepository();
		JaptRepository japtRepository = repository.repository;
		EntryPointLister lister = repository.entryPointLister;
		boolean checkEntryPoints = (lister != null) && !japtRepository.isInternalClass(getDeclaringClass().getUnderlyingType());
		
		
		//determine which classes are required by the method body
		BT_ClassReferenceSiteVector referencedClasses = code.referencedClasses;
		for(int i=0; i<referencedClasses.size(); i++) {
			BT_Class referencedClass = referencedClasses.elementAt(i).getTarget();
			if(checkEntryPoints && !referencedClass.isPrimitive() && japtRepository.isInternalClass(referencedClass)) {
				lister.foundEntryTo(referencedClass, underlyingMethod);
			}
			
			Clazz clazz = repository.getClazz(referencedClass);
			clazz.setRequired();
			context.enter(clazz);
		}
		
		//if a method is included, then anything that it explicitly declares that it can catch is required
		BT_ExceptionTableEntryVector exceptions = code.getExceptionTableEntries();
		for (int t=0; t < exceptions.size(); t++) {
			BT_ExceptionTableEntry e = exceptions.elementAt(t);
			BT_Class catchType = e.catchType;
			if (catchType == null)
				continue;
			
			if(checkEntryPoints && !catchType.isPrimitive() && japtRepository.isInternalClass(catchType)) {
				lister.foundEntryTo(catchType, underlyingMethod);
			}
			Clazz clazz = repository.getClazz(catchType);
			clazz.setRequired();
			context.enter(clazz);
		}
	}
	
	
	
	Clazz[] getConditionalInstantiations() {
		if(conditionalInstantiations == null) {
			BT_ClassVector conditionals = getRepository().getConditionallyCreatedObjects(underlyingMethod);
			
			Clazz result[] = null;
			int index = 0;
			if(cannotBeFollowed()) {
				//TODO maybe trash this part
				Clazz returnType = getReturnType();
				Clazz declaredExceptions[] = getDeclaredExceptions();
				Repository rep = getRepository();
				Clazz error = rep.getJavaLangError();
				Clazz runtimeException = rep.getJavaLangRuntimeException();
				int count = declaredExceptions.length + 2;
				if(!returnType.isPrimitive()) {
					count++;
				}
				Clazz unknowns[] = new Clazz[count + conditionals.size()];
				index = declaredExceptions.length;
				System.arraycopy(declaredExceptions, 0, unknowns, 0, index);
				unknowns[index++] = error;
				unknowns[index++] = runtimeException;
				if(!returnType.isPrimitive()) {
					unknowns[index++] = returnType;
				}
			} 
			if(conditionals.size() > 0) {
				if(result == null) {
					result = new Clazz[conditionals.size()];
					index = 0;
				}
				Repository rep = getRepository();
				for(int i=0; i<conditionals.size(); i++) {
					result[index++] = rep.getClazz(conditionals.elementAt(i));
				}
			} else if(result == null) {
				result = emptyTypes;
			}
			conditionalInstantiations = result;
		}
		return conditionalInstantiations;
	}
	
	public String toString() {
		return underlyingMethod.toString();
	}
}
