package com.ibm.ive.tools.japt.load;

import java.io.IOException;
import java.util.ArrayList;

import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.ConditionalInterfaceItemCollection;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InterfaceItemCollection;
import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.MemberActor;
import com.ibm.ive.tools.japt.MemberSelector;
import com.ibm.ive.tools.japt.RelatedMethodMap;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.MemberActor.ClassActor;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Factory;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_HashedMethodVector;
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
public class RepositoryLoader extends com.ibm.ive.tools.japt.RepositoryLoader {

	protected Options data;
	protected JaptFactory factory;
	private LoadClassPathEntry loadClassPathEntries[] = new LoadClassPathEntry[0];
	protected RefReport ref;
	private InternalClassesInterface internalClassesInterface;
	
	public RepositoryLoader(RefReport ref, JaptRepository repo, Options data, Logger logger)  {
		super(repo, logger, data.archiveExtensions);
		this.factory = repo.getFactory();
		this.data = data;
		this.ref = ref;
		this.internalClassesInterface = repo.getInternalClassesInterface();
	}
	
	
	/**
	 * The classpath is ordered as follows:
	 * 1. command line -load options 
	 * 2. command line internal class paths (left to right order)
	 * 3. command line external class paths (option -cp, left to right order)
	 * 4. whatever was on the class path prior to calling this method
	 */
	public void setClassPath() {
		String externalClassPath[] = data.jreClassPath.getEntries();
		for(int i=externalClassPath.length - 1; i>=0; i--) {
			String cps = externalClassPath[i];
			repository.prependClassPath(cps);
		}
		
		externalClassPath = data.externalClassPathAll.getEntries();
		for(int i=externalClassPath.length - 1; i>=0; i--) {
			String cps = externalClassPath[i];
			repository.prependClassPath(cps);
		}
		
		externalClassPath = data.externalClassPathList.getEntries();
		for(int i=externalClassPath.length - 1; i>=0; i--) {
			String cp = externalClassPath[i];
			repository.prependClassPath(cp);
		}
		
		String internalClassPath[] = data.internalClassPathAll.getEntries();
		for(int i=internalClassPath.length - 1; i>=0; i--) {
			repository.prependInternalClassPathEntry(internalClassPath[i], true);		
		}
		
		internalClassPath = data.internalClassPathList.getEntries();
		for(int i=internalClassPath.length - 1; i>=0; i--) {
			repository.prependInternalClassPathEntry(internalClassPath[i], true);		
		}
		
		String loadClassPathStrings[] = data.load.getEntries();
		String recursiveLoadStrings[] = data.loadAll.getEntries();
		ArrayList loadClassPathList = new ArrayList(
				loadClassPathStrings.length + recursiveLoadStrings.length * 4);
			
		for(int i=recursiveLoadStrings.length - 1; i>=0; i--) {
			String cps = recursiveLoadStrings[i];
			ClassPathEntry cpes[] = repository.prependInternalClassPathEntry(cps, false);
			//we will actually get a one element array here because we have already
			//separated the path
			for(int k=0; k<cpes.length; k++) {
				ClassPathEntry cpe = cpes[k];
				if(cpe != null) {
					loadClassPathList.add(0, new LoadClassPathEntry(cps, cpe));
				}
			}
		}
		
		
		for(int i=loadClassPathStrings.length - 1; i>=0; i--) {
			String cps = loadClassPathStrings[i];
			if(isClassPathEntry(cps)) {
				ClassPathEntry cpes[] = repository.prependInternalClassPathEntry(cps, false);
				//we will actually get a one element array here because we have already
				//separated the path
				for(int k=0; k<cpes.length; k++) {
					ClassPathEntry cpe = cpes[k];
					if(cpe != null) {
						loadClassPathList.add(0, new LoadClassPathEntry(cps, cpe));
					}
				}
			}
		}
		
		loadClassPathEntries = (LoadClassPathEntry[]) loadClassPathList.toArray(new LoadClassPathEntry[loadClassPathList.size()]);	
	}
	
	private static class LoadClassPathEntry {
		String name;
		ClassPathEntry entry;
	
		LoadClassPathEntry(String name, ClassPathEntry e) {
			this.name = name;
			this.entry = e;
		}
	}
	
	interface InterfaceSpecifierProxy {
		void setInterfaceItemSpecifier(InterfaceItemCollection spec);
	}
	
	
	class InterfaceClassActor extends ClassActor implements InterfaceSpecifierProxy {
		InterfaceItemCollection interfaceSpecifier = internalClassesInterface;
		
