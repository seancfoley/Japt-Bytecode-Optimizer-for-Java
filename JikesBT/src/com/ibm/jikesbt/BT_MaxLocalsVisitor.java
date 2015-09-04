/*
 * Created on Oct 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;




/**
 * @author sfoley
 *
 * This code vistor object simulates the change in the locals and the operand stack
 * as the code in a method is executed.
 * <p>
 */
public class BT_MaxLocalsVisitor extends BT_CodeVisitor {
	private int maxLocals = 0;
	
	public int getMaxLocals() {
		return maxLocals;
	}
	
	protected void setUp() {
		BT_Method method = code.getMethod();
		if(method != null) {
			maxLocals = method.getArgsSize();
		}
		super.setUp();
	}
	
	protected boolean visit(
			final BT_Ins currentIns, 
			int iin, 
			BT_Ins previousInstruction, 
			int prev_iin, 
			BT_ExceptionTableEntry handler) {
		BT_LocalIns loadIns;
		if(currentIns instanceof BT_StoreLocalIns) {
			loadIns = (BT_LocalIns) currentIns;
			if(loadIns.is2Slot()) {
				maxLocals = Math.max(maxLocals, loadIns.target.localNr + 2);
			} else {
				maxLocals = Math.max(maxLocals, loadIns.target.localNr + 1);
			}
		} else if(currentIns.isLocalReadIns()) {
			if (currentIns instanceof BT_LoadLocalIns) {
				loadIns = (BT_LocalIns) currentIns;
				if(loadIns.is2Slot()) {
					maxLocals = Math.max(maxLocals, loadIns.target.localNr + 2);
				} else {
					maxLocals = Math.max(maxLocals, loadIns.target.localNr + 1);
				}
			} else if (currentIns instanceof BT_RetIns) {
				maxLocals = Math.max(maxLocals, ((BT_RetIns) currentIns).target.localNr + 1);
			} else if (currentIns instanceof BT_IIncIns) {
				maxLocals = Math.max(maxLocals, ((BT_IIncIns) currentIns).target.localNr + 1);
			} 
		}
		return true;
	}
}
