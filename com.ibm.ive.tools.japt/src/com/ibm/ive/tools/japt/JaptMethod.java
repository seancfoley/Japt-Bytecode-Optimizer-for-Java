package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.*;

/**
 * @author sfoley
 *
 */
public class JaptMethod extends BT_Method {

	public JaptMethod(BT_Class cls) {
		super(cls);
		JaptRepository rep = getRepository();
		if(rep.relatedMethodMap != null) {
			rep.relatedMethodMap.reset();
		}
	}

	/* ensure that the related method map is up-to-date */
	public void remove() {
		super.remove();
		JaptRepository rep = getRepository();
		if(rep.relatedMethodMap != null) {
			rep.relatedMethodMap.remove(this);
		}
		AccessorMethodGenerator gen = rep.getAccessorMethodGenerator(cls);
		if(gen != null) {
			gen.accessorMethods.removeElement(this);
		}
	}
	
	private JaptRepository getRepository() {
		return (JaptRepository) cls.getRepository();
	}
	
//	protected BT_MethodCallSiteVector getReferencingCallSites() {
//		return getAllCallSites();
//	}
	
	/**
	 * Finds all sites from which a method can be called, which include
	 * sites directly calling the method and sites calling an overrided superclass
	 * method or an implemented interface method.
	 * @return the collection of callsites
	 */
	public BT_MethodCallSiteVector getAllCallSites() {
		BT_MethodCallSiteVector sites = (BT_MethodCallSiteVector) callSites.clone();
		if(isStatic() || isPrivate() || isConstructor()) {
			return sites;
		}
		
		JaptRepository repository = (JaptRepository) cls.getRepository();
		
		//Any call to a parent method might actually resolve to the current method
		BT_MethodVector parents = repository.getRelatedMethodMap().getAllParents(this);
		if(parents.size() == 0) {
			return sites;
		}
	
		
		for(int i=0; i<parents.size(); i++) {
			JaptMethod parent = (JaptMethod) parents.elementAt(i);
			BT_MethodCallSiteVector parentSites = parent.getAllCallSites();
			for(int j=0; j<parentSites.size(); j++) {
				BT_MethodCallSite parentSite = parentSites.elementAt(j);
				BT_MethodRefIns parentIns = parentSite.instruction;
				if(parentIns.isInvokeSpecialIns()) {
					BT_Method from = parentSite.getFrom();
					//an invokespecial from a calling class to a class not in the calling class hierarchy
					//can only call private methods or constructors and are not overridable
					
					//additionally, the overridable invocation cannot come from a parent class, 
					//it must come from a child class
					if(!from.cls.isDescendentOf(cls)) {
						continue;
					}
				}
				sites.addUnique(parentSite);
			}
		}
		return sites;
	}
}
