package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.CallingContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.ConstructedObject;
import com.ibm.ive.tools.japt.reduction.ita.ContextProperties;
import com.ibm.ive.tools.japt.reduction.ita.DefaultInstantiatorProvider;
import com.ibm.ive.tools.japt.reduction.ita.GenericObject;
import com.ibm.ive.tools.japt.reduction.ita.Instantiator;
import com.ibm.ive.tools.japt.reduction.ita.InstantiatorProvider;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.SpecificMethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.ThreadStartInvocation;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Method;

public class RTSJInstantiatorProvider implements InstantiatorProvider {
	final TypeProperties props;
	final ContextProperties contextProperties;
	final DefaultInstantiatorProvider provider;
	final RTSJContextProvider contextProvider;
	
	public RTSJInstantiatorProvider(TypeProperties props, ContextProperties contextProperties, 
			com.ibm.ive.tools.japt.reduction.ClassProperties classProps,
			RTSJContextProvider contextProvider) {
		this.props = props;
		this.contextProperties = contextProperties;
		this.provider = new DefaultInstantiatorProvider(classProps);
		this.contextProvider = contextProvider;
	}

	public Instantiator getInstantiator(BT_Class clazz) {
		final BT_Class rt = props.classProperties.realtimeThread;
		final BT_Class nhrt = props.classProperties.noHeapRealtimeThread;
		final Instantiator defaultInstantiator = provider.getInstantiator(clazz);
		if(rt.isInstance(clazz)) {
			return new Instantiator() {
				public SpecificMethodInvocation create(Method method, int depth, CallingContext context) {
					Clazz declaringClass = method.getDeclaringClass();
					BT_Class dc = declaringClass.getUnderlyingType();
					if(rt.equals(dc)) {
						BT_Method meth = method.getMethod();
						if(meth.equals(props.classProperties.realtimeThreadStart)) {
							return new ThreadStartInvocation(method, depth, context);
						}
						if(meth.isConstructor()) {
							return new RealtimeThreadConstructor(method, depth, context, contextProvider, props);
						}
					} else if(nhrt.equals(dc)) {
						BT_Method meth = method.getMethod();
						if(meth.equals(props.classProperties.noHeapRealtimeThreadStart)) {
							return new ThreadStartInvocation(method, depth, context);
						}
						if(meth.isConstructor()) {
							return new RealtimeThreadConstructor(method, depth, context, contextProvider, props);
						}
					}
					return defaultInstantiator.create(method, depth, context);
				}
			
				public ConstructedObject instantiate(Clazz type, AllocationContext context) {
					return new RealtimeThreadObject(type, context, props);
				}
				
				public GenericObject instantiateGeneric(Clazz type, AllocationContext context) {
					return new GenericRealtimeThreadObject(type, context, props);
				}
			};
		}
		final BT_Class memClass = props.classProperties.memClass;
		if(memClass.isInstance(clazz)) {
			return new Instantiator() {
				public SpecificMethodInvocation create(Method method, int depth, CallingContext context) {
					Clazz declaringClass = method.getDeclaringClass();
					BT_Class dc = declaringClass.getUnderlyingType();
					if(memClass.equals(dc)) {
						BT_Method meth = method.getMethod();
						if(meth.equals(props.classProperties.memNewArray) 
								|| meth.equals(props.classProperties.memNewInstance) 
								|| meth.equals(props.classProperties.memNewInstanceCon)) {
							return new MemAreaInstantiator(method, depth, context, contextProvider, props);
						}
						
					}
					return defaultInstantiator.create(method, depth, context);
				}
			
				public ConstructedObject instantiate(Clazz type, AllocationContext context) {
					return defaultInstantiator.instantiate(type, context);
				}
				
				public GenericObject instantiateGeneric(Clazz type, AllocationContext context) {
					return defaultInstantiator.instantiateGeneric(type, context);
				}
			};
		}
		return defaultInstantiator;
	}
}
