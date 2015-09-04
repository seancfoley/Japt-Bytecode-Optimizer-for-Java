/*
 * Created on Apr 11, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_LoadLocalIns;
import com.ibm.jikesbt.BT_LocalIns;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_Repository;

public class AccessorMethod {
	final BT_Member target;
	final BT_Class throughClass;
	public final BT_Method method;
	
	AccessorMethod(BT_Class declaringClass, BT_Method toMethod, BT_Class throughClass, boolean isSpecial) {
		method = createMethod(declaringClass, toMethod, throughClass, isSpecial);
		target = toMethod;
		this.throughClass = throughClass;
	}

	AccessorMethod(BT_Class inClass, BT_Field toField, BT_Class throughClass, boolean isGetter) {
		method = createMethod(inClass, toField, throughClass, isGetter);
		target = toField;
		this.throughClass = throughClass;
	}
	
	AccessorMethod(BT_Method method, BT_Member target, BT_Class throughClass) {
		this.method = method;
		this.target = target;
		this.throughClass = throughClass;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer("Accessor ");
		buffer.append(method.useName());
		if(isFieldGetter()) {
			buffer.append(" for read access to ");
		} else if(isFieldSetter()) {
			buffer.append(" for write access to ");
		} else if(isSpecialInvocation()) {
			buffer.append(" for special invocation of ");
		} else {
			buffer.append(" for invocation of ");
		}
		buffer.append(throughClass.useName());
		buffer.append('.');
		buffer.append(target.qualifiedName());
		return buffer.toString();
	}

	/**
	 * if the given method is an accessor method, returns an AccessorMethod object 
	 * for that method.  Otherwise returns null.
	 * @param method
	 * @return
	 */
	static AccessorMethod getAccessor(BT_Method method) {
		if(!method.isSynthetic()) {
			return null;
		}
		BT_CodeAttribute code = method.getCode();
		if(code == null) {
			return null;
		}
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		if(calledMethods.size() > 1) {
			return null;
		}
		BT_AccessorVector accessedFields = code.accessedFields;
		if(calledMethods.size() == 1) {
			if(accessedFields.size() != 0) {
				return null;
			}
			return getMethodAccessor(method, calledMethods.elementAt(0));
		} 
		if(accessedFields.size() != 1) {
			return null;
		}
		return getFieldAccessor(method, accessedFields.elementAt(0));
	}
	
	static AccessorMethod getFieldAccessor(BT_Method method, BT_Accessor accessor) {
		BT_Field targetField = accessor.getTarget();
		BT_Class throughClass = accessor.getClassTarget();
		BT_Repository rep = method.getDeclaringClass().getRepository();
		BT_CodeAttribute code = accessor.from;
		BT_InsVector instructions = code.getInstructions();
		if(instructions.size() < 2) {
			return null;
		}
		BT_Ins secondLastInstruction = instructions.elementAt(instructions.size() - 2);
		if(secondLastInstruction != accessor.getInstruction()) {
			return null;
		}
		BT_Ins lastInstruction = instructions.lastElement();
		if(!lastInstruction.isReturnIns()) {
			return null;
		}
		int returnOpcode = lastInstruction.opcode;
		BT_Ins firstInstruction = instructions.firstElement();
		BT_Class fieldType = targetField.getFieldType();
		BT_MethodSignature sig = method.getSignature();
		if(returnOpcode == fieldType.getOpcodeForReturn()) {
			//check for a getter
			if(targetField.isStatic()) {
				//check for a getStatic
				if(!method.isStatic()) {
					return null;
				}
				if(firstInstruction != secondLastInstruction) {
					return null;
				}
				if(firstInstruction.opcode != BT_Ins.opc_getstatic) {
					return null;
				}
				if(!sig.returnType.equals(fieldType) || sig.types.size() != 0) {
					return null;
				}
				return new AccessorMethod(method, targetField, throughClass);
			} else {
				//check for a getField
				if(method.isStatic()) {
					return null;
				}
				BT_Ins secondInstruction = instructions.elementAt(1);
				if(secondInstruction != secondLastInstruction) {
					return null;
				}
				if(secondInstruction.opcode != BT_Ins.opc_getfield) {
					return null;
				}
				if(BT_LocalIns.getBaseOpcode(firstInstruction.opcode) != targetField.getDeclaringClass().getOpcodeForLoadLocal()) {
					return null;
				}
				BT_LoadLocalIns loadLocalIns = (BT_LoadLocalIns) firstInstruction;
				if(loadLocalIns.target.localNr != 0) {
					return null;
				}
				if(!sig.returnType.equals(fieldType) || sig.types.size() != 0) {
					return null;
				}
				return new AccessorMethod(method, targetField, throughClass);
			}
		} else if(returnOpcode == targetField.getDeclaringClass().getRepository().getVoid().getOpcodeForReturn()) {
			BT_Ins secondInstruction = instructions.elementAt(1);
			//check for a setter
			if(targetField.isStatic()) {
				//check for a putstatic
				if(!method.isStatic()) {
					return null;
				}
				if(secondInstruction != secondLastInstruction) {
					return null;
				}
				if(secondInstruction.opcode != BT_Ins.opc_putstatic) {
					return null;
				}
				if(BT_LocalIns.getBaseOpcode(firstInstruction.opcode) != fieldType.getOpcodeForLoadLocal()) {
					return null;
				}
				BT_LoadLocalIns localIns = (BT_LoadLocalIns) firstInstruction;
				if(localIns.target.localNr != 0) {
					return null;
				}
				if(!sig.returnType.equals(rep.getVoid()) || sig.types.size() != 1 || !sig.types.elementAt(0).equals(fieldType)) {
					return null;
				}
				return new AccessorMethod(method, targetField, throughClass);
			} else {
				//check for a putfield
				if(method.isStatic()) {
					return null;
				}
				BT_Ins thirdInstruction = instructions.elementAt(2);
				if(thirdInstruction != secondLastInstruction) {
					return null;
				}
				if(thirdInstruction.opcode != BT_Ins.opc_putfield) {
					return null;
				}
				if(BT_LocalIns.getBaseOpcode(secondInstruction.opcode) != fieldType.getOpcodeForLoadLocal()) {
					return null;
				}
				BT_LoadLocalIns localIns = (BT_LoadLocalIns) secondInstruction;
				if(localIns.target.localNr != 1) {
					return null;
				}
				if(BT_LocalIns.getBaseOpcode(firstInstruction.opcode) != targetField.getDeclaringClass().getOpcodeForLoadLocal()) {
					return null;
				}
				localIns = (BT_LoadLocalIns) firstInstruction;
				if(localIns.target.localNr != 0) {
					return null;
				}
				if(!sig.returnType.equals(rep.getVoid()) || sig.types.size() != 1 || !sig.types.elementAt(0).equals(fieldType)) {
					return null;
				}
				return new AccessorMethod(method, targetField, throughClass);
			}
		}
		return null;
		
	}
	
	static AccessorMethod getMethodAccessor(BT_Method method, BT_MethodCallSite accessor) {
		if(!method.isPublic()) {
			return null;
		}
		BT_Method target = accessor.getTarget();
		boolean isStatic = target.isStatic();
		if(isStatic) {
			if(!method.isStatic()) {
				return null;
			}
		} else {
			if(method.isStatic()) {
				return null;
			}
		}
		BT_MethodSignature sig = method.getSignature();
		BT_MethodSignature targetSignature = target.getSignature();
		if(!sig.equals(targetSignature)) {
			return null;
		}
		
		BT_Class throughClass = accessor.getClassTarget();
		BT_CodeAttribute code = accessor.from;
		BT_InsVector instructions = code.getInstructions();
		if(instructions.size() < 2) {
			return null;
		}
		BT_Ins secondLastInstruction = instructions.elementAt(instructions.size() - 2);
		if(secondLastInstruction != accessor.getInstruction()) {
			return null;
		}
		BT_Ins lastInstruction = instructions.lastElement();
		if(!lastInstruction.isReturnIns()) {
			return null;
		}
		int returnOpcode = lastInstruction.opcode;
		if(returnOpcode != sig.returnType.getOpcodeForReturn()) {
			return null;
		}
		
		BT_Ins firstInstruction = instructions.firstElement();
		int sigSize = sig.types.size();
		int virtualSigSize;
		int i;
		if(isStatic) {
			i = 0;
			virtualSigSize = sigSize;
		} else {
			if(BT_LocalIns.getBaseOpcode(firstInstruction.opcode) != target.getDeclaringClass().getOpcodeForLoadLocal()) {
				return null;
			}
			i = 1;
			virtualSigSize = sigSize + 1;
		}
		if(virtualSigSize != instructions.size() - 2) {
			return null;
		}
		
		for(int j=0; i<virtualSigSize; i++,j++) {
			BT_Ins instruction = instructions.elementAt(i);
			BT_Class type = sig.types.elementAt(j);
			if(BT_LocalIns.getBaseOpcode(instruction.opcode) != type.getOpcodeForLoadLocal()) {
				return null;
			}
		}
		return new AccessorMethod(method, target, throughClass);
	}

	public BT_Member getTarget() {
		return target;
	}
	
	public boolean removeIfUnused() {
		if(method.callSites.size() == 0) {
			remove();
			return true;
		}
		return false;
	}
	
	/**
	 * @param declaringClass
	 * @param toMethod
	 * @param isSuper
	 */
	private static BT_Method createMethod(BT_Class declaringClass, BT_Method toMethod, BT_Class throughClass, boolean isSpecial) {
		BT_MethodSignature signature = toMethod.getSignature();
		String name = getMethodAccessorName(toMethod, signature, throughClass, isSpecial);
		short accessFlags = BT_Method.PUBLIC | BT_Method.SYNTHETIC;
		if(toMethod.isStatic()) {
			if(isSpecial) {
				throw new IllegalArgumentException();
			}
			accessFlags |= BT_Method.STATIC;
		}
		BT_Method method = BT_Method.createMethod(declaringClass, accessFlags, signature, name);
		method.replaceBodyWithMethodCall(toMethod, throughClass, isSpecial);
		return method;
	}
	
	/**
	 * @param clazz
	 * @param toField
	 * @param isGetter
	 */
	private static BT_Method createMethod(BT_Class inClass, BT_Field toField, BT_Class throughClass, boolean isGetter) {
		BT_Repository rep = inClass.getRepository();
		BT_Method method;
		if(isGetter) {
			BT_MethodSignature signature = BT_MethodSignature.create(
					toField.getFieldType(),
					BT_ClassVector.emptyVector,
					rep);
			method = createFieldAccessorMethod(inClass, toField, throughClass, signature, isGetter);
			method.replaceBodyWithGetField(toField, throughClass);
		} else {
			BT_MethodSignature signature = BT_MethodSignature.create(
					rep.getVoid(), 
					new BT_ClassVector(new BT_Class[] {toField.getFieldType()}), 
					rep);
			method = createFieldAccessorMethod(inClass, toField, throughClass, signature, isGetter);
			method.replaceBodyWithSetField(toField, throughClass);
		}
		return method;
	}
	
	private static String makeUniqueNameInternal(BT_Class inClass, String name, BT_MethodSignature signature) {
		return makeUniqueName(inClass, name, signature);
	}
	
	public static String makeUniqueName(BT_Class inClass, String name, BT_MethodSignature signature) {
		while (inClass.findInheritedMethod(name, signature) != null) {
			name += '_';
		}
		return name;
	}
	
	private static String getMethodAccessorName(BT_Method method, BT_MethodSignature signature, BT_Class throughClass, boolean isSpecial) {
		StringBuffer ret = getPrefix(method, throughClass);
		if(method.isConstructor()) {
			ret.append("init$");
		} else {
			if(isSpecial) {
				ret.append("spec$");
			}
			ret.append(method.getName());
		}
		String name = ret.toString();
		return makeUniqueNameInternal(throughClass, name, signature);
	}
	
	private static StringBuffer getPrefix(BT_Member member, BT_Class throughClass) {
		StringBuffer ret = new StringBuffer();
		String className = throughClass.getName().replace('.', '$');
		ret.append(className);
		ret.append('$');
		return ret;
	}
	
	private static String getFieldAccessorName(BT_Field field, BT_MethodSignature signature, BT_Class throughClass, boolean isGetter) {
		StringBuffer ret = getPrefix(field, throughClass);
		ret.append(isGetter ? 'g' : 's');
		ret.append('$');
		ret.append(field.getName());
		String name = ret.toString();
		return makeUniqueNameInternal(throughClass, name, signature);
	}
	
	private static BT_Method createFieldAccessorMethod(BT_Class inClass, BT_Field toField, BT_Class throughClass, BT_MethodSignature signature, boolean isGetter) {
		String name = getFieldAccessorName(toField, signature, throughClass, isGetter);
		short accessFlags = BT_Method.PUBLIC | BT_Method.SYNTHETIC;
		if(toField.isStatic()) {
			accessFlags |= BT_Method.STATIC;
		}
		return BT_Method.createMethod(inClass, accessFlags, signature, name);
	}
	
	public boolean invokesSpecial(BT_Method target, BT_Class throughClass) {
		return target.equals(this.target)
			&& throughClass.equals(this.throughClass)
			&& isSpecialInvocation();
	}
	
	public boolean invokes(BT_Method target, BT_Class throughClass) {
		return target.equals(this.target)
			&& throughClass.equals(this.throughClass)
			&& isRegularInvocation();
	}
	
	public boolean sets(BT_Field target, BT_Class throughClass) {
		return target.equals(this.target)
			&& throughClass.equals(this.throughClass)
			&& isFieldSetter();
	}
	
	public boolean gets(BT_Field target, BT_Class throughClass) {
		return target.equals(this.target)
			&& throughClass.equals(this.throughClass)
			&& isFieldGetter();
	}
	
	private boolean isFieldGetter() {
		BT_AccessorVector accessedFields = method.getCode().accessedFields;
		if(accessedFields.size() > 0) {
			BT_Accessor accessor = accessedFields.firstElement();
			return accessor.instruction.isFieldReadIns();
		}
		return false;
	}
	
	private boolean isFieldSetter() {
		BT_AccessorVector accessedFields = method.getCode().accessedFields;
		if(accessedFields.size() > 0) {
			BT_Accessor accessor = accessedFields.firstElement();
			return accessor.instruction.isFieldWriteIns();
		}
		return false;
	}
	
	private boolean isSpecialInvocation() {
		BT_MethodCallSiteVector calledMethods = method.getCode().calledMethods;
		if(calledMethods.size() > 0) {
			BT_MethodCallSite site = calledMethods.firstElement();
			return site.instruction.isInvokeSpecialIns();
		}
		return false;
	}
	
	private boolean isRegularInvocation() {
		BT_MethodCallSiteVector calledMethods = method.getCode().calledMethods; 
		if(calledMethods.size() > 0) {
			/* access to constructors and private methods is also by invokespecial.  
			 * 
			 * For others, a regular invocation is by invokevirtual, invokeinterface, or invokestatic.
			 */
			BT_MethodCallSite site = calledMethods.firstElement();
			BT_Method target = site.getTarget();
			if(target.isConstructor() || target.isPrivate() || target.isStatic()) {
				return true;
			}
			return !site.instruction.isInvokeSpecialIns();
		}
		return false;
	}
	
	public boolean isUnused() {
		return method.callSites == null || method.callSites.size() == 0;
	}
	
	public void remove() {
		method.remove();
		BT_Class declaringClass = method.getDeclaringClass();
		JaptRepository repository = (JaptRepository) declaringClass.getRepository();
		AccessorMethodGenerator gen = repository.getAccessorMethodGenerator(declaringClass);
		gen.removeAccessor(this);
	}
}
