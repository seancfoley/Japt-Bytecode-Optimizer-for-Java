/*
 * Created on Feb 27, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.coldMethod;

import java.util.Comparator;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.AccessorCallSite;
import com.ibm.ive.tools.japt.AccessorCallSiteVector;
import com.ibm.ive.tools.japt.AccessorMethod;
import com.ibm.ive.tools.japt.AccessorMethodGenerator;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.MemberActor;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

public class ColdMethodExtension implements CommandLineExtension {
	final private Messages messages = new Messages(this);

	private SpecifierOption coldMethods = new SpecifierOption(messages.COLD_LABEL, messages.COLD);
	private SpecifierOption warmMethods = new SpecifierOption(messages.WARM_LABEL, messages.WARM);
	private FlagOption notWarmIsCold = new FlagOption(messages.NOT_WARM_IS_COLD_LABEL, messages.NOT_WARM_IS_COLD);
	
	//cold method refactoring works better if permissions are expanded to allow increased access
//TODO maybe change the default behaviour to allowing accessors and overriding permissions? or overriding permissions and not allowing accessors?
	final public FlagOption overridePermissions = new FlagOption(messages.EXPAND_PERMISSIONS_LABEL, messages.EXPAND_PERMISSIONS); 
	final public FlagOption allowAccessors = new FlagOption(messages.ALLOW_ACCESSORS_LABEL, messages.ALLOW_ACCESSORS);
	final public FlagOption doHalf = new FlagOption("doHalf", "migrate half the methods in internal classes");
	{
		doHalf.setVisible(false);
	}
	
	static final int MIN_METHOD_SIZE = 16;
	static final int MIN_CLASS_METHOD_SIZE = 128;
	
	public ColdMethodExtension() {}

	public Option[] getOptions() {
		return new Option[] {
				coldMethods,
				warmMethods,
				notWarmIsCold,
				overridePermissions,
				allowAccessors,
				doHalf};
	}
	
	/*
	 * Things that are better in this implmentation from D Wood:
	 * -instead of always using accessors, can increase visibility
	 * -inaccessible classes (instanceof and checkcast) are now accounted for, although by migrating within the same package this is irrelevant
	 * -no migrating methods and then removing them after if shown to be illegal, now we ensure they can be moved before moving them
	 */

	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		
		Specifier methodsSpec[] = warmMethods.getSpecifiers();
		BT_MethodVector warmMethods = new BT_HashedMethodVector();
		if(methodsSpec.length > 0) {
			MemberActor notColdActor = 
				new MemberCollectorActor(warmMethods, null);
			repository.findMethods(methodsSpec, notColdActor);
		}
		methodsSpec = coldMethods.getSpecifiers();
		BT_MethodVector coldMethods = new BT_HashedMethodVector();
		if(methodsSpec.length > 0) {
			MemberActor actor = 
				new MemberCollectorActor(coldMethods, null);
			repository.findMethods(methodsSpec, actor);
		}
		
		
		for(int i=0; i<warmMethods.size(); i++) {
			coldMethods.removeElement(warmMethods.elementAt(i));
		}
		
		if(notWarmIsCold.isFlagged()) {
			//sort the warm methods by class name
			warmMethods.sort();
			int j;
			for(int i=0; i<warmMethods.size(); i=j) {
				BT_Method warmMethod = warmMethods.elementAt(i);
				BT_Class declaringClass = warmMethod.getDeclaringClass();
				for(j=i+1; j<warmMethods.size(); j++) {
					BT_Method nextWarmMethod = warmMethods.elementAt(j);
					if(!nextWarmMethod.getDeclaringClass().equals(declaringClass)) {
						break;
					}
				}
				BT_MethodVector clazzMethods = declaringClass.methods;
				nextCold:
				for(int k=0; k<clazzMethods.size(); k++) {
					BT_Method potentialCold = clazzMethods.elementAt(k);
					if(potentialCold.isStaticInitializer()) {
						continue;
					}
					for(int m=i; m<j; m++) {
						warmMethod = warmMethods.elementAt(m);
						if(potentialCold.equals(warmMethod)) {
							continue nextCold;
						}
					}
					coldMethods.addUnique(potentialCold);
				}
			}
		}
		
		//populate the repository
		final ExtensionRepository repo = new ExtensionRepository(repository, messages, logger);
		repo.allowAccessors = allowAccessors.isFlagged();
		repo.allowChangedPermissions = overridePermissions.isFlagged();
		
		
		
		
		//this method checks whether the cold methods are valid cold methods before they are set as cold in the repository
		populateRepository(coldMethods, repo);
		
		int coldClassCount = 0;
		int coldMethodCount = 0;
		JaptFactory factory = repository.getFactory();
		
		
		if(doHalf.isFlagged()) {
			BT_ClassVector internalClasses = (BT_ClassVector) repository.getInternalClasses().clone();
			for(int i=0; i<internalClasses.size(); i++) {
				BT_Class clazz = internalClasses.elementAt(i);
				if(clazz.isInterface()) {
					continue;
				}
				Clazz coldClass = repo.getClazz(clazz); 
				try {
					String newName = coldClass.findCounterpartName();
					BT_Class newClass = repository.getClass(newName);
					boolean preexists = newClass != null;
					if(!preexists) {
						newClass = coldClass.createColdCounterpart(newName, repo);
					}
					
					
					int count = 0;
					
					//BT_MethodVector tried = new BT_HashedMethodVector();
					BT_MethodVector valid = new BT_HashedMethodVector();
					BT_MethodVector methods = clazz.getMethods();
					for(int j=0; j<methods.size(); j++) {
						BT_Method method = methods.elementAt(j);
						if(isValidColdMethod(true, method, logger)) {
							valid.addElement(method);
						}
					}
					BT_MethodVector newMethods = new BT_MethodVector();
					for(int k=0; k<valid.size(); k++) {
						if((k % 2) == 0) {
							newMethods.addElement(valid.elementAt(k));
						} else {
							newMethods.insertElementAt(valid.elementAt(k), 0);
						}
					}
					
					for(int k=0; k<newMethods.size(); k++) {
						BT_Method method = newMethods.elementAt(k);
						Method meth = coldClass.getMethod(method);
						if(coldClass.migrateColdMethod(newClass, repo, meth)) {
							count++;
							if(count > newMethods.size() / 2) {
								break;
							}
						}
					}
					
					if(count > 0) {
						coldMethodCount += count;
						coldClassCount++;
					} else if(!preexists) {
						newClass.remove();
					}
				} catch(InvalidIdentifierException e) {
					factory.noteInvalidIdentifier(e.getIdentifier());
				}
				
				
			}
		} 
		
		Clazz classes[] = repo.getClazzes();
		for(int i=0; i<classes.length; i++) {
			Clazz clazz = classes[i];
			if(repository.isInternalClass(clazz.clazz) && clazz.hasColdMethods()) {
				try {
					String newName = clazz.findCounterpartName();
					BT_Class newClass = repository.getClass(newName);
					boolean preexists = newClass != null;
					if(!preexists) {
						newClass = clazz.createColdCounterpart(newName, repo);
					}
					
					int count = clazz.migrateColdMethods(newClass, repo);
					if(count > 0) {
						coldMethodCount += count;
						coldClassCount++;
					} else if(!preexists) {
						newClass.remove();
					}
				} catch(InvalidIdentifierException e) {
					factory.noteInvalidIdentifier(e.getIdentifier());
				}
			}
		}
		
		if(repo.allowAccessors) {
			//now stop the use of accessors that are no longer necessary
			for(int i=0; i<classes.length; i++) {
				Clazz clazz = classes[i];
				AccessorCallSiteVector removedAccessors = clazz.bypassAccessors();
				//remove accessors that are no longer in use
				for(int j=0; j<removedAccessors.size(); j++) {
					AccessorCallSite callSite = removedAccessors.elementAt(j);
					AccessorMethod accessorMethod = callSite.method;
					accessorMethod.removeIfUnused();
				}
			}
		}
		
		BT_ClassVector internalClasses = repository.getInternalClasses();
		for(int i=0; i<internalClasses.size(); i++) {
			BT_Class internalClass = internalClasses.elementAt(i);
			BT_MethodVector methods = internalClass.getMethods();
			methods.sort(new Comparator() {
				// reg method, then cold, then accessors
				public int compare(Object one, Object two) {
					BT_Method methodOne = (BT_Method) one;
					BT_Method methodTwo = (BT_Method) two;
					int oneStatus = getStatus(methodOne);
					if(oneStatus == 0) {
						return -1;
					}
					return oneStatus - getStatus(methodTwo);
				}
				
				private int getStatus(BT_Method method) {
					if(AccessorMethodGenerator.isMethodAccessor(method) != null) {
						return 2;
					} else if(methodIsCold(method)) {
						return 1;
					} else {
						return 0;
					}
				}
				
				boolean methodIsCold(BT_Method method) {
					BT_Class declaringClass = method.getDeclaringClass();
					Clazz clazz = repo.hasClazz(declaringClass);
					if(clazz == null) {
						return false;
					}
					Method meth = clazz.getMethod(method);
					if(meth == null) {
						return false;
					}
					return meth.isCold();
				}
			});
		}

		messages.SUMMARY.log(
				logger, 
				new Object[] {
					Integer.toString(coldMethodCount),
					Integer.toString(coldClassCount)}
			);
	}
	
	/**
	 * @param methods
	 * @param repo
	 */
	private void populateRepository(BT_MethodVector coldMethods, ExtensionRepository repo) {
		if(coldMethods.size() == 0) {
			return;
		}
		coldMethods.sort();
		BT_Method coldMethod = coldMethods.firstElement();
		BT_Class coldClass = coldMethod.getDeclaringClass();
		int i=0;
		while(true) {
			Clazz clazz = null;
			JaptRepository repository = (JaptRepository) coldClass.getRepository();
			boolean isInternal = repository.isInternalClass(coldClass);
			BT_Class nextColdClass;
			do {
				if(isValidColdMethod(isInternal, coldMethod, repo.logger)) {
					if(clazz == null) {
						clazz = repo.getClazz(coldClass);
					}
					Method method = clazz.getMethod(coldMethod);
					method.setCold(true);
				}
				if(++i >= coldMethods.size()) {
					return;
				}
				coldMethod = coldMethods.elementAt(i);
				nextColdClass = coldMethod.getDeclaringClass();
			} while(coldClass.equals(nextColdClass));
			if(clazz != null && clazz.originalMethods != null) {
				String reason = isValidColdClass(clazz);
				if(reason != null) {
					for(int j=0; j<clazz.originalMethods.size(); j++) {
						Method method = clazz.originalMethods.elementAt(j);
						if(method.isCold()) {
							method.setCold(false);
							messages.CANNOT_MIGRATE_METHOD.log(repo.logger, new String[] {method.method.useName(), reason});
						}
					}
				}
			}
			coldClass = nextColdClass;
		}
		
	}
	
	private String isValidColdClass(Clazz clazz) {
		if(clazz.totalColdMethodByteSize() < MIN_CLASS_METHOD_SIZE) {
			return messages.CLASS_TOO_SMALL;
		}
		return null;
	}
	
	private boolean isValidColdMethod(boolean isInternal, BT_Method method, Logger logger) {
		if(method.getDeclaringClass().isArray()) {
			return false;
		}
		if(!isInternal) {
			messages.CANNOT_MIGRATE_METHOD.log(logger, new String[] {method.useName(), messages.EXTERNAL_CLASS});
			return false;
		}
		if (method.isNative()) {
			messages.CANNOT_MIGRATE_METHOD.log(logger, new String[] {method.useName(), method.keywordModifierString(BT_Method.NATIVE)});
			return false;
		}
		if (method.isAbstract()) {
			messages.CANNOT_MIGRATE_METHOD.log(logger, new String[] {method.useName(), method.keywordModifierString(BT_Method.ABSTRACT)});
			return false;
		}
		if (method.isStaticInitializer()) {
			BT_Class declaringClass = method.getDeclaringClass();
			messages.CANNOT_MIGRATE_CLINIT.log(logger, new Object[] {declaringClass.fullKindName(), declaringClass});
			return false;
		}
		if (method.isStub()) {
			messages.CANNOT_MIGRATE_METHOD.log(logger, new String[] {method.useName(), messages.NOT_LOADED});
			return false;
		}
		BT_CodeAttribute code = method.getCode();
		int codeSize = code.computeInstructionSizes();
		if (codeSize < MIN_METHOD_SIZE) {
			messages.CANNOT_MIGRATE_METHOD.log(logger, new Object[] {method, messages.METHOD_TOO_SMALL});
			return false;
		}
		return true;
		
		//STUFF David W checked for but I've chosen not to:
		//-if clinit is selected for a given class then the whole class is ignored
		//-classes to avoid: stuff to not migrate because accessed internally by VM (why??)
		//-overloaded methods
	}
	
	public String getName() {
		return messages.DESCRIPTION;
	}

}