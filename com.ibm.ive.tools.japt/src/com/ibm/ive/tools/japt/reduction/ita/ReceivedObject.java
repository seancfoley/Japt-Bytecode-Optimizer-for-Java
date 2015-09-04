package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.ObjectSet.Entry;

public interface ReceivedObject extends Entry {
	PropagatedObject getObject();
	
	MethodInvocationLocation getLocation();
	
	int hashCode();
}
