package com.ibm.ive.tools.japt.reduction;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.ive.tools.japt.reduction.bta.BasicReducer;
import com.ibm.ive.tools.japt.reduction.ita.ContextProperties;
import com.ibm.ive.tools.japt.reduction.ita.DefaultInstantiatorProvider;
import com.ibm.ive.tools.japt.reduction.ita.ITAReducer;
import com.ibm.ive.tools.japt.reduction.ita.PropagationProperties;
import com.ibm.ive.tools.japt.reduction.rta.RTAReducer;
import com.ibm.ive.tools.japt.reduction.xta.XTAReducer;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 */
public class ReductionExtension implements com.ibm.ive.tools.japt.commandLine.CommandLineExtension {

	protected Messages messages = new Messages(this);
	private String name = messages.DESCRIPTION;
	
	public SpecifierOption removeSubclasses = new SpecifierOption(messages.REMOVE_SUBCLASS_LABEL, messages.REMOVE_SUBCLASS);
	public SpecifierOption removeClass = new SpecifierOption(messages.REMOVE_CLASS_LABEL, messages.REMOVE_CLASS);
	
	public SpecifierOption removeMethod = new SpecifierOption(messages.REMOVE_METHOD_LABEL, messages.REMOVE_METHOD);
	public SpecifierOption removeField = new SpecifierOption(messages.REMOVE_FIELD_LABEL, messages.REMOVE_FIELD);
	
	//the four reduction algorithms, in ascending order of complexity and power
	public FlagOption bta = new FlagOption(messages.BASIC_LABEL, messages.BASIC);
	public FlagOption rta = new FlagOption(messages.RTA_LABEL, messages.RTA);
	public FlagOption xta = new FlagOption(messages.XTA_LABEL, messages.XTA);
	public FlagOption ita = new FlagOption(messages.ITA_LABEL, messages.ITA);
	
	public FlagOption doNotMakeClassesAbstract = new FlagOption(messages.NO_MAKE_CLASSES_ABSTRACT_LABEL, messages.NO_MAKE_CLASSES_ABSTRACT);
	public FlagOption noRemoveUnused = new FlagOption(messages.NO_REMOVE_UNUSED_LABEL, messages.NO_REMOVE_UNUSED);
	
	public FlagOption noMarkEntryPoints = new FlagOption(messages.NO_MARK_ENTRY_LABEL, messages.NO_MARK_ENTRY);
	
	public FlagOption noAlterClasses = new FlagOption(messages.NO_ALTER_LABEL, messages.NO_ALTER);
	public ValueOption entryPointFile = new ValueOption(messages.ENTRY_POINT_LABEL, messages.ENTRY_POINT);
	
	public FlagOption removesNotResolvable = new FlagOption(messages.REMOVES_NOT_RESOLVABLE_LABEL, messages.REMOVES_NOT_RESOLVABLE);
	
