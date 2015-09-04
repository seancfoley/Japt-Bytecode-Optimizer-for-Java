package com.ibm.ive.tools.japt.testcase.mergeCandidates;

import com.ibm.ive.tools.japt.testcase.mergeCandidates.VisibleSuper.Sub1;
import com.ibm.ive.tools.japt.testcase.mergeCandidates.VisibleSuper.Sub2;

public class App {
	public void testVerifier(boolean condition) {
		VisibleSuper sup;
		if (condition) {
			sup = new Sub1();
		} else {
			sup = new Sub2();
		}
		sup.access();
		sup.toString();
	}
}
