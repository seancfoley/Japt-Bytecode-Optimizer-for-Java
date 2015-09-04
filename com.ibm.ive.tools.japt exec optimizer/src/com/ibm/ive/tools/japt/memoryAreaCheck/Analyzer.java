package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.reduction.EntryPointLister;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.ive.tools.japt.reduction.ita.ContextProperties;
import com.ibm.ive.tools.japt.reduction.ita.EntryPointInvoker;
import com.ibm.ive.tools.japt.reduction.ita.PropagationException;
import com.ibm.ive.tools.japt.reduction.ita.PropagationProperties;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 */
public class Analyzer {
	private Messages messages;
	private Component component;
	final PropagationProperties propagationProperties;
	final ContextProperties contextProperties;
	protected final Logger logger;
	public final JaptRepository repository;
	protected EntryPointLister entryPointLister;
	
	
	public Analyzer(JaptRepository repository, Logger logger, Messages messages, Component component, 
			PropagationProperties propagationProperties, ContextProperties contextProperties) {
		this.messages = messages;
		this.component = component;
		this.propagationProperties = propagationProperties;
		this.contextProperties = contextProperties;
		this.logger = logger;
		this.repository = repository;
	}
	
	public Repository analyze() {
		Repository rep = new Repository(propagationProperties, contextProperties, repository, messages, logger, component);
		rep.entryPointLister = entryPointLister;
		InternalClassesInterface internalClassesInterface = repository.getInternalClassesInterface();
		
		/*
		 * Initialize by setting the included methods and fields as seeds 
		 */
		BT_FieldVector fields = internalClassesInterface.getInterfaceFields();
		BT_MethodVector methods = internalClassesInterface.getInterfaceMethods();
		BT_ClassVector classes = internalClassesInterface.getInterfaceClasses();
		
		messages.SEEDING.log(logger, new String[] {
				Integer.toString(methods.size()), 
				Integer.toString(fields.size()),
				Integer.toString(classes.size())
				}); 
		
		EntryPointInvoker invoker = new EntryPointInvoker(rep, repository, propagationProperties);
		invoker.propagateToInterfaceElements(fields, methods, classes);

		messages.STARTING_PROPAGATION.log(logger);
		
		/*
		 * Now we propagate objects from one member to the next until 
		 * everything that can be propagated has been propagated
		 */
		 
		try {
			while(rep.doPropagation());
		} catch(PropagationException e) {
			/* currently only GenericInvocationException is thrown and never with regular reduction */
			System.out.println(e);
		}
		rep.doCount();
		return rep;
	}
	
}