		public void setInterfaceItemSpecifier(InterfaceItemCollection spec) {
			interfaceSpecifier = spec;
		}
		
		public void actOnClass(BT_Class referencedClass) {
			interfaceSpecifier.addToInterface(referencedClass);
			interfaceSpecifier.addTargetedClassToInterface(referencedClass);
		}
		
		public void actOnMethod(BT_Class referencedClass, BT_Method method) {
			actOnQualifiedMethod(referencedClass, method);
		}

		public void actOnField(BT_Class referencedClass, BT_Field field) {
			ref.printEntry("\tField: ", field.useName());
			interfaceSpecifier.addToInterface(field);
		}
		
		void actOnQualifiedMethod(BT_Class referencedClass, BT_Method method) {
			ref.printEntry("\tMethod: ", method.useName());
			interfaceSpecifier.addToInterface(method);
		}
		
		public void actOnInterfaceMethod(BT_Class referencedInterface, BT_Method method) {
			actOnQualifiedMethod(referencedInterface, method);
		}
	}
	
	/**
	 * A useful class for specifying specific members in the repository as "specified classes".
	 * See Repository for a description of what it means for a member to be specified.
	 */
	class InterfaceMemberActor extends MemberActor implements InterfaceSpecifierProxy {
		
		InterfaceItemCollection interfaceSpecifier = internalClassesInterface;
		public boolean memberOnly;
		
		public void setInterfaceItemSpecifier(InterfaceItemCollection spec) {
			interfaceSpecifier = spec;
		}
		
		public void actOnMethod(BT_Class referencedClass, BT_Method method) {
			actOnQualifiedMethod(referencedClass, method);
		}

		public void actOnField(BT_Class referencedClass, BT_Field field) {
			ref.printEntry("\tField: ", field.useName());
			interfaceSpecifier.addToInterface(field);
			if(!memberOnly) {
				interfaceSpecifier.addTargetedClassToInterface(referencedClass);
				interfaceSpecifier.addToInterface(referencedClass);
			}
		}
		
		void actOnQualifiedMethod(BT_Class referencedClass, BT_Method method) {
			ref.printEntry("\tMethod: ", method.useName());
			interfaceSpecifier.addToInterface(method);
			if(!memberOnly) {
				interfaceSpecifier.addTargetedClassToInterface(referencedClass);
				interfaceSpecifier.addToInterface(referencedClass);
			}
		}
		
		public void actOnInterfaceMethod(BT_Class referencedInterface, BT_Method method) {
			actOnQualifiedMethod(referencedInterface, method);
		}
	};
	
	class ExtendedInterfaceClassActor extends InterfaceClassActor {
		RelatedMethodMap map = repository.getRelatedMethodMap();
		
		public void actOnMethod(BT_Class referencedClass, BT_Method method) {
			actOnQualifiedMethod(referencedClass, method);
			if(referencedClass.isClass()) {
				BT_MethodVector ovMethods = map.getOverridingMethods(method);
				for(int i=0; i<ovMethods.size(); i++) {
					BT_Method ov = ovMethods.elementAt(i);
					interfaceSpecifier.addToInterface(ov);
				}
			}
		}

		public void actOnInterfaceMethod(BT_Class referencedInterface, BT_Method method) {
			actOnQualifiedMethod(referencedInterface, method);
			if(referencedInterface.isInterface()) {
				BT_MethodVector implementingMethods = map.getImplementingMethods(method);
				for(int i=0; i<implementingMethods.size(); i++) {
					BT_Method implementor = implementingMethods.elementAt(i);
					interfaceSpecifier.addToInterface(implementor);
				}
			}
		}
	}
	
	class MarkAllKidsActor extends ClassActor implements InterfaceSpecifierProxy {
		RelatedMethodMap map = repository.getRelatedMethodMap();
		InterfaceItemCollection interfaceSpecifier = internalClassesInterface;
		
		final MemberActor methodSpecActor = new MemberActor() {
			public void actOnMethod(BT_Class referencedClass, BT_Method method) {
				interfaceSpecifier.addToInterface(method);
			}
		};
		
