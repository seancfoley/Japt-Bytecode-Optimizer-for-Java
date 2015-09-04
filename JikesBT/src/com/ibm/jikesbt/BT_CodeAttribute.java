package com.ibm.jikesbt;


/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import com.ibm.jikesbt.BT_AttributeVector.WrittenAttributesLength;
import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;
import com.ibm.jikesbt.BT_CodeException.BT_StackUnderflowException;
import com.ibm.jikesbt.BT_ConstructorLocator.ChainedConstructorLocator;
import com.ibm.jikesbt.BT_ConstructorLocator.NewInsConstructorLocator;
import com.ibm.jikesbt.BT_ObjectCode.SubRoutine;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

// The Code attribute has the format
// <code><pre>
//     Code_attribute {
//
//         u2 attribute_name_index;
//         u4 attribute_length;
//         u2 max_stack;               <<<<< 'byte[] bytecodes' starts here
//         u2 max_locals;
//         u4 code_length;
//         u1 code[code_length];       <<<<< index '8' of bytecodes
//         u2 exception_table_length;
//         {       u2 start_pc;
//                 u2 end_pc;
//                 u2  handler_pc;
//                 u2  catch_type;
//         }       exception_table[exception_table_length];
//         u2 attributes_count;
//         attribute_info attributes[attributes_count];
//     }
// </pre></code>

/**
 Represents the Code method attribute, including instructions, and
 try-catch-block exception descriptions, stack size, locals, ....

 <p> See the <a href=../jikesbt/doc-files/UserGuide.html#BT_CODE>User Guide<a>.

 * @author IBM
**/
public final class BT_CodeAttribute extends BT_Attribute implements BT_AttributeOwner {
	
	public static final String ATTRIBUTE_NAME = "Code";

	/**
	 * represents the instructions and exception handlers for this code attribute.
	 */
	BT_Code code;
	
	/**
	 * the contained attributes
	 */
	public BT_AttributeVector attributes;
	
	/*
	 * The following vectors are provided for convenience, so that it is not necessary to
	 * scan the body every time we wish to determine which outside items are accessed from the body. 
	 */
	/**
	 Method invoke instructions in the method body.
	 **/
	public BT_MethodCallSiteVector calledMethods = new BT_MethodCallSiteVector();

	/**
	 Field reference instructions in the method body
	 **/
	public BT_AccessorVector accessedFields = new BT_AccessorVector();
	
	/**
	 Classes created in the method body
	 **/
	public BT_CreationSiteVector createdClasses = new BT_CreationSiteVector();
	
	/**
	 Classes referenced in the method body, which includes those classes which are created
	 **/
	public BT_ClassReferenceSiteVector referencedClasses = new BT_ClassReferenceSiteVector();
	
	/**
	 * provides information about the operand stack and locals for this code attribute.
	 */
	private CodeInfo codeInfo;
	
	/**
	 The amount of stack space this method may use.
	 Set when the method is read and by {#resolve}, but not kept up to date if
	 the method is changed.
	**/
	interface CodeInfo {
		int getMaxStack() throws BT_CodeException;
		
		int getMaxStackQuickly() throws BT_CodeException;
	
		int getMaxLocals() throws BT_CodeException;
		
		int getMaxLocalsQuickly() throws BT_CodeException;
		
		/**
		 * The instructions have changed, reset all stored
		 * local and stack information.
		 */
		void reset();
		
		/**
		 * force the max locals to be recalculated.
		 */
		void resetMaxLocals();
	}
	
	class CodeAttributeInfo implements CodeInfo {
		int maxStack;
		int maxLocals;
		
		CodeAttributeInfo() {
			this(-1, -1);
		}
		
		CodeAttributeInfo(int maxStack, int maxLocals) {
			this.maxStack = maxStack;
			this.maxLocals = maxLocals;
		}
		
		void computeBoth() throws BT_CodeException {
			BT_MaxLocalsVisitor localsVisitor = new BT_MaxLocalsVisitor();
			BT_StackDepthVisitor stackVisitor = new BT_StackDepthVisitor();
			visitReachableCode(new BT_DualCodeVisitor(localsVisitor, stackVisitor));
			maxStack = stackVisitor.getMaxDepth();
			maxLocals = localsVisitor.getMaxLocals();
		}
		
		public int getMaxLocalsQuickly() throws BT_CodeException {
			if(maxLocals < 0) {
				maxLocals = computeMaxLocals();
			}
			return maxLocals;
		}
		
		public int getMaxLocals() throws BT_CodeException {
			if(maxLocals < 0) {
				if(maxStack < 0) {
					computeBoth();
				} else {
					maxLocals = computeMaxLocals();
				}
			}
			return maxLocals;
		}
		
		public int getMaxStackQuickly() throws BT_CodeException {
			return getMaxStack();
		}
		
		public int getMaxStack() throws BT_CodeException {
			if(maxStack < 0) {
				if(maxLocals < 0) {
					computeBoth();
				} else {
					maxStack = computeMaxStackDepth();
				}
			}
			return maxStack;
		}
		
		public void reset() {
			maxLocals = maxStack = -1;
		}
		
		public void resetMaxLocals() {
			maxLocals = -1;
		}
	}
	
