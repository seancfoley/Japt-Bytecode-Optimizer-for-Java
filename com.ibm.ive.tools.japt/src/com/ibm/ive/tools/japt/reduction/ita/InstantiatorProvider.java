package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_Class;

public interface InstantiatorProvider {
	Instantiator getInstantiator(BT_Class clazz);

}
