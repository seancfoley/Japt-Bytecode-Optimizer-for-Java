/*
 * Created on May 8, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodCallSite;

public class AccessorCallSite {
	public final BT_MethodCallSite site;
	public final AccessorMethod method;
	
	public AccessorCallSite(BT_MethodCallSite site, AccessorMethod method) {
		if(!site.getTarget().equals(method.method)) {
			throw new IllegalArgumentException();
		}
		this.site = site;
		this.method = method;
	}
	
	public BT_Ins getInstruction() {
		return site.getInstruction();
	}
	
	public BT_Method getFrom() {
		return site.getFrom();
	}

	public AccessorMethod getTarget() {
		return method;
	}
	
	public BT_Class getClassTarget() {
		return site.getClassTarget();
	}
	
	public String toString() {
		return site.toString();
	}
	
	
}
