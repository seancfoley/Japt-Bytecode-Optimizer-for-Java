package com.ibm.ive.tools.japt.out;

import com.ibm.ive.tools.japt.AccessorMethod;
import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.SyntheticClassPathEntry;
import com.ibm.jikesbt.BT_Attribute;
import com.ibm.jikesbt.BT_BasicBlockMarkerIns;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_DuplicateClassException;
import com.ibm.jikesbt.BT_ExceptionTableEntry;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_GenericAttribute;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_Opcodes;

public class AutoLoaderCreator implements BT_Opcodes {
	final JaptRepository repository;
	final BT_ClassVector classes = new BT_ClassVector();
	
	AutoLoaderCreator(JaptRepository repository) {
		this.repository = repository;
	}
	
	void addWrittenClass(BT_Class clz) {
		classes.addElement(clz);
	}
	
	private BT_ClassVersion getHighestVersion() {
		BT_ClassVersion result = classes.firstElement().getVersion();
		for(int i=classes.size() - 1; i>0; i--) {
			BT_ClassVersion version = classes.elementAt(i).getVersion();
			if(version.isHigherThan(result)) {
				result = version;
			}
		}
		return result;
	}
	
	public BT_Method createLoaderMethod(
			String className, 
			boolean removingStackmaps,
			BT_ClassVector excludeCandidates) throws InvalidIdentifierException, BT_DuplicateClassException {
		if(classes.size() == 0) {
			return null;
		}
		BT_Class loaderClass = repository.getClass(className);
		if(loaderClass != null && excludeCandidates != null && excludeCandidates.contains(loaderClass)) {
			loaderClass.remove();
			loaderClass = null;
		}
		BT_ClassVersion classVersion = getHighestVersion();
		if(loaderClass == null) {
			ClassPathEntry cpe = new SyntheticClassPathEntry("Loader");
			repository.appendInternalClassPathEntry(cpe);
			loaderClass = repository.createInternalClass(new Identifier(className), cpe, classVersion);
		} else {
			throw new BT_DuplicateClassException("Loader class already exists", loaderClass);
		}
		BT_Class voidClass = repository.getVoid();
		BT_MethodSignature sig = repository.basicSignature;
		String methodName = "load";
		BT_Method loaderMethod = loaderClass.findMethodOrNull(methodName, sig);
		String loaderMethodAttributeName = "LoaderMethod";
		boolean withStackMaps = !removingStackmaps && classVersion.shouldHaveStackMaps();
		if(loaderMethod == null) {
			loaderMethod = createLoadMethod(withStackMaps, loaderClass,
					voidClass, sig, loaderMethodAttributeName);
		} else {
			if(loaderMethod.isStatic() 
					&& loaderMethod.isPublic() 
					&& loaderMethod.isSynchronized() 
					&& loaderMethod.isSynthetic() 
					&& loaderMethod.getAttributes().contains(loaderMethodAttributeName)) {
				loaderMethod.makeCodeSimplyReturn();
			} else {
				methodName = AccessorMethod.makeUniqueName(loaderClass, methodName, sig);
				loaderMethod = createLoadMethod(withStackMaps, loaderClass,
						voidClass, sig, loaderMethodAttributeName);
			}
		}
		return loaderMethod;
	}
	
	private static String getFieldName(BT_Class clazz, String name) {
		BT_Field f = clazz.getFields().findField(name);
		while(f != null) {
			name += 'x';
			f = clazz.getFields().findField(name);
		}
		return name;
	}
	
