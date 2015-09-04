package com.ibm.ive.tools.japt.reduction.xta;

import java.util.Iterator;
import java.util.List;

import com.ibm.ive.tools.japt.ConditionalInterfaceItemCollection;
import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.reduction.Counter;
import com.ibm.ive.tools.japt.reduction.GenericReducer;
import com.ibm.ive.tools.japt.reduction.Messages;
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
public class XTAReducer extends GenericReducer {

	protected Messages messages;
	
	/**
	 * Constructor for XTAReducer.
	 */
	public XTAReducer(JaptRepository repository, 
			Logger logger, 
			Messages messages) {
		super(repository, logger);
		this.messages = messages;
	}
	
	protected Repository constructRepository(JaptRepository rep) {
		return new Repository(this);
	}
	
	protected void reduce() {
		Repository rep = constructRepository(repository);
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
				Integer.toString(classes.size())}); 
		propagateToInterfaceElements(rep, fields, methods, classes);
		messages.STARTING_PROPAGATION.log(logger);
		int iterationCounter = iterate(rep, 1);
		
		
		/*
		 * now we apply conditional interface elements
		 */
		List conds = (List) internalClassesInterface.getBackingConditionalList().clone();
		Counter counter = new Counter();
		while(propagateToConditionalInterfaceElements(rep, conds, counter)) {
			messages.SEEDING.log(logger, new String[] {
					Integer.toString(counter.methodCount), 
					Integer.toString(counter.fieldCount), 
					Integer.toString(counter.classCount)}); 
			iterationCounter = iterate(rep, iterationCounter);
			counter.reset();
		}
		
		/*
		 * propagation is finished, remove the items that we can remove
		 */
		messages.REMOVING_ITEMS.log(logger);
		removeItems(rep);
	}
	
	private boolean propagateToConditionalInterfaceElements(Repository rep, List conds, Counter counter) {
		int originalSize = conds.size();
		
		top:
		for(int i=0; i<conds.size(); i++) {
			ConditionalInterfaceItemCollection cond = 
				(ConditionalInterfaceItemCollection) conds.get(i);
			BT_Class items[] = cond.getConditionalClasses();
			for(int j=0; j<items.length; j++) {
				BT_Class item = items[j];
				Clazz clazz = rep.getClazz(item);
				if(clazz.isRequired()) {
					propagateInterfaceElements(rep, cond, counter);
					conds.remove(i--);
					continue top;
				}
			}
			BT_Method meths[] = cond.getConditionalMethods();
			for(int j=0; j<meths.length; j++) {
				BT_Method m = meths[j];
				Method method = rep.getMethod(m);
				if(method.isRequired() || repository.methodFulfillsClassRequirements(m, false)) {
					//xx;
					//additionally, how do we represent an object being passed into an entry point (and the objects contained in each field?)
					//methodNameOrFieldName(){Object#field1{Object#f1{Object},f2{Object}},field2{Object},field3{Object}}
					//methodNameOrFieldName(){Object#field1{Object#f1{Object}#f2{Object}}#field2{Object}#field3{Object}}
					//methodNameOrFieldName(){Object.field1{Object.f1{Object}f2{Object}}field2{Object}field3{Object}}
					//are the commas redundant?
					//I think I like the 3rd one, and I htink it works OK
					//TODO make the fields and methods in the interface a target of the conditional method
					//method1 to method2: add method2 as a method called by method1
					//field1 to method1: add method1 as a reading method of field1
					//method1 to field1: add field1 as a written field of method1
					propagateInterfaceElements(rep, cond, counter);
					conds.remove(i--);
					continue top;
				}
				
			}
			BT_Field fields[] = cond.getConditionalFields();
			for(int j=0; j<fields.length; j++) {
				BT_Field f = fields[j];
				Field field = rep.getField(f);
				if(field.isRequired()) {
					//xx;
					//TODO make the methods in the interface a target of the conditional field
					propagateInterfaceElements(rep, cond, counter);
					conds.remove(i--);
					continue top;
				}
			}
		}
		return conds.size() < originalSize;
	}
	
	/**
	 * @param rep
	 * @param cond
	 */
	private void propagateInterfaceElements(Repository rep, ConditionalInterfaceItemCollection cond, Counter counter) {
		BT_FieldVector fs = cond.getInterfaceFields();
		BT_MethodVector ms = cond.getInterfaceMethods();
		BT_ClassVector cls = cond.getInterfaceClasses();
		counter.methodCount += ms.size();
		counter.fieldCount += fs.size();
		counter.classCount += cls.size();
		propagateToInterfaceElements(rep, fs, ms, cls);
	}

	/**
	 * @param rep
	 * @param fields
	 * @param methods
	 * @param classes
	 */
	private void propagateToInterfaceElements(Repository rep, BT_FieldVector fields, BT_MethodVector methods, BT_ClassVector classes) {
		for(int i=0; i<fields.size(); i++) {
			Field field = rep.getField(fields.elementAt(i));
			field.propagateFromUnknownSource();
		}
		for(int i=0; i<methods.size(); i++) {
			Method method = rep.getMethod(methods.elementAt(i));
			method.propagateFromUnknownSource();
		}
		for(int i=0; i<classes.size(); i++) {
			Clazz clazz = rep.getClazz(classes.elementAt(i));
			clazz.setRequired();
		}
	}

	int iterate(Repository rep, int iterationCounter) {
		while(doIteration(rep, iterationCounter)) {
			iterationCounter++;
		}
		return iterationCounter;
	}
	
	protected boolean doIteration(Repository rep, int iterationCounter) {
		boolean propagated = false;
		
		/*
		 * Now we propagate objects from one member to the next until 
		 * everything that can be propagated has been propagated
		 */		
		 Iterator members = rep.getMembers();
		 while(members.hasNext()) {
		 	Propagator member = (Propagator) members.next();
		 	
		 	
		 	
		 	if(member.isPropagationRequired()) {
		 		propagated = true;
		 		member.doPropagation();
		 		int numPropagated = 1;
		 		while(members.hasNext()) {
		 			member = (Propagator) members.next();
		 			if(member.isPropagationRequired()) {
		 				numPropagated++;
		 				member.doPropagation();
		 			}
		 		}
		 		outputIterationMessage(iterationCounter, numPropagated);
				break;
		 	}
	 	}
	 	return propagated;
	}
	
	protected void outputIterationMessage(int iterationCounter, int numPropagated) {
		messages.XTA_ITERATION_INFO.log(logger, new String[] {Integer.toString(iterationCounter), Integer.toString(numPropagated)});
	}
	
	
	class ReductionStats {
		int classesMadeAbstract;
		int classesRemoved;
		int methodsRemoved;
		int methodsImplicitlyRemoved;
		int fieldsImplicitlyRemoved;
		int fieldsRemoved;
		int simplifiedMethods;
		int abstractedMethods;
		int originalClassCount;
		int originalMethodCount;
		int originalFieldCount;
	}
	
	void removeItems(Repository rep) {
		//by cloning the vector, we know that classes will not be removed from it
		//when they are removed from the repository
		BT_ClassVector classes = (BT_ClassVector) repository.getInternalClasses().clone();
		ReductionStats stats = new ReductionStats();
		stats.originalClassCount = classes.size();
		InternalClassesInterface internalClassesInterface = repository.getInternalClassesInterface(); 
		
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if(!rep.contains(clazz)) {
				continue;
			}
			Clazz clz = rep.getClazz(clazz);
			if(alterClasses
					&& !doNotMakeClassesAbstract 
					&& clz.isRequired() 
					&& !clz.isInstantiated() 
					&& !clazz.isInterface() 
					&& !clazz.isAbstract() 
					&& !clazz.isFinal() 
					&& !internalClassesInterface.isInEntireInterface(clazz)
				) {
					messages.MADE_CLASS_ABSTRACT.log(logger, clazz);
					clazz.becomeAbstract();
					stats.classesMadeAbstract++;
			}
		}
		
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if(!rep.contains(clazz) || !rep.getClazz(clazz).isRequired()) {
				/*
				 * Note that in some cases we will be removing a class here that has already
				 * been removed because its superclass was removed, or in the case of an interface
				 * one of its superinterfaces was removed.  This poses no problems though, since
				 * calling the remove method a second time will not have an effect and we would 
				 * like to have the log message and the stats anwyay
				 */
				
				clazz.remove();
				messages.REMOVED_UNUSED_CLASS.log(logger, new Object[] {clazz.kindName(), clazz});
				stats.classesRemoved++;
				stats.methodsImplicitlyRemoved += clazz.getMethods().size();
				stats.fieldsImplicitlyRemoved += clazz.getFields().size();
				continue;
			}
			
			if(alterClasses) {
				BT_MethodVector methods = clazz.getMethods();
				stats.originalMethodCount += methods.size();
				for(int j=0; j<methods.size(); j++) {
					BT_Method method = methods.elementAt(j);
					if(removeMethod(stats, rep, method, clazz)) {
						j--;
					}
				}
	
				BT_FieldVector fields = clazz.getFields();
				stats.originalFieldCount += fields.size();
				for(int j=0; j<fields.size(); j++) {
					BT_Field field = fields.elementAt(j);
					if(rep.contains(field) && rep.getField(field).isRequired()) {
						continue;
					}
					field.remove();
					messages.REMOVED_UNUSED_FIELD.log(logger, field.useName());
					stats.fieldsRemoved++;
					j--;
				}
			}
			
		}
		
		messages.SUMMARY.log(logger, new String[] {
			Integer.toString(stats.classesRemoved),
			Integer.toString(stats.fieldsImplicitlyRemoved),
			Integer.toString(stats.methodsImplicitlyRemoved),
			Integer.toString(stats.classesMadeAbstract),
			Integer.toString(stats.fieldsRemoved),
			Integer.toString(stats.originalFieldCount),
			Integer.toString(stats.methodsRemoved),
			Integer.toString(stats.originalMethodCount),
			Integer.toString(stats.simplifiedMethods),
			Integer.toString(stats.abstractedMethods)});
	}
	
	
	
	boolean removeMethod(ReductionStats stats, Repository rep, BT_Method method, BT_Class declaringClass) {
		Method mtd;

		//when a class is set as initialized it marks its static initializer as accessed,
		//so no need to make any special arrangements for static intializers

		
		//if the method has been marked required in the repository, or it must be kept for other reasons
		if(rep.contains(method) && (mtd = rep.getMethod(method)).isRequired()) {
			if(mtd.isAccessed() || method.isAbstract() || method.isNative() || method.isStub()) {
				return false;	
			}
			
			if(!declaringClass.isAbstract()
				|| declaringClass.isFinal() //a final class cannot become abstract
				|| method.isFinal() //a final method cannot become abstract
				|| method.isStatic() //static methods cannot become abstract
				|| repository.methodFulfillsClassRequirements(method, false) //the method must remain because it overrides an abstract method
														//or implements an interface method, in which case it cannot become abstract
				) {
					if(!method.simplyReturns()) {
						stats.simplifiedMethods++;
						method.makeCodeSimplyReturn();
						messages.REMOVED_CODE_FROM_METHOD.log(logger, method.useName());
					}
			} else {
				stats.abstractedMethods++;
				method.removeCode();
				messages.MADE_METHOD_ABSTRACT.log(logger, method.useName());
			}
			
		}
		//we have some rather convoluted code to determine whether a method must remain
		//because it prevents an instantiated class from containing an abstract method (ie implements an
		//interface method or overrides an abstract superclass method)
		else if(repository.methodFulfillsClassRequirements(method, true)) {//TODO methodFulfillsClassRequirements can make an abstract method non-abstract, so we should log that here if it happens
			if(!method.isNative() && !method.isStub() && !method.simplyReturns()) {//no sense in altering the code attribute if there is no code attribute
				stats.simplifiedMethods++;
				method.makeCodeSimplyReturn();
				messages.REMOVED_CODE_FROM_METHOD.log(logger, method.useName());
			}
		}
		else {		
			method.remove();
			messages.REMOVED_UNUSED_METHOD.log(logger, method.useName());
			stats.methodsRemoved++;
			return true;
		}
		return false;
	}

	
}
