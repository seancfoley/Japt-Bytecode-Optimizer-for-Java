package com.ibm.ive.tools.japt.devirtualization;

import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptMethod;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Devirtualizer {

	JaptRepository repository;
	Logger logger;
	Messages messages;
	DevirtualizeRepository devirtualizerRepository;
	boolean assumeUnknownVirtualTargets;
	
	//statistics
	int staticCount;
	int specialCount;
	int callSiteCount;
	int classesFinalCount;
	int methodsFinalCount;
	int candidateClassesCount;
	int methodsFinalCandidateCount;
	boolean devirtualizationInitialized;
	
	/**
	 * Constructor for J9Devirtualizer.
	 */
	
	public Devirtualizer(JaptRepository repository, Logger logger, 
						 Messages messages, boolean assumeUnknownVirtualTargets) {
		this.repository = repository;
		this.logger = logger;
		this.messages = messages;
		this.assumeUnknownVirtualTargets = assumeUnknownVirtualTargets;
	}
	
	private void initializeDevirtualization() {
		if(devirtualizationInitialized) {
			return;
		}
		devirtualizationInitialized = true;
		devirtualizerRepository = new DevirtualizeRepository(repository, logger, messages, assumeUnknownVirtualTargets);
		BT_ClassVector classes = repository.getInternalClasses();
		for (int i = 0; i < classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			BT_MethodVector methods = clazz.methods;
			for(int j=0; j < methods.size(); j++) {
				BT_CodeAttribute code = methods.elementAt(j).getCode();
				if(code != null) {
					callSiteCount += code.calledMethods.size();
				}
			}
		}
	}
	
	void devirtualizeCallsToStatic() {
		
		messages.STARTED_STATIC_DEVIRTUALIZING.log(logger);
		initializeDevirtualization();
		BT_ClassVector classes = repository.getInternalClasses();
		for (int i = 0; i < classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			BT_MethodVector methods = (BT_MethodVector) clazz.methods.clone();
		
			for (int j = 0; j < methods.size(); j++) {
				BT_Method meth = methods.elementAt(j);
				
				Method method = devirtualizerRepository.getMethod(meth);
				Method copiedMethods[] = method.convertInvokesToStatic();
				staticCount += method.staticConversionCount;
				checkLoop:
				for(int k = 0; k<copiedMethods.length; k++) {
					Method newMeth = copiedMethods[k];
					BT_Method copiedMethod = newMeth.method;
				
					//if the new static method is in a class that we already devirtualized,
					//then we know it is a copy of a virtual that was already devirtualized,
					//so it needs no further devirtualizing itself
				
					//if the new method is in a class that we have not yet devirtualized, then
					//it will be devirtualized later
				
					//if the method is in the class we are currently devirtualizing, we must
					//add it to the list if it was not already devirtualized
					if(copiedMethod.getDeclaringClass().equals(clazz)) {
						for(int l=0; l<j; l++) {
							if(copiedMethod.equals(methods.elementAt(l))) {
								//the method is a copy of a devirtualized method
								continue checkLoop;
							}
						}
						//the method has not been devirtualized yet, add it to the list of methods in this class
						//that need to be devirtualized
						methods.addElement(newMeth.staticEquivalent);
					}
				}
			}
		}
	}
	
	void removeUnreferencedMethods() {
		messages.REMOVING_UNREFERENCED_METHODS.log(logger);
		BT_ClassVector classes = repository.getInternalClasses();
		for (int i = 0; i < classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			BT_MethodVector methods = (BT_MethodVector) clazz.methods.clone();

			for (int j = 0; j < methods.size(); j++) {
				BT_Method meth = methods.elementAt(j);
				Method method = devirtualizerRepository.getMethod(meth);
				if(method.getEquivalentStaticMethod() != null) {
					if(((JaptMethod) meth).getAllCallSites().isEmpty()) {
						meth.remove();
						messages.REMOVED_UNREFERENCED_METHOD.log(logger, meth.useName());
					}
				}
			}
		}
	}
		
	void devirtualizeCallsToSpecial() {
		
		messages.STARTED_SPECIAL_DEVIRTUALIZING.log(logger);
		initializeDevirtualization();
		BT_ClassVector classes = repository.getInternalClasses();
		// Always convertInvokevirtualToSpecial - no overhead in size, always faster
		for (int i = 0; i < classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			BT_MethodVector methods = clazz.methods;
			for (int j = methods.size() - 1; j >= 0 ; j--) {
				BT_Method meth = methods.elementAt(j);
				Method method = devirtualizerRepository.getMethod(meth);
				//callsiteCount += method.callSites.size();
					// Do not include in count
				method.convertInvokesToSpecial();
				specialCount += method.specialConversionCount;
			}
		}
		
	}
	
	/**
	* First make all referenced non-extensible leaf classes final.
	* Next, make all leaf instance methods final that have no override and
	* no extensible leaf class or subclass.
	*/
	void makeFinal() {
		messages.STARTED_MAKING_CLASSES_FINAL.log(logger);
		BT_ClassVector classes = repository.getInternalClasses();
		BT_Class clazz;
		BT_Method method;

		InternalClassesInterface internalClassesInterface = repository.getInternalClassesInterface();
		for (int i = 0; i < classes.size(); i++) {
			clazz = classes.elementAt(i);
			if (!clazz.isFinal() && !clazz.isAbstract() && !clazz.isInterface()) {
				if(!internalClassesInterface.isInEntireInterface(clazz)
					&& !assumeUnknownVirtualTargets
					&& clazz.getKids().size() == 0) {
					clazz.setFinal(true);
					classesFinalCount++;
					messages.MADE_FINAL_CLASS.log(logger, clazz.getName());
				}
				candidateClassesCount++;
			}
			
		}
		
		messages.STARTED_MAKING_METHODS_FINAL.log(logger);
		for (int i = 0; i < classes.size(); i++) {
			clazz = classes.elementAt(i);
			BT_MethodVector methods = clazz.methods;
			for (int j = 0; j < methods.size(); j++) {
				method = methods.elementAt(j);
				if (method.isInstanceMethod() 
						&& !method.isConstructor() 
						&& !method.isFinal() 
						&& !method.isAbstract()) {
					
					if(!internalClassesInterface.isInEntireInterface(method) 
							&& !assumeUnknownVirtualTargets
							&& method.getKids().size() == 0) {
						method.setFinal(true);
						methodsFinalCount++;
						messages.MADE_FINAL_METHOD.log(logger, method.useName());
					}
					methodsFinalCandidateCount++;
				}
			}
		}
	}
		
	

}
