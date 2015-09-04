/*
 * Created on Mar 30, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reorderTest;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.jikesbt.BT_ClassComparator;

public class ReorderExtension implements CommandLineExtension {
	
	private FlagOption slow = new FlagOption("slow", "slow class order");
	
	public Option[] getOptions() {
		return new Option[] {slow};
	}

	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		repository.sortClasses(slow.isFlagged() ? new SlowExecutionClassComparator() : 
			(BT_ClassComparator) new FastExecutionClassComparator());
	}
	
	
	
	public String getName() {
		return "Reorder extension";
	}

}