		final MemberActor overriderActor = new MemberActor() {
			public void actOnMethod(BT_Class referencedClass, BT_Method method) {
				if(referencedClass.isClass) {
					interfaceSpecifier.addToInterface(method);
					BT_MethodVector ovMethods = map.getOverridingMethods(method);
					for(int i=0; i<ovMethods.size(); i++) {
						BT_Method ov = ovMethods.elementAt(i);
						interfaceSpecifier.addToInterface(ov);
					}
				}
			}
		};
		
		public void setInterfaceItemSpecifier(InterfaceItemCollection spec) {
			interfaceSpecifier = spec;
		}
		
		public void actOnClass(BT_Class referencedClass) {
			//for any and all implementing classes, mark all public methods as possible implementors
			if(referencedClass.isClass()) {
				markAllClassKids(referencedClass);
			}
			else {
				markAllInterfaceKids(referencedClass);
			}
		}

		void markAllInterfaceKids(BT_Class clazz) {
			BT_ClassVector kids = clazz.getKids();
			for(int k=0; k<kids.size(); k++) {
				BT_Class kid = kids.elementAt(k);
				if(kid.isInterface() || kid.isAbstract()) {
					markAllInterfaceKids(kid);
				}
				else {
					overriderActor.actOn(kid, MemberSelector.publicNonStaticMethodSelector);
				}
			}
		}
		
		void markAllClassKids(BT_Class clazz) {
			BT_ClassVector kids = clazz.getKids();
			methodSpecActor.actOn(kids, MemberSelector.publicNonStaticNonResolvableMethodSelector);
			for(int k=0; k<kids.size(); k++) {
				BT_Class kid = kids.elementAt(k);
				markAllClassKids(kid);
			}
		}
	};
	
	/**
	 * loads classes specified on the command line using the configured class path.
	 */	
	public void load(boolean strictVerify, boolean preloadRelated) {
		BT_Factory.strictVerification = strictVerify;
		factory.preloadRelatedClasses = preloadRelated;
		
		//first we load classes and resources specified with -loadFile
		//all identifiers will specify files, whether class files, resource files or archive files.
		
		//if -loadFile xxx conflicts with something on the classpath
		//then it takes precedence.  If the user wishes otherwise
		//then he can simply remove it from the command line.
		
		//Also, loading classes and resources must be done first before loading entry points, 
		//because the loading creates new classes and resources available for matching by entry point
		//specifications  
		//eg you could load class yyy.Xxx with -loadFile zzz\Xxx.class (if the class file can be found in directory zzz) 
		//and then specify -includeWholeClass yyy.Xxx
		
		String[] othersToLoad = data.load.getEntries();
		for(int i=0; i<othersToLoad.length; i++) {
			String toLoad = othersToLoad[i];
			if(!isClassPathEntry(toLoad)) {
				try {
					ref.printRule(data.load, toLoad);
					loadFile(toLoad);
				} catch(IOException e) {
					data.messages.ERROR_READING_FILE.log(logger, toLoad + " (" + e.toString() + ")");
				}
			}
		}					

		//now load archives specified with -load and -loadAll
		LoadClassPathEntry[] loadClassPath = loadClassPathEntries;
		for(int i=0; i<loadClassPath.length; i++) {
			LoadClassPathEntry lce = loadClassPath[i];
			ClassPathEntry toLoad = lce.entry;
			ref.printRule(data.load, lce.name);
			loadAll(toLoad);
		}
		
		InterfaceMemberActor memberActor = new InterfaceMemberActor();
		
		//load the main class if there is one
		if(data.mainClass.appears()) {
			try {
				Identifier id = data.mainClass.getIdentifier();
				if(!id.isRegularString()) {
					factory.noteInvalidIdentifier(id);
				} else {
					BT_ClassVector loadedClasses = repository.loadClasses(id, false);
					if(loadedClasses.size() > 0) {
						repository.setMainClass(loadedClasses.elementAt(0));
						memberActor.actOn(loadedClasses.elementAt(0), MemberSelector.mainMethodSelector);
					}
				}
			} catch(InvalidIdentifierException e) {
				factory.noteInvalidIdentifier(e.getIdentifier());
			}
		}
		loadSpecifiers(false);
		loadSpecifiers(true);
	}

	private static boolean conditionIsTrue(JaptRepository rep, boolean conditional, Specifier spec) 
		throws InvalidIdentifierException {
		if(conditional) {
			if(!spec.isConditional()) {
				return false;
			}
			return conditionIsTrue(rep, spec);
		}
		return !spec.isConditional();
	}
	
