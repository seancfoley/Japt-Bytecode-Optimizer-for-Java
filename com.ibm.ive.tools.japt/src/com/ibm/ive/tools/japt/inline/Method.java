package com.ibm.ive.tools.japt.inline;

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.ive.tools.japt.JaptMethod;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_HashedMethodCallSiteVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;
import com.ibm.jikesbt.BT_MethodCallSiteVector;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public abstract class Method {

	protected BT_Method method;
	private Boolean canInlineMethod;
	protected boolean inlineFromAnywhere;
	
	/* contains duplicate code to test an inline into this method */ 
	private InliningCodeAttribute testingCodeAttribute;
	
	private boolean inlinedJSRs;
	private boolean attemptedInlinedJSRs;
	
	protected InlineRepository inlineRep;
	
	/**
	 * Constructor for Method.
	 */
	public Method(BT_Method method, InlineRepository inlineRep, boolean inlineFromAnywhere) {
		this.method = method;
		this.inlineRep = inlineRep;
		this.inlineFromAnywhere = inlineFromAnywhere;
	}
	
	public String toString() {
		return method.toString();
	}
	
	JaptRepository getRepository() {
		return (JaptRepository) method.getDeclaringClass().getRepository();
	}

	public boolean isInlinable() {
		if(canInlineMethod == null) {
			canInlineMethod = 
				(method.getCode() == null 
				|| method.isAbstract()
				|| method.isSynchronized() //TODO remove this condition when inliner can handle synchronized methods, which is no small matter
				|| !(inlineFromAnywhere || getRepository().isInternalClass(method.cls)) 
				|| method.isStaticInitializer()
				|| method.isConstructor()
				|| method.isRecursive(false, false)  //TODO recheck this is recursive call which I have just refactored
				) 
					? Boolean.FALSE : Boolean.TRUE;
		}
		return canInlineMethod.booleanValue();
	}
	
	/**
	 * inline this method at all call sites
	 * 
	 * @param overridePermissions if a method cannot be inlined at a particular 
	 * callsite because of access permissions, change the relevant permissions to public and proceed
	 */
	public InlineReport inline(boolean overridePermissions, boolean alterTarget, BT_HashedMethodCallSiteVector uninlinableCallSites) throws BT_CodeException {
		if(isInlinable()) {
			BT_MethodCallSiteVector allSites = ((JaptMethod) method).getAllCallSites();
			for (int j = allSites.size() - 1; j >= 0; j--) {
				if(uninlinableCallSites.contains(allSites.elementAt(j))) {
					allSites.removeElementAt(j);
				}
			}
			if(allSites.size() > 0) {
				return inlineCallSites(allSites.toArray(), overridePermissions, alterTarget, true, uninlinableCallSites);
			}
		}
		return null;
	}
	
	boolean isJSRInlinable() {
		return !attemptedInlinedJSRs 
		&& method.getCode() != null 
		&& !method.isAbstract()
		&& !method.isNative()
		&& getRepository().isInternalClass(method.getDeclaringClass());
	}
	
	boolean hasAttemptedInlineJSRs() {
		return attemptedInlinedJSRs;
	}
	
	boolean inlineJSRs() throws BT_CodeException {
		if(isJSRInlinable()) {
			attemptedInlinedJSRs = true;
			BT_CodeAttribute code = method.getCode();
			if(inlinedJSRs = code.inlineJsrs()) {
				code.optimizeAndRemoveDeadCode(false);
			}
		}
		return inlinedJSRs;
	}
	
	static class InlineReport {
		InlineRepository repository;
		final BT_MethodCallSiteVector callSitesInlined;
		final BT_MethodVector methodsInlinedInto;
		final BT_MethodVector methodsInlined;
		int totalSites;
		
		InlineReport(InlineRepository repository) {
			this(repository, new BT_MethodVector(), new BT_MethodVector(), new BT_MethodCallSiteVector(), 0);
		}
		
		InlineReport(InlineRepository repository, BT_Method methodInlined, BT_MethodVector methodsInlinedInto, BT_MethodCallSiteVector sitesInlined, int totalSites) {
			this(repository, new BT_MethodVector(1), methodsInlinedInto, sitesInlined, totalSites);
			methodsInlined.addElement(methodInlined);
		}
		
		private InlineReport(InlineRepository repository, BT_MethodVector methodsInlined, BT_MethodVector methodsInlinedInto, BT_MethodCallSiteVector sitesInlined, int totalSites) {
			this.callSitesInlined = sitesInlined;
			this.methodsInlinedInto = methodsInlinedInto;
			this.totalSites = totalSites;
			this.methodsInlined = methodsInlined;
			this.repository = repository;
		}
		
		void optimizeMethodsInlinedInto() {
			BT_MethodVector methodsInlinedInto = this.methodsInlinedInto;
			for(int i=0; i<methodsInlinedInto.size(); i++) {
				BT_Method method = methodsInlinedInto.elementAt(i);
				// 
				/*
				We need to check if the code is null:
				class C {
					void a() {b();}
					private void b() {c();}
					private void c() {x=1;}
				}
				
				inline c into b:
				class C {
					void a() {b();}
					private void b() {x=1;}
					private void c() {x=1;}
				}
				inlined into: b
				
				now inline b into a:
				class C {
					void a() {x=1;}
					private void b() {x=1;}
					private void c() {x=1;}
				}
				inlined into: b, a
				
				now remove unused methods:
				class C {
					void a() {x=1;}
				}
				Removed and code removed: b, c
				inlined into: b, a
				
				Method b shows that our list of methods inlined into might contains methods that no longer have code.
				*/
				BT_CodeAttribute code = method.getCode();
				if(code != null) {
					try {
						code.optimizeAndRemoveDeadCode(false);
					} catch(BT_CodeException e) {
						repository.rep.getFactory().noteCodeException(e);
					}
				}
			}
		}
	}
	
	private InlineReport inlineCallSites(BT_MethodCallSite callSites[], 
										 boolean overridePermissions, 
										 boolean alterTarget, 
										 boolean provokeInitialization,
										 BT_HashedMethodCallSiteVector uninlinableCallSites) {
		int totalSites = callSites.length;
		BT_HashedMethodVector methodsInlinedInto = null;
		BT_MethodCallSiteVector callSitesInlined = null;
		
		for (int j = callSites.length - 1; j >= 0; j--) {
			BT_MethodCallSite site = callSites[j];
			BT_Method methodCalling = site.getFrom();
			boolean succeeded = inlineCallSite(site, callSites, overridePermissions, alterTarget, provokeInitialization);
			if(succeeded) {
				if(methodsInlinedInto == null) {
					methodsInlinedInto = new BT_HashedMethodVector(totalSites);
					callSitesInlined = new BT_MethodCallSiteVector(totalSites);
				}
				methodsInlinedInto.addUnique(methodCalling);
				callSitesInlined.addElement(site);
			} else {
				if(uninlinableCallSites != null) {
					uninlinableCallSites.addElement(site);
				}
			}
			callSites[j] = null;
			
		}
		if(methodsInlinedInto != null) {
			return new InlineReport(inlineRep, method, methodsInlinedInto, callSitesInlined, totalSites);
		}
		return null;
	}
	
	private boolean inlineCallSite(
			BT_MethodCallSite site, 
			BT_MethodCallSite callSites[], 
			boolean overridePermissions,
			boolean alterTarget,
			boolean provokeInitialization) {
		BT_Method from = site.getFrom();
		Method fromMethod = inlineRep.getMethod(from);
		InliningCodeAttribute attr = new InliningCodeAttribute(from.getCode());
		MethodCallSite callSite = getMethodCallSite(attr, site.instruction, overridePermissions);
		boolean result = false;
		try {
			if(callSite.isInlinable()) {
				
				//get a new code attribute for purposes of inlining, we need to do so because there
				//is the change that the new code is invalid (for instance, it may be too large
				//to be a valid code attribute)
				callSite = fromMethod.getDuplicateCallSite(this, site.instruction, overridePermissions);
				callSite.isInlinable = Boolean.TRUE; //we just checked if the original callsite is inlinable
						
				try {
					if(result = callSite.inline(alterTarget, provokeInitialization)) {
						replaceCallSites(site, callSites, callSite);
						finalizeInline(fromMethod);
					} else {
						reverseInline(fromMethod);
					}
				} catch(BT_CodeException e) {
					inlineRep.rep.getFactory().noteCodeException(e);
					reverseInline(fromMethod);
				}
			}
		} catch(BT_CodeException e) {
			inlineRep.rep.getFactory().noteCodeException(e);
		}
		return result;
	}

	/**
	 * @param site
	 * @param callSites
	 * @param callSite
	 */
	private void replaceCallSites(BT_MethodCallSite site, BT_MethodCallSite[] callSites, MethodCallSite callSite) {
		//We are about to replace the old code attribute of fromMethod with the new one that has the method inlined.
		//However, the calllSites[] vector contains a list of other call sites we intend to inline and some of them
		//might also exist in the old code attribute of fromMethod.  So we need to replace such list items
		//with the equivalent call site in the new code attribute.
		topLoop:
		for(int i=0; i<callSites.length; i++) {
			BT_MethodCallSite otherSite = callSites[i];
			if(otherSite == null || otherSite == site) {
				continue;
			}
			if(otherSite.from.equals(site.from)) {
				//this other site comes from the same code attribute
				//so we need to replace the call site in the list with the new call site
				InliningCodeAttribute ica = callSite.inliningCode;
				BT_MethodRefIns mappedInstruction = (BT_MethodRefIns) ica.getEquivalentInstruction(otherSite.instruction);
				
				//find the equivalent call site
				BT_MethodCallSiteVector currentSites = method.callSites;
				for(int j=0; j<currentSites.size(); j++) {
					BT_MethodCallSite newSite = currentSites.elementAt(j);
					if(newSite.instruction.isSame(mappedInstruction)) {
						callSites[i] = newSite;
						continue topLoop;
					}
				}
				throw new RuntimeException("cannot find equivalent call site");
				
			}
		}
	}
	
	
	abstract MethodCallSite getMethodCallSite(
			InliningCodeAttribute attr, 
			BT_MethodRefIns callSiteInstruction, 
			boolean overridePermissions);
	
	/**
	 * @param overridePermissions whether access permissions can be changed
	 * @return the report detailing which call sites of the method were inlined.
	 * If null is returned, then the method was not inlinable or inlining would 
	 * provide no performance benefits.
	 */
	InlineReport inlineForPerformance(
			BT_MethodCallSite callSites[],  boolean overridePermissions, final int bytecodeSizeThreshold) {
		if(!isInlinable()) {
			return null;
		}
		BT_CodeAttribute code = method.getCode();
		int byteCodeSize = code.computeMaxInstructionSizes();
		//int byteCodeSize = code.bytecodeSize();
		if(byteCodeSize > bytecodeSizeThreshold) {
			return null;
		}  
		return inlineCallSites(callSites, overridePermissions, false, false, null);	
	}
	
	
	/**
	 * @param overridePermissions whether access permissions can be changed
	 * @return the report detailing which call sites of the method were inlined.
	 * If null is returned, then the method was not inlinable or inlining would
	 * provide no compression benefits.
	 */
	InlineReport inlineForCompression(BT_MethodCallSite callSites[], boolean overridePermissions) {
		if(!isInlinable()) {
			return null;
		}
		JaptRepository repository = getRepository();
		if(repository.getInternalClassesInterface().isInEntireInterface(method) 
			|| !repository.isInternalClass(method.getDeclaringClass())
			|| repository.methodFulfillsClassRequirements(method, false)) {
			//the method to be inlined cannot be removed, so there will be no compression benefits
			return null;
		}

		int callSiteCount = callSites.length;
		if(callSiteCount == 0) {
			method.remove();
			return new InlineReport(inlineRep, method, new BT_MethodVector(0), new BT_MethodCallSiteVector(0), 0);
		} else if(callSiteCount == 1) {
			BT_MethodCallSite site = callSites[0];
			BT_Method siteMethod = site.getFrom();
			if(inlineCallSite(site, callSites, overridePermissions, false, true)) {
				BT_MethodVector res = new BT_MethodVector(1);
				BT_MethodCallSiteVector res2 = new BT_MethodCallSiteVector(1);
				res.addElement(siteMethod);
				res2.addElement(site);
				method.remove();
				return new InlineReport(inlineRep, method, res, res2, 1);
			}
			return null;
		}
		
		/* 
		 * Here is the structure of a method:
			method_info {
    			u2 access_flags;
    			u2 name_index;
    			u2 descriptor_index;
    			u2 attributes_count;
    			attribute_info attributes[attributes_count];
    		}
    		
    		and an attribute:
    		attribute_info {
    			u2 attribute_name_index;
    			u4 attribute_length;
    			u1 info[attribute_length];
    		}
	

			Typically, we are interested in the Code and Exceptions (lists checked 
			exceptions a method may throw), and indirectly the LineNumberTable and
			LocalVariableTable attributes which appear for debugging purposes
			as part of the code attribute.
			
			We shall assume that in the interest of compression the debugging
			attributes will not exist, or that they shall be removed.
			
			The code attribute:
			Code_attribute {
		    	u2 attribute_name_index;
		    	u4 attribute_length;
		    	u2 max_stack;
		    	u2 max_locals;
		    	u4 code_length;
		    	u1 code[code_length];
		    	u2 exception_table_length;
		    	{    	u2 start_pc;
		    	      	u2 end_pc;
		    	      	u2  handler_pc;
		    	      	u2  catch_type;
		    	}	exception_table[exception_table_length];
		    	u2 attributes_count;
		    	attribute_info attributes[attributes_count];
		    }
		    
		    The exceptions attribute:
		    Exceptions_attribute {
		    	u2 attribute_name_index;
		    	u4 attribute_length;
		    	u2 number_of_exceptions;
		    	u2 exception_index_table[number_of_exceptions];
		    }

			We will save space by avoiding the method info structure:  assuming there
			are no checked exceptions, we can save 8 bytes from the method_info structure
			and a minimum of 14 (members up to code) + 2 (exception table length) + 2 (attributes
			count), so we are saving 18 bytes.
			
			The total savings is 26 bytes.
		 * 
		 * We save 26 bytes in the method structure size when we inline a method, and
    	 * we remove the bytecodes from the original method and supplant a similar copy
    	 * (a few instructions and changed, some are added, some are removed) in the
    	 * body of the calling methods at each callsite.
    	 * So we save approximately, in bytes:
    	 *  savings = 22 - (x - 1)b  where x is the number of callsites, b is the bytecode
    	 * size of the inlined method.  For any method with just a single callsite, inlining will
    	 * save space, while for x > 2, we have
    	 * savings > 0 
    	 * iff 22 - b(x - 1) > 0
    	 * or b < 22/(x - 1)
    	 * 
    	 * In the end, it may turn out that we cannot inline all callsites, in which case
    	 * there is no savings.
    	 */
    	 
    	 /*
    	  * Note: The calculation used does not take into account variations in constant pool size.
    	  * In particular, if a method is inlined into several places, then all of its constant pool references may or may not
    	  * be duplicated, thus possibly increasing the size of the deliverable.  However, it is also technically possible
    	  * that this contributes to a deliverable of decreased size by removing constant pool entries, by making duplication
    	  * possible in the inlined sites and by removing the constant pool entry for the removed method.
    	  * 
    	  * An improved algorithm would take note of these eventualities.  To be noted:
    	  * - inlining within the same class would never increase constant pool size, only decrease
    	  * - the actual method reference removed in the inlining code removes a constant pool reference
    	  * - when inlining to a single callsite we might conclude that it all comes out in the wash:  we do not
    	  * concern oursleves with which class holds the relevant constant pool entries 
    	  * 
    	  */
    	final int minMethodStructureSize = 26;
    	
		BT_CodeAttribute code = method.getCode();
		int byteCodeSize = code.computeMaxInstructionSizes();
		if(byteCodeSize >= (minMethodStructureSize / (callSiteCount - 1))) {
			return null;
		}  
		
		HashSet inliningMethods = new HashSet();
		
		//we have a potential candidate for inlining
		
		//note that as sites are inlined they might disappear from the vector callSites
		//when the inlines are finalized and the call site instructions are removed.
		int siteSize = callSites.length;
		for(int i=siteSize - 1; i>=0; i--) {
			BT_MethodCallSite site = callSites[i];
			BT_Method from = site.getFrom();
			
			Method fromMethod = inlineRep.getMethod(from);
			InliningCodeAttribute attr = new InliningCodeAttribute(from.getCode());
			MethodCallSite callSite = getMethodCallSite(attr, site.instruction, overridePermissions);
			try {
				if(callSite.isInlinable()) {
					
					/* Note: using a Set ensures we do not add the same method twice */
					inliningMethods.add(fromMethod);
					
					//get a new code attribute for purposes of inlining
					
					callSite = fromMethod.getDuplicateCallSite(this, site.instruction, overridePermissions);
					callSite.isInlinable = Boolean.TRUE; //we already checked if the callsite is inlinable
					if(!callSite.inline(false, true)) {
						reverseInlines(inliningMethods);
						return null;
					}
				}
				else {
					reverseInlines(inliningMethods);
					return null;
				}
			} catch(BT_CodeException e) {
				repository.getFactory().noteCodeException(e);
				reverseInlines(inliningMethods);
				return null;
			}
		}
		
		
		//we want to check that
		//22 + bytecodesize of inlined method + sum (inlining method bytecode size before inlining)
		// > sum (inlining method bytecode size after inlining)
		//if so, then we will finalize all inlines...
		
		Iterator iterator = inliningMethods.iterator();
		int diff = minMethodStructureSize; //start with the method structure size
		BT_CodeAttribute inlinedCode = method.getCode();
		diff += inlinedCode.computeMaxInstructionSizes();
		while(iterator.hasNext()) {
			Method next = (Method) iterator.next();
			
			//calculate the bytecode size of both the inlined attribute and the previous...
			BT_CodeAttribute newCode = next.testingCodeAttribute.getCode();
			BT_CodeAttribute oldCode = next.method.getCode();
			diff += oldCode.computeMaxInstructionSizes() - newCode.computeMaxInstructionSizes();
		
		}
		
		if(diff <= 0) {
			reverseInlines(inliningMethods);
			return null;
		}
		
		//finalize the inlines
		BT_MethodVector result = new BT_MethodVector(inliningMethods.size());
		iterator = inliningMethods.iterator();
		while(iterator.hasNext()) {
			Method next = (Method) iterator.next();
			finalizeInline(next);
			result.addElement(next.method);
		}
		method.remove();
		
		BT_MethodCallSiteVector sitesInlined = new BT_MethodCallSiteVector(callSites.length);
		sitesInlined.copyInto(callSites);
		return new InlineReport(inlineRep, method, result, sitesInlined, siteSize);
	}
	
	
	
	private void finalizeInline(Method inliningMeth) {
		//MethodCallSite.errorStream.println("finalizing");
		BT_Method inliningMethod = inliningMeth.method;
		BT_CodeAttribute oldCode = inliningMethod.getCode();
		BT_CodeAttribute newCode = inliningMeth.testingCodeAttribute.getCode();
		//removing the instructions backs out of any instruction related relationships stored in JikesBT
		oldCode.removeAllInstructions();
		inliningMethod.setCode(newCode);
		inliningMeth.testingCodeAttribute = null;
	}
					
	private void reverseInlines(HashSet inliningMethods) {
		Iterator iterator = inliningMethods.iterator();
		while(iterator.hasNext()) {
			Method next = (Method) iterator.next();
			reverseInline(next);
		}
	}
	
	private void reverseInline(Method next) {
		//MethodCallSite.errorStream.println("reversing");
		next.testingCodeAttribute.getCode().remove();
		next.testingCodeAttribute = null;
	}
	
	MethodCallSite getDuplicateCallSite(Method target, BT_MethodRefIns ins, boolean overridePermissions) {
		if(testingCodeAttribute == null) {
			BT_CodeAttribute code = method.getCode();
			BT_CodeAttribute inlinedAttribute = (BT_CodeAttribute) code.clone();
			inlinedAttribute.resetCachedCodeInfo();
			testingCodeAttribute = new InliningCodeAttribute(inlinedAttribute, code);
		}
		BT_MethodRefIns newIns = (BT_MethodRefIns) testingCodeAttribute.getEquivalentInstruction(ins);
		return target.getMethodCallSite(testingCodeAttribute, newIns, overridePermissions);
	}
	
}
