package com.ibm.ive.tools.japt.reduction.ita;

import java.util.HashMap;
import java.util.List;

import com.ibm.ive.tools.japt.ConditionalInterfaceItemCollection;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.reduction.Counter;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.ParameterLocation;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

public class EntryPointInvoker {
	final JaptRepository repository;
	final PropagationProperties propagationProperties;
	final Repository rep;
	final CallingContext initialContext;
	final AllocationContext allContext;
	
	public EntryPointInvoker(Repository rep, JaptRepository repository, PropagationProperties propagationProperties) {
		this.repository = repository;
		this.propagationProperties = propagationProperties;
		this.rep = rep;
		ContextProvider provider = propagationProperties.provider;
		initialContext = provider.getInitialCallingContext();
		allContext = initialContext.getAllocationContext();
	}
	
	public boolean propagateToConditionalInterfaceElements(List conds, Counter counter) {
		int originalSize = conds.size();
		top:
		for(int i=0; i<conds.size(); i++) {
			ConditionalInterfaceItemCollection cond = 
				(ConditionalInterfaceItemCollection) conds.get(i);
			BT_Class items[] = cond.getConditionalClasses();
			for(int j=0; j<items.length; j++) {
				BT_Class item = items[j];
				Clazz clazz = rep.getClazz(item);
				if(clazz.isRequired()) {
					propagateInterfaceElements(cond, counter);
					conds.remove(i--);
					continue top;
				}
			}
			BT_Method meths[] = cond.getConditionalMethods();
			for(int j=0; j<meths.length; j++) {
				BT_Method m = meths[j];
				BT_Class clz = m.getDeclaringClass();;
				Clazz declaringClass = rep.getClazz(clz);
				Method method = declaringClass.getMethod(m);
				if(method.isRequired() || repository.methodFulfillsClassRequirements(m, false)) {
					//TODO make the fields and methods in the interface a target of the conditional method
					propagateInterfaceElements(cond, counter);
					conds.remove(i--);
					continue top;
				}
				
			}
			BT_Field fields[] = cond.getConditionalFields();
			for(int j=0; j<fields.length; j++) {
				BT_Field f = fields[j];
				BT_Class clz = f.getDeclaringClass();;
				Clazz declaringClass = rep.getClazz(clz);
				Field field = declaringClass.getField(f);
				if(field.isRequired()) {
					//TODO make the methods in the interface a target of the conditional field
					propagateInterfaceElements(cond, counter);
					conds.remove(i--);
					continue top;
				}
			}
		}
		return conds.size() < originalSize;
	}
	
	/**
	 * @param rep
	 * @param cond
	 */
	private void propagateInterfaceElements(ConditionalInterfaceItemCollection cond, Counter counter) {
		//xxx need to link the condition to the interface elements;
		BT_FieldVector fs = cond.getInterfaceFields();
		BT_MethodVector ms = cond.getInterfaceMethods();
		BT_ClassVector cls = cond.getInterfaceClasses();
		counter.methodCount += ms.size();
		counter.fieldCount += fs.size();
		counter.classCount += cls.size();
		propagateToInterfaceElements(fs, ms, cls);
	}

