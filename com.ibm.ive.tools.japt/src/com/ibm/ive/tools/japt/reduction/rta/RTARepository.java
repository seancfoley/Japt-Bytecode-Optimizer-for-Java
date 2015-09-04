package com.ibm.ive.tools.japt.reduction.rta;

import com.ibm.ive.tools.japt.reduction.GenericReducer;
import com.ibm.ive.tools.japt.reduction.SimpleTreeSet;
import com.ibm.ive.tools.japt.reduction.xta.ArrayElement;
import com.ibm.ive.tools.japt.reduction.xta.Clazz;
import com.ibm.ive.tools.japt.reduction.xta.Field;
import com.ibm.ive.tools.japt.reduction.xta.Method;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_Method;

/**
 * @author sfoley
 *
 */
public class RTARepository extends Repository {

	private BT_HashedClassVector newlyPropagatedObjects = new BT_HashedClassVector();
	private BT_HashedClassVector propagatedObjects = new BT_HashedClassVector();
	private SimpleTreeSet allPropagatedObjects = new SimpleTreeSet();
	
	/**
	 * Constructor for RTARepository.
	 * @param props
	 * @param relatedMethodMap
	 * @param rta
	 */
	public RTARepository(GenericReducer red) {
		super(red);
	}
	
	/**
	 * Newly constructed objects are changed to oldly constructed objects.
	 * @return true if newly constructed objects existed and such a migration took place, false otherwise
	 */
	boolean migrateObjects() {
		boolean result = false;
		if(propagatedObjects.size() > 0) {
			for(int i=0; i<propagatedObjects.size(); i++) {
				allPropagatedObjects.add(propagatedObjects.elementAt(i));
			}
			propagatedObjects.removeAllElements();
			result = true;
		}
		if(newlyPropagatedObjects.size() > 0) {
			for(int i=0; i<newlyPropagatedObjects.size(); i++) {
				propagatedObjects.addElement(newlyPropagatedObjects.elementAt(i));
			}
			newlyPropagatedObjects.removeAllElements();
			result = true;
		}
		return result;
	}

	protected ArrayElement constructArrayElement(Clazz declaringClass, BT_Class elementClass) {
		ArrayElement result =  new RTAArrayElement(declaringClass, elementClass, propagatedObjects, allPropagatedObjects);
		result.scheduleRepropagation();
		return result;
	}
	
	protected Method constructMethod(BT_Method method) {
		Method result =  new RTAMethod(this, method, newlyPropagatedObjects, propagatedObjects, allPropagatedObjects);
		result.scheduleRepropagation();
		return result;
	}
	
	protected Field constructField(BT_Field field) {
		Field result =  new RTAField(this, field, propagatedObjects, allPropagatedObjects);
		result.scheduleRepropagation();
		return result;
	}
	
	
}
