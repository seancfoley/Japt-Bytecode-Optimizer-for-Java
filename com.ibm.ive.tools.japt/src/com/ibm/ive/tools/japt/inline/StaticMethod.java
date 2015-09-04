package com.ibm.ive.tools.japt.inline;

import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodRefIns;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class StaticMethod extends Method {

	public StaticMethod(BT_Method method, InlineRepository inlineRep, boolean inlineFromAnywhere) {
		super(method, inlineRep, inlineFromAnywhere);
	}
	
	MethodCallSite getMethodCallSite(InliningCodeAttribute attr, 
			BT_MethodRefIns callSiteInstruction, 
			boolean overridePermissions) {
		
		return new MethodCallSite(
				attr, 
				callSiteInstruction, 
				callSiteInstruction.target,
				overridePermissions,
				inlineFromAnywhere);
		
	}
}