	/**
	 * @param rep
	 * @param fields
	 * @param methods
	 * @param classes
	 */
	public void propagateToInterfaceElements(BT_FieldVector fields, BT_MethodVector methods, BT_ClassVector classes) {
		
		HashMap instantiatedTypes = new HashMap();
		
		for(int i=0; i<fields.size(); i++) {
			BT_Field member = fields.elementAt(i);
			BT_Class clz = member.getDeclaringClass();
			Clazz declaringClass = rep.getClazz(clz);
			Field field = declaringClass.getField(member);
			
			declaringClass.enterVerifierRequiredClasses(initialContext);
			if(rep.entryPointLister != null && repository.isInternalClass(clz)) {
				rep.entryPointLister.foundEntryTo(member);
			}
			initialContext.enter(field.getDeclaringClass(), field);
			if(field.isStatic()) {
				StaticFieldInstance staticField = declaringClass.getStaticFieldInstance(member);
				propagateTypes(instantiatedTypes, field, staticField);
			}
			else {
				
				FieldObject object = (FieldObject) instantiatedTypes.get(clz);
				if(object == null) {
					//object = createObject(rep, instantiatedTypes, declaringClass);
					object = (FieldObject) declaringClass.instantiate(allContext);
					if(propagationProperties.isReachabilityAnalysis()) {
						declaringClass.shareMembers();
					}
					instantiatedTypes.put(clz, object);
				}
				FieldInstance fieldInstance = object.getFieldInstance(field);
				if(fieldInstance != null) {
					propagateTypes(instantiatedTypes, field, fieldInstance);
					if(!fieldInstance.hasPropagated(object)) {
						fieldInstance.addInstantiatedObject(object);
					}
				}
			}
		}
		
		for(int i=0; i<methods.size(); i++) {
			BT_Method member = methods.elementAt(i);
			BT_Class clz = member.getDeclaringClass();
			Clazz declaringClass = rep.getClazz(clz);
			Method method = declaringClass.getMethod(member);
			if(rep.entryPointLister != null && repository.isInternalClass(clz)) {
				rep.entryPointLister.foundEntryTo(member);
			}
			initialContext.enter(method.getDeclaringClass(), method);
			declaringClass.enterVerifierRequiredClasses(initialContext);
			if(method.isStatic()) {
				MethodInvocation staticMethod = declaringClass.getMethodInvocation(method, initialContext);
				//initialContext.enter(declaringClass, method);
				staticMethod.setAccessed();
				propagateArguments(instantiatedTypes, method, staticMethod);
			} else if(!method.isAbstract() && !method.isNative()) {
				PropagatedObject object = (PropagatedObject) instantiatedTypes.get(clz);
				if(object == null) {
					//object = createObject(rep, instantiatedTypes, declaringClass);
					object = declaringClass.instantiate(allContext);
					if(propagationProperties.isReachabilityAnalysis()) {
						declaringClass.shareMembers();
					}
					instantiatedTypes.put(clz, object);
				}
				MethodInvocation methodInstance = declaringClass.getMethodInvocation(method, initialContext);
				//initialContext.enter(declaringClass, method);
				methodInstance.setAccessed();	
				propagateArguments(instantiatedTypes, method, methodInstance);
				ReceivedObject obj;
				if(method.useIntraProceduralAnalysis()) {
					ParameterLocation thisObjLocation = rep.locationPool.getParamLocation(0);
					obj = new TargetedObject(object, thisObjLocation);
				} else {
					obj = object;
				}
				if(!methodInstance.hasPropagated(obj)) {
					methodInstance.addInstantiatedObject(obj);
				}
				
				
			} else {
				method.setRequired();
			}
			
			if(!method.isStatic()) {
				PropagatedObject object = (PropagatedObject) instantiatedTypes.get(clz);
				if(object == null) {
					object = declaringClass.instantiate(allContext);
					instantiatedTypes.put(clz, object);
				}
				
				ReceivedObject obj;
				if(method.useIntraProceduralAnalysis()) {
					obj = new TargetedObject(object, new ParameterLocation());
				} else {
					obj = object;
				}
				
				//activate the constructors, but not their arguments
				BT_MethodVector constructors = clz.getConstructors();
				for(int j=0; j<constructors.size(); j++) {
					BT_Method cons = constructors.elementAt(j);
					Method constructor = declaringClass.getMethod(cons);
					MethodInvocation consInstance = declaringClass.getMethodInvocation(constructor, initialContext);
					initialContext.enter(declaringClass, constructor);
					consInstance.setAccessed();
					//initialContext.enter(constructor.getDeclaringClass(), constructor);
					if(!consInstance.hasPropagated(obj)) {
						consInstance.addInstantiatedObject(obj);
					}
				}
			}
		}
		
		for(int i=0; i<classes.size(); i++) {
			BT_Class clz = classes.elementAt(i);
			Clazz clazz = rep.getClazz(clz);
			if(rep.entryPointLister != null && repository.isInternalClass(clz)) {
				rep.entryPointLister.foundEntryTo(clz);
			}
			initialContext.enter(clazz);
			clazz.enterVerifierRequiredClasses(initialContext);
			clazz.setInitialized();
		}
	}


	private void propagateTypes(HashMap instantiatedTypes, Field field, FieldInstance fieldPropagator) {
		initialContext.enter(field.getDeclaringClass(), field);
		fieldPropagator.setAccessed();
		BT_ClassVector unknowns = new BT_HashedClassVector();
		if(!field.getType().isPrimitive()) {
			unknowns.addUnique(field.getType().getUnderlyingType());
			//field.getType().addSubtypes(unknowns); //a field of type Object ens up propagating all types
		}
		
		for(int j=0; j<unknowns.size(); j++) {
			BT_Class creationType = unknowns.elementAt(j);
			Clazz clazz = rep.getClazz(creationType);
	
			PropagatedObject obj = (PropagatedObject) instantiatedTypes.get(creationType);
			if(obj == null) {
				//obj = createObject(rep, instantiatedTypes, clazz);
				obj = clazz.instantiate(allContext);
				instantiatedTypes.put(creationType, obj);
			}
			//in the case of Strings and other select objects, the instantiated
			//object might have already been propagated because only a single instance
			//is ever created
			if(!fieldPropagator.hasPropagated(obj)) {
				fieldPropagator.addInstantiatedObject(obj);
			}

		}
	}
	
	
	
	private void propagateArguments(HashMap instantiatedTypes, Method method, MethodInvocation methodPropagator) {
		Clazz params[] = method.typesPropagatable;
		for(int j=0; j<params.length; j++) {
			Clazz clazz = params[j];
			if(clazz == null) {
				continue;
			}
			BT_Class creationType = clazz.getUnderlyingType();
			PropagatedObject obj = (PropagatedObject) instantiatedTypes.get(creationType);
			if(obj == null) {
				//obj = createObject(rep, instantiatedTypes, clazz);
				obj = clazz.instantiate(allContext);
				if(propagationProperties.isReachabilityAnalysis()) {
					clazz.shareMembers();
				}
				instantiatedTypes.put(creationType, obj);
			}
			ReceivedObject object;
			if(method.useIntraProceduralAnalysis()) {
				object = new TargetedObject(obj, rep.locationPool.getParamLocation((method.isStatic() ? 0 : 1)+ j));
					//new ParameterLocation(method.getSignature(), j));
			} else {
				object = obj;
			}
			/* 
			 * In the case of Strings and other select objects, the instantiated
			 * object might have already been propagated because only a single instance
			 * is created and shared
			 */
			if(!methodPropagator.hasPropagated(object)) {
				methodPropagator.addInstantiatedObject(object);
			}
		}
	}

}
