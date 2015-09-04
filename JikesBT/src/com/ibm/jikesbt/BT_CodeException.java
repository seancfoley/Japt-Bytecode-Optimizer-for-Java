/*
 * Created on Sep 27, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

/**
 * 
 * @author sfoley
 *
 * Describes an error in the structure of the code in a BT_CodeAttribute.  This may entail
 * either the bytecodes or the exception table entries.  These errors would result in VerifyErrors
 * if the code were to be verified by a java virtual machine.
 */
public class BT_CodeException extends BT_AttributeException {
	public final BT_CodeAttribute code;
	public final BT_Ins instruction;
	public final int instructionIndex;
	
	private static final String INCOMPATIBLE_CLASS_CHANGE_ERROR = BT_Repository.JAVA_LANG_INCOMPATIBLE_CLASS_CHANGE_ERROR;
	private static final String VERIFY_ERROR = BT_Repository.JAVA_LANG_VERIFY_ERROR;
	private static final String ILLEGAL_ACCESS_ERROR = BT_Repository.JAVA_LANG_ILLEGAL_ACCESS_ERROR;
	private static final String NO_SUCH_METHOD_ERROR = BT_Repository.JAVA_LANG_NO_SUCH_METHOD_ERROR;
	private static final String INSTANTIATION_ERROR = BT_Repository.JAVA_LANG_INSTANTIATION_ERROR;
	private static final String ABSTRACT_METHOD_ERROR = BT_Repository.JAVA_LANG_ABSTRACT_METHOD_ERROR;
	
	//TODO internationalize strings in this class
	
