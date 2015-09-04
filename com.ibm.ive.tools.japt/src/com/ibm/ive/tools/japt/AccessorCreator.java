/*
 * Created on May 4, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.util.ArrayList;

import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassReferenceSite;
import com.ibm.jikesbt.BT_ClassReferenceSiteVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_InstantiationLocator;
import com.ibm.jikesbt.BT_ItemReference;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_StackPool;
import com.ibm.jikesbt.BT_ConstructorLocator.Initialization;
import com.ibm.jikesbt.BT_ConstructorLocator.Instantiation;

public class AccessorCreator implements AccessPreserver {
	
	private final BT_Class oldFrom;
	private final BT_Class newFrom;
	private final BT_Method fromMethod;
	private final JaptRepository repository;
	private final BT_StackPool stackPool;
	
	private ArrayList accessors;
	
	public AccessorCallSiteVector accessedAccessors = new AccessorCallSiteVector();
	
	public AccessorCreator(BT_Class newFrom, BT_Class oldFrom, BT_Method fromMethod) {
		this(newFrom, oldFrom, fromMethod, new BT_StackPool());
	}
	
	public AccessorCreator(BT_Class newFrom, BT_Class oldFrom, BT_Method fromMethod, BT_StackPool stackPool) {
		this.newFrom = newFrom;
		this.oldFrom = oldFrom;
		this.fromMethod = fromMethod;
		this.stackPool = stackPool;
		repository = (JaptRepository) oldFrom.getRepository();
	}
	
	public boolean canChangeClass(BT_Class clazz) {
		boolean isInternal = repository.isInternalClass(clazz);
		if(isInternal) {
			JaptClass japtClass = (JaptClass) clazz;
			if(japtClass.isSerializable()) {
				//TODO maybe make this behaviour configurable??
				//adding an accessor changes the auto-generated serial version uid of a serializable class, 
				//if the class has not generated a serial version uid of its own
				if (clazz.findFieldOrNull(InternalClassesInterface.serialVersionFieldName, repository.getLong()) == null) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	/* 
	 * New array instructions can be treated like any other class reference instruction, such
	 * as checkcast and instanceof.
	 */
	
	public boolean preserveAccess(BT_CodeAttribute code, BT_InsVector ignoreInstructions) throws BT_CodeException {
		//First we take care of constructor accessors, then we take care of the rest
		Instantiation instantiations[] = BT_InstantiationLocator.findInstantiations(fromMethod, stackPool);
		//boolean failed = false;
		for(int i=0; i<instantiations.length; i++) {
			Instantiation instantiation = instantiations[i];
			//if(!failed) {
				BT_Ins newIns = instantiation.creationSite.getInstruction();
				BT_Ins constructorIns = instantiation.site.getInstruction();
				boolean ignoreNew = ignoreInstructions.contains(newIns);
				boolean ignoreConstructor = ignoreInstructions.contains(constructorIns);
				if(ignoreNew || ignoreConstructor) {
					if(!ignoreNew || !ignoreConstructor) {
						return false;
						//failed = true;
					}
				} else { 
					MigratingReference newReference = new MigratingReference(instantiation.creationSite, newFrom);
					MigratingReference constructorReference = new MigratingReference(instantiation.site, newFrom);
					if(newReference.isAccessible() && constructorReference.isAccessible()) {
						ignoreInstructions.addElement(newIns);
						ignoreInstructions.addElement(constructorIns);
					} else if(preserveAccess(newReference, constructorReference, instantiation)) {
						//TODO add any other instructions as necessary that should now be ignored
						ignoreInstructions.addElement(newIns);
						ignoreInstructions.addElement(constructorIns);
					} else {
						return false;
						//failed = true;
					} 
				}
			//}
			//instantiation.returnStacks();
		}
		//if(failed) {
		//	return false;
		//}
		
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		BT_ClassReferenceSiteVector referencedClasses = code.referencedClasses;
		BT_AccessorVector accessedFields = code.accessedFields;
		
		for (int i = 0; i < referencedClasses.size(); i++) {
			BT_ClassReferenceSite classSite = referencedClasses.elementAt(i);
			if(!preserveAccess(classSite, ignoreInstructions)) {
				return false;
			}
		}
		
		for(int i=0; i<accessedFields.size(); i++) {
			BT_Accessor accessor = accessedFields.elementAt(i);
			if(!preserveAccess(accessor, ignoreInstructions)) {
				return false;
			}
		}
		
		for(int i=0; i<calledMethods.size(); i++) {
			BT_MethodCallSite site = calledMethods.elementAt(i);
			if(!preserveAccess(site, ignoreInstructions)) {
				return false;
			}
		}
		return true;
	
	}
	
	public boolean preserveAccess(
			BT_ItemReference newReference,
			BT_ItemReference constructorReference,
			Initialization initialization,
			BT_InsVector ignoreInstructions) {
		return ignoreInstructions.contains(newReference.getInstruction()) && ignoreInstructions.contains(constructorReference.getInstruction());
	}
	
	public boolean preserveAccess(BT_ItemReference reference, BT_InsVector ignoreInstructions) {
		return ignoreInstructions.contains(reference.getInstruction())
			|| preserveAccess(new MigratingReference(reference, newFrom));
	}
	
	public boolean preserveAccess(
			MigratingReference newReference,
			MigratingReference constructorReference,
			Initialization initialization) {
		//TODO constructor accessors - use the stackShapes in initialization to analyze the stack changes,
		//and in fact add stuff to BT_StackShapeVisitor if required in order to see what has changed
		//Note that unlike the method below, when this method has been called the permissions have already been checked,
		//BUT with the MigratingReference class the way it is it does not hurt to check them again for consistency
		
		//Note that it may be difficult to determine how to alter the code to replace the new/constructor pair with the
		//accessor call
		
		
		/*
		 How to use constructor accessors:
 
How the accessor works on class C for constructor D:
 
signature matches D's signature
 
new C
dup
for(int i=0; i<args.size; i++)
aload arg i
}
invokespecial D
return C
 
 
 
How to replace the instantiation with the accessor call:
 
1. Check that uninit objects cannot be merged...  Or at least if they are merged they are exactly equal by reference.  Otherwise we will have trouble figuring out how to identify the object uninit in the stacks and locals. 
 They cannot be merged in the sense that they will have to both refer to the same instruction to be merged.
 But they might not be equal by reference (although they probably will be, because they are only created when an instruction is visited, so when they are merged the same will always remain), 
 although the instruction inside will always be equal by reference when they are merged, so compare the instruction inside.
 
2. At each constructor site for an instantiation (unlikely there is more than one, but might have to account for this):
 
a. the site of the constructor call will become the site of the accessor call.  the accessor will exist with the class that is instantiated, and it will be static.  The uninit object will not be consumed, instead it must be swapped and popped after the accessor call. See 3 for more details about removing all operations on the uninit object, which will include the swap and pop. 
 
b. must reconstruct the locals to replace each uninit (easy, just dup and storelocal for each)
 
c. must reconstruct the stack to replace each uninit.  For stuff close to the top, can use dup, dup_x1, dup_x2, but only if I have already removed occurrences of the uninit.  Otherwise, must use a new set of locals.  If the optimizer works properly, then no worries, these stores will eventually be removed.  First astore-aload pairs are replaced by dup-astore.  then the astore is removed becausre there is not sorresponding load. 
 
3. Must remove the occurrences of the uninit - in all instructions from the new to the constructor and any new instructions inserted after.  Must use a visitor to acomlish this.  (To do this, must change the optimizer methods to recognize an uninit that is never constructed, or must do it explicitly).  Replace the new instruction with a special aconst_null that creates a particular null value.  
 
4.  This step is optional but desirable:  Then must remove any instructions that move the special null around.  This includes instructions that consume the special null off the stack.  We can ignore methods and field ops, also class ref instructions, arithmetic instructions, and so on.  Creating a new visitor that removes unused stack items might do the trick. 
 
		 */
		return false; 
	}
	
	public boolean preserveAccess(MigratingReference reference) {
		if(reference.siteWritesToFinal()) {
			/* 
			 * if the site writes to a final field then it cannot be moved
			 * outside of the initializer in which it resides, and we cannot 
			 * create an accessor for it.
			 */
			return false;
		}
		boolean siteClassIsVisible = reference.siteClassIsVisible();
		if(reference.isClassReference() && !siteClassIsVisible) {
			/* 
			 * currently we have no accessors for class references such as instanceof and checkcast,
			 * although this could be changed.
			 */
			return false;
		}
		boolean siteIsVisible = reference.siteIsUnconditionallyVisible();
		boolean siteIsConstructor = reference.siteIsConstructorInvocation();
		if(siteIsConstructor && !(siteClassIsVisible && siteIsVisible)) {
			/*  constructor accessors need to have been created already in conjunction with their respective new instructions */
			return false;
		}
		BT_Class throughClass = reference.site.getClassTarget();
		if(reference.siteIsSpecialInvocation()) { /* note that at this point we know it is not a constructor invocation, 
												and that migrations by definition occur from one class to another distinct class  */
			
			/* Note: for super invocations, access must go through oldFrom, and for private invocations oldFrom == throughClass */
			if(canChangeClass(oldFrom)) {
				useAccessorFor(new AccessorSpecification(oldFrom, reference));
			} else {
				/* we cannot create the required accessor */
				return false;
			}
		} else if(!siteClassIsVisible || !siteIsVisible) {
			if(canChangeClass(throughClass)) {
				useAccessorFor(new AccessorSpecification(throughClass, reference));
			} else if(canChangeClass(oldFrom) && reference.siteIsStaticReference()) {
				useAccessorFor(new AccessorSpecification(oldFrom, reference));
			} else {
				/* we cannot create the required accessor */
				return false;
			}
		} 
		return true;
	}
	
	static class AccessorSpecification {
		BT_Class accessorClass;
		MigratingReference reference;
		
		AccessorSpecification(BT_Class accessorClass, MigratingReference reference) {
			this.accessorClass = accessorClass;
			this.reference = reference;
		}
		
		public String toString() {
			return " specification of " + accessorClass + " accessing " + reference;
		}
	}
	
	private void useAccessorFor(AccessorSpecification accessorSpec) {
		if(accessors == null) {
			BT_ItemReference site = accessorSpec.reference.site;
			accessors = new ArrayList(site.from.accessedFields.size() + site.from.calledMethods.size() 
					/* no resizes will be required */);
		}
		accessors.add(accessorSpec);
	}
	
	public boolean changesAreRequired() {
		return accessors != null && accessors.size() > 0;
	}
	
	public void doChanges() {
		if(accessors != null) {
			JaptFactory factory = repository.getFactory();
			for(int i=0; i<accessors.size(); i++) {
				AccessorSpecification accessorSpec = (AccessorSpecification) accessors.get(i);
				AccessorMethod accessorMethod = useAccessor(accessorSpec);
				factory.noteAccessorUsed(accessorMethod, newFrom);
			}
			accessors = null;
		}
	}
	
	private AccessorMethod useAccessor(AccessorSpecification accessorSpec) {
		MigratingReference ref = accessorSpec.reference;
		BT_ItemReference site = ref.site;
		BT_Class accessingClass = accessorSpec.accessorClass;
		AccessorMethodGenerator generator = repository.createAccessorMethodGenerator(accessingClass);
		AccessorMethod accessorMethod;
		BT_Class targetClass = site.getClassTarget();
		
		if(ref.siteIsSpecialInvocation()) {
			BT_MethodCallSite callSite = (BT_MethodCallSite) site;
			BT_Method targetMethod = callSite.getTarget();
			accessorMethod = generator.getSpecialMethodAccessor(targetMethod, targetClass);
		} else {
			if(site.isMethodReference()) {
				BT_MethodCallSite callSite = (BT_MethodCallSite) site;
				BT_Method targetMethod = callSite.getTarget();
				accessorMethod = generator.getMethodAccessor(targetMethod, targetClass);
			} else /* field reference, since we do not have accessors for class references */ {
				BT_Accessor accessor = (BT_Accessor) site;
				BT_Field targetField = accessor.getTarget();
				accessorMethod = accessor.isFieldRead() ? 
						generator.getFieldGetter(targetField, targetClass) 
						: generator.getFieldSetter(targetField, targetClass);
			}
		}
		changeTarget(site, accessorMethod);
		return accessorMethod;
	}
	
	private void changeTarget(BT_ItemReference accessor, AccessorMethod accessorMethod) {
		BT_Method newTarget = accessorMethod.method;
		BT_MethodRefIns newInstruction = changeTarget(accessor, newTarget);
		AccessorCallSite site = new AccessorCallSite(newTarget.findCallSite(newInstruction), accessorMethod);
		accessedAccessors.addElement(site);
	}
	
	public BT_MethodRefIns changeTarget(BT_ItemReference accessor, BT_Method method) {
		BT_CodeAttribute accessorCode = accessor.from;
		BT_MethodRefIns newInstruction = (BT_MethodRefIns) BT_Ins.make(method.getOpcodeForInvoke(), method);
		accessorCode.replaceInstructionWith(accessor.getInstruction(), newInstruction);
		return newInstruction;
	}
}
