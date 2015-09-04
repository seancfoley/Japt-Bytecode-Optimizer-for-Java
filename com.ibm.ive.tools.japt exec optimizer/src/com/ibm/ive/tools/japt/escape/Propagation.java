package com.ibm.ive.tools.japt.escape;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.ive.tools.japt.reduction.ita.CallingContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.ContextProperties;
import com.ibm.ive.tools.japt.reduction.ita.GenericObject;
import com.ibm.ive.tools.japt.reduction.ita.LocationPool;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation;
import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator;
import com.ibm.ive.tools.japt.reduction.ita.ObjectSet;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.PropagationException;
import com.ibm.ive.tools.japt.reduction.ita.PropagationProperties;
import com.ibm.ive.tools.japt.reduction.ita.ReceivedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.SpecificMethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.TargetedObject;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Method;


public class Propagation {
	final BT_Method method;
	final Messages messages;
	final Logger logger;
	final JaptRepository japtRepository;
	final Component component;
	final PropagationProperties props;
	final ContextProperties contextProps;
	
	static class MethodPropagation {
		Repository rep;
		SpecificMethodInvocation methodInvocation;
		ObjectSet[] arguments; //each ObjectSet contains ReceivedObject objects
		ObjectSet thisArgument;
	}
	
	Propagation(BT_Method method, JaptRepository rep, Messages messages, Logger logger, Component component, 
			PropagationProperties props, ContextProperties contextProps) {
		this.method = method;
		this.japtRepository = rep;
		this.logger = logger;
		this.messages = messages;
		this.component = component;
		this.props = props;
		this.contextProps = contextProps;
	}
	
	public MethodPropagation propagate() throws PropagationException {
		Repository rep = new Repository(props, contextProps, japtRepository, messages, logger, component);
		MethodPropagation prop = getInvocation(rep);
		rep.addInterfaceMethod(prop.methodInvocation);
		/*
		 * Now we propagate objects from one member to the next until 
		 * everything that can be propagated has been propagated
		 */
		while(rep.doPropagation());
		return prop;
	}
	
	MethodPropagation getInvocation(Repository rep) {
		MethodPropagation prop = new MethodPropagation();
		prop.rep = rep;
		Clazz declaringClass = rep.getClazz(method.getDeclaringClass());
		Method meth = declaringClass.getMethod(method);
		CallingContext context = props.provider.getInitialCallingContext();
		
		
		SpecificMethodInvocation methodInvocation = 
			(SpecificMethodInvocation) declaringClass.getMethodInvocation(meth, context);
		prop.methodInvocation = methodInvocation;
		methodInvocation.setAccessed();
		context.enter(declaringClass, meth);
		
		//TODO use an invoker, although at this time it's a little complicated due to our generic object arguments
//		MethodInvokeFromVM invoke = new MethodInvokeFromVM(rep, 
//				context, 
//				meth,
//				declaringClass);
//		if(meth.isStatic()) {
//			invoke.invokeStaticMethod();
//		} else {
//			invoke.invokeInstanceMethod(obj, isGeneric)
//		}
		
		Clazz typesPropagatable[] = meth.typesPropagatable;
		
		ObjectSet[] arguments = new ObjectSet[typesPropagatable.length];
		prop.arguments = arguments;
		
		int thisArg;
		if(method.isStatic()) {
			thisArg = 0;
		} else {
			ObjectSet set = getArgument(methodInvocation, declaringClass, 0);
			prop.thisArgument = set;
			thisArg = 1;
		}
		for(int i=0; i<typesPropagatable.length; i++) {
			Clazz type = typesPropagatable[i];
			if(type == null) {
				continue;
			}
			ObjectSet set = getArgument(methodInvocation, type, i + thisArg);
			arguments[i] = set;
		}
		return prop;
	}
	
	ObjectSet getArgument(SpecificMethodInvocation methodInvocation, Clazz argType, int argLocation) {
		
		Repository repository = methodInvocation.getRepository();
		boolean intra = repository.getPropagationProperties().useIntraProceduralAnalysis();
		LocationPool pool = repository.locationPool;
		MethodInvocationLocation loc = intra ? pool.getParamLocation(argLocation) : null;
		ObjectSet set = new ObjectSet();
		
		
		if(repository.getPropagationProperties().useGenericObjects() 
				//&& !argType.isFinal() /* even if final, the object must have the property that all fields are prepopulated */
				) {
			GenericObject object = 
				argType.instantiateGeneric(repository.getPropagationProperties().provider.getGenericContext());
			argType.addCreated(object);
			ReceivedObject obj;
			if(intra) {
				obj = new TargetedObject(object, loc);
			} else {
				obj = object;
			}
			set.add(object);
			if(!methodInvocation.hasPropagated(obj)) {
				methodInvocation.addPropagatedObject(obj, ObjectPropagator.INVOCATION_ARGUMENT, null);
			}
		} else {
			Clazz all[] = getMatchingTypes(argType);
			for(int j=0; j<all.length; j++) {
				Clazz clazz = all[j];
				if(clazz.isAbstract() || clazz.isInterface()) {
					continue;
				}
				PropagatedObject object = clazz.instantiate(repository.getHeap());
				ReceivedObject obj;
				if(intra) {
					obj = new TargetedObject(object, loc);
				} else {
					obj = object;
				}
				set.add(object);
				if(!methodInvocation.hasPropagated(obj)) {
					methodInvocation.addPropagatedObject(obj, ObjectPropagator.INVOCATION_ARGUMENT, null);
				}
			}
		}
		return set;
	}
	
	public Clazz[] getMatchingTypes(Clazz clazz) {
		BT_ClassVector classes = new BT_HashedClassVector(10);
		addSubtypes(classes, clazz.getUnderlyingType());
		Clazz all[] = new Clazz[classes.size()];
		Repository rep = clazz.repository;
		for(int i=0; i<classes.size(); i++) {
			BT_Class claz = classes.elementAt(i);
			Clazz clz = rep.getClazz(claz);
			all[i] = clz;
		}
		return all;
	}
	
	public static void addSubtypes(BT_ClassVector types, BT_Class type) {
		types.addElement(type);
		BT_ClassVector kids = type.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Class kid = kids.elementAt(i);
			if(!types.contains(kid)) {
				addSubtypes(types, kid);	
			}
		}
	}
}
