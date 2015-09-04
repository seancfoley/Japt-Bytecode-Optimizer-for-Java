/*
 * Created on Oct 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.instrument;

import java.util.HashMap;
import java.util.HashSet;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.MemberSelector;
import com.ibm.ive.tools.japt.Message;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class InstrumentExtension implements CommandLineExtension {

	Messages messages = new Messages(this);
	private String name = messages.DESCRIPTION;
	private SpecifierOption instrumentClasses = new SpecifierOption(messages.INSTRUMENT_CLASS_LABEL, messages.INSTRUMENT_CLASS);
	private SpecifierOption instrumentAccessibleClasses = new SpecifierOption(messages.INSTRUMENT_ACCESSIBLE_CLASS_LABEL, messages.INSTRUMENT_ACCESSIBLE_CLASS);
	private SpecifierOption instrumentMethods = new SpecifierOption(messages.INSTRUMENT_METHOD_LABEL, messages.INSTRUMENT_METHOD);
	private FlagOption instrumentByObject = new FlagOption(messages.INSTRUMENT_BY_OBJECT_LABEL, messages.INSTRUMENT_BY_OBJECT);
	private ValueOption runtimeObserverClassName = new ValueOption(messages.RUNTIME_OBSERVER_CLASS_LABEL, 
			new Message(new FormattedString(messages.RUNTIME_OBSERVER_CLASS)).toString(RuntimeObserver.class.getName()));
	
	/**
	 * 
	 */
	public InstrumentExtension() {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.commandLine.CommandLineExtension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {
				instrumentClasses, 
				instrumentMethods,
				instrumentAccessibleClasses,
				instrumentByObject,
				runtimeObserverClassName};
	}

	
	
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		
		if(!instrumentClasses.appears() && !instrumentMethods.appears()) {
			return;
		}
		InstrumentorHelper helper = new InstrumentorHelper(this, repository, runtimeObserverClassName.getValue(), logger, messages);
		HashMap methodsToInstrument = new HashMap();
		BT_ClassVector classesToInstrument = new BT_ClassVector();
		HashSet failed = new HashSet();
		
		if(instrumentAccessibleClasses.appears()) {
			
			BT_ClassVector classes = new BT_HashedClassVector();
			Specifier[] classSpecifiers = instrumentAccessibleClasses.getSpecifiers();
			for(int j=0; j<classSpecifiers.length; j++) {
				Specifier classSpecifier = classSpecifiers[j];
				try {
					if(!classSpecifier.isConditional() || classSpecifier.conditionIsTrue(repository)) {
						BT_ClassVector found = repository.findClasses(classSpecifier.getIdentifier(), true);
						classes.addAll(found);
					}
				}
				catch(InvalidIdentifierException e) {
					repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
				}
				
			}
			
			for(int i=0; i<classes.size(); i++) {
				BT_Class clazz = classes.elementAt(i);
				if(failed.contains(clazz)) {
					continue;
				}
				if(!canInstrument(clazz, repository, logger, null, helper)) {
					failed.add(clazz);
					continue;
				}
				
				
				if(!classesToInstrument.contains(clazz)) {
					MemberCollectorActor actor = new MemberCollectorActor(new BT_MethodVector(), null);
					actor.actOn(clazz, MemberSelector.publicProtectedNonResolvableMethodSelector);
					if(actor.methods.size() > 0) {
						classesToInstrument.addElement(clazz);
						methodsToInstrument.put(clazz, actor.methods);
					}
				}
			}
		}
		
		if(instrumentClasses.appears()) {
			BT_ClassVector classes = new BT_HashedClassVector();
			Specifier[] classSpecifiers = instrumentClasses.getSpecifiers();
			for(int j=0; j<classSpecifiers.length; j++) {
				Specifier classSpecifier = classSpecifiers[j];
				try {
					if(!classSpecifier.isConditional() || classSpecifier.conditionIsTrue(repository)) {
						BT_ClassVector found = repository.findClasses(classSpecifier.getIdentifier(), true);
						classes.addAll(found);
					}
				}
				catch(InvalidIdentifierException e) {
					repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
				}
				
			}
			
			for(int i=0; i<classes.size(); i++) {
				BT_Class clazz = classes.elementAt(i);
				if(failed.contains(clazz)) {
					continue;
				}
				if(!canInstrument(clazz, repository, logger, null, helper)) {
					failed.add(clazz);
					continue;
				}
				
				
				if(!classesToInstrument.contains(clazz)) {
					classesToInstrument.addElement(clazz);
					methodsToInstrument.put(clazz, clazz.getMethods().clone());
				}
			}
		}
		if(instrumentMethods.appears()) {
			Specifier[] methodSpecifiers = instrumentMethods.getSpecifiers();
			for(int j=0; j<methodSpecifiers.length; j++) {
				Specifier methodSpecifier = methodSpecifiers[j];
				try {
					if(!methodSpecifier.isConditional() || methodSpecifier.conditionIsTrue(repository)) {
						MemberCollectorActor identifierActor = new MemberCollectorActor();
						Identifier ident = methodSpecifier.getIdentifier();
						repository.findMethods(ident, true, identifierActor);
						BT_MethodVector methods = identifierActor.methods;
						
						
						for(int i=0; i<methods.size(); i++) {
							BT_Method meth = methods.elementAt(i);
							BT_Class clz = meth.getDeclaringClass();
							
							if(failed.contains(clz)) {
								continue;
							}
							if(!canInstrument(clz, repository, logger, meth, helper)) {
								failed.add(clz);
								continue;
							}
							
							if(!classesToInstrument.contains(clz)) {
								classesToInstrument.addElement(clz);
								BT_MethodVector v = new BT_MethodVector();
								v.addElement(meth);
								methodsToInstrument.put(clz, v);
							}
							else {
								BT_MethodVector v = (BT_MethodVector) methodsToInstrument.get(clz);
								v.addUnique(meth);
							}
							
						}
					}
				}
				catch(InvalidIdentifierException e) {
					repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
				}
			}
		}
		for(int i=0; i<classesToInstrument.size(); i++) {
			BT_Class clazz = classesToInstrument.elementAt(i);
			ClassInstrumentor ci = new ClassInstrumentor(helper, clazz, instrumentByObject.isFlagged(), logger, messages);
			ci.instrumentClass((BT_MethodVector) methodsToInstrument.get(clazz));
		}
	}
	
	boolean canInstrument(BT_Class clazz, JaptRepository rep, Logger logger, BT_Method meth, InstrumentorHelper helper) {
		
		if(helper.classObserverType.isInstance(clazz)
				|| helper.objectObserverType.isInstance(clazz)
				|| helper.methodObserverType.isInstance(clazz)
				|| helper.specializedRuntimeObserverType.isInstance(clazz)
				) {
			return false;
		}
		
		if(clazz.isBasicTypeClass || clazz.isArray()) {
			return false;
		}
		if(clazz.isInterface()) {
			if(meth == null) {
				messages.CANNOT_INSTRUMENT_CLASS.log(logger, clazz);
			}
			else {
				messages.CANNOT_INSTRUMENT_METHOD_CLASS.log(logger, new Object[] {meth.qualifiedName(), clazz});
			}
			return false;
		}
		if(!rep.isInternalClass(clazz)) {
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}
}
