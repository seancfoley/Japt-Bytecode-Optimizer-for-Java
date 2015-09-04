/*
 * Created on Apr 11, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.coldMethod;

import com.ibm.ive.tools.japt.AccessorCallSiteVector;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Method;

/**
 * Represents a class for cold method refactoring.  Not all methods in the underlying class may be known
 * by this class, only those methods which we may be interested in are known.
 * The methods are added to an object of this type as needed by the getMethod() method.  
 * @author sfoley
 *
 */
public class Clazz {
	static final String COLD_SUFFIX = "$$Cold";
	final BT_Class clazz;
	
	/**
	 * represents methods originally a part of this class, but which may be migrated to another class.
	 */
	MethodVector originalMethods;
	
	Clazz(BT_Class clazz) {
		this.clazz = clazz;
	}
	
	public String findCounterpartName() {
		String newName = clazz.getName() + COLD_SUFFIX;
		JaptRepository repository = getRepository();
		while(true) {
			BT_Class coldClass = repository.getClass(newName);
			if(coldClass == null || coldClass.isSynthetic()) {
				return newName;
			}
			newName += '$';
		}
	}
	
	public BT_Class createColdCounterpart(String newName, ExtensionRepository coldRep) throws InvalidIdentifierException {
		JaptRepository repository = getRepository();
		Identifier identifier = new Identifier(newName);
		BT_Class ret = repository.createInternalClass(identifier, coldRep.coldClassesEntry, clazz.getVersion());
		ret.setSynthetic(true);
		return ret;
	}
	
	public boolean hasColdMethods() {
		if(originalMethods == null) {
			return false;
		}
		for(int i=0; i<originalMethods.size(); i++) {
			Method method = originalMethods.elementAt(i);
			if(method.isCold()) {
				return true;
			}
		}
		return false;
	}
	
	
	public int migrateColdMethods(BT_Class toClass, ExtensionRepository coldRep) {
		if(clazz.equals(toClass)) {
			return 0;
		}
		if(originalMethods == null) {
			return 0;
		}
		int count = 0;
		for(int i=0; i<originalMethods.size(); i++) {
			Method method = originalMethods.elementAt(i);
			if(!method.isCold()) {
				continue;
			}
			if(migrateColdMethod(toClass, coldRep, method)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @param toClass
	 * @param coldRep
	 * @param count
	 * @param method
	 * @return
	 */
	public boolean migrateColdMethod(BT_Class toClass, ExtensionRepository coldRep, Method method) {
		if(clazz.equals(toClass)) {
			return false;
		}
		try {
			if(method.migrate(toClass, coldRep, this)) {
				coldRep.messages.MIGRATED_METHOD.log(coldRep.logger, 
						new String[] {method.method.useName(), method.getMigratedMethod().useName()});
				return true;
			}
		} catch(BT_CodeException e) {
			JaptRepository repository = getRepository();
			JaptFactory factory = repository.getFactory();
			factory.noteCodeException(e);
			//TODO a message (same one as failure above
		}
		return false;
	}
	
	public AccessorCallSiteVector bypassAccessors() {
		if(originalMethods == null) {
			return AccessorCallSiteVector.emptySiteVector;
		}
		AccessorCallSiteVector totalAccessors = null;
		for(int i=0; i<originalMethods.size(); i++) {
			Method method = originalMethods.elementAt(i);
			if(!method.isCold()) {
				continue;
			}
			AccessorCallSiteVector removedAccessors = method.bypassAccessors();
			if(totalAccessors == null) {
				totalAccessors = removedAccessors;
			} else {
				totalAccessors.addAllUnique(removedAccessors);
			}
		}
		if(totalAccessors == null) {
			return AccessorCallSiteVector.emptySiteVector;
		}
		return totalAccessors;
	}
	
	public Method hasMethod(BT_Method method) {
		if(originalMethods == null) {
			return null;
		}
		for(int i=0; i<originalMethods.size(); i++) {
			Method existingMethod = originalMethods.elementAt(i);
			if(existingMethod.method.equals(method)) {
				return existingMethod;
			}
		}
		return null;
	}
	
	public Method getMethod(BT_Method method) {
		if(originalMethods == null) {
			originalMethods = new MethodVector();
		} else {
			for(int i=0; i<originalMethods.size(); i++) {
				Method existingMethod = originalMethods.elementAt(i);
				if(existingMethod.method.equals(method)) {
					return existingMethod;
				}
			}
		}
		Method newMethod = new Method(method);
		originalMethods.addElement(newMethod);
		return newMethod;
	}
	
	public int totalColdMethodByteSize() {
		int size = 0;
		if(originalMethods != null) {
			for(int i=0; i<originalMethods.size(); i++) {
				Method method = originalMethods.elementAt(i);
				if(method.isCold()) {
					size += originalMethods.elementAt(i).method.getCode().computeInstructionSizes();
				}
			}
		}
		return size;
	}
	
	public JaptRepository getRepository() {
		return (JaptRepository) clazz.getRepository();
	}
}
