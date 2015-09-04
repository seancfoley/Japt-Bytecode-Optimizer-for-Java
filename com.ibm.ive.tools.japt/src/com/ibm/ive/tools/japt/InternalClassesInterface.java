/*
 * Created on Jun 2, 2005
 *
 * Specifies the interface to the internal classes, either from the external classes or elsewhere.
 */
package com.ibm.ive.tools.japt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_ConstantStringIns;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Repository;

/**
 * @author sfoley
 *
 * Describes the interface to the internal classes of an application.  The interface consists of
 * all points of access to an application or library from outside the application or library.  For many
 * applications this may simply consist of the main method.  For libraries this may consist of the
 * complete public and protected API.  The use of reflection creates interface elements that are used
 * by the virtual machine itself.  When running a transformation such as obfuscation, reduction or inlining,
 * it is important the the complete interface be specified, so that all points of access to the application or
 * library do not change any of their identifying characteristics.
 */
public class InternalClassesInterface extends InterfaceItemCollection {

	/**
	 * the interface consists of the elements above, and all the conditional interfaces, each of which
	 * also specifies a number of elements BUT those elements only apply when the condition is satisified,
	 * the condition in general being that a prerequisite class, method or field is required and accessed. 
	 */
	private ArrayList conditionals = new ArrayList();  //contains ConditionalInterfaceItemCollection
	private HashSet conditionSet = new HashSet(); //used for a quicker check to see it a BT_Item is listed as one of 
													//the conditions in one of the ConditionalInterfaceItemCollection's
	private JaptRepository repository;
	public static final String serialVersionFieldName = "serialVersionUID";
	
	public InternalClassesInterface(JaptRepository repository) {
		super(repository.getFactory());
		this.repository = repository;
	}
	
	public ArrayList getBackingConditionalList() {
		return conditionals;
	}
	
	public ConditionalInterfaceItemCollection[] getConditionals() {
		return (ConditionalInterfaceItemCollection[]) conditionals.toArray(new ConditionalInterfaceItemCollection[conditionals.size()]);
	}
	
	public void addConditional(ConditionalInterfaceItemCollection cis) {
		conditionals.add(cis);
		conditionSet.addAll(Arrays.asList(cis.getConditionalClasses()));
		conditionSet.addAll(Arrays.asList(cis.getConditionalFields()));
		conditionSet.addAll(Arrays.asList(cis.getConditionalMethods()));
	}
	
	public boolean satisfiesAConditional(BT_Item item) {
		return conditionSet.contains(item);
	}
	
