package com.ibm.ive.tools.japt.reduction.ita;

import java.util.List;

import com.ibm.ive.tools.japt.Component;
import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.reduction.Counter;
import com.ibm.ive.tools.japt.reduction.GenericReducer;
import com.ibm.ive.tools.japt.reduction.Messages;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 */
public class ITAReducer extends GenericReducer {
	private Messages messages;
	private Component component;
	final PropagationProperties propagationProperties;
	final ContextProperties contextProperties;
	
	
	/**
	 * Constructor for ITAReducer.
	 */
	public ITAReducer(JaptRepository repository, Logger logger, Messages messages, Component component, 
			PropagationProperties propagationProperties, ContextProperties contextProps) {
		super(repository, logger);
		this.messages = messages;
		this.component = component;
		this.propagationProperties = propagationProperties;
		this.contextProperties = contextProps;
	}
	
	protected void reduce() {
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
			 	
			/*
			 * now we apply conditional interface elements
			 */
			Counter counter = new Counter();
			List conds = (List) internalClassesInterface.getBackingConditionalList().clone();
			while(invoker.propagateToConditionalInterfaceElements(conds, counter)) {
				messages.SEEDING.log(logger, new String[] {
						Integer.toString(counter.methodCount), 
						Integer.toString(counter.fieldCount), 
						Integer.toString(counter.classCount)}); 
				while(rep.doPropagation());
				counter.reset();
			}
			 
			rep.removeItems(rep, alterClasses, doNotMakeClassesAbstract);
		} catch(PropagationException e) {
			/* currently only GenericInvocationException is thrown and never with regular reduction */
			System.out.println(e);
		}
	}
}
