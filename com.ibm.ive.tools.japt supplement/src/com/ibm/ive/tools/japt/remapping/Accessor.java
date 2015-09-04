/*
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.remapping;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Accessor;
import com.ibm.jikesbt.BT_AccessorVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldRefIns;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_InsVector;
import com.ibm.jikesbt.BT_Opcodes;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Accessor {

	BT_Accessor site;
	BT_CodeAttribute owner;
	boolean overridePermissions;
	boolean checkTypes;
	Messages messages;
	
	/**
	 * 
	 */
	public Accessor(BT_Accessor site, Messages messages, boolean overridePermissions, boolean checkTypes) {
		this.site = site;
		this.checkTypes = checkTypes;
		this.owner = site.from;
		if(owner == null) { //should never happen, having a call site from non-existent code
			throw new IllegalArgumentException();
		}
		this.overridePermissions = overridePermissions;
		this.messages = messages;
	}
	
	String isValidTarget(BT_Field newTarget, BT_Class newClassTarget) {
		BT_Field oldTarget = site.instruction.getTarget();
		BT_Class oldClassTarget = site.getClassTarget();
		if(newTarget.equals(oldTarget)) {
			return messages.IDENT;
		}
		

		//check type compatibility
		if(checkTypes) {
			BT_Class oldType = oldTarget.getFieldType();
			BT_Class newType = newTarget.getFieldType();
			if(site.instruction.isFieldReadIns()) {
				if(!oldType.isInstance(newType)) {
					return messages.TYPES_INCOMPATIBLE;
				}
			}
			else {
				if(!newType.isInstance(oldType)) {
					return messages.TYPES_INCOMPATIBLE;
				}
			}
		}
		
		if(!newTarget.isStatic()) {
			if(oldTarget.isStatic()) {
				//we need the object and the field type on the stack, which is impossible since there is only one
				//item on the stack
				return messages.ACCESSES_STATIC_TO_NON_STATIC;
			}
			//the object on the stack must be an instance of the new target's class
			if(checkTypes && !newTarget.cls.isInstance(oldTarget.cls)) {
				return messages.TYPES_INCOMPATIBLE;
			}
		}
		
		
		//check visibilities
		
		BT_Class owningClass = owner.getMethod().cls;
			
		//was the original target visible?
		if(!oldTarget.isVisibleFrom(owningClass) || !oldClassTarget.isVisibleFrom(owningClass)) {
			return messages.VISIBILITY_INCOMPATIBLE;
		}
		
		if(!newClassTarget.isVisibleFrom(owningClass)) {
			if(!newTarget.isUnconditionallyVisibleFrom(owningClass)) {
				if(canChangeClass(newClassTarget) 
						&& canChangeTarget(newTarget)) {
					newTarget.becomeVisibleFrom(owningClass);
					newClassTarget.becomeVisibleFrom(owningClass);
				} else {
					return messages.VISIBILITY_INCOMPATIBLE;
				}
			} else {
				if(canChangeClass(newClassTarget)) {
					newClassTarget.becomeVisibleFrom(owningClass);
				} else {
					return messages.VISIBILITY_INCOMPATIBLE;
				}
			}
		} else {
			if(!newTarget.isUnconditionallyVisibleFrom(owningClass)) {
				if(canChangeTarget(newTarget)) {
					newTarget.becomeVisibleFrom(owningClass);
				} else {
					return messages.VISIBILITY_INCOMPATIBLE;
				}
			}
		}
		
		return null;
	}
	
	private static boolean canChangeTarget(BT_Field field) {
		return canChangeClass(field.getDeclaringClass());
	}
	
	static boolean canChangeClass(BT_Class clazz) {
		return ((JaptRepository) clazz.getRepository()).isInternalClass(clazz);
	}
	
	String remap(BT_Field newTarget) {
		JaptRepository rep = (JaptRepository) owner.getMethod().cls.getRepository();
		if(!rep.isInternalClass(owner.getMethod().cls)) {
			return messages.EXTERNAL_CLASS;
		}
		String reason = isValidTarget(newTarget, newTarget.getDeclaringClass());
		if(reason != null) {
			return reason;
		}
		BT_Field oldTarget = site.instruction.getTarget();
		if(newTarget.isStatic()) {
			if(oldTarget.isStatic()) {
				//just change the target, the instruction type is fine
				site.instruction.target = newTarget;
			}
			else {
				changeInstruction(newTarget);
			}
		}
		else { //newTarget is non-static, oldTarget is non-static (because a static oldTarget has been ruled out by isValidTarget)
			site.instruction.target = newTarget;
		}
		return null;
	}
	
	/**
	 * changes a getfield or putfield to a getstatic or putstatic respectively
	 * @param newTarget
	 */
	private void changeInstruction(BT_Field newTarget) {
		BT_FieldRefIns oldIns = site.instruction;
		BT_InsVector instructionVector = owner.getInstructions();
		
		int index = instructionVector.indexOf(oldIns);
		oldIns.unlink(owner);
		instructionVector.removeElementAt(index);
		boolean isRead = oldIns.isFieldReadIns();
		
		//change the xxxfield to xxxstatic
		int newOpcode = isRead ? BT_Opcodes.opc_getstatic : BT_Opcodes.opc_putstatic;
		BT_Ins newFieldAccessIns = BT_Ins.make(newOpcode, newTarget);
		owner.changeReferencesFromTo(oldIns, newFieldAccessIns, false);
		
		instructionVector.insertElementAt(newFieldAccessIns, index);
		newFieldAccessIns.link(owner);
		
		//insert a pop before the instruction for getfield, after the instruction for putfield
		BT_Ins newIns = BT_Ins.make(BT_Opcodes.opc_pop);
		instructionVector.insertElementAt(newIns, isRead ? index : index + 1);
		newIns.link(owner);
		
		
		//change the accessor in the callsite list to reflect the new instruction
		BT_AccessorVector accessedFields = owner.accessedFields;
		int n = accessedFields.size() - 1;
		for (; n >= 0; n--) {
			BT_Accessor s = accessedFields.elementAt(n);
			if (s.instruction == newFieldAccessIns) {
				site = s;
				break;
			}
		}
		if(n < 0) { //we could not find the changed callsite
			throw new RuntimeException();
		}
	
	}

}
