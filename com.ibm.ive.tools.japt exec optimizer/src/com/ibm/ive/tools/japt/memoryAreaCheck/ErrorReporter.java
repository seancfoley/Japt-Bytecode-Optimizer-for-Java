package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.reduction.ita.CreatedObject;
import com.ibm.ive.tools.japt.reduction.ita.DataMember;
import com.ibm.ive.tools.japt.reduction.ita.InstructionLocation;
import com.ibm.ive.tools.japt.reduction.ita.Member;
import com.ibm.ive.tools.japt.reduction.ita.Method;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocation;
import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.ive.tools.japt.reduction.ita.Repository;
import com.ibm.ive.tools.japt.reduction.ita.ObjectPropagator.PropagationAction;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_InsVector;

public class ErrorReporter {

	com.ibm.ive.tools.japt.ErrorReporter listener;
	BT_InsVector invocationInitiators = new BT_InsVector();
	BT_InsVector accessInitiators = new BT_InsVector();
	boolean checkExternal;
	
	public ErrorReporter(com.ibm.ive.tools.japt.ErrorReporter listener, boolean checkExternal) {
		this.listener = listener;
		this.checkExternal = checkExternal;
	}
	
	CreatedObject findCreator(PropagatedObject object) {
		Repository rep = object.getType().repository;
		return rep.findCreator(object);
	}
	
	public void noteInvocationError(
			ErrorWrapper error, 
			PropagatedObject object,
			Member to,
			MethodInvocation initiator,
			InstructionLocation initiatorLocation) {
		//TODO use messages and not the logger directly
		if(listener != null && listener.logging() && (checkExternal 
				|| (initiator != null && initiator.getDefiningClass().isInternal()) 
				|| to.getDeclaringClass().isInternal())
				&& !invocationInitiators.contains(initiatorLocation.instruction)) {
			invocationInitiators.addElement(initiatorLocation.instruction);
			listener.logError(++listener.errorCount + ": potential " + error + Logger.endl);
			//TODO provide option -noLoadExternalDebug instead of just -noLoadDebug
			listener.logError("\t" + ObjectPropagator.INVOKED.toString(object, to, initiator) + Logger.endl);
			
			Method initiatorMethod = initiator.getMethod();
			BT_CodeAttribute code = initiatorMethod.getCode();
			int num = code.findLineNumber(initiatorLocation.instruction);
			if(num > 0) {
				listener.logError("\tinitiated at line number " 
						+ num + " of " + code.getMethod().getDeclaringClass().getSourceFile()
						+ " inside " + initiatorMethod + Logger.endl);
			}
			CreatedObject origin = findCreator(object);
			if(origin != null) {
				listener.logError("\t" + origin + Logger.endl);
			}
		}
	}
	
	public void noteReadAccessError(
			ErrorWrapper error, 
			PropagatedObject object,
			MethodInvocation to,
			InstructionLocation toLocation,
			DataMember from,
			PropagationAction action) {
		String actionString = action.toString(object, to, from);
		noteAccessError(error, object, from, to, toLocation, actionString, null);
	}
	
	public void noteWriteAccessError(
			ErrorWrapper error, 
			PropagatedObject object,
			DataMember to,
			MethodInvocation from,
			InstructionLocation fromLocation,
			PropagationAction action,
			PropagatedObject targetObject) {
		String actionString = action.toString(object, to, from);
		if(noteAccessError(error, object, to, from, fromLocation, actionString, targetObject)) {
			CreatedObject targetOrigin = null;
			if(targetObject != null) {
				targetOrigin = findCreator(targetObject);
				if(targetOrigin != null) {
					listener.logError("\t" + targetOrigin + Logger.endl);
				}
			}
		}
	}
	
	private boolean noteAccessError(
			ErrorWrapper error, 
			PropagatedObject object,
			DataMember field,
			MethodInvocation initiator,
			InstructionLocation initiatorLocation,
			String action,
			PropagatedObject targetObject) {
		if(listener != null && listener.logging() && 
				(checkExternal || (initiator != null && initiator.getDefiningClass().isInternal()) || field.getDefiningClass().isInternal())
				&& !accessInitiators.contains(initiatorLocation.instruction)) {
			accessInitiators.addElement(initiatorLocation.instruction);
			listener.logError(++listener.errorCount + ": potential " + error + Logger.endl);
			listener.logError("\t" + action + Logger.endl);
			
			Method initiatorMethod = initiator.getMethod();
			BT_CodeAttribute code = initiatorMethod.getCode();
			int num = code.findLineNumber(initiatorLocation.instruction);
			if(num > 0) {
				listener.logError("\tinitiated at line number " 
						+ num + " of " + code.getMethod().getDeclaringClass().getSourceFile()
						+ " inside " + initiatorMethod + Logger.endl);
			}
			CreatedObject origin = findCreator(object);
			if(origin != null) {
				listener.logError("\t" + origin + Logger.endl);
			}
			return true;
		}
		return false;
	}
	
	
	
}
