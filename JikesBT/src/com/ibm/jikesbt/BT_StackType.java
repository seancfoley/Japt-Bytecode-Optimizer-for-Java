/*
 * Created on Aug 23, 2006
 *
 * 
 */
package com.ibm.jikesbt;

import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_BytecodeException.BT_InstructionReferenceException;

/**
The following diagram depicts the type scheme used to model local variables and operand stack items.
Types preceded by a '+' are represented by subclasses of BT_StackType
				|---+return address (for jsr/ret)
				|---+int
	 |--oneWord-|---+float
	 |			|---reference-----uninitialized--|--+uninitializedThis
	 |							|				 |--+uninitialized(offset)
	 |							|		
	 |							|-object----+java class hierarchy (including arrays and interfaces)----+null
+top--|		
	 |			|---+long
	 |--twoWord-|---+double
	 
This class also uses a ReturnAddress type but this type is not represented in stack maps
because the jsr and ret instructions are obsolete in java6, so there cannot be a stack map
representation of a method with the jsr and ret instructions.
*/
public abstract class BT_StackType implements BT_LocalCell, BT_StackCell, Cloneable {

	static final int ITEM_UNDEFINED = -1;
	static final int ITEM_TOP = 0;
	static final int ITEM_INTEGER = 1;
	static final int ITEM_FLOAT = 2;
	static final int ITEM_LONG = 4;
	static final int ITEM_DOUBLE = 3;
	static final int ITEM_NULL = 5;
	static final int ITEM_UNINITIALIZED_THIS = 6;
	static final int ITEM_OBJECT = 7;
	static final int ITEM_UNINITIALIZED = 8;
	
	public static final Top TOP = new Top();
	public static final ClassType NULL = new ClassType();
	public static final UninitializedThis UNINITIALIZED_THIS = new UninitializedThis();
	public static final UnknownType UNKNOWN_TYPE = new UnknownType();
	
	
	private static final BT_InsVector emptyVector = new BT_InsVector(0);
	
	
	
	
	/**
	 * this method returns whether the type is the return address type
	 * @return
	 */
	public boolean isReturnAddress() {
		return false;
	}
	
	/**
	 * Adds a return instruction that this type represents.
	 * @param returnInstructions
	 */
	public void addReturnInstructions(BT_InsVector returnInstructions) {}
	
	/**
	 * Gets the return instruction that this type represents.
	 */
	public BT_InsVector getReturnInstructions() {
		return emptyVector;
	}
	
	/**
	 * this method returns whether the type is an object according to the type hierarchy above, so
	 * it returns true if the type is an object type or the null type.  Uninitialized objects return false.
	 * According to the above hierarchy, the null type is a subtype of all object types.
	 * @return
	 */
	public boolean isObjectType() {
		return false;
	}
	
	/**
	 * this method return whether the type is an object type according to the type hierarchy above,
	 * and that object that object type has not been loaded, and so the true type is unknown.
	 * @return
	 */
	public boolean isStubObjectType() {
		return false;
	}
	
	/**
	 * this method return whether the type is an uninitialized object according to the type hierarchy above.
	 * @return
	 */
	public boolean isUninitializedObject() {
		return false;
	}
	
	/**
	 * this method return whether the type is an uninitialized "this" object according to the type hierarchy above.
	 * @return
	 */
	public boolean isUninitializedThis() {
		return false;
	}
	
	/**
	 * returns whether the type is the null type.
	 * @return
	 */
	public boolean isNull() {
		return false;
	}
	
	/**
	 * returns whether the type is the top type.
	 * @return
	 */
	public boolean isTop() {
		return false;
	}
	
	/**
	 * @return the result of: isObjectType() && !isNull()
	 */
	public boolean isNonNullObjectType() {
		return false;
	}
	
	/**
	 * @return whether the type is double or long, the two types that require two local or stack slots
	 */
	public boolean isTwoSlot() {
		return false;
	}
	
	/**
	 * Return the type contained within this cell
	 */
	public BT_StackType getCellType() {
		return this;
	}
	
	/**
	 * this method returns whether the type is a class type, which means it corresponds
	 * to one of the java primitive or class types: int, float, long, double, object, or null. 
	 * This means that the type is not return address, top or an uninitialized object.
	 * @return
	 */
	public boolean isClassType() {
		/* this method is overidden in ClassType and should not be overridden in any other subclass */
		return false;
	}
	