	public void populateLoaderMethod(BT_Method loaderMethod, boolean withClassInitialize 
			/* TODO if withClassInitialize is ever false, we need to test whether this method produces good code 
			 * we would end up throwing out the exception handling in that case */) {
		BT_CodeAttribute code = loaderMethod.getCode();
		BT_Ins lastIns = code.getLastInstruction();
		if(!lastIns.isReturnIns()) {
			lastIns = BT_Ins.make(repository.getVoid().getOpcodeForReturn());
			code.insertInstruction(lastIns);
		}
		BT_BasicBlockMarkerIns block = BT_Ins.make();
		code.insertInstructionAt(block, code.getInstructionSize() - 1);
		BT_ClassVersion version = loaderMethod.getVersion();
		boolean useClassLDC = !withClassInitialize && version.canLDCClassObject();
		BT_BasicBlockMarkerIns handlerStartIns = null;
		if(!useClassLDC) {
			handlerStartIns = BT_Ins.make();
			BT_Ins gotoEndIns = BT_Ins.make(opc_goto, block);
			BT_BasicBlockMarkerIns handlerTargetIns = BT_Ins.make();
			BT_BasicBlockMarkerIns handlerEndIns = BT_Ins.make();
			BT_ExceptionTableEntry entry = new BT_ExceptionTableEntry(
				handlerStartIns, 
				handlerEndIns, 
				handlerTargetIns, 
				repository.forName("java.lang.ClassNotFoundException"), 
				code);
			BT_Ins astore1Ins = BT_Ins.make(opc_astore_1);
			BT_Class noClassDefFoundError = repository.forName("java.lang.NoClassDefFoundError");
			BT_Ins newIns = BT_Ins.make(opc_new, noClassDefFoundError);
			BT_Ins dupIns = BT_Ins.make(opc_dup);
			
			BT_ClassVector stringArgs = new BT_ClassVector(1);
			stringArgs.addElement(repository.findJavaLangString());
			BT_MethodSignature stringSig = BT_MethodSignature.create(repository.getVoid(), stringArgs, repository);
			
			
			BT_Class stringBuilder = repository.forName(version.getStringBuilderClass());
			BT_Ins stringBuilderNewIns = BT_Ins.make(opc_new, stringBuilder);
			BT_Ins stringBuilderDupIns = BT_Ins.make(opc_dup);
			BT_Ins ldcIns = BT_Ins.make(opc_ldc, "auto-loaded class missing: ", repository);
			BT_Method stringBuilderInit = stringBuilder.findMethodOrNull(BT_Method.INITIALIZER_NAME, stringSig);
			if(stringBuilderInit == null) {
				stringBuilderInit = stringBuilder.addStubMethod(BT_Method.INITIALIZER_NAME, stringSig);
			}
			BT_Ins constructStringBuilderIns = BT_Ins.make(opc_invokespecial, stringBuilderInit, stringBuilder);
			BT_Ins loadClassNameIns = BT_Ins.make(opc_aload_0);
			String appendMethodName = "append";
			BT_MethodSignature appendStringSig = BT_MethodSignature.create(stringBuilder, stringArgs, repository);
			BT_Method stringBuilderAppend = stringBuilder.findMethodOrNull(appendMethodName, appendStringSig);
			if(stringBuilderAppend == null) {
				stringBuilderAppend = stringBuilder.addStubMethod(appendMethodName, appendStringSig);
			}
			BT_Ins appendStringBuilderIns = BT_Ins.make(opc_invokevirtual, stringBuilderAppend, stringBuilder);
			String toStringMethodName = "toString";
			BT_MethodSignature toStringSig = BT_MethodSignature.create(repository.findJavaLangString(), BT_ClassVector.emptyVector, repository);
			BT_Method stringBuilderToString = stringBuilder.findMethodOrNull(toStringMethodName, toStringSig);
			if(stringBuilderToString == null) {
				stringBuilderToString = stringBuilder.addStubMethod(toStringMethodName, toStringSig);
			}
			BT_Ins toStringIns = BT_Ins.make(opc_invokevirtual, stringBuilderToString, stringBuilder);
			BT_Method noClassDefFoundErrorInit = noClassDefFoundError.findMethodOrNull(BT_Method.INITIALIZER_NAME, stringSig);
			if(noClassDefFoundErrorInit == null) {
				noClassDefFoundErrorInit = noClassDefFoundError.addStubMethod(BT_Method.INITIALIZER_NAME, stringSig);
			}
			BT_Ins invokeSpecialIns = BT_Ins.make(opc_invokespecial, noClassDefFoundErrorInit, noClassDefFoundError);
			BT_Ins handlerSequence[];
			BT_Ins athrowIns = BT_Ins.make(opc_athrow);
			if(loaderMethod.getVersion().hasThrowableInitCause()) {
				BT_Ins dupAgainIns = BT_Ins.make(opc_dup);
				BT_Ins astore2Ins = BT_Ins.make(opc_astore_2);
				BT_Ins aload1Ins = BT_Ins.make(opc_aload_1);
				
				String initCauseName = "initCause";
				BT_Class javaLangThrowable = repository.findJavaLangThrowable();
				BT_ClassVector javaLangThrowableArgs = new BT_ClassVector(1);
				javaLangThrowableArgs.addElement(javaLangThrowable);
				BT_MethodSignature initCauseSig = BT_MethodSignature.create(javaLangThrowable, javaLangThrowableArgs, repository);
				
				//ensure we have the right class hierarchy
				if(!noClassDefFoundError.isInstanceOf(javaLangThrowable)) {
					BT_Class linkageError = repository.forName("java.lang.LinkageError");
					if(!noClassDefFoundError.isInstanceOf(linkageError)) {
						noClassDefFoundError.setSuperClass(linkageError);
					}
					BT_Class error = repository.forName("java.lang.Error");
					if(!linkageError.isInstanceOf(error)) {
						linkageError.setSuperClass(error);
					}
					if(!error.isInstanceOf(javaLangThrowable)) {
						error.setSuperClass(javaLangThrowable);
					}
				}
				BT_Method initCauseMethod = noClassDefFoundError.findInheritedMethod(initCauseName, initCauseSig, true);
				if(initCauseMethod == null) {
					initCauseMethod = javaLangThrowable.addStubMethod(initCauseName, initCauseSig);
				}
				BT_Ins invokeVirtualIns = BT_Ins.make(opc_invokevirtual, initCauseMethod, noClassDefFoundError);
				BT_Ins popIns = BT_Ins.make(opc_pop);
				BT_Ins aload2Ins = BT_Ins.make(opc_aload_2);
				handlerSequence = new BT_Ins[] {
						handlerEndIns,
						gotoEndIns,
						handlerTargetIns,
						astore1Ins,
						newIns,
						dupIns,
						
						stringBuilderNewIns,
						stringBuilderDupIns,
						
						ldcIns,
						
						constructStringBuilderIns,
						loadClassNameIns,
						appendStringBuilderIns,
						toStringIns,
						
						invokeSpecialIns,
						dupAgainIns,
						astore2Ins,
						aload1Ins,
						invokeVirtualIns,
						popIns,
						aload2Ins,
						athrowIns
				};
			} else {
				handlerSequence = new BT_Ins[] {
						handlerEndIns,
						gotoEndIns,
						handlerTargetIns,
						astore1Ins,
						newIns,
						dupIns,
						ldcIns,
						invokeSpecialIns,
						athrowIns
				};
			}
			code.insertInstructionsAt(handlerSequence, 0);
			code.insertExceptionTableEntry(entry);
		}
		BT_Class forNameClass = null;
		BT_Method forNameMethod = null;
		if(!useClassLDC) {
			forNameClass = repository.findJavaLangClass();
			BT_ClassVector forNameArgs = new BT_ClassVector(1);
			forNameArgs.addElement(repository.findJavaLangString());
			BT_MethodSignature forNameSig = BT_MethodSignature.create(forNameClass, forNameArgs, repository);
			String forNameName = "forName";
			forNameMethod = forNameClass.findMethodOrNull(forNameName, forNameSig);
			if(forNameMethod == null){
				forNameMethod = BT_Method.createMethod(forNameClass, 
						(short) (BT_Method.PUBLIC | BT_Method.STATIC), forNameSig, forNameName);
				forNameMethod.setStub(true);
			}
		}
		for(int i=classes.size() - 1; i>=0; i--) {
			BT_Class toLoad = classes.elementAt(i);
			if(useClassLDC) {
				BT_Ins classLDC = BT_Ins.make(opc_ldc, toLoad);
				code.insertInstructionAt(classLDC, 0);
			} else {
				BT_Ins classNameLDC = BT_Ins.make(opc_ldc, toLoad.getName(), repository);
				BT_Ins classNameDupIns = BT_Ins.make(opc_dup);
				BT_Ins classNameStoreIns = BT_Ins.make(opc_astore_0);
				BT_Ins forNameCall = BT_Ins.make(opc_invokestatic, forNameMethod, forNameClass);
				BT_Ins pop = BT_Ins.make(opc_pop);
				code.insertInstructionsAt(new BT_Ins[] {
						classNameLDC, classNameDupIns, classNameStoreIns, forNameCall, pop}, 0);
			}
		}
		if(!useClassLDC) {
			BT_Ins nullIns = BT_Ins.make(opc_aconst_null);
			BT_Ins nullStoreIns = BT_Ins.make(opc_astore_0);
			code.insertInstructionsAt(new BT_Ins[] {nullIns, nullStoreIns, handlerStartIns}, 0);
		}
		
		BT_Class owningClass = loaderMethod.getDeclaringClass();
	   	String fieldName = getFieldName(owningClass, "loadFlag");
		BT_Field callFlag = BT_Field.createField(
				owningClass, (short) (BT_Field.PRIVATE | BT_Field.STATIC), 
				repository.getBoolean(), fieldName);
		BT_Ins getStaticIns = BT_Ins.make(opc_getstatic, callFlag, owningClass);
		BT_BasicBlockMarkerIns ifEqBlock = BT_Ins.make();
		BT_Ins ifEqIns = BT_Ins.make(opc_ifeq, ifEqBlock);
		BT_Ins returnIns = BT_Ins.make(repository.getVoid().getOpcodeForReturn());
		BT_Ins iconst = BT_Ins.make(opc_iconst_1);
		BT_Ins putStaticIns = BT_Ins.make(opc_putstatic, callFlag, owningClass);
		
		
		BT_Ins preamble[] = new BT_Ins[] {
				getStaticIns, ifEqIns, returnIns, ifEqBlock, iconst, putStaticIns};
		code.insertInstructionsAt(preamble, 0);
		
		BT_Class javaLangObject = repository.findJavaLangObject();
		if(javaLangObject.equals(owningClass.getSuperClass())) {
			//add a private default constructor if there is no default constructor
			BT_Method init = owningClass.createDefaultConstructor();
			if(init.getCode() == null) {
				init.makeCodeSimplyReturn();
				init.becomePrivate();
				
				BT_Method objectInit = javaLangObject.createDefaultConstructor();
				BT_Ins superInvokeIns = BT_Ins.make(opc_invokespecial, objectInit, javaLangObject);
				BT_Ins loadThisIns = BT_Ins.make(opc_aload_0);
				BT_CodeAttribute initCode = init.getCode();
				initCode.insertInstructionAt(superInvokeIns, 0);
				initCode.insertInstructionAt(loadThisIns, 0);
			}
		}
		
		//add a call to the loader from the static initializer
		BT_Method clinit = owningClass.createClassInitializer();
		BT_CodeAttribute clinitCode = clinit.getCode();
		if(clinitCode == null) {
			clinit.makeCodeSimplyReturn();
			clinitCode = clinit.getCode();
		}
		BT_Ins loaderCall = BT_Ins.make(opc_invokestatic, loaderMethod, owningClass);
		clinitCode.insertInstructionAt(loaderCall, 0);
	}

	private BT_Method createLoadMethod(
			boolean withStackMaps, 
			BT_Class loaderClass,
			BT_Class voidClass, 
			BT_MethodSignature sig,
			String loaderMethodAttributeName) {
		BT_Method method;
		method = BT_Method.createMethod(loaderClass, 
				(short) (BT_Item.PUBLIC | BT_Item.STATIC | BT_Item.SYNTHETIC | BT_Item.SYNCHRONIZED), sig, "load");
		BT_Attribute loaderMethodAttribute = new BT_GenericAttribute(loaderMethodAttributeName, new byte[0], method);
		method.attributes.addElement(loaderMethodAttribute);
		method.signalNewBody();
		BT_Ins ins[] = new BT_Ins[] {BT_Ins.make(voidClass.getOpcodeForReturn())};
		method.setCode(new BT_CodeAttribute(ins, withStackMaps));
		return method;
	}
}