	public BT_CodeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, String explanation) {
		super(BT_CodeAttribute.ATTRIBUTE_NAME, "the code " + ((code.getMethod() == null) ? "" : ("for method " + code.getMethod().useName())) + 
				" is invalid at instruction " + (instructionIndex + 1) + ", bytecode \"" + instruction 
				+ getLineString(code, instruction) + "\": " + explanation);
		this.code = code;
		this.instruction = instruction;
		this.instructionIndex = instructionIndex;
	}
	
	static String getLineString(BT_CodeAttribute code, BT_Ins ins) {
		int line = code.findLineNumber(ins);
		if(line == 0) {
			return "";
		}
		String sourceFile = code.getMethod().getDeclaringClass().getSourceFile();
		if(sourceFile == null) {
			return "";
		}
		return ", line number " + line + " in " + sourceFile;
	}
	
	/**
	 * Section 4.8.1 of the VM spec defines the static constraints, and these correspond to ClassFormatErrors.  
	 * Structural constraints in 4.8.2 are VerofyErrors.
	 * @return
	 */
	public String getCorrespondingRuntimeError() {
		/* most code exceptions are VerifyError */
		return VERIFY_ERROR;
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * Thrown when a successor of an instruction in the code does not exist.
	 * Either a jump instruction attempts to jump outside the code or
	 * the last instruction is not an throw, ret, goto or return instruction.
	 */
	public static class BT_CodePathException extends BT_CodeException {
		public BT_CodePathException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex) {
			super(code, instruction, instructionIndex, "code path jumps outside method");
		}
	}
	
	public static class BT_CircularJSRException extends BT_CodeException {
		public BT_CircularJSRException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex) {
			super(code, instruction, instructionIndex, "circular java subroutine");
		}
	}
	
	public static class BT_InconsistentStackDepthException extends BT_CodeException {
		public BT_InconsistentStackDepthException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex) {
			super(code, instruction, instructionIndex, "stack depth is inconsistent from different code paths");
		}
	}
	
	public static class BT_InconsistentStackTypeException extends BT_CodeException {
		public BT_InconsistentStackTypeException(
				BT_CodeAttribute code,
				BT_Ins instruction,
				int instructionIndex,
				BT_StackType type1, /* can be null */
				BT_StackType type2, /* can be null */
				int stackDepth) {
			super(code, instruction, instructionIndex, 
					"stack type is inconsistent from different code paths, cannot merge " + type1 + " with " + type2 + " at stack depth " + stackDepth);
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * The wrong type has been found in a stack location
	 */
	public static class BT_InvalidStackTypeException extends BT_CodeException {
		public BT_InvalidStackTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, String message) {
			super(code, instruction, instructionIndex, message);
		}
		
		public static class BT_UninitializedObjectTypeException extends BT_InvalidStackTypeException {
			public BT_UninitializedObjectTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_StackType invalidType, int stackDepth) {
				super(code, instruction, instructionIndex, "access to uninitialized type " + invalidType + " at stack depth " + stackDepth);
			}
			
			public BT_UninitializedObjectTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_Class expectedType, BT_StackType invalidType, int stackDepth) {
				super(code, instruction, instructionIndex, "access to uninitialized type " + invalidType + " rather than expected type " + expectedType + " at stack depth " + stackDepth);
			}
		}
		
		public static class BT_InvalidArgumentTypeException extends BT_InvalidStackTypeException {
			public BT_InvalidArgumentTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, 
					BT_Class paramType, BT_StackType invalidType, int stackDepth, BT_Method target) {
				super(code, instruction, instructionIndex, "invalid type " + invalidType + " for parameter of type " + paramType + " for invocation of " + target + " at stack depth " + stackDepth);
			}
			
			public BT_InvalidArgumentTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_StackType invalidType, BT_Field target) {
				super(code, instruction, instructionIndex, "invalid type " + invalidType + " on stack for store into field of type " + target.getFieldType());
			}
		}
		
		public static class BT_ExpectedPrimitiveTypeException extends BT_InvalidStackTypeException {
			public BT_ExpectedPrimitiveTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, 
					BT_Class expectedType, BT_StackType invalidType, int stackDepth) {
				super(code, instruction, instructionIndex, "expected " + expectedType + " but found " + invalidType + " at stack depth " + stackDepth);
			}
		}
		
		public static class BT_ExpectedUninitializedTypeException extends BT_InvalidStackTypeException {
			public BT_ExpectedUninitializedTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, 
					BT_Class expectedUninitializedType, BT_StackType invalidType, int stackDepth) {
				super(code, instruction, instructionIndex, "expected an uninitialized " + expectedUninitializedType + " but found " + invalidType + " at stack depth " + stackDepth);
			}
		}
		
		public static class BT_ExpectedArrayTypeException extends BT_InvalidStackTypeException {
			public BT_ExpectedArrayTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_StackType invalidType, int stackDepth) {
				super(code, instruction, instructionIndex, "expected an array object but found " + invalidType + " at stack depth " + stackDepth);
			}
		}
		
		public static class BT_ExpectedObjectTypeException extends BT_InvalidStackTypeException {
			public BT_ExpectedObjectTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_StackType invalidType, int stackDepth) {
				super(code, instruction, instructionIndex, "expected an object but found type " + invalidType + " at stack depth " + stackDepth);
			}
			
			public BT_ExpectedObjectTypeException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_Class expectedType, BT_StackType invalidType, int stackDepth) {
				super(code, instruction, instructionIndex, "expected an instance of " + expectedType + " but found " + invalidType + " at stack depth " + stackDepth);
			}
		}
		
		public static class BT_SplitDoubleWordException extends BT_InvalidStackTypeException {
			public BT_SplitDoubleWordException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_StackType splitType, int stackDepth) {
				super(code, instruction, instructionIndex, "cannot split type "  + splitType + " at stack depth " + stackDepth);
			}
		}
	}
	
	public static class BT_InvalidLoadException extends BT_CodeException {
		public BT_InvalidLoadException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_StackType type) {
			super(code, instruction, instructionIndex, "invalid type " + type + " in local variable for local variable load");
		}
	}
	
	public static class BT_InvalidStoreException extends BT_CodeException {
		public BT_InvalidStoreException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex, BT_StackType type) {
			super(code, instruction, instructionIndex, "invalid type " + type + " on stack for local variable store");
		}
	}
		
	public static class BT_StackUnderflowException extends BT_CodeException {
		public BT_StackUnderflowException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex) {
			super(code, instruction, instructionIndex, "stack underflow");
		}
	}
	
	public static class BT_StackOverflowException extends BT_CodeException {
		public BT_StackOverflowException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex) {
			super(code, instruction, instructionIndex, "stack overflow: max stack exceeded");
		}
	}
	
	public static class BT_LocalsOverflowException extends BT_CodeException {
		public BT_LocalsOverflowException(BT_CodeAttribute code, BT_Ins instruction, int instructionIndex) {
			super(code, instruction, instructionIndex, "max locals exceeded");
		}
	}
	
	public static class BT_UninitializedLocalException extends BT_CodeException {
		public BT_UninitializedLocalException(BT_CodeAttribute code, BT_LoadLocalIns loadIns, int instructionIndex) {
			super(code, loadIns, instructionIndex, "use of uninitialized local variable");
		}
	}
	
	public static class BT_IllegalInitException extends BT_CodeException {
		public BT_IllegalInitException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex) {
			super(code, ins, instructionIndex, "illegal invocation of constructor");
		}
	}
	
	public static class BT_IllegalClinitException extends BT_CodeException {
		public BT_IllegalClinitException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex) {
			super(code, ins, instructionIndex, "illegal invocation of class initializer");
		}
	}
	
	public static class BT_InvalidReturnException extends BT_CodeException {
		public BT_InvalidReturnException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex, BT_Class returnType) {
			super(code, ins, instructionIndex, "return instruction does not match method signature return type " + returnType.getName());
		}
	}
	
	public static class BT_AccessException extends BT_CodeException {
		public final BT_Member accessed;
		
		public BT_AccessException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex, BT_Member accessed) {
			super(code, ins, instructionIndex, "access to " + accessed.useName() + " prohibited by flags \"" + accessed.accessString() + "\"");
			this.accessed = accessed;
		}
		
		public String getCorrespondingRuntimeError() {
			return ILLEGAL_ACCESS_ERROR;
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * A method resolution resolves to an incompatible method.  Either it is static or not when it should be the opposite, or
	 * it resolves to a constructor in the wrong class.
	 */
	public static class BT_IncompatibleMethodException extends BT_CodeException {
		public BT_IncompatibleMethodException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex, BT_Method target) {
			super(code, ins, instructionIndex, "target method incompatible with method invocation");
		}
		
		public String getCorrespondingRuntimeError() {
			return INCOMPATIBLE_CLASS_CHANGE_ERROR;
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * A field resolution resolves to an incompatible field.  Either it is static or not when it should be the opposite.
	 */
	public static class BT_IncompatibleFieldException extends BT_CodeException {
		public BT_IncompatibleFieldException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex, BT_Field target) {
			super(code, ins, instructionIndex, "target field incompatible with field access");
		}
		
		public String getCorrespondingRuntimeError() {
			return INCOMPATIBLE_CLASS_CHANGE_ERROR;
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * A class resolution resolves to an incompatible class.  Either it is an interface or not when it should be the opposite.
	 */
	public static class BT_IncompatibleClassException extends BT_CodeException {
		public BT_IncompatibleClassException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex, BT_Class target) {
			super(code, ins, instructionIndex, "target class/interface incompatible with method/field access");
		}
		
		public String getCorrespondingRuntimeError() {
			return INCOMPATIBLE_CLASS_CHANGE_ERROR;
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * A method resolution resolves to an incompatible method.  It resolves to a constructor in the wrong class.
	 */
	public static class BT_MissingConstructorException extends BT_CodeException {
		public BT_MissingConstructorException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex, BT_Method target) {
			super(code, ins, instructionIndex, "target constructor missing");
		}
		
		public String getCorrespondingRuntimeError() {
			return NO_SUCH_METHOD_ERROR;
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * A method is resolved to an abstract method because no overriding method exists.
	 */
	public static class BT_AbstractMethodException extends BT_CodeException {
		public BT_AbstractMethodException(BT_CodeAttribute code, BT_Ins ins, int instructionIndex, BT_Method target) {
			super(code, ins, instructionIndex, "missing implementation of abstract method");
		}
		
		public String getCorrespondingRuntimeError() {
			return ABSTRACT_METHOD_ERROR;
		}
	}
	
	/**
	 * 
	 * @author sfoley
	 *
	 * A "new" instruction attempts to instantiate an abstract class or interface.
	 */
	public static class BT_AbstractInstantiationException extends BT_CodeException {
		public BT_AbstractInstantiationException(BT_CodeAttribute code, BT_NewIns ins, int instructionIndex, BT_Class target) {
			super(code, ins, instructionIndex, "cannot instantiate interface or abstract class " + target.useName());
		}
		
		public String getCorrespondingRuntimeError() {
			return INSTANTIATION_ERROR;
		}
	}
}