	/**
	 This form of constructor is used when a class file is read.
	 @param data  The part of the Code attribute starting after "attribute_length".
	**/
	protected BT_CodeAttribute(byte data[], BT_Method m, BT_ConstantPool pool, LoadLocation loadedFrom)
		throws BT_ClassFileException, IOException {
		super(m, loadedFrom);
		if (data.length < 8)
			throw new BT_ClassFileException(
			Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
		BT_ByteCode undef = new BT_ByteCode(this, loadedFrom);
		this.code = undef;
		int maxStack = BT_Misc.bytesToUnsignedShort(data, 0);
		int maxLocals = BT_Misc.bytesToUnsignedShort(data, 2);
		int codeLen = undef.codeLen = BT_Misc.bytesToInt(data, 4);
		if (codeLen == 0) {
			throw new BT_ClassFileException(
				ATTRIBUTE_NAME + Messages.getString("JikesBT._empty_bytecode_array_4"));
		}
		// The following limitiation comes from VM spec 2nd ed, paragraph 4.10
		if (BT_Misc.overflowsUnsignedShort(codeLen - 1)) {
			throw new BT_ClassFileException(
				ATTRIBUTE_NAME + Messages.getString("JikesBT._bytecode_array_too_long_5"));
		}
		undef.bytecodes = data;
		if (data.length < 10 + codeLen) {
			throw new BT_ClassFileException(
					Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
		}
		int nrExceptions = undef.nrExceptions = BT_Misc.bytesToUnsignedShort(data, 8 + codeLen);
		int next = (10 + codeLen) + 8 * nrExceptions;
		BT_Repository repo = m.getDeclaringClass().getRepository();
		try {
			DataInputStream dis =
				new DataInputStream(
					new ByteArrayInputStream(data, next, data.length - next));
			attributes = BT_AttributeVector.read(dis, m.cls.pool, this, m, repo, loadedFrom);
			if (dis.available() > 0) {
				throw new BT_ClassFileException(
				Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
			}
		} catch(IOException e) {
			throw new BT_ClassFileException(e.getMessage());
		}
		BT_StackMapAttribute att = (BT_StackMapAttribute) attributes.getAttribute(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
		if(att == null) {
			codeInfo = new CodeAttributeInfo(maxStack, maxLocals);
		} else {
			codeInfo = att;
			att.setMaxes(maxLocals, maxStack);
		}
	}

	/**
	 Constructs from an array of instructions.
	 As a side-effect, sets the offset of each instruction in instrs.
	**/
	public BT_CodeAttribute(BT_Ins[] instrs, int maxLocals, int maxStack, boolean withStackMaps) throws BT_CodeException {
		this(instrs, withStackMaps);
		if(maxLocals > 0 || maxStack > 0) {
			if(withStackMaps) {
				BT_StackMapAttribute att = (BT_StackMapAttribute) codeInfo;
				if(maxStack > 0) {
					att.tempMaxStack = maxStack;
				}
				if(maxLocals > 0) {
					att.tempMaxLocals = maxLocals;
				}
			} else {
				CodeAttributeInfo codeAttInfo = (CodeAttributeInfo) codeInfo;
				if(maxStack > 0) {
					codeAttInfo.maxStack = maxStack;
				}
				if(maxLocals > 0) {
					codeAttInfo.maxLocals = maxLocals;
				}
			}
		}
	}
	
	/**
	 Constructs from an array of instructions.
	**/
	public BT_CodeAttribute(BT_Ins[] instrs, boolean withStackMaps) {
		super(null);
		attributes = new BT_AttributeVector();
		if(withStackMaps) {//TODO everywhere we add new stackmaps, have the option to add new CLDC stackmaps instead (see GenerationExtension, there we add directly to attribtues vector)
			BT_StackMapAttribute att = new BT_StackMapAttribute(this, BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
			attributes.addElement(att);
			codeInfo = att;
		} else {
			codeInfo = new CodeAttributeInfo();
		}
		BT_ObjectCode theCode = new BT_ObjectCode(this, instrs.length, 0, 0);
		code = theCode;
		insertInstructionsAt(instrs, 0);
		theCode.ins.trimToSize();
		computeMaxInstructionSizes();
	}
	
	/**
	 Constructs from an array of instructions.
	**/
	public BT_CodeAttribute(BT_Ins[] instrs, BT_ClassVersion version) {
		this(instrs, version.shouldHaveStackMaps());
	}
	
	/**
	 * clones this code attribute with a deep clone, and also makes all cloned contained
	 * elements point to the new code attribute.  The contained elements that are cloned include the 
	 * contained attributes (such as stack maps, local variable table and line number table), 
	 * the contained exception table entries, the contained instruction vector
	 * and each instruction in the vector, and the local vector of the instruction vector.
	 */
	public Object clone() {
		BT_CodeAttribute clone = (BT_CodeAttribute) super.clone();
		
		//these four fields will be repopulated when we dereference instructions
		clone.calledMethods = new BT_MethodCallSiteVector(calledMethods.size());
		clone.accessedFields = new BT_AccessorVector(accessedFields.size());
		clone.createdClasses = new BT_CreationSiteVector(createdClasses.size());
		clone.referencedClasses = new BT_ClassReferenceSiteVector(referencedClasses.size());
		
		if(code != null) {
			BT_Code clonedCode = clone.code = (BT_Code) code.clone();
			if(clonedCode == null) {
				return null;
			}
			clonedCode.codeAttribute = clone;
			try {
				clonedCode.dereference(getMethod());
			} catch(BT_ClassFileException e) {
				clonedCode.removeAllInstructions();
				return null;
			} catch(BT_BytecodeException e) {
				clonedCode.removeAllInstructions();
				return null;
			}
		}
		
		clone.attributes = (BT_AttributeVector) attributes.clone();
		BT_StackMapAttribute stackMapAtt = null;
		for(int i=0; i<clone.attributes.size(); i++) {
			BT_Attribute clonedAttribute = clone.attributes.cloneElementAt(i);
			if(clonedAttribute instanceof BT_LocalVariableAttribute) {
				((BT_LocalVariableAttribute) clonedAttribute).container = clone;
			} else if(clonedAttribute instanceof BT_LineNumberAttribute) {
				((BT_LineNumberAttribute) clonedAttribute).container = clone;
			} else if(clonedAttribute instanceof BT_StackMapAttribute) {
				stackMapAtt = (BT_StackMapAttribute) clonedAttribute;
				stackMapAtt.container = clone;
			}
		}
		
		BT_InsVector inst = getInstructions();
		if(inst != null) {
			BT_InsVector clonedIns = clone.getInstructions();
			
			// Change references to new code
			for (int k = 0; k < inst.size(); k++) {
				BT_Ins from = inst.elementAt(k);
				BT_Ins to = clonedIns.elementAt(k);
				//this will change the references in all the attributes
				clone.attributes.changeReferencesFromTo(from, to, true);
			}
		}
		if(stackMapAtt != null) {
			clone.codeInfo = stackMapAtt;
		} else {
			clone.codeInfo = clone.new CodeAttributeInfo();
		}
		return clone;
	}
	
	public BT_InsVector getInstructions() {
		return code.getInstructions();
	}
	
	public int getInstructionSize() {
		return code.getInstructionSize();
	}
	
	public BT_ExceptionTableEntryVector getExceptionTableEntries() {
		return code.getExceptionTableEntries();
	}
	
	public void insertExceptionTableEntry(BT_ExceptionTableEntry e, int index) {
		code.insertExceptionTableEntry(e, index);
	}
	
	public void insertExceptionTableEntry(BT_ExceptionTableEntry e) {
		code.insertExceptionTableEntry(e);
	}
	
	public int getExceptionTableEntryCount() {
		return code.getExceptionTableEntryCount();
	}
	
	public void setExceptionTable(BT_ExceptionTableEntryVector table) {
		code.setExceptionTable(table);
	}
	
	public BT_LocalVector getLocals() {
		return code.getLocals();
	}
	
	public BT_AttributeVector getAttributes() {
		return attributes;
	}
	
	public BT_Item getEnclosingItem() {
		return getMethod();
	}
	
	/** 
	 * Ensures that this code attribute has the given stack map attribute.  If the code attribute already has
	 * a stack map attribute, it is replaced by the given one.
	 */
	public void addStackMaps(BT_StackMapAttribute att) {
		if(att == null) {
			throw new NullPointerException();
		}
		attributes.removeAttribute(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
		codeInfo = att;
		attributes.addElement(att);
	}
	
	/** 
	 * Ensures that this code attribute has a stack map attribute.  Does not create one if the code
	 * attribute already has one.
	 */
	public BT_StackMapAttribute addStackMaps() {
		BT_StackMapAttribute att = getStackMaps();
		if(att != null) {
			return att;
		}
		att = new BT_StackMapAttribute(this, BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
		codeInfo = att;
		attributes.addElement(att);
		return att;
	}
	
	public BT_StackMapAttribute addCLDCStackMaps() {
		BT_StackMapAttribute att = (BT_StackMapAttribute) attributes.getAttribute(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
		if(att != null) {
			return att;
		}
		att = new BT_StackMapAttribute(this, BT_StackMapAttribute.CLDC_STACKMAP_NAME);
		attributes.addElement(att);
		return att;
	}
	
	/** 
	 * Removes the stack map attribute from this code attribute if it has one.
	 * @return the attribute that was removed, or null if none to be found
	 */
	public BT_StackMapAttribute removeStackMaps() {
		BT_StackMapAttribute att =
			(BT_StackMapAttribute) attributes.removeAttribute(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
		codeInfo = new CodeAttributeInfo();
		return att;
	}
	
	/** 
	 * @return whether this attribute has a stack map attribute
	 */
	public boolean hasStackMaps() {
		return getStackMaps() != null;
	}
	
	public BT_StackMapAttribute getStackMaps() {
		return (BT_StackMapAttribute) attributes.getAttribute(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
	}
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total length of all instructions, see BT_Ins.size().
	**/
	public int computeInstructionSizes() {
		return code.computeInstructionSizes();
	}
	
	/**
	 Sets the byte offset in each instruction.
	 @return  The total max length of all instructions, see BT_Ins.maxSize().
	**/
	public int computeMaxInstructionSizes() {
		return code.computeMaxInstructionSizes();
	}

	/**
	 * the max stack and max locals values are cached until the bytecode
	 * instructions have changed.  Call this method to ensure that these
	 * values are recalculated from the instructions.
	 */
	public void resetCachedCodeInfo() {
		codeInfo.reset(); /* codeInfo is the StackMapTable stackmaps if they exist */
		BT_StackMapAttribute att = (BT_StackMapAttribute) attributes.getAttribute(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
		if(att != null) {
			att.reset();
		}
	}
	
	/**
	 * @deprecated replaced by resetCachedCodeInfo
	 */
	public void resetMaxStack() {
		resetCachedCodeInfo();
	}
	
	/**
	 * Make this a code attribute of the given method prior to
	 * calling BT_Method.setCode.  
	 * Callers of this method should eventually call BT_Method.setCode.
	 * Callers of BT_Method.setCode need not call this method. 
	 * @param m
	 */
	public void setMethod(BT_Method m) {
		if(this.container == m) {
			return;
		}
		if(m == null || this.container == null || m.getArgsSize() != getMethod().getArgsSize()) {
			resetArgs();
		}
		this.container = m;
	}
	
	void resetArgs() {
		codeInfo.resetMaxLocals();
	}
	
	void resetMaxLocals() {
		codeInfo.resetMaxLocals();
	}
	
	/**
	 * Adds a method called by an instruction within this code attribute.
	 */
	void addCalledMethod(BT_MethodCallSite site) {
		for (int n = calledMethods.size() - 1; n >= 0; n--)
			if (calledMethods.elementAt(n).instruction == site.instruction)
				return;
		calledMethods.addElement(site);
	}
	
	/**
	 * Removes a method called by an instruction within this code attribute.
	 */
	void removeCalledMethod(BT_MethodRefIns ins) {
		for (int n = calledMethods.size() - 1; n >= 0; n--)
			if (calledMethods.elementAt(n).instruction == ins)
				calledMethods.removeElementAt(n);
	}
	
	/**
	 * Adds a field accessed by an instruction within this code attribute.
	 */
	void addAccessedField(BT_Accessor site) {
		for (int n = accessedFields.size() - 1; n >= 0; n--)
			if (accessedFields.elementAt(n).instruction == site.instruction)
				return;
		accessedFields.addElement(site);
	}
	
	/**
	 * Removes a field accessed by an instruction within this code attribute.
	 */
	void removeAccessedField(BT_FieldRefIns ins) {
		for (int n = accessedFields.size() - 1; n >= 0; n--)
			if (accessedFields.elementAt(n).instruction == ins)
				accessedFields.removeElementAt(n);
	}
	
	/**
	 * Adds a creation site by an instruction within this code attribute.
	 */
	void addCreatedClass(BT_CreationSite site) {
		for (int n = createdClasses.size() - 1; n >= 0; n--) {
			if (createdClasses.elementAt(n).instruction == site.instruction) {
				return;
			}
		}
		createdClasses.addElement(site);
	}
	
	/**
	 * Removes creation sites by an instruction within this code attribute.
	 * Note that the multianewarray will create
	 * instances of multiple classes so multiple sites will be removed for this type
	 * of instruction.
	 */
	void removeCreatedClass(BT_Ins ins) {
		for (int n = createdClasses.size() - 1; n >= 0; n--) {
			if (createdClasses.elementAt(n).instruction == ins) {
				createdClasses.removeElementAt(n);
			}
		}
	}
	
	/**
	 * Adds a class reference site by an instruction within this code attribute.
	 */
	void addReferencedClass(BT_ClassReferenceSite site) {
		for (int n = referencedClasses.size() - 1; n >= 0; n--) {
			if (referencedClasses.elementAt(n).instruction == site.instruction) {
				return;
			}
		}
		referencedClasses.addElement(site);
	}
	
	/**
	 * Removes class reference sites by an instruction within this code attribute.
	 * Note that the multianewarray instruction will remove multiple class references.
	 */
	void removeReferencedClass(BT_Ins ins) {
		for (int n = referencedClasses.size() - 1; n >= 0; n--) {
			if (referencedClasses.elementAt(n).instruction == ins) {
				referencedClasses.removeElementAt(n);
			}
		}
	}	

	public BT_Method getMethod() {
		return (BT_Method) container;
	}
	
	/**
	 Transforms bytes into a vector of instructions and
	 links instructions directly to objects with which they have a
	 relationship, e.g. methods, classes, other instructions.
	 <p> For more information, see
	 <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>.
	 @see BT_Class#loadMethods
	 @see BT_Class#keepConstantPool
	 @see BT_CodeAttribute#keepBytecodes
	**/
	void dereference(BT_Repository rep) throws BT_ClassFileException {
		BT_Method method = getMethod();
		if (rep.factory.loadMethods) {
			/*
			Note: at this time, the instructions contain the opcodes from which 
			they were constructed.
			
			The size of the instructions are calculated based on these opcodes, 
			and thus the offsets currently being stored in the instructions are 
			based on these original sizes.
			
			When the instructions are dereferenced below, 
			they may potentially change their opcodes, 
			and thus their sizes could potentially change.  
			So it is important that we DO NOT recompute the offsets stored in the byteIndex field 
			of each instruction, 
			until after we have dereferenced the exception table entries and the attributes, 
			because these entries and attributes are based on the original offsets 
			and will use the original offsets to locate referenced instructions.
			
			However, we dereference the instructions first, since the attributes may need
			to check the instructions' properties.
			*/
			
			dereference(method);
			
			// Now it is OK to recompute the offsets in the instructions.
			// Dereferencing can change the sizes of instructions.
			// But we cannot recompute the offsets in the instructions until the attributes
			// have been dereferenced.
			code.computeMaxInstructionSizes();
		}
		
		accessedFields.trimToSize();
		calledMethods.trimToSize();
		createdClasses.trimToSize();
		referencedClasses.trimToSize();
	}
	
	public void dereference(BT_Method method) throws BT_ClassFileException {
		code = code.dereference(method);
		attributes.dereference(method, method.cls.getRepository());
	}
	
	public void removeContents() {
		removeAllInstructions();
		attributes.remove();
	}
	
	public void remove() {
		removeContents();
		super.remove();
	}
	
	/**
	 * Undo any reference to the given item.
	 * Does not include references by instructions.
	 */
	void removeReference(BT_Item reference) {
		attributes.removeReference(reference);
	}

	/**
	 Adds a try/catch/finally block.
	 Note the stack must have the same depth before startInsNr and
	 after EndInsNr??
	
	 <p> BEWARE:  This can insert basic-block pseudo-instructions as a
	 side effect, and these would change the indices of later
	 instructions.
	
	 @param  endInsNr  The index (not the byte-offset) of the last
	 instruction to be included in the try block.
	**/
	public void setExceptionHandler(
		int startInsNr,
		int endInsNr,
		int handlerInsNr,
		BT_Class catchType) throws BT_InstructionReferenceException {
		if (startInsNr >= endInsNr) {
			throw new IllegalArgumentException();
		}
		code.setExceptionHandler(startInsNr, endInsNr, handlerInsNr, catchType);
	}

	/**
	 Changes all references to one instructions into references to another.
	 If oldIns is a BT_BasicBlockMarker, then newIns must be a BT_BasicBlockMarker.
	 References to block markers can only be changed to references to other block markers.
	 @param switching true if either oldIns or newIns are not in this code attribute
	 */
	public void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching) {
		code.changeReferencesFromTo(oldIns, newIns, switching);
		attributes.changeReferencesFromTo(oldIns, newIns, switching);
	}
	
	/**
	 Changes all references to one instructions into references to another.
	 If oldIns is a BT_BasicBlockMarker, then newIns must be a BT_BasicBlockMarker.
	 References to block markers can only be changed to references to other block markers.
	 @param switching true if either oldIns or newIns are not in this code attribute
	 */
	public void changeOtherReferencesFromTo(BT_Ins oldIns, BT_Ins newIns, boolean switching, 
			int excludeHowMany, int excludeIndex) {
		code.changeOtherReferencesFromTo(oldIns, newIns, switching, excludeHowMany, excludeIndex);
		attributes.changeReferencesFromTo(oldIns, newIns, switching);
	}
	
	/**
	 Finds the given instruction and returns its successor.
	**/
	public int findInstruction(BT_Ins in1, int start) {
		return code.indexOf(in1, start);
	}
	
	/**
	 Finds the given instruction and returns its successor.
	**/
	public int findInstruction(BT_Ins in1) {
		return code.indexOf(in1, 0);
	}
	
	/**
	 Finds the given instruction and returns its predecessor.
	**/
	public BT_Ins getPreviousInstruction(int iin) {
		return code.getPreviousInstruction(iin);
	}
	
	/**
	 Finds the given instruction and returns its predecessor.
	**/
	public BT_Ins getPreviousInstruction(BT_Ins in1) {
		return code.getPreviousInstruction(in1);
	}
	
	/**
	 Finds the given instruction and returns its successor.
	**/
	public BT_Ins getNextInstruction(int iin) {
		return code.getNextInstruction(iin);
	}

	/**
	 Finds the given instruction and returns its successor.
	**/
	public BT_Ins getNextInstruction(BT_Ins in1) {
		return code.getNextInstruction(in1);
	}
	
	/**
	 Returns the first instruction.
	**/
	public BT_Ins getFirstInstruction() {
		return code.getFirstInstruction();
	}
	
	/**
	 Returns the last instruction.
	**/
	public BT_Ins getLastInstruction() {
		return code.getLastInstruction();
	}

	/**
	 Removes the instruction from the instruction vector.
	 This method should be used rather than removing the instructions
	 from the instruction vector directly, since it also calls the
	 "remove" method of the instructions to back out of any
	 relationships the instructions have with other objects.
	 @param in1 the instruction to be removed
	**/
	public boolean removeInstruction(BT_Ins in1) {
		return code.removeInstruction(in1);
	}

	/**
	 Replaces an instruction with a new instruction.
	
	 The "remove" method is called on the replaced instruction so it
	 can back out of any relationships it has with other objects.
	
	 <p> The same instruction object should not be used to represent more
	 than one instruction;  use the clone() method to create a new
	 instruction object for each instruction.
	
	 <p> Related: {@link BT_Method#replaceInstructionWith}.
	**/
	public boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns) {
		return code.replaceInstructionWith(oldIns, newIns);
	}

	/**
	 Replaces an instruction with 2 new instructions.
	
	 The "remove" method is called on the replaced instruction so it
	 can back out of any relationships it has with other objects.
	
	 <p> The same instruction object should not be used to represent more
	 than one instruction; use the clone() method to create a new
	 instruction object for each instruction.
	**/
	public boolean replaceInstructionWith(BT_Ins oldIns, BT_Ins newIns1, BT_Ins newIns2) {
		return code.replaceInstructionWith(oldIns, newIns1, newIns2);
	}

	/**
	 Removes the instruction from the instruction vector.
	 This method should be used rather than removing the instruction
	 from the instruction vector directly, since it also calls the
	 "remove" method of the instructions to back out of any
	 relationships the instructions have with other objects.
	
	 @param n index in the instruction vector of the instruction
	   to be removed
	**/
	public boolean removeInstructionAt(int n) {
		return removeInstructionsAt(1, n);
	}
	
	/**
	 Removes instructions from the instruction vector.
	 This method should be used rather than removing the instructions from the
	 instruction vector directly, since it also calls the "remove" method of the
	 instructions to back out of any relationships the instructions have with
	 other objects.
	
	 @param  howMany  number of instructions to remove
	 @param  iin  index in the instruction vector of the first instruction to be
	   removed
	**/
	public boolean removeInstructionsAt(int howMany, int iin) {
		code.removeInstructionsAt(howMany, iin);
		return true;
	}

	public boolean replaceInstructionsAtWith(int howMany, int n, BT_Ins in1) {
		if(howMany == 1) {
			code.replaceInstructionsAtWith(n, in1);
			return true;
		}
		code.replaceInstructionsAtWith(howMany, n, new BT_Ins[] {in1});
		return true;
	}

	public boolean replaceInstructionsAtWith(int howMany, int n, BT_Ins in1, BT_Ins in2) {
		if(howMany == 1) {
			code.replaceInstructionsAtWith(n, in1, in2);
			return true;
		}
		code.replaceInstructionsAtWith(howMany, n, new BT_Ins[] {in1, in2});
		return true;
	}
	
	public boolean replaceInstructionsAtWith(int howMany, int n, BT_Ins in1, BT_Ins in2, BT_Ins in3) {
		return replaceInstructionsAtWith(howMany, n, new BT_Ins[] {in1, in2, in3});
	}
	
	/**
	 Replaces a range of instructions with 2 new instructions.
	
	 The "unlink" method is called on the replaced instructions so they
	 can back out of any relationships they have with other objects.
	 
	 The "link" method is called in the inserted instructions.
	 
	 <p> The same instruction object should not be used to represent more
	 than one instruction; use the clone() method to create a new
	 instruction object for each instruction.
	**/
	public boolean replaceInstructionsAtWith(int howMany, int n, BT_Ins instructions[]) {
		code.replaceInstructionsAtWith(howMany, n, instructions);
		return true;
	}
	
	/**
	 Removes all instructions from the instruction vector.
	 This method should be used rather than removing the instructions
	 from the instruction vector directly, since it also calls the
	 "remove" method of the instructions to back out of any
	 relationships the instructions have with other objects.
	**/
	public void removeAllInstructions() {
		//remove the attributes
		for(int i=attributes.size() - 1; i>=0; i--) {
			BT_Attribute att = attributes.elementAt(i);
			if(att instanceof BT_LocalVariableAttribute
					|| att instanceof BT_LineNumberAttribute) {
				attributes.removeElementAt(i);
			}
			//the stack map table attribute is reset by resetCachedCodeInfo
		}
		code.removeAllInstructions();
	}

	/**
	 Make room in the allocated and used locals for this code attribute. 
	 Instructions referring to local <tt>n</tt>
	 will refer to local <tt>n + inc</tt> after a call to this method.
	 This does not increment the use of the locals reserved for the parameters to 
	 the method this code belongs to. Very useful 
	 @param  inc  The increment.
	 @see #incrementLocalsAndParametersAccessWith
	**/
	public void incrementLocalsAccessWith(int inc) {
		BT_Method method = getMethod();
		incrementLocalsAndParamsAccessWith(inc, method.getSignature().getArgsSize() + (method.isStatic() ? 0 : 1));
	}

	/**
	 * @param inc
	 * @param start
	 */
	private void incrementLocalsAndParamsAccessWith(int inc, int start) {
		code.incrementLocalsAndParamsAccessWith(inc, start);
		BT_LocalVariableAttribute localVarAttributes[] = this.getLocalVarAttributes();
		for (int k=0; k<localVarAttributes.length; k++) {
			localVarAttributes[k].incrementLocalsAccessWith(inc, start, code.getLocals());
		}
	}

	/**
	 Make room in the allocated and used locals for this code attribute. 
	 Instructions referring to local <tt>n</tt>
	 will refer to local <tt>n + inc</tt> after a call to this method.
	 This will also not increment the use of the locals reserved for 
	 the parameters to the method this code belongs to. Useful when someone
	 wants to add a (synthetic) parameter to a method.
	 @param  inc  The increment.
	 @see incrementLocalsAccessWith
	**/
	void incrementLocalsAndParametersAccessWith(int inc) {
		incrementLocalsAndParamsAccessWith(inc, 0);
	}

	/**
	 Inserts instructions into the instruction vector.
	
	 The same instruction object should not be used to represent more
	 than one instruction; use the clone() method to create a new
	 instruction object for each instruction.
	
	 @param  n  The index that the first instruction is to be inserted before.
	**/
	public void insertInstructionsAt(BT_InsVector newIns, int n) {
		insertInstructionsAt(newIns.toArray(), n);
	}

	/**
	 Inserts instructions into the instruction vector.
	
	 The same instruction object should not be used to represent more
	 than one instruction; use the clone() method to create a new
	 instruction object for each instruction.
	**/
	public void insertInstructionsAt(BT_Ins newIns[], int n) {
		code.insertInstructionsAt(newIns, n);
	}
	
	/**
	 Inserts an instruction into the instruction vector and dereferences it.
	
	 The same instruction object should not be used to represent more than one
	 instruction (because an offset is stored in it).
	 Use the clone() method to create a new instruction object for each
	 similar instruction.
	
	 @param  n       The number of the instruction (not its byte
	                 offset) before which to insert.
	 @return Always true
	**/
	public void insertInstructionAt(BT_Ins in1, int n) {
		code.insertInstructionAt(in1, n);
	}

	/**
	 Inserts a new last instruction.
	**/
	public void insertInstruction(BT_Ins in1) {
		code.insertInstruction(in1);
	}

	public boolean inlineJsrs() throws BT_CodeException {
		return BT_Inliner.inlineJsrs(this);
	}
	
	public boolean inline(BT_CodeAttribute code, BT_Ins atInstruction) throws BT_CodeException {
		return inline(code, atInstruction, true, true);
	}
	
	/**
	 * @param code the code to inline
	 * @param atInstruction where to inline the given code into this code. This instruction will be replaced.  If this instruction is an
	 * 	invoke instruction, then the arguments to the invocation will be handled properly.
	 * @param alterTargetClass whether or not the target class can be altered
	 * @param provokeInitialization whether to provoke class initialization in the case of inlining a static method
	 * @return
	 * @throws BT_CodeException
	 */
	public boolean inline(
			BT_CodeAttribute code,
			BT_Ins atInstruction,
			boolean alterTargetClass,
			boolean provokeInitialization) throws BT_CodeException {
		return BT_Inliner.inline(code, this, atInstruction, alterTargetClass, provokeInitialization);
	}
	
	/**
	 Applies some local optimizations.
	 
	 @param strict Strict preservation of semantics with respect to bytecode verification
	**/
	public boolean optimize(boolean strict) throws BT_CodeException {
		BT_Method method = getMethod();
		if(method == null) {
			throw new IllegalStateException();
		}
		return code.optimize(method.cls.repository, strict);
	}
	
	/**
	 * Optimize, then run dead code elimination.
	 *
	 * @param strict This parameter is passed to BT_CodeAttribute.optimize. 
	 * When false dead code elimination is enabled.
	 */
	public boolean optimizeAndRemoveDeadCode(boolean strict) throws BT_CodeException {
		BT_Method method = getMethod();
		if(method == null) {
			throw new IllegalStateException();
		}
		return code.optimizeAndRemoveDeadCode(method.cls.repository, strict);
	}

	/**
	   Visits reachable code using visitor BT_CodeVisitor
		
		The visitReachableCode method will throw only the BT_CodePathException exception
	 	if codeVisitor is not a subclass of BT_CodeVisitor.  When the visitor is a subclass of BT_CodeVisitor
	 	then the visitor may cause other subclasses of BT_CodeException to be thrown by this method.
	 
	 **/
	public void visitReachableCode(BT_CodeVisitor codeVisitor) throws BT_CodeException {
		code.visitReachableCode(codeVisitor);
	}

	/**
	 Visits reachable code starting from a particular instruction.
	
	 The visitReachableCode method will throw only the BT_CodePathException exception
	 if codeVisitor is not a subclass of BT_CodeVisitor.  When the visitor is a subclass of BT_CodeVisitor
	 then the visitor may cause other subclasses of BT_CodeException to be thrown by this method.
	 
	 @param instr the instruction at which to start.
	 @param codeVisitor the code visitor object to use
	 @param prevIns the previous instruction visited (may be null if prevInstrIndex is ENTRY_POINT)
	 @param prevInstrIndex the index of the previous instruction (may be ENTRY_POINT)
	 @see com.ibm.jikesbt.BT_CodeVisitor
	**/
	public void visitReachableCode(BT_CodeVisitor codeVisitor, BT_Ins instr, BT_Ins prevIns, int prevInstrIndex)
			throws BT_CodeException {
		code.visitReachableCode(codeVisitor, instr, prevIns, prevInstrIndex);
	}
	
	/**
	 Visits reachable code within the given subroutine, starting from the given instruction.
	 Both the visitor and the subroutine will be updated as visiting takes place.
	
	 The visitReachableCode method will throw only the BT_CodePathException exception
	 if codeVisitor is not a subclass of BT_CodeVisitor.  When the visitor is a subclass of BT_CodeVisitor
	 then the visitor may cause other subclasses of BT_CodeException to be thrown by this method.
	 
	 @param instr the instruction at which to start.
	 @param codeVisitor the code visitor object to use
	 @param prevIns the previous instruction visited (may be null)
	 @param prevInstrIndex the index of the previous instruction
	 @param subroutine the subroutine within which the instruction lies
	 @throws BT_CodeException
	 @see com.ibm.jikesbt.BT_CodeVisitor
	**/
	public void visitReachableCode(
			BT_CodeVisitor codeVisitor, 
			BT_Ins instr, 
			BT_Ins prevIns,
			int prevInstrIndex,
			SubRoutine sub) throws BT_CodeException {
		code.visitReachableCode(codeVisitor, instr, prevIns, prevInstrIndex, sub);
	}

	public int computeMaxLocals() throws BT_CodeException {
		BT_MaxLocalsVisitor visitor = new BT_MaxLocalsVisitor();
		visitReachableCode(visitor);
		return visitor.getMaxLocals();
	}
	
	/**
	 Returns the maximum stack depth needed for this method.
	**/
	public int computeMaxStackDepth() throws BT_CodeException {
		BT_StackDepthVisitor visitor = new BT_StackDepthVisitor();
		visitReachableCode(visitor);
		return visitor.getMaxDepth();
	}
	
	/**
	 Returns the stack depth for this method just before a specific instruction.
	 @param instIndex The index of the instruction to calculate the stack depth for
	**/
	public int computeStackDepth(final int instIndex) 
			throws BT_CodeException {
		class DepthVisitor extends BT_StackDepthVisitor {
			int depth = -1; 
			
			protected boolean visit(
					BT_Ins instruction,
					int iin,
					BT_Ins previousInstruction,
					int prev_iin,
					BT_ExceptionTableEntry handler) throws BT_StackUnderflowException {
				if(super.visit(instruction, iin, previousInstruction, prev_iin, handler)) {
					if(iin == instIndex) {
						depth = stackDepth[iin];
						exit();
						return false;
					}
					return true;
				}
				return false;
			}
		}
		DepthVisitor visitor = new DepthVisitor();
		visitReachableCode(visitor);
		return visitor.depth;
	}
	
	public void resolve(BT_ConstantPool pool) throws BT_AttributeException, BT_ClassWriteException {
		pool.indexOfUtf8(getName());
		code.resolve(pool);
		
		//calculate the max stack and max locals and verify their values are not excessive
		int maxLocals = getMaxLocals();
		if(BT_Misc.overflowsUnsignedShort(maxLocals)) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, Messages.getString("JikesBT.{0}_count_too_large_109", "maxLocals"));
		}
		if(BT_Misc.overflowsUnsignedShort(getMaxStack())) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, Messages.getString("JikesBT.{0}_count_too_large_109", "maxStack"));
		}
		
		//first do some clean-up
		BT_LocalVariableAttribute atts[] = getLocalVarAttributes();
		for(int i=0; i<atts.length; i++) {
			atts[i].removeEmptyRanges(maxLocals);
		}
		
		
		//we resolve the stack map table attribute separately, 
		//because if we cannot resolve them then we must remove it properly
		//so that the max locals and max stack can be calculated anyway
		BT_AttributeVector skip = null;
		BT_StackMapAttribute att = getStackMaps();
		if(att != null) {
			try {
				att.resolve(pool);
				skip = new BT_AttributeVector(1);
				skip.addElement(att);
			} catch(BT_AttributeException e) {
				try {
					pool.getRepository().factory.noteAttributeWriteFailure(getMethod(), att, e);
				} finally {
					//we remove the invalid attribute
					removeStackMaps();
				}
			}
		} 
		attributes.resolve(getMethod(), pool, skip);
	}
		
	/**
	 Does verification of the code.
	 @throws BT_CodeException if errors are found.
	**/
	public void verify() throws BT_CodeException {
		BT_StackShapeVisitor visitor = new BT_StackShapeVisitor(this);
		visitor.ignoreUpcasts(false);
		visitor.useMergeCandidates(false); //no need to use merge candidates when verifying, in fact should not in order to be able to do more verification
		BT_StackShapes shapes = visitor.populate();
		if(shapes == null) {
			return;
		}
		shapes.verifyStacks();
		if(shapes.maxDepth != codeInfo.getMaxStack() || shapes.maxLocals != codeInfo.getMaxLocals()) {
			resetCachedCodeInfo();
		}
	}
	
 	/**
	 Does verification of the relationship of the code to various items
	 such as the return type, invoked methods and accessed fields. 
	 @param strict if true, verification will assume that other classes that are referenced will not change and are correct
 	 @throws BT_CodeException if errors are found.
	**/
	public void verifyRelationships(boolean strict) throws BT_CodeException {
		BT_Method method = getMethod();
		if(method == null) {
			throw new IllegalStateException();
		}
		code.verifyRelationships(method, strict);
	}
	
	public void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		int codeLen = computeInstructionSizes();
		dos.writeShort(pool.indexOfUtf8(ATTRIBUTE_NAME));
		WrittenAttributesLength wal;
		try {
			wal = attributes.writtenLength();
			int totalLength = 2 + 2 + // maxlocal + maxstack
				4 + codeLen + // the bytecodes
				2 + getExceptionTableEntryCount() * 8 + // exceptions
				wal.writtenLength;// attributes
			dos.writeInt(totalLength); 
			int maxStack = codeInfo.getMaxStack();
			dos.writeShort(maxStack);
			int maxLocals = codeInfo.getMaxLocals();
			dos.writeShort(maxLocals);
		} catch(BT_CodeException e) {
			IllegalStateException ise = new IllegalStateException("must call resolve first");
			throw ise;
		}
		code.write(dos, pool, codeLen);
		attributes.write(wal.count, dos, pool);
	}
	
	public void print(PrintStream ps, int printFlag) throws BT_CodeException {
		print(ps, printFlag, null);
	}
	
	/**
	 * Call this method instead of getMaxLocals if this code is likely to be changed.
	 * This will avoid the creation of stackmaps and other items in the process of 
	 * calculating max locals, and will get max locals only.
	 * @return
	 * @throws BT_CodeException
	 */
	public int getMaxLocalsQuickly() throws BT_CodeException {
		return codeInfo.getMaxLocalsQuickly();
	}
	
	/**
	 * Call this method instead of getMaxStack if this code is likely to be changed.
	 * This will avoid the creation of stackmaps and other items in the process of 
	 * calculating max stack depth.
	 * @return
	 * @throws BT_CodeException
	 */
	public int getMaxStackQuickly() throws BT_CodeException {
		return codeInfo.getMaxStackQuickly();
	}
	
	public int getMaxLocals() throws BT_CodeException {
		return codeInfo.getMaxLocals();
	}
	
	public int getMaxStack() throws BT_CodeException {
		return codeInfo.getMaxStack();
	}

	/**
	 Prints the instructions and side information.
	 Has a side-effect of resetting all instructions' offsets unless
	 {@link BT_Misc#PRINT_ZERO_OFFSETS} is specified.
	
	 @param  printFlag  Should be 0 or {@link BT_Misc#PRINT_ZERO_OFFSETS} that can be
	   used to make comparing files easier.
	   Other bits are ignored.
	   @throws BT_CodeException if the max stack or max locals are being printed and one or the other could not be calculated.
	**/
	public void print(PrintStream ps, int printFlag, BT_SourceFile source) throws BT_CodeException {
		
		if ((printFlag & BT_Misc.PRINT_IN_ASSEMBLER_MODE) == 0) {/* assembler does not support reading attributes */
			/* we combine the printing of stack maps with the printing of the code */
			BT_StackMapAttribute stackMaps = getStackMaps();
			
			if(stackMaps == null || !stackMaps.isPopulated()) {
				//super.print(ps, "\t");xx;
				ps.println("\t" + code.toString());
				code.print(ps, printFlag, source);
			} else {
				//super.print(ps, "\t");xx;
				ps.println("\t" + code.toString());
				ps.println("\tand " + stackMaps);
				try {
					stackMaps.printFrames(ps);
				} catch(BT_AttributeException e) {
					//TODO same as what we do in BT_StackMapAttribute.print when we catch this exception
				}
			}
			for (int i = 0; i < attributes.size(); i++) {
				BT_Attribute attr = attributes.elementAt(i);
				if(attr.equals(stackMaps)) {
					continue;
				}
				attr.print(ps, "\t");
			}
		} else {
			code.print(ps, printFlag, source);
		}
	}

	public BT_LocalVariableAttribute[] getLocalVarAttributes() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < attributes.size(); i++) {
			BT_Attribute attr = attributes.elementAt(i);
			if (attr instanceof BT_LocalVariableAttribute) {
				list.add(attr);
			}
		}
		return (BT_LocalVariableAttribute[]) list.toArray(new BT_LocalVariableAttribute[list.size()]);
	}
	
	public String getLocalName(BT_Ins ins, int localNumber) {
		BT_LocalVariableAttribute attributes[] = getLocalVarAttributes();
		if(attributes.length > 0) {
			this.computeInstructionSizes();
			for (int i = 0; i < attributes.length; i++) {
				BT_LocalVariableAttribute localVarAttr = attributes[i];
						
				//we search the dereferenced table
				if(localVarAttr.isLocalTable()) {
					BT_LocalVariableAttribute.LV[] localVariables =
						localVarAttr.localVariables;
					if (localVariables.length > 0) {
						for (int j = 0; j < localVariables.length; j++) {
							BT_LocalVariableAttribute.LV lv = localVariables[j];
							if (lv.isWithinRange(ins) && lv.getLocalIndex() == localNumber) {
								return(lv.nameS);
							}
						}
					}
				}
			}
		}
		return "";
	}

	/**
	 * print the source source line number of the instruction.
	 * when the original source can be found, we read the correct source line and print it too.
	 */
	public void printSourceLine(PrintStream ps, int lineNumber, BT_SourceFile source) {
		String line = source.getLine(lineNumber);
		if(line != null) {
			ps.println();
			ps.println("\t\t// " + source.name + ": " + lineNumber);
			ps.println("\t\t// "+ source.getLine(lineNumber));
		}
	}
	
	/**
	 * find the constructor invocation corresponding to the call to the super constructor or
	 * another constructor in the same class.
	 * @return the call site for the constructor invocation.  If this code attribute does not correspond
	 * to a constructor, or there was no corresponding constructor invocation found, then null is returned.
	 * @throws BT_CodeException
	 */
	public BT_MethodCallSite findConstructorInvocation() throws BT_CodeException {
		ChainedConstructorLocator locator = new ChainedConstructorLocator(this);
		BT_StackShapes shapes = locator.find();
		if(locator.siteInstructionIndex < 0) {
			return null;
		}
		shapes.returnStacks();
		return locator.site;
	}
	
	/**
	 * find the constructor invocation corresponding to the instruction at the given index.
	 * @param newInsIndex
	 * @return the call site for the constructor invocation.  If the instruction at the given index
	 * is not a new instruction, or there was no corresponding constructor invocation found, then null is returned.
	 * @throws BT_CodeException
	 */
	public BT_MethodCallSite findConstructorInvocation(int newInsIndex) throws BT_CodeException {
		BT_Ins instruction = code.getInstruction(newInsIndex);
		if(!instruction.isNewIns()) {
			return null;
		}
		BT_NewIns newIns = (BT_NewIns) instruction;
		return findConstructorInvocation(newInsIndex, newIns);
	}
	
	public BT_MethodCallSite findConstructorInvocation(BT_NewIns newIns) throws BT_CodeException {
		int index = code.indexOf(newIns, 0);
		if(index < 0) {
			return null;
		}
		return findConstructorInvocation(index, newIns);
	}
	
	private BT_MethodCallSite findConstructorInvocation(int newInsIndex, BT_NewIns newIns) throws BT_CodeException {
		if(newIns.isNewArrayIns()) {
			return null;
		}
		NewInsConstructorLocator locator = new NewInsConstructorLocator(newInsIndex, newIns, this);
		locator.findConstructor();
		return locator.site;
	}
	
	/**
	 * given a instruction target of a jsr, will find the instruction
	 * that officially starts the subroutine (the first one that is not a block marker).
	 * @param target
	 * @return
	 */
	BT_Ins getSubroutineStartInstruction(BT_Ins target) {
		return code.getSubroutineStartInstruction(target);
	}
	
	/**
	 * 
	 * @param ins
	 * @return either the line number in the source file for the given instruction or 0 if no such line
	 */
	public int findLineNumber(BT_Ins ins) {
		BT_LineNumberAttribute.PcRange result = null;
	    for (int i = 0; i < attributes.size(); i++) {
	      BT_Attribute attr = attributes.elementAt(i);
	      if(!(attr instanceof BT_LineNumberAttribute)) {
	      	continue;
	      }
	      BT_LineNumberAttribute lineAtt = (BT_LineNumberAttribute) attr;
	      BT_LineNumberAttribute.PcRange range = lineAtt.getEncompassingRange(ins);
	      if(range != null) {
	    	  if(result == null || lineAtt.compareRanges(range, result) > 0) {
	    		  result = range;
	    	  }
	      }
	    }
	    if(result == null) {
	    	return 0;
	    }
	    return result.lineNumber; 
	  }
	
	/**
	 Abbreviation of {@link BT_CodeAttribute#print(PrintStream,int) print(ps,0)}.
	**/
	public void print(PrintStream ps, BT_SourceFile source) throws BT_CodeException {
		print(ps, 0, source);
	}
	
	public void print(PrintStream ps) throws BT_CodeException {
		print(ps, 0, null);
	}
	
	public String useName() {
		if(getMethod() == null) {
			return getName();
		}
		return getMethod().useName() + ' ' + getName();
	}
	
	public String toString() {
		return code.toString() + '\n' + attributes.toString();
	}
	
	public BT_ClassVector getVerifierRequiredClasses(BT_StackPool pool) {
		BT_Method method = getMethod();
		try {
			/* we do not use merge candidates because we do not know what the stack maps look like,
			 * so we err on the conservative side (which means potentially more classes returned here) 
			 */
			BT_StackShapeVisitor visitor = new BT_StackShapeVisitor(this, pool);
			visitor.ignoreUpcasts(false);
			visitor.useMergeCandidates(false);
			BT_StackShapes shapes = visitor.populate();
			if(shapes == null) {
				return BT_ClassVector.emptyVector;
			}
			shapes.verifyStacks();
			BT_ClassVector result = getVerifierRequiredClasses(shapes.stackShapes);
			shapes.returnStacks();
			return result;
		} catch(BT_CodeException e) {
			//ignore code that is not formatted properly, otherwise we will need to be much more careful
			//when analyzing the code, since we make assumptions that the code is well-formed below (TODO: try not to make these assumptions)
			if(method != null) {
				method.cls.getRepository().factory.noteCodeException(e);
			}
			return BT_ClassVector.emptyVector;
		}
	}
	
	public BT_ClassVector getVerifierRequiredClasses(BT_StackCell shapes[][]) {
		BT_Method method = getMethod();
		if(method == null) {
			throw new IllegalStateException();
		}
		return code.getVerifierRequiredClasses(method, shapes);
	}
	
	public boolean wideningCastRequiresClassLoad(BT_Class stackType, BT_Class intendedType) {
		BT_Method method = getMethod();
		if(method == null) {
			throw new IllegalStateException();
		}
		return code.wideningCastRequiresClassLoad(method, stackType, intendedType);
	}
	
	public boolean isWideningCastTarget(BT_Class intendedType) {
		return code.isWideningCastTarget(intendedType);
	}
	
	SubRoutine createSubRoutine(BT_Ins startInstruction) {
		return code.createSubRoutine(startInstruction);
	}
	
	public String getName() {
		return ATTRIBUTE_NAME;
	}
}