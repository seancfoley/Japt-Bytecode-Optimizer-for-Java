/*
 * Created on Oct 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.instrument;

import java.util.HashMap;

import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Opcodes;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ClassInstrumentor implements BT_Opcodes {
	private BT_Class clazz;
	private boolean instrumentByObject;
	private InstrumentorHelper helper;
	private boolean previouslyInstrumented;
	private Logger logger;
	private Messages messages;
	
	/**
	 * 
	 */
	public ClassInstrumentor(
			InstrumentorHelper helper, 
			BT_Class clazz, 
			boolean instrumentByObject,
			Logger logger,
			Messages messages) {
		this.clazz = clazz;
		this.instrumentByObject = instrumentByObject;
		this.helper = helper;
		this.logger = logger;
		this.messages = messages;
	}
	
	void instrumentClass(BT_MethodVector methods) {
		BT_MethodVector staticMethods = new BT_MethodVector();
		BT_MethodVector instanceMethods = new BT_MethodVector();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(method.getCode() == null) {
				messages.NO_CODE_TO_INSTRUMENT.log(logger, method.useName());
			}
			else if(method.isStatic()) {
				staticMethods.addElement(method);
			}
			else {
				instanceMethods.addElement(method);
			}
		}
		if(staticMethods.size() > 0 || instanceMethods.size() > 0) {
			instrumentClass(staticMethods, instanceMethods);
		}
	}
	
	String getFieldName(String name) {
		BT_Field f = clazz.getFields().findField(name);
		while(f != null) {
			name += 'x';
			f = clazz.getFields().findField(name);
		}
		return name;
	}
	
	
	private void instrumentClass(
			BT_MethodVector staticMethods, 
			BT_MethodVector instanceMethods) {
		if(clazz.equals(helper.javaLangObject)) {
			//one of the reasons we do not instrument java.lang.Object is that it
			//is the only class whose constructors do not call a constructor in the
			//same class or a superclass, and we search for such callsites when instrumenting
			//constructors
			messages.CANNOT_INSTRUMENT.log(logger, new Object[] {clazz.fullKindName(), clazz});
			return;
		}
		if(previouslyInstrumented) {
			//should never reach here, we should only instrument a class once
			//messages.CANNOT_INSTRUMENT.log(logger, new Object[] {clazz.fullKindName(), clazz});
			return;
		}
		previouslyInstrumented = true;
		BT_Method classInitializer = clazz.createClassInitializer();
		BT_CodeAttribute classInitializerCode = classInitializer.getCode();
		int maxLocals;
		try {
			maxLocals = classInitializerCode.getMaxLocalsQuickly();
		} catch(BT_CodeException e) {
			//TODO a message that we cannot instrument this code because the code is invalid
			return;
		}
		BT_InsVector classInitIns = new BT_InsVector(100);
		
		if(instanceMethods.size() == 0) {
			instrumentByObject = false;
		}
		boolean instrumentStatic = (staticMethods.size() > 0) || !instrumentByObject; 
		BT_Field classObserverArrayHolderField = null;
		if(instrumentStatic) {
			classObserverArrayHolderField = BT_Field.createField(clazz, (short) (BT_Field.PRIVATE | BT_Field.STATIC | BT_Field.FINAL), helper.arrayMethodObserverType, getFieldName("_japt_method_observer_array"));
		}
		BT_Field classObserverField = null;
		BT_Field objectObserverArrayHolderField = null;
		if(instrumentByObject) {
			classObserverField = BT_Field.createField(clazz, (short) (BT_Field.PRIVATE | BT_Field.STATIC | BT_Field.FINAL), helper.classObserverType, getFieldName("_japt_class_observer"));
			objectObserverArrayHolderField = BT_Field.createField(clazz, (short) (BT_Field.PRIVATE | BT_Field.FINAL), helper.arrayMethodObserverType, getFieldName("_japt_instance_method_observer_array"));
		}
		
		classInitIns.addElement(BT_Ins.make(opc_ldc, (Object) helper.specializedRuntimeObserverType, clazz.getRepository()));
		classInitIns.addElement(BT_Ins.make(opc_invokestatic, helper.getRuntimeObserverMethod));
		classInitIns.addElement(BT_Ins.make(opc_ldc, (Object) clazz, clazz.getRepository()));
		classInitIns.addElement(BT_Ins.make(opc_invokeinterface, helper.createClassObserverMethod));
		if(instrumentByObject) {
			classInitIns.addElement(BT_Ins.make(opc_dup));
		}
		int classObserverLocalNum = (classInitializerCode == null) ? 0 : maxLocals;
		classInitIns.addElement(BT_Ins.make(opc_astore, classObserverLocalNum));
		BT_MethodVector methodsToInstrument = null;
		if(instrumentByObject) {
			classInitIns.addElement(BT_Ins.make(opc_putstatic, classObserverField));
			instrumentObjectObservers(classObserverField, objectObserverArrayHolderField, instanceMethods);
			methodsToInstrument = staticMethods;
		}
		else {
			methodsToInstrument = new BT_MethodVector(staticMethods.size() + instanceMethods.size());
			methodsToInstrument.addElements(staticMethods);
			methodsToInstrument.addElements(instanceMethods);
		}
		if(instrumentStatic) {
			int arrayLocalNum = classObserverLocalNum + 1;
			instrumentInitializerCreateArray(arrayLocalNum, 
					classInitIns, 
					classObserverArrayHolderField,
					instrumentByObject ? staticMethods.size() : staticMethods.size() + instanceMethods.size(),
					false);
			for(int i=0; i<methodsToInstrument.size(); i++) {
				BT_Method method = methodsToInstrument.elementAt(i);
				instrumentInitializerCreateMethodObserver(classInitIns, classObserverLocalNum, arrayLocalNum, i, method);
				instrumentMethodEnterObserver(classObserverArrayHolderField, i, method, false, 0);
			}
		}
		if(classInitializerCode == null) {
			classInitIns.addElement(BT_Ins.make(opc_return));
			classInitializerCode = new BT_CodeAttribute(classInitIns.toArray(), clazz.getVersion());
			classInitializer.setCode(classInitializerCode);
		}
		else {
			classInitializerCode.insertInstructionsAt(classInitIns, 0);
		}
	}
	
	private BT_MethodCallSite locateConstructorCall(BT_Method cons) throws BT_CodeException {
		BT_CodeAttribute constructorCode = cons.getCode();
		return constructorCode.findConstructorInvocation();
	}
	
	private void instrumentObjectObservers(BT_Field classObserverField, BT_Field objectObserverArrayHolderField, BT_MethodVector instanceMethods) {
		//messages.NO_CONSTRUCTOR_INVOCATION
		HashMap insertionIndexMap = new HashMap();
		BT_MethodVector potentialConstructors = clazz.getMethods();
		for(int i=potentialConstructors.size() - 1; i>=0; i--) {
			BT_Method cons = potentialConstructors.elementAt(i);
			if(!cons.isConstructor()) {
				continue;
			}
			BT_MethodCallSite site;
			try {
				site = locateConstructorCall(cons);
				if(site == null) {
					messages.NO_CONSTRUCTOR_INVOCATION.log(logger, cons.useName());
					return;
				}
			} catch(BT_CodeException e) {
				messages.NO_CONSTRUCTOR_INVOCATION.log(logger, cons.useName());
				return;
			}
			insertionIndexMap.put(cons, site);
		}
		for(int i=0; i<instanceMethods.size(); i++) {
			BT_Method method = instanceMethods.elementAt(i);
			int instrumentIndex = 0;
			if(method.isConstructor()) {
				//the verifier must ensure that the contructor calls another constructor in
				//the same class or the superclass constructor before any instance fields
				//in this class can be accessed.
				//Since the object's method observers are in an instance field, we must
				//access these observers afterwards
				BT_MethodCallSite site = (BT_MethodCallSite) insertionIndexMap.get(method);
				instrumentIndex = method.getCode().findInstruction(site.instruction) + 1;
			}
			instrumentMethodEnterObserver(objectObserverArrayHolderField, i, method, true, instrumentIndex);
		}
		for(int i=potentialConstructors.size() - 1; i>=0; i--) {
			BT_Method cons = potentialConstructors.elementAt(i);
			if(!cons.isConstructor()) {
				continue;
			}
			BT_MethodCallSite site = (BT_MethodCallSite) insertionIndexMap.get(cons);
			if(site.instruction.getTarget().cls.equals(clazz)) {
				//this constructor calls another constructor in the same class,
				//so we need not instrument it since the other constructor will be
				//instrumented
				continue;
			}
			BT_CodeAttribute constructorCode = cons.getCode();
			BT_InsVector constructorIns = new BT_InsVector(100);
			try {
				int objectObserverLocalNum = constructorCode.getMaxLocals();
				int arrayLocalNum = objectObserverLocalNum + 1;
				instrumentConstructor(constructorIns, 
						classObserverField, 
						objectObserverArrayHolderField, 
						instanceMethods,
						objectObserverLocalNum,
						arrayLocalNum);
				int instrumentIndex = constructorCode.findInstruction(site.instruction) + 1;
				constructorCode.insertInstructionsAt(constructorIns, instrumentIndex);
			} catch(BT_CodeException e) {
				//TODO a message that we cannot instrument this one because the code is invalid
			}
		}
	}
	
	private void instrumentConstructor(
			BT_InsVector ins,
			BT_Field classObserverField, 
			BT_Field objectObserverArrayHolderField, 
			BT_MethodVector instanceMethods,
			int objectObserverLocalNum,
			int arrayLocalNum) {
		ins.addElement(BT_Ins.make(opc_getstatic, classObserverField));
		ins.addElement(BT_Ins.make(opc_ldc, (Object) clazz, clazz.getRepository()));
		ins.addElement(BT_Ins.make(opc_invokeinterface, helper.createObjectObserverMethod));
		ins.addElement(BT_Ins.make(opc_astore, objectObserverLocalNum));
		instrumentInitializerCreateArray(arrayLocalNum, 
				ins, 
				objectObserverArrayHolderField,
				instanceMethods.size(),
				true);
		
		for(int i=0; i<instanceMethods.size(); i++) {
			BT_Method method = instanceMethods.elementAt(i);
			instrumentInitializerCreateMethodObserver(ins, objectObserverLocalNum, arrayLocalNum, i, method);
		}
	}
	
	private void instrumentInitializerCreateArray(
			int arrayLocalNum, 
			BT_InsVector initIns, 
			BT_Field arrayHolderField,
			int methodObserverArraySize,
			boolean isObjectArrayHolder) {
		if(isObjectArrayHolder) {
			initIns.addElement(BT_Ins.make(opc_aload_0));
		}
		initIns.addElement(BT_Ins.make(opc_ldc, methodObserverArraySize));
		initIns.addElement(BT_Ins.make(opc_anewarray, helper.methodObserverType));
		initIns.addElement(BT_Ins.make(opc_dup));
		initIns.addElement(BT_Ins.make(opc_astore, arrayLocalNum));
		if(isObjectArrayHolder) {
			initIns.addElement(BT_Ins.make(opc_putfield, arrayHolderField));
		}
		else {
			initIns.addElement(BT_Ins.make(opc_putstatic, arrayHolderField));
		}
	}

	private void instrumentInitializerCreateMethodObserver(BT_InsVector initIns, int objectObserverLocalNum, int arrayLocalNum, int arrayIndex, final BT_Method method) {
		initIns.addElement(BT_Ins.make(opc_aload, arrayLocalNum));
		initIns.addElement(BT_Ins.make(opc_ldc, arrayIndex));
		initIns.addElement(BT_Ins.make(opc_aload, objectObserverLocalNum));
		initIns.addElement(BT_Ins.make(opc_ldc, (Object) method.cls, method.cls.getRepository()));
		initIns.addElement(BT_Ins.make(opc_ldc, new Object() {
			public String toString() {
				return method.getName();
			}
		}, method.cls.getRepository()));
		initIns.addElement(BT_Ins.make(opc_ldc, (Object) method.getSignature(), method.cls.getRepository()));
		initIns.addElement(BT_Ins.make(opc_invokeinterface, helper.createMethodObserverMethod));
		initIns.addElement(BT_Ins.make(opc_aastore));
	}

	private void instrumentMethodEnterObserver(
			BT_Field arrayHolderField, 
			int arrayIndex, 
			BT_Method method,
			boolean isObjectArrayHolder,
			int instructionIndex) {
		
		//now instrument the method
		BT_CodeAttribute methodCode = method.getCode();
		//methodCode is not null because we already filtered out such methods
		BT_InsVector entryIns = new BT_InsVector();
		if(isObjectArrayHolder) {
			entryIns.addElement(BT_Ins.make(opc_aload_0));
			entryIns.addElement(BT_Ins.make(opc_getfield, arrayHolderField));
		}
		else {
			entryIns.addElement(BT_Ins.make(opc_getstatic, arrayHolderField));
		}
		entryIns.addElement(BT_Ins.make(opc_ldc, arrayIndex));
		entryIns.addElement(BT_Ins.make(opc_aaload));
		if (method.isConstructor() || method.isStatic()) {
			entryIns.addElement(BT_Ins.make(opc_aconst_null));
		} 
		else { // An instance method -- pass its Object
			entryIns.addElement(BT_Ins.make(opc_aload_0));
		}
		entryIns.addElement(BT_Ins.make(opc_invokeinterface, helper.observeEntryMethod));
		methodCode.insertInstructionsAt(entryIns, instructionIndex);
	}


}
