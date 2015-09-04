package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ClassProperties;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_Repository;

public class DefaultInstantiatorProvider implements InstantiatorProvider {
	ClassProperties properties;
	
	public DefaultInstantiatorProvider(ClassProperties properties) {
		this.properties = properties;
	}
	
	public Instantiator getInstantiator(BT_Class clazz) {
		if(properties.javaLangThread.isInstance(clazz)) {
			return new Instantiator() {
				public SpecificMethodInvocation create(Method method, int depth, CallingContext context) {
					Clazz declaringClass = method.getDeclaringClass();
					BT_Class tc = properties.javaLangThread;
					BT_Class dc = declaringClass.getUnderlyingType();
					if(tc.equals(dc)) {
						BT_Method meth = method.getMethod();
						BT_Repository repo = dc.getRepository();
						if(meth.getName().equals("start") && meth.getSignature().equals(repo.basicSignature)) {
							return new ThreadStartInvocation(method, depth, context);
						}
					}
					return Instantiator.DEFAULT_INSTANTIATOR.create(method, depth, context);
				}
			
				public ConstructedObject instantiate(Clazz type, AllocationContext context) {
					return new ThreadObject(type, context);
				}
				
				public GenericObject instantiateGeneric(Clazz type, AllocationContext context) {
					return new GenericThreadObject(type, context);
				}
			};
		}
		return Instantiator.DEFAULT_INSTANTIATOR;
	}
}