	/**
	 * @param rep
	 * @param spec
	 * @return
	 * @throws InvalidIdentifierException
	 */
	private static boolean conditionIsTrue(JaptRepository rep, Specifier spec) throws InvalidIdentifierException {
		Identifier id = spec.getIdentifier();
		ConditionalInterfaceItemCollection conInterfaceSpecifier = 
			new ConditionalInterfaceItemCollection(rep, id.getFrom(), id.isResolvable(), spec.getCondition());
		return conInterfaceSpecifier.conditionIsTrue();
	}


	private boolean setInterfaceSpecifierProxy(
			final boolean conditional, 
			InterfaceSpecifierProxy proxy, 
			Specifier spec) throws InvalidIdentifierException {
		return setInterfaceSpecifierProxies(conditional, proxy, null, spec);
	}
	
	private boolean setInterfaceSpecifierProxies(
			final boolean conditional, 
			InterfaceSpecifierProxy proxy1, 
			InterfaceSpecifierProxy proxy2, 
			Specifier spec) throws InvalidIdentifierException {
	
		//TODO to support identifier arguments:
		//here we need to check if spec has arguments attached,
		//and is so, then we need to make the proxies/actors aware when we call setInterfaceItemSpecifier
		//ONE way is to change the proxy/actor so that they know there is an argument when they call addToInterface
		//ANOTHER way is to create a "shell" for the proxy that intercepts the calls and thus constructs the arguments
		//and calls a different method in the interface collection
		
		//Need to change the interface collection class so that each item can have an argument, which is a pain in the
		//ass since we can no longer use the jikesbt class, field and method vectors
		if(conditional) {
			if(!spec.isConditional()) {
				return false;
			}
			Identifier id = spec.getIdentifier();
			ConditionalInterfaceItemCollection conInterfaceSpecifier = 
				new ConditionalInterfaceItemCollection(repository, id.getFrom(), id.isResolvable(), spec.getCondition());
			if(!conInterfaceSpecifier.conditionIsTrue()) {
				return false;
			}
			internalClassesInterface.addConditional(conInterfaceSpecifier);
			if(proxy1 != null) {
				proxy1.setInterfaceItemSpecifier(conInterfaceSpecifier);
			}
			if(proxy2 != null) {
				proxy2.setInterfaceItemSpecifier(conInterfaceSpecifier);
			}
		}
		else {
			if(spec.isConditional()) {
				return false;
			}
			if(proxy1 != null) {
				proxy1.setInterfaceItemSpecifier(internalClassesInterface);
			}
			if(proxy2 != null) {
				proxy2.setInterfaceItemSpecifier(internalClassesInterface);
			}
		}
		return true;
	}
	
	
	/**
	 * @param resourceRunnables
	 * @param classRunnables
	 */
	private void loadSpecifiers(final boolean conditional) {
		
		//load explicitly specified classes and resources
		SpecifierOption option = data.loadClass;
		Specifier[] specifiers = option.getSpecifiers();
		for(int i=0; i<specifiers.length; i++) {
			Specifier spec = specifiers[i];
			
			try {
				if(!conditionIsTrue(repository, conditional, spec)) {
					continue;
				}
				ref.printRule(option, spec.getFullString());
				repository.loadClasses(spec.getIdentifier(), false);
			}
			catch(InvalidIdentifierException e) {
				factory.noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		
		for(int k=0; k<2; k++) {
			if(k==0) {
				specifiers = data.loadResource.getSpecifiers();
			}
			else {//includeResource is a deprecated option which does the same as loadResource
				specifiers = data.includeResource.getSpecifiers();
			}
			for(int i=0; i<specifiers.length; i++) {
				Specifier spec = specifiers[i];
				try {
					if(!conditionIsTrue(repository, conditional, spec)) {
						continue;
					}
					repository.loadResources(spec.getIdentifier(), false);
				}
				catch(InvalidIdentifierException e) {
					factory.noteInvalidIdentifier(e.getIdentifier());
				}
			}
		}
		
		
		InterfaceClassActor classActor = new InterfaceClassActor();
		
		//now load interface elements specified with -includeXXX xxx
		option = data.includeClass;
		specifiers = option.getSpecifiers();
		for(int i=0; i<specifiers.length; i++) {
			Specifier spec = specifiers[i];
			try {
				if(!setInterfaceSpecifierProxy(conditional, classActor, spec)) {
					continue;
				}
				ref.printRule(option, spec.getFullString());
				repository.loadClasses(spec.getIdentifier(), classActor, null);
			}
			catch(InvalidIdentifierException e) {
				factory.noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		
		for(int k=0; k<2; k++) {
			MemberSelector memberSpecifier;
			if(k==0) {
				memberSpecifier = MemberSelector.nonPrivateSelector;
				option = data.includeLibraryClass;
			}
			else {
				memberSpecifier = MemberSelector.publicProtectedSelector;
				option = data.includeAccessibleClass;
			}
			specifiers = option.getSpecifiers();
			for(int i=0; i<specifiers.length; i++) {
				Specifier spec = specifiers[i];
				
				try {
					if(!setInterfaceSpecifierProxy(conditional, classActor, spec)) {
						continue;
					}
					ref.printRule(option, spec.getFullString());
					repository.loadClasses(spec.getIdentifier(), classActor, memberSpecifier);
				}
				catch(InvalidIdentifierException e) {
					factory.noteInvalidIdentifier(e.getIdentifier());
				}
			}
			
			if(k==0) {
				option = data.includeExtendedLibraryClass;
			}
			else {
				option = data.includeExtendedAccessibleClass;
			}
			
			
			ExtendedInterfaceClassActor specMemberActor = new ExtendedInterfaceClassActor();
			MarkAllKidsActor markAllKidsActor = new MarkAllKidsActor();
			specifiers = option.getSpecifiers();
			for(int i=0; i<specifiers.length; i++) {
				Specifier spec = specifiers[i];
				
				
				try {
					if(!setInterfaceSpecifierProxies(conditional, specMemberActor, markAllKidsActor, spec)) {
						continue;
					}
					ref.printRule(option, spec.getFullString());
					repository.loadClasses(spec.getIdentifier(), specMemberActor, memberSpecifier);
					
					//TODO a warning is output: "No matches found for identifier..."
					//when there are no matches, however we may end up finding subclasses/implementors of the
					//stubs found, however the user will not know this after seeing the warning...
					//make the information more accurate by outputting a warning here to indicate the action
					//on any found stub subclasses and implementors
					findClassStubs(spec.getIdentifier(), markAllKidsActor);
				}
				catch(InvalidIdentifierException e) {
					factory.noteInvalidIdentifier(e.getIdentifier());
				}
			}
		}
		
		option = data.includeWholeClass;
		specifiers = option.getSpecifiers();
		for(int i=0; i<specifiers.length; i++) {
			Specifier spec = specifiers[i];
			
			try {
				
				if(!setInterfaceSpecifierProxy(conditional, classActor, spec)) {
					continue;
				}
				ref.printRule(option, spec.getFullString());
				repository.loadClasses(spec.getIdentifier(), classActor, MemberSelector.allSelector);
			}
			catch(InvalidIdentifierException e) {
				factory.noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		
		for(int k=0; k<2; ++k) {
			InterfaceMemberActor actor = new InterfaceMemberActor();
			if(k==0) {
				actor.memberOnly = false;
				option = data.includeMethod;
			}
			else {
				actor.memberOnly = true;
				option = data.includeMethodEx;
			}
			specifiers = option.getSpecifiers();
			for(int i=0; i<specifiers.length; i++) {
				Specifier spec = specifiers[i];
				
				
				try {
					if(!setInterfaceSpecifierProxy(conditional, actor, spec)) {
						continue;
					}
					ref.printRule(option, spec.getFullString());
					repository.loadMethods(spec.getIdentifier(), actor);	
				}
				catch(InvalidIdentifierException e) {
					factory.noteInvalidIdentifier(e.getIdentifier());
				}
			}
		}
		
		InterfaceMemberActor actor = new InterfaceMemberActor();
		option = data.includeField;
		specifiers = option.getSpecifiers();
		for(int i=0; i<specifiers.length; i++) {
			Specifier spec = specifiers[i];
			try {
				if(!setInterfaceSpecifierProxy(conditional, actor, spec)) {
					continue;
				}
				ref.printRule(option, spec.getFullString());
				repository.loadFields(spec.getIdentifier(), actor);	
			}
			catch(InvalidIdentifierException e) {
				factory.noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		InterfaceMemberActor mainActor = new InterfaceMemberActor() {
			public void actOnMethod(BT_Class referencedClass, BT_Method method) {
				repository.setOtherMainClass(method.getDeclaringClass());
				actOnQualifiedMethod(referencedClass, method);
			}
		};
		
		option = data.includeMainMethod;
		specifiers = option.getSpecifiers();
		for(int i=0; i<specifiers.length; i++) {
			Specifier spec = specifiers[i];
			Specifier newSpec = spec.append(".main(java.lang.String[])");
			try {
				if(!setInterfaceSpecifierProxy(conditional, mainActor, spec)) {
					continue;
				}
				ref.printRule(option, spec.getFullString());
				repository.loadMethods(newSpec.getIdentifier(), mainActor);
			}
			catch(InvalidIdentifierException e) {
				factory.noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		class SubclassActor extends ClassActor implements InterfaceSpecifierProxy {
		
			InterfaceItemCollection interfaceSpecifier = internalClassesInterface;
			
			public void setInterfaceItemSpecifier(InterfaceItemCollection spec) {
				interfaceSpecifier = spec;
			}
			
			public void actOnClass(BT_Class referencedClass) {
				specifySubClasses(referencedClass);
			}
			
			private void specifySubClasses(BT_Class clazz) {
				BT_ClassVector subclasses = clazz.getKids();
				for(int i=0; i<subclasses.size(); i++) {
					BT_Class subClass = subclasses.elementAt(i);
					interfaceSpecifier.addToInterface(subClass);
					interfaceSpecifier.addTargetedClassToInterface(subClass);
					specifySubClasses(subClass);
				}
			}
		};
		SubclassActor subclassActor = new SubclassActor();
		
		//We load the subclasses last because there are more and more subclasses loaded above.
		//We cannot tell if a class S is a subclass of a class C until it has been loaded, and we
		//cannot load all classes to see which ones may be subclasses of C, so we wait as
		//long as we can before we check for subclasses.
		option = data.includeSubclass;
		specifiers = option.getSpecifiers();
		for(int i=0; i<specifiers.length; i++) {
			Specifier spec = specifiers[i];
			
			
			try {
				if(!setInterfaceSpecifierProxy(conditional, subclassActor, spec)) {
					continue;
				}
				ref.printRule(option, spec.getFullString());
				repository.loadClasses(spec.getIdentifier(), subclassActor, null);
			}
			catch(InvalidIdentifierException e) {
				factory.noteInvalidIdentifier(e.getIdentifier());
			}
			
		}
	}
		
	public void inspect() {
		
		if(data.verify.isFlagged()) {
			verify();
		}

		if(data.printClass.appears()) {
			printClass();
		}
		
		if(data.printMethod.appears()) {
			printMethod();
		}
		
		if(data.optimize.isFlagged()) {
			optimize(true);
		}

		if(data.includeSerialized.isFlagged() || data.includeSerializable.isFlagged() || data.includeExternalized.isFlagged()) {
			BT_ClassVector classes = repository.classes;
			for(int i=0; i<classes.size(); i++) {
				BT_Class clazz = classes.elementAt(i);
				if(data.includeSerialized.isFlagged()) {
					repository.getInternalClassesInterface().addSerialized(clazz, true);
				} else if(data.includeSerializable.isFlagged()) {
					repository.getInternalClassesInterface().addSerialized(clazz, false);
				}
				if(data.includeExternalized.isFlagged()) {
					repository.getInternalClassesInterface().addExternalized(clazz);
				}
			}
		}

		if(data.includeDynamicClassLoad.isFlagged()) {
			repository.getInternalClassesInterface().addDynamicallyAccessed(
					data.reflectionWarnings.isFlagged(), logger, super.messages);
		}
	}
	
	void printMethod() {
		Identifier methodsSpec[] = data.printMethod.getIdentifiers();
		if(methodsSpec.length > 0) {
			BT_MethodVector methodsToPrint = new BT_HashedMethodVector();
			MemberActor actor = new MemberCollectorActor(methodsToPrint, null);
			repository.findMethods(methodsSpec, actor);
			for(int j=0; j<methodsToPrint.size(); j++) {
				BT_Method method = methodsToPrint.elementAt(j);
				method.print(System.out);
			}
		}
	}
	
	void printClass() {
		Identifier spec[] = data.printClass.getIdentifiers();
		BT_ClassVector toPrint;
		if(spec.length > 0) {
			toPrint = repository.findClasses(spec, false);
			for(int j=0; j<toPrint.size(); j++) {
				BT_Class clazz = toPrint.elementAt(j);
				clazz.print(System.out);
			}
		}
		
	}
	
	
}