	/**
	 * Return this type as a class type.  
	 * If this object is not a class type, then a ClassCastException is thrown to the caller.  
	 * Callers to this methods should have called isClassType() (alternatively isNonNullObjectType(), isNull(), or isObjectType()) 
	 * or should have verified this type object previously to ensure that it is indeed a ClassType.
	 */
	public ClassType getClassType() {
		/* this method is overidden in ClassType and should not be overridden in any other subclass */
		throw new ClassCastException();
	}
	
	/**
	 * Return this type as a class type.  
	 * If this object is not a class type, then a ClassCastException is thrown to the caller.  
	 * Callers to this methods should have called isStubObjectType()
	 * or should have verified this type object previously to ensure that it is indeed a ClassType.
	 */
	public StubType getStubType() {
		/* this method is overidden in StubType and should not be overridden in any other subclass */
		throw new ClassCastException();
	}
	
	/**
	 * for a BT_StackType, for two types to be equal that means that in a stack they represent the same type, and
	 * additionally all other characteristics remain the same.
	 */
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof BT_StackType) {
			BT_StackType other = (BT_StackType) o;
			return isSameType(other);
		}
		return false;
	}
	
	/**
	 * Returns whether the other type represents the same type as this object.
	 */
	public abstract boolean isSameType(BT_StackType other);
	
	
	/**
	 * the unsigned byte value that represents the type in a class file
	 * @return
	 */
	abstract int getStackMapType();
	
	/**
	 * change any oldIns references to newIns references
	 * @param oldIns
	 * @param newIns
	 */
	void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {}
	
	/**
	 * link to instruction and class objects
	 * @param rep the repository that contains this stack type
	 * @param owner the attribute that contains this stack type
	 */
	void dereference(BT_Repository rep, BT_StackMapAttribute owner) throws BT_InstructionReferenceException {}
	
	/**
	 * resolve the needed constant pool items
	 * @param pool
	 */
	void resolve(BT_ConstantPool pool) {}

	/**
	 * write to the stream, as dictated by the java class file format
	 * @param dos
	 * @param pool
	 * @throws IOException
	 */
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		int typeType = getStackMapType();
		dos.writeByte(typeType);
	}
	
	/**
	 * the length of this type when written by the write method
	 * @return the length in bytes
	 */
	int getWrittenLength() {
		return 1;
	}
	
	public abstract String toString();
	
	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {
			return null;
		}
	}
	
	public static String toString(BT_LocalCell locals[], BT_StackCell stack[]) {
    	if(locals == null) {
    		if(stack == null) {
    			return "";
    		}
    		locals = BT_StackPool.emptyLocals;
        } else if(stack == null) {
    		stack = BT_StackPool.emptyStack;
        }
    	StringBuffer buffer = new StringBuffer();
    	String spaces = "                                                    ";
		String filler = "----------------------------------------------------";
        String tabs = "\t\t\t";
        int i=0;
        for(; i<locals.length; i++) {
        	BT_LocalCell type = locals[i];
        	String local;
        	if(type == null) {
        		local = "";
        	} else {
        		local = type.toString();
        	}
        	buffer.append(tabs);
        	buffer.append(" [");
        	buffer.append(toString(local, filler));
        	buffer.append(']');
        	if(i < stack.length) {
        		buffer.append(" [");
        		buffer.append(toString(stack[i].toString(), filler));
        		buffer.append("]\n");
        	} else {
        		buffer.append("\n");
        	}
        }
        for(; i<stack.length; i++) {
        	buffer.append(tabs);
        	buffer.append("  ");
        	buffer.append(spaces);
        	buffer.append("  [");
        	buffer.append(toString(stack[i].toString(), filler));
        	buffer.append("]\n");
        }
        return buffer.toString();
    }
    
    private static String toString(String stackElement, String spaces) {
    	if(stackElement.length() > spaces.length()) {
    		//String result = stackElement.substring(0, spaces.length());
    		String result = "..." + stackElement.substring((stackElement.length() - spaces.length()) + 3);
			return result;
		} else {
			return stackElement + spaces.substring(stackElement.length());
		}
    }
    
    static class UnknownType extends BT_StackType {
		int getStackMapType() {
			return BT_StackType.ITEM_UNDEFINED;
		}
		
		public String toString() {
			return "unknown";
		}
		
		public boolean equals(Object other) {
			return this == other || other instanceof UnknownType;
		}
		
		public boolean isSameType(BT_StackType other) {
			return equals(other);
		}
		
		public Object clone() {
			return this;
		}
	}
    
	/*
	 * The subtypes appear below.  Refer to the hierarchy diagram above for more information
	 * about these various types. 
	 */
    
    
	public static class Top extends BT_StackType {
		
		public boolean equals(Object o) {
			return this == o || o instanceof Top;
		}
		
		public boolean isSameType(BT_StackType other) {
			return equals(other);
		}
		
		public String toString() {
			return "TOP";
		}
		
		int getStackMapType() {
			return ITEM_TOP;
		}
		
		public boolean isTop() {
			return true;
		}
		
		public Object clone() {
			return this;
		}
	}
	
	public static class ClassType extends BT_StackType {
		ClassType convertToClass;
		BT_Class type;
		final int stackMapType;
		
		public ClassType() { //used to construct the "null" type
			type = null;
			this.convertToClass = this;
			stackMapType = ITEM_NULL;
		}
		
		public ClassType(BT_Class type, int stackMapType) {
			this.type = type;
			this.convertToClass = this;
			this.stackMapType = stackMapType;
		}
		
		public ClassType(BT_Class type, ClassType convertToClass) {
			this.type = type;
			this.convertToClass = convertToClass;
			this.stackMapType = ITEM_UNDEFINED;
		}
		
		public ClassType getClassType() {
			return this;
		}
		
		boolean isSameClassType(ClassType other) {
			if(isNull()) {
				return other.isNull();
			}
			return type.equals(other.type) && !other.isStubObjectType();
		}
		
		public boolean equals(Object o) {
			if(this == o) {
				return true;
			}
			if(o instanceof ClassType) {
				return isSameClassType((ClassType) o);
			}
			return false;
		}
		
		public boolean isSameType(BT_StackType o) {
			return equals(o);
		}
		
		public boolean isClassType() {
			return true;
		}
		
		public boolean isObjectType() {
			return isNull() || !type.isPrimitive();
		}
		
		public boolean isNonNullObjectType() {
			return !isNull() && !type.isPrimitive();
		}
		
		public boolean isNull() {
			return stackMapType == ITEM_NULL;
		}
		
		public String toString() {
			return isNull() ? "null" : type.name;
		}
		
		public boolean isTwoSlot() {
			return !isNull() && (type.getSizeForLocal() == 2);
		}
		
		int getStackMapType() {
			return stackMapType;
		}
		
		ClassType convert() {
			return convertToClass;
		}
		
		public BT_Class getType() {
			return type;
		}
		
		void resolve(BT_ConstantPool pool) {
			if(isNonNullObjectType()) {
				pool.indexOfClassRef(type);
			}
		}
	
		void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
			if(isNonNullObjectType()) {
				dos.writeByte(ITEM_OBJECT);
				dos.writeShort(pool.indexOfClassRef(type));
			} else {
				dos.writeByte(stackMapType);
			}
		}
		
		int getWrittenLength() {
			if(isNonNullObjectType()) {
				return 3;
			} else {
				return 1;
			}
		}
		
		public Object clone() {
			return this;
		}
		
	}
	
	/**
	 * Used for unknown type hierarchies due to classes not loaded.
	 * In such cases, we use java.lang.Object (or an array with java.lang.Object as element type)
	 * but the use of StubType indicates that in fact there may be a non-loaded intermediate
	 * type that would be the proper choice.
	 * @author sfoley
	 *
	 */
	public static class StubType extends ClassType {
		private BT_ClassVector stubs = new BT_ClassVector();
		
		public StubType(BT_Repository rep) {
			this(rep.findJavaLangObject());
		}
		
		public StubType(BT_Class clazz) {
			super(clazz, ITEM_OBJECT);
		}
		
		public StubType getStubType() {
			return this;
		}
		
		public boolean isStubObjectType() {
			return true;
		}
		
		public boolean equals(Object o) {
			if(this == o) {
				return true;
			}
			if(o instanceof StubType) {
				StubType other = (StubType) o;
				return stubs.hasSameClasses(other.stubs);
			}
			return false;
		}
		
		public boolean isSameType(BT_StackType other) {
			return other instanceof StubType && type.equals(((ClassType) other).type);
		}
		
		boolean isSameClassType(ClassType other) {
			return other instanceof StubType && type.equals(other.type);
		}
		
		public StubType addStub(BT_Class stub) {
			if(!stubs.contains(stub)) {
				stubs.addElement(stub);
			}
			return this;
		}
		
		public BT_ClassVector getStubs() {
			return stubs;
		}
		
		public StubType addStubs(BT_ClassVector stubs) {
			for(int i = 0; i < stubs.size(); i++) {
				addStub(stubs.elementAt(i));
			}
			return this;
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer(40);
			buf.append("Resolved to ").append(type.name).append(" due to missing class definition");
			if(stubs.size() > 1) {
				buf.append("s");
			}
			buf.append(": ");
			for(int i=0; i<stubs.size(); i++) {
				if(i > 0) {
					buf.append(", ");
				}
				buf.append(stubs.elementAt(i));
			}
			return buf.toString();
		}
		
		public Object clone() {
			StubType add = (StubType) super.clone();
			add.stubs = (BT_ClassVector) stubs.clone();
			return add;
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * This type does not appear in the hierarchy diagram.  It is used to denote
	 * an address stored on the stack by the jsr instruction.
	 */
	public static class ReturnAddress extends BT_StackType {
		/**
		 * return instructions represent the instructions immediately following the
		 * jsr instructions that have created this return address type, pushed on the
		 * top of the stack at the beginning of a subroutine, which may include several
		 * jsr instructions that jump to the same subroutine.
		 */
		BT_InsVector returnInstructions = new BT_InsVector();
		
		public ReturnAddress(BT_Ins returnInstruction) {
			returnInstructions.addElement(returnInstruction);
		}
		
		public ReturnAddress(ReturnAddress one, ReturnAddress two) {
			returnInstructions.addAll(one.returnInstructions);
			returnInstructions.addAllUnique(two.returnInstructions);
		}
		
		private void addReturnInstruction(BT_Ins returnInstruction) {
			if(!returnInstructions.contains(returnInstruction)) {
				returnInstructions.addElement(returnInstruction);
			}
		}
		
		public BT_InsVector getReturnInstructions() {
			return returnInstructions;
		}
		
		public void addReturnInstructions(BT_InsVector returnInstructions) {
			for(int i=0; i<returnInstructions.size(); i++) {
				addReturnInstruction(returnInstructions.elementAt(i));
			}
		}

		void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
			for(int i=0; i<returnInstructions.size(); i++) {
				BT_Ins ins = returnInstructions.elementAt(i);
				if(oldIns == ins) {
					returnInstructions.setElementAt(newIns, i);
				}
			}
		}
		
		public boolean equals(Object o) {
			if(this == o) {
				return true;
			}
			if(o instanceof ReturnAddress) {
				ReturnAddress other = (ReturnAddress) o;
				return returnInstructions.hasSameInstructions(other.returnInstructions);
			}
			return false;
		}
		
		public boolean isSameType(BT_StackType other) {
			return other instanceof ReturnAddress;
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer(40);
			if(returnInstructions.size() > 1) {
				buf.append("address of instructions: ");
			} else {
				buf.append("address of instruction: ");
			}
			for(int i=0; i<returnInstructions.size(); i++) {
				if(i > 0) {
					buf.append(", ");
				}
				buf.append(returnInstructions.elementAt(i));
			}
			return buf.toString();
		}
		
		int getStackMapType() {
			throw new IllegalStateException("stackmaps do not support subroutines");
		}
		
		public boolean isReturnAddress() {
			return true;
		}
		
		public Object clone() {
			ReturnAddress add = (ReturnAddress) super.clone();
			add.returnInstructions = (BT_InsVector) returnInstructions.clone();
			return add;
		}
	}
	
	public static class UninitializedThis extends BT_StackType {
		public UninitializedThis() {}
		
		public boolean equals(Object o) {
			return this == o || o instanceof UninitializedThis;
		}
		
		public boolean isSameType(BT_StackType other) {
			/* we do not consider an uninitialized type the same as another unless it was
			 * created by the same instruction.
			 */
			return equals(other);
		}
		
		public String toString() {
			return "uninit<this>";
		}
		
		int getStackMapType() {
			return ITEM_UNINITIALIZED_THIS;
		}
		
		public boolean isUninitializedObject() {
			return true;
		}
		
		public boolean isUninitializedThis() {
			return true;
		}
		
		public Object clone() {
			return this;
		}
	}
	
	public static class UninitializedObject extends BT_StackType {
		BT_NewIns creatingInstruction;
		
		UninitializedObject() {}
		
		public UninitializedObject(BT_NewIns creatingInstruction) {
			if(creatingInstruction == null) {
				throw new NullPointerException();
			}
			this.creatingInstruction = creatingInstruction;
		}
		
		public boolean equals(Object o) {
			if(this == o) {
				return true;
			}
			return (o instanceof UninitializedObject) 
				&& creatingInstruction == ((UninitializedObject) o).creatingInstruction;
		}
		
		public boolean isSameType(BT_StackType other) {
			/* we do not consider an uninitialized type the same as another unless it was
			 * created by the same instruction.
			 */
			return equals(other);
		}
		
		void changeReferencesFromTo(BT_Ins oldIns, BT_Ins newIns) {
			if(oldIns == creatingInstruction) {
				creatingInstruction = (BT_NewIns) newIns;
			}
		}
		
		public String toString() {
			return "uninit<" + creatingInstruction.target.name + '>';
		}
		
		int getStackMapType() {
			return ITEM_UNINITIALIZED;
		}
		
		void write(java.io.DataOutputStream dos, BT_ConstantPool pool)
			throws java.io.IOException {
			dos.writeByte(ITEM_UNINITIALIZED);
			dos.writeShort(creatingInstruction.byteIndex);
		}
		
		int getWrittenLength() {
			return 3;
		}
		
		public boolean isUninitializedObject() {
			return true;
		}
		
		public Object clone() {
			return super.clone();
		}
	}
	
	public static class StackFrameClassType extends ClassType {
 		String className;
 		
 		StackFrameClassType(String className) {
 			super(null, ITEM_OBJECT);
 			this.className = className;
 		}
 		
 		void dereference(BT_Repository rep, BT_StackMapAttribute owner) {
 			if(type == null) {
 				type = rep.linkTo(className);
 				className = null;
 			}
		}
 		
 		public boolean equals(Object o) {
 			if(type == null) {
 				return false;
 			}
 			return super.equals(o);
 		}
 		
 		public boolean isObjectType() {
			return true;
		}
		
 		public boolean isNonNullObjectType() {
			return true;
		}
		
 		public boolean isNull() {
			return false;
		}
		
		public String toString() {
			return (type == null) ? className : type.name;
		}
		
		public boolean isTwoSlot() {
			return false;
		}
 	}
 	
 	public static class StackFrameUninitializedOffset extends UninitializedObject {
 		int instructionOffset;
 		
 		StackFrameUninitializedOffset(int instructionOffset) {
 			this.instructionOffset = instructionOffset;
 		}
 		
 		void dereference(BT_Repository rep, BT_StackMapAttribute owner) 
 				throws BT_InstructionReferenceException {
 			if(creatingInstruction == null) {
 				BT_CodeAttribute container = owner.getCode();
	 			BT_Ins ins = container.getInstructions().findNonBlockInstruction(container, owner, instructionOffset);
	 			if(ins instanceof BT_NewIns) {
					creatingInstruction = (BT_NewIns) ins;
				} else {
					throw new BT_InstructionReferenceException(container, owner, instructionOffset, 
							"stack map frame references instruction that is not a new instruction");
				}
			}
		}
 		
 		public boolean equals(Object o) {
 			if(creatingInstruction == null) {
 				return false;
 			}
 			return super.equals(o);
		}
		
		public String toString() {
			if(creatingInstruction == null) {
				return "uninit<" + instructionOffset + '>';
			}
			return super.toString();
		}
		
 	}
}
