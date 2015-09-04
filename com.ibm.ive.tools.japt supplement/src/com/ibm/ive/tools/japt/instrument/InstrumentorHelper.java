/*
 * Created on Oct 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.instrument;

import java.io.IOException;

import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.JaptClass;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.TransferredClassPathEntry;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Opcodes;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class InstrumentorHelper implements BT_Opcodes {

	final JaptClass classObserverType;
	final JaptClass objectObserverType;
	final JaptClass methodObserverType;
	final JaptClass specializedRuntimeObserverType;
	final BT_Class arrayMethodObserverType;
	final BT_Class javaLangObject;
	final BT_Method createClassObserverMethod;
	final BT_Method createObjectObserverMethod;
	final BT_Method createMethodObserverMethod;
	final BT_Method observeEntryMethod;
	BT_Method getRuntimeObserverMethod;
	
	public InstrumentorHelper(InstrumentExtension ie, JaptRepository rep, String runtimeObserverClassName, Logger logger, Messages messages) throws ExtensionException {
		javaLangObject = rep.findJavaLangObject();
		
		//the following classes cannot change shape because 
		//of references in this method:
		//DefaultObserver, RuntimeObserverHolder, RuntimeObserverCreator
		
		//Note: unfortunately, changing the classes/methods referred to in this method could possibly
		//break instrumentation without generating a compile time error
		TransferredClassPathEntry tcpe;
		try {
			tcpe = new TransferredClassPathEntry(new PatternString("com/ibm/ive/tools/japt/instrument/" + PatternString.wildCard));
		} catch(IOException e) { 
			throw new ExtensionException(ie, e.toString()); 
		}
		rep.appendInternalClassPathEntry(tcpe);
		String defaultObserverName = DefaultObserver.class.getName();
		if(runtimeObserverClassName == null) {
			runtimeObserverClassName = defaultObserverName;
		}
		specializedRuntimeObserverType = (JaptClass) rep.forName(runtimeObserverClassName);
		
		if(specializedRuntimeObserverType.isStub()) {
			messages.NOT_FOUND_RUNTIME_OBSERVER.log(logger, runtimeObserverClassName);
			throw new ExtensionException(ie);
		}
		
		//TODO if I could somehow get at RuntimeObserverHolder without referrring to the name, that would be nice
		BT_Class runtimeHolderType = rep.forName(RuntimeObserverHolder.class.getName());
		BT_Class runtimeObserverType = runtimeHolderType.getFields().elementAt(0).getFieldType();
		
		
		if(!runtimeObserverType.isInterfaceAncestorOf(specializedRuntimeObserverType) 
				|| !specializedRuntimeObserverType.isClass() 
				|| specializedRuntimeObserverType.isBasicTypeClass) {
			messages.INVALID_RUNTIME_OBSERVER.log(logger, new Object[] {runtimeObserverClassName, runtimeObserverType.getName()});
			throw new ExtensionException(ie);
		}
		BT_MethodVector holderMethods = runtimeHolderType.getMethods();
		for(int i=0; i<holderMethods.size(); i++) {
			BT_Method meth = holderMethods.elementAt(i);
			if(!meth.isConstructor()) {
				getRuntimeObserverMethod = meth;
			}
		}
		if(getRuntimeObserverMethod == null) {
			throw new NullPointerException();
		}
		BT_Class runtimeCreatorType = runtimeHolderType.getSuperClass();
					
		//we doctor the class runtimeHolderType so that the methods returning error message strings 
		//return internationalized strings from Japt
		
		//the alternative would be to add a ExternalMessages.properties to the instrumented app
		BT_MethodSignature simpleSig = BT_MethodSignature.create("java.lang.String", "()", rep);
		BT_Method method = runtimeCreatorType.findMethod("getClassNotFoundErrorMessage", simpleSig);
		method.setCode(new BT_CodeAttribute(new BT_Ins[] {BT_Ins.make(opc_ldc, (Object) messages.CLASS_NOT_FOUND, rep), BT_Ins.make(opc_areturn)}, method.getCode().hasStackMaps()));
		method = runtimeCreatorType.findMethod("getInstantiationErrorMessage", simpleSig);
		method.setCode(new BT_CodeAttribute(new BT_Ins[] {BT_Ins.make(opc_ldc, (Object) messages.COULD_NOT_INSTANTIATE, rep), BT_Ins.make(opc_areturn)}, method.getCode().hasStackMaps()));
		method = runtimeCreatorType.findMethod("getInvalidTypeErrorMessage", simpleSig);
		method.setCode(new BT_CodeAttribute(new BT_Ins[] {BT_Ins.make(opc_ldc, (Object) messages.INVALID_TYPE, rep), BT_Ins.make(opc_areturn)}, method.getCode().hasStackMaps()));
		method = runtimeCreatorType.findMethod("getLoadingErrorMessage", simpleSig);
		method.setCode(new BT_CodeAttribute(new BT_Ins[] {BT_Ins.make(opc_ldc, (Object) messages.ERROR_LOADING, rep), BT_Ins.make(opc_areturn)}, method.getCode().hasStackMaps()));
		
		//we doctor the class com.ibm.ive.tools.japt.instrument.DefaultObserver 
		//so that the methods returning strings return internationalized strings from Japt
		if(specializedRuntimeObserverType.getName().equals(defaultObserverName)) {
			method = specializedRuntimeObserverType.findMethod("getClassObserverString", simpleSig);
			method.setCode(new BT_CodeAttribute(new BT_Ins[] {BT_Ins.make(opc_ldc, (Object) messages.CREATED_CLASS_OBSERVER, rep), BT_Ins.make(opc_areturn)}, method.getCode().hasStackMaps()));
			method = specializedRuntimeObserverType.findMethod("getObjectObserverString", simpleSig);
			method.setCode(new BT_CodeAttribute(new BT_Ins[] {BT_Ins.make(opc_ldc, (Object) messages.CREATED_OBJECT_OBSERVER, rep), BT_Ins.make(opc_areturn)}, method.getCode().hasStackMaps()));
			method = specializedRuntimeObserverType.findMethod("getMethodEntryString", simpleSig);
			method.setCode(new BT_CodeAttribute(new BT_Ins[] {BT_Ins.make(opc_ldc, (Object) messages.ENTERED, rep), BT_Ins.make(opc_areturn)}, method.getCode().hasStackMaps()));
		}
		
		createClassObserverMethod = runtimeObserverType.getMethods().elementAt(0);
		classObserverType = (JaptClass) createClassObserverMethod.getSignature().returnType;
		createObjectObserverMethod = classObserverType.getMethods().elementAt(0);
		objectObserverType = (JaptClass) createObjectObserverMethod.getSignature().returnType;
		createMethodObserverMethod = objectObserverType.getMethods().elementAt(0);
		methodObserverType = (JaptClass) createMethodObserverMethod.getSignature().returnType;
		arrayMethodObserverType = rep.forName(methodObserverType.getName() + "[]");
		observeEntryMethod = methodObserverType.getMethods().elementAt(0);
		
	}

}