	public boolean isInEntireInterface(BT_Class clazz) {
		if(isInInterface(clazz)) {
			return true;
		}
		ConditionalInterfaceItemCollection[] conds = getConditionals();
		for(int i=0; i<conds.length; i++) {
			if(conds[i].isInInterface(clazz)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInEntireInterface(BT_Method method) {
		if(isInInterface(method)) {
			return true;
		}
		ConditionalInterfaceItemCollection[] conds = getConditionals();
		for(int i=0; i<conds.length; i++) {
			if(conds[i].isInInterface(method)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInEntireInterface(BT_Field field) {
		if(isInInterface(field)) {
			return true;
		}
		ConditionalInterfaceItemCollection[] conds = getConditionals();
		for(int i=0; i<conds.length; i++) {
			if(conds[i].isInInterface(field)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return all classes that are a part of the interface to the internal classes in 
	 * the japt repository, including conditional interface classes
	 */
	public BT_ClassVector getAllInterfaceClasses() {
		BT_ClassVector specs = (BT_ClassVector) super.getInterfaceClasses().clone();
		ConditionalInterfaceItemCollection[] conds = getConditionals();
		for(int i=0; i<conds.length; i++) {
			ConditionalInterfaceItemCollection next = conds[i];
			BT_ClassVector nextClasses = next.getInterfaceClasses();
			for(int j=0; j<nextClasses.size(); j++) {
				specs.addUnique(nextClasses.elementAt(j));
			}
		}
		return specs;
	}
	
	/**
	 * @return all methods that are a part of the interface to the internal classes in 
	 * the japt repository, including conditional interface elements
	 */
	public BT_MethodVector getAllInterfaceMethods() {
		BT_MethodVector specs = (BT_MethodVector) super.getInterfaceMethods().clone();
		ConditionalInterfaceItemCollection[] conds = getConditionals();
		for(int i=0; i<conds.length; i++) {
			ConditionalInterfaceItemCollection next = conds[i];
			BT_MethodVector nexts = next.getInterfaceMethods();
			for(int j=0; j<nexts.size(); j++) {
				specs.addUnique(nexts.elementAt(j));
			}
		}
		return specs;
	}
	
	/**
	 * @return all fields that are a part of the interface to the internal classes in 
	 * the japt repository, including conditional interface elements
	 */
	public BT_FieldVector getAllInterfaceFields() {
		BT_FieldVector specs = (BT_FieldVector) super.getInterfaceFields().clone();
		ConditionalInterfaceItemCollection[] conds = getConditionals();
		for(int i=0; i<conds.length; i++) {
			ConditionalInterfaceItemCollection next = conds[i];
			BT_FieldVector nexts = next.getInterfaceFields();
			for(int j=0; j<nexts.size(); j++) {
				specs.addUnique(nexts.elementAt(j));
			}
		}
		return specs;
	}
	
	public void removeFromInterface(BT_Class clazz) {
		super.removeFromInterface(clazz);
		boolean satisfiesConditional = satisfiesAConditional(clazz);
		for(int i=0; i<conditionals.size(); i++) {
			ConditionalInterfaceItemCollection cond = (ConditionalInterfaceItemCollection) conditionals.get(i);
			cond.removeFromInterface(clazz, satisfiesConditional);
			if(cond.isEmpty() || (satisfiesConditional && cond.hasNoConditions())) {
				conditionals.remove(i);
				i--;
			}
		}
		if(satisfiesConditional) {
			conditionSet.remove(clazz);
		}
	}
	
	public void removeFromInterface(BT_Method meth) {
		super.removeFromInterface(meth);
		boolean satisfiesConditional = satisfiesAConditional(meth);
		for(int i=0; i<conditionals.size(); i++) {
			ConditionalInterfaceItemCollection cond = (ConditionalInterfaceItemCollection) conditionals.get(i);
			cond.removeFromInterface(meth, satisfiesConditional);
			if(cond.isEmpty() || (satisfiesConditional && cond.hasNoConditions())) {
				conditionals.remove(i);
				i--;
			}
		}
		if(satisfiesConditional) {
			conditionSet.remove(meth);
		}
	}
	
	public void removeFromInterface(BT_Field field) {
		super.removeFromInterface(field);
		boolean satisfiesConditional = satisfiesAConditional(field);
		for(int i=0; i<conditionals.size(); i++) {
			ConditionalInterfaceItemCollection cond = (ConditionalInterfaceItemCollection) conditionals.get(i);
			cond.removeFromInterface(field, satisfiesConditional);
			if(cond.isEmpty() || (satisfiesConditional && cond.hasNoConditions())) {
				conditionals.remove(i);
				i--;
			}
		}
		if(satisfiesConditional) {
			conditionSet.remove(field);
		}
	}

	public void addSerialized(BT_Class clazz, boolean generalized) {
//		serializability
		if(!((JaptClass) clazz).isSerializable()) {
			return;
		}
		addToInterface(clazz);
		//in serialized classes the VM looks for methods with specific names
		boolean preservedConstructor = false;
		BT_Class currentClass = clazz;
		do {
			BT_MethodVector methods = currentClass.getMethods();
			for(int i=0; i<methods.size(); i++) {
				BT_Method method = methods.elementAt(i);
				BT_MethodSignature signature = method.getSignature();
				String sig = signature.toString();
				String name = method.getName();
				//the readObject and writeObject methods are also usually private
				if(    (name.equals("writeObject") && sig.equals("(Ljava/io/ObjectOutputStream;)V") )
					|| (name.equals("readObject") && sig.equals("(Ljava/io/ObjectInputStream;)V"))
					|| (name.equals("readResolve") && sig.equals("()Ljava/lang/Object;"))
					|| (name.equals("writeReplace") && sig.equals("()Ljava/lang/Object;"))
					|| (name.equals("readObjectNoData")  && sig.equals("()V"))
					) {
					addToInterface(method);
				}
				//the "closest" non-serializable base class of a serializable
				// class must have a default constructor
				
				//must preserve the contructor of the first non-serializable superclass
				else if(!preservedConstructor 
						&& method.isConstructor() 
						&& sig.equals("()V") 
						&& !((JaptClass) currentClass).isSerializable()) {
					preservedConstructor = true;
					addToInterface(method);
				}
			}
			currentClass = currentClass.getSuperClass();
		} while(currentClass != null);

		BT_FieldVector fields = clazz.getFields();
		boolean preserveAllFields = false;
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			if(preserveAllFields) {
				addToInterface(field);
			}
			else {
				String name = field.getName();
				String type = field.getTypeName();
				//the serialVersionUID field is also usually private but not always
				if(name.equals(serialVersionFieldName) 
						&& field.getFieldType().equals(repository.getLong()) 
						&& field.isStatic() 
						&& field.isFinal()) {
					addToInterface(field);
				}
				else if(name.equals("serialPersistentFields") 
						&& type.equals(BT_Repository.JAVA_IO_OBJECT_STREAM_FIELD_ARRAY)  
						&& field.isStatic() 
						&& field.isFinal()) {
					//we could potentially get the names of the serializable fields by reading the final static string names of the serially
					//persistent fields, but that would be a lot of work, going into the constant pool and so on...
					//so we assume everything is in there instead
					preserveAllFields = true;
					i = -1;
					continue;
				}
				else if(!field.isTransient() 
						&& !field.isStatic()
						&& generalized) {
					//this field is serializable hence the name should not be changed
					addToInterface(field);
				}
				//else: the field is not serialized and is also not used by the serialization process
			}
		}
	}
	
	public void addExternalized(BT_Class clazz) {
		if(!((JaptClass) clazz).isExternalizable()) {
			return;
		}
		addToInterface(clazz);
				
		//in externalized classes the VM looks for methods with specific names
		BT_Class currentClass = clazz;
		do {
			BT_MethodVector methods = currentClass.getMethods();
			for(int i=0; i<methods.size(); i++) {
				BT_Method method = methods.elementAt(i);
				BT_MethodSignature signature = method.getSignature();
				String sig = signature.toString();
				String name = method.getName();
				if((name.equals("readResolve") && sig.equals("()Ljava/lang/Object;"))
					|| (name.equals("writeReplace") && sig.equals("()Ljava/lang/Object;"))
					|| ((name.equals("readExternal") && sig.equals("(Ljava/io/ObjectInput;)V")) 
					|| (name.equals("writeExternal") && sig.equals("(Ljava/io/ObjectOutput;)V")))	
					) {
					addToInterface(method);
				
				}
				// the top level constructor name must be preserved
				else if(currentClass == clazz && method.isConstructor()) {
					addToInterface(method);
				}
			}
			currentClass = currentClass.getSuperClass();
		} while(currentClass != null);

		//in externalizable classes the class itself is responsible for the persistence of fields and not the VM,
		//so we need not concern ourselves with the field names except for the versioning name
		BT_FieldVector fields = clazz.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			String name = field.getName();
			if(name.equals(serialVersionFieldName) 
					&& field.getFieldType().equals(repository.getLong()) 
					&& field.isStatic() 
					&& field.isFinal()) {
				addToInterface(field);
				break;
			}
		}
	}
	
	public void addDynamicallyAccessed(boolean verbose, Logger logger, Messages messages) {
		BT_Class javaLangClass = repository.findJavaLangClass();
		if(javaLangClass != null) {
			//handle occurences of Class.forName
			BT_MethodVector methods = javaLangClass.getMethods();
			for(int i=0; i<methods.size(); i++) {
				BT_Method method = methods.elementAt(i);
				BT_MethodSignature signature = method.getSignature();
				String sig = signature.toString();
				String name = method.getName();
				if(!name.equals("forName")) {
					continue;
				}
				
				
			  	BT_MethodCallSiteVector cs = method.callSites;
			  	for (int j = 0; j < cs.size(); j++) {
			  		BT_MethodRefIns forNameCall = cs.elementAt(j).instruction;
			  		BT_CodeAttribute fromCode = cs.elementAt(j).from;
			  		BT_Method from = fromCode.getMethod();
			  		if (fromCode != null) {
			  			
			  			BT_InsVector inst = fromCode.getInstructions();
			  			BT_Ins ins = null;
			  			int index = inst.indexOf(forNameCall);
			  			if(index - 1 >= 0) {
			  				ins = inst.elementAt(--index);
			  			}
			  			
			  			
						//BT_Ins ins = fromCode.previousInstruction(forNameCall);
						if(sig.equals("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")) { //we have called the forName with two arguments, so skip over the arguments
							//the code below is weak, would need a history visitor to know the right instruction to look at
//							if(ins != null && index >= 0) {
//								ins = inst.elementAt(--index);
//								if(ins != null && index >= 0) {
//									ins = inst.elementAt(--index);
//								}
//							}
							ins = null;
						} 
						
						//else sig.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
						if (ins != null && ins.isConstantIns() && ins instanceof BT_ConstantStringIns) {
							BT_ConstantStringIns ldc = (BT_ConstantStringIns) ins;
			  				BT_Class fixedClass = repository.getClass(ldc.getValue());
			  				if(fixedClass != null) {
			  					ConditionalInterfaceItemCollection cond = 
			  						new ConditionalInterfaceItemCollection(repository, new BT_Method[] {from}, ConditionalInterfaceItemCollection.emptyFields, ConditionalInterfaceItemCollection.emptyClasses);
			  					addConditional(cond);
			  					cond.addToInterface(fixedClass);
			  					cond.addTargetedClassToInterface(fixedClass);
			  					
			  					//also specify the default constructor in case Class.newInstance() is called
			  					BT_MethodVector meths = fixedClass.getMethods();
			  					for(int l=0; l<meths.size(); l++) {
			  						BT_Method m = meths.elementAt(l);
			  						BT_MethodSignature sign = m.getSignature();
			  						if(m.isConstructor()&& sign.types.size() == 0 && sign.returnType.isVoid()) {
			  							addToInterface(m);
			  							break;
			  						}
			  					}
							}
							else {
								if(verbose) {
									messages.INDETERMINATE_CLASS_FORNAME.log(logger, from.useName());
								}
							}
			  			}
			  			else {
							if(verbose) {
								messages.INDETERMINATE_CLASS_FORNAME.log(logger, from.useName());
							}
						}
			  		}
			  		else {
						if(verbose) {
							messages.INDETERMINATE_CLASS_FORNAME.log(logger, from.useName());
						}
					}
					if(verbose) {
						messages.CLASS_OBJECT_ACCESS.log(logger, from.useName());
					}
			  	}
			  	
			}
		}
		BT_Class javaLangObject = repository.findJavaLangObject();
		if(javaLangObject == null) {
			return;
		}
		
		//handle occurences of Object.getClass
		BT_MethodVector methods = javaLangObject.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			String name = method.getName();
			if(!name.equals("getClass")) {
				continue;
			}
			
			BT_MethodCallSiteVector cs = method.callSites;
			for (int j = 0; j < cs.size(); j++) {
				BT_Method from = cs.elementAt(j).getFrom();
				if(verbose) {
					messages.CLASS_OBJECT_ACCESS.log(logger, from.useName());
				}
			}
			break;
		}		  
	}
	
}
