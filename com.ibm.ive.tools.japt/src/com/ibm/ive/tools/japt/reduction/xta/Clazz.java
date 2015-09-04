package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.jikesbt.*;

/**
 * @author sfoley
 *
 */
public class Clazz {

	/**
	 * this class is required (ie it is acessed or referenced or used or needed in some way).
	 */
	protected boolean isRequired;
	
	/**
	 * this class is initialized (hence it is also required) 
	 */
	protected boolean isInitialized;
	
	/**
	 * this class is instantiated (hence it is also initialized)
	 */
	protected boolean isInstantiated;
	
	protected BT_Class clazz;
	
	/**
	 * If this class represents an array class, then this is the element propagator
	 */
	protected ArrayElement arrayElement;
	
	protected Repository repository;
	
	private boolean includedConstructors;
	
	
	protected Clazz(Repository r, BT_Class clazz) {
		this.clazz = clazz;
		this.repository = r;
	}
	
	public boolean isRequired() {
		return isRequired;
	}
	
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public boolean isInstantiated() {
		return isInstantiated;
	}
	
	BT_Class getBTClass() {
		return clazz;
	}
	
//	public void trace(String msg) {
//		if(	false
//				|| toString().indexOf("ProxyObjectCache") != -1
//				
//				) {
//			System.out.println("setting required: " + toString());
//		}
//		System.out.println("remove me");
//	}
		
	public void setRequired() {
		if(isRequired) {
			return;
		}
		isRequired = true;
		
		//trace("set required");
		
		if(clazz.isArray()) {
			repository.getClazz(clazz.arrayType).setRequired();
		}	
		
		BT_ClassVector parents = clazz.getParents();
		for(int i=0; i<parents.size(); i++) {
			BT_Class c = parents.elementAt(i);
			Clazz parent = repository.getClazz(c);
			parent.setRequired();
		}
	}
	
	public void setInitialized() {
		if(isInitialized) {
			return;
		}
		isRequired = isInitialized = true;
		includeImplicitMethods(false);
		if(clazz.isArray()) {
			repository.getClazz(clazz.arrayType).setInitialized();
		} else {
			//verifyMethods();		
		}
		BT_ClassVector parents = clazz.getParents();
		for(int i=0; i<parents.size(); i++) {
			Clazz parent = repository.getClazz(parents.elementAt(i));
			parent.setInitialized();
		}
	}

//	private void verifyMethods() {
//		if(!clazz.isInterface()){
//			BT_MethodVector methods = clazz.getMethods();
//			for(int k=0; k<methods.size(); k++) {
//				BT_Method method = methods.elementAt(k);
//				if(!method.isStub() && !method.isAbstract() && !method.isNative()) {
//					Method meth = repository.getMethod(method);
//					meth.findVerifierRequiredClasses(method.getCode());
//				}
//			}
//		}
//	}
	
	public void setInstantiated() {
		if(isInstantiated) {
			return;
		}
		//boolean wasInitialized = isInitialized;
		isRequired = isInitialized = isInstantiated = true;
		includeImplicitMethods(true);
		if(clazz.isArray()) {
			repository.getClazz(clazz.arrayType).setRequired();
		} else {
//			if(!wasInitialized) {
//				verifyMethods();		
//			}
		}
		BT_ClassVector parents = clazz.getParents();
		for(int i=0; i<parents.size(); i++) {
			BT_Class p = parents.elementAt(i);
			Clazz parent = repository.getClazz(p);
			if(p.isInterface()) {
				parent.setRequired();
			}
			else {	
				parent.setInstantiated();
			}
		}
	}
	
	private void includeImplicitMethods(boolean isInstanceAccess) {
		BT_MethodVector methods = clazz.getMethods();
		for(int k=0; k<methods.size(); k++) {
			BT_Method method = methods.elementAt(k);
			if(method.isStaticInitializer() || (isInstanceAccess && method.isFinalizer())) {
				repository.getMethod(method).setAccessed();
			}
		}
	}
	
	
	/**
	 * it is known that an object of this class type has been instantiated.  Therefore,
	 * if there is only a single constructor we know it must have been called at some point.
	 * Similarly, if one of the constructors is called by all the other constructors, then
	 * we know it must have been called.  So we set these constructors as accessed and propagate
	 * the instantiated type.
	 *
	 */
	void includeConstructors() {
		if(includedConstructors) {
			return;
		}
		includedConstructors = true;
		
		BT_MethodVector constructors = clazz.getConstructors();
		for(int k=0; k<constructors.size(); k++) {
			Method m = repository.getMethod(constructors.elementAt(k));
			m.setAccessed();
			m.addPropagatedObject(clazz);
		}
		
//		switch(constructors.size()) {
//			case 0:
//				return;
//			case 1:
//				Method m = repository.getMethod(constructors.elementAt(0));
//				m.setAccessed();
//				m.addPropagatedObject(clazz);
//				return;
//			default:
//				BT_MethodVector allConstructors = (BT_MethodVector) constructors.clone();
//				for(int i=constructors.size() - 1; i>=0; i--) {
//					BT_Method constructor = constructors.elementAt(i);
//					//if this constructor calls one of the other constructors, then we remove this constructor
//					BT_CodeAttribute code = constructor.getCode();
//					if(code != null) {
//						BT_MethodCallSiteVector calledMethods = code.calledMethods;
//						for(int j=0; j<calledMethods.size(); j++) {
//							BT_Method calledMethod = calledMethods.elementAt(j).getTarget();
//							if(allConstructors.contains(calledMethod)) {
//								constructors.removeElementAt(i);
//							}
//						}
//					}
//				}
//				//if there is just one constructor left, then we know it must be called
//				if(constructors.size() == 1) {
//					m = repository.getMethod(constructors.elementAt(0));
//					m.setAccessed();
//					m.addPropagatedObject(clazz);
//				}
//		}
	}
	
	public String toString() {
		return clazz.toString();
	}
}
