package com.ibm.ive.tools.japt.reduction.xta;

import com.ibm.jikesbt.*;

/**
 * @author sfoley
 *
 */
public class MethodPotentialArrayTargets implements MethodPotentialTargets {

	Method method;
	
	/**
	 * Constructor for MethodArrayPropagationSources.
	 */
	public MethodPotentialArrayTargets(Method method) {
		this.method = method;
	}
	
	boolean hasNoSources() {
		return !(method.storesIntoArrays() || method.loadsFromArrays());
	}
	
	public void findTargets(BT_CodeAttribute code) {}

	public void findNewTargets(BT_Class objectType) {
		if(!objectType.isArray()) {
			return;
		}
		
			
		//for each possible element type we propagate
				
		ArrayElement element = method.getRepository().getArrayElement(objectType);
		do {
			if(method.storesIntoArrays()) {
				element.setAccessed();
				if(!method.hasPreviouslyWrittenTo(element)) {
					method.addWrittenDataMember(element);
				}
				if(method.loadsFromArrays()) {
					if(!element.hasPreviouslyBeenReadBy(method)) {
						element.addReadingMethod(method);
					}
				}
			}
			else if(method.loadsFromArrays()) {
				element.setAccessed();
				if(!element.hasPreviouslyBeenReadBy(method)) {
					element.addReadingMethod(method);
				}
			}
			element = element.subType;
		} while(element != null);
	}
}