	public ReductionExtension() {}

//	//TODO need to have a way of spacifying "links" ie one jni call goes into native which goes into some other java method
//	//for xta and rta this was handled weakly by interface specifiers, must link methods to methods/fields
//	//"pass objects from invocations of named method to instances of named field or invocations of named method"
//	//acually, can just use the conditional specifiers!  
//	
//	//TODO need to have a way of specifying ita object inputs (also applies to xta and rta actually)
//	//Use the interface specifiers (hopefully not too ugly) by appending stuff to them
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#getName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @see com.ibm.ive.tools.japt.Extension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {
			noRemoveUnused, removeClass, removeMethod, removeField, 
			removeSubclasses, xta, ita, rta, bta, doNotMakeClassesAbstract,
			noMarkEntryPoints, entryPointFile, noAlterClasses, removesNotResolvable};
	}

	/**
	 * @see com.ibm.ive.tools.japt.Extension#execute(JaptRepository, Logger)
	 */
	public void execute(JaptRepository repository, Logger logger) throws ExtensionException {
		JaptFactory factory = repository.getFactory();
		if(!noRemoveUnused.isFlagged()) {
			if(factory.getNotLoadedClassCount() > 0 || factory.getNotFoundClassCount() > 0) {
				messages.MISSING_CLASSES.log(logger, Integer.toString(factory.getNotLoadedClassCount() + factory.getNotFoundClassCount()));
			}
			GenericReducer reducer = createReducer(repository, logger); 
			reducer.doNotMakeClassesAbstract = doNotMakeClassesAbstract.isFlagged();
			boolean markEntryPoints = !noMarkEntryPoints.isFlagged();
			PrintStream entryPointStream = null;
			if(entryPointFile.appears()) {
				String fileName = entryPointFile.getValue();
				try {
					entryPointStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
					messages.CREATING_ENTRY_POINT_FILE.log(logger, fileName);
				}
				catch(IOException e) {
					messages.COULD_NOT_OPEN_FILE.log(logger, fileName);
				}
			}
			if(entryPointStream != null || markEntryPoints) {
				reducer.entryPointLister = new EntryPointLister(entryPointStream, markEntryPoints, repository);
			}
			reducer.alterClasses = !noAlterClasses.isFlagged();
			reducer.reduce();
			if(entryPointStream != null) {
				entryPointStream.close();
			}
		}
		removeExplicitlyExcluded(repository, logger);
	}
	
	/**
	 * @param repository
	 * @param logger
	 * @return
	 */
	protected GenericReducer createReducer(JaptRepository repository, Logger logger) {
		if(bta.isFlagged()) {
			return new BasicReducer(repository, logger, messages);
		} 
		if(rta.isFlagged()) {
			return new RTAReducer(repository, logger, messages);
		} 
		if(ita.isFlagged()) { //ITA might eventually become the default
			ClassProperties classProps = new ClassProperties(repository);
			ContextProperties contextProperties = new ContextProperties();
			PropagationProperties props = 
				new PropagationProperties(
						PropagationProperties.REACHABILITY_ANALYSIS,
						contextProperties.new SimpleContextProvider(),
						new DefaultInstantiatorProvider(classProps),
						classProps);
			props.verboseIterations = true;
			props.setIntraProceduralAnalysis(true);
			return new ITAReducer(repository, logger, messages, this, props, contextProperties);
		}
		/* xta.isFlagged() is default */
		return new XTAReducer(repository, logger, messages);
	}

	void removeExplicitlyExcluded(JaptRepository repository, Logger logger) {
		
		Specifier classesToRemove[] = removeClass.getSpecifiers();
		for(int i=0; i<classesToRemove.length; i++) {
			Specifier specifier = classesToRemove[i];
			try {
				if(!specifier.isConditional() || specifier.conditionIsTrue(repository)) {
					removeClasses(repository, logger, specifier.getIdentifier(), false);
				}
			}
			catch(InvalidIdentifierException e) {
				repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		classesToRemove = removeSubclasses.getSpecifiers();
		for(int i=0; i<classesToRemove.length; i++) {
			Specifier specifier = classesToRemove[i];
			try {
				if(!specifier.isConditional() || specifier.conditionIsTrue(repository)) {
					removeClasses(repository, logger, specifier.getIdentifier(), true);
				}
			}
			catch(InvalidIdentifierException e) {
				repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
		
		//You might be wondering why we simply don't use an actor that
		//removes the methods and fields that need removing?
		//In the process or removing them, the class method vectors
		//and field vectors will shrink, thus preventing the
		//actor from working properly.
		//So instead we generate a list and remove them afterwards.
		
		MemberCollectorActor actor = new MemberCollectorActor();
		Specifier methodsToRemoveSpec[] = removeMethod.getSpecifiers();
		for(int i=0; i<methodsToRemoveSpec.length; i++) {
			//essentially, we ensure that the identifier contained in this specifier is resolvable as desired...
			Specifier specifier = methodsToRemoveSpec[i];
			specifier.setResolvable(!removesNotResolvable.isFlagged());
			repository.findMethods(specifier, actor);
			BT_MethodVector methodsToRemove = actor.methods;
			for(int j=0; j<methodsToRemove.size(); j++) {
				BT_Method method = methodsToRemove.elementAt(j);
				if(repository.isInternalClass(method.getDeclaringClass())) {
					method.remove();
					messages.REMOVED_METHOD.log(logger, method.useName());
				}
			}
			methodsToRemove.removeAllElements();
		}
		
		Specifier fieldsToRemoveSpec[] = removeField.getSpecifiers();
		for(int i=0; i<fieldsToRemoveSpec.length; i++) {
			Specifier fieldId1 = fieldsToRemoveSpec[i];
			fieldId1.setResolvable(!removesNotResolvable.isFlagged());
			repository.findFields(fieldId1, actor);
			BT_FieldVector fieldsToRemove = actor.fields;
			for(int j=0; j<fieldsToRemove.size(); j++) {
				BT_Field field = fieldsToRemove.elementAt(j);
				if(repository.isInternalClass(field.getDeclaringClass())) {
					field.remove();
					messages.REMOVED_FIELD.log(logger, field.useName());
				}
			}
			fieldsToRemove.removeAllElements();
		}			
	}
	
	private void removeClasses(JaptRepository repository, Logger logger, Identifier classSpecification, boolean children) {
		try {
			BT_ClassVector candidates = repository.findClasses(classSpecification, false);
			for(int j=0; j<candidates.size(); j++) {
				if(children) {
					//note that no recursion here is necessary since removing a class recursively removes its subclasses
					BT_Class clazz = candidates.elementAt(j);
					BT_ClassVector kids = clazz.getKids();
					for(int k=0; k < kids.size(); k++) {
						BT_Class kid = kids.elementAt(k);
						if(!clazz.isInterface() || kid.isInterface()) {
							removeClass(repository, logger, kid);
						}
					}
				}
				else {
					removeClass(repository, logger, candidates.elementAt(j));
				}
			}
		}
		catch(InvalidIdentifierException e) {
			repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
		}
	}

	private void removeClass(JaptRepository repository, Logger logger, BT_Class candidate) {
		if(repository.isInternalClass(candidate)) {
			candidate.remove();
			messages.REMOVED_CLASS.log(logger, new Object[] {candidate.kindName(), candidate});
		}
	}

	
}

/*
 TODO:

-remove copyright stuff, including the copyright class
-see email exchange below


Sean Foley/Ottawa/IBM
05/21/2008 01:43 PM

Default custom expiration date of 05/21/2009	To	Berthold Lebert/Phoenix/IBM@IBMUS
cc	
bcc	
	
Subject	Re: Fw: Development Requirement - J9/DesktopEE: Use JAPT to reduce size and increase performance
	
 


Doesn't the -removeAllThrows, -removeRedundantThrows and -removeRedundantInterfaces  go more in line with -removeDebugInfo, -removeAttribute?
There is a connection.  The exception declarations are contained within the "Exceptions" attribute. 

 I was always confused why the -removeAttribute style options are under -jarOutput and -dirOutput?
The only things removed by the output extensions are attributes (-removeDebugInfo involves removing a set of attributes).  
I guess that it was two reasons this ended up in -jarOutput and -dirOutput:
1. because the operation is so simple, an attribute is a specific element in the class file that can be removed instantaneously.
2. because most attributes are optional and are not a part of the classes/methods/fields/code, or the normal operation of the virtual machine.  There are a couple of exceptions though.  The code itself is stored as an attribute, the "Code" attribute.  Most attributes are optional to some degree, including the "Exceptions" attribute.

They are listed at:
http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#43817
new to Java 5: http://java.sun.com/docs/books/jvms/second_edition/ClassFileFormat-Java5.pdf
new to Java 6: http://java.sun.com/javase/6/webnotes/adoption/adoptionguide.html#4.1.1
also there may be a few that are specific to JME such as the CLDC StackMap attribute.

I can see your point, it makes more sense to move them to -reduce also.  I have to re-orient myself to think of these things from the persepective of a user, rather than a developer.


Sean Foley
J9 Real-Time Java Ottawa Technical Lead
Sean_Foley@ca.ibm.com (613)356-5012



Berthold Lebert/Phoenix/IBM@IBMUS
05/21/2008 12:54 PM	
To	Sean Foley/Ottawa/IBM@IBMCA
cc	
Subject	Re: Fw: Development Requirement - J9/DesktopEE: Use JAPT to reduce size and increase performance(1)

	
 

Yes, no doubt it makes no sense right now.  I am not fond of the word refactor because almost every japt extension does refactoring, and therefore it would be confusing to any new developers or users, so I wanted something specific.  In fact, I think we should move the -removeRedundantThrows and -removeRedundantExceptions to the -reduce extension (both are essentially class file reductions, where you remove unused/unecessary stuff).  I'd like to keep the inner class stuff in its own extension, and call it -mergeInner perhaps.    

Doesn't the -removeAllThrows, -removeRedundantThrows and -removeRedundantInterfaces  go more in line with -removeDebugInfo, -removeAttribute?  I was always confused why the -removeAttribute style options are under -jarOutput and -dirOutput, so maybe it should be the other way around and all -remove should go to -reduce?

Should we make -removeRedundantThrows and -removeRedundantInterfaces default options? 

- Berthold
____________________________________________________________________________________
Berthold M. Lebert | Embedded Java | IBM Phoenix | +1-602-217-2576 | berthold_lebert@us.ibm.com




From:	Sean Foley/Ottawa/IBM@IBMCA
To:	Berthold Lebert/Phoenix/IBM@IBMUS
Date:	05/20/2008 02:24 PM
Subject:	Re: Fw: Development Requirement - J9/DesktopEE: Use JAPT to reduce size and increase performance



Also, I would like the extension option be name -refactor instead of  -refactorInner . It seems akward to use -refactorInner -removeRedundantThrows. 

Yes, no doubt it makes no sense right now.  I am not fond of the word refactor because almost every japt extension does refactoring, and therefore it would be confusing to any new developers or users, so I wanted something specific.  In fact, I think we should move the -removeRedundantThrows and -removeRedundantExceptions to the -reduce extension (both are essentially class file reductions, where you remove unused/unecessary stuff).  I'd like to keep the inner class stuff in its own extension, and call it -mergeInner perhaps.    

So then to apply the inner class stuff, you would just use -mergeInner.

For the others, you would do -reduce -redundantThrows -redundantExceptions.

And the -refactorInnerVerbose should be using a common verbose option, right?

Yes, I'd prefer to not have -refactorInnerVerbose at all, I would just like any verbose information to go to the log file, and to use the common japt output options.  The verbose stuff should just be targeted for the log file I think.

Sean Foley
J9 Real-Time Java Ottawa Technical Lead
Sean_Foley@ca.ibm.com (613)356-5012





Also, I would like the extension option be name -refactor instead of  -refactorInner . It seems akward to use -refactorInner -removeRedundantThrows. And the -refactorInnerVerbose should be using a common verbose option, right?

- Berthold
____________________________________________________________________________________
Berthold M. Lebert | Embedded Java | IBM Phoenix | +1-602-217-2576 | berthold_lebert@us.ibm.com

 
 */

