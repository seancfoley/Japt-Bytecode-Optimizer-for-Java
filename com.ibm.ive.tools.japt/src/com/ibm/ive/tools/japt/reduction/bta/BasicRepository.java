package com.ibm.ive.tools.japt.reduction.bta;

import com.ibm.ive.tools.japt.reduction.GenericReducer;
import com.ibm.ive.tools.japt.reduction.xta.Clazz;
import com.ibm.ive.tools.japt.reduction.xta.Field;
import com.ibm.ive.tools.japt.reduction.xta.Method;
import com.ibm.ive.tools.japt.reduction.xta.Repository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Method;

/**
 * @author sfoley
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class BasicRepository extends Repository {

	
	/**
	 * Constructor for RTARepository.
	 * @param props
	 * @param relatedMethodMap
	 * @param rta
	 */
	public BasicRepository(GenericReducer red) {
		super(red);
	}
	
	protected Clazz constructClass(BT_Class clz) {
		return new BasicClass(this, clz);
	}
	
	protected Method constructMethod(BT_Method method) {
		return new BasicMethod(this, method);
	}
	
	protected Field constructField(BT_Field field) {
		return new BasicField(this, field);
	}
	
	
}